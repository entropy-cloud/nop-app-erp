package app.erp.qa.service.spc;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SPC 计数型控制图公式纯函数单测（plan 2026-07-19-0120-2 Phase 3 Exit Criteria）。
 *
 * <p>{@link AttributesControlLimitFormulas} 4 公式（P/NP/C/U）+ 负数下限钳 0 边界场景。
 * 纯函数无 DB 依赖，HALF_UP scale=6 精确数值断言。
 */
public class TestErpQaSpcAttributesControlLimit {

    @Test
    public void pChartFormulaComputesControlLimits() {
        // 20 子组，每子组 inspected=100，defects 序列合计 = 5+3+7+4+6+2+5+4+3+6+5+4+7+3+5+6+4+5+3+6 = 93
        // p̄ = 93 / 2000 = 0.0465
        // n̄ = 100
        // σ = sqrt(0.0465 × 0.9535 / 100) = sqrt(0.0004433775) ≈ 0.021056553
        // UCL = 0.0465 + 3×0.021056553 ≈ 0.109669658
        // LCL = 0.0465 − 3×0.021056553 ≈ -0.016669658 → 钳到 0
        java.util.List<Integer> defects = Arrays.asList(5, 3, 7, 4, 6, 2, 5, 4, 3, 6, 5, 4, 7, 3, 5, 6, 4, 5, 3, 6);
        java.util.List<Integer> inspected = Collections.nCopies(20, 100);

        AttributesControlLimitFormulas.Result r = AttributesControlLimitFormulas.calcP(defects, inspected);
        assertNotNull(r.getCl());
        assertNotNull(r.getUcl());
        // LCL 负数钳 0
        assertEquals(BigDecimal.ZERO.setScale(6), r.getLcl(), "P 图 LCL<0 钳 0");

        BigDecimal pBar = new BigDecimal("93").divide(new BigDecimal("2000"), 10, java.math.RoundingMode.HALF_UP)
                .setScale(6, java.math.RoundingMode.HALF_UP);
        assertEquals(pBar, r.getCl(), "P 图 CL=p̄=Σd/Σn");
        assertTrue(r.getUcl().compareTo(r.getCl()) > 0, "P 图 UCL > CL");
        assertEquals(new BigDecimal("0.046500"), r.getCl());
    }

    @Test
    public void npChartFormulaComputesControlLimits() {
        // 同 P 图数据：n=100, p̄=0.0465, NP CL = n·p̄ = 4.65
        // σ = sqrt(n·p̄·(1−p̄)) = sqrt(100 × 0.0465 × 0.9535) = sqrt(4.433775) ≈ 2.105655346
        // UCL = 4.65 + 3×2.105... ≈ 10.966965
        // LCL = 4.65 − 3×2.105... ≈ -1.966965 → 钳 0
        java.util.List<Integer> defects = Arrays.asList(5, 3, 7, 4, 6, 2, 5, 4, 3, 6, 5, 4, 7, 3, 5, 6, 4, 5, 3, 6);
        java.util.List<Integer> inspected = Collections.nCopies(20, 100);

        AttributesControlLimitFormulas.Result r = AttributesControlLimitFormulas.calcNp(defects, inspected);
        assertNotNull(r.getCl());
        assertNotNull(r.getUcl());
        assertEquals(BigDecimal.ZERO.setScale(6), r.getLcl(), "NP 图 LCL<0 钳 0");

        BigDecimal npCl = new BigDecimal("100").multiply(
                new BigDecimal("93").divide(new BigDecimal("2000"), 10, java.math.RoundingMode.HALF_UP))
                .setScale(6, java.math.RoundingMode.HALF_UP);
        assertEquals(npCl, r.getCl(), "NP 图 CL=n·p̄");
        assertEquals(new BigDecimal("4.650000"), r.getCl());
        assertTrue(r.getUcl().compareTo(r.getCl()) > 0, "NP 图 UCL > CL");
    }

    @Test
    public void cChartFormulaComputesControlLimits() {
        // 20 子组 defects 序列合计 = 5+3+7+4+6+2+5+4+3+6+5+4+7+3+5+6+4+5+3+6 = 93
        // c̄ = 93 / 20 = 4.65
        // σ = sqrt(c̄) = sqrt(4.65) ≈ 2.156385865
        // UCL = 4.65 + 3×2.156385865 ≈ 11.119157596
        // LCL = 4.65 − 3×2.156385865 ≈ -1.819157596 → 钳 0
        java.util.List<Integer> defects = Arrays.asList(5, 3, 7, 4, 6, 2, 5, 4, 3, 6, 5, 4, 7, 3, 5, 6, 4, 5, 3, 6);

        AttributesControlLimitFormulas.Result r = AttributesControlLimitFormulas.calcC(defects);
        assertNotNull(r.getCl());
        assertNotNull(r.getUcl());
        assertEquals(BigDecimal.ZERO.setScale(6), r.getLcl(), "C 图 LCL<0 钳 0");

        assertEquals(new BigDecimal("4.650000"), r.getCl(), "C 图 CL=c̄=Σc/k");
        assertTrue(r.getUcl().compareTo(r.getCl()) > 0, "C 图 UCL > CL");
    }

    @Test
    public void uChartFormulaComputesControlLimits() {
        // 同 P 图数据：ū = 93 / 2000 = 0.0465；n̄ = 100
        // σ = sqrt(ū/n̄) = sqrt(0.0465/100) = sqrt(0.000465) ≈ 0.021563386
        // UCL = 0.0465 + 3×0.021563386 ≈ 0.111190158
        // LCL = 0.0465 − 3×0.021563386 ≈ -0.018190158 → 钳 0
        java.util.List<Integer> defects = Arrays.asList(5, 3, 7, 4, 6, 2, 5, 4, 3, 6, 5, 4, 7, 3, 5, 6, 4, 5, 3, 6);
        java.util.List<Integer> inspected = Collections.nCopies(20, 100);

        AttributesControlLimitFormulas.Result r = AttributesControlLimitFormulas.calcU(defects, inspected);
        assertNotNull(r.getCl());
        assertNotNull(r.getUcl());
        assertEquals(BigDecimal.ZERO.setScale(6), r.getLcl(), "U 图 LCL<0 钳 0");

        BigDecimal uBar = new BigDecimal("93").divide(new BigDecimal("2000"), 10, java.math.RoundingMode.HALF_UP)
                .setScale(6, java.math.RoundingMode.HALF_UP);
        assertEquals(uBar, r.getCl(), "U 图 CL=ū=Σc/Σn");
        assertEquals(new BigDecimal("0.046500"), r.getCl());
        assertTrue(r.getUcl().compareTo(r.getCl()) > 0, "U 图 UCL > CL");
    }

    @Test
    public void emptyListReturnsNulls() {
        AttributesControlLimitFormulas.Result p = AttributesControlLimitFormulas.calcP(Collections.emptyList(), Collections.emptyList());
        assertNull(p.getCl());
        assertNull(p.getUcl());
        assertNull(p.getLcl());

        AttributesControlLimitFormulas.Result c = AttributesControlLimitFormulas.calcC(Collections.emptyList());
        assertNull(c.getCl());
    }

    @Test
    public void nonNegativeLclWhenHighDefectRate() {
        // 高缺陷率场景：defects=80/100 inspected，p̄=0.8 → σ=sqrt(0.8×0.2/100)=0.04
        // LCL = 0.8 − 3×0.04 = 0.68（正数，不钳 0）
        java.util.List<Integer> defects = Collections.nCopies(20, 80);
        java.util.List<Integer> inspected = Collections.nCopies(20, 100);

        AttributesControlLimitFormulas.Result r = AttributesControlLimitFormulas.calcP(defects, inspected);
        assertEquals(new BigDecimal("0.800000"), r.getCl());
        assertNotNull(r.getLcl());
        assertTrue(r.getLcl().signum() > 0, "高缺陷率 LCL > 0 不钳 0");
        assertTrue(r.getUcl().compareTo(r.getCl()) > 0);
        assertTrue(r.getCl().compareTo(r.getLcl()) > 0);
    }

    @Test
    public void sizeMismatchThrows() {
        try {
            AttributesControlLimitFormulas.calcP(Arrays.asList(1, 2), Collections.singletonList(100));
            org.junit.jupiter.api.Assertions.fail("P 图 defects/inspected 长度不匹配应抛异常");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("P"));
        }
    }
}
