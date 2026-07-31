package app.erp.mfg.service.processor;

import app.erp.mfg.biz.CostRollupResult;
import app.erp.mfg.service.costing.CostRollupService;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpMfgBom rollupCost per-mutation Processor（R6.2，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 BOM 成本卷算编排（委托 {@link CostRollupService}）；从 ErpMfgBomBizModel 内联 @BizMutation 提取。
 */
public class ErpMfgBomRollupCostProcessor {

    @Inject
    CostRollupService costRollupService;

    public CostRollupResult rollupCost(Long bomId, IServiceContext context) {
        return costRollupService.rollup(bomId);
    }
}
