package app.erp.mfg.service.processor;

import app.erp.mfg.service.crp.CrpLoadCalculator;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.util.List;

/**
 * ErpMfgCrpLoad calculateLoad per-mutation Processor（R6.2，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 CRP 负荷计算编排（委托 {@link CrpLoadCalculator}）；从 ErpMfgCrpLoadBizModel 内联 @BizMutation 提取。
 */
public class ErpMfgCrpLoadCalculateLoadProcessor {

    @Inject
    CrpLoadCalculator crpLoadCalculator;

    public Integer calculateLoad(LocalDate periodFrom, LocalDate periodTo,
                                 List<Long> workcenterIds, IServiceContext context) {
        return crpLoadCalculator.calculateLoad(periodFrom, periodTo, workcenterIds);
    }
}
