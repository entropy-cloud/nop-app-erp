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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan Phase 1 Proof）。
 *
 * <p>针对 {@link ErpHrLeaveRequestStateMachine} Bean 的纯矩阵完备性遍历：不经 BizModel 入口（层 3 职责），
 * 不断言副作用/审计/余额联动。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边（4 边）；</li>
 *   <li>(b) 从 DRAFT 可达全部 4 非初始态，终态（APPROVED/REJECTED/CANCELLED）中的纯终态（REJECTED/CANCELLED）无出边
 *       （APPROVED 既终态又 cancel 合法源 = 已批准休假取消，有 cancel 出边，特例显式断言）；</li>
 *   <li>(c) cancel <strong>单源</strong> APPROVED：DRAFT/SUBMITTED/REJECTED/CANCELLED 均非法（对齐生产代码
 *       {@code ErpHrLeaveRequestCancelProcessor:21}，owner doc §2/§6 DRAFT/SUBMITTED→CANCELLED 漂移在 layer-2 登记）；</li>
 *   <li>(d) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(e) 终态/初始态集合正确。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpHrLeaveRequestStateMachineMatrix {

    static final List<String> ALL_STATUSES = Arrays.asList(
            ErpHrConstants.LEAVE_STATUS_DRAFT,
            ErpHrConstants.LEAVE_STATUS_SUBMITTED,
            ErpHrConstants.LEAVE_STATUS_APPROVED,
            ErpHrConstants.LEAVE_STATUS_REJECTED,
            ErpHrConstants.LEAVE_STATUS_CANCELLED);

    private final ErpHrLeaveRequestStateMachine sm = new ErpHrLeaveRequestStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpHrLeaveRequestStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpHrLeaveRequestStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(4, edges.size(), "迁移矩阵应有 4 条边（submit/approve/reject/cancel 单源）");
    }

    // ---------- (b) 从 DRAFT 可达全部 4 非初始态 ----------

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpHrConstants.LEAVE_STATUS_DRAFT);
        for (String s : ALL_STATUSES) {
            if (ErpHrConstants.LEAVE_STATUS_DRAFT.equals(s)) {
                continue;
            }
            assertTrue(reachable.contains(s), "从 DRAFT 应可达状态: " + s);
        }
    }

    @Test
    public void testPureTerminalStatusesHaveNoOutgoingEdges() {
        // REJECTED/CANCELLED 是纯终态（无任何 action 以其为源），不应出现在任何边的 fromStatus。
        // APPROVED 虽 isTerminal=true 但为 cancel 单源（已批准休假取消），有 cancel 出边——特例不参与本断言。
        for (ErpHrLeaveRequestStateMachine.TransitionDefinition e : sm.transitions()) {
            assertFalse(ErpHrConstants.LEAVE_STATUS_REJECTED.equals(e.getFromStatus()),
                    "REJECTED 纯终态不应有出边: but edge " + e.getAction() + " leaves it");
            assertFalse(ErpHrConstants.LEAVE_STATUS_CANCELLED.equals(e.getFromStatus()),
                    "CANCELLED 纯终态不应有出边: but edge " + e.getAction() + " leaves it");
        }
    }

    // ---------- (c) cancel 单源 APPROVED；其他态全非法 ----------

    @Test
    public void testCancelSingleSourceApproved() {
        // 仅 APPROVED 合法
        sm.assertCanCancel(ErpHrConstants.LEAVE_STATUS_APPROVED); // 不抛
        assertEquals(ErpHrConstants.LEAVE_STATUS_CANCELLED, sm.cancelTargetStatus());

        // 其余态（含 owner doc 声明但代码未实现的 DRAFT/SUBMITTED）全部非法
        for (String s : ALL_STATUSES) {
            if (ErpHrConstants.LEAVE_STATUS_APPROVED.equals(s)) {
                continue;
            }
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanCancel(s),
                    "cancel 单源 APPROVED：对 [" + s + "] 应非法");
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                    "Bean 报告 common 层非法迁移码: status=" + s);
            assertEquals("cancel", ex.getParam(ErpHrLeaveRequestStateMachine.ARG_ACTION),
                    "拒绝元数据携带动作名: status=" + s);
            assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                    "拒绝元数据携带当前态: status=" + s);
        }
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpHrLeaveRequestStateMachine.TransitionDefinition e : sm.transitions()) {
            invokeAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (e) 终态/初始态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(
                ErpHrConstants.LEAVE_STATUS_APPROVED,
                ErpHrConstants.LEAVE_STATUS_REJECTED,
                ErpHrConstants.LEAVE_STATUS_CANCELLED), sm.terminalStatuses(),
                "终态集合 = {APPROVED, REJECTED, CANCELLED}");
        assertEquals(Arrays.asList(ErpHrConstants.LEAVE_STATUS_DRAFT), sm.initialStatuses(),
                "初始态集合 = {DRAFT}");

        assertTrue(sm.isTerminal(ErpHrConstants.LEAVE_STATUS_APPROVED));
        assertTrue(sm.isTerminal(ErpHrConstants.LEAVE_STATUS_REJECTED));
        assertTrue(sm.isTerminal(ErpHrConstants.LEAVE_STATUS_CANCELLED));
        assertFalse(sm.isTerminal(ErpHrConstants.LEAVE_STATUS_DRAFT));
        assertFalse(sm.isTerminal(ErpHrConstants.LEAVE_STATUS_SUBMITTED));
    }

    // ---------- 合法/非法来源态显式断言（补充显式方法语义核对） ----------

    @Test
    public void testExplicitActionGuards() {
        // submit: 仅 DRAFT 合法
        assertActionAllowsOnly("submit", ErpHrConstants.LEAVE_STATUS_DRAFT);
        // approve: 仅 SUBMITTED 合法
        assertActionAllowsOnly("approve", ErpHrConstants.LEAVE_STATUS_SUBMITTED);
        // reject: 仅 SUBMITTED 合法
        assertActionAllowsOnly("reject", ErpHrConstants.LEAVE_STATUS_SUBMITTED);
        // cancel: 仅 APPROVED 合法（单源，已在 testCancelSingleSourceApproved 详测；此处补矩阵对称）
        assertActionAllowsOnly("cancel", ErpHrConstants.LEAVE_STATUS_APPROVED);
    }

    @Test
    public void testTargetStatusMethods() {
        assertEquals(ErpHrConstants.LEAVE_STATUS_SUBMITTED, sm.submitTargetStatus());
        assertEquals(ErpHrConstants.LEAVE_STATUS_APPROVED, sm.approveTargetStatus());
        assertEquals(ErpHrConstants.LEAVE_STATUS_REJECTED, sm.rejectTargetStatus());
        assertEquals(ErpHrConstants.LEAVE_STATUS_CANCELLED, sm.cancelTargetStatus());
    }

    // ---------- helpers ----------

    private void assertActionAllowsOnly(String action, String allowedFrom) {
        for (String s : ALL_STATUSES) {
            if (allowedFrom.equals(s)) {
                invokeAssert(action, s); // 不抛 = 合法
            } else {
                NopException ex = assertThrows(NopException.class, () -> invokeAssert(action, s),
                        action + " 对非允许来源态应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                        "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + s);
                assertEquals(action, ex.getParam(ErpHrLeaveRequestStateMachine.ARG_ACTION),
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
            case "cancel":
                sm.assertCanCancel(status);
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
            case "cancel":
                return sm.cancelTargetStatus();
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
            for (ErpHrLeaveRequestStateMachine.TransitionDefinition e : sm.transitions()) {
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
