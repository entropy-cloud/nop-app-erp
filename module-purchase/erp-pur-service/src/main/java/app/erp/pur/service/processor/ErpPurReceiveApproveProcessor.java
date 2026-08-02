package app.erp.pur.service.processor;

import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.common.service.AbstractApproveProcessor;
import app.erp.common.service.SoDGuard;
import io.nop.api.core.exceptions.ErrorCode;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpPurReceive approve per-mutation Processor (plan 2026-07-25-1057-2).
 * Overrides the public approve method to replicate the facade flow (stock move + posting + rollup),
 * calling facade helper methods for each step. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpPurReceiveApproveProcessor extends AbstractApproveProcessor<ErpPurReceive> {

    @Inject
    ErpPurReceiveProcessor processor;

    @Override
    public ErpPurReceive approve(String id, IServiceContext context) {
        ErpPurReceive receive = requireEntity(id);
        if (isApproved(receive)) {
            return receive;
        }
        SoDGuard.assertApproverNotCreator(getCreatedBy(receive), currentUserId(), sodErrorCode());
        processor.validateNotCancelled(receive, context);
        validateTransitionForApprove(receive, context);
        processor.validateBusinessRulesForApprove(receive, context);
        processor.enforceInspectionGate(receive, context);

        ErpInvStockMove move = processor.triggerIncomingMove(receive, context);
        receive = dao().getEntityById(id);

        setApproveStatus(receive, approvedStatus());
        setApprovedBy(receive, currentUserId());
        setApprovedAt(receive, now());
        processor.applyPostingResult(receive, move);
        receive.setReceiveStatus(ErpPurConstants.RECEIVE_STATUS_RECEIVED);
        dao().updateEntity(receive);

        processor.postProcessApprove(receive, context);
        return receive;
    }

    @Override
    protected IEntityDao<ErpPurReceive> dao() {
        return daoProvider.daoFor(ErpPurReceive.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpPurErrors.ERR_RECEIVE_NOT_FOUND)
                .param(ErpPurErrors.ARG_RECEIVE_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpPurReceive entity, String current, String... expected) {
        return new NopException(ErpPurErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_RECEIVE_CODE, entity.getCode())
                .param(ErpPurErrors.ARG_CURRENT_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected void validateNotCancelled(ErpPurReceive entity, IServiceContext context) {
        processor.validateNotCancelled(entity, context);
    }

    @Override
    protected String getApproveStatus(ErpPurReceive entity) {
        String status = entity.getApproveStatus();
        return status == null ? ErpPurConstants.APPROVE_STATUS_UNSUBMITTED : status;
    }

    @Override
    protected void setApproveStatus(ErpPurReceive entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpPurReceive entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpPurReceive entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isApproved(ErpPurReceive entity) {
        return entity.isApproved();
    }

    @Override
    protected boolean isCancelled(ErpPurReceive entity) {
        return entity.isCancelled();
    }

    @Override
    protected String submittedStatus() {
        return ErpPurConstants.APPROVE_STATUS_SUBMITTED;
    }

    @Override
    protected String approvedStatus() {
        return ErpPurConstants.APPROVE_STATUS_APPROVED;
    }

    @Override
    protected ErrorCode sodErrorCode() {
        return ErpPurErrors.ERR_PUR_APPROVER_IS_CREATOR;
    }
}
