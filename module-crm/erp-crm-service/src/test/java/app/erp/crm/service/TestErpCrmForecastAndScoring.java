package app.erp.crm.service;

import app.erp.crm.dao.entity.ErpCrmForecast;
import app.erp.crm.dao.entity.ErpCrmForecastAccuracy;
import app.erp.crm.dao.entity.ErpCrmForecastLine;
import app.erp.crm.dao.entity.ErpCrmForecastPeriod;
import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmLeadScore;
import app.erp.crm.dao.entity.ErpCrmLeadScoreConfig;
import app.erp.crm.dao.entity.ErpCrmLeadScoreConfigLine;
import app.erp.crm.dao.entity.ErpCrmLeadScoreLine;
import app.erp.crm.dao.entity.ErpCrmStage;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
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
 * CRM 线索评分 + 销售预测端到端测试（plan 2026-07-04-0700-1 Phase 2 + Phase 3 Proof）。
 *
 * <p>经 {@link IGraphQLEngine} 调评分引擎 + 预测引擎 + 期间状态机，覆盖：
 * 评分 LOOKUP+BOOLEAN 准则归一化 + auto-qualify；预测 refreshForecast 单 owner commit/upside/best-case 分类 +
 * 层级 rollup + ForecastLine 快照；期间 OPEN→CLOSED 准确率；FROZEN 拒绝重算。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCrmForecastAndScoring extends JunitAutoTestCase {

    static final Long ORG_ID = 1301L;
    static final Long PERIOD_ID = 5101L;
    static final Long SCORE_CONFIG_ID = 5201L;
    static final Long STAGE_QUALIFIED = 5301L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testScoringAndAutoQualify() {
        Long leadId = 5001L;
        ormTemplate.runInSession(() -> {
            seedStage(STAGE_QUALIFIED, "STG-Q", "已验证", 10, 50);
            seedScoreConfig(SCORE_CONFIG_ID, "标准评分", 70, 30);
            // LOOKUP 准则：jobTitle 匹配 C-level 得 15 分
            seedConfigLine(5211L, SCORE_CONFIG_ID, "JOB_TITLE", "职位层级", 50,
                    ErpCrmConstants.SCORING_METHOD_LOOKUP, "jobTitle",
                    "[{\"value\":\"C-level\",\"label\":\"C-level\",\"score\":15},{\"value\":\"Manager\",\"label\":\"经理\",\"score\":5}]",
                    null, 15, 10);
            // BOOLEAN 准则：companyName 非空匹配 → maxScore
            seedConfigLine(5212L, SCORE_CONFIG_ID, "COMPANY_NAME", "公司名称", 50,
                    ErpCrmConstants.SCORING_METHOD_BOOLEAN, "companyName",
                    "[{\"value\":\"Acme Corp\"}]", null, 10, 20);

            ErpCrmLead lead = newLead(leadId, "LEAD-SCORE-001", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_NEW);
            lead.setJobTitle("C-level");
            lead.setCompanyName("Acme Corp");
            daoProvider.daoFor(ErpCrmLead.class).saveEntity(lead);
        });

        // recalculateScore：LOOKUP(C-level=15) + BOOLEAN(companyName match=10) → 归一化 totalScore
        // weightedScore: LOOKUP = 15×50=750, BOOLEAN = 10×50=500
        // maxWeighted: 15×50=750, 10×50=500 → totalScore = (750+500)/(750+500)×100 = 100
        // 但 LOOKUP rawScore=15=maxScore → 满分；BOOLEAN rawScore=10=maxScore → 满分 → totalScore=100
        ApiResponse<?> resp = recalculateScore(leadId, ErpCrmConstants.TRIGGER_EVENT_MANUAL);
        assertEquals(0, resp.getStatus(), "recalculateScore 应成功");

        ErpCrmLeadScore score = reloadScore(leadId);
        assertNotNull(score, "评分记录已创建");
        assertEquals(100, score.getTotalScore(), "两条准则均满分 → totalScore=100");
        assertEquals(ErpCrmConstants.TRIGGERED_ACTION_AUTO_QUALIFY, score.getTriggeredAction(),
                "totalScore(100) ≥ autoQualifyThreshold(70) 且 LEAD+NEW → AUTO_QUALIFY");
        assertTrue(score.getAutoQualified(), "autoQualified=true");

        // auto-qualify 触发：Lead docStatus NEW → QUALIFIED
        ErpCrmLead lead = reloadLead(leadId);
        assertEquals(ErpCrmConstants.DOC_STATUS_QUALIFIED, lead.getDocStatus(),
                "评分 auto-qualify → Lead docStatus NEW → QUALIFIED");

        // append-only：再次评分生成新记录（不覆盖）
        assertEquals(0, recalculateScore(leadId, ErpCrmConstants.TRIGGER_EVENT_MANUAL).getStatus(),
                "再次 recalculateScore 应成功");
        List<ErpCrmLeadScore> scores = loadScores(leadId);
        assertEquals(2, scores.size(), "append-only：第二次评分生成新记录（不覆盖）");

        // 行级快照
        List<ErpCrmLeadScoreLine> lines = loadScoreLines(scores.get(0).getId());
        assertEquals(2, lines.size(), "2 条评分准则 = 2 行快照");
    }

    @Test
    public void testNoActiveConfigReturnsNull() {
        Long leadId = 5002L;
        ormTemplate.runInSession(() -> {
            ErpCrmLead lead = newLead(leadId, "LEAD-NOSCORE-001", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_NEW);
            daoProvider.daoFor(ErpCrmLead.class).saveEntity(lead);
        });
        // 无 active config → recalculateScore 返回 null（不抛错）
        ApiResponse<?> resp = recalculateScore(leadId, ErpCrmConstants.TRIGGER_EVENT_MANUAL);
        assertEquals(0, resp.getStatus(), "无 active config 时 recalculateScore 应成功（不阻断）");
        assertNull(reloadScore(leadId), "无 active config → 无评分记录");
    }

    @Test
    public void testRefreshForecastAndRollup() {
        Long ownerA = 5401L;
        Long ownerB = 5402L;
        Long teamId = 5501L;
        ormTemplate.runInSession(() -> {
            seedStage(STAGE_QUALIFIED, "STG-Q", "已验证", 10, 50);
            seedPeriod(PERIOD_ID, "2026-07", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                    ErpCrmConstants.FORECAST_PERIOD_STATUS_OPEN);

            // 商机 A：probability=90（commit），expectedRevenue=1000，owner=userA，teamId
            seedOpportunity(5411L, "OPP-FC-A", "userA", teamId, 90, new BigDecimal("1000"),
                    LocalDate.of(2026, 7, 15), ErpCrmConstants.DOC_STATUS_QUALIFIED);
            // 商机 B：probability=50（upside），expectedRevenue=2000，owner=userB，teamId
            seedOpportunity(5412L, "OPP-FC-B", "userB", teamId, 50, new BigDecimal("2000"),
                    LocalDate.of(2026, 7, 20), ErpCrmConstants.DOC_STATUS_QUALIFIED);
            // 商机 C：probability=20（best_case，不计 commit/upside），owner=userA
            seedOpportunity(5413L, "OPP-FC-C", "userA", teamId, 20, new BigDecimal("500"),
                    LocalDate.of(2026, 7, 25), ErpCrmConstants.DOC_STATUS_QUALIFIED);
        });

        assertEquals(0, refreshForecast(PERIOD_ID).getStatus(), "refreshForecast 应成功");

        // userA 个人预测：commit(90≥80)=1000 + upside=0 + best=1000+500=1500
        ErpCrmForecast userA = reloadForecast(PERIOD_ID, "userA");
        assertNotNull(userA, "userA 个人预测已创建");
        assertAmountEquals(new BigDecimal("1000"), userA.getCommitAmount(), "userA commit = OPP-A(1000)");
        assertAmountEquals(BigDecimal.ZERO, userA.getUpsideAmount(), "userA upside = 0");
        assertAmountEquals(new BigDecimal("1500"), userA.getBestCaseAmount(), "userA best = OPP-A + OPP-C = 1500");
        assertEquals(2, userA.getOpportunityCount(), "userA 商机数 = 2（OPP-A + OPP-C）");
        assertEquals(1, userA.getCommitOpportunityCount(), "userA commit 商机数 = 1（OPP-A）");

        // userB 个人预测：commit=0 + upside(50>=30)=2000 + best=2000
        ErpCrmForecast userB = reloadForecast(PERIOD_ID, "userB");
        assertNotNull(userB, "userB 个人预测已创建");
        assertAmountEquals(BigDecimal.ZERO, userB.getCommitAmount(), "userB commit = 0");
        assertAmountEquals(new BigDecimal("2000"), userB.getUpsideAmount(), "userB upside = OPP-B(2000)");
        assertAmountEquals(new BigDecimal("2000"), userB.getBestCaseAmount(), "userB best = OPP-B = 2000");

        // 团队 rollup：commit=1000+0=1000, upside=0+2000=2000, best=1500+2000=3500
        ErpCrmForecast teamFc = reloadTeamForecast(PERIOD_ID, teamId);
        assertNotNull(teamFc, "团队 rollup 预测已创建");
        assertAmountEquals(new BigDecimal("1000"), teamFc.getCommitAmount(), "team commit = sum");
        assertAmountEquals(new BigDecimal("2000"), teamFc.getUpsideAmount(), "team upside = sum");
        assertAmountEquals(new BigDecimal("3500"), teamFc.getBestCaseAmount(), "team best = sum");

        // ForecastLine 快照（userA 有 2 个商机 → 2 行）
        List<ErpCrmForecastLine> linesA = loadForecastLines(userA.getId());
        assertEquals(2, linesA.size(), "userA ForecastLine 快照 = 2 商机");
    }

    @Test
    public void testFrozenRejectsRefresh() {
        Long periodId = 5601L;
        ormTemplate.runInSession(() -> {
            seedStage(STAGE_QUALIFIED, "STG-Q", "已验证", 10, 50);
            seedPeriod(periodId, "2026-08", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31),
                    ErpCrmConstants.FORECAST_PERIOD_STATUS_OPEN);
        });
        // freeze: OPEN → FROZEN
        assertEquals(0, freeze(periodId).getStatus(), "freeze 应成功");
        assertEquals(ErpCrmConstants.FORECAST_PERIOD_STATUS_FROZEN,
                reloadPeriod(periodId).getStatus(), "OPEN → FROZEN");

        // FROZEN 拒绝重算
        ApiResponse<?> bad = refreshForecast(periodId);
        assertEquals(ErpCrmErrors.ERR_FORECAST_PERIOD_NOT_OPEN.getErrorCode(), bad.getCode(),
                "FROZEN 期间拒绝 refreshForecast → ERR_FORECAST_PERIOD_NOT_OPEN");
    }

    @Test
    public void testClosePeriodTriggersAccuracy() {
        Long periodId = 5701L;
        Long leadId = 5711L;
        ormTemplate.runInSession(() -> {
            seedStage(STAGE_QUALIFIED, "STG-Q", "已验证", 10, 50);
            seedPeriod(periodId, "2026-09", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
                    ErpCrmConstants.FORECAST_PERIOD_STATUS_OPEN);
            // commit 商机：probability=90, expectedRevenue=1000
            seedOpportunity(leadId, "OPP-ACC-001", "userC", null, 90, new BigDecimal("1000"),
                    LocalDate.of(2026, 9, 15), ErpCrmConstants.DOC_STATUS_QUALIFIED);
        });
        // 先刷新预测
        assertEquals(0, refreshForecast(periodId).getStatus(), "refreshForecast 应成功");
        // 标记商机为 CONVERTED（模拟实际关闭收入）
        ormTemplate.runInSession(() -> {
            ErpCrmLead opp = reloadLead(leadId);
            opp.setDocStatus(ErpCrmConstants.DOC_STATUS_CONVERTED);
            daoProvider.daoFor(ErpCrmLead.class).updateEntity(opp);
        });
        // closePeriod: OPEN → CLOSED + 触发准确率
        assertEquals(0, closePeriod(periodId).getStatus(), "closePeriod 应成功");
        assertEquals(ErpCrmConstants.FORECAST_PERIOD_STATUS_CLOSED,
                reloadPeriod(periodId).getStatus(), "OPEN → CLOSED");

        // 准确率记录已创建
        ErpCrmForecastAccuracy accuracy = reloadAccuracy(periodId, "userC");
        assertNotNull(accuracy, "closePeriod 触发准确率计算");
        assertAmountEquals(new BigDecimal("1000"), accuracy.getActualClosedRevenue(),
                "actualClosedRevenue = CONVERTED 商机 expectedRevenue");
        assertAmountEquals(new BigDecimal("1000"), accuracy.getCommitAmount(),
                "commitAmount 来自预测行");
        // commit=1000, actual=1000 → accuracy=1.0
        assertEquals(1.0, accuracy.getCommitAccuracy(), 0.001, "commit 与 actual 相等 → accuracy=1.0");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> recalculateScore(Long leadId, String triggerEvent) {
        return rpc(mutation, "ErpCrmLeadScore__recalculateScore",
                Map.of("leadId", leadId, "triggerEvent", triggerEvent));
    }

    private ApiResponse<?> refreshForecast(Long periodId) {
        return rpc(mutation, "ErpCrmForecast__refreshForecast", Map.of("periodId", periodId));
    }

    private ApiResponse<?> freeze(Long periodId) {
        return rpc(mutation, "ErpCrmForecastPeriod__freeze", Map.of("periodId", periodId));
    }

    private ApiResponse<?> closePeriod(Long periodId) {
        return rpc(mutation, "ErpCrmForecastPeriod__closePeriod", Map.of("periodId", periodId));
    }

    private ApiResponse<?> rpc(GraphQLOperationType opType, String action, Map<String, Object> data) {
        return graphQLEngine.executeRpc(
                graphQLEngine.newRpcContext(opType, action, ApiRequest.build(data)));
    }

    private void assertAmountEquals(BigDecimal expected, BigDecimal actual, String message) {
        assertEquals(0, expected.compareTo(actual), message + " (expected=" + expected + ", actual=" + actual + ")");
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

    private void seedScoreConfig(Long id, String name, int autoThreshold, int minFollowUp) {
        IEntityDao<ErpCrmLeadScoreConfig> dao = daoProvider.daoFor(ErpCrmLeadScoreConfig.class);
        ErpCrmLeadScoreConfig config = new ErpCrmLeadScoreConfig();
        config.setId(id);
        config.setCode("SCORE-" + id);
        config.setOrgId(ORG_ID);
        config.setConfigName(name);
        config.setIsActive(Boolean.TRUE);
        config.setAutoQualifyThreshold(autoThreshold);
        config.setMinScoreForFollowUp(minFollowUp);
        dao.saveEntity(config);
    }

    private void seedConfigLine(Long id, Long configId, String code, String name, int weight,
                                String method, String formula, String lookupTable,
                                String formulaExpr, Integer maxScore, int sequence) {
        IEntityDao<ErpCrmLeadScoreConfigLine> dao = daoProvider.daoFor(ErpCrmLeadScoreConfigLine.class);
        ErpCrmLeadScoreConfigLine line = new ErpCrmLeadScoreConfigLine();
        line.setId(id);
        line.setConfigId(configId);
        line.setOrgId(ORG_ID);
        line.setCriterionCode(code);
        line.setCriterionName(name);
        line.setWeight(weight);
        line.setScoringMethod(method);
        line.setFormula(formula);
        line.setLookupTable(lookupTable);
        line.setMaxScore(maxScore);
        line.setSequence(sequence);
        dao.saveEntity(line);
    }

    private ErpCrmLead newLead(Long id, String code, String leadType, String docStatus) {
        ErpCrmLead lead = new ErpCrmLead();
        lead.setId(id);
        lead.setCode(code);
        lead.setOrgId(ORG_ID);
        lead.setLeadType(leadType);
        lead.setDocStatus(docStatus);
        lead.setContactName("联系人" + id);
        return lead;
    }

    private void seedOpportunity(Long id, String code, String ownerId, Long teamId,
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

    private ErpCrmLead reloadLead(Long id) {
        return daoProvider.daoFor(ErpCrmLead.class).getEntityById(id);
    }

    private ErpCrmLeadScore reloadScore(Long leadId) {
        IEntityDao<ErpCrmLeadScore> dao = daoProvider.daoFor(ErpCrmLeadScore.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("leadId", leadId));
        q.addOrderField("calculatedAt", true);
        q.setLimit(1);
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private List<ErpCrmLeadScore> loadScores(Long leadId) {
        IEntityDao<ErpCrmLeadScore> dao = daoProvider.daoFor(ErpCrmLeadScore.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("leadId", leadId));
        return dao.findAllByQuery(q);
    }

    private List<ErpCrmLeadScoreLine> loadScoreLines(Long scoreId) {
        IEntityDao<ErpCrmLeadScoreLine> dao = daoProvider.daoFor(ErpCrmLeadScoreLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("scoreId", scoreId));
        return dao.findAllByQuery(q);
    }

    private ErpCrmForecast reloadForecast(Long periodId, String ownerId) {
        IEntityDao<ErpCrmForecast> dao = daoProvider.daoFor(ErpCrmForecast.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", periodId));
        q.addFilter(eq("ownerId", ownerId));
        q.setLimit(1);
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private ErpCrmForecast reloadTeamForecast(Long periodId, Long teamId) {
        IEntityDao<ErpCrmForecast> dao = daoProvider.daoFor(ErpCrmForecast.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", periodId));
        q.addFilter(eq("teamId", teamId));
        q.addFilter(io.nop.api.core.beans.FilterBeans.isNull("ownerId"));
        q.setLimit(1);
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private List<ErpCrmForecastLine> loadForecastLines(Long forecastId) {
        IEntityDao<ErpCrmForecastLine> dao = daoProvider.daoFor(ErpCrmForecastLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("forecastId", forecastId));
        return dao.findAllByQuery(q);
    }

    private ErpCrmForecastPeriod reloadPeriod(Long id) {
        return daoProvider.daoFor(ErpCrmForecastPeriod.class).getEntityById(id);
    }

    private ErpCrmForecastAccuracy reloadAccuracy(Long periodId, String ownerId) {
        IEntityDao<ErpCrmForecastAccuracy> dao = daoProvider.daoFor(ErpCrmForecastAccuracy.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", periodId));
        q.addFilter(eq("ownerId", ownerId));
        q.setLimit(1);
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }
}
