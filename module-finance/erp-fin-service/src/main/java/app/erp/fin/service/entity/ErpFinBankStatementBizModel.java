
package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.fin.biz.IErpFinBankStatementBiz;
import app.erp.fin.dao.entity.ErpFinBankStatement;

@BizModel("ErpFinBankStatement")
public class ErpFinBankStatementBizModel extends CrudBizModel<ErpFinBankStatement> implements IErpFinBankStatementBiz{
    public ErpFinBankStatementBizModel(){
        setEntityName(ErpFinBankStatement.class.getName());
    }
}
