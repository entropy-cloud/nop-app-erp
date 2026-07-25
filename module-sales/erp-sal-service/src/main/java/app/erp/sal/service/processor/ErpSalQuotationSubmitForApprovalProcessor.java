package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalQuotation;
import app.erp.sal.service.ErpSalConstants;
import app.erp.common.service.AbstractSubmitForApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpSalQuotation submitForApproval per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractSubmitForApprovalProcessor to activate the abstract base class; delegates to ErpSalQuotationProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpSalQuotationSubmitForApprovalProcessor extends AbstractSubmitForApprovalProcessor<ErpSalQuotation> {

    @Inject
    ErpSalQuotationProcessor processor;

    public ErpSalQuotationSubmitForApprovalProcessor() {
        super("ErpSalQuotation");
    }

    @Override
    public ErpSalQuotation submitForApproval(String id, IServiceContext context) {
        return processor.submitForApproval(id, context);
    }

    @Override
    protected IEntityDao<ErpSalQuotation> dao() {
        return daoProvider.daoFor(ErpSalQuotation.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
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
        return ErpSalConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    @Override
    protected String submittedStatus() {
        return ErpSalConstants.APPROVE_STATUS_SUBMITTED;
    }

    @Override
    protected String rejectedStatus() {
        return ErpSalConstants.APPROVE_STATUS_REJECTED;
    }
}
