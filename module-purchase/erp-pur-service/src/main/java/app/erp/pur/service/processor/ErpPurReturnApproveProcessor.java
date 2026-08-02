package app.erp.pur.service.processor;

import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.pur.dao.entity.ErpPurReturn;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.pur.service.posting.PurReturnPostingDispatcher;
import app.erp.common.service.AbstractApproveProcessor;
import app.erp.common.service.SoDGuard;
import io.nop.api.core.exceptions.ErrorCode;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

/**
 * ErpPurReturn approve per-mutation Processor (plan 2026-07-25-1057-2).
 * Overrides the public approve method to replicate the facade flow (outgoing move + flush + posting + commitment release),
 * calling facade helper methods for each step. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpPurReturnApproveProcessor extends AbstractApproveProcessor<ErpPurReturn> {

    @Inject
    ErpPurReturnProcessor processor;

    @Inject
    IOrmTemplate ormTemplate;

    @Inject
    PurReturnPostingDispatcher postingDispatcher;

    @Override
    public ErpPurReturn approve(String id, IServiceContext context) {
        ErpPurReturn returnOrder = requireEntity(id);
        if (isApproved(returnOrder)) {
            return returnOrder;
        }
        SoDGuard.assertApproverNotCreator(getCreatedBy(returnOrder), currentUserId(), sodErrorCode());
        processor.validateNotCancelled(returnOrder, context);
        validateTransitionForApprove(returnOrder, context);
        processor.validateBusinessRulesForApprove(returnOrder, context);

        ErpInvStockMove move = processor.triggerOutgoingMove(returnOrder, context);
        ormTemplate.flushSession();
        boolean posted = postingDispatcher.tryPost(returnOrder);

        returnOrder = dao().getEntityById(id);

        setApproveStatus(returnOrder, approvedStatus());
        setApprovedBy(returnOrder, currentUserId());
        setApprovedAt(returnOrder, now());
        if (posted) {
            returnOrder.setPosted(true);
            returnOrder.setPostedAt(now());
            returnOrder.setPostedBy(currentUserId());
        }
        dao().updateEntity(returnOrder);

        processor.runCommitmentReleaseOnReturnHook(returnOrder, context);
        return returnOrder;
    }

    @Override
    protected IEntityDao<ErpPurReturn> dao() {
        return daoProvider.daoFor(ErpPurReturn.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpPurErrors.ERR_RETURN_NOT_FOUND)
                .param(ErpPurErrors.ARG_RETURN_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpPurReturn entity, String current, String... expected) {
        return new NopException(ErpPurErrors.ERR_RETURN_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_RETURN_CODE, entity.getCode())
                .param(ErpPurErrors.ARG_CURRENT_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected void validateNotCancelled(ErpPurReturn entity, IServiceContext context) {
        processor.validateNotCancelled(entity, context);
    }

    @Override
    protected String getApproveStatus(ErpPurReturn entity) {
        String status = entity.getApproveStatus();
        return status == null ? ErpPurConstants.APPROVE_STATUS_UNSUBMITTED : status;
    }

    @Override
    protected void setApproveStatus(ErpPurReturn entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpPurReturn entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpPurReturn entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isApproved(ErpPurReturn entity) {
        return entity.isApproved();
    }

    @Override
    protected boolean isCancelled(ErpPurReturn entity) {
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
