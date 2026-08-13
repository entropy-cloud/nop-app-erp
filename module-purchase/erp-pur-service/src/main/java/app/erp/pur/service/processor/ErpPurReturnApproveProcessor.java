package app.erp.pur.service.processor;

import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.pur.dao.entity.ErpPurReturn;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.pur.service.posting.PurReturnPostingDispatcher;
import app.erp.pur.service.statemachine.ErpPurReturnApprovalStateMachine;
import app.erp.common.service.AbstractApproveProcessor;
import app.erp.common.service.SoDGuard;
import io.nop.api.core.exceptions.ErrorCode;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

/**
 * ErpPurReturn approve per-mutation Processor (plan 2026-07-25-1057-2；审批轴 Bean 接线 plan 2026-08-13-1950-1 M4.20)。
 *
 * <p>整体覆写 public approve 方法以编排业财过账副作用（出库 stock move + flush + posting + commitment release）；
 * 固定来源态/目标态判断委托 {@link ErpPurReturnApprovalStateMachine}（approveStatus 审批轴 Bean，契约 §4/§7）；
 * 动态业务守卫/副作用（triggerOutgoingMove/runCommitmentReleaseOnReturnHook/PostingDispatcher/SoD）保留原位。
 * Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpPurReturnApproveProcessor extends AbstractApproveProcessor<ErpPurReturn> {

    @Inject
    ErpPurReturnProcessor processor;

    @Inject
    IOrmTemplate ormTemplate;

    @Inject
    PurReturnPostingDispatcher postingDispatcher;

    @Inject
    ErpPurReturnApprovalStateMachine stateMachine;

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
    protected void validateTransitionForApprove(ErpPurReturn entity, IServiceContext context) {
        try {
            stateMachine.assertCanApprove(getApproveStatus(entity));
        } catch (NopException e) {
            throw illegalStatusException(entity, getApproveStatus(entity), ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        }
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
        return stateMachine.approveTargetStatus();
    }

    @Override
    protected ErrorCode sodErrorCode() {
        return ErpPurErrors.ERR_PUR_APPROVER_IS_CREATOR;
    }
}
