
package app.erp.md.service.entity;

import app.erp.md.biz.IErpMdCurrencyBiz;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdExchangeRate;
import app.erp.md.service.processor.ErpMdCurrencyRefreshRatesFromApiProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.List;

@BizModel("ErpMdCurrency")
public class ErpMdCurrencyBizModel extends CrudBizModel<ErpMdCurrency> implements IErpMdCurrencyBiz {

    @Inject
    ErpMdCurrencyRefreshRatesFromApiProcessor refreshRatesFromApiProcessor;

    public ErpMdCurrencyBizModel() {
        setEntityName(ErpMdCurrency.class.getName());
    }

    @Override
    @BizMutation
    public List<ErpMdExchangeRate> refreshRatesFromApi(@Name("baseCurrency") String baseCurrency,
                                                       IServiceContext context) {
        return refreshRatesFromApiProcessor.refreshRatesFromApi(baseCurrency, context);
    }
}
