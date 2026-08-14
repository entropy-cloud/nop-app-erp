package app.erp.crm.service.processor;

import app.erp.crm.dao.entity.ErpCrmLead;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpCrmConversion convertToOpportunity per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 LEAD→OPPORTUNITY 原地直接升格编排（P1-RC-032，UC-CRM-02「不创建客户」分支）；
 * 共享 protected helper 单一真相源在 {@link ErpCrmConversionProcessor}（delete-after-extract facade）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCrmConversionConvertToOpportunityProcessor {

    @Inject
    ErpCrmConversionProcessor facade;

    public ErpCrmLead convertToOpportunity(Long leadId, IServiceContext context) {
        ErpCrmLead lead = facade.requireLead(leadId, context);
        return facade.promoteToOpportunity(lead, context);
    }
}
