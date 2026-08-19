package app.erp.mnt.service.entity;

import app.erp.mnt.biz.IErpMntScheduleBiz;
import app.erp.mnt.dao.entity.ErpMntSchedule;
import app.erp.mnt.service.processor.ErpMntScheduleGenerateDueVisitsProcessor;
import app.erp.mnt.service.support.DecommissionedEquipmentGuard;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.time.LocalDate;

@BizModel("ErpMntSchedule")
public class ErpMntScheduleBizModel extends CrudBizModel<ErpMntSchedule> implements IErpMntScheduleBiz {

    @Inject
    ErpMntScheduleGenerateDueVisitsProcessor generateDueVisitsProcessor;

    // RC-R1.77 / UC-MAIN-08：DECOMMISSIONED 设备不可被新维护计划引用（save 钩子守卫，
    // 内部到期生成路径经 ScheduleDueGenerator 查询侧排除豁免）。
    @Inject
    DecommissionedEquipmentGuard decommissionedGuard;

    public ErpMntScheduleBizModel() {
        setEntityName(ErpMntSchedule.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpMntSchedule> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        decommissionedGuard.rejectIfDecommissioned(entityData.getEntity().getEquipmentId(), context);
    }

    @Override
    protected void defaultPrepareUpdate(EntityData<ErpMntSchedule> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        rejectIfEquipmentChanged(entityData, context);
    }

    private void rejectIfEquipmentChanged(EntityData<ErpMntSchedule> entityData, IServiceContext context) {
        ErpMntSchedule entity = entityData.getEntity();
        Object oldEquipmentId = entity.orm_dirtyOldValues().get("equipmentId");
        if (oldEquipmentId != null && !oldEquipmentId.equals(entity.getEquipmentId())) {
            decommissionedGuard.rejectIfDecommissioned(entity.getEquipmentId(), context);
        }
    }

    @Override
    @BizMutation
    public Integer generateDueVisits(@Name("asOfDate") LocalDate asOfDate, IServiceContext context) {
        return generateDueVisitsProcessor.generateDueVisits(asOfDate, context);
    }
}
