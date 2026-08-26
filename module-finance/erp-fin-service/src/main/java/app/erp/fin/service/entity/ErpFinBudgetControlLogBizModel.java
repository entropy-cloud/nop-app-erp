
package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.fin.biz.IErpFinBudgetControlLogBiz;
import app.erp.fin.dao.entity.ErpFinBudgetControlLog;

@BizModel("ErpFinBudgetControlLog")
public class ErpFinBudgetControlLogBizModel extends AbstractErpCrudBizModel<ErpFinBudgetControlLog> implements IErpFinBudgetControlLogBiz{
    public ErpFinBudgetControlLogBizModel(){
        setEntityName(ErpFinBudgetControlLog.class.getName());
    }
}
