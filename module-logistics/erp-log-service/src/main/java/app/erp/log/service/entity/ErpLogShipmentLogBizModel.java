
package app.erp.log.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.log.biz.IErpLogShipmentLogBiz;
import app.erp.log.dao.entity.ErpLogShipmentLog;

@BizModel("ErpLogShipmentLog")
public class ErpLogShipmentLogBizModel extends AbstractErpCrudBizModel<ErpLogShipmentLog> implements IErpLogShipmentLogBiz{
    public ErpLogShipmentLogBizModel(){
        setEntityName(ErpLogShipmentLog.class.getName());
    }

}
