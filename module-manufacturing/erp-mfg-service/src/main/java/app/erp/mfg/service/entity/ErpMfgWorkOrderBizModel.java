
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgWorkOrderBiz;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;

@BizModel("ErpMfgWorkOrder")
public class ErpMfgWorkOrderBizModel extends CrudBizModel<ErpMfgWorkOrder> implements IErpMfgWorkOrderBiz{
    public ErpMfgWorkOrderBizModel(){
        setEntityName(ErpMfgWorkOrder.class.getName());
    }
}
