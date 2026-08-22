package app.erp.hr.service.processor;

import app.erp.hr.biz.IErpHrDevelopmentPlanItemBiz;
import app.erp.hr.biz.IErpHrGapAnalysisBiz;
import app.erp.hr.dao.entity.ErpHrDevelopmentPlan;
import app.erp.hr.dao.entity.ErpHrDevelopmentPlanItem;
import app.erp.hr.dao.entity.ErpHrGapAnalysis;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * 发展计划 per-mutation Processor 共享基类（R6.7，{@code processor-extension-pattern.md} facade protected helper 范式）。
 * 承载 generateDevelopmentPlan/updatePlanItemStatus 共用的加载、差距筛选、排序与计划项状态守卫辅助（单一真相源）。
 * 子类只编排单 mutation 步骤顺序。
 */
public abstract class AbstractErpHrDevelopmentPlanProcessor {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IErpHrGapAnalysisBiz gapAnalysisBiz;
    @Inject
    IErpHrDevelopmentPlanItemBiz planItemBiz;

    protected IEntityDao<ErpHrDevelopmentPlan> planDao() {
        return daoProvider.daoFor(ErpHrDevelopmentPlan.class);
    }

    protected IEntityDao<ErpHrDevelopmentPlanItem> planItemDao() {
        return daoProvider.daoFor(ErpHrDevelopmentPlanItem.class);
    }

    protected List<ErpHrGapAnalysis> findActionableGaps(String employeeId, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("employeeId", employeeId),
                in("gapSeverity", actionableSeverities())));
        return gapAnalysisBiz.findList(q, null, context);
    }

    static List<String> actionableSeverities() {
        List<String> list = new ArrayList<>();
        list.add(ErpHrConstants.GAP_SEVERITY_CRITICAL);
        list.add(ErpHrConstants.GAP_SEVERITY_MODERATE);
        return list;
    }

    /**
     * 排序优先级：CRITICAL 先于 MODERATE；同 severity 内 isCritical=true 先于 false；
     * 最后按 gapValue 降序（差距越大越先处理）。
     */
    protected List<ErpHrGapAnalysis> sortByPriority(List<ErpHrGapAnalysis> gaps) {
        List<ErpHrGapAnalysis> list = new ArrayList<>(gaps);
        list.sort(Comparator
                .comparingInt((ErpHrGapAnalysis g) -> severityRank(g.getGapSeverity()))
                .thenComparing(g -> gapValueForCompare(g.getGapValue()))
                .reversed());
        return list;
    }

    static int severityRank(String severity) {
        if (ErpHrConstants.GAP_SEVERITY_CRITICAL.equals(severity)) return 2;
        if (ErpHrConstants.GAP_SEVERITY_MODERATE.equals(severity)) return 1;
        return 0;
    }

    static int gapValueForCompare(Integer v) {
        return v != null ? v : 0;
    }

    protected ErpHrDevelopmentPlanItem newPlanItem(String planId, ErpHrGapAnalysis gap) {
        ErpHrDevelopmentPlanItem item = planItemDao().newEntity();
        item.setPlanId(planId);
        item.setCompetencyId(gap.getCompetencyId());
        item.setGapId(gap.getId());
        item.setTargetLevel(gap.getRequiredLevel());
        item.setDevelopmentAction("针对胜任力差距 (gap=" + nz(gap.getGapValue())
                + ",severity=" + gap.getGapSeverity() + ") 的建议发展行动");
        item.setStatus(ErpHrConstants.PLAN_ITEM_STATUS_NOT_STARTED);
        item.setStartDate(io.nop.api.core.time.CoreMetrics.currentDate());
        item.setEndDate(io.nop.api.core.time.CoreMetrics.currentDate().plusMonths(3));
        return item;
    }

    protected ErpHrDevelopmentPlanItem requirePlanItem(String planItemId, IServiceContext context) {
        ErpHrDevelopmentPlanItem item = planItemBiz.get(String.valueOf(planItemId), false, context);
        if (item == null) {
            throw new NopException(ErpHrErrors.ERR_DEV_PLAN_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpHrErrors.ARG_DEV_PLAN_ITEM_ID, planItemId);
        }
        return item;
    }

    protected void assertPlanItemTransition(String current, String target) {
        if (!isValidPlanItemTransition(current, target)) {
            throw new NopException(ErpHrErrors.ERR_DEV_PLAN_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpHrErrors.ARG_DEV_PLAN_ITEM_ID, null)
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, current)
                    .param(ErpHrErrors.ARG_TARGET_STATUS, target);
        }
    }

    /**
     * 计划项状态机（competency-management.md §发展计划项 status）：
     * NOT_STARTED→IN_PROGRESS；IN_PROGRESS→ACHIEVED/OVERDUE；OVERDUE/ACHIEVED 终态。
     */
    static boolean isValidPlanItemTransition(String current, String target) {
        if (Objects.equals(current, target)) return false;
        if (ErpHrConstants.PLAN_ITEM_STATUS_NOT_STARTED.equals(current)) {
            return ErpHrConstants.PLAN_ITEM_STATUS_IN_PROGRESS.equals(target);
        }
        if (ErpHrConstants.PLAN_ITEM_STATUS_IN_PROGRESS.equals(current)) {
            return ErpHrConstants.PLAN_ITEM_STATUS_ACHIEVED.equals(target)
                    || ErpHrConstants.PLAN_ITEM_STATUS_OVERDUE.equals(target);
        }
        return false;
    }

    static int nz(Integer v) {
        return v != null ? v : 0;
    }
}
