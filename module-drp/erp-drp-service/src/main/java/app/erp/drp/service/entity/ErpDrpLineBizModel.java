package app.erp.drp.service.entity;

import java.util.Objects;
import app.erp.drp.biz.IErpDrpLineBiz;
import app.erp.drp.dao.entity.ErpDrpLine;
import app.erp.drp.service.ErpDrpConstants;
import app.erp.drp.service.ErpDrpErrors;
import app.erp.drp.service.processor.ErpDrpLineCancelLineProcessor;
import app.erp.drp.service.processor.ErpDrpLineReleaseApprovedProcessor;
import app.erp.drp.service.processor.ErpDrpLineReleaseLineProcessor;
import app.erp.drp.service.processor.ErpDrpLineRejectLineProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * DRP 明细行 BizModel。薄委派层（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）：
 * {@link #releaseLine}/{@link #releaseApproved}/{@link #rejectLine}/{@link #cancelLine} 各委派独立自包含 Processor；
 * {@link #approveLine}（单步状态迁移 SUGGESTED→APPROVED）保留内联实现。
 */
@BizModel("ErpDrpLine")
public class ErpDrpLineBizModel extends CrudBizModel<ErpDrpLine> implements IErpDrpLineBiz {

    @Inject
    ErpDrpLineReleaseLineProcessor releaseLineProcessor;
    @Inject
    ErpDrpLineReleaseApprovedProcessor releaseApprovedProcessor;
    @Inject
    ErpDrpLineRejectLineProcessor rejectLineProcessor;
    @Inject
    ErpDrpLineCancelLineProcessor cancelLineProcessor;

    public ErpDrpLineBizModel() {
        setEntityName(ErpDrpLine.class.getName());
    }

    @Override
    @BizMutation
    public ErpDrpLine releaseLine(@Name("lineId") Long lineId, IServiceContext context) {
        return releaseLineProcessor.releaseLine(lineId, context);
    }

    @Override
    @BizMutation
    public app.erp.drp.dao.entity.ErpDrpPlan releaseApproved(@Name("planId") Long planId, IServiceContext context) {
        return releaseApprovedProcessor.releaseApproved(planId, context);
    }

    @Override
    @BizMutation
    public ErpDrpLine approveLine(@Name("lineId") Long lineId, IServiceContext context) {
        ErpDrpLine line = requireEntity(String.valueOf(lineId), null, context);
        if (!Objects.equals(line.getStatus(), ErpDrpConstants.DRP_LINE_STATUS_SUGGESTED)) {
            throw new NopException(ErpDrpErrors.ERR_DRP_LINE_ILLEGAL_TRANSITION)
                    .param(ErpDrpErrors.ARG_DRP_LINE_ID, lineId)
                    .param(ErpDrpErrors.ARG_CURRENT_STATUS, line.getStatus());
        }
        line.setStatus(ErpDrpConstants.DRP_LINE_STATUS_APPROVED);
        updateEntity(line, null, context);
        return line;
    }

    @Override
    @BizMutation
    public ErpDrpLine rejectLine(@Name("lineId") Long lineId, IServiceContext context) {
        return rejectLineProcessor.rejectLine(lineId, context);
    }

    @Override
    @BizMutation
    public ErpDrpLine cancelLine(@Name("lineId") Long lineId, IServiceContext context) {
        return cancelLineProcessor.cancelLine(lineId, context);
    }

    @Override
    @BizQuery
    public Map<String, Object> findNetReqGroups(@Optional @Name("planId") Long planId, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.setLimit(5000);
        if (planId != null) {
            q.addFilter(eq("planId", planId));
        }
        List<ErpDrpLine> lines = findList(q, null, context);

        Map<Long, List<ErpDrpLine>> matMap = new LinkedHashMap<>();
        List<Long> matOrder = new ArrayList<>();
        for (ErpDrpLine l : lines) {
            Long mid = l.getMaterialId();
            if (!matMap.containsKey(mid)) {
                matMap.put(mid, new ArrayList<>());
                matOrder.add(mid);
            }
            matMap.get(mid).add(l);
        }

        List<Map<String, Object>> groups = new ArrayList<>();
        BigDecimal grandNet = BigDecimal.ZERO;
        BigDecimal grandSuggested = BigDecimal.ZERO;
        for (Long mid : matOrder) {
            List<ErpDrpLine> gl = matMap.get(mid);
            BigDecimal tSafety = BigDecimal.ZERO, tForecast = BigDecimal.ZERO, tStock = BigDecimal.ZERO;
            BigDecimal tAlloc = BigDecimal.ZERO, tOnOrder = BigDecimal.ZERO, tNet = BigDecimal.ZERO, tSuggested = BigDecimal.ZERO;
            List<Map<String, Object>> detailRows = new ArrayList<>();
            for (ErpDrpLine l : gl) {
                tSafety = tSafety.add(nz(l.getSafetyStock()));
                tForecast = tForecast.add(nz(l.getForecastDemand()));
                tStock = tStock.add(nz(l.getCurrentStock()));
                tAlloc = tAlloc.add(nz(l.getAllocatedQty()));
                tOnOrder = tOnOrder.add(nz(l.getOnOrderQty()));
                tNet = tNet.add(nz(l.getNetRequirement()));
                tSuggested = tSuggested.add(nz(l.getSuggestedQty()));
                Map<String, Object> d = new LinkedHashMap<>();
                d.put("warehouseId", l.getWarehouseId());
                d.put("safetyStock", nz(l.getSafetyStock()).setScale(2, RoundingMode.HALF_UP));
                d.put("forecastDemand", nz(l.getForecastDemand()).setScale(2, RoundingMode.HALF_UP));
                d.put("currentStock", nz(l.getCurrentStock()).setScale(2, RoundingMode.HALF_UP));
                d.put("allocatedQty", nz(l.getAllocatedQty()).setScale(2, RoundingMode.HALF_UP));
                d.put("onOrderQty", nz(l.getOnOrderQty()).setScale(2, RoundingMode.HALF_UP));
                d.put("netRequirement", nz(l.getNetRequirement()).setScale(2, RoundingMode.HALF_UP));
                d.put("suggestedQty", nz(l.getSuggestedQty()).setScale(2, RoundingMode.HALF_UP));
                d.put("replenishmentType", l.getReplenishmentType());
                d.put("status", l.getStatus());
                detailRows.add(d);
            }
            Map<String, Object> g = new LinkedHashMap<>();
            g.put("materialId", mid);
            g.put("lineCount", gl.size());
            g.put("totSafety", tSafety.setScale(2, RoundingMode.HALF_UP));
            g.put("totForecast", tForecast.setScale(2, RoundingMode.HALF_UP));
            g.put("totStock", tStock.setScale(2, RoundingMode.HALF_UP));
            g.put("totAlloc", tAlloc.setScale(2, RoundingMode.HALF_UP));
            g.put("totOnOrder", tOnOrder.setScale(2, RoundingMode.HALF_UP));
            g.put("totNet", tNet.setScale(2, RoundingMode.HALF_UP));
            g.put("totSuggested", tSuggested.setScale(2, RoundingMode.HALF_UP));
            g.put("detailRows", detailRows);
            groups.add(g);
            grandNet = grandNet.add(tNet);
            grandSuggested = grandSuggested.add(tSuggested);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("groups", groups);
        result.put("groupCount", groups.size());
        result.put("totalLines", lines.size());
        result.put("grandNet", grandNet.setScale(2, RoundingMode.HALF_UP));
        result.put("grandSuggested", grandSuggested.setScale(2, RoundingMode.HALF_UP));
        return result;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
