package app.erp.fin.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.fin.service.ErpFinConstants;
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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-13-1146-3 Phase 1 Proof）。
 *
 * <p>针对 {@link ErpFinEmployeeAdvanceApprovalStateMachine} Bean（approveStatus 审批轴）的纯矩阵完备性遍历：
 * 不经 BizModel 入口（层 3 职责），不断言副作用/审计/前置校验。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边（submit×2 + withdraw + approve + reject + reverseApprove = 6 条边、5 命名动作）；</li>
 *   <li>(b) submit（UNSUBMITTED/REJECTED 合法、SUBMITTED/APPROVED 非法）；</li>
 *   <li>(c) withdraw/approve/reject（SUBMITTED 合法、其余非法）；reverseApprove（APPROVED 合法、其余非法）；</li>
 *   <li>(d) 终态 APPROVED/REJECTED 为<b>可逆终态</b>（APPROVED 经 reverseApprove、REJECTED 经 submit 有出边，
 *       不适用「终态无出边」强断言，对齐 ErpFinExpenseClaimApprovalStateMachine/ErpPurOrderApprovalStateMachine 先例）；</li>
 *   <li>(e) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(f) initial/terminal 集合正确；null 归一化为 UNSUBMITTED。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpFinEmployeeAdvanceApprovalStateMachineMatrix {

    private static final List<String> ALL_DICT_STATUSES = Arrays.asList(
            ErpFinConstants.APPROVE_STATUS_UNSUBMITTED,
            ErpFinConstants.APPROVE_STATUS_SUBMITTED,
            ErpFinConstants.APPROVE_STATUS_APPROVED,
            ErpFinConstants.APPROVE_STATUS_REJECTED);

    private final ErpFinEmployeeAdvanceApprovalStateMachine sm = new ErpFinEmployeeAdvanceApprovalStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpFinEmployeeAdvanceApprovalStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpFinEmployeeAdvanceApprovalStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(6, edges.size(), "迁移矩阵应有 6 条边（submit×2 + withdraw + approve + reject + reverseApprove）");
        Set<String> actions = edges.stream().map(ErpFinEmployeeAdvanceApprovalStateMachine.TransitionDefinition::getAction)
                .collect(Collectors.toSet());
        assertEquals(5, actions.size(), "5 命名动作（submit/withdraw/approve/reject/reverseApprove）");
    }

    // ---------- (b) submit：UNSUBMITTED/REJECTED 合法、SUBMITTED/APPROVED 非法 ----------

    @Test
    public void testAssertCanSubmitLegalAndIllegal() {
        sm.assertCanSubmit(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED);
        sm.assertCanSubmit(null);
        sm.assertCanSubmit(ErpFinConstants.APPROVE_STATUS_REJECTED);
        assertEquals(ErpFinConstants.APPROVE_STATUS_SUBMITTED, sm.submitTargetStatus());

        for (String illegal : Arrays.asList(ErpFinConstants.APPROVE_STATUS_SUBMITTED, ErpFinConstants.APPROVE_STATUS_APPROVED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanSubmit(illegal),
                    "submit 对非法来源态应抛异常: " + illegal);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                    "Bean 报告 common 层非法迁移码");
            assertEquals("submit", ex.getParam(ErpFinEmployeeAdvanceApprovalStateMachine.ARG_ACTION),
                    "拒绝元数据携带动作名");
            assertEquals(illegal, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                    "拒绝元数据携带当前态");
        }
    }

    // ---------- (c) withdraw/approve/reject：SUBMITTED 合法、其余非法 ----------

    @Test
    public void testAssertCanWithdrawLegalAndIllegal() {
        sm.assertCanWithdraw(ErpFinConstants.APPROVE_STATUS_SUBMITTED);
        assertEquals(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED, sm.withdrawTargetStatus());
        for (String s : Arrays.asList(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED,
                ErpFinConstants.APPROVE_STATUS_APPROVED, ErpFinConstants.APPROVE_STATUS_REJECTED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanWithdraw(s),
                    "withdraw 对非法来源态应抛异常: " + s);
            assertEquals("withdraw", ex.getParam(ErpFinEmployeeAdvanceApprovalStateMachine.ARG_ACTION));
            assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
        }
    }

    @Test
    public void testAssertCanApproveLegalAndIllegal() {
        sm.assertCanApprove(ErpFinConstants.APPROVE_STATUS_SUBMITTED);
        assertEquals(ErpFinConstants.APPROVE_STATUS_APPROVED, sm.approveTargetStatus());
        for (String s : Arrays.asList(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED,
                ErpFinConstants.APPROVE_STATUS_APPROVED, ErpFinConstants.APPROVE_STATUS_REJECTED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanApprove(s),
                    "approve 对非法来源态应抛异常: " + s);
            assertEquals("approve", ex.getParam(ErpFinEmployeeAdvanceApprovalStateMachine.ARG_ACTION));
            assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
        }
    }

    @Test
    public void testAssertCanRejectLegalAndIllegal() {
        sm.assertCanReject(ErpFinConstants.APPROVE_STATUS_SUBMITTED);
        assertEquals(ErpFinConstants.APPROVE_STATUS_REJECTED, sm.rejectTargetStatus());
        for (String s : Arrays.asList(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED,
                ErpFinConstants.APPROVE_STATUS_APPROVED, ErpFinConstants.APPROVE_STATUS_REJECTED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanReject(s),
                    "reject 对非法来源态应抛异常: " + s);
            assertEquals("reject", ex.getParam(ErpFinEmployeeAdvanceApprovalStateMachine.ARG_ACTION));
            assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
        }
    }

    @Test
    public void testAssertCanReverseApproveLegalAndIllegal() {
        sm.assertCanReverseApprove(ErpFinConstants.APPROVE_STATUS_APPROVED);
        assertEquals(ErpFinConstants.APPROVE_STATUS_REJECTED, sm.reverseApproveTargetStatus(),
                "reverseApprove→REJECTED（已合规 §16.4）");
        for (String s : Arrays.asList(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED,
                ErpFinConstants.APPROVE_STATUS_SUBMITTED, ErpFinConstants.APPROVE_STATUS_REJECTED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanReverseApprove(s),
                    "reverseApprove 对非法来源态应抛异常: " + s);
            assertEquals("reverseApprove", ex.getParam(ErpFinEmployeeAdvanceApprovalStateMachine.ARG_ACTION));
            assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
        }
    }

    // ---------- (d) 可逆终态：APPROVED 经 reverseApprove / REJECTED 经 submit 有出边 ----------

    @Test
    public void testTerminalStatusesAreReversible() {
        // 终态 APPROVED/REJECTED 均为可逆终态（对齐 ExpenseClaim/PurOrder 先例）
        for (String terminal : sm.terminalStatuses()) {
            assertTrue(sm.isTerminal(terminal), "声明终态应 isTerminal=true: " + terminal);
        }
        // APPROVED 的唯一出边 = reverseApprove → REJECTED
        assertTrue(hasOutgoing(ErpFinConstants.APPROVE_STATUS_APPROVED), "APPROVED 经 reverseApprove 有出边（可逆终态）");
        // REJECTED 的唯一出边 = submit → SUBMITTED
        assertTrue(hasOutgoing(ErpFinConstants.APPROVE_STATUS_REJECTED), "REJECTED 经 submit 有出边（重提）");
    }

    // ---------- (e) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpFinEmployeeAdvanceApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
            // 每条边的 fromStatus 对该 action 合法（assert 放行不抛）
            switch (e.getAction()) {
                case "submit":
                    sm.assertCanSubmit(e.getFromStatus());
                    break;
                case "withdraw":
                    sm.assertCanWithdraw(e.getFromStatus());
                    break;
                case "approve":
                    sm.assertCanApprove(e.getFromStatus());
                    break;
                case "reject":
                    sm.assertCanReject(e.getFromStatus());
                    break;
                case "reverseApprove":
                    sm.assertCanReverseApprove(e.getFromStatus());
                    break;
                default:
                    throw new IllegalStateException("未知 action: " + e.getAction());
            }
            // 每条边的 toStatus 与对应 TargetStatus() 一致
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (f) 终态/初始态集合正确 + null 归一化 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpFinConstants.APPROVE_STATUS_APPROVED, ErpFinConstants.APPROVE_STATUS_REJECTED),
                sm.terminalStatuses(), "终态集合 = {APPROVED, REJECTED}");
        assertEquals(Arrays.asList(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED), sm.initialStatuses(),
                "初始态集合 = {UNSUBMITTED}");

        assertTrue(sm.isTerminal(ErpFinConstants.APPROVE_STATUS_APPROVED));
        assertTrue(sm.isTerminal(ErpFinConstants.APPROVE_STATUS_REJECTED));
        assertFalse(sm.isTerminal(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED));
        assertFalse(sm.isTerminal(ErpFinConstants.APPROVE_STATUS_SUBMITTED));
        assertFalse(sm.isTerminal(null), "null 归一化为 UNSUBMITTED（非终态）");
    }

    @Test
    public void testNullNormalizesToUnsubmitted() {
        // null 归一化为 UNSUBMITTED：submit(null) 合法（初始提交）
        sm.assertCanSubmit(null);
        // withdraw/approve/reject/reverseApprove(null=UNSUBMITTED) 非法
        for (String illegalAction : List.of("withdraw", "approve", "reject", "reverseApprove")) {
            NopException ex = assertThrows(NopException.class, () -> {
                if ("withdraw".equals(illegalAction)) {
                    sm.assertCanWithdraw(null);
                } else if ("approve".equals(illegalAction)) {
                    sm.assertCanApprove(null);
                } else if ("reject".equals(illegalAction)) {
                    sm.assertCanReject(null);
                } else if ("reverseApprove".equals(illegalAction)) {
                    sm.assertCanReverseApprove(null);
                } else {
                    throw new IllegalStateException();
                }
            }, "null（=UNSUBMITTED）对 " + illegalAction + " 应非法");
            assertEquals(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                    "拒绝元数据携带归一化后的当前态: action=" + illegalAction);
        }
    }

    // ---------- 可达性：从 UNSUBMITTED 可达全部声明态 ----------

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED);
        assertTrue(reachable.contains(ErpFinConstants.APPROVE_STATUS_SUBMITTED), "从 UNSUBMITTED 应可达 SUBMITTED");
        assertTrue(reachable.contains(ErpFinConstants.APPROVE_STATUS_APPROVED), "从 UNSUBMITTED 应可达 APPROVED");
        assertTrue(reachable.contains(ErpFinConstants.APPROVE_STATUS_REJECTED), "从 UNSUBMITTED 应可达 REJECTED");
    }

    @Test
    public void testRejectedCanResubmitToSubmitted() {
        sm.assertCanSubmit(ErpFinConstants.APPROVE_STATUS_REJECTED); // REJECTED 经 submit 重提合法
        assertEquals(ErpFinConstants.APPROVE_STATUS_SUBMITTED, sm.submitTargetStatus());
    }

    // ---------- helpers ----------

    private String targetStatusFor(String action) {
        if ("submit".equals(action)) {
            return sm.submitTargetStatus();
        }
        if ("withdraw".equals(action)) {
            return sm.withdrawTargetStatus();
        }
        if ("approve".equals(action)) {
            return sm.approveTargetStatus();
        }
        if ("reject".equals(action)) {
            return sm.rejectTargetStatus();
        }
        if ("reverseApprove".equals(action)) {
            return sm.reverseApproveTargetStatus();
        }
        throw new IllegalStateException("未知 action: " + action);
    }

    private boolean hasOutgoing(String status) {
        return sm.transitions().stream().anyMatch(e -> e.getFromStatus().equals(status));
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
            for (ErpFinEmployeeAdvanceApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
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
