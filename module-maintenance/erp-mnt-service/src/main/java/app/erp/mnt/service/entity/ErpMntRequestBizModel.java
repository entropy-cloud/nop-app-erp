package app.erp.mnt.service.entity;

import app.erp.mnt.biz.IErpMntRequestBiz;
import app.erp.mnt.dao.entity.ErpMntRequest;
import app.erp.mnt.service.processor.ErpMntRequestAcceptProcessor;
import app.erp.mnt.service.processor.ErpMntRequestCancelProcessor;
import app.erp.mnt.service.processor.ErpMntRequestCompleteProcessor;
import app.erp.mnt.service.processor.ErpMntRequestRejectRequestProcessor;
import app.erp.mnt.service.processor.ErpMntRequestStartRepairProcessor;
import app.erp.mnt.service.support.DecommissionedEquipmentGuard;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

@BizModel("ErpMntRequest")
public class ErpMntRequestBizModel extends CrudBizModel<ErpMntRequest> implements IErpMntRequestBiz {

    @Inject
    ErpMntRequestAcceptProcessor acceptProcessor;
    @Inject
    ErpMntRequestStartRepairProcessor startRepairProcessor;
    @Inject
    ErpMntRequestCompleteProcessor completeProcessor;
    @Inject
    ErpMntRequestRejectRequestProcessor rejectRequestProcessor;
    @Inject
    ErpMntRequestCancelProcessor cancelProcessor;

    // RC-R1.77 / UC-MAIN-08：DECOMMISSIONED 设备不可被新报修请求引用（save 钩子守卫；
    // 存量 OPEN 请求的 accept 拒绝在 ErpMntRequestAcceptProcessor 显式守卫）。
    @Inject
    DecommissionedEquipmentGuard decommissionedGuard;

    public ErpMntRequestBizModel() {
        setEntityName(ErpMntRequest.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpMntRequest> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        decommissionedGuard.rejectIfDecommissioned(entityData.getEntity().getEquipmentId(), context);
    }

    @Override
    protected void defaultPrepareUpdate(EntityData<ErpMntRequest> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        ErpMntRequest entity = entityData.getEntity();
        Object oldEquipmentId = entity.orm_dirtyOldValues().get("equipmentId");
        if (oldEquipmentId != null && !oldEquipmentId.equals(entity.getEquipmentId())) {
            decommissionedGuard.rejectIfDecommissioned(entity.getEquipmentId(), context);
        }
    }

    @Override
    @BizMutation
    public ErpMntRequest accept(@Name("requestId") Long requestId, IServiceContext context) {
        return acceptProcessor.accept(requestId, context);
    }

    @Override
    @BizMutation
    public ErpMntRequest startRepair(@Name("requestId") Long requestId, IServiceContext context) {
        return startRepairProcessor.startRepair(requestId, context);
    }

    @Override
    @BizMutation
    public ErpMntRequest complete(@Name("requestId") Long requestId, IServiceContext context) {
        return completeProcessor.complete(requestId, context);
    }

    @Override
    @BizMutation
    public ErpMntRequest rejectRequest(@Name("requestId") Long requestId, IServiceContext context) {
        return rejectRequestProcessor.rejectRequest(requestId, context);
    }

    @Override
    @BizMutation
    public ErpMntRequest cancel(@Name("requestId") Long requestId, IServiceContext context) {
        return cancelProcessor.cancel(requestId, context);
    }
}
