package app.erp.inv.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.inv.dao.constants.ErpInvDocStatus;
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
 * <p>针对两 Bean 的纯矩阵完备性遍历：{@link ErpInvStockMoveStateMachine}（移动单 docStatus 单轴）+
 * {@link ErpInvStockTakeStateMachine}（盘点单 docStatus 单轴，复用 erp-inv/move-status dict）。
 * 不经 BizModel 入口（层 3 职责），不断言副作用/过账/记账/效期/预留量。
 *
 * <p>每轴覆盖：(a) 无重复/冲突边；(b) 从初始态可达全部声明状态 + 终态无出边；(c) 各动作合法/非法来源态全集；
 * (d) {@code transitions()} 元数据与显式方法语义一致；(e) 终态/初始态集合正确；(f) 4 值全可达（无死状态）。
 * Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 *
 * <p><b>COUNTING 标签漂移核对</b>：{@code erp-inv/move-status} dict 4 值 DRAFT/CONFIRMED/DONE/CANCELLED
 * <b>无 COUNTING</b>；盘点单「盘点中」实际复用 CONFIRMED（{@link ErpInvStockTakeStateMachine#startTakeTargetStatus()}
 * 返回 CONFIRMED）。
 */
public class TestErpInvStockMoveAndStockTakeStateMachines {

    // erp-inv/move-status 字典四态（移动单与盘点单共享；无 COUNTING 值）
    private static final List<String> ALL_MOVE_STATUSES = Arrays.asList(
            ErpInvDocStatus.DOC_STATUS_DRAFT,
            ErpInvDocStatus.DOC_STATUS_CONFIRMED,
            ErpInvDocStatus.DOC_STATUS_DONE,
            ErpInvDocStatus.DOC_STATUS_CANCELLED);

    private final ErpInvStockMoveStateMachine moveSm = new ErpInvStockMoveStateMachine();
    private final ErpInvStockTakeStateMachine takeSm = new ErpInvStockTakeStateMachine();

    // ==================== 移动单 docStatus 轴 ====================

    @Test
    public void moveNoDuplicateOrConflictingEdges() {
        List<ErpInvStockMoveStateMachine.TransitionDefinition> edges = moveSm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpInvStockMoveStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(4, edges.size(), "迁移矩阵应有 4 条边（confirm 1 + complete 1 + cancel 2 来源）");
    }

    @Test
    public void moveReachabilityFromInitial() {
        Set<String> reachable = reachableMoveFrom(ErpInvDocStatus.DOC_STATUS_DRAFT);
        // 4 值全可达（从 DRAFT 出发可达 CONFIRMED/DONE/CANCELLED）
        assertTrue(reachable.contains(ErpInvDocStatus.DOC_STATUS_CONFIRMED), "从 DRAFT 应可达 CONFIRMED");
        assertTrue(reachable.contains(ErpInvDocStatus.DOC_STATUS_DONE), "从 DRAFT 应可达 DONE");
        assertTrue(reachable.contains(ErpInvDocStatus.DOC_STATUS_CANCELLED), "从 DRAFT 应可达 CANCELLED");
    }

    @Test
    public void moveConfirmAllowsOnlyDraft() {
        assertMoveAllowsOnly("confirm", ErpInvDocStatus.DOC_STATUS_DRAFT);
    }

    @Test
    public void moveCompleteAllowsOnlyConfirmed() {
        assertMoveAllowsOnly("complete", ErpInvDocStatus.DOC_STATUS_CONFIRMED);
    }

    @Test
    public void moveCancelAllowsOnlyDraftOrConfirmed() {
        for (String s : ALL_MOVE_STATUSES) {
            if (ErpInvDocStatus.DOC_STATUS_DRAFT.equals(s) || ErpInvDocStatus.DOC_STATUS_CONFIRMED.equals(s)) {
                moveSm.assertCanCancel(s); // 合法不抛
            } else {
                NopException ex = assertThrows(NopException.class, () -> moveSm.assertCanCancel(s),
                        "cancel 对 DONE/CANCELLED 应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
                assertEquals("cancel", ex.getParam(ErpInvStockMoveStateMachine.ARG_ACTION));
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
            }
        }
    }

    @Test
    public void moveTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpInvStockMoveStateMachine.TransitionDefinition e : moveSm.transitions()) {
            invokeMoveAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), moveTargetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    @Test
    public void moveTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpInvDocStatus.DOC_STATUS_DONE, ErpInvDocStatus.DOC_STATUS_CANCELLED),
                moveSm.terminalStatuses(), "终态集合 = {DONE, CANCELLED}");
        assertEquals(Collections.singletonList(ErpInvDocStatus.DOC_STATUS_DRAFT),
                moveSm.initialStatuses(), "初始态集合 = {DRAFT}");
        assertTrue(moveSm.isTerminal(ErpInvDocStatus.DOC_STATUS_DONE));
        assertTrue(moveSm.isTerminal(ErpInvDocStatus.DOC_STATUS_CANCELLED));
        assertFalse(moveSm.isTerminal(ErpInvDocStatus.DOC_STATUS_DRAFT));
        assertFalse(moveSm.isTerminal(ErpInvDocStatus.DOC_STATUS_CONFIRMED));
    }

    /** DONE 与 CANCELLED 均为真终态（无出边——DONE 的「冲销」是生成反向新单非状态迁移）。 */
    @Test
    public void moveTerminalsAreTrueTerminals() {
        for (ErpInvStockMoveStateMachine.TransitionDefinition e : moveSm.transitions()) {
            assertFalse(moveSm.isTerminal(e.getFromStatus()),
                    "真终态不应有出边: but edge " + e.getAction() + " leaves " + e.getFromStatus());
        }
    }

    @Test
    public void moveTargetStatusMethods() {
        assertEquals(ErpInvDocStatus.DOC_STATUS_CONFIRMED, moveSm.confirmTargetStatus());
        assertEquals(ErpInvDocStatus.DOC_STATUS_DONE, moveSm.completeTargetStatus());
        assertEquals(ErpInvDocStatus.DOC_STATUS_CANCELLED, moveSm.cancelTargetStatus());
    }

    @Test
    public void moveAllDictValuesReachable() {
        // 4 值全可达：每个值都是某迁移边的 toStatus（DRAFT=初始 seed，不在迁移边目标，单独核验为初始态）
        for (String s : ALL_MOVE_STATUSES) {
            if (ErpInvDocStatus.DOC_STATUS_DRAFT.equals(s)) {
                assertTrue(moveSm.initialStatuses().contains(s), "DRAFT 为初始态（seed 写入）");
            } else {
                boolean written = moveSm.transitions().stream().anyMatch(e -> s.equals(e.getToStatus()));
                assertTrue(written, "dict 值应有 writer 可达: " + s);
            }
        }
    }

    // ==================== 盘点单 docStatus 轴 ====================

    @Test
    public void takeNoDuplicateOrConflictingEdges() {
        List<ErpInvStockTakeStateMachine.TransitionDefinition> edges = takeSm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpInvStockTakeStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(4, edges.size(), "迁移矩阵应有 4 条边（startTake 1 + completeTake 1 + cancel 2 来源）");
    }

    @Test
    public void takeReachabilityFromInitial() {
        Set<String> reachable = reachableTakeFrom(ErpInvDocStatus.DOC_STATUS_DRAFT);
        assertTrue(reachable.contains(ErpInvDocStatus.DOC_STATUS_CONFIRMED), "从 DRAFT 应可达 CONFIRMED");
        assertTrue(reachable.contains(ErpInvDocStatus.DOC_STATUS_DONE), "从 DRAFT 应可达 DONE");
        assertTrue(reachable.contains(ErpInvDocStatus.DOC_STATUS_CANCELLED), "从 DRAFT 应可达 CANCELLED");
    }

    @Test
    public void takeStartTakeAllowsOnlyDraft() {
        assertTakeAllowsOnly("startTake", ErpInvDocStatus.DOC_STATUS_DRAFT);
    }

    @Test
    public void takeCompleteTakeAllowsOnlyConfirmed() {
        assertTakeAllowsOnly("completeTake", ErpInvDocStatus.DOC_STATUS_CONFIRMED);
    }

    @Test
    public void takeCancelAllowsOnlyDraftOrConfirmed() {
        for (String s : ALL_MOVE_STATUSES) {
            if (ErpInvDocStatus.DOC_STATUS_DRAFT.equals(s) || ErpInvDocStatus.DOC_STATUS_CONFIRMED.equals(s)) {
                takeSm.assertCanCancel(s); // 合法不抛
            } else {
                NopException ex = assertThrows(NopException.class, () -> takeSm.assertCanCancel(s),
                        "cancel 对 DONE/CANCELLED 应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
                assertEquals("cancel", ex.getParam(ErpInvStockTakeStateMachine.ARG_ACTION));
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
            }
        }
    }

    @Test
    public void takeTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpInvStockTakeStateMachine.TransitionDefinition e : takeSm.transitions()) {
            invokeTakeAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), takeTargetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    @Test
    public void takeTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpInvDocStatus.DOC_STATUS_DONE, ErpInvDocStatus.DOC_STATUS_CANCELLED),
                takeSm.terminalStatuses(), "终态集合 = {DONE, CANCELLED}");
        assertEquals(Collections.singletonList(ErpInvDocStatus.DOC_STATUS_DRAFT),
                takeSm.initialStatuses(), "初始态集合 = {DRAFT}");
        assertTrue(takeSm.isTerminal(ErpInvDocStatus.DOC_STATUS_DONE));
        assertTrue(takeSm.isTerminal(ErpInvDocStatus.DOC_STATUS_CANCELLED));
        assertFalse(takeSm.isTerminal(ErpInvDocStatus.DOC_STATUS_DRAFT));
        assertFalse(takeSm.isTerminal(ErpInvDocStatus.DOC_STATUS_CONFIRMED));
    }

    /** DONE 与 CANCELLED 均为真终态（无出边）。 */
    @Test
    public void takeTerminalsAreTrueTerminals() {
        for (ErpInvStockTakeStateMachine.TransitionDefinition e : takeSm.transitions()) {
            assertFalse(takeSm.isTerminal(e.getFromStatus()),
                    "真终态不应有出边: but edge " + e.getAction() + " leaves " + e.getFromStatus());
        }
    }

    @Test
    public void takeTargetStatusMethods() {
        // startTake 目标态 = CONFIRMED（owner doc 标签「盘点中 (COUNTING)」，dict 无 COUNTING）
        assertEquals(ErpInvDocStatus.DOC_STATUS_CONFIRMED, takeSm.startTakeTargetStatus());
        assertEquals(ErpInvDocStatus.DOC_STATUS_DONE, takeSm.completeTakeTargetStatus());
        assertEquals(ErpInvDocStatus.DOC_STATUS_CANCELLED, takeSm.cancelTargetStatus());
    }

    /**
     * COUNTING 标签漂移核对：owner doc §盘点单状态图标「盘点中 (COUNTING)」，但 dict erp-inv/move-status
     * 无 COUNTING；实际 startTake 目标态 = CONFIRMED。Bean 按既有 writer 建模（保持行为）。
     */
    @Test
    public void takeCountingLabelDriftResolvedToConfirmed() {
        boolean countingIsDictValue = ALL_MOVE_STATUSES.contains("COUNTING");
        assertFalse(countingIsDictValue, "erp-inv/move-status dict 无 COUNTING 值（标签漂移）");
        assertEquals(ErpInvDocStatus.DOC_STATUS_CONFIRMED, takeSm.startTakeTargetStatus(),
                "盘点中标签 (COUNTING) 实际对应 dict/code 值 CONFIRMED");
    }

    // ==================== helpers ====================

    private void assertMoveAllowsOnly(String action, String allowedFrom) {
        for (String s : ALL_MOVE_STATUSES) {
            if (allowedFrom.equals(s)) {
                invokeMoveAssert(action, s); // 不抛 = 合法
            } else {
                NopException ex = assertThrows(NopException.class, () -> invokeMoveAssert(action, s),
                        action + " 对非允许来源态应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                        "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + s);
                assertEquals(action, ex.getParam(ErpInvStockMoveStateMachine.ARG_ACTION));
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
            }
        }
    }

    private void assertTakeAllowsOnly(String action, String allowedFrom) {
        for (String s : ALL_MOVE_STATUSES) {
            if (allowedFrom.equals(s)) {
                invokeTakeAssert(action, s); // 不抛 = 合法
            } else {
                NopException ex = assertThrows(NopException.class, () -> invokeTakeAssert(action, s),
                        action + " 对非允许来源态应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                        "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + s);
                assertEquals(action, ex.getParam(ErpInvStockTakeStateMachine.ARG_ACTION));
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
            }
        }
    }

    private void invokeMoveAssert(String action, String status) {
        switch (action) {
            case "confirm":
                moveSm.assertCanConfirm(status);
                break;
            case "complete":
                moveSm.assertCanComplete(status);
                break;
            case "cancel":
                moveSm.assertCanCancel(status);
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String moveTargetStatusFor(String action) {
        switch (action) {
            case "confirm":
                return moveSm.confirmTargetStatus();
            case "complete":
                return moveSm.completeTargetStatus();
            case "cancel":
                return moveSm.cancelTargetStatus();
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private void invokeTakeAssert(String action, String status) {
        switch (action) {
            case "startTake":
                takeSm.assertCanStartTake(status);
                break;
            case "completeTake":
                takeSm.assertCanCompleteTake(status);
                break;
            case "cancel":
                takeSm.assertCanCancel(status);
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String takeTargetStatusFor(String action) {
        switch (action) {
            case "startTake":
                return takeSm.startTakeTargetStatus();
            case "completeTake":
                return takeSm.completeTakeTargetStatus();
            case "cancel":
                return takeSm.cancelTargetStatus();
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private Set<String> reachableMoveFrom(String start) {
        return reachableFromPairs(start, moveSm.transitions().stream()
                .map(e -> new String[]{e.getFromStatus(), e.getToStatus()})
                .collect(Collectors.toList()));
    }

    private Set<String> reachableTakeFrom(String start) {
        return reachableFromPairs(start, takeSm.transitions().stream()
                .map(e -> new String[]{e.getFromStatus(), e.getToStatus()})
                .collect(Collectors.toList()));
    }

    private static Set<String> reachableFromPairs(String start, List<String[]> edges) {
        Set<String> visited = new LinkedHashSet<>();
        List<String> frontier = new ArrayList<>();
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
