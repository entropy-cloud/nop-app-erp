package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.pur.service.statemachine.ErpPurOrderApprovalStateMachine;
import app.erp.common.service.AbstractReverseApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpPurOrder reverseApprove per-mutation Processor (plan 2026-07-25-1057-2；审批轴 Bean 接线 plan 2026-08-13-0945-1 M3.5)。
 *
 * <p>运行 {@link AbstractReverseApproveProcessor} 骨架；固定来源态/目标态判断委托
 * {@link ErpPurOrderApprovalStateMachine}（approveStatus 审批轴 Bean，契约 §4/§7）；
 * 动态业务副作用（commitment-release/intercompany-reverse）保留原位（经 {@link #beforeStateChange} 钩子执行）。
 *
 * <p>reverseApprove 目标态=REJECTED（据 Phase 1 Decision 实仓纠正：Order {@code doReverseApprove} 已覆写为 REJECTED，
 * 已合规 §16.4；Bean 据实保持）。骨架 {@code AbstractReverseApproveProcessor}→SUBMITTED 对 Order 为经覆写绕过的死路径。
 */
public class ErpPurOrderReverseApproveProcessor extends AbstractReverseApproveProcessor<ErpPurOrder> {

    @Inject
    ErpPurOrderProcessor processor;

    @Inject
    ErpPurOrderApprovalStateMachine stateMachine;

    @Override
    protected IEntityDao<ErpPurOrder> dao() {
        return daoProvider.daoFor(ErpPurOrder.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpPurErrors.ERR_ORDER_NOT_FOUND)
                .param(ErpPurErrors.ARG_ORDER_CODE, id);
    }

    @Override
    protected NopException illegalStatusException(ErpPurOrder entity, String current, String... expected) {
        return new NopException(ErpPurErrors.ERR_ORDER_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_ORDER_CODE, entity.getCode())
                .param(ErpPurErrors.ARG_CURRENT_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected void beforeStateChange(ErpPurOrder entity, IServiceContext context) {
        processor.runCommitmentReleaseHook(entity, context);
        processor.runIntercompanyReverseHook(entity, context);
    }

    @Override
    protected void validateTransitionForReverseApprove(ErpPurOrder entity, IServiceContext context) {
        try {
            stateMachine.assertCanReverseApprove(getApproveStatus(entity));
        } catch (NopException e) {
            throw illegalStatusException(entity, getApproveStatus(entity), ErpPurConstants.APPROVE_STATUS_APPROVED);
        }
    }

    @Override
    protected String getApproveStatus(ErpPurOrder entity) {
        String status = entity.getApproveStatus();
        return status == null ? ErpPurConstants.APPROVE_STATUS_UNSUBMITTED : status;
    }

    @Override
    protected void setApproveStatus(ErpPurOrder entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpPurOrder entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpPurOrder entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isRejected(ErpPurOrder entity) {
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

    @Override
    protected void doReverseApprove(ErpPurOrder entity, IServiceContext context) {
        setApproveStatus(entity, stateMachine.reverseApproveTargetStatus());
        setApprovedBy(entity, null);
        setApprovedAt(entity, null);
    }
}
