
package app.erp.sal.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.sal.biz.IErpSalInvoiceLineBiz;
import app.erp.sal.dao.entity.ErpSalInvoiceLine;

import java.util.List;

@BizModel("ErpSalInvoiceLine")
public class ErpSalInvoiceLineBizModel extends AbstractErpCrudBizModel<ErpSalInvoiceLine> implements IErpSalInvoiceLineBiz{
    public ErpSalInvoiceLineBizModel(){
        setEntityName(ErpSalInvoiceLine.class.getName());
    }

}
