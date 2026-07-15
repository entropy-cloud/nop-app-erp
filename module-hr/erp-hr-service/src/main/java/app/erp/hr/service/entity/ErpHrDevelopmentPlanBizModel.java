package app.erp.hr.service.entity;

import app.erp.hr.biz.IErpHrDevelopmentPlanBiz;
import app.erp.hr.biz.IErpHrDevelopmentPlanItemBiz;
import app.erp.hr.biz.IErpHrGapAnalysisBiz;
import app.erp.hr.dao.entity.ErpHrDevelopmentPlan;
import app.erp.hr.dao.entity.ErpHrDevelopmentPlanItem;
import app.erp.hr.dao.entity.ErpHrGapAnalysis;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;
import io.nop.biz.crud.EntityData;

/**
 * 发展计划聚合根 BizModel（competency-management.md §发展计划生成）。CRUD 之上承载：
 * <ul>
 *   <li>{@link #generateDevelopmentPlan} 针对 CRITICAL/MODERATE 差距按 weight/isCritical 排序生成建议项。</li>
 *   <li>{@link #updatePlanItemStatus} 计划项状态机。</li>
 *   <li>{@link #completePlan} 计划状态机。</li>
 * </ul>
 *
 * <p>跨实体读 GapAnalysis 经 {@link IErpHrGapAnalysisBiz}；计划项状态校验经
 * {@link IErpHrDevelopmentPlanItemBiz}。
 */
@BizModel("ErpHrDevelopmentPlan")
public class ErpHrDevelopmentPlanBizModel extends CrudBizModel<ErpHrDevelopmentPlan>
        implements IErpHrDevelopmentPlanBiz {

    @Inject
    IErpHrGapAnalysisBiz gapAnalysisBiz;
    @Inject
    IErpHrDevelopmentPlanItemBiz planItemBiz;

    public ErpHrDevelopmentPlanBizModel() {
        setEntityName(ErpHrDevelopmentPlan.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpHrDevelopmentPlan> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpHrDevelopmentPlan entity = entityData.getEntity();
        if (entity.getBusinessDate() == null) {
            entity.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        }
    }

    @Override
    @BizMutation
    public ErpHrDevelopmentPlan generateDevelopmentPlan(@Name("employeeId") Long employeeId,
                                                         IServiceContext context) {
        List<ErpHrGapAnalysis> gaps = findActionableGaps(employeeId, context);
        if (gaps.isEmpty()) {
            return null;
        }

        ErpHrDevelopmentPlan plan = newEntity();

        plan.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        plan.setEmployeeId(employeeId);
        plan.setPlanName("发展计划-" + employeeId + "-" + CoreMetrics.currentDate());
        plan.setTargetDate(CoreMetrics.currentDate().plusMonths(3));
        plan.setStatus(ErpHrConstants.DEV_PLAN_STATUS_IN_PROGRESS);
        saveEntity(plan, null, context);

        List<ErpHrGapAnalysis> sorted = sortByPriority(gaps);
        for (ErpHrGapAnalysis gap : sorted) {
            ErpHrDevelopmentPlanItem item = newPlanItem(plan.getId(), gap);
            planItemBiz.saveEntity(item, null, context);
        }
        return plan;
    }

    @Override
    @BizMutation
    public ErpHrDevelopmentPlanItem updatePlanItemStatus(@Name("planItemId") Long planItemId,
                                                          @Name("status") String status,
                                                          IServiceContext context) {
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

    @Override
    @BizMutation
    public ErpHrDevelopmentPlan completePlan(@Name("planId") Long planId, IServiceContext context) {
        ErpHrDevelopmentPlan plan = requireEntity(String.valueOf(planId), null, context);
        String status = plan.getStatus();
        if (!ErpHrConstants.DEV_PLAN_STATUS_DRAFT.equals(status)
                && !ErpHrConstants.DEV_PLAN_STATUS_IN_PROGRESS.equals(status)) {
            throw new NopException(ErpHrErrors.ERR_DEV_PLAN_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpHrErrors.ARG_DEV_PLAN_ID, planId)
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, status)
                    .param(ErpHrErrors.ARG_TARGET_STATUS, ErpHrConstants.DEV_PLAN_STATUS_COMPLETED);
        }
        plan.setStatus(ErpHrConstants.DEV_PLAN_STATUS_COMPLETED);
        updateEntity(plan, null, context);
        return plan;
    }

    // ---------- helpers ----------

    List<ErpHrGapAnalysis> findActionableGaps(Long employeeId, IServiceContext context) {
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
    List<ErpHrGapAnalysis> sortByPriority(List<ErpHrGapAnalysis> gaps) {
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

    ErpHrDevelopmentPlanItem newPlanItem(Long planId, ErpHrGapAnalysis gap) {
        IEntityDao<ErpHrDevelopmentPlanItem> dao = daoProvider().daoFor(ErpHrDevelopmentPlanItem.class);
        ErpHrDevelopmentPlanItem item = dao.newEntity();
        item.setPlanId(planId);
        item.setCompetencyId(gap.getCompetencyId());
        item.setGapId(gap.getId());
        item.setTargetLevel(gap.getRequiredLevel());
        item.setDevelopmentAction("针对胜任力差距 (gap=" + nz(gap.getGapValue())
                + ",severity=" + gap.getGapSeverity() + ") 的建议发展行动");
        item.setStatus(ErpHrConstants.PLAN_ITEM_STATUS_NOT_STARTED);
        item.setStartDate(CoreMetrics.currentDate());
        item.setEndDate(CoreMetrics.currentDate().plusMonths(3));
        return item;
    }

    ErpHrDevelopmentPlanItem requirePlanItem(Long planItemId, IServiceContext context) {
        ErpHrDevelopmentPlanItem item = planItemBiz.get(String.valueOf(planItemId), false, context);
        if (item == null) {
            throw new NopException(ErpHrErrors.ERR_DEV_PLAN_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpHrErrors.ARG_DEV_PLAN_ITEM_ID, planItemId);
        }
        return item;
    }

    void assertPlanItemTransition(String current, String target) {
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
