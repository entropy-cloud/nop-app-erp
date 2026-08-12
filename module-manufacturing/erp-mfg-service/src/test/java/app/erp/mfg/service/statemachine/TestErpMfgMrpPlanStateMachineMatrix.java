package app.erp.mfg.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.mfg.service.ErpMfgConstants;
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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan Phase 1 Proof）。
 *
 * <p>针对 {@link ErpMfgMrpPlanStateMachine} Bean 的纯矩阵完备性遍历：不经引擎/释放服务入口（层 3 职责），
 * 不断言净需求计算/释放副作用。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边；</li>
 *   <li>(b) 从 DRAFT 可达 RUNNING/COMPLETED/FIRMED；</li>
 *   <li>(c) run（null/DRAFT 合法、RUNNING/COMPLETED/FIRMED/CANCELLED 非法）；</li>
 *   <li>(d) complete（RUNNING 合法、其余非法）；</li>
 *   <li>(e) firm（COMPLETED 合法、其余非法）；</li>
 *   <li>(f) FIRMED 终态无出边；</li>
 *   <li>(g) {@code transitions()} 元数据（3 边）与显式方法语义一致；</li>
 *   <li>(h) 终态/初始态集合正确；</li>
 *   <li>(i) <strong>断言无 COMPLETED→DRAFT 边</strong>（清单漂移，Bean 如实排除）；CANCELLED 不在 transitions（死状态）。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpMfgMrpPlanStateMachineMatrix {

    private static final List<String> ALL_STATUSES = Arrays.asList(
            ErpMfgConstants.MRP_STATUS_DRAFT,
            ErpMfgConstants.MRP_STATUS_RUNNING,
            ErpMfgConstants.MRP_STATUS_COMPLETED,
            ErpMfgConstants.MRP_STATUS_FIRMED,
            ErpMfgConstants.MRP_STATUS_CANCELLED);

    private final ErpMfgMrpPlanStateMachine sm = new ErpMfgMrpPlanStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpMfgMrpPlanStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpMfgMrpPlanStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(3, edges.size(), "迁移矩阵应有 3 条边（run 1 + complete 1 + firm 1）");
    }

    // ---------- (b) 从 DRAFT 可达 RUNNING/COMPLETED/FIRMED ----------

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpMfgConstants.MRP_STATUS_DRAFT);
        assertTrue(reachable.contains(ErpMfgConstants.MRP_STATUS_RUNNING), "从 DRAFT 应可达 RUNNING");
        assertTrue(reachable.contains(ErpMfgConstants.MRP_STATUS_COMPLETED), "从 DRAFT 应可达 COMPLETED");
        assertTrue(reachable.contains(ErpMfgConstants.MRP_STATUS_FIRMED), "从 DRAFT 应可达 FIRMED");
        // CANCELLED 死状态从 DRAFT 不可达
        assertFalse(reachable.contains(ErpMfgConstants.MRP_STATUS_CANCELLED),
                "CANCELLED 为预留死状态，从 DRAFT 不应可达");
    }

    // ---------- (c) run（null/DRAFT 合法、其余非法） ----------

    @Test
    public void testRunLegalForNullAndDraft() {
        // null（新建实体未初始化）合法
        sm.assertCanRun(null);
        // DRAFT 合法
        sm.assertCanRun(ErpMfgConstants.MRP_STATUS_DRAFT);
        assertEquals(ErpMfgConstants.MRP_STATUS_RUNNING, sm.runTargetStatus());
        for (String s : illegalFor(ALL_STATUSES, ErpMfgConstants.MRP_STATUS_DRAFT)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanRun(s),
                    "run 对非 null/DRAFT 应非法: " + s);
            assertCommonTransitionMetadata(ex, "run", s);
        }
    }

    // ---------- (d) complete（RUNNING 合法、其余非法） ----------

    @Test
    public void testCompleteLegalForRunning() {
        sm.assertCanComplete(ErpMfgConstants.MRP_STATUS_RUNNING);
        assertEquals(ErpMfgConstants.MRP_STATUS_COMPLETED, sm.completeTargetStatus());
        for (String s : illegalFor(ALL_STATUSES, ErpMfgConstants.MRP_STATUS_RUNNING)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanComplete(s),
                    "complete 对非 RUNNING 应非法: " + s);
            assertCommonTransitionMetadata(ex, "complete", s);
        }
    }

    // ---------- (e) firm（COMPLETED 合法、其余非法） ----------

    @Test
    public void testFirmLegalForCompleted() {
        sm.assertCanFirm(ErpMfgConstants.MRP_STATUS_COMPLETED);
        assertEquals(ErpMfgConstants.MRP_STATUS_FIRMED, sm.firmTargetStatus());
        for (String s : illegalFor(ALL_STATUSES, ErpMfgConstants.MRP_STATUS_COMPLETED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanFirm(s),
                    "firm 对非 COMPLETED 应非法: " + s);
            assertCommonTransitionMetadata(ex, "firm", s);
        }
    }

    // ---------- (f) FIRMED 终态无出边 ----------

    @Test
    public void testTerminalStatusHasNoOutgoingEdges() {
        for (String terminal : sm.terminalStatuses()) {
            for (ErpMfgMrpPlanStateMachine.TransitionDefinition e : sm.transitions()) {
                assertFalse(e.getFromStatus().equals(terminal),
                        "终态不应有出边: terminal=" + terminal + ", but edge " + e.getAction() + " leaves it");
            }
        }
    }

    // ---------- (g) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpMfgMrpPlanStateMachine.TransitionDefinition e : sm.transitions()) {
            invokeAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (h) 终态/初始态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(java.util.Collections.singletonList(ErpMfgConstants.MRP_STATUS_FIRMED),
                sm.terminalStatuses(), "终态集合 = {FIRMED}");
        assertEquals(java.util.Collections.singletonList(ErpMfgConstants.MRP_STATUS_DRAFT),
                sm.initialStatuses(), "初始态集合 = {DRAFT}");

        assertTrue(sm.isTerminal(ErpMfgConstants.MRP_STATUS_FIRMED));
        assertFalse(sm.isTerminal(ErpMfgConstants.MRP_STATUS_DRAFT));
        assertFalse(sm.isTerminal(ErpMfgConstants.MRP_STATUS_RUNNING));
        assertFalse(sm.isTerminal(ErpMfgConstants.MRP_STATUS_COMPLETED));
        assertFalse(sm.isTerminal(ErpMfgConstants.MRP_STATUS_CANCELLED));
    }

    // ---------- (i) 无 COMPLETED→DRAFT 边 + CANCELLED 不在 transitions（死状态/清单漂移） ----------

    @Test
    public void testNoCompletedToDraftRevertAndCancelledDeadState() {
        // 断言无 COMPLETED→DRAFT revert 边（M0.2 清单漂移，Bean 不编码此不存在边）
        for (ErpMfgMrpPlanStateMachine.TransitionDefinition e : sm.transitions()) {
            assertFalse(ErpMfgConstants.MRP_STATUS_COMPLETED.equals(e.getFromStatus())
                            && ErpMfgConstants.MRP_STATUS_DRAFT.equals(e.getToStatus()),
                    "不应存在 COMPLETED→DRAFT revert 边（清单漂移；实仓守卫要求 DRAFT，COMPLETED 不可 revert）");
        }
        // CANCELLED 不在 transitions（死状态）
        for (ErpMfgMrpPlanStateMachine.TransitionDefinition e : sm.transitions()) {
            assertFalse(ErpMfgConstants.MRP_STATUS_CANCELLED.equals(e.getFromStatus()),
                    "CANCELLED 不应是任何边的来源: edge=" + e.getAction());
            assertFalse(ErpMfgConstants.MRP_STATUS_CANCELLED.equals(e.getToStatus()),
                    "CANCELLED 不应是任何边的目标: edge=" + e.getAction());
        }
        // CANCELLED 不在终态集（预留死状态，不可达）
        assertFalse(sm.isTerminal(ErpMfgConstants.MRP_STATUS_CANCELLED),
                "CANCELLED 不入终态集（预留死状态）");
        assertFalse(sm.terminalStatuses().contains(ErpMfgConstants.MRP_STATUS_CANCELLED),
                "terminalStatuses() 不应包含 CANCELLED");
        // CANCELLED 对全部 3 动作均非法
        assertThrows(NopException.class, () -> sm.assertCanRun(ErpMfgConstants.MRP_STATUS_CANCELLED),
                "run 对 CANCELLED 应非法");
        assertThrows(NopException.class, () -> sm.assertCanComplete(ErpMfgConstants.MRP_STATUS_CANCELLED),
                "complete 对 CANCELLED 应非法");
        assertThrows(NopException.class, () -> sm.assertCanFirm(ErpMfgConstants.MRP_STATUS_CANCELLED),
                "firm 对 CANCELLED 应非法");
    }

    // ---------- helpers ----------

    private void invokeAssert(String action, String status) {
        switch (action) {
            case "run": sm.assertCanRun(status); break;
            case "complete": sm.assertCanComplete(status); break;
            case "firm": sm.assertCanFirm(status); break;
            default: throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "run": return sm.runTargetStatus();
            case "complete": return sm.completeTargetStatus();
            case "firm": return sm.firmTargetStatus();
            default: throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private void assertCommonTransitionMetadata(NopException ex, String action, String status) {
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + status);
        assertEquals(action, ex.getParam(ErpMfgMrpPlanStateMachine.ARG_ACTION), "拒绝元数据携带动作名");
        assertEquals(status, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS), "拒绝元数据携带当前态");
    }

    private List<String> illegalFor(List<String> all, String... legal) {
        Set<String> legalSet = new HashSet<>(Arrays.asList(legal));
        return all.stream().filter(s -> !legalSet.contains(s)).collect(Collectors.toList());
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
            for (ErpMfgMrpPlanStateMachine.TransitionDefinition e : sm.transitions()) {
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
