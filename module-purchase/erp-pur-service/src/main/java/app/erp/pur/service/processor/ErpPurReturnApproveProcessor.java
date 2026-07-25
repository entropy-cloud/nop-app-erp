package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurReturn;
import app.erp.pur.service.ErpPurConstants;
import app.erp.common.service.AbstractApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpPurReturn approve per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractApproveProcessor to activate the abstract base class; delegates to ErpPurReturnProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpPurReturnApproveProcessor extends AbstractApproveProcessor<ErpPurReturn> {

    @Inject
    ErpPurReturnProcessor processor;

    @Override
    public ErpPurReturn approve(String id, IServiceContext context) {
        return processor.approve(id, context);
    }

    @Override
    protected IEntityDao<ErpPurReturn> dao() {
        return daoProvider.daoFor(ErpPurReturn.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getApproveStatus(ErpPurReturn entity) {
        return entity.getApproveStatus();
    }

    @Override
    protected void setApproveStatus(ErpPurReturn entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpPurReturn entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpPurReturn entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isApproved(ErpPurReturn entity) {
        return entity.isApproved();
    }

    @Override
    protected boolean isCancelled(ErpPurReturn entity) {
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
