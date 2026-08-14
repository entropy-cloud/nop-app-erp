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
 * <p>针对 {@link ErpAstDepreciationScheduleStateMachine}（ErpAstDepreciationSchedule.status 执行状态轴，
 * 4 命名动作）的纯矩阵完备性遍历：不经 BizModel 入口（层 3 职责），不断言副作用/审计。
 *
 * <p>覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边（4 条边，4 命名动作）；</li>
 *   <li>(b) 从 PENDING 可达 EXECUTED/REVERSED/CANCELLED，且 CANCELLED 经 restore 回到 PENDING
 *       （CANCELLED 非终态——业务恢复路径，对齐 Movement REJECTED 非终态先例）；</li>
 *   <li>(c) 各动作合法来源态通过、非法来源态抛 common 层码（携带 action/currentStatus）；
 *       execute null 归一化 PENDING 合法（新建条目创建种子语义）；</li>
 *   <li>(d) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(e) 终态/初始态集合正确（terminal={EXECUTED（可逆终态）, REVERSED}，initial={PENDING}）。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpAstDepreciationScheduleStateMachineMatrix {

    /** dict erp-ast/depreciation-schedule-status 全 4 值。 */
    private static final List<String> ALL_SCHEDULE_STATUSES = Arrays.asList(
            ErpAstConstants.SCHEDULE_STATUS_PENDING,
            ErpAstConstants.SCHEDULE_STATUS_EXECUTED,
            ErpAstConstants.SCHEDULE_STATUS_REVERSED,
            ErpAstConstants.SCHEDULE_STATUS_CANCELLED);

    private final ErpAstDepreciationScheduleStateMachine sm = new ErpAstDepreciationScheduleStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void noDuplicateOrConflictingEdges() {
        List<ErpAstDepreciationScheduleStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpAstDepreciationScheduleStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(4, edges.size(), "迁移矩阵应有 4 条边（4 命名动作）");
    }

    // ---------- (b) 可达性 ----------

    @Test
    public void reachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpAstConstants.SCHEDULE_STATUS_PENDING);
        assertTrue(reachable.contains(ErpAstConstants.SCHEDULE_STATUS_EXECUTED), "从 PENDING 应可达 EXECUTED");
        assertTrue(reachable.contains(ErpAstConstants.SCHEDULE_STATUS_REVERSED),
                "从 PENDING 经 execute→reverse 应可达 REVERSED");
        assertTrue(reachable.contains(ErpAstConstants.SCHEDULE_STATUS_CANCELLED), "从 PENDING 应可达 CANCELLED");
        // CANCELLED→PENDING 经 restore 回到初始态（合法往复，退出条件为执行/作废到终态）
        boolean restored = sm.transitions().stream()
                .anyMatch(e -> ErpAstConstants.SCHEDULE_STATUS_CANCELLED.equals(e.getFromStatus())
                        && ErpAstConstants.SCHEDULE_STATUS_PENDING.equals(e.getToStatus()));
        assertTrue(restored, "CANCELLED 经 restore 应有回到 PENDING 的边");
    }

    // ---------- (c) 各动作合法/非法来源态 ----------

    @Test
    public void executeAllowsOnlyPending() {
        assertAllowsOnly("execute", ErpAstConstants.SCHEDULE_STATUS_PENDING);
        assertEquals(ErpAstConstants.SCHEDULE_STATUS_EXECUTED, sm.executeTargetStatus());
    }

    @Test
    public void executeNullTreatedAsPending() {
        sm.assertCanExecute(null); // null 归一化为 PENDING（创建种子语义），合法不抛
    }

    @Test
    public void reverseAllowsOnlyExecuted() {
        assertAllowsOnly("reverse", ErpAstConstants.SCHEDULE_STATUS_EXECUTED);
        assertEquals(ErpAstConstants.SCHEDULE_STATUS_REVERSED, sm.reverseTargetStatus());
    }

    @Test
    public void cancelAllowsOnlyPending() {
        assertAllowsOnly("dispose-cancel", ErpAstConstants.SCHEDULE_STATUS_PENDING);
        assertEquals(ErpAstConstants.SCHEDULE_STATUS_CANCELLED, sm.cancelTargetStatus());
    }

    @Test
    public void restoreAllowsOnlyCancelled() {
        assertAllowsOnly("restore", ErpAstConstants.SCHEDULE_STATUS_CANCELLED);
        assertEquals(ErpAstConstants.SCHEDULE_STATUS_PENDING, sm.restoreTargetStatus());
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void transitionsMetadataConsistentWithExplicitMethods() {
        for (ErpAstDepreciationScheduleStateMachine.TransitionDefinition e : sm.transitions()) {
            invokeAssert(e.getAction(), e.getFromStatus());
            assertEquals(targetStatusFor(e.getAction()), e.getToStatus(),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (e) 终态/初始态集合 ----------

    @Test
    public void terminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpAstConstants.SCHEDULE_STATUS_EXECUTED, ErpAstConstants.SCHEDULE_STATUS_REVERSED),
                sm.terminalStatuses(), "终态集合 = {EXECUTED, REVERSED}");
        assertEquals(Arrays.asList(ErpAstConstants.SCHEDULE_STATUS_PENDING),
                sm.initialStatuses(), "初始态集合 = {PENDING}");

        assertTrue(sm.isTerminal(ErpAstConstants.SCHEDULE_STATUS_EXECUTED),
                "EXECUTED 为可逆终态（经 reverse 有出边，不适用「终态无出边」强断言）");
        assertTrue(sm.isTerminal(ErpAstConstants.SCHEDULE_STATUS_REVERSED), "REVERSED 为红冲后终态");
        assertFalse(sm.isTerminal(ErpAstConstants.SCHEDULE_STATUS_PENDING));
        assertFalse(sm.isTerminal(ErpAstConstants.SCHEDULE_STATUS_CANCELLED),
                "CANCELLED 非终态（经 restore 可恢复回 PENDING，业务恢复路径）");
    }

    /**
     * EXECUTED 为可逆终态——经 reverse 有出边，故不适用「终态无出边」强断言。
     * 仅断言：EXECUTED 在 transitions() 中存在出边（reverse），如实反映可逆终态语义。
     */
    @Test
    public void executedIsReversibleTerminal() {
        boolean hasOutgoing = sm.transitions().stream()
                .anyMatch(e -> ErpAstConstants.SCHEDULE_STATUS_EXECUTED.equals(e.getFromStatus()));
        assertTrue(hasOutgoing, "EXECUTED 经 reverse 应有出边（可逆终态）");
        assertEquals("reverse",
                sm.transitions().stream()
                        .filter(e -> ErpAstConstants.SCHEDULE_STATUS_EXECUTED.equals(e.getFromStatus()))
                        .findFirst().get().getAction(),
                "EXECUTED 的唯一出边动作应为 reverse");
    }

    @Test
    public void allDictStatusesClassified() {
        // 机器化核对：dict 全 4 值在 Bean 中均有语义归类
        for (String s : ALL_SCHEDULE_STATUSES) {
            sm.isTerminal(s); // 不抛即可
        }
    }

    // ==================== helpers ====================

    private void assertAllowsOnly(String action, String allowedFrom) {
        for (String s : ALL_SCHEDULE_STATUSES) {
            if (allowedFrom.equals(s)) {
                invokeAssert(action, s); // 不抛 = 合法
            } else {
                NopException ex = assertThrows(NopException.class, () -> invokeAssert(action, s),
                        action + " 对非允许来源态应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                        "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + s);
                assertEquals(action, ex.getParam(ErpAstDepreciationScheduleStateMachine.ARG_ACTION),
                        "拒绝元数据携带动作名: action=" + action);
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                        "拒绝元数据携带当前态: action=" + action + ", status=" + s);
            }
        }
    }

    private void invokeAssert(String action, String status) {
        switch (action) {
            case "execute":
                sm.assertCanExecute(status);
                break;
            case "reverse":
                sm.assertCanReverse(status);
                break;
            case "dispose-cancel":
                sm.assertCanCancel(status);
                break;
            case "restore":
                sm.assertCanRestore(status);
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "execute":
                return sm.executeTargetStatus();
            case "reverse":
                return sm.reverseTargetStatus();
            case "dispose-cancel":
                return sm.cancelTargetStatus();
            case "restore":
                return sm.restoreTargetStatus();
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
            for (ErpAstDepreciationScheduleStateMachine.TransitionDefinition e : sm.transitions()) {
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
