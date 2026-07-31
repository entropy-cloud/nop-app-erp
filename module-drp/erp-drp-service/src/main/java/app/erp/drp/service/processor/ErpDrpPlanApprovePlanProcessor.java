package app.erp.drp.service.processor;

import app.erp.drp.dao.entity.ErpDrpLine;
import app.erp.drp.dao.entity.ErpDrpPlan;
import app.erp.drp.service.ErpDrpConstants;
import app.erp.drp.service.ErpDrpErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpDrpPlan approvePlan per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含计划审批编排（COMPUTED→APPROVED）：状态门校验 + 该计划下所有 SUGGESTED 行→APPROVED（approvedQty 空则回填 suggestedQty）+ 计划置 APPROVED。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpDrpPlanApprovePlanProcessor {

    @Inject
    IDaoProvider daoProvider;

    public ErpDrpPlan approvePlan(Long planId, IServiceContext context) {
        ErpDrpPlan plan = requirePlan(planId);
        if (!Objects.equals(plan.getStatus(), ErpDrpConstants.DRP_PLAN_STATUS_COMPUTED)) {
            throw new NopException(ErpDrpErrors.ERR_DRP_PLAN_ILLEGAL_TRANSITION)
                    .param(ErpDrpErrors.ARG_PLAN_CODE, plan.getCode())
                    .param(ErpDrpErrors.ARG_CURRENT_STATUS, plan.getStatus())
                    .param(ErpDrpErrors.ARG_EXPECTED_STATUS, ErpDrpConstants.DRP_PLAN_STATUS_COMPUTED);
        }
        // 该计划下所有 SUGGESTED 行 → APPROVED
        for (ErpDrpLine line : suggestedLinesOf(planId)) {
            line.setStatus(ErpDrpConstants.DRP_LINE_STATUS_APPROVED);
            if (line.getApprovedQty() == null || line.getApprovedQty().signum() <= 0) {
                line.setApprovedQty(line.getSuggestedQty());
            }
            lineDao().updateEntity(line);
        }
        plan.setStatus(ErpDrpConstants.DRP_PLAN_STATUS_APPROVED);
        dao().updateEntity(plan);
        return plan;
    }

    // ---------- 内部辅助 ----------

    protected ErpDrpPlan requirePlan(Long planId) {
        ErpDrpPlan plan = dao().getEntityById(planId);
        if (plan == null) {
            throw new NopException(ErpDrpErrors.ERR_DRP_PLAN_NOT_FOUND)
                    .param(ErpDrpErrors.ARG_DRP_PLAN_ID, planId);
        }
        return plan;
    }

    protected List<ErpDrpLine> suggestedLinesOf(Long planId) {
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
