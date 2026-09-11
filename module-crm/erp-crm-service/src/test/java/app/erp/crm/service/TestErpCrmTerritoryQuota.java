package app.erp.crm.service;

import app.erp.crm.dao.entity.ErpCrmForecast;
import app.erp.crm.dao.entity.ErpCrmForecastPeriod;
import app.erp.crm.dao.entity.ErpCrmLeadConvLog;
import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmQuota;
import app.erp.crm.dao.entity.ErpCrmTerritory;
import app.erp.crm.dao.entity.ErpCrmTerritoryAssignmentRule;
import app.erp.crm.dao.entity.ErpCrmTerritoryPipeline;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.isNull;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CRM 区域管理 + 配额 + 分配引擎端到端测试（plan 2026-07-07-1100-1 Phase 4 Proof）。
 *
 * <p>经 {@link IGraphQLEngine} 调区域树维护 + 分配引擎 + 配额聚合 + 管道对比入口，覆盖：
 * 区域树建子节点（level/fullPath/isLeaf 回填）+ 移动重算 + 成环拒绝 + 深度超限 + 有子节点禁删 +
 * 分配引擎四 conditionType 匹配 + default 兜底 + 无匹配留空 + auto-assign config-gated 关闭 + reassignLead 覆盖 +
 * 配额层级 Σ + 显式值优先 + 定稿/解冻 + 年度均分 + 区域管道对比入口三段返回。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCrmTerritoryQuota extends JunitAutoTestCase {

    static final String ORG_ID = "1301";
    static final String ROOT_TERRITORY_ID = "6001";
    static final String REGION_HUADONG_ID = "6002";
    static final String AREA_SHANGHAI_ID = "6003";
    static final String TEAM_PUDONG_ID = "6004";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    // ---------- 区域树维护 ----------

    @Test
    public void testCreateChildBackfillsLevelFullPathIsLeaf() {
        ormTemplate.runInSession(() -> {
            seedTerritory(ROOT_TERRITORY_ID, "T-ROOT", "公司", null,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 0, "/", true);
        });

        ApiResponse<?> resp = createChild(ROOT_TERRITORY_ID, "T-HUADONG",
                "华东", ErpCrmConstants.TERRITORY_TYPE_REGION, null);
        assertEquals(0, resp.getStatus(), "createChild 应成功");

        ErpCrmTerritory region = findTerritoryByCode("T-HUADONG");
        assertNotNull(region, "子节点已创建");
        assertEquals(1, region.getLevel(), "level=parent.level+1=1");
        assertEquals("/T-HUADONG", region.getFullPath(), "fullPath=parent.fullPath+\"/\"+code");
        assertTrue(region.getIsLeaf(), "新建子节点默认叶子");

        ErpCrmTerritory parent = reloadTerritory(ROOT_TERRITORY_ID);
        assertFalse(parent.getIsLeaf(), "父节点 isLeaf 翻转为 false");
    }

    @Test
    public void testMoveTerritoryReroutesSubtree() {
        ormTemplate.runInSession(() -> {
            seedTerritory("7001", "T-ROOT", "公司", null,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 0, "/", true);
            seedTerritory("7002", "T-HUABEI", "华北", "7001",
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 1, "/T-HUABEI", true);
            seedTerritory("7003", "T-HUADONG", "华东", "7001",
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 1, "/T-HUADONG", true);
            seedTerritory("7004", "T-SHANGHAI", "上海", "7003",
                    ErpCrmConstants.TERRITORY_TYPE_AREA, 2, "/T-HUADONG/T-SHANGHAI", true);
        });

        // 移动 SHANGHAI 从 HUADONG 到 HUABEI
        ApiResponse<?> resp = moveTerritory("7004", "7002");
        assertEquals(0, resp.getStatus(), "moveTerritory 应成功");

        ErpCrmTerritory moved = reloadTerritory("7004");
        assertEquals("7002", moved.getParentId(), "parentId 重指向新父");
        assertEquals("/T-HUABEI/T-SHANGHAI", moved.getFullPath(), "fullPath 重算");
        assertEquals(2, moved.getLevel(), "level 保持与新父对应");
    }

    @Test
    public void testMoveTerritoryRejectsCycle() {
        ormTemplate.runInSession(() -> {
            seedTerritory("7101", "T-ROOT-B", "公司B", null,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 0, "/", true);
            seedTerritory("7102", "T-CHILD-B", "子B", "7101",
                    ErpCrmConstants.TERRITORY_TYPE_AREA, 1, "/T-ROOT-B/T-CHILD-B", true);
        });
        // 把 ROOT 移到自己的子节点下 → 成环
        ApiResponse<?> resp = moveTerritory("7101", "7102");
        assertEquals(ErpCrmErrors.ERR_TERRITORY_CYCLE.getErrorCode(), resp.getCode(),
                "成环 → ERR_TERRITORY_CYCLE");
    }

    @Test
    public void testMaxDepthExceededRejected() {
        ormTemplate.runInSession(() -> {
            seedTerritory("7201", "T-ROOT-C", "公司C", null,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 0, "/", true);
            seedTerritory("7202", "T-L1", "L1", "7201",
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 1, "/T-ROOT-C/T-L1", true);
            seedTerritory("7203", "T-L2", "L2", "7202",
                    ErpCrmConstants.TERRITORY_TYPE_AREA, 2, "/T-ROOT-C/T-L1/T-L2", true);
            seedTerritory("7204", "T-L3", "L3", "7203",
                    ErpCrmConstants.TERRITORY_TYPE_BRANCH, 3, "/T-ROOT-C/T-L1/T-L2/T-L3", true);
        });
        // 在 level=3 节点下建子 → level=4，max-depth=4 允许；但建下一层 level=5 应拒绝
        ApiResponse<?> ok = createChild("7204", "T-L4-OK", "L4OK",
                ErpCrmConstants.TERRITORY_TYPE_TEAM, null);
        assertEquals(0, ok.getStatus(), "level=4 ≤ max-depth=4 应允许");
        String level4Id = findTerritoryByCode("T-L4-OK").getId();
        // 在 level=4 节点下建子 → level=5，超过 max-depth=4
        ApiResponse<?> bad = createChild(level4Id, "T-L5-BAD", "L5BAD",
                ErpCrmConstants.TERRITORY_TYPE_TEAM, null);
        assertEquals(ErpCrmErrors.ERR_TERRITORY_MAX_DEPTH_EXCEEDED.getErrorCode(), bad.getCode(),
                "level=5 > max-depth=4 → ERR_TERRITORY_MAX_DEPTH_EXCEEDED");
    }

    @Test
    public void testDeleteRejectsWhenHasChildren() {
        ormTemplate.runInSession(() -> {
            seedTerritory("7301", "T-ROOT-D", "公司D", null,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 0, "/", false);
            seedTerritory("7302", "T-CHILD-D", "子D", "7301",
                    ErpCrmConstants.TERRITORY_TYPE_AREA, 1, "/T-ROOT-D/T-CHILD-D", true);
        });
        ApiResponse<?> resp = graphQLEngine.executeRpc(
                graphQLEngine.newRpcContext(mutation, "ErpCrmTerritory__delete",
                        ApiRequest.build(Map.of("id", "7301"))));
        assertEquals(ErpCrmErrors.ERR_TERRITORY_HAS_CHILDREN.getErrorCode(), resp.getCode(),
                "有子节点禁删 → ERR_TERRITORY_HAS_CHILDREN");
    }

    // ---------- 分配引擎 ----------

    @Test
    public void testAssignmentEngineAllConditionTypes() {
        String territoryIdGeo = "8001";
        String territoryIdIndustry = "8002";
        String territoryIdSize = "8003";
        String territoryIdCustom = "8004";
        String defaultTerritoryId = "8005";
        ormTemplate.runInSession(() -> {
            seedTerritory("8000", "T-ROOT-E", "公司E", null,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 0, "/", true);
            seedTerritory(territoryIdGeo, "T-GEO", "地理区", "8000",
                    ErpCrmConstants.TERRITORY_TYPE_AREA, 1, "/T-ROOT-E/T-GEO", true);
            seedTerritory(territoryIdIndustry, "T-IND", "行业区", "8000",
                    ErpCrmConstants.TERRITORY_TYPE_AREA, 1, "/T-ROOT-E/T-IND", true);
            seedTerritory(territoryIdSize, "T-SIZE", "规模区", "8000",
                    ErpCrmConstants.TERRITORY_TYPE_AREA, 1, "/T-ROOT-E/T-SIZE", true);
            seedTerritory(territoryIdCustom, "T-CUST", "自定义区", "8000",
                    ErpCrmConstants.TERRITORY_TYPE_AREA, 1, "/T-ROOT-E/T-CUST", true);
            seedTerritory(defaultTerritoryId, "T-DEF", "默认区", "8000",
                    ErpCrmConstants.TERRITORY_TYPE_AREA, 1, "/T-ROOT-E/T-DEF", true);

            seedRule("8101", "GEO-RULE", 10, territoryIdGeo,
                    ErpCrmConstants.ASSIGNMENT_CONDITION_GEOGRAPHY,
                    "{\"province\":[\"上海\"]}",
                    ErpCrmConstants.ASSIGNMENT_METHOD_MANUAL, null, false);
            seedRule("8102", "IND-RULE", 20, territoryIdIndustry,
                    ErpCrmConstants.ASSIGNMENT_CONDITION_INDUSTRY,
                    "{\"industryCode\":[\"manufacturing\"]}",
                    ErpCrmConstants.ASSIGNMENT_METHOD_MANUAL, null, false);
            seedRule("8103", "SIZE-RULE", 30, territoryIdSize,
                    ErpCrmConstants.ASSIGNMENT_CONDITION_CUSTOMER_SIZE,
                    "{\"minEmployees\":1000,\"maxEmployees\":100000}",
                    ErpCrmConstants.ASSIGNMENT_METHOD_MANUAL, null, false);
            seedRule("8104", "CUST-RULE", 40, territoryIdCustom,
                    ErpCrmConstants.ASSIGNMENT_CONDITION_CUSTOM_FIELD,
                    "{\"utmSource\":\"baidu\"}",
                    ErpCrmConstants.ASSIGNMENT_METHOD_MANUAL, null, false);
            seedRule("8105", "DEFAULT-RULE", 100, defaultTerritoryId,
                    ErpCrmConstants.ASSIGNMENT_CONDITION_CUSTOM_FIELD,
                    "{}",
                    ErpCrmConstants.ASSIGNMENT_METHOD_MANUAL, null, true);
        });

        // GEOGRAPHY 命中
        String geoLead = "8201";
        ormTemplate.runInSession(() -> seedLead(geoLead, "LEAD-GEO-001", "上海某制造", "manufacturing",
                new BigDecimal("500"), "baidu", null));
        assertEquals(0, assignLead(geoLead).getStatus(), "GEOGRAPHY assignLead 应成功");
        assertEquals(territoryIdGeo, reloadLead(geoLead).getTerritoryId(),
                "GEOGRAPHY 命中 → territoryId=geo");

        // INDUSTRY 命中（无 province 关键词）
        String indLead = "8202";
        ormTemplate.runInSession(() -> seedLead(indLead, "LEAD-IND-001", "某公司", "manufacturing",
                new BigDecimal("500"), null, null));
        assertEquals(0, assignLead(indLead).getStatus(), "INDUSTRY assignLead 应成功");
        assertEquals(territoryIdIndustry, reloadLead(indLead).getTerritoryId(),
                "INDUSTRY 命中 → territoryId=industry");

        // CUSTOMER_SIZE 命中
        String sizeLead = "8203";
        ormTemplate.runInSession(() -> seedLead(sizeLead, "LEAD-SIZE-001", "无关键地名", "其他",
                new BigDecimal("5000"), null, null));
        assertEquals(0, assignLead(sizeLead).getStatus(), "SIZE assignLead 应成功");
        assertEquals(territoryIdSize, reloadLead(sizeLead).getTerritoryId(),
                "CUSTOMER_SIZE 命中 → territoryId=size");

        // CUSTOM_FIELD 命中（utmSource=baidu，SIZE 不在范围）
        String custLead = "8204";
        ormTemplate.runInSession(() -> seedLead(custLead, "LEAD-CUST-001", "无关键地名", "其他",
                new BigDecimal("10"), "baidu", null));
        assertEquals(0, assignLead(custLead).getStatus(), "CUST assignLead 应成功");
        assertEquals(territoryIdCustom, reloadLead(custLead).getTerritoryId(),
                "CUSTOM_FIELD 命中 → territoryId=custom");
    }

    @Test
    public void testAssignmentDefaultFallbackAndNoMatch() {
        String defaultTerritoryId = "8301";
        ormTemplate.runInSession(() -> {
            seedTerritory("8300", "T-ROOT-F", "公司F", null,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 0, "/", true);
            seedTerritory(defaultTerritoryId, "T-DEF-F", "默认F", "8300",
                    ErpCrmConstants.TERRITORY_TYPE_AREA, 1, "/T-ROOT-F/T-DEF-F", true);
            seedRule("8311", "GEO-RULE-F", 10, defaultTerritoryId,
                    ErpCrmConstants.ASSIGNMENT_CONDITION_GEOGRAPHY,
                    "{\"province\":[\"上海\"]}",
                    ErpCrmConstants.ASSIGNMENT_METHOD_MANUAL, null, false);
            seedRule("8312", "DEFAULT-RULE-F", 100, defaultTerritoryId,
                    ErpCrmConstants.ASSIGNMENT_CONDITION_CUSTOM_FIELD,
                    "{}",
                    ErpCrmConstants.ASSIGNMENT_METHOD_MANUAL, null, true);
        });

        // 无 GEOGRAPHY 命中 → 走 default
        String leadId = "8401";
        ormTemplate.runInSession(() -> seedLead(leadId, "LEAD-DEF-001", "无地名", "其他",
                new BigDecimal("10"), null, null));
        assertEquals(0, assignLead(leadId).getStatus(), "default assignLead 应成功");
        assertEquals(defaultTerritoryId, reloadLead(leadId).getTerritoryId(),
                "无匹配 GEOGRAPHY → 走 default rule");
    }

    @Test
    public void testReassignLeadOverridesEngine() {
        String territoryA = "8501";
        String territoryB = "8502";
        String teamId = "8601";
        String ownerId = "userZZ";
        ormTemplate.runInSession(() -> {
            seedTerritory("8500", "T-ROOT-G", "公司G", null,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 0, "/", true);
            seedTerritory(territoryA, "T-A", "区A", "8500",
                    ErpCrmConstants.TERRITORY_TYPE_AREA, 1, "/T-ROOT-G/T-A", true);
            seedTerritory(territoryB, "T-B", "区B", "8500",
                    ErpCrmConstants.TERRITORY_TYPE_AREA, 1, "/T-ROOT-G/T-B", true);
            seedLead("8701", "LEAD-REASSIGN-001", "公司无配", "其他",
                    new BigDecimal("100"), null, territoryA);
        });
        ApiResponse<?> resp = graphQLEngine.executeRpc(graphQLEngine.newRpcContext(
                mutation, "ErpCrmLead__reassignLead",
                ApiRequest.build(Map.of("leadId", "8701",
                        "territoryId", territoryB, "teamId", teamId, "ownerId", ownerId))));
        assertEquals(0, resp.getStatus(), "reassignLead 应成功");
        ErpCrmLead lead = reloadLead("8701");
        assertEquals(territoryB, lead.getTerritoryId(), "territoryId 被覆盖");
        assertEquals(teamId, lead.getTeamId(), "teamId 被设置");
        assertEquals(ownerId, lead.getOwnerId(), "ownerId 被设置");
    }

    // ---------- 配额管理 ----------

    @Test
    public void testQuotaRollupExplicitValuePriorityAndAggregate() {
        String company = "9001";
        String regionId = "9002";
        String teamId = "9101";
        ormTemplate.runInSession(() -> {
            seedTerritory(company, "T-ROOT-Q", "公司Q", null,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 0, "/", false);
            seedTerritory(regionId, "T-REG-Q", "区Q", company,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 1, "/T-ROOT-Q/T-REG-Q", false);

            // 团队级显式配额
            seedQuota("9201", regionId, teamId, null,
                    ErpCrmConstants.QUOTA_PERIOD_QUARTERLY, 2026, "2026-Q3",
                    new BigDecimal("1000"), false);
            // 个人级配额（区域子节点）
            seedQuota("9202", regionId, teamId, "userQ1",
                    ErpCrmConstants.QUOTA_PERIOD_QUARTERLY, 2026, "2026-Q3",
                    new BigDecimal("500"), false);
        });

        // 区域级聚合：1000 + 500 = 1500
        ApiResponse<?> resp = graphQLEngine.executeRpc(graphQLEngine.newRpcContext(
                query, "ErpCrmQuota__getQuotaRollup",
                ApiRequest.build(Map.of("territoryId", regionId,
                        "periodType", ErpCrmConstants.QUOTA_PERIOD_QUARTERLY,
                        "fiscalYear", 2026, "periodLabel", "2026-Q3"))));
        assertEquals(0, resp.getStatus(), "getQuotaRollup 应成功");
        assertNotNull(resp.getData(), "聚合返回非空");
        @SuppressWarnings("unchecked")
        Map<String, Object> rollup = (Map<String, Object>) resp.getData();
        BigDecimal rollupAmount = new BigDecimal(String.valueOf(rollup.get("quotaAmount")));
        assertEquals(0, new BigDecimal("1500").compareTo(rollupAmount),
                "区域聚合 = 1000 + 500 = 1500");
    }

    @Test
    public void testFinalizeAndUnfinalizeQuota() {
        String quotaId = "9301";
        ormTemplate.runInSession(() -> {
            seedTerritory("9300", "T-ROOT-Q2", "公司Q2", null,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 0, "/", true);
            seedQuota(quotaId, "9300", null, null,
                    ErpCrmConstants.QUOTA_PERIOD_QUARTERLY, 2026, "2026-Q3",
                    new BigDecimal("1000"), false);
        });

        assertEquals(0, finalizeQuota(quotaId).getStatus(), "finalizeQuota 应成功");
        assertTrue(reloadQuota(quotaId).getIsFinalized(), "isFinalized=true");

        // 已定稿拒绝再次定稿
        ApiResponse<?> bad = finalizeQuota(quotaId);
        assertEquals(ErpCrmErrors.ERR_QUOTA_FINALIZED.getErrorCode(), bad.getCode(),
                "重复定稿 → ERR_QUOTA_FINALIZED");

        // 解冻
        assertEquals(0, unfinalizeQuota(quotaId).getStatus(), "unfinalizeQuota 应成功");
        assertFalse(reloadQuota(quotaId).getIsFinalized(), "isFinalized=false");
    }

    @Test
    public void testDistributeAnnualQuota() {
        String quotaId = "9401";
        ormTemplate.runInSession(() -> {
            seedTerritory("9400", "T-ROOT-Q3", "公司Q3", null,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 0, "/", true);
            seedQuota(quotaId, "9400", null, null,
                    ErpCrmConstants.QUOTA_PERIOD_ANNUAL, 2026, "2026",
                    new BigDecimal("1200"), false);
        });

        ApiResponse<?> resp = graphQLEngine.executeRpc(graphQLEngine.newRpcContext(
                mutation, "ErpCrmQuota__distributeAnnualQuota",
                ApiRequest.build(Map.of("quotaId", quotaId,
                        "periodType", ErpCrmConstants.QUOTA_PERIOD_QUARTERLY))));
        assertEquals(0, resp.getStatus(), "distributeAnnualQuota 应成功");
        // 验证 4 个季度配额行已生成
        List<ErpCrmQuota> subs = listQuotasByTerritory("9400");
        assertEquals(4 + 1, subs.size(), "原 1 行 + 新 4 季度行 = 5 行");
        BigDecimal each = new BigDecimal("300.00");
        boolean hasExpected = subs.stream().anyMatch(q ->
                ErpCrmConstants.QUOTA_PERIOD_QUARTERLY.equals(q.getPeriodType())
                        && each.compareTo(q.getQuotaAmount()) == 0);
        assertTrue(hasExpected, "均分 1200/4=300/季");
    }

    @Test
    public void testDistributeAnnualRejectsFinalized() {
        String quotaId = "9501";
        ormTemplate.runInSession(() -> {
            seedTerritory("9500", "T-ROOT-Q4", "公司Q4", null,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 0, "/", true);
            seedQuota(quotaId, "9500", null, null,
                    ErpCrmConstants.QUOTA_PERIOD_ANNUAL, 2026, "2026",
                    new BigDecimal("1200"), true);
        });
        ApiResponse<?> resp = graphQLEngine.executeRpc(graphQLEngine.newRpcContext(
                mutation, "ErpCrmQuota__distributeAnnualQuota",
                ApiRequest.build(Map.of("quotaId", quotaId))));
        assertEquals(ErpCrmErrors.ERR_QUOTA_FINALIZED.getErrorCode(), resp.getCode(),
                "已定稿年度配额不可均分 → ERR_QUOTA_FINALIZED");
    }

    // ---------- 区域管道对比入口 ----------

    @Test
    public void testGetTerritoryPipelineReturnsThreeSections() {
        String territoryId = "9601";
        String teamId = "9602";
        String ownerId = "userP";
        String periodLabel = "2026-Q3";
        ormTemplate.runInSession(() -> {
            seedTerritory("9600", "T-ROOT-P", "公司P", null,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 0, "/", false);
            seedTerritory(territoryId, "T-REG-P", "区P", "9600",
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 1, "/T-ROOT-P/T-REG-P", false);

            // Quota：目标段
            seedQuota("9611", territoryId, teamId, ownerId,
                    ErpCrmConstants.QUOTA_PERIOD_QUARTERLY, 2026, periodLabel,
                    new BigDecimal("10000"), false);

            // Forecast：预测段（P1-CK-crm-001 期间过滤契约：期间 label 须可解析——补种期间行）
            ErpCrmForecastPeriod period = daoProvider.daoFor(ErpCrmForecastPeriod.class).newEntity();
            period.setId("9999");
            period.setCode("PER-9999");
            period.setOrgId(ORG_ID);
            period.setPeriodType("QUARTERLY");
            period.setPeriodStart(java.time.LocalDate.of(2026, 7, 1));
            period.setPeriodEnd(java.time.LocalDate.of(2026, 9, 30));
            period.setLabel(periodLabel);
            period.setStatus(ErpCrmConstants.FORECAST_PERIOD_STATUS_OPEN);
            period.setIsCurrent(Boolean.TRUE);
            daoProvider.daoFor(ErpCrmForecastPeriod.class).saveEntity(period);

            ErpCrmForecast forecast = new ErpCrmForecast();
            forecast.setId("9621");
            forecast.setOrgId(ORG_ID);
            forecast.setPeriodId("9999");
            forecast.setTerritoryId(territoryId);
            forecast.setCommitAmount(new BigDecimal("8000"));
            forecast.setUpsideAmount(new BigDecimal("1500"));
            forecast.setWeightedAmount(new BigDecimal("5000"));
            forecast.setBestCaseAmount(new BigDecimal("9500"));
            forecast.setOpportunityCount(7);
            forecast.setExpectedClosedRevenue(BigDecimal.ZERO);
            daoProvider.daoFor(ErpCrmForecast.class).saveEntity(forecast);

            // Lead CONVERTED：实际段
            ErpCrmLead lead = new ErpCrmLead();
            lead.setId("9631");
            lead.setCode("OPP-P-001");
            lead.setOrgId(ORG_ID);
            lead.setLeadType(ErpCrmConstants.LEAD_TYPE_OPPORTUNITY);
            lead.setDocStatus(ErpCrmConstants.DOC_STATUS_CONVERTED);
            lead.setTerritoryId(territoryId);
            lead.setExpectedRevenue(new BigDecimal("4000"));
            daoProvider.daoFor(ErpCrmLead.class).saveEntity(lead);

            // P1-CK-crm-001 期间归因契约：CONVERTED lead 须有期间内 ConvLog 事件方计入实际段
            ErpCrmLeadConvLog convLog = daoProvider.daoFor(ErpCrmLeadConvLog.class).newEntity();
            convLog.setId("9641");
            convLog.setLeadId("9631");
            convLog.setOrgId(ORG_ID);
            convLog.setChangedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.of(2026, 8, 15, 10, 0)));
            daoProvider.daoFor(ErpCrmLeadConvLog.class).saveEntity(convLog);
        });

        ApiResponse<?> resp = graphQLEngine.executeRpc(graphQLEngine.newRpcContext(
                query, "ErpCrmQuota__getTerritoryPipeline",
                ApiRequest.build(Map.of("territoryId", territoryId, "periodLabel", periodLabel))));
        assertEquals(0, resp.getStatus(), "getTerritoryPipeline 应成功");
        @SuppressWarnings("unchecked")
        Map<String, Object> pipeline = (Map<String, Object>) resp.getData();
        assertNotNull(pipeline, "返回非空");
        assertNotNull(pipeline.get("quota"), "目标段非空");
        assertNotNull(pipeline.get("forecast"), "预测段非空");
        assertNotNull(pipeline.get("actual"), "实际段非空");
        @SuppressWarnings("unchecked")
        Map<String, Object> quota = (Map<String, Object>) pipeline.get("quota");
        BigDecimal quotaAmount = new BigDecimal(String.valueOf(quota.get("quotaAmount")));
        assertEquals(0, new BigDecimal("10000").compareTo(quotaAmount),
                "目标段 = 10000");
        @SuppressWarnings("unchecked")
        Map<String, Object> forecast = (Map<String, Object>) pipeline.get("forecast");
        BigDecimal commitAmount = new BigDecimal(String.valueOf(forecast.get("commitAmount")));
        assertEquals(0, new BigDecimal("8000").compareTo(commitAmount),
                "预测段 commit = 8000");
        @SuppressWarnings("unchecked")
        Map<String, Object> actual = (Map<String, Object>) pipeline.get("actual");
        BigDecimal actualRevenue = new BigDecimal(String.valueOf(actual.get("actualRevenue")));
        assertEquals(0, new BigDecimal("4000").compareTo(actualRevenue),
                "实际段 = 4000");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> createChild(String parentId, String code, String name,
                                        String territoryType, String managerId) {
        return graphQLEngine.executeRpc(graphQLEngine.newRpcContext(
                mutation, "ErpCrmTerritory__createChild",
                ApiRequest.build(Map.of("parentId", parentId, "code", code, "name", name,
                        "territoryType", territoryType,
                        "managerId", managerId == null ? "" : managerId))));
    }

    private ApiResponse<?> moveTerritory(String territoryId, String newParentId) {
        return graphQLEngine.executeRpc(graphQLEngine.newRpcContext(
                mutation, "ErpCrmTerritory__moveTerritory",
                ApiRequest.build(Map.of("territoryId", territoryId, "newParentId", newParentId))));
    }

    private ApiResponse<?> assignLead(String leadId) {
        return graphQLEngine.executeRpc(graphQLEngine.newRpcContext(
                mutation, "ErpCrmLead__assignLead",
                ApiRequest.build(Map.of("leadId", leadId))));
    }

    private ApiResponse<?> finalizeQuota(String quotaId) {
        return graphQLEngine.executeRpc(graphQLEngine.newRpcContext(
                mutation, "ErpCrmQuota__finalizeQuota",
                ApiRequest.build(Map.of("quotaId", quotaId))));
    }

    private ApiResponse<?> unfinalizeQuota(String quotaId) {
        return graphQLEngine.executeRpc(graphQLEngine.newRpcContext(
                mutation, "ErpCrmQuota__unfinalizeQuota",
                ApiRequest.build(Map.of("quotaId", quotaId))));
    }

    // ---------- seed helpers ----------

    private void seedTerritory(String id, String code, String name, String parentId,
                                String territoryType, int level, String fullPath, boolean isLeaf) {
        IEntityDao<ErpCrmTerritory> dao = daoProvider.daoFor(ErpCrmTerritory.class);
        ErpCrmTerritory t = new ErpCrmTerritory();
        t.setId(id);
        t.setCode(code);
        t.setName(name);
        t.setOrgId(ORG_ID);
        t.setParentId(parentId);
        t.setTerritoryType(territoryType);
        t.setLevel(level);
        t.setFullPath(fullPath);
        t.setIsActive(Boolean.TRUE);
        t.setIsLeaf(isLeaf);
        t.setSortOrder(0);
        dao.saveEntity(t);
    }

    private void seedRule(String id, String name, int priority, String territoryId,
                          String conditionType, String conditionValue,
                          String assignmentMethod, String groupId, boolean isDefault) {
        IEntityDao<ErpCrmTerritoryAssignmentRule> dao = daoProvider.daoFor(ErpCrmTerritoryAssignmentRule.class);
        ErpCrmTerritoryAssignmentRule rule = new ErpCrmTerritoryAssignmentRule();
        rule.setId(id);
        rule.setOrgId(ORG_ID);
        rule.setRuleName(name);
        rule.setPriority(priority);
        rule.setTerritoryId(territoryId);
        rule.setConditionType(conditionType);
        rule.setConditionValue(conditionValue);
        rule.setAssignmentMethod(assignmentMethod);
        rule.setGroupId(groupId);
        rule.setIsDefault(isDefault);
        rule.setIsActive(Boolean.TRUE);
        dao.saveEntity(rule);
    }

    private void seedLead(String id, String code, String companyName, String department,
                          BigDecimal expectedRevenue, String utmSource, String territoryId) {
        ErpCrmLead lead = new ErpCrmLead();
        lead.setId(id);
        lead.setCode(code);
        lead.setOrgId(ORG_ID);
        lead.setLeadType(ErpCrmConstants.LEAD_TYPE_LEAD);
        lead.setDocStatus(ErpCrmConstants.DOC_STATUS_NEW);
        lead.setCompanyName(companyName);
        lead.setDepartment(department);
        lead.setExpectedRevenue(expectedRevenue);
        lead.setUtmSource(utmSource);
        lead.setTerritoryId(territoryId);
        daoProvider.daoFor(ErpCrmLead.class).saveEntity(lead);
    }

    private void seedQuota(String id, String territoryId, String teamId, String ownerId,
                            String periodType, int fiscalYear, String periodLabel,
                            BigDecimal amount, boolean finalized) {
        IEntityDao<ErpCrmQuota> dao = daoProvider.daoFor(ErpCrmQuota.class);
        ErpCrmQuota q = new ErpCrmQuota();
        q.setId(id);
        q.setOrgId(ORG_ID);
        q.setTerritoryId(territoryId);
        q.setTeamId(teamId);
        q.setOwnerId(ownerId);
        q.setPeriodType(periodType);
        q.setFiscalYear(fiscalYear);
        q.setPeriodLabel(periodLabel);
        q.setQuotaAmount(amount);
        q.setIsFinalized(finalized);
        dao.saveEntity(q);
    }

    // ---------- reload helpers ----------

    private ErpCrmTerritory reloadTerritory(String id) {
        return daoProvider.daoFor(ErpCrmTerritory.class).getEntityById(id);
    }

    private ErpCrmTerritory findTerritoryByCode(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        return daoProvider.daoFor(ErpCrmTerritory.class).findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private ErpCrmLead reloadLead(String id) {
        return daoProvider.daoFor(ErpCrmLead.class).getEntityById(id);
    }

    private ErpCrmQuota reloadQuota(String id) {
        return daoProvider.daoFor(ErpCrmQuota.class).getEntityById(id);
    }

    private List<ErpCrmQuota> listQuotasByTerritory(String territoryId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("territoryId", territoryId));
        return daoProvider.daoFor(ErpCrmQuota.class).findAllByQuery(q);
    }
}
