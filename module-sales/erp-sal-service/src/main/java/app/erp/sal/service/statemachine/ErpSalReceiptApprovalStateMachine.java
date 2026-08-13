package app.erp.sal.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.sal.dao.constants.ErpSalDocStatus;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 销售收款单（{@code ErpSalReceipt}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code approveStatus} 审批轴）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/sales/state-machine.md}（§三轴状态分离 + §审批轴 + §实现模式与守卫边界）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载审批轴 5 动作迁移矩阵
 * （submit/approve/reject/reverseApprove/withdraw）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>命名带 {@code Approval} 后缀（契约 §1 双轴约定，与 {@code ErpSalReceiptDocumentStateMachine} docStatus 轴分离）。
 *
 * <p><b>M4 业财过账副作用边界（§11.2 M4）</b>：本 Bean 仅集中固定迁移矩阵。approve 触发的收款过账
 * （{@code SalReceiptPostingDispatcher}→RECEIPT 凭证）+ 核销 + workflow 保留在 {@code ErpSalReceiptApproveProcessor}
 * 原位（副作用不入轴，契约 §11.2 M4 (ii)/(iv)）。{@code SalReversalListener} 跨域红冲回写（RECEIPT→posted=false +
 * APPROVED→REJECTED）保留原位不改（§11.2 M4 (v)）。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor（契约 §7）。
 *
 * <p><b>reverseApprove 目标态</b>：实仓核实 {@code ErpSalReceiptReverseApproveProcessor.reverseApprove} 已设
 * REJECTED（已合规 {@code domain-design-guidelines.md §16.4}）。故本 Bean {@code reverseApproveTargetStatus()}=REJECTED。
 */
public class ErpSalReceiptApprovalStateMachine {

    public static final String ARG_ACTION = "action";

    public void assertCanSubmit(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpSalDocStatus.APPROVE_STATUS_UNSUBMITTED.equals(status)
                && !ErpSalDocStatus.APPROVE_STATUS_REJECTED.equals(status)) {
            throw illegal("submit", status, ErpSalDocStatus.APPROVE_STATUS_UNSUBMITTED + " / " + ErpSalDocStatus.APPROVE_STATUS_REJECTED);
        }
    }

    public void assertCanApprove(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpSalDocStatus.APPROVE_STATUS_SUBMITTED.equals(status)) {
            throw illegal("approve", status, ErpSalDocStatus.APPROVE_STATUS_SUBMITTED);
        }
    }

    public void assertCanReject(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpSalDocStatus.APPROVE_STATUS_SUBMITTED.equals(status)) {
            throw illegal("reject", status, ErpSalDocStatus.APPROVE_STATUS_SUBMITTED);
        }
    }

    public void assertCanReverseApprove(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpSalDocStatus.APPROVE_STATUS_APPROVED.equals(status)) {
            throw illegal("reverseApprove", status, ErpSalDocStatus.APPROVE_STATUS_APPROVED);
        }
    }

    public void assertCanWithdraw(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpSalDocStatus.APPROVE_STATUS_SUBMITTED.equals(status)) {
            throw illegal("withdraw", status, ErpSalDocStatus.APPROVE_STATUS_SUBMITTED);
        }
    }

    public String submitTargetStatus() {
        return ErpSalDocStatus.APPROVE_STATUS_SUBMITTED;
    }

    public String approveTargetStatus() {
        return ErpSalDocStatus.APPROVE_STATUS_APPROVED;
    }

    public String rejectTargetStatus() {
        return ErpSalDocStatus.APPROVE_STATUS_REJECTED;
    }

    public String reverseApproveTargetStatus() {
        return ErpSalDocStatus.APPROVE_STATUS_REJECTED;
    }

    public String withdrawTargetStatus() {
        return ErpSalDocStatus.APPROVE_STATUS_UNSUBMITTED;
    }

    public boolean isTerminal(String approveStatus) {
        return ErpSalDocStatus.APPROVE_STATUS_APPROVED.equals(approveStatus);
    }

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("submit", ErpSalDocStatus.APPROVE_STATUS_UNSUBMITTED, ErpSalDocStatus.APPROVE_STATUS_SUBMITTED),
                new TransitionDefinition("submit", ErpSalDocStatus.APPROVE_STATUS_REJECTED, ErpSalDocStatus.APPROVE_STATUS_SUBMITTED),
                new TransitionDefinition("approve", ErpSalDocStatus.APPROVE_STATUS_SUBMITTED, ErpSalDocStatus.APPROVE_STATUS_APPROVED),
                new TransitionDefinition("reject", ErpSalDocStatus.APPROVE_STATUS_SUBMITTED, ErpSalDocStatus.APPROVE_STATUS_REJECTED),
                new TransitionDefinition("reverseApprove", ErpSalDocStatus.APPROVE_STATUS_APPROVED, ErpSalDocStatus.APPROVE_STATUS_REJECTED),
                new TransitionDefinition("withdraw", ErpSalDocStatus.APPROVE_STATUS_SUBMITTED, ErpSalDocStatus.APPROVE_STATUS_UNSUBMITTED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(ErpSalDocStatus.APPROVE_STATUS_APPROVED));
    }

    public List<String> initialStatuses() {
        return Collections.unmodifiableList(Arrays.asList(ErpSalDocStatus.APPROVE_STATUS_UNSUBMITTED));
    }

    private static String normalize(String approveStatus) {
        return approveStatus == null ? ErpSalDocStatus.APPROVE_STATUS_UNSUBMITTED : approveStatus;
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
