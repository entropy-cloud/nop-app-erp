package app.erp.crm.service.processor;

import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.service.ErpCrmConstants;
import app.erp.sal.dao.entity.ErpSalQuotation;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.Map;

/**
 * ErpCrmConversion convertToQuotation per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 OPPORTUNITY→报价单 转化编排；共享 protected helper 单一真相源在 {@link ErpCrmConversionProcessor}（delete-after-extract facade）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCrmConversionConvertToQuotationProcessor {

    @Inject
    ErpCrmConversionProcessor facade;

    public ErpSalQuotation convertToQuotation(Long leadId, Map<String, Object> quotationData, IServiceContext context) {
        ErpCrmLead lead = facade.requireLead(leadId, context);
        facade.validateNotConverted(lead, context);
        facade.validateLeadType(lead, ErpCrmConstants.LEAD_TYPE_OPPORTUNITY, context);
        facade.validateDocStatus(lead, ErpCrmConstants.DOC_STATUS_QUALIFIED, context);
        facade.validateWonStage(lead, context);
        facade.requireOpportunityPartner(lead, context);
        ErpSalQuotation quotation = facade.createQuotationFromOpportunity(lead, quotationData, context);
        facade.markLeadConverted(lead, ErpCrmConstants.RELATED_BILL_TYPE_SALES_QUOTATION,
                quotation.getCode(), context);
        return quotation;
    }
}
