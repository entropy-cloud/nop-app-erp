package app.erp.fin.service.posting;

import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.exceptions.NopException;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MQ Q3 P6 属性测试——多币种折算借贷平衡不变量（设计文档 {@code property-based-testing.md} §9 successor +
 * §5.1/§5.3 + §1.3 真相源 {@code posting.md §多币种处理 line 436-452}）。
 *
 * <p><b>路径 C 类一 + 策略 F2</b>（设计文档 §3.4）：纯 JUnit 5 + jqwik，不继承 {@code JunitAutoTestCase}；纯内存状态。
 *
 * <p><b>包位置裁决（同 P1 先例）</b>：本类位于包 {@code app.erp.fin.service.posting}（与生产 {@link ErpFinPostingProcessor}
 * 同包），而非 plan header 建议的 {@code .../posting/property/} 子包。原因：{@code balanceTotals}（{@code :722}）/
 * {@code assertBalanced}（{@code :736}）是 {@code protected}——{@code protected} 含同包访问权（Java 语义），同包类可在
 * {@link ErpFinPostingProcessor} 实例上直接调用，无须子类暴露。{@code ErpFinPostingProcessor} 带 {@code @SingleSession}
 * AOP 注解，其测试子类触发 Nop {@code gen-aop-proxy-for-test} 增强失败（P1 先例已验证，回填设计文档 Review Record）。
 * 故 P6 同 P1 改同包直接访问——零子类 → 零 AOP 代理生成。plan header 的 {@code .../posting/property/} 目标路径系笔误
 *（faithfulness 硬约束 §5.1 优先于路径，直调生产纯函数须同包访问 protected 方法）。
 *
 * <p><b>保真度机制裁决（Decision (a)——直调生产纯函数，最强保真度）</b>：每个 {@code @Property} 直接调用<b>生产</b>
 * {@link ErpFinPostingProcessor#balanceTotals}（{@code :722-734}，纯函数累加）+ {@link ErpFinPostingProcessor#assertBalanced}
 *（{@code :736-742}）——{@code new ErpFinPostingProcessor()} 不经 DI（两方法是纯算术，不触 @Inject 字段）。
 * <ul>
 *   <li>选择：(a) 直调生产 {@code balanceTotals}，{@code VoucherFact.amount} 设为本位币额（{@code functional = source × rate}，
 *       镜像生产 Provider 折算——{@code posting.md:449} 「amount 字段保留作功能金额（balanceTotals/assertBalanced 以本位币为准）」
 *       + {@code :452} 「辅助账项按 event.exchangeRate 折算 amountFunctional = source × rate」）。
 *       {@code balanceTotals:726} 累加 {@code fact.getAmount()}（即本位币额），与本属性 oracle 独立按 dcDirection 累加本位币额对齐。</li>
 *   <li>替代（否决）：(b) 内存折算模型镜像——不必要，{@code balanceTotals} 本身是可独立实例化的纯函数（{@code new ErpFinPostingProcessor()}
 *       + 同包 protected 访问），直调即可，无须并行 reimplementation（避免 model/production drift）。
 *       (c) 测试侧累加 amountFunctional——但生产 {@code balanceTotals:726} 消费 {@code getAmount()} 非 {@code getAmountFunctional()}，
 *       测试侧改累加 amountFunctional 会与生产路径解耦（drift 风险）；本属性正确做法是设 {@code amount = functional} 后直调生产。</li>
 *   <li>关键事实核验（plan Phase 2 Decision 要求）：生产 {@code balanceTotals:726} 累加 {@code fact.getAmount()}（非
 *       {@code getAmountFunctional()}）；但 per {@code posting.md:449}，{@code amount} 字段保留作「功能金额」（balanceTotals
 *       以本位币为准）。故多币种折算后，{@code amount = functional}（= source × rate，由 Provider 计算），{@code balanceTotals}
 *       消费 {@code amount} 即消费本位币额——与本属性「折算后功能币额 Σ debitFunctional == Σ creditFunctional」不变量一致。
 *       无现成「累加 amountFunctional」入口的事实，通过「amount = functional」语义桥梁解决，无需抽取新函数。</li>
 *   <li>残留风险（R2）：纯内存不验证 DB 持久化层 / Provider 折算落库；由既有 P2P/O2C Provider 端到端测试覆盖（双层互补）。</li>
 * </ul>
 *
 * <p><b>不变量真相源</b>（设计文档 §1.3，**不重新推导**）：{@code docs/design/finance/posting.md §多币种处理}——
 * 每张凭证折算后功能币额 {@code Σ debitFunctional == Σ creditFunctional}（本位币为准，{@code posting.md:449}）。
 *
 * <p><b>多币种相对 P1 的增量价值</b>：P1 单币种（source==functional==amount 三者相等）；P6 引入每行独立汇率，
 * 折算 {@code functional = source × rate} 是非平凡算术。关键属性：source 币种平衡 ≠ 功能币平衡——当各行汇率不同时，
 * source 平衡的凭证可能功能币不平衡，{@code assertBalanced}（功能币）能捕获 source 平衡掩盖的失衡。
 *
 * <p><b>tautology 自检</b>（设计文档 §5.1）：{@link #tautologySelfCheck_translationMutationIsDetected()} 注入折算变异
 *（{@code functional = source × rate × rate}），证明属性 test 能发现折算算术错误——非恒等式。
 *
 * <p><b>种子固定</b>（设计文档 §8 C-1 + R7）：每 {@code @Property} 经 {@code seed} 固化。
 */
class PropertyErpFinMultiCurrencyBalance {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000");
    private static final int FUNCTIONAL_SCALE = 4; // 本位币额精度（对齐 ErpFinConfigs 金额 scale）

    /**
     * 多币种凭证行规格：源币种额 + 每行汇率 + 方向。oracle 在生成期捕获方向标志独立累加功能币额
     *（与生产读 {@code dcDirection} + 消费 {@code amount=功能币} 的代码路径解耦）。
     */
    static final class MultiCurrencyFactSpec {
        final BigDecimal sourceAmount;
        final BigDecimal exchangeRate;
        final boolean credit;

        MultiCurrencyFactSpec(BigDecimal sourceAmount, BigDecimal exchangeRate, boolean credit) {
            this.sourceAmount = sourceAmount;
            this.exchangeRate = exchangeRate;
            this.credit = credit;
        }

        /** 功能币额 = 源币种 × 汇率（镜像 Provider 折算 posting.md:452）。 */
        BigDecimal functionalAmount() {
            return sourceAmount.multiply(exchangeRate).setScale(FUNCTIONAL_SCALE, RoundingMode.HALF_UP);
        }

        VoucherFact toFact() {
            VoucherFact f = new VoucherFact();
            BigDecimal functional = functionalAmount();
            f.setAmount(functional);              // amount = 功能币额（posting.md:449 balanceTotals 以本位币为准）
            f.setAmountSource(sourceAmount);       // 源币种额（写实）
            f.setAmountFunctional(functional);     // 功能币额（写实）
            f.setDcDirection(credit ? ErpFinConstants.DC_CREDIT : ErpFinConstants.DC_DEBIT);
            return f;
        }
    }

    /**
     * P6-属性 1：随机多币种凭证行（每行独立汇率）下，生产 {@code balanceTotals} 按方向正确累加功能币额。
     * oracle 在生成期以 boolean 标志独立累加功能币额（= source × rate）；生产 {@code balanceTotals} 消费
     * {@code amount=功能币} 路由累加——两者必须一致（证明多币种折算 + 借贷路由正确）。
     */
    @Property(tries = 100, seed = "20260811")
    void balanceTotalsRoutesFunctionalAmountsByDcDirection(
            @ForAll("multiCurrencyFactSpecs") List<MultiCurrencyFactSpec> specs) {
        ErpFinPostingProcessor processor = new ErpFinPostingProcessor();

        BigDecimal oracleDebitFunctional = BigDecimal.ZERO;
        BigDecimal oracleCreditFunctional = BigDecimal.ZERO;
        List<VoucherFact> facts = new ArrayList<>(specs.size());
        for (MultiCurrencyFactSpec s : specs) {
            facts.add(s.toFact());
            BigDecimal functional = s.functionalAmount();
            if (s.credit) {
                oracleCreditFunctional = oracleCreditFunctional.add(functional);
            } else {
                oracleDebitFunctional = oracleDebitFunctional.add(functional);
            }
        }

        BigDecimal[] totals = processor.balanceTotals(facts, null);

        assertEquals(0, oracleDebitFunctional.compareTo(totals[0]),
                "totalDebit(功能币) 应等于 oracle debit 功能币累加 (oracle=" + oracleDebitFunctional
                        + ", prod=" + totals[0] + ")");
        assertEquals(0, oracleCreditFunctional.compareTo(totals[1]),
                "totalCredit(功能币) 应等于 oracle credit 功能币累加 (oracle=" + oracleCreditFunctional
                        + ", prod=" + totals[1] + ")");
    }

    /**
     * P6-属性 2：功能币平衡的凭证 → {@code assertBalanced} 放行；功能币不平衡（注入 delta）→ 抛 {@link NopException}。
     * 多币种场景：debit 侧各源币种额 × 各汇率 功能币累加 = credit 侧功能币累加时放行。
     */
    @Property(tries = 100, seed = "20260812")
    void assertBalancedAcceptsFunctionalBalancedRejectsUnbalanced(
            @ForAll("functionalBalancedSpecs") List<MultiCurrencyFactSpec> balancedSpecs,
            @ForAll("positiveDelta") BigDecimal functionalDelta) {
        ErpFinPostingProcessor processor = new ErpFinPostingProcessor();

        List<VoucherFact> facts = new ArrayList<>();
        BigDecimal totalDebitFunctional = BigDecimal.ZERO;
        BigDecimal totalCreditFunctional = BigDecimal.ZERO;
        for (MultiCurrencyFactSpec s : balancedSpecs) {
            facts.add(s.toFact());
            BigDecimal functional = s.functionalAmount();
            if (s.credit) {
                totalCreditFunctional = totalCreditFunctional.add(functional);
            } else {
                totalDebitFunctional = totalDebitFunctional.add(functional);
            }
        }
        // 构造保证：balancedSpecs 的功能币借 == 贷
        assertEquals(0, totalDebitFunctional.compareTo(totalCreditFunctional),
                "测试前置：balancedSpecs 功能币借 == 贷");

        // 平衡：放行
        processor.assertBalanced(totalDebitFunctional, totalCreditFunctional, null);

        // 非平衡（借 > 贷 差量 functionalDelta）：抛异常
        BigDecimal unbalancedDebit = totalDebitFunctional.add(functionalDelta);
        BigDecimal finalCredit = totalCreditFunctional; // effectively-final 副本供 lambda 捕获
        assertThrows(NopException.class,
                () -> processor.assertBalanced(unbalancedDebit, finalCredit, null),
                "功能币借贷不等应抛 NopException(ERR_UNBALANCED)");
    }

    /**
     * P6-属性 3（多币种增量价值）：source 币种平衡 ≠ 功能币平衡。当各行汇率不同时，source 平衡的凭证
     * 可能功能币不平衡——{@code balanceTotals}（功能币）能捕获 source 平衡掩盖的失衡。本属性显式构造
     * 「source 平衡但功能币不平衡」场景，证明功能币平衡校验（生产 {@code assertBalanced}）的必要性。
     */
    @Property(tries = 100, seed = "20260813")
    void sourceCurrencyBalanceDoesNotImplyFunctionalBalance(
            @ForAll("sourceAmount") BigDecimal debitSource,
            @ForAll("debitRate") BigDecimal debitRate,
            @ForAll("creditRate") BigDecimal creditRate) {
        ErpFinPostingProcessor processor = new ErpFinPostingProcessor();
        // source 平衡：debit source == credit source = debitSource
        MultiCurrencyFactSpec debitSpec = new MultiCurrencyFactSpec(debitSource, debitRate, false);
        MultiCurrencyFactSpec creditSpec = new MultiCurrencyFactSpec(debitSource, creditRate, true);
        // source 侧平衡（两侧 source 相等）
        assertEquals(0, debitSpec.sourceAmount.compareTo(creditSpec.sourceAmount),
                "source 侧平衡：debit source == credit source");

        BigDecimal debitFunctional = debitSpec.functionalAmount();
        BigDecimal creditFunctional = creditSpec.functionalAmount();

        BigDecimal[] totals = processor.balanceTotals(
                new ArrayList<>(List.of(debitSpec.toFact(), creditSpec.toFact())), null);
        assertEquals(0, debitFunctional.compareTo(totals[0]), "生产 balanceTotals debit = 功能币 debit");
        assertEquals(0, creditFunctional.compareTo(totals[1]), "生产 balanceTotals credit = 功能币 credit");

        if (debitRate.compareTo(creditRate) != 0) {
            // 汇率不同 → 功能币不平衡（source 平衡掩盖）
            assertFalse(0 == debitFunctional.compareTo(creditFunctional),
                    "汇率不同时 source 平衡但功能币不平衡");
            assertThrows(NopException.class,
                    () -> processor.assertBalanced(debitFunctional, creditFunctional, null),
                    "功能币不平衡应被 assertBalanced 拒绝（即使 source 平衡）");
        } else {
            // 汇率相同 → 功能币也平衡（单币种退化场景）
            assertEquals(0, debitFunctional.compareTo(creditFunctional),
                    "汇率相同时 source 平衡蕴含功能币平衡");
            processor.assertBalanced(debitFunctional, creditFunctional, null); // 放行
        }
    }

    /**
     * golden 交叉校验（保真度锚定）：复现 {@code posting.md §多币种处理} 典型场景——
     * 借：100 USD @ 7.0（功能币 700 CNY）+ 200 USD @ 7.0（功能币 1400）；贷：300 USD @ 7.0（功能币 2100）。
     * source 平衡（300==300）+ 同汇率 → 功能币平衡（2100==2100），生产 {@code assertBalanced} 放行。
     * 第二场景：借 100@7.0（700）+ 100@6.5（650）= 1350；贷 200@7.0（1400）→ source 平衡（200==200）
     * 但功能币不平衡（1350≠1400），生产 {@code assertBalanced} 拒绝（证明多币种功能币校验的必要性）。
     */
    @Test
    void goldenCrossCheckMatchesPostingMdMultiCurrencySemantics() {
        ErpFinPostingProcessor processor = new ErpFinPostingProcessor();

        // 场景 1：同汇率，source + 功能币双平衡
        List<VoucherFact> balancedFacts = new ArrayList<>();
        balancedFacts.add(spec(100, "7.0", false).toFact());
        balancedFacts.add(spec(200, "7.0", false).toFact());
        balancedFacts.add(spec(300, "7.0", true).toFact());
        BigDecimal[] t1 = processor.balanceTotals(balancedFacts, null);
        assertEquals(0, new BigDecimal("2100.0000").compareTo(t1[0]), "debit 功能币 = 700+1400 = 2100");
        assertEquals(0, new BigDecimal("2100.0000").compareTo(t1[1]), "credit 功能币 = 2100");
        // 放行
        processor.assertBalanced(t1[0], t1[1], null);

        // 场景 2：不同汇率，source 平衡但功能币不平衡
        List<VoucherFact> unbalancedFacts = new ArrayList<>();
        unbalancedFacts.add(spec(100, "7.0", false).toFact());   // 700
        unbalancedFacts.add(spec(100, "6.5", false).toFact());   // 650
        unbalancedFacts.add(spec(200, "7.0", true).toFact());    // 1400
        BigDecimal[] t2 = processor.balanceTotals(unbalancedFacts, null);
        assertEquals(0, new BigDecimal("1350.0000").compareTo(t2[0]), "debit 功能币 = 700+650 = 1350");
        assertEquals(0, new BigDecimal("1400.0000").compareTo(t2[1]), "credit 功能币 = 1400");
        assertFalse(0 == t2[0].compareTo(t2[1]), "source 平衡(200==200) 但功能币不平衡(1350≠1400)");
        // 拒绝
        assertThrows(NopException.class,
                () -> processor.assertBalanced(t2[0], t2[1], null),
                "功能币不平衡应被拒绝");
    }

    /**
     * tautology 自检（设计文档 §5.1）：注入折算变异（{@code functional = source × rate × rate}，模拟汇率乘两次）——
     * 变异功能币累加与 oracle（正确折算）必然不符，证明属性 1 的断言能发现折算算术错误，非恒等式。
     */
    @Test
    void tautologySelfCheck_translationMutationIsDetected() {
        List<MultiCurrencyFactSpec> specs = List.of(
                new MultiCurrencyFactSpec(new BigDecimal("100"), new BigDecimal("7.0"), false),
                new MultiCurrencyFactSpec(new BigDecimal("100"), new BigDecimal("6.5"), true));

        // oracle：正确折算 functional = source × rate
        BigDecimal oracleDebit = new BigDecimal("100").multiply(new BigDecimal("7.0"))
                .setScale(FUNCTIONAL_SCALE, RoundingMode.HALF_UP); // 700.0000
        BigDecimal oracleCredit = new BigDecimal("100").multiply(new BigDecimal("6.5"))
                .setScale(FUNCTIONAL_SCALE, RoundingMode.HALF_UP); // 650.0000

        // 变异折算：functional = source × rate × rate（汇率乘两次，模拟折算算术错误）
        BigDecimal mutatedDebit = new BigDecimal("100").multiply(new BigDecimal("7.0"))
                .multiply(new BigDecimal("7.0")).setScale(FUNCTIONAL_SCALE, RoundingMode.HALF_UP); // 4900.0000
        BigDecimal mutatedCredit = new BigDecimal("100").multiply(new BigDecimal("6.5"))
                .multiply(new BigDecimal("6.5")).setScale(FUNCTIONAL_SCALE, RoundingMode.HALF_UP); // 4225.0000

        // 变异结果必然与 oracle 不符——若属性 1 跑在此变异折算上会 assertEquals 失败（即「发现变异」）
        assertTrue(oracleDebit.compareTo(mutatedDebit) != 0 || oracleCredit.compareTo(mutatedCredit) != 0,
                "折算变异（rate×rate）必须改变功能币累加结果，否则属性 test 是 tautology");

        // 反向确认：正确生产 balanceTotals 与 oracle 一致（属性 1 在正确折算上绿）
        ErpFinPostingProcessor processor = new ErpFinPostingProcessor();
        List<VoucherFact> facts = new ArrayList<>();
        for (MultiCurrencyFactSpec s : specs) {
            facts.add(s.toFact());
        }
        BigDecimal[] totals = processor.balanceTotals(facts, null);
        assertEquals(0, oracleDebit.compareTo(totals[0]), "正确折算下生产 balanceTotals debit 与 oracle 一致");
        assertEquals(0, oracleCredit.compareTo(totals[1]), "正确折算下生产 balanceTotals credit 与 oracle 一致");
    }

    // ---------- @Provide 生成器 ----------

    @Provide
    Arbitrary<List<MultiCurrencyFactSpec>> multiCurrencyFactSpecs() {
        return factSpecArbitrary().list().ofMinSize(0).ofMaxSize(10);
    }

    /**
     * 生成「功能币平衡」的凭证行列表：随机生成 debit 行 + 汇率，再构造单一 credit 行使其功能币额 == Σ debit 功能币额。
     * oracle 独立验证两侧相等。
     */
    @Provide
    Arbitrary<List<MultiCurrencyFactSpec>> functionalBalancedSpecs() {
        return Arbitraries.integers().between(1, 5)
                .flatMap(n -> debitSpecs(n).map(debits -> {
                    BigDecimal totalDebitFunctional = BigDecimal.ZERO;
                    for (MultiCurrencyFactSpec d : debits) {
                        totalDebitFunctional = totalDebitFunctional.add(d.functionalAmount());
                    }
                    // credit 行：源币种 = 总功能币额（汇率 1，简化——保证功能币 credit == totalDebitFunctional）
                    MultiCurrencyFactSpec credit = new MultiCurrencyFactSpec(
                            totalDebitFunctional, BigDecimal.ONE, true);
                    List<MultiCurrencyFactSpec> all = new ArrayList<>(debits);
                    all.add(credit);
                    return all;
                }));
    }

    @Provide
    Arbitrary<BigDecimal> positiveDelta() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("0.0001"), new BigDecimal("1000000"))
                .ofScale(FUNCTIONAL_SCALE);
    }

    @Provide
    Arbitrary<BigDecimal> sourceAmount() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("0.01"), new BigDecimal("1000000"))
                .ofScale(2);
    }

    @Provide
    Arbitrary<BigDecimal> debitRate() {
        return exchangeRateArbitrary();
    }

    @Provide
    Arbitrary<BigDecimal> creditRate() {
        return exchangeRateArbitrary();
    }

    /** 汇率生成器：正数，含极端汇率边界（极小汇率 0.01 / 单币种 1 / 极大汇率 100）。 */
    private Arbitrary<BigDecimal> exchangeRateArbitrary() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("0.01"), new BigDecimal("100"))
                .ofScale(4);
    }

    private Arbitrary<MultiCurrencyFactSpec> factSpecArbitrary() {
        return Arbitraries.bigDecimals()
                .between(BigDecimal.ZERO, MAX_AMOUNT).ofScale(2)
                .flatMap(amt -> exchangeRateArbitrary()
                        .flatMap(rate -> Arbitraries.of(true, false)
                                .map(credit -> new MultiCurrencyFactSpec(amt, rate, credit))));
    }

    private Arbitrary<List<MultiCurrencyFactSpec>> debitSpecs(int n) {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("0.01"), new BigDecimal("100000")).ofScale(2)
                .flatMap(amt -> exchangeRateArbitrary()
                        .map(rate -> new MultiCurrencyFactSpec(amt, rate, false)))
                .list().ofSize(n);
    }

    private static MultiCurrencyFactSpec spec(double source, String rate, boolean credit) {
        return new MultiCurrencyFactSpec(
                BigDecimal.valueOf(source), new BigDecimal(rate), credit);
    }
}
