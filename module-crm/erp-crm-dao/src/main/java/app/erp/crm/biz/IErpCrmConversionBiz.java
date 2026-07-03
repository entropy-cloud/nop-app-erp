package app.erp.crm.biz;

import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.dao.entity.ErpSalQuotation;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;

import java.util.Map;

/**
 * CRM 转化服务契约（对齐 {@code docs/design/crm/README.md §衔接契约}）。
 *
 * <p>两条转化链（核心零污染：转化结果存在 CRM 侧弱指针，sales/master-data 实体零字段新增）：
 * <ul>
 *   <li>{@code convertToCustomer}：LEAD → {@link ErpMdPartner}（建客户）+ 新建 OPPORTUNITY lead + 原 lead CONVERTED。</li>
 *   <li>{@code convertToQuotation}：OPPORTUNITY → {@link ErpSalQuotation}（跨域经 {@code IErpSalQuotationBiz}）+ 弱指针回写 + CONVERTED。</li>
 * </ul>
 */
public interface IErpCrmConversionBiz {

    /**
     * 线索转客户：校验 leadType==LEAD；从 contactName/companyName/contactPhone/contactEmail 派生建客户；
     * 新建 ErpCrmLead(leadType=OPPORTUNITY, partnerId=新客户)；原 lead 弱指针回写 + CONVERTED。
     */
    @BizMutation
    ErpMdPartner convertToCustomer(@Name("leadId") Long leadId, IServiceContext context);

    /**
     * 商机转报价单：校验 leadType==OPPORTUNITY 且 partnerId 非空；经 IErpSalQuotationBiz 建报价单（跨域，核心零污染）；
     * 回写 lead relatedBillType=SALES_QUOTATION + relatedBillCode + CONVERTED。
     *
     * @param quotationData 报价单补充字段（如 currencyId/validFrom/validTo 等），可为空使用默认
     */
    @BizMutation
    ErpSalQuotation convertToQuotation(@Name("leadId") Long leadId,
                                       @Name("quotationData") Map<String, Object> quotationData,
                                       IServiceContext context);

    /**
     * 转化后新建的商机（convertToCustomer 产物），供调用方获取新商机 lead。
     */
    @BizMutation
    ErpCrmLead getCreatedOpportunity(@Name("leadId") Long leadId, IServiceContext context);
}
