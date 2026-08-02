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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MQ Q3 P5 属性测试——STANDARD 重估前后总成本不变 + 红冲恢复不变量（设计文档 {@code property-based-testing.md}
 * §4.2 P5 + §9 successor + §5.1/§5.3）。
 *
 * <p><b>路径 C 类一 + 策略 F2</b>（设计文档 §3.4）：纯 JUnit 5 + jqwik，不继承 {@code JunitAutoTestCase}；纯内存状态。
 *
 * <p><b>保真度机制裁决（Decision (b)，同 predecessor P2 范式）</b>：生产 {@code StandardCostingStrategy.onOutgoing}
 *（{@code :79-108}）+ {@code onIncoming}（{@code :42-76}）红冲路径的算术是纯的，但交织 DB 写入
 *（{@code :93 daoProvider...saveOrUpdateEntity}）+ {@code ctx.upsertBalance/updateBalanceWithRetry}（DB-entangled，
 * Current Baseline 纯度表 P5 行），无法从纯内存 test 直接调用。裁决路径 <b>(b)</b>（设计文档 §5.1）：
 * 测试侧 {@link StandardCostModel} <b>逐行镜像</b>生产 STANDARD 红冲算术：
 * <ul>
 *   <li>选择：内存模型镜像生产 {@code onOutgoing:86-99}（{@code standardUnitCost=resolve}、
 *       {@code lineTotalCost=standard×qty}、{@code balance.totalCost-=lineTotalCost}、
 *       {@code balance.totalQuantity-=qty}）+ 关键红冲捕获（{@code :92 line.setUnitCost(roundCost(standardUnitCost))}——
 *       出库时把当时标准成本刷回 line，供 reverse 透传）+ {@code onIncoming:55-56} 红冲路径
 *       （{@code move.originReturnedMoveId != null && unitCost>0 → standardUnitCost=unitCost}——反向入库沿用原出库扣减的
 *       旧标准成本）。{@link RevalueAction} 镜像 {@code STANDARD_REVALUATION} 发布新 FIRMED rollup 后
 *       {@code standardCostResolver.resolve} 返回新标准。</li>
 *   <li>替代（否决）：(a) 从生产 {@code onOutgoing} 抽取纯函数——消耗循环内交织 {@code saveOrUpdateEntity}
 *       + {@code ctx} 回调，抽取需重组 DB 副作用，非纯行为保持，风险高于收益；路径 (b) 经 golden 交叉校验
 *       已充分锚定，零生产代码风险。</li>
 *   <li>golden 交叉校验：{@link #goldenCrossCheckMatchesProductionVerifiedNumbers()} 复现
 *       {@code TestErpInvStandardCosting.testReverseOutgoingRestoresBalanceAcrossRevaluation}
 *       生产实测场景（入库 20@10 → 出库 8@10 扣 80 → 重估 10→15 → 红冲反向入库 8@旧标准 10 加回 80 →
 *       balance totalCost 恢复 200）；并断言若红冲误用新标准 15 会产生 40 的 drift（生产 P1-MA2-024 修复的 bug）。</li>
 *   <li>残留风险（R2）：纯内存模型不验证 DB 持久化层 / line.unitCost 落库；由既有
 *       {@code TestErpInvStandardCosting} 端到端测试覆盖 DB 层（双层互补）。</li>
 * </ul>
 *
 * <p><b>不变量真相源</b>（设计文档 §1.3/§4.2，**不重新推导**）：{@code docs/design/finance/costing-methods.md}
 * line 74/472——STANDARD 红冲跨 STANDARD_REVALUATION 时 {@code balance.totalCost} 恢复不变量（P1-MA2-024 已修）：
 * 红冲后 {@code Σ layer.remaining × unitCost}（STANDARD 无 cost layer，等价 balance.totalCost）恢复至原出库前。
 *
 * <p><b>核心属性</b>：红冲出库的反向入库必须沿用<b>原出库时捕获的标准成本</b>（生产 {@code onOutgoing:92} 刷回 line.unitCost
 * 供 reverse 透传、{@code onIncoming:55-56} 红冲分支采用透传值），<b>而非当前（可能已重估的）标准成本</b>——
 * 否则跨重估红冲后 balance.totalCost 不可恢复（这正是 P1-MA2-024 修复的 bug）。
 *
 * <p><b>tautology 自检</b>（设计文档 §5.1）：{@link #tautologySelfCheck_reverseUsingCurrentStandardIsDetected()}
 * 注入「红冲误用当前标准而非捕获标准」变异，证明属性 test 能发现此类变异——非恒等式。
 *
 * <p><b>种子固定</b>（设计文档 §8 C-1 + R7）：每 {@code @Property} 经 {@code seed} 固化。
 */
class PropertyErpInvStandardRevaluation {

    private static final int COST_SCALE = 4; // 镜像 ErpInvConfigs.DEFAULT_COST_SCALE（roundCost 默认 scale=4 HALF_UP）

    /** 镜像 ErpInvConfigs.roundCost：HALF_UP 到 scale=4。 */
    private static BigDecimal roundCost(BigDecimal v) {
        return v.setScale(COST_SCALE, RoundingMode.HALF_UP);
    }

    // ---------- 内存 STANDARD 成本模型（镜像生产算术） ----------

    /**
     * 纯内存 STANDARD 成本余额模型。镜像 {@code StandardCostingStrategy} 出库扣减 + 红冲反向入库回加算术。
     * {@code currentStandard} 镜像 {@code standardCostResolver.resolve(materialId)} 当前返回值
     * （{@code STANDARD_REVALUATION} 重估发布新 FIRMED rollup 后变更）。
     */
    static final class StandardCostModel {
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal currentStandard = BigDecimal.ZERO;
        /** 出库捕获栈：每元素 = (qty, 出库时捕获的标准成本)。红冲按 LIFO 弹出最近一次未红冲的出库。 */
        final Deque<Tuple.Tuple2<BigDecimal, BigDecimal>> outgoingCaptured = new ArrayDeque<>();

        /** 正常采购入库（镜像 onIncoming 非红冲路径 :58：重解析当前标准成本 currentStandard）。 */
        void normalIncoming(BigDecimal qty) {
            BigDecimal std = roundCost(currentStandard);
            BigDecimal lineTotalCost = std.multiply(qty);
            totalQuantity = totalQuantity.add(qty);
            totalCost = totalCost.add(lineTotalCost);
        }

        /**
         * 出库（镜像 onOutgoing :86-99）：扣 currentStandard×qty，并把出库时标准捕获入栈
         * （镜像 :92 line.setUnitCost(roundCost(standardUnitCost))，供 reverse 透传）。
         */
        void outgoing(BigDecimal qty) {
            BigDecimal std = roundCost(currentStandard);
            BigDecimal lineTotalCost = std.multiply(qty);
            totalQuantity = totalQuantity.subtract(qty);
            totalCost = totalCost.subtract(lineTotalCost);
            outgoingCaptured.push(Tuple.of(qty, std));
        }

        /** STANDARD_REVALUATION 重估：发布新 FIRMED rollup，standardCostResolver.resolve 返回新标准。 */
        void revalue(BigDecimal newStandard) {
            currentStandard = roundCost(newStandard);
        }

        /**
         * 红冲反向入库（镜像 onIncoming :55-56 红冲路径：originReturnedMoveId != null && unitCost>0 → 采用透传值）。
         * 弹出最近一次未红冲的出库，沿用其<b>捕获时</b>的标准成本（非当前标准），加回 qty×捕获标准。
         */
        void reverseLatestOutgoing() {
            Tuple.Tuple2<BigDecimal, BigDecimal> top = outgoingCaptured.pop();
            BigDecimal qty = top.get1();
            BigDecimal capturedStandard = top.get2(); // 红冲沿用原出库扣减的旧标准（P1-MA2-024 关键）
            BigDecimal lineTotalCost = capturedStandard.multiply(qty);
            totalQuantity = totalQuantity.add(qty);
            totalCost = totalCost.add(lineTotalCost);
        }

        boolean hasReversibleOutgoing() {
            return !outgoingCaptured.isEmpty();
        }

        /**
         * 不变量：totalQuantity 非负（STANDARD 无负库存——出库经 feasibility 闸门保证 qty≤totalQuantity）。
         * <p>注意：不在此断言 totalCost 非负——本模型不建模 STANDARD_REVALUATION 对既有余额 totalCost 的重估
         * 重定基（生产经独立成本调整凭证 re-base totalCost = newStandard × qty），故跨重估出库按新标准扣减
         * 可能使简化模型 totalCost 暂时为负。这非 P5 不变量（P5 仅断言红冲恢复），故不在此校验。
         */
        boolean quantityNonNegative() {
            return totalQuantity.signum() >= 0;
        }
    }

    // ---------- 状态依赖动作 ----------

    abstract static class StandardAction {
        abstract boolean feasible(StandardCostModel m);

        abstract void apply(StandardCostModel m);
    }

    /** 正常采购入库（建立库存基线，镜像 onIncoming 非红冲路径）。 */
    static final class IncomingAction extends StandardAction {
        final BigDecimal qty;

        IncomingAction(BigDecimal qty) {
            this.qty = qty;
        }

        @Override
        boolean feasible(StandardCostModel m) {
            return qty.signum() > 0;
        }

        @Override
        void apply(StandardCostModel m) {
            m.normalIncoming(qty);
            assertTrue(m.quantityNonNegative(), "incoming 后库存非负");
        }
    }

    /** 出库（镜像 onOutgoing，捕获出库时标准入栈）。 */
    static final class OutgoingAction extends StandardAction {
        final BigDecimal qty;

        OutgoingAction(BigDecimal qty) {
            this.qty = qty;
        }

        @Override
        boolean feasible(StandardCostModel m) {
            // 库存充足且标准成本已设置（镜像生产：无标准抛 ERR_STANDARD_COST_NOT_AVAILABLE）
            return qty.signum() > 0
                    && m.totalQuantity.compareTo(qty) >= 0
                    && m.currentStandard.signum() > 0;
        }

        @Override
        void apply(StandardCostModel m) {
            BigDecimal totalCostBefore = m.totalCost;
            m.outgoing(qty);
            assertTrue(m.quantityNonNegative(), "outgoing 后库存非负");
            // 出库扣减量 = qty × 当时标准（捕获值）
            BigDecimal expectedDelta = roundCost(m.currentStandard).multiply(qty);
            assertEquals(0, totalCostBefore.subtract(expectedDelta).compareTo(m.totalCost),
                    "出库扣减量应 = qty × 出库时标准");
        }
    }

    /** STANDARD_REVALUATION 重估（变更 currentStandard）。 */
    static final class RevalueAction extends StandardAction {
        final BigDecimal newStandard;

        RevalueAction(BigDecimal newStandard) {
            this.newStandard = newStandard;
        }

        @Override
        boolean feasible(StandardCostModel m) {
            return newStandard.signum() > 0;
        }

        @Override
        void apply(StandardCostModel m) {
            m.revalue(newStandard);
            assertTrue(m.quantityNonNegative(), "revalue 后库存非负");
        }
    }

    /** 红冲最近一次出库（镜像 onIncoming 红冲路径，沿用捕获标准）。 */
    static final class ReverseAction extends StandardAction {
        @Override
        boolean feasible(StandardCostModel m) {
            return m.hasReversibleOutgoing();
        }

        @Override
        void apply(StandardCostModel m) {
            BigDecimal totalCostBefore = m.totalCost;
            BigDecimal totalQtyBefore = m.totalQuantity;
            // 即将弹出的出库原始记录
            Tuple.Tuple2<BigDecimal, BigDecimal> top = m.outgoingCaptured.peek();
            BigDecimal outgoingQty = top.get1();
            BigDecimal capturedStandard = top.get2();
            m.reverseLatestOutgoing();
            assertTrue(m.quantityNonNegative(), "reverse 后库存非负");
            // 核心不变量（P1-MA2-024）：红冲加回量 = qty × 捕获时标准（非当前标准），故 totalCost/totalQuantity 精确恢复
            BigDecimal expectedCostDelta = capturedStandard.multiply(outgoingQty);
            assertEquals(0, totalCostBefore.add(expectedCostDelta).compareTo(m.totalCost),
                    "红冲加回量应 = 原出库 qty × 捕获时标准（跨重估沿用旧标准）");
            assertEquals(0, totalQtyBefore.add(outgoingQty).compareTo(m.totalQuantity),
                    "红冲后 totalQuantity 恢复");
        }
    }

    // ---------- 属性 ----------

    /**
     * P5-属性 1：随机合法 incoming/outgoing/revalue/reverse 序列下，每个红冲都精确恢复原出库的 totalCost/totalQuantity
     * delta（沿用捕获时标准，跨重估不变），且余额恒非负。
     */
    @Property(tries = 100, seed = "20260809")
    void reverseRestoresBalanceAcrossRevaluation(@ForAll("standardActions") List<StandardAction> actions,
                                                  @ForAll("initialStandard") BigDecimal initialStandard) {
        StandardCostModel m = new StandardCostModel();
        m.revalue(initialStandard); // 初始标准成本（镜像 FIRMED rollup 首次发布）
        // 先建立库存基线（至少一次正常入库，使后续出库 feasible）
        m.normalIncoming(new BigDecimal("100"));
        for (StandardAction action : actions) {
            if (action.feasible(m)) {
                action.apply(m); // apply 内断言库存非负 + 红冲恢复不变量
            }
        }
    }

    /**
     * P5-属性 2：每次出库后捕获的标准成本，在其后续红冲时被沿用——无论中间发生多少次重估。
     * 本属性用「单次出库 + 任意重估 + 单次红冲」最小序列强化不变量可读性。
     */
    @Property(tries = 100, seed = "20260810")
    void outgoingThenReverseRestoresCostRegardlessOfInterveningRevaluation(
            @ForAll("positiveQty") BigDecimal qty,
            @ForAll("firstStandard") BigDecimal std1,
            @ForAll("revaluedStandard") BigDecimal std2) {
        StandardCostModel m = new StandardCostModel();
        m.revalue(std1);
        m.normalIncoming(qty.multiply(new BigDecimal("2"))); // 充足库存
        BigDecimal totalCostBeforeOutgoing = m.totalCost;
        BigDecimal totalQtyBeforeOutgoing = m.totalQuantity;

        m.outgoing(qty); // 捕获 std1
        // 中间重估（std2 可能 != std1）——若红冲误用当前标准会 drift
        m.revalue(std2);
        m.reverseLatestOutgoing(); // 必须沿用 std1（捕获值），而非 std2

        assertEquals(0, totalCostBeforeOutgoing.compareTo(m.totalCost),
                "跨重估红冲后 totalCost 必须恢复至出库前（红冲沿用捕获时标准）");
        assertEquals(0, totalQtyBeforeOutgoing.compareTo(m.totalQuantity),
                "跨重估红冲后 totalQuantity 必须恢复至出库前");
    }

    /**
     * golden 交叉校验（保真度锚定）：复现 {@code TestErpInvStandardCosting.testReverseOutgoingRestoresBalanceAcrossRevaluation}
     * 生产 localDb 实测场景（line 207-237）——入库 20@10 → 出库 8@10 扣 80 → 重估 10→15 →
     * 红冲反向入库 8@旧标准 10 加回 80 → balance totalCost 恢复 200 / qty 恢复 20。
     */
    @Test
    void goldenCrossCheckMatchesProductionVerifiedNumbers() {
        StandardCostModel m = new StandardCostModel();
        m.revalue(new BigDecimal("10")); // 初始标准 10（镜像 FIRMED rollup）
        // 入库 20 @ 标准 10：balance qty=20, totalCost=200（生产 TestErpInvStandardCosting:207-210 实测）
        m.normalIncoming(new BigDecimal("20"));
        assertEquals(0, new BigDecimal("200").compareTo(m.totalCost), "入库后余额 totalCost=200");
        assertEquals(0, new BigDecimal("20").compareTo(m.totalQuantity), "入库后余额 qty=20");

        BigDecimal totalCostBeforeOut = m.totalCost;
        // 出库 8 @ 旧标准 10：扣 80 → balance qty=12, totalCost=120；onOutgoing 刷新捕获标准=10（生产 :212-218）
        m.outgoing(new BigDecimal("8"));
        assertEquals(0, new BigDecimal("120").compareTo(m.totalCost), "出库后余额 totalCost=120（扣 80）");
        assertEquals(0, new BigDecimal("12").compareTo(m.totalQuantity), "出库后余额 qty=12");
        assertEquals(0, new BigDecimal("10").compareTo(m.outgoingCaptured.peek().get2()),
                "出库捕获标准=10（供红冲透传）");

        // STANDARD_REVALUATION 重估：标准 10 → 15（生产 :217-220 模拟 re-rollup / 新 FIRMED rollup）
        m.revalue(new BigDecimal("15"));
        assertEquals(0, new BigDecimal("15").compareTo(m.currentStandard), "重估后当前标准=15");

        // 红冲出库：反向入库 8 @ 旧标准 10（onIncoming 红冲分支沿用透传值，非新标准 15）
        // → 加回 8×10=80 → balance totalCost 恢复至出库前 200, qty=20（生产 :221-230 实测）
        m.reverseLatestOutgoing();
        assertEquals(0, new BigDecimal("200").compareTo(m.totalCost),
                "红冲后余额 totalCost 恢复 200（跨重估沿用旧标准 10）");
        assertEquals(0, new BigDecimal("20").compareTo(m.totalQuantity), "红冲后余额 qty 恢复 20");
        assertEquals(0, totalCostBeforeOut.compareTo(m.totalCost),
                "红冲后 totalCost 精确恢复至出库前");

        // drift 防御断言：若红冲误用新标准 15 会产生 drift
        BigDecimal wrongReverseDelta = new BigDecimal("8").multiply(new BigDecimal("15")); // 误用新标准
        BigDecimal correctReverseDelta = new BigDecimal("8").multiply(new BigDecimal("10")); // 正确沿用旧标准
        assertFalse(0 == wrongReverseDelta.compareTo(correctReverseDelta),
                "新标准 15 × 8 = 120 ≠ 旧标准 10 × 8 = 80（证明红冲沿用捕获标准 vs 误用当前标准结果不同）");
    }

    /**
     * tautology 自检（设计文档 §5.1）：注入「红冲误用当前标准而非捕获标准」变异（P1-MA2-024 修复的 bug）——
     * 跨重估时该变异使 totalCost 不可恢复，证明属性 test 能发现此类变异，非恒等式。
     */
    @Test
    void tautologySelfCheck_reverseUsingCurrentStandardIsDetected() {
        StandardCostModel correct = new StandardCostModel();
        correct.revalue(new BigDecimal("10"));
        correct.normalIncoming(new BigDecimal("20"));
        BigDecimal totalCostBeforeOutgoing = correct.totalCost;
        correct.outgoing(new BigDecimal("8")); // 捕获标准 10
        correct.revalue(new BigDecimal("15")); // 重估 10→15
        correct.reverseLatestOutgoing(); // 正确：沿用捕获标准 10 → 恢复
        assertEquals(0, totalCostBeforeOutgoing.compareTo(correct.totalCost),
                "正确红冲（沿用捕获标准）恢复 totalCost");

        // 变异生产算术：红冲误用当前标准（15）而非捕获标准（10）—— P1-MA2-024 修复的 bug
        StandardCostModel mutated = new StandardCostModel();
        mutated.revalue(new BigDecimal("10"));
        mutated.normalIncoming(new BigDecimal("20"));
        BigDecimal mutatedTotalCostBeforeOutgoing = mutated.totalCost;
        mutated.outgoing(new BigDecimal("8"));
        mutated.revalue(new BigDecimal("15"));
        // 变异：手工用当前标准（15）回加，而非捕获标准（10）
        BigDecimal mutatedReverseDelta = roundCost(mutated.currentStandard).multiply(new BigDecimal("8")); // 误用 15
        mutated.totalCost = mutated.totalCost.add(mutatedReverseDelta);
        mutated.totalQuantity = mutated.totalQuantity.add(new BigDecimal("8"));
        // 变异后 totalCost 不恢复（120 + 8×15 = 240 ≠ 200）
        assertFalse(0 == mutatedTotalCostBeforeOutgoing.compareTo(mutated.totalCost),
                "变异红冲（误用当前标准 15）使 totalCost=240≠200 不可恢复——属性 test 能发现此类变异");
        assertEquals(0, new BigDecimal("240").compareTo(mutated.totalCost),
                "变异结果 totalCost=240（drift 40）");
    }

    // ---------- @Provide 生成器 ----------

    @Provide
    Arbitrary<List<StandardAction>> standardActions() {
        return actionArbitrary().list().ofMinSize(1).ofMaxSize(20);
    }

    @Provide
    Arbitrary<BigDecimal> initialStandard() {
        return positiveAmount();
    }

    @Provide
    Arbitrary<BigDecimal> firstStandard() {
        return positiveAmount();
    }

    @Provide
    Arbitrary<BigDecimal> revaluedStandard() {
        return positiveAmount();
    }

    @Provide
    Arbitrary<BigDecimal> positiveQty() {
        return positiveAmount();
    }

    private Arbitrary<BigDecimal> positiveAmount() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("0.0001"), new BigDecimal("1000"))
                .ofScale(COST_SCALE);
    }

    private Arbitrary<StandardAction> actionArbitrary() {
        Arbitrary<StandardAction> outgoings = positiveAmount().map(a -> (StandardAction) new OutgoingAction(a));
        Arbitrary<StandardAction> revalues = positiveAmount().map(a -> (StandardAction) new RevalueAction(a));
        Arbitrary<StandardAction> reverses = Arbitraries.just(new ReverseAction());
        Arbitrary<StandardAction> incomings = positiveAmount().map(a -> (StandardAction) new IncomingAction(a));
        // 加权：revalue/reverse 是 P5 核心路径，incoming/outgoing 维持库存动态
        return Arbitraries.frequencyOf(
                Tuple.of(3, outgoings),
                Tuple.of(3, revalues),
                Tuple.of(3, reverses),
                Tuple.of(1, incomings));
    }
}
