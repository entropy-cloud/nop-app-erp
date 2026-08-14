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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-14-1931-2 Phase 3 Proof，M4.43）。
 *
 * <p>针对 {@link ErpAstValueAdjustmentApprovalStateMachine}（approveStatus 5 动作审批轴）的纯矩阵完备性遍历：
 * 不经 BizModel 入口（层 3 职责），不断言副作用/审计。
 *
 * <p>覆盖：无重复/冲突边（6 边）→ 从 UNSUBMITTED 可达 3 非初始态 → submitForApproval 双源 + 单源动作合法性 →
 * transitions() 元数据与显式方法一致 → 终态/初始态集合（APPROVED 可逆终态）。
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpAstValueAdjustmentApprovalStateMachineMatrix {

    private static final List<String> ALL_APPROVE_STATUSES = Arrays.asList(
            ErpAstConstants.APPROVE_STATUS_UNSUBMITTED,
            ErpAstConstants.APPROVE_STATUS_SUBMITTED,
            ErpAstConstants.APPROVE_STATUS_APPROVED,
            ErpAstConstants.APPROVE_STATUS_REJECTED);

    private final ErpAstValueAdjustmentApprovalStateMachine approvalSm =
            new ErpAstValueAdjustmentApprovalStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void noDuplicateOrConflictingEdges() {
        List<ErpAstValueAdjustmentApprovalStateMachine.TransitionDefinition> edges = approvalSm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpAstValueAdjustmentApprovalStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(6, edges.size(), "迁移矩阵应有 6 条边（5 命名动作，submitForApproval 双源）");
    }

    // ---------- (b) 从 UNSUBMITTED 可达全部 3 非初始态 ----------

    @Test
    public void reachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpAstConstants.APPROVE_STATUS_UNSUBMITTED);
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
                assertEquals("submitForApproval", ex.getParam(ErpAstValueAdjustmentApprovalStateMachine.ARG_ACTION),
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
        assertAllowsOnly("approve", ErpAstConstants.APPROVE_STATUS_SUBMITTED);
    }

    @Test
    public void rejectAllowsOnlySubmitted() {
        assertAllowsOnly("reject", ErpAstConstants.APPROVE_STATUS_SUBMITTED);
    }

    @Test
    public void reverseApproveAllowsOnlyApproved() {
        assertAllowsOnly("reverseApprove", ErpAstConstants.APPROVE_STATUS_APPROVED);
    }

    @Test
    public void withdrawApprovalAllowsOnlySubmitted() {
        assertAllowsOnly("withdrawApproval", ErpAstConstants.APPROVE_STATUS_SUBMITTED);
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void transitionsMetadataConsistentWithExplicitMethods() {
        for (ErpAstValueAdjustmentApprovalStateMachine.TransitionDefinition e : approvalSm.transitions()) {
            invokeAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (e) 终态/初始态集合正确 ----------

    @Test
    public void terminalAndInitialSets() {
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
    public void targetStatusMethods() {
        assertEquals(ErpAstConstants.APPROVE_STATUS_SUBMITTED, approvalSm.submitForApprovalTargetStatus());
        assertEquals(ErpAstConstants.APPROVE_STATUS_APPROVED, approvalSm.approveTargetStatus());
        assertEquals(ErpAstConstants.APPROVE_STATUS_REJECTED, approvalSm.rejectTargetStatus());
        assertEquals(ErpAstConstants.APPROVE_STATUS_REJECTED, approvalSm.reverseApproveTargetStatus(),
                "reverseApprove 目标态=REJECTED（对齐 §16.4）");
        assertEquals(ErpAstConstants.APPROVE_STATUS_UNSUBMITTED, approvalSm.withdrawApprovalTargetStatus());
    }

    // ==================== helpers ====================

    private void assertAllowsOnly(String action, String allowedFrom) {
        for (String s : ALL_APPROVE_STATUSES) {
            if (allowedFrom.equals(s)) {
                invokeAssert(action, s); // 不抛 = 合法
            } else {
                NopException ex = assertThrows(NopException.class, () -> invokeAssert(action, s),
                        action + " 对非允许来源态应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                        "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + s);
                assertEquals(action, ex.getParam(ErpAstValueAdjustmentApprovalStateMachine.ARG_ACTION),
                        "拒绝元数据携带动作名: action=" + action);
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                        "拒绝元数据携带当前态: action=" + action + ", status=" + s);
            }
        }
    }

    private void invokeAssert(String action, String status) {
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

    private String targetStatusFor(String action) {
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

    private Set<String> reachableFrom(String start) {
        Set<String> visited = new LinkedHashSet<>();
        List<String> frontier = new java.util.ArrayList<>();
        frontier.add(start);
        while (!frontier.isEmpty()) {
            String cur = frontier.remove(0);
            if (!visited.add(cur)) {
                continue;
            }
            for (ErpAstValueAdjustmentApprovalStateMachine.TransitionDefinition e : approvalSm.transitions()) {
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
