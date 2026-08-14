package app.erp.crm.service;

import app.erp.crm.dao.entity.ErpCrmCampaign;
import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.service.report.ErpCrmReportBizModel;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CRM UTM 归因族测试（plan 2026-08-14-1815-3 RC-R1.24 Phase 3）。
 *
 * <p>覆盖 P1-RC-037（UTM copy-on-create：新建 Lead 时经 IErpCrmCampaignBiz 复制 campaign.medium/source，
 * 显式传入不覆盖，campaign 缺失跳过，仅新建路径）+ P1-RC-038（营销活动归因报表：
 * buildCampaignAttributionDataset/campaignAttributionData 聚合 + renderHtml 冒烟）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCrmUtmAttribution extends JunitAutoTestCase {

    private static final io.nop.core.context.IServiceContext CTX = new io.nop.core.context.ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    ErpCrmReportBizModel reportBiz;

    // ===================== P1-RC-037 UTM copy-on-create =====================

    @Test
    public void testUtmCopyOnCreate() {
        ormTemplate.runInSession(() -> seedCampaign(7001L, "CAM-UTM-1", "春季促销", "cpc", "google"));
        ApiResponse<?> resp = save(leadData("LEAD-UTM-1", null, null, 7001L));
        assertEquals(0, resp.getStatus(), "新建 Lead 应成功");
        ErpCrmLead lead = reloadByCode("LEAD-UTM-1");
        assertNotNull(lead, "Lead 已落库");
        assertEquals("cpc", lead.getUtmMedium(), "utmMedium 复制 campaign.medium");
        assertEquals("google", lead.getUtmSource(), "utmSource 复制 campaign.source");
        output("response.json5", resp);
    }

    @Test
    public void testUtmExplicitValueNotOverridden() {
        ormTemplate.runInSession(() -> seedCampaign(7002L, "CAM-UTM-2", "邮件营销", "email", "newsletter"));
        ApiResponse<?> resp = save(leadData("LEAD-UTM-2", "social", null, 7002L));
        assertEquals(0, resp.getStatus(), "新建 Lead 应成功");
        ErpCrmLead lead = reloadByCode("LEAD-UTM-2");
        assertEquals("social", lead.getUtmMedium(), "显式传入 utmMedium 不被覆盖");
        assertEquals("newsletter", lead.getUtmSource(), "未显式传入 utmSource 复制 campaign.source");
        output("response.json5", resp);
    }

    @Test
    public void testUtmCopySkippedWhenCampaignMissingOrNullFields() {
        // campaign 不存在：平台 FK 校验（OrmEntityCopier.copyRefEntity loadEntityById）在 defaultPrepareSave 前拒绝，
        // 干净错误码不崩溃；UTM copy 分支对不存在 campaign 的防御性跳过（get(ignoreUnknown=true) → null → skip）保持。
        ApiResponse<?> missing = save(leadData("LEAD-UTM-3", null, null, 99999L));
        assertEquals("nop.err.dao.unknown-entity", missing.getCode(), "campaign 不存在 → 平台 FK 校验拒绝");

        // campaign 存在但 medium/source 为 null：复制跳过不抛，utm 字段保持 null。
        ormTemplate.runInSession(() -> seedCampaign(7004L, "CAM-UTM-4", "无参活动", null, null));
        ApiResponse<?> nullFields = save(leadData("LEAD-UTM-5", null, null, 7004L));
        assertEquals(0, nullFields.getStatus(), "campaign 字段 null 不抛异常");
        ErpCrmLead lead = reloadByCode("LEAD-UTM-5");
        assertNull(lead.getUtmMedium(), "campaign.medium 为 null → utmMedium 保持 null");
        assertNull(lead.getUtmSource(), "campaign.source 为 null → utmSource 保持 null");
        output("response.json5", nullFields);
    }

    @Test
    public void testUtmCopyNotTriggeredOnUpdate() {
        ormTemplate.runInSession(() -> seedCampaign(7003L, "CAM-UTM-3", "社媒营销", "social", "wechat"));
        ApiResponse<?> created = save(leadData("LEAD-UTM-4", null, null, null));
        assertEquals(0, created.getStatus(), "新建 Lead 应成功");
        String id = String.valueOf(((Map<?, ?>) created.getData()).get("id"));

        Map<String, Object> upd = new LinkedHashMap<>();
        upd.put("id", id);
        upd.put("campaignId", 7003L);
        ApiResponse<?> updated = update(upd);
        assertEquals(0, updated.getStatus(), "更新 Lead 应成功");

        ErpCrmLead lead = reloadByCode("LEAD-UTM-4");
        assertEquals(7003L, lead.getCampaignId(), "campaignId 已更新");
        assertNull(lead.getUtmMedium(), "更新路径不触发 UTM copy（仅新建）");
        assertNull(lead.getUtmSource(), "更新路径不触发 UTM copy（仅新建）");
        output("2_update_response.json5", updated);
    }

    // ===================== P1-RC-038 归因报表 =====================

    @Test
    public void testCampaignAttributionDataset() {
        ormTemplate.runInSession(() -> {
            seedCampaign(7101L, "CAM-ATTR-1", "春季促销", "cpc", "google");
            seedCampaign(7102L, "CAM-ATTR-2", "秋季促销", "email", "newsletter");
            seedLead(7201L, "LEAD-ATTR-1", 7101L, bd("10000"));
            seedLead(7202L, "LEAD-ATTR-2", 7101L, bd("5000"));
            seedLead(7203L, "LEAD-ATTR-3", 7102L, bd("3000"));
            seedLead(7204L, "LEAD-ATTR-4", null, bd("9999"));
        });
        List<Map<String, Object>> ds = reportBiz.campaignAttributionData(CTX);
        assertNotNull(ds, "数据集非空");
        assertFalse(ds.isEmpty(), "有归因数据");
        assertEquals(2, ds.size(), "仅 2 个 campaign 行（无 campaign 关联的 lead 不计入）");
        Map<String, Object> row1 = findRow(ds, 7101L);
        assertNotNull(row1, "campaign 7101 行存在");
        assertEquals("春季促销", row1.get("campaignName"), "campaignName 经 to-one 解析");
        assertEquals(2, ((Number) row1.get("leadCount")).intValue(), "leadCount=2");
        assertEquals(0, bd("15000").compareTo(toBd(row1.get("expectedRevenue"))), "expectedRevenue=10000+5000");
        Map<String, Object> row2 = findRow(ds, 7102L);
        assertNotNull(row2, "campaign 7102 行存在");
        assertEquals(1, ((Number) row2.get("leadCount")).intValue(), "leadCount=1");
        assertEquals(0, bd("3000").compareTo(toBd(row2.get("expectedRevenue"))), "expectedRevenue=3000");
        assertNull(findRow(ds, null), "无 null campaignId 组");
    }

    @Test
    public void testCampaignAttributionRenderHtml() {
        ormTemplate.runInSession(() -> {
            seedCampaign(7101L, "CAM-ATTR-1", "春季促销", "cpc", "google");
            seedLead(7201L, "LEAD-ATTR-1", 7101L, bd("10000"));
        });
        String html = reportBiz.renderHtml("campaign-attribution", null, CTX);
        assertNotNull(html, "renderHtml 非空");
        assertFalse(html.trim().isEmpty(), "renderHtml 文本非空");
        assertTrue(html.contains("春季促销"), "renderHtml 含 campaignName");
    }

    // ===================== helpers =====================

    private Map<String, Object> leadData(String code, String utmMedium, String utmSource, Long campaignId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", code);
        data.put("leadType", ErpCrmConstants.LEAD_TYPE_LEAD);
        data.put("docStatus", ErpCrmConstants.DOC_STATUS_NEW);
        data.put("contactName", "联系人" + code);
        if (utmMedium != null) data.put("utmMedium", utmMedium);
        if (utmSource != null) data.put("utmSource", utmSource);
        if (campaignId != null) data.put("campaignId", campaignId);
        return data;
    }

    private ApiResponse<?> save(Map<String, Object> data) {
        return rpc(GraphQLOperationType.mutation, "ErpCrmLead__save", Map.of("data", data));
    }

    private ApiResponse<?> update(Map<String, Object> data) {
        return rpc(GraphQLOperationType.mutation, "ErpCrmLead__update", Map.of("data", data));
    }

    private ApiResponse<?> rpc(GraphQLOperationType opType, String action, Map<String, Object> data) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, ApiRequest.build(data));
        return graphQLEngine.executeRpc(ctx);
    }

    private void seedCampaign(Long id, String code, String name, String medium, String source) {
        IEntityDao<ErpCrmCampaign> dao = daoProvider.daoFor(ErpCrmCampaign.class);
        ErpCrmCampaign c = new ErpCrmCampaign();
        c.setId(id);
        c.setCode(code);
        c.setName(name);
        c.setCampaignName(name);
        c.setMedium(medium);
        c.setSource(source);
        dao.saveEntity(c);
    }

    private void seedLead(Long id, String code, Long campaignId, BigDecimal expectedRevenue) {
        IEntityDao<ErpCrmLead> dao = daoProvider.daoFor(ErpCrmLead.class);
        ErpCrmLead l = new ErpCrmLead();
        l.setId(id);
        l.setCode(code);
        l.setLeadType(ErpCrmConstants.LEAD_TYPE_LEAD);
        l.setDocStatus(ErpCrmConstants.DOC_STATUS_NEW);
        l.setContactName("联系人" + id);
        l.setCampaignId(campaignId);
        l.setExpectedRevenue(expectedRevenue);
        dao.saveEntity(l);
    }

    private ErpCrmLead reloadByCode(String code) {
        IEntityDao<ErpCrmLead> dao = daoProvider.daoFor(ErpCrmLead.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private static Map<String, Object> findRow(List<Map<String, Object>> ds, Long campaignId) {
        for (Map<String, Object> row : ds) {
            Object cid = row.get("campaignId");
            if (campaignId == null ? cid == null : campaignId.equals(((Number) cid).longValue())) {
                return row;
            }
        }
        return null;
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static BigDecimal toBd(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Number) return new BigDecimal(v.toString());
        return new BigDecimal(String.valueOf(v));
    }
}
