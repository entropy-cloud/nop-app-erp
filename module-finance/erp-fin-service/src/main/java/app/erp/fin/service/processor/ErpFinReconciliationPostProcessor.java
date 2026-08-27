package app.erp.fin.service.processor;

import app.erp.common.service.ErpCommonErrors;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinReconciliation;
import app.erp.fin.dao.entity.ErpFinReconciliationLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.statemachine.ErpFinReconciliationDocumentStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.List;

/**
 * ErpFinReconciliation post per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含核销单过账编排（结算 + 余额重算 + 可选汇兑损益凭证）。共享 helper 单一真相源在
 * {@link AbstractErpFinReconciliationProcessor}。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinReconciliationPostProcessor extends AbstractErpFinReconciliationProcessor {

    @Inject
    ErpFinReconciliationDocumentStateMachine stateMachine;

    public ErpFinReconciliation post(String reconciliationId, IServiceContext context) {
        ErpFinReconciliation head = requireHead(reconciliationId, context);
        assertCanPost(head);
        List<ErpFinReconciliationLine> lines = loadLines(reconciliationId);
        if (lines.isEmpty()) {
            throw new NopException(ErpFinErrors.ERR_RECONCILIATION_NOT_FOUND)
                    .param(ErpFinErrors.ARG_RECONCILIATION_ID, reconciliationId);
        }

        BigDecimal precision = reconcilePrecision();
        for (ErpFinReconciliationLine line : lines) {
            validateLine(head, line, precision);
        }
        // F2.2（P1-CK-fin2-003）：聚合同一辅助账项的多行累计校验——逐行 validateLine 只对单行金额
        // vs item 当前 open，手工单多行共享同一 item 时可静默超核销（settled>amount/open 为负）。
        // 引擎路径自带本地累计 map（构造性合法）不受影响；本校验同时兜底 fin2-001 类超开。
        validateAggregatedNotOver(lines, precision);

        if (isReconFxGainLossEnabled()) {
            BigDecimal fxGainLoss = settler.settleWithFx(head, lines);
            generateReconFxVoucher(head, fxGainLoss);
        } else {
            settler.settle(head, lines);
        }
        head.setDocStatus(stateMachine.postTargetStatus());
        head.setPostedAt(CoreMetrics.currentTimestamp());
        head.setPostedBy(context.getUserContext() != null ? context.getUserContext().getUserId() : null);

        flushBeforeBalance();
        partnerBalanceUpdater.refresh(head.getPartnerId());
        return head;
    }

    /** post 迁移守卫：固定来源态矩阵判断委托状态机 Bean（common 码作 cause，领域码 {@code ERR_RECONCILIATION_STATUS_INVALID}）。 */
    private void assertCanPost(ErpFinReconciliation head) {
        try {
            stateMachine.assertCanPost(head.getDocStatus());
        } catch (NopException e) {
            if (ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode().equals(e.getErrorCode())) {
                throw statusError(head, e);
            }
            throw e;
        }
    }

    /**
     * F2.2（P1-CK-fin2-003）：按 invoiceItemId 与 paymentItemId 分别聚合行金额，
     * 对累计值做 assertNotOver（等价引擎本地 map 逻辑下沉校验层）。
     */
    protected void validateAggregatedNotOver(List<ErpFinReconciliationLine> lines, BigDecimal precision) {
        java.util.Map<String, BigDecimal> invoiceAgg = new java.util.HashMap<>();
        java.util.Map<String, BigDecimal> paymentAgg = new java.util.HashMap<>();
        for (ErpFinReconciliationLine line : lines) {
            BigDecimal amt = line.getSettledAmountFunctional();
            if (amt == null || amt.signum() <= 0) {
                continue;
            }
            if (line.getInvoiceItemId() != null) {
                invoiceAgg.merge(line.getInvoiceItemId(), amt, BigDecimal::add);
            }
            if (line.getPaymentItemId() != null) {
                paymentAgg.merge(line.getPaymentItemId(), amt, BigDecimal::add);
            }
        }
        IEntityDao<ErpFinArApItem> itemDao = daoProvider.daoFor(ErpFinArApItem.class);
        for (java.util.Map.Entry<String, BigDecimal> e : invoiceAgg.entrySet()) {
            ErpFinArApItem item = itemDao.getEntityById(e.getKey());
            if (item != null) {
                assertNotOver(e.getValue(), item, e.getKey(), precision);
            }
        }
        for (java.util.Map.Entry<String, BigDecimal> e : paymentAgg.entrySet()) {
            ErpFinArApItem item = itemDao.getEntityById(e.getKey());
            if (item != null) {
                assertNotOver(e.getValue(), item, e.getKey(), precision);
            }
        }
    }
}