
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgSubcontractOrderBiz;
import app.erp.mfg.dao.entity.ErpMfgSubcontractOrder;

@BizModel("ErpMfgSubcontractOrder")
public class ErpMfgSubcontractOrderBizModel extends CrudBizModel<ErpMfgSubcontractOrder> implements IErpMfgSubcontractOrderBiz{
    public ErpMfgSubcontractOrderBizModel(){
        setEntityName(ErpMfgSubcontractOrder.class.getName());
    }
}
