package app.erp.crm.service.processor;

import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.common.service.AbstractCancelProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpCrmLead cancel per-mutation Processor (plan 2026-07-30-2046-1 R5.7, Pattern B)。
 * 自包含编排：requireLead → validateTransitionForCancel → doCancel(docStatus=CANCELLED)。
 * 域逻辑经 facade {@link ErpCrmLeadProcessor} protected helper（单一真相源）。
 * 运行时经 BizModel→facade 旧路径，R5.8 重配线后激活本路径。
 */
public class ErpCrmLeadCancelProcessor extends AbstractCancelProcessor<ErpCrmLead> {

    @Inject
    ErpCrmLeadProcessor processor;

    @Override
    public ErpCrmLead cancel(String id, IServiceContext context) {
        ErpCrmLead lead = processor.requireLead(id, context);
        processor.validateTransitionForCancel(lead, context);
        processor.doCancel(lead, context);
        return lead;
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
