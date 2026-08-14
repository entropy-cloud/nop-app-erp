package app.erp.crm.service;

import app.erp.crm.dao.entity.ErpCrmForecast;
import app.erp.crm.dao.entity.ErpCrmForecastPeriod;
import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmStage;
import app.erp.crm.dao.entity.ErpCrmTerritory;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.isNull;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * CRM 销售预测 territory 级 rollup 矩阵测试（plan 2026-08-14-2304-1 RC-R1.25）。
 *
 * <p>覆盖 finding P1-RC-039 三个维度（区域行生成 / leaf-exact 无重复 / 管道闭合）：
 * <ul>
 *   <li>区域行生成：leaf-exact 直接归属聚合（父节点行不含子节点商机，行间不重叠）；</li>
 *   <li>无 territory 商机不进区域行但计入公司行；公司行恒等式 = Σ 区域行 + Σ（无 territory 且有 owner 商机）；</li>
 *   <li>区域行字段维度（territoryId 非空 + ownerId/teamId 空）；</li>
 *   <li>管道闭合：accumulatePipeline(T1) 子树查询恰命中区域行一次（无重复计数——leaf-exact 语义直接证据）。</li>
 * </ul>
 *
 * <p>注意：accumulatePipeline Forecast 段无 periodId/periodLabel 过滤（QuotaRollupCalculator:185-199），
 * 本测试断言依赖单期间测试 DB（territoryId 唯一于本类 seed，无跨测试污染）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCrmForecastTerritoryRollup extends JunitAutoTestCase {

    static final Long ORG_ID = 1301L;
    static final Long PERIOD_ID = 5900L;
    static final Long STAGE_QUALIFIED = 5301L;
    static final Long TERRITORY_T1 = 5801L;
    static final Long TERRITORY_T1_CHILD = 5802L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testRefreshForecastTerritoryRollup() {
        ormTemplate.runInSession(() -> {
            seedStage(STAGE_QUALIFIED, "STG-Q", "已验证", 10, 50);
            seedPeriod(PERIOD_ID, "2026-10", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                    ErpCrmConstants.FORECAST_PERIOD_STATUS_OPEN);
            // territory 树：T1（父）→ T1-1（子）
            seedTerritory(TERRITORY_T1, "T-ROOT-FC", "区域T1", null,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 0, "/T-ROOT-FC", false);
            seedTerritory(TERRITORY_T1_CHILD, "T-CHILD-FC", "子区T1-1", TERRITORY_T1,
                    ErpCrmConstants.TERRITORY_TYPE_AREA, 1, "/T-ROOT-FC/T-CHILD-FC", true);

            // 商机 A：直接归属 T1（父节点），probability=90（commit），expectedRevenue=1000
            seedOpportunity(5911L, "OPP-T-A", "userT1", null, TERRITORY_T1, 90,
                    new BigDecimal("1000"), LocalDate.of(2026, 10, 15), ErpCrmConstants.DOC_STATUS_QUALIFIED);
            // 商机 B：直接归属 T1 子节点 T1-1，probability=50（upside），expectedRevenue=2000
            seedOpportunity(5912L, "OPP-T-B", "userT2", null, TERRITORY_T1_CHILD, 50,
                    new BigDecimal("2000"), LocalDate.of(2026, 10, 20), ErpCrmConstants.DOC_STATUS_QUALIFIED);
            // 商机 C：无 territory（territoryId=null），probability=20（best_case），expectedRevenue=500
            seedOpportunity(5913L, "OPP-T-C", "userT1", null, null, 20,
                    new BigDecimal("500"), LocalDate.of(2026, 10, 25), ErpCrmConstants.DOC_STATUS_QUALIFIED);
        });

        assertEquals(0, refreshForecast(PERIOD_ID).getStatus(), "refreshForecast 应成功");

        // ① 区域行生成（leaf-exact 直接归属）：T1 行 = Σ(A)=1000，不含子节点 B；T1-1 行 = Σ(B)=2000，两行不重叠
        ErpCrmForecast t1 = reloadTerritoryForecast(PERIOD_ID, TERRITORY_T1);
        assertNotNull(t1, "T1 区域行已生成（有直接商机 A）");
        assertAmountEquals(new BigDecimal("1000"), t1.getCommitAmount(), "T1 commit = OPP-A(1000)，leaf-exact 不含子节点 B");
        assertAmountEquals(BigDecimal.ZERO, t1.getUpsideAmount(), "T1 upside = 0");
        assertAmountEquals(new BigDecimal("1000"), t1.getBestCaseAmount(), "T1 best = 1000");
        assertEquals(1, t1.getOpportunityCount(), "T1 商机数 = 1（OPP-A）");
        assertEquals(1, t1.getCommitOpportunityCount(), "T1 commit 商机数 = 1");

        ErpCrmForecast t1Child = reloadTerritoryForecast(PERIOD_ID, TERRITORY_T1_CHILD);
        assertNotNull(t1Child, "T1-1 区域行已生成（有直接商机 B）");
        assertAmountEquals(BigDecimal.ZERO, t1Child.getCommitAmount(), "T1-1 commit = 0");
        assertAmountEquals(new BigDecimal("2000"), t1Child.getUpsideAmount(), "T1-1 upside = OPP-B(2000)");
        assertAmountEquals(new BigDecimal("2000"), t1Child.getBestCaseAmount(), "T1-1 best = 2000");
        assertEquals(1, t1Child.getOpportunityCount(), "T1-1 商机数 = 1（OPP-B）");

        // ④ 区域行字段维度：territoryId 非空 + ownerId/teamId null
        assertEquals(TERRITORY_T1, t1.getTerritoryId(), "区域行 territoryId 非空");
        assertNull(t1.getOwnerId(), "区域行 ownerId=null");
        assertNull(t1.getTeamId(), "区域行 teamId=null");
        assertEquals(TERRITORY_T1_CHILD, t1Child.getTerritoryId(), "子节点区域行 territoryId 非空");

        // ② 无 territory 商机（C）不进区域行：期间内区域行恰 2 行（T1 + T1-1），无 territoryId=null 区域行
        assertEquals(2, countTerritoryForecasts(PERIOD_ID), "区域行恰 2 行（leaf-exact 仅直接归属节点）");

        // ③ 公司行 = Σ 全部商机（含无 territory C）= 1000+2000+500 = 3500；
        //    恒等式 = Σ 区域行 + 无 territory 商机 Σ = (1000+2000) + 500 = 3500
        ErpCrmForecast company = reloadCompanyForecast(PERIOD_ID);
        assertNotNull(company, "公司行已生成");
        assertAmountEquals(new BigDecimal("1000"), company.getCommitAmount(), "公司 commit = 1000");
        assertAmountEquals(new BigDecimal("2000"), company.getUpsideAmount(), "公司 upside = 2000");
        assertAmountEquals(new BigDecimal("3500"), company.getBestCaseAmount(),
                "公司 best = A+B+C = 3500 = Σ 区域行(3000) + 无 territory C(500)");
        assertEquals(3, company.getOpportunityCount(), "公司商机数 = 3");
        assertNull(company.getTerritoryId(), "公司行 territoryId=null");
        assertNull(company.getTeamId(), "公司行 teamId=null");
        assertNull(company.getOwnerId(), "公司行 ownerId=null");
    }

    @Test
    public void testTerritoryPipelineClosureNoDoubleCount() {
        ormTemplate.runInSession(() -> {
            seedStage(STAGE_QUALIFIED, "STG-Q", "已验证", 10, 50);
            seedPeriod(PERIOD_ID, "2026-10", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                    ErpCrmConstants.FORECAST_PERIOD_STATUS_OPEN);
            seedTerritory(TERRITORY_T1, "T-ROOT-FC", "区域T1", null,
                    ErpCrmConstants.TERRITORY_TYPE_REGION, 0, "/T-ROOT-FC", false);
            seedTerritory(TERRITORY_T1_CHILD, "T-CHILD-FC", "子区T1-1", TERRITORY_T1,
                    ErpCrmConstants.TERRITORY_TYPE_AREA, 1, "/T-ROOT-FC/T-CHILD-FC", true);
            seedOpportunity(5911L, "OPP-T-A", "userT1", null, TERRITORY_T1, 90,
                    new BigDecimal("1000"), LocalDate.of(2026, 10, 15), ErpCrmConstants.DOC_STATUS_QUALIFIED);
            seedOpportunity(5912L, "OPP-T-B", "userT2", null, TERRITORY_T1_CHILD, 50,
                    new BigDecimal("2000"), LocalDate.of(2026, 10, 20), ErpCrmConstants.DOC_STATUS_QUALIFIED);
            seedOpportunity(5913L, "OPP-T-C", "userT1", null, null, 20,
                    new BigDecimal("500"), LocalDate.of(2026, 10, 25), ErpCrmConstants.DOC_STATUS_QUALIFIED);
        });

        assertEquals(0, refreshForecast(PERIOD_ID).getStatus(), "refreshForecast 应成功");

        // 管道闭合：accumulatePipeline(T1) 子树 in 查询 {T1, T1-1} 恰命中两区域行各一次
        // → 子树总额 = T1 行 + T1-1 行 = A+B（无重复计数，leaf-exact 直接证据；A4.2.93 缺口闭合）
        ApiResponse<?> resp = graphQLEngine.executeRpc(graphQLEngine.newRpcContext(
                query, "ErpCrmQuota__getTerritoryPipeline",
                ApiRequest.build(Map.of("territoryId", TERRITORY_T1, "periodLabel", "2026-10"))));
        assertEquals(0, resp.getStatus(), "getTerritoryPipeline 应成功");
        @SuppressWarnings("unchecked")
        Map<String, Object> pipeline = (Map<String, Object>) resp.getData();
        assertNotNull(pipeline, "返回非空");
        assertNotNull(pipeline.get("forecast"), "预测段非空");
        @SuppressWarnings("unchecked")
        Map<String, Object> forecast = (Map<String, Object>) pipeline.get("forecast");
        assertAmountEquals(new BigDecimal("1000"), new BigDecimal(String.valueOf(forecast.get("commitAmount"))),
                "pipeline(T1) commit = T1 行(A) + T1-1 行(0) = 1000，无重复计数");
        assertAmountEquals(new BigDecimal("2000"), new BigDecimal(String.valueOf(forecast.get("upsideAmount"))),
                "pipeline(T1) upside = T1 行(0) + T1-1 行(B) = 2000，无重复计数");
        assertAmountEquals(new BigDecimal("3000"), new BigDecimal(String.valueOf(forecast.get("bestCaseAmount"))),
                "pipeline(T1) best = A+B = 3000（父行+子行恰一次，非 A+2B 双计）");
        assertEquals(2, Integer.parseInt(String.valueOf(forecast.get("opportunityCount"))),
                "pipeline(T1) 商机数 = 2（A+B，无重复计数）");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> refreshForecast(Long periodId) {
        return graphQLEngine.executeRpc(graphQLEngine.newRpcContext(
                mutation, "ErpCrmForecast__refreshForecast", ApiRequest.build(Map.of("periodId", periodId))));
    }

    // ---------- seed helpers ----------

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

    private void seedOpportunity(Long id, String code, String ownerId, Long teamId, Long territoryId,
                                 int probability, BigDecimal expectedRevenue,
                                 LocalDate expectedCloseDate, String docStatus) {
        ErpCrmLead opp = new ErpCrmLead();
        opp.setId(id);
        opp.setCode(code);
        opp.setOrgId(ORG_ID);
        opp.setLeadType(ErpCrmConstants.LEAD_TYPE_OPPORTUNITY);
        opp.setDocStatus(docStatus);
        opp.setOwnerId(ownerId);
        opp.setTeamId(teamId);
        opp.setTerritoryId(territoryId);
        opp.setProbability(probability);
        opp.setExpectedRevenue(expectedRevenue);
        opp.setExpectedCloseDate(expectedCloseDate);
        opp.setStageId(STAGE_QUALIFIED);
        daoProvider.daoFor(ErpCrmLead.class).saveEntity(opp);
    }

    private void seedPeriod(Long id, String label, LocalDate start, LocalDate end, String status) {
        IEntityDao<ErpCrmForecastPeriod> dao = daoProvider.daoFor(ErpCrmForecastPeriod.class);
        ErpCrmForecastPeriod period = new ErpCrmForecastPeriod();
        period.setId(id);
        period.setCode("PER-" + id);
        period.setOrgId(ORG_ID);
        period.setPeriodType("MONTHLY");
        period.setPeriodStart(start);
        period.setPeriodEnd(end);
        period.setLabel(label);
        period.setStatus(status);
        period.setIsCurrent(Boolean.TRUE);
        dao.saveEntity(period);
    }

    // ---------- reload helpers ----------

    private ErpCrmForecast reloadTerritoryForecast(Long periodId, Long territoryId) {
        IEntityDao<ErpCrmForecast> dao = daoProvider.daoFor(ErpCrmForecast.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", periodId));
        q.addFilter(eq("territoryId", territoryId));
        q.addFilter(isNull("ownerId"));
        q.addFilter(isNull("teamId"));
        q.setLimit(1);
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private ErpCrmForecast reloadCompanyForecast(Long periodId) {
        IEntityDao<ErpCrmForecast> dao = daoProvider.daoFor(ErpCrmForecast.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", periodId));
        q.addFilter(isNull("ownerId"));
        q.addFilter(isNull("teamId"));
        q.addFilter(isNull("territoryId"));
        q.setLimit(1);
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private int countTerritoryForecasts(Long periodId) {
        IEntityDao<ErpCrmForecast> dao = daoProvider.daoFor(ErpCrmForecast.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", periodId));
        q.addFilter(io.nop.api.core.beans.FilterBeans.notNull("territoryId"));
        return dao.findAllByQuery(q).size();
    }

    private void assertAmountEquals(BigDecimal expected, BigDecimal actual, String message) {
        assertEquals(0, expected.compareTo(actual), message + " (expected=" + expected + ", actual=" + actual + ")");
    }
}
