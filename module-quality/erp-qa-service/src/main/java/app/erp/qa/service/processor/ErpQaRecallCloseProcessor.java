package app.erp.qa.service.processor;

import app.erp.qa.dao.entity.ErpQaRecall;
import app.erp.qa.service.ErpQaConfigs;
import app.erp.qa.service.ErpQaConstants;
import app.erp.qa.service.ErpQaErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpQaRecall close per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 IN_PROGRESS→CLOSED 关闭编排，含通知门控（config-gated erp-qua.recall-notify-required-to-close）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpQaRecallProcessor}。
 */
public class ErpQaRecallCloseProcessor extends AbstractErpQaRecallProcessor {

    public ErpQaRecall close(Long recallId, IServiceContext context) {
        ErpQaRecall recall = requireRecall(recallId, context);
        requireRecallStatus(recall, ErpQaConstants.RECALL_STATUS_IN_PROGRESS, "IN_PROGRESS");
        // 通知门控：配置开启时全部 target returnStatus≠PENDING 且 notifyCustomer=true 方可 CLOSED
        if (ErpQaConfigs.isRecallNotifyRequiredToClose()) {
            if (!Boolean.TRUE.equals(recall.getNotifyCustomer())) {
                throw new NopException(ErpQaErrors.ERR_RECALL_NOTIFY_INCOMPLETE)
                        .param(ErpQaErrors.ARG_RECALL_CODE, recall.getCode());
            }
            QueryBean q = new QueryBean();
            q.addFilter(eq("recallId", recallId));
            q.addFilter(eq("returnStatus", ErpQaConstants.RECALL_TARGET_RETURN_PENDING));
            long pending = recallTargetBiz.findCount(q, context);
            if (pending > 0) {
                throw new NopException(ErpQaErrors.ERR_RECALL_NOTIFY_INCOMPLETE)
                        .param(ErpQaErrors.ARG_RECALL_CODE, recall.getCode());
            }
        }
        recall.setStatus(ErpQaConstants.RECALL_STATUS_CLOSED);
        recallDao().updateEntity(recall);
        return recall;
    }
}
