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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-13-0945-1 Phase 3 Proof）。
 *
 * <p>针对 {@link ErpPurQuotationApprovalStateMachine} Bean 的纯矩阵完备性遍历（不经 BizModel 入口）。
 * 矩阵与 {@code TestErpPurOrderApprovalStateMachineMatrix} 同构。reverseApprove→REJECTED（据 Phase 1 Decision）。
 */
public class TestErpPurQuotationApprovalStateMachineMatrix {

    private final ErpPurQuotationApprovalStateMachine sm = new ErpPurQuotationApprovalStateMachine();

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpPurQuotationApprovalStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpPurQuotationApprovalStateMachine.TransitionDefinition e : edges) {
            assertTrue(seen.add(e.getAction() + "|" + e.getFromStatus()));
        }
        assertEquals(6, edges.size());
    }

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpPurDocStatus.APPROVE_STATUS_UNSUBMITTED);
        assertTrue(reachable.contains(ErpPurDocStatus.APPROVE_STATUS_SUBMITTED));
        assertTrue(reachable.contains(ErpPurDocStatus.APPROVE_STATUS_APPROVED));
        assertTrue(reachable.contains(ErpPurDocStatus.APPROVE_STATUS_REJECTED));
    }

    @Test
    public void testSubmitAllowsResubmitFromRejected() {
        sm.assertCanSubmit(null);
        sm.assertCanSubmit(ErpPurDocStatus.APPROVE_STATUS_UNSUBMITTED);
        sm.assertCanSubmit(ErpPurDocStatus.APPROVE_STATUS_REJECTED);
        assertEquals(ErpPurDocStatus.APPROVE_STATUS_SUBMITTED, sm.submitTargetStatus());
        NopException ex = assertThrows(NopException.class, () -> sm.assertCanSubmit(ErpPurDocStatus.APPROVE_STATUS_SUBMITTED));
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
        assertEquals("submit", ex.getParam(ErpPurQuotationApprovalStateMachine.ARG_ACTION));
    }

    @Test
    public void testApproveRejectWithdrawFromSubmittedOnly() {
        for (String s : Arrays.asList(ErpPurDocStatus.APPROVE_STATUS_UNSUBMITTED, ErpPurDocStatus.APPROVE_STATUS_APPROVED, ErpPurDocStatus.APPROVE_STATUS_REJECTED)) {
            assertThrows(NopException.class, () -> sm.assertCanApprove(s));
            assertThrows(NopException.class, () -> sm.assertCanReject(s));
            assertThrows(NopException.class, () -> sm.assertCanWithdraw(s));
        }
        sm.assertCanApprove(ErpPurDocStatus.APPROVE_STATUS_SUBMITTED);
        sm.assertCanReject(ErpPurDocStatus.APPROVE_STATUS_SUBMITTED);
        sm.assertCanWithdraw(ErpPurDocStatus.APPROVE_STATUS_SUBMITTED);
        assertEquals(ErpPurDocStatus.APPROVE_STATUS_APPROVED, sm.approveTargetStatus());
        assertEquals(ErpPurDocStatus.APPROVE_STATUS_REJECTED, sm.rejectTargetStatus());
        assertEquals(ErpPurDocStatus.APPROVE_STATUS_UNSUBMITTED, sm.withdrawTargetStatus());
    }

    @Test
    public void testReverseApproveFromApprovedOnly() {
        sm.assertCanReverseApprove(ErpPurDocStatus.APPROVE_STATUS_APPROVED);
        assertEquals(ErpPurDocStatus.APPROVE_STATUS_REJECTED, sm.reverseApproveTargetStatus());
        for (String s : Arrays.asList(ErpPurDocStatus.APPROVE_STATUS_UNSUBMITTED, ErpPurDocStatus.APPROVE_STATUS_SUBMITTED, ErpPurDocStatus.APPROVE_STATUS_REJECTED)) {
            assertThrows(NopException.class, () -> sm.assertCanReverseApprove(s));
        }
    }

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpPurQuotationApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
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
            default: throw new IllegalArgumentException(action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "submit": return sm.submitTargetStatus();
            case "approve": return sm.approveTargetStatus();
            case "reject": return sm.rejectTargetStatus();
            case "reverseApprove": return sm.reverseApproveTargetStatus();
            case "withdraw": return sm.withdrawTargetStatus();
            default: throw new IllegalArgumentException(action);
        }
    }

    private Set<String> reachableFrom(String start) {
        Set<String> visited = new java.util.LinkedHashSet<>();
        List<String> frontier = new java.util.ArrayList<>();
        frontier.add(start);
        while (!frontier.isEmpty()) {
            String cur = frontier.remove(0);
            if (!visited.add(cur)) continue;
            for (ErpPurQuotationApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
                if (e.getFromStatus().equals(cur) && !visited.contains(e.getToStatus())) frontier.add(e.getToStatus());
            }
        }
        visited.remove(start);
        return visited;
    }
}
