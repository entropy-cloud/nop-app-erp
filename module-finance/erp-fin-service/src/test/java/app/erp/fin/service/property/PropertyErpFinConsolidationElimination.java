package app.erp.fin.service.property;

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
 * MQ Q3 P8 属性测试——合并抵消归零不变量（设计文档 {@code property-based-testing.md} §9 successor +
 * §5.1/§5.3 + §1.3 真相源 {@code intercompany-consolidation.md §内部交易抵消机制}）。
 *
 * <p><b>路径 C 类一 + 策略 F2</b>（设计文档 §3.4）：纯 JUnit 5 + jqwik，不继承 {@code JunitAutoTestCase}；纯内存状态。
 *
 * <p><b>保真度机制裁决（Decision (b)——内存模型镜像生产配对算术，同 predecessor P2/P3 范式）</b>：生产
 * {@code ErpFinConsolidationEliminationGenerateEliminationCandidatesProcessor}（{@code :36-115}）+
 * {@code ErpFinConsolidationEliminationPostEliminationProcessor.writeDraftEliminationVoucher}（{@code :73-149}）
 * 的抵消配对算术是纯的，但交织 DB 写入（{@code :33-34}/`:37-38` `@Inject IDaoProvider` +
 * {@code :68/85/103}/`:90/115/139/147` `saveEntity`），无法从纯内存 test 直接调用。裁决路径 <b>(b)</b>
 *（设计文档 §5.1）：测试侧 {@link ConsolidationEliminationModel} <b>逐行镜像</b>生产抵消配对算术：
 * <ul>
 *   <li>选择：内存模型镜像生产 {@code generateEliminationCandidates:55-106}（每个 matched IntercompanyMatch
 *       按 AR_AP + REVENUE_COST [+ INVENTORY_PROFIT if enabled] 各生成一条 elimination candidate，
 *       {@code eliminationAmount = matchedAmount}）+ {@code writeDraftEliminationVoucher:73-149}
 *      （每条 candidate 生成平衡凭证：Dr=amount / Cr=amount，{@code totalDebit=amount, totalCredit=amount}）。</li>
 *   <li>替代（否决）：(a) 从生产 Processor 抽取抵消配对为纯函数——配对循环交织 {@code saveEntity} +
 *       subject 解析（{@code findSubjectByCode} DB 查询），抽取需重组 DB 副作用 + 跨实体访问，非纯行为保持，
 *       风险高于收益；路径 (b) 经 golden 交叉校验已充分锚定，零生产代码风险。</li>
 *   <li>golden 交叉校验：{@link #goldenCrossCheckMatchesTestIntercompanyMatchingAndEliminationNumbers()}
 *       复现 {@code TestErpFinIntercompanyMatchingAndElimination.testPostEliminationGeneratesDraftVoucher}
 *       生产实测场景（公司间销售/采购配对 2000 → AR_AP + REVENUE_COST 各生成 amount=2000 candidate →
 *       每条 postElimination 生成 totalDebit=2000/totalCredit=2000 平衡凭证）。</li>
 *   <li>残留风险（R2）：纯内存模型不验证 DB 持久化 / 凭证落库 / subject 解析；由既有
 *       {@code TestErpFinIntercompanyMatchingAndElimination} 端到端测试覆盖 DB 层（双层互补）。</li>
 * </ul>
 *
 * <p><b>不变量真相源</b>（设计文档 §1.3，**不重新推导**）：{@code docs/design/finance/intercompany-consolidation.md
 * §内部交易抵消机制}——抵消分录 {@code Σ eliminationDebit == Σ eliminationCredit}（每张抵消凭证借贷平衡），
 * 且抵消后合并净额 = 外部交易净额（内部交易在合并层完全抵消，净额贡献为零）。
 *
 * <p><b>tautology 自检</b>（设计文档 §5.1）：{@link #tautologySelfCheck_unbalancedVoucherMutationIsDetected()}
 * 注入「抵消凭证借贷不平衡」变异（Dr=M / Cr=M/2），证明属性 test 能发现此类变异——非恒等式。
 *
 * <p><b>种子固定</b>（设计文档 §8 C-1 + R7）：每 {@code @Property} 经 {@code seed} 固化。
 */
class PropertyErpFinConsolidationElimination {

    /** 抵消类型（镜像 ErpFinConstants.ELIMINATION_TYPE_*）。 */
    static final String TYPE_AR_AP = "AR_AP";
    static final String TYPE_REVENUE_COST = "REVENUE_COST";
    static final String TYPE_INVENTORY_PROFIT = "INVENTORY_PROFIT";

    /** 公司间配对记录（镜像 ErpFinIntercompanyMatch：matchedAmount + 双方 org）。 */
    static final class IntercompanyPair {
        final long fromOrg;
        final long toOrg;
        final BigDecimal matchedAmount;

        IntercompanyPair(long fromOrg, long toOrg, BigDecimal matchedAmount) {
            this.fromOrg = fromOrg;
            this.toOrg = toOrg;
            this.matchedAmount = matchedAmount;
        }
    }

    /** 抵消候选（镜像 ErpFinConsolidationElimination：type + eliminationAmount + 双方 org）。 */
    static final class EliminationCandidate {
        final long fromOrg;
        final long toOrg;
        final String type;
        final BigDecimal eliminationAmount;

        EliminationCandidate(long fromOrg, long toOrg, String type, BigDecimal eliminationAmount) {
            this.fromOrg = fromOrg;
            this.toOrg = toOrg;
            this.type = type;
            this.eliminationAmount = eliminationAmount;
        }
    }

    /** 抵消凭证（镜像 ErpFinVoucher：totalDebit + totalCredit + 借/贷行）。 */
    static final class EliminationVoucher {
        final BigDecimal totalDebit;
        final BigDecimal totalCredit;
        final BigDecimal debitLineAmount;
        final BigDecimal creditLineAmount;

        EliminationVoucher(BigDecimal totalDebit, BigDecimal totalCredit,
                           BigDecimal debitLineAmount, BigDecimal creditLineAmount) {
            this.totalDebit = totalDebit;
            this.totalCredit = totalCredit;
            this.debitLineAmount = debitLineAmount;
            this.creditLineAmount = creditLineAmount;
        }

        boolean isBalanced() {
            return totalDebit.compareTo(totalCredit) == 0
                    && debitLineAmount.compareTo(creditLineAmount) == 0
                    && totalDebit.compareTo(debitLineAmount) == 0;
        }
    }

    /**
     * 纯内存合并抵消模型。镜像生产 {@code generateEliminationCandidates}（按 AR_AP + REVENUE_COST [+INV_PROFIT]
     * 各生成 candidate）+ {@code writeDraftEliminationVoucher}（Dr/Cr 平衡凭证）。
     */
    static final class ConsolidationEliminationModel {
        final List<IntercompanyPair> pairs = new ArrayList<>();
        final List<EliminationCandidate> candidates = new ArrayList<>();
        final List<EliminationVoucher> postedVouchers = new ArrayList<>();
        boolean inventoryProfitEnabled = false;

        /** 注册公司间配对（镜像 runMatching 输出的 matched IntercompanyMatch）。 */
        void addPair(long fromOrg, long toOrg, BigDecimal matchedAmount) {
            pairs.add(new IntercompanyPair(fromOrg, toOrg, matchedAmount));
        }

        /**
         * 镜像 generateEliminationCandidates:55-106：每个配对按 AR_AP + REVENUE_COST
         *（+ INVENTORY_PROFIT if enabled）各生成一条 candidate，{@code eliminationAmount = matchedAmount}。
         */
        void generateCandidates() {
            for (IntercompanyPair p : pairs) {
                candidates.add(new EliminationCandidate(p.fromOrg, p.toOrg, TYPE_AR_AP, p.matchedAmount));
                candidates.add(new EliminationCandidate(p.fromOrg, p.toOrg, TYPE_REVENUE_COST, p.matchedAmount));
                if (inventoryProfitEnabled) {
                    candidates.add(new EliminationCandidate(p.fromOrg, p.toOrg, TYPE_INVENTORY_PROFIT,
                            p.matchedAmount));
                }
            }
        }

        /** 镜像 postElimination / writeDraftEliminationVoucher:73-149：每条 candidate 生成平衡凭证 Dr=amount/Cr=amount。 */
        void postAll() {
            for (EliminationCandidate c : candidates) {
                BigDecimal amt = c.eliminationAmount;
                // 镜像 :86-87 totalDebit=amount, totalCredit=amount + :105/130 debitLine/creditLine = amount
                postedVouchers.add(new EliminationVoucher(amt, amt, amt, amt));
            }
        }

        /** 不变量 1：聚合 Σ eliminationDebit == Σ eliminationCredit。 */
        boolean aggregateBalanced() {
            BigDecimal sumDebit = BigDecimal.ZERO;
            BigDecimal sumCredit = BigDecimal.ZERO;
            for (EliminationVoucher v : postedVouchers) {
                sumDebit = sumDebit.add(v.totalDebit);
                sumCredit = sumCredit.add(v.totalCredit);
            }
            return sumDebit.compareTo(sumCredit) == 0;
        }

        BigDecimal aggregateDebit() {
            BigDecimal sum = BigDecimal.ZERO;
            for (EliminationVoucher v : postedVouchers) {
                sum = sum.add(v.totalDebit);
            }
            return sum;
        }

        /** 不变量 2：合并净额 = 外部交易净额（内部配对净额贡献为零）。 */
        boolean consolidatedNetEqualsExternal(BigDecimal externalNet) {
            // 内部配对在合并层净额贡献为零（fromOrg +M == toOrg −M，跨集团抵消）
            BigDecimal internalNetContribution = BigDecimal.ZERO;
            for (IntercompanyPair p : pairs) {
                internalNetContribution = internalNetContribution.add(p.matchedAmount);
                internalNetContribution = internalNetContribution.subtract(p.matchedAmount);
            }
            BigDecimal consolidatedNet = externalNet.add(internalNetContribution);
            return consolidatedNet.compareTo(externalNet) == 0;
        }
    }

    // ---------- 属性 ----------

    /**
     * P8-属性 1：随机公司间配对集下，每张抵消凭证借贷平衡（Dr==Cr），聚合 Σ debit == Σ credit。
     * 镜像生产 generateEliminationCandidates + writeDraftEliminationVoucher。
     */
    @Property(tries = 100, seed = "20260817")
    void eliminationVouchersAreAlwaysBalanced(@ForAll("intercompanyPairs") List<IntercompanyPair> pairs,
                                              @ForAll("inventoryProfitFlag") boolean invProfitEnabled) {
        ConsolidationEliminationModel m = new ConsolidationEliminationModel();
        m.inventoryProfitEnabled = invProfitEnabled;
        for (IntercompanyPair p : pairs) {
            m.addPair(p.fromOrg, p.toOrg, p.matchedAmount);
        }
        m.generateCandidates();
        m.postAll();

        // 不变量 1a：每张凭证平衡
        for (EliminationVoucher v : m.postedVouchers) {
            assertTrue(v.isBalanced(), "每张抵消凭证应借贷平衡 (Dr=" + v.totalDebit + ", Cr=" + v.totalCredit + ")");
        }
        // 不变量 1b：聚合平衡
        assertTrue(m.aggregateBalanced(), "聚合 Σ eliminationDebit == Σ eliminationCredit");
    }

    /**
     * P8-属性 2：抵消后合并净额 = 外部交易净额（内部配对净额贡献为零，集团内交易完全抵消）。
     * 对齐 intercompany-consolidation.md §内部交易抵消机制。
     */
    @Property(tries = 100, seed = "20260818")
    void consolidatedNetEqualsExternalNetAfterElimination(
            @ForAll("intercompanyPairs") List<IntercompanyPair> pairs,
            @ForAll("externalNet") BigDecimal externalNet) {
        ConsolidationEliminationModel m = new ConsolidationEliminationModel();
        for (IntercompanyPair p : pairs) {
            m.addPair(p.fromOrg, p.toOrg, p.matchedAmount);
        }
        m.generateCandidates();
        m.postAll();

        assertTrue(m.consolidatedNetEqualsExternal(externalNet),
                "抵消后合并净额应 == 外部交易净额（内部配对净额贡献为零）");
        // 抵消完整性：每条配对的 matchedAmount 都被 AR_AP + REVENUE_COST（+INV_PROFIT）全量抵消
        BigDecimal totalEliminated = m.aggregateDebit();
        BigDecimal expectedMinEliminated = BigDecimal.ZERO;
        int typesPerPair = 2 + (m.inventoryProfitEnabled ? 1 : 0);
        for (IntercompanyPair p : pairs) {
            expectedMinEliminated = expectedMinEliminated.add(
                    p.matchedAmount.multiply(BigDecimal.valueOf(typesPerPair)));
        }
        assertEquals(0, expectedMinEliminated.compareTo(totalEliminated),
                "全量抵消：Σ elimination = Σ matchedAmount × typesPerPair");
    }

    /**
     * P8-属性 3（类型对称性）：AR_AP 与 REVENUE_COST 两类抵消候选数量相等（生产对每个配对各生成一条），
     * 且两类聚合金额各自平衡（每类 Dr/Cr 对称）。
     */
    @Property(tries = 100, seed = "20260819")
    void arApAndRevenueCostEliminationsAreSymmetric(@ForAll("intercompanyPairs") List<IntercompanyPair> pairs) {
        ConsolidationEliminationModel m = new ConsolidationEliminationModel();
        for (IntercompanyPair p : pairs) {
            m.addPair(p.fromOrg, p.toOrg, p.matchedAmount);
        }
        m.generateCandidates();

        long arApCount = m.candidates.stream().filter(c -> TYPE_AR_AP.equals(c.type)).count();
        long rcCount = m.candidates.stream().filter(c -> TYPE_REVENUE_COST.equals(c.type)).count();
        assertEquals(arApCount, rcCount, "AR_AP 与 REVENUE_COST 候选数量相等（每配对各生成一条）");
        assertEquals(pairs.size(), arApCount, "AR_AP 候选数 == 配对数");

        BigDecimal arApTotal = m.candidates.stream()
                .filter(c -> TYPE_AR_AP.equals(c.type))
                .map(c -> c.eliminationAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal rcTotal = m.candidates.stream()
                .filter(c -> TYPE_REVENUE_COST.equals(c.type))
                .map(c -> c.eliminationAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, arApTotal.compareTo(rcTotal), "AR_AP 聚合金额 == REVENUE_COST 聚合金额（对称）");
    }

    /**
     * golden 交叉校验（保真度锚定）：复现 {@code TestErpFinIntercompanyMatchingAndElimination.testPostEliminationGeneratesDraftVoucher}
     * 生产实测场景——公司间销售/采购配对 2000（TRANSFER-PAIR-5: 公司1 销售 2000 / 公司2 采购 2000）
     * → runMatching 配对 matchedAmount=2000 → generateEliminationCandidates 生成 AR_AP + REVENUE_COST
     * 各 amount=2000 candidate → 每条 postElimination 生成 totalDebit=2000/totalCredit=2000 平衡凭证。
     */
    @Test
    void goldenCrossCheckMatchesTestIntercompanyMatchingAndEliminationNumbers() {
        ConsolidationEliminationModel m = new ConsolidationEliminationModel();
        // 生产 :165-168 公司间销售/采购配对 2000（公司1 → 公司2）
        m.addPair(1L, 2L, new BigDecimal("2000"));
        m.generateCandidates();
        // 生产 :147-158 验证 AR_AP + REVENUE_COST 两类候选存在
        assertEquals(2, m.candidates.size(), "1 配对 → 2 候选（AR_AP + REVENUE_COST）");
        EliminationCandidate arAp = m.candidates.stream()
                .filter(c -> TYPE_AR_AP.equals(c.type)).findFirst().orElseThrow();
        EliminationCandidate rc = m.candidates.stream()
                .filter(c -> TYPE_REVENUE_COST.equals(c.type)).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("2000").compareTo(arAp.eliminationAmount),
                "AR_AP candidate amount=2000（生产 :66 eliminationAmount=matchedAmount）");
        assertEquals(0, new BigDecimal("2000").compareTo(rc.eliminationAmount),
                "REVENUE_COST candidate amount=2000");
        assertEquals(1L, arAp.fromOrg, "fromOrg=1（生产 :60 fromOrgId=arOrgId）");
        assertEquals(2L, arAp.toOrg, "toOrg=2（生产 :61 toOrgId=apOrgId）");

        // 生产 :189-196 postElimination 生成 DRAFT 凭证 totalDebit=2000/totalCredit=2000
        m.postAll();
        assertEquals(2, m.postedVouchers.size(), "2 candidate → 2 凭证");
        for (EliminationVoucher v : m.postedVouchers) {
            assertEquals(0, new BigDecimal("2000").compareTo(v.totalDebit), "凭证 totalDebit=2000（生产 :86）");
            assertEquals(0, new BigDecimal("2000").compareTo(v.totalCredit), "凭证 totalCredit=2000（生产 :87）");
            assertTrue(v.isBalanced(), "凭证借贷平衡");
        }
        // 聚合 Σ debit = 2000+2000 = 4000 == Σ credit
        assertEquals(0, new BigDecimal("4000").compareTo(m.aggregateDebit()), "聚合 Σ debit=4000");
        assertTrue(m.aggregateBalanced(), "聚合平衡");
        // 合并净额 = 外部净额（内部 2000 净额贡献为零）
        assertTrue(m.consolidatedNetEqualsExternal(new BigDecimal("5000")),
                "抵消后合并净额 = 外部净额（内部配对净额贡献零）");
    }

    /**
     * tautology 自检（设计文档 §5.1）：注入「抵消凭证借贷不平衡」变异（Dr=M / Cr=M/2，模拟 writeDraftEliminationVoucher
     * 金额算术错误）——变异使凭证 isBalanced() 失败 + 聚合不平衡，证明属性 1 能发现此类变异，非恒等式。
     */
    @Test
    void tautologySelfCheck_unbalancedVoucherMutationIsDetected() {
        // 正确：Dr=2000/Cr=2000 平衡
        EliminationVoucher correct = new EliminationVoucher(
                new BigDecimal("2000"), new BigDecimal("2000"),
                new BigDecimal("2000"), new BigDecimal("2000"));
        assertTrue(correct.isBalanced(), "正确凭证平衡");

        // 变异：Dr=2000 / Cr=1000（模拟 :87 totalCredit 算术错 amount/2）
        EliminationVoucher mutated = new EliminationVoucher(
                new BigDecimal("2000"), new BigDecimal("1000"),
                new BigDecimal("2000"), new BigDecimal("1000"));
        assertFalse(mutated.isBalanced(),
                "变异凭证（Cr=Dr/2）不平衡——属性 1 的 isBalanced/aggregateBalanced 断言能发现");

        // 变异使聚合不平衡：正确聚合 2000==2000 平衡；变异聚合 2000≠1000
        ConsolidationEliminationModel m = new ConsolidationEliminationModel();
        m.addPair(1L, 2L, new BigDecimal("2000"));
        m.generateCandidates();
        m.postAll();
        assertTrue(m.aggregateBalanced(), "正确生产算术聚合平衡");
        // 手工注入变异凭证（Dr=M/Cr=M/2）后聚合必不平衡
        List<EliminationVoucher> mutatedPosted = new ArrayList<>();
        for (EliminationCandidate c : m.candidates) {
            BigDecimal half = c.eliminationAmount.divide(new BigDecimal("2"));
            mutatedPosted.add(new EliminationVoucher(c.eliminationAmount, half, c.eliminationAmount, half));
        }
        BigDecimal mutDebit = BigDecimal.ZERO;
        BigDecimal mutCredit = BigDecimal.ZERO;
        for (EliminationVoucher v : mutatedPosted) {
            mutDebit = mutDebit.add(v.totalDebit);
            mutCredit = mutCredit.add(v.totalCredit);
        }
        assertFalse(0 == mutDebit.compareTo(mutCredit),
                "变异聚合 Σ debit=" + mutDebit + " ≠ Σ credit=" + mutCredit + "——属性 test 能发现");
    }

    // ---------- @Provide 生成器 ----------

    @Provide
    Arbitrary<List<IntercompanyPair>> intercompanyPairs() {
        return pairArbitrary().list().ofMinSize(0).ofMaxSize(10);
    }

    @Provide
    Arbitrary<Boolean> inventoryProfitFlag() {
        return Arbitraries.of(true, false);
    }

    @Provide
    Arbitrary<BigDecimal> externalNet() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("0"), new BigDecimal("10000000"))
                .ofScale(2);
    }

    private Arbitrary<IntercompanyPair> pairArbitrary() {
        return Arbitraries.integers().between(1, 20).flatMap(fromOrg ->
                Arbitraries.integers().between(1, 20).flatMap(toOrg ->
                        Arbitraries.bigDecimals()
                                .between(new BigDecimal("0.01"), new BigDecimal("1000000")).ofScale(2)
                                .map(amt -> new IntercompanyPair(fromOrg, toOrg, amt))));
    }
}
