package app.erp.drp.service.processor;

import app.erp.drp.dao.entity.ErpDrpLine;
import app.erp.drp.dao.entity.ErpDrpPlan;
import app.erp.drp.service.ErpDrpConstants;
import app.erp.drp.service.ErpDrpErrors;
import app.erp.drp.service.statemachine.ErpDrpLineStateMachine;
import app.erp.drp.service.statemachine.ErpDrpPlanStateMachine;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpDrpLine、ErpDrpPlan）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * ErpDrpPlan approvePlan per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含计划审批编排（COMPUTED→APPROVED）：状态门校验 + 该计划下所有 SUGGESTED 行→APPROVED（approvedQty 空则回填 suggestedQty）+ 计划置 APPROVED。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 *
 * <p>接线（plan 2026-08-12-1841-1 Phase 2）：Plan COMPUTED→APPROVED 经 {@link ErpDrpPlanStateMachine}，
 * 行级联 SUGGESTED→APPROVED 经 {@link ErpDrpLineStateMachine}（行的状态迁移也由 Bean 治理）。
 */
public class ErpDrpPlanApprovePlanProcessor {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    ErpDrpPlanStateMachine planStateMachine;
    @Inject
    ErpDrpLineStateMachine lineStateMachine;

    public ErpDrpPlan approvePlan(String planId, IServiceContext context) {
        ErpDrpPlan plan = requirePlan(planId);
        // 固定来源态守卫经 Plan StateMachine Bean（非法边 Bean 直抛领域码，plan 2026-09-07-2200-1）；本处同码补参 planCode。
        try {
            planStateMachine.assertCanApprovePlan(plan.getStatus());
        } catch (NopException e) {
            throw e.param(ErpDrpErrors.ARG_PLAN_CODE, plan.getCode());
        }
        // 该计划下所有 SUGGESTED 行 → APPROVED（行级联是 approvePlan 的副作用，行的状态迁移也经 Line Bean 治理）
        for (ErpDrpLine line : suggestedLinesOf(planId)) {
            try {
                lineStateMachine.assertCanApproveLine(line.getStatus());
            } catch (NopException e) {
                // suggestedLinesOf 已 filter status=SUGGESTED，此处为防御性守卫；若数据漂移导致非 SUGGESTED 行入选，
                // Bean 直抛领域码 ERR_DRP_LINE_ILLEGAL_TRANSITION，本处同码补参 drpLineId。
                throw e.param(ErpDrpErrors.ARG_DRP_LINE_ID, line.getId());
            }
            line.setStatus(lineStateMachine.approveLineTargetStatus());
            if (line.getApprovedQty() == null || line.getApprovedQty().signum() <= 0) {
                line.setApprovedQty(line.getSuggestedQty());
            }
            lineDao().updateEntity(line);
        }
        plan.setStatus(planStateMachine.approvePlanTargetStatus());
        dao().updateEntity(plan);
        return plan;
    }

    // ---------- 内部辅助 ----------

    protected ErpDrpPlan requirePlan(String planId) {
        ErpDrpPlan plan = dao().getEntityById(planId);
        if (plan == null) {
            throw new NopException(ErpDrpErrors.ERR_DRP_PLAN_NOT_FOUND)
                    .param(ErpDrpErrors.ARG_DRP_PLAN_ID, planId);
        }
        return plan;
    }

    protected List<ErpDrpLine> suggestedLinesOf(String planId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("planId", planId));
        q.addFilter(eq("status", ErpDrpConstants.DRP_LINE_STATUS_SUGGESTED));
        return lineDao().findAllByQuery(q);
    }

    private IEntityDao<ErpDrpPlan> dao() {
        return daoProvider.daoFor(ErpDrpPlan.class);
    }

    private IEntityDao<ErpDrpLine> lineDao() {
        return daoProvider.daoFor(ErpDrpLine.class);
    }
}
