package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalReturn;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.sal.service.statemachine.ErpSalReturnApprovalStateMachine;
import app.erp.common.service.AbstractWithdrawApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpSalReturn withdrawApproval per-mutation Processor (plan 2026-07-30-1433-2 R5.2；审批轴 Bean 接线 plan 2026-08-13-1950-2 M4.28)。
 *
 * <p>运行 {@link AbstractWithdrawApprovalProcessor} 骨架；固定来源态/目标态判断委托
 * {@link ErpSalReturnApprovalStateMachine}（approveStatus 审批轴 Bean，契约 §4/§7）。
 */
public class ErpSalReturnWithdrawApprovalProcessor extends AbstractWithdrawApprovalProcessor<ErpSalReturn> {

    @Inject
    ErpSalReturnProcessor processor;

    @Inject
    ErpSalReturnApprovalStateMachine stateMachine;

    @Override
    protected IEntityDao<ErpSalReturn> dao() {
        return daoProvider.daoFor(ErpSalReturn.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpSalErrors.ERR_RETURN_NOT_FOUND)
                .param(ErpSalErrors.ARG_RETURN_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpSalReturn entity, String current, String... expected) {
        return new NopException(ErpSalErrors.ERR_RETURN_ILLEGAL_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_RETURN_CODE, entity.getCode())
                .param(ErpSalErrors.ARG_CURRENT_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected void validateNotCancelled(ErpSalReturn entity, IServiceContext context) {
        processor.validateNotCancelled(entity, context);
    }

    @Override
    protected void validateTransitionForWithdraw(ErpSalReturn entity, IServiceContext context) {
        try {
            stateMachine.assertCanWithdraw(getApproveStatus(entity));
        } catch (NopException e) {
            throw illegalStatusException(entity, getApproveStatus(entity), ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    @Override
    protected String getApproveStatus(ErpSalReturn entity) {
        return entity.getApproveStatus();
    }

    @Override
    protected void setApproveStatus(ErpSalReturn entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected boolean isCancelled(ErpSalReturn entity) {
        return entity.isCancelled();
    }

    @Override
    protected String unsubmittedStatus() {
        return stateMachine.withdrawTargetStatus();
    }

    @Override
    protected String submittedStatus() {
        return ErpSalConstants.APPROVE_STATUS_SUBMITTED;
    }
}
