package app.erp.drp.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.drp.service.ErpDrpConstants;
import io.nop.api.core.exceptions.NopException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
 * <p>针对 {@link ErpDrpPlanStateMachine} Bean 的纯矩阵完备性遍历：不经 BizModel 入口（层 3 职责），
 * 不断言副作用/审计/净需求公式/参数校验。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边；</li>
 *   <li>(b) 从 DRAFT 可达 COMPUTED/APPROVED/EXECUTED，APPROVED 经 resetToDraft 可回 DRAFT；</li>
 *   <li>(c) resetToDraft 多源 {COMPUTED, APPROVED} 合法、对 DRAFT/EXECUTED 非法；</li>
 *   <li>(d) EXECUTED 终态无出边；<b>APPROVED 非终态</b>（有 resetToDraft 出边，对应 D-DRP-1 裁定）；</li>
 *   <li>(e) {@code transitions()} 元数据一致（5 边）；</li>
 *   <li>(f) 初始/终态集合正确。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpDrpPlanStateMachineMatrix {

    private static final List<String> ALL_STATUSES = Arrays.asList(
            ErpDrpConstants.DRP_PLAN_STATUS_DRAFT,
            ErpDrpConstants.DRP_PLAN_STATUS_COMPUTED,
            ErpDrpConstants.DRP_PLAN_STATUS_APPROVED,
            ErpDrpConstants.DRP_PLAN_STATUS_EXECUTED);

    private final ErpDrpPlanStateMachine sm = new ErpDrpPlanStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpDrpPlanStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpDrpPlanStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            // 同一 action + 同一 fromStatus 不得出现多次（否则冲突）
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(5, edges.size(), "迁移矩阵应有 5 条边（runDrp/approvePlan/advanceToExecuted 各 1 + resetToDraft 2 来源）");
    }

    // ---------- (b) 从 DRAFT 可达 COMPUTED/APPROVED/EXECUTED ----------

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpDrpConstants.DRP_PLAN_STATUS_DRAFT);
        for (String s : ALL_STATUSES) {
            if (ErpDrpConstants.DRP_PLAN_STATUS_DRAFT.equals(s)) {
                continue;
            }
            assertTrue(reachable.contains(s), "从 DRAFT 应可达状态: " + s);
        }
    }

    @Test
    public void testApprovedCanRollBackToDraft() {
        // D-DRP-1：APPROVED 经 resetToDraft 可回退 DRAFT（非终态、有出边）。
        // 验证 transitions() 含 APPROVED 的 resetToDraft→DRAFT 出边，且 assertCanResetToDraft(APPROVED) 放行。
        boolean hasApprovedRollback = false;
        for (ErpDrpPlanStateMachine.TransitionDefinition e : sm.transitions()) {
            if (ErpDrpConstants.DRP_PLAN_STATUS_APPROVED.equals(e.getFromStatus())
                    && "resetToDraft".equals(e.getAction())) {
                hasApprovedRollback = true;
                assertEquals(ErpDrpConstants.DRP_PLAN_STATUS_DRAFT, e.getToStatus(),
                        "APPROVED 经 resetToDraft → DRAFT");
            }
        }
        assertTrue(hasApprovedRollback, "APPROVED 应有 resetToDraft→DRAFT 出边（D-DRP-1：非终态可回退）");
        // 显式断言 resetToDraft(APPROVED) 放行
        sm.assertCanResetToDraft(ErpDrpConstants.DRP_PLAN_STATUS_APPROVED);
    }

    // ---------- (c) resetToDraft 多源 {COMPUTED, APPROVED} 合法、对 DRAFT/EXECUTED 非法 ----------

    @Test
    public void testResetToDraftMultiSourceLegalAndTerminalIllegal() {
        // 合法来源：COMPUTED、APPROVED
        sm.assertCanResetToDraft(ErpDrpConstants.DRP_PLAN_STATUS_COMPUTED);
        sm.assertCanResetToDraft(ErpDrpConstants.DRP_PLAN_STATUS_APPROVED);
        assertEquals(ErpDrpConstants.DRP_PLAN_STATUS_DRAFT, sm.resetToDraftTargetStatus());

        // 非法来源：DRAFT、EXECUTED
        for (String s : Arrays.asList(ErpDrpConstants.DRP_PLAN_STATUS_DRAFT, ErpDrpConstants.DRP_PLAN_STATUS_EXECUTED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanResetToDraft(s),
                    "resetToDraft 对非 COMPUTED/APPROVED 应非法: " + s);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                    "Bean 报告 common 层非法迁移码");
            assertEquals("resetToDraft", ex.getParam(ErpDrpPlanStateMachine.ARG_ACTION), "拒绝元数据携带动作名");
            assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS), "拒绝元数据携带当前态");
        }
    }

    // ---------- (d) EXECUTED 终态无出边；APPROVED 非终态 ----------

    @Test
    public void testTerminalStatusesHaveNoOutgoingEdges() {
        for (String terminal : sm.terminalStatuses()) {
            for (ErpDrpPlanStateMachine.TransitionDefinition e : sm.transitions()) {
                assertFalse(e.getFromStatus().equals(terminal),
                        "终态不应有出边: terminal=" + terminal + ", but edge " + e.getAction() + " leaves it");
            }
        }
    }

    @Test
    public void testApprovedIsNotTerminal() {
        // D-DRP-1：APPROVED 非终态（有 resetToDraft 出边）。owner doc §1/§3 误标终态，Bean 如实不认其为终态。
        assertFalse(sm.isTerminal(ErpDrpConstants.DRP_PLAN_STATUS_APPROVED),
                "APPROVED 非终态：有 resetToDraft 出边（owner doc §3:42 + 代码 DrpEngine.resetToDraft 接受 APPROVED）");
        assertTrue(sm.isTerminal(ErpDrpConstants.DRP_PLAN_STATUS_EXECUTED),
                "EXECUTED 是终态");
    }

    // ---------- (e) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpDrpPlanStateMachine.TransitionDefinition e : sm.transitions()) {
            // 每条边的 fromStatus 对该 action 合法（assert 放行不抛）。单来源动作的「其余态非法」由
            // testExplicitActionGuards 覆盖；resetToDraft 为多来源动作（COMPUTED/APPROVED），此处只验证声明的边均合法。
            invokeAssert(e.getAction(), e.getFromStatus());
            // 每条边的 toStatus 与 <action>TargetStatus() 一致
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (f) 初始/终态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Collections.singletonList(ErpDrpConstants.DRP_PLAN_STATUS_EXECUTED), sm.terminalStatuses(),
                "终态集合 = {EXECUTED}（仅 EXECUTED；APPROVED 非终态——D-DRP-1）");
        assertEquals(Collections.singletonList(ErpDrpConstants.DRP_PLAN_STATUS_DRAFT), sm.initialStatuses(),
                "初始态集合 = {DRAFT}");

        assertTrue(sm.isTerminal(ErpDrpConstants.DRP_PLAN_STATUS_EXECUTED));
        assertFalse(sm.isTerminal(ErpDrpConstants.DRP_PLAN_STATUS_DRAFT));
        assertFalse(sm.isTerminal(ErpDrpConstants.DRP_PLAN_STATUS_COMPUTED));
        assertFalse(sm.isTerminal(ErpDrpConstants.DRP_PLAN_STATUS_APPROVED));
    }

    // ---------- 合法/非法来源态显式断言（补充显式方法语义核对） ----------

    @Test
    public void testExplicitActionGuards() {
        // runDrp: 仅 DRAFT 合法
        assertActionAllowsOnly("runDrp", ErpDrpConstants.DRP_PLAN_STATUS_DRAFT);
        // approvePlan: 仅 COMPUTED 合法
        assertActionAllowsOnly("approvePlan", ErpDrpConstants.DRP_PLAN_STATUS_COMPUTED);
        // advanceToExecuted: 仅 APPROVED 合法
        assertActionAllowsOnly("advanceToExecuted", ErpDrpConstants.DRP_PLAN_STATUS_APPROVED);
    }

    @Test
    public void testTargetStatusMethods() {
        assertEquals(ErpDrpConstants.DRP_PLAN_STATUS_COMPUTED, sm.runDrpTargetStatus());
        assertEquals(ErpDrpConstants.DRP_PLAN_STATUS_APPROVED, sm.approvePlanTargetStatus());
        assertEquals(ErpDrpConstants.DRP_PLAN_STATUS_DRAFT, sm.resetToDraftTargetStatus());
        assertEquals(ErpDrpConstants.DRP_PLAN_STATUS_EXECUTED, sm.advanceToExecutedTargetStatus());
    }

    // ---------- helpers ----------

    /**
     * 断言某单来源 action 仅允许指定来源态：该来源态放行（不抛），其余全部状态非法（抛 common 码 + action 元数据）。
     */
    private void assertActionAllowsOnly(String action, String allowedFrom) {
        for (String s : ALL_STATUSES) {
            if (allowedFrom.equals(s)) {
                invokeAssert(action, s); // 不抛 = 合法
            } else {
                NopException ex = assertThrows(NopException.class, () -> invokeAssert(action, s),
                        action + " 对非允许来源态应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                        "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + s);
                assertEquals(action, ex.getParam(ErpDrpPlanStateMachine.ARG_ACTION),
                        "拒绝元数据携带动作名: action=" + action);
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                        "拒绝元数据携带当前态: action=" + action + ", status=" + s);
            }
        }
    }

    private void invokeAssert(String action, String status) {
        switch (action) {
            case "runDrp":
                sm.assertCanRunDrp(status);
                break;
            case "approvePlan":
                sm.assertCanApprovePlan(status);
                break;
            case "resetToDraft":
                sm.assertCanResetToDraft(status);
                break;
            case "advanceToExecuted":
                sm.assertCanAdvanceToExecuted(status);
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "runDrp":
                return sm.runDrpTargetStatus();
            case "approvePlan":
                return sm.approvePlanTargetStatus();
            case "resetToDraft":
                return sm.resetToDraftTargetStatus();
            case "advanceToExecuted":
                return sm.advanceToExecutedTargetStatus();
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
            for (ErpDrpPlanStateMachine.TransitionDefinition e : sm.transitions()) {
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
