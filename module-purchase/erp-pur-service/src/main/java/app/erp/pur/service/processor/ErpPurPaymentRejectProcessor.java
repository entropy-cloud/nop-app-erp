package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurPayment;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.pur.service.statemachine.ErpPurPaymentApprovalStateMachine;
import app.erp.common.service.AbstractRejectProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpPurPayment reject per-mutation Processor（审批轴 Bean 接线 plan 2026-08-13-1950-1 M4.18，skeleton 路径）。
 *
 * <p>运行 {@link AbstractRejectProcessor} 骨架；固定来源态/目标态判断委托
 * {@link ErpPurPaymentApprovalStateMachine}（approveStatus 审批轴 Bean，契约 §4/§7）。
 */
public class ErpPurPaymentRejectProcessor extends AbstractRejectProcessor<ErpPurPayment> {

    @Inject
    ErpPurPaymentProcessor processor;

    @Inject
    ErpPurPaymentApprovalStateMachine stateMachine;

    @Override
    protected IEntityDao<ErpPurPayment> dao() {
        return daoProvider.daoFor(ErpPurPayment.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpPurErrors.ERR_PAYMENT_NOT_FOUND)
                .param(ErpPurErrors.ARG_PAYMENT_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpPurPayment entity, String current, String... expected) {
        return new NopException(ErpPurErrors.ERR_PAYMENT_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_PAYMENT_CODE, entity.getCode())
                .param(ErpPurErrors.ARG_CURRENT_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected void validateNotCancelled(ErpPurPayment entity, IServiceContext context) {
        processor.validateNotCancelled(entity, context);
    }

    @Override
    protected void validateTransitionForReject(ErpPurPayment entity, IServiceContext context) {
        try {
            stateMachine.assertCanReject(getApproveStatus(entity));
        } catch (NopException e) {
            throw illegalStatusException(entity, getApproveStatus(entity), ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    @Override
    protected String getApproveStatus(ErpPurPayment entity) {
        String status = entity.getApproveStatus();
        return status == null ? ErpPurConstants.APPROVE_STATUS_UNSUBMITTED : status;
    }

    @Override
    protected void setApproveStatus(ErpPurPayment entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpPurPayment entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpPurPayment entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isRejected(ErpPurPayment entity) {
        return entity.isRejected();
    }

    @Override
    protected boolean isCancelled(ErpPurPayment entity) {
        return entity.isCancelled();
    }

    @Override
    protected String submittedStatus() {
        return ErpPurConstants.APPROVE_STATUS_SUBMITTED;
    }

    @Override
    protected String rejectedStatus() {
        return stateMachine.rejectTargetStatus();
    }

    @Override
    protected void doReject(ErpPurPayment entity, IServiceContext context) {
        setApproveStatus(entity, rejectedStatus());
    }
}
