package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.common.service.AbstractApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpSalOrder approve per-mutation Processor (plan 2026-07-30-1433-2 R5.2).
 * Runs the AbstractApproveProcessor skeleton; delegates domain-specific hooks to ErpSalOrderProcessor.
 * approve 业务校验（客户启用 + 信用额度）经 facade.validateBusinessRulesForApprove；
 * pricingSource 审计 + commitment-commit + intercompany-approve 经 beforeStateChange/afterStateChange hooks。
 */
public class ErpSalOrderApproveProcessor extends AbstractApproveProcessor<ErpSalOrder> {

    @Inject
    ErpSalOrderProcessor processor;

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
        processor.validateBusinessRulesForApprove(entity, context);
    }

    @Override
    protected void beforeStateChange(ErpSalOrder entity, IServiceContext context) {
        processor.auditPricingSourceDistribution(entity, context);
    }

    @Override
    protected void afterStateChange(ErpSalOrder entity, IServiceContext context) {
        processor.runCommitmentCommitHook(entity, context);
        processor.runIntercompanyApproveHook(entity, context);
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
    protected boolean isApproved(ErpSalOrder entity) {
        return entity.isApproved();
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
    protected String approvedStatus() {
        return ErpSalConstants.APPROVE_STATUS_APPROVED;
    }
}
