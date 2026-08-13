package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.sal.service.statemachine.ErpSalDeliveryApprovalStateMachine;
import app.erp.common.service.AbstractSubmitForApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpSalDelivery submitForApproval per-mutation Processor (plan 2026-07-30-1433-2 R5.2；审批轴 Bean 接线 plan 2026-08-13-1950-2 M4.22)。
 *
 * <p>运行 {@link AbstractSubmitForApprovalProcessor} 骨架；固定来源态/目标态判断委托
 * {@link ErpSalDeliveryApprovalStateMachine}（approveStatus 审批轴 Bean，契约 §4/§7）；
 * 动态业务守卫（requireLinesNonEmpty/requireCustomerActive）保留在 {@link ErpSalDeliveryProcessor} 经
 * {@link #validateBusinessRules} 钩子执行。
 */
public class ErpSalDeliverySubmitForApprovalProcessor extends AbstractSubmitForApprovalProcessor<ErpSalDelivery> {

    @Inject
    ErpSalDeliveryProcessor processor;

    @Inject
    ErpSalDeliveryApprovalStateMachine stateMachine;

    public ErpSalDeliverySubmitForApprovalProcessor() {
        super("ErpSalDelivery");
    }

    @Override
    protected IEntityDao<ErpSalDelivery> dao() {
        return daoProvider.daoFor(ErpSalDelivery.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpSalErrors.ERR_DELIVERY_NOT_FOUND)
                .param(ErpSalErrors.ARG_DELIVERY_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpSalDelivery entity, String current, String... expected) {
        return new NopException(ErpSalErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_DELIVERY_CODE, entity.getCode())
                .param(ErpSalErrors.ARG_CURRENT_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected void validateNotCancelled(ErpSalDelivery entity, IServiceContext context) {
        processor.validateNotCancelled(entity, context);
    }

    @Override
    protected void validateBusinessRules(ErpSalDelivery entity, IServiceContext context) {
        processor.requireLinesNonEmpty(entity, context);
        processor.requireCustomerActive(entity, context);
    }

    @Override
    protected void validateTransitionForSubmit(ErpSalDelivery entity, IServiceContext context) {
        try {
            stateMachine.assertCanSubmit(getApproveStatus(entity));
        } catch (NopException e) {
            throw illegalStatusException(entity, getApproveStatus(entity),
                    ErpSalConstants.APPROVE_STATUS_UNSUBMITTED + " / " + ErpSalConstants.APPROVE_STATUS_REJECTED);
        }
    }

    @Override
    protected String getApproveStatus(ErpSalDelivery entity) {
        String status = entity.getApproveStatus();
        return status == null ? ErpSalConstants.APPROVE_STATUS_UNSUBMITTED : status;
    }

    @Override
    protected void setApproveStatus(ErpSalDelivery entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected boolean isCancelled(ErpSalDelivery entity) {
        return entity.isCancelled();
    }

    @Override
    protected String unsubmittedStatus() {
        return ErpSalConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    @Override
    protected String submittedStatus() {
        return stateMachine.submitTargetStatus();
    }

    @Override
    protected String rejectedStatus() {
        return ErpSalConstants.APPROVE_STATUS_REJECTED;
    }
}
