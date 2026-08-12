package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurRequisition;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.pur.service.statemachine.ErpPurRequisitionApprovalStateMachine;
import app.erp.common.service.AbstractRejectProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpPurRequisition reject per-mutation Processor (plan 2026-07-25-1057-2；审批轴 Bean 接线 plan 2026-08-13-0945-1 M3.4)。
 *
 * <p>运行 {@link AbstractRejectProcessor} 骨架；固定来源态/目标态判断委托
 * {@link ErpPurRequisitionApprovalStateMachine}（approveStatus 审批轴 Bean，契约 §4/§7）。
 */
public class ErpPurRequisitionRejectProcessor extends AbstractRejectProcessor<ErpPurRequisition> {

    @Inject
    ErpPurRequisitionProcessor processor;

    @Inject
    ErpPurRequisitionApprovalStateMachine stateMachine;

    @Override
    protected IEntityDao<ErpPurRequisition> dao() {
        return daoProvider.daoFor(ErpPurRequisition.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpPurErrors.ERR_REQ_NOT_FOUND)
                .param(ErpPurErrors.ARG_REQUISITION_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpPurRequisition entity, String current, String... expected) {
        return new NopException(ErpPurErrors.ERR_REQ_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_REQUISITION_CODE, entity.getCode())
                .param(ErpPurErrors.ARG_CURRENT_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected void validateNotCancelled(ErpPurRequisition entity, IServiceContext context) {
        processor.validateNotCancelled(entity, context);
    }

    @Override
    protected void validateTransitionForReject(ErpPurRequisition entity, IServiceContext context) {
        try {
            stateMachine.assertCanReject(getApproveStatus(entity));
        } catch (NopException e) {
            throw illegalStatusException(entity, getApproveStatus(entity), ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    @Override
    protected String getApproveStatus(ErpPurRequisition entity) {
        String status = entity.getApproveStatus();
        return status == null ? ErpPurConstants.APPROVE_STATUS_UNSUBMITTED : status;
    }

    @Override
    protected void setApproveStatus(ErpPurRequisition entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpPurRequisition entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpPurRequisition entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isRejected(ErpPurRequisition entity) {
        return entity.isRejected();
    }

    @Override
    protected boolean isCancelled(ErpPurRequisition entity) {
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
    protected void doReject(ErpPurRequisition entity, IServiceContext context) {
        setApproveStatus(entity, rejectedStatus());
    }
}
