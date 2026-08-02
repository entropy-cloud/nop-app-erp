package app.erp.drp.service.processor;

import app.erp.drp.dao.entity.ErpInvDrpSafetyStockCalc;
import app.erp.drp.service.safetystock.SafetyStockEngine;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpInvDrpSafetyStockCalc calculate per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含安全库存计算编排：委派 {@link SafetyStockEngine#calculate}（STATISTICAL/SIMPLE/DDMRP 三法，历史不足自动降级 SIMPLE）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpInvDrpSafetyStockCalcCalculateProcessor {

    @Inject
    SafetyStockEngine safetyStockEngine;

    public ErpInvDrpSafetyStockCalc calculate(Long calcId, IServiceContext context) {
        return safetyStockEngine.calculate(calcId);
    }
}
