package app.erp.prj.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.prj.service.ErpPrjConstants;
import io.nop.api.core.exceptions.NopException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan Phase 1 Proof）。
 *
 * <p>针对三 Bean 的纯矩阵完备性遍历：{@link ErpPrjTimesheetStateMachine}（工时 status 单轴）+
 * {@link ErpPrjProjectSettlementApprovalStateMachine}（结算 approveStatus 轴）+
 * {@link ErpPrjProjectSettlementDocumentStateMachine}（结算 docStatus 轴）。不经 BizModel 入口（层 3 职责），
 * 不断言副作用/审计/过账。
 *
 * <p>每轴覆盖：(a) 无重复/冲突边；(b) 从初始态可达性；(c) 各动作合法/非法来源态；(d) {@code transitions()} 元数据
 * 与显式方法语义一致；(e) 终态/初始态集合正确。Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpPrjTimesheetAndSettlementStateMachines {

    // wf/approve-status 字典四态（工时 status 与结算 approveStatus 共享）
    private static final List<String> ALL_APPROVE_STATUSES = Arrays.asList(
            ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED,
            ErpPrjConstants.APPROVE_STATUS_SUBMITTED,
            ErpPrjConstants.APPROVE_STATUS_APPROVED,
            ErpPrjConstants.APPROVE_STATUS_REJECTED);

    // 结算 docStatus 实际使用态（APPROVED 被写入但不在 erp-prj/project-status 字典内——dict-value drift）
    private static final List<String> SETTLEMENT_DOC_STATUSES = Arrays.asList(
            ErpPrjConstants.DOC_STATUS_DRAFT,
            ErpPrjConstants.DOC_STATUS_APPROVED,
            ErpPrjConstants.DOC_STATUS_CANCELLED);

    // erp-prj/project-status 字典死状态（对结算无 writer）
    private static final List<String> DOC_DEAD_STATUSES = Arrays.asList(
            ErpPrjConstants.PROJECT_STATUS_OPEN,
            ErpPrjConstants.PROJECT_STATUS_ON_HOLD,
            ErpPrjConstants.PROJECT_STATUS_COMPLETED);

    private final ErpPrjTimesheetStateMachine timesheetSm = new ErpPrjTimesheetStateMachine();
    private final ErpPrjProjectSettlementApprovalStateMachine approvalSm = new ErpPrjProjectSettlementApprovalStateMachine();
    private final ErpPrjProjectSettlementDocumentStateMachine documentSm = new ErpPrjProjectSettlementDocumentStateMachine();

    // ==================== 工时 status 轴 ====================

    @Test
    public void timesheetNoDuplicateOrConflictingEdges() {
        List<ErpPrjTimesheetStateMachine.TransitionDefinition> edges = timesheetSm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpPrjTimesheetStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(5, edges.size(), "迁移矩阵应有 5 条边（submit/approve/reject 各 1 + cancel 2 来源）");
    }

    @Test
    public void timesheetReachabilityFromInitial() {
        Set<String> reachable = reachableTimesheetFrom(ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED);
        // 从 UNSUBMITTED 可达 SUBMITTED 与 APPROVED（REJECTED 为死状态——无 writer 产生，不可达）
        assertTrue(reachable.contains(ErpPrjConstants.APPROVE_STATUS_SUBMITTED), "从 UNSUBMITTED 应可达 SUBMITTED");
        assertTrue(reachable.contains(ErpPrjConstants.APPROVE_STATUS_APPROVED), "从 UNSUBMITTED 应可达 APPROVED");
    }

    @Test
    public void timesheetSubmitAllowsOnlyUnsubmitted() {
        for (String s : ALL_APPROVE_STATUSES) {
            if (ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED.equals(s)) {
                timesheetSm.assertCanSubmit(s); // 合法边不抛
            } else {
                NopException ex = assertThrows(NopException.class, () -> timesheetSm.assertCanSubmit(s),
                        "submit 对非 UNSUBMITTED 应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
                assertEquals("submit", ex.getParam(ErpPrjTimesheetStateMachine.ARG_ACTION));
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
            }
        }
    }

    @Test
    public void timesheetSubmitNullTreatedAsUnsubmitted() {
        timesheetSm.assertCanSubmit(null); // null 归一化为 UNSUBMITTED，合法不抛
    }

    @Test
    public void timesheetApproveAllowsOnlySubmitted() {
        assertTimesheetAllowsOnly("approve", ErpPrjConstants.APPROVE_STATUS_SUBMITTED);
    }

    @Test
    public void timesheetRejectAllowsOnlySubmitted() {
        assertTimesheetAllowsOnly("reject", ErpPrjConstants.APPROVE_STATUS_SUBMITTED);
    }

    /**
     * cancel 撤回语义：基线对全部 dict 值放行（行为保持——既有 CancelProcessor 对状态不抛）。
     * APPROVED 为可逆终态（经 cancel 有出边）。
     */
    @Test
    public void timesheetCancelAllowsAllStatuses() {
        for (String s : ALL_APPROVE_STATUSES) {
            assertDoesNotThrow(() -> timesheetSm.assertCanCancel(s), "cancel 基线对所有状态放行: " + s);
        }
        timesheetSm.assertCanCancel(null); // null 同样放行（行为保持）
    }

    @Test
    public void timesheetApprovedIsReversibleTerminal() {
        // APPROVED 为可逆终态——经 cancel 有出边
        boolean hasOutgoing = timesheetSm.transitions().stream()
                .anyMatch(e -> ErpPrjConstants.APPROVE_STATUS_APPROVED.equals(e.getFromStatus()));
        assertTrue(hasOutgoing, "APPROVED 经 cancel 应有出边（可逆终态）");
        assertEquals("cancel",
                timesheetSm.transitions().stream()
                        .filter(e -> ErpPrjConstants.APPROVE_STATUS_APPROVED.equals(e.getFromStatus()))
                        .findFirst().get().getAction(),
                "APPROVED 的唯一出边动作应为 cancel");
    }

    @Test
    public void timesheetRejectedIsDeadState() {
        // REJECTED 为 dict 死状态：reject 目标态为 UNSUBMITTED（非 REJECTED），无 writer 产生 REJECTED
        boolean rejectedReachable = timesheetSm.transitions().stream()
                .anyMatch(e -> ErpPrjConstants.APPROVE_STATUS_REJECTED.equals(e.getToStatus()));
        assertFalse(rejectedReachable, "REJECTED 为死状态，无 writer 产生");
    }

    @Test
    public void timesheetTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpPrjTimesheetStateMachine.TransitionDefinition e : timesheetSm.transitions()) {
            invokeTimesheetAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), timesheetTargetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    @Test
    public void timesheetTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpPrjConstants.APPROVE_STATUS_APPROVED),
                timesheetSm.terminalStatuses(), "终态集合 = {APPROVED}");
        assertEquals(Collections.singletonList(ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED),
                timesheetSm.initialStatuses(), "初始态集合 = {UNSUBMITTED}");
        assertTrue(timesheetSm.isTerminal(ErpPrjConstants.APPROVE_STATUS_APPROVED));
        assertFalse(timesheetSm.isTerminal(ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED));
        assertFalse(timesheetSm.isTerminal(ErpPrjConstants.APPROVE_STATUS_SUBMITTED));
        assertFalse(timesheetSm.isTerminal(ErpPrjConstants.APPROVE_STATUS_REJECTED));
    }

    @Test
    public void timesheetTargetStatusMethods() {
        assertEquals(ErpPrjConstants.APPROVE_STATUS_SUBMITTED, timesheetSm.submitTargetStatus());
        assertEquals(ErpPrjConstants.APPROVE_STATUS_APPROVED, timesheetSm.approveTargetStatus());
        assertEquals(ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED, timesheetSm.rejectTargetStatus());
        assertEquals(ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED, timesheetSm.cancelTargetStatus());
    }

    // ==================== 结算 approveStatus 轴 ====================

    @Test
    public void approvalNoDuplicateOrConflictingEdges() {
        List<ErpPrjProjectSettlementApprovalStateMachine.TransitionDefinition> edges = approvalSm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpPrjProjectSettlementApprovalStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(3, edges.size(), "迁移矩阵应有 3 条边（submit/approve/reject 各 1）");
    }

    @Test
    public void approvalReachabilityFromInitial() {
        Set<String> reachable = reachableApprovalFrom(ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED);
        assertTrue(reachable.contains(ErpPrjConstants.APPROVE_STATUS_SUBMITTED), "从 UNSUBMITTED 应可达 SUBMITTED");
        assertTrue(reachable.contains(ErpPrjConstants.APPROVE_STATUS_APPROVED), "从 UNSUBMITTED 应可达 APPROVED");
        assertTrue(reachable.contains(ErpPrjConstants.APPROVE_STATUS_REJECTED), "从 UNSUBMITTED 应可达 REJECTED");
    }

    @Test
    public void approvalSubmitAllowsOnlyUnsubmitted() {
        assertApprovalAllowsOnly("submit", ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED);
    }

    @Test
    public void approvalApproveAllowsOnlySubmitted() {
        assertApprovalAllowsOnly("approve", ErpPrjConstants.APPROVE_STATUS_SUBMITTED);
    }

    @Test
    public void approvalRejectAllowsOnlySubmitted() {
        assertApprovalAllowsOnly("reject", ErpPrjConstants.APPROVE_STATUS_SUBMITTED);
    }

    @Test
    public void approvalTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpPrjProjectSettlementApprovalStateMachine.TransitionDefinition e : approvalSm.transitions()) {
            invokeApprovalAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), approvalTargetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    @Test
    public void approvalTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpPrjConstants.APPROVE_STATUS_APPROVED),
                approvalSm.terminalStatuses(), "终态集合 = {APPROVED}");
        assertEquals(Collections.singletonList(ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED),
                approvalSm.initialStatuses(), "初始态集合 = {UNSUBMITTED}");
    }

    /**
     * APPROVED 为真终态（无 writer 将 approveStatus 从 APPROVED 迁出——cancel 只写 docStatus，
     * reverseSettlement 只写 posted）。适用「终态无出边」强断言。
     */
    @Test
    public void approvalApprovedIsTrueTerminal() {
        for (ErpPrjProjectSettlementApprovalStateMachine.TransitionDefinition e : approvalSm.transitions()) {
            assertFalse(ErpPrjConstants.APPROVE_STATUS_APPROVED.equals(e.getFromStatus()),
                    "APPROVED 真终态不应有出边: but edge " + e.getAction() + " leaves it");
        }
        assertTrue(approvalSm.isTerminal(ErpPrjConstants.APPROVE_STATUS_APPROVED));
    }

    @Test
    public void approvalTargetStatusMethods() {
        assertEquals(ErpPrjConstants.APPROVE_STATUS_SUBMITTED, approvalSm.submitTargetStatus());
        assertEquals(ErpPrjConstants.APPROVE_STATUS_APPROVED, approvalSm.approveTargetStatus());
        assertEquals(ErpPrjConstants.APPROVE_STATUS_REJECTED, approvalSm.rejectTargetStatus());
    }

    // ==================== 结算 docStatus 轴 ====================

    @Test
    public void documentNoDuplicateOrConflictingEdges() {
        List<ErpPrjProjectSettlementDocumentStateMachine.TransitionDefinition> edges = documentSm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpPrjProjectSettlementDocumentStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(3, edges.size(), "迁移矩阵应有 3 条边（approve 1 + cancel 2 来源）");
    }

    @Test
    public void documentReachabilityFromInitial() {
        Set<String> reachable = reachableDocumentFrom(ErpPrjConstants.DOC_STATUS_DRAFT);
        assertTrue(reachable.contains(ErpPrjConstants.DOC_STATUS_APPROVED), "从 DRAFT 应可达 APPROVED");
        assertTrue(reachable.contains(ErpPrjConstants.DOC_STATUS_CANCELLED), "从 DRAFT 应可达 CANCELLED");
    }

    @Test
    public void documentApproveAllowsOnlyDraft() {
        for (String s : SETTLEMENT_DOC_STATUSES) {
            if (ErpPrjConstants.DOC_STATUS_DRAFT.equals(s)) {
                documentSm.assertCanApprove(s); // 合法不抛
            } else {
                NopException ex = assertThrows(NopException.class, () -> documentSm.assertCanApprove(s),
                        "approve 对非 DRAFT 应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
                assertEquals("approve", ex.getParam(ErpPrjProjectSettlementDocumentStateMachine.ARG_ACTION));
            }
        }
    }

    /**
     * cancel 守卫：仅终态 CANCELLED 非法，其余放行（行为保持，对齐 facade validateTransitionForCancel）。
     */
    @Test
    public void documentCancelIllegalOnlyForCancelled() {
        for (String s : SETTLEMENT_DOC_STATUSES) {
            if (ErpPrjConstants.DOC_STATUS_CANCELLED.equals(s)) {
                NopException ex = assertThrows(NopException.class, () -> documentSm.assertCanCancel(s),
                        "cancel 对 CANCELLED 应非法");
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
                assertEquals("cancel", ex.getParam(ErpPrjProjectSettlementDocumentStateMachine.ARG_ACTION));
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
            } else {
                documentSm.assertCanCancel(s); // 合法不抛
            }
        }
    }

    @Test
    public void documentTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpPrjProjectSettlementDocumentStateMachine.TransitionDefinition e : documentSm.transitions()) {
            invokeDocumentAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), documentTargetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    @Test
    public void documentTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpPrjConstants.DOC_STATUS_CANCELLED),
                documentSm.terminalStatuses(), "终态集合 = {CANCELLED}");
        assertEquals(Collections.singletonList(ErpPrjConstants.DOC_STATUS_DRAFT),
                documentSm.initialStatuses(), "初始态集合 = {DRAFT}");
        assertTrue(documentSm.isTerminal(ErpPrjConstants.DOC_STATUS_CANCELLED));
        assertFalse(documentSm.isTerminal(ErpPrjConstants.DOC_STATUS_DRAFT));
        assertFalse(documentSm.isTerminal(ErpPrjConstants.DOC_STATUS_APPROVED),
                "APPROVED 非终态（经 cancel 有出边）");
    }

    /**
     * CANCELLED 真终态无出边。APPROVED 非终态（经 cancel 有出边）。
     */
    @Test
    public void documentCancelledIsTrueTerminal() {
        for (ErpPrjProjectSettlementDocumentStateMachine.TransitionDefinition e : documentSm.transitions()) {
            assertFalse(ErpPrjConstants.DOC_STATUS_CANCELLED.equals(e.getFromStatus()),
                    "CANCELLED 真终态不应有出边: but edge " + e.getAction() + " leaves it");
        }
    }

    @Test
    public void documentTargetStatusMethods() {
        assertEquals(ErpPrjConstants.DOC_STATUS_APPROVED, documentSm.approveTargetStatus());
        assertEquals(ErpPrjConstants.DOC_STATUS_CANCELLED, documentSm.cancelTargetStatus());
    }

    /**
     * 共享 dict 死状态核对：erp-prj/project-status 的 OPEN/ON_HOLD/COMPLETED 对结算单无 writer
     * （不在任一迁移边的 toStatus）。保留为预留语义入口（Phase 3 Decision）。
     */
    @Test
    public void documentSharedDictDeadStatusesHaveNoWriter() {
        for (String dead : DOC_DEAD_STATUSES) {
            boolean reachable = documentSm.transitions().stream()
                    .anyMatch(e -> dead.equals(e.getToStatus()));
            assertFalse(reachable, "共享 dict 死状态无 writer 可达: " + dead);
        }
    }

    /**
     * APPROVED dict-value drift 核对：APPROVED 被 doApprove 写入（在 approve 边的 toStatus），
     * 虽不在 erp-prj/project-status 字典内。Bean 按既有 writer 建模（保持行为）。
     */
    @Test
    public void documentApprovedDictValueDriftModeledAsWritten() {
        boolean approvedWritten = documentSm.transitions().stream()
                .anyMatch(e -> ErpPrjConstants.DOC_STATUS_APPROVED.equals(e.getToStatus()));
        assertTrue(approvedWritten, "APPROVED 被 doApprove 写入（dict-value drift，Bean 按既有 writer 建模）");
    }

    // ==================== helpers ====================

    private void assertTimesheetAllowsOnly(String action, String allowedFrom) {
        for (String s : ALL_APPROVE_STATUSES) {
            if (allowedFrom.equals(s)) {
                invokeTimesheetAssert(action, s); // 不抛 = 合法
            } else {
                NopException ex = assertThrows(NopException.class, () -> invokeTimesheetAssert(action, s),
                        action + " 对非允许来源态应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                        "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + s);
                assertEquals(action, ex.getParam(ErpPrjTimesheetStateMachine.ARG_ACTION));
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
            }
        }
    }

    private void invokeTimesheetAssert(String action, String status) {
        switch (action) {
            case "submit":
                timesheetSm.assertCanSubmit(status);
                break;
            case "approve":
                timesheetSm.assertCanApprove(status);
                break;
            case "reject":
                timesheetSm.assertCanReject(status);
                break;
            case "cancel":
                timesheetSm.assertCanCancel(status);
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String timesheetTargetStatusFor(String action) {
        switch (action) {
            case "submit":
                return timesheetSm.submitTargetStatus();
            case "approve":
                return timesheetSm.approveTargetStatus();
            case "reject":
                return timesheetSm.rejectTargetStatus();
            case "cancel":
                return timesheetSm.cancelTargetStatus();
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private void assertApprovalAllowsOnly(String action, String allowedFrom) {
        for (String s : ALL_APPROVE_STATUSES) {
            if (allowedFrom.equals(s)) {
                invokeApprovalAssert(action, s); // 不抛 = 合法
            } else {
                NopException ex = assertThrows(NopException.class, () -> invokeApprovalAssert(action, s),
                        action + " 对非允许来源态应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                        "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + s);
                assertEquals(action, ex.getParam(ErpPrjProjectSettlementApprovalStateMachine.ARG_ACTION));
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
            }
        }
    }

    private void invokeApprovalAssert(String action, String status) {
        switch (action) {
            case "submit":
                approvalSm.assertCanSubmit(status);
                break;
            case "approve":
                approvalSm.assertCanApprove(status);
                break;
            case "reject":
                approvalSm.assertCanReject(status);
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String approvalTargetStatusFor(String action) {
        switch (action) {
            case "submit":
                return approvalSm.submitTargetStatus();
            case "approve":
                return approvalSm.approveTargetStatus();
            case "reject":
                return approvalSm.rejectTargetStatus();
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private void invokeDocumentAssert(String action, String status) {
        switch (action) {
            case "approve":
                documentSm.assertCanApprove(status);
                break;
            case "cancel":
                documentSm.assertCanCancel(status);
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String documentTargetStatusFor(String action) {
        switch (action) {
            case "approve":
                return documentSm.approveTargetStatus();
            case "cancel":
                return documentSm.cancelTargetStatus();
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private Set<String> reachableTimesheetFrom(String start) {
        return reachableFromPairs(start, timesheetSm.transitions().stream()
                .map(e -> new String[]{e.getFromStatus(), e.getToStatus()})
                .collect(Collectors.toList()));
    }

    private Set<String> reachableApprovalFrom(String start) {
        return reachableFromPairs(start, approvalSm.transitions().stream()
                .map(e -> new String[]{e.getFromStatus(), e.getToStatus()})
                .collect(Collectors.toList()));
    }

    private Set<String> reachableDocumentFrom(String start) {
        return reachableFromPairs(start, documentSm.transitions().stream()
                .map(e -> new String[]{e.getFromStatus(), e.getToStatus()})
                .collect(Collectors.toList()));
    }

    private static Set<String> reachableFromPairs(String start, List<String[]> edges) {
        Set<String> visited = new LinkedHashSet<>();
        List<String> frontier = new java.util.ArrayList<>();
        frontier.add(start);
        while (!frontier.isEmpty()) {
            String cur = frontier.remove(0);
            if (!visited.add(cur)) {
                continue;
            }
            for (String[] e : edges) {
                if (e[0].equals(cur) && !visited.contains(e[1])) {
                    frontier.add(e[1]);
                }
            }
        }
        return visited.stream()
                .filter(s -> !s.equals(start))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
