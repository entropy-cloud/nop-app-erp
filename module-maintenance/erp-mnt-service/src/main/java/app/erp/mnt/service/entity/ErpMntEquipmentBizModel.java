package app.erp.mnt.service.entity;

import app.erp.mnt.biz.IErpMntEquipmentBiz;
import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.service.support.EquipmentStatusLogWriter;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

@BizModel("ErpMntEquipment")
public class ErpMntEquipmentBizModel extends CrudBizModel<ErpMntEquipment> implements IErpMntEquipmentBiz {

    @Inject
    EquipmentStatusLogWriter statusLogWriter;

    public ErpMntEquipmentBizModel() {
        setEntityName(ErpMntEquipment.class.getName());
    }

    @Override
    @BizMutation
    public ErpMntEquipment changeStatus(@Name("equipmentId") Long equipmentId,
                                        @Name("newStatus") String newStatus,
                                        IServiceContext context) {
        ErpMntEquipment equipment = requireEntity(String.valueOf(equipmentId), null, context);
        String fromStatus = equipment.getStatus();
        equipment.setStatus(newStatus);
        updateEntity(equipment, null, context);
        // RC-R1.73 / UC-MAIN-02：手动状态变更同事务追加状态日志行（来源 MANUAL），
        // 供运行时长 Σ RUNNING 段聚合消费。
        statusLogWriter.append(equipmentId, fromStatus, newStatus,
                ErpMntDaoConstants.STATUS_LOG_SOURCE_MANUAL, null);
        return equipment;
    }
}
