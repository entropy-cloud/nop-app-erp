package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.service.ErpPurConstants;
import app.erp.common.service.AbstractWithdrawApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpPurReceive withdrawApproval per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractWithdrawApprovalProcessor to activate the abstract base class; delegates to ErpPurReceiveProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpPurReceiveWithdrawApprovalProcessor extends AbstractWithdrawApprovalProcessor<ErpPurReceive> {

    @Inject
    ErpPurReceiveProcessor processor;

    @Override
    protected IEntityDao<ErpPurReceive> dao() {
        return daoProvider.daoFor(ErpPurReceive.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getApproveStatus(ErpPurReceive entity) {
        return entity.getApproveStatus();
    }

    @Override
    protected void setApproveStatus(ErpPurReceive entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected boolean isCancelled(ErpPurReceive entity) {
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
