
package app.erp.mfg.service.entity;

import app.erp.mfg.biz.CrpLoadReportItem;
import app.erp.mfg.biz.IErpMfgCrpLoadBiz;
import app.erp.mfg.dao.entity.ErpMfgCrpLoad;
import app.erp.mfg.service.crp.CrpLoadCalculator;
import app.erp.mfg.service.processor.ErpMfgCrpLoadCalculateLoadProcessor;
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

/**
 * CRP 负荷 BizModel（Facade，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * {@code calculateLoad}（@BizMutation）委托 {@link ErpMfgCrpLoadCalculateLoadProcessor}（R6.2 per-mutation 拆分）；
 * {@code getLoadReport} 为 :45 只读查询保留委托 {@link CrpLoadCalculator}。
 */
@BizModel("ErpMfgCrpLoad")
public class ErpMfgCrpLoadBizModel extends CrudBizModel<ErpMfgCrpLoad> implements IErpMfgCrpLoadBiz {
    @Inject
    CrpLoadCalculator crpLoadCalculator;
    @Inject
    ErpMfgCrpLoadCalculateLoadProcessor calculateLoadProcessor;

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
        return calculateLoadProcessor.calculateLoad(periodFrom, periodTo, workcenterIds, context);
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
