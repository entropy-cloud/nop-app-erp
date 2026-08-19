
package app.erp.mnt.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mnt.biz.IErpMntEquipmentStatusLogBiz;
import app.erp.mnt.dao.entity.ErpMntEquipmentStatusLog;

@BizModel("ErpMntEquipmentStatusLog")
public class ErpMntEquipmentStatusLogBizModel extends CrudBizModel<ErpMntEquipmentStatusLog> implements IErpMntEquipmentStatusLogBiz{
    public ErpMntEquipmentStatusLogBizModel(){
        setEntityName(ErpMntEquipmentStatusLog.class.getName());
    }
}
