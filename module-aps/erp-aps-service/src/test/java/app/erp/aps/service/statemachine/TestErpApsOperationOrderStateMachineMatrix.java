package app.erp.aps.service.statemachine;

import app.erp.aps.service.ErpApsConstants;
import app.erp.common.service.ErpCommonErrors;
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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-12-2142-3 Phase 1 Proof）。
 *
 * <p>针对 {@link ErpApsOperationOrderStateMachine} Bean 的纯矩阵完备性遍历：不经 BizModel 入口（层 3 职责），
 * 不断言副作用/审计。覆盖（RC-R1.87/RC-R1.88 后 13 条边 = 基线 7 + 引擎边 3 + hold/unhold 命名边 3）：
 * <ul>
 *   <li>(a) 无重复/冲突边（13 条边，cancel 多源占 3 + unhold 多源占 2）；</li>
 *   <li>(b) 从 DRAFT 经声明边可达全部状态（DRAFT→PLANNED→IN_PROGRESS→FINISHED；
 *       DRAFT/PLANNED/IN_PROGRESS→CANCELLED；PLANNED→DRAFT 回退环）；</li>
 *   <li>(c) cancel 三源 {DRAFT,PLANNED,IN_PROGRESS} 全覆盖、对终态 FINISHED/CANCELLED 非法；</li>
 *   <li>(d) {@code transitions()} 元数据（7 条边）与显式方法 + owner doc §2 一致；
 *       <strong>注</strong>：「schedule」边（DRAFT→PLANNED）无 assertCan 守卫（引擎边界裁定，
 *       引擎按可行性写状态无可集中守卫），仅作可达性分析声明边，不参与 assertCan 一致性核对；</li>
 *   <li>(e) 终态/初始态集合正确（终态 = {FINISHED, CANCELLED}；初始态 = {DRAFT}）。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpApsOperationOrderStateMachineMatrix {

    /** dict {@code erp-aps/operation-order-status} 的 8 值（RC-R1.87/88 增 UNSCHEDULABLE/HOLD/ON_HOLD）。 */
    private static final List<String> ALL_STATUSES = Arrays.asList(
            ErpApsConstants.OP_STATUS_DRAFT,
            ErpApsConstants.OP_STATUS_PLANNED,
            ErpApsConstants.OP_STATUS_IN_PROGRESS,
            ErpApsConstants.OP_STATUS_FINISHED,
            ErpApsConstants.OP_STATUS_CANCELLED,
            ErpApsConstants.OP_STATUS_UNSCHEDULABLE,
            ErpApsConstants.OP_STATUS_HOLD,
            ErpApsConstants.OP_STATUS_ON_HOLD);

    /** 引擎驱动的边（无 assertCan 守卫，仅可达性声明）：排产/自愈重排/不可排产标记/缺料暂停。 */
    private static final java.util.Set<String> ENGINE_ACTIONS = java.util.Set.of(
            "schedule", "markUnschedulable", "shortageHold");

    private final ErpApsOperationOrderStateMachine sm = new ErpApsOperationOrderStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpApsOperationOrderStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpApsOperationOrderStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            // 同一 action + 同一 fromStatus 不得出现多次（否则冲突）；cancel 多源 = 不同 fromStatus，合法
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(13, edges.size(), "迁移矩阵应有 13 条边（基线 7 + 引擎边 3 + hold/unhold 命名边 3）");
    }

    // ---------- (b) 从 DRAFT 经声明边可达全部状态 ----------

    @Test
    public void testReachabilityFromDraftCoversAllStatuses() {
        Set<String> reachable = reachableFrom(ErpApsConstants.OP_STATUS_DRAFT);
        // 从 DRAFT 经声明边可达全部状态（含 PLANNED→DRAFT 回退环）
        for (String s : ALL_STATUSES) {
            if (s.equals(ErpApsConstants.OP_STATUS_DRAFT)) {
                continue;
            }
            assertTrue(reachable.contains(s), "从 DRAFT 经声明边应可达: " + s + ", reachable=" + reachable);
        }
        // PLANNED→DRAFT 回退环可达（重排场景）
        assertTrue(reachable.contains(ErpApsConstants.OP_STATUS_PLANNED), "DRAFT→PLANNED 经 schedule 边可达");
        assertTrue(reachable.contains(ErpApsConstants.OP_STATUS_DRAFT)
                || reachableFrom(ErpApsConstants.OP_STATUS_PLANNED).contains(ErpApsConstants.OP_STATUS_DRAFT),
                "PLANNED→DRAFT 回退环可达");
    }

    @Test
    public void testTerminalStatusesHaveNoOutgoingEdges() {
        for (String terminal : sm.terminalStatuses()) {
            for (ErpApsOperationOrderStateMachine.TransitionDefinition e : sm.transitions()) {
                assertFalse(e.getFromStatus().equals(terminal),
                        "终态不应有出边: terminal=" + terminal + ", but edge " + e.getAction() + " leaves it");
            }
        }
    }

    // ---------- (c) cancel 三源全覆盖、对终态非法 ----------

    @Test
    public void testCancelLegalForThreeSourcesAndIllegalForTerminals() {
        // 合法来源态（三源）
        sm.assertCanCancel(ErpApsConstants.OP_STATUS_DRAFT);
        sm.assertCanCancel(ErpApsConstants.OP_STATUS_PLANNED);
        sm.assertCanCancel(ErpApsConstants.OP_STATUS_IN_PROGRESS);
        assertEquals(ErpApsConstants.OP_STATUS_CANCELLED, sm.cancelTargetStatus());

        // 终态非法
        for (String s : ALL_STATUSES) {
            if (s.equals(ErpApsConstants.OP_STATUS_DRAFT)
                    || s.equals(ErpApsConstants.OP_STATUS_PLANNED)
                    || s.equals(ErpApsConstants.OP_STATUS_IN_PROGRESS)) {
                continue;
            }
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanCancel(s),
                    "cancel 对终态应非法: " + s);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                    "Bean 报告 common 层非法迁移码");
            assertEquals("cancel", ex.getParam(ErpApsOperationOrderStateMachine.ARG_ACTION), "拒绝元数据携带动作名");
            assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS), "拒绝元数据携带当前态");
        }
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpApsOperationOrderStateMachine.TransitionDefinition e : sm.transitions()) {
            // 引擎边（schedule/markUnschedulable/shortageHold）无 assertCan 守卫，仅作可达性声明，
            // 跳过 assertCan 一致性核对（引擎边界裁定，见类注释）。
            if (ENGINE_ACTIONS.contains(e.getAction())) {
                continue;
            }
            // 每条边的 fromStatus 对该 action 合法（assert 放行不抛）
            invokeAssert(e.getAction(), e.getFromStatus());
            // 每条边的 toStatus 与 <action>TargetStatus() 一致
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (e) 终态/初始态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpApsConstants.OP_STATUS_FINISHED, ErpApsConstants.OP_STATUS_CANCELLED),
                sm.terminalStatuses(), "终态集合 = {FINISHED, CANCELLED}");
        assertEquals(Arrays.asList(ErpApsConstants.OP_STATUS_DRAFT), sm.initialStatuses(), "初始态集合 = {DRAFT}");

        assertTrue(sm.isTerminal(ErpApsConstants.OP_STATUS_FINISHED));
        assertTrue(sm.isTerminal(ErpApsConstants.OP_STATUS_CANCELLED));
        assertFalse(sm.isTerminal(ErpApsConstants.OP_STATUS_DRAFT));
        assertFalse(sm.isTerminal(ErpApsConstants.OP_STATUS_PLANNED));
        assertFalse(sm.isTerminal(ErpApsConstants.OP_STATUS_IN_PROGRESS));
    }

    // ---------- 合法/非法来源态显式断言（补充显式方法语义核对） ----------

    @Test
    public void testExplicitActionGuards() {
        // start: 仅 PLANNED 合法
        assertActionAllowsOnly("start", ErpApsConstants.OP_STATUS_PLANNED);
        // complete: 仅 IN_PROGRESS 合法
        assertActionAllowsOnly("complete", ErpApsConstants.OP_STATUS_IN_PROGRESS);
        // revertToDraft: 仅 PLANNED 合法（插单回退路径矩阵权威）
        assertActionAllowsOnly("revertToDraft", ErpApsConstants.OP_STATUS_PLANNED);
        // hold: 仅 PLANNED 合法（RC-R1.88 派工保持）
        assertActionAllowsOnly("hold", ErpApsConstants.OP_STATUS_PLANNED);
        // unhold: HOLD/ON_HOLD 合法（解除保持双源，RC-R1.88）
        assertActionAllowsOnly("unhold", ErpApsConstants.OP_STATUS_HOLD, ErpApsConstants.OP_STATUS_ON_HOLD);
    }

    @Test
    public void testTargetStatusMethods() {
        assertEquals(ErpApsConstants.OP_STATUS_IN_PROGRESS, sm.startTargetStatus());
        assertEquals(ErpApsConstants.OP_STATUS_FINISHED, sm.completeTargetStatus());
        assertEquals(ErpApsConstants.OP_STATUS_CANCELLED, sm.cancelTargetStatus());
        assertEquals(ErpApsConstants.OP_STATUS_DRAFT, sm.revertToDraftTargetStatus());
        assertEquals(ErpApsConstants.OP_STATUS_HOLD, sm.holdTargetStatus());
        assertEquals(ErpApsConstants.OP_STATUS_PLANNED, sm.unholdTargetStatus());
    }

    @Test
    public void testCancelRejectsAllTerminalStatuses() {
        // 终态全动作拒绝（start/complete/cancel/revertToDraft 对终态均非法）
        for (String terminal : sm.terminalStatuses()) {
            assertThrows(NopException.class, () -> sm.assertCanStart(terminal),
                    "start 对终态应非法: " + terminal);
            assertThrows(NopException.class, () -> sm.assertCanComplete(terminal),
                    "complete 对终态应非法: " + terminal);
            assertThrows(NopException.class, () -> sm.assertCanCancel(terminal),
                    "cancel 对终态应非法: " + terminal);
            assertThrows(NopException.class, () -> sm.assertCanRevertToDraft(terminal),
                    "revertToDraft 对终态应非法: " + terminal);
        }
    }

    // ---------- helpers ----------

    /**
     * 断言某 action 仅允许指定来源态：该来源态放行（不抛），其余全部状态非法（抛 common 码 + action 元数据）。
     */
    private void assertActionAllowsOnly(String action, String... allowedFrom) {
        java.util.Set<String> allowed = new java.util.HashSet<>(Arrays.asList(allowedFrom));
        for (String s : ALL_STATUSES) {
            if (allowed.contains(s)) {
                invokeAssert(action, s); // 不抛 = 合法
            } else {
                NopException ex = assertThrows(NopException.class, () -> invokeAssert(action, s),
                        action + " 对非允许来源态应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                        "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + s);
                assertEquals(action, ex.getParam(ErpApsOperationOrderStateMachine.ARG_ACTION),
                        "拒绝元数据携带动作名: action=" + action);
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                        "拒绝元数据携带当前态: action=" + action + ", status=" + s);
            }
        }
    }

    private void invokeAssert(String action, String status) {
        switch (action) {
            case "start":
                sm.assertCanStart(status);
                break;
            case "complete":
                sm.assertCanComplete(status);
                break;
            case "cancel":
                sm.assertCanCancel(status);
                break;
            case "revertToDraft":
                sm.assertCanRevertToDraft(status);
                break;
            case "hold":
                sm.assertCanHold(status);
                break;
            case "unhold":
                sm.assertCanUnhold(status);
                break;
            default:
                throw new IllegalArgumentException("unknown action (engine actions have no guard): " + action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "start":
                return sm.startTargetStatus();
            case "complete":
                return sm.completeTargetStatus();
            case "cancel":
                return sm.cancelTargetStatus();
            case "revertToDraft":
                return sm.revertToDraftTargetStatus();
            case "hold":
                return sm.holdTargetStatus();
            case "unhold":
                return sm.unholdTargetStatus();
            default:
                throw new IllegalArgumentException("unknown action (engine actions have no target method): " + action);
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
            for (ErpApsOperationOrderStateMachine.TransitionDefinition e : sm.transitions()) {
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
