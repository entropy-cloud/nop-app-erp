package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.sal.service.statemachine.ErpSalOrderApprovalStateMachine;
import app.erp.common.service.AbstractReverseApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpSalOrder reverseApprove per-mutation Processor (plan 2026-07-30-1433-2 R5.2；审批轴 Bean 接线 plan 2026-08-13-0945-2 M3.7)。
 *
 * <p>运行 {@link AbstractReverseApproveProcessor} 骨架；固定来源态/目标态判断委托
 * {@link ErpSalOrderApprovalStateMachine}（approveStatus 审批轴 Bean，契约 §4/§7）；
 * 动态业务副作用（commitment-release/intercompany-reverse）保留原位（经 {@link #beforeStateChange} 钩子执行）。
 *
 * <p>reverseApprove 目标态=REJECTED（据实保持 Order 当前行为：doReverseApprove 已覆写为 REJECTED，已合规 §16.4；
 * Bean 据实保持）。骨架 {@code AbstractReverseApproveProcessor}→SUBMITTED 对 Order 为经覆写绕过的死路径。
 */
public class ErpSalOrderReverseApproveProcessor extends AbstractReverseApproveProcessor<ErpSalOrder> {

    @Inject
    ErpSalOrderProcessor processor;

    @Inject
    ErpSalOrderApprovalStateMachine stateMachine;

    @Override
    protected IEntityDao<ErpSalOrder> dao() {
        return daoProvider.daoFor(ErpSalOrder.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpSalErrors.ERR_ORDER_NOT_FOUND)
                .param(ErpSalErrors.ARG_ORDER_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpSalOrder entity, String current, String... expected) {
        return new NopException(ErpSalErrors.ERR_ORDER_ILLEGAL_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_ORDER_CODE, entity.getCode())
                .param(ErpSalErrors.ARG_CURRENT_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected void beforeStateChange(ErpSalOrder entity, IServiceContext context) {
        processor.runCommitmentReleaseHook(entity, context);
        processor.runIntercompanyReverseHook(entity, context);
    }

    @Override
    protected void validateTransitionForReverseApprove(ErpSalOrder entity, IServiceContext context) {
        try {
            stateMachine.assertCanReverseApprove(getApproveStatus(entity));
        } catch (NopException e) {
            throw illegalStatusException(entity, getApproveStatus(entity), ErpSalConstants.APPROVE_STATUS_APPROVED);
        }
    }

    @Override
    protected String getApproveStatus(ErpSalOrder entity) {
        String status = entity.getApproveStatus();
        return status == null ? ErpSalConstants.APPROVE_STATUS_UNSUBMITTED : status;
    }

    @Override
    protected void setApproveStatus(ErpSalOrder entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpSalOrder entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpSalOrder entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isRejected(ErpSalOrder entity) {
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

    @Override
    protected void doReverseApprove(ErpSalOrder entity, IServiceContext context) {
        setApproveStatus(entity, stateMachine.reverseApproveTargetStatus());
        setApprovedBy(entity, null);
        setApprovedAt(entity, null);
    }
}
