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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan Phase 1 Proof）。
 *
 * <p>针对 {@link ErpFinAccountingPeriodStateMachine} Bean 的纯矩阵完备性遍历：不经 BizModel 入口（层 3 职责），
 * 不断言副作用/审计/前置校验/红冲时序/kill-switch。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边；</li>
 *   <li>(b) 4 动作合法/非法来源态：openPeriod（NEVER_OPENED 合法）、close（OPEN 合法）、
 *       finalize（CLOSED 合法）、reverseClose（CLOSED_FINAL 合法），其余态皆非法；</li>
 *   <li>(c) 终态 CLOSED_FINAL 仅 reverseClose 恢复出边（无前向推进边）；</li>
 *   <li>(d) {@code transitions()} 元数据与显式方法语义一致（含 close 两段：OPEN→CLOSING 事务内进入 +
 *       CLOSING→CLOSED 事务内完成）；</li>
 *   <li>(e) initial={NEVER_OPENED} / terminal={CLOSED_FINAL} 集合正确；</li>
 *   <li>可达性：从 NEVER_OPENED 可达全部 5 态（CLOSING 经 close 进入段瞬态可达）。</li>
 * </ul>
 *
 * <p><b>CLOSING 瞬态裁定</b>（plan Phase 1 Decision）：closePeriod 为 {@code @BizMutation}（事务包裹），
 * CLOSING 在事务内设置后于 CLOSED，失败则整 mutation 回滚（CLOSING 不持久化）；故 {@code assertCanClose(CLOSING)}
 * 抛非法（CLOSING 不可作「发起结账」入口，仅事务内瞬态中间态），CLOSING→OPEN（结账失败）= 事务回滚语义非命名边。
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpFinAccountingPeriodStateMachineMatrix {

    private static final List<String> ALL_DICT_STATUSES = Arrays.asList(
            ErpFinConstants.PERIOD_STATUS_NEVER_OPENED,
            ErpFinConstants.PERIOD_STATUS_OPEN,
            ErpFinConstants.PERIOD_STATUS_CLOSING,
            ErpFinConstants.PERIOD_STATUS_CLOSED,
            ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL);

    private final ErpFinAccountingPeriodStateMachine sm = new ErpFinAccountingPeriodStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpFinAccountingPeriodStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpFinAccountingPeriodStateMachine.TransitionDefinition e : edges) {
            // close 两段同 action 不同 fromStatus（OPEN / CLOSING），故 key 须含 fromStatus 区分
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        // 4 命名动作（openPeriod/close/finalize/reverseClose），close 编码为两段 → 5 条边
        assertEquals(5, edges.size(), "迁移矩阵应有 5 条边（openPeriod/finalize/reverseClose 各 1 + close 两段）");
    }

    // ---------- (b) 4 动作合法/非法来源态 ----------

    @Test
    public void testOpenPeriodLegalForNeverOpenedAndIllegalForOthers() {
        sm.assertCanOpenPeriod(ErpFinConstants.PERIOD_STATUS_NEVER_OPENED); // 不抛
        for (String s : illegalFor(ErpFinConstants.PERIOD_STATUS_NEVER_OPENED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanOpenPeriod(s),
                    "openPeriod 对非 NEVER_OPENED 应非法: " + s);
            assertCommonIllegalMetadata(ex, "openPeriod", s, ErpFinConstants.PERIOD_STATUS_NEVER_OPENED);
        }
    }

    @Test
    public void testCloseLegalForOpenAndIllegalForOthers() {
        sm.assertCanClose(ErpFinConstants.PERIOD_STATUS_OPEN); // 不抛
        for (String s : illegalFor(ErpFinConstants.PERIOD_STATUS_OPEN)) {
            // 注意 CLOSING 亦非法：CLOSING 不可作「发起结账」入口（事务内瞬态中间态，见类级裁定）
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanClose(s),
                    "close 对非 OPEN 应非法: " + s);
            assertCommonIllegalMetadata(ex, "close", s, ErpFinConstants.PERIOD_STATUS_OPEN);
        }
    }

    @Test
    public void testFinalizeLegalForClosedAndIllegalForOthers() {
        sm.assertCanFinalize(ErpFinConstants.PERIOD_STATUS_CLOSED); // 不抛
        for (String s : illegalFor(ErpFinConstants.PERIOD_STATUS_CLOSED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanFinalize(s),
                    "finalize 对非 CLOSED 应非法: " + s);
            assertCommonIllegalMetadata(ex, "finalize", s, ErpFinConstants.PERIOD_STATUS_CLOSED);
        }
    }

    @Test
    public void testReverseCloseLegalForClosedFinalAndIllegalForOthers() {
        sm.assertCanReverseClose(ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL); // 不抛
        for (String s : illegalFor(ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanReverseClose(s),
                    "reverseClose 对非 CLOSED_FINAL 应非法: " + s);
            assertCommonIllegalMetadata(ex, "reverseClose", s, ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL);
        }
    }

    // ---------- (c) 终态 CLOSED_FINAL 无前向推进出边（唯一 reverseClose 恢复边）----------

    @Test
    public void testTerminalStatusHasOnlyRecoveryOutgoingEdge() {
        // CLOSED_FINAL 为业务终态；其唯一出边为 reverseClose 恢复路径（owner doc §对象二 §3/§5），
        // 无前向推进边。这区别于「终态零出边」的一般死状态启发式——reverseClose 是显式管理员恢复 action。
        for (String terminal : sm.terminalStatuses()) {
            List<ErpFinAccountingPeriodStateMachine.TransitionDefinition> outgoing = sm.transitions().stream()
                    .filter(e -> e.getFromStatus().equals(terminal))
                    .collect(Collectors.toList());
            assertEquals(1, outgoing.size(), "终态 " + terminal + " 应仅有 reverseClose 恢复出边");
            assertEquals("reverseClose", outgoing.get(0).getAction());
            assertEquals(ErpFinConstants.PERIOD_STATUS_OPEN, outgoing.get(0).getToStatus());
        }
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpFinAccountingPeriodStateMachine.TransitionDefinition e : sm.transitions()) {
            switch (e.getAction()) {
                case "openPeriod":
                    sm.assertCanOpenPeriod(e.getFromStatus());
                    assertEquals(e.getToStatus(), sm.openPeriodTargetStatus());
                    break;
                case "close":
                    // 两段：进入段 OPEN→CLOSING（assertCanClose 守卫入口来源 OPEN）；
                    // 完成段 CLOSING→CLOSED（CLOSING 为事务内瞬态，fromStatus 须 == closeEnteringTargetStatus，
                    // toStatus 须 == closeTargetStatus）。CLOSING 不经 assertCanClose（见类级裁定）。
                    if (ErpFinConstants.PERIOD_STATUS_OPEN.equals(e.getFromStatus())) {
                        sm.assertCanClose(e.getFromStatus());
                        assertEquals(sm.closeEnteringTargetStatus(), e.getToStatus(),
                                "close 进入段 toStatus 应为 CLOSING");
                    } else {
                        assertEquals(sm.closeEnteringTargetStatus(), e.getFromStatus(),
                                "close 完成段 fromStatus 应为事务内瞬态 CLOSING");
                        assertEquals(sm.closeTargetStatus(), e.getToStatus(),
                                "close 完成段 toStatus 应为 CLOSED");
                    }
                    break;
                case "finalize":
                    sm.assertCanFinalize(e.getFromStatus());
                    assertEquals(e.getToStatus(), sm.finalizeTargetStatus());
                    break;
                case "reverseClose":
                    sm.assertCanReverseClose(e.getFromStatus());
                    assertEquals(e.getToStatus(), sm.reverseCloseTargetStatus());
                    break;
                default:
                    throw new AssertionError("未知 action: " + e.getAction());
            }
        }
    }

    // ---------- (e) 终态/初始态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Collections.singletonList(ErpFinConstants.PERIOD_STATUS_NEVER_OPENED),
                sm.initialStatuses(), "初始态集合 = {NEVER_OPENED}");
        assertEquals(Collections.singletonList(ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL),
                sm.terminalStatuses(), "终态集合 = {CLOSED_FINAL}");

        assertTrue(sm.isTerminal(ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL));
        assertFalse(sm.isTerminal(ErpFinConstants.PERIOD_STATUS_CLOSED), "CLOSED 为待复核中间态，非终态");
        assertFalse(sm.isTerminal(ErpFinConstants.PERIOD_STATUS_OPEN));
        assertFalse(sm.isTerminal(ErpFinConstants.PERIOD_STATUS_CLOSING));
        assertFalse(sm.isTerminal(ErpFinConstants.PERIOD_STATUS_NEVER_OPENED));
    }

    // ---------- 可达性：从 NEVER_OPENED 可达全部 5 态 ----------

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpFinConstants.PERIOD_STATUS_NEVER_OPENED);
        // 主路径 NEVER_OPENED→OPEN→CLOSING→CLOSED→CLOSED_FINAL 全部可达（CLOSING 经 close 进入段瞬态可达）。
        // reachableFrom 按约定排除起点 NEVER_OPENED，故断言其余 4 态皆可达（无死状态）。
        for (String s : Arrays.asList(ErpFinConstants.PERIOD_STATUS_OPEN, ErpFinConstants.PERIOD_STATUS_CLOSING,
                ErpFinConstants.PERIOD_STATUS_CLOSED, ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL)) {
            assertTrue(reachable.contains(s), "从 NEVER_OPENED 应可达: " + s);
        }
    }

    // ---------- 目标态方法 ----------

    @Test
    public void testTargetStatusMethods() {
        assertEquals(ErpFinConstants.PERIOD_STATUS_OPEN, sm.openPeriodTargetStatus());
        assertEquals(ErpFinConstants.PERIOD_STATUS_CLOSING, sm.closeEnteringTargetStatus());
        assertEquals(ErpFinConstants.PERIOD_STATUS_CLOSED, sm.closeTargetStatus());
        assertEquals(ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL, sm.finalizeTargetStatus());
        assertEquals(ErpFinConstants.PERIOD_STATUS_OPEN, sm.reverseCloseTargetStatus());
    }

    // ---------- helpers ----------

    /** 返回除 legal 外的全部 dict 态（即该单来源态动作的非法来源集）。 */
    private static List<String> illegalFor(String legal) {
        return ALL_DICT_STATUSES.stream().filter(s -> !s.equals(legal)).collect(Collectors.toList());
    }

    private static void assertCommonIllegalMetadata(NopException ex, String action, String currentStatus,
                                                    String expectedStatus) {
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "Bean 报告 common 层非法迁移码: status=" + currentStatus);
        assertEquals(action, ex.getParam(ErpFinAccountingPeriodStateMachine.ARG_ACTION),
                "拒绝元数据携带动作名: status=" + currentStatus);
        assertEquals(currentStatus, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                "拒绝元数据携带当前态: status=" + currentStatus);
        assertEquals(expectedStatus, ex.getParam(ErpCommonErrors.ARG_EXPECTED_STATUS),
                "拒绝元数据携带期望态: status=" + currentStatus);
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
            for (ErpFinAccountingPeriodStateMachine.TransitionDefinition e : sm.transitions()) {
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
