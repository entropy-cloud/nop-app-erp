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
 * <p>针对 {@link ErpCtRebateAgreementStateMachine} Bean 的完备性遍历：
 * 不经 BizModel 入口（层 3 职责），不断言副作用。覆盖：
 * <ul>
 *   <li>(a) {@code transitions()} = {DRAFT→ACTIVE}（ct-026-r3 修复后命名动作 activate 边）；</li>
 *   <li>(b) {@code isActive} 分类正确（ACTIVE=true，DRAFT/EXPIRED/SETTLED=false）；</li>
 *   <li>(c) 初始态集合 = {DRAFT}；</li>
 *   <li>(d) **断言 EXPIRED/SETTLED 不在 transitions / initial / terminal 任一集合**（预留死状态——
 *       layer-2 裁定登记为 intentional reserved；ACTIVE 已由 ct-026-r3 activate writer 激活，
 *       plan 2026-09-10-1141-2 Phase 1）；</li>
 *   <li>(e) {@code terminalStatuses()} 为空、{@code isTerminal} 对全部状态返回 false（无真正终态）；</li>
 *   <li>(f) {@code assertCanActivate} 守卫：仅 DRAFT 放行。</li>
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

    /** layer-2 裁定的预留死状态（intentional reserved；ct-026-r3 后 ACTIVE 已可达，出列）。 */
    private static final List<String> DEAD_STATUSES = Arrays.asList(
            ErpCtConstants.REBATE_AGREEMENT_STATUS_EXPIRED,
            ErpCtConstants.REBATE_AGREEMENT_STATUS_SETTLED);

    private final ErpCtRebateAgreementStateMachine sm = new ErpCtRebateAgreementStateMachine();

    // ---------- (a) transitions() = {DRAFT→ACTIVE}（ct-026-r3 命名动作 activate） ----------

    @Test
    public void testTransitionsContainActivateEdge() {
        List<ErpCtRebateAgreementStateMachine.TransitionDefinition> transitions = sm.transitions();
        assertEquals(1, transitions.size(), "ct-026-r3 后 transitions 应恰含 activate 单边");
        ErpCtRebateAgreementStateMachine.TransitionDefinition edge = transitions.get(0);
        assertEquals("activate", edge.getAction());
        assertEquals(ErpCtConstants.REBATE_AGREEMENT_STATUS_DRAFT, edge.getFromStatus());
        assertEquals(ErpCtConstants.REBATE_AGREEMENT_STATUS_ACTIVE, edge.getToStatus());
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

    // ---------- (d) 预留死状态不在 transitions / initial / terminal 任一集合 ----------

    @Test
    public void testDeadStatusesNotInAnySet() {
        List<String> initial = sm.initialStatuses();
        List<String> terminal = sm.terminalStatuses();
        for (String dead : DEAD_STATUSES) {
            assertFalse(initial.contains(dead),
                    "预留死状态不应在 initialStatuses: " + dead);
            assertFalse(terminal.contains(dead),
                    "预留死状态不应在 terminalStatuses: " + dead);
            assertEquals(0, sm.transitions().stream()
                            .filter(t -> dead.equals(t.getFromStatus()) || dead.equals(t.getToStatus()))
                            .count(),
                    "预留死状态不应出现在 transitions 任一边: " + dead);
        }
    }

    // ---------- (e) terminalStatuses 空 + isTerminal 对全部状态返回 false ----------

    @Test
    public void testNoTerminalStatusesForDegenerateAxis() {
        assertEquals(Collections.emptyList(), sm.terminalStatuses(),
                "无终态：EXPIRED/SETTLED 为预留死状态，非真正终态");
        for (String s : ALL_STATUSES) {
            assertFalse(sm.isTerminal(s), "所有状态均非终态: " + s);
        }
    }

    // ---------- (f) assertCanActivate 守卫（ct-026-r3） ----------

    @Test
    public void testAssertCanActivateGuard() {
        sm.assertCanActivate(ErpCtConstants.REBATE_AGREEMENT_STATUS_DRAFT);
        for (String s : Arrays.asList(
                ErpCtConstants.REBATE_AGREEMENT_STATUS_ACTIVE,
                ErpCtConstants.REBATE_AGREEMENT_STATUS_EXPIRED,
                ErpCtConstants.REBATE_AGREEMENT_STATUS_SETTLED,
                null)) {
            org.junit.jupiter.api.Assertions.assertThrows(io.nop.api.core.exceptions.NopException.class,
                    () -> sm.assertCanActivate(s), "非 DRAFT 源态激活应拒绝: " + s);
        }
    }
}
