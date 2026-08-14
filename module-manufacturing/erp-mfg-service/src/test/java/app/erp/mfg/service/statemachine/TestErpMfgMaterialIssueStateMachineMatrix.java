package app.erp.mfg.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.mfg.service.ErpMfgConstants;
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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-14-0930-1 Phase 3 Proof）。
 *
 * <p>针对 {@link ErpMfgMaterialIssueStateMachine} Bean（M4.39 docStatus 单轴）的纯矩阵完备性遍历：
 * 不经 BizModel/Processor 入口（层 3 职责），不断言副作用/审计（出库移动单/领料 GL 过账/红冲归层 3）。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边（2 边唯一 action|fromStatus 键）；</li>
 *   <li>(b) 从 DRAFT 可达 DONE（confirm），DONE 经 reverseConfirm 可达 CANCELLED；</li>
 *   <li>(c) 各 {@code assertCanXxx} 合法来源态通过、非法来源态抛 common 码携带 {@code action}/{@code fromStatus}；</li>
 *   <li>(d) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(e) 终态 {DONE, CANCELLED} 无出边；初始态 {DRAFT}；</li>
 *   <li>(f) CONFIRMED 为瞬态中间态（confirm 动作内部写入、有 writer、非死状态）：不入初始/终态集、
 *       不暴露为命名动作边（confirm 入口守卫仅 DRAFT），Bean javadoc 显式标注（plan Phase 3 Decision）。</li>
 * </ul>
 *
 * <p>层 2 四方对照（MaterialIssue 单轴单条）：dict {@code erp-mfg/issue-status}（4 值）↔
 * {@code docs/design/manufacturing/state-machine.md} §实现约定 ↔ Bean 元数据 ↔ 全部 writer
 * （Confirm/ReverseConfirm Processor live + CONFIRMED 瞬态中间态 writer + CRUD 路径 §9.4 选项 c 排除）。
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpMfgMaterialIssueStateMachineMatrix {

    private static final List<String> ALL_STATUSES = Arrays.asList(
            ErpMfgConstants.ISSUE_STATUS_DRAFT,
            ErpMfgConstants.ISSUE_STATUS_CONFIRMED,
            ErpMfgConstants.ISSUE_STATUS_DONE,
            ErpMfgConstants.ISSUE_STATUS_CANCELLED);

    private final ErpMfgMaterialIssueStateMachine sm = new ErpMfgMaterialIssueStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpMfgMaterialIssueStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpMfgMaterialIssueStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(2, edges.size(), "迁移矩阵应有 2 条边（confirm DRAFT→DONE + reverseConfirm DONE→CANCELLED）");
    }

    // ---------- (b) 从 DRAFT 可达 ----------

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpMfgConstants.ISSUE_STATUS_DRAFT);
        assertTrue(reachable.contains(ErpMfgConstants.ISSUE_STATUS_DONE), "从 DRAFT 应可达 DONE（confirm）");
        assertTrue(reachable.contains(ErpMfgConstants.ISSUE_STATUS_CANCELLED), "从 DRAFT 应可达 CANCELLED（confirm→reverseConfirm）");
        // CONFIRMED 为瞬态中间态，不暴露为独立动作边，故经 transitions() 图不可达
        assertFalse(reachable.contains(ErpMfgConstants.ISSUE_STATUS_CONFIRMED),
                "CONFIRMED 为瞬态中间态，不暴露为独立动作边，不应经 transitions() 图可达");
    }

    // ---------- (c) assertCanXxx 合法/非法 ----------

    @Test
    public void testAssertCanConfirmLegalAndIllegal() {
        sm.assertCanConfirm(ErpMfgConstants.ISSUE_STATUS_DRAFT);
        assertEquals(ErpMfgConstants.ISSUE_STATUS_DONE, sm.confirmTargetStatus());
        for (String illegal : illegalFor(ALL_STATUSES, ErpMfgConstants.ISSUE_STATUS_DRAFT)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanConfirm(illegal),
                    "confirm 对非 DRAFT 应非法: " + illegal);
            assertCommonTransitionMetadata(ex, "confirm", illegal);
        }
    }

    @Test
    public void testAssertCanReverseConfirmLegalAndIllegal() {
        sm.assertCanReverseConfirm(ErpMfgConstants.ISSUE_STATUS_DONE);
        assertEquals(ErpMfgConstants.ISSUE_STATUS_CANCELLED, sm.reverseConfirmTargetStatus());
        for (String illegal : illegalFor(ALL_STATUSES, ErpMfgConstants.ISSUE_STATUS_DONE)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanReverseConfirm(illegal),
                    "reverseConfirm 对非 DONE 应非法: " + illegal);
            assertCommonTransitionMetadata(ex, "reverseConfirm", illegal);
        }
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpMfgMaterialIssueStateMachine.TransitionDefinition e : sm.transitions()) {
            invokeAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (e) 终态出边约束：CANCELLED 无出边；DONE 为「可逆业务终态」仅 reverseConfirm 出边 ----------

    @Test
    public void testTerminalStatusesHaveNoOutgoingEdges() {
        for (ErpMfgMaterialIssueStateMachine.TransitionDefinition e : sm.transitions()) {
            assertFalse(e.getFromStatus().equals(ErpMfgConstants.ISSUE_STATUS_CANCELLED),
                    "CANCELLED 终态不应有出边: edge=" + e.getAction());
        }
    }

    /** DONE 是「可逆业务终态」——经 reverseConfirm 有出边（不适用「终态无出边」强断言）。 */
    @Test
    public void testDoneIsReversibleTerminal() {
        boolean doneHasOutgoing = false;
        for (ErpMfgMaterialIssueStateMachine.TransitionDefinition e : sm.transitions()) {
            if (e.getFromStatus().equals(ErpMfgConstants.ISSUE_STATUS_DONE)) {
                doneHasOutgoing = true;
                assertEquals("reverseConfirm", e.getAction(), "DONE 的唯一出边应为 reverseConfirm");
                assertEquals(ErpMfgConstants.ISSUE_STATUS_CANCELLED, e.getToStatus());
            }
        }
        assertTrue(doneHasOutgoing, "DONE 应有 reverseConfirm 出边（可逆终态）");
    }

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpMfgConstants.ISSUE_STATUS_DONE, ErpMfgConstants.ISSUE_STATUS_CANCELLED),
                sm.terminalStatuses(), "终态集合 = {DONE, CANCELLED}");
        assertEquals(java.util.Collections.singletonList(ErpMfgConstants.ISSUE_STATUS_DRAFT),
                sm.initialStatuses(), "初始态集合 = {DRAFT}");

        assertTrue(sm.isTerminal(ErpMfgConstants.ISSUE_STATUS_DONE));
        assertTrue(sm.isTerminal(ErpMfgConstants.ISSUE_STATUS_CANCELLED));
        assertFalse(sm.isTerminal(ErpMfgConstants.ISSUE_STATUS_DRAFT));
        assertFalse(sm.isTerminal(ErpMfgConstants.ISSUE_STATUS_CONFIRMED));
    }

    // ---------- (f) CONFIRMED 为瞬态中间态（有 writer、非死状态、非命名动作边界） ----------

    @Test
    public void testConfirmedIsTransientIntermediateNotDeadState() {
        String confirmed = ErpMfgConstants.ISSUE_STATUS_CONFIRMED;
        // 非命名动作边界：不入初始/终态集、不暴露为 transitions() 边（confirm 入口守卫仅 DRAFT）
        assertFalse(sm.initialStatuses().contains(confirmed), "CONFIRMED 不入初始态集");
        assertFalse(sm.terminalStatuses().contains(confirmed), "CONFIRMED 不入终态集");
        assertFalse(sm.isTerminal(confirmed), "CONFIRMED 非终态");
        for (ErpMfgMaterialIssueStateMachine.TransitionDefinition e : sm.transitions()) {
            assertFalse(confirmed.equals(e.getFromStatus()), "CONFIRMED 不暴露为动作来源边");
            assertFalse(confirmed.equals(e.getToStatus()), "CONFIRMED 不暴露为动作目标边");
        }
        // 非死状态：CONFIRMED 有 writer（confirm 动作内部 DRAFT→CONFIRMED 两步写入，ConfirmProcessor），
        // 非「零 writer 预留死状态」（对比 JobCard TRANSFERRED 两态）——此处断言 confirm 入口守卫拒绝 CONFIRMED
        // （命名动作边界之外），但 Bean javadoc/层 2 四方对照登记 CONFIRMED 为瞬态中间态（有 writer）
        assertThrows(NopException.class, () -> sm.assertCanConfirm(confirmed), "confirm 入口守卫仅 DRAFT，CONFIRMED 拒绝");
    }

    // ---------- helpers ----------

    private void invokeAssert(String action, String status) {
        switch (action) {
            case "confirm": sm.assertCanConfirm(status); break;
            case "reverseConfirm": sm.assertCanReverseConfirm(status); break;
            default: throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "confirm": return sm.confirmTargetStatus();
            case "reverseConfirm": return sm.reverseConfirmTargetStatus();
            default: throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private void assertCommonTransitionMetadata(NopException ex, String action, String status) {
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + status);
        assertEquals(action, ex.getParam(ErpMfgMaterialIssueStateMachine.ARG_ACTION), "拒绝元数据携带动作名");
        assertEquals(status, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS), "拒绝元数据携带当前态");
    }

    private List<String> illegalFor(List<String> all, String... legal) {
        Set<String> legalSet = new HashSet<>(Arrays.asList(legal));
        return all.stream().filter(s -> !legalSet.contains(s)).collect(Collectors.toList());
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
            for (ErpMfgMaterialIssueStateMachine.TransitionDefinition e : sm.transitions()) {
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
