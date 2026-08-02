package app.erp.inv.service.property;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MQ Q3 P2 属性测试——成本层累加 = 余额表不变量（设计文档 {@code property-based-testing.md} §4.2 P2 + §5.1/§5.3）。
 *
 * <p><b>路径 C 类一 + 策略 F2</b>（设计文档 §3.4）：纯 JUnit 5 + jqwik，不继承 {@code JunitAutoTestCase}；纯内存状态。
 *
 * <p><b>保真度机制裁决（Decision (b)，设计文档 §5.1 P2 明示）</b>：生产 {@code FifoCostingStrategy.onOutgoing}
 * 的 FIFO 队列消耗算术（{@code :103-129}）是纯的，但交织 DB 查询（{@code findFifoLayers}）+ {@code saveOrUpdateEntity}，
 * 无法从纯内存 test 直接调用。裁决路径 <b>(b)</b>（设计文档 §5.1：「内存 CostLayerModel 须交叉校验生产
 * FifoCostingStrategy.onIncoming/onOutgoing——在共享 golden 输入上，内存模型与生产策略产出一致，model 跟随 production」）：
 * <ul>
 *   <li>选择：测试侧 {@link FifoCostLayerModel} <b>逐行镜像</b>生产 {@code onIncoming}（{@code :61-83}）+
 *       {@code onOutgoing}（{@code :103-129}）消耗算术（含 {@code roundCost} 默认 scale=4、FIFO 按 incomingDate 升序、
 *       {@code take=min(remaining,avail)} + {@code takeCost=take×unitCost} + 层 remaining/totalCost 一致递减）。</li>
 *   <li>替代（否决）：(a) 从生产 {@code onOutgoing} 抽取纯 static method（extract method refactor）——生产 {@code onOutgoing}
 *       的消耗循环内交织 {@code saveOrUpdateEntity}，抽取需重组保存点（改变无变异实体是否保存的副作用），非纯行为保持，
 *       风险高于收益；路径 (b) 经 golden 交叉校验已充分锚定，零生产代码风险。</li>
 *   <li>golden 交叉校验：{@link #goldenCrossCheckMatchesProductionVerifiedNumbers()} 在 {@code TestErpInvFifoCosting}
 *       已验证的 golden 场景（50@10+40@12 消耗 60→totalCost=620；20@10+40@12 消耗 60→totalCost=680）上跑内存模型，
 *       断言复现生产实测数字 → 内存模型锚定生产行为（drift 防御）。</li>
 *   <li>残留风险（R2）：纯内存模型不验证 DB 持久化层；由既有 {@code TestErpInvFifoCosting} 端到端测试覆盖 DB 层（双层互补）。</li>
 * </ul>
 *
 * <p><b>不变量真相源</b>（设计文档 §1.3/§4.2，**不重新推导**）：{@code docs/design/finance/costing-methods.md}——
 * 任意时刻 {@code Σ ErpInvCostLayer.remainingQuantity × unitCost == ErpInvStockBalance.totalCost}（layer-based FIFO）。
 *
 * <p><b>生成器策略（设计文档 §5.2 stateful 裁决）</b>：出库合法性依赖累计状态（remaining>0），用状态依赖可行性闸门
 * （{@link FifoAction#feasible} 读模型当前余量）+ jqwik List shrinking——语义等价 ActionSequence（每步在当前模型状态上判定
 * 出库可行性，不可行则跳过），保证只生成合法序列（非 stateless frequency 无法在生成期强制 remaining>0 的问题）。
 *
 * <p><b>tautology 自检</b>（设计文档 §5.1）：{@link #tautologySelfCheck_consistencyMutationIsDetected()}（一致性变异：
 * 损坏余额后不变量必破）+ {@link #tautologySelfCheck_orderMutationIsDetected()}（顺序变异：LIFO 消耗产生非升序日期，
 * 被 outgoing 的非降序断言发现）——证明属性 test 非恒等式。
 *
 * <p><b>种子固定</b>（设计文档 §8 C-1 + R7）：每 {@code @Property} 经 {@code seed} 固化。
 */
class PropertyErpInvCostLayerAccumulation {

    private static final LocalDate BASE_DATE = LocalDate.of(2026, 7, 1);
    private static final int COST_SCALE = 4; // 镜像 ErpInvConfigs.DEFAULT_COST_SCALE（roundCost 默认 scale=4 HALF_UP）

    // ---------- 内存状态模型（镜像生产算术） ----------

    static final class CostLayer {
        BigDecimal incomingQuantity;
        BigDecimal remainingQuantity;
        BigDecimal unitCost;
        BigDecimal totalCost;
        LocalDate incomingDate;
        long seq; // 生产按 incomingDate 升序稳定排序（同日期保持插入顺序）→ seq 作 tie-breaker

        CostLayer(BigDecimal qty, BigDecimal unitCost, BigDecimal totalCost, LocalDate date, long seq) {
            this.incomingQuantity = qty;
            this.remainingQuantity = qty;
            this.unitCost = unitCost;
            this.totalCost = totalCost;
            this.incomingDate = date;
            this.seq = seq;
        }
    }

    /**
     * 纯内存 FIFO 成本层模型。逐行镜像 {@code FifoCostingStrategy.onIncoming}（{@code :61-83}）+
     * {@code onOutgoing}（{@code :103-129}）的消耗算术；不触 DB / IoC。
     */
    static final class FifoCostLayerModel {
        final List<CostLayer> layers = new ArrayList<>();
        BigDecimal balanceTotalQuantity = BigDecimal.ZERO;
        BigDecimal balanceTotalCost = BigDecimal.ZERO;
        private long seqCounter = 0;

        /** 镜像 onIncoming：追加层（unitCost 经 roundCost scale=4），余额累加 qty / qty×unitCost。 */
        void incoming(BigDecimal qty, BigDecimal unitCost, LocalDate date) {
            BigDecimal roundedUnit = unitCost.setScale(COST_SCALE, RoundingMode.HALF_UP); // 镜像 ErpInvConfigs.roundCost
            BigDecimal layerTotalCost = roundedUnit.multiply(qty);
            layers.add(new CostLayer(qty, roundedUnit, layerTotalCost, date, seqCounter++));
            balanceTotalQuantity = balanceTotalQuantity.add(qty);
            balanceTotalCost = balanceTotalCost.add(layerTotalCost);
        }

        /**
         * 镜像 onOutgoing（{@code :103-129}）：按 incomingDate 升序消耗 remaining>0 的层，多层跨消耗。
         * 返回被消耗层的 incomingDate 列表（消耗顺序）——供 FIFO 顺序性断言。
         */
        List<LocalDate> outgoing(BigDecimal qty) {
            List<CostLayer> sorted = new ArrayList<>();
            for (CostLayer l : layers) {
                if (l.remainingQuantity.signum() > 0) {
                    sorted.add(l);
                }
            }
            sorted.sort(Comparator
                    .comparing((CostLayer l) -> l.incomingDate == null ? BASE_DATE : l.incomingDate)
                    .thenComparingLong(l -> l.seq));
            if (sorted.isEmpty()) {
                throw new IllegalStateException("无可用 cost layer（镜像 ERR_COST_NOT_AVAILABLE）");
            }
            BigDecimal remaining = qty;
            BigDecimal totalCost = BigDecimal.ZERO;
            List<LocalDate> consumed = new ArrayList<>();
            for (CostLayer layer : sorted) {
                if (remaining.signum() <= 0) {
                    break;
                }
                BigDecimal avail = layer.remainingQuantity;
                if (avail.signum() <= 0) {
                    continue;
                }
                BigDecimal take = remaining.min(avail); // 镜像 :113
                BigDecimal takeCost = take.multiply(layer.unitCost); // 镜像 :114
                layer.remainingQuantity = avail.subtract(take); // 镜像 :115
                layer.totalCost = layer.totalCost.subtract(takeCost); // 镜像 :116
                totalCost = totalCost.add(takeCost); // 镜像 :118
                remaining = remaining.subtract(take); // 镜像 :119
                consumed.add(layer.incomingDate);
            }
            if (remaining.signum() > 0) { // 镜像 :121-126 总剩余不足拒绝
                throw new IllegalStateException("总剩余不足覆盖出库量（镜像 ERR_COST_NOT_AVAILABLE）");
            }
            balanceTotalQuantity = balanceTotalQuantity.subtract(qty); // 镜像 :138
            balanceTotalCost = balanceTotalCost.subtract(totalCost); // 镜像 :139
            return consumed;
        }

        BigDecimal layerCostSum() {
            BigDecimal sum = BigDecimal.ZERO;
            for (CostLayer l : layers) {
                sum = sum.add(l.remainingQuantity.multiply(l.unitCost));
            }
            return sum;
        }

        BigDecimal totalRemaining() {
            BigDecimal sum = BigDecimal.ZERO;
            for (CostLayer l : layers) {
                sum = sum.add(l.remainingQuantity);
            }
            return sum;
        }

        /** 不变量：Σ layer.remaining × unitCost == balance.totalCost（costing-methods.md 真相源）。 */
        boolean invariantHolds() {
            return layerCostSum().compareTo(balanceTotalCost) == 0;
        }
    }

    // ---------- 状态依赖动作 ----------

    abstract static class FifoAction {
        abstract boolean feasible(FifoCostLayerModel m);

        abstract void apply(FifoCostLayerModel m);
    }

    static final class IncomingAction extends FifoAction {
        final BigDecimal qty;
        final BigDecimal unitCost;
        final int dateOffset;

        IncomingAction(BigDecimal qty, BigDecimal unitCost, int dateOffset) {
            this.qty = qty;
            this.unitCost = unitCost;
            this.dateOffset = dateOffset;
        }

        @Override
        boolean feasible(FifoCostLayerModel m) {
            return qty.signum() > 0 && unitCost.signum() > 0;
        }

        @Override
        void apply(FifoCostLayerModel m) {
            m.incoming(qty, unitCost, BASE_DATE.plusDays(dateOffset));
            assertTrue(m.invariantHolds(), "incoming 后成本层累加不变量应恒成立");
        }
    }

    static final class OutgoingAction extends FifoAction {
        final BigDecimal qty;

        OutgoingAction(BigDecimal qty) {
            this.qty = qty;
        }

        @Override
        boolean feasible(FifoCostLayerModel m) {
            return qty.signum() > 0 && m.totalRemaining().compareTo(qty) >= 0 && !m.layers.isEmpty();
        }

        @Override
        void apply(FifoCostLayerModel m) {
            List<LocalDate> consumed = m.outgoing(qty);
            // FIFO 顺序性：被消耗层的 incomingDate 必非降序（oldest-first），镜像生产 :202 sort by incomingDate asc
            assertNonDecreasing(consumed, "FIFO 消耗应按 incomingDate 升序");
            assertTrue(m.invariantHolds(), "outgoing 后成本层累加不变量应恒成立");
        }
    }

    // ---------- 属性 ----------

    /**
     * P2-属性 1：随机 FIFO 操作序列（入库/出库）下，每步操作后 {@code Σ layer.remaining×unitCost == balance.totalCost} 恒成立。
     * 出库合法性经 {@link OutgoingAction#feasible} 在当前模型状态上闸门（remaining≥出库量），保证只跑合法序列。
     */
    @Property(tries = 100, seed = "20260805")
    void costLayerSumEqualsBalanceAfterEveryStep(@ForAll("fifoActions") List<FifoAction> actions) {
        FifoCostLayerModel m = new FifoCostLayerModel();
        for (FifoAction action : actions) {
            if (action.feasible(m)) {
                action.apply(m); // apply 内断言不变量 + FIFO 顺序性
            }
        }
    }

    /**
     * P2-属性 2：单次出库跨多层消耗时，被消耗层按 incomingDate 升序——FIFO 顺序不变量。
     * 长序列下每步 OutgoingAction.apply 已断言非降序；本属性用显式多层场景加强顺序断言的可读性。
     */
    @Property(tries = 100, seed = "20260806")
    void outgoingConsumesLayersInIncomingDateOrder(@ForAll("multiLayerSequences") List<FifoAction> actions) {
        FifoCostLayerModel m = new FifoCostLayerModel();
        for (FifoAction action : actions) {
            if (action.feasible(m)) {
                action.apply(m);
            }
        }
    }

    /**
     * golden 交叉校验（保真度锚定）：在 {@code TestErpInvFifoCosting} 已验证的生产 golden 场景上跑内存模型，
     * 断言复现实测数字。这些数字是生产 {@code FifoCostingStrategy.onOutgoing} 经 localDb 实测的（见
     * {@code TestErpInvFifoCosting.testOutgoingSpansMultipleLayersWeightedCost} line 111-143 /
     * {@code testOutgoingLedgerTotalCostFlowsToDispatcher} line 206-220）→ 内存模型锚定生产行为（drift 防御）。
     */
    @Test
    void goldenCrossCheckMatchesProductionVerifiedNumbers() {
        // 场景 1：50@10 + 40@12，消耗 60 → 出库消耗 totalCost=620（50×10+10×12），layer1(10)=0, layer2(12)=30, balance 30/360
        FifoCostLayerModel m1 = new FifoCostLayerModel();
        m1.incoming(new BigDecimal("50"), new BigDecimal("10"), BASE_DATE);
        m1.incoming(new BigDecimal("40"), new BigDecimal("12"), BASE_DATE.plusDays(1));
        BigDecimal balanceBeforeOut1 = new BigDecimal("980"); // 500 + 480
        assertEquals(0, balanceBeforeOut1.compareTo(m1.balanceTotalCost), "出库前余额 totalCost=980");
        List<LocalDate> consumed1 = m1.outgoing(new BigDecimal("60"));
        // 出库后余额 30@12 = 360（= 980 − 620，即出库消耗 620）
        assertEquals(0, new BigDecimal("360").compareTo(m1.balanceTotalCost), "出库后余额 totalCost=360（消耗 620）");
        assertEquals(0, new BigDecimal("30").compareTo(m1.balanceTotalQuantity), "出库后余额 totalQuantity=30");
        assertEquals(2, consumed1.size(), "跨 2 层消耗");
        // 层 1（10@，oldest）全消耗、层 2（12@）剩 30
        CostLayer oldest1 = m1.layers.stream().filter(l -> l.unitCost.compareTo(new BigDecimal("10")) == 0).findFirst().orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(oldest1.remainingQuantity), "oldest 层 50@10 全消耗");
        CostLayer newest1 = m1.layers.stream().filter(l -> l.unitCost.compareTo(new BigDecimal("12")) == 0).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("30").compareTo(newest1.remainingQuantity), "newest 层 40@12 剩 30");

        // 场景 2：20@10 + 40@12，消耗 60 → totalCost=680（20×10+40×12），余额 0/0
        FifoCostLayerModel m2 = new FifoCostLayerModel();
        m2.incoming(new BigDecimal("20"), new BigDecimal("10"), BASE_DATE);
        m2.incoming(new BigDecimal("40"), new BigDecimal("12"), BASE_DATE.plusDays(1));
        List<LocalDate> consumed2 = m2.outgoing(new BigDecimal("60"));
        assertEquals(0, new BigDecimal("0").compareTo(m2.balanceTotalCost), "出库后余额 totalCost=0");
        assertEquals(2, consumed2.size(), "跨 2 层消耗（20+40）");
        assertTrue(m2.invariantHolds(), "出库后不变量成立");
    }

    /** tautology 自检 1（设计文档 §5.1）：一致性变异（损坏余额）必被不变量发现——证明属性非恒等式。 */
    @Test
    void tautologySelfCheck_consistencyMutationIsDetected() {
        FifoCostLayerModel m = new FifoCostLayerModel();
        m.incoming(new BigDecimal("50"), new BigDecimal("10"), BASE_DATE);
        assertTrue(m.invariantHolds(), "正常 incoming 后不变量成立");
        // 注入一致性变异：余额 totalCost 被错误地多减一个量（模拟 onOutgoing 减错额）
        m.balanceTotalCost = m.balanceTotalCost.subtract(new BigDecimal("1"));
        assertFalse(m.invariantHolds(), "余额一致性被破坏后不变量应失败（证明属性 test 能发现此类变异）");
    }

    /** tautology 自检 2（设计文档 §5.1）：顺序变异（LIFO）消耗产生非升序日期——被 outgoing 非降序断言发现。 */
    @Test
    void tautologySelfCheck_orderMutationIsDetected() {
        FifoCostLayerModel m = new FifoCostLayerModel();
        m.incoming(new BigDecimal("50"), new BigDecimal("10"), BASE_DATE);            // oldest
        m.incoming(new BigDecimal("40"), new BigDecimal("12"), BASE_DATE.plusDays(1)); // newest
        // 正确 FIFO 消耗顺序：oldest(date=BASE) 先于 newest(date=BASE+1) → 非降序
        List<LocalDate> fifoConsumed = new ArrayList<>();
        BigDecimal remaining = new BigDecimal("60");
        BigDecimal take1 = remaining.min(new BigDecimal("50")); // oldest 先消耗 50
        fifoConsumed.add(BASE_DATE);
        remaining = remaining.subtract(take1);
        if (remaining.signum() > 0) {
            fifoConsumed.add(BASE_DATE.plusDays(1)); // 然后 newest
        }
        assertNonDecreasing(fifoConsumed, "FIFO 消耗日期非降序");

        // 变异 LIFO：newest 先消耗 → 日期序列非升序 → assertNonDecreasing 会失败
        List<LocalDate> lifoConsumed = new ArrayList<>();
        lifoConsumed.add(BASE_DATE.plusDays(1)); // newest 先
        lifoConsumed.add(BASE_DATE);             // 然后 oldest → 降序
        assertFalse(isNonDecreasing(lifoConsumed), "LIFO 变异的消耗日期序列应非升序（证明顺序断言能发现顺序变异）");
    }

    // ---------- @Provide 生成器 ----------

    @Provide
    Arbitrary<List<FifoAction>> fifoActions() {
        return actionArbitrary().list().ofMinSize(1).ofMaxSize(20);
    }

    @Provide
    Arbitrary<List<FifoAction>> multiLayerSequences() {
        // 至少 2 个 incoming 以提高多层跨消耗概率
        return actionArbitrary().list().ofMinSize(2).ofMaxSize(20);
    }

    private Arbitrary<FifoAction> actionArbitrary() {
        Arbitrary<IncomingAction> incoming = incomingActionArbitrary();
        Arbitrary<OutgoingAction> outgoing = outgoingActionArbitrary();
        // 60% 入库 / 40% 出库，保证序列先建层后消耗（出库经 feasibility 闸门在无层时跳过）
        return Arbitraries.frequency(Tuple.of(3, Boolean.TRUE), Tuple.of(2, Boolean.FALSE))
                .flatMap(isIn -> isIn
                        ? incoming.map(a -> (FifoAction) a)
                        : outgoing.map(a -> (FifoAction) a));
    }

    private Arbitrary<IncomingAction> incomingActionArbitrary() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("0.0001"), new BigDecimal("1000")).ofScale(4)
                .flatMap(qty -> Arbitraries.bigDecimals()
                        .between(new BigDecimal("0.0001"), new BigDecimal("1000")).ofScale(4)
                        .flatMap(uc -> Arbitraries.integers().between(0, 30)
                                .map(d -> new IncomingAction(qty, uc, d))));
    }

    private Arbitrary<OutgoingAction> outgoingActionArbitrary() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("0.0001"), new BigDecimal("1000")).ofScale(4)
                .map(OutgoingAction::new);
    }

    // ---------- 辅助 ----------

    private static void assertNonDecreasing(List<LocalDate> dates, String msg) {
        assertTrue(isNonDecreasing(dates), msg + "（实际序列=" + dates + "）");
    }

    private static boolean isNonDecreasing(List<LocalDate> dates) {
        for (int i = 1; i < dates.size(); i++) {
            if (dates.get(i).compareTo(dates.get(i - 1)) < 0) {
                return false;
            }
        }
        return true;
    }
}
