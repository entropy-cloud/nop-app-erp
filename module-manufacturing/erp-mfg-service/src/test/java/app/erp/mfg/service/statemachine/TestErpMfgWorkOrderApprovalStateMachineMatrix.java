package app.erp.mfg.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.mfg.service.ErpMfgConstants;
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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-14-0930-1 Phase 1 Proof）。
 *
 * <p>针对 {@link ErpMfgWorkOrderApprovalStateMachine} Bean（M4.36 approveStatus 审批轴）的纯矩阵完备性遍历：
 * 不经 BizModel/Processor 入口（层 3 职责），不断言副作用/审计。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边（6 边唯一 action|fromStatus 键）；</li>
 *   <li>(b) 从 UNSUBMITTED 可达 SUBMITTED/APPROVED/REJECTED，REJECTED 经 submit 重提可达 SUBMITTED；</li>
 *   <li>(c) 各 {@code assertCanXxx} 合法来源态通过（submit 含 null 归一）、非法来源态抛 common 码携带 {@code action}/{@code fromStatus}；</li>
 *   <li>(d) {@code transitions()}（6 边）与显式方法语义一致；</li>
 *   <li>(e) 初始/终态集合正确（APPROVED 为可逆业务终态，经 reverseApprove 有出边；reverseApprove 目标态=REJECTED）。</li>
 * </ul>
 *
 * <p>层 2 四方对照（WorkOrder 审批轴单条）：dict {@code wf/approve-status} ↔
 * {@code docs/design/manufacturing/state-machine.md} §适用对象一 ↔ Bean 元数据 ↔ 全部 writer
 * （5 审批 Processor live + MrpRelease spawn 写 UNSUBMITTED + CRUD 路径 §9.4 选项 c 排除）。
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpMfgWorkOrderApprovalStateMachineMatrix {

    private static final List<String> ALL_APPROVE_STATUSES = Arrays.asList(
            ErpMfgConstants.APPROVE_STATUS_UNSUBMITTED,
            ErpMfgConstants.APPROVE_STATUS_SUBMITTED,
            ErpMfgConstants.APPROVE_STATUS_APPROVED,
            ErpMfgConstants.APPROVE_STATUS_REJECTED);

    private final ErpMfgWorkOrderApprovalStateMachine sm = new ErpMfgWorkOrderApprovalStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpMfgWorkOrderApprovalStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpMfgWorkOrderApprovalStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(6, edges.size(), "迁移矩阵应有 6 条边（submit×2 + approve + reject + reverseApprove + withdraw）");
    }

    // ---------- (b) 从 UNSUBMITTED 可达全部声明状态 ----------

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpMfgConstants.APPROVE_STATUS_UNSUBMITTED);
        assertTrue(reachable.contains(ErpMfgConstants.APPROVE_STATUS_SUBMITTED), "从 UNSUBMITTED 应可达 SUBMITTED");
        assertTrue(reachable.contains(ErpMfgConstants.APPROVE_STATUS_APPROVED), "从 UNSUBMITTED 应可达 APPROVED");
        assertTrue(reachable.contains(ErpMfgConstants.APPROVE_STATUS_REJECTED), "从 UNSUBMITTED 应可达 REJECTED");
    }

    @Test
    public void testRejectedCanResubmitToSubmitted() {
        Set<String> reachable = reachableFrom(ErpMfgConstants.APPROVE_STATUS_REJECTED);
        assertTrue(reachable.contains(ErpMfgConstants.APPROVE_STATUS_SUBMITTED), "REJECTED 经 submit 重提应可达 SUBMITTED");
    }

    // ---------- (c) assertCanXxx 合法/非法 ----------

    @Test
    public void testAssertCanSubmitLegalAndIllegal() {
        sm.assertCanSubmit(ErpMfgConstants.APPROVE_STATUS_UNSUBMITTED);
        sm.assertCanSubmit(null);
        sm.assertCanSubmit(ErpMfgConstants.APPROVE_STATUS_REJECTED);
        assertEquals(ErpMfgConstants.APPROVE_STATUS_SUBMITTED, sm.submitTargetStatus());
        for (String illegal : illegalFor(ALL_APPROVE_STATUSES,
                ErpMfgConstants.APPROVE_STATUS_UNSUBMITTED, ErpMfgConstants.APPROVE_STATUS_REJECTED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanSubmit(illegal),
                    "submit 对非法来源态应抛: " + illegal);
            assertCommonTransitionMetadata(ex, "submit", illegal);
        }
    }

    @Test
    public void testAssertCanApproveLegalAndIllegal() {
        sm.assertCanApprove(ErpMfgConstants.APPROVE_STATUS_SUBMITTED);
        assertEquals(ErpMfgConstants.APPROVE_STATUS_APPROVED, sm.approveTargetStatus());
        for (String illegal : illegalFor(ALL_APPROVE_STATUSES, ErpMfgConstants.APPROVE_STATUS_SUBMITTED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanApprove(illegal),
                    "approve 对非法来源态应抛: " + illegal);
            assertCommonTransitionMetadata(ex, "approve", illegal);
        }
    }

    @Test
    public void testAssertCanRejectLegalAndIllegal() {
        sm.assertCanReject(ErpMfgConstants.APPROVE_STATUS_SUBMITTED);
        assertEquals(ErpMfgConstants.APPROVE_STATUS_REJECTED, sm.rejectTargetStatus());
        for (String illegal : illegalFor(ALL_APPROVE_STATUSES, ErpMfgConstants.APPROVE_STATUS_SUBMITTED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanReject(illegal),
                    "reject 对非法来源态应抛: " + illegal);
            assertCommonTransitionMetadata(ex, "reject", illegal);
        }
    }

    @Test
    public void testAssertCanReverseApproveLegalAndIllegal() {
        sm.assertCanReverseApprove(ErpMfgConstants.APPROVE_STATUS_APPROVED);
        // reverseApprove 目标态=REJECTED（据实保持 WorkOrder 当前行为，覆写已合规 §16.4）
        assertEquals(ErpMfgConstants.APPROVE_STATUS_REJECTED, sm.reverseApproveTargetStatus());
        for (String illegal : illegalFor(ALL_APPROVE_STATUSES, ErpMfgConstants.APPROVE_STATUS_APPROVED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanReverseApprove(illegal),
                    "reverseApprove 对非法来源态应抛: " + illegal);
            assertCommonTransitionMetadata(ex, "reverseApprove", illegal);
        }
    }

    @Test
    public void testAssertCanWithdrawLegalAndIllegal() {
        sm.assertCanWithdraw(ErpMfgConstants.APPROVE_STATUS_SUBMITTED);
        assertEquals(ErpMfgConstants.APPROVE_STATUS_UNSUBMITTED, sm.withdrawTargetStatus());
        for (String illegal : illegalFor(ALL_APPROVE_STATUSES, ErpMfgConstants.APPROVE_STATUS_SUBMITTED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanWithdraw(illegal),
                    "withdraw 对非法来源态应抛: " + illegal);
            assertCommonTransitionMetadata(ex, "withdraw", illegal);
        }
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpMfgWorkOrderApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
            invokeAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (e) 初始/终态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpMfgConstants.APPROVE_STATUS_UNSUBMITTED), sm.initialStatuses(),
                "初始态集合 = {UNSUBMITTED}");
        assertEquals(Arrays.asList(ErpMfgConstants.APPROVE_STATUS_APPROVED), sm.terminalStatuses(),
                "业务终态集合 = {APPROVED}");

        assertTrue(sm.isTerminal(ErpMfgConstants.APPROVE_STATUS_APPROVED));
        assertFalse(sm.isTerminal(ErpMfgConstants.APPROVE_STATUS_UNSUBMITTED));
        assertFalse(sm.isTerminal(ErpMfgConstants.APPROVE_STATUS_SUBMITTED));
        assertFalse(sm.isTerminal(ErpMfgConstants.APPROVE_STATUS_REJECTED));
    }

    /** APPROVED 是「可逆业务终态」——经 reverseApprove 有出边（不适用「终态无出边」强断言）。 */
    @Test
    public void testApprovedIsReversibleTerminal() {
        boolean approvedHasOutgoing = false;
        for (ErpMfgWorkOrderApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
            if (e.getFromStatus().equals(ErpMfgConstants.APPROVE_STATUS_APPROVED)) {
                approvedHasOutgoing = true;
                assertEquals("reverseApprove", e.getAction(), "APPROVED 的唯一出边应为 reverseApprove");
                assertEquals(ErpMfgConstants.APPROVE_STATUS_REJECTED, e.getToStatus());
            }
        }
        assertTrue(approvedHasOutgoing, "APPROVED 应有 reverseApprove 出边（可逆终态）");
    }

    // ---------- helpers ----------

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

    private void assertCommonTransitionMetadata(NopException ex, String action, String status) {
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + status);
        assertEquals(action, ex.getParam(ErpMfgWorkOrderApprovalStateMachine.ARG_ACTION), "拒绝元数据携带动作名");
        assertEquals(status, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS), "拒绝元数据携带当前态");
    }

    private List<String> illegalFor(List<String> all, String... legal) {
        Set<String> legalSet = new HashSet<>(Arrays.asList(legal));
        return all.stream().filter(s -> !legalSet.contains(s)).collect(Collectors.toList());
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
            for (ErpMfgWorkOrderApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
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
