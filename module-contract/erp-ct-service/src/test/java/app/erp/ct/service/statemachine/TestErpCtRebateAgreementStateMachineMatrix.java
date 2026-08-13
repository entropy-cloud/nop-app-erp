package app.erp.ct.service.statemachine;

import app.erp.ct.service.ErpCtConstants;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-13-1430-3 Phase 1 Proof）。
 *
 * <p>针对 {@link ErpCtRebateAgreementStateMachine} Bean（**退化分类 Bean**）的退化解完备性遍历：
 * 不经 BizModel 入口（层 3 职责），不断言副作用。覆盖：
 * <ul>
 *   <li>(a) {@code transitions()} 为**空**（退化轴——零命名动作迁移 writer）；</li>
 *   <li>(b) {@code isActive} 分类正确（ACTIVE=true，DRAFT/EXPIRED/SETTLED=false）；</li>
 *   <li>(c) 初始态集合 = {DRAFT}；</li>
 *   <li>(d) **断言 ACTIVE/EXPIRED/SETTLED 不在 transitions / initial / terminal 任一集合**（死状态——
 *       layer-2 裁定登记为 intentional reserved，对齐 Contract CANCELLED/NEGOTIATION + hr SUSPENDED 先例）；</li>
 *   <li>(e) {@code terminalStatuses()} 为空、{@code isTerminal} 对全部状态（含三死状态）返回 false（退化轴无终态）。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpCtRebateAgreementStateMachineMatrix {

    /** dict {@code erp-ct/rebate-agreement-status} 的 4 值。 */
    private static final List<String> ALL_STATUSES = Arrays.asList(
            ErpCtConstants.REBATE_AGREEMENT_STATUS_DRAFT,
            ErpCtConstants.REBATE_AGREEMENT_STATUS_ACTIVE,
            ErpCtConstants.REBATE_AGREEMENT_STATUS_EXPIRED,
            ErpCtConstants.REBATE_AGREEMENT_STATUS_SETTLED);

    /** layer-2 裁定的 3 个死状态（intentional reserved）。 */
    private static final List<String> DEAD_STATUSES = Arrays.asList(
            ErpCtConstants.REBATE_AGREEMENT_STATUS_ACTIVE,
            ErpCtConstants.REBATE_AGREEMENT_STATUS_EXPIRED,
            ErpCtConstants.REBATE_AGREEMENT_STATUS_SETTLED);

    private final ErpCtRebateAgreementStateMachine sm = new ErpCtRebateAgreementStateMachine();

    // ---------- (a) transitions() 为空（退化轴） ----------

    @Test
    public void testTransitionsEmptyForDegenerateAxis() {
        assertEquals(Collections.emptyList(), sm.transitions(),
                "退化轴：零命名动作迁移 writer，transitions() 应为空");
    }

    // ---------- (b) isActive 分类（accrual 只读守卫集中化） ----------

    @Test
    public void testIsActiveClassification() {
        assertTrue(sm.isActive(ErpCtConstants.REBATE_AGREEMENT_STATUS_ACTIVE), "ACTIVE 协议可计提");
        assertFalse(sm.isActive(ErpCtConstants.REBATE_AGREEMENT_STATUS_DRAFT), "DRAFT 不可计提");
        assertFalse(sm.isActive(ErpCtConstants.REBATE_AGREEMENT_STATUS_EXPIRED), "EXPIRED 不可计提");
        assertFalse(sm.isActive(ErpCtConstants.REBATE_AGREEMENT_STATUS_SETTLED), "SETTLED 不可计提");
        assertFalse(sm.isActive(null), "null 不可计提");
    }

    // ---------- (c) 初始态集合 = {DRAFT} ----------

    @Test
    public void testInitialStatuses() {
        assertEquals(Collections.singletonList(ErpCtConstants.REBATE_AGREEMENT_STATUS_DRAFT),
                sm.initialStatuses(), "初始态集合 = {DRAFT}（新建 seed 经 CRUD 创建写入）");
    }

    // ---------- (d) 死状态不在 transitions / initial / terminal 任一集合 ----------

    @Test
    public void testDeadStatusesNotInAnySet() {
        // transitions 已空（testTransitionsEmptyForDegenerateAxis 已证），死状态自然不在
        List<String> initial = sm.initialStatuses();
        List<String> terminal = sm.terminalStatuses();
        for (String dead : DEAD_STATUSES) {
            assertFalse(initial.contains(dead),
                    "死状态不应在 initialStatuses: " + dead);
            assertFalse(terminal.contains(dead),
                    "死状态不应在 terminalStatuses: " + dead);
            // transitions 为空，死状态不在任一迁移边
            assertEquals(0, sm.transitions().stream()
                            .filter(t -> dead.equals(t.getFromStatus()) || dead.equals(t.getToStatus()))
                            .count(),
                    "死状态不应出现在 transitions 任一边: " + dead);
        }
    }

    // ---------- (e) terminalStatuses 空 + isTerminal 对全部状态返回 false（退化轴无终态） ----------

    @Test
    public void testNoTerminalStatusesForDegenerateAxis() {
        assertEquals(Collections.emptyList(), sm.terminalStatuses(),
                "退化轴无终态：ACTIVE/EXPIRED/SETTLED 为预留死状态，非真正终态");
        for (String s : ALL_STATUSES) {
            assertFalse(sm.isTerminal(s), "退化轴：所有状态均非终态（含三死状态）: " + s);
        }
    }
}
