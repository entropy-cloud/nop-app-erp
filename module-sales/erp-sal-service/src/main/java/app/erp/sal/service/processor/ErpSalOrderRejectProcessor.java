package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.sal.service.statemachine.ErpSalOrderApprovalStateMachine;
import app.erp.common.service.AbstractRejectProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpSalOrder reject per-mutation Processor (plan 2026-07-30-1433-2 R5.2；审批轴 Bean 接线 plan 2026-08-13-0945-2 M3.7)。
 *
 * <p>运行 {@link AbstractRejectProcessor} 骨架；固定来源态/目标态判断委托
 * {@link ErpSalOrderApprovalStateMachine}（approveStatus 审批轴 Bean，契约 §4/§7）。
 * doReject override 仅设 REJECTED（对齐 facade 语义，纠正抽象骨架误设 approvedBy/approvedAt）。
 */
public class ErpSalOrderRejectProcessor extends AbstractRejectProcessor<ErpSalOrder> {

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
    protected void validateNotCancelled(ErpSalOrder entity, IServiceContext context) {
        processor.validateNotCancelled(entity, context);
    }

    @Override
    protected void validateTransitionForReject(ErpSalOrder entity, IServiceContext context) {
        try {
            stateMachine.assertCanReject(getApproveStatus(entity));
        } catch (NopException e) {
            throw illegalStatusException(entity, getApproveStatus(entity), ErpSalConstants.APPROVE_STATUS_SUBMITTED);
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
    protected boolean isCancelled(ErpSalOrder entity) {
        return entity.isCancelled();
    }

    @Override
    protected String submittedStatus() {
        return ErpSalConstants.APPROVE_STATUS_SUBMITTED;
    }

    @Override
    protected String rejectedStatus() {
        return stateMachine.rejectTargetStatus();
    }

    @Override
    protected void doReject(ErpSalOrder entity, IServiceContext context) {
        setApproveStatus(entity, rejectedStatus());
    }
}
