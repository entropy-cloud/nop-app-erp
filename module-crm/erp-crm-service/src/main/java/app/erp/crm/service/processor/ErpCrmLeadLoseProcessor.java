package app.erp.crm.service.processor;

import app.erp.crm.dao.entity.ErpCrmLead;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpCrmLead lose per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 NEW/QUALIFIED→LOST 丢单编排（原因必填）；共享 protected helper 单一真相源在 {@link ErpCrmLeadProcessor}（slim-to-S-delegation facade）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCrmLeadLoseProcessor {

    @Inject
    ErpCrmLeadProcessor facade;

    public ErpCrmLead lose(Long leadId, Long lostReasonId, String lostReasonDesc, IServiceContext context) {
        ErpCrmLead lead = facade.requireLead(leadId, context);
        facade.validateTransitionForLose(lead, context);
        facade.requireLostReason(lead, lostReasonId, context);
        facade.doLose(lead, lostReasonId, lostReasonDesc, context);
        return lead;
    }
}
