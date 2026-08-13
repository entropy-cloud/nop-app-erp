package app.erp.mnt.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.mnt.dao.ErpMntDaoConstants;
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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan Phase 2 Proof）。
 *
 * <p>针对 {@link ErpMntSparePartUsageDocumentStateMachine} Bean（docStatus 单轴）的纯矩阵完备性遍历：不经
 * BizModel 入口（层 3 职责），不断言副作用/审计。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边；</li>
 *   <li>(b) 从 DRAFT 可达 ACTIVE 与 CANCELLED，终态 CANCELLED 无出边；</li>
 *   <li>(c) confirm 仅 DRAFT 合法、reverseConfirm 仅 ACTIVE 合法；</li>
 *   <li>(d) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(e) 终态/初始态集合正确。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpMntSparePartUsageDocumentStateMachineMatrix {

    private static final List<String> ALL_DOC_STATUSES = Arrays.asList(
            ErpMntDaoConstants.DOC_STATUS_DRAFT,
            ErpMntDaoConstants.DOC_STATUS_ACTIVE,
            ErpMntDaoConstants.DOC_STATUS_CANCELLED);

    private final ErpMntSparePartUsageDocumentStateMachine sm = new ErpMntSparePartUsageDocumentStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpMntSparePartUsageDocumentStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpMntSparePartUsageDocumentStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(2, edges.size(), "迁移矩阵应有 2 条边");
    }

    // ---------- (b) 从 DRAFT 可达 ACTIVE 与 CANCELLED；终态无出边 ----------

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpMntDaoConstants.DOC_STATUS_DRAFT);
        for (String s : ALL_DOC_STATUSES) {
            if (ErpMntDaoConstants.DOC_STATUS_DRAFT.equals(s)) {
                continue;
            }
            assertTrue(reachable.contains(s), "从 DRAFT 应可达状态: " + s);
        }
    }

    @Test
    public void testTerminalStatusesHaveNoOutgoingEdges() {
        for (String terminal : sm.terminalStatuses()) {
            for (ErpMntSparePartUsageDocumentStateMachine.TransitionDefinition e : sm.transitions()) {
                assertFalse(e.getFromStatus().equals(terminal),
                        "终态不应有出边: terminal=" + terminal + ", but edge " + e.getAction() + " leaves it");
            }
        }
    }

    // ---------- (c) confirm 仅 DRAFT 合法、reverseConfirm 仅 ACTIVE 合法 ----------

    @Test
    public void testExplicitActionGuards() {
        assertActionAllowsOnly("confirm", ErpMntDaoConstants.DOC_STATUS_DRAFT);
        assertActionAllowsOnly("reverseConfirm", ErpMntDaoConstants.DOC_STATUS_ACTIVE);
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpMntSparePartUsageDocumentStateMachine.TransitionDefinition e : sm.transitions()) {
            invokeAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (e) 终态/初始态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Collections.singletonList(ErpMntDaoConstants.DOC_STATUS_CANCELLED),
                sm.terminalStatuses(), "终态集合 = {CANCELLED}");
        assertEquals(Collections.singletonList(ErpMntDaoConstants.DOC_STATUS_DRAFT),
                sm.initialStatuses(), "初始态集合 = {DRAFT}");

        assertTrue(sm.isTerminal(ErpMntDaoConstants.DOC_STATUS_CANCELLED));
        assertFalse(sm.isTerminal(ErpMntDaoConstants.DOC_STATUS_DRAFT));
        assertFalse(sm.isTerminal(ErpMntDaoConstants.DOC_STATUS_ACTIVE));
    }

    @Test
    public void testTargetStatusMethods() {
        assertEquals(ErpMntDaoConstants.DOC_STATUS_ACTIVE, sm.confirmTargetStatus());
        assertEquals(ErpMntDaoConstants.DOC_STATUS_CANCELLED, sm.reverseConfirmTargetStatus());
    }

    // ---------- helpers ----------

    private void assertActionAllowsOnly(String action, String allowedFrom) {
        for (String s : ALL_DOC_STATUSES) {
            if (allowedFrom.equals(s)) {
                invokeAssert(action, s); // 不抛 = 合法
            } else {
                NopException ex = assertThrows(NopException.class, () -> invokeAssert(action, s),
                        action + " 对非允许来源态应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                        "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + s);
                assertEquals(action, ex.getParam(ErpMntSparePartUsageDocumentStateMachine.ARG_ACTION),
                        "拒绝元数据携带动作名: action=" + action);
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                        "拒绝元数据携带当前态: action=" + action + ", status=" + s);
            }
        }
    }

    private void invokeAssert(String action, String status) {
        switch (action) {
            case "confirm":
                sm.assertCanConfirm(status);
                break;
            case "reverseConfirm":
                sm.assertCanReverseConfirm(status);
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "confirm":
                return sm.confirmTargetStatus();
            case "reverseConfirm":
                return sm.reverseConfirmTargetStatus();
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
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
            for (ErpMntSparePartUsageDocumentStateMachine.TransitionDefinition e : sm.transitions()) {
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
