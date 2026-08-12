package app.erp.pur.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.pur.dao.constants.ErpPurDocStatus;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 采购请购单（{@code ErpPurRequisition}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code approveStatus} 审批轴）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/purchase/state-machine.md}（§三轴状态分离 + §审批轴）。
 *
 * <p>严格无状态（契约 §2）。承载审批轴 5 动作迁移矩阵 + 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 命名带 {@code Approval} 后缀（契约 §1 双轴约定，与 {@code ErpPurRequisitionDocumentStateMachine} docStatus 轴分离）。
 *
 * <p>矩阵与 {@code ErpPurOrderApprovalStateMachine} 同构（共享 dict {@code wf/approve-status} + 同 5 动作）。
 * reverseApprove 目标态=REJECTED（据实保持 Requisition 当前行为，已合规 §16.4；骨架 SUBMITTED 为经覆写绕过的死路径）。
 */
public class ErpPurRequisitionApprovalStateMachine {

    public static final String ARG_ACTION = "action";

    public void assertCanSubmit(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpPurDocStatus.APPROVE_STATUS_UNSUBMITTED.equals(status)
                && !ErpPurDocStatus.APPROVE_STATUS_REJECTED.equals(status)) {
            throw illegal("submit", status, ErpPurDocStatus.APPROVE_STATUS_UNSUBMITTED + " / " + ErpPurDocStatus.APPROVE_STATUS_REJECTED);
        }
    }

    public void assertCanApprove(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpPurDocStatus.APPROVE_STATUS_SUBMITTED.equals(status)) {
            throw illegal("approve", status, ErpPurDocStatus.APPROVE_STATUS_SUBMITTED);
        }
    }

    public void assertCanReject(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpPurDocStatus.APPROVE_STATUS_SUBMITTED.equals(status)) {
            throw illegal("reject", status, ErpPurDocStatus.APPROVE_STATUS_SUBMITTED);
        }
    }

    public void assertCanReverseApprove(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpPurDocStatus.APPROVE_STATUS_APPROVED.equals(status)) {
            throw illegal("reverseApprove", status, ErpPurDocStatus.APPROVE_STATUS_APPROVED);
        }
    }

    public void assertCanWithdraw(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpPurDocStatus.APPROVE_STATUS_SUBMITTED.equals(status)) {
            throw illegal("withdraw", status, ErpPurDocStatus.APPROVE_STATUS_SUBMITTED);
        }
    }

    public String submitTargetStatus() {
        return ErpPurDocStatus.APPROVE_STATUS_SUBMITTED;
    }

    public String approveTargetStatus() {
        return ErpPurDocStatus.APPROVE_STATUS_APPROVED;
    }

    public String rejectTargetStatus() {
        return ErpPurDocStatus.APPROVE_STATUS_REJECTED;
    }

    public String reverseApproveTargetStatus() {
        return ErpPurDocStatus.APPROVE_STATUS_REJECTED;
    }

    public String withdrawTargetStatus() {
        return ErpPurDocStatus.APPROVE_STATUS_UNSUBMITTED;
    }

    public boolean isTerminal(String approveStatus) {
        return ErpPurDocStatus.APPROVE_STATUS_APPROVED.equals(approveStatus);
    }

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("submit", ErpPurDocStatus.APPROVE_STATUS_UNSUBMITTED, ErpPurDocStatus.APPROVE_STATUS_SUBMITTED),
                new TransitionDefinition("submit", ErpPurDocStatus.APPROVE_STATUS_REJECTED, ErpPurDocStatus.APPROVE_STATUS_SUBMITTED),
                new TransitionDefinition("approve", ErpPurDocStatus.APPROVE_STATUS_SUBMITTED, ErpPurDocStatus.APPROVE_STATUS_APPROVED),
                new TransitionDefinition("reject", ErpPurDocStatus.APPROVE_STATUS_SUBMITTED, ErpPurDocStatus.APPROVE_STATUS_REJECTED),
                new TransitionDefinition("reverseApprove", ErpPurDocStatus.APPROVE_STATUS_APPROVED, ErpPurDocStatus.APPROVE_STATUS_REJECTED),
                new TransitionDefinition("withdraw", ErpPurDocStatus.APPROVE_STATUS_SUBMITTED, ErpPurDocStatus.APPROVE_STATUS_UNSUBMITTED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(ErpPurDocStatus.APPROVE_STATUS_APPROVED));
    }

    public List<String> initialStatuses() {
        return Collections.unmodifiableList(Arrays.asList(ErpPurDocStatus.APPROVE_STATUS_UNSUBMITTED));
    }

    private static String normalize(String approveStatus) {
        return approveStatus == null ? ErpPurDocStatus.APPROVE_STATUS_UNSUBMITTED : approveStatus;
    }

    private static NopException illegal(String action, String currentStatus, String expectedStatus) {
        return new NopException(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                .param(ErpCommonErrors.ARG_CURRENT_STATUS, currentStatus)
                .param(ErpCommonErrors.ARG_EXPECTED_STATUS, expectedStatus)
                .param(ARG_ACTION, action);
    }

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
