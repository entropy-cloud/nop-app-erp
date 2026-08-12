package app.erp.pur.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.pur.dao.constants.ErpPurDocStatus;
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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-12-0918-1 Phase 2 Proof）。
 *
 * <p>针对 {@link ErpPurRequisitionDocumentStateMachine} Bean 的纯矩阵完备性遍历：不经 BizModel 入口（层 3 职责），
 * 不断言副作用/审计。覆盖：(a) 无重复/冲突边；(b) 从 DRAFT 可达 CANCELLED、CANCELLED 终态无出边；
 * (c) cancel 对 DRAFT 合法、对 CANCELLED 抛 common 码携带 action/fromStatus；(d) transitions() 元数据与显式方法
 * 语义一致；(e) 初始/终态集合正确。
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpPurRequisitionDocumentStateMachineMatrix {

    private final ErpPurRequisitionDocumentStateMachine sm = new ErpPurRequisitionDocumentStateMachine();

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpPurRequisitionDocumentStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpPurRequisitionDocumentStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(1, edges.size(), "迁移矩阵应有 1 条边（DRAFT→CANCELLED）");
    }

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpPurDocStatus.DOC_STATUS_DRAFT);
        assertTrue(reachable.contains(ErpPurDocStatus.DOC_STATUS_CANCELLED), "从 DRAFT 应可达 CANCELLED");
    }

    @Test
    public void testTerminalStatusesHaveNoOutgoingEdges() {
        for (String terminal : sm.terminalStatuses()) {
            for (ErpPurRequisitionDocumentStateMachine.TransitionDefinition e : sm.transitions()) {
                assertFalse(e.getFromStatus().equals(terminal),
                        "终态不应有出边: terminal=" + terminal + ", but edge " + e.getAction() + " leaves it");
            }
        }
    }

    @Test
    public void testCancelLegalForDraftAndIllegalForCancelled() {
        sm.assertCanCancel(ErpPurDocStatus.DOC_STATUS_DRAFT);
        assertEquals(ErpPurDocStatus.DOC_STATUS_CANCELLED, sm.cancelTargetStatus());

        NopException ex = assertThrows(NopException.class,
                () -> sm.assertCanCancel(ErpPurDocStatus.DOC_STATUS_CANCELLED),
                "cancel 对 CANCELLED 应非法");
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "Bean 报告 common 层非法迁移码");
        assertEquals("cancel", ex.getParam(ErpPurRequisitionDocumentStateMachine.ARG_ACTION), "拒绝元数据携带动作名");
        assertEquals(ErpPurDocStatus.DOC_STATUS_CANCELLED, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                "拒绝元数据携带当前态");
    }

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpPurRequisitionDocumentStateMachine.TransitionDefinition e : sm.transitions()) {
            sm.assertCanCancel(e.getFromStatus());
            assertEquals(e.getToStatus(), sm.cancelTargetStatus(),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpPurDocStatus.DOC_STATUS_CANCELLED), sm.terminalStatuses(),
                "终态集合 = {CANCELLED}");
        assertEquals(Arrays.asList(ErpPurDocStatus.DOC_STATUS_DRAFT), sm.initialStatuses(),
                "初始态集合 = {DRAFT}");
        assertTrue(sm.isTerminal(ErpPurDocStatus.DOC_STATUS_CANCELLED));
        assertFalse(sm.isTerminal(ErpPurDocStatus.DOC_STATUS_DRAFT));
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
            for (ErpPurRequisitionDocumentStateMachine.TransitionDefinition e : sm.transitions()) {
                if (e.getFromStatus().equals(cur) && !visited.contains(e.getToStatus())) {
                    frontier.add(e.getToStatus());
                }
            }
        }
        visited.remove(start);
        return visited;
    }
}
