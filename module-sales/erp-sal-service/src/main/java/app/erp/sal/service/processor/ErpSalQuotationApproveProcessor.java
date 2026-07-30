package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalQuotation;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.common.service.AbstractApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpSalQuotation approve per-mutation Processor (plan 2026-07-30-1433-2 R5.2).
 * Runs the AbstractApproveProcessor skeleton; delegates domain-specific hooks to ErpSalQuotationProcessor.
 * 报价单 approve 无业务校验（facade approve 无 validateBusinessRulesForApprove），validateBusinessRules 保持默认空实现。
 */
public class ErpSalQuotationApproveProcessor extends AbstractApproveProcessor<ErpSalQuotation> {

    @Inject
    ErpSalQuotationProcessor processor;

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
        return ErpSalConstants.APPROVE_STATUS_APPROVED;
    }
}
