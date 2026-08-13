package app.erp.qa.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.qa.service.ErpQaConstants;
import io.nop.api.core.exceptions.NopException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-14-0930-2 Phase 3 Proof）。
 *
 * <p>针对 {@link ErpQaRecallStateMachine} Bean（status 操作轴 5 态 OPEN/APPROVED/IN_PROGRESS/CLOSED/CANCELLED）的
 * 纯矩阵完备性遍历：不经 Processor/facade 入口（层 3 职责），不断言副作用/审计。覆盖：
 * <ul>
 *   <li>(a) 无完全重复边（action|from|to 三元唯一，cancel 多来源态）；</li>
 *   <li>(b) 从 OPEN 可达 APPROVED/IN_PROGRESS/CLOSED/CANCELLED；</li>
 *   <li>(c) 各 {@code assertCanXxx} 合法来源态通过、非法来源态抛 common 码携带 {@code action}/{@code fromStatus}；</li>
 *   <li>(d) {@code transitions()} 与显式方法语义一致；</li>
 *   <li>(e) 初始/终态集合正确（CLOSED/CANCELLED 无出边）。</li>
 * </ul>
 *
 * <p>注：approve/reject 联动边（OPEN→APPROVED / OPEN→CANCELLED）纳入 status 矩阵（文档完整性），实际联动写入在
 * facade doApprove/doReject 原位。register 为初始写（§9.2 选项 c），不经 assert。
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpQaRecallStateMachineMatrix {

    private static final List<String> ALL_RECALL_STATUSES = Arrays.asList(
            ErpQaConstants.RECALL_STATUS_OPEN,
            ErpQaConstants.RECALL_STATUS_APPROVED,
            ErpQaConstants.RECALL_STATUS_IN_PROGRESS,
            ErpQaConstants.RECALL_STATUS_CLOSED,
            ErpQaConstants.RECALL_STATUS_CANCELLED);

    private final ErpQaRecallStateMachine sm = new ErpQaRecallStateMachine();

    // ---------- (a) 无完全重复边 ----------

    @Test
    public void testNoDuplicateEdges() {
        List<ErpQaRecallStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpQaRecallStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus() + "|" + e.getToStatus();
            assertTrue(seen.add(key), "重复边: " + key);
        }
        // approve + locateTargets + close + reject + cancel×3 = 7 边
        assertEquals(7, edges.size(), "迁移矩阵应有 7 条边");
    }

    // ---------- (b) 从 OPEN 可达全部声明状态 ----------

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpQaConstants.RECALL_STATUS_OPEN);
        assertTrue(reachable.contains(ErpQaConstants.RECALL_STATUS_APPROVED), "从 OPEN 应可达 APPROVED");
        assertTrue(reachable.contains(ErpQaConstants.RECALL_STATUS_IN_PROGRESS), "从 OPEN 应可达 IN_PROGRESS");
        assertTrue(reachable.contains(ErpQaConstants.RECALL_STATUS_CLOSED), "从 OPEN 应可达 CLOSED");
        assertTrue(reachable.contains(ErpQaConstants.RECALL_STATUS_CANCELLED), "从 OPEN 应可达 CANCELLED");
    }

    // ---------- (c) assertCanXxx 合法/非法 ----------

    @Test
    public void testAssertCanApproveLegalAndIllegal() {
        sm.assertCanApprove(ErpQaConstants.RECALL_STATUS_OPEN);
        assertEquals(ErpQaConstants.RECALL_STATUS_APPROVED, sm.approveTargetStatus());
        for (String illegal : Arrays.asList(ErpQaConstants.RECALL_STATUS_APPROVED,
                ErpQaConstants.RECALL_STATUS_IN_PROGRESS, ErpQaConstants.RECALL_STATUS_CLOSED,
                ErpQaConstants.RECALL_STATUS_CANCELLED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanApprove(illegal));
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
            assertEquals("approve", ex.getParam(ErpQaRecallStateMachine.ARG_ACTION));
            assertEquals(illegal, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
        }
    }

    @Test
    public void testAssertCanLocateTargetsLegalAndIllegal() {
        sm.assertCanLocateTargets(ErpQaConstants.RECALL_STATUS_APPROVED);
        assertEquals(ErpQaConstants.RECALL_STATUS_IN_PROGRESS, sm.locateTargetsTargetStatus());
        for (String illegal : Arrays.asList(ErpQaConstants.RECALL_STATUS_OPEN,
                ErpQaConstants.RECALL_STATUS_IN_PROGRESS, ErpQaConstants.RECALL_STATUS_CLOSED,
                ErpQaConstants.RECALL_STATUS_CANCELLED)) {
            assertThrows(NopException.class, () -> sm.assertCanLocateTargets(illegal));
        }
    }

    @Test
    public void testAssertCanCloseLegalAndIllegal() {
        sm.assertCanClose(ErpQaConstants.RECALL_STATUS_IN_PROGRESS);
        assertEquals(ErpQaConstants.RECALL_STATUS_CLOSED, sm.closeTargetStatus());
        for (String illegal : Arrays.asList(ErpQaConstants.RECALL_STATUS_OPEN,
                ErpQaConstants.RECALL_STATUS_APPROVED, ErpQaConstants.RECALL_STATUS_CLOSED,
                ErpQaConstants.RECALL_STATUS_CANCELLED)) {
            assertThrows(NopException.class, () -> sm.assertCanClose(illegal));
        }
    }

    @Test
    public void testAssertCanCancelLegalAndIllegal() {
        sm.assertCanCancel(ErpQaConstants.RECALL_STATUS_OPEN);
        sm.assertCanCancel(ErpQaConstants.RECALL_STATUS_APPROVED);
        sm.assertCanCancel(ErpQaConstants.RECALL_STATUS_IN_PROGRESS);
        assertEquals(ErpQaConstants.RECALL_STATUS_CANCELLED, sm.cancelTargetStatus());
        for (String illegal : Arrays.asList(ErpQaConstants.RECALL_STATUS_CLOSED,
                ErpQaConstants.RECALL_STATUS_CANCELLED)) {
            assertThrows(NopException.class, () -> sm.assertCanCancel(illegal));
        }
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpQaRecallStateMachine.TransitionDefinition e : sm.transitions()) {
            invokeAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (e) 初始/终态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpQaConstants.RECALL_STATUS_OPEN), sm.initialStatuses(),
                "初始态集合 = {OPEN}");
        assertEquals(Arrays.asList(ErpQaConstants.RECALL_STATUS_CLOSED,
                ErpQaConstants.RECALL_STATUS_CANCELLED), sm.terminalStatuses(),
                "终态集合 = {CLOSED, CANCELLED}");

        assertTrue(sm.isTerminal(ErpQaConstants.RECALL_STATUS_CLOSED));
        assertTrue(sm.isTerminal(ErpQaConstants.RECALL_STATUS_CANCELLED));
        assertFalse(sm.isTerminal(ErpQaConstants.RECALL_STATUS_OPEN));
        assertFalse(sm.isTerminal(ErpQaConstants.RECALL_STATUS_APPROVED));
        assertFalse(sm.isTerminal(ErpQaConstants.RECALL_STATUS_IN_PROGRESS));
    }

    @Test
    public void testTerminalsHaveNoOutgoingEdges() {
        for (String terminal : sm.terminalStatuses()) {
            for (ErpQaRecallStateMachine.TransitionDefinition e : sm.transitions()) {
                assertFalse(e.getFromStatus().equals(terminal),
                        "终态 " + terminal + " 不应有出边: " + e.getAction());
            }
        }
    }

    @Test
    public void testRegisterInitialTargetStatus() {
        assertEquals(ErpQaConstants.RECALL_STATUS_OPEN, sm.registerTargetStatus(), "register 初始写目标态=OPEN");
    }

    // ---------- helpers ----------

    private void invokeAssert(String action, String status) {
        switch (action) {
            case "approve": sm.assertCanApprove(status); break;
            case "locateTargets": sm.assertCanLocateTargets(status); break;
            case "close": sm.assertCanClose(status); break;
            case "cancel": sm.assertCanCancel(status); break;
            case "reject":
                // reject 联动边无 status assert（审批轴 approveStatus=SUBMITTED 为 gate），仅校验目标态
                break;
            default: throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "approve": return sm.approveTargetStatus();
            case "locateTargets": return sm.locateTargetsTargetStatus();
            case "close": return sm.closeTargetStatus();
            case "reject": return sm.rejectTargetStatus();
            case "cancel": return sm.cancelTargetStatus();
            default: throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private Set<String> reachableFrom(String start) {
        Set<String> visited = new java.util.LinkedHashSet<>();
        List<String> frontier = new java.util.ArrayList<>();
        frontier.add(start);
        while (!frontier.isEmpty()) {
            String cur = frontier.remove(0);
            if (!visited.add(cur)) continue;
            for (ErpQaRecallStateMachine.TransitionDefinition e : sm.transitions()) {
                if (e.getFromStatus().equals(cur) && !visited.contains(e.getToStatus())) {
                    frontier.add(e.getToStatus());
                }
            }
        }
        visited.remove(start);
        return visited;
    }
}
