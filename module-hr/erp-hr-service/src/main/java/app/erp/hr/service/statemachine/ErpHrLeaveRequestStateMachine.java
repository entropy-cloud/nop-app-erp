package app.erp.hr.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.hr.service.ErpHrConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 休假申请（{@code ErpHrLeaveRequest}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code status}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/human-resource/state-machine.md §适用对象一}。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载五态迁移矩阵
 * （DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor/BizModel（契约 §7）。
 *
 * <p>迁移矩阵（4 条边，编码<strong>已实现</strong>行为）：submit(DRAFT→SUBMITTED)、
 * approve(SUBMITTED→APPROVED)、reject(SUBMITTED→REJECTED)、cancel(APPROVED→CANCELLED <strong>单源</strong>）。
 *
 * <p><strong>cancel 单源对齐生产代码</strong>：{@code ErpHrLeaveRequestCancelProcessor:21} 守卫
 * {@code requireStatus(leave, APPROVED, CANCELLED)} —— cancel 仅允许 APPROVED 源（已批准休假由 HR/员工取消）。
 * owner doc {@code state-machine.md §2/§6} 声明 DRAFT/SUBMITTED→CANCELLED（员工未审批前自撤），生产代码无此 writer；
 * 该 doc drift 由本计划 Phase 3 layer-2 四方对照登记 + owner doc 就地补正，Bean 矩阵如实编码已实现行为。
 */
public class ErpHrLeaveRequestStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    public void assertCanSubmit(String status) {
        if (!ErpHrConstants.LEAVE_STATUS_DRAFT.equals(status)) {
            throw illegal("submit", status, ErpHrConstants.LEAVE_STATUS_DRAFT);
        }
    }

    public String submitTargetStatus() {
        return ErpHrConstants.LEAVE_STATUS_SUBMITTED;
    }

    public void assertCanApprove(String status) {
        if (!ErpHrConstants.LEAVE_STATUS_SUBMITTED.equals(status)) {
            throw illegal("approve", status, ErpHrConstants.LEAVE_STATUS_SUBMITTED);
        }
    }

    public String approveTargetStatus() {
        return ErpHrConstants.LEAVE_STATUS_APPROVED;
    }

    public void assertCanReject(String status) {
        if (!ErpHrConstants.LEAVE_STATUS_SUBMITTED.equals(status)) {
            throw illegal("reject", status, ErpHrConstants.LEAVE_STATUS_SUBMITTED);
        }
    }

    public String rejectTargetStatus() {
        return ErpHrConstants.LEAVE_STATUS_REJECTED;
    }

    /**
     * cancel 守卫：<strong>单源</strong> APPROVED→CANCELLED（对齐 {@code ErpHrLeaveRequestCancelProcessor:21}）。
     *
     * <p>APPROVED 本身是终态（isTerminal 返回 true），但它是 cancel 的合法源——cancel 是「已批准休假取消」，
     * 由 HR/员工触发并返还假期余额。其余态（DRAFT/SUBMITTED/REJECTED/CANCELLED）均非法。
     */
    public void assertCanCancel(String status) {
        if (!ErpHrConstants.LEAVE_STATUS_APPROVED.equals(status)) {
            throw illegal("cancel", status, ErpHrConstants.LEAVE_STATUS_APPROVED);
        }
    }

    public String cancelTargetStatus() {
        return ErpHrConstants.LEAVE_STATUS_CANCELLED;
    }

    // ---------- 终态/初始态分类 ----------

    public boolean isTerminal(String status) {
        return ErpHrConstants.LEAVE_STATUS_APPROVED.equals(status)
                || ErpHrConstants.LEAVE_STATUS_REJECTED.equals(status)
                || ErpHrConstants.LEAVE_STATUS_CANCELLED.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("submit", ErpHrConstants.LEAVE_STATUS_DRAFT, ErpHrConstants.LEAVE_STATUS_SUBMITTED),
                new TransitionDefinition("approve", ErpHrConstants.LEAVE_STATUS_SUBMITTED, ErpHrConstants.LEAVE_STATUS_APPROVED),
                new TransitionDefinition("reject", ErpHrConstants.LEAVE_STATUS_SUBMITTED, ErpHrConstants.LEAVE_STATUS_REJECTED),
                new TransitionDefinition("cancel", ErpHrConstants.LEAVE_STATUS_APPROVED, ErpHrConstants.LEAVE_STATUS_CANCELLED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpHrConstants.LEAVE_STATUS_APPROVED,
                ErpHrConstants.LEAVE_STATUS_REJECTED,
                ErpHrConstants.LEAVE_STATUS_CANCELLED));
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpHrConstants.LEAVE_STATUS_DRAFT);
    }

    // ---------- 内部 ----------

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
