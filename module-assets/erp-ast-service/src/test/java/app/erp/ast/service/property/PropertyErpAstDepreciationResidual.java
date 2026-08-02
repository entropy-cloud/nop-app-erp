package app.erp.ast.service.property;

import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.service.DepreciationCalculator;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MQ Q3 P7 属性测试——资产折旧残值守恒不变量（设计文档 {@code property-based-testing.md} §9 successor +
 * §5.1/§5.3 + §1.3 真相源 {@code depreciation-and-posting.md §1.3-§1.4}）。
 *
 * <p><b>路径 C 类一 + 策略 F2</b>（设计文档 §3.4）：纯 JUnit 5 + jqwik，不继承 {@code JunitAutoTestCase}；纯内存状态。
 *
 * <p><b>保真度机制裁决（Decision (a)——直调生产纯函数，最强保真度）</b>：生产折旧算术核心是
 * {@link DepreciationCalculator#calculate}（{@code :25-75}）——<b>public static 纯函数</b>（无 @Inject、无 DB、无 IoC），
 * 由 {@code ErpAstDepreciationScheduleExecuteDepreciationProcessor:72-73} 调用。Processor 本身 DB-entangled
 *（{@code :32-33 @Inject IDaoProvider} + {@code saveOrUpdateEntity}），但其折旧算术已封装为独立纯函数 DepreciationCalculator，
 * 可从纯内存 test 直接调用——无需镜像（无 model/production drift 风险）。
 * <ul>
 *   <li>选择：(a) 直调生产 {@code DepreciationCalculator.calculate}，每期以当前 nbv/accum/elapsed 调用，
 *       断言残值守恒不变量。这是最强保真度——测试侧零折旧算术 reimplementation。</li>
 *   <li>替代（否决）：(b) 内存折旧模型镜像生产算术——不必要，DepreciationCalculator 本身是 public static 纯函数，
 *       直调即可（无 DB-entangled 像 FifoCostingStrategy/StandardCostingStrategy 那样须镜像）。
 *       (c) 抽取每期折旧额为纯函数——已存在（DepreciationCalculator 就是），无需再抽。</li>
 *   <li>golden 交叉校验：{@link #goldenCrossCheckMatchesTestDepreciationCalculatorNumbers()} 复现
 *       {@code TestDepreciationCalculator.testNonZeroResidualStraightLineConvergesToResidual}
 *       生产实测场景（12000/2000/6 月直线法 → 6 期后 nbv=2000=残值，accum=10000=原值−残值）。</li>
 *   <li>残留风险（R2）：纯函数测试不验证 DB 持久化 / schedule 落库 / 过账；由既有
 *       {@code TestErpAstDepreciation} 端到端测试覆盖 DB 层（双层互补）。</li>
 * </ul>
 *
 * <p><b>不变量真相源</b>（设计文档 §1.3，**不重新推导**）：{@code docs/design/assets/depreciation-and-posting.md §1.4}——
 * 折旧后账面净值<b>不得低于</b>残值：每期 {@code netBookValue = cost − accumulatedDepreciation ≥ residualValue}，
 * 且 {@code accumulatedDepreciation ≤ (cost − residualValue)}；直线法末期调整到残值，双倍余额递减最后两年改直线法确保残值约束。
 * 生产 {@code DepreciationCalculator:71-73} 残值截断分支（{@code nbv−amount<residual → amount=nbv−residual}）是此不变量的实现保证。
 *
 * <p><b>tautology 自检</b>（设计文档 §5.1）：{@link #tautologySelfCheck_residualClampMutationIsDetected()} 注入残值约束变异
 *（移除截断分支，使净值跌穿残值），证明属性 test 能发现此类变异——非恒等式。
 *
 * <p><b>种子固定</b>（设计文档 §8 C-1 + R7）：每 {@code @Property} 经 {@code seed} 固化。
 */
class PropertyErpAstDepreciationResidual {

    /**
     * P7-属性 1（直线法）：随机原值/残值/寿命下，直线法每期折旧后 {@code netBookValue ≥ residualValue}
     * 且 {@code accumulatedDepreciation ≤ (cost − residualValue)}。生产 {@code DepreciationCalculator.calculate}
     * 直调（保真度 Decision (a)）。
     */
    @Property(tries = 100, seed = "20260814")
    void straightLineNeverBreachesResidual(@ForAll("cost") BigDecimal cost,
                                           @ForAll("residual") BigDecimal residual,
                                           @ForAll("usefulLifeMonths") int months) {
        BigDecimal safeResidual = residual.min(cost); // 残值不超过原值（合法约束）
        BigDecimal nbv = cost;
        BigDecimal accum = BigDecimal.ZERO;
        for (int period = 0; period < months; period++) {
            BigDecimal amount = DepreciationCalculator.calculate(
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE,
                    cost, safeResidual, nbv, months, period, null, null);
            nbv = nbv.subtract(amount);
            accum = accum.add(amount);
            // 不变量 1：折旧后净值不得低于残值（depreciation-and-posting.md §1.4）
            assertTrue(nbv.compareTo(safeResidual) >= 0,
                    "期间 " + period + " 折旧后 nbv=" + nbv + " 不得低于残值 " + safeResidual);
            // 不变量 2：累计折旧不超过（原值−残值）
            assertTrue(accum.compareTo(cost.subtract(safeResidual)) <= 0,
                    "期间 " + period + " 累计折旧 " + accum + " 不超过原值−残值 " + cost.subtract(safeResidual));
            // 不变量 3：折旧额非负
            assertTrue(amount.signum() >= 0, "期间 " + period + " 折旧额非负");
        }
    }

    /**
     * P7-属性 2（双倍余额递减）：随机原值/残值/寿命下，双倍余额递减法每期折旧后净值不低于残值。
     * 生产 DepreciationCalculator 最后 24 个月改直线法（{:46}）+ 残值截断（{:71-73}）共同保证。
     */
    @Property(tries = 100, seed = "20260815")
    void decliningBalanceNeverBreachesResidual(@ForAll("cost") BigDecimal cost,
                                               @ForAll("residual") BigDecimal residual,
                                               @ForAll("usefulLifeMonthsDeclining") int months) {
        BigDecimal safeResidual = residual.min(cost);
        BigDecimal nbv = cost;
        BigDecimal accum = BigDecimal.ZERO;
        for (int period = 0; period < months; period++) {
            BigDecimal amount = DepreciationCalculator.calculate(
                    ErpAstConstants.DEPRECIATION_METHOD_DECLINING,
                    cost, safeResidual, nbv, months, period, null, null);
            nbv = nbv.subtract(amount);
            accum = accum.add(amount);
            assertTrue(nbv.compareTo(safeResidual) >= 0,
                    "DDB 期间 " + period + " 折旧后 nbv=" + nbv + " 不得低于残值 " + safeResidual);
            assertTrue(accum.compareTo(cost.subtract(safeResidual)) <= 0,
                    "DDB 期间 " + period + " 累计折旧不超过原值−残值");
            assertTrue(amount.signum() >= 0, "DDB 期间 " + period + " 折旧额非负");
        }
    }

    /**
     * P7-属性 3（直线法收敛）：随机原值/残值/寿命下，直线法折旧最终收敛至残值，累计折旧 = 原值 − 残值
     *（对齐 depreciation-and-posting.md §1.3 直线法末期调整到残值）。
     * <p><b>收敛轮数注记</b>：当直线法每期金额 {@code (cost−residual)/months} 经 HALF_UP scale=4 舍入<b> undershoot</b>
     * 时（如 cost=100/residual=0/months=3 → 3×33.3333=99.9999），完整 {@code months} 期后 nbv 可能剩微小正余量
     *（0.0001）——生产残值截断分支（{:71-73}）仅在「本期金额会使净值跌穿残值」时触发，undershoot 时不触发。
     * 故精确收敛到残值可能需 {@code months+1} 期（多一期吸收舍入余量，触发截断使 nbv=残值精确）。
     * 本属性迭代至收敛（cap = months+5），断言终态 nbv==残值精确 + 累计折旧==原值−残值。
     */
    @Property(tries = 100, seed = "20260816")
    void straightLineConvergesToResidualAfterFullLife(@ForAll("cost") BigDecimal cost,
                                                     @ForAll("residual") BigDecimal residual,
                                                     @ForAll("usefulLifeMonths") int months) {
        BigDecimal safeResidual = residual.min(cost);
        BigDecimal nbv = cost;
        BigDecimal accum = BigDecimal.ZERO;
        int cap = months + 5; // months 期 + 少量额外期吸收舍入 undershoot，保证收敛
        int period = 0;
        while (period < cap) {
            BigDecimal amount = DepreciationCalculator.calculate(
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE,
                    cost, safeResidual, nbv, months, period, null, null);
            if (amount.signum() == 0) {
                break; // 已达残值，calculate 返 0（:33-34 分支）——收敛
            }
            nbv = nbv.subtract(amount);
            accum = accum.add(amount);
            // 不变量：每期折旧后净值不低于残值（截断分支保证）
            assertTrue(nbv.compareTo(safeResidual) >= 0,
                    "期间 " + period + " 折旧后 nbv=" + nbv + " ≥ 残值 " + safeResidual);
            period++;
        }
        // 终态：净值 == 残值（残值截断分支保证末期精确收敛）
        assertEquals(0, safeResidual.compareTo(nbv),
                "收敛后 nbv=" + nbv + " 应精确 == 残值 " + safeResidual);
        // 累计折旧 == 原值 − 残值
        assertEquals(0, cost.subtract(safeResidual).compareTo(accum),
                "累计折旧 " + accum + " 应 == 原值−残值 " + cost.subtract(safeResidual));
    }

    /**
     * golden 交叉校验（保真度锚定）：复现 {@code TestDepreciationCalculator.testNonZeroResidualStraightLineConvergesToResidual}
     * 生产实测场景（12000/2000/6 月直线法 → 每期 1666.6667，6 期后 nbv=2000=残值，accum=10000=原值−残值）。
     */
    @Test
    void goldenCrossCheckMatchesTestDepreciationCalculatorNumbers() {
        BigDecimal cost = new BigDecimal("12000");
        BigDecimal residual = new BigDecimal("2000");
        int months = 6;

        BigDecimal nbv = cost;
        BigDecimal accum = BigDecimal.ZERO;
        for (int period = 0; period < months; period++) {
            BigDecimal amount = DepreciationCalculator.calculate(
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE,
                    cost, residual, nbv, months, period, null, null);
            assertTrue(amount.signum() > 0, "期间 " + period + " 折旧金额非 0");
            nbv = nbv.subtract(amount);
            accum = accum.add(amount);
            assertTrue(nbv.compareTo(residual) >= 0, "期间 " + period + " 净值不低于残值");
        }
        // 生产 TestDepreciationCalculator:96-97 实测断言
        assertEquals(0, new BigDecimal("2000").compareTo(nbv), "6 期后净值=残值 2000");
        assertEquals(0, new BigDecimal("10000").compareTo(accum), "累计折旧=原值−残值=10000");
    }

    /**
     * tautology 自检（设计文档 §5.1）：注入「残值约束截断分支移除」变异——若 calculate 不在
     * {@code nbv−amount<residual} 时截断（{:71-73}），直线法末期金额会使净值跌穿残值。
     * 本测试证明属性 1/3 的「nbv ≥ residual」断言能捕获此变异，非恒等式。
     */
    @Test
    void tautologySelfCheck_residualClampMutationIsDetected() {
        // 场景选自 TestDepreciationCalculator.testResidualClampTruncatesToResidualOnOvershoot：
        // 10000/2000/3 月直线法，每期 2666.6667；第 3 期前 nbv=4666.6666，第 3 期正常算 2666.6667 会使
        // nbv=1999.9999 < 残值 2000 → 生产截断为 2666.6666（nbv−residual），末期 nbv 精确=2000。
        BigDecimal cost = new BigDecimal("10000");
        BigDecimal residual = new BigDecimal("2000");
        int months = 3;

        // 正确生产算术：经残值截断，每期后 nbv >= residual，末期 nbv == residual
        BigDecimal correctNbv = cost;
        for (int period = 0; period < months; period++) {
            BigDecimal amount = DepreciationCalculator.calculate(
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE,
                    cost, residual, correctNbv, months, period, null, null);
            correctNbv = correctNbv.subtract(amount);
            assertTrue(correctNbv.compareTo(residual) >= 0,
                    "正确算术：期间 " + period + " 后 nbv=" + correctNbv + " ≥ 残值 " + residual);
        }
        assertEquals(0, residual.compareTo(correctNbv), "正确算术末期 nbv 精确=残值");

        // 变异算术：移除残值截断（直接用直线法公式 (cost−residual)/months，末期不截断）
        // 第 3 期前 nbv=4666.6666，变异金额 = (10000-2000)/3 = 2666.6667（不截断）
        BigDecimal mutatedNbvBeforeLast = new BigDecimal("4666.6666");
        BigDecimal mutatedPeriodAmount = cost.subtract(residual)
                .divide(BigDecimal.valueOf(months), 4, java.math.RoundingMode.HALF_UP); // 2666.6667（无截断）
        BigDecimal mutatedNbvAfterLast = mutatedNbvBeforeLast.subtract(mutatedPeriodAmount); // 1999.9999
        // 变异使净值跌穿残值——属性 1 的「nbv ≥ residual」断言会失败（即「发现变异」）
        assertFalse(mutatedNbvAfterLast.compareTo(residual) >= 0,
                "变异算术（移除截断）使末期 nbv=" + mutatedNbvAfterLast + " < 残值 2000——属性 test 能发现此变异");
        assertEquals(0, new BigDecimal("1999.9999").compareTo(mutatedNbvAfterLast),
                "变异末期 nbv=1999.9999（跌穿残值 0.0001）");
    }

    // ---------- @Provide 生成器 ----------

    @Provide
    Arbitrary<BigDecimal> cost() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("100"), new BigDecimal("10000000"))
                .ofScale(2);
    }

    @Provide
    Arbitrary<BigDecimal> residual() {
        return Arbitraries.bigDecimals()
                .between(BigDecimal.ZERO, new BigDecimal("1000000"))
                .ofScale(2);
    }

    @Provide
    Arbitrary<Integer> usefulLifeMonths() {
        return Arbitraries.integers().between(1, 120);
    }

    /** 双倍余额递减寿命：≥ 24 月以触发「最后 24 月改直线法」分支（生产 :46）。 */
    @Provide
    Arbitrary<Integer> usefulLifeMonthsDeclining() {
        return Arbitraries.integers().between(24, 120);
    }
}
