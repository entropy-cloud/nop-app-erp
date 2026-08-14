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
 * <p>三条转化链（核心零污染：转化结果存在 CRM 侧弱指针，sales/master-data 实体零字段新增）：
 * <ul>
 *   <li>{@code convertToCustomer}：LEAD → {@link ErpMdPartner}（建客户）+ 新建 OPPORTUNITY lead + 原 lead CONVERTED。</li>
 *   <li>{@code convertToOpportunity}：LEAD → 原 lead 原地升格为 OPPORTUNITY（不建 Partner/新 Lead，docStatus 保持 QUALIFIED）。</li>
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
     * 线索直接升格（UC-CRM-02「不创建客户」分支）：校验 leadType==LEAD 且 docStatus==QUALIFIED；
     * 原 lead 原地 setLeadType(OPPORTUNITY)——不创建 ErpMdPartner、不新建 ErpCrmLead，docStatus 保持 QUALIFIED
     * （后续 convertToQuotation 前置（QUALIFIED + won-stage）成立）。
     */
    @BizMutation
    ErpCrmLead convertToOpportunity(@Name("leadId") Long leadId, IServiceContext context);

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
