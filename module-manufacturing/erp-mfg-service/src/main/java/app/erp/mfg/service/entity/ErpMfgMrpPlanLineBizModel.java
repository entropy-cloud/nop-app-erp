
package app.erp.mfg.service.entity;

import app.erp.mfg.biz.IErpMfgMrpPlanLineBiz;
import app.erp.mfg.dao.entity.ErpMfgMrpPlanLine;
import app.erp.mfg.service.processor.ErpMfgMrpPlanLineReleasePurchaseRequestProcessor;
import app.erp.mfg.service.processor.ErpMfgMrpPlanLineReleaseSubcontractRequestProcessor;
import app.erp.mfg.service.processor.ErpMfgMrpPlanLineReleaseWorkRequestProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * MRP 计划行 BizModel（Facade，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 三个 release*（@BizMutation）各委托独立 per-mutation Processor（R6.2 拆分）。
 */
@BizModel("ErpMfgMrpPlanLine")
public class ErpMfgMrpPlanLineBizModel extends CrudBizModel<ErpMfgMrpPlanLine> implements IErpMfgMrpPlanLineBiz {
    @Inject
    ErpMfgMrpPlanLineReleasePurchaseRequestProcessor releasePurchaseRequestProcessor;
    @Inject
    ErpMfgMrpPlanLineReleaseSubcontractRequestProcessor releaseSubcontractRequestProcessor;
    @Inject
    ErpMfgMrpPlanLineReleaseWorkRequestProcessor releaseWorkRequestProcessor;

    public ErpMfgMrpPlanLineBizModel() {
        setEntityName(ErpMfgMrpPlanLine.class.getName());
    }

    @Override
    @BizMutation
    public ErpMfgMrpPlanLine releasePurchaseRequest(@Name("planLineId") Long planLineId,
                                                     @Name("supplierId") Long supplierId,
                                                     @Name("currencyId") Long currencyId,
                                                     IServiceContext context) {
        return releasePurchaseRequestProcessor.releasePurchaseRequest(planLineId, supplierId, currencyId, context);
    }

    @Override
    @BizMutation
    public ErpMfgMrpPlanLine releaseWorkRequest(@Name("planLineId") Long planLineId, IServiceContext context) {
        return releaseWorkRequestProcessor.releaseWorkRequest(planLineId, context);
    }

    @Override
    @BizMutation
    public ErpMfgMrpPlanLine releaseSubcontractRequest(@Name("planLineId") Long planLineId,
                                                        @Name("supplierId") Long supplierId,
                                                        @Name("currencyId") Long currencyId,
                                                        IServiceContext context) {
        return releaseSubcontractRequestProcessor.releaseSubcontractRequest(planLineId, supplierId, currencyId, context);
    }
}
