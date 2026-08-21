
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinAccountingPeriodBiz;
import app.erp.fin.dao.PeriodPreCheckReport;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.service.processor.ErpFinAccountingPeriodClosePeriodProcessor;
import app.erp.fin.service.processor.ErpFinAccountingPeriodFinalizePeriodProcessor;
import app.erp.fin.service.processor.ErpFinAccountingPeriodGenerateNextYearPeriodsProcessor;
import app.erp.fin.service.processor.ErpFinAccountingPeriodOpenPeriodProcessor;
import app.erp.fin.service.processor.ErpFinAccountingPeriodPreCheckProcessor;
import app.erp.fin.service.processor.ErpFinAccountingPeriodReverseCloseProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 会计期间聚合根 Biz（Facade，{@code processor-extension-pattern.md} 两层结构）。
 * 期末结账全流程编排（{@code period-close.md §期末结账步骤 / §反结账流程}）委托
 * {@link ErpFinAccountingPeriodProcessor}（protected step 方法，下游可逐 step 覆盖）。
 *
 * <p>事务/会话边界：{@code @BizMutation} 钉在 Facade；
 * ORM Session 由编排层 {@link ErpFinAccountingPeriodProcessor} 获取，期末凭证生成完成后再做状态簿记 + flush。
 */
@BizModel("ErpFinAccountingPeriod")
public class ErpFinAccountingPeriodBizModel extends CrudBizModel<ErpFinAccountingPeriod>
        implements IErpFinAccountingPeriodBiz {

    @Inject
    ErpFinAccountingPeriodPreCheckProcessor preCheckProcessor;
    @Inject
    ErpFinAccountingPeriodClosePeriodProcessor closePeriodProcessor;
    @Inject
    ErpFinAccountingPeriodFinalizePeriodProcessor finalizePeriodProcessor;
    @Inject
    ErpFinAccountingPeriodGenerateNextYearPeriodsProcessor generateNextYearPeriodsProcessor;
    @Inject
    ErpFinAccountingPeriodReverseCloseProcessor reverseCloseProcessor;
    @Inject
    ErpFinAccountingPeriodOpenPeriodProcessor openPeriodProcessor;

    public ErpFinAccountingPeriodBizModel() {
        setEntityName(ErpFinAccountingPeriod.class.getName());
    }

    @Override
    @BizQuery
    public PeriodPreCheckReport preCheck(@Name("periodId") String periodId, IServiceContext context) {
        return preCheckProcessor.preCheck(periodId, context);
    }

    @Override
    @BizMutation
    public ErpFinAccountingPeriod closePeriod(@Name("periodId") String periodId, IServiceContext context) {
        return closePeriodProcessor.closePeriod(periodId, context);
    }

    @Override
    @BizMutation
    public ErpFinAccountingPeriod finalizePeriod(@Name("periodId") String periodId, IServiceContext context) {
        return finalizePeriodProcessor.finalizePeriod(periodId, context);
    }

    @Override
    @BizMutation
    public ErpFinAccountingPeriod reverseClose(@Name("periodId") String periodId,
                                               @Name("reason") String reason,
                                               IServiceContext context) {
        return reverseCloseProcessor.reverseClose(periodId, reason, context);
    }

    @Override
    @BizMutation
    public ErpFinAccountingPeriod openPeriod(@Name("periodId") String periodId, IServiceContext context) {
        return openPeriodProcessor.openPeriod(periodId, context);
    }

    @Override
    @BizMutation
    public Integer generateNextYearPeriods(@Name("year") Integer year, IServiceContext context) {
        return generateNextYearPeriodsProcessor.generateNextYearPeriods(year, context);
    }
}
