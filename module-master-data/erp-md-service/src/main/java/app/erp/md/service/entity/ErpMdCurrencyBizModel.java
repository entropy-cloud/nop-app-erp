
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.md.biz.IErpMdCurrencyBiz;
import app.erp.md.dao.entity.ErpMdCurrency;

@BizModel("ErpMdCurrency")
public class ErpMdCurrencyBizModel extends CrudBizModel<ErpMdCurrency> implements IErpMdCurrencyBiz{
    public ErpMdCurrencyBizModel(){
        setEntityName(ErpMdCurrency.class.getName());
    }
}
