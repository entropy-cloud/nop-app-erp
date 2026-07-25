package app.erp.crm.service.processor;

import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.service.ErpCrmConstants;
import app.erp.common.service.AbstractCancelProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpCrmLead cancel per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractCancelProcessor to activate the abstract base class; delegates to ErpCrmLeadProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpCrmLeadCancelProcessor extends AbstractCancelProcessor<ErpCrmLead> {

    @Inject
    ErpCrmLeadProcessor processor;

    @Override
    public ErpCrmLead cancel(String id, IServiceContext context) {
        return processor.cancel(Long.valueOf(id), context);
    }

    @Override
    protected IEntityDao<ErpCrmLead> dao() {
        return daoProvider.daoFor(ErpCrmLead.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getDocStatus(ErpCrmLead entity) {
        return null;
    }

    @Override
    protected void setDocStatus(ErpCrmLead entity, String status) {
        // not reached: main method delegates to monolithic Processor
    }

    @Override
    protected String cancelledDocStatus() {
        return null;
    }
}
