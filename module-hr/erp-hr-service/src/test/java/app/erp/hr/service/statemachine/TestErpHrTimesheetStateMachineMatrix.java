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
 * <p>针对 {@link ErpHrTimesheetStateMachine} Bean 的纯矩阵完备性遍历：不经 BizModel 入口（层 3 职责），
 * 不断言副作用/审计/24h 校验/totalHours 派生。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边（4 边：submit 2 源 + approve 1 + reject 1）；</li>
 *   <li>(b) 从 DRAFT 可达全部非初始态（SUBMITTED/APPROVED/REJECTED）；APPROVED 严格终态无出边；
 *       REJECTED 为可恢复终态（有 submit 重提边）；</li>
 *   <li>(c) submit 多来源态（DRAFT/REJECTED 合法，SUBMITTED/APPROVED 非法）、approve/reject 单源 SUBMITTED；</li>
 *   <li>(d) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(e) 终态/初始态集合正确（APPROVED/REJECTED 终态、DRAFT 初始态）；</li>
 *   <li>(f) <strong>断言无 cancel 边</strong>：dict {@code erp-hr/timesheet-status} 4 值无 CANCELLED（RC-R1.8 权威），
 *       Bean 不编码 cancel——cancel doc drift 对照在此落实（layer-2 四方对照登记于 plan Phase 3）。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpHrTimesheetStateMachineMatrix {

    static final List<String> ALL_STATUSES = Arrays.asList(
            ErpHrConstants.TIMESHEET_STATUS_DRAFT,
            ErpHrConstants.TIMESHEET_STATUS_SUBMITTED,
            ErpHrConstants.TIMESHEET_STATUS_APPROVED,
            ErpHrConstants.TIMESHEET_STATUS_REJECTED);

    private final ErpHrTimesheetStateMachine sm = new ErpHrTimesheetStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpHrTimesheetStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpHrTimesheetStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(4, edges.size(), "迁移矩阵应有 4 条边（submit 2 源 + approve 1 + reject 1）");
    }

    // ---------- (b) 从 DRAFT 可达全部非初始态；APPROVED 严格终态；REJECTED 可恢复终态 ----------

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpHrConstants.TIMESHEET_STATUS_DRAFT);
        for (String s : ALL_STATUSES) {
            if (ErpHrConstants.TIMESHEET_STATUS_DRAFT.equals(s)) {
                continue;
            }
            assertTrue(reachable.contains(s), "从 DRAFT 应可达状态: " + s);
        }
    }

    @Test
    public void testApprovedStrictTerminalNoOutgoingEdges() {
        // APPROVED = 严格终态（无任何出边，审核通过后不可逆转）
        for (ErpHrTimesheetStateMachine.TransitionDefinition e : sm.transitions()) {
            assertFalse(ErpHrConstants.TIMESHEET_STATUS_APPROVED.equals(e.getFromStatus()),
                    "APPROVED 严格终态不应有出边: but edge " + e.getAction() + " leaves it");
        }
    }

    @Test
    public void testRejectedIsRecoverableTerminalWithResubmitEdge() {
        // REJECTED = 可恢复终态（有 submit 重提边 REJECTED→SUBMITTED，员工修改后可重新提交）
        List<ErpHrTimesheetStateMachine.TransitionDefinition> rejectedOut = sm.transitions().stream()
                .filter(e -> ErpHrConstants.TIMESHEET_STATUS_REJECTED.equals(e.getFromStatus()))
                .collect(Collectors.toList());
        assertEquals(1, rejectedOut.size(), "REJECTED 仅有 1 条出边（submit 重提）");
        assertEquals("submit", rejectedOut.get(0).getAction(), "REJECTED 唯一出边 action = submit");
        assertEquals(ErpHrConstants.TIMESHEET_STATUS_SUBMITTED, rejectedOut.get(0).getToStatus(),
                "REJECTED→submit 目标态 = SUBMITTED");
    }

    // ---------- (c) submit 多来源态 + approve/reject 单源 ----------

    @Test
    public void testSubmitLegalForDraftOrRejected() {
        // submit: DRAFT/REJECTED 合法（首次提交 + 驳回后重提）
        sm.assertCanSubmit(ErpHrConstants.TIMESHEET_STATUS_DRAFT); // 不抛
        sm.assertCanSubmit(ErpHrConstants.TIMESHEET_STATUS_REJECTED); // 不抛
        assertEquals(ErpHrConstants.TIMESHEET_STATUS_SUBMITTED, sm.submitTargetStatus(),
                "submit 目标态恒为 SUBMITTED");

        // SUBMITTED/APPROVED 非法
        for (String s : Arrays.asList(ErpHrConstants.TIMESHEET_STATUS_SUBMITTED,
                ErpHrConstants.TIMESHEET_STATUS_APPROVED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanSubmit(s),
                    "submit 对已提交/已批准应非法: " + s);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                    "Bean 报告 common 层非法迁移码: status=" + s);
            assertEquals("submit", ex.getParam(ErpHrTimesheetStateMachine.ARG_ACTION),
                    "拒绝元数据携带动作名: status=" + s);
            assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                    "拒绝元数据携带当前态: status=" + s);
        }
    }

    @Test
    public void testApproveAllowsOnlySubmitted() {
        assertActionAllowsOnly("approve", ErpHrConstants.TIMESHEET_STATUS_SUBMITTED);
    }

    @Test
    public void testRejectAllowsOnlySubmitted() {
        assertActionAllowsOnly("reject", ErpHrConstants.TIMESHEET_STATUS_SUBMITTED);
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpHrTimesheetStateMachine.TransitionDefinition e : sm.transitions()) {
            invokeAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (e) 终态/初始态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(
                ErpHrConstants.TIMESHEET_STATUS_APPROVED,
                ErpHrConstants.TIMESHEET_STATUS_REJECTED), sm.terminalStatuses(),
                "终态集合 = {APPROVED, REJECTED}");
        assertEquals(Arrays.asList(ErpHrConstants.TIMESHEET_STATUS_DRAFT), sm.initialStatuses(),
                "初始态集合 = {DRAFT}");

        assertTrue(sm.isTerminal(ErpHrConstants.TIMESHEET_STATUS_APPROVED));
        assertTrue(sm.isTerminal(ErpHrConstants.TIMESHEET_STATUS_REJECTED));
        assertFalse(sm.isTerminal(ErpHrConstants.TIMESHEET_STATUS_DRAFT));
        assertFalse(sm.isTerminal(ErpHrConstants.TIMESHEET_STATUS_SUBMITTED));
    }

    @Test
    public void testTargetStatusMethods() {
        assertEquals(ErpHrConstants.TIMESHEET_STATUS_SUBMITTED, sm.submitTargetStatus());
        assertEquals(ErpHrConstants.TIMESHEET_STATUS_APPROVED, sm.approveTargetStatus());
        assertEquals(ErpHrConstants.TIMESHEET_STATUS_REJECTED, sm.rejectTargetStatus());
    }

    // ---------- (f) 断言无 cancel 边（dict 无 CANCELLED，doc drift 对照） ----------

    @Test
    public void testNoCancelEdgeDictHasNoCancelledValue() {
        // dict erp-hr/timesheet-status 仅 4 值（DRAFT/SUBMITTED/APPROVED/REJECTED），无 CANCELLED。
        // owner doc §2 图表（state-machine.md:199）画 DRAFT→CANCELLED 为 doc drift（RC-R1.8 权威注记仅 3 动作）。
        // Bean 如实不编码 cancel——任何 action 的 from/toStatus 均不应出现 CANCELLED。
        for (ErpHrTimesheetStateMachine.TransitionDefinition e : sm.transitions()) {
            assertFalse("cancel".equals(e.getAction()),
                    "Bean 不应编码 cancel 边（dict 无 CANCELLED，RC-R1.8 权威）: " + e);
            assertFalse("CANCELLED".equals(e.getFromStatus()),
                    "CANCELLED 不在 dict，不应为任何边的源: " + e);
            assertFalse("CANCELLED".equals(e.getToStatus()),
                    "CANCELLED 不在 dict，不应为任何边的目标: " + e);
        }
        // Bean 无 assertCanCancel 方法（编译期保证）；此处再断言 transitions 中无 cancel action
        boolean hasCancel = sm.transitions().stream().anyMatch(e -> "cancel".equals(e.getAction()));
        assertFalse(hasCancel, "transitions 不应含 cancel 边");
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
                assertEquals(action, ex.getParam(ErpHrTimesheetStateMachine.ARG_ACTION),
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
            for (ErpHrTimesheetStateMachine.TransitionDefinition e : sm.transitions()) {
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
