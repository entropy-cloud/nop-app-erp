
package app.erp.sal.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.sal.biz.IErpSalReceiptBiz;
import app.erp.sal.dao.entity.ErpSalReceipt;

@BizModel("ErpSalReceipt")
public class ErpSalReceiptBizModel extends CrudBizModel<ErpSalReceipt> implements IErpSalReceiptBiz{
    public ErpSalReceiptBizModel(){
        setEntityName(ErpSalReceipt.class.getName());
    }
}
