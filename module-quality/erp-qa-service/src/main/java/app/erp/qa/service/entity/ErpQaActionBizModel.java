
package app.erp.qa.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.qa.biz.IErpQaActionBiz;
import app.erp.qa.dao.entity.ErpQaAction;

@BizModel("ErpQaAction")
public class ErpQaActionBizModel extends CrudBizModel<ErpQaAction> implements IErpQaActionBiz{
    public ErpQaActionBizModel(){
        setEntityName(ErpQaAction.class.getName());
    }
}
