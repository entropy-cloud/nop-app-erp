
package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.fin.biz.IErpFinTrialBalanceBiz;
import app.erp.fin.dao.entity.ErpFinTrialBalance;

@BizModel("ErpFinTrialBalance")
public class ErpFinTrialBalanceBizModel extends CrudBizModel<ErpFinTrialBalance> implements IErpFinTrialBalanceBiz{
    public ErpFinTrialBalanceBizModel(){
        setEntityName(ErpFinTrialBalance.class.getName());
    }
}
