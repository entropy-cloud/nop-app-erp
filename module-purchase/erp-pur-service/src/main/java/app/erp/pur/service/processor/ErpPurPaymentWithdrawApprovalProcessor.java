package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurPayment;
import app.erp.pur.service.ErpPurConstants;
import app.erp.common.service.AbstractWithdrawApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpPurPayment withdrawApproval per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractWithdrawApprovalProcessor to activate the abstract base class; delegates to ErpPurPaymentProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpPurPaymentWithdrawApprovalProcessor extends AbstractWithdrawApprovalProcessor<ErpPurPayment> {

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
    protected boolean isCancelled(ErpPurPayment entity) {
        return entity.isCancelled();
    }

    @Override
    protected String unsubmittedStatus() {
        return ErpPurConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    @Override
    protected String submittedStatus() {
        return ErpPurConstants.APPROVE_STATUS_SUBMITTED;
    }
}
