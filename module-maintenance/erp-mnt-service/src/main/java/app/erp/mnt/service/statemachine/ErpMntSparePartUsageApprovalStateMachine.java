package app.erp.mnt.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.mnt.dao.ErpMntDaoConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 备件消耗单（{@code ErpMntSparePartUsage}）审批轴状态机 Bean —— 一 Bean 对应一实体一轴（{@code approveStatus}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}（§1 双轴约定——同一实体的多个状态轴各落独立 Bean，
 * 后缀区分轴名）；业务语义：{@code docs/design/maintenance/state-machine.md §实现约定}（confirm 一步到位 →APPROVED）。
 *
 * <p>严格无状态（契约 §2）。承载最小迁移矩阵（UNSUBMITTED/SUBMITTED/APPROVED/REJECTED 四态字典，但仅
 * confirmApprove 单边可达）+ 终态/初始态分类 + 只读 {@link #transitions()}。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor/BizModel（契约 §7）。
 *
 * <p>迁移矩阵（1 条边）：confirmApprove(null/UNSUBMITTED→APPROVED)。
 *
 * <p>注：备件消耗单无独立 submit/approve/reject 审批 Processor——confirm 动作同时推进 docStatus→ACTIVE + approveStatus→APPROVED
 * （一步到位，非标准 5 动作审批生命周期，Decision (A)，plan 2026-08-14-0930-3 Phase 2）。{@code null} 视同 UNSUBMITTED
 * （新建实体 approveStatus 未设置），{@code transitions()} 仅收录 UNSUBMITTED→APPROVED 规范边。
 */
public class ErpMntSparePartUsageApprovalStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * confirmApprove 守卫：null 或 UNSUBMITTED 合法（新建/草稿 → 直接审批通过）。
     */
    public void assertCanConfirmApprove(String approveStatus) {
        if (approveStatus != null && !ErpMntDaoConstants.APPROVE_STATUS_UNSUBMITTED.equals(approveStatus)) {
            throw illegal("confirmApprove", approveStatus, "null 或 UNSUBMITTED");
        }
    }

    public String confirmApproveTargetStatus() {
        return ErpMntDaoConstants.APPROVE_STATUS_APPROVED;
    }

    // ---------- 终态/初始态分类 ----------

    public boolean isTerminal(String approveStatus) {
        return ErpMntDaoConstants.APPROVE_STATUS_APPROVED.equals(approveStatus)
                || ErpMntDaoConstants.APPROVE_STATUS_REJECTED.equals(approveStatus);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.singletonList(
                new TransitionDefinition("confirmApprove",
                        ErpMntDaoConstants.APPROVE_STATUS_UNSUBMITTED, ErpMntDaoConstants.APPROVE_STATUS_APPROVED));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpMntDaoConstants.APPROVE_STATUS_APPROVED,
                ErpMntDaoConstants.APPROVE_STATUS_REJECTED));
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpMntDaoConstants.APPROVE_STATUS_UNSUBMITTED);
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
