package app.erp.ast.service.statemachine;

import app.erp.ast.service.ErpAstConstants;
import app.erp.common.service.ErpCommonErrors;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 资产移动单（{@code ErpAstMovement}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code approveStatus} 审批轴）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/assets/state-machine.md §适用对象二：资产移动单}。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载审批轴 5 动作迁移矩阵
 * （submitForApproval/approve/reject/reverseApprove/withdrawApproval）+ 终态/初始态分类 +
 * 只读 {@link #transitions()} 元数据。可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>命名带 {@code Approval} 后缀（契约 §1 双轴约定，与 {@link ErpAstMovementDocumentStateMachine} docStatus 轴分离）。
 *
 * <p><b>本 Bean 是首个 INLINE→Bean 迁移范式</b>：Movement 审批状态逻辑原 100% 内联在
 * {@code ErpAstMovement.xbiz} 的 {@code <source>} 脚本（无 Processor），本 Bean 将固定守卫/目标态集中化后，
 * 由 xbiz source 经 {@code inject} 取得并委托调用（行为/错误码/权限保持不变）。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 xbiz source（契约 §7，
 * 本实体无 Processor 层）。
 *
 * <p>迁移矩阵（6 条边，对应 5 命名动作——submitForApproval 双源）：
 * submitForApproval(UNSUBMITTED→SUBMITTED)、submitForApproval(REJECTED→SUBMITTED)、
 * approve(SUBMITTED→APPROVED)、reject(SUBMITTED→REJECTED)、reverseApprove(APPROVED→REJECTED)、
 * withdrawApproval(SUBMITTED→UNSUBMITTED)。
 *
 * <p>分类：initial={UNSUBMITTED}，terminal={APPROVED}。APPROVED 为「可逆终态」——经 reverseApprove 有出边，
 * 故 {@link #transitions()} 中 APPROVED 存在出边，不适用「终态无出边」的强可达性断言。REJECTED 经
 * submitForApproval 可重新进入 → 非终态（对齐现状 guard，驳回后可重新提交）。
 */
public class ErpAstMovementApprovalStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * submitForApproval 守卫：来源态为 {@code UNSUBMITTED}/{@code null}/{@code REJECTED} 合法（初始提交或驳回后重新提交）。
     *
     * <p>非法来源态报告 common 层非法边（携带 {@code action=submitForApproval}/{@code fromStatus}）。
     * xbiz source 映射为 {@code nop.err.wf.approve.invalid-status}（错误码对外不变）。
     */
    public void assertCanSubmitForApproval(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpAstConstants.APPROVE_STATUS_UNSUBMITTED.equals(status)
                && !ErpAstConstants.APPROVE_STATUS_REJECTED.equals(status)) {
            throw illegal("submitForApproval", status,
                    ErpAstConstants.APPROVE_STATUS_UNSUBMITTED + " 或 " + ErpAstConstants.APPROVE_STATUS_REJECTED);
        }
    }

    /** approve 守卫：来源态为 {@code SUBMITTED} 合法。 */
    public void assertCanApprove(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpAstConstants.APPROVE_STATUS_SUBMITTED.equals(status)) {
            throw illegal("approve", status, ErpAstConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    /** reject 守卫：来源态为 {@code SUBMITTED} 合法（与 approve 同源态）。 */
    public void assertCanReject(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpAstConstants.APPROVE_STATUS_SUBMITTED.equals(status)) {
            throw illegal("reject", status, ErpAstConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    /** reverseApprove 守卫：来源态为 {@code APPROVED} 合法。 */
    public void assertCanReverseApprove(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpAstConstants.APPROVE_STATUS_APPROVED.equals(status)) {
            throw illegal("reverseApprove", status, ErpAstConstants.APPROVE_STATUS_APPROVED);
        }
    }

    /** withdrawApproval 守卫：来源态为 {@code SUBMITTED} 合法（审核人未处理时提交人撤回）。 */
    public void assertCanWithdrawApproval(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpAstConstants.APPROVE_STATUS_SUBMITTED.equals(status)) {
            throw illegal("withdrawApproval", status, ErpAstConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    // ---------- 动作目标态（供 xbiz source 写回） ----------

    public String submitForApprovalTargetStatus() {
        return ErpAstConstants.APPROVE_STATUS_SUBMITTED;
    }

    public String approveTargetStatus() {
        return ErpAstConstants.APPROVE_STATUS_APPROVED;
    }

    public String rejectTargetStatus() {
        return ErpAstConstants.APPROVE_STATUS_REJECTED;
    }

    /** reverseApprove 目标态=REJECTED（对齐 assets 域 R1.x 既有行为 + domain-design-guidelines §16.4）。 */
    public String reverseApproveTargetStatus() {
        return ErpAstConstants.APPROVE_STATUS_REJECTED;
    }

    public String withdrawApprovalTargetStatus() {
        return ErpAstConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 业务终态判定。审批轴业务终态为 {@code APPROVED}；其为「可逆终态」——经 reverseApprove 有出边，
     * 故 {@link #transitions()} 中 APPROVED 存在出边，不适用「终态无出边」的强可达性断言（见矩阵测试）。
     */
    public boolean isTerminal(String approveStatus) {
        return ErpAstConstants.APPROVE_STATUS_APPROVED.equals(approveStatus);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 xbiz source 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("submitForApproval", ErpAstConstants.APPROVE_STATUS_UNSUBMITTED, ErpAstConstants.APPROVE_STATUS_SUBMITTED),
                new TransitionDefinition("submitForApproval", ErpAstConstants.APPROVE_STATUS_REJECTED, ErpAstConstants.APPROVE_STATUS_SUBMITTED),
                new TransitionDefinition("approve", ErpAstConstants.APPROVE_STATUS_SUBMITTED, ErpAstConstants.APPROVE_STATUS_APPROVED),
                new TransitionDefinition("reject", ErpAstConstants.APPROVE_STATUS_SUBMITTED, ErpAstConstants.APPROVE_STATUS_REJECTED),
                new TransitionDefinition("reverseApprove", ErpAstConstants.APPROVE_STATUS_APPROVED, ErpAstConstants.APPROVE_STATUS_REJECTED),
                new TransitionDefinition("withdrawApproval", ErpAstConstants.APPROVE_STATUS_SUBMITTED, ErpAstConstants.APPROVE_STATUS_UNSUBMITTED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(ErpAstConstants.APPROVE_STATUS_APPROVED));
    }

    public List<String> initialStatuses() {
        return Collections.unmodifiableList(Arrays.asList(ErpAstConstants.APPROVE_STATUS_UNSUBMITTED));
    }

    // ---------- 内部 ----------

    /** null 归一化为 UNSUBMITTED（初始态语义：未设置=未提交），与既有 xbiz source guard（status !== null）一致。 */
    private static String normalize(String approveStatus) {
        return approveStatus == null ? ErpAstConstants.APPROVE_STATUS_UNSUBMITTED : approveStatus;
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
