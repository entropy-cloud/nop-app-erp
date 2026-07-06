package app.erp.qa.service.spc;

import app.erp.qa.dao.entity.ErpQaAction;
import app.erp.qa.dao.entity.ErpQaNonConformance;
import app.erp.qa.dao.entity.ErpQaSpcChart;
import app.erp.qa.dao.entity.ErpQaSpcSample;
import app.erp.qa.service.ErpQaConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SPC 失控级联（NCR/CAPA）端到端集成测试（plan 2026-07-07-0305-2 Phase 3 Exit Criteria）。
 *
 * <p>覆盖：
 * <ul>
 *   <li>样本失控时 violatedRules/isOutOfControl 标记；</li>
 *   <li>handler 在 config 开启时创建 NCR(sourceType=SPC) + CAPA Action（直接同步调用，绕过 afterCommit 时序）；</li>
 *   <li>config 关（{@code erp-qa.spc-auto-ncr-enabled=false}）→ 仅标记，不建 NCR；</li>
 *   <li>幂等：已建 NCR 不重复。</li>
 * </ul>
 *
 * <p>设计决策：post-commit 的 {@code afterCommit} 钩子在 JunitAutoTestCase 测试事务退出时由平台调度执行，
 * 测试方法体内强断言会因时序问题不稳定。本测试聚焦规则评估标记 + handler 同步路径正确性，
 * 真正的事务时序验证归 SpcOutOfControlHandler 单测（同事务内 sourceCode 反查幂等）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpQaSpcOutOfControl extends JunitAutoTestCase {

    static final Long MATERIAL_ID = 7701L;
    static final Long PARAMETER_ID = 8901L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    SpcRuleEngine spcRuleEngine;
    @Inject
    SpcOutOfControlHandler outOfControlHandler;

    @AfterEach
    void clearConfig() {
        AppConfig.getConfigProvider().assignConfigValue(ErpQaConstants.CONFIG_SPC_AUTO_NCR_ENABLED, "");
    }

    @Test
    public void outOfControlMarksSampleAndHandlerCreatesNcr() {
        Long chartId = seedChart("CHART-OOC", new BigDecimal("10"), new BigDecimal("13"), new BigDecimal("7"));
        Long sampleId = seedSample(chartId, 1, new BigDecimal("20"));

        // 规则评估：标记失控
        ormTemplate.runInSession(() -> spcRuleEngine.evaluate(chartId, null));
        ErpQaSpcSample sample = daoProvider.daoFor(ErpQaSpcSample.class).getEntityById(sampleId);
        assertEquals(Boolean.TRUE, sample.getIsOutOfControl());
        assertNotNull(sample.getViolatedRules());
        assertTrue(sample.getViolatedRules().contains("1"), "应命中规则 1，实际: " + sample.getViolatedRules());

        // 直接同步调用 handler 内部建单逻辑（绕过 afterCommit 时序）
        ErpQaSpcChart chart = daoProvider.daoFor(ErpQaSpcChart.class).getEntityById(chartId);
        ormTemplate.runInSession(() -> invokeCreateNcrDirectly(chart, sample, Set.of("1")));

        QueryBean q = new QueryBean();
        q.addFilter(eq("sourceType", ErpQaConstants.NCR_SOURCE_TYPE_SPC));
        q.addFilter(eq("sourceCode", chart.getCode() + "#" + sample.getSubgroupNo()));
        List<ErpQaNonConformance> ncrs = daoProvider.daoFor(ErpQaNonConformance.class).findAllByQuery(q);
        assertEquals(1, ncrs.size(), "建 1 NCR");
        ErpQaNonConformance ncr = ncrs.get(0);
        assertEquals(ErpQaConstants.NCR_STATUS_OPEN, ncr.getStatus());
        assertEquals(MATERIAL_ID, ncr.getMaterialId());

        QueryBean actionQ = new QueryBean();
        actionQ.addFilter(eq("ncrId", ncr.getId()));
        List<ErpQaAction> actions = daoProvider.daoFor(ErpQaAction.class).findAllByQuery(actionQ);
        assertEquals(1, actions.size(), "建 1 CAPA Action");
        assertEquals("CAPA", actions.get(0).getActionType());
        assertEquals(ErpQaConstants.ACTION_STATUS_PENDING, actions.get(0).getStatus());

        // 幂等：再次调用不重建
        ormTemplate.runInSession(() -> invokeCreateNcrDirectly(chart, sample, Set.of("1")));
        QueryBean q2 = new QueryBean();
        q2.addFilter(eq("sourceType", ErpQaConstants.NCR_SOURCE_TYPE_SPC));
        assertEquals(1, daoProvider.daoFor(ErpQaNonConformance.class).findAllByQuery(q2).size(),
                "幂等：不重建 NCR");
    }

    @Test
    public void outOfControlAutoNcrDisabledDoesNotCreateNcr() {
        AppConfig.getConfigProvider().assignConfigValue(ErpQaConstants.CONFIG_SPC_AUTO_NCR_ENABLED, "false");

        Long chartId = seedChart("CHART-OOC2", new BigDecimal("10"), new BigDecimal("13"), new BigDecimal("7"));
        Long sampleId = seedSample(chartId, 1, new BigDecimal("20"));

        ormTemplate.runInSession(() -> spcRuleEngine.evaluate(chartId, null));
        ErpQaSpcSample sample = daoProvider.daoFor(ErpQaSpcSample.class).getEntityById(sampleId);
        assertEquals(Boolean.TRUE, sample.getIsOutOfControl(), "config 关闭仍标记 isOutOfControl");

        // handler 应跳过（config 关闭）
        ErpQaSpcChart chart = daoProvider.daoFor(ErpQaSpcChart.class).getEntityById(chartId);
        ormTemplate.runInSession(() -> outOfControlHandler.cascadeNcrAndCapa(chart, sample, Set.of("1"), null));

        QueryBean q = new QueryBean();
        q.addFilter(eq("sourceType", ErpQaConstants.NCR_SOURCE_TYPE_SPC));
        assertTrue(daoProvider.daoFor(ErpQaNonConformance.class).findAllByQuery(q).isEmpty(),
                "config 关闭不建 NCR");
    }

    @Test
    public void severityMapping() {
        assertEquals("NORMAL", SpcOutOfControlHandler.mapSeverity(Set.of("2")));
        assertEquals("HIGH", SpcOutOfControlHandler.mapSeverity(Set.of("1")));
        assertEquals("CRITICAL", SpcOutOfControlHandler.mapSeverity(Set.of("1", "2")));
        assertEquals("NORMAL", SpcOutOfControlHandler.mapSeverity(null));
    }

    @Test
    public void controlledSampleHasNoViolations() {
        Long chartId = seedChart("CHART-CTL", new BigDecimal("10"), new BigDecimal("13"), new BigDecimal("7"));
        for (int i = 1; i <= 5; i++) {
            seedSample(chartId, i, new BigDecimal("10"));
        }
        ormTemplate.runInSession(() -> spcRuleEngine.evaluate(chartId, null));

        QueryBean q = new QueryBean();
        q.addFilter(eq("chartId", chartId));
        List<ErpQaSpcSample> samples = daoProvider.daoFor(ErpQaSpcSample.class).findAllByQuery(q);
        for (ErpQaSpcSample s : samples) {
            assertNull(s.getViolatedRules(), "受控样本无违规");
            assertFalse(Boolean.TRUE.equals(s.getIsOutOfControl()));
        }
    }

    /**
     * 直接调用 handler 的私有建单逻辑（包内可见，反射）。绕过 afterCommit 时序，用于测试内强断言。
     * 生产代码仍走 afterCommit post-commit 路径，本测试仅验证建单算法。
     */
    private void invokeCreateNcrDirectly(ErpQaSpcChart chart, ErpQaSpcSample sample, Set<String> violatedRules) {
        try {
            java.lang.reflect.Method m = SpcOutOfControlHandler.class.getDeclaredMethod(
                    "createNcrAndAction", ErpQaSpcChart.class, ErpQaSpcSample.class, Set.class, io.nop.core.context.IServiceContext.class);
            m.setAccessible(true);
            m.invoke(outOfControlHandler, chart, sample, violatedRules, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Long seedChart(String code, BigDecimal cl, BigDecimal ucl, BigDecimal lcl) {
        Long id = 75000L + (long) Math.abs(code.hashCode() % 10000);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpQaSpcChart> dao = daoProvider.daoFor(ErpQaSpcChart.class);
            ErpQaSpcChart chart = dao.newEntity();
            chart.orm_propValueByName("id", id);
            chart.setCode(code);
            chart.setName("SPC-" + code);
            chart.setChartType(ErpQaConstants.SPC_CHART_TYPE_X_BAR_R);
            chart.setParameterId(PARAMETER_ID);
            chart.setMaterialId(MATERIAL_ID);
            chart.setSubgroupSize(ErpQaConstants.DEFAULT_SPC_SUBGROUP_SIZE);
            chart.setClCenterType(ErpQaConstants.SPC_CL_CENTER_AUTO_FROM_DATA);
            chart.setRuleSet(ErpQaConstants.DEFAULT_RULE_SET);
            chart.setCalcStatus(ErpQaConstants.SPC_CALC_STATUS_CALCULATED);
            chart.setCl(cl);
            chart.setUcl(ucl);
            chart.setLcl(lcl);
            chart.setIsActive(Boolean.TRUE);
            chart.setDocStatus(ErpQaConstants.DOC_STATUS_ACTIVE);
            chart.setApproveStatus(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED);
            dao.saveEntity(chart);
        });
        return id;
    }

    private Long seedSample(Long chartId, int subgroupNo, BigDecimal mean) {
        Long id = 85000L + chartId * 10 + subgroupNo;
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpQaSpcSample> dao = daoProvider.daoFor(ErpQaSpcSample.class);
            ErpQaSpcSample sample = dao.newEntity();
            sample.orm_propValueByName("id", id);
            sample.setChartId(chartId);
            sample.setSubgroupNo(subgroupNo);
            sample.setSampleTime(java.time.LocalDateTime.now());
            sample.setMeasuredValues("[" + mean.toPlainString() + "]");
            sample.setMean(mean);
            sample.setRange(BigDecimal.ZERO);
            sample.setStdDev(BigDecimal.ZERO);
            sample.setIsOutOfControl(false);
            dao.saveEntity(sample);
        });
        return id;
    }
}
