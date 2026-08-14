package app.erp.fin.service.processor;

import app.erp.common.service.ErpCommonErrors;
import app.erp.fin.dao.entity.ErpFinReconciliation;
import app.erp.fin.dao.entity.ErpFinReconciliationLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.statemachine.ErpFinReconciliationDocumentStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
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

    public ErpFinReconciliation post(Long reconciliationId, IServiceContext context) {
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
}
