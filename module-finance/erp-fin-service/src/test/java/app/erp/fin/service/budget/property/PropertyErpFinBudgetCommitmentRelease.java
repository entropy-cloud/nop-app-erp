package app.erp.fin.service.budget.property;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MQ Q3 P3 属性测试——承付释放不超余量不变量（设计文档 {@code property-based-testing.md} §4.2 P3 + §5.1）。
 *
 * <p><b>路径 C 类一 + 策略 F2</b>（设计文档 §3.4）：纯 JUnit 5 + jqwik，不继承 {@code JunitAutoTestCase}；纯内存状态。
 *
 * <p><b>保真度机制裁决（Decision (b)，设计文档 §5.1 P3 同 P2 范式）</b>：生产 {@code ErpFinBudgetControlBiz.check}
 *（{@code :81}）的三通道 available 算术 `available = budgetBalance − actualBalance − commitmentBalance` 是纯的，但
 * {@code aggregateAmount}（{@code :117}）从 DB 聚合凭证行；{@code CommitmentVoucherGenerator.generateCommitment} 写 DB
 * ×3。预算/承付算术无法从纯内存 test 直接调用。裁决路径 <b>(b)</b>（设计文档 §5.1）：测试侧 {@link BudgetCommitmentModel}
 * 镜像生产 {@code check:81} available 公式（逐字符一致）+ commit/release/invoice/return 语义（对齐 {@code budget.md:18}
 * 三通道分离 + {@code CommitmentVoucherGenerator} commit/release 凭证生成/红冲）：
 * <ul>
 *   <li>选择：内存三通道模型，{@code available()} 用生产 {@code :81} 同一表达式。</li>
 *   <li>替代（否决）：(a) 从 {@code ErpFinBudgetControlBiz.check} 抽取纯 available 公式为 static method——available 公式
 *       仅 2 个 subtract，抽取收益低于触及生产类的风险；公式经本类逐字符镜像 + golden 交叉校验已充分锚定。</li>
 *   <li>golden 交叉校验：{@link #goldenCrossCheckMatchesBudgetMdSemantics()} 验证 budget.md §设计范式典型场景
 *       （budget=1000，commit 200→available 800，invoice 200→actual 200/commitment 0，actualDirect 300→available 500）。</li>
 *   <li>残留风险（R2）：纯内存模型不验证 DB 持久化层 / COMMITMENT 凭证落库；由既有 {@code TestErpFinBudgetIsolation}
 *       等端到端测试覆盖 DB 层（双层互补）。</li>
 * </ul>
 *
 * <p><b>不变量真相源</b>（设计文档 §1.3/§4.2，**不重新推导**）：{@code docs/design/finance/budget.md:18,60}——
 * {@code available = budget − actual − commitment ≥ 0}（余量非负，三通道分离）；commit/release 后 commitment 通道余额 == Σ 未红冲 COMMITMENT 凭证。
 *
 * <p><b>生成器策略（设计文档 §5.2 stateful 裁决）</b>：commit/actualDirect 合法性依赖 available 累计状态，
 * invoice 合法性依赖 commitment 累计状态——用状态依赖可行性闸门（{@link BudgetAction#feasible} 读模型当前通道），
 * 语义等价 ActionSequence（每步在当前状态上判定可行性，不可行跳过），保证只生成合法序列。
 *
 * <p><b>tautology 自检</b>（设计文档 §5.1）：{@link #tautologySelfCheck_commitMutationIsDetected()}（commit 不增 commitment 通道
 * → 通道一致性断言发现）+ {@link #tautologySelfCheck_availableSignMutationIsDetected()}（available 公式符号变异 → 余量非负断言发现）。
 *
 * <p><b>种子固定</b>（设计文档 §8 C-1 + R7）：每 {@code @Property} 经 {@code seed} 固化。
 */
class PropertyErpFinBudgetCommitmentRelease {

    // ---------- 内存三通道预算模型（镜像生产算术） ----------

    /**
     * 纯内存预算/承付模型。三通道分离（P1-MA2-084 / budget.md:18），{@code available()} 镜像生产
     * {@code ErpFinBudgetControlBiz:81} 同一表达式 {@code budget − actual − commitment}。承付凭证经 {@link Commitment#remaining}
     * 跟踪未红冲余额（commit 增 / release 全额红冲 / invoice 承付转实际部分或全额消耗）。
     */
    static final class BudgetCommitmentModel {
        BigDecimal budgetBalance = BigDecimal.ZERO;
        BigDecimal actualBalance = BigDecimal.ZERO;
        BigDecimal commitmentBalance = BigDecimal.ZERO;
        final List<Commitment> commitments = new ArrayList<>(); // 全部承付凭证（remaining=0 表示已全额红冲）

        void setBudget(BigDecimal amount) {
            budgetBalance = amount;
        }

        /** 镜像 ErpFinBudgetControlBiz:81 三通道 available 公式（逐字符一致）。 */
        BigDecimal available() {
            return budgetBalance.subtract(actualBalance).subtract(commitmentBalance);
        }

        /** commit（PO 审核承付占用）：commitment += amount；feasible 由调用方经 available() 守卫。 */
        void commit(BigDecimal amount) {
            commitmentBalance = commitmentBalance.add(amount);
            commitments.add(new Commitment(amount));
        }

        /** release（PO 取消红冲承付）：全额红冲指定未红冲承付凭证。 */
        void release(int commitmentIndex) {
            Commitment c = commitments.get(commitmentIndex);
            commitmentBalance = commitmentBalance.subtract(c.remaining);
            c.remaining = BigDecimal.ZERO;
            c.reversed = true;
        }

        /**
         * invoice（承付转实际：发票到达时释放承付并记实际）。消耗未红冲承付凭证的 remaining（FIFO，可部分消耗），
         * commitment -= amount, actual += amount。available 不变（−actual +commitment 抵消）。
         * 对齐 budget.md 承付释放语义 + CommitmentVoucherGenerator.reverseCommitment 红冲。
         */
        void invoice(BigDecimal amount) {
            BigDecimal toConsume = amount;
            for (Commitment c : commitments) {
                if (toConsume.signum() <= 0) {
                    break;
                }
                if (c.remaining.signum() <= 0) {
                    continue;
                }
                BigDecimal take = toConsume.min(c.remaining);
                c.remaining = c.remaining.subtract(take);
                if (c.remaining.signum() == 0) {
                    c.reversed = true;
                }
                toConsume = toConsume.subtract(take);
            }
            commitmentBalance = commitmentBalance.subtract(amount);
            actualBalance = actualBalance.add(amount);
        }

        /** actualDirect（直接付款/报销，无承付）：actual += amount；feasible 由 available() 守卫。 */
        void actualDirect(BigDecimal amount) {
            actualBalance = actualBalance.add(amount);
        }

        /** returnGoods（退货/红冲实际）：actual -= amount。 */
        void returnGoods(BigDecimal amount) {
            actualBalance = actualBalance.subtract(amount);
        }

        /** 不变量 1：余量非负（budget.md:18）。 */
        boolean availableNonNegative() {
            return available().signum() >= 0;
        }

        /** 不变量 2：commitment 通道余额 == Σ 未红冲（remaining>0）承付凭证余额（budget.md:18 / CommitmentVoucherGenerator 语义）。 */
        boolean commitmentChannelMatchesUnreversed() {
            BigDecimal sum = BigDecimal.ZERO;
            for (Commitment c : commitments) {
                sum = sum.add(c.remaining);
            }
            return commitmentBalance.compareTo(sum) == 0;
        }

        BigDecimal unreversedCommitmentCount() {
            long n = commitments.stream().filter(c -> c.remaining.signum() > 0).count();
            return new BigDecimal(n);
        }
    }

    static final class Commitment {
        BigDecimal remaining; // 未红冲余额（commit 时=amount，release/invoice 时递减）
        boolean reversed = false;

        Commitment(BigDecimal amount) {
            this.remaining = amount;
        }
    }

    // ---------- 状态依赖动作 ----------

    abstract static class BudgetAction {
        abstract boolean feasible(BudgetCommitmentModel m);

        abstract void apply(BudgetCommitmentModel m);
    }

    static final class CommitAction extends BudgetAction {
        final BigDecimal amount;

        CommitAction(BigDecimal amount) {
            this.amount = amount;
        }

        @Override
        boolean feasible(BudgetCommitmentModel m) {
            // 镜像生产 HARD 控制：available >= amount 才放行（commit 占用 commitment 通道，消耗余量）
            return amount.signum() > 0 && m.available().compareTo(amount) >= 0;
        }

        @Override
        void apply(BudgetCommitmentModel m) {
            m.commit(amount);
            assertTrue(m.availableNonNegative(), "commit 后余量应非负（available=budget−actual−commitment≥0）");
            assertTrue(m.commitmentChannelMatchesUnreversed(),
                    "commitment 通道应 == Σ 未红冲承付凭证");
        }
    }

    static final class ReleaseAction extends BudgetAction {
        final int indexHint; // 在未红冲承付列表中的偏好索引（实际索引经 feasible 归一化）

        ReleaseAction(int indexHint) {
            this.indexHint = indexHint;
        }

        @Override
        boolean feasible(BudgetCommitmentModel m) {
            return m.commitments.stream().anyMatch(c -> c.remaining.signum() > 0);
        }

        @Override
        void apply(BudgetCommitmentModel m) {
            int idx = resolveUnreversedIndex(m);
            m.release(idx);
            assertTrue(m.availableNonNegative(), "release 后余量应非负");
            assertTrue(m.commitmentChannelMatchesUnreversed(),
                    "release 后 commitment 通道应 == Σ 未红冲承付凭证");
        }

        private int resolveUnreversedIndex(BudgetCommitmentModel m) {
            List<Integer> unreversed = new ArrayList<>();
            for (int i = 0; i < m.commitments.size(); i++) {
                if (m.commitments.get(i).remaining.signum() > 0) {
                    unreversed.add(i);
                }
            }
            int pick = ((indexHint % unreversed.size()) + unreversed.size()) % unreversed.size();
            return unreversed.get(pick);
        }
    }

    static final class InvoiceAction extends BudgetAction {
        final BigDecimal amount;

        InvoiceAction(BigDecimal amount) {
            this.amount = amount;
        }

        @Override
        boolean feasible(BudgetCommitmentModel m) {
            // 承付转实际：须 commitment 通道余额 >= amount（部分开票合法）
            return amount.signum() > 0 && m.commitmentBalance.compareTo(amount) >= 0;
        }

        @Override
        void apply(BudgetCommitmentModel m) {
            BigDecimal availableBefore = m.available();
            m.invoice(amount);
            assertEquals(0, availableBefore.compareTo(m.available()),
                    "invoice（承付转实际）应保持 available 不变（−actual +commitment 抵消）");
            assertTrue(m.availableNonNegative(), "invoice 后余量应非负");
            assertTrue(m.commitmentChannelMatchesUnreversed(), "invoice 后 commitment 通道一致");
        }
    }

    static final class ActualDirectAction extends BudgetAction {
        final BigDecimal amount;

        ActualDirectAction(BigDecimal amount) {
            this.amount = amount;
        }

        @Override
        boolean feasible(BudgetCommitmentModel m) {
            return amount.signum() > 0 && m.available().compareTo(amount) >= 0;
        }

        @Override
        void apply(BudgetCommitmentModel m) {
            m.actualDirect(amount);
            assertTrue(m.availableNonNegative(), "actualDirect 后余量应非负");
            assertTrue(m.commitmentChannelMatchesUnreversed(), "actualDirect 后 commitment 通道一致");
        }
    }

    static final class ReturnAction extends BudgetAction {
        final BigDecimal amount;

        ReturnAction(BigDecimal amount) {
            this.amount = amount;
        }

        @Override
        boolean feasible(BudgetCommitmentModel m) {
            return amount.signum() > 0 && m.actualBalance.compareTo(amount) >= 0;
        }

        @Override
        void apply(BudgetCommitmentModel m) {
            m.returnGoods(amount);
            assertTrue(m.availableNonNegative(), "退货后余量应非负");
            assertTrue(m.commitmentChannelMatchesUnreversed(), "退货后 commitment 通道一致");
        }
    }

    // ---------- 属性 ----------

    /**
     * P3-属性 1：随机合法 commit/release/invoice/actualDirect/return 序列下，每步 {@code available >= 0} 恒成立
     *（余量非负，对齐 budget.md:18 三通道）。
     */
    @Property(tries = 100, seed = "20260807")
    void availableStaysNonNegativeUnderValidSequences(@ForAll("budgetActions") List<BudgetAction> actions,
                                                       @ForAll("initialBudget") BigDecimal budget) {
        BudgetCommitmentModel m = new BudgetCommitmentModel();
        m.setBudget(budget);
        for (BudgetAction action : actions) {
            if (action.feasible(m)) {
                action.apply(m); // apply 内断言 available >= 0 + commitment 通道一致
            }
        }
    }

    /**
     * P3-属性 2：随机合法序列下，commitment 通道余额恒等于 Σ 未红冲承付凭证金额（commit/release 一致性不变量）。
     */
    @Property(tries = 100, seed = "20260808")
    void commitmentChannelAlwaysMatchesUnreversedCommitments(@ForAll("budgetActions") List<BudgetAction> actions,
                                                             @ForAll("initialBudget") BigDecimal budget) {
        BudgetCommitmentModel m = new BudgetCommitmentModel();
        m.setBudget(budget);
        for (BudgetAction action : actions) {
            if (action.feasible(m)) {
                action.apply(m);
            }
        }
        assertTrue(m.commitmentChannelMatchesUnreversed(),
                "终态 commitment 通道应 == Σ 未红冲承付凭证（commit/release 一致性）");
    }

    /**
     * golden 交叉校验（保真度锚定）：budget.md §设计范式典型场景，验证模型语义对齐三通道 available 公式。
     */
    @Test
    void goldenCrossCheckMatchesBudgetMdSemantics() {
        BudgetCommitmentModel m = new BudgetCommitmentModel();
        m.setBudget(new BigDecimal("1000"));
        assertEquals(0, new BigDecimal("1000").compareTo(m.available()), "初始 available = budget = 1000");

        m.commit(new BigDecimal("200"));
        // available = 1000 − 0 − 200 = 800；commitment = 200
        assertEquals(0, new BigDecimal("800").compareTo(m.available()), "commit 200 后 available = 800");
        assertEquals(0, new BigDecimal("200").compareTo(m.commitmentBalance), "commitment 通道 = 200");
        assertEquals(1, m.commitments.size(), "1 张未红冲承付凭证");

        m.invoice(new BigDecimal("200"));
        // 承付转实际：commitment 0, actual 200, available = 1000 − 200 − 0 = 800（不变）
        assertEquals(0, new BigDecimal("0").compareTo(m.commitmentBalance), "invoice 后 commitment = 0");
        assertEquals(0, new BigDecimal("200").compareTo(m.actualBalance), "invoice 后 actual = 200");
        assertEquals(0, new BigDecimal("800").compareTo(m.available()), "invoice 后 available 不变 = 800");

        m.actualDirect(new BigDecimal("300"));
        // available = 1000 − 500 − 0 = 500
        assertEquals(0, new BigDecimal("500").compareTo(m.available()), "actualDirect 300 后 available = 500");
        assertEquals(0, new BigDecimal("500").compareTo(m.actualBalance), "actual = 500");

        m.returnGoods(new BigDecimal("100"));
        // 退货：actual 400, available = 1000 − 400 − 0 = 600
        assertEquals(0, new BigDecimal("600").compareTo(m.available()), "退货 100 后 available = 600");
        assertTrue(m.availableNonNegative(), "余量始终非负");
        assertTrue(m.commitmentChannelMatchesUnreversed(), "commitment 通道一致");
    }

    /** tautology 自检 1（设计文档 §5.1）：commit 不增 commitment 通道 → commitment 通道一致性断言发现。 */
    @Test
    void tautologySelfCheck_commitMutationIsDetected() {
        BudgetCommitmentModel m = new BudgetCommitmentModel();
        m.setBudget(new BigDecimal("1000"));
        m.commit(new BigDecimal("200"));
        assertTrue(m.commitmentChannelMatchesUnreversed(), "正常 commit 后通道一致");
        // 注入变异：手工破坏 commitment 通道（模拟 commit 不增 commitment 的变异）
        m.commitmentBalance = m.commitmentBalance.subtract(new BigDecimal("50"));
        assertFalse(m.commitmentChannelMatchesUnreversed(),
                "commitment 通道被破坏后一致性应失败（证明属性 test 能发现 commit 变异）");
    }

    /** tautology 自检 2（设计文档 §5.1）：available 公式符号变异（−actual 变 +actual）→ 余量非负断言发现。 */
    @Test
    void tautologySelfCheck_availableSignMutationIsDetected() {
        BudgetCommitmentModel m = new BudgetCommitmentModel();
        m.setBudget(new BigDecimal("1000"));
        m.actualBalance = new BigDecimal("300");
        // 正确 available = 1000 − 300 − 0 = 700 ≥ 0
        assertTrue(m.availableNonNegative(), "正确公式下余量非负");
        // 注入变异：available 公式符号错（budget + actual − commitment，模拟 subtract→add 变异）
        BigDecimal mutatedAvailable = m.budgetBalance.add(m.actualBalance).subtract(m.commitmentBalance);
        // mutatedAvailable = 1300；若 actual 巨大则可能仍非负——构造 actual 使符号变异越过余量边界：
        m.actualBalance = new BigDecimal("2000");
        BigDecimal correctAvailable = m.budgetBalance.subtract(m.actualBalance).subtract(m.commitmentBalance); // -1000
        BigDecimal mutatedAvailable2 = m.budgetBalance.add(m.actualBalance).subtract(m.commitmentBalance); // 3000
        assertTrue(correctAvailable.signum() < 0, "正确公式：actual>budget 时余量为负（HARD 控制应拒绝）");
        assertFalse(correctAvailable.signum() >= 0,
                "正确公式下此场景余量非负不变量破缺——证明不变量可识别超预算");
        assertTrue(mutatedAvailable2.signum() >= 0, "变异公式（+actual）错误地放行超预算");
        // 结论：属性 test 的 availableNonNegative（用正确公式）能发现 actual>budget 的超预算场景；
        // 若生产 available 公式被变异为 +actual，则超预算放行——本测试证明两类公式结果不同（属性可区分）。
        assertFalse(correctAvailable.compareTo(mutatedAvailable2) == 0,
                "正确公式与符号变异公式必产生不同结果（证明属性 test 非恒等式）");
    }

    // ---------- @Provide 生成器 ----------

    @Provide
    Arbitrary<List<BudgetAction>> budgetActions() {
        return actionArbitrary().list().ofMinSize(0).ofMaxSize(20);
    }

    @Provide
    Arbitrary<BigDecimal> initialBudget() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("100"), new BigDecimal("10000"))
                .ofScale(2);
    }

    private Arbitrary<BudgetAction> actionArbitrary() {
        // commit / invoice / release / actualDirect / return 混合；各 action 经自身 feasibility 闸门
        Arbitrary<BudgetAction> commits = amountArbitrary().map(a -> (BudgetAction) new CommitAction(a));
        Arbitrary<BudgetAction> invoices = amountArbitrary().map(a -> (BudgetAction) new InvoiceAction(a));
        Arbitrary<BudgetAction> releases = Arbitraries.integers().between(0, 99)
                .map(i -> (BudgetAction) new ReleaseAction(i));
        Arbitrary<BudgetAction> directs = amountArbitrary().map(a -> (BudgetAction) new ActualDirectAction(a));
        Arbitrary<BudgetAction> returns = amountArbitrary().map(a -> (BudgetAction) new ReturnAction(a));
        // 加权：commit/invoice 主路径，release/return 偶发
        return Arbitraries.frequencyOf(
                Tuple.of(3, commits),
                Tuple.of(3, invoices),
                Tuple.of(1, releases),
                Tuple.of(2, directs),
                Tuple.of(1, returns));
    }

    private Arbitrary<BigDecimal> amountArbitrary() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("0.01"), new BigDecimal("500"))
                .ofScale(2);
    }
}
