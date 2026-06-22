
package app.erp.mnt.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mnt.biz.IErpMntEquipmentBiz;
import app.erp.mnt.dao.entity.ErpMntEquipment;

@BizModel("ErpMntEquipment")
public class ErpMntEquipmentBizModel extends CrudBizModel<ErpMntEquipment> implements IErpMntEquipmentBiz{
    public ErpMntEquipmentBizModel(){
        setEntityName(ErpMntEquipment.class.getName());
    }
}
