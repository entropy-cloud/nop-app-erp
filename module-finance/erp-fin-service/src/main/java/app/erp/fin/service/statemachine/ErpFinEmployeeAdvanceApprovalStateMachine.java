package app.erp.fin.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 员工借款单（{@code ErpFinEmployeeAdvance}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code approveStatus}
 * 审批轴，平台共享字典 {@code wf/approve-status} 4 值：UNSUBMITTED/SUBMITTED/APPROVED/REJECTED）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/finance/state-machine.md} §对象六 + {@code docs/design/finance/expense-claim.md}。
 *
 * <p><b>治理裁定（§11.2 M4 plan-first，plan 2026-08-13-1146-3）</b>：借款 approve 触发业财过账
 * （{@code EmployeeAdvancePostingDispatcher}→{@code FinPostingExecutor} 生成 EMPLOYEE_ADVANCE 凭证 + {@code ErpFinArApItem}
 * RECEIVABLE 1221 其他应收-员工预支），reverseApprove/cancel 触发红冲。依契约 §11.2 M4 硬约束
 * (i)–(v)：过账时序/编排/失败兜底继续由 {@code EmployeeAdvancePostingDispatcher}→{@code FinPostingExecutor} +
 * {@code posted} 标志契约管理（§11.2 M4 (ii)/(v)），Bean 不触碰；跨域副作用（ArApItem 生成、被报销单冲抵引用、
 * 红冲）保留原 Processor 路径（§11.2 M4 (iv)）；{@code posted} 不入轴（§11.2 M4 (iii)）；
 * SoD（approver-is-creator）为动态业务守卫保留原位（非 Bean 范畴，架构契约 {@code entity-state-machine-bean.md:274}）。
 * 本实体为费用报销冲抵的<b>被冲抵方</b>（冲抵由 ExpenseClaim 侧 {@code AdvanceOffsetOrchestrator} 按 advance code 驱动，
 * 本 Bean 无 offset 调用）。
 *
 * <p>命名带 {@code Approval} 后缀（契约 §1 双轴约定，与 {@code ErpFinEmployeeAdvanceDocumentStateMachine} docStatus
 * 轴分离；docStatus dict 中 SUBMITTED/APPROVED/REJECTED 为残余值不纳入 Document Bean）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。
 *
 * <p><b>5 命名动作迁移矩阵</b>：
 * <ul>
 *   <li>{@code submitForApproval} {UNSUBMITTED,REJECTED}→SUBMITTED（{@code null} 归一化为 UNSUBMITTED，初始提交或
 *       驳回后重新提交）；</li>
 *   <li>{@code withdrawApproval} {SUBMITTED}→UNSUBMITTED；</li>
 *   <li>{@code approve} {SUBMITTED}→APPROVED；</li>
 *   <li>{@code reject} {SUBMITTED}→REJECTED；</li>
 *   <li>{@code reverseApprove} {APPROVED}→REJECTED（已合规 domain-design-guidelines §16.4，无 drift）。</li>
 * </ul>
 *
 * <p><b>终态</b>：{@code APPROVED}/{@code REJECTED}（均为可逆终态——APPROVED 经 reverseApprove 有出边、
 * REJECTED 经 submitForApproval 有出边，不适用「终态无出边」强可达性断言，见矩阵测试）。initial：{@code UNSUBMITTED}。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor（契约 §7）。
 */
public class ErpFinEmployeeAdvanceApprovalStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * submitForApproval 入口守卫：来源态为 {@code UNSUBMITTED}/{@code null}/{@code REJECTED} 合法（初始提交或驳回后重新提交）。
     *
     * <p>非法来源态报告 common 层非法边（携带 {@code action=submit}/{@code fromStatus}）。
     * 接线方 {@code ErpFinEmployeeAdvanceProcessor.validateTransitionForSubmit} 映射为领域码
     * {@code ERR_EMPLOYEE_ADVANCE_ILLEGAL_STATUS_TRANSITION}（common 码作 cause 保留）。
     */
    public void assertCanSubmit(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpFinConstants.APPROVE_STATUS_UNSUBMITTED.equals(status)
                && !ErpFinConstants.APPROVE_STATUS_REJECTED.equals(status)) {
            throw illegal("submit", status, ErpFinConstants.APPROVE_STATUS_UNSUBMITTED + " / " + ErpFinConstants.APPROVE_STATUS_REJECTED);
        }
    }

    /** withdrawApproval 入口守卫：来源态为 {@code SUBMITTED} 合法（审核人未处理时提交人撤回）。 */
    public void assertCanWithdraw(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpFinConstants.APPROVE_STATUS_SUBMITTED.equals(status)) {
            throw illegal("withdraw", status, ErpFinConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    /** approve 入口守卫：来源态为 {@code SUBMITTED} 合法。 */
    public void assertCanApprove(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpFinConstants.APPROVE_STATUS_SUBMITTED.equals(status)) {
            throw illegal("approve", status, ErpFinConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    /** reject 入口守卫：来源态为 {@code SUBMITTED} 合法。 */
    public void assertCanReject(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpFinConstants.APPROVE_STATUS_SUBMITTED.equals(status)) {
            throw illegal("reject", status, ErpFinConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    /** reverseApprove 入口守卫：来源态为 {@code APPROVED} 合法（目标态 REJECTED，已合规 §16.4）。 */
    public void assertCanReverseApprove(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpFinConstants.APPROVE_STATUS_APPROVED.equals(status)) {
            throw illegal("reverseApprove", status, ErpFinConstants.APPROVE_STATUS_APPROVED);
        }
    }

    // ---------- 动作目标态（供 Processor 写回） ----------

    /** submitForApproval 的目标态（SUBMITTED）。 */
    public String submitTargetStatus() {
        return ErpFinConstants.APPROVE_STATUS_SUBMITTED;
    }

    /** withdrawApproval 的目标态（UNSUBMITTED）。 */
    public String withdrawTargetStatus() {
        return ErpFinConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    /** approve 的目标态（APPROVED）。 */
    public String approveTargetStatus() {
        return ErpFinConstants.APPROVE_STATUS_APPROVED;
    }

    /** reject 的目标态（REJECTED）。 */
    public String rejectTargetStatus() {
        return ErpFinConstants.APPROVE_STATUS_REJECTED;
    }

    /** reverseApprove 的目标态（REJECTED，据实保持 Processor 当前行为，已合规 §16.4）。 */
    public String reverseApproveTargetStatus() {
        return ErpFinConstants.APPROVE_STATUS_REJECTED;
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 业务终态判定。审批轴业务终态为 {@code APPROVED}/{@code REJECTED}（均为可逆终态——经
     * {@code reverseApprove}/{@code submitForApproval} 有出边，故 {@link #transitions()} 中二者存在出边，
     * 不适用「终态无出边」强可达性断言，见矩阵测试）。
     */
    public boolean isTerminal(String approveStatus) {
        return ErpFinConstants.APPROVE_STATUS_APPROVED.equals(approveStatus)
                || ErpFinConstants.APPROVE_STATUS_REJECTED.equals(approveStatus);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    /**
     * 迁移矩阵只读快照（5 命名边）：submit（UNSUBMITTED→SUBMITTED + REJECTED→SUBMITTED）/
     * withdraw/approve/reject/reverseApprove。多源动作（submit）的完整合法来源态以显式
     * {@code assertCanSubmit} 方法为准。
     */
    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("submit", ErpFinConstants.APPROVE_STATUS_UNSUBMITTED, ErpFinConstants.APPROVE_STATUS_SUBMITTED),
                new TransitionDefinition("submit", ErpFinConstants.APPROVE_STATUS_REJECTED, ErpFinConstants.APPROVE_STATUS_SUBMITTED),
                new TransitionDefinition("withdraw", ErpFinConstants.APPROVE_STATUS_SUBMITTED, ErpFinConstants.APPROVE_STATUS_UNSUBMITTED),
                new TransitionDefinition("approve", ErpFinConstants.APPROVE_STATUS_SUBMITTED, ErpFinConstants.APPROVE_STATUS_APPROVED),
                new TransitionDefinition("reject", ErpFinConstants.APPROVE_STATUS_SUBMITTED, ErpFinConstants.APPROVE_STATUS_REJECTED),
                new TransitionDefinition("reverseApprove", ErpFinConstants.APPROVE_STATUS_APPROVED, ErpFinConstants.APPROVE_STATUS_REJECTED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpFinConstants.APPROVE_STATUS_APPROVED,
                ErpFinConstants.APPROVE_STATUS_REJECTED));
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED);
    }

    // ---------- 内部 ----------

    /** null 归一化为 UNSUBMITTED（初始态语义：未设置=未提交），与 Processor currentApproveStatus 归一一致。 */
    private static String normalize(String approveStatus) {
        return approveStatus == null ? ErpFinConstants.APPROVE_STATUS_UNSUBMITTED : approveStatus;
    }

    private static NopException illegal(String action, String currentStatus, String expectedStatus) {
        return new NopException(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                .param(ErpCommonErrors.ARG_CURRENT_STATUS, currentStatus)
                .param(ErpCommonErrors.ARG_EXPECTED_STATUS, expectedStatus)
                .param(ARG_ACTION, action);
    }

    /** 只读迁移定义记录（供 M5.1/M5.2 可达性/完备性分析与文档一致性校验消费）。 */
    public static final class TransitionDefinition {
        private final String action;
        private final String fromStatus;
        private final String toStatus;

        TransitionDefinition(String action, String fromStatus, String toStatus) {
            this.action = action;
            this.fromStatus = fromStatus;
            this.toStatus = toStatus;
        }

        public String getAction() {
            return action;
        }

        public String getFromStatus() {
            return fromStatus;
        }

        public String getToStatus() {
            return toStatus;
        }
    }
}
