
package app.erp.pur.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.pur.biz.IErpPurInvoiceBiz;
import app.erp.pur.dao.entity.ErpPurInvoice;

@BizModel("ErpPurInvoice")
public class ErpPurInvoiceBizModel extends CrudBizModel<ErpPurInvoice> implements IErpPurInvoiceBiz{
    public ErpPurInvoiceBizModel(){
        setEntityName(ErpPurInvoice.class.getName());
    }
}
