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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan Phase 1 Proof）。
 *
 * <p>针对 {@link ErpMfgJobCardStateMachine} Bean 的纯矩阵完备性遍历：不经 BizModel/Processor 入口（层 3 职责），
 * 不断言副作用/报工归集。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边；</li>
 *   <li>(b) 从 OPEN 可达全部非死、非终态目标；</li>
 *   <li>(c) startJob 仅 OPEN 合法；</li>
 *   <li>(d) submitJob 多来源 {WORK_IN_PROGRESS, ON_HOLD} 合法、对终态/死状态非法；</li>
 *   <li>(e) completeJob 仅 SUBMITTED 合法；</li>
 *   <li>(f) holdJob 仅 WORK_IN_PROGRESS 合法；</li>
 *   <li>(g) resumeJob 仅 ON_HOLD 合法；</li>
 *   <li>(h) cancelJob 多来源 {OPEN, WORK_IN_PROGRESS, ON_HOLD} 合法、对终态/死状态非法；</li>
 *   <li>(i) recordWork 来源 allow-list {WORK_IN_PROGRESS, SUBMITTED}（validation-only，无目标态、不计入 transitions）；</li>
 *   <li>(j) 终态 {COMPLETED, CANCELLED} 无出边；</li>
 *   <li>(k) {@code transitions()} 元数据（9 边）与显式方法语义一致；</li>
 *   <li>(l) 终态/初始态集合正确；</li>
 *   <li>(m) PARTIALLY_TRANSFERRED / MATERIAL_TRANSFERRED 无任何边、不在终态集（预留死状态，Bean 不编码两态）。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpMfgJobCardStateMachineMatrix {

    private static final List<String> ALL_STATUSES = Arrays.asList(
            ErpMfgConstants.JOB_CARD_STATUS_OPEN,
            ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS,
            ErpMfgConstants.JOB_CARD_STATUS_PARTIALLY_TRANSFERRED,
            ErpMfgConstants.JOB_CARD_STATUS_MATERIAL_TRANSFERRED,
            ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD,
            ErpMfgConstants.JOB_CARD_STATUS_SUBMITTED,
            ErpMfgConstants.JOB_CARD_STATUS_COMPLETED,
            ErpMfgConstants.JOB_CARD_STATUS_CANCELLED);

    private final ErpMfgJobCardStateMachine sm = new ErpMfgJobCardStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpMfgJobCardStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpMfgJobCardStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(9, edges.size(), "迁移矩阵应有 9 条边（startJob 1 + submitJob 2 + completeJob 1 + holdJob 1 + resumeJob 1 + cancelJob 3）");
    }

    // ---------- (b) 从 OPEN 可达 ----------

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpMfgConstants.JOB_CARD_STATUS_OPEN);
        assertTrue(reachable.contains(ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS), "从 OPEN 应可达 WORK_IN_PROGRESS");
        assertTrue(reachable.contains(ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD), "从 OPEN 应可达 ON_HOLD");
        assertTrue(reachable.contains(ErpMfgConstants.JOB_CARD_STATUS_SUBMITTED), "从 OPEN 应可达 SUBMITTED");
        assertTrue(reachable.contains(ErpMfgConstants.JOB_CARD_STATUS_COMPLETED), "从 OPEN 应可达 COMPLETED");
        assertTrue(reachable.contains(ErpMfgConstants.JOB_CARD_STATUS_CANCELLED), "从 OPEN 应可达 CANCELLED");
        // 两 TRANSFERRED 死状态从 OPEN 不可达
        assertFalse(reachable.contains(ErpMfgConstants.JOB_CARD_STATUS_PARTIALLY_TRANSFERRED),
                "PARTIALLY_TRANSFERRED 为预留死状态，从 OPEN 不应可达");
        assertFalse(reachable.contains(ErpMfgConstants.JOB_CARD_STATUS_MATERIAL_TRANSFERRED),
                "MATERIAL_TRANSFERRED 为预留死状态，从 OPEN 不应可达");
    }

    // ---------- (c) startJob 仅 OPEN 合法 ----------

    @Test
    public void testStartJobAllowsOnlyOpen() {
        sm.assertCanStartJob(ErpMfgConstants.JOB_CARD_STATUS_OPEN);
        assertEquals(ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS, sm.startJobTargetStatus());
        for (String s : illegalFor(ALL_STATUSES,
                ErpMfgConstants.JOB_CARD_STATUS_OPEN)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanStartJob(s),
                    "startJob 对非 OPEN 应非法: " + s);
            assertCommonTransitionMetadata(ex, "startJob", s);
        }
    }

    // ---------- (d) submitJob 多来源 {WORK_IN_PROGRESS, ON_HOLD} 合法、对终态/死状态非法 ----------

    @Test
    public void testSubmitJobLegalForWipAndOnHold() {
        sm.assertCanSubmitJob(ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS);
        sm.assertCanSubmitJob(ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD);
        assertEquals(ErpMfgConstants.JOB_CARD_STATUS_SUBMITTED, sm.submitJobTargetStatus());
        for (String s : illegalFor(ALL_STATUSES,
                ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS,
                ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanSubmitJob(s),
                    "submitJob 对非 WIP/ON_HOLD 应非法: " + s);
            assertCommonTransitionMetadata(ex, "submitJob", s);
        }
    }

    // ---------- (e) completeJob 仅 SUBMITTED 合法 ----------

    @Test
    public void testCompleteJobAllowsOnlySubmitted() {
        sm.assertCanCompleteJob(ErpMfgConstants.JOB_CARD_STATUS_SUBMITTED);
        assertEquals(ErpMfgConstants.JOB_CARD_STATUS_COMPLETED, sm.completeJobTargetStatus());
        for (String s : illegalFor(ALL_STATUSES,
                ErpMfgConstants.JOB_CARD_STATUS_SUBMITTED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanCompleteJob(s),
                    "completeJob 对非 SUBMITTED 应非法: " + s);
            assertCommonTransitionMetadata(ex, "completeJob", s);
        }
    }

    // ---------- (f) holdJob 仅 WORK_IN_PROGRESS 合法 ----------

    @Test
    public void testHoldJobAllowsOnlyWip() {
        sm.assertCanHoldJob(ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS);
        assertEquals(ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD, sm.holdJobTargetStatus());
        for (String s : illegalFor(ALL_STATUSES,
                ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanHoldJob(s),
                    "holdJob 对非 WIP 应非法: " + s);
            assertCommonTransitionMetadata(ex, "holdJob", s);
        }
    }

    // ---------- (g) resumeJob 仅 ON_HOLD 合法 ----------

    @Test
    public void testResumeJobAllowsOnlyOnHold() {
        sm.assertCanResumeJob(ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD);
        assertEquals(ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS, sm.resumeJobTargetStatus());
        for (String s : illegalFor(ALL_STATUSES,
                ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanResumeJob(s),
                    "resumeJob 对非 ON_HOLD 应非法: " + s);
            assertCommonTransitionMetadata(ex, "resumeJob", s);
        }
    }

    // ---------- (h) cancelJob 多来源 {OPEN, WORK_IN_PROGRESS, ON_HOLD} 合法、对终态/死状态非法 ----------

    @Test
    public void testCancelJobLegalForOpenWipOnHold() {
        sm.assertCanCancelJob(ErpMfgConstants.JOB_CARD_STATUS_OPEN);
        sm.assertCanCancelJob(ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS);
        sm.assertCanCancelJob(ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD);
        assertEquals(ErpMfgConstants.JOB_CARD_STATUS_CANCELLED, sm.cancelJobTargetStatus());
        for (String s : illegalFor(ALL_STATUSES,
                ErpMfgConstants.JOB_CARD_STATUS_OPEN,
                ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS,
                ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanCancelJob(s),
                    "cancelJob 对非 OPEN/WIP/ON_HOLD 应非法: " + s);
            assertCommonTransitionMetadata(ex, "cancelJob", s);
        }
    }

    // ---------- (i) recordWork 来源 allow-list {WORK_IN_PROGRESS, SUBMITTED}（validation-only） ----------

    @Test
    public void testRecordWorkAllowList() {
        // 合法来源：WORK_IN_PROGRESS、SUBMITTED
        sm.assertCanRecordWork(ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS);
        sm.assertCanRecordWork(ErpMfgConstants.JOB_CARD_STATUS_SUBMITTED);
        // recordWork 无目标态方法（validation-only，不改 status）
        // 其余全部非法
        for (String s : illegalFor(ALL_STATUSES,
                ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS,
                ErpMfgConstants.JOB_CARD_STATUS_SUBMITTED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanRecordWork(s),
                    "recordWork 对非 WIP/SUBMITTED 应非法: " + s);
            assertCommonTransitionMetadata(ex, "recordWork", s);
        }
    }

    // ---------- (j) 终态 {COMPLETED, CANCELLED} 无出边 ----------

    @Test
    public void testTerminalStatusesHaveNoOutgoingEdges() {
        for (String terminal : sm.terminalStatuses()) {
            for (ErpMfgJobCardStateMachine.TransitionDefinition e : sm.transitions()) {
                assertFalse(e.getFromStatus().equals(terminal),
                        "终态不应有出边: terminal=" + terminal + ", but edge " + e.getAction() + " leaves it");
            }
        }
    }

    // ---------- (k) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpMfgJobCardStateMachine.TransitionDefinition e : sm.transitions()) {
            invokeAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
        // recordWork 不在 transitions（validation-only）
        for (ErpMfgJobCardStateMachine.TransitionDefinition e : sm.transitions()) {
            assertFalse("recordWork".equals(e.getAction()),
                    "recordWork 为 validation-only 动作，不应计入 transitions() 迁移边");
        }
    }

    // ---------- (l) 终态/初始态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpMfgConstants.JOB_CARD_STATUS_COMPLETED,
                        ErpMfgConstants.JOB_CARD_STATUS_CANCELLED),
                sm.terminalStatuses(), "终态集合 = {COMPLETED, CANCELLED}");
        assertEquals(java.util.Collections.singletonList(ErpMfgConstants.JOB_CARD_STATUS_OPEN),
                sm.initialStatuses(), "初始态集合 = {OPEN}");

        assertTrue(sm.isTerminal(ErpMfgConstants.JOB_CARD_STATUS_COMPLETED));
        assertTrue(sm.isTerminal(ErpMfgConstants.JOB_CARD_STATUS_CANCELLED));
        assertFalse(sm.isTerminal(ErpMfgConstants.JOB_CARD_STATUS_OPEN));
        assertFalse(sm.isTerminal(ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS));
    }

    // ---------- (m) PARTIALLY_TRANSFERRED / MATERIAL_TRANSFERRED 无任何边、不在终态集（预留死状态） ----------

    @Test
    public void testTransferredDeadStatesNoEdgesNotTerminal() {
        List<String> dead = Arrays.asList(
                ErpMfgConstants.JOB_CARD_STATUS_PARTIALLY_TRANSFERRED,
                ErpMfgConstants.JOB_CARD_STATUS_MATERIAL_TRANSFERRED);
        for (String d : dead) {
            for (ErpMfgJobCardStateMachine.TransitionDefinition e : sm.transitions()) {
                assertFalse(d.equals(e.getFromStatus()),
                        d + " 不应是任何边的来源: edge=" + e.getAction());
                assertFalse(d.equals(e.getToStatus()),
                        d + " 不应是任何边的目标: edge=" + e.getAction());
            }
            assertFalse(sm.isTerminal(d), d + " 不入终态集（预留死状态）");
            assertFalse(sm.terminalStatuses().contains(d), "terminalStatuses() 不应包含 " + d);
            assertFalse(sm.initialStatuses().contains(d), "initialStatuses() 不应包含 " + d);
            // 对全部 6 状态变更动作 + recordWork 均非法（Bean 不编码涉及两态的边）
            assertThrows(NopException.class, () -> sm.assertCanStartJob(d), "startJob 对 " + d + " 应非法");
            assertThrows(NopException.class, () -> sm.assertCanSubmitJob(d), "submitJob 对 " + d + " 应非法");
            assertThrows(NopException.class, () -> sm.assertCanCompleteJob(d), "completeJob 对 " + d + " 应非法");
            assertThrows(NopException.class, () -> sm.assertCanHoldJob(d), "holdJob 对 " + d + " 应非法");
            assertThrows(NopException.class, () -> sm.assertCanResumeJob(d), "resumeJob 对 " + d + " 应非法");
            assertThrows(NopException.class, () -> sm.assertCanCancelJob(d), "cancelJob 对 " + d + " 应非法");
            assertThrows(NopException.class, () -> sm.assertCanRecordWork(d), "recordWork 对 " + d + " 应非法");
        }
    }

    // ---------- helpers ----------

    private void invokeAssert(String action, String status) {
        switch (action) {
            case "startJob": sm.assertCanStartJob(status); break;
            case "submitJob": sm.assertCanSubmitJob(status); break;
            case "completeJob": sm.assertCanCompleteJob(status); break;
            case "holdJob": sm.assertCanHoldJob(status); break;
            case "resumeJob": sm.assertCanResumeJob(status); break;
            case "cancelJob": sm.assertCanCancelJob(status); break;
            default: throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "startJob": return sm.startJobTargetStatus();
            case "submitJob": return sm.submitJobTargetStatus();
            case "completeJob": return sm.completeJobTargetStatus();
            case "holdJob": return sm.holdJobTargetStatus();
            case "resumeJob": return sm.resumeJobTargetStatus();
            case "cancelJob": return sm.cancelJobTargetStatus();
            default: throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private void assertCommonTransitionMetadata(NopException ex, String action, String status) {
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + status);
        assertEquals(action, ex.getParam(ErpMfgJobCardStateMachine.ARG_ACTION), "拒绝元数据携带动作名");
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
            for (ErpMfgJobCardStateMachine.TransitionDefinition e : sm.transitions()) {
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
