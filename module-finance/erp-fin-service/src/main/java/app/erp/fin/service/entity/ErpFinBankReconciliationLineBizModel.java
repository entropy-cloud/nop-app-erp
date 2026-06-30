
package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.fin.biz.IErpFinBankReconciliationLineBiz;
import app.erp.fin.dao.entity.ErpFinBankReconciliationLine;

@BizModel("ErpFinBankReconciliationLine")
public class ErpFinBankReconciliationLineBizModel extends CrudBizModel<ErpFinBankReconciliationLine> implements IErpFinBankReconciliationLineBiz{
    public ErpFinBankReconciliationLineBizModel(){
        setEntityName(ErpFinBankReconciliationLine.class.getName());
    }
}
