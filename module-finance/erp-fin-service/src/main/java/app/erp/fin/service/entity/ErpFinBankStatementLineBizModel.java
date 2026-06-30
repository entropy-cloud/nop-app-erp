
package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.fin.biz.IErpFinBankStatementLineBiz;
import app.erp.fin.dao.entity.ErpFinBankStatementLine;

@BizModel("ErpFinBankStatementLine")
public class ErpFinBankStatementLineBizModel extends CrudBizModel<ErpFinBankStatementLine> implements IErpFinBankStatementLineBiz{
    public ErpFinBankStatementLineBizModel(){
        setEntityName(ErpFinBankStatementLine.class.getName());
    }
}
