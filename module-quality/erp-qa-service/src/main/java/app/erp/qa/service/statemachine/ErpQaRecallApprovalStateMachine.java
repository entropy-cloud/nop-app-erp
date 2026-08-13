package app.erp.qa.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.qa.service.ErpQaConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 召回事件（{@code ErpQaRecall}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code approveStatus} 审批轴）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/quality/recall.md}（§召回状态机 + §审批轴）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载 approveStatus 轴 5 动作迁移矩阵
 * （submit/approve/reject/reverseApprove/withdraw）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>命名带 {@code Approval} 后缀（契约 §1 双轴约定，与 {@code ErpQaRecallStateMachine} status 操作轴分离）。
 * 审批轴经 dedicated orchestrator-facade {@code ErpQaRecallProcessor.validateTransitionForXxx} 接线（M4 采购 facade 路径同构）。
 *
 * <p><b>reverseApprove 目标态裁定</b>（plan Phase 3 Decision (E)）：实仓 {@code ErpQaRecallProcessor.doReverseApprove}
 * 已覆写=REJECTED（非骨架 SUBMITTED），合规 {@code domain-design-guidelines.md §16.4}。故本 Bean
 * {@code reverseApproveTargetStatus()}=REJECTED，零行为回归。
 *
 * <p><b>approve/reject 联动</b>（Decision (B)）：Bean 按单轴建模；approve/reject 联动写 status=APPROVED/CANCELLED
 * 保留在 facade {@code doApprove/doReject} 原位（status 轴归 {@link ErpQaRecallStateMachine}）。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 facade（契约 §7）。
 */
public class ErpQaRecallApprovalStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /** submit 守卫：来源态为 {@code UNSUBMITTED}/{@code null}/{@code REJECTED} 合法。 */
    public void assertCanSubmit(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpQaConstants.APPROVE_STATUS_UNSUBMITTED.equals(status)
                && !ErpQaConstants.APPROVE_STATUS_REJECTED.equals(status)) {
            throw illegal("submit", status, ErpQaConstants.APPROVE_STATUS_UNSUBMITTED + " / " + ErpQaConstants.APPROVE_STATUS_REJECTED);
        }
    }

    /** approve 守卫：来源态为 {@code SUBMITTED} 合法。 */
    public void assertCanApprove(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpQaConstants.APPROVE_STATUS_SUBMITTED.equals(status)) {
            throw illegal("approve", status, ErpQaConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    /** reject 守卫：来源态为 {@code SUBMITTED} 合法。 */
    public void assertCanReject(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpQaConstants.APPROVE_STATUS_SUBMITTED.equals(status)) {
            throw illegal("reject", status, ErpQaConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    /** reverseApprove 守卫：来源态为 {@code APPROVED} 合法。 */
    public void assertCanReverseApprove(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpQaConstants.APPROVE_STATUS_APPROVED.equals(status)) {
            throw illegal("reverseApprove", status, ErpQaConstants.APPROVE_STATUS_APPROVED);
        }
    }

    /** withdraw 守卫：来源态为 {@code SUBMITTED} 合法。 */
    public void assertCanWithdraw(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpQaConstants.APPROVE_STATUS_SUBMITTED.equals(status)) {
            throw illegal("withdraw", status, ErpQaConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    // ---------- 动作目标态（供 facade 写回） ----------

    public String submitTargetStatus() {
        return ErpQaConstants.APPROVE_STATUS_SUBMITTED;
    }

    public String approveTargetStatus() {
        return ErpQaConstants.APPROVE_STATUS_APPROVED;
    }

    public String rejectTargetStatus() {
        return ErpQaConstants.APPROVE_STATUS_REJECTED;
    }

    /** reverseApprove 目标态=REJECTED（据实保持 Recall 当前行为，已合规 §16.4）。 */
    public String reverseApproveTargetStatus() {
        return ErpQaConstants.APPROVE_STATUS_REJECTED;
    }

    public String withdrawTargetStatus() {
        return ErpQaConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    // ---------- 终态/初始态分类 ----------

    /** 业务终态判定。审批轴业务终态为 {@code APPROVED}（可逆终态——经 reverseApprove 有出边）。 */
    public boolean isTerminal(String approveStatus) {
        return ErpQaConstants.APPROVE_STATUS_APPROVED.equals(approveStatus);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 facade 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("submit", ErpQaConstants.APPROVE_STATUS_UNSUBMITTED, ErpQaConstants.APPROVE_STATUS_SUBMITTED),
                new TransitionDefinition("submit", ErpQaConstants.APPROVE_STATUS_REJECTED, ErpQaConstants.APPROVE_STATUS_SUBMITTED),
                new TransitionDefinition("approve", ErpQaConstants.APPROVE_STATUS_SUBMITTED, ErpQaConstants.APPROVE_STATUS_APPROVED),
                new TransitionDefinition("reject", ErpQaConstants.APPROVE_STATUS_SUBMITTED, ErpQaConstants.APPROVE_STATUS_REJECTED),
                new TransitionDefinition("reverseApprove", ErpQaConstants.APPROVE_STATUS_APPROVED, ErpQaConstants.APPROVE_STATUS_REJECTED),
                new TransitionDefinition("withdraw", ErpQaConstants.APPROVE_STATUS_SUBMITTED, ErpQaConstants.APPROVE_STATUS_UNSUBMITTED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(ErpQaConstants.APPROVE_STATUS_APPROVED));
    }

    public List<String> initialStatuses() {
        return Collections.unmodifiableList(Arrays.asList(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED));
    }

    // ---------- 内部 ----------

    /** null 归一化为 UNSUBMITTED（初始态语义：未设审核状态=未提交）。 */
    private static String normalize(String approveStatus) {
        return approveStatus == null ? ErpQaConstants.APPROVE_STATUS_UNSUBMITTED : approveStatus;
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
