package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurPayment;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.pur.service.statemachine.ErpPurPaymentApprovalStateMachine;
import app.erp.common.service.AbstractSubmitForApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpPurPayment submitForApproval per-mutation Processor（审批轴 Bean 接线 plan 2026-08-13-1950-1 M4.18，skeleton 路径）。
 *
 * <p>整体覆写 public submitForApproval 方法以编排 workflow（{@code nopFlowId}/{@code useWorkflow}）；
 * 固定来源态/目标态判断委托 {@link ErpPurPaymentApprovalStateMachine}（approveStatus 审批轴 Bean，契约 §4/§7）；
 * 动态业务守卫/副作用（requireSupplierActive/maybeStartWorkflow）保留原位。
 */
public class ErpPurPaymentSubmitForApprovalProcessor extends AbstractSubmitForApprovalProcessor<ErpPurPayment> {

    @Inject
    ErpPurPaymentProcessor processor;

    @Inject
    ErpPurPaymentApprovalStateMachine stateMachine;

    public ErpPurPaymentSubmitForApprovalProcessor() {
        super("ErpPurPayment");
    }

    @Override
    public ErpPurPayment submitForApproval(String id, IServiceContext context) {
        ErpPurPayment entity = requireEntity(id);
        processor.validateNotCancelled(entity, context);
        validateTransitionForSubmit(entity, context);
        processor.requireSupplierActive(entity, context);
        setApproveStatus(entity, submittedStatus());
        dao().updateEntity(entity);
        maybeStartWorkflow(entity, context);
        return entity;
    }

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
    protected void validateTransitionForSubmit(ErpPurPayment entity, IServiceContext context) {
        try {
            stateMachine.assertCanSubmit(getApproveStatus(entity));
        } catch (NopException e) {
            throw illegalStatusException(entity, getApproveStatus(entity),
                    ErpPurConstants.APPROVE_STATUS_UNSUBMITTED + " / " + ErpPurConstants.APPROVE_STATUS_REJECTED);
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
    protected boolean isCancelled(ErpPurPayment entity) {
        return entity.isCancelled();
    }

    @Override
    protected String unsubmittedStatus() {
        return ErpPurConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    @Override
    protected String submittedStatus() {
        return stateMachine.submitTargetStatus();
    }

    @Override
    protected String rejectedStatus() {
        return ErpPurConstants.APPROVE_STATUS_REJECTED;
    }
}
