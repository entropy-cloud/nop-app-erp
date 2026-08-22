package app.erp.hr.service.processor;

import app.erp.hr.dao.entity.ErpHrDevelopmentPlan;
import app.erp.hr.dao.entity.ErpHrDevelopmentPlanItem;
import app.erp.hr.dao.entity.ErpHrGapAnalysis;
import app.erp.hr.service.ErpHrConstants;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;

import java.util.List;

/**
 * ErpHrDevelopmentPlan generateDevelopmentPlan per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含发展计划生成编排（筛选 CRITICAL/MODERATE 差距 → 新建计划 → 按优先级排序逐项生成建议项）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpHrDevelopmentPlanProcessor}。
 */
public class ErpHrDevelopmentPlanGenerateDevelopmentPlanProcessor extends AbstractErpHrDevelopmentPlanProcessor {

    public ErpHrDevelopmentPlan generateDevelopmentPlan(String employeeId, IServiceContext context) {
        List<ErpHrGapAnalysis> gaps = findActionableGaps(employeeId, context);
        if (gaps.isEmpty()) {
            return null;
        }

        ErpHrDevelopmentPlan plan = planDao().newEntity();

        plan.setBusinessDate(CoreMetrics.today());
        plan.setEmployeeId(employeeId);
        plan.setPlanName("发展计划-" + employeeId + "-" + CoreMetrics.currentDate());
        plan.setTargetDate(CoreMetrics.currentDate().plusMonths(3));
        plan.setStatus(ErpHrConstants.DEV_PLAN_STATUS_IN_PROGRESS);
        planDao().saveEntity(plan);

        List<ErpHrGapAnalysis> sorted = sortByPriority(gaps);
        for (ErpHrGapAnalysis gap : sorted) {
            ErpHrDevelopmentPlanItem item = newPlanItem(plan.getId(), gap);
            planItemBiz.saveEntity(item, null, context);
        }
        return plan;
    }
}
