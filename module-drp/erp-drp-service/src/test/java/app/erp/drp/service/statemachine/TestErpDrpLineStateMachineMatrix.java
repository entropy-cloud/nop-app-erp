package app.erp.drp.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.drp.service.ErpDrpConstants;
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
 * <p>针对 {@link ErpDrpLineStateMachine} Bean 的纯矩阵完备性遍历：不经 BizModel 入口（层 3 职责），
 * 不断言副作用/释放下游单据生成/类型守卫/幂等码。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边；</li>
 *   <li>(b) 从 SUGGESTED 可达 APPROVED/ORDERED/CANCELLED；</li>
 *   <li>(c) cancel 多源 {SUGGESTED, APPROVED} 合法、对终态 {ORDERED, CANCELLED} 非法；</li>
 *   <li>(d) ORDERED/CANCELLED 终态无出边；</li>
 *   <li>(e) {@code transitions()} 元数据一致（4 边）；</li>
 *   <li>(f) 初始/终态集合正确。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpDrpLineStateMachineMatrix {

    private static final List<String> ALL_STATUSES = Arrays.asList(
            ErpDrpConstants.DRP_LINE_STATUS_SUGGESTED,
            ErpDrpConstants.DRP_LINE_STATUS_APPROVED,
            ErpDrpConstants.DRP_LINE_STATUS_ORDERED,
            ErpDrpConstants.DRP_LINE_STATUS_CANCELLED);

    private final ErpDrpLineStateMachine sm = new ErpDrpLineStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpDrpLineStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpDrpLineStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            // 同一 action + 同一 fromStatus 不得出现多次（否则冲突）
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(4, edges.size(), "迁移矩阵应有 4 条边（approveLine/releaseLine 各 1 + cancel 2 来源）");
    }

    // ---------- (b) 从 SUGGESTED 可达 APPROVED/ORDERED/CANCELLED ----------

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpDrpConstants.DRP_LINE_STATUS_SUGGESTED);
        for (String s : ALL_STATUSES) {
            if (ErpDrpConstants.DRP_LINE_STATUS_SUGGESTED.equals(s)) {
                continue;
            }
            assertTrue(reachable.contains(s), "从 SUGGESTED 应可达状态: " + s);
        }
    }

    // ---------- (c) cancel 多源 {SUGGESTED, APPROVED} 合法、对终态 {ORDERED, CANCELLED} 非法 ----------

    @Test
    public void testCancelLegalForNonTerminalAndIllegalForTerminal() {
        for (String s : ALL_STATUSES) {
            if (sm.isTerminal(s)) {
                NopException ex = assertThrows(NopException.class, () -> sm.assertCanCancel(s),
                        "cancel 对终态应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                        "Bean 报告 common 层非法迁移码");
                assertEquals("cancel", ex.getParam(ErpDrpLineStateMachine.ARG_ACTION), "拒绝元数据携带动作名");
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS), "拒绝元数据携带当前态");
            } else {
                sm.assertCanCancel(s); // 合法边不抛
                assertEquals(ErpDrpConstants.DRP_LINE_STATUS_CANCELLED, sm.cancelTargetStatus());
            }
        }
    }

    // ---------- (d) ORDERED/CANCELLED 终态无出边 ----------

    @Test
    public void testTerminalStatusesHaveNoOutgoingEdges() {
        for (String terminal : sm.terminalStatuses()) {
            for (ErpDrpLineStateMachine.TransitionDefinition e : sm.transitions()) {
                assertFalse(e.getFromStatus().equals(terminal),
                        "终态不应有出边: terminal=" + terminal + ", but edge " + e.getAction() + " leaves it");
            }
        }
    }

    // ---------- (e) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpDrpLineStateMachine.TransitionDefinition e : sm.transitions()) {
            // 每条边的 fromStatus 对该 action 合法（assert 放行不抛）。单来源动作的「其余态非法」由
            // testExplicitActionGuards 覆盖；cancel 为多来源动作（SUGGESTED/APPROVED），此处只验证声明的边均合法。
            invokeAssert(e.getAction(), e.getFromStatus());
            // 每条边的 toStatus 与 <action>TargetStatus() 一致
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (f) 初始/终态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpDrpConstants.DRP_LINE_STATUS_ORDERED, ErpDrpConstants.DRP_LINE_STATUS_CANCELLED),
                sm.terminalStatuses(), "终态集合 = {ORDERED, CANCELLED}");
        assertEquals(Collections.singletonList(ErpDrpConstants.DRP_LINE_STATUS_SUGGESTED), sm.initialStatuses(),
                "初始态集合 = {SUGGESTED}");

        assertTrue(sm.isTerminal(ErpDrpConstants.DRP_LINE_STATUS_ORDERED));
        assertTrue(sm.isTerminal(ErpDrpConstants.DRP_LINE_STATUS_CANCELLED));
        assertFalse(sm.isTerminal(ErpDrpConstants.DRP_LINE_STATUS_SUGGESTED));
        assertFalse(sm.isTerminal(ErpDrpConstants.DRP_LINE_STATUS_APPROVED));
    }

    // ---------- 合法/非法来源态显式断言（补充显式方法语义核对） ----------

    @Test
    public void testExplicitActionGuards() {
        // approveLine: 仅 SUGGESTED 合法
        assertActionAllowsOnly("approveLine", ErpDrpConstants.DRP_LINE_STATUS_SUGGESTED);
        // releaseLine: 仅 APPROVED 合法
        assertActionAllowsOnly("releaseLine", ErpDrpConstants.DRP_LINE_STATUS_APPROVED);
    }

    @Test
    public void testTargetStatusMethods() {
        assertEquals(ErpDrpConstants.DRP_LINE_STATUS_APPROVED, sm.approveLineTargetStatus());
        assertEquals(ErpDrpConstants.DRP_LINE_STATUS_ORDERED, sm.releaseTargetStatus());
        assertEquals(ErpDrpConstants.DRP_LINE_STATUS_CANCELLED, sm.cancelTargetStatus());
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
                assertEquals(action, ex.getParam(ErpDrpLineStateMachine.ARG_ACTION),
                        "拒绝元数据携带动作名: action=" + action);
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                        "拒绝元数据携带当前态: action=" + action + ", status=" + s);
            }
        }
    }

    private void invokeAssert(String action, String status) {
        switch (action) {
            case "approveLine":
                sm.assertCanApproveLine(status);
                break;
            case "releaseLine":
                sm.assertCanRelease(status);
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
            case "approveLine":
                return sm.approveLineTargetStatus();
            case "releaseLine":
                return sm.releaseTargetStatus();
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
            for (ErpDrpLineStateMachine.TransitionDefinition e : sm.transitions()) {
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
