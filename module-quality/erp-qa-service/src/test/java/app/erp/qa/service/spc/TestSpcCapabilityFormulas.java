package app.erp.qa.service.spc;

import app.erp.qa.service.ErpQaConstants;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SPC 过程能力指数计算纯函数单测（plan 2026-07-07-0305-2 Phase 4 Exit Criteria）。
 *
 * <p>{@link SpcCapabilityCalculator} 的 Cp/Cpk/Cpm 公式与 capabilityLevel 分档为静态纯函数，独立单测无 DB 依赖。
 */
public class TestSpcCapabilityFormulas {

    private static final BigDecimal USL = new BigDecimal("13");
    private static final BigDecimal LSL = new BigDecimal("7");
    private static final BigDecimal MEAN = new BigDecimal("10");

    @Test
    public void cpFormulaUslMinusLslDiv6Sigma() {
        // sigma=1 → Cp=(13-7)/6=1.0
        BigDecimal cp = SpcCapabilityCalculator.computeCp(USL, LSL, new BigDecimal("1"));
        assertEquals(new BigDecimal("1.000000"), cp);
    }

    @Test
    public void cpkFormulaMinUpperLower() {
        // 均值居中（mean=10, USL=13, LSL=7, sigma=1）→ CPU=CPL=1.0 → Cpk=1.0
        BigDecimal cpk = SpcCapabilityCalculator.computeCpk(USL, LSL, MEAN, new BigDecimal("1"));
        assertEquals(new BigDecimal("1.000000"), cpk);
    }

    @Test
    public void cpkFormulaShiftedMeanTakesMin() {
        // 均值漂移至 11：CPU=(13-11)/3=0.667，CPL=(11-7)/3=1.333 → Cpk=0.667
        BigDecimal cpk = SpcCapabilityCalculator.computeCpk(USL, LSL, new BigDecimal("11"), new BigDecimal("1"));
        assertEquals(new BigDecimal("0.666667"), cpk);
    }

    @Test
    public void classifyByCpkThresholds() {
        assertEquals(ErpQaConstants.SPC_CAPABILITY_INADEQUATE,
                SpcCapabilityCalculator.classifyByCpk(new BigDecimal("0.99")));
        assertEquals(ErpQaConstants.SPC_CAPABILITY_ACCEPTABLE,
                SpcCapabilityCalculator.classifyByCpk(new BigDecimal("1.0")));
        assertEquals(ErpQaConstants.SPC_CAPABILITY_ACCEPTABLE,
                SpcCapabilityCalculator.classifyByCpk(new BigDecimal("1.32")));
        assertEquals(ErpQaConstants.SPC_CAPABILITY_CAPABLE,
                SpcCapabilityCalculator.classifyByCpk(new BigDecimal("1.33")));
        assertEquals(ErpQaConstants.SPC_CAPABILITY_CAPABLE,
                SpcCapabilityCalculator.classifyByCpk(new BigDecimal("1.66")));
        assertEquals(ErpQaConstants.SPC_CAPABILITY_EXCELLENT,
                SpcCapabilityCalculator.classifyByCpk(new BigDecimal("1.67")));
        assertEquals(ErpQaConstants.SPC_CAPABILITY_EXCELLENT,
                SpcCapabilityCalculator.classifyByCpk(new BigDecimal("2.0")));
        assertEquals(ErpQaConstants.SPC_CAPABILITY_INADEQUATE,
                SpcCapabilityCalculator.classifyByCpk(null));
    }

    @Test
    public void nullGuards() {
        assertNull(SpcCapabilityCalculator.computeCp(null, LSL, new BigDecimal("1")));
        assertNull(SpcCapabilityCalculator.computeCp(USL, null, new BigDecimal("1")));
        assertNull(SpcCapabilityCalculator.computeCp(USL, LSL, BigDecimal.ZERO));
        assertNull(SpcCapabilityCalculator.computeCpk(USL, LSL, null, new BigDecimal("1")));
        assertNull(SpcCapabilityCalculator.computeCpm(USL, LSL, MEAN, BigDecimal.ZERO));
    }

    @Test
    public void cpmIncludesMeanDrift() {
        // 居中无漂移：Cpm == Cp
        BigDecimal cpmCentered = SpcCapabilityCalculator.computeCpm(USL, LSL, MEAN, new BigDecimal("1"));
        assertTrue(cpmCentered.doubleValue() > 0.99 && cpmCentered.doubleValue() < 1.01,
                "居中时 Cpm≈Cp=1.0，实际 " + cpmCentered);
        // 偏移至 11：Cpm 应更小（drift 项放大分母）
        BigDecimal cpmShifted = SpcCapabilityCalculator.computeCpm(USL, LSL, new BigDecimal("11"), new BigDecimal("1"));
        assertTrue(cpmShifted.doubleValue() < cpmCentered.doubleValue(),
                "偏移时 Cpm 应 < 居中 Cpm");
    }
}
