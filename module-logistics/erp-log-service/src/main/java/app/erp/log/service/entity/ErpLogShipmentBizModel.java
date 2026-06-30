
package app.erp.log.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.log.biz.IErpLogShipmentBiz;
import app.erp.log.dao.entity.ErpLogShipment;

@BizModel("ErpLogShipment")
public class ErpLogShipmentBizModel extends CrudBizModel<ErpLogShipment> implements IErpLogShipmentBiz{
    public ErpLogShipmentBizModel(){
        setEntityName(ErpLogShipment.class.getName());
    }
}
