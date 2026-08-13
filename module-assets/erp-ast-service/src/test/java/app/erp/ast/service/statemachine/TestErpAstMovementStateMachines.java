package app.erp.ast.service.statemachine;

import app.erp.ast.service.ErpAstConstants;
import app.erp.common.service.ErpCommonErrors;
import io.nop.api.core.exceptions.NopException;
import org.junit.jupiter.api.Test;

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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan Phase 1 Proof）。
 *
 * <p>针对 {@link ErpAstMovementApprovalStateMachine}（approveStatus 5 动作）+ {@link ErpAstMovementDocumentStateMachine}
 * （docStatus 退化轴）双 Bean 的纯矩阵完备性遍历：不经 BizModel 入口（层 3 职责），不断言副作用/审计。
 *
 * <p>Approval 轴覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边（6 条边，5 命名动作——submitForApproval 双源）；</li>
 *   <li>(b) 从 UNSUBMITTED 可达全部 3 非初始态（SUBMITTED/APPROVED/REJECTED）；</li>
 *   <li>(c) submitForApproval 双源（UNSUBMITTED/REJECTED）合法、对其余态非法；单源动作对其余态非法；</li>
 *   <li>(d) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(e) 终态/初始态集合正确（APPROVED 为可逆终态——经 reverseApprove 有出边，不适用「终态无出边」强断言）。</li>
 * </ul>
 *
 * <p>Document 退化轴覆盖：{@code transitions()} 空 + {@code isCancelled(CANCELLED)=true} + ACTIVE 死状态无 writer
 * （transitions 空即无 writer）+ initial/terminal 集合正确。
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpAstMovementStateMachines {

    private static final List<String> ALL_APPROVE_STATUSES = Arrays.asList(
            ErpAstConstants.APPROVE_STATUS_UNSUBMITTED,
            ErpAstConstants.APPROVE_STATUS_SUBMITTED,
            ErpAstConstants.APPROVE_STATUS_APPROVED,
            ErpAstConstants.APPROVE_STATUS_REJECTED);

    private static final List<String> ALL_DOC_STATUSES = Arrays.asList(
            ErpAstConstants.DOC_STATUS_DRAFT,
            ErpAstConstants.DOC_STATUS_ACTIVE,
            ErpAstConstants.DOC_STATUS_CANCELLED);

    private final ErpAstMovementApprovalStateMachine approvalSm = new ErpAstMovementApprovalStateMachine();
    private final ErpAstMovementDocumentStateMachine documentSm = new ErpAstMovementDocumentStateMachine();

    // ==================== Approval 轴 ====================

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void approvalNoDuplicateOrConflictingEdges() {
        List<ErpAstMovementApprovalStateMachine.TransitionDefinition> edges = approvalSm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpAstMovementApprovalStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(6, edges.size(), "迁移矩阵应有 6 条边（5 命名动作，submitForApproval 双源）");
    }

    // ---------- (b) 从 UNSUBMITTED 可达全部 3 非初始态 ----------

    @Test
    public void approvalReachabilityFromInitial() {
        Set<String> reachable = reachableApprovalFrom(ErpAstConstants.APPROVE_STATUS_UNSUBMITTED);
        for (String s : ALL_APPROVE_STATUSES) {
            if (ErpAstConstants.APPROVE_STATUS_UNSUBMITTED.equals(s)) {
                continue;
            }
            assertTrue(reachable.contains(s), "从 UNSUBMITTED 应可达状态: " + s);
        }
    }

    // ---------- (c) submitForApproval 双源合法/其余非法；单源动作对其余态非法 ----------

    @Test
    public void submitForApprovalDualSourceLegal() {
        for (String s : ALL_APPROVE_STATUSES) {
            if (ErpAstConstants.APPROVE_STATUS_UNSUBMITTED.equals(s)
                    || ErpAstConstants.APPROVE_STATUS_REJECTED.equals(s)) {
                approvalSm.assertCanSubmitForApproval(s); // 合法边不抛
                assertEquals(ErpAstConstants.APPROVE_STATUS_SUBMITTED, approvalSm.submitForApprovalTargetStatus());
            } else {
                NopException ex = assertThrows(NopException.class, () -> approvalSm.assertCanSubmitForApproval(s),
                        "submitForApproval 对非双源态应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                        "Bean 报告 common 层非法迁移码");
                assertEquals("submitForApproval", ex.getParam(ErpAstMovementApprovalStateMachine.ARG_ACTION),
                        "拒绝元数据携带动作名");
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS), "拒绝元数据携带当前态");
            }
        }
    }

    @Test
    public void submitForApprovalNullTreatedAsUnsubmitted() {
        approvalSm.assertCanSubmitForApproval(null); // null 归一化为 UNSUBMITTED，合法不抛
    }

    @Test
    public void approveAllowsOnlySubmitted() {
        assertApprovalAllowsOnly("approve", ErpAstConstants.APPROVE_STATUS_SUBMITTED);
    }

    @Test
    public void rejectAllowsOnlySubmitted() {
        assertApprovalAllowsOnly("reject", ErpAstConstants.APPROVE_STATUS_SUBMITTED);
    }

    @Test
    public void reverseApproveAllowsOnlyApproved() {
        assertApprovalAllowsOnly("reverseApprove", ErpAstConstants.APPROVE_STATUS_APPROVED);
    }

    @Test
    public void withdrawApprovalAllowsOnlySubmitted() {
        assertApprovalAllowsOnly("withdrawApproval", ErpAstConstants.APPROVE_STATUS_SUBMITTED);
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void approvalTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpAstMovementApprovalStateMachine.TransitionDefinition e : approvalSm.transitions()) {
            invokeApprovalAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), approvalTargetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (e) 终态/初始态集合正确 ----------

    @Test
    public void approvalTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpAstConstants.APPROVE_STATUS_APPROVED),
                approvalSm.terminalStatuses(), "终态集合 = {APPROVED}");
        assertEquals(Arrays.asList(ErpAstConstants.APPROVE_STATUS_UNSUBMITTED),
                approvalSm.initialStatuses(), "初始态集合 = {UNSUBMITTED}");

        assertTrue(approvalSm.isTerminal(ErpAstConstants.APPROVE_STATUS_APPROVED));
        assertFalse(approvalSm.isTerminal(ErpAstConstants.APPROVE_STATUS_UNSUBMITTED));
        assertFalse(approvalSm.isTerminal(ErpAstConstants.APPROVE_STATUS_SUBMITTED));
        assertFalse(approvalSm.isTerminal(ErpAstConstants.APPROVE_STATUS_REJECTED),
                "REJECTED 非终态（经 submitForApproval 可重新提交）");
    }

    /**
     * APPROVED 为可逆终态——经 reverseApprove 有出边，故不适用「终态无出边」强断言。
     * 仅断言：APPROVED 在 transitions() 中存在出边（reverseApprove），如实反映可逆终态语义。
     */
    @Test
    public void approvedIsReversibleTerminal() {
        boolean hasOutgoing = approvalSm.transitions().stream()
                .anyMatch(e -> ErpAstConstants.APPROVE_STATUS_APPROVED.equals(e.getFromStatus()));
        assertTrue(hasOutgoing, "APPROVED 经 reverseApprove 应有出边（可逆终态）");
        assertEquals("reverseApprove",
                approvalSm.transitions().stream()
                        .filter(e -> ErpAstConstants.APPROVE_STATUS_APPROVED.equals(e.getFromStatus()))
                        .findFirst().get().getAction(),
                "APPROVED 的唯一出边动作应为 reverseApprove");
    }

    @Test
    public void approvalTargetStatusMethods() {
        assertEquals(ErpAstConstants.APPROVE_STATUS_SUBMITTED, approvalSm.submitForApprovalTargetStatus());
        assertEquals(ErpAstConstants.APPROVE_STATUS_APPROVED, approvalSm.approveTargetStatus());
        assertEquals(ErpAstConstants.APPROVE_STATUS_REJECTED, approvalSm.rejectTargetStatus());
        assertEquals(ErpAstConstants.APPROVE_STATUS_REJECTED, approvalSm.reverseApproveTargetStatus(),
                "reverseApprove 目标态=REJECTED");
        assertEquals(ErpAstConstants.APPROVE_STATUS_UNSUBMITTED, approvalSm.withdrawApprovalTargetStatus());
    }

    // ==================== Document 退化轴 ====================

    @Test
    public void documentTransitionsEmpty() {
        assertTrue(documentSm.transitions().isEmpty(), "退化轴 transitions() 应为空（零命名动作 writer）");
    }

    @Test
    public void documentIsCancelledGuard() {
        assertTrue(documentSm.isCancelled(ErpAstConstants.DOC_STATUS_CANCELLED),
                "CANCELLED 应识别为已作废");
        assertFalse(documentSm.isCancelled(ErpAstConstants.DOC_STATUS_DRAFT));
        assertFalse(documentSm.isCancelled(ErpAstConstants.DOC_STATUS_ACTIVE),
                "ACTIVE 非作废（死状态，保留为预留语义入口）");
    }

    @Test
    public void documentDeadStateActiveHasNoWriter() {
        // ACTIVE 死状态：不在 transitions() 任一迁移边（transitions 空），无命名动作 writer 可达 ACTIVE
        boolean activeReachable = documentSm.transitions().stream()
                .anyMatch(e -> ErpAstConstants.DOC_STATUS_ACTIVE.equals(e.getToStatus()));
        assertFalse(activeReachable, "ACTIVE 为死状态，无命名动作 writer 可达");
    }

    @Test
    public void documentTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpAstConstants.DOC_STATUS_CANCELLED),
                documentSm.terminalStatuses(), "终态集合 = {CANCELLED}");
        assertEquals(Arrays.asList(ErpAstConstants.DOC_STATUS_DRAFT),
                documentSm.initialStatuses(), "初始态集合 = {DRAFT}");

        assertTrue(documentSm.isTerminal(ErpAstConstants.DOC_STATUS_CANCELLED));
        assertFalse(documentSm.isTerminal(ErpAstConstants.DOC_STATUS_DRAFT));
        assertFalse(documentSm.isTerminal(ErpAstConstants.DOC_STATUS_ACTIVE),
                "ACTIVE 死状态非真正终态");
    }

    @Test
    public void documentAllStatusesCovered() {
        // 机器化核对：dict 三个值在 Bean 中均有语义归类
        for (String s : ALL_DOC_STATUSES) {
            documentSm.isCancelled(s); // 不抛即可
            documentSm.isTerminal(s);
        }
    }

    // ==================== helpers ====================

    private void assertApprovalAllowsOnly(String action, String allowedFrom) {
        for (String s : ALL_APPROVE_STATUSES) {
            if (allowedFrom.equals(s)) {
                invokeApprovalAssert(action, s); // 不抛 = 合法
            } else {
                NopException ex = assertThrows(NopException.class, () -> invokeApprovalAssert(action, s),
                        action + " 对非允许来源态应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                        "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + s);
                assertEquals(action, ex.getParam(ErpAstMovementApprovalStateMachine.ARG_ACTION),
                        "拒绝元数据携带动作名: action=" + action);
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                        "拒绝元数据携带当前态: action=" + action + ", status=" + s);
            }
        }
    }

    private void invokeApprovalAssert(String action, String status) {
        switch (action) {
            case "submitForApproval":
                approvalSm.assertCanSubmitForApproval(status);
                break;
            case "approve":
                approvalSm.assertCanApprove(status);
                break;
            case "reject":
                approvalSm.assertCanReject(status);
                break;
            case "reverseApprove":
                approvalSm.assertCanReverseApprove(status);
                break;
            case "withdrawApproval":
                approvalSm.assertCanWithdrawApproval(status);
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String approvalTargetStatusFor(String action) {
        switch (action) {
            case "submitForApproval":
                return approvalSm.submitForApprovalTargetStatus();
            case "approve":
                return approvalSm.approveTargetStatus();
            case "reject":
                return approvalSm.rejectTargetStatus();
            case "reverseApprove":
                return approvalSm.reverseApproveTargetStatus();
            case "withdrawApproval":
                return approvalSm.withdrawApprovalTargetStatus();
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private Set<String> reachableApprovalFrom(String start) {
        Set<String> visited = new LinkedHashSet<>();
        List<String> frontier = new java.util.ArrayList<>();
        frontier.add(start);
        while (!frontier.isEmpty()) {
            String cur = frontier.remove(0);
            if (!visited.add(cur)) {
                continue;
            }
            for (ErpAstMovementApprovalStateMachine.TransitionDefinition e : approvalSm.transitions()) {
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
