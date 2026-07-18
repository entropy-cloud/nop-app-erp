
package app.erp.mfg.service.entity;

import app.erp.mfg.biz.IErpMfgCostVarianceBiz;
import app.erp.mfg.dao.entity.ErpMfgCostVariance;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.mfg.service.costing.ProductionVarianceCalculator;
import app.erp.mfg.service.posting.ProductionVarianceDispatcher;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 成本差异记录 BizModel（plan 2026-07-05-1838-2）。CRUD 由 {@link CrudBizModel} 默认提供，
 * 额外承载差异分析入口（手动重算 + 查询聚合）。计算逻辑委托
 * {@link ProductionVarianceCalculator}（service-helper，对齐 {@code CostRollupService} 范式）。
 *
 * <p>权威：{@code docs/design/manufacturing/variance-analysis.md}。
 */
@BizModel("ErpMfgCostVariance")
public class ErpMfgCostVarianceBizModel extends CrudBizModel<ErpMfgCostVariance> implements IErpMfgCostVarianceBiz {

    @Inject
    ProductionVarianceCalculator productionVarianceCalculator;
    @Inject
    ProductionVarianceDispatcher productionVarianceDispatcher;

    public ErpMfgCostVarianceBizModel() {
        setEntityName(ErpMfgCostVariance.class.getName());
    }

    public void setProductionVarianceCalculator(ProductionVarianceCalculator productionVarianceCalculator) {
        this.productionVarianceCalculator = productionVarianceCalculator;
    }

    public void setProductionVarianceDispatcher(ProductionVarianceDispatcher productionVarianceDispatcher) {
        this.productionVarianceDispatcher = productionVarianceDispatcher;
    }

    @Override
    @BizMutation
    public List<ErpMfgCostVariance> calculateVariances(@Name("workOrderId") Long workOrderId, IServiceContext context) {
        ErpMfgWorkOrder wo = daoProvider().daoFor(ErpMfgWorkOrder.class).getEntityById(workOrderId);
        if (wo == null) {
            throw new NopException(ErpMfgErrors.ERR_WORK_ORDER_NOT_FOUND)
                    .param(ErpMfgErrors.ARG_WORK_ORDER_ID, workOrderId);
        }
        if (!Objects.equals(wo.getDocStatus(), ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED)) {
            throw new NopException(ErpMfgErrors.ERR_VARIANCE_WORKORDER_NOT_COMPLETED)
                    .param(ErpMfgErrors.ARG_WORK_ORDER_CODE, wo.getCode())
                    .param(ErpMfgErrors.ARG_CURRENT_STATUS, wo.getDocStatus());
        }
        // 重算幂等闭环（plan 2026-07-18-2251-1）：先红冲既有 PRODUCTION_VARIANCE 凭证 → 再删差异旧行 → 再重算 → 再派发新凭证。
        // 缺失红冲步骤会致「数据行新金额 + GL 旧凭证金额」数据分叉（reverseIfExists 内部 try/catch 守护吞无原凭证异常）。
        productionVarianceDispatcher.reverseIfExists(workOrderId);
        // 幂等：先删该工单全部差异旧行，再重算
        productionVarianceCalculator.deleteByWorkOrder(workOrderId);
        List<ErpMfgCostVariance> lines = productionVarianceCalculator.calculateVariances(workOrderId);
        // 差异过账（承接 PPV 范式，失败隔离吞异常保持 posted=false）
        productionVarianceDispatcher.dispatchIfApplicable(workOrderId);
        return lines;
    }

    @Override
    @BizQuery
    public List<ErpMfgCostVariance> findByWorkOrder(@Name("workOrderId") Long workOrderId, IServiceContext context) {
        return productionVarianceCalculator.findByWorkOrder(workOrderId);
    }

    @Override
    @BizQuery
    public Map<String, Map<String, Object>> aggregateByType(@Name("workOrderId") Long workOrderId,
                                                            @Optional @Name("costElement") String costElement,
                                                            IServiceContext context) {
        List<ErpMfgCostVariance> lines = productionVarianceCalculator.findByWorkOrder(workOrderId);
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (ErpMfgCostVariance line : lines) {
            if (costElement != null && !costElement.isEmpty()
                    && !Objects.equals(line.getCostElement(), costElement)) {
                continue;
            }
            String type = line.getVarianceType();
            Map<String, Object> bucket = result.computeIfAbsent(type, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("varianceType", k);
                m.put("standardAmount", BigDecimal.ZERO);
                m.put("actualAmount", BigDecimal.ZERO);
                m.put("varianceAmount", BigDecimal.ZERO);
                return m;
            });
            bucket.put("standardAmount",
                    ((BigDecimal) bucket.get("standardAmount")).add(nullToZero(line.getStandardAmount())));
            bucket.put("actualAmount",
                    ((BigDecimal) bucket.get("actualAmount")).add(nullToZero(line.getActualAmount())));
            bucket.put("varianceAmount",
                    ((BigDecimal) bucket.get("varianceAmount")).add(nullToZero(line.getVarianceAmount())));
        }
        return result;
    }

    private static BigDecimal nullToZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

}
