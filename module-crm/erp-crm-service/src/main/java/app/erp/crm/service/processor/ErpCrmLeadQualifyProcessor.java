package app.erp.crm.service.processor;

import app.erp.crm.dao.entity.ErpCrmLead;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpCrmLead qualify per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 NEW→QUALIFIED 状态机编排；共享 protected helper 单一真相源在 {@link ErpCrmLeadProcessor}（slim-to-S-delegation facade）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCrmLeadQualifyProcessor {

    @Inject
    ErpCrmLeadProcessor facade;

    public ErpCrmLead qualify(Long leadId, IServiceContext context) {
        ErpCrmLead lead = facade.requireLead(leadId, context);
        facade.validateTransitionForQualify(lead, context);
        facade.doQualify(lead, context);
        return lead;
    }
}
