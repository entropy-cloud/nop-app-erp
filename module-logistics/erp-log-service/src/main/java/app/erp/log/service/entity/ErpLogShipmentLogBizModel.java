
package app.erp.log.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.log.biz.IErpLogShipmentLogBiz;
import app.erp.log.dao.entity.ErpLogShipmentLog;

@BizModel("ErpLogShipmentLog")
public class ErpLogShipmentLogBizModel extends CrudBizModel<ErpLogShipmentLog> implements IErpLogShipmentLogBiz{
    public ErpLogShipmentLogBizModel(){
        setEntityName(ErpLogShipmentLog.class.getName());
    }
}
