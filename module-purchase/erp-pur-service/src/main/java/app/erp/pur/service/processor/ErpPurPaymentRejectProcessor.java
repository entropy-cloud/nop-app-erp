package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurPayment;
import app.erp.pur.service.ErpPurConstants;
import app.erp.common.service.AbstractRejectProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpPurPayment reject per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractRejectProcessor to activate the abstract base class; delegates to ErpPurPaymentProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpPurPaymentRejectProcessor extends AbstractRejectProcessor<ErpPurPayment> {

    @Inject
    ErpPurPaymentProcessor processor;

    @Override
    protected IEntityDao<ErpPurPayment> dao() {
        return daoProvider.daoFor(ErpPurPayment.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getApproveStatus(ErpPurPayment entity) {
        return entity.getApproveStatus();
    }

    @Override
    protected void setApproveStatus(ErpPurPayment entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpPurPayment entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpPurPayment entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isRejected(ErpPurPayment entity) {
        return entity.isRejected();
    }

    @Override
    protected boolean isCancelled(ErpPurPayment entity) {
        return entity.isCancelled();
    }

    @Override
    protected String submittedStatus() {
        return ErpPurConstants.APPROVE_STATUS_SUBMITTED;
    }

    @Override
    protected String rejectedStatus() {
        return ErpPurConstants.APPROVE_STATUS_REJECTED;
    }
}
