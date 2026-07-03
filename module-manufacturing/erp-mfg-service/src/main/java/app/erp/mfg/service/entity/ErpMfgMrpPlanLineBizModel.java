
package app.erp.mfg.service.entity;

import app.erp.mfg.biz.IErpMfgMrpPlanLineBiz;
import app.erp.mfg.dao.entity.ErpMfgMrpPlanLine;
import app.erp.mfg.service.mrp.MrpReleaseService;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

@BizModel("ErpMfgMrpPlanLine")
public class ErpMfgMrpPlanLineBizModel extends CrudBizModel<ErpMfgMrpPlanLine> implements IErpMfgMrpPlanLineBiz {
    @Inject
    MrpReleaseService mrpReleaseService;

    public ErpMfgMrpPlanLineBizModel() {
        setEntityName(ErpMfgMrpPlanLine.class.getName());
    }

    public void setMrpReleaseService(MrpReleaseService mrpReleaseService) {
        this.mrpReleaseService = mrpReleaseService;
    }

    @Override
    @BizMutation
    public ErpMfgMrpPlanLine releasePurchaseRequest(@Name("planLineId") Long planLineId,
                                                     @Name("supplierId") Long supplierId,
                                                     @Name("currencyId") Long currencyId,
                                                     IServiceContext context) {
        mrpReleaseService.releasePurchaseRequest(planLineId, supplierId, currencyId);
        return get(String.valueOf(planLineId), false, context);
    }

    @Override
    @BizMutation
    public ErpMfgMrpPlanLine releaseWorkRequest(@Name("planLineId") Long planLineId, IServiceContext context) {
        mrpReleaseService.releaseWorkRequest(planLineId);
        return get(String.valueOf(planLineId), false, context);
    }
}
