package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.sal.service.statemachine.ErpSalOrderApprovalStateMachine;
import app.erp.common.service.AbstractSubmitForApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpSalOrder submitForApproval per-mutation Processor (plan 2026-07-30-1433-2 R5.2；审批轴 Bean 接线 plan 2026-08-13-0945-2 M3.7)。
 *
 * <p>运行 {@link AbstractSubmitForApprovalProcessor} 骨架；固定来源态/目标态判断委托
 * {@link ErpSalOrderApprovalStateMachine}（approveStatus 审批轴 Bean，契约 §4/§7）；
 * 动态业务守卫/副作用（requireLinesNonEmpty/requireCustomerActive）保留在 {@link ErpSalOrderProcessor} 经
 * {@link #validateBusinessRules} 钩子执行。
 *
 * <p>非法边映射：Bean 抛 common 层 {@code ERR_ILLEGAL_STATUS_TRANSITION}（含 {@code action=submit}/
 * {@code fromStatus} 元数据）作 cause，{@link #validateTransitionForSubmit} 捕获后映射领域码
 * {@link ErpSalErrors#ERR_ORDER_ILLEGAL_STATUS_TRANSITION}（+ {@code orderCode} 实体编号/上下文）。
 */
public class ErpSalOrderSubmitForApprovalProcessor extends AbstractSubmitForApprovalProcessor<ErpSalOrder> {

    @Inject
    ErpSalOrderProcessor processor;

    @Inject
    ErpSalOrderApprovalStateMachine stateMachine;

    public ErpSalOrderSubmitForApprovalProcessor() {
        super("ErpSalOrder");
    }

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
    protected void validateBusinessRules(ErpSalOrder entity, IServiceContext context) {
        processor.requireLinesNonEmpty(entity, context);
        processor.requireCustomerActive(entity, context);
    }

    @Override
    protected void validateTransitionForSubmit(ErpSalOrder entity, IServiceContext context) {
        try {
            stateMachine.assertCanSubmit(getApproveStatus(entity));
        } catch (NopException e) {
            throw illegalStatusException(entity, getApproveStatus(entity),
                    ErpSalConstants.APPROVE_STATUS_UNSUBMITTED + " / " + ErpSalConstants.APPROVE_STATUS_REJECTED);
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
    protected boolean isCancelled(ErpSalOrder entity) {
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
