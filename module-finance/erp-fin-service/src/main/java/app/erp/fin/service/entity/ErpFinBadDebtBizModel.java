
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinBadDebtBiz;
import app.erp.fin.dao.dto.BadDebtProvisionResult;
import app.erp.fin.dao.entity.ErpFinBadDebt;
import app.erp.fin.service.baddebt.BadDebtProvisionService;
import app.erp.fin.service.processor.ErpFinBadDebtProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 坏账单聚合根 Biz（Facade，{@code processor-extension-pattern.md} 两层结构）。
 * 三轴审批状态机 + ArApItem 变异 + 坏账凭证生成委托 {@link ErpFinBadDebtProcessor}；
 * 期末计提/释放委托 {@link BadDebtProvisionService}。
 *
 * <p>语义与配置门控见 {@code bad-debt.md}；{@code @BizMutation}+{@code @SingleSession} 钉事务/会话边界。
 */
@BizModel("ErpFinBadDebt")
public class ErpFinBadDebtBizModel extends CrudBizModel<ErpFinBadDebt> implements IErpFinBadDebtBiz {

    @Inject
    ErpFinBadDebtProcessor badDebtProcessor;
    @Inject
    BadDebtProvisionService badDebtProvisionService;

    public ErpFinBadDebtBizModel() {
        setEntityName(ErpFinBadDebt.class.getName());
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinBadDebt writeOff(@Name("arApItemId") Long arApItemId,
                                  @Name("reason") String reason,
                                  IServiceContext context) {
        return badDebtProcessor.writeOff(arApItemId, reason, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinBadDebt recover(@Name("arApItemId") Long arApItemId,
                                 @Name("reason") String reason,
                                 IServiceContext context) {
        return badDebtProcessor.recover(arApItemId, reason, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinBadDebt submit(@Name("id") Long id, IServiceContext context) {
        return badDebtProcessor.submit(id, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinBadDebt approve(@Name("id") Long id, IServiceContext context) {
        return badDebtProcessor.approve(id, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinBadDebt reject(@Name("id") Long id, IServiceContext context) {
        return badDebtProcessor.reject(id, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public BadDebtProvisionResult runBadDebtProvision(@Name("periodId") Long periodId, IServiceContext context) {
        return badDebtProvisionService.runBadDebtProvision(periodId, context);
    }
}
