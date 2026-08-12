package app.erp.sal.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.sal.dao.constants.ErpSalDocStatus;
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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-12-0918-2 Phase 2 Proof）。
 *
 * <p>针对 {@link ErpSalQuotationDocumentStateMachine} Bean 的纯矩阵完备性遍历：不经 BizModel 入口（层 3 职责），
 * 不断言副作用/审计。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边；</li>
 *   <li>(b) 从 DRAFT 可达 CANCELLED、CANCELLED 终态无出边；</li>
 *   <li>(c) cancel 对 DRAFT 合法、对 CANCELLED 抛 common 码携带 {@code action=cancel}/{@code fromStatus=CANCELLED}；</li>
 *   <li>(d) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(e) 初始/终态集合正确。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpSalQuotationDocumentStateMachineMatrix {

    private static final List<String> ALL_DOC_STATUSES = Arrays.asList(
            ErpSalDocStatus.DOC_STATUS_DRAFT,
            ErpSalDocStatus.DOC_STATUS_ACTIVE,
            ErpSalDocStatus.DOC_STATUS_CANCELLED);

    private final ErpSalQuotationDocumentStateMachine sm = new ErpSalQuotationDocumentStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpSalQuotationDocumentStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpSalQuotationDocumentStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(1, edges.size(), "迁移矩阵应有 1 条边（DRAFT→CANCELLED）");
    }

    // ---------- (b) 从 DRAFT 可达 CANCELLED；终态无出边 ----------

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpSalDocStatus.DOC_STATUS_DRAFT);
        assertTrue(reachable.contains(ErpSalDocStatus.DOC_STATUS_CANCELLED),
                "从 DRAFT 应可达 CANCELLED");
    }

    @Test
    public void testTerminalStatusesHaveNoOutgoingEdges() {
        for (String terminal : sm.terminalStatuses()) {
            for (ErpSalQuotationDocumentStateMachine.TransitionDefinition e : sm.transitions()) {
                assertFalse(e.getFromStatus().equals(terminal),
                        "终态不应有出边: terminal=" + terminal + ", but edge " + e.getAction() + " leaves it");
            }
        }
    }

    // ---------- (c) cancel 对 DRAFT 合法、对 CANCELLED 非法 ----------

    @Test
    public void testCancelLegalForDraftAndIllegalForCancelled() {
        // DRAFT 合法（不抛）
        sm.assertCanCancel(ErpSalDocStatus.DOC_STATUS_DRAFT);
        assertEquals(ErpSalDocStatus.DOC_STATUS_CANCELLED, sm.cancelTargetStatus());

        // CANCELLED 非法 → 抛 common 层码 + action/fromStatus 元数据
        NopException ex = assertThrows(NopException.class,
                () -> sm.assertCanCancel(ErpSalDocStatus.DOC_STATUS_CANCELLED),
                "cancel 对 CANCELLED 应非法");
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "Bean 报告 common 层非法迁移码");
        assertEquals("cancel", ex.getParam(ErpSalQuotationDocumentStateMachine.ARG_ACTION), "拒绝元数据携带动作名");
        assertEquals(ErpSalDocStatus.DOC_STATUS_CANCELLED, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                "拒绝元数据携带当前态");
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpSalQuotationDocumentStateMachine.TransitionDefinition e : sm.transitions()) {
            // 每条边的 fromStatus 对该 action 合法（assert 放行不抛）
            invokeAssert(e.getAction(), e.getFromStatus());
            // 每条边的 toStatus 与 <action>TargetStatus() 一致
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (e) 终态/初始态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpSalDocStatus.DOC_STATUS_CANCELLED), sm.terminalStatuses(),
                "终态集合 = {CANCELLED}");
        assertEquals(Arrays.asList(ErpSalDocStatus.DOC_STATUS_DRAFT), sm.initialStatuses(),
                "初始态集合 = {DRAFT}");

        assertTrue(sm.isTerminal(ErpSalDocStatus.DOC_STATUS_CANCELLED));
        assertFalse(sm.isTerminal(ErpSalDocStatus.DOC_STATUS_DRAFT));
        assertFalse(sm.isTerminal(ErpSalDocStatus.DOC_STATUS_ACTIVE));
    }

    // ---------- helpers ----------

    private void invokeAssert(String action, String docStatus) {
        switch (action) {
            case "cancel":
                sm.assertCanCancel(docStatus);
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "cancel":
                return sm.cancelTargetStatus();
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
            for (ErpSalQuotationDocumentStateMachine.TransitionDefinition e : sm.transitions()) {
                if (e.getFromStatus().equals(cur) && !visited.contains(e.getToStatus())) {
                    frontier.add(e.getToStatus());
                }
            }
        }
        visited.remove(start);
        return visited;
    }
}
