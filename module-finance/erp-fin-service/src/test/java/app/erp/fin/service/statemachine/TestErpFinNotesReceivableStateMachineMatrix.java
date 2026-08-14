package app.erp.fin.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.fin.service.ErpFinConstants;
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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-13-1146-1 Phase 1 Proof）。
 *
 * <p>针对 {@link ErpFinNotesReceivableStateMachine} 的纯矩阵完备性遍历：{@code status} 单轴
 * （dict {@code erp-fin/notes-receivable-status} 7 值 RECEIVED/DISCOUNTED/ENDORSED/COLLECTION_PENDING/
 * HONORED/DISHONORED/WRITE_OFF）。不经 BizModel/Processor 入口（层 3 职责），不断言副作用/过账/红冲/FX 派生。
 *
 * <p>每轴覆盖：(a) 无重复/冲突边；(b) 从初始态可达全部声明状态 + 终态无出边；(c) 各动作合法/非法来源态全集；
 * (d) {@code transitions()} 元数据与显式方法语义一致；(e) 终态/初始态集合正确；(f) 7 值全可达（无死状态）。
 * Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 *
 * <p><b>ENDORSED 非终态边界</b>：ENDORSED 背书后票据所有权已转移，仅可 writeOff 出边——
 * collect/discount/endorse 从 ENDORSED 均非法（collect 只允许 RECEIVED/DISCOUNTED）。
 *
 * <p><b>receive initial 写入</b>：{@code null}（初始写入 §9.2 选项 c）与 RECEIVED（幂等）合法，
 * 其余来源态非法（receive 守卫有意收窄）。
 */
public class TestErpFinNotesReceivableStateMachineMatrix {

    // erp-fin/notes-receivable-status 字典七态
    private static final List<String> ALL_STATUSES = Arrays.asList(
            ErpFinConstants.NOTES_RECV_RECEIVED,
            ErpFinConstants.NOTES_RECV_DISCOUNTED,
            ErpFinConstants.NOTES_RECV_ENDORSED,
            ErpFinConstants.NOTES_RECV_COLLECTION_PENDING,
            ErpFinConstants.NOTES_RECV_HONORED,
            ErpFinConstants.NOTES_RECV_DISHONORED,
            ErpFinConstants.NOTES_RECV_WRITE_OFF);

    private final ErpFinNotesReceivableStateMachine sm = new ErpFinNotesReceivableStateMachine();

    @Test
    public void noDuplicateOrConflictingEdges() {
        List<ErpFinNotesReceivableStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpFinNotesReceivableStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(7, edges.size(), "迁移矩阵应有 7 命名边（receive/discount/endorse/collect/honor/dishonor/writeOff）");
    }

    @Test
    public void reachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpFinConstants.NOTES_RECV_RECEIVED);
        assertTrue(reachable.contains(ErpFinConstants.NOTES_RECV_DISCOUNTED), "从 RECEIVED 应可达 DISCOUNTED");
        assertTrue(reachable.contains(ErpFinConstants.NOTES_RECV_ENDORSED), "从 RECEIVED 应可达 ENDORSED");
        assertTrue(reachable.contains(ErpFinConstants.NOTES_RECV_COLLECTION_PENDING), "从 RECEIVED 应可达 COLLECTION_PENDING");
        assertTrue(reachable.contains(ErpFinConstants.NOTES_RECV_HONORED), "从 RECEIVED 应可达 HONORED");
        assertTrue(reachable.contains(ErpFinConstants.NOTES_RECV_DISHONORED), "从 RECEIVED 应可达 DISHONORED");
        assertTrue(reachable.contains(ErpFinConstants.NOTES_RECV_WRITE_OFF), "从 RECEIVED 应可达 WRITE_OFF");
    }

    @Test
    public void receiveAllowsOnlyInitialOrIdempotent() {
        sm.assertCanReceive(null); // initial 写入（§9.2 选项 c）
        sm.assertCanReceive(ErpFinConstants.NOTES_RECV_RECEIVED); // 幂等（isAlreadyReceived 短路）
        for (String s : Arrays.asList(ErpFinConstants.NOTES_RECV_DISCOUNTED,
                ErpFinConstants.NOTES_RECV_ENDORSED,
                ErpFinConstants.NOTES_RECV_COLLECTION_PENDING,
                ErpFinConstants.NOTES_RECV_HONORED,
                ErpFinConstants.NOTES_RECV_DISHONORED,
                ErpFinConstants.NOTES_RECV_WRITE_OFF)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanReceive(s),
                    "receive 对非 initial 来源态应非法: " + s);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
            assertEquals("receive", ex.getParam(ErpFinNotesReceivableStateMachine.ARG_ACTION));
            assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
        }
    }

    @Test
    public void discountAllowsOnlyReceived() {
        assertAllowsOnly("discount", ErpFinConstants.NOTES_RECV_RECEIVED);
    }

    @Test
    public void endorseAllowsOnlyReceived() {
        assertAllowsOnly("endorse", ErpFinConstants.NOTES_RECV_RECEIVED);
    }

    @Test
    public void collectAllowsOnlyReceivedOrDiscounted() {
        sm.assertCanCollect(ErpFinConstants.NOTES_RECV_RECEIVED); // 合法不抛
        sm.assertCanCollect(ErpFinConstants.NOTES_RECV_DISCOUNTED); // 合法不抛
        for (String s : Arrays.asList(ErpFinConstants.NOTES_RECV_ENDORSED,
                ErpFinConstants.NOTES_RECV_COLLECTION_PENDING,
                ErpFinConstants.NOTES_RECV_HONORED,
                ErpFinConstants.NOTES_RECV_DISHONORED,
                ErpFinConstants.NOTES_RECV_WRITE_OFF)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanCollect(s),
                    "collect 对 ENDORSED/COLLECTION_PENDING/终态应非法: " + s);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
            assertEquals("collect", ex.getParam(ErpFinNotesReceivableStateMachine.ARG_ACTION));
            assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
            assertEquals("RECEIVED 或 DISCOUNTED", ex.getParam(ErpCommonErrors.ARG_EXPECTED_STATUS),
                    "collect expected 文案对外不变");
        }
    }

    @Test
    public void honorAllowsOnlyCollectionPending() {
        assertAllowsOnly("honor", ErpFinConstants.NOTES_RECV_COLLECTION_PENDING);
    }

    @Test
    public void dishonorAllowsOnlyCollectionPending() {
        assertAllowsOnly("dishonor", ErpFinConstants.NOTES_RECV_COLLECTION_PENDING);
    }

    @Test
    public void writeOffAllowsOnlyNonTerminal() {
        // 4 非终态合法
        for (String s : Arrays.asList(ErpFinConstants.NOTES_RECV_RECEIVED,
                ErpFinConstants.NOTES_RECV_DISCOUNTED,
                ErpFinConstants.NOTES_RECV_ENDORSED,
                ErpFinConstants.NOTES_RECV_COLLECTION_PENDING)) {
            sm.assertCanWriteOff(s); // 合法不抛
        }
        // 3 终态非法（expected「非终态」文案对外不变）
        for (String s : Arrays.asList(ErpFinConstants.NOTES_RECV_HONORED,
                ErpFinConstants.NOTES_RECV_DISHONORED,
                ErpFinConstants.NOTES_RECV_WRITE_OFF)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanWriteOff(s),
                    "writeOff 对终态应非法: " + s);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
            assertEquals("writeOff", ex.getParam(ErpFinNotesReceivableStateMachine.ARG_ACTION));
            assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
            assertEquals("非终态", ex.getParam(ErpCommonErrors.ARG_EXPECTED_STATUS),
                    "writeOff expected 文案对外不变");
        }
    }

    /** ENDORSED 非终态但仅 writeOff 出边：collect/discount/endorse 从 ENDORSED 均非法。 */
    @Test
    public void endorsedIsNonTerminalIntermediateWithOnlyWriteOffExit() {
        assertFalse(sm.isTerminal(ErpFinConstants.NOTES_RECV_ENDORSED), "ENDORSED 非终态");
        sm.assertCanWriteOff(ErpFinConstants.NOTES_RECV_ENDORSED); // 唯一出边 writeOff
        assertThrows(NopException.class, () -> sm.assertCanCollect(ErpFinConstants.NOTES_RECV_ENDORSED),
                "背书后不可托收");
        assertThrows(NopException.class, () -> sm.assertCanDiscount(ErpFinConstants.NOTES_RECV_ENDORSED),
                "背书后不可贴现");
        assertThrows(NopException.class, () -> sm.assertCanEndorse(ErpFinConstants.NOTES_RECV_ENDORSED),
                "背书后不可再次背书");
    }

    @Test
    public void terminalsAreTrueTerminals() {
        for (ErpFinNotesReceivableStateMachine.TransitionDefinition e : sm.transitions()) {
            assertFalse(sm.isTerminal(e.getFromStatus()),
                    "真终态不应有出边: but edge " + e.getAction() + " leaves " + e.getFromStatus());
        }
    }

    @Test
    public void transitionsMetadataConsistentWithExplicitMethods() {
        for (ErpFinNotesReceivableStateMachine.TransitionDefinition e : sm.transitions()) {
            invokeAssert(e.getAction(), e.getFromStatus()); // 代表源态须合法
            assertEquals(targetStatusFor(e.getAction()), e.getToStatus(),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    @Test
    public void targetStatusMethods() {
        assertEquals(ErpFinConstants.NOTES_RECV_RECEIVED, sm.receiveTargetStatus());
        assertEquals(ErpFinConstants.NOTES_RECV_DISCOUNTED, sm.discountTargetStatus());
        assertEquals(ErpFinConstants.NOTES_RECV_ENDORSED, sm.endorseTargetStatus());
        assertEquals(ErpFinConstants.NOTES_RECV_COLLECTION_PENDING, sm.collectTargetStatus());
        assertEquals(ErpFinConstants.NOTES_RECV_HONORED, sm.honorTargetStatus());
        assertEquals(ErpFinConstants.NOTES_RECV_DISHONORED, sm.dishonorTargetStatus());
        assertEquals(ErpFinConstants.NOTES_RECV_WRITE_OFF, sm.writeOffTargetStatus());
    }

    @Test
    public void terminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpFinConstants.NOTES_RECV_HONORED,
                ErpFinConstants.NOTES_RECV_DISHONORED,
                ErpFinConstants.NOTES_RECV_WRITE_OFF), sm.terminalStatuses(), "终态集合 = {HONORED, DISHONORED, WRITE_OFF}");
        assertEquals(Collections.singletonList(ErpFinConstants.NOTES_RECV_RECEIVED),
                sm.initialStatuses(), "初始态集合 = {RECEIVED}");
        assertTrue(sm.isTerminal(ErpFinConstants.NOTES_RECV_HONORED));
        assertTrue(sm.isTerminal(ErpFinConstants.NOTES_RECV_DISHONORED));
        assertTrue(sm.isTerminal(ErpFinConstants.NOTES_RECV_WRITE_OFF));
        assertFalse(sm.isTerminal(ErpFinConstants.NOTES_RECV_RECEIVED));
        assertFalse(sm.isTerminal(ErpFinConstants.NOTES_RECV_DISCOUNTED));
        assertFalse(sm.isTerminal(ErpFinConstants.NOTES_RECV_COLLECTION_PENDING));
    }

    @Test
    public void allDictValuesReachable() {
        // 7 值全可达：RECEIVED=初始态（receive initial 写入），其余 6 值均为某迁移边 toStatus
        for (String s : ALL_STATUSES) {
            if (ErpFinConstants.NOTES_RECV_RECEIVED.equals(s)) {
                assertTrue(sm.initialStatuses().contains(s), "RECEIVED 为初始态（receive 写入）");
            } else {
                boolean written = sm.transitions().stream().anyMatch(e -> s.equals(e.getToStatus()));
                assertTrue(written, "dict 值应有 writer 可达: " + s);
            }
        }
    }

    // ==================== helpers ====================

    private void assertAllowsOnly(String action, String allowedFrom) {
        for (String s : ALL_STATUSES) {
            if (allowedFrom.equals(s)) {
                invokeAssert(action, s); // 合法不抛
            } else {
                NopException ex = assertThrows(NopException.class, () -> invokeAssert(action, s),
                        action + " 对非允许来源态应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                        "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + s);
                assertEquals(action, ex.getParam(ErpFinNotesReceivableStateMachine.ARG_ACTION));
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
            }
        }
    }

    private void invokeAssert(String action, String status) {
        switch (action) {
            case "receive":
                sm.assertCanReceive(status);
                break;
            case "discount":
                sm.assertCanDiscount(status);
                break;
            case "endorse":
                sm.assertCanEndorse(status);
                break;
            case "collect":
                sm.assertCanCollect(status);
                break;
            case "honor":
                sm.assertCanHonor(status);
                break;
            case "dishonor":
                sm.assertCanDishonor(status);
                break;
            case "writeOff":
                sm.assertCanWriteOff(status);
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "receive":
                return sm.receiveTargetStatus();
            case "discount":
                return sm.discountTargetStatus();
            case "endorse":
                return sm.endorseTargetStatus();
            case "collect":
                return sm.collectTargetStatus();
            case "honor":
                return sm.honorTargetStatus();
            case "dishonor":
                return sm.dishonorTargetStatus();
            case "writeOff":
                return sm.writeOffTargetStatus();
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
            for (ErpFinNotesReceivableStateMachine.TransitionDefinition e : sm.transitions()) {
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
