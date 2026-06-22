
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.md.biz.IErpMdTaxRateBiz;
import app.erp.md.dao.entity.ErpMdTaxRate;

@BizModel("ErpMdTaxRate")
public class ErpMdTaxRateBizModel extends CrudBizModel<ErpMdTaxRate> implements IErpMdTaxRateBiz{
    public ErpMdTaxRateBizModel(){
        setEntityName(ErpMdTaxRate.class.getName());
    }
}
