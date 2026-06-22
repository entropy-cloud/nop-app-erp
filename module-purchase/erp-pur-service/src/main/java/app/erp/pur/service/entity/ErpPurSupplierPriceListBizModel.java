
package app.erp.pur.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.pur.biz.IErpPurSupplierPriceListBiz;
import app.erp.pur.dao.entity.ErpPurSupplierPriceList;

@BizModel("ErpPurSupplierPriceList")
public class ErpPurSupplierPriceListBizModel extends CrudBizModel<ErpPurSupplierPriceList> implements IErpPurSupplierPriceListBiz{
    public ErpPurSupplierPriceListBizModel(){
        setEntityName(ErpPurSupplierPriceList.class.getName());
    }
}
