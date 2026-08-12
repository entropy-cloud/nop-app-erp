package app.erp.prj.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.prj.service.ErpPrjConstants;
import io.nop.api.core.exceptions.NopException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
 * <p>针对 {@link ErpPrjTaskStateMachine} Bean 的纯矩阵完备性遍历：不经 BizModel 入口（层 3 职责），
 * 不断言副作用/审计/DAG 校验。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边；</li>
 *   <li>(b) 从 TODO 可达全部 3 非初始态，DONE 终态无出边；</li>
 *   <li>(c) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(d) 终态/初始态集合正确；</li>
 *   <li>(e) 各动作合法/非法来源态显式断言（含 IN_PROGRESS↔BLOCKED 往复）。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpPrjTaskStateMachineMatrix {

    private static final List<String> ALL_STATUSES = Arrays.asList(
            ErpPrjConstants.TASK_STATUS_TODO,
            ErpPrjConstants.TASK_STATUS_IN_PROGRESS,
            ErpPrjConstants.TASK_STATUS_DONE,
            ErpPrjConstants.TASK_STATUS_BLOCKED);

    private final ErpPrjTaskStateMachine sm = new ErpPrjTaskStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpPrjTaskStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpPrjTaskStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            // 同一 action + 同一 fromStatus 不得出现多次（否则冲突）
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(4, edges.size(), "迁移矩阵应有 4 条边");
    }

    // ---------- (b) 从 TODO 可达全部 3 非初始态；终态无出边 ----------

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpPrjConstants.TASK_STATUS_TODO);
        for (String s : ALL_STATUSES) {
            if (ErpPrjConstants.TASK_STATUS_TODO.equals(s)) {
                continue;
            }
            assertTrue(reachable.contains(s), "从 TODO 应可达状态: " + s);
        }
    }

    @Test
    public void testTerminalStatusesHaveNoOutgoingEdges() {
        for (String terminal : sm.terminalStatuses()) {
            for (ErpPrjTaskStateMachine.TransitionDefinition e : sm.transitions()) {
                assertFalse(e.getFromStatus().equals(terminal),
                        "终态不应有出边: terminal=" + terminal + ", but edge " + e.getAction() + " leaves it");
            }
        }
    }

    // ---------- (c) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpPrjTaskStateMachine.TransitionDefinition e : sm.transitions()) {
            // 每条边的 fromStatus 对该 action 合法（assert 放行不抛）
            invokeAssert(e.getAction(), e.getFromStatus());
            // 每条边的 toStatus 与 <action>TargetStatus() 一致
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (d) 终态/初始态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Collections.singletonList(ErpPrjConstants.TASK_STATUS_DONE), sm.terminalStatuses(),
                "终态集合 = {DONE}");
        assertEquals(Collections.singletonList(ErpPrjConstants.TASK_STATUS_TODO), sm.initialStatuses(),
                "初始态集合 = {TODO}");

        assertTrue(sm.isTerminal(ErpPrjConstants.TASK_STATUS_DONE));
        assertFalse(sm.isTerminal(ErpPrjConstants.TASK_STATUS_TODO));
        assertFalse(sm.isTerminal(ErpPrjConstants.TASK_STATUS_IN_PROGRESS));
        assertFalse(sm.isTerminal(ErpPrjConstants.TASK_STATUS_BLOCKED));
    }

    // ---------- (e) 合法/非法来源态显式断言（含 IN_PROGRESS↔BLOCKED 往复） ----------

    @Test
    public void testExplicitActionGuards() {
        // start: 仅 TODO 合法
        assertActionAllowsOnly("start", ErpPrjConstants.TASK_STATUS_TODO);
        // complete: 仅 IN_PROGRESS 合法
        assertActionAllowsOnly("complete", ErpPrjConstants.TASK_STATUS_IN_PROGRESS);
        // block: 仅 IN_PROGRESS 合法
        assertActionAllowsOnly("block", ErpPrjConstants.TASK_STATUS_IN_PROGRESS);
        // unblock: 仅 BLOCKED 合法
        assertActionAllowsOnly("unblock", ErpPrjConstants.TASK_STATUS_BLOCKED);
    }

    @Test
    public void testInProgressBlockedRoundTrip() {
        // IN_PROGRESS↔BLOCKED 合法往复（block→unblock→block...）
        sm.assertCanBlock(ErpPrjConstants.TASK_STATUS_IN_PROGRESS); // IN_PROGRESS → block 合法
        sm.assertCanUnblock(ErpPrjConstants.TASK_STATUS_BLOCKED);   // BLOCKED → unblock 合法
        sm.assertCanBlock(ErpPrjConstants.TASK_STATUS_IN_PROGRESS); // 回到 IN_PROGRESS 可再 block
    }

    @Test
    public void testDoneTerminalHasNoOutgoingAction() {
        // DONE 终态：所有动作均非法
        for (String action : Arrays.asList("start", "complete", "block", "unblock")) {
            NopException ex = assertThrows(NopException.class,
                    () -> invokeAssert(action, ErpPrjConstants.TASK_STATUS_DONE),
                    "DONE 终态对动作应非法: " + action);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                    "Bean 报告 common 层非法迁移码: action=" + action);
            assertEquals(action, ex.getParam(ErpPrjTaskStateMachine.ARG_ACTION),
                    "拒绝元数据携带动作名: action=" + action);
        }
    }

    @Test
    public void testTargetStatusMethods() {
        assertEquals(ErpPrjConstants.TASK_STATUS_IN_PROGRESS, sm.startTargetStatus());
        assertEquals(ErpPrjConstants.TASK_STATUS_DONE, sm.completeTargetStatus());
        assertEquals(ErpPrjConstants.TASK_STATUS_BLOCKED, sm.blockTargetStatus());
        assertEquals(ErpPrjConstants.TASK_STATUS_IN_PROGRESS, sm.unblockTargetStatus());
    }

    // ---------- helpers ----------

    /**
     * 断言某 action 仅允许指定来源态：该来源态放行（不抛），其余全部状态非法（抛 common 码 + action 元数据）。
     */
    private void assertActionAllowsOnly(String action, String allowedFrom) {
        for (String s : ALL_STATUSES) {
            if (allowedFrom.equals(s)) {
                invokeAssert(action, s); // 不抛 = 合法
            } else {
                NopException ex = assertThrows(NopException.class, () -> invokeAssert(action, s),
                        action + " 对非允许来源态应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                        "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + s);
                assertEquals(action, ex.getParam(ErpPrjTaskStateMachine.ARG_ACTION),
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
            case "block":
                sm.assertCanBlock(status);
                break;
            case "unblock":
                sm.assertCanUnblock(status);
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "start":
                return sm.startTargetStatus();
            case "complete":
                return sm.completeTargetStatus();
            case "block":
                return sm.blockTargetStatus();
            case "unblock":
                return sm.unblockTargetStatus();
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
            for (ErpPrjTaskStateMachine.TransitionDefinition e : sm.transitions()) {
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
