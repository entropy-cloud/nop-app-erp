
package app.erp.pur.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.pur.biz.IErpPurInvoiceLineBiz;
import app.erp.pur.dao.entity.ErpPurInvoiceLine;

import java.util.List;

@BizModel("ErpPurInvoiceLine")
public class ErpPurInvoiceLineBizModel extends CrudBizModel<ErpPurInvoiceLine> implements IErpPurInvoiceLineBiz{
    public ErpPurInvoiceLineBizModel(){
        setEntityName(ErpPurInvoiceLine.class.getName());
    }

}
