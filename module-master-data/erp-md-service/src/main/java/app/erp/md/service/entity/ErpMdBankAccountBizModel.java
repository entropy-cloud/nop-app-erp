
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.md.biz.IErpMdBankAccountBiz;
import app.erp.md.dao.entity.ErpMdBankAccount;

@BizModel("ErpMdBankAccount")
public class ErpMdBankAccountBizModel extends CrudBizModel<ErpMdBankAccount> implements IErpMdBankAccountBiz{
    public ErpMdBankAccountBizModel(){
        setEntityName(ErpMdBankAccount.class.getName());
    }
}
