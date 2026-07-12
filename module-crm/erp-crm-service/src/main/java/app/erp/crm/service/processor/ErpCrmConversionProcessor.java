package app.erp.crm.service.processor;

import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.service.ErpCrmConstants;
import app.erp.crm.service.ErpCrmErrors;
import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.biz.IErpSalQuotationBiz;
import app.erp.sal.dao.entity.ErpSalQuotation;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * CRM 转化闭环 Processor（{@code processor-extension-pattern.md} Facade + Processor）。
 *
 * <p>两条转化链（核心零污染：转化结果存在 CRM 侧弱指针，sales/master-data 实体零字段新增）：
 * <ul>
 *   <li>{@code convertToCustomer}：LEAD → 经 {@link IErpMdPartnerBiz} 建客户 + 新建 OPPORTUNITY lead + 原 lead 弱指针 + CONVERTED。</li>
 *   <li>{@code convertToQuotation}：OPPORTUNITY → 经 {@link IErpSalQuotationBiz}（{@code ICrudBiz.save}）建报价单 + 弱指针回写 + CONVERTED。</li>
 * </ul>
 *
 * <p>幂等性：{@code docStatus==CONVERTED} 的 lead 再次转化抛 {@code ERR_LEAD_ALREADY_CONVERTED}（幂等键=lead.id）。
 *
 * <p>跨域访问经 I*Biz 接口（{@code IErpMdPartnerBiz}/{@code IErpSalQuotationBiz}），不直接操作 sales/master-data 的 dao。
 */
public class ErpCrmConversionProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpMdPartnerBiz partnerBiz;

    @Inject
    IErpSalQuotationBiz quotationBiz;

    public ErpMdPartner convertToCustomer(Long leadId, IServiceContext context) {
        ErpCrmLead lead = requireLead(leadId, context);
        validateNotConverted(lead, context);
        validateLeadType(lead, ErpCrmConstants.LEAD_TYPE_LEAD, context);
        ErpMdPartner partner = createPartnerFromLead(lead, context);
        ErpCrmLead opportunity = createOpportunityFromLead(lead, partner, context);
        markLeadConverted(lead, ErpCrmConstants.RELATED_BILL_TYPE_CRM_LEAD,
                opportunity.getCode(), context);
        return partner;
    }

    public ErpSalQuotation convertToQuotation(Long leadId, Map<String, Object> quotationData, IServiceContext context) {
        ErpCrmLead lead = requireLead(leadId, context);
        validateNotConverted(lead, context);
        validateLeadType(lead, ErpCrmConstants.LEAD_TYPE_OPPORTUNITY, context);
        requireOpportunityPartner(lead, context);
        ErpSalQuotation quotation = createQuotationFromOpportunity(lead, quotationData, context);
        markLeadConverted(lead, ErpCrmConstants.RELATED_BILL_TYPE_SALES_QUOTATION,
                quotation.getCode(), context);
        return quotation;
    }

    public ErpCrmLead getCreatedOpportunity(Long leadId, IServiceContext context) {
        ErpCrmLead lead = requireLead(leadId, context);
        if (!Objects.equals(lead.getRelatedBillType(), ErpCrmConstants.RELATED_BILL_TYPE_CRM_LEAD)) {
            return null;
        }
        IEntityDao<ErpCrmLead> dao = leadDao();
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("code", lead.getRelatedBillCode()));
        q.setLimit(1);
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    // ---------- step：跨域创建（经 I*Biz）----------

    protected ErpMdPartner createPartnerFromLead(ErpCrmLead lead, IServiceContext context) {
        Map<String, Object> data = new HashMap<>();
        data.put("code", "CUS-" + lead.getId());
        data.put("name", pickName(lead));
        data.put("partnerType", ErpCrmConstants.PARTNER_TYPE_CUSTOMER);
        data.put("status", ErpCrmConstants.PARTNER_STATUS_ACTIVE);
        ifPresent(data, "contactPerson", lead.getContactName());
        ifPresent(data, "phone", lead.getContactPhone());
        ifPresent(data, "email", lead.getContactEmail());
        // 经 IErpMdPartnerBiz.save（ICrudBiz）走跨域管道：Meta 校验 + 数据权限。
        return partnerBiz.save(data, context);
    }

    protected ErpSalQuotation createQuotationFromOpportunity(ErpCrmLead lead,
                                                             Map<String, Object> quotationData,
                                                             IServiceContext context) {
        // 核心零污染：经 IErpSalQuotationBiz.save（ICrudBiz）建报价单，sales 实体零字段新增。
        Map<String, Object> data = quotationData == null ? new HashMap<>() : new HashMap<>(quotationData);
        data.put("code", "SQ-" + lead.getId());
        data.put("orgId", lead.getOrgId());
        data.put("customerId", lead.getPartnerId());
        return quotationBiz.save(data, context);
    }

    // ---------- step：域内创建/更新 ----------

    protected ErpCrmLead createOpportunityFromLead(ErpCrmLead lead, ErpMdPartner partner, IServiceContext context) {
        ErpCrmLead opportunity = leadDao().newEntity();
        opportunity.setOrgId(lead.getOrgId());
        opportunity.setCode("OPP-" + lead.getId());
        opportunity.setLeadType(ErpCrmConstants.LEAD_TYPE_OPPORTUNITY);
        opportunity.setPartnerId(partner.getId());
        opportunity.setDocStatus(ErpCrmConstants.DOC_STATUS_NEW);
        opportunity.setContactName(lead.getContactName());
        opportunity.setContactPhone(lead.getContactPhone());
        opportunity.setContactEmail(lead.getContactEmail());
        opportunity.setCompanyName(lead.getCompanyName());
        opportunity.setOwnerId(lead.getOwnerId());
        opportunity.setTeamId(lead.getTeamId());
        ifPresent(opportunity::setExpectedRevenue, lead.getExpectedRevenue());
        leadDao().saveEntity(opportunity);
        return opportunity;
    }

    protected void markLeadConverted(ErpCrmLead lead, String relatedBillType, String relatedBillCode,
                                     IServiceContext context) {
        lead.setRelatedBillType(relatedBillType);
        lead.setRelatedBillCode(relatedBillCode);
        lead.setDocStatus(ErpCrmConstants.DOC_STATUS_CONVERTED);
        leadDao().updateEntity(lead);
    }

    // ---------- step：校验 ----------

    protected void validateNotConverted(ErpCrmLead lead, IServiceContext context) {
        if (Objects.equals(lead.getDocStatus(), ErpCrmConstants.DOC_STATUS_CONVERTED)) {
            throw new NopException(ErpCrmErrors.ERR_LEAD_ALREADY_CONVERTED)
                    .param(ErpCrmErrors.ARG_LEAD_CODE, lead.getCode());
        }
    }

    protected void validateLeadType(ErpCrmLead lead, String expectedType, IServiceContext context) {
        if (!Objects.equals(lead.getLeadType(), expectedType)) {
            throw new NopException(ErpCrmErrors.ERR_LEAD_TYPE_MISMATCH)
                    .param(ErpCrmErrors.ARG_LEAD_CODE, lead.getCode())
                    .param(ErpCrmErrors.ARG_LEAD_TYPE, expectedType);
        }
    }

    protected void requireOpportunityPartner(ErpCrmLead lead, IServiceContext context) {
        if (lead.getPartnerId() == null) {
            throw new NopException(ErpCrmErrors.ERR_OPPORTUNITY_PARTNER_REQUIRED)
                    .param(ErpCrmErrors.ARG_LEAD_CODE, lead.getCode());
        }
    }

    // ---------- 辅助 ----------

    protected ErpCrmLead requireLead(Long leadId, IServiceContext context) {
        ErpCrmLead lead = leadDao().getEntityById(leadId);
        if (lead == null) {
            throw new NopException(ErpCrmErrors.ERR_LEAD_NOT_FOUND)
                    .param(ErpCrmErrors.ARG_LEAD_ID, leadId);
        }
        return lead;
    }

    protected String pickName(ErpCrmLead lead) {
        if (lead.getCompanyName() != null && !lead.getCompanyName().trim().isEmpty()) {
            return lead.getCompanyName();
        }
        return lead.getContactName() != null ? lead.getContactName() : ("客户-" + lead.getId());
    }

    protected void ifPresent(Map<String, Object> data, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            data.put(key, value);
        }
    }

    @java.lang.FunctionalInterface
    protected interface Setter<T> {
        void set(T value);
    }

    protected <T> void ifPresent(Setter<T> setter, T value) {
        if (value != null) {
            setter.set(value);
        }
    }

    protected IEntityDao<ErpCrmLead> leadDao() {
        return daoProvider.daoFor(ErpCrmLead.class);
    }
}
