package app.erp.crm.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.crm.service.ErpCrmConstants;
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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-12-2142-2 Phase 1 Proof）。
 *
 * <p>针对 {@link ErpCrmEventStateMachine} Bean 的纯矩阵完备性遍历：不经 BizModel 入口（层 3 职责），
 * 不断言副作用/Lead 派生/审计。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边；</li>
 *   <li>(b) 从 PLANNED 命名动作可达 COMPLETED 与 CANCELLED 全部声明状态；</li>
 *   <li>(c) 终态 COMPLETED/CANCELLED 对 complete 与 cancel 均非法（无出边）；</li>
 *   <li>(d) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(e) 终态/初始态集合正确（终态 = {COMPLETED, CANCELLED}；初始态 = {PLANNED}）。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpCrmEventStateMachineMatrix {

    /** dict {@code erp-crm/event-status} 的 3 值。 */
    private static final List<String> ALL_STATUSES = Arrays.asList(
            ErpCrmConstants.EVENT_STATUS_PLANNED,
            ErpCrmConstants.EVENT_STATUS_COMPLETED,
            ErpCrmConstants.EVENT_STATUS_CANCELLED);

    private final ErpCrmEventStateMachine sm = new ErpCrmEventStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpCrmEventStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpCrmEventStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            // 同一 action + 同一 fromStatus 不得出现多次（否则冲突）
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(2, edges.size(), "迁移矩阵应有 2 条边（complete + cancel）");
    }

    // ---------- (b) 从 PLANNED 命名动作可达 COMPLETED 与 CANCELLED ----------

    @Test
    public void testReachabilityFromPlannedCoversAllDeclaredStatuses() {
        Set<String> reachable = reachableFrom(ErpCrmConstants.EVENT_STATUS_PLANNED);
        assertTrue(reachable.contains(ErpCrmConstants.EVENT_STATUS_COMPLETED),
                "PLANNED→COMPLETED 经 complete 可达");
        assertTrue(reachable.contains(ErpCrmConstants.EVENT_STATUS_CANCELLED),
                "PLANNED→CANCELLED 经 cancel 可达");
    }

    @Test
    public void testTerminalStatusesHaveNoOutgoingEdges() {
        for (String terminal : sm.terminalStatuses()) {
            for (ErpCrmEventStateMachine.TransitionDefinition e : sm.transitions()) {
                assertFalse(e.getFromStatus().equals(terminal),
                        "终态不应有出边: terminal=" + terminal + ", but edge " + e.getAction() + " leaves it");
            }
        }
    }

    // ---------- (c) 终态 COMPLETED/CANCELLED 对 complete 与 cancel 均非法 ----------

    @Test
    public void testTerminalStatusesRejectAllActions() {
        // 终态对 complete 与 cancel 均非法（无出边）
        for (String terminal : sm.terminalStatuses()) {
            NopException completeEx = assertThrows(NopException.class, () -> sm.assertCanComplete(terminal),
                    "complete 对终态应非法: " + terminal);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), completeEx.getErrorCode(),
                    "complete(terminal) 报告 common 层非法迁移码");
            assertEquals("complete", completeEx.getParam(ErpCrmEventStateMachine.ARG_ACTION),
                    "complete 拒绝元数据携带动作名");

            NopException cancelEx = assertThrows(NopException.class, () -> sm.assertCanCancel(terminal),
                    "cancel 对终态应非法: " + terminal);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), cancelEx.getErrorCode(),
                    "cancel(terminal) 报告 common 层非法迁移码");
            assertEquals("cancel", cancelEx.getParam(ErpCrmEventStateMachine.ARG_ACTION),
                    "cancel 拒绝元数据携带动作名");
        }
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpCrmEventStateMachine.TransitionDefinition e : sm.transitions()) {
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
        assertEquals(Arrays.asList(ErpCrmConstants.EVENT_STATUS_COMPLETED, ErpCrmConstants.EVENT_STATUS_CANCELLED),
                sm.terminalStatuses(), "终态集合 = {COMPLETED, CANCELLED}");
        assertEquals(Arrays.asList(ErpCrmConstants.EVENT_STATUS_PLANNED), sm.initialStatuses(), "初始态集合 = {PLANNED}");

        assertTrue(sm.isTerminal(ErpCrmConstants.EVENT_STATUS_COMPLETED));
        assertTrue(sm.isTerminal(ErpCrmConstants.EVENT_STATUS_CANCELLED));
        assertFalse(sm.isTerminal(ErpCrmConstants.EVENT_STATUS_PLANNED));
    }

    // ---------- 合法/非法来源态显式断言（补充显式方法语义核对） ----------

    @Test
    public void testExplicitActionGuards() {
        // complete: 仅 PLANNED 合法
        assertActionAllowsOnly("complete", ErpCrmConstants.EVENT_STATUS_PLANNED);
        // cancel: 仅 PLANNED 合法
        assertActionAllowsOnly("cancel", ErpCrmConstants.EVENT_STATUS_PLANNED);
    }

    @Test
    public void testTargetStatusMethods() {
        assertEquals(ErpCrmConstants.EVENT_STATUS_COMPLETED, sm.completeTargetStatus());
        assertEquals(ErpCrmConstants.EVENT_STATUS_CANCELLED, sm.cancelTargetStatus());
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
                assertEquals(action, ex.getParam(ErpCrmEventStateMachine.ARG_ACTION),
                        "拒绝元数据携带动作名: action=" + action);
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                        "拒绝元数据携带当前态: action=" + action + ", status=" + s);
            }
        }
    }

    private void invokeAssert(String action, String status) {
        switch (action) {
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
            for (ErpCrmEventStateMachine.TransitionDefinition e : sm.transitions()) {
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
