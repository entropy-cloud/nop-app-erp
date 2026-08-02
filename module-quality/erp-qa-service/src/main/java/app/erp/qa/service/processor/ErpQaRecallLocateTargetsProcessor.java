package app.erp.qa.service.processor;

import app.erp.qa.dao.entity.ErpQaRecall;
import app.erp.qa.service.ErpQaConstants;
import app.erp.qa.service.entity.RecallTargetLocator;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpQaRecall locateTargets per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 APPROVED→IN_PROGRESS 目标定位编排（经 {@link RecallTargetLocator} 反查受影响销售出库 → ErpQaRecallTarget）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpQaRecallProcessor}。
 */
public class ErpQaRecallLocateTargetsProcessor extends AbstractErpQaRecallProcessor {

    @Inject
    RecallTargetLocator targetLocator;

    public ErpQaRecall locateTargets(Long recallId, IServiceContext context) {
        ErpQaRecall recall = requireRecall(recallId, context);
        requireRecallStatus(recall, ErpQaConstants.RECALL_STATUS_APPROVED, "APPROVED");
        targetLocator.locate(recall, context);
        recall.setStatus(ErpQaConstants.RECALL_STATUS_IN_PROGRESS);
        recallDao().updateEntity(recall);
        return recall;
    }
}
