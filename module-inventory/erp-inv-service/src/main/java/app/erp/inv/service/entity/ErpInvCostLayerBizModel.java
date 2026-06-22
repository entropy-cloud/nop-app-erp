
package app.erp.inv.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.inv.biz.IErpInvCostLayerBiz;
import app.erp.inv.dao.entity.ErpInvCostLayer;

@BizModel("ErpInvCostLayer")
public class ErpInvCostLayerBizModel extends CrudBizModel<ErpInvCostLayer> implements IErpInvCostLayerBiz{
    public ErpInvCostLayerBizModel(){
        setEntityName(ErpInvCostLayer.class.getName());
    }
}
