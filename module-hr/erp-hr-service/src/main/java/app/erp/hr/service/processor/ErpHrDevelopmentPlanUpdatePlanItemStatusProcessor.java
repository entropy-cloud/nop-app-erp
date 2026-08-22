package app.erp.hr.service.processor;

import app.erp.hr.dao.entity.ErpHrDevelopmentPlanItem;
import app.erp.hr.service.ErpHrConstants;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;

/**
 * ErpHrDevelopmentPlan updatePlanItemStatus per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含计划项状态机编排（状态转换守卫 + IN_PROGRESS 起始日 / ACHIEVED 完成日自动簿记）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpHrDevelopmentPlanProcessor}。
 */
public class ErpHrDevelopmentPlanUpdatePlanItemStatusProcessor extends AbstractErpHrDevelopmentPlanProcessor {

    public ErpHrDevelopmentPlanItem updatePlanItemStatus(String planItemId, String status, IServiceContext context) {
        ErpHrDevelopmentPlanItem item = requirePlanItem(planItemId, context);
        String current = item.getStatus();
        assertPlanItemTransition(current, status);
        item.setStatus(status);
        if (ErpHrConstants.PLAN_ITEM_STATUS_IN_PROGRESS.equals(status)
                && item.getStartDate() == null) {
            item.setStartDate(CoreMetrics.currentDate());
        }
        if (ErpHrConstants.PLAN_ITEM_STATUS_ACHIEVED.equals(status)
                && item.getEndDate() == null) {
            item.setEndDate(CoreMetrics.currentDate());
        }
        planItemBiz.updateEntity(item, null, context);
        return item;
    }
}
