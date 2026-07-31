package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgMrpPlanLine;
import app.erp.mfg.service.mrp.MrpReleaseService;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpMfgMrpPlanLine releaseSubcontractRequest per-mutation Processor（R6.2，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含委外申请释放编排（委托 {@link MrpReleaseService} → 重载计划行返回）；从 ErpMfgMrpPlanLineBizModel 内联 @BizMutation 提取。
 */
public class ErpMfgMrpPlanLineReleaseSubcontractRequestProcessor {

    @Inject
    MrpReleaseService mrpReleaseService;
    @Inject
    IDaoProvider daoProvider;

    public ErpMfgMrpPlanLine releaseSubcontractRequest(Long planLineId, Long supplierId, Long currencyId,
                                                       IServiceContext context) {
        mrpReleaseService.releaseSubcontractRequest(planLineId, supplierId, currencyId);
        return lineDao().getEntityById(planLineId);
    }

    protected IEntityDao<ErpMfgMrpPlanLine> lineDao() {
        return daoProvider.daoFor(ErpMfgMrpPlanLine.class);
    }
}
