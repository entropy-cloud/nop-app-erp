
package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.fin.biz.IErpFinReconciliationBiz;
import app.erp.fin.dao.entity.ErpFinReconciliation;

@BizModel("ErpFinReconciliation")
public class ErpFinReconciliationBizModel extends CrudBizModel<ErpFinReconciliation> implements IErpFinReconciliationBiz{
    public ErpFinReconciliationBizModel(){
        setEntityName(ErpFinReconciliation.class.getName());
    }
}
