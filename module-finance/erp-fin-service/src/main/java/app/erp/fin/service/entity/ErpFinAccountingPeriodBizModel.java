
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinAccountingPeriodBiz;
import app.erp.fin.dao.PeriodPreCheckReport;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.service.processor.ErpFinAccountingPeriodProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 会计期间聚合根 Biz（Facade，{@code processor-extension-pattern.md} 两层结构）。
 * 期末结账全流程编排（{@code period-close.md §期末结账步骤 / §反结账流程}）委托
 * {@link ErpFinAccountingPeriodProcessor}（protected step 方法，下游可逐 step 覆盖）。
 *
 * <p>事务/会话边界：{@code @BizMutation}+{@code @SingleSession} 钉在 Facade；
 * ORM Session 由编排层 {@link ErpFinAccountingPeriodProcessor} 获取，期末凭证生成完成后再做状态簿记 + flush。
 */
@BizModel("ErpFinAccountingPeriod")
public class ErpFinAccountingPeriodBizModel extends CrudBizModel<ErpFinAccountingPeriod>
        implements IErpFinAccountingPeriodBiz {

    @Inject
    ErpFinAccountingPeriodProcessor periodProcessor;

    public ErpFinAccountingPeriodBizModel() {
        setEntityName(ErpFinAccountingPeriod.class.getName());
    }

    @Override
    @BizQuery
    public PeriodPreCheckReport preCheck(@Name("periodId") Long periodId, IServiceContext context) {
        return periodProcessor.preCheck(periodId, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinAccountingPeriod closePeriod(@Name("periodId") Long periodId, IServiceContext context) {
        return periodProcessor.closePeriod(periodId, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinAccountingPeriod finalizePeriod(@Name("periodId") Long periodId, IServiceContext context) {
        return periodProcessor.finalizePeriod(periodId, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinAccountingPeriod reverseClose(@Name("periodId") Long periodId, IServiceContext context) {
        return periodProcessor.reverseClose(periodId, context);
    }
}
