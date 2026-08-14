package app.erp.mfg.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.mfg.service.ErpMfgConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 委外加工单（{@code ErpMfgSubcontractOrder}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code docStatus} 业务生命周期轴）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/manufacturing/state-machine.md} §适用对象三（8 态核心可执行子集）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载 docStatus 操作动作迁移矩阵
 * （submit/approve/reject/issueMaterials/receiveFinished/postProcessingFee/reverseCompletion/cancel）+ 终态/初始态
 * 分类 + 只读 {@link #transitions()} 元数据。可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>命名带 {@code Document} 后缀（契约 §1 双轴约定，与 {@link ErpMfgSubcontractOrderApprovalStateMachine} approveStatus 轴分离）。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor（契约 §7）。
 * Subcontract 领域码为 {@code ERR_SUBCONTRACT_ILLEGAL_STATUS_TRANSITION}。
 *
 * <p><b>Subcontract 独有差异（plan Phase 2 Decision）</b>：
 * <ul>
 *   <li><b>reject 联动写 docStatus=REJECTED</b>（与 WorkOrder 不同——WorkOrder reject 不写 docStatus）：
 *       本 Bean 编码 reject: SUBMITTED→REJECTED 边；联动写入保留在 facade {@code doReject} 原位（契约 §9.2 选项 c）。</li>
 *   <li><b>submit（docStatus 侧）来源 {DRAFT, REJECTED}</b>：doSubmit 无条件写 docStatus=SUBMITTED（审批轴守卫
 *       {UNSUBMITTED, REJECTED} 是唯一运行时守卫），驳回后重提 docStatus REJECTED→SUBMITTED。本边为元数据/守卫语义，
 *       无独立 docStatus 侧守卫接线。</li>
 *   <li><b>reverseCompletion 为「动态不对称守卫 + 固定状态边」</b>：posted=true 判定是动态业务守卫（含 posted，
 *       非固定状态迁移边），保留在 facade {@code validateCanReverse} 原位；本 Bean 仅编码固定状态边 COMPLETED→CANCELLED。</li>
 *   <li><b>业财红冲回写（{@code MfgSubcontractReversalListener} 写 docStatus=CANCELLED + posted=false）与
 *       MrpRelease spawn（docStatus=APPROVED + approveStatus=APPROVED，跳过审批）</b>：均不经本 Bean 守卫
 *       （§9.2 选项 c 豁免路径）。</li>
 * </ul>
 */
public class ErpMfgSubcontractOrderDocumentStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * submit（docStatus 侧）守卫：来源 {DRAFT, REJECTED}（初始提交或驳回后重提）。
     * 运行时守卫由审批轴 Bean 承载（approveStatus {UNSUBMITTED, REJECTED}），本边为 docStatus 侧矩阵语义。
     */
    public void assertCanSubmit(String status) {
        if (!ErpMfgConstants.SUBCONTRACT_STATUS_DRAFT.equals(status)
                && !ErpMfgConstants.SUBCONTRACT_STATUS_REJECTED.equals(status)) {
            throw illegal("submit", status,
                    ErpMfgConstants.SUBCONTRACT_STATUS_DRAFT + " / " + ErpMfgConstants.SUBCONTRACT_STATUS_REJECTED);
        }
    }

    public String submitTargetStatus() {
        return ErpMfgConstants.SUBCONTRACT_STATUS_SUBMITTED;
    }

    /** approve（docStatus 侧）守卫：仅 SUBMITTED 合法（审核通过后委外单进入已审核）。 */
    public void assertCanApprove(String status) {
        if (!ErpMfgConstants.SUBCONTRACT_STATUS_SUBMITTED.equals(status)) {
            throw illegal("approve", status, ErpMfgConstants.SUBCONTRACT_STATUS_SUBMITTED);
        }
    }

    public String approveTargetStatus() {
        return ErpMfgConstants.SUBCONTRACT_STATUS_APPROVED;
    }

    /** reject（docStatus 侧）守卫：仅 SUBMITTED 合法（Subcontract reject 联动写 docStatus=REJECTED）。 */
    public void assertCanReject(String status) {
        if (!ErpMfgConstants.SUBCONTRACT_STATUS_SUBMITTED.equals(status)) {
            throw illegal("reject", status, ErpMfgConstants.SUBCONTRACT_STATUS_SUBMITTED);
        }
    }

    public String rejectTargetStatus() {
        return ErpMfgConstants.SUBCONTRACT_STATUS_REJECTED;
    }

    /** issueMaterials 守卫：仅 APPROVED 合法（审核通过后方可发料）。 */
    public void assertCanIssueMaterials(String status) {
        if (!ErpMfgConstants.SUBCONTRACT_STATUS_APPROVED.equals(status)) {
            throw illegal("issueMaterials", status, ErpMfgConstants.SUBCONTRACT_STATUS_APPROVED);
        }
    }

    public String issueMaterialsTargetStatus() {
        return ErpMfgConstants.SUBCONTRACT_STATUS_ISSUED;
    }

    /** receiveFinished 守卫：仅 ISSUED 合法（发料后收货）。 */
    public void assertCanReceiveFinished(String status) {
        if (!ErpMfgConstants.SUBCONTRACT_STATUS_ISSUED.equals(status)) {
            throw illegal("receiveFinished", status, ErpMfgConstants.SUBCONTRACT_STATUS_ISSUED);
        }
    }

    public String receiveFinishedTargetStatus() {
        return ErpMfgConstants.SUBCONTRACT_STATUS_RECEIVED;
    }

    /** postProcessingFee 守卫：仅 RECEIVED 合法（收货后加工费过账）。 */
    public void assertCanPostProcessingFee(String status) {
        if (!ErpMfgConstants.SUBCONTRACT_STATUS_RECEIVED.equals(status)) {
            throw illegal("postProcessingFee", status, ErpMfgConstants.SUBCONTRACT_STATUS_RECEIVED);
        }
    }

    public String postProcessingFeeTargetStatus() {
        return ErpMfgConstants.SUBCONTRACT_STATUS_COMPLETED;
    }

    /**
     * reverseCompletion 状态边守卫：仅 COMPLETED 合法。
     *
     * <p>posted=true 前置为动态业务守卫（含 posted 判定，非固定状态迁移边），保留在 facade {@code validateCanReverse}
     * 原位；本 Bean 仅编码固定状态边 COMPLETED→CANCELLED。
     */
    public void assertCanReverseCompletion(String status) {
        if (!ErpMfgConstants.SUBCONTRACT_STATUS_COMPLETED.equals(status)) {
            throw illegal("reverseCompletion", status, ErpMfgConstants.SUBCONTRACT_STATUS_COMPLETED);
        }
    }

    public String reverseCompletionTargetStatus() {
        return ErpMfgConstants.SUBCONTRACT_STATUS_CANCELLED;
    }

    /** cancel 守卫：来源 {DRAFT, SUBMITTED, APPROVED}（未发料/未收货前可取消）。 */
    public void assertCanCancel(String status) {
        if (!ErpMfgConstants.SUBCONTRACT_STATUS_DRAFT.equals(status)
                && !ErpMfgConstants.SUBCONTRACT_STATUS_SUBMITTED.equals(status)
                && !ErpMfgConstants.SUBCONTRACT_STATUS_APPROVED.equals(status)) {
            throw illegal("cancel", status,
                    ErpMfgConstants.SUBCONTRACT_STATUS_DRAFT + " / " + ErpMfgConstants.SUBCONTRACT_STATUS_SUBMITTED
                            + " / " + ErpMfgConstants.SUBCONTRACT_STATUS_APPROVED);
        }
    }

    public String cancelTargetStatus() {
        return ErpMfgConstants.SUBCONTRACT_STATUS_CANCELLED;
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 终态分类：COMPLETED / CANCELLED 均为终态（owner doc §适用对象三 §终态与外部依赖）。
     */
    public boolean isTerminal(String status) {
        return ErpMfgConstants.SUBCONTRACT_STATUS_COMPLETED.equals(status)
                || ErpMfgConstants.SUBCONTRACT_STATUS_CANCELLED.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("submit",
                        ErpMfgConstants.SUBCONTRACT_STATUS_DRAFT, ErpMfgConstants.SUBCONTRACT_STATUS_SUBMITTED),
                new TransitionDefinition("submit",
                        ErpMfgConstants.SUBCONTRACT_STATUS_REJECTED, ErpMfgConstants.SUBCONTRACT_STATUS_SUBMITTED),
                new TransitionDefinition("approve",
                        ErpMfgConstants.SUBCONTRACT_STATUS_SUBMITTED, ErpMfgConstants.SUBCONTRACT_STATUS_APPROVED),
                new TransitionDefinition("reject",
                        ErpMfgConstants.SUBCONTRACT_STATUS_SUBMITTED, ErpMfgConstants.SUBCONTRACT_STATUS_REJECTED),
                new TransitionDefinition("issueMaterials",
                        ErpMfgConstants.SUBCONTRACT_STATUS_APPROVED, ErpMfgConstants.SUBCONTRACT_STATUS_ISSUED),
                new TransitionDefinition("receiveFinished",
                        ErpMfgConstants.SUBCONTRACT_STATUS_ISSUED, ErpMfgConstants.SUBCONTRACT_STATUS_RECEIVED),
                new TransitionDefinition("postProcessingFee",
                        ErpMfgConstants.SUBCONTRACT_STATUS_RECEIVED, ErpMfgConstants.SUBCONTRACT_STATUS_COMPLETED),
                new TransitionDefinition("reverseCompletion",
                        ErpMfgConstants.SUBCONTRACT_STATUS_COMPLETED, ErpMfgConstants.SUBCONTRACT_STATUS_CANCELLED),
                new TransitionDefinition("cancel",
                        ErpMfgConstants.SUBCONTRACT_STATUS_DRAFT, ErpMfgConstants.SUBCONTRACT_STATUS_CANCELLED),
                new TransitionDefinition("cancel",
                        ErpMfgConstants.SUBCONTRACT_STATUS_SUBMITTED, ErpMfgConstants.SUBCONTRACT_STATUS_CANCELLED),
                new TransitionDefinition("cancel",
                        ErpMfgConstants.SUBCONTRACT_STATUS_APPROVED, ErpMfgConstants.SUBCONTRACT_STATUS_CANCELLED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpMfgConstants.SUBCONTRACT_STATUS_COMPLETED,
                ErpMfgConstants.SUBCONTRACT_STATUS_CANCELLED));
    }

    public List<String> initialStatuses() {
        return Collections.unmodifiableList(Collections.singletonList(ErpMfgConstants.SUBCONTRACT_STATUS_DRAFT));
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
