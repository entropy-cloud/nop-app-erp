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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MQ Q3 P1 属性测试——借贷平衡不变量（设计文档 {@code property-based-testing.md} §4.2 P1 + §5.1 保真度硬约束）。
 *
 * <p><b>路径 C 类一 + 策略 F2</b>（设计文档 §3.4）：纯 JUnit 5 + jqwik，**不继承** {@code JunitAutoTestCase}
 *（绕过快照录制/校验语义冲突，设计文档 §1.4/§3.2）；纯内存状态（不触 DB / localDb / IoC 容器）。
 *
 * <p><b>包位置裁决（实施期发现，回填设计文档 Review Record）</b>：本类位于包 {@code app.erp.fin.service.posting}
 *（与生产 {@link ErpFinPostingProcessor} 同包），而非设计文档 §6.2 建议的 {@code .../property/} 子包。原因：{@code balanceTotals}
 *（{@code :709}）/ {@code assertBalanced}（{@code :723}）是 {@code protected}——{@code protected} 含同包访问权，同包类可在
 * {@link ErpFinPostingProcessor} 实例上直接调用，无须子类暴露。初版放在 {@code .../property/} 子包用测试侧子类
 * {@code AccessiblePostingProcessor extends ErpFinPostingProcessor} 暴露 protected 方法，但 {@link ErpFinPostingProcessor}
 * 带 {@code @SingleSession} AOP 注解，其测试子类触发 Nop {@code gen-aop-proxy-for-test} 增强失败
 *（{@code NoClassDefFoundError}，测试内部类在增强器 classloader 不可达）。改同包直接访问——零子类 → 零 AOP 代理生成。
 *
 * <p><b>保真度硬约束</b>（设计文档 §5.1）：每个 {@code @Property} 直接调用<b>生产</b>算术
 * {@link ErpFinPostingProcessor#balanceTotals}（纯函数累加）+ {@link ErpFinPostingProcessor#assertBalanced}——
 * {@code new ErpFinPostingProcessor()} 不经 DI（两方法是纯算术，不触 @Inject 字段）。**非测试侧并行 reimplementation**：
 * 路由 oracle 在生成期以独立 boolean 标志捕获（与生产读 {@code fact.getDcDirection()} 的代码路径解耦），变异翻转生产路由会被发现。
 *
 * <p><b>不变量真相源</b>（设计文档 §1.3/§4.2，**不重新推导**）：{@code docs/design/finance/posting.md}——
 * 每张凭证 {@code Σ debitAmount == Σ creditAmount}；{@code balanceTotals} 按 {@code dcDirection} 路由累加。
 *
 * <p><b>tautology 自检</b>（设计文档 §5.1）：{@link #tautologySelfCheck_routingMutationIsDetected()} 注入路由翻转变异，
 * 确认本属性 test 能发现——证明属性 test 非恒等式「自证平衡」。
 *
 * <p><b>种子固定</b>（设计文档 §8 C-1 + R7）：每 {@code @Property} 经 {@code seed} 固化，CI 确定性可复现，消除 flaky。
 */
class PropertyErpFinDebitCreditBalance {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000");

    /**
     * 生成期捕获路由决策的事实行——oracle 用 {@code credit} 标志累加（独立于生产读 {@code dcDirection}）。
     */
    static final class FactSpec {
        final BigDecimal amount;
        final boolean credit;

        FactSpec(BigDecimal amount, boolean credit) {
            this.amount = amount;
            this.credit = credit;
        }

        VoucherFact toFact() {
            VoucherFact f = new VoucherFact();
            f.setAmount(amount);
            f.setDcDirection(credit ? ErpFinConstants.DC_CREDIT : ErpFinConstants.DC_DEBIT);
            return f;
        }
    }

    /**
     * P1-属性 1：生产 {@code balanceTotals} 按 {@code dcDirection} 正确路由累加。
     * oracle 在生成期以 boolean 标志独立累加；若生产路由被变异翻转（CREDIT→借方），totalDebit/totalCredit 对调 → 不符 → 发现。
     */
    @Property(tries = 100, seed = "20260802")
    void balanceTotalsRoutesAmountsByDcDirection(@ForAll("factSpecs") List<FactSpec> specs) {
        ErpFinPostingProcessor processor = new ErpFinPostingProcessor();

        BigDecimal oracleDebit = BigDecimal.ZERO;
        BigDecimal oracleCredit = BigDecimal.ZERO;
        List<VoucherFact> facts = new ArrayList<>(specs.size());
        for (FactSpec s : specs) {
            facts.add(s.toFact());
            BigDecimal amt = s.amount != null ? s.amount : BigDecimal.ZERO;
            if (s.credit) {
                oracleCredit = oracleCredit.add(amt);
            } else {
                oracleDebit = oracleDebit.add(amt);
            }
        }

        BigDecimal[] totals = processor.balanceTotals(facts, null);

        assertEquals(0, oracleDebit.compareTo(totals[0]),
                "totalDebit 应等于生成期 debit 标志累加 (oracle=" + oracleDebit + ", prod=" + totals[0] + ")");
        assertEquals(0, oracleCredit.compareTo(totals[1]),
                "totalCredit 应等于生成期 credit 标志累加 (oracle=" + oracleCredit + ", prod=" + totals[1] + ")");
    }

    /**
     * P1-属性 2：生产 {@code assertBalanced} 对借贷相等的总额放行、对不等总额抛 {@link NopException}。
     */
    @Property(tries = 100, seed = "20260803")
    void assertBalancedAcceptsBalancedRejectsUnbalanced(
            @ForAll("balancedTotals") Tuple.Tuple2<BigDecimal, BigDecimal> balanced,
            @ForAll("positiveDelta") BigDecimal delta) {
        ErpFinPostingProcessor processor = new ErpFinPostingProcessor();

        // 平衡：放行
        processor.assertBalanced(balanced.get1(), balanced.get2(), null);

        // 非平衡（借 > 贷 差量 delta）：抛异常
        BigDecimal unbalancedDebit = balanced.get1().add(delta);
        assertThrows(NopException.class,
                () -> processor.assertBalanced(unbalancedDebit, balanced.get2(), null),
                "借贷不等应抛 NopException(ERR_UNBALANCED)");
    }

    /**
     * P1-属性 3：生产 {@code balanceTotals} 把 null 金额按 ZERO 处理（不 NPE、不贡献累加）。对齐生产 {@code :713}。
     */
    @Property(tries = 100, seed = "20260804")
    void balanceTotalsTreatsNullAmountAsZero(@ForAll("factSpecsWithNulls") List<FactSpec> specs) {
        ErpFinPostingProcessor processor = new ErpFinPostingProcessor();

        BigDecimal oracleDebit = BigDecimal.ZERO;
        BigDecimal oracleCredit = BigDecimal.ZERO;
        List<VoucherFact> facts = new ArrayList<>(specs.size());
        for (FactSpec s : specs) {
            facts.add(s.toFact());
            BigDecimal amt = s.amount != null ? s.amount : BigDecimal.ZERO;
            if (s.credit) {
                oracleCredit = oracleCredit.add(amt);
            } else {
                oracleDebit = oracleDebit.add(amt);
            }
        }

        BigDecimal[] totals = processor.balanceTotals(facts, null);
        assertEquals(0, oracleDebit.compareTo(totals[0]), "null 金额按 ZERO 处理 (debit)");
        assertEquals(0, oracleCredit.compareTo(totals[1]), "null 金额按 ZERO 处理 (credit)");
    }

    /**
     * tautology 自检（设计文档 §5.1）：注入「路由翻转」变异——若生产 balanceTotals 把 CREDIT 金额累加到 debit、
     * DEBIT 累加到 credit，则 oracle（正确路由）将与「变异生产」结果不符。本测试确认属性 1 的断言能捕获该变异，
     * 证明属性 test 非恒等式「自证平衡」。
     */
    @Test
    void tautologySelfCheck_routingMutationIsDetected() {
        List<FactSpec> specs = List.of(
                new FactSpec(new BigDecimal("123.4500"), false),
                new FactSpec(new BigDecimal("777.0000"), true));

        BigDecimal oracleDebit = new BigDecimal("123.4500");
        BigDecimal oracleCredit = new BigDecimal("777.0000");

        // 变异生产算术：路由翻转（CREDIT→debit / DEBIT→credit），模拟 i++→i-- 同型的累加方向变异
        BigDecimal mutatedDebit = BigDecimal.ZERO;
        BigDecimal mutatedCredit = BigDecimal.ZERO;
        for (FactSpec s : specs) {
            BigDecimal amt = s.amount;
            if (s.credit) {
                mutatedDebit = mutatedDebit.add(amt); // 变异：CREDIT 错累加到 debit
            } else {
                mutatedCredit = mutatedCredit.add(amt);
            }
        }

        // 变异结果必然与 oracle 不符——若属性 1 跑在此变异生产上会 assertEquals 失败（即「发现变异」）
        assertTrue(oracleDebit.compareTo(mutatedDebit) != 0 || oracleCredit.compareTo(mutatedCredit) != 0,
                "路由翻转变异必须改变累加结果，否则属性 test 是 tautology");
        // 反向确认：正确生产算术与 oracle 一致（属性 1 在正确生产上绿）
        ErpFinPostingProcessor processor = new ErpFinPostingProcessor();
        List<VoucherFact> facts = new ArrayList<>();
        for (FactSpec s : specs) {
            facts.add(s.toFact());
        }
        BigDecimal[] totals = processor.balanceTotals(facts, null);
        assertEquals(0, oracleDebit.compareTo(totals[0]), "正确生产算术应与 oracle 一致");
        assertEquals(0, oracleCredit.compareTo(totals[1]), "正确生产算术应与 oracle 一致");
    }

    // ---------- @Provide 生成器 ----------

    @Provide
    Arbitrary<List<FactSpec>> factSpecs() {
        return factSpecArbitrary(false);
    }

    @Provide
    Arbitrary<List<FactSpec>> factSpecsWithNulls() {
        return factSpecArbitrary(true);
    }

    private Arbitrary<List<FactSpec>> factSpecArbitrary(boolean allowNullAmount) {
        Arbitrary<BigDecimal> amounts = Arbitraries.bigDecimals()
                .between(BigDecimal.ZERO, MAX_AMOUNT)
                .ofScale(4);
        Arbitrary<BigDecimal> maybeNullAmounts = allowNullAmount
                ? Arbitraries.frequency(Tuple.of(3, Boolean.TRUE), Tuple.of(1, Boolean.FALSE))
                        .flatMap(useNull -> useNull ? Arbitraries.just((BigDecimal) null) : amounts)
                : amounts;
        // 生成期捕获路由决策（boolean 标志），与生产读 dcDirection 解耦 → 非 tautology
        Arbitrary<FactSpec> spec = maybeNullAmounts.flatMap(amt ->
                Arbitraries.of(true, false).map(credit -> new FactSpec(amt, credit)));
        // 空列表是合法边界（balanceTotals 返回 [ZERO, ZERO]），显式纳入；长度上界 10 控制迭代开销
        return spec.list().ofMinSize(0).ofMaxSize(10);
    }

    @Provide
    Arbitrary<Tuple.Tuple2<BigDecimal, BigDecimal>> balancedTotals() {
        // 平衡 = 借贷两侧相等：随机金额 X，两侧均为 X
        return Arbitraries.bigDecimals()
                .between(BigDecimal.ZERO, MAX_AMOUNT)
                .ofScale(4)
                .map(x -> Tuple.of(x, x));
    }

    @Provide
    Arbitrary<BigDecimal> positiveDelta() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("0.0001"), new BigDecimal("1000000"))
                .ofScale(4);
    }
}
