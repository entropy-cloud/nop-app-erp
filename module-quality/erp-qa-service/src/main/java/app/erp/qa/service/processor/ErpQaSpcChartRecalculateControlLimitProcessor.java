package app.erp.qa.service.processor;

import app.erp.qa.service.spc.SpcControlLimitCalculator;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpQaSpcChart recalculateControlLimit per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含控制图控制限重算编排（委托 {@link SpcControlLimitCalculator}）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpQaSpcChartRecalculateControlLimitProcessor {

    @Inject
    SpcControlLimitCalculator spcControlLimitCalculator;

    public Boolean recalculateControlLimit(Long chartId, IServiceContext context) {
        return spcControlLimitCalculator.recalculate(chartId);
    }
}
