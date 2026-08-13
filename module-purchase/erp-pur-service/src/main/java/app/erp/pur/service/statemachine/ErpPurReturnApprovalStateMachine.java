package app.erp.pur.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.pur.dao.constants.ErpPurDocStatus;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 采购退货单（{@code ErpPurReturn}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code approveStatus} 审批轴）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/purchase/state-machine.md}（§三轴状态分离 + §审批轴 + §实现模式与守卫边界）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载审批轴 5 动作迁移矩阵
 * （submit/approve/reject/reverseApprove/withdraw）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>命名带 {@code Approval} 后缀（契约 §1 双轴约定，与 {@code ErpPurReturnDocumentStateMachine} docStatus 轴分离）。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor（契约 §7）。
 * Return 领域码为 {@code ERR_RETURN_ILLEGAL_STATUS_TRANSITION}。
 *
 * <p><b>接线路径（plan Phase 1 Decision）</b>：Return 全部 5 动作经 per-mutation Processor 覆写
 * {@code validateTransitionForXxx} 委托 Bean（skeleton 路径，与 Receive 同）。出库 stock move +
 * PurReturnPostingDispatcher 过账编排 + commitment-restore 保留原位。
 *
 * <p><b>reverseApprove 目标态裁定（plan Phase 1 Decision）</b>：实仓核实 {@code ErpPurReturnReverseApproveProcessor.reverseApprove}
 * 整体覆写写入 REJECTED（非骨架默认 SUBMITTED）——即 Return 当前行为已合规 §16.4。故本 Bean {@code reverseApproveTargetStatus()}=REJECTED。
 */
public class ErpPurReturnApprovalStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

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

    // ---------- 动作目标态（供 Processor 写回） ----------

    public String submitTargetStatus() {
        return ErpPurDocStatus.APPROVE_STATUS_SUBMITTED;
    }

    public String approveTargetStatus() {
        return ErpPurDocStatus.APPROVE_STATUS_APPROVED;
    }

    public String rejectTargetStatus() {
        return ErpPurDocStatus.APPROVE_STATUS_REJECTED;
    }

    /** reverseApprove 目标态=REJECTED（据实保持 Return 当前行为，已合规 §16.4）。 */
    public String reverseApproveTargetStatus() {
        return ErpPurDocStatus.APPROVE_STATUS_REJECTED;
    }

    public String withdrawTargetStatus() {
        return ErpPurDocStatus.APPROVE_STATUS_UNSUBMITTED;
    }

    // ---------- 终态/初始态分类 ----------

    public boolean isTerminal(String approveStatus) {
        return ErpPurDocStatus.APPROVE_STATUS_APPROVED.equals(approveStatus);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

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

    // ---------- 内部 ----------

    private static String normalize(String approveStatus) {
        return approveStatus == null ? ErpPurDocStatus.APPROVE_STATUS_UNSUBMITTED : approveStatus;
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
