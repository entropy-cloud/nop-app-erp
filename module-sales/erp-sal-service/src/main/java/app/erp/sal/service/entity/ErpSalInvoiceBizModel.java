
package app.erp.sal.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.sal.biz.IErpSalInvoiceBiz;
import app.erp.sal.dao.entity.ErpSalInvoice;

@BizModel("ErpSalInvoice")
public class ErpSalInvoiceBizModel extends CrudBizModel<ErpSalInvoice> implements IErpSalInvoiceBiz{
    public ErpSalInvoiceBizModel(){
        setEntityName(ErpSalInvoice.class.getName());
    }
}
