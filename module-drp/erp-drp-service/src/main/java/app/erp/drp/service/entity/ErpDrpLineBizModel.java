package app.erp.drp.service.entity;

import app.erp.drp.biz.IErpDrpLineBiz;
import app.erp.drp.dao.entity.ErpDrpLine;
import app.erp.drp.dao.entity.ErpDrpPlan;
import app.erp.drp.service.drp.DrpReleaseService;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * DRP 明细行 BizModel。薄委派层：{@link #releaseLine}/{@link #releaseApproved} 委派给 {@link DrpReleaseService}。
 */
@BizModel("ErpDrpLine")
public class ErpDrpLineBizModel extends CrudBizModel<ErpDrpLine> implements IErpDrpLineBiz {

    @Inject
    DrpReleaseService drpReleaseService;

    public ErpDrpLineBizModel() {
        setEntityName(ErpDrpLine.class.getName());
    }

    public void setDrpReleaseService(DrpReleaseService drpReleaseService) {
        this.drpReleaseService = drpReleaseService;
    }

    @Override
    @BizMutation
    public ErpDrpLine releaseLine(@Name("lineId") Long lineId, IServiceContext context) {
        drpReleaseService.releaseLine(lineId);
        return get(String.valueOf(lineId), false, context);
    }

    @Override
    @BizMutation
    public ErpDrpPlan releaseApproved(@Name("planId") Long planId, IServiceContext context) {
        drpReleaseService.releaseApproved(planId);
        return null;
    }
}
