package app.erp.mnt.service.entity;

import app.erp.mnt.biz.IErpMntEquipmentBiz;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

@BizModel("ErpMntEquipment")
public class ErpMntEquipmentBizModel extends CrudBizModel<ErpMntEquipment> implements IErpMntEquipmentBiz {
    public ErpMntEquipmentBizModel() {
        setEntityName(ErpMntEquipment.class.getName());
    }

    @Override
    @BizMutation
    public ErpMntEquipment changeStatus(@Name("equipmentId") Long equipmentId,
                                        @Name("newStatus") Integer newStatus,
                                        IServiceContext context) {
        ErpMntEquipment equipment = requireEntity(String.valueOf(equipmentId), null, context);
        equipment.setStatus(newStatus);
        updateEntity(equipment, null, context);
        return equipment;
    }
}
