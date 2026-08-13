package app.erp.sal.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.sal.dao.constants.ErpSalDocStatus;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 销售出库单（{@code ErpSalDelivery}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code approveStatus} 审批轴）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/sales/state-machine.md}（§三轴状态分离 + §审批轴 + §实现模式与守卫边界）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载审批轴 5 动作迁移矩阵
 * （submit/approve/reject/reverseApprove/withdraw）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>命名带 {@code Approval} 后缀（契约 §1 双轴约定，与 {@code ErpSalDeliveryDocumentStateMachine} docStatus 轴分离）。
 *
 * <p><b>M4 业财过账副作用边界（§11.2 M4）</b>：本 Bean 仅集中固定迁移矩阵。approve 触发的出库移动
 * （{@code triggerOutgoingMove}→{@code IErpInvStockMoveBiz}）+ SALES_OUTPUT 凭证过账（库存域 {@code InvAcctDocProvider}）
 * 保留在 {@code ErpSalDeliveryApproveProcessor} 原位（副作用不入轴，契约 §11.2 M4 (ii)/(iv)）。
 * {@code SalReversalListener} 跨域红冲回写（SALES_OUTPUT 仅 posted）保留原位不改（§11.2 M4 (v)）。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor（契约 §7）。
 * Delivery 领域码为泛型 {@code ERR_ILLEGAL_STATUS_TRANSITION}（无 DELIVERY_ 前缀，沿用既有）。
 *
 * <p><b>reverseApprove 目标态</b>：实仓核实 {@code ErpSalDeliveryReverseApproveProcessor.reverseApprove} 已设
 * REJECTED（已合规 {@code domain-design-guidelines.md §16.4}）。故本 Bean {@code reverseApproveTargetStatus()}=REJECTED。
 * 共享骨架 {@code AbstractReverseApproveProcessor.doReverseApprove}→SUBMITTED 为已确认 live 缺陷
 * （对 Delivery 为经覆写绕过的死路径），其 §16.4 合规化移交显式 successor。
 */
public class ErpSalDeliveryApprovalStateMachine {

    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * submit 守卫：来源态为 {@code UNSUBMITTED}/{@code null}/{@code REJECTED} 合法（初始提交或驳回后重新提交）。
     *
     * <p>非法来源态报告 common 层非法边（携带 {@code action=submit}/{@code fromStatus}）。
     * 接线方 {@code ErpSalDeliverySubmitForApprovalProcessor} 映射为领域码 {@code ERR_ILLEGAL_STATUS_TRANSITION}。
     */
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

    // ---------- 动作目标态（供 Processor 写回） ----------

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

    // ---------- 终态/初始态分类 ----------

    public boolean isTerminal(String approveStatus) {
        return ErpSalDocStatus.APPROVE_STATUS_APPROVED.equals(approveStatus);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

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

    // ---------- 内部 ----------

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
