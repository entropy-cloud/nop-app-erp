
package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.fin.biz.IErpFinBudgetRollforwardLogBiz;
import app.erp.fin.dao.entity.ErpFinBudgetRollforwardLog;

@BizModel("ErpFinBudgetRollforwardLog")
public class ErpFinBudgetRollforwardLogBizModel extends AbstractErpCrudBizModel<ErpFinBudgetRollforwardLog> implements IErpFinBudgetRollforwardLogBiz{
    public ErpFinBudgetRollforwardLogBizModel(){
        setEntityName(ErpFinBudgetRollforwardLog.class.getName());
    }
}
