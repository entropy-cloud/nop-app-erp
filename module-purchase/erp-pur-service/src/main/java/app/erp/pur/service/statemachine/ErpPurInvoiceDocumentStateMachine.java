package app.erp.pur.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.pur.dao.constants.ErpPurDocStatus;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 采购发票（{@code ErpPurInvoice}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code docStatus} 业务生命周期轴）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/purchase/state-machine.md}（§三轴状态分离 + §实现模式与守卫边界）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载最小生命周期迁移矩阵
 * （DRAFT→CANCELLED）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>命名带 {@code Document} 后缀（契约 §1 双轴约定，为 approveStatus Bean
 * {@code ErpPurInvoiceApprovalStateMachine} 预留命名空间）。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor/BizModel（契约 §7）。
 *
 * <p>迁移矩阵（1 条边）：cancel(DRAFT→CANCELLED)。cancel 守卫保留既有骨架行为——仅 CANCELLED 终态非法，
 * 其余非终态（DRAFT 及 dict 中其它非终态值）放行，与 {@code AbstractCancelProcessor.validateTransitionForCancel}
 * 收敛前行为一致（行为不变 Non-Goal）。ACTIVE 为 dict 存在但无生产 writer 的死状态（同 Order/Quotation 裁定，
 * 不编码入边）。
 *
 * <p>接线方 {@code ErpPurInvoiceProcessor.validateTransitionForCancel}（经
 * {@code ErpPurInvoiceCancelProcessor} 编排链调用）将 common 码映射为领域码
 * {@code ERR_INVOICE_ILLEGAL_DOC_STATUS_TRANSITION}。
 */
public class ErpPurInvoiceDocumentStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * cancel 守卫：非 CANCELLED 终态合法。
     *
     * <p>对 CANCELLED 报告 common 层非法边（携带 {@code action=cancel}/{@code fromStatus=CANCELLED}）。
     * 接线方 {@code ErpPurInvoiceProcessor.validateTransitionForCancel} 映射为领域码
     * {@code ERR_INVOICE_ILLEGAL_DOC_STATUS_TRANSITION}。
     */
    public void assertCanCancel(String docStatus) {
        if (isTerminal(docStatus)) {
            throw illegal("cancel", docStatus, "非已作废");
        }
    }

    public String cancelTargetStatus() {
        return ErpPurDocStatus.DOC_STATUS_CANCELLED;
    }

    // ---------- 终态/初始态分类 ----------

    public boolean isTerminal(String docStatus) {
        return ErpPurDocStatus.DOC_STATUS_CANCELLED.equals(docStatus);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("cancel", ErpPurDocStatus.DOC_STATUS_DRAFT, ErpPurDocStatus.DOC_STATUS_CANCELLED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(ErpPurDocStatus.DOC_STATUS_CANCELLED));
    }

    public List<String> initialStatuses() {
        return Collections.unmodifiableList(Arrays.asList(ErpPurDocStatus.DOC_STATUS_DRAFT));
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
