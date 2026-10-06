package app.erp.ast.service.service;

import app.erp.ast.service.ErpAstConstants;
import io.nop.core.unittest.BaseTestCase;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 折旧金额计算器纯函数单元测试（plan 2026-07-31-0744-2-r2-12 G4 残差）。
 *
 * <p>repo 此前无 {@link DepreciationCalculator} 直接单元测试。本类确定性覆盖两个未被集成测试触及的分支：
 * <ul>
 *   <li>残值约束截断分支（{@code DepreciationCalculator} nbv−amount&lt;residual → amount=nbv−residual）。</li>
 *   <li>已达残值返 0 分支（nbv≤residual → ZERO）。</li>
 * </ul>
 * 纯函数式：无 DB、无 IoC，{@link BaseTestCase} 仅作帮助类基类。
 *
 * <p>P2-CK-ast-016≡ast2-010（分支 A）净值轨迹口径回归：直线法改 (净值−残值)/剩余期——A3 双夹具尺度声明
 * （整除夹具精确相等 + 舍入反馈夹具 1e-4 容差）见 ast016 前缀用例。
 */
public class TestDepreciationCalculator extends BaseTestCase {

    private static final String SL = ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE;
    /** 与 DepreciationCalculator 内部 SCALE 对齐（计算器口径 4 位 HALF_UP）。 */
    private static final int SCALE = 4;

    /**
     * 截断分支：当直线法每期金额会使净值跌穿残值时，amount 被截断为 nbv−residual，
     * 保证末期净值精确收敛到残值（非负、非 0）。参数选取 (10000/2000/3) 因 8000/3 向上舍入为 2666.6667，
     * 第 3 期 nbv−amount=1999.9999&lt;2000 触发截断。
     */
    @Test
    public void testResidualClampTruncatesToResidualOnOvershoot() {
        BigDecimal original = new BigDecimal("10000");
        BigDecimal residual = new BigDecimal("2000");
        int months = 3;
        // 直线法每期 = (10000-2000)/3 = 2666.6667（HALF_UP 向上舍入）
        BigDecimal perPeriod = DepreciationCalculator.calculate(SL, original, residual,
                original, months, 0, null, null);
        assertEquals(0, perPeriod.compareTo(new BigDecimal("2666.6667")), "首期直线法金额 2666.6667");

        // 推进到第 3 期前净值：10000 - 2×2666.6667 = 4666.6666
        BigDecimal nbvBeforeLast = original.subtract(perPeriod.multiply(BigDecimal.valueOf(2)));
        assertEquals(0, nbvBeforeLast.compareTo(new BigDecimal("4666.6666")), "第 3 期前净值 4666.6666");

        // 第 3 期：直线法金额 2666.6667 会使净值 = 4666.6666 - 2666.6667 = 1999.9999 < 残值 2000 → 截断
        BigDecimal lastAmount = DepreciationCalculator.calculate(SL, original, residual,
                nbvBeforeLast, months, 2, null, null);
        assertEquals(0, lastAmount.compareTo(new BigDecimal("2666.6666")),
                "截断分支触发：amount=nbv−residual=2666.6666（非 2666.6667）");
        BigDecimal nbvAfterLast = nbvBeforeLast.subtract(lastAmount);
        assertEquals(0, nbvAfterLast.compareTo(residual), "末期净值精确=残值 2000（非 0）");
        assertTrue(nbvAfterLast.signum() > 0, "末期净值非 0（=残值）");
    }

    /**
     * 已达残值返 0 分支：净值已等于/低于残值时，calculate 返回 ZERO，不再计提。
     */
    @Test
    public void testReturnsZeroWhenNetBookValueAtOrBelowResidual() {
        BigDecimal original = new BigDecimal("10000");
        BigDecimal residual = new BigDecimal("2000");

        // nbv == residual → 返回 0
        BigDecimal atResidual = DepreciationCalculator.calculate(SL, original, residual,
                residual, 12, 11, null, null);
        assertEquals(0, atResidual.compareTo(BigDecimal.ZERO), "nbv==residual 时返回 0");

        // nbv < residual → 返回 0
        BigDecimal belowResidual = DepreciationCalculator.calculate(SL, original, residual,
                new BigDecimal("1500"), 12, 11, null, null);
        assertEquals(0, belowResidual.compareTo(BigDecimal.ZERO), "nbv<residual 时返回 0");
    }

    /**
     * 非零残值直线法全周期收敛校验：6 期折旧后净值收敛到残值，累计折旧=原值−残值。
     * 选取 (12000/2000/6)：每期 (12000−2000)/6=1666.6667（HALF_UP 向上舍入），6×1666.6667&gt;10000，
     * 末期触发截断分支补差使净值精确=2000。
     */
    @Test
    public void testNonZeroResidualStraightLineConvergesToResidual() {
        BigDecimal original = new BigDecimal("12000");
        BigDecimal residual = new BigDecimal("2000");
        int months = 6;

        BigDecimal nbv = original;
        BigDecimal accum = BigDecimal.ZERO;
        for (int i = 0; i < months; i++) {
            BigDecimal amount = DepreciationCalculator.calculate(SL, original, residual,
                    nbv, months, i, null, null);
            assertTrue(amount.signum() > 0, "期间 " + i + " 折旧金额非 0");
            nbv = nbv.subtract(amount);
            accum = accum.add(amount);
            // 残值约束：折旧后净值不得低于残值
            assertTrue(nbv.compareTo(residual) >= 0, "期间 " + i + " 净值不低于残值");
        }
        assertEquals(0, nbv.compareTo(residual), "6 期后净值=残值 2000");
        assertEquals(0, accum.compareTo(original.subtract(residual)), "累计折旧=原值−残值=10000");
    }

    /**
     * P2-CK-ast-016≡ast2-010（分支 A）A3 夹具①：整除夹具名义轨迹精确 no-op。
     * 原值 7200 / 残值 1200 / 60 期 → 月额 m=100（整除无舍入）。净值轨迹口径 (nbv−残值)/剩余期
     * 与旧口径 (原值−残值)/总期数 逐值精确相等（代数恒等：净值=原值−k·m ⟹ 商≡m），减值前折旧行为零变更。
     */
    @Test
    public void testAst016StraightLineNominalTrajectoryExactNoOp() {
        BigDecimal original = new BigDecimal("7200");
        BigDecimal residual = new BigDecimal("1200");
        int months = 60;
        BigDecimal legacyPerPeriod = original.subtract(residual)
                .divide(BigDecimal.valueOf(months), SCALE, RoundingMode.HALF_UP);
        assertEquals(0, legacyPerPeriod.compareTo(new BigDecimal("100")), "整除夹具月额 m=100");

        for (int k = 0; k < months; k++) {
            BigDecimal nbv = original.subtract(legacyPerPeriod.multiply(BigDecimal.valueOf(k)));
            BigDecimal amount = DepreciationCalculator.calculate(SL, original, residual, nbv, months, k, null, null);
            assertEquals(0, amount.compareTo(new BigDecimal("100")),
                    "期间 " + k + " 净值轨迹口径月额精确=100（(nbv−残值)/剩余期 代数恒等）");
            assertEquals(0, amount.compareTo(legacyPerPeriod),
                    "期间 " + k + " 新旧口径逐值相等（名义轨迹 no-op）");
        }
    }

    /**
     * P2-CK-ast-016≡ast2-010（分支 A）A3 夹具②：减值/重估后月额跟随净值轨迹（不再恒 100）。
     * 同资产第 13 期：自然轨迹 nbv=5900。减值下修至 nbv=5000 → (5000−1200)/47=80.8511 &lt; 100
     * （减值后未来折旧下降——修复前恒 100 不降）；重估上调至 nbv=8000 → (8000−1200)/47=144.6809
     * （SCALE=4 HALF_UP，月额随净值上移，同样证明轨迹跟随）。
     */
    @Test
    public void testAst016ImpairedAssetDepreciationFollowsNetValueTrajectory() {
        BigDecimal original = new BigDecimal("7200");
        BigDecimal residual = new BigDecimal("1200");
        int months = 60;
        int elapsedAtImpairment = 13;

        // 减值下修：nbv=5000 → 月额下降
        BigDecimal impairedDown = DepreciationCalculator.calculate(SL, original, residual,
                new BigDecimal("5000"), months, elapsedAtImpairment, null, null);
        assertEquals(0, impairedDown.compareTo(new BigDecimal("80.8511")),
                "减值后月额=(5000−1200)/47=80.8511（SCALE=4 HALF_UP）");
        assertTrue(impairedDown.compareTo(new BigDecimal("100")) < 0,
                "减值后月额 < 减值前 100（净值轨迹下降；修复前恒 100 不降）");

        // 重估上调：nbv=8000 → 月额随净值上移（轨迹跟随，非恒值）
        BigDecimal revaluedUp = DepreciationCalculator.calculate(SL, original, residual,
                new BigDecimal("8000"), months, elapsedAtImpairment, null, null);
        assertEquals(0, revaluedUp.compareTo(new BigDecimal("144.6809")),
                "重估后月额=(8000−1200)/47=144.6809（SCALE=4 HALF_UP）");
        assertTrue(revaluedUp.compareTo(new BigDecimal("100")) > 0,
                "重估上调后月额 > 100（轨迹跟随）");

        // 未减值对照：同期待遇不变（nbv=7200−13×100=5900 → (5900−1200)/47=100 精确）
        BigDecimal unimpairedNbv = original.subtract(new BigDecimal("100").multiply(BigDecimal.valueOf(13)));
        BigDecimal unimpaired = DepreciationCalculator.calculate(SL, original, residual,
                unimpairedNbv, months, elapsedAtImpairment, null, null);
        assertEquals(0, unimpaired.compareTo(new BigDecimal("100")), "未减值同期待遇不变（no-op 半边）");
    }

    /**
     * P2-CK-ast-016≡ast2-010（分支 A）A3 夹具③：舍入反馈夹具（非整除数字）名义轨迹。
     * 原值 10000 / 残值 333.33 / 7 期——旧口径恒 1380.9529，新口径逐期以净值反馈（SCALE=4 舍入），
     * 逐期 |新−旧| ≤ 1e-4（明示容差，名义轨迹等价）；净值全程 ≥ 残值，末期精确收敛残值 333.33，
     * 累计折旧 = 原值−残值 = 9666.67。
     */
    @Test
    public void testAst016RoundingFeedbackNominalTrajectoryWithinTolerance() {
        BigDecimal original = new BigDecimal("10000");
        BigDecimal residual = new BigDecimal("333.33");
        int months = 7;
        BigDecimal tolerance = new BigDecimal("0.0001");
        BigDecimal legacyPerPeriod = original.subtract(residual)
                .divide(BigDecimal.valueOf(months), SCALE, RoundingMode.HALF_UP);

        BigDecimal nbv = original;
        BigDecimal accum = BigDecimal.ZERO;
        for (int k = 0; k < months; k++) {
            BigDecimal amount = DepreciationCalculator.calculate(SL, original, residual, nbv, months, k, null, null);
            BigDecimal drift = amount.subtract(legacyPerPeriod).abs();
            assertTrue(drift.compareTo(tolerance) <= 0,
                    "期间 " + k + " 舍入反馈漂移 " + amount + " ≤ 1e-4（名义轨迹等价）");
            nbv = nbv.subtract(amount);
            accum = accum.add(amount);
            assertTrue(nbv.compareTo(residual) >= 0, "期间 " + k + " 净值不低于残值");
        }
        assertEquals(0, nbv.compareTo(residual), "7 期后净值精确=残值 333.33（截断分支收敛）");
        assertEquals(0, accum.compareTo(original.subtract(residual)), "累计折旧=原值−残值=9666.67");
    }
}
