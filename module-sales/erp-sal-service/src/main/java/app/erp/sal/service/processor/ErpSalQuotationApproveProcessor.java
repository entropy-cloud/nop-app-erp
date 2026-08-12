package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalQuotation;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.sal.service.statemachine.ErpSalQuotationApprovalStateMachine;
import app.erp.common.service.AbstractApproveProcessor;
import io.nop.api.core.exceptions.ErrorCode;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpSalQuotation approve per-mutation Processor (plan 2026-07-30-1433-2 R5.2；审批轴 Bean 接线 plan 2026-08-13-0945-2 M3.6)。
 *
 * <p>运行 {@link AbstractApproveProcessor} 骨架；固定来源态/目标态判断委托
 * {@link ErpSalQuotationApprovalStateMachine}（approveStatus 审批轴 Bean，契约 §4/§7）。
 * 报价单 approve 无业务校验（facade approve 无 validateBusinessRulesForApprove），validateBusinessRules 保持默认空实现。
 */
public class ErpSalQuotationApproveProcessor extends AbstractApproveProcessor<ErpSalQuotation> {

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
    protected void validateTransitionForApprove(ErpSalQuotation entity, IServiceContext context) {
        try {
            stateMachine.assertCanApprove(getApproveStatus(entity));
        } catch (NopException e) {
            throw illegalStatusException(entity, getApproveStatus(entity), ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    @Override
    protected String getApproveStatus(ErpSalQuotation entity) {
        String status = entity.getApproveStatus();
        return status == null ? ErpSalConstants.APPROVE_STATUS_UNSUBMITTED : status;
    }

    @Override
    protected void setApproveStatus(ErpSalQuotation entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpSalQuotation entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpSalQuotation entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isApproved(ErpSalQuotation entity) {
        return entity.isApproved();
    }

    @Override
    protected boolean isCancelled(ErpSalQuotation entity) {
        return entity.isCancelled();
    }

    @Override
    protected String submittedStatus() {
        return ErpSalConstants.APPROVE_STATUS_SUBMITTED;
    }

    @Override
    protected String approvedStatus() {
        return stateMachine.approveTargetStatus();
    }

    @Override
    protected ErrorCode sodErrorCode() {
        return ErpSalErrors.ERR_SAL_APPROVER_IS_CREATOR;
    }
}
