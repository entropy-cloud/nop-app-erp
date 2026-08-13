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
 * <p>针对 {@link ErpQaRecallApprovalStateMachine} Bean（approveStatus 审批轴 5 动作）的纯矩阵完备性遍历：不经 facade
 * 入口（层 3 职责），不断言副作用/审计。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边（6 边唯一 action|from）；</li>
 *   <li>(b) 从 UNSUBMITTED 可达 SUBMITTED/APPROVED/REJECTED，REJECTED 经 submit 重提可达 SUBMITTED；</li>
 *   <li>(c) 各 {@code assertCanXxx} 合法来源态通过、非法来源态抛 common 码携带 {@code action}/{@code fromStatus}；</li>
 *   <li>(d) {@code transitions()} 与显式方法语义一致；</li>
 *   <li>(e) 初始/终态集合正确（APPROVED 为可逆业务终态，经 reverseApprove 有出边）；reverseApprove 目标态=REJECTED。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpQaRecallApprovalStateMachineMatrix {

    private static final List<String> ALL_APPROVE_STATUSES = Arrays.asList(
            ErpQaConstants.APPROVE_STATUS_UNSUBMITTED,
            ErpQaConstants.APPROVE_STATUS_SUBMITTED,
            ErpQaConstants.APPROVE_STATUS_APPROVED,
            ErpQaConstants.APPROVE_STATUS_REJECTED);

    private final ErpQaRecallApprovalStateMachine sm = new ErpQaRecallApprovalStateMachine();

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpQaRecallApprovalStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpQaRecallApprovalStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(6, edges.size(), "迁移矩阵应有 6 条边（submit×2 + approve + reject + reverseApprove + withdraw）");
    }

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED);
        assertTrue(reachable.contains(ErpQaConstants.APPROVE_STATUS_SUBMITTED));
        assertTrue(reachable.contains(ErpQaConstants.APPROVE_STATUS_APPROVED));
        assertTrue(reachable.contains(ErpQaConstants.APPROVE_STATUS_REJECTED));
    }

    @Test
    public void testAssertCanSubmitLegalAndIllegal() {
        sm.assertCanSubmit(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED);
        sm.assertCanSubmit(null);
        sm.assertCanSubmit(ErpQaConstants.APPROVE_STATUS_REJECTED);
        assertEquals(ErpQaConstants.APPROVE_STATUS_SUBMITTED, sm.submitTargetStatus());
        for (String illegal : Arrays.asList(ErpQaConstants.APPROVE_STATUS_SUBMITTED, ErpQaConstants.APPROVE_STATUS_APPROVED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanSubmit(illegal));
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
            assertEquals("submit", ex.getParam(ErpQaRecallApprovalStateMachine.ARG_ACTION));
            assertEquals(illegal, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
        }
    }

    @Test
    public void testAssertCanApproveLegalAndIllegal() {
        sm.assertCanApprove(ErpQaConstants.APPROVE_STATUS_SUBMITTED);
        assertEquals(ErpQaConstants.APPROVE_STATUS_APPROVED, sm.approveTargetStatus());
        for (String illegal : Arrays.asList(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED,
                ErpQaConstants.APPROVE_STATUS_APPROVED, ErpQaConstants.APPROVE_STATUS_REJECTED)) {
            assertThrows(NopException.class, () -> sm.assertCanApprove(illegal));
        }
    }

    @Test
    public void testAssertCanRejectLegalAndIllegal() {
        sm.assertCanReject(ErpQaConstants.APPROVE_STATUS_SUBMITTED);
        assertEquals(ErpQaConstants.APPROVE_STATUS_REJECTED, sm.rejectTargetStatus());
        for (String illegal : Arrays.asList(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED,
                ErpQaConstants.APPROVE_STATUS_APPROVED, ErpQaConstants.APPROVE_STATUS_REJECTED)) {
            assertThrows(NopException.class, () -> sm.assertCanReject(illegal));
        }
    }

    @Test
    public void testAssertCanReverseApproveLegalAndIllegal() {
        sm.assertCanReverseApprove(ErpQaConstants.APPROVE_STATUS_APPROVED);
        // reverseApprove 目标态=REJECTED（据实保持 Recall 当前行为，已合规 §16.4）
        assertEquals(ErpQaConstants.APPROVE_STATUS_REJECTED, sm.reverseApproveTargetStatus());
        for (String illegal : Arrays.asList(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED,
                ErpQaConstants.APPROVE_STATUS_SUBMITTED, ErpQaConstants.APPROVE_STATUS_REJECTED)) {
            assertThrows(NopException.class, () -> sm.assertCanReverseApprove(illegal));
        }
    }

    @Test
    public void testAssertCanWithdrawLegalAndIllegal() {
        sm.assertCanWithdraw(ErpQaConstants.APPROVE_STATUS_SUBMITTED);
        assertEquals(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED, sm.withdrawTargetStatus());
        for (String illegal : Arrays.asList(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED,
                ErpQaConstants.APPROVE_STATUS_APPROVED, ErpQaConstants.APPROVE_STATUS_REJECTED)) {
            assertThrows(NopException.class, () -> sm.assertCanWithdraw(illegal));
        }
    }

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpQaRecallApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
            invokeAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()));
        }
    }

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED), sm.initialStatuses());
        assertEquals(Arrays.asList(ErpQaConstants.APPROVE_STATUS_APPROVED), sm.terminalStatuses());
        assertTrue(sm.isTerminal(ErpQaConstants.APPROVE_STATUS_APPROVED));
        assertFalse(sm.isTerminal(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED));
    }

    @Test
    public void testApprovedIsReversibleTerminal() {
        boolean approvedHasOutgoing = false;
        for (ErpQaRecallApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
            if (e.getFromStatus().equals(ErpQaConstants.APPROVE_STATUS_APPROVED)) {
                approvedHasOutgoing = true;
                assertEquals("reverseApprove", e.getAction());
                assertEquals(ErpQaConstants.APPROVE_STATUS_REJECTED, e.getToStatus());
            }
        }
        assertTrue(approvedHasOutgoing, "APPROVED 应有 reverseApprove 出边（可逆终态）");
    }

    private void invokeAssert(String action, String approveStatus) {
        switch (action) {
            case "submit": sm.assertCanSubmit(approveStatus); break;
            case "approve": sm.assertCanApprove(approveStatus); break;
            case "reject": sm.assertCanReject(approveStatus); break;
            case "reverseApprove": sm.assertCanReverseApprove(approveStatus); break;
            case "withdraw": sm.assertCanWithdraw(approveStatus); break;
            default: throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "submit": return sm.submitTargetStatus();
            case "approve": return sm.approveTargetStatus();
            case "reject": return sm.rejectTargetStatus();
            case "reverseApprove": return sm.reverseApproveTargetStatus();
            case "withdraw": return sm.withdrawTargetStatus();
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
            for (ErpQaRecallApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
                if (e.getFromStatus().equals(cur) && !visited.contains(e.getToStatus())) {
                    frontier.add(e.getToStatus());
                }
            }
        }
        visited.remove(start);
        return visited;
    }
}
