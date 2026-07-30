package app.erp.sal.service.processor;

import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.common.service.AbstractApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpSalDelivery approve per-mutation Processor (plan 2026-07-30-1433-2 R5.2).
 * approve 触发出库移动单 + 过账 + 订单发货状态回写（facade doApprove 流程），需 custom public override
 * 调 facade 各 step helper（对齐 R5.1 ErpPurReceiveApproveProcessor 模式 B）。
 */
public class ErpSalDeliveryApproveProcessor extends AbstractApproveProcessor<ErpSalDelivery> {

    @Inject
    ErpSalDeliveryProcessor processor;

    @Override
    public ErpSalDelivery approve(String id, IServiceContext context) {
        ErpSalDelivery delivery = requireEntity(id);
        if (isApproved(delivery)) {
            return delivery;
        }
        processor.validateNotCancelled(delivery, context);
        validateTransitionForApprove(delivery, context);
        processor.validateBusinessRulesForApprove(delivery, context);
        processor.enforceInspectionGate(delivery, context);

        ErpInvStockMove move = processor.triggerOutgoingMove(delivery, context);
        processor.applyPostingResult(delivery, move);
        setApproveStatus(delivery, approvedStatus());
        setApprovedBy(delivery, currentUserId());
        setApprovedAt(delivery, now());
        dao().updateEntity(delivery);

        processor.postProcessApprove(delivery, context);
        return delivery;
    }

    @Override
    protected IEntityDao<ErpSalDelivery> dao() {
        return daoProvider.daoFor(ErpSalDelivery.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpSalErrors.ERR_DELIVERY_NOT_FOUND)
                .param(ErpSalErrors.ARG_DELIVERY_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpSalDelivery entity, String current, String... expected) {
        return new NopException(ErpSalErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_DELIVERY_CODE, entity.getCode())
                .param(ErpSalErrors.ARG_CURRENT_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected String getApproveStatus(ErpSalDelivery entity) {
        String status = entity.getApproveStatus();
        return status == null ? ErpSalConstants.APPROVE_STATUS_UNSUBMITTED : status;
    }

    @Override
    protected void setApproveStatus(ErpSalDelivery entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpSalDelivery entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpSalDelivery entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isApproved(ErpSalDelivery entity) {
        return entity.isApproved();
    }

    @Override
    protected boolean isCancelled(ErpSalDelivery entity) {
        return entity.isCancelled();
    }

    @Override
    protected String submittedStatus() {
        return ErpSalConstants.APPROVE_STATUS_SUBMITTED;
    }

    @Override
    protected String approvedStatus() {
        return ErpSalConstants.APPROVE_STATUS_APPROVED;
    }
}
