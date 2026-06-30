
package app.erp.drp.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.drp.biz.IErpDrpParameterBiz;
import app.erp.drp.dao.entity.ErpDrpParameter;

@BizModel("ErpDrpParameter")
public class ErpDrpParameterBizModel extends CrudBizModel<ErpDrpParameter> implements IErpDrpParameterBiz{
    public ErpDrpParameterBizModel(){
        setEntityName(ErpDrpParameter.class.getName());
    }
}
