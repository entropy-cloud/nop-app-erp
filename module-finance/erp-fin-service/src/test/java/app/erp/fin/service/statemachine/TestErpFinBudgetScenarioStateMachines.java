package app.erp.fin.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.fin.service.ErpFinConstants;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan Phase 1 Proof）。
 *
 * <p>针对 {@link ErpFinBudgetScenarioDocumentStateMachine}（docStatus 主轴）+ {@link ErpFinBudgetScenarioApprovalStateMachine}
 * （approveStatus 镜像轴）的纯矩阵完备性遍历：不经 BizModel/facade 入口（层 3 职责），不断言副作用/审计/前置校验。
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpFinBudgetScenarioStateMachines {

    private static final List<String> ALL_DOC_STATUSES = Arrays.asList(
            ErpFinConstants.BUDGET_STATUS_DRAFT,
            ErpFinConstants.BUDGET_STATUS_SUBMITTED,
            ErpFinConstants.BUDGET_STATUS_APPROVED,
            ErpFinConstants.BUDGET_STATUS_REJECTED,
            ErpFinConstants.BUDGET_STATUS_CANCELLED,
            ErpFinConstants.BUDGET_STATUS_CLOSED);

    private static final List<String> ALL_APPROVE_STATUSES = Arrays.asList(
            ErpFinConstants.APPROVE_STATUS_UNSUBMITTED,
            ErpFinConstants.APPROVE_STATUS_SUBMITTED,
            ErpFinConstants.APPROVE_STATUS_APPROVED,
            ErpFinConstants.APPROVE_STATUS_REJECTED);

    private final ErpFinBudgetScenarioDocumentStateMachine docSm = new ErpFinBudgetScenarioDocumentStateMachine();
    private final ErpFinBudgetScenarioApprovalStateMachine appSm = new ErpFinBudgetScenarioApprovalStateMachine();

    // ==================== docStatus 主轴 ====================

    @Test
    public void docNoDuplicateOrConflictingEdges() {
        List<ErpFinBudgetScenarioDocumentStateMachine.TransitionDefinition> edges = docSm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpFinBudgetScenarioDocumentStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(7, edges.size(), "应有 7 条边（5 源迁移 submit×2/approve/reject/cancel/carryForward + rollForward spawn）");
    }

    @Test
    public void docSubmitLegalAndIllegal() {
        docSm.assertCanSubmit(ErpFinConstants.BUDGET_STATUS_DRAFT); // 不抛
        docSm.assertCanSubmit(ErpFinConstants.BUDGET_STATUS_REJECTED); // 重提不抛
        for (String s : Arrays.asList(ErpFinConstants.BUDGET_STATUS_SUBMITTED,
                ErpFinConstants.BUDGET_STATUS_APPROVED,
                ErpFinConstants.BUDGET_STATUS_CANCELLED,
                ErpFinConstants.BUDGET_STATUS_CLOSED)) {
            NopException ex = assertThrows(NopException.class, () -> docSm.assertCanSubmit(s),
                    "submit 对非 DRAFT/REJECTED 应非法: " + s);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
            assertEquals("submit", ex.getParam(ErpFinBudgetScenarioDocumentStateMachine.ARG_ACTION));
            assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
        }
    }

    @Test
    public void docApproveLegalAndIllegal() {
        docSm.assertCanApprove(ErpFinConstants.BUDGET_STATUS_SUBMITTED); // 不抛
        for (String s : Arrays.asList(ErpFinConstants.BUDGET_STATUS_DRAFT,
                ErpFinConstants.BUDGET_STATUS_APPROVED,
                ErpFinConstants.BUDGET_STATUS_REJECTED,
                ErpFinConstants.BUDGET_STATUS_CANCELLED,
                ErpFinConstants.BUDGET_STATUS_CLOSED)) {
            assertThrows(NopException.class, () -> docSm.assertCanApprove(s),
                    "approve 对非 SUBMITTED 应非法: " + s);
        }
    }

    @Test
    public void docRejectLegalAndIllegal() {
        docSm.assertCanReject(ErpFinConstants.BUDGET_STATUS_SUBMITTED); // 不抛
        for (String s : Arrays.asList(ErpFinConstants.BUDGET_STATUS_DRAFT,
                ErpFinConstants.BUDGET_STATUS_APPROVED,
                ErpFinConstants.BUDGET_STATUS_REJECTED,
                ErpFinConstants.BUDGET_STATUS_CANCELLED,
                ErpFinConstants.BUDGET_STATUS_CLOSED)) {
            assertThrows(NopException.class, () -> docSm.assertCanReject(s),
                    "reject 对非 SUBMITTED 应非法: " + s);
        }
    }

    @Test
    public void docCancelLegalAndIllegal() {
        docSm.assertCanCancel(ErpFinConstants.BUDGET_STATUS_APPROVED); // 不抛
        for (String s : Arrays.asList(ErpFinConstants.BUDGET_STATUS_DRAFT,
                ErpFinConstants.BUDGET_STATUS_SUBMITTED,
                ErpFinConstants.BUDGET_STATUS_REJECTED,
                ErpFinConstants.BUDGET_STATUS_CANCELLED,
                ErpFinConstants.BUDGET_STATUS_CLOSED)) {
            assertThrows(NopException.class, () -> docSm.assertCanCancel(s),
                    "cancel 对非 APPROVED 应非法: " + s);
        }
    }

    @Test
    public void docCarryForwardLegalAndIllegal() {
        docSm.assertCanCarryForward(ErpFinConstants.BUDGET_STATUS_APPROVED); // 不抛
        for (String s : Arrays.asList(ErpFinConstants.BUDGET_STATUS_DRAFT,
                ErpFinConstants.BUDGET_STATUS_SUBMITTED,
                ErpFinConstants.BUDGET_STATUS_REJECTED,
                ErpFinConstants.BUDGET_STATUS_CANCELLED,
                ErpFinConstants.BUDGET_STATUS_CLOSED)) {
            assertThrows(NopException.class, () -> docSm.assertCanCarryForward(s),
                    "carryForward 对非 APPROVED 应非法: " + s);
        }
    }

    @Test
    public void docTerminalStatusesHaveNoOutgoingEdges() {
        for (String terminal : docSm.terminalStatuses()) {
            for (ErpFinBudgetScenarioDocumentStateMachine.TransitionDefinition e : docSm.transitions()) {
                assertFalse(!e.isSpawn() && e.getFromStatus().equals(terminal),
                        "终态不应有 source 迁移出边: terminal=" + terminal + ", but edge " + e.getAction() + " leaves it");
            }
        }
    }

    @Test
    public void docTransitionsMetadataConsistent() {
        for (ErpFinBudgetScenarioDocumentStateMachine.TransitionDefinition e : docSm.transitions()) {
            if (e.isSpawn()) {
                assertEquals("rollForward", e.getAction(), "spawn 边仅 rollForward");
                continue;
            }
            switch (e.getAction()) {
                case "submit":
                    docSm.assertCanSubmit(e.getFromStatus());
                    assertEquals(ErpFinConstants.BUDGET_STATUS_SUBMITTED, e.getToStatus());
                    break;
                case "approve":
                    docSm.assertCanApprove(e.getFromStatus());
                    assertEquals(ErpFinConstants.BUDGET_STATUS_APPROVED, e.getToStatus());
                    break;
                case "reject":
                    docSm.assertCanReject(e.getFromStatus());
                    assertEquals(ErpFinConstants.BUDGET_STATUS_REJECTED, e.getToStatus());
                    break;
                case "cancel":
                    docSm.assertCanCancel(e.getFromStatus());
                    assertEquals(ErpFinConstants.BUDGET_STATUS_CANCELLED, e.getToStatus());
                    break;
                case "carryForward":
                    docSm.assertCanCarryForward(e.getFromStatus());
                    assertEquals(ErpFinConstants.BUDGET_STATUS_CLOSED, e.getToStatus());
                    break;
                default:
                    throw new AssertionError("未知 action: " + e.getAction());
            }
        }
    }

    @Test
    public void docTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpFinConstants.BUDGET_STATUS_CANCELLED,
                ErpFinConstants.BUDGET_STATUS_CLOSED), docSm.terminalStatuses());
        assertEquals(Collections.singletonList(ErpFinConstants.BUDGET_STATUS_DRAFT), docSm.initialStatuses());

        assertTrue(docSm.isTerminal(ErpFinConstants.BUDGET_STATUS_CANCELLED));
        assertTrue(docSm.isTerminal(ErpFinConstants.BUDGET_STATUS_CLOSED));
        assertFalse(docSm.isTerminal(ErpFinConstants.BUDGET_STATUS_DRAFT));
        assertFalse(docSm.isTerminal(ErpFinConstants.BUDGET_STATUS_SUBMITTED));
        assertFalse(docSm.isTerminal(ErpFinConstants.BUDGET_STATUS_APPROVED));
        assertFalse(docSm.isTerminal(ErpFinConstants.BUDGET_STATUS_REJECTED));
    }

    @Test
    public void docTargetStatuses() {
        assertEquals(ErpFinConstants.BUDGET_STATUS_SUBMITTED, docSm.submitTargetStatus());
        assertEquals(ErpFinConstants.BUDGET_STATUS_APPROVED, docSm.approveTargetStatus());
        assertEquals(ErpFinConstants.BUDGET_STATUS_REJECTED, docSm.rejectTargetStatus());
        assertEquals(ErpFinConstants.BUDGET_STATUS_CANCELLED, docSm.cancelTargetStatus());
        assertEquals(ErpFinConstants.BUDGET_STATUS_CLOSED, docSm.carryForwardTargetStatus());
    }

    @Test
    public void docReachabilityFromInitial() {
        Set<String> reachable = docReachableFrom(ErpFinConstants.BUDGET_STATUS_DRAFT);
        assertTrue(reachable.contains(ErpFinConstants.BUDGET_STATUS_SUBMITTED));
        assertTrue(reachable.contains(ErpFinConstants.BUDGET_STATUS_APPROVED));
        assertTrue(reachable.contains(ErpFinConstants.BUDGET_STATUS_REJECTED));
        assertTrue(reachable.contains(ErpFinConstants.BUDGET_STATUS_CANCELLED));
        assertTrue(reachable.contains(ErpFinConstants.BUDGET_STATUS_CLOSED));
    }

    @Test
    public void docRollForwardIsSpawnMetadataOnly() {
        ErpFinBudgetScenarioDocumentStateMachine.TransitionDefinition rollForward = null;
        for (ErpFinBudgetScenarioDocumentStateMachine.TransitionDefinition e : docSm.transitions()) {
            if ("rollForward".equals(e.getAction())) {
                rollForward = e;
                break;
            }
        }
        assertNotNull(rollForward);
        assertTrue(rollForward.isSpawn(), "rollForward 应标注为 spawn");
        assertEquals(ErpFinConstants.BUDGET_STATUS_APPROVED, rollForward.getFromStatus(),
                "rollForward 源方案需 APPROVED");
        assertEquals(ErpFinConstants.BUDGET_STATUS_DRAFT, rollForward.getToStatus(),
                "rollForward 创建新方案 DRAFT");
    }

    // ==================== approveStatus 镜像轴 ====================

    @Test
    public void appNoDuplicateOrConflictingEdges() {
        List<ErpFinBudgetScenarioApprovalStateMachine.TransitionDefinition> edges = appSm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpFinBudgetScenarioApprovalStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(4, edges.size(), "应有 4 条边（submit×2 + approve + reject）");
    }

    @Test
    public void appSubmitLegalAndIllegal() {
        appSm.assertCanSubmit(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED); // 不抛
        appSm.assertCanSubmit(ErpFinConstants.APPROVE_STATUS_REJECTED); // 重提不抛
        for (String s : Arrays.asList(ErpFinConstants.APPROVE_STATUS_SUBMITTED,
                ErpFinConstants.APPROVE_STATUS_APPROVED)) {
            NopException ex = assertThrows(NopException.class, () -> appSm.assertCanSubmit(s),
                    "submit 对非 UNSUBMITTED/REJECTED 应非法: " + s);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
            assertEquals("submit", ex.getParam(ErpFinBudgetScenarioApprovalStateMachine.ARG_ACTION));
            assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
        }
    }

    @Test
    public void appApproveLegalAndIllegal() {
        appSm.assertCanApprove(ErpFinConstants.APPROVE_STATUS_SUBMITTED); // 不抛
        for (String s : Arrays.asList(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED,
                ErpFinConstants.APPROVE_STATUS_APPROVED,
                ErpFinConstants.APPROVE_STATUS_REJECTED)) {
            assertThrows(NopException.class, () -> appSm.assertCanApprove(s),
                    "approve 对非 SUBMITTED 应非法: " + s);
        }
    }

    @Test
    public void appRejectLegalAndIllegal() {
        appSm.assertCanReject(ErpFinConstants.APPROVE_STATUS_SUBMITTED); // 不抛
        for (String s : Arrays.asList(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED,
                ErpFinConstants.APPROVE_STATUS_APPROVED,
                ErpFinConstants.APPROVE_STATUS_REJECTED)) {
            assertThrows(NopException.class, () -> appSm.assertCanReject(s),
                    "reject 对非 SUBMITTED 应非法: " + s);
        }
    }

    @Test
    public void appTerminalStatusesHaveNoOutgoingEdges() {
        for (String terminal : appSm.terminalStatuses()) {
            for (ErpFinBudgetScenarioApprovalStateMachine.TransitionDefinition e : appSm.transitions()) {
                assertFalse(e.getFromStatus().equals(terminal),
                        "终态不应有出边: terminal=" + terminal + ", but edge " + e.getAction() + " leaves it");
            }
        }
    }

    @Test
    public void appTransitionsMetadataConsistent() {
        for (ErpFinBudgetScenarioApprovalStateMachine.TransitionDefinition e : appSm.transitions()) {
            switch (e.getAction()) {
                case "submit":
                    appSm.assertCanSubmit(e.getFromStatus());
                    assertEquals(ErpFinConstants.APPROVE_STATUS_SUBMITTED, e.getToStatus());
                    break;
                case "approve":
                    appSm.assertCanApprove(e.getFromStatus());
                    assertEquals(ErpFinConstants.APPROVE_STATUS_APPROVED, e.getToStatus());
                    break;
                case "reject":
                    appSm.assertCanReject(e.getFromStatus());
                    assertEquals(ErpFinConstants.APPROVE_STATUS_REJECTED, e.getToStatus());
                    break;
                default:
                    throw new AssertionError("未知 action: " + e.getAction());
            }
        }
    }

    @Test
    public void appTerminalAndInitialSets() {
        assertEquals(Collections.singletonList(ErpFinConstants.APPROVE_STATUS_APPROVED),
                appSm.terminalStatuses(), "终态 = {APPROVED}（REJECTED 可重提非终态）");
        assertEquals(Collections.singletonList(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED),
                appSm.initialStatuses(), "初始态 = {UNSUBMITTED}");

        assertTrue(appSm.isTerminal(ErpFinConstants.APPROVE_STATUS_APPROVED));
        assertFalse(appSm.isTerminal(ErpFinConstants.APPROVE_STATUS_REJECTED), "REJECTED 可重提非终态");
        assertFalse(appSm.isTerminal(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED));
        assertFalse(appSm.isTerminal(ErpFinConstants.APPROVE_STATUS_SUBMITTED));
    }

    @Test
    public void appTargetStatuses() {
        assertEquals(ErpFinConstants.APPROVE_STATUS_SUBMITTED, appSm.submitTargetStatus());
        assertEquals(ErpFinConstants.APPROVE_STATUS_APPROVED, appSm.approveTargetStatus());
        assertEquals(ErpFinConstants.APPROVE_STATUS_REJECTED, appSm.rejectTargetStatus());
    }

    @Test
    public void appReachabilityFromInitial() {
        Set<String> reachable = appReachableFrom(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED);
        assertTrue(reachable.contains(ErpFinConstants.APPROVE_STATUS_SUBMITTED));
        assertTrue(reachable.contains(ErpFinConstants.APPROVE_STATUS_APPROVED));
        assertTrue(reachable.contains(ErpFinConstants.APPROVE_STATUS_REJECTED),
                "REJECTED 从 UNSUBMITTED 经 submit→reject 可达");
    }

    // ---------- helpers ----------

    private Set<String> docReachableFrom(String start) {
        Set<String> visited = new LinkedHashSet<>();
        List<String> frontier = new java.util.ArrayList<>();
        frontier.add(start);
        while (!frontier.isEmpty()) {
            String cur = frontier.remove(0);
            if (!visited.add(cur)) {
                continue;
            }
            for (ErpFinBudgetScenarioDocumentStateMachine.TransitionDefinition e : docSm.transitions()) {
                if (e.isSpawn()) {
                    continue;
                }
                if (e.getFromStatus().equals(cur) && !visited.contains(e.getToStatus())) {
                    frontier.add(e.getToStatus());
                }
            }
        }
        return visited.stream()
                .filter(s -> !s.equals(start))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> appReachableFrom(String start) {
        Set<String> visited = new LinkedHashSet<>();
        List<String> frontier = new java.util.ArrayList<>();
        frontier.add(start);
        while (!frontier.isEmpty()) {
            String cur = frontier.remove(0);
            if (!visited.add(cur)) {
                continue;
            }
            for (ErpFinBudgetScenarioApprovalStateMachine.TransitionDefinition e : appSm.transitions()) {
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
