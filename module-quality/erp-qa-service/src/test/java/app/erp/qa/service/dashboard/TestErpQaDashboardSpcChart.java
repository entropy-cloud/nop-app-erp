package app.erp.qa.service.dashboard;

import app.erp.qa.dao.entity.ErpQaSpcChart;
import app.erp.qa.dao.entity.ErpQaSpcSample;
import app.erp.qa.service.ErpQaConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SPC 控制图看板聚合（{@code ErpQaDashboard__getSpcControlChartData}）集成测试
 * （plan 2026-07-17-2010-1 Phase 3）。覆盖：空数据零值结构、多样本序列按 subgroupNo 升序、
 * ucl/lcl/cl 经 chart 字段真实传递、isOutOfControl/violatedRules 透传、chartId 入参过滤与默认最近一张。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpQaDashboardSpcChart extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    static final Long PARAMETER_ID = 96001L;
    static final Long MATERIAL_ID = 96002L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ErpQaDashboardBizModel dashboardBiz;

    @AfterEach
    void clearConfig() {
        AppConfig.getConfigProvider().assignConfigValue(ErpQaConstants.CONFIG_DASH_QA_SPC_DEFAULT_CHART_ID, "");
    }

    @Test
    public void testEmptyDatasetReturnsZeroValueStructure() {
        Map<String, Object> result = dashboardBiz.getSpcControlChartData(null, CTX);
        assertNotNull(result, "空数据返回零值结构非 null");
        assertNull(result.get("chartId"));
        assertNull(result.get("chartType"));
        assertNull(result.get("cl"));
        assertNull(result.get("ucl"));
        assertNull(result.get("lcl"));
        assertNotNull(result.get("samples"), "samples 列表非 null");
        assertTrue(((List<?>) result.get("samples")).isEmpty(), "空数据 samples 列表为空");
    }

    @Test
    public void testControlLimitsPassedFromChartEntity() {
        Long chartId = seedChart("SPC-CL", new BigDecimal("50"), new BigDecimal("60"), new BigDecimal("40"));
        Map<String, Object> result = dashboardBiz.getSpcControlChartData(chartId, CTX);
        assertEquals(chartId, result.get("chartId"));
        assertEquals(ErpQaConstants.SPC_CHART_TYPE_X_BAR_R, result.get("chartType"));
        assertEquals(0, new BigDecimal("50").compareTo(toBd(result.get("cl"))), "cl=chart.cl");
        assertEquals(0, new BigDecimal("60").compareTo(toBd(result.get("ucl"))), "ucl 经 chart 字段传递");
        assertEquals(0, new BigDecimal("40").compareTo(toBd(result.get("lcl"))), "lcl 经 chart 字段传递");
    }

    @Test
    public void testSamplesOrderedBySubgroupNoAscending() {
        Long chartId = seedChart("SPC-ORD", new BigDecimal("10"), new BigDecimal("20"), new BigDecimal("0"));
        // 故意倒序塞入：3,1,2 → 应升序返回 1,2,3
        seedSample(chartId, 3, bd("3"), false, null);
        seedSample(chartId, 1, bd("1"), false, null);
        seedSample(chartId, 2, bd("2"), true, "1,2");

        Map<String, Object> result = dashboardBiz.getSpcControlChartData(chartId, CTX);
        List<Map<String, Object>> samples = extractSamples(result);
        assertEquals(3, samples.size(), "3 样本点");
        assertEquals(1, samples.get(0).get("subgroupNo"));
        assertEquals(2, samples.get(1).get("subgroupNo"));
        assertEquals(3, samples.get(2).get("subgroupNo"));
    }

    @Test
    public void testIsOutOfControlAndViolatedRulesPassedThrough() {
        Long chartId = seedChart("SPC-VIO", new BigDecimal("10"), new BigDecimal("20"), new BigDecimal("0"));
        seedSample(chartId, 1, bd("12"), false, null);
        seedSample(chartId, 2, bd("25"), true, "1");
        seedSample(chartId, 3, bd("30"), true, "1,2,3");

        Map<String, Object> result = dashboardBiz.getSpcControlChartData(chartId, CTX);
        List<Map<String, Object>> samples = extractSamples(result);
        // subgroupNo=1 受控
        assertFalse(Boolean.TRUE.equals(samples.get(0).get("isOutOfControl")), "subgroupNo=1 受控");
        // subgroupNo=2 失控 + 违规规则 "1"
        assertTrue(Boolean.TRUE.equals(samples.get(1).get("isOutOfControl")), "subgroupNo=2 失控");
        assertEquals("1", samples.get(1).get("violatedRules"));
        // subgroupNo=3 失控 + 违规规则 "1,2,3"
        assertEquals("1,2,3", samples.get(2).get("violatedRules"));
    }

    @Test
    public void testChartIdInputFilters() {
        Long chartA = seedChart("SPC-A", new BigDecimal("10"), new BigDecimal("20"), new BigDecimal("0"));
        Long chartB = seedChart("SPC-B", new BigDecimal("100"), new BigDecimal("200"), new BigDecimal("50"));
        seedSample(chartA, 1, bd("11"), false, null);
        seedSample(chartB, 1, bd("110"), false, null);

        Map<String, Object> a = dashboardBiz.getSpcControlChartData(chartA, CTX);
        Map<String, Object> b = dashboardBiz.getSpcControlChartData(chartB, CTX);
        assertEquals(chartA, a.get("chartId"));
        assertEquals(chartB, b.get("chartId"));
        assertEquals(0, new BigDecimal("20").compareTo(toBd(a.get("ucl"))), "chartA UCL=20");
        assertEquals(0, new BigDecimal("200").compareTo(toBd(b.get("ucl"))), "chartB UCL=200");
        assertEquals(1, ((List<?>) a.get("samples")).size(), "chartA 仅 1 样本");
        assertEquals(1, ((List<?>) b.get("samples")).size(), "chartB 仅 1 样本");
    }

    @Test
    public void testDefaultPicksLatestChartWhenNoChartIdAndNoConfig() {
        Long first = seedChart("SPC-FIRST", new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("0"));
        Long second = seedChart("SPC-SECOND", new BigDecimal("5"), new BigDecimal("10"), new BigDecimal("1"));

        Map<String, Object> result = dashboardBiz.getSpcControlChartData(null, CTX);
        // id 较大者 = 最近一张
        assertEquals(Long.max(first, second), result.get("chartId"),
                "chartId 入参空 + config 空 → 取 id 最大（最近）一张 ErpQaSpcChart");
    }

    @Test
    public void testConfigDefaultChartIdOverridesLatestPick() {
        Long first = seedChart("SPC-CF1", new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("0"));
        Long second = seedChart("SPC-CF2", new BigDecimal("5"), new BigDecimal("10"), new BigDecimal("1"));
        // config 指向较小 id（first），覆盖"最近一张"默认
        AppConfig.getConfigProvider().assignConfigValue(
                ErpQaConstants.CONFIG_DASH_QA_SPC_DEFAULT_CHART_ID, String.valueOf(first));

        Map<String, Object> result = dashboardBiz.getSpcControlChartData(null, CTX);
        assertEquals(first, result.get("chartId"),
                "config erp-dash.qa-spc-default-chart-id 覆盖最近一张默认");
    }

    // ---------- helpers ----------

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractSamples(Map<String, Object> result) {
        Object s = result.get("samples");
        assertNotNull(s, "samples 非空");
        return (List<Map<String, Object>>) s;
    }

    private BigDecimal toBd(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        return new BigDecimal(String.valueOf(v));
    }

    private Long seedChart(String code, BigDecimal cl, BigDecimal ucl, BigDecimal lcl) {
        Long id = 97000L + (long) Math.abs(code.hashCode() % 10000);
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
            chart.setCl(cl);
            chart.setUcl(ucl);
            chart.setLcl(lcl);
            chart.setCalcStatus(ErpQaConstants.SPC_CALC_STATUS_CALCULATED);
            chart.setIsActive(Boolean.TRUE);
            chart.setDocStatus(ErpQaConstants.DOC_STATUS_ACTIVE);
            chart.setApproveStatus(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED);
            dao.saveEntity(chart);
        });
        return id;
    }

    private void seedSample(Long chartId, int subgroupNo, BigDecimal mean, boolean outOfControl, String violatedRules) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpQaSpcSample> dao = daoProvider.daoFor(ErpQaSpcSample.class);
            ErpQaSpcSample s = dao.newEntity();
            s.orm_propValueByName("id", 108000L + chartId * 10 + subgroupNo);
            s.setChartId(chartId);
            s.setSubgroupNo(subgroupNo);
            s.setSampleTime(CoreMetrics.currentTimestamp());
            s.setMeasuredValues("[" + mean + "]");
            s.setMean(mean);
            s.setRange(BigDecimal.ZERO);
            s.setStdDev(BigDecimal.ZERO);
            s.setIsOutOfControl(outOfControl);
            if (violatedRules != null) {
                s.setViolatedRules(violatedRules);
            }
            dao.saveEntity(s);
        });
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
