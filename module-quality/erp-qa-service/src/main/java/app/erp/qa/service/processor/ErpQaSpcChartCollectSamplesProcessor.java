package app.erp.qa.service.processor;

import app.erp.qa.service.spc.SpcSamplingService;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpQaSpcChart collectSamples per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含控制图采样编排（委托 {@link SpcSamplingService}）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpQaSpcChartCollectSamplesProcessor {

    @Inject
    SpcSamplingService spcSamplingService;

    public Integer collectSamples(Long chartId, IServiceContext context) {
        return spcSamplingService.collectSamples(chartId, context);
    }
}
