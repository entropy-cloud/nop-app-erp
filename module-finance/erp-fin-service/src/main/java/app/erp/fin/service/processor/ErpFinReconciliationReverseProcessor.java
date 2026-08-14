package app.erp.fin.service.processor;

import app.erp.common.service.ErpCommonErrors;
import app.erp.fin.dao.entity.ErpFinReconciliation;
import app.erp.fin.dao.entity.ErpFinReconciliationLine;
import app.erp.fin.service.statemachine.ErpFinReconciliationDocumentStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.List;

/**
 * ErpFinReconciliation reverse per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含核销单冲销编排（恢复辅助账 + 余额重算 + 可选汇兑损益凭证红冲）。共享 helper 单一真相源在
 * {@link AbstractErpFinReconciliationProcessor}。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinReconciliationReverseProcessor extends AbstractErpFinReconciliationProcessor {

    @Inject
    ErpFinReconciliationDocumentStateMachine stateMachine;

    public ErpFinReconciliation reverse(Long reconciliationId, IServiceContext context) {
        ErpFinReconciliation head = requireHead(reconciliationId, context);
        assertCanReverse(head);
        List<ErpFinReconciliationLine> lines = loadLines(reconciliationId);

        settler.reverseSettle(lines);
        reverseReconFxVoucher(head, context);
        head.setDocStatus(stateMachine.reverseTargetStatus());

        flushBeforeBalance();
        partnerBalanceUpdater.refresh(head.getPartnerId());
        return head;
    }

    /** reverse 迁移守卫：固定来源态矩阵判断委托状态机 Bean（common 码作 cause，领域码 {@code ERR_RECONCILIATION_STATUS_INVALID}）。 */
    private void assertCanReverse(ErpFinReconciliation head) {
        try {
            stateMachine.assertCanReverse(head.getDocStatus());
        } catch (NopException e) {
            if (ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode().equals(e.getErrorCode())) {
                throw statusError(head, e);
            }
            throw e;
        }
    }
}
