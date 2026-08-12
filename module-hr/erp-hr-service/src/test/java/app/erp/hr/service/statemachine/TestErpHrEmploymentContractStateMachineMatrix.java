package app.erp.hr.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.hr.service.ErpHrConstants;
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
 * <p>针对 {@link ErpHrEmploymentContractStateMachine} Bean 的纯矩阵完备性遍历：不经 BizModel 入口（层 3 职责），
 * 不断言副作用/调动联动/到期 Job 编排。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边（4 边：renew ACTIVE→ACTIVE 自环 + renew EXPIRED→ACTIVE + expire ACTIVE→EXPIRED +
 *       terminate ACTIVE→TERMINATED）；</li>
 *   <li>(b) 从 ACTIVE 可达全部活态（含自环 ACTIVE / EXPIRED / TERMINATED）；纯终态（EXPIRED/TERMINATED）无出边；</li>
 *   <li>(c) renew 多来源态（ACTIVE/EXPIRED 合法，TERMINATED/SUSPENDED 非法）、expire/terminate 单源 ACTIVE；</li>
 *   <li>(d) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(e) 终态/初始态集合正确（= 活态，<strong>SUSPENDED 不在矩阵</strong>：dict 含值但零 writer = 死状态，layer-2 登记）。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpHrEmploymentContractStateMachineMatrix {

    /** 活态全集（不含死状态 SUSPENDED）：ACTIVE/EXPIRED/TERMINATED。 */
    static final List<String> LIVE_STATUSES = Arrays.asList(
            ErpHrConstants.CONTRACT_STATUS_ACTIVE,
            ErpHrConstants.CONTRACT_STATUS_EXPIRED,
            ErpHrConstants.CONTRACT_STATUS_TERMINATED);

    /** dict 全集（含死状态 SUSPENDED）：用于断言 SUSPENDED 不在 transitions/initial/terminal。 */
    static final List<String> DICT_STATUSES = Arrays.asList(
            ErpHrConstants.CONTRACT_STATUS_ACTIVE,
            ErpHrConstants.CONTRACT_STATUS_EXPIRED,
            ErpHrConstants.CONTRACT_STATUS_TERMINATED,
            ErpHrConstants.CONTRACT_STATUS_SUSPENDED);

    private final ErpHrEmploymentContractStateMachine sm = new ErpHrEmploymentContractStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpHrEmploymentContractStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpHrEmploymentContractStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(4, edges.size(), "迁移矩阵应有 4 条边（renew 2 源 + expire 1 + terminate 1）");
    }

    // ---------- (b) 从 ACTIVE 可达活态；TERMINATED 纯终态无出边，EXPIRED 可被续签（renew 例外） ----------

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpHrConstants.CONTRACT_STATUS_ACTIVE);
        // 经 expire 可达 EXPIRED，经 terminate 可达 TERMINATED（helper 排除起点自身）
        assertTrue(reachable.contains(ErpHrConstants.CONTRACT_STATUS_EXPIRED),
                "从 ACTIVE 应可达 EXPIRED");
        assertTrue(reachable.contains(ErpHrConstants.CONTRACT_STATUS_TERMINATED),
                "从 ACTIVE 应可达 TERMINATED");
        // ACTIVE 经 renew 自环可达自身（self-loop），与 reachableFrom helper 排除起点无关——单独断言自环存在
        boolean hasActiveSelfLoop = sm.transitions().stream().anyMatch(e ->
                ErpHrConstants.CONTRACT_STATUS_ACTIVE.equals(e.getFromStatus())
                        && ErpHrConstants.CONTRACT_STATUS_ACTIVE.equals(e.getToStatus())
                        && "renew".equals(e.getAction()));
        assertTrue(hasActiveSelfLoop, "renew 应有 ACTIVE→ACTIVE 自环（对齐 renew:94-100 ACTIVE/EXPIRED→ACTIVE）");
    }

    @Test
    public void testTerminalOutgoingEdgeSemantics() {
        // TERMINATED = 纯终态，无任何出边（终止合同不可恢复）
        for (ErpHrEmploymentContractStateMachine.TransitionDefinition e : sm.transitions()) {
            assertFalse(ErpHrConstants.CONTRACT_STATUS_TERMINATED.equals(e.getFromStatus()),
                    "TERMINATED 纯终态不应有出边: but edge " + e.getAction() + " leaves it");
        }
        // EXPIRED = 终态但可被续签（renew EXCEPTION，对齐 ErpHrEmploymentContractBizModel.renew:94-100
        // 守卫 ACTIVE/EXPIRED→ACTIVE：已过期合同可经 renew 复活为 ACTIVE）。其唯一出边 = renew→ACTIVE。
        List<ErpHrEmploymentContractStateMachine.TransitionDefinition> expiredOut = sm.transitions().stream()
                .filter(e -> ErpHrConstants.CONTRACT_STATUS_EXPIRED.equals(e.getFromStatus()))
                .collect(Collectors.toList());
        assertEquals(1, expiredOut.size(), "EXPIRED 仅有 1 条出边（renew 例外）");
        assertEquals("renew", expiredOut.get(0).getAction(), "EXPIRED 唯一出边 action = renew");
        assertEquals(ErpHrConstants.CONTRACT_STATUS_ACTIVE, expiredOut.get(0).getToStatus(),
                "EXPIRED→renew 目标态 = ACTIVE");
    }

    // ---------- (c) renew 多来源态 + expire/terminate 单源 ----------

    @Test
    public void testRenewLegalForActiveOrExpired() {
        // renew: ACTIVE/EXPIRED 合法
        sm.assertCanRenew(ErpHrConstants.CONTRACT_STATUS_ACTIVE); // 不抛（自环）
        sm.assertCanRenew(ErpHrConstants.CONTRACT_STATUS_EXPIRED); // 不抛
        assertEquals(ErpHrConstants.CONTRACT_STATUS_ACTIVE, sm.renewTargetStatus(),
                "renew 目标态恒为 ACTIVE");

        // TERMINATED/SUSPENDED 非法
        for (String s : Arrays.asList(ErpHrConstants.CONTRACT_STATUS_TERMINATED,
                ErpHrConstants.CONTRACT_STATUS_SUSPENDED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanRenew(s),
                    "renew 对终态/死状态应非法: " + s);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                    "Bean 报告 common 层非法迁移码: status=" + s);
            assertEquals("renew", ex.getParam(ErpHrEmploymentContractStateMachine.ARG_ACTION),
                    "拒绝元数据携带动作名: status=" + s);
            assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                    "拒绝元数据携带当前态: status=" + s);
        }
    }

    @Test
    public void testExpireAllowsOnlyActive() {
        assertActionAllowsOnly("expire", ErpHrConstants.CONTRACT_STATUS_ACTIVE);
    }

    @Test
    public void testTerminateAllowsOnlyActive() {
        assertActionAllowsOnly("terminate", ErpHrConstants.CONTRACT_STATUS_ACTIVE);
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpHrEmploymentContractStateMachine.TransitionDefinition e : sm.transitions()) {
            invokeAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (e) 终态/初始态集合正确 + SUSPENDED 死状态不在矩阵 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(
                ErpHrConstants.CONTRACT_STATUS_EXPIRED,
                ErpHrConstants.CONTRACT_STATUS_TERMINATED), sm.terminalStatuses(),
                "终态集合 = {EXPIRED, TERMINATED}");
        assertEquals(Arrays.asList(ErpHrConstants.CONTRACT_STATUS_ACTIVE), sm.initialStatuses(),
                "初始态集合 = {ACTIVE}");

        assertTrue(sm.isTerminal(ErpHrConstants.CONTRACT_STATUS_EXPIRED));
        assertTrue(sm.isTerminal(ErpHrConstants.CONTRACT_STATUS_TERMINATED));
        assertFalse(sm.isTerminal(ErpHrConstants.CONTRACT_STATUS_ACTIVE));
        assertFalse(sm.isTerminal(ErpHrConstants.CONTRACT_STATUS_SUSPENDED),
                "SUSPENDED 是死状态，isTerminal 应返回 false（不在终态集合）");
    }

    @Test
    public void testSuspendedIsDeadStatusNotInMatrix() {
        // SUSPENDED 是 dict 含值但零 writer 的死状态：不出现在 transitions/initial/terminal 任一集合
        for (ErpHrEmploymentContractStateMachine.TransitionDefinition e : sm.transitions()) {
            assertFalse(ErpHrConstants.CONTRACT_STATUS_SUSPENDED.equals(e.getFromStatus()),
                    "SUSPENDED 死状态不应为任何边的源: " + e);
            assertFalse(ErpHrConstants.CONTRACT_STATUS_SUSPENDED.equals(e.getToStatus()),
                    "SUSPENDED 死状态不应为任何边的目标: " + e);
        }
        assertFalse(sm.initialStatuses().contains(ErpHrConstants.CONTRACT_STATUS_SUSPENDED),
                "SUSPENDED 不在 initial 集合");
        assertFalse(sm.terminalStatuses().contains(ErpHrConstants.CONTRACT_STATUS_SUSPENDED),
                "SUSPENDED 不在 terminal 集合");

        // SUSPENDED 对所有 action 均非法（renew/expire/terminate）
        for (String action : Arrays.asList("renew", "expire", "terminate")) {
            NopException ex = assertThrows(NopException.class,
                    () -> invokeAssert(action, ErpHrConstants.CONTRACT_STATUS_SUSPENDED),
                    "SUSPENDED 对 " + action + " 应非法（死状态无 writer）");
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
        }
    }

    @Test
    public void testTargetStatusMethods() {
        assertEquals(ErpHrConstants.CONTRACT_STATUS_ACTIVE, sm.renewTargetStatus());
        assertEquals(ErpHrConstants.CONTRACT_STATUS_EXPIRED, sm.expireTargetStatus());
        assertEquals(ErpHrConstants.CONTRACT_STATUS_TERMINATED, sm.terminateTargetStatus());
    }

    // ---------- helpers ----------

    private void assertActionAllowsOnly(String action, String allowedFrom) {
        for (String s : DICT_STATUSES) {
            if (allowedFrom.equals(s)) {
                invokeAssert(action, s); // 不抛 = 合法
            } else {
                NopException ex = assertThrows(NopException.class, () -> invokeAssert(action, s),
                        action + " 对非允许来源态应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                        "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + s);
                assertEquals(action, ex.getParam(ErpHrEmploymentContractStateMachine.ARG_ACTION),
                        "拒绝元数据携带动作名: action=" + action + ", status=" + s);
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                        "拒绝元数据携带当前态: action=" + action + ", status=" + s);
            }
        }
    }

    private void invokeAssert(String action, String status) {
        switch (action) {
            case "renew":
                sm.assertCanRenew(status);
                break;
            case "expire":
                sm.assertCanExpire(status);
                break;
            case "terminate":
                sm.assertCanTerminate(status);
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "renew":
                return sm.renewTargetStatus();
            case "expire":
                return sm.expireTargetStatus();
            case "terminate":
                return sm.terminateTargetStatus();
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
            for (ErpHrEmploymentContractStateMachine.TransitionDefinition e : sm.transitions()) {
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
