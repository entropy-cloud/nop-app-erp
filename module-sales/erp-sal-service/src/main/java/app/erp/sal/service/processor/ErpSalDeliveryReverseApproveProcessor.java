package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.common.service.AbstractReverseApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpSalDelivery reverseApprove per-mutation Processor (plan 2026-07-30-1433-2 R5.2).
 * reverseApprove 冲销出库移动单（facade ensureReversed）后 reload 设 REJECTED + 清空审计字段，需 custom public override
 * （ensureReversed 后实体引用变更，不能走抽象骨架 doReverseApprove）。
 */
public class ErpSalDeliveryReverseApproveProcessor extends AbstractReverseApproveProcessor<ErpSalDelivery> {

    @Inject
    ErpSalDeliveryProcessor processor;

    @Override
    public ErpSalDelivery reverseApprove(String id, IServiceContext context) {
        ErpSalDelivery delivery = requireEntity(id);
        if (isRejected(delivery)) {
            return delivery;
        }
        validateTransitionForReverseApprove(delivery, context);
        processor.ensureReversed(delivery, context);
        delivery = dao().getEntityById(id);
        setApproveStatus(delivery, ErpSalConstants.APPROVE_STATUS_REJECTED);
        setApprovedBy(delivery, null);
        setApprovedAt(delivery, null);
        dao().updateEntity(delivery);
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
    protected boolean isRejected(ErpSalDelivery entity) {
        return entity.isRejected();
    }

    @Override
    protected String approvedStatus() {
        return ErpSalConstants.APPROVE_STATUS_APPROVED;
    }

    @Override
    protected String submittedStatus() {
        return ErpSalConstants.APPROVE_STATUS_SUBMITTED;
    }
}
