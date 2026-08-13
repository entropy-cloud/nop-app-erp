package app.erp.sal.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.sal.dao.constants.ErpSalDocStatus;
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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-13-1950-2 Phase 2 Proof）。
 *
 * <p>针对 {@link ErpSalReturnApprovalStateMachine} Bean 的纯矩阵完备性遍历。Bean 严格无状态，直接 {@code new} 测试。
 */
public class TestErpSalReturnApprovalStateMachineMatrix {

    private final ErpSalReturnApprovalStateMachine sm = new ErpSalReturnApprovalStateMachine();

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpSalReturnApprovalStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpSalReturnApprovalStateMachine.TransitionDefinition e : edges) {
            assertTrue(seen.add(e.getAction() + "|" + e.getFromStatus()), "重复/冲突边");
        }
        assertEquals(6, edges.size());
    }

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpSalDocStatus.APPROVE_STATUS_UNSUBMITTED);
        assertTrue(reachable.contains(ErpSalDocStatus.APPROVE_STATUS_SUBMITTED));
        assertTrue(reachable.contains(ErpSalDocStatus.APPROVE_STATUS_APPROVED));
        assertTrue(reachable.contains(ErpSalDocStatus.APPROVE_STATUS_REJECTED));
    }

    @Test
    public void testRejectedCanResubmitToSubmitted() {
        assertTrue(reachableFrom(ErpSalDocStatus.APPROVE_STATUS_REJECTED).contains(ErpSalDocStatus.APPROVE_STATUS_SUBMITTED));
    }

    @Test
    public void testAssertCanSubmitLegalAndIllegal() {
        sm.assertCanSubmit(ErpSalDocStatus.APPROVE_STATUS_UNSUBMITTED);
        sm.assertCanSubmit(null);
        sm.assertCanSubmit(ErpSalDocStatus.APPROVE_STATUS_REJECTED);
        assertEquals(ErpSalDocStatus.APPROVE_STATUS_SUBMITTED, sm.submitTargetStatus());
        for (String illegal : Arrays.asList(ErpSalDocStatus.APPROVE_STATUS_SUBMITTED, ErpSalDocStatus.APPROVE_STATUS_APPROVED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanSubmit(illegal));
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
            assertEquals("submit", ex.getParam(ErpSalReturnApprovalStateMachine.ARG_ACTION));
            assertEquals(illegal, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
        }
    }

    @Test
    public void testAssertCanApproveLegalAndIllegal() {
        sm.assertCanApprove(ErpSalDocStatus.APPROVE_STATUS_SUBMITTED);
        assertEquals(ErpSalDocStatus.APPROVE_STATUS_APPROVED, sm.approveTargetStatus());
        for (String illegal : Arrays.asList(ErpSalDocStatus.APPROVE_STATUS_UNSUBMITTED,
                ErpSalDocStatus.APPROVE_STATUS_APPROVED, ErpSalDocStatus.APPROVE_STATUS_REJECTED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanApprove(illegal));
            assertEquals("approve", ex.getParam(ErpSalReturnApprovalStateMachine.ARG_ACTION));
        }
    }

    @Test
    public void testAssertCanRejectLegalAndIllegal() {
        sm.assertCanReject(ErpSalDocStatus.APPROVE_STATUS_SUBMITTED);
        assertEquals(ErpSalDocStatus.APPROVE_STATUS_REJECTED, sm.rejectTargetStatus());
        for (String illegal : Arrays.asList(ErpSalDocStatus.APPROVE_STATUS_UNSUBMITTED,
                ErpSalDocStatus.APPROVE_STATUS_APPROVED, ErpSalDocStatus.APPROVE_STATUS_REJECTED)) {
            assertThrows(NopException.class, () -> sm.assertCanReject(illegal));
        }
    }

    @Test
    public void testAssertCanReverseApproveLegalAndIllegal() {
        sm.assertCanReverseApprove(ErpSalDocStatus.APPROVE_STATUS_APPROVED);
        assertEquals(ErpSalDocStatus.APPROVE_STATUS_REJECTED, sm.reverseApproveTargetStatus());
        for (String illegal : Arrays.asList(ErpSalDocStatus.APPROVE_STATUS_UNSUBMITTED,
                ErpSalDocStatus.APPROVE_STATUS_SUBMITTED, ErpSalDocStatus.APPROVE_STATUS_REJECTED)) {
            assertThrows(NopException.class, () -> sm.assertCanReverseApprove(illegal));
        }
    }

    @Test
    public void testAssertCanWithdrawLegalAndIllegal() {
        sm.assertCanWithdraw(ErpSalDocStatus.APPROVE_STATUS_SUBMITTED);
        assertEquals(ErpSalDocStatus.APPROVE_STATUS_UNSUBMITTED, sm.withdrawTargetStatus());
        for (String illegal : Arrays.asList(ErpSalDocStatus.APPROVE_STATUS_UNSUBMITTED,
                ErpSalDocStatus.APPROVE_STATUS_APPROVED, ErpSalDocStatus.APPROVE_STATUS_REJECTED)) {
            assertThrows(NopException.class, () -> sm.assertCanWithdraw(illegal));
        }
    }

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpSalReturnApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
            invokeAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()));
        }
    }

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpSalDocStatus.APPROVE_STATUS_UNSUBMITTED), sm.initialStatuses());
        assertEquals(Arrays.asList(ErpSalDocStatus.APPROVE_STATUS_APPROVED), sm.terminalStatuses());
        assertTrue(sm.isTerminal(ErpSalDocStatus.APPROVE_STATUS_APPROVED));
        assertFalse(sm.isTerminal(ErpSalDocStatus.APPROVE_STATUS_UNSUBMITTED));
        assertFalse(sm.isTerminal(ErpSalDocStatus.APPROVE_STATUS_SUBMITTED));
        assertFalse(sm.isTerminal(ErpSalDocStatus.APPROVE_STATUS_REJECTED));
    }

    @Test
    public void testApprovedIsReversibleTerminal() {
        boolean approvedHasOutgoing = false;
        for (ErpSalReturnApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
            if (e.getFromStatus().equals(ErpSalDocStatus.APPROVE_STATUS_APPROVED)) {
                approvedHasOutgoing = true;
                assertEquals("reverseApprove", e.getAction());
                assertEquals(ErpSalDocStatus.APPROVE_STATUS_REJECTED, e.getToStatus());
            }
        }
        assertTrue(approvedHasOutgoing);
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
            for (ErpSalReturnApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
                if (e.getFromStatus().equals(cur) && !visited.contains(e.getToStatus())) {
                    frontier.add(e.getToStatus());
                }
            }
        }
        visited.remove(start);
        return visited;
    }
}
