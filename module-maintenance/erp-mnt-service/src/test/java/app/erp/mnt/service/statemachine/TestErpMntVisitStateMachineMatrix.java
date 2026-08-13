package app.erp.mnt.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.mnt.dao.ErpMntDaoConstants;
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
 * <p>针对 {@link ErpMntVisitStateMachine} Bean 的纯矩阵完备性遍历：不经 BizModel 入口（层 3 职责），
 * 不断言副作用/审计。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边；</li>
 *   <li>(b) 从 DRAFT 可达全部 4 非初始态，两个终态（COMPLETED/CANCELLED）无出边；</li>
 *   <li>(c) cancel 对三源（DRAFT/SCHEDULED/IN_PROGRESS）合法、对终态非法；</li>
 *   <li>(d) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(e) 终态/初始态集合正确。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpMntVisitStateMachineMatrix {

    private static final List<String> ALL_STATUSES = Arrays.asList(
            ErpMntDaoConstants.VISIT_STATUS_DRAFT,
            ErpMntDaoConstants.VISIT_STATUS_SCHEDULED,
            ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS,
            ErpMntDaoConstants.VISIT_STATUS_COMPLETED,
            ErpMntDaoConstants.VISIT_STATUS_CANCELLED);

    private final ErpMntVisitStateMachine sm = new ErpMntVisitStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpMntVisitStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpMntVisitStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(6, edges.size(), "迁移矩阵应有 6 条边");
    }

    // ---------- (b) 从 DRAFT 可达全部 4 非初始态；终态无出边 ----------

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpMntDaoConstants.VISIT_STATUS_DRAFT);
        for (String s : ALL_STATUSES) {
            if (ErpMntDaoConstants.VISIT_STATUS_DRAFT.equals(s)) {
                continue;
            }
            assertTrue(reachable.contains(s), "从 DRAFT 应可达状态: " + s);
        }
    }

    @Test
    public void testTerminalStatusesHaveNoOutgoingEdges() {
        for (String terminal : sm.terminalStatuses()) {
            for (ErpMntVisitStateMachine.TransitionDefinition e : sm.transitions()) {
                assertFalse(e.getFromStatus().equals(terminal),
                        "终态不应有出边: terminal=" + terminal + ", but edge " + e.getAction() + " leaves it");
            }
        }
    }

    // ---------- (c) cancel 对三源（DRAFT/SCHEDULED/IN_PROGRESS）合法、对终态非法 ----------

    @Test
    public void testCancelTripleSourceLegal() {
        for (String s : ALL_STATUSES) {
            if (ErpMntDaoConstants.VISIT_STATUS_DRAFT.equals(s)
                    || ErpMntDaoConstants.VISIT_STATUS_SCHEDULED.equals(s)
                    || ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS.equals(s)) {
                sm.assertCanCancel(s); // 合法边不抛
                assertEquals(ErpMntDaoConstants.VISIT_STATUS_CANCELLED, sm.cancelTargetStatus());
            } else {
                NopException ex = assertThrows(NopException.class, () -> sm.assertCanCancel(s),
                        "cancel 对终态应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                        "Bean 报告 common 层非法迁移码");
                assertEquals("cancel", ex.getParam(ErpMntVisitStateMachine.ARG_ACTION), "拒绝元数据携带动作名");
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS), "拒绝元数据携带当前态");
            }
        }
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpMntVisitStateMachine.TransitionDefinition e : sm.transitions()) {
            invokeAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (e) 终态/初始态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpMntDaoConstants.VISIT_STATUS_COMPLETED,
                        ErpMntDaoConstants.VISIT_STATUS_CANCELLED),
                sm.terminalStatuses(), "终态集合 = {COMPLETED, CANCELLED}");
        assertEquals(Arrays.asList(ErpMntDaoConstants.VISIT_STATUS_DRAFT), sm.initialStatuses(), "初始态集合 = {DRAFT}");

        assertTrue(sm.isTerminal(ErpMntDaoConstants.VISIT_STATUS_COMPLETED));
        assertTrue(sm.isTerminal(ErpMntDaoConstants.VISIT_STATUS_CANCELLED));
        assertFalse(sm.isTerminal(ErpMntDaoConstants.VISIT_STATUS_DRAFT));
        assertFalse(sm.isTerminal(ErpMntDaoConstants.VISIT_STATUS_SCHEDULED));
        assertFalse(sm.isTerminal(ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS));
    }

    // ---------- 合法/非法来源态显式断言（补充显式方法语义核对） ----------

    @Test
    public void testExplicitActionGuards() {
        // schedule: 仅 DRAFT 合法
        assertActionAllowsOnly("schedule", ErpMntDaoConstants.VISIT_STATUS_DRAFT);
        // start: 仅 SCHEDULED 合法
        assertActionAllowsOnly("start", ErpMntDaoConstants.VISIT_STATUS_SCHEDULED);
        // complete: 仅 IN_PROGRESS 合法
        assertActionAllowsOnly("complete", ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS);
    }

    @Test
    public void testTargetStatusMethods() {
        assertEquals(ErpMntDaoConstants.VISIT_STATUS_SCHEDULED, sm.scheduleTargetStatus());
        assertEquals(ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS, sm.startTargetStatus());
        assertEquals(ErpMntDaoConstants.VISIT_STATUS_COMPLETED, sm.completeTargetStatus());
        assertEquals(ErpMntDaoConstants.VISIT_STATUS_CANCELLED, sm.cancelTargetStatus());
    }

    // ---------- helpers ----------

    private void assertActionAllowsOnly(String action, String allowedFrom) {
        for (String s : ALL_STATUSES) {
            if (allowedFrom.equals(s)) {
                invokeAssert(action, s); // 不抛 = 合法
            } else {
                NopException ex = assertThrows(NopException.class, () -> invokeAssert(action, s),
                        action + " 对非允许来源态应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                        "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + s);
                assertEquals(action, ex.getParam(ErpMntVisitStateMachine.ARG_ACTION),
                        "拒绝元数据携带动作名: action=" + action);
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                        "拒绝元数据携带当前态: action=" + action + ", status=" + s);
            }
        }
    }

    private void invokeAssert(String action, String status) {
        switch (action) {
            case "schedule":
                sm.assertCanSchedule(status);
                break;
            case "start":
                sm.assertCanStart(status);
                break;
            case "complete":
                sm.assertCanComplete(status);
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
            case "schedule":
                return sm.scheduleTargetStatus();
            case "start":
                return sm.startTargetStatus();
            case "complete":
                return sm.completeTargetStatus();
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
            for (ErpMntVisitStateMachine.TransitionDefinition e : sm.transitions()) {
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
