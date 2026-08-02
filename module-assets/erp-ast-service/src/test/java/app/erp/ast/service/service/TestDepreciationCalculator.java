package app.erp.ast.service.service;

import app.erp.ast.service.ErpAstConstants;
import io.nop.core.unittest.BaseTestCase;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 折旧金额计算器纯函数单元测试（plan 2026-07-31-0744-2-r2-12 G4 残差）。
 *
 * <p>repo 此前无 {@link DepreciationCalculator} 直接单元测试。本类确定性覆盖两个未被集成测试触及的分支：
 * <ul>
 *   <li>残值约束截断分支（{@code DepreciationCalculator:71-73}：nbv−amount&lt;residual → amount=nbv−residual）。</li>
 *   <li>已达残值返 0 分支（{@code DepreciationCalculator:32-34}：nbv≤residual → ZERO）。</li>
 * </ul>
 * 纯函数式：无 DB、无 IoC，{@link BaseTestCase} 仅作帮助类基类。
 */
public class TestDepreciationCalculator extends BaseTestCase {

    private static final String SL = ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE;

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
}
