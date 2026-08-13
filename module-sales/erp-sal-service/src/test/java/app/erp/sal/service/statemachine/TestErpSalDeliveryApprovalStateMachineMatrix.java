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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-13-1950-2 Phase 1 Proof）。
 *
 * <p>针对 {@link ErpSalDeliveryApprovalStateMachine} Bean 的纯矩阵完备性遍历：不经 BizModel 入口（层 3 职责），
 * 不断言副作用/审计。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边；</li>
 *   <li>(b) 从 UNSUBMITTED 可达 SUBMITTED/APPROVED/REJECTED，REJECTED 经 submit 重提可达 SUBMITTED；</li>
 *   <li>(c) 各 {@code assertCanXxx} 合法来源态通过、非法来源态抛 common 码携带 {@code action}/{@code fromStatus}；</li>
 *   <li>(d) {@code transitions()} 与显式方法语义一致；</li>
 *   <li>(e) 初始/终态集合正确（APPROVED 为可逆业务终态，经 reverseApprove 有出边）。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpSalDeliveryApprovalStateMachineMatrix {

    private static final List<String> ALL_APPROVE_STATUSES = Arrays.asList(
            ErpSalDocStatus.APPROVE_STATUS_UNSUBMITTED,
            ErpSalDocStatus.APPROVE_STATUS_SUBMITTED,
            ErpSalDocStatus.APPROVE_STATUS_APPROVED,
            ErpSalDocStatus.APPROVE_STATUS_REJECTED);

    private final ErpSalDeliveryApprovalStateMachine sm = new ErpSalDeliveryApprovalStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpSalDeliveryApprovalStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpSalDeliveryApprovalStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(6, edges.size(), "迁移矩阵应有 6 条边（submit×2 + approve + reject + reverseApprove + withdraw）");
    }

    // ---------- (b) 从 UNSUBMITTED 可达全部声明状态 ----------

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpSalDocStatus.APPROVE_STATUS_UNSUBMITTED);
        assertTrue(reachable.contains(ErpSalDocStatus.APPROVE_STATUS_SUBMITTED), "从 UNSUBMITTED 应可达 SUBMITTED");
        assertTrue(reachable.contains(ErpSalDocStatus.APPROVE_STATUS_APPROVED), "从 UNSUBMITTED 应可达 APPROVED");
        assertTrue(reachable.contains(ErpSalDocStatus.APPROVE_STATUS_REJECTED), "从 UNSUBMITTED 应可达 REJECTED");
    }

    @Test
    public void testRejectedCanResubmitToSubmitted() {
        Set<String> reachable = reachableFrom(ErpSalDocStatus.APPROVE_STATUS_REJECTED);
        assertTrue(reachable.contains(ErpSalDocStatus.APPROVE_STATUS_SUBMITTED), "REJECTED 经 submit 重提应可达 SUBMITTED");
    }

    // ---------- (c) assertCanXxx 合法/非法 ----------

    @Test
    public void testAssertCanSubmitLegalAndIllegal() {
        sm.assertCanSubmit(ErpSalDocStatus.APPROVE_STATUS_UNSUBMITTED);
        sm.assertCanSubmit(null);
        sm.assertCanSubmit(ErpSalDocStatus.APPROVE_STATUS_REJECTED);
        assertEquals(ErpSalDocStatus.APPROVE_STATUS_SUBMITTED, sm.submitTargetStatus());
        for (String illegal : Arrays.asList(ErpSalDocStatus.APPROVE_STATUS_SUBMITTED, ErpSalDocStatus.APPROVE_STATUS_APPROVED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanSubmit(illegal));
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
            assertEquals("submit", ex.getParam(ErpSalDeliveryApprovalStateMachine.ARG_ACTION));
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
            assertEquals("approve", ex.getParam(ErpSalDeliveryApprovalStateMachine.ARG_ACTION));
            assertEquals(illegal, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
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

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpSalDeliveryApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
            invokeAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (e) 初始/终态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpSalDocStatus.APPROVE_STATUS_UNSUBMITTED), sm.initialStatuses(),
                "初始态集合 = {UNSUBMITTED}");
        assertEquals(Arrays.asList(ErpSalDocStatus.APPROVE_STATUS_APPROVED), sm.terminalStatuses(),
                "业务终态集合 = {APPROVED}");

        assertTrue(sm.isTerminal(ErpSalDocStatus.APPROVE_STATUS_APPROVED));
        assertFalse(sm.isTerminal(ErpSalDocStatus.APPROVE_STATUS_UNSUBMITTED));
        assertFalse(sm.isTerminal(ErpSalDocStatus.APPROVE_STATUS_SUBMITTED));
        assertFalse(sm.isTerminal(ErpSalDocStatus.APPROVE_STATUS_REJECTED));
    }

    @Test
    public void testApprovedIsReversibleTerminal() {
        boolean approvedHasOutgoing = false;
        for (ErpSalDeliveryApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
            if (e.getFromStatus().equals(ErpSalDocStatus.APPROVE_STATUS_APPROVED)) {
                approvedHasOutgoing = true;
                assertEquals("reverseApprove", e.getAction(), "APPROVED 的唯一出边应为 reverseApprove");
                assertEquals(ErpSalDocStatus.APPROVE_STATUS_REJECTED, e.getToStatus());
            }
        }
        assertTrue(approvedHasOutgoing, "APPROVED 应有 reverseApprove 出边（可逆终态）");
    }

    // ---------- helpers ----------

    private void invokeAssert(String action, String approveStatus) {
        switch (action) {
            case "submit":
                sm.assertCanSubmit(approveStatus);
                break;
            case "approve":
                sm.assertCanApprove(approveStatus);
                break;
            case "reject":
                sm.assertCanReject(approveStatus);
                break;
            case "reverseApprove":
                sm.assertCanReverseApprove(approveStatus);
                break;
            case "withdraw":
                sm.assertCanWithdraw(approveStatus);
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "submit":
                return sm.submitTargetStatus();
            case "approve":
                return sm.approveTargetStatus();
            case "reject":
                return sm.rejectTargetStatus();
            case "reverseApprove":
                return sm.reverseApproveTargetStatus();
            case "withdraw":
                return sm.withdrawTargetStatus();
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private Set<String> reachableFrom(String start) {
        Set<String> visited = new java.util.LinkedHashSet<>();
        List<String> frontier = new java.util.ArrayList<>();
        frontier.add(start);
        while (!frontier.isEmpty()) {
            String cur = frontier.remove(0);
            if (!visited.add(cur)) {
                continue;
            }
            for (ErpSalDeliveryApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
                if (e.getFromStatus().equals(cur) && !visited.contains(e.getToStatus())) {
                    frontier.add(e.getToStatus());
                }
            }
        }
        visited.remove(start);
        return visited;
    }
}
