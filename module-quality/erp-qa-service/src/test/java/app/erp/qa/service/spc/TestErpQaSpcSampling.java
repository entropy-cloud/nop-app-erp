package app.erp.qa.service.spc;

import app.erp.qa.biz.IErpQaSpcChartBiz;
import app.erp.qa.dao.entity.ErpQaInspection;
import app.erp.qa.dao.entity.ErpQaInspectionLine;
import app.erp.qa.dao.entity.ErpQaSpcChart;
import app.erp.qa.dao.entity.ErpQaSpcSample;
import app.erp.qa.service.ErpQaConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SPC 采样与控制限计算端到端集成测试（plan 2026-07-07-0305-2 Phase 2 Exit Criteria）。
 *
 * <p>覆盖：
 * <ul>
 *   <li>{@link SpcSamplingService#collectSamples} 对 APPROVED 质检单的命中行聚合成 ErpQaSpcSample
 *       （mean/range/stdDev 数值正确）；</li>
 *   <li>无命中数据不报错（返回 0）；</li>
 *   <li>幂等（重复 collect 不重建）；</li>
 *   <li>{@link SpcControlLimitCalculator#recalculate} 在 sample≥20 时算出 ucl/lcl/cl；</li>
 *   <li>sample<20 时 calcStatus 保持 PENDING。</li>
 * </ul>
 *
 * <p>所有服务调用经 {@code ormTemplate.runInSession} 包装（镜像生产侧 BizModel @BizMutation 会话边界）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpQaSpcSampling extends JunitAutoTestCase {

    static final Long MATERIAL_ID = 7601L;
    static final Long PARAMETER_ID = 8801L;
    static final Long INSPECTOR_ID = 7701L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpQaSpcChartBiz spcChartBiz;
    @Inject
    SpcSamplingService spcSamplingService;
    @Inject
    SpcControlLimitCalculator spcControlLimitCalculator;

    @Test
    public void collectSamplesAggregatesApprovedInspectionLines() {
        Long chartId = seedChart("CHART-SAMPLE", 5);
        for (int i = 0; i < 5; i++) {
            seedApprovedInspectionLine("INS-S" + i, chartId, BigDecimal.valueOf(10 + i * 10));
        }

        Integer created = ormTemplate.runInSession(s -> spcSamplingService.collectSamples(chartId, null));
        assertEquals(Integer.valueOf(1), created, "5 个值聚合成 1 子组");

        List<ErpQaSpcSample> samples = findSamples(chartId);
        assertEquals(1, samples.size());
        ErpQaSpcSample s = samples.get(0);
        assertEquals(0, new BigDecimal("30.000000").compareTo(s.getMean()), "mean=30");
        assertEquals(0, new BigDecimal("40.000000").compareTo(s.getRange()), "range=40");
        assertNotNull(s.getStdDev());
        assertTrue(s.getStdDev().signum() > 0);
        assertNotNull(s.getMeasuredValues());
        assertTrue(s.getMeasuredValues().contains("10"));
        assertTrue(s.getMeasuredValues().contains("50"));
        assertEquals(ErpQaConstants.SPC_SOURCE_BILL_TYPE_INSPECTION, s.getSourceBillType());
        assertEquals(Boolean.FALSE, s.getIsOutOfControl());
    }

    @Test
    public void collectSamplesIsIdempotent() {
        Long chartId = seedChart("CHART-IDEMP", 5);
        for (int i = 0; i < 5; i++) {
            seedApprovedInspectionLine("INS-IDEMP" + i, chartId, BigDecimal.valueOf(10 + i));
        }
        Integer first = ormTemplate.runInSession(s -> spcSamplingService.collectSamples(chartId, null));
        Integer second = ormTemplate.runInSession(s -> spcSamplingService.collectSamples(chartId, null));
        assertEquals(Integer.valueOf(1), first);
        assertEquals(Integer.valueOf(0), second, "重复采样不重建，返回 0");
        assertEquals(1, findSamples(chartId).size());
    }

    @Test
    public void collectSamplesWithNoDataReturnsZero() {
        Long chartId = seedChart("CHART-EMPTY", 5);
        Integer created = ormTemplate.runInSession(s -> spcSamplingService.collectSamples(chartId, null));
        assertEquals(Integer.valueOf(0), created);
        assertTrue(findSamples(chartId).isEmpty());
    }

    @Test
    public void recalculateComputesControlLimitsWhen20Subgroups() {
        Long chartId = seedChart("CHART-CL", 5);
        for (int i = 0; i < 100; i++) {
            seedApprovedInspectionLine("INS-CL" + i, chartId, BigDecimal.valueOf(10 + (i % 5)));
        }
        ormTemplate.runInSession(s -> spcSamplingService.collectSamples(chartId, null));
        assertEquals(20, findSamples(chartId).size(), "20 子组样本");

        boolean recalculated = ormTemplate.runInSession(s -> spcControlLimitCalculator.recalculate(chartId));
        assertTrue(recalculated, "20 子组触发重算");

        ErpQaSpcChart chart = daoProvider.daoFor(ErpQaSpcChart.class).getEntityById(chartId);
        assertEquals(ErpQaConstants.SPC_CALC_STATUS_CALCULATED, chart.getCalcStatus());
        assertNotNull(chart.getCl());
        assertNotNull(chart.getUcl());
        assertNotNull(chart.getLcl());
        assertTrue(chart.getUcl().compareTo(chart.getCl()) > 0, "UCL > CL");
        assertTrue(chart.getCl().compareTo(chart.getLcl()) > 0, "CL > LCL");
    }

    @Test
    public void recalculateKeepsPendingWhenLessThan20() {
        Long chartId = seedChart("CHART-PENDING", 5);
        for (int i = 0; i < 10; i++) {
            seedApprovedInspectionLine("INS-P" + i, chartId, BigDecimal.TEN);
        }
        ormTemplate.runInSession(s -> spcSamplingService.collectSamples(chartId, null));
        assertEquals(2, findSamples(chartId).size());

        boolean recalculated = ormTemplate.runInSession(s -> spcControlLimitCalculator.recalculate(chartId));
        assertEquals(false, recalculated, "样本不足 20 不重算");

        ErpQaSpcChart chart = daoProvider.daoFor(ErpQaSpcChart.class).getEntityById(chartId);
        assertEquals(ErpQaConstants.SPC_CALC_STATUS_PENDING, chart.getCalcStatus());
        assertNull(chart.getUcl());
    }

    @Test
    public void nonNumericMeasuredValueIsSkipped() {
        Long chartId = seedChart("CHART-NUM", 2);
        // 1 数值 + 1 非数值（"N/A"）→ 仅 1 有效，子组 size=2 不足成组
        seedApprovedInspectionLine("INS-NUM1", chartId, BigDecimal.TEN);
        seedApprovedInspectionLineRaw("INS-NUM2", chartId, "N/A");
        Integer created = ormTemplate.runInSession(s -> spcSamplingService.collectSamples(chartId, null));
        assertEquals(Integer.valueOf(0), created, "1 个有效值无法凑足 subgroupSize=2");
    }

    // ---------- helpers ----------

    private Long seedChart(String code, int subgroupSize) {
        Long id = 70000L + (long) Math.abs(code.hashCode() % 10000);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpQaSpcChart> dao = daoProvider.daoFor(ErpQaSpcChart.class);
            ErpQaSpcChart chart = dao.newEntity();
            chart.orm_propValueByName("id", id);
            chart.setCode(code);
            chart.setName("SPC-" + code);
            chart.setChartType(ErpQaConstants.SPC_CHART_TYPE_X_BAR_R);
            chart.setParameterId(PARAMETER_ID);
            chart.setMaterialId(MATERIAL_ID);
            chart.setSubgroupSize(subgroupSize);
            chart.setClCenterType(ErpQaConstants.SPC_CL_CENTER_AUTO_FROM_DATA);
            chart.setRuleSet(ErpQaConstants.DEFAULT_RULE_SET);
            chart.setCalcStatus(ErpQaConstants.SPC_CALC_STATUS_PENDING);
            chart.setIsActive(Boolean.TRUE);
            chart.setDocStatus(ErpQaConstants.DOC_STATUS_ACTIVE);
            chart.setApproveStatus(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED);
            dao.saveEntity(chart);
        });
        return id;
    }

    private void seedApprovedInspectionLine(String code, Long chartId, BigDecimal measuredValue) {
        seedApprovedInspectionLineRaw(code, chartId, measuredValue.toPlainString());
    }

    private void seedApprovedInspectionLineRaw(String code, Long chartId, String measuredValue) {
        Long insId = 80000L + (long) Math.abs(code.hashCode() % 10000);
        Long lineId = insId * 100 + 1;
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpQaInspection> insDao = daoProvider.daoFor(ErpQaInspection.class);
            ErpQaInspection ins = insDao.newEntity();
            ins.orm_propValueByName("id", insId);
            ins.setCode(code);
            ins.setInspectionType(ErpQaConstants.INSPECTION_TYPE_INCOMING);
            ins.setMaterialId(MATERIAL_ID);
            ins.setResult(ErpQaConstants.INSPECTION_RESULT_ACCEPTED);
            ins.setDocStatus(ErpQaConstants.DOC_STATUS_ACTIVE);
            ins.setApproveStatus(ErpQaConstants.APPROVE_STATUS_APPROVED);
            ins.setPosted(Boolean.FALSE);
            ins.setInspectionDate(CoreMetrics.currentDate());
            ins.setBusinessDate(CoreMetrics.currentDate());
            ins.setInspectorId(INSPECTOR_ID);
            insDao.saveEntity(ins);

            IEntityDao<ErpQaInspectionLine> lineDao = daoProvider.daoFor(ErpQaInspectionLine.class);
            ErpQaInspectionLine line = lineDao.newEntity();
            line.orm_propValueByName("id", lineId);
            line.setInspectionId(insId);
            line.setLineNo(1);
            line.setParameterId(PARAMETER_ID);
            line.setParameterName("尺寸");
            line.setMeasuredValue(measuredValue);
            line.setResult(ErpQaConstants.INSPECTION_RESULT_ACCEPTED);
            lineDao.saveEntity(line);
        });
    }

    private List<ErpQaSpcSample> findSamples(Long chartId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("chartId", chartId));
        return daoProvider.daoFor(ErpQaSpcSample.class).findAllByQuery(q);
    }
}
