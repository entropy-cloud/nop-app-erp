package app.erp.mnt.service.entity;

import app.erp.mnt.biz.IErpMntVisitBiz;
import app.erp.mnt.dao.entity.ErpMntRequest;
import app.erp.mnt.dao.entity.ErpMntVisit;
import app.erp.mnt.service.processor.ErpMntVisitCancelProcessor;
import app.erp.mnt.service.processor.ErpMntVisitCompleteProcessor;
import app.erp.mnt.service.processor.ErpMntVisitReportAdditionalFaultProcessor;
import app.erp.mnt.service.processor.ErpMntVisitScheduleProcessor;
import app.erp.mnt.service.processor.ErpMntVisitStartProcessor;
import app.erp.mnt.service.support.DecommissionedEquipmentGuard;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

@BizModel("ErpMntVisit")
public class ErpMntVisitBizModel extends CrudBizModel<ErpMntVisit> implements IErpMntVisitBiz {

    @Inject
    ErpMntVisitScheduleProcessor scheduleProcessor;
    @Inject
    ErpMntVisitStartProcessor startProcessor;
    @Inject
    ErpMntVisitCompleteProcessor completeProcessor;
    @Inject
    ErpMntVisitCancelProcessor cancelProcessor;
    @Inject
    ErpMntVisitReportAdditionalFaultProcessor reportAdditionalFaultProcessor;

    // RC-R1.77 / UC-MAIN-08：DECOMMISSIONED 设备不可被新维护工单（visit，矩阵 :312 MNT_VISIT 词条）引用。
    // save 钩子对内部保存路径（到期生成/报修受理）同样触发——批量路径经 ScheduleDueGenerator 查询侧
    // 排除豁免（防一条违规计划中断整批日批 job），手工 accept 路径经 Processor 显式守卫报可理解错误。
    @Inject
    DecommissionedEquipmentGuard decommissionedGuard;

    public ErpMntVisitBizModel() {
        setEntityName(ErpMntVisit.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpMntVisit> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        decommissionedGuard.rejectIfDecommissioned(entityData.getEntity().getEquipmentId(), context);
    }

    @Override
    protected void defaultPrepareUpdate(EntityData<ErpMntVisit> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        ErpMntVisit entity = entityData.getEntity();
        Object oldEquipmentId = entity.orm_dirtyOldValues().get("equipmentId");
        if (oldEquipmentId != null && !oldEquipmentId.equals(entity.getEquipmentId())) {
            decommissionedGuard.rejectIfDecommissioned(entity.getEquipmentId(), context);
        }
    }

    @Override
    @BizMutation
    public ErpMntVisit schedule(@Name("visitId") Long visitId, IServiceContext context) {
        return scheduleProcessor.schedule(visitId, context);
    }

    @Override
    @BizMutation
    public ErpMntVisit start(@Name("visitId") Long visitId, IServiceContext context) {
        return startProcessor.start(visitId, context);
    }

    @Override
    @BizMutation
    public ErpMntVisit complete(@Name("visitId") Long visitId, IServiceContext context) {
        return completeProcessor.complete(visitId, context);
    }

    @Override
    @BizMutation
    public ErpMntVisit cancel(@Name("visitId") Long visitId, IServiceContext context) {
        return cancelProcessor.cancel(visitId, context);
    }

    @Override
    @BizMutation
    public ErpMntRequest reportAdditionalFault(@Name("visitId") Long visitId,
                                               @Name("description") String description,
                                               @Name("priority") @Optional String priority,
                                               @Name("remark") @Optional String remark,
                                               IServiceContext context) {
        return reportAdditionalFaultProcessor.reportAdditionalFault(visitId, description, priority, remark, context);
    }
}
