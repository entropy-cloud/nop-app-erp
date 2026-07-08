package app.erp.qa.service.spc;

import app.erp.qa.dao.entity.ErpQaQualityGoal;
import app.erp.qa.dao.entity.ErpQaRiskRegister;
import app.erp.qa.dao.entity.ErpQaSpcCapability;
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
 * SPC 过程能力分析端到端集成测试（plan 2026-07-07-0305-2 Phase 4 Exit Criteria）。
 *
 * <p>覆盖：
 * <ul>
 *   <li>正常 chart 计算 Cp/Cpk/Pp/Ppk/Cpm + capabilityLevel 分档；</li>
 *   <li>等级 INADEQUATE 回写 {@link ErpQaQualityGoal#setCurrentValue}（同 chart.code 关联）；</li>
 *   <li>等级 INADEQUATE 登记 {@link ErpQaRiskRegister}（category=SPC_PROCESS_CAPABILITY）；</li>
 *   <li>无样本时返回 null（不创建 Capability）。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpQaSpcCapability extends JunitAutoTestCase {

    static final Long MATERIAL_ID = 7801L;
    static final Long PARAMETER_ID = 9001L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    SpcCapabilityCalculator spcCapabilityCalculator;

    @Test
    public void calculateCapabilityExcellentLevel() {
        // 窄规格但稳定过程：spec 9-11，sigma≈0.1 → Cpk≈3.33 → EXCELLENT
        Long chartId = seedChart("CHART-CAP-EXCELLENT",
                new BigDecimal("9"), new BigDecimal("11"));
        // 5 子组，每子组 mean=10 range=0.1
        for (int i = 1; i <= 5; i++) {
            seedSample(chartId, i, "9.95", "10.05");
        }

        ErpQaSpcCapability cap = ormTemplate.runInSession(s -> spcCapabilityCalculator.calculateCapability(
                chartId, CoreMetrics.currentDate().minusDays(30), CoreMetrics.currentDate(), null));
        assertNotNull(cap);
        assertEquals(ErpQaConstants.SPC_CAPABILITY_EXCELLENT, cap.getCapabilityLevel(),
                "Cpk 高应判定 EXCELLENT，实际 cpk=" + cap.getCpk());
        assertNotNull(cap.getCp());
        assertNotNull(cap.getCpk());
        assertNotNull(cap.getPp());
        assertNotNull(cap.getPpk());
        assertNotNull(cap.getCpm());
        assertEquals(Integer.valueOf(5), cap.getSampleCount());
        assertEquals(Boolean.TRUE, cap.getIsStable());
        assertTrue(findRiskByChart(chartId).isEmpty(), "EXCELLENT 不登记风险");
    }

    @Test
    public void calculateCapabilityInadequateWritesBackGoalAndRegistersRisk() {
        // 宽幅值漂移但极窄规格 → Cpk 极低 → INADEQUATE
        Long chartId = seedChart("CHART-CAP-INADEQUATE",
                new BigDecimal("40"), new BigDecimal("42"));
        // chart.code 关联同名 QualityGoal（用于回写）
        seedQualityGoal("CHART-CAP-INADEQUATE");

        // 5 子组，均值漂移剧烈（每组 2 值 mean = 10, 90, 30, 70, 5；总体 mean ≈ 41）
        seedSample(chartId, 1, "0", "20");
        seedSample(chartId, 2, "80", "100");
        seedSample(chartId, 3, "20", "40");
        seedSample(chartId, 4, "60", "80");
        seedSample(chartId, 5, "0", "10");

        ErpQaSpcCapability cap = ormTemplate.runInSession(s -> spcCapabilityCalculator.calculateCapability(
                chartId, CoreMetrics.currentDate().minusDays(30), CoreMetrics.currentDate(), null));
        assertNotNull(cap);
        assertEquals(ErpQaConstants.SPC_CAPABILITY_INADEQUATE, cap.getCapabilityLevel(),
                "Cpk 低应判定 INADEQUATE，实际 cpk=" + cap.getCpk());

        // 验证 QualityGoal 回写
        ErpQaQualityGoal goal = findQualityGoalByCode("CHART-CAP-INADEQUATE");
        assertNotNull(goal, "chart.code 关联 QualityGoal 应存在");
        assertNotNull(goal.getCurrentValue(), "Cpk 已回写 currentValue");
        // 验证 RiskRegister 登记
        List<ErpQaRiskRegister> risks = findRiskByChart(chartId);
        assertEquals(false, risks.isEmpty(), "INADEQUATE 登记风险");
        ErpQaRiskRegister risk = risks.get(0);
        assertEquals("SPC_PROCESS_CAPABILITY", risk.getCategory());
        assertEquals("OPEN", risk.getStatus());
        assertNotNull(risk.getRiskScore());
        assertTrue(risk.getRiskScore() > 0);
    }

    @Test
    public void noSamplesReturnsNull() {
        Long chartId = seedChart("CHART-CAP-EMPTY",
                new BigDecimal("0"), new BigDecimal("100"));
        ErpQaSpcCapability cap = ormTemplate.runInSession(s -> spcCapabilityCalculator.calculateCapability(
                chartId, CoreMetrics.currentDate().minusDays(30), CoreMetrics.currentDate(), null));
        assertNull(cap, "无样本不创建 Capability");
    }

    // ---------- helpers ----------

    private Long seedChart(String code, BigDecimal specMin, BigDecimal specMax) {
        Long id = 95000L + (long) Math.abs(code.hashCode() % 10000);
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
            chart.setSpecMin(specMin);
            chart.setSpecMax(specMax);
            chart.setClCenterType(ErpQaConstants.SPC_CL_CENTER_AUTO_FROM_DATA);
            chart.setRuleSet(ErpQaConstants.DEFAULT_RULE_SET);
            chart.setCalcStatus(ErpQaConstants.SPC_CALC_STATUS_CALCULATED);
            chart.setIsActive(Boolean.TRUE);
            chart.setDocStatus(ErpQaConstants.DOC_STATUS_ACTIVE);
            chart.setApproveStatus(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED);
            dao.saveEntity(chart);
        });
        return id;
    }

    private void seedSample(Long chartId, int subgroupNo, String min, String max) {
        Long id = 105000L + chartId * 10 + subgroupNo;
        BigDecimal lo = new BigDecimal(min);
        BigDecimal hi = new BigDecimal(max);
        BigDecimal mean = lo.add(hi).divide(new BigDecimal("2"));
        BigDecimal range = hi.subtract(lo);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpQaSpcSample> dao = daoProvider.daoFor(ErpQaSpcSample.class);
            ErpQaSpcSample sample = dao.newEntity();
            sample.orm_propValueByName("id", id);
            sample.setChartId(chartId);
            sample.setSubgroupNo(subgroupNo);
            sample.setSampleTime(CoreMetrics.currentDateTime());
            sample.setMeasuredValues("[" + min + "," + max + "]");
            sample.setMean(mean);
            sample.setRange(range);
            sample.setStdDev(range.divide(new BigDecimal("2")));
            sample.setIsOutOfControl(false);
            dao.saveEntity(sample);
        });
    }

    private void seedQualityGoal(String code) {
        Long id = 110000L + (long) Math.abs(code.hashCode() % 10000);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpQaQualityGoal> dao = daoProvider.daoFor(ErpQaQualityGoal.class);
            ErpQaQualityGoal goal = dao.newEntity();
            goal.orm_propValueByName("id", id);
            goal.setCode(code);
            goal.setName("QualityGoal-" + code);
            goal.setStatus("ACTIVE");
            dao.saveEntity(goal);
        });
    }

    private ErpQaQualityGoal findQualityGoalByCode(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        List<ErpQaQualityGoal> list = daoProvider.daoFor(ErpQaQualityGoal.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpQaRiskRegister> findRiskByChart(Long chartId) {
        // 通过 RiskRegister.code 前缀反查：RISK-SPC-{chartCode}-{date}
        // 简化：查 category=SPC_PROCESS_CAPABILITY 的所有风险
        QueryBean q = new QueryBean();
        q.addFilter(eq("category", "SPC_PROCESS_CAPABILITY"));
        return daoProvider.daoFor(ErpQaRiskRegister.class).findAllByQuery(q);
    }
}
