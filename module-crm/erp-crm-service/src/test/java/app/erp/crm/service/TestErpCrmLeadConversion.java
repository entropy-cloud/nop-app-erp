package app.erp.crm.service;

import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmLeadConvLog;
import app.erp.crm.dao.entity.ErpCrmLostReason;
import app.erp.crm.dao.entity.ErpCrmStage;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdOrganization;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.dao.entity.ErpSalQuotation;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CRM 线索→商机→报价单转化端到端测试（plan 2026-07-04-0549-2 Phase 3）。
 *
 * <p>经 {@link IGraphQLEngine} 调 {@code ErpCrmLead__qualify/lose/moveStage} 与
 * {@code ErpCrmConversion__convertToCustomer/convertToQuotation}，引擎负责建 session/事务/管道。
 *
 * <p>覆盖：(a) 全链路 LEAD→客户→商机→报价单；(b) 异常路径（LOST 缺原因/已转化重复/OPPORTUNITY 缺客户）；
 * (c) 核心零污染断言（sales/master-data 实体无 CRM 外键）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCrmLeadConversion extends JunitAutoTestCase {

    static final Long ORG_ID = 1301L;
    static final Long STAGE_NEW = 1101L;
    static final Long STAGE_DEMO = 1102L;
    static final Long LOST_REASON_ID = 1201L;
    static final Long CURRENCY_ID = 6401L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testFullConversionChain() {
        ormTemplate.runInSession(() -> {
            seedCurrency();
            seedOrganization();
            seedStage(STAGE_NEW, "STG-NEW", "新线索", 10, 20);
            seedStage(STAGE_DEMO, "STG-DEMO", "方案演示", 20, 40);
            seedLead(2001L, "LEAD-001", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_NEW, "Acme Corp", "john@acme.com", "13800000001");
        });

        // qualify: NEW → QUALIFIED，入漏斗设默认 stageId + probability
        assertEquals(0, qualify(2001L).getStatus(), "qualify 应成功");
        ErpCrmLead qualified = reloadLead(2001L);
        assertEquals(ErpCrmConstants.DOC_STATUS_QUALIFIED, qualified.getDocStatus(), "LEAD → QUALIFIED");
        assertEquals(STAGE_NEW, qualified.getStageId(), "qualify 设默认 stageId");
        assertEquals(20, qualified.getProbability(), "probability 取阶段 defaultProbability");

        // moveStage 前移 + convLog 审计（probability 已由 qualify 回填为 20，moveStage 不覆盖已填值）
        assertEquals(0, moveStage(2001L, STAGE_DEMO).getStatus(), "moveStage 应成功");
        ErpCrmLead moved = reloadLead(2001L);
        assertEquals(STAGE_DEMO, moved.getStageId(), "stageId 前移");
        assertEquals(20, moved.getProbability(), "probability 已填则 moveStage 不覆盖（保留 qualify 回填值）");
        List<ErpCrmLeadConvLog> logs = loadConvLogs(2001L);
        assertFalse(logs.isEmpty(), "moveStage 写入 convLog 审计行");
        assertEquals(STAGE_NEW, logs.get(0).getFromStageId(), "convLog fromStageId");
        assertEquals(STAGE_DEMO, logs.get(0).getToStageId(), "convLog toStageId");

        // convertToCustomer: LEAD → 客户 + 新建 OPPORTUNITY + 原 lead CONVERTED 弱指针
        ApiResponse<?> conv = convertToCustomer(2001L);
        assertEquals(0, conv.getStatus(), "convertToCustomer 应成功");
        Long partnerId = extractId(conv.getData(), "id");
        ErpMdPartner partner = daoProvider.daoFor(ErpMdPartner.class).getEntityById(partnerId);
        assertNotNull(partner, "客户已创建");
        assertEquals("CUSTOMER", partner.getPartnerType(), "客户 partnerType=CUSTOMER");

        ErpCrmLead originalLead = reloadLead(2001L);
        assertEquals(ErpCrmConstants.DOC_STATUS_CONVERTED, originalLead.getDocStatus(), "原 lead → CONVERTED");
        assertEquals(ErpCrmConstants.RELATED_BILL_TYPE_CRM_LEAD, originalLead.getRelatedBillType(),
                "原 lead 弱指针 relatedBillType=CRM_LEAD");
        assertNotNull(originalLead.getRelatedBillCode(), "原 lead 弱指针 relatedBillCode=新商机 code");

        // 新商机 OPPORTUNITY
        ErpCrmLead opportunity = reloadLeadByCode(originalLead.getRelatedBillCode());
        assertNotNull(opportunity, "新商机已创建");
        assertEquals(ErpCrmConstants.LEAD_TYPE_OPPORTUNITY, opportunity.getLeadType(), "新商机 leadType=OPPORTUNITY");
        assertEquals(partnerId, opportunity.getPartnerId(), "新商机 partnerId=新客户");
        assertEquals(ErpCrmConstants.DOC_STATUS_NEW, opportunity.getDocStatus(), "新商机 docStatus=NEW");

        // convertToQuotation: OPPORTUNITY → 报价单（经 IErpSalQuotationBiz save）+ 弱指针 + CONVERTED
        ApiResponse<?> qconv = convertToQuotation(opportunity.getId(), quotationData());
        assertEquals(0, qconv.getStatus(), "convertToQuotation 应成功");
        Long quotationId = extractId(qconv.getData(), "id");
        ErpSalQuotation quotation = daoProvider.daoFor(ErpSalQuotation.class).getEntityById(quotationId);
        assertNotNull(quotation, "报价单已创建");
        assertEquals(partnerId, quotation.getCustomerId(), "报价单 customerId=商机 partnerId");

        ErpCrmLead convertedOpp = reloadLead(opportunity.getId());
        assertEquals(ErpCrmConstants.DOC_STATUS_CONVERTED, convertedOpp.getDocStatus(), "商机 → CONVERTED");
        assertEquals(ErpCrmConstants.RELATED_BILL_TYPE_SALES_QUOTATION, convertedOpp.getRelatedBillType(),
                "商机弱指针 relatedBillType=SALES_QUOTATION");
        assertEquals(quotation.getCode(), convertedOpp.getRelatedBillCode(), "商机弱指针 relatedBillCode=报价单号");
    }

    @Test
    public void testLoseWithoutReasonRejected() {
        ormTemplate.runInSession(() -> {
            seedStage(STAGE_NEW, "STG-NEW", "新线索", 10, 20);
            seedLostReason();
            seedLead(2101L, "LEAD-LOSE-001", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_NEW, "Beta Corp", null, null);
        });
        ApiResponse<?> bad = lose(2101L, null, null);
        assertEquals(ErpCrmErrors.ERR_LOST_REASON_REQUIRED.getErrorCode(), bad.getCode(),
                "LOST 缺 lostReasonId → ERR_LOST_REASON_REQUIRED");

        ApiResponse<?> ok = lose(2101L, LOST_REASON_ID, "价格过高");
        assertEquals(0, ok.getStatus(), "带 lostReasonId 的 lose 应成功");
        assertEquals(ErpCrmConstants.DOC_STATUS_LOST, reloadLead(2101L).getDocStatus(), "LEAD → LOST");
    }

    @Test
    public void testIllegalTransitionRejected() {
        ormTemplate.runInSession(() -> {
            seedStage(STAGE_NEW, "STG-NEW", "新线索", 10, 20);
            seedLead(2201L, "LEAD-ILLEGAL-001", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_QUALIFIED, "Gamma Corp", null, null);
        });
        // QUALIFIED 不可再次 qualify（仅 NEW 可）
        ApiResponse<?> bad = qualify(2201L);
        assertEquals(ErpCrmErrors.ERR_LEAD_ILLEGAL_STATUS_TRANSITION.getErrorCode(), bad.getCode(),
                "QUALIFIED 不可再次 qualify → ERR_LEAD_ILLEGAL_STATUS_TRANSITION");
    }

    @Test
    public void testAlreadyConvertedRejected() {
        ormTemplate.runInSession(() -> {
            seedCurrency();
            seedOrganization();
            seedStage(STAGE_NEW, "STG-NEW", "新线索", 10, 20);
            seedLead(2301L, "LEAD-CONV-001", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_NEW, "Delta Corp", "delta@corp.com", null);
        });
        assertEquals(0, convertToCustomer(2301L).getStatus(), "首次 convertToCustomer 应成功");

        ApiResponse<?> second = convertToCustomer(2301L);
        assertEquals(ErpCrmErrors.ERR_LEAD_ALREADY_CONVERTED.getErrorCode(), second.getCode(),
                "已 CONVERTED 重复转化 → ERR_LEAD_ALREADY_CONVERTED");
    }

    @Test
    public void testOpportunityWithoutPartnerRejected() {
        ormTemplate.runInSession(() -> {
            seedCurrency();
            seedStage(STAGE_NEW, "STG-NEW", "新线索", 10, 20);
            // 商机但无 partnerId
            ErpCrmLead opp = newLead(2401L, "OPP-NOPARTNER-001", ErpCrmConstants.LEAD_TYPE_OPPORTUNITY,
                    ErpCrmConstants.DOC_STATUS_QUALIFIED);
            daoProvider.daoFor(ErpCrmLead.class).saveEntity(opp);
        });
        ApiResponse<?> bad = convertToQuotation(2401L, quotationData());
        assertEquals(ErpCrmErrors.ERR_OPPORTUNITY_PARTNER_REQUIRED.getErrorCode(), bad.getCode(),
                "OPPORTUNITY 缺 partner → ERR_OPPORTUNITY_PARTNER_REQUIRED");
    }

    @Test
    public void testLeadTypeMismatchRejected() {
        ormTemplate.runInSession(() -> {
            seedCurrency();
            seedStage(STAGE_NEW, "STG-NEW", "新线索", 10, 20);
            // 商机尝试 convertToCustomer（期望 LEAD）
            ErpCrmLead opp = newLead(2501L, "OPP-TYPE-001", ErpCrmConstants.LEAD_TYPE_OPPORTUNITY,
                    ErpCrmConstants.DOC_STATUS_NEW);
            daoProvider.daoFor(ErpCrmLead.class).saveEntity(opp);
        });
        ApiResponse<?> bad = convertToCustomer(2501L);
        assertEquals(ErpCrmErrors.ERR_LEAD_TYPE_MISMATCH.getErrorCode(), bad.getCode(),
                "OPPORTUNITY 调 convertToCustomer → ERR_LEAD_TYPE_MISMATCH");
    }

    @Test
    public void testDuplicateDetection() {
        ormTemplate.runInSession(() -> {
            seedStage(STAGE_NEW, "STG-NEW", "新线索", 10, 20);
            seedLead(2601L, "LEAD-DUP-001", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_NEW, "Duplicate Corp", "dup@corp.com", "13900000000");
            seedLead(2602L, "LEAD-DUP-002", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_NEW, "Duplicate Corp", "other@corp.com", null);
        });
        ApiResponse<?> resp = findDuplicates(2601L);
        assertEquals(0, resp.getStatus(), "findDuplicates 应成功");
        Object data = resp.getData();
        assertTrue(data instanceof List, "返回候选列表");
        assertFalse(((List<?>) data).isEmpty(), "companyName 命中 → 返回重复候选");
    }

    @Test
    public void testZeroPollutionAssertion() {
        // 核心零污染：sales/master-data 实体无 CRM 外键（无 opportunityId getter）。
        assertFalse(hasMethod(ErpSalQuotation.class, "getOpportunityId"),
                "ErpSalQuotation 无 opportunityId（核心零污染）");
        assertFalse(hasMethod(ErpMdPartner.class, "getOpportunityId"),
                "ErpMdPartner 无 opportunityId（核心零污染）");
        assertFalse(hasMethod(ErpMdPartner.class, "getLeadId"),
                "ErpMdPartner 无 leadId（核心零污染）");
    }

    @Test
    public void testCancel() {
        ormTemplate.runInSession(() -> {
            seedStage(STAGE_NEW, "STG-NEW", "新线索", 10, 20);
            seedLead(2701L, "LEAD-CANCEL-001", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_NEW, "Epsilon Corp", null, null);
        });
        assertEquals(0, cancel(2701L).getStatus(), "cancel 应成功");
        assertEquals(ErpCrmConstants.DOC_STATUS_CANCELLED, reloadLead(2701L).getDocStatus(),
                "LEAD → CANCELLED");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> qualify(Long leadId) {
        return rpc(mutation, "ErpCrmLead__qualify", Map.of("leadId", leadId));
    }

    private ApiResponse<?> lose(Long leadId, Long lostReasonId, String desc) {
        Map<String, Object> m = new HashMap<>();
        m.put("leadId", leadId);
        m.put("lostReasonId", lostReasonId);
        m.put("lostReasonDesc", desc);
        return rpc(mutation, "ErpCrmLead__lose", m);
    }

    private ApiResponse<?> cancel(Long leadId) {
        return rpc(mutation, "ErpCrmLead__cancel", Map.of("leadId", leadId));
    }

    private ApiResponse<?> moveStage(Long leadId, Long toStageId) {
        return rpc(mutation, "ErpCrmLead__moveStage", Map.of("leadId", leadId, "toStageId", toStageId));
    }

    private ApiResponse<?> convertToCustomer(Long leadId) {
        return rpc(mutation, "ErpCrmLead__convertToCustomer", Map.of("leadId", leadId));
    }

    private ApiResponse<?> convertToQuotation(Long leadId, Map<String, Object> quotationData) {
        return rpc(mutation, "ErpCrmLead__convertToQuotation",
                Map.of("leadId", leadId, "quotationData", quotationData));
    }

    private ApiResponse<?> findDuplicates(Long leadId) {
        return rpc(query, "ErpCrmLead__findDuplicates", Map.of("leadId", leadId));
    }

    private ApiResponse<?> rpc(GraphQLOperationType opType, String action, Map<String, Object> data) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, ApiRequest.build(data));
        return graphQLEngine.executeRpc(ctx);
    }

    private Map<String, Object> quotationData() {
        Map<String, Object> data = new HashMap<>();
        data.put("businessDate", LocalDate.of(2026, 7, 1));
        data.put("validFrom", LocalDate.of(2026, 7, 1));
        data.put("validTo", LocalDate.of(2026, 12, 31));
        data.put("currencyId", CURRENCY_ID);
        data.put("exchangeRate", new BigDecimal("1"));
        data.put("totalAmount", new BigDecimal("100"));
        data.put("totalTaxAmount", BigDecimal.ZERO);
        data.put("totalAmountWithTax", new BigDecimal("100"));
        data.put("docStatus", "DRAFT");
        data.put("approveStatus", "UNSUBMITTED");
        return data;
    }

    private Long extractId(Object data, String key) {
        Object idVal = ((Map<?, ?>) data).get(key);
        if (idVal instanceof Number) {
            return ((Number) idVal).longValue();
        }
        return Long.valueOf(String.valueOf(idVal));
    }

    // ---------- seed helpers ----------

    private void seedCurrency() {
        IEntityDao<ErpMdCurrency> dao = daoProvider.daoFor(ErpMdCurrency.class);
        ErpMdCurrency currency = new ErpMdCurrency();
        currency.setId(CURRENCY_ID);
        currency.setCode("CNY");
        currency.setName("人民币");
        dao.saveEntity(currency);
    }

    private void seedOrganization() {
        // 报价单 save 经 IErpSalQuotationBiz 管道做 orgId 引用校验（→ ErpMdOrganization），须提供组织主数据。
        IEntityDao<ErpMdOrganization> dao = daoProvider.daoFor(ErpMdOrganization.class);
        ErpMdOrganization org = new ErpMdOrganization();
        org.setId(ORG_ID);
        org.setCode("ORG-" + ORG_ID);
        org.setName("测试组织");
        org.setOrgType("COMPANY");
        org.setStatus("ACTIVE");
        dao.saveEntity(org);
    }

    private void seedStage(Long id, String code, String name, int sequence, int defaultProbability) {
        IEntityDao<ErpCrmStage> dao = daoProvider.daoFor(ErpCrmStage.class);
        ErpCrmStage stage = new ErpCrmStage();
        stage.setId(id);
        stage.setCode(code);
        stage.setStageName(name);
        stage.setSequence(sequence);
        stage.setDefaultProbability(defaultProbability);
        dao.saveEntity(stage);
    }

    private void seedLostReason() {
        IEntityDao<ErpCrmLostReason> dao = daoProvider.daoFor(ErpCrmLostReason.class);
        ErpCrmLostReason reason = new ErpCrmLostReason();
        reason.setId(LOST_REASON_ID);
        reason.setCode("PRICE");
        reason.setName("价格过高");
        dao.saveEntity(reason);
    }

    private void seedLead(Long id, String code, String leadType, String docStatus,
                          String companyName, String email, String phone) {
        ErpCrmLead lead = newLead(id, code, leadType, docStatus);
        lead.setCompanyName(companyName);
        lead.setContactEmail(email);
        lead.setContactPhone(phone);
        lead.setContactName("联系人" + id);
        daoProvider.daoFor(ErpCrmLead.class).saveEntity(lead);
    }

    private ErpCrmLead newLead(Long id, String code, String leadType, String docStatus) {
        ErpCrmLead lead = new ErpCrmLead();
        lead.setId(id);
        lead.setCode(code);
        lead.setOrgId(ORG_ID);
        lead.setLeadType(leadType);
        lead.setDocStatus(docStatus);
        return lead;
    }

    // ---------- reload helpers ----------

    private ErpCrmLead reloadLead(Long id) {
        return daoProvider.daoFor(ErpCrmLead.class).getEntityById(id);
    }

    private ErpCrmLead reloadLeadByCode(String code) {
        IEntityDao<ErpCrmLead> dao = daoProvider.daoFor(ErpCrmLead.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private List<ErpCrmLeadConvLog> loadConvLogs(Long leadId) {
        IEntityDao<ErpCrmLeadConvLog> dao = daoProvider.daoFor(ErpCrmLeadConvLog.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("leadId", leadId));
        return dao.findAllByQuery(q);
    }

    private boolean hasMethod(Class<?> clazz, String methodName) {
        try {
            clazz.getMethod(methodName);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
