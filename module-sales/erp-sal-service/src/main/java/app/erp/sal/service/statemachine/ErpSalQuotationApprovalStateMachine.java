package app.erp.sal.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.sal.dao.constants.ErpSalDocStatus;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 销售报价单（{@code ErpSalQuotation}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code approveStatus} 审批轴）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/sales/state-machine.md}（§三轴状态分离 + §审批轴 + §实现模式与守卫边界）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载审批轴 5 动作迁移矩阵
 * （submit/approve/reject/reverseApprove/withdraw）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>命名带 {@code Approval} 后缀（契约 §1 双轴约定，与 {@code ErpSalQuotationDocumentStateMachine} docStatus 轴分离）。
 * 矩阵结构与 {@code ErpSalOrderApprovalStateMachine} 一致（同 dict {@code wf/approve-status}，两实体审批轴同构）。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor（契约 §7）。
 *
 * <p><b>reverseApprove 目标态=REJECTED</b>（据实保持 Quotation 当前行为）：实仓核实
 * {@code ErpSalQuotationReverseApproveProcessor.doReverseApprove} **已覆写**为 REJECTED（非骨架默认 SUBMITTED），
 * 已合规 {@code domain-design-guidelines.md §16.4}，且与 owner doc 一致。
 */
public class ErpSalQuotationApprovalStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * submit 守卫：来源态为 {@code UNSUBMITTED}/{@code null}/{@code REJECTED} 合法（初始提交或驳回后重新提交）。
     *
     * <p>非法来源态报告 common 层非法边（携带 {@code action=submit}/{@code fromStatus}）。
     * 接线方 {@code ErpSalQuotationSubmitForApprovalProcessor} 映射为领域码 {@code ERR_QUOTATION_ILLEGAL_STATUS_TRANSITION}。
     */
    public void assertCanSubmit(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpSalDocStatus.APPROVE_STATUS_UNSUBMITTED.equals(status)
                && !ErpSalDocStatus.APPROVE_STATUS_REJECTED.equals(status)) {
            throw illegal("submit", status, ErpSalDocStatus.APPROVE_STATUS_UNSUBMITTED + " / " + ErpSalDocStatus.APPROVE_STATUS_REJECTED);
        }
    }

    /** approve 守卫：来源态为 {@code SUBMITTED} 合法。 */
    public void assertCanApprove(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpSalDocStatus.APPROVE_STATUS_SUBMITTED.equals(status)) {
            throw illegal("approve", status, ErpSalDocStatus.APPROVE_STATUS_SUBMITTED);
        }
    }

    /** reject 守卫：来源态为 {@code SUBMITTED} 合法（与 approve 同源态）。 */
    public void assertCanReject(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpSalDocStatus.APPROVE_STATUS_SUBMITTED.equals(status)) {
            throw illegal("reject", status, ErpSalDocStatus.APPROVE_STATUS_SUBMITTED);
        }
    }

    /** reverseApprove 守卫：来源态为 {@code APPROVED} 合法。 */
    public void assertCanReverseApprove(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpSalDocStatus.APPROVE_STATUS_APPROVED.equals(status)) {
            throw illegal("reverseApprove", status, ErpSalDocStatus.APPROVE_STATUS_APPROVED);
        }
    }

    /** withdraw 守卫：来源态为 {@code SUBMITTED} 合法（审核人未处理时提交人撤回）。 */
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

    /** reverseApprove 目标态=REJECTED（据实保持 Quotation 当前行为，已合规 §16.4）。 */
    public String reverseApproveTargetStatus() {
        return ErpSalDocStatus.APPROVE_STATUS_REJECTED;
    }

    public String withdrawTargetStatus() {
        return ErpSalDocStatus.APPROVE_STATUS_UNSUBMITTED;
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 业务终态判定。审批轴业务终态为 {@code APPROVED}（owner doc §3）；其为「可逆终态」——经 reverseApprove 有出边，
     * 故 {@link #transitions()} 中 APPROVED 存在出边，不适用「终态无出边」的强可达性断言（见矩阵测试）。
     */
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

    /** null 归一化为 UNSUBMITTED（初始态语义：未设置=未提交），与各 Processor getApproveStatus 归一一致。 */
    private static String normalize(String approveStatus) {
        return approveStatus == null ? ErpSalDocStatus.APPROVE_STATUS_UNSUBMITTED : approveStatus;
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
