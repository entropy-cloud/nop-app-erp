
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.md.biz.IErpMdExchangeRateBiz;
import app.erp.md.dao.entity.ErpMdExchangeRate;

@BizModel("ErpMdExchangeRate")
public class ErpMdExchangeRateBizModel extends CrudBizModel<ErpMdExchangeRate> implements IErpMdExchangeRateBiz{
    public ErpMdExchangeRateBizModel(){
        setEntityName(ErpMdExchangeRate.class.getName());
    }
}
