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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-14-0456-1 Phase 2 Proof）。
 *
 * <p>针对 {@link ErpFinBadDebtApprovalStateMachine} Bean（approvalStatus 坏账审批轴）的纯矩阵完备性遍历：
 * 不经 BizModel/facade 入口（层 3 职责），不断言副作用/审计/前置校验。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边（submit + approve×2 + reject×2 + reverseApprove = 6 条边、4 命名动作）；</li>
 *   <li>(b) submit（UNSUBMITTED 合法、SUBMITTED/APPROVED/REJECTED 非法——facade 无 REJECTED 重提路径）；</li>
 *   <li>(c) approve/reject（UNSUBMITTED/SUBMITTED 合法、其余非法）；</li>
 *   <li>(d) reverseApprove（APPROVED 合法、其余非法）；</li>
 *   <li>(e) 终态 APPROVED（可逆终态——经 reverseApprove 有出边）与 REJECTED（不可逆终态——无出边）；</li>
 *   <li>(f) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(g) initial/terminal 集合正确；null 归一化为 UNSUBMITTED。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpFinBadDebtStateMachineMatrix {

    private static final List<String> ALL_DICT_STATUSES = Arrays.asList(
            ErpFinConstants.APPROVE_STATUS_UNSUBMITTED,
            ErpFinConstants.APPROVE_STATUS_SUBMITTED,
            ErpFinConstants.APPROVE_STATUS_APPROVED,
            ErpFinConstants.APPROVE_STATUS_REJECTED);

    private final ErpFinBadDebtApprovalStateMachine sm = new ErpFinBadDebtApprovalStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpFinBadDebtApprovalStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpFinBadDebtApprovalStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(6, edges.size(), "迁移矩阵应有 6 条边（submit + approve×2 + reject×2 + reverseApprove）");
        Set<String> actions = edges.stream().map(ErpFinBadDebtApprovalStateMachine.TransitionDefinition::getAction)
                .collect(Collectors.toSet());
        assertEquals(4, actions.size(), "4 命名动作（submit/approve/reject/reverseApprove）");
    }

    // ---------- (b) submit：UNSUBMITTED 合法、其余非法 ----------

    @Test
    public void testAssertCanSubmitLegalAndIllegal() {
        sm.assertCanSubmit(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED);
        sm.assertCanSubmit(null); // null 归一化为 UNSUBMITTED
        assertEquals(ErpFinConstants.APPROVE_STATUS_SUBMITTED, sm.submitTargetStatus());

        for (String illegal : Arrays.asList(ErpFinConstants.APPROVE_STATUS_SUBMITTED,
                ErpFinConstants.APPROVE_STATUS_APPROVED, ErpFinConstants.APPROVE_STATUS_REJECTED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanSubmit(illegal),
                    "submit 对非法来源态应抛异常: " + illegal);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                    "Bean 报告 common 层非法迁移码");
            assertEquals("submit", ex.getParam(ErpFinBadDebtApprovalStateMachine.ARG_ACTION),
                    "拒绝元数据携带动作名");
            assertEquals(illegal, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                    "拒绝元数据携带当前态");
        }
    }

    // ---------- (c) approve/reject：UNSUBMITTED/SUBMITTED 合法、APPROVED/REJECTED 非法 ----------

    @Test
    public void testAssertCanApproveLegalAndIllegal() {
        sm.assertCanApprove(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED);
        sm.assertCanApprove(ErpFinConstants.APPROVE_STATUS_SUBMITTED);
        sm.assertCanApprove(null); // null 归一化为 UNSUBMITTED（facade 允许直接审批未提交单）
        assertEquals(ErpFinConstants.APPROVE_STATUS_APPROVED, sm.approveTargetStatus());

        for (String illegal : Arrays.asList(ErpFinConstants.APPROVE_STATUS_APPROVED,
                ErpFinConstants.APPROVE_STATUS_REJECTED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanApprove(illegal),
                    "approve 对非法来源态应抛异常: " + illegal);
            assertEquals("approve", ex.getParam(ErpFinBadDebtApprovalStateMachine.ARG_ACTION));
            assertEquals(illegal, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
        }
    }

    @Test
    public void testAssertCanRejectLegalAndIllegal() {
        sm.assertCanReject(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED);
        sm.assertCanReject(ErpFinConstants.APPROVE_STATUS_SUBMITTED);
        sm.assertCanReject(null); // null 归一化为 UNSUBMITTED（facade 允许直接驳回未提交单）
        assertEquals(ErpFinConstants.APPROVE_STATUS_REJECTED, sm.rejectTargetStatus());

        for (String illegal : Arrays.asList(ErpFinConstants.APPROVE_STATUS_APPROVED,
                ErpFinConstants.APPROVE_STATUS_REJECTED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanReject(illegal),
                    "reject 对非法来源态应抛异常: " + illegal);
            assertEquals("reject", ex.getParam(ErpFinBadDebtApprovalStateMachine.ARG_ACTION));
            assertEquals(illegal, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
        }
    }

    // ---------- (d) reverseApprove：APPROVED 合法、其余非法 ----------

    @Test
    public void testAssertCanReverseApproveLegalAndIllegal() {
        sm.assertCanReverseApprove(ErpFinConstants.APPROVE_STATUS_APPROVED);
        assertEquals(ErpFinConstants.APPROVE_STATUS_REJECTED, sm.reverseApproveTargetStatus(),
                "reverseApprove→REJECTED（已合规 §16.4）");

        for (String illegal : Arrays.asList(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED,
                ErpFinConstants.APPROVE_STATUS_SUBMITTED, ErpFinConstants.APPROVE_STATUS_REJECTED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanReverseApprove(illegal),
                    "reverseApprove 对非法来源态应抛异常: " + illegal);
            assertEquals("reverseApprove", ex.getParam(ErpFinBadDebtApprovalStateMachine.ARG_ACTION));
            assertEquals(illegal, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
        }
    }

    // ---------- (e) 终态：APPROVED 可逆（reverseApprove 出边）/ REJECTED 不可逆（无出边） ----------

    @Test
    public void testTerminalStatuses() {
        // 业务终态 = {APPROVED, REJECTED}
        for (String terminal : sm.terminalStatuses()) {
            assertTrue(sm.isTerminal(terminal), "声明终态应 isTerminal=true: " + terminal);
        }
        assertFalse(sm.isTerminal(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED));
        assertFalse(sm.isTerminal(ErpFinConstants.APPROVE_STATUS_SUBMITTED));
        assertFalse(sm.isTerminal(null), "null 归一化为 UNSUBMITTED（非终态）");

        // APPROVED 为可逆终态：唯一出边 = reverseApprove → REJECTED
        assertTrue(hasOutgoing(ErpFinConstants.APPROVE_STATUS_APPROVED),
                "APPROVED 经 reverseApprove 有出边（可逆终态）");
        // REJECTED 为不可逆终态：无出边（facade 无 REJECTED 重提路径）
        assertFalse(hasOutgoing(ErpFinConstants.APPROVE_STATUS_REJECTED),
                "REJECTED 无出边（不可逆终态，无重提路径）");
    }

    // ---------- (f) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpFinBadDebtApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
            // 每条边的 fromStatus 对该 action 合法（assert 放行不抛）
            switch (e.getAction()) {
                case "submit":
                    sm.assertCanSubmit(e.getFromStatus());
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

    // ---------- (g) 终态/初始态集合正确 + null 归一化 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpFinConstants.APPROVE_STATUS_APPROVED, ErpFinConstants.APPROVE_STATUS_REJECTED),
                sm.terminalStatuses(), "终态集合 = {APPROVED, REJECTED}");
        assertEquals(Arrays.asList(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED), sm.initialStatuses(),
                "初始态集合 = {UNSUBMITTED}");
    }

    @Test
    public void testNullNormalizesToUnsubmitted() {
        // null 归一化为 UNSUBMITTED：submit/approve/reject(null) 合法（初始提交/直接审批/直接驳回）
        sm.assertCanSubmit(null);
        sm.assertCanApprove(null);
        sm.assertCanReject(null);
        // reverseApprove(null=UNSUBMITTED) 非法
        NopException ex = assertThrows(NopException.class, () -> sm.assertCanReverseApprove(null),
                "null（=UNSUBMITTED）对 reverseApprove 应非法");
        assertEquals(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                "拒绝元数据携带归一化后的当前态");
    }

    // ---------- 可达性：从 UNSUBMITTED 可达全部声明态，REJECTED 无出边 ----------

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED);
        assertTrue(reachable.contains(ErpFinConstants.APPROVE_STATUS_SUBMITTED), "从 UNSUBMITTED 应可达 SUBMITTED");
        assertTrue(reachable.contains(ErpFinConstants.APPROVE_STATUS_APPROVED), "从 UNSUBMITTED 应可达 APPROVED");
        assertTrue(reachable.contains(ErpFinConstants.APPROVE_STATUS_REJECTED), "从 UNSUBMITTED 应可达 REJECTED");
        assertFalse(reachableFrom(ErpFinConstants.APPROVE_STATUS_REJECTED).contains(ErpFinConstants.APPROVE_STATUS_SUBMITTED),
                "REJECTED 无重提出边（不可逆终态）");
    }

    // ---------- helpers ----------

    private String targetStatusFor(String action) {
        if ("submit".equals(action)) {
            return sm.submitTargetStatus();
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
            for (ErpFinBadDebtApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
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
