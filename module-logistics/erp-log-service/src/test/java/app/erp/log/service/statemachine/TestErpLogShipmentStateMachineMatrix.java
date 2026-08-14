package app.erp.log.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.log.service.ErpLogConstants;
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
 * <p>针对 {@link ErpLogShipmentStateMachine}（发运单 status 单轴，dict {@code erp-log/shipment-status} 6 值）的
 * 纯矩阵完备性遍历。不经 BizModel 入口（层 3 职责），不断言副作用/过账/网关编排。
 *
 * <p>每轴覆盖：(a) 无重复/冲突边；(b) 从初始态可达全部声明状态 + 终态无出边；(c) 各动作合法/非法来源态全集；
 * (d) {@code transitions()} 元数据与显式方法语义一致；(e) 终态/初始态集合正确；(f) 6 值全可达（无死状态）。
 * Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 *
 * <p><b>Decision (C) 收紧核对</b>：advanceToDelivered 合法来源态 = {ADVISED,DISPATCHED,IN_TRANSIT}
 * （排除 DRAFT/CANCELLED——code 原无来源态守卫，Bean 刻意收紧，四方对照登记 {@code intentional narrowing}）。
 * 5 动作 10 边：advise 1 + completeShipment 1 + advanceToInTransit 1 + advanceToDelivered 3 + cancelShipment 4。
 */
public class TestErpLogShipmentStateMachineMatrix {

    // erp-log/shipment-status 字典六态
    private static final List<String> ALL_SHIPMENT_STATUSES = Arrays.asList(
            ErpLogConstants.SHIPMENT_STATUS_DRAFT,
            ErpLogConstants.SHIPMENT_STATUS_ADVISED,
            ErpLogConstants.SHIPMENT_STATUS_DISPATCHED,
            ErpLogConstants.SHIPMENT_STATUS_IN_TRANSIT,
            ErpLogConstants.SHIPMENT_STATUS_DELIVERED,
            ErpLogConstants.SHIPMENT_STATUS_CANCELLED);

    private final ErpLogShipmentStateMachine sm = new ErpLogShipmentStateMachine();

    // ==================== 矩阵完备性 ====================

    @Test
    public void noDuplicateOrConflictingEdges() {
        List<ErpLogShipmentStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpLogShipmentStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(10, edges.size(),
                "迁移矩阵应有 10 条边（advise 1 + completeShipment 1 + advanceToInTransit 1 + advanceToDelivered 3 + cancelShipment 4）");
    }

    @Test
    public void reachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpLogConstants.SHIPMENT_STATUS_DRAFT);
        // 6 值全可达（DRAFT 为初始态，其余 5 值为某迁移边目标）
        assertEquals(5, reachable.size(), "从 DRAFT 应可达全部 5 个非初始状态");
        assertTrue(reachable.contains(ErpLogConstants.SHIPMENT_STATUS_ADVISED));
        assertTrue(reachable.contains(ErpLogConstants.SHIPMENT_STATUS_DISPATCHED));
        assertTrue(reachable.contains(ErpLogConstants.SHIPMENT_STATUS_IN_TRANSIT));
        assertTrue(reachable.contains(ErpLogConstants.SHIPMENT_STATUS_DELIVERED));
        assertTrue(reachable.contains(ErpLogConstants.SHIPMENT_STATUS_CANCELLED));
    }

    @Test
    public void adviseAllowsOnlyDraft() {
        assertAllowsOnly("advise", ErpLogConstants.SHIPMENT_STATUS_DRAFT);
    }

    @Test
    public void completeShipmentAllowsOnlyAdvised() {
        assertAllowsOnly("completeShipment", ErpLogConstants.SHIPMENT_STATUS_ADVISED);
    }

    @Test
    public void advanceToInTransitAllowsOnlyDispatched() {
        assertAllowsOnly("advanceToInTransit", ErpLogConstants.SHIPMENT_STATUS_DISPATCHED);
    }

    /**
     * Decision (C) 收紧核对：advanceToDelivered 合法来源态 = {ADVISED,DISPATCHED,IN_TRANSIT}。
     * DRAFT/CANCELLED 为非法来源（code 原无来源态守卫，Bean 刻意收紧——DRAFT 无 trackingNo 不可达、
     * CANCELLED 是终态不应可逆到 DELIVERED）。
     */
    @Test
    public void advanceToDeliveredAllowsOnlyAdvisedDispatchedInTransit() {
        for (String s : ALL_SHIPMENT_STATUSES) {
            if (ErpLogConstants.SHIPMENT_STATUS_ADVISED.equals(s)
                    || ErpLogConstants.SHIPMENT_STATUS_DISPATCHED.equals(s)
                    || ErpLogConstants.SHIPMENT_STATUS_IN_TRANSIT.equals(s)) {
                sm.assertCanAdvanceToDelivered(s); // 合法不抛
            } else {
                NopException ex = assertThrows(NopException.class, () -> sm.assertCanAdvanceToDelivered(s),
                        "advanceToDelivered 对 DRAFT/DELIVERED/CANCELLED 应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
                assertEquals("advanceToDelivered", ex.getParam(ErpLogShipmentStateMachine.ARG_ACTION));
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
            }
        }
    }

    @Test
    public void cancelShipmentAllowsOnlyNonTerminal() {
        for (String s : ALL_SHIPMENT_STATUSES) {
            if (ErpLogConstants.SHIPMENT_STATUS_DRAFT.equals(s)
                    || ErpLogConstants.SHIPMENT_STATUS_ADVISED.equals(s)
                    || ErpLogConstants.SHIPMENT_STATUS_DISPATCHED.equals(s)
                    || ErpLogConstants.SHIPMENT_STATUS_IN_TRANSIT.equals(s)) {
                sm.assertCanCancelShipment(s); // 合法不抛
            } else {
                NopException ex = assertThrows(NopException.class, () -> sm.assertCanCancelShipment(s),
                        "cancelShipment 对 DELIVERED/CANCELLED 应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
                assertEquals("cancelShipment", ex.getParam(ErpLogShipmentStateMachine.ARG_ACTION));
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
            }
        }
    }

    @Test
    public void transitionsMetadataConsistentWithExplicitMethods() {
        for (ErpLogShipmentStateMachine.TransitionDefinition e : sm.transitions()) {
            invokeAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    @Test
    public void terminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpLogConstants.SHIPMENT_STATUS_DELIVERED,
                        ErpLogConstants.SHIPMENT_STATUS_CANCELLED),
                sm.terminalStatuses(), "终态集合 = {DELIVERED, CANCELLED}");
        assertEquals(Collections.singletonList(ErpLogConstants.SHIPMENT_STATUS_DRAFT),
                sm.initialStatuses(), "初始态集合 = {DRAFT}");
        assertTrue(sm.isTerminal(ErpLogConstants.SHIPMENT_STATUS_DELIVERED));
        assertTrue(sm.isTerminal(ErpLogConstants.SHIPMENT_STATUS_CANCELLED));
        assertFalse(sm.isTerminal(ErpLogConstants.SHIPMENT_STATUS_DRAFT));
        assertFalse(sm.isTerminal(ErpLogConstants.SHIPMENT_STATUS_ADVISED));
        assertFalse(sm.isTerminal(ErpLogConstants.SHIPMENT_STATUS_DISPATCHED));
        assertFalse(sm.isTerminal(ErpLogConstants.SHIPMENT_STATUS_IN_TRANSIT));
    }

    /** DELIVERED 与 CANCELLED 均为真终态（无出边——DELIVERED 的退货走 sales 域新流程、CANCELLED 后新建发运单）。 */
    @Test
    public void terminalsAreTrueTerminals() {
        for (ErpLogShipmentStateMachine.TransitionDefinition e : sm.transitions()) {
            assertFalse(sm.isTerminal(e.getFromStatus()),
                    "真终态不应有出边: but edge " + e.getAction() + " leaves " + e.getFromStatus());
        }
    }

    @Test
    public void targetStatusMethods() {
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_ADVISED, sm.adviseTargetStatus());
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_DISPATCHED, sm.completeShipmentTargetStatus());
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_IN_TRANSIT, sm.advanceToInTransitTargetStatus());
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_DELIVERED, sm.advanceToDeliveredTargetStatus());
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_CANCELLED, sm.cancelShipmentTargetStatus());
    }

    @Test
    public void allDictValuesReachable() {
        // 6 值全活跃：DRAFT=初始 seed，其余 5 值为某迁移边目标（无死状态）
        for (String s : ALL_SHIPMENT_STATUSES) {
            if (ErpLogConstants.SHIPMENT_STATUS_DRAFT.equals(s)) {
                assertTrue(sm.initialStatuses().contains(s), "DRAFT 为初始态（seed 写入）");
            } else {
                boolean written = sm.transitions().stream().anyMatch(e -> s.equals(e.getToStatus()));
                assertTrue(written, "dict 值应有 writer 可达: " + s);
            }
        }
    }

    // ==================== helpers ====================

    private void assertAllowsOnly(String action, String allowedFrom) {
        for (String s : ALL_SHIPMENT_STATUSES) {
            if (allowedFrom.equals(s)) {
                invokeAssert(action, s); // 不抛 = 合法
            } else {
                NopException ex = assertThrows(NopException.class, () -> invokeAssert(action, s),
                        action + " 对非允许来源态应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                        "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + s);
                assertEquals(action, ex.getParam(ErpLogShipmentStateMachine.ARG_ACTION));
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
            }
        }
    }

    private void invokeAssert(String action, String status) {
        switch (action) {
            case "advise":
                sm.assertCanAdvise(status);
                break;
            case "completeShipment":
                sm.assertCanCompleteShipment(status);
                break;
            case "advanceToInTransit":
                sm.assertCanAdvanceToInTransit(status);
                break;
            case "advanceToDelivered":
                sm.assertCanAdvanceToDelivered(status);
                break;
            case "cancelShipment":
                sm.assertCanCancelShipment(status);
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "advise":
                return sm.adviseTargetStatus();
            case "completeShipment":
                return sm.completeShipmentTargetStatus();
            case "advanceToInTransit":
                return sm.advanceToInTransitTargetStatus();
            case "advanceToDelivered":
                return sm.advanceToDeliveredTargetStatus();
            case "cancelShipment":
                return sm.cancelShipmentTargetStatus();
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
            for (ErpLogShipmentStateMachine.TransitionDefinition e : sm.transitions()) {
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
