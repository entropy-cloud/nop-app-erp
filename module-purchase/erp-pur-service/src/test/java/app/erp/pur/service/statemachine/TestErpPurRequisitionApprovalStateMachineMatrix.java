package app.erp.pur.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.pur.dao.constants.ErpPurDocStatus;
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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-13-0945-1 Phase 2 Proof）。
 *
 * <p>针对 {@link ErpPurRequisitionApprovalStateMachine} Bean 的纯矩阵完备性遍历（不经 BizModel 入口）。
 * 矩阵与 {@code TestErpPurOrderApprovalStateMachineMatrix} 同构（共享 dict + 同 5 动作）。
 */
public class TestErpPurRequisitionApprovalStateMachineMatrix {

    private final ErpPurRequisitionApprovalStateMachine sm = new ErpPurRequisitionApprovalStateMachine();

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpPurRequisitionApprovalStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpPurRequisitionApprovalStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(6, edges.size(), "迁移矩阵应有 6 条边");
    }

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpPurDocStatus.APPROVE_STATUS_UNSUBMITTED);
        assertTrue(reachable.contains(ErpPurDocStatus.APPROVE_STATUS_SUBMITTED));
        assertTrue(reachable.contains(ErpPurDocStatus.APPROVE_STATUS_APPROVED));
        assertTrue(reachable.contains(ErpPurDocStatus.APPROVE_STATUS_REJECTED));
    }

    @Test
    public void testRejectedCanResubmitToSubmitted() {
        assertTrue(reachableFrom(ErpPurDocStatus.APPROVE_STATUS_REJECTED).contains(ErpPurDocStatus.APPROVE_STATUS_SUBMITTED));
    }

    @Test
    public void testAssertCanSubmitLegalAndIllegal() {
        sm.assertCanSubmit(ErpPurDocStatus.APPROVE_STATUS_UNSUBMITTED);
        sm.assertCanSubmit(null);
        sm.assertCanSubmit(ErpPurDocStatus.APPROVE_STATUS_REJECTED);
        assertEquals(ErpPurDocStatus.APPROVE_STATUS_SUBMITTED, sm.submitTargetStatus());
        for (String illegal : Arrays.asList(ErpPurDocStatus.APPROVE_STATUS_SUBMITTED, ErpPurDocStatus.APPROVE_STATUS_APPROVED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanSubmit(illegal));
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
            assertEquals("submit", ex.getParam(ErpPurRequisitionApprovalStateMachine.ARG_ACTION));
            assertEquals(illegal, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
        }
    }

    @Test
    public void testAssertCanApproveLegalAndIllegal() {
        sm.assertCanApprove(ErpPurDocStatus.APPROVE_STATUS_SUBMITTED);
        assertEquals(ErpPurDocStatus.APPROVE_STATUS_APPROVED, sm.approveTargetStatus());
        for (String illegal : Arrays.asList(ErpPurDocStatus.APPROVE_STATUS_UNSUBMITTED,
                ErpPurDocStatus.APPROVE_STATUS_APPROVED, ErpPurDocStatus.APPROVE_STATUS_REJECTED)) {
            assertThrows(NopException.class, () -> sm.assertCanApprove(illegal));
        }
    }

    @Test
    public void testAssertCanRejectLegalAndIllegal() {
        sm.assertCanReject(ErpPurDocStatus.APPROVE_STATUS_SUBMITTED);
        assertEquals(ErpPurDocStatus.APPROVE_STATUS_REJECTED, sm.rejectTargetStatus());
        assertThrows(NopException.class, () -> sm.assertCanReject(ErpPurDocStatus.APPROVE_STATUS_UNSUBMITTED));
    }

    @Test
    public void testAssertCanReverseApproveLegalAndIllegal() {
        sm.assertCanReverseApprove(ErpPurDocStatus.APPROVE_STATUS_APPROVED);
        assertEquals(ErpPurDocStatus.APPROVE_STATUS_REJECTED, sm.reverseApproveTargetStatus());
        assertThrows(NopException.class, () -> sm.assertCanReverseApprove(ErpPurDocStatus.APPROVE_STATUS_SUBMITTED));
    }

    @Test
    public void testAssertCanWithdrawLegalAndIllegal() {
        sm.assertCanWithdraw(ErpPurDocStatus.APPROVE_STATUS_SUBMITTED);
        assertEquals(ErpPurDocStatus.APPROVE_STATUS_UNSUBMITTED, sm.withdrawTargetStatus());
        assertThrows(NopException.class, () -> sm.assertCanWithdraw(ErpPurDocStatus.APPROVE_STATUS_APPROVED));
    }

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpPurRequisitionApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
            invokeAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()));
        }
    }

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpPurDocStatus.APPROVE_STATUS_UNSUBMITTED), sm.initialStatuses());
        assertEquals(Arrays.asList(ErpPurDocStatus.APPROVE_STATUS_APPROVED), sm.terminalStatuses());
        assertTrue(sm.isTerminal(ErpPurDocStatus.APPROVE_STATUS_APPROVED));
        assertFalse(sm.isTerminal(ErpPurDocStatus.APPROVE_STATUS_REJECTED));
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
            for (ErpPurRequisitionApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
                if (e.getFromStatus().equals(cur) && !visited.contains(e.getToStatus())) {
                    frontier.add(e.getToStatus());
                }
            }
        }
        visited.remove(start);
        return visited;
    }
}
