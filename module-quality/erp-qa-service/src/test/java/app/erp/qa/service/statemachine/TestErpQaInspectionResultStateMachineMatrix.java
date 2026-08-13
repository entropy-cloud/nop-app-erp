package app.erp.qa.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.qa.service.ErpQaConstants;
import io.nop.api.core.exceptions.NopException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-14-0930-2 Phase 1 Proof）。
 *
 * <p>针对 {@link ErpQaInspectionResultStateMachine} Bean 的纯矩阵完备性遍历：不经 BizModel 入口（层 3 职责），
 * 不断言副作用/审计。覆盖：
 * <ul>
 *   <li>(a) 无完全重复边（action|from|to 三元唯一，支持 recordResult 数据驱动三分支）；</li>
 *   <li>(b) 从 PENDING 可达 ACCEPTED/CONDITIONAL/REJECTED；</li>
 *   <li>(c) 各 {@code assertCanXxx} 合法来源态通过、非法来源态抛 common 码携带 {@code action}/{@code fromStatus}；</li>
 *   <li>(d) {@code transitions()} 与显式方法语义一致（passInspection/failInspection 目标态）；</li>
 *   <li>(e) 初始/终态集合正确（终态无出边）。</li>
 * </ul>
 *
 * <p>层 2 四方对照：dict {@code erp-qa/inspection-result}（PENDING/ACCEPTED/CONDITIONAL/REJECTED）
 * ↔ {@code docs/design/quality/state-machine.md} §适用对象一 ↔ Bean 元数据 ↔ 全部 writer
 * （recordResult/passInspection/failInspection 3 Processor live + CRUD 路径 §9.4 选项 c 排除）。
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpQaInspectionResultStateMachineMatrix {

    private static final List<String> ALL_RESULTS = Arrays.asList(
            ErpQaConstants.INSPECTION_RESULT_PENDING,
            ErpQaConstants.INSPECTION_RESULT_ACCEPTED,
            ErpQaConstants.INSPECTION_RESULT_CONDITIONAL,
            ErpQaConstants.INSPECTION_RESULT_REJECTED);

    private final ErpQaInspectionResultStateMachine sm = new ErpQaInspectionResultStateMachine();

    // ---------- (a) 无完全重复/冲突边（action|from|to 三元唯一） ----------

    @Test
    public void testNoDuplicateEdges() {
        List<ErpQaInspectionResultStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpQaInspectionResultStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus() + "|" + e.getToStatus();
            assertTrue(seen.add(key), "重复边: " + key);
        }
        // recordResult 3 分支 + passInspection + failInspection = 5 边
        assertEquals(5, edges.size(), "迁移矩阵应有 5 条边（recordResult×3 + passInspection + failInspection）");
    }

    // ---------- (b) 从 PENDING 可达全部终态 ----------

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpQaConstants.INSPECTION_RESULT_PENDING);
        assertTrue(reachable.contains(ErpQaConstants.INSPECTION_RESULT_ACCEPTED), "从 PENDING 应可达 ACCEPTED");
        assertTrue(reachable.contains(ErpQaConstants.INSPECTION_RESULT_CONDITIONAL), "从 PENDING 应可达 CONDITIONAL");
        assertTrue(reachable.contains(ErpQaConstants.INSPECTION_RESULT_REJECTED), "从 PENDING 应可达 REJECTED");
    }

    // ---------- (c) assertCanXxx 合法/非法 ----------

    @Test
    public void testAssertCanRecordResultLegalAndIllegal() {
        sm.assertCanRecordResult(ErpQaConstants.INSPECTION_RESULT_PENDING);
        sm.assertCanRecordResult(null);
        for (String illegal : Arrays.asList(ErpQaConstants.INSPECTION_RESULT_ACCEPTED,
                ErpQaConstants.INSPECTION_RESULT_CONDITIONAL, ErpQaConstants.INSPECTION_RESULT_REJECTED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanRecordResult(illegal));
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
            assertEquals("recordResult", ex.getParam(ErpQaInspectionResultStateMachine.ARG_ACTION));
            assertEquals(illegal, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
        }
    }

    @Test
    public void testAssertCanPassInspectionLegalAndIllegal() {
        sm.assertCanPassInspection(ErpQaConstants.INSPECTION_RESULT_PENDING);
        sm.assertCanPassInspection(null);
        assertEquals(ErpQaConstants.INSPECTION_RESULT_ACCEPTED, sm.passInspectionTargetStatus());
        for (String illegal : Arrays.asList(ErpQaConstants.INSPECTION_RESULT_ACCEPTED,
                ErpQaConstants.INSPECTION_RESULT_CONDITIONAL, ErpQaConstants.INSPECTION_RESULT_REJECTED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanPassInspection(illegal));
            assertEquals("passInspection", ex.getParam(ErpQaInspectionResultStateMachine.ARG_ACTION));
        }
    }

    @Test
    public void testAssertCanFailInspectionLegalAndIllegal() {
        sm.assertCanFailInspection(ErpQaConstants.INSPECTION_RESULT_PENDING);
        sm.assertCanFailInspection(null);
        assertEquals(ErpQaConstants.INSPECTION_RESULT_REJECTED, sm.failInspectionTargetStatus());
        for (String illegal : Arrays.asList(ErpQaConstants.INSPECTION_RESULT_ACCEPTED,
                ErpQaConstants.INSPECTION_RESULT_CONDITIONAL, ErpQaConstants.INSPECTION_RESULT_REJECTED)) {
            assertThrows(NopException.class, () -> sm.assertCanFailInspection(illegal));
        }
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpQaInspectionResultStateMachine.TransitionDefinition e : sm.transitions()) {
            invokeAssert(e.getAction(), e.getFromStatus());
            // passInspection/failInspection 目标态固定；recordResult 目标态数据驱动（无单一 getter，校验目标态为合法终态）
            String to = e.getToStatus();
            switch (e.getAction()) {
                case "passInspection":
                    assertEquals(sm.passInspectionTargetStatus(), to);
                    break;
                case "failInspection":
                    assertEquals(sm.failInspectionTargetStatus(), to);
                    break;
                case "recordResult":
                    assertTrue(sm.isTerminal(to), "recordResult 目标态应为终态: " + to);
                    break;
                default:
                    throw new IllegalArgumentException("unknown action: " + e.getAction());
            }
        }
    }

    // ---------- (e) 初始/终态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpQaConstants.INSPECTION_RESULT_PENDING), sm.initialStatuses(),
                "初始态集合 = {PENDING}");
        assertEquals(Arrays.asList(ErpQaConstants.INSPECTION_RESULT_ACCEPTED,
                ErpQaConstants.INSPECTION_RESULT_CONDITIONAL,
                ErpQaConstants.INSPECTION_RESULT_REJECTED), sm.terminalStatuses(),
                "终态集合 = {ACCEPTED, CONDITIONAL, REJECTED}");

        assertTrue(sm.isTerminal(ErpQaConstants.INSPECTION_RESULT_ACCEPTED));
        assertTrue(sm.isTerminal(ErpQaConstants.INSPECTION_RESULT_CONDITIONAL));
        assertTrue(sm.isTerminal(ErpQaConstants.INSPECTION_RESULT_REJECTED));
        assertFalse(sm.isTerminal(ErpQaConstants.INSPECTION_RESULT_PENDING));
    }

    /** 终态不可恢复：终态无出边。 */
    @Test
    public void testTerminalsHaveNoOutgoingEdges() {
        for (String terminal : sm.terminalStatuses()) {
            for (ErpQaInspectionResultStateMachine.TransitionDefinition e : sm.transitions()) {
                assertFalse(e.getFromStatus().equals(terminal),
                        "终态 " + terminal + " 不应有出边: " + e.getAction());
            }
        }
    }

    // ---------- helpers ----------

    private void invokeAssert(String action, String result) {
        switch (action) {
            case "recordResult":
                sm.assertCanRecordResult(result);
                break;
            case "passInspection":
                sm.assertCanPassInspection(result);
                break;
            case "failInspection":
                sm.assertCanFailInspection(result);
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private Set<String> reachableFrom(String start) {
        Set<String> visited = new java.util.LinkedHashSet<>();
        List<String> frontier = new java.util.ArrayList<>();
        frontier.add(start);
        while (!frontier.isEmpty()) {
            String cur = frontier.remove(0);
            if (!visited.add(cur)) {
                continue;
            }
            for (ErpQaInspectionResultStateMachine.TransitionDefinition e : sm.transitions()) {
                if (e.getFromStatus().equals(cur) && !visited.contains(e.getToStatus())) {
                    frontier.add(e.getToStatus());
                }
            }
        }
        visited.remove(start);
        return visited;
    }
}
