
package app.erp.mfg.service.entity;

import app.erp.mfg.biz.IErpMfgCostVarianceBiz;
import app.erp.mfg.dao.entity.ErpMfgCostVariance;
import app.erp.mfg.service.costing.ProductionVarianceCalculator;
import app.erp.mfg.service.processor.ErpMfgCostVarianceCalculateVariancesProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
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
 * 额外承载差异分析入口。{@code calculateVariances}（@BizMutation）委托
 * {@link ErpMfgCostVarianceCalculateVariancesProcessor}（R6.2 per-mutation 拆分）；查询聚合（@BizQuery）保留委托
 * {@link ProductionVarianceCalculator}。
 *
 * <p>权威：{@code docs/design/manufacturing/variance-analysis.md}。
 */
@BizModel("ErpMfgCostVariance")
public class ErpMfgCostVarianceBizModel extends CrudBizModel<ErpMfgCostVariance> implements IErpMfgCostVarianceBiz {

    @Inject
    ProductionVarianceCalculator productionVarianceCalculator;
    @Inject
    ErpMfgCostVarianceCalculateVariancesProcessor calculateVariancesProcessor;

    public ErpMfgCostVarianceBizModel() {
        setEntityName(ErpMfgCostVariance.class.getName());
    }

    public void setProductionVarianceCalculator(ProductionVarianceCalculator productionVarianceCalculator) {
        this.productionVarianceCalculator = productionVarianceCalculator;
    }

    @Override
    @BizMutation
    public List<ErpMfgCostVariance> calculateVariances(@Name("workOrderId") Long workOrderId, IServiceContext context) {
        return calculateVariancesProcessor.calculateVariances(workOrderId, context);
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
