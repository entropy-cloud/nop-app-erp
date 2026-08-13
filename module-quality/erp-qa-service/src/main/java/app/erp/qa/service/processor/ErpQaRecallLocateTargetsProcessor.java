package app.erp.qa.service.processor;

import app.erp.qa.dao.entity.ErpQaRecall;
import app.erp.qa.service.entity.RecallTargetLocator;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpQaRecall locateTargets per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 APPROVED→IN_PROGRESS 目标定位编排（经 {@link RecallTargetLocator} 反查受影响销售出库 → ErpQaRecallTarget）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpQaRecallProcessor}。
 *
 * <p>status 轴来源态守卫委托 {@code statusStateMachine.assertCanLocateTargets}（非法边→领域码
 * {@code ERR_INVALID_RECALL_STATUS_TRANSITION}）；目标态委托 {@code statusStateMachine.locateTargetsTargetStatus()}。
 */
public class ErpQaRecallLocateTargetsProcessor extends AbstractErpQaRecallProcessor {

    @Inject
    RecallTargetLocator targetLocator;

    public ErpQaRecall locateTargets(Long recallId, IServiceContext context) {
        ErpQaRecall recall = requireRecall(recallId, context);
        String current = recall.getStatus();
        try {
            statusStateMachine.assertCanLocateTargets(current);
        } catch (NopException e) {
            throw illegalRecallTransition(recall, current, "APPROVED");
        }
        targetLocator.locate(recall, context);
        recall.setStatus(statusStateMachine.locateTargetsTargetStatus());
        recallDao().updateEntity(recall);
        return recall;
    }
}
