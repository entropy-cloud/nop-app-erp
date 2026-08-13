package app.erp.qa.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.qa.service.ErpQaConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 质检单（{@code ErpQaInspection}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code approveStatus} 让步审批轴）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/quality/state-machine.md}（§适用对象一 §让步接收审批流简化 + §实现约定「让步接收审批流简化」）。
 *
 * <p><b>M4.59 迁移范围裁定</b>（plan Phase 1 Decision）：质检单 {@code approveStatus} 仅有让步审批单边 writer
 * （{@code RecordResultProcessor} 在 concession+CONDITIONAL 时写 APPROVED + approvedBy/At），非完整 5 动作审批生命周期
 * （无 submit/reject/reverseApprove/withdraw writer）。故本 Bean 仅承载让步审批单边迁移矩阵
 * （concessionApprove: UNSUBMITTED→APPROVED），不建模完整 5 动作。owner doc §实现约定「让步接收审批流简化为
 * approveStatus=APPROVED，完整多级让步审批工作流 Non-Goal」一致。
 *
 * <p>严格无状态（契约 §2）。命名带 {@code Approval} 后缀（契约 §1 双轴约定，与 result 轴分离）。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}，并附 {@code action} 补充诊断参数；
 * 领域 ErrorCode 映射归 Processor（契约 §7）。
 */
public class ErpQaInspectionApprovalStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * concessionApprove（让步审批）守卫：来源态为 {@code UNSUBMITTED}/{@code null} 合法（让步接收降级审批）。
     *
     * <p>非法来源态报告 common 层非法边（携带 {@code action=concessionApprove}/{@code fromStatus}）。
     */
    public void assertCanConcessionApprove(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpQaConstants.APPROVE_STATUS_UNSUBMITTED.equals(status)) {
            throw illegal("concessionApprove", status, ErpQaConstants.APPROVE_STATUS_UNSUBMITTED);
        }
    }

    // ---------- 动作目标态（供 Processor 写回） ----------

    /** concessionApprove 目标态=APPROVED（让步审批通过，质量主管审核）。 */
    public String concessionApproveTargetStatus() {
        return ErpQaConstants.APPROVE_STATUS_APPROVED;
    }

    // ---------- 终态/初始态分类 ----------

    /** 业务终态判定。让步审批轴终态为 APPROVED（owner doc §实现约定「让步接收简化为 approveStatus=APPROVED」）。 */
    public boolean isTerminal(String approveStatus) {
        return ErpQaConstants.APPROVE_STATUS_APPROVED.equals(approveStatus);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("concessionApprove", ErpQaConstants.APPROVE_STATUS_UNSUBMITTED, ErpQaConstants.APPROVE_STATUS_APPROVED)));
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
