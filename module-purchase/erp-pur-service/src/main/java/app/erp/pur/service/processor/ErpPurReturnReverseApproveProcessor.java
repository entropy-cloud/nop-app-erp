package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurReturn;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.pur.service.statemachine.ErpPurReturnApprovalStateMachine;
import app.erp.common.service.AbstractReverseApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpPurReturn reverseApprove per-mutation Processor (plan 2026-07-25-1057-2；审批轴 Bean 接线 plan 2026-08-13-1950-1 M4.20)。
 *
 * <p>整体覆写 public reverseApprove 方法以编排业财过账副作用逆转（stock move reversal + commitment restore）；
 * 固定来源态/目标态判断委托 {@link ErpPurReturnApprovalStateMachine}（approveStatus 审批轴 Bean，契约 §4/§7）；
 * 动态副作用（ensureReversed/runCommitmentRestoreOnReturnReverseHook）保留原位。
 *
 * <p>reverseApprove 目标态=REJECTED（据实保持 Return 当前行为，已合规 §16.4；Bean 据实编码）。
 */
public class ErpPurReturnReverseApproveProcessor extends AbstractReverseApproveProcessor<ErpPurReturn> {

    @Inject
    ErpPurReturnProcessor processor;

    @Inject
    ErpPurReturnApprovalStateMachine stateMachine;

    @Override
    public ErpPurReturn reverseApprove(String id, IServiceContext context) {
        ErpPurReturn returnOrder = requireEntity(id);
        if (isRejected(returnOrder)) {
            return returnOrder;
        }
        validateTransitionForReverseApprove(returnOrder, context);
        processor.ensureReversed(returnOrder, context);
        returnOrder = dao().getEntityById(id);

        returnOrder.setApproveStatus(stateMachine.reverseApproveTargetStatus());
        returnOrder.setApprovedBy(null);
        returnOrder.setApprovedAt(null);
        dao().updateEntity(returnOrder);
        processor.runCommitmentRestoreOnReturnReverseHook(returnOrder, true, context);
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
    protected void validateTransitionForReverseApprove(ErpPurReturn entity, IServiceContext context) {
        try {
            stateMachine.assertCanReverseApprove(getApproveStatus(entity));
        } catch (NopException e) {
            throw illegalStatusException(entity, getApproveStatus(entity), ErpPurConstants.APPROVE_STATUS_APPROVED);
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
    protected boolean isRejected(ErpPurReturn entity) {
        return entity.isRejected();
    }

    @Override
    protected String approvedStatus() {
        return ErpPurConstants.APPROVE_STATUS_APPROVED;
    }

    @Override
    protected String submittedStatus() {
        return ErpPurConstants.APPROVE_STATUS_SUBMITTED;
    }
}
