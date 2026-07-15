
package app.erp.mfg.service.entity;

import app.erp.mfg.biz.CrpLoadReportItem;
import app.erp.mfg.biz.IErpMfgCrpLoadBiz;
import app.erp.mfg.dao.entity.ErpMfgCrpLoad;
import app.erp.mfg.service.crp.CrpLoadCalculator;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.util.List;

@BizModel("ErpMfgCrpLoad")
public class ErpMfgCrpLoadBizModel extends CrudBizModel<ErpMfgCrpLoad> implements IErpMfgCrpLoadBiz {
    @Inject
    CrpLoadCalculator crpLoadCalculator;

    public ErpMfgCrpLoadBizModel() {
        setEntityName(ErpMfgCrpLoad.class.getName());
    }

    public void setCrpLoadCalculator(CrpLoadCalculator crpLoadCalculator) {
        this.crpLoadCalculator = crpLoadCalculator;
    }

    @Override
    @BizMutation
    public Integer calculateLoad(@Name("periodFrom") LocalDate periodFrom,
                                 @Name("periodTo") LocalDate periodTo,
                                 @Optional @Name("workcenterIds") List<Long> workcenterIds,
                                 IServiceContext context) {
        return crpLoadCalculator.calculateLoad(periodFrom, periodTo, workcenterIds);
    }

    @Override
    @BizQuery
    public List<CrpLoadReportItem> getLoadReport(@Name("periodFrom") LocalDate periodFrom,
                                                 @Name("periodTo") LocalDate periodTo,
                                                 @Optional @Name("workcenterIds") List<Long> workcenterIds,
                                                 IServiceContext context) {
        return crpLoadCalculator.getLoadReport(periodFrom, periodTo, workcenterIds);
    }

}
