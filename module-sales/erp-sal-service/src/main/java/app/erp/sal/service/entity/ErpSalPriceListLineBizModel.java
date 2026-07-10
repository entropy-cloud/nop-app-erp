
package app.erp.sal.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.sal.biz.IErpSalPriceListLineBiz;
import app.erp.sal.dao.entity.ErpSalPriceListLine;

@BizModel("ErpSalPriceListLine")
public class ErpSalPriceListLineBizModel extends CrudBizModel<ErpSalPriceListLine> implements IErpSalPriceListLineBiz{
    public ErpSalPriceListLineBizModel(){
        setEntityName(ErpSalPriceListLine.class.getName());
    }
}
