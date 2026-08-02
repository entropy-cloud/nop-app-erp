package app.erp.qa.service.processor;

import app.erp.qa.dao.entity.ErpQaRecall;
import app.erp.qa.dao.entity.ErpQaRecallTarget;
import app.erp.qa.service.ErpQaConstants;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;

import java.util.List;

/**
 * ErpQaRecall notifyCustomers per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含客户通知编排：逐 target 簿记 notifiedAt/notifiedBy/returnStatus=NOTIFIED，recall.notifyCustomer=true。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpQaRecallProcessor}。
 */
public class ErpQaRecallNotifyCustomersProcessor extends AbstractErpQaRecallProcessor {

    public ErpQaRecall notifyCustomers(Long recallId, IServiceContext context) {
        ErpQaRecall recall = requireRecall(recallId, context);
        requireRecallStatus(recall, ErpQaConstants.RECALL_STATUS_IN_PROGRESS, "IN_PROGRESS");
        for (ErpQaRecallTarget target : loadTargets(recallId, null, context)) {
            target.setNotifiedAt(CoreMetrics.currentTimestamp());
            target.setNotifiedBy(context.getUserId());
            target.setReturnStatus(ErpQaConstants.RECALL_TARGET_RETURN_NOTIFIED);
            recallTargetBiz.updateEntity(target, null, context);
        }
        recall.setNotifyCustomer(Boolean.TRUE);
        recallDao().updateEntity(recall);
        return recall;
    }
}
