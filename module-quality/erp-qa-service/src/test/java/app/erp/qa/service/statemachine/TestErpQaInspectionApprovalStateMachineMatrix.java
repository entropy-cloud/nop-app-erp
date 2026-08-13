package app.erp.qa.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.qa.service.ErpQaConstants;
import io.nop.api.core.exceptions.NopException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-14-0930-2 Phase 1 Proof）。
 *
 * <p>针对 {@link ErpQaInspectionApprovalStateMachine} Bean（让步审批轴，concession-approve 单边）的矩阵完备性遍历。
 * M4.59 裁定：质检单 approveStatus 仅有让步审批单边 writer（CONDITIONAL 时写 APPROVED），非完整 5 动作生命周期。
 *
 * <p>覆盖：
 * <ul>
 *   <li>(a) 无重复边（单边 concessionApprove UNSUBMITTED→APPROVED）；</li>
 *   <li>(b) 从 UNSUBMITTED 可达 APPROVED；</li>
 *   <li>(c) {@code assertCanConcessionApprove} 合法来源态通过、非法来源态抛 common 码携带 {@code action}/{@code fromStatus}；</li>
 *   <li>(d) {@code transitions()} 与显式方法语义一致；</li>
 *   <li>(e) 初始/终态集合正确（APPROVED 为终态，让步审批无 reverse，无出边）。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpQaInspectionApprovalStateMachineMatrix {

    private final ErpQaInspectionApprovalStateMachine sm = new ErpQaInspectionApprovalStateMachine();

    // ---------- (a) 无重复边 ----------

    @Test
    public void testNoDuplicateEdges() {
        Set<String> seen = new HashSet<>();
        for (ErpQaInspectionApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
            String key = e.getAction() + "|" + e.getFromStatus() + "|" + e.getToStatus();
            assertTrue(seen.add(key), "重复边: " + key);
        }
        assertEquals(1, sm.transitions().size(), "让步审批矩阵应有 1 条边（concessionApprove UNSUBMITTED→APPROVED）");
    }

    // ---------- (b) 从 UNSUBMITTED 可达 APPROVED ----------

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = new java.util.LinkedHashSet<>();
        for (ErpQaInspectionApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
            if (e.getFromStatus().equals(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED)) {
                reachable.add(e.getToStatus());
            }
        }
        assertTrue(reachable.contains(ErpQaConstants.APPROVE_STATUS_APPROVED), "从 UNSUBMITTED 应可达 APPROVED");
    }

    // ---------- (c) assertCanConcessionApprove 合法/非法 ----------

    @Test
    public void testAssertCanConcessionApproveLegalAndIllegal() {
        sm.assertCanConcessionApprove(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED);
        sm.assertCanConcessionApprove(null);
        assertEquals(ErpQaConstants.APPROVE_STATUS_APPROVED, sm.concessionApproveTargetStatus());
        for (String illegal : Arrays.asList(ErpQaConstants.APPROVE_STATUS_SUBMITTED,
                ErpQaConstants.APPROVE_STATUS_APPROVED, ErpQaConstants.APPROVE_STATUS_REJECTED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanConcessionApprove(illegal));
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
            assertEquals("concessionApprove", ex.getParam(ErpQaInspectionApprovalStateMachine.ARG_ACTION));
            assertEquals(illegal, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
        }
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpQaInspectionApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
            sm.assertCanConcessionApprove(e.getFromStatus());
            assertEquals(sm.concessionApproveTargetStatus(), e.getToStatus());
        }
    }

    // ---------- (e) 初始/终态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED), sm.initialStatuses(),
                "初始态集合 = {UNSUBMITTED}");
        assertEquals(Arrays.asList(ErpQaConstants.APPROVE_STATUS_APPROVED), sm.terminalStatuses(),
                "终态集合 = {APPROVED}");

        assertTrue(sm.isTerminal(ErpQaConstants.APPROVE_STATUS_APPROVED));
        assertFalse(sm.isTerminal(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED));
    }

    /** APPROVED 终态无出边（让步审批简化为单边，无 reverse/withdraw writer）。 */
    @Test
    public void testApprovedTerminalHasNoOutgoing() {
        for (ErpQaInspectionApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
            assertFalse(e.getFromStatus().equals(ErpQaConstants.APPROVE_STATUS_APPROVED),
                    "APPROVED 终态不应有出边");
        }
    }
}
