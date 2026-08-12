package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalQuotation;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.sal.service.statemachine.ErpSalQuotationApprovalStateMachine;
import app.erp.common.service.AbstractWithdrawApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpSalQuotation withdrawApproval per-mutation Processor (plan 2026-07-30-1433-2 R5.2；审批轴 Bean 接线 plan 2026-08-13-0945-2 M3.6)。
 *
 * <p>运行 {@link AbstractWithdrawApprovalProcessor} 骨架；固定来源态/目标态判断委托
 * {@link ErpSalQuotationApprovalStateMachine}（approveStatus 审批轴 Bean，契约 §4/§7）。
 * NopScriptError → NopException 语义等价：doc-cancelled→ERR_QUOTATION_ILLEGAL_DOC_STATUS_TRANSITION，
 * invalid-status→ERR_QUOTATION_ILLEGAL_STATUS_TRANSITION。
 */
public class ErpSalQuotationWithdrawApprovalProcessor extends AbstractWithdrawApprovalProcessor<ErpSalQuotation> {

    @Inject
    ErpSalQuotationProcessor processor;

    @Inject
    ErpSalQuotationApprovalStateMachine stateMachine;

    @Override
    protected IEntityDao<ErpSalQuotation> dao() {
        return daoProvider.daoFor(ErpSalQuotation.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpSalErrors.ERR_QUOTATION_NOT_FOUND)
                .param(ErpSalErrors.ARG_QUOTATION_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpSalQuotation entity, String current, String... expected) {
        return new NopException(ErpSalErrors.ERR_QUOTATION_ILLEGAL_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_QUOTATION_CODE, entity.getCode())
                .param(ErpSalErrors.ARG_CURRENT_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected void validateNotCancelled(ErpSalQuotation entity, IServiceContext context) {
        processor.validateNotCancelled(entity, context);
    }

    @Override
    protected void validateTransitionForWithdraw(ErpSalQuotation entity, IServiceContext context) {
        try {
            stateMachine.assertCanWithdraw(getApproveStatus(entity));
        } catch (NopException e) {
            throw illegalStatusException(entity, getApproveStatus(entity), ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    @Override
    protected String getApproveStatus(ErpSalQuotation entity) {
        return entity.getApproveStatus();
    }

    @Override
    protected void setApproveStatus(ErpSalQuotation entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected boolean isCancelled(ErpSalQuotation entity) {
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
