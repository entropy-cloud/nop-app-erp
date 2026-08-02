package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinReconciliation;
import app.erp.fin.dao.entity.ErpFinReconciliationLine;
import app.erp.fin.service.ErpFinConstants;
import io.nop.core.context.IServiceContext;

import java.util.List;

/**
 * ErpFinReconciliation reverse per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含核销单冲销编排（恢复辅助账 + 余额重算 + 可选汇兑损益凭证红冲）。共享 helper 单一真相源在
 * {@link AbstractErpFinReconciliationProcessor}。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinReconciliationReverseProcessor extends AbstractErpFinReconciliationProcessor {

    public ErpFinReconciliation reverse(Long reconciliationId, IServiceContext context) {
        ErpFinReconciliation head = requireHead(reconciliationId, context);
        if (!ErpFinConstants.RECON_STATUS_POSTED.equals(head.getDocStatus())) {
            throw statusError(head);
        }
        List<ErpFinReconciliationLine> lines = loadLines(reconciliationId);

        settler.reverseSettle(lines);
        reverseReconFxVoucher(head, context);
        head.setDocStatus(ErpFinConstants.RECON_STATUS_REVERSED);

        flushBeforeBalance();
        partnerBalanceUpdater.refresh(head.getPartnerId());
        return head;
    }
}
