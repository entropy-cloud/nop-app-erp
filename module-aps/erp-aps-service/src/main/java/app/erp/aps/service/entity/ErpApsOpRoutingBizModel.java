
package app.erp.aps.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.aps.biz.IErpApsOpRoutingBiz;
import app.erp.aps.dao.entity.ErpApsOpRouting;

@BizModel("ErpApsOpRouting")
public class ErpApsOpRoutingBizModel extends CrudBizModel<ErpApsOpRouting> implements IErpApsOpRoutingBiz{
    public ErpApsOpRoutingBizModel(){
        setEntityName(ErpApsOpRouting.class.getName());
    }

}
