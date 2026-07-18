package app.erp.qa.service.spc;

import app.erp.qa.biz.IErpQaSpcChartBiz;
import app.erp.qa.dao.entity.ErpQaInspection;
import app.erp.qa.dao.entity.ErpQaInspectionLine;
import app.erp.qa.dao.entity.ErpQaNonConformance;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SPC 计数型采样端到端集成测试（plan 2026-07-19-0120-2 Phase 3 Exit Criteria）。
 *
 * <p>覆盖：
 * <ul>
 *   <li>P 图采样：APPROVED inspection line + REJECTED defect 行 → ErpQaSpcSample.defectCount + inspectedCount 派生；</li>
 *   <li>C 图采样：APPROVED inspection + NCR（sourceType=INSPECTION, quantity）→ ErpQaSpcSample.defectCount + inspectedCount；</li>
 *   <li>chartType 分支覆盖：计量型 chart 仍走 measuredValues 路径（0 回归）；</li>
 *   <li>recalculate 调 AttributesControlLimitFormulas 公式正确分派。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpQaSpcAttributesSampling extends JunitAutoTestCase {

    static final Long MATERIAL_ID = 7701L;
    static final Long PARAMETER_ID = 8901L;
    static final Long INSPECTOR_ID = 7801L;

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
    public void pChartSamplingAggregatesRejectedInspectionLines() {
        Long chartId = seedChart("CHART-ATTR-P", ErpQaConstants.SPC_CHART_TYPE_P, 2);
        // 子组 1：2 inspections，每个 inspection 含 5 lines（2 REJECTED + 3 ACCEPTED）
        seedAttributesInspection("INS-ATTR-P1", chartId, 5, 2);
        seedAttributesInspection("INS-ATTR-P2", chartId, 5, 2);

        Integer created = ormTemplate.runInSession(s -> spcSamplingService.collectSamples(chartId, null));
        assertEquals(Integer.valueOf(1), created, "2 inspections 聚合成 1 子组");

        List<ErpQaSpcSample> samples = findSamples(chartId);
        assertEquals(1, samples.size());
        ErpQaSpcSample s = samples.get(0);
        assertEquals(Integer.valueOf(4), s.getDefectCount(), "P 图 defectCount = REJECTED line 数 = 2+2");
        assertEquals(Integer.valueOf(10), s.getInspectedCount(), "P 图 inspectedCount = line 总数 = 5+5");
        assertNotNull(s.getMean(), "defectRate 派生 mean");
        // defectRate = 4/10 = 0.4
        assertEquals(0, new BigDecimal("0.400000").compareTo(s.getMean()));
    }

    @Test
    public void cChartSamplingAggregatesNcrQuantities() {
        Long chartId = seedChart("CHART-ATTR-C", ErpQaConstants.SPC_CHART_TYPE_C, 2);
        // 子组 1：2 inspections，每个挂 1 NCR（sourceType=INSPECTION, quantity=3 / 5）
        seedAttributesInspectionWithNcr("INS-ATTR-C1", chartId, 3);
        seedAttributesInspectionWithNcr("INS-ATTR-C2", chartId, 5);

        Integer created = ormTemplate.runInSession(s -> spcSamplingService.collectSamples(chartId, null));
        assertEquals(Integer.valueOf(1), created);

        List<ErpQaSpcSample> samples = findSamples(chartId);
        assertEquals(1, samples.size());
        ErpQaSpcSample s = samples.get(0);
        assertEquals(Integer.valueOf(8), s.getDefectCount(), "C 图 defectCount = NCR.quantity 累计 = 3+5");
        assertEquals(Integer.valueOf(2), s.getInspectedCount(), "C 图 inspectedCount = inspection 数 = 2");
    }

    @Test
    public void attributesRecalculateRoutesToAttributesFormulas() {
        Long chartId = seedChart("CHART-ATTR-RECALC", ErpQaConstants.SPC_CHART_TYPE_P, 2);
        // 撒 40 inspections → 20 子组（满足 ≥20 守卫）
        for (int i = 0; i < 40; i++) {
            // defect pattern: 1 rejected of 5 lines per inspection
            seedAttributesInspection("INS-ATTR-RECALC" + i, chartId, 5, 1);
        }
        ormTemplate.runInSession(s -> spcSamplingService.collectSamples(chartId, null));
        assertEquals(20, findSamples(chartId).size(), "40 inspections ÷ subgroupSize=2 = 20 子组");

        boolean recalculated = ormTemplate.runInSession(s -> spcControlLimitCalculator.recalculate(chartId));
        assertTrue(recalculated, "20 子组触发重算");

        ErpQaSpcChart chart = daoProvider.daoFor(ErpQaSpcChart.class).getEntityById(chartId);
        assertEquals(ErpQaConstants.SPC_CALC_STATUS_CALCULATED, chart.getCalcStatus());
        assertNotNull(chart.getCl());
        assertNotNull(chart.getUcl());
        // p̄ = (8 defects × 20 子组... wait, 1 rejected per inspection × 40 = 40 defects / (5 × 40 = 200 inspected) = 0.2
        assertEquals(0, new BigDecimal("0.200000").compareTo(chart.getCl()), "P 图 CL=p̄=40/200=0.2");
        assertTrue(chart.getUcl().compareTo(chart.getCl()) > 0, "UCL > CL");
    }

    @Test
    public void measurementChartUnaffectedByAttributesBranch() {
        // 计量型 chart 仍走既有 measuredValues 聚合路径（0 回归守门）
        Long chartId = seedChart("CHART-MEAS-CHECK", ErpQaConstants.SPC_CHART_TYPE_X_BAR_R, 5);
        for (int i = 0; i < 5; i++) {
            seedMeasurementInspectionLine("INS-MEAS" + i, chartId, BigDecimal.valueOf(10 + i));
        }

        Integer created = ormTemplate.runInSession(s -> spcSamplingService.collectSamples(chartId, null));
        assertEquals(Integer.valueOf(1), created, "5 measuredValue 聚合成 1 子组");

        List<ErpQaSpcSample> samples = findSamples(chartId);
        assertEquals(1, samples.size());
        ErpQaSpcSample s = samples.get(0);
        assertEquals(null, s.getDefectCount(), "计量型样本 defectCount 未设（null）");
        assertEquals(null, s.getInspectedCount(), "计量型样本 inspectedCount 未设（null）");
        assertNotNull(s.getMeasuredValues());
        assertTrue(s.getMeasuredValues().contains("10"));
    }

    @Test
    public void attributesSamplingIsIdempotent() {
        Long chartId = seedChart("CHART-ATTR-IDEMP", ErpQaConstants.SPC_CHART_TYPE_P, 2);
        seedAttributesInspection("INS-ATTR-IDEMP1", chartId, 5, 2);
        seedAttributesInspection("INS-ATTR-IDEMP2", chartId, 5, 2);

        Integer first = ormTemplate.runInSession(s -> spcSamplingService.collectSamples(chartId, null));
        Integer second = ormTemplate.runInSession(s -> spcSamplingService.collectSamples(chartId, null));
        assertEquals(Integer.valueOf(1), first);
        assertEquals(Integer.valueOf(0), second, "重复采样不重建，返回 0");
        assertEquals(1, findSamples(chartId).size());
    }

    // ---------- helpers ----------

    private Long seedChart(String code, String chartType, int subgroupSize) {
        Long id = 71000L + (long) Math.abs(code.hashCode() % 10000);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpQaSpcChart> dao = daoProvider.daoFor(ErpQaSpcChart.class);
            ErpQaSpcChart chart = dao.newEntity();
            chart.orm_propValueByName("id", id);
            chart.setCode(code);
            chart.setName("SPC-" + code);
            chart.setChartType(chartType);
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

    private void seedAttributesInspection(String code, Long chartId, int totalLines, int rejectedLines) {
        Long insId = 81000L + (long) Math.abs(code.hashCode() % 10000);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpQaInspection> insDao = daoProvider.daoFor(ErpQaInspection.class);
            ErpQaInspection ins = insDao.newEntity();
            ins.orm_propValueByName("id", insId);
            ins.setCode(code);
            ins.setInspectionType(ErpQaConstants.INSPECTION_TYPE_INCOMING);
            ins.setMaterialId(MATERIAL_ID);
            ins.setResult(rejectedLines > 0 ? ErpQaConstants.INSPECTION_RESULT_REJECTED : ErpQaConstants.INSPECTION_RESULT_ACCEPTED);
            ins.setDocStatus(ErpQaConstants.DOC_STATUS_ACTIVE);
            ins.setApproveStatus(ErpQaConstants.APPROVE_STATUS_APPROVED);
            ins.setPosted(Boolean.FALSE);
            ins.setInspectionDate(CoreMetrics.currentDate());
            ins.setBusinessDate(CoreMetrics.currentDate());
            ins.setInspectorId(INSPECTOR_ID);
            insDao.saveEntity(ins);

            IEntityDao<ErpQaInspectionLine> lineDao = daoProvider.daoFor(ErpQaInspectionLine.class);
            for (int i = 1; i <= totalLines; i++) {
                ErpQaInspectionLine line = lineDao.newEntity();
                line.orm_propValueByName("id", insId * 100 + i);
                line.setInspectionId(insId);
                line.setLineNo(i);
                line.setParameterId(PARAMETER_ID);
                line.setParameterName("尺寸");
                line.setMeasuredValue("1");
                line.setResult(i <= rejectedLines
                        ? ErpQaConstants.INSPECTION_RESULT_REJECTED
                        : ErpQaConstants.INSPECTION_RESULT_ACCEPTED);
                lineDao.saveEntity(line);
            }
        });
    }

    private void seedAttributesInspectionWithNcr(String code, Long chartId, int ncrQuantity) {
        Long insId = 82000L + (long) Math.abs(code.hashCode() % 10000);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpQaInspection> insDao = daoProvider.daoFor(ErpQaInspection.class);
            ErpQaInspection ins = insDao.newEntity();
            ins.orm_propValueByName("id", insId);
            ins.setCode(code);
            ins.setInspectionType(ErpQaConstants.INSPECTION_TYPE_INCOMING);
            ins.setMaterialId(MATERIAL_ID);
            ins.setResult(ErpQaConstants.INSPECTION_RESULT_REJECTED);
            ins.setDocStatus(ErpQaConstants.DOC_STATUS_ACTIVE);
            ins.setApproveStatus(ErpQaConstants.APPROVE_STATUS_APPROVED);
            ins.setPosted(Boolean.FALSE);
            ins.setInspectionDate(CoreMetrics.currentDate());
            ins.setBusinessDate(CoreMetrics.currentDate());
            ins.setInspectorId(INSPECTOR_ID);
            insDao.saveEntity(ins);

            // 1 line per inspection（满足 findApprovedInspectionLines 命中 parameterId）
            IEntityDao<ErpQaInspectionLine> lineDao = daoProvider.daoFor(ErpQaInspectionLine.class);
            ErpQaInspectionLine line = lineDao.newEntity();
            line.orm_propValueByName("id", insId * 100 + 1);
            line.setInspectionId(insId);
            line.setLineNo(1);
            line.setParameterId(PARAMETER_ID);
            line.setParameterName("缺陷计数");
            line.setMeasuredValue(String.valueOf(ncrQuantity));
            line.setResult(ErpQaConstants.INSPECTION_RESULT_REJECTED);
            lineDao.saveEntity(line);

            // 挂 NCR
            IEntityDao<ErpQaNonConformance> ncrDao = daoProvider.daoFor(ErpQaNonConformance.class);
            ErpQaNonConformance ncr = ncrDao.newEntity();
            ncr.orm_propValueByName("id", insId + 5000000L);
            ncr.setCode("NCR-" + code);
            ncr.setNcrDate(CoreMetrics.currentDate());
            ncr.setSourceType(ErpQaConstants.NCR_SOURCE_TYPE_INSPECTION);
            ncr.setSourceCode(code);
            ncr.setMaterialId(MATERIAL_ID);
            ncr.setInspectionId(insId);
            ncr.setQuantity(BigDecimal.valueOf(ncrQuantity));
            ncr.setSeverity("MAJOR");
            ncr.setStatus(ErpQaConstants.NCR_STATUS_OPEN);
            ncrDao.saveEntity(ncr);
        });
    }

    private void seedMeasurementInspectionLine(String code, Long chartId, BigDecimal measuredValue) {
        Long insId = 83000L + (long) Math.abs(code.hashCode() % 10000);
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
            line.setMeasuredValue(measuredValue.toPlainString());
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
