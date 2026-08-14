package app.erp.fin.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 坏账单（{@code ErpFinBadDebt}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code approveStatus}
 * 坏账审批轴，平台共享字典 {@code wf/approve-status} 4 值：UNSUBMITTED/SUBMITTED/APPROVED/REJECTED）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/finance/state-machine.md} §对象八 + {@code docs/design/finance/bad-debt.md}。
 *
 * <p><b>治理裁定（§11.2 M4 plan-first，plan 2026-08-14-0456-1）</b>：坏账 {@code approve} 触发
 * BAD_DEBT_WRITE_OFF/RECOVERY 凭证 + ArApItem 变异（config-gated {@code erp-fin.bad-debt-write-off-require-approval}），
 * {@code reverseApprove} 红冲凭证 + ArApItem 对称回滚。依契约 §11.2 M4 硬约束 (i)–(v)：过账时序/编排/失败回退继续由
 * {@code ErpFinBadDebtProcessor} + {@code FinPostingExecutor} 管理（§11.2 M4 (ii)/(v)），Bean 不触碰；跨域副作用
 * （BAD_DEBT_WRITE_OFF/RECOVERY 凭证、ArApItem 变异、红冲）保留原 Processor 路径（§11.2 M4 (iv)）；{@code posted}
 * 不入轴（§11.2 M4 (iii)，本实体无独立 {@code posted} boolean 字段，以 {@code voucherId} 非空作为已过账标志）；
 * SoD（approver-is-creator）为动态业务守卫保留原位（非 Bean 范畴，架构契约 {@code entity-state-machine-bean.md:274}）。
 *
 * <p>命名带 {@code Approval} 后缀（契约 §1 双轴约定）。**注意**：ORM 列名为 {@code approvalStatus}（单 p，
 * {@code app-erp-finance.orm.xml:1688}，dict {@code wf/approve-status}），Bean 命名用 {@code Approval} 后缀对齐
 * dict 语义（§1 约定），ORM 列名不改。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。
 *
 * <p><b>4 命名动作迁移矩阵</b>（镜像 {@code ErpFinBadDebtProcessor} facade 固定守卫语义，层 2 四方对照以代码为权威）：
 * <ul>
 *   <li>{@code submit} {UNSUBMITTED}→SUBMITTED（无 REJECTED 重提——facade {@code validateTransitionForSubmit}
 *       require UNSUBMITTED only，REJECTED 为不可逆终态）；</li>
 *   <li>{@code approve} {UNSUBMITTED,SUBMITTED}→APPROVED（facade 允许直接审批未提交单）；</li>
 *   <li>{@code reject} {UNSUBMITTED,SUBMITTED}→REJECTED（facade 允许直接驳回未提交单）；</li>
 *   <li>{@code reverseApprove} {APPROVED}→REJECTED（红冲侧，已合规 {@code domain-design-guidelines.md §16.4}）。</li>
 * </ul>
 *
 * <p><b>终态</b>：{@code APPROVED}/{@code REJECTED}。APPROVED 为<b>可逆终态</b>（经 reverseApprove 有出边）；
 * REJECTED 为<b>不可逆终态</b>（无出边——无重提路径）。initial：{@code UNSUBMITTED}（{@code null} 归一化）。
 *
 * <p><b>auto-approve 旁路不经 Bean</b>（契约 §9.2 选项 c 初始/生成写入路径，不调 {@code assertCan*}）：
 * {@code ErpFinBadDebtWriteOffProcessor}/{@code ErpFinBadDebtRecoverProcessor} 在 config
 * {@code erp-fin.bad-debt-write-off-require-approval=false} 时直接写 APPROVED（与 Voucher 生成路径先例一致）。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 facade（契约 §7，
 * {@code ERR_BAD_DEBT_ILLEGAL_APPROVAL_TRANSITION}，common 码作 cause 保留）。
 */
public class ErpFinBadDebtApprovalStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * submit 入口守卫：来源态为 {@code UNSUBMITTED} 合法（唯一来源态；facade require UNSUBMITTED only，
     * REJECTED 无重提路径）。
     */
    public void assertCanSubmit(String approvalStatus) {
        String status = normalize(approvalStatus);
        if (!ErpFinConstants.APPROVE_STATUS_UNSUBMITTED.equals(status)) {
            throw illegal("submit", status, ErpFinConstants.APPROVE_STATUS_UNSUBMITTED);
        }
    }

    /** approve 入口守卫：来源态为 {@code UNSUBMITTED} 或 {@code SUBMITTED} 合法（facade 允许直接审批未提交单）。 */
    public void assertCanApprove(String approvalStatus) {
        String status = normalize(approvalStatus);
        if (!ErpFinConstants.APPROVE_STATUS_SUBMITTED.equals(status)
                && !ErpFinConstants.APPROVE_STATUS_UNSUBMITTED.equals(status)) {
            throw illegal("approve", status, ErpFinConstants.APPROVE_STATUS_UNSUBMITTED + " / " + ErpFinConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    /** reject 入口守卫：来源态为 {@code UNSUBMITTED} 或 {@code SUBMITTED} 合法（facade 允许直接驳回未提交单）。 */
    public void assertCanReject(String approvalStatus) {
        String status = normalize(approvalStatus);
        if (!ErpFinConstants.APPROVE_STATUS_SUBMITTED.equals(status)
                && !ErpFinConstants.APPROVE_STATUS_UNSUBMITTED.equals(status)) {
            throw illegal("reject", status, ErpFinConstants.APPROVE_STATUS_UNSUBMITTED + " / " + ErpFinConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    /** reverseApprove 入口守卫：来源态为 {@code APPROVED} 合法（红冲侧唯一来源态）。 */
    public void assertCanReverseApprove(String approvalStatus) {
        String status = normalize(approvalStatus);
        if (!ErpFinConstants.APPROVE_STATUS_APPROVED.equals(status)) {
            throw illegal("reverseApprove", status, ErpFinConstants.APPROVE_STATUS_APPROVED);
        }
    }

    // ---------- 动作目标态（供 facade 写回） ----------

    public String submitTargetStatus() {
        return ErpFinConstants.APPROVE_STATUS_SUBMITTED;
    }

    public String approveTargetStatus() {
        return ErpFinConstants.APPROVE_STATUS_APPROVED;
    }

    public String rejectTargetStatus() {
        return ErpFinConstants.APPROVE_STATUS_REJECTED;
    }

    public String reverseApproveTargetStatus() {
        return ErpFinConstants.APPROVE_STATUS_REJECTED;
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 业务终态判定：{@code APPROVED}/{@code REJECTED}（均为业务终态；APPROVED 为可逆终态——经 reverseApprove
     * 有出边，不适用「终态无出边」强断言；REJECTED 为不可逆终态——无重提路径）。
     */
    public boolean isTerminal(String approvalStatus) {
        return ErpFinConstants.APPROVE_STATUS_APPROVED.equals(approvalStatus)
                || ErpFinConstants.APPROVE_STATUS_REJECTED.equals(approvalStatus);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 facade 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("submit", ErpFinConstants.APPROVE_STATUS_UNSUBMITTED, ErpFinConstants.APPROVE_STATUS_SUBMITTED),
                new TransitionDefinition("approve", ErpFinConstants.APPROVE_STATUS_UNSUBMITTED, ErpFinConstants.APPROVE_STATUS_APPROVED),
                new TransitionDefinition("approve", ErpFinConstants.APPROVE_STATUS_SUBMITTED, ErpFinConstants.APPROVE_STATUS_APPROVED),
                new TransitionDefinition("reject", ErpFinConstants.APPROVE_STATUS_UNSUBMITTED, ErpFinConstants.APPROVE_STATUS_REJECTED),
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

    /** null 归一化为 UNSUBMITTED（初始态语义：未设置=未提交），与 facade {@code currentApprovalStatus} 归一一致。 */
    private static String normalize(String approvalStatus) {
        return approvalStatus == null ? ErpFinConstants.APPROVE_STATUS_UNSUBMITTED : approvalStatus;
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
