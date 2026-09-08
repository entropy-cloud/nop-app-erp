package app.erp.crm.service.processor;

import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.common.service.AbstractCancelProcessor;
import app.erp.crm.service.ErpCrmErrors;
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

    /**
     * 骨架守卫裸奔通道修正（plan 2026-09-07-2200-1 form-3）：骨架 {@code illegalStatusException} 的非法迁移错误
     * 由 common 码改为直抛领域码 ERR_LEAD_ILLEGAL_STATUS_TRANSITION（参数形态与 facade illegalTransition 一致）。
     */
    @Override
    protected NopException illegalStatusException(ErpCrmLead entity, String current, String... expected) {
        return new NopException(ErpCrmErrors.ERR_LEAD_ILLEGAL_STATUS_TRANSITION)
                .param(ErpCrmErrors.ARG_LEAD_CODE, entity.getCode())
                .param(ErpCrmErrors.ARG_CURRENT_STATUS, current)
                .param(ErpCrmErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
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
