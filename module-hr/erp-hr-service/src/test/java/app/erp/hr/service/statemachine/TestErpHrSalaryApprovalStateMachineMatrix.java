package app.erp.hr.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.hr.service.ErpHrConstants;
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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan Phase 2 Proof）。
 *
 * <p>针对 {@link ErpHrSalaryApprovalStateMachine} Bean（approveStatus 审批轴，{@code wf/approve-status} 四态）
 * 的纯矩阵完备性遍历：不经 BizModel 入口（层 3 职责），不断言副作用（xwf 审批链/notify/过账）。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边（6 边：submit 2 源 + approve/reject/reverseApprove/withdrawApproval 各 1）；</li>
 *   <li>(b) 从 UNSUBMITTED 可达全部 3 非初始态（SUBMITTED/APPROVED/REJECTED）；</li>
 *   <li>(c) 矩阵裁定依据实仓守卫：approve/reject 仅 SUBMITTED 单源（UNSUBMITTED 直接 approve 被拒——
 *       {@code ErpHrSalary.xbiz:62,:87} + {@code TestErpHrPayrollEngine.testIllegalTransitionRejects}）；
 *       submit 允许 UNSUBMITTED/null/REJECTED；reverseApprove 仅 APPROVED；withdrawApproval 仅 SUBMITTED；</li>
 *   <li>(d) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(e) 终态/初始态集合正确——APPROVED 为「可逆终态」（唯一出边 reverseApprove→SUBMITTED），REJECTED
 *       为「可重提终态」（唯一出边 submit→SUBMITTED，owner doc §适用对象四 §2「终态，可修改后重新提交」）；</li>
 *   <li>(f) markPaid 交叉守卫（一致性，非迁移边）：仅 APPROVED 合法。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpHrSalaryApprovalStateMachineMatrix {

    static final List<String> ALL_STATUSES = Arrays.asList(
            ErpHrConstants.APPROVE_STATUS_UNSUBMITTED,
            ErpHrConstants.APPROVE_STATUS_SUBMITTED,
            ErpHrConstants.APPROVE_STATUS_APPROVED,
            ErpHrConstants.APPROVE_STATUS_REJECTED);

    private final ErpHrSalaryApprovalStateMachine sm = new ErpHrSalaryApprovalStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpHrSalaryApprovalStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpHrSalaryApprovalStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(6, edges.size(), "迁移矩阵应有 6 条边（submit 2 源 + approve/reject/reverseApprove/withdrawApproval）");
    }

    // ---------- (b) 从 UNSUBMITTED 可达全部 3 非初始态 ----------

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpHrConstants.APPROVE_STATUS_UNSUBMITTED);
        for (String s : ALL_STATUSES) {
            if (ErpHrConstants.APPROVE_STATUS_UNSUBMITTED.equals(s)) {
                continue;
            }
            assertTrue(reachable.contains(s), "从 UNSUBMITTED 应可达状态: " + s);
        }
    }

    // ---------- (c) 合法/非法来源态显式断言（矩阵裁定依据实仓 xbiz 守卫） ----------

    @Test
    public void testExplicitActionGuards() {
        // submit: UNSUBMITTED/REJECTED 合法（+null 归一化后合法，见 testSubmitAllowsNull）
        assertActionAllowsOnly("submit", ErpHrConstants.APPROVE_STATUS_UNSUBMITTED,
                ErpHrConstants.APPROVE_STATUS_REJECTED);
        // approve: 仅 SUBMITTED（UNSUBMITTED 直接 approve 被拒——实仓守卫 + testIllegalTransitionRejects）
        assertActionAllowsOnly("approve", ErpHrConstants.APPROVE_STATUS_SUBMITTED);
        // reject: 仅 SUBMITTED（与 approve 同源态）
        assertActionAllowsOnly("reject", ErpHrConstants.APPROVE_STATUS_SUBMITTED);
        // reverseApprove: 仅 APPROVED
        assertActionAllowsOnly("reverseApprove", ErpHrConstants.APPROVE_STATUS_APPROVED);
        // withdrawApproval: 仅 SUBMITTED
        assertActionAllowsOnly("withdrawApproval", ErpHrConstants.APPROVE_STATUS_SUBMITTED);
    }

    @Test
    public void testSubmitAllowsNull() {
        sm.assertCanSubmit(null); // null 归一化为 UNSUBMITTED → 合法（初始态语义）
        assertEquals(ErpHrConstants.APPROVE_STATUS_SUBMITTED, sm.submitTargetStatus());
    }

    @Test
    public void testNullIllegalForOtherActions() {
        // 其余动作 null → 归一化 UNSUBMITTED → 非法（非 SUBMITTED/APPROVED）
        for (String action : Arrays.asList("approve", "reject", "reverseApprove", "withdrawApproval", "markPaid")) {
            NopException ex = assertThrows(NopException.class, () -> invokeAssert(action, null),
                    action + " 对 null 应非法（归一化为 UNSUBMITTED）");
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
            assertEquals(ErpHrConstants.APPROVE_STATUS_UNSUBMITTED, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                    "null 归一化后 currentStatus=UNSUBMITTED: action=" + action);
        }
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpHrSalaryApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
            invokeAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (e) 终态/初始态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpHrConstants.APPROVE_STATUS_APPROVED), sm.terminalStatuses(),
                "终态集合 = {APPROVED}");
        assertEquals(Arrays.asList(ErpHrConstants.APPROVE_STATUS_UNSUBMITTED), sm.initialStatuses(),
                "初始态集合 = {UNSUBMITTED}");

        assertTrue(sm.isTerminal(ErpHrConstants.APPROVE_STATUS_APPROVED));
        assertFalse(sm.isTerminal(ErpHrConstants.APPROVE_STATUS_UNSUBMITTED));
        assertFalse(sm.isTerminal(ErpHrConstants.APPROVE_STATUS_SUBMITTED));
        assertFalse(sm.isTerminal(ErpHrConstants.APPROVE_STATUS_REJECTED));
    }

    @Test
    public void testTerminalReversibleOnlyViaReverseApprove() {
        // APPROVED 是「可逆终态」：唯一出边 = reverseApprove→SUBMITTED（owner doc §适用对象四 §3）
        for (ErpHrSalaryApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
            if (ErpHrConstants.APPROVE_STATUS_APPROVED.equals(e.getFromStatus())) {
                assertEquals("reverseApprove", e.getAction(),
                        "APPROVED 终态唯一出边应为 reverseApprove");
                assertEquals(ErpHrConstants.APPROVE_STATUS_SUBMITTED, e.getToStatus());
            }
        }
    }

    @Test
    public void testRejectedResubmittableSingleEdge() {
        // REJECTED 是「可重提终态」（owner doc §适用对象四 §2「终态，可修改后重新提交」）：唯一出边 = submit→SUBMITTED
        List<ErpHrSalaryApprovalStateMachine.TransitionDefinition> fromRejected = sm.transitions().stream()
                .filter(e -> ErpHrConstants.APPROVE_STATUS_REJECTED.equals(e.getFromStatus()))
                .collect(Collectors.toList());
        assertEquals(1, fromRejected.size(), "REJECTED 应仅 1 条出边（重提）");
        assertEquals("submit", fromRejected.get(0).getAction());
        assertEquals(ErpHrConstants.APPROVE_STATUS_SUBMITTED, fromRejected.get(0).getToStatus());
    }

    // ---------- (f) markPaid 交叉守卫（一致性，非迁移边） ----------

    @Test
    public void testMarkPaidCrossGuardOnlyApproved() {
        sm.assertCanMarkPaid(ErpHrConstants.APPROVE_STATUS_APPROVED); // 不抛
        for (String s : ALL_STATUSES) {
            if (ErpHrConstants.APPROVE_STATUS_APPROVED.equals(s)) {
                continue;
            }
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanMarkPaid(s),
                    "markPaid 交叉守卫对非 APPROVED 应非法: " + s);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
            assertEquals("markPaid", ex.getParam(ErpHrSalaryApprovalStateMachine.ARG_ACTION));
        }
        // 交叉守卫不在 transitions() 元数据中编码（非迁移边，契约 §4.2）
        assertTrue(sm.transitions().stream().noneMatch(e -> "markPaid".equals(e.getAction())),
                "markPaid 交叉守卫不应出现在 transitions() 迁移边中");
    }

    @Test
    public void testTargetStatusMethods() {
        assertEquals(ErpHrConstants.APPROVE_STATUS_SUBMITTED, sm.submitTargetStatus());
        assertEquals(ErpHrConstants.APPROVE_STATUS_APPROVED, sm.approveTargetStatus());
        assertEquals(ErpHrConstants.APPROVE_STATUS_REJECTED, sm.rejectTargetStatus());
        assertEquals(ErpHrConstants.APPROVE_STATUS_SUBMITTED, sm.reverseApproveTargetStatus());
        assertEquals(ErpHrConstants.APPROVE_STATUS_UNSUBMITTED, sm.withdrawApprovalTargetStatus());
    }

    // ---------- helpers ----------

    private void assertActionAllowsOnly(String action, String... allowedFrom) {
        List<String> allowed = Arrays.asList(allowedFrom);
        for (String s : ALL_STATUSES) {
            if (allowed.contains(s)) {
                invokeAssert(action, s); // 不抛 = 合法
            } else {
                NopException ex = assertThrows(NopException.class, () -> invokeAssert(action, s),
                        action + " 对非允许来源态应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                        "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + s);
                assertEquals(action, ex.getParam(ErpHrSalaryApprovalStateMachine.ARG_ACTION),
                        "拒绝元数据携带动作名: action=" + action + ", status=" + s);
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                        "拒绝元数据携带当前态: action=" + action + ", status=" + s);
            }
        }
    }

    private void invokeAssert(String action, String status) {
        switch (action) {
            case "submit":
                sm.assertCanSubmit(status);
                break;
            case "approve":
                sm.assertCanApprove(status);
                break;
            case "reject":
                sm.assertCanReject(status);
                break;
            case "reverseApprove":
                sm.assertCanReverseApprove(status);
                break;
            case "withdrawApproval":
                sm.assertCanWithdrawApproval(status);
                break;
            case "markPaid":
                sm.assertCanMarkPaid(status);
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
            case "withdrawApproval":
                return sm.withdrawApprovalTargetStatus();
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
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
            for (ErpHrSalaryApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
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
