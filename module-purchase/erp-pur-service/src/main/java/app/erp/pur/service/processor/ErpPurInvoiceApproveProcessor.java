package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.service.ErpPurConstants;
import app.erp.common.service.AbstractApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpPurInvoice approve per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractApproveProcessor to activate the abstract base class; delegates to ErpPurInvoiceProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpPurInvoiceApproveProcessor extends AbstractApproveProcessor<ErpPurInvoice> {

    @Inject
    ErpPurInvoiceProcessor processor;

    @Override
    public ErpPurInvoice approve(String id, IServiceContext context) {
        return processor.approve(id, context);
    }

    @Override
    protected IEntityDao<ErpPurInvoice> dao() {
        return daoProvider.daoFor(ErpPurInvoice.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getApproveStatus(ErpPurInvoice entity) {
        return entity.getApproveStatus();
    }

    @Override
    protected void setApproveStatus(ErpPurInvoice entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpPurInvoice entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpPurInvoice entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isApproved(ErpPurInvoice entity) {
        return entity.isApproved();
    }

    @Override
    protected boolean isCancelled(ErpPurInvoice entity) {
        return entity.isCancelled();
    }

    @Override
    protected String submittedStatus() {
        return ErpPurConstants.APPROVE_STATUS_SUBMITTED;
    }

    @Override
    protected String approvedStatus() {
        return ErpPurConstants.APPROVE_STATUS_APPROVED;
    }
}
