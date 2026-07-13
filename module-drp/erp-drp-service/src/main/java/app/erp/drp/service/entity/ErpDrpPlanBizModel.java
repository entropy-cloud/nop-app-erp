package app.erp.drp.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import app.erp.drp.biz.IErpDrpLineBiz;
import app.erp.drp.biz.IErpDrpPlanBiz;
import app.erp.drp.dao.entity.ErpDrpLine;
import app.erp.drp.dao.entity.ErpDrpPlan;
import app.erp.drp.service.ErpDrpConstants;
import app.erp.drp.service.ErpDrpErrors;
import app.erp.drp.service.drp.DrpDemandAggregator;
import app.erp.drp.service.drp.DrpEngine;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;
import io.nop.biz.crud.EntityData;

/**
 * DRP 计划 BizModel。薄委派层：{@link #runDrp}/{@link #resetToDraft}/{@link #approvePlan} 委派给
 * {@link DrpEngine}（{@code mrp.md §服务层} 范式：BizModel 只负责注解 + 用户身份 + 状态门，编排逻辑在 helper 引擎）。
 */
@BizModel("ErpDrpPlan")
public class ErpDrpPlanBizModel extends CrudBizModel<ErpDrpPlan> implements IErpDrpPlanBiz {

    @Inject
    DrpEngine drpEngine;
    @Inject
    DrpDemandAggregator demandAggregator;
    @Inject
    IErpDrpLineBiz drpLineBiz;

    public ErpDrpPlanBizModel() {
        setEntityName(ErpDrpPlan.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpDrpPlan> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpDrpPlan entity = entityData.getEntity();
        if (entity.getBusinessDate() == null) {
            entity.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        }
    }


    public void setDrpEngine(DrpEngine drpEngine) {
        this.drpEngine = drpEngine;
    }

    public void setDemandAggregator(DrpDemandAggregator demandAggregator) {
        this.demandAggregator = demandAggregator;
    }

    public void setDrpLineBiz(IErpDrpLineBiz drpLineBiz) {
        this.drpLineBiz = drpLineBiz;
    }

    @Override
    @BizMutation
    public ErpDrpPlan runDrp(@Name("planId") Long planId, IServiceContext context) {
        drpEngine.runDrp(planId, demandAggregator.aggregate(planId));
        return get(String.valueOf(planId), false, context);
    }

    @Override
    @BizMutation
    public ErpDrpPlan resetToDraft(@Name("planId") Long planId, IServiceContext context) {
        drpEngine.resetToDraft(planId);
        return get(String.valueOf(planId), false, context);
    }

    @Override
    @BizMutation
    public ErpDrpPlan approvePlan(@Name("planId") Long planId, IServiceContext context) {
        ErpDrpPlan plan = requireEntity(String.valueOf(planId), null, context);
        if (!Objects.equals(plan.getStatus(), ErpDrpConstants.DRP_PLAN_STATUS_COMPUTED)) {
            throw new NopException(ErpDrpErrors.ERR_DRP_PLAN_ILLEGAL_TRANSITION)
                    .param(ErpDrpErrors.ARG_PLAN_CODE, plan.getCode())
                    .param(ErpDrpErrors.ARG_CURRENT_STATUS, plan.getStatus())
                    .param(ErpDrpErrors.ARG_EXPECTED_STATUS, ErpDrpConstants.DRP_PLAN_STATUS_COMPUTED);
        }
        // 该计划下所有 SUGGESTED 行 → APPROVED
        for (ErpDrpLine line : suggestedLinesOf(planId, context)) {
            line.setStatus(ErpDrpConstants.DRP_LINE_STATUS_APPROVED);
            if (line.getApprovedQty() == null || line.getApprovedQty().signum() <= 0) {
                line.setApprovedQty(line.getSuggestedQty());
            }
            drpLineBiz.updateEntity(line, null, context);
        }
        plan.setStatus(ErpDrpConstants.DRP_PLAN_STATUS_APPROVED);
        updateEntity(plan, null, context);
        return plan;
    }

    private List<ErpDrpLine> suggestedLinesOf(Long planId, IServiceContext context) {
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(eq("planId", planId));
        q.addFilter(eq("status", ErpDrpConstants.DRP_LINE_STATUS_SUGGESTED));
        return drpLineBiz.findList(q, null, context);
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpDrpPlan.class)
    public List<String> orgName(@ContextSource List<ErpDrpPlan> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpDrpPlan row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

}
