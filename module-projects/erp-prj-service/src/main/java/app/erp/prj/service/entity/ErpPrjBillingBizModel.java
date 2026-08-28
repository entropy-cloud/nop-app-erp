package app.erp.prj.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.prj.biz.IErpPrjBillingBiz;
import app.erp.prj.dao.entity.ErpPrjBilling;

import java.math.BigDecimal;

/**
 * 开票单 BizModel（P1-CK-prj-002 修复：amountFunctional 联动）。
 *
 * <p>修复前 {@code amountFunctional} 全仓零 writer（默认 0）——PnL 收入/结算收入读该字段恒 0，
 * 开票金额 totalAmount 永不进入收入计算（盈利分析/结算主链失效）。本钩子在 save/update 时联动
 * {@code amountFunctional = totalAmount × exchangeRate}（单币种 rate=1 时等于 totalAmount，
 * 对齐 mfg/inventory 派生金额 builder 回写范式）。
 */
@BizModel("ErpPrjBilling")
public class ErpPrjBillingBizModel extends AbstractErpCrudBizModel<ErpPrjBilling> implements IErpPrjBillingBiz{
    public ErpPrjBillingBizModel(){
        setEntityName(ErpPrjBilling.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpPrjBilling> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        syncAmountFunctional(entityData.getEntity());
    }

    @Override
    protected void defaultPrepareUpdate(EntityData<ErpPrjBilling> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        syncAmountFunctional(entityData.getEntity());
    }

    /** P1-CK-prj-002：amountFunctional = totalAmount × exchangeRate（rate null → 1）。 */
    protected void syncAmountFunctional(ErpPrjBilling billing) {
        if (billing == null) {
            return;
        }
        BigDecimal total = billing.getTotalAmount() != null ? billing.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal rate = billing.getExchangeRate() != null ? billing.getExchangeRate() : BigDecimal.ONE;
        billing.setAmountFunctional(total.multiply(rate));
        if (billing.getAmountSource() == null) {
            billing.setAmountSource(total);
        }
    }
}
