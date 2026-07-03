package app.erp.pur.service;

import app.erp.pur.biz.IErpPurSupplierScorecardBiz;
import app.erp.pur.dao.entity.ErpPurSupplierScorecard;
import app.erp.pur.dao.entity.ErpPurSupplierScorecardCriteria;
import app.erp.pur.dao.entity.ErpPurSupplierScorecardVariable;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
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
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 供应商评分卡周期评分引擎单测（{@code docs/plans/2026-07-03-1707-2} Phase 2）。
 *
 * <p>覆盖：多 criteria 加权→totalScore；GREEN/YELLOW/RED 档位映射；公式经 XLang 表达式引擎取变量；
 * 权重和=100 校验；FINALIZED 不可重算。standing=RED→AVL 联动由 Phase 3 端到端测试覆盖。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurScorecardCalc extends JunitAutoTestCase {

    static final Long PARTNER_ID = 8001L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testGreenStanding() {
        ErpPurSupplierScorecard sc = newScorecard(new BigDecimal("80"), new BigDecimal("60"));
        ormTemplate.runInSession(() -> {
            scorecardDao().saveEntity(sc);
            // 质量维度 40%：pass_rate=95 → score=95；交货维度 60%：on_time=90 → score=90
            saveCriteria(sc.getId(), "质量", "40", "pass_rate", 95, "交货", "60", "on_time", 90);
        });

        assertEquals(0, finalizeScorecard(sc.getId()).getStatus(), "定稿应成功");
        ErpPurSupplierScorecard done = reload(sc.getId());
        assertEquals(ErpPurConstants.SCORECARD_STATUS_FINALIZED, done.getStatus(), "status → FINALIZED");
        assertEquals(ErpPurConstants.STANDING_GREEN, done.getStanding(), "92 ≥ 80 → GREEN");
        // totalScore = 95*0.4 + 90*0.6 = 38 + 54 = 92
        assertEquals(new BigDecimal("92.00"), done.getTotalScore(), "加权总分 = Σ score×weight/100");
    }

    @Test
    public void testYellowStanding() {
        ErpPurSupplierScorecard sc = newScorecard(new BigDecimal("80"), new BigDecimal("60"));
        ormTemplate.runInSession(() -> {
            scorecardDao().saveEntity(sc);
            // 70 ∈ [60, 80) → YELLOW
            saveCriteria(sc.getId(), "质量", "50", "pass_rate", 70, "交货", "50", "on_time", 70);
        });

        assertEquals(0, finalizeScorecard(sc.getId()).getStatus());
        ErpPurSupplierScorecard done = reload(sc.getId());
        assertEquals(ErpPurConstants.STANDING_YELLOW, done.getStanding(), "70 ∈ [60,80) → YELLOW");
        assertEquals(new BigDecimal("70.00"), done.getTotalScore());
    }

    @Test
    public void testRedStanding() {
        ErpPurSupplierScorecard sc = newScorecard(new BigDecimal("80"), new BigDecimal("60"));
        ormTemplate.runInSession(() -> {
            scorecardDao().saveEntity(sc);
            // 50 < 60 → RED
            saveCriteria(sc.getId(), "质量", "50", "pass_rate", 50, "交货", "50", "on_time", 50);
        });

        assertEquals(0, finalizeScorecard(sc.getId()).getStatus());
        assertEquals(ErpPurConstants.STANDING_RED, reload(sc.getId()).getStanding(), "50 < 60 → RED");
    }

    @Test
    public void testFormulaUsesVariablesViaExpressionEngine() {
        ErpPurSupplierScorecard sc = newScorecard(new BigDecimal("80"), new BigDecimal("60"));
        ormTemplate.runInSession(() -> {
            scorecardDao().saveEntity(sc);
            // 公式含算术 + 变量混合：pass_rate * 0.6 + bonus * 0.4，weight 100 单维度
            ErpPurSupplierScorecardCriteria c = new ErpPurSupplierScorecardCriteria();
            c.setScorecardId(sc.getId());
            c.setCriteriaName("综合质量");
            c.setWeight(new BigDecimal("100"));
            c.setFormula("pass_rate * 0.6 + bonus * 0.4");
            criteriaDao().saveEntity(c);

            saveVariable(c.getId(), "pass_rate", 80);
            saveVariable(c.getId(), "bonus", 90);
        });

        assertEquals(0, finalizeScorecard(sc.getId()).getStatus());
        ErpPurSupplierScorecard done = reload(sc.getId());
        // score = 80*0.6 + 90*0.4 = 48 + 36 = 84 → GREEN
        assertEquals(ErpPurConstants.STANDING_GREEN, done.getStanding());
        assertEquals(new BigDecimal("84.00"), done.getTotalScore());
    }

    @Test
    public void testWeightSumMustBe100() {
        ErpPurSupplierScorecard sc = newScorecard(new BigDecimal("80"), new BigDecimal("60"));
        ormTemplate.runInSession(() -> {
            scorecardDao().saveEntity(sc);
            // 权重和 = 40 + 50 = 90 ≠ 100
            saveCriteria(sc.getId(), "质量", "40", "pass_rate", 95, "交货", "50", "on_time", 90);
        });

        ApiResponse<?> bad = finalizeScorecard(sc.getId());
        assertEquals(ErpPurErrors.ERR_SCORECARD_WEIGHT_NOT_100.getErrorCode(), bad.getCode(),
                "权重和 ≠ 100 应拒绝定稿");
    }

    @Test
    public void testFinalizedCannotRecalculate() {
        ErpPurSupplierScorecard sc = newScorecard(new BigDecimal("80"), new BigDecimal("60"));
        ormTemplate.runInSession(() -> {
            scorecardDao().saveEntity(sc);
            saveCriteria(sc.getId(), "质量", "100", "pass_rate", 90, null, null, null, 0);
        });

        assertEquals(0, finalizeScorecard(sc.getId()).getStatus(), "首次定稿成功");
        ApiResponse<?> bad = finalizeScorecard(sc.getId());
        assertEquals(ErpPurErrors.ERR_SCORECARD_ALREADY_FINALIZED.getErrorCode(), bad.getCode(),
                "FINALIZED 不可重算，需新建周期");
    }

    @Test
    public void testNoCriteriaRejected() {
        ErpPurSupplierScorecard sc = newScorecard(new BigDecimal("80"), new BigDecimal("60"));
        ormTemplate.runInSession(() -> scorecardDao().saveEntity(sc));

        ApiResponse<?> bad = finalizeScorecard(sc.getId());
        assertEquals(ErpPurErrors.ERR_SCORECARD_NO_CRITERIA.getErrorCode(), bad.getCode(),
                "无评分维度不可定稿");
    }

    // ---------- helpers ----------

    private ApiResponse<?> finalizeScorecard(Long id) {
        return executeRpc(mutation, "ErpPurSupplierScorecard__finalizeScorecard",
                ApiRequest.build(Map.of("scorecardId", id)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpPurSupplierScorecard newScorecard(BigDecimal warn, BigDecimal hold) {
        ErpPurSupplierScorecard sc = new ErpPurSupplierScorecard();
        sc.setPartnerId(PARTNER_ID);
        sc.setPeriodFrom(LocalDate.of(2026, 1, 1));
        sc.setPeriodTo(LocalDate.of(2026, 3, 31));
        sc.setWarnThreshold(warn);
        sc.setHoldThreshold(hold);
        sc.setPreventThreshold(hold);
        sc.setStatus(ErpPurConstants.SCORECARD_STATUS_DRAFT);
        return sc;
    }

    private void saveCriteria(Long scId, String name1, String w1, String var1, int val1,
                              String name2, String w2, String var2, int val2) {
        ErpPurSupplierScorecardCriteria c1 = new ErpPurSupplierScorecardCriteria();
        c1.setScorecardId(scId);
        c1.setCriteriaName(name1);
        c1.setWeight(new BigDecimal(w1));
        c1.setFormula(var1);
        criteriaDao().saveEntity(c1);
        saveVariable(c1.getId(), var1, val1);

        if (name2 == null) {
            return;
        }
        ErpPurSupplierScorecardCriteria c2 = new ErpPurSupplierScorecardCriteria();
        c2.setScorecardId(scId);
        c2.setCriteriaName(name2);
        c2.setWeight(new BigDecimal(w2));
        c2.setFormula(var2);
        criteriaDao().saveEntity(c2);
        saveVariable(c2.getId(), var2, val2);
    }

    private void saveVariable(Long criteriaId, String name, int value) {
        ErpPurSupplierScorecardVariable v = new ErpPurSupplierScorecardVariable();
        v.setCriteriaId(criteriaId);
        v.setVariableName(name);
        v.setPath(name);
        v.setValue(new BigDecimal(value));
        variableDao().saveEntity(v);
    }

    private ErpPurSupplierScorecard reload(Long id) {
        return scorecardDao().getEntityById(id);
    }

    private IEntityDao<ErpPurSupplierScorecard> scorecardDao() {
        return daoProvider.daoFor(ErpPurSupplierScorecard.class);
    }

    private IEntityDao<ErpPurSupplierScorecardCriteria> criteriaDao() {
        return daoProvider.daoFor(ErpPurSupplierScorecardCriteria.class);
    }

    private IEntityDao<ErpPurSupplierScorecardVariable> variableDao() {
        return daoProvider.daoFor(ErpPurSupplierScorecardVariable.class);
    }
}
