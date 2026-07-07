package app.erp.crm.service;

import app.erp.crm.dao.entity.ErpCrmForecast;
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

    static final Long ORG_ID = 1301L;
    static final Long ROOT_TERRITORY_ID = 6001L;
    static final Long REGION_HUADONG_ID = 6002L;
    static final Long AREA_SHANGHAI_ID = 6003L;
    static final Long TEAM_PUDONG_ID = 6004L;

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
            seedTerritory(7001L, "T-ROOT", "公司", null,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 0, "/", true);
            seedTerritory(7002L, "T-HUABEI", "华北", 7001L,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 1, "/T-HUABEI", true);
            seedTerritory(7003L, "T-HUADONG", "华东", 7001L,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 1, "/T-HUADONG", true);
            seedTerritory(7004L, "T-SHANGHAI", "上海", 7003L,
                    ErpCrmConstants.TERRITORY_TYPE_AREA, 2, "/T-HUADONG/T-SHANGHAI", true);
        });

        // 移动 SHANGHAI 从 HUADONG 到 HUABEI
        ApiResponse<?> resp = moveTerritory(7004L, 7002L);
        assertEquals(0, resp.getStatus(), "moveTerritory 应成功");

        ErpCrmTerritory moved = reloadTerritory(7004L);
        assertEquals(7002L, moved.getParentId(), "parentId 重指向新父");
        assertEquals("/T-HUABEI/T-SHANGHAI", moved.getFullPath(), "fullPath 重算");
        assertEquals(2, moved.getLevel(), "level 保持与新父对应");
    }

    @Test
    public void testMoveTerritoryRejectsCycle() {
        ormTemplate.runInSession(() -> {
            seedTerritory(7101L, "T-ROOT-B", "公司B", null,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 0, "/", true);
            seedTerritory(7102L, "T-CHILD-B", "子B", 7101L,
                    ErpCrmConstants.TERRITORY_TYPE_AREA, 1, "/T-ROOT-B/T-CHILD-B", true);
        });
        // 把 ROOT 移到自己的子节点下 → 成环
        ApiResponse<?> resp = moveTerritory(7101L, 7102L);
        assertEquals(ErpCrmErrors.ERR_TERRITORY_CYCLE.getErrorCode(), resp.getCode(),
                "成环 → ERR_TERRITORY_CYCLE");
    }

    @Test
    public void testMaxDepthExceededRejected() {
        ormTemplate.runInSession(() -> {
            seedTerritory(7201L, "T-ROOT-C", "公司C", null,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 0, "/", true);
            seedTerritory(7202L, "T-L1", "L1", 7201L,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 1, "/T-ROOT-C/T-L1", true);
            seedTerritory(7203L, "T-L2", "L2", 7202L,
                    ErpCrmConstants.TERRITORY_TYPE_AREA, 2, "/T-ROOT-C/T-L1/T-L2", true);
            seedTerritory(7204L, "T-L3", "L3", 7203L,
                    ErpCrmConstants.TERRITORY_TYPE_BRANCH, 3, "/T-ROOT-C/T-L1/T-L2/T-L3", true);
        });
        // 在 level=3 节点下建子 → level=4，max-depth=4 允许；但建下一层 level=5 应拒绝
        ApiResponse<?> ok = createChild(7204L, "T-L4-OK", "L4OK",
                ErpCrmConstants.TERRITORY_TYPE_TEAM, null);
        assertEquals(0, ok.getStatus(), "level=4 ≤ max-depth=4 应允许");
        Long level4Id = findTerritoryByCode("T-L4-OK").getId();
        // 在 level=4 节点下建子 → level=5，超过 max-depth=4
        ApiResponse<?> bad = createChild(level4Id, "T-L5-BAD", "L5BAD",
                ErpCrmConstants.TERRITORY_TYPE_TEAM, null);
        assertEquals(ErpCrmErrors.ERR_TERRITORY_MAX_DEPTH_EXCEEDED.getErrorCode(), bad.getCode(),
                "level=5 > max-depth=4 → ERR_TERRITORY_MAX_DEPTH_EXCEEDED");
    }

    @Test
    public void testDeleteRejectsWhenHasChildren() {
        ormTemplate.runInSession(() -> {
            seedTerritory(7301L, "T-ROOT-D", "公司D", null,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 0, "/", false);
            seedTerritory(7302L, "T-CHILD-D", "子D", 7301L,
                    ErpCrmConstants.TERRITORY_TYPE_AREA, 1, "/T-ROOT-D/T-CHILD-D", true);
        });
        ApiResponse<?> resp = graphQLEngine.executeRpc(
                graphQLEngine.newRpcContext(mutation, "ErpCrmTerritory__delete",
                        ApiRequest.build(Map.of("id", String.valueOf(7301L)))));
        assertEquals(ErpCrmErrors.ERR_TERRITORY_HAS_CHILDREN.getErrorCode(), resp.getCode(),
                "有子节点禁删 → ERR_TERRITORY_HAS_CHILDREN");
    }

    // ---------- 分配引擎 ----------

    @Test
    public void testAssignmentEngineAllConditionTypes() {
        Long territoryIdGeo = 8001L;
        Long territoryIdIndustry = 8002L;
        Long territoryIdSize = 8003L;
        Long territoryIdCustom = 8004L;
        Long defaultTerritoryId = 8005L;
        ormTemplate.runInSession(() -> {
            seedTerritory(8000L, "T-ROOT-E", "公司E", null,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 0, "/", true);
            seedTerritory(territoryIdGeo, "T-GEO", "地理区", 8000L,
                    ErpCrmConstants.TERRITORY_TYPE_AREA, 1, "/T-ROOT-E/T-GEO", true);
            seedTerritory(territoryIdIndustry, "T-IND", "行业区", 8000L,
                    ErpCrmConstants.TERRITORY_TYPE_AREA, 1, "/T-ROOT-E/T-IND", true);
            seedTerritory(territoryIdSize, "T-SIZE", "规模区", 8000L,
                    ErpCrmConstants.TERRITORY_TYPE_AREA, 1, "/T-ROOT-E/T-SIZE", true);
            seedTerritory(territoryIdCustom, "T-CUST", "自定义区", 8000L,
                    ErpCrmConstants.TERRITORY_TYPE_AREA, 1, "/T-ROOT-E/T-CUST", true);
            seedTerritory(defaultTerritoryId, "T-DEF", "默认区", 8000L,
                    ErpCrmConstants.TERRITORY_TYPE_AREA, 1, "/T-ROOT-E/T-DEF", true);

            seedRule(8101L, "GEO-RULE", 10, territoryIdGeo,
                    ErpCrmConstants.ASSIGNMENT_CONDITION_GEOGRAPHY,
                    "{\"province\":[\"上海\"]}",
                    ErpCrmConstants.ASSIGNMENT_METHOD_MANUAL, null, false);
            seedRule(8102L, "IND-RULE", 20, territoryIdIndustry,
                    ErpCrmConstants.ASSIGNMENT_CONDITION_INDUSTRY,
                    "{\"industryCode\":[\"manufacturing\"]}",
                    ErpCrmConstants.ASSIGNMENT_METHOD_MANUAL, null, false);
            seedRule(8103L, "SIZE-RULE", 30, territoryIdSize,
                    ErpCrmConstants.ASSIGNMENT_CONDITION_CUSTOMER_SIZE,
                    "{\"minEmployees\":1000,\"maxEmployees\":100000}",
                    ErpCrmConstants.ASSIGNMENT_METHOD_MANUAL, null, false);
            seedRule(8104L, "CUST-RULE", 40, territoryIdCustom,
                    ErpCrmConstants.ASSIGNMENT_CONDITION_CUSTOM_FIELD,
                    "{\"utmSource\":\"baidu\"}",
                    ErpCrmConstants.ASSIGNMENT_METHOD_MANUAL, null, false);
            seedRule(8105L, "DEFAULT-RULE", 100, defaultTerritoryId,
                    ErpCrmConstants.ASSIGNMENT_CONDITION_CUSTOM_FIELD,
                    "{}",
                    ErpCrmConstants.ASSIGNMENT_METHOD_MANUAL, null, true);
        });

        // GEOGRAPHY 命中
        Long geoLead = 8201L;
        ormTemplate.runInSession(() -> seedLead(geoLead, "LEAD-GEO-001", "上海某制造", "manufacturing",
                new BigDecimal("500"), "baidu", null));
        assertEquals(0, assignLead(geoLead).getStatus(), "GEOGRAPHY assignLead 应成功");
        assertEquals(territoryIdGeo, reloadLead(geoLead).getTerritoryId(),
                "GEOGRAPHY 命中 → territoryId=geo");

        // INDUSTRY 命中（无 province 关键词）
        Long indLead = 8202L;
        ormTemplate.runInSession(() -> seedLead(indLead, "LEAD-IND-001", "某公司", "manufacturing",
                new BigDecimal("500"), null, null));
        assertEquals(0, assignLead(indLead).getStatus(), "INDUSTRY assignLead 应成功");
        assertEquals(territoryIdIndustry, reloadLead(indLead).getTerritoryId(),
                "INDUSTRY 命中 → territoryId=industry");

        // CUSTOMER_SIZE 命中
        Long sizeLead = 8203L;
        ormTemplate.runInSession(() -> seedLead(sizeLead, "LEAD-SIZE-001", "无关键地名", "其他",
                new BigDecimal("5000"), null, null));
        assertEquals(0, assignLead(sizeLead).getStatus(), "SIZE assignLead 应成功");
        assertEquals(territoryIdSize, reloadLead(sizeLead).getTerritoryId(),
                "CUSTOMER_SIZE 命中 → territoryId=size");

        // CUSTOM_FIELD 命中（utmSource=baidu，SIZE 不在范围）
        Long custLead = 8204L;
        ormTemplate.runInSession(() -> seedLead(custLead, "LEAD-CUST-001", "无关键地名", "其他",
                new BigDecimal("10"), "baidu", null));
        assertEquals(0, assignLead(custLead).getStatus(), "CUST assignLead 应成功");
        assertEquals(territoryIdCustom, reloadLead(custLead).getTerritoryId(),
                "CUSTOM_FIELD 命中 → territoryId=custom");
    }

    @Test
    public void testAssignmentDefaultFallbackAndNoMatch() {
        Long defaultTerritoryId = 8301L;
        ormTemplate.runInSession(() -> {
            seedTerritory(8300L, "T-ROOT-F", "公司F", null,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 0, "/", true);
            seedTerritory(defaultTerritoryId, "T-DEF-F", "默认F", 8300L,
                    ErpCrmConstants.TERRITORY_TYPE_AREA, 1, "/T-ROOT-F/T-DEF-F", true);
            seedRule(8311L, "GEO-RULE-F", 10, defaultTerritoryId,
                    ErpCrmConstants.ASSIGNMENT_CONDITION_GEOGRAPHY,
                    "{\"province\":[\"上海\"]}",
                    ErpCrmConstants.ASSIGNMENT_METHOD_MANUAL, null, false);
            seedRule(8312L, "DEFAULT-RULE-F", 100, defaultTerritoryId,
                    ErpCrmConstants.ASSIGNMENT_CONDITION_CUSTOM_FIELD,
                    "{}",
                    ErpCrmConstants.ASSIGNMENT_METHOD_MANUAL, null, true);
        });

        // 无 GEOGRAPHY 命中 → 走 default
        Long leadId = 8401L;
        ormTemplate.runInSession(() -> seedLead(leadId, "LEAD-DEF-001", "无地名", "其他",
                new BigDecimal("10"), null, null));
        assertEquals(0, assignLead(leadId).getStatus(), "default assignLead 应成功");
        assertEquals(defaultTerritoryId, reloadLead(leadId).getTerritoryId(),
                "无匹配 GEOGRAPHY → 走 default rule");
    }

    @Test
    public void testReassignLeadOverridesEngine() {
        Long territoryA = 8501L;
        Long territoryB = 8502L;
        Long teamId = 8601L;
        String ownerId = "userZZ";
        ormTemplate.runInSession(() -> {
            seedTerritory(8500L, "T-ROOT-G", "公司G", null,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 0, "/", true);
            seedTerritory(territoryA, "T-A", "区A", 8500L,
                    ErpCrmConstants.TERRITORY_TYPE_AREA, 1, "/T-ROOT-G/T-A", true);
            seedTerritory(territoryB, "T-B", "区B", 8500L,
                    ErpCrmConstants.TERRITORY_TYPE_AREA, 1, "/T-ROOT-G/T-B", true);
            seedLead(8701L, "LEAD-REASSIGN-001", "公司无配", "其他",
                    new BigDecimal("100"), null, territoryA);
        });
        ApiResponse<?> resp = graphQLEngine.executeRpc(graphQLEngine.newRpcContext(
                mutation, "ErpCrmLead__reassignLead",
                ApiRequest.build(Map.of("leadId", 8701L,
                        "territoryId", territoryB, "teamId", teamId, "ownerId", ownerId))));
        assertEquals(0, resp.getStatus(), "reassignLead 应成功");
        ErpCrmLead lead = reloadLead(8701L);
        assertEquals(territoryB, lead.getTerritoryId(), "territoryId 被覆盖");
        assertEquals(teamId, lead.getTeamId(), "teamId 被设置");
        assertEquals(ownerId, lead.getOwnerId(), "ownerId 被设置");
    }

    // ---------- 配额管理 ----------

    @Test
    public void testQuotaRollupExplicitValuePriorityAndAggregate() {
        Long company = 9001L;
        Long regionId = 9002L;
        Long teamId = 9101L;
        ormTemplate.runInSession(() -> {
            seedTerritory(company, "T-ROOT-Q", "公司Q", null,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 0, "/", false);
            seedTerritory(regionId, "T-REG-Q", "区Q", company,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 1, "/T-ROOT-Q/T-REG-Q", false);

            // 团队级显式配额
            seedQuota(9201L, regionId, teamId, null,
                    ErpCrmConstants.QUOTA_PERIOD_QUARTERLY, 2026, "2026-Q3",
                    new BigDecimal("1000"), false);
            // 个人级配额（区域子节点）
            seedQuota(9202L, regionId, teamId, "userQ1",
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
        Long quotaId = 9301L;
        ormTemplate.runInSession(() -> {
            seedTerritory(9300L, "T-ROOT-Q2", "公司Q2", null,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 0, "/", true);
            seedQuota(quotaId, 9300L, null, null,
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
        Long quotaId = 9401L;
        ormTemplate.runInSession(() -> {
            seedTerritory(9400L, "T-ROOT-Q3", "公司Q3", null,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 0, "/", true);
            seedQuota(quotaId, 9400L, null, null,
                    ErpCrmConstants.QUOTA_PERIOD_ANNUAL, 2026, "2026",
                    new BigDecimal("1200"), false);
        });

        ApiResponse<?> resp = graphQLEngine.executeRpc(graphQLEngine.newRpcContext(
                mutation, "ErpCrmQuota__distributeAnnualQuota",
                ApiRequest.build(Map.of("quotaId", quotaId,
                        "periodType", ErpCrmConstants.QUOTA_PERIOD_QUARTERLY))));
        assertEquals(0, resp.getStatus(), "distributeAnnualQuota 应成功");
        // 验证 4 个季度配额行已生成
        List<ErpCrmQuota> subs = listQuotasByTerritory(9400L);
        assertEquals(4 + 1, subs.size(), "原 1 行 + 新 4 季度行 = 5 行");
        BigDecimal each = new BigDecimal("300.00");
        boolean hasExpected = subs.stream().anyMatch(q ->
                ErpCrmConstants.QUOTA_PERIOD_QUARTERLY.equals(q.getPeriodType())
                        && each.compareTo(q.getQuotaAmount()) == 0);
        assertTrue(hasExpected, "均分 1200/4=300/季");
    }

    @Test
    public void testDistributeAnnualRejectsFinalized() {
        Long quotaId = 9501L;
        ormTemplate.runInSession(() -> {
            seedTerritory(9500L, "T-ROOT-Q4", "公司Q4", null,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 0, "/", true);
            seedQuota(quotaId, 9500L, null, null,
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
        Long territoryId = 9601L;
        Long teamId = 9602L;
        String ownerId = "userP";
        String periodLabel = "2026-Q3";
        ormTemplate.runInSession(() -> {
            seedTerritory(9600L, "T-ROOT-P", "公司P", null,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 0, "/", false);
            seedTerritory(territoryId, "T-REG-P", "区P", 9600L,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 1, "/T-ROOT-P/T-REG-P", false);

            // Quota：目标段
            seedQuota(9611L, territoryId, teamId, ownerId,
                    ErpCrmConstants.QUOTA_PERIOD_QUARTERLY, 2026, periodLabel,
                    new BigDecimal("10000"), false);

            // Forecast：预测段
            ErpCrmForecast forecast = new ErpCrmForecast();
            forecast.setId(9621L);
            forecast.setOrgId(ORG_ID);
            forecast.setPeriodId(9999L);
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
            lead.setId(9631L);
            lead.setCode("OPP-P-001");
            lead.setOrgId(ORG_ID);
            lead.setLeadType(ErpCrmConstants.LEAD_TYPE_OPPORTUNITY);
            lead.setDocStatus(ErpCrmConstants.DOC_STATUS_CONVERTED);
            lead.setTerritoryId(territoryId);
            lead.setExpectedRevenue(new BigDecimal("4000"));
            daoProvider.daoFor(ErpCrmLead.class).saveEntity(lead);
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

    private ApiResponse<?> createChild(Long parentId, String code, String name,
                                        String territoryType, Long managerId) {
        return graphQLEngine.executeRpc(graphQLEngine.newRpcContext(
                mutation, "ErpCrmTerritory__createChild",
                ApiRequest.build(Map.of("parentId", parentId, "code", code, "name", name,
                        "territoryType", territoryType,
                        "managerId", managerId == null ? "" : managerId))));
    }

    private ApiResponse<?> moveTerritory(Long territoryId, Long newParentId) {
        return graphQLEngine.executeRpc(graphQLEngine.newRpcContext(
                mutation, "ErpCrmTerritory__moveTerritory",
                ApiRequest.build(Map.of("territoryId", territoryId, "newParentId", newParentId))));
    }

    private ApiResponse<?> assignLead(Long leadId) {
        return graphQLEngine.executeRpc(graphQLEngine.newRpcContext(
                mutation, "ErpCrmLead__assignLead",
                ApiRequest.build(Map.of("leadId", leadId))));
    }

    private ApiResponse<?> finalizeQuota(Long quotaId) {
        return graphQLEngine.executeRpc(graphQLEngine.newRpcContext(
                mutation, "ErpCrmQuota__finalizeQuota",
                ApiRequest.build(Map.of("quotaId", quotaId))));
    }

    private ApiResponse<?> unfinalizeQuota(Long quotaId) {
        return graphQLEngine.executeRpc(graphQLEngine.newRpcContext(
                mutation, "ErpCrmQuota__unfinalizeQuota",
                ApiRequest.build(Map.of("quotaId", quotaId))));
    }

    // ---------- seed helpers ----------

    private void seedTerritory(Long id, String code, String name, Long parentId,
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

    private void seedRule(Long id, String name, int priority, Long territoryId,
                          String conditionType, String conditionValue,
                          String assignmentMethod, Long groupId, boolean isDefault) {
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

    private void seedLead(Long id, String code, String companyName, String department,
                          BigDecimal expectedRevenue, String utmSource, Long territoryId) {
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

    private void seedQuota(Long id, Long territoryId, Long teamId, String ownerId,
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

    private ErpCrmTerritory reloadTerritory(Long id) {
        return daoProvider.daoFor(ErpCrmTerritory.class).getEntityById(id);
    }

    private ErpCrmTerritory findTerritoryByCode(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        return daoProvider.daoFor(ErpCrmTerritory.class).findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private ErpCrmLead reloadLead(Long id) {
        return daoProvider.daoFor(ErpCrmLead.class).getEntityById(id);
    }

    private ErpCrmQuota reloadQuota(Long id) {
        return daoProvider.daoFor(ErpCrmQuota.class).getEntityById(id);
    }

    private List<ErpCrmQuota> listQuotasByTerritory(Long territoryId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("territoryId", territoryId));
        return daoProvider.daoFor(ErpCrmQuota.class).findAllByQuery(q);
    }
}
