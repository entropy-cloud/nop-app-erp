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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-14-0930-1 Phase 1 Proof）。
 *
 * <p>针对 {@link ErpMfgWorkOrderDocumentStateMachine} Bean（M4.35 docStatus 业务生命周期轴）的纯矩阵完备性遍历：
 * 不经 BizModel/Processor 入口（层 3 职责），不断言副作用/审计（过账/stock move 归层 3）。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边（14 边唯一 action|fromStatus 键）；</li>
 *   <li>(b) 从 DRAFT 可达全部非终态 + 各终态，STOCK_PARTIAL 可达；</li>
 *   <li>(c) 各 {@code assertCanXxx} 合法来源态通过、非法来源态抛 common 码携带 {@code action}/{@code fromStatus}；</li>
 *   <li>(d) {@code transitions()} 元数据与显式方法语义一致（checkAvailability 为多目标动作特殊处理）；</li>
 *   <li>(e) 终态 {COMPLETED, CLOSED, CANCELLED} 无出边；初始态 {DRAFT}；</li>
 *   <li>(f) 终态/初始态集合正确。</li>
 * </ul>
 *
 * <p>层 2 四方对照（WorkOrder docStatus 轴单条）：dict {@code erp-mfg/work-order-status}（10 值）↔
 * {@code docs/design/manufacturing/state-machine.md} §适用对象一（10 态完整矩阵）↔ Bean 元数据 ↔ 全部 writer
 * （10+5 Processor live + checkAvailability/cancel facade 直入 + MrpRelease spawn 写 DRAFT + CRUD 路径 §9.4 选项 c 排除）。
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpMfgWorkOrderDocumentStateMachineMatrix {

    private static final List<String> ALL_STATUSES = Arrays.asList(
            ErpMfgConstants.WORK_ORDER_STATUS_DRAFT,
            ErpMfgConstants.WORK_ORDER_STATUS_SUBMITTED,
            ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED,
            ErpMfgConstants.WORK_ORDER_STATUS_STOCK_RESERVED,
            ErpMfgConstants.WORK_ORDER_STATUS_STOCK_PARTIAL,
            ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS,
            ErpMfgConstants.WORK_ORDER_STATUS_STOPPED,
            ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED,
            ErpMfgConstants.WORK_ORDER_STATUS_CLOSED,
            ErpMfgConstants.WORK_ORDER_STATUS_CANCELLED);

    private final ErpMfgWorkOrderDocumentStateMachine sm = new ErpMfgWorkOrderDocumentStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpMfgWorkOrderDocumentStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpMfgWorkOrderDocumentStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus() + "|" + e.getToStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus()
                    + ", toStatus=" + e.getToStatus());
        }
        assertEquals(14, edges.size(),
                "迁移矩阵应有 14 条边（submit 1 + approve 1 + checkAvailability 2 + start 2 + stop 1 + resume 1 + close 2 + reportCompletion 1 + cancel 3）");
    }

    // ---------- (b) 从 DRAFT 可达全部非终态 + 终态 ----------

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpMfgConstants.WORK_ORDER_STATUS_DRAFT);
        assertTrue(reachable.contains(ErpMfgConstants.WORK_ORDER_STATUS_SUBMITTED), "从 DRAFT 应可达 SUBMITTED");
        assertTrue(reachable.contains(ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED), "从 DRAFT 应可达 NOT_STARTED");
        assertTrue(reachable.contains(ErpMfgConstants.WORK_ORDER_STATUS_STOCK_RESERVED), "从 DRAFT 应可达 STOCK_RESERVED");
        assertTrue(reachable.contains(ErpMfgConstants.WORK_ORDER_STATUS_STOCK_PARTIAL), "从 DRAFT 应可达 STOCK_PARTIAL");
        assertTrue(reachable.contains(ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS), "从 DRAFT 应可达 IN_PROCESS");
        assertTrue(reachable.contains(ErpMfgConstants.WORK_ORDER_STATUS_STOPPED), "从 DRAFT 应可达 STOPPED");
        assertTrue(reachable.contains(ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED), "从 DRAFT 应可达 COMPLETED");
        assertTrue(reachable.contains(ErpMfgConstants.WORK_ORDER_STATUS_CLOSED), "从 DRAFT 应可达 CLOSED");
        assertTrue(reachable.contains(ErpMfgConstants.WORK_ORDER_STATUS_CANCELLED), "从 DRAFT 应可达 CANCELLED");
    }

    // ---------- (c) assertCanXxx 合法/非法 ----------

    @Test
    public void testAssertCanSubmitLegalAndIllegal() {
        sm.assertCanSubmit(ErpMfgConstants.WORK_ORDER_STATUS_DRAFT);
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_SUBMITTED, sm.submitTargetStatus());
        for (String illegal : illegalFor(ALL_STATUSES, ErpMfgConstants.WORK_ORDER_STATUS_DRAFT)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanSubmit(illegal),
                    "submit 对非 DRAFT 应非法: " + illegal);
            assertCommonTransitionMetadata(ex, "submit", illegal);
        }
    }

    @Test
    public void testAssertCanApproveLegalAndIllegal() {
        sm.assertCanApprove(ErpMfgConstants.WORK_ORDER_STATUS_SUBMITTED);
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED, sm.approveTargetStatus());
        for (String illegal : illegalFor(ALL_STATUSES, ErpMfgConstants.WORK_ORDER_STATUS_SUBMITTED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanApprove(illegal),
                    "approve 对非 SUBMITTED 应非法: " + illegal);
            assertCommonTransitionMetadata(ex, "approve", illegal);
        }
    }

    @Test
    public void testAssertCanCheckAvailabilityLegalAndIllegal() {
        sm.assertCanCheckAvailability(ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED);
        for (String illegal : illegalFor(ALL_STATUSES, ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanCheckAvailability(illegal),
                    "checkAvailability 对非 NOT_STARTED 应非法: " + illegal);
            assertCommonTransitionMetadata(ex, "checkAvailability", illegal);
        }
    }

    @Test
    public void testAssertCanStartLegalAndIllegal() {
        sm.assertCanStart(ErpMfgConstants.WORK_ORDER_STATUS_STOCK_RESERVED);
        sm.assertCanStart(ErpMfgConstants.WORK_ORDER_STATUS_STOCK_PARTIAL);
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS, sm.startTargetStatus());
        for (String illegal : illegalFor(ALL_STATUSES,
                ErpMfgConstants.WORK_ORDER_STATUS_STOCK_RESERVED, ErpMfgConstants.WORK_ORDER_STATUS_STOCK_PARTIAL)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanStart(illegal),
                    "start 对非 STOCK_RESERVED/STOCK_PARTIAL 应非法: " + illegal);
            assertCommonTransitionMetadata(ex, "start", illegal);
        }
    }

    @Test
    public void testAssertCanStopLegalAndIllegal() {
        sm.assertCanStop(ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS);
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_STOPPED, sm.stopTargetStatus());
        for (String illegal : illegalFor(ALL_STATUSES, ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanStop(illegal),
                    "stop 对非 IN_PROCESS 应非法: " + illegal);
            assertCommonTransitionMetadata(ex, "stop", illegal);
        }
    }

    @Test
    public void testAssertCanResumeLegalAndIllegal() {
        sm.assertCanResume(ErpMfgConstants.WORK_ORDER_STATUS_STOPPED);
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS, sm.resumeTargetStatus());
        for (String illegal : illegalFor(ALL_STATUSES, ErpMfgConstants.WORK_ORDER_STATUS_STOPPED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanResume(illegal),
                    "resume 对非 STOPPED 应非法: " + illegal);
            assertCommonTransitionMetadata(ex, "resume", illegal);
        }
    }

    @Test
    public void testAssertCanCloseLegalAndIllegal() {
        sm.assertCanClose(ErpMfgConstants.WORK_ORDER_STATUS_STOPPED);
        sm.assertCanClose(ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS);
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_CLOSED, sm.closeTargetStatus());
        for (String illegal : illegalFor(ALL_STATUSES,
                ErpMfgConstants.WORK_ORDER_STATUS_STOPPED, ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanClose(illegal),
                    "close 对非 STOPPED/IN_PROCESS 应非法: " + illegal);
            assertCommonTransitionMetadata(ex, "close", illegal);
        }
    }

    @Test
    public void testAssertCanReportCompletionLegalAndIllegal() {
        sm.assertCanReportCompletion(ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS);
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED, sm.reportCompletionTargetStatus());
        for (String illegal : illegalFor(ALL_STATUSES, ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanReportCompletion(illegal),
                    "reportCompletion 对非 IN_PROCESS 应非法: " + illegal);
            assertCommonTransitionMetadata(ex, "reportCompletion", illegal);
        }
    }

    @Test
    public void testAssertCanCancelLegalAndIllegal() {
        sm.assertCanCancel(ErpMfgConstants.WORK_ORDER_STATUS_DRAFT);
        sm.assertCanCancel(ErpMfgConstants.WORK_ORDER_STATUS_SUBMITTED);
        sm.assertCanCancel(ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED);
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_CANCELLED, sm.cancelTargetStatus());
        for (String illegal : illegalFor(ALL_STATUSES,
                ErpMfgConstants.WORK_ORDER_STATUS_DRAFT,
                ErpMfgConstants.WORK_ORDER_STATUS_SUBMITTED,
                ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanCancel(illegal),
                    "cancel 对非 DRAFT/SUBMITTED/NOT_STARTED 应非法: " + illegal);
            assertCommonTransitionMetadata(ex, "cancel", illegal);
        }
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpMfgWorkOrderDocumentStateMachine.TransitionDefinition e : sm.transitions()) {
            invokeAssert(e.getAction(), e.getFromStatus());
            if ("checkAvailability".equals(e.getAction())) {
                // checkAvailability 为多目标动作（STOCK_RESERVED/STOCK_PARTIAL 由齐套校验动态决定），
                // 无单值 TargetStatus() 方法——两条边目标均为合法动态结果集成员
                assertTrue(
                        ErpMfgConstants.WORK_ORDER_STATUS_STOCK_RESERVED.equals(e.getToStatus())
                                || ErpMfgConstants.WORK_ORDER_STATUS_STOCK_PARTIAL.equals(e.getToStatus()),
                        "checkAvailability 目标态应 ∈ {STOCK_RESERVED, STOCK_PARTIAL}: " + e.getToStatus());
            } else {
                assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                        "toStatus 与目标态方法不一致: action=" + e.getAction());
            }
        }
    }

    // ---------- (e) 终态无出边 ----------

    @Test
    public void testTerminalStatusesHaveNoOutgoingEdges() {
        for (String terminal : sm.terminalStatuses()) {
            for (ErpMfgWorkOrderDocumentStateMachine.TransitionDefinition e : sm.transitions()) {
                assertFalse(e.getFromStatus().equals(terminal),
                        "终态不应有出边: terminal=" + terminal + ", but edge " + e.getAction() + " leaves it");
            }
        }
    }

    // ---------- (f) 终态/初始态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED,
                        ErpMfgConstants.WORK_ORDER_STATUS_CLOSED,
                        ErpMfgConstants.WORK_ORDER_STATUS_CANCELLED),
                sm.terminalStatuses(), "终态集合 = {COMPLETED, CLOSED, CANCELLED}");
        assertEquals(java.util.Collections.singletonList(ErpMfgConstants.WORK_ORDER_STATUS_DRAFT),
                sm.initialStatuses(), "初始态集合 = {DRAFT}");

        assertTrue(sm.isTerminal(ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED));
        assertTrue(sm.isTerminal(ErpMfgConstants.WORK_ORDER_STATUS_CLOSED));
        assertTrue(sm.isTerminal(ErpMfgConstants.WORK_ORDER_STATUS_CANCELLED));
        assertFalse(sm.isTerminal(ErpMfgConstants.WORK_ORDER_STATUS_DRAFT));
        assertFalse(sm.isTerminal(ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS));
        assertFalse(sm.isTerminal(ErpMfgConstants.WORK_ORDER_STATUS_STOPPED));
    }

    // ---------- helpers ----------

    private void invokeAssert(String action, String status) {
        switch (action) {
            case "submit": sm.assertCanSubmit(status); break;
            case "approve": sm.assertCanApprove(status); break;
            case "checkAvailability": sm.assertCanCheckAvailability(status); break;
            case "start": sm.assertCanStart(status); break;
            case "stop": sm.assertCanStop(status); break;
            case "resume": sm.assertCanResume(status); break;
            case "close": sm.assertCanClose(status); break;
            case "reportCompletion": sm.assertCanReportCompletion(status); break;
            case "cancel": sm.assertCanCancel(status); break;
            default: throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "submit": return sm.submitTargetStatus();
            case "approve": return sm.approveTargetStatus();
            case "start": return sm.startTargetStatus();
            case "stop": return sm.stopTargetStatus();
            case "resume": return sm.resumeTargetStatus();
            case "close": return sm.closeTargetStatus();
            case "reportCompletion": return sm.reportCompletionTargetStatus();
            case "cancel": return sm.cancelTargetStatus();
            default: throw new IllegalArgumentException("unknown action without single target: " + action);
        }
    }

    private void assertCommonTransitionMetadata(NopException ex, String action, String status) {
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + status);
        assertEquals(action, ex.getParam(ErpMfgWorkOrderDocumentStateMachine.ARG_ACTION), "拒绝元数据携带动作名");
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
            for (ErpMfgWorkOrderDocumentStateMachine.TransitionDefinition e : sm.transitions()) {
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
