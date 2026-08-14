package app.erp.ct.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.ct.service.ErpCtConstants;
import io.nop.api.core.exceptions.NopException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-14-2000-1 Phase 1 Proof，M4.65）。
 *
 * <p>针对 {@link ErpCtRebateSettlementStateMachine} Bean 的纯矩阵完备性遍历：不经 BizModel 入口（层 3 职责），
 * 不断言副作用/审计。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边（1 实现边 postSettlement DRAFT→POSTED）；</li>
 *   <li>(b) 从 DRAFT 命名动作可达 POSTED；</li>
 *   <li>(c) postSettlement 仅 DRAFT 合法、对 POSTED/CANCELLED 非法（common 码携带 action/currentStatus）；</li>
 *   <li>(d) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(e) 终态/初始态集合正确（终态 = {POSTED}；初始 = {DRAFT}）；</li>
 *   <li>(f) CANCELLED 不在 initialStatuses/terminalStatuses/transitions 任一集合（intentional reserved 死状态）。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpCtRebateSettlementStateMachineMatrix {

    /** dict {@code erp-ct/settlement-status} 的 3 值。 */
    private static final List<String> ALL_STATUSES = Arrays.asList(
            ErpCtConstants.SETTLEMENT_STATUS_DRAFT,
            ErpCtConstants.SETTLEMENT_STATUS_POSTED,
            ErpCtConstants.SETTLEMENT_STATUS_CANCELLED);

    private final ErpCtRebateSettlementStateMachine sm = new ErpCtRebateSettlementStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpCtRebateSettlementStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpCtRebateSettlementStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(1, edges.size(), "迁移矩阵应有 1 条实现边（postSettlement DRAFT→POSTED）");
    }

    // ---------- (b) 从 DRAFT 命名动作可达 POSTED；CANCELLED 不可达 ----------

    @Test
    public void testPostSettlementReachabilityFromDraft() {
        Set<String> reachable = reachableFrom(ErpCtConstants.SETTLEMENT_STATUS_DRAFT);
        assertTrue(reachable.contains(ErpCtConstants.SETTLEMENT_STATUS_POSTED), "DRAFT→POSTED 经 postSettlement 可达");
        assertEquals(1, reachable.size(), "从 DRAFT 经命名动作仅可达到 POSTED");
    }

    @Test
    public void testTerminalStatusHasNoOutgoingEdges() {
        for (String terminal : sm.terminalStatuses()) {
            for (ErpCtRebateSettlementStateMachine.TransitionDefinition e : sm.transitions()) {
                assertFalse(e.getFromStatus().equals(terminal),
                        "终态不应有出边: terminal=" + terminal + ", but edge " + e.getAction() + " leaves it");
            }
        }
    }

    // ---------- (c) postSettlement 合法+非法来源态 ----------

    @Test
    public void testPostSettlementLegalOnlyForDraft() {
        // 仅 DRAFT 合法
        sm.assertCanPostSettlement(ErpCtConstants.SETTLEMENT_STATUS_DRAFT);
        assertEquals(ErpCtConstants.SETTLEMENT_STATUS_POSTED, sm.postSettlementTargetStatus());

        for (String s : ALL_STATUSES) {
            if (s.equals(ErpCtConstants.SETTLEMENT_STATUS_DRAFT)) {
                continue;
            }
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanPostSettlement(s),
                    "postSettlement 对非 DRAFT 应非法: " + s);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                    "Bean 报告 common 层非法迁移码");
            assertEquals("postSettlement", ex.getParam(ErpCtRebateSettlementStateMachine.ARG_ACTION),
                    "拒绝元数据携带动作名");
            assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS), "拒绝元数据携带当前态");
        }
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpCtRebateSettlementStateMachine.TransitionDefinition e : sm.transitions()) {
            invokeAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (e) 终态/初始态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpCtConstants.SETTLEMENT_STATUS_POSTED), sm.terminalStatuses(),
                "终态集合 = {POSTED}");
        assertEquals(Arrays.asList(ErpCtConstants.SETTLEMENT_STATUS_DRAFT), sm.initialStatuses(),
                "初始态集合 = {DRAFT}");

        assertTrue(sm.isTerminal(ErpCtConstants.SETTLEMENT_STATUS_POSTED));
        assertFalse(sm.isTerminal(ErpCtConstants.SETTLEMENT_STATUS_DRAFT));
        assertFalse(sm.isTerminal(ErpCtConstants.SETTLEMENT_STATUS_CANCELLED));
    }

    // ---------- (f) CANCELLED 不在任一集合（intentional reserved 死状态） ----------

    @Test
    public void testCancelledNotInAnySet() {
        assertFalse(sm.initialStatuses().contains(ErpCtConstants.SETTLEMENT_STATUS_CANCELLED),
                "CANCELLED 不在初始态集合");
        assertFalse(sm.terminalStatuses().contains(ErpCtConstants.SETTLEMENT_STATUS_CANCELLED),
                "CANCELLED 不在终态集合（非真正终态，仅预留语义入口）");
        for (ErpCtRebateSettlementStateMachine.TransitionDefinition e : sm.transitions()) {
            assertFalse(e.getFromStatus().equals(ErpCtConstants.SETTLEMENT_STATUS_CANCELLED),
                    "CANCELLED 不应有出边");
            assertFalse(e.getToStatus().equals(ErpCtConstants.SETTLEMENT_STATUS_CANCELLED),
                    "CANCELLED 不应有入边");
        }
        // 且 CANCELLED 在 dict 中（保留为预留语义入口，不删除）
        assertTrue(ALL_STATUSES.contains(ErpCtConstants.SETTLEMENT_STATUS_CANCELLED),
                "dict 值 CANCELLED 保留（intentional reserved，对齐先例）");
    }

    // ---------- helpers ----------

    private void invokeAssert(String action, String status) {
        switch (action) {
            case "postSettlement":
                sm.assertCanPostSettlement(status);
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "postSettlement":
                return sm.postSettlementTargetStatus();
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private Set<String> reachableFrom(String start) {
        Set<String> visited = new LinkedHashSet<>();
        List<String> frontier = new ArrayList<>();
        frontier.add(start);
        while (!frontier.isEmpty()) {
            String cur = frontier.remove(0);
            if (!visited.add(cur)) {
                continue;
            }
            for (ErpCtRebateSettlementStateMachine.TransitionDefinition e : sm.transitions()) {
                if (e.getFromStatus().equals(cur) && !visited.contains(e.getToStatus())) {
                    frontier.add(e.getToStatus());
                }
            }
        }
        return visited.stream()
                .filter(s -> !s.equals(start))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
