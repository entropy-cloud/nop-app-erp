
package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.fin.biz.IErpFinBudgetCarryForwardLogBiz;
import app.erp.fin.dao.entity.ErpFinBudgetCarryForwardLog;

@BizModel("ErpFinBudgetCarryForwardLog")
public class ErpFinBudgetCarryForwardLogBizModel extends CrudBizModel<ErpFinBudgetCarryForwardLog> implements IErpFinBudgetCarryForwardLogBiz{
    public ErpFinBudgetCarryForwardLogBizModel(){
        setEntityName(ErpFinBudgetCarryForwardLog.class.getName());
    }
}
