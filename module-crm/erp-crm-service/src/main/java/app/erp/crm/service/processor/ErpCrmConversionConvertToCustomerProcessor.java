package app.erp.crm.service.processor;

import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.service.ErpCrmConstants;
import app.erp.md.dao.entity.ErpMdPartner;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpCrmConversion convertToCustomer per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 LEAD→客户+商机 转化编排；共享 protected helper 单一真相源在 {@link ErpCrmConversionProcessor}（delete-after-extract facade）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCrmConversionConvertToCustomerProcessor {

    @Inject
    ErpCrmConversionProcessor facade;

    public ErpMdPartner convertToCustomer(Long leadId, IServiceContext context) {
        ErpCrmLead lead = facade.requireLead(leadId, context);
        facade.validateNotConverted(lead, context);
        facade.validateLeadType(lead, ErpCrmConstants.LEAD_TYPE_LEAD, context);
        facade.validateDocStatus(lead, ErpCrmConstants.DOC_STATUS_QUALIFIED, context);
        ErpMdPartner partner = facade.createPartnerFromLead(lead, context);
        ErpCrmLead opportunity = facade.createOpportunityFromLead(lead, partner, context);
        facade.markLeadConverted(lead, ErpCrmConstants.RELATED_BILL_TYPE_CRM_LEAD,
                opportunity.getCode(), context);
        return partner;
    }
}
