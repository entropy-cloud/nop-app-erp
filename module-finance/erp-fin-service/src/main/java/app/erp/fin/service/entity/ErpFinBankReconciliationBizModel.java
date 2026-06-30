
package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.fin.biz.IErpFinBankReconciliationBiz;
import app.erp.fin.dao.entity.ErpFinBankReconciliation;

@BizModel("ErpFinBankReconciliation")
public class ErpFinBankReconciliationBizModel extends CrudBizModel<ErpFinBankReconciliation> implements IErpFinBankReconciliationBiz{
    public ErpFinBankReconciliationBizModel(){
        setEntityName(ErpFinBankReconciliation.class.getName());
    }
}
