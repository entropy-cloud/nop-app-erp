
package app.erp.sal.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.sal.biz.IErpSalReturnBiz;
import app.erp.sal.dao.entity.ErpSalReturn;

@BizModel("ErpSalReturn")
public class ErpSalReturnBizModel extends CrudBizModel<ErpSalReturn> implements IErpSalReturnBiz{
    public ErpSalReturnBizModel(){
        setEntityName(ErpSalReturn.class.getName());
    }
}
