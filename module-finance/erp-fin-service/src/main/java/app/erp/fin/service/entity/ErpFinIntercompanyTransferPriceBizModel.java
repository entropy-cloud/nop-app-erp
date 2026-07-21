
package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.fin.biz.IErpFinIntercompanyTransferPriceBiz;
import app.erp.fin.dao.entity.ErpFinIntercompanyTransferPrice;

@BizModel("ErpFinIntercompanyTransferPrice")
public class ErpFinIntercompanyTransferPriceBizModel extends CrudBizModel<ErpFinIntercompanyTransferPrice> implements IErpFinIntercompanyTransferPriceBiz{
    public ErpFinIntercompanyTransferPriceBizModel(){
        setEntityName(ErpFinIntercompanyTransferPrice.class.getName());
    }
}
