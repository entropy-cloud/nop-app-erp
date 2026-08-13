package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.pur.service.statemachine.ErpPurReceiveApprovalStateMachine;
import app.erp.common.service.AbstractReverseApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpPurReceive reverseApprove per-mutation Processor (plan 2026-07-25-1057-2；审批轴 Bean 接线 plan 2026-08-13-1950-1 M4.14)。
 *
 * <p>整体覆写 public reverseApprove 方法以编排业财过账副作用逆转（stock move reversal + posted 回写）；
 * 固定来源态/目标态判断委托 {@link ErpPurReceiveApprovalStateMachine}（approveStatus 审批轴 Bean，契约 §4/§7）；
 * 动态副作用（ensureReversed）保留原位。
 *
 * <p>reverseApprove 目标态=REJECTED（据实保持 Receive 当前行为，已合规 §16.4；Bean 据实编码）。
 */
public class ErpPurReceiveReverseApproveProcessor extends AbstractReverseApproveProcessor<ErpPurReceive> {

    @Inject
    ErpPurReceiveProcessor processor;

    @Inject
    ErpPurReceiveApprovalStateMachine stateMachine;

    @Override
    public ErpPurReceive reverseApprove(String id, IServiceContext context) {
        ErpPurReceive receive = requireEntity(id);
        if (isRejected(receive)) {
            return receive;
        }
        validateTransitionForReverseApprove(receive, context);
        processor.ensureReversed(receive, context);
        receive = dao().getEntityById(id);

        receive.setApproveStatus(stateMachine.reverseApproveTargetStatus());
        receive.setApprovedBy(null);
        receive.setApprovedAt(null);
        dao().updateEntity(receive);
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
    protected void validateTransitionForReverseApprove(ErpPurReceive entity, IServiceContext context) {
        try {
            stateMachine.assertCanReverseApprove(getApproveStatus(entity));
        } catch (NopException e) {
            throw illegalStatusException(entity, getApproveStatus(entity), ErpPurConstants.APPROVE_STATUS_APPROVED);
        }
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
    protected boolean isRejected(ErpPurReceive entity) {
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
