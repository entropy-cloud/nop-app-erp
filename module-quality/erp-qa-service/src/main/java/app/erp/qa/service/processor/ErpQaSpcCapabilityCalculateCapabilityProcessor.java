package app.erp.qa.service.processor;

import app.erp.qa.dao.entity.ErpQaSpcCapability;
import app.erp.qa.service.spc.SpcCapabilityCalculator;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.time.LocalDate;

/**
 * ErpQaSpcCapability calculateCapability per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含过程能力分析编排（委托 {@link SpcCapabilityCalculator}）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpQaSpcCapabilityCalculateCapabilityProcessor {

    @Inject
    SpcCapabilityCalculator spcCapabilityCalculator;

    public ErpQaSpcCapability calculateCapability(Long chartId, LocalDate periodFrom, LocalDate periodTo,
                                                  IServiceContext context) {
        return spcCapabilityCalculator.calculateCapability(chartId, periodFrom, periodTo, context);
    }
}
