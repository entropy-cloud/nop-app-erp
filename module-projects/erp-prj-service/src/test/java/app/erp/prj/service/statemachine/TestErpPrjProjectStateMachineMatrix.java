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
 * <p>针对 {@link ErpPrjProjectStateMachine} Bean 的纯矩阵完备性遍历：不经 BizModel 入口（层 3 职责），
 * 不断言副作用/审计/前置校验。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边；</li>
 *   <li>(b) 从 DRAFT 可达全部 4 非初始态，COMPLETED/CANCELLED 终态无出边；</li>
 *   <li>(c) cancel 多源 {DRAFT,OPEN,ON_HOLD} 合法、对终态非法；</li>
 *   <li>(d) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(e) 终态/初始态集合正确。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpPrjProjectStateMachineMatrix {

    private static final List<String> ALL_STATUSES = Arrays.asList(
            ErpPrjConstants.PROJECT_STATUS_DRAFT,
            ErpPrjConstants.PROJECT_STATUS_OPEN,
            ErpPrjConstants.PROJECT_STATUS_ON_HOLD,
            ErpPrjConstants.PROJECT_STATUS_COMPLETED,
            ErpPrjConstants.PROJECT_STATUS_CANCELLED);

    private final ErpPrjProjectStateMachine sm = new ErpPrjProjectStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpPrjProjectStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpPrjProjectStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            // 同一 action + 同一 fromStatus 不得出现多次（否则冲突）
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(7, edges.size(), "迁移矩阵应有 7 条边（start/hold/resume/close 各 1 + cancel 3 来源）");
    }

    // ---------- (b) 从 DRAFT 可达全部 4 非初始态；终态无出边 ----------

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpPrjConstants.PROJECT_STATUS_DRAFT);
        for (String s : ALL_STATUSES) {
            if (ErpPrjConstants.PROJECT_STATUS_DRAFT.equals(s)) {
                continue;
            }
            assertTrue(reachable.contains(s), "从 DRAFT 应可达状态: " + s);
        }
    }

    @Test
    public void testTerminalStatusesHaveNoOutgoingEdges() {
        for (String terminal : sm.terminalStatuses()) {
            for (ErpPrjProjectStateMachine.TransitionDefinition e : sm.transitions()) {
                assertFalse(e.getFromStatus().equals(terminal),
                        "终态不应有出边: terminal=" + terminal + ", but edge " + e.getAction() + " leaves it");
            }
        }
    }

    // ---------- (c) cancel 多源 {DRAFT,OPEN,ON_HOLD} 合法、对终态非法 ----------

    @Test
    public void testCancelLegalForNonTerminalAndIllegalForTerminal() {
        for (String s : ALL_STATUSES) {
            if (sm.isTerminal(s)) {
                NopException ex = assertThrows(NopException.class, () -> sm.assertCanCancel(s),
                        "cancel 对终态应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                        "Bean 报告 common 层非法迁移码");
                assertEquals("cancel", ex.getParam(ErpPrjProjectStateMachine.ARG_ACTION), "拒绝元数据携带动作名");
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS), "拒绝元数据携带当前态");
            } else {
                sm.assertCanCancel(s); // 合法边不抛
                assertEquals(ErpPrjConstants.PROJECT_STATUS_CANCELLED, sm.cancelTargetStatus());
            }
        }
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpPrjProjectStateMachine.TransitionDefinition e : sm.transitions()) {
            // 每条边的 fromStatus 对该 action 合法（assert 放行不抛）。单来源动作的「其余态非法」由
            // testExplicitActionGuards 覆盖；cancel 为多来源动作（3 非终态），此处只验证声明的边均合法。
            invokeAssert(e.getAction(), e.getFromStatus());
            // 每条边的 toStatus 与 <action>TargetStatus() 一致
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (e) 终态/初始态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpPrjConstants.PROJECT_STATUS_COMPLETED, ErpPrjConstants.PROJECT_STATUS_CANCELLED),
                sm.terminalStatuses(), "终态集合 = {COMPLETED, CANCELLED}");
        assertEquals(Collections.singletonList(ErpPrjConstants.PROJECT_STATUS_DRAFT), sm.initialStatuses(),
                "初始态集合 = {DRAFT}");

        assertTrue(sm.isTerminal(ErpPrjConstants.PROJECT_STATUS_COMPLETED));
        assertTrue(sm.isTerminal(ErpPrjConstants.PROJECT_STATUS_CANCELLED));
        assertFalse(sm.isTerminal(ErpPrjConstants.PROJECT_STATUS_DRAFT));
        assertFalse(sm.isTerminal(ErpPrjConstants.PROJECT_STATUS_OPEN));
        assertFalse(sm.isTerminal(ErpPrjConstants.PROJECT_STATUS_ON_HOLD));
    }

    // ---------- 合法/非法来源态显式断言（补充显式方法语义核对） ----------

    @Test
    public void testExplicitActionGuards() {
        // start: 仅 DRAFT 合法
        assertActionAllowsOnly("start", ErpPrjConstants.PROJECT_STATUS_DRAFT);
        // hold: 仅 OPEN 合法
        assertActionAllowsOnly("hold", ErpPrjConstants.PROJECT_STATUS_OPEN);
        // resume: 仅 ON_HOLD 合法
        assertActionAllowsOnly("resume", ErpPrjConstants.PROJECT_STATUS_ON_HOLD);
        // close: 仅 OPEN 合法
        assertActionAllowsOnly("close", ErpPrjConstants.PROJECT_STATUS_OPEN);
    }

    @Test
    public void testOpenOnHoldRoundTrip() {
        // OPEN↔ON_HOLD 合法往复（hold→resume→hold...）
        sm.assertCanHold(ErpPrjConstants.PROJECT_STATUS_OPEN);   // OPEN → hold 合法
        sm.assertCanResume(ErpPrjConstants.PROJECT_STATUS_ON_HOLD); // ON_HOLD → resume 合法
        sm.assertCanHold(ErpPrjConstants.PROJECT_STATUS_OPEN);   // 回到 OPEN 可再 hold
    }

    @Test
    public void testTargetStatusMethods() {
        assertEquals(ErpPrjConstants.PROJECT_STATUS_OPEN, sm.startTargetStatus());
        assertEquals(ErpPrjConstants.PROJECT_STATUS_ON_HOLD, sm.holdTargetStatus());
        assertEquals(ErpPrjConstants.PROJECT_STATUS_OPEN, sm.resumeTargetStatus());
        assertEquals(ErpPrjConstants.PROJECT_STATUS_COMPLETED, sm.closeTargetStatus());
        assertEquals(ErpPrjConstants.PROJECT_STATUS_CANCELLED, sm.cancelTargetStatus());
    }

    // ---------- helpers ----------

    /**
     * 断言某单来源 action 仅允许指定来源态：该来源态放行（不抛），其余全部状态非法（抛 common 码 + action 元数据）。
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
                assertEquals(action, ex.getParam(ErpPrjProjectStateMachine.ARG_ACTION),
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
            case "hold":
                sm.assertCanHold(status);
                break;
            case "resume":
                sm.assertCanResume(status);
                break;
            case "close":
                sm.assertCanClose(status);
                break;
            case "cancel":
                sm.assertCanCancel(status);
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "start":
                return sm.startTargetStatus();
            case "hold":
                return sm.holdTargetStatus();
            case "resume":
                return sm.resumeTargetStatus();
            case "close":
                return sm.closeTargetStatus();
            case "cancel":
                return sm.cancelTargetStatus();
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
            for (ErpPrjProjectStateMachine.TransitionDefinition e : sm.transitions()) {
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
