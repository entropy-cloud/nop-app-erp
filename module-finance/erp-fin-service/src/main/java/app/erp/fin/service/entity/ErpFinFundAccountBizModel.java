
package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.fin.biz.IErpFinFundAccountBiz;
import app.erp.fin.dao.entity.ErpFinFundAccount;

import java.util.List;

@BizModel("ErpFinFundAccount")
public class ErpFinFundAccountBizModel extends CrudBizModel<ErpFinFundAccount> implements IErpFinFundAccountBiz{
    public ErpFinFundAccountBizModel(){
        setEntityName(ErpFinFundAccount.class.getName());
    }

}
