
package app.erp.qa.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.qa.biz.IErpQaRiskRegisterBiz;
import app.erp.qa.dao.entity.ErpQaRiskRegister;

@BizModel("ErpQaRiskRegister")
public class ErpQaRiskRegisterBizModel extends AbstractErpCrudBizModel<ErpQaRiskRegister> implements IErpQaRiskRegisterBiz{
    public ErpQaRiskRegisterBizModel(){
        setEntityName(ErpQaRiskRegister.class.getName());
    }
}
