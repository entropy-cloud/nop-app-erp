package app.erp.mfg.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.mfg.service.ErpMfgConstants;
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
 * <p>针对 {@link ErpMfgForecastStateMachine} Bean 的纯矩阵完备性遍历：不经 BizModel 入口（层 3 职责），
 * 不断言副作用/审计。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边；</li>
 *   <li>(b) 从 DRAFT 可达 APPROVED/CANCELLED（cancel 多源含 DRAFT 直达）；</li>
 *   <li>(c) cancel 多来源 {DRAFT, APPROVED} 合法、对终态 CANCELLED 非法、对 CONSUMED 非法（refuse-dead-state）；</li>
 *   <li>(d) approve 仅 DRAFT 合法、对 APPROVED/CANCELLED/CONSUMED 非法；</li>
 *   <li>(e) CANCELLED 终态无出边；</li>
 *   <li>(f) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(g) 终态/初始态集合正确；</li>
 *   <li>(h) CONSUMED 无任何边、不在终态集（预留死状态，Bean 不编码该态）。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpMfgForecastStateMachineMatrix {

    private static final List<String> ALL_STATUSES = Arrays.asList(
            ErpMfgConstants.FORECAST_STATUS_DRAFT,
            ErpMfgConstants.FORECAST_STATUS_APPROVED,
            ErpMfgConstants.FORECAST_STATUS_CONSUMED,
            ErpMfgConstants.FORECAST_STATUS_CANCELLED);

    private final ErpMfgForecastStateMachine sm = new ErpMfgForecastStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpMfgForecastStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpMfgForecastStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            // 同一 action + 同一 fromStatus 不得出现多次（否则冲突）
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(3, edges.size(), "迁移矩阵应有 3 条边（approve 1 + cancel 多源 2）");
    }

    // ---------- (b) 从 DRAFT 可达 APPROVED/CANCELLED（cancel 多源含 DRAFT 直达） ----------

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpMfgConstants.FORECAST_STATUS_DRAFT);
        assertTrue(reachable.contains(ErpMfgConstants.FORECAST_STATUS_APPROVED), "从 DRAFT 应可达 APPROVED");
        assertTrue(reachable.contains(ErpMfgConstants.FORECAST_STATUS_CANCELLED), "从 DRAFT 应可达 CANCELLED（cancel 多源含 DRAFT 直达）");
        // CONSUMED 为预留死状态，从 DRAFT 不可达
        assertFalse(reachable.contains(ErpMfgConstants.FORECAST_STATUS_CONSUMED),
                "CONSUMED 为预留死状态，从 DRAFT 不应可达");
    }

    // ---------- (c) cancel 多来源 {DRAFT, APPROVED} 合法、对终态 CANCELLED / 死状态 CONSUMED 非法 ----------

    @Test
    public void testCancelLegalForDraftAndApprovedIllegalForTerminalAndDead() {
        // 合法来源：DRAFT、APPROVED
        sm.assertCanCancel(ErpMfgConstants.FORECAST_STATUS_DRAFT);
        sm.assertCanCancel(ErpMfgConstants.FORECAST_STATUS_APPROVED);
        assertEquals(ErpMfgConstants.FORECAST_STATUS_CANCELLED, sm.cancelTargetStatus());

        // 非法来源：CANCELLED 终态（refuse-terminal）
        NopException fromTerminal = assertThrows(NopException.class,
                () -> sm.assertCanCancel(ErpMfgConstants.FORECAST_STATUS_CANCELLED),
                "cancel 对终态 CANCELLED 应非法");
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), fromTerminal.getErrorCode(),
                "Bean 报告 common 层非法迁移码");
        assertEquals("cancel", fromTerminal.getParam(ErpMfgForecastStateMachine.ARG_ACTION), "拒绝元数据携带动作名");
        assertEquals(ErpMfgConstants.FORECAST_STATUS_CANCELLED,
                fromTerminal.getParam(ErpCommonErrors.ARG_CURRENT_STATUS), "拒绝元数据携带当前态");

        // 非法来源：CONSUMED 预留死状态（refuse-dead-state）
        NopException fromDead = assertThrows(NopException.class,
                () -> sm.assertCanCancel(ErpMfgConstants.FORECAST_STATUS_CONSUMED),
                "cancel 对预留死状态 CONSUMED 应非法");
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), fromDead.getErrorCode(),
                "CONSUMED 拒绝同样报告 common 层非法迁移码（Bean 经正向 allow-list 拒绝）");
        assertEquals(ErpMfgConstants.FORECAST_STATUS_CONSUMED,
                fromDead.getParam(ErpCommonErrors.ARG_CURRENT_STATUS), "拒绝元数据携带当前态 CONSUMED");
    }

    // ---------- (d) approve 仅 DRAFT 合法、对 APPROVED/CANCELLED/CONSUMED 非法 ----------

    @Test
    public void testApproveAllowsOnlyDraft() {
        // 仅 DRAFT 合法
        sm.assertCanApprove(ErpMfgConstants.FORECAST_STATUS_DRAFT);
        assertEquals(ErpMfgConstants.FORECAST_STATUS_APPROVED, sm.approveTargetStatus());

        // 其余全部非法
        for (String s : Arrays.asList(
                ErpMfgConstants.FORECAST_STATUS_APPROVED,
                ErpMfgConstants.FORECAST_STATUS_CANCELLED,
                ErpMfgConstants.FORECAST_STATUS_CONSUMED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanApprove(s),
                    "approve 对非 DRAFT 应非法: " + s);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                    "Bean 报告 common 层非法迁移码: status=" + s);
            assertEquals("approve", ex.getParam(ErpMfgForecastStateMachine.ARG_ACTION),
                    "拒绝元数据携带动作名");
            assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                    "拒绝元数据携带当前态: status=" + s);
        }
    }

    // ---------- (e) CANCELLED 终态无出边 ----------

    @Test
    public void testTerminalStatusHasNoOutgoingEdges() {
        for (String terminal : sm.terminalStatuses()) {
            for (ErpMfgForecastStateMachine.TransitionDefinition e : sm.transitions()) {
                assertFalse(e.getFromStatus().equals(terminal),
                        "终态不应有出边: terminal=" + terminal + ", but edge " + e.getAction() + " leaves it");
            }
        }
    }

    // ---------- (f) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpMfgForecastStateMachine.TransitionDefinition e : sm.transitions()) {
            // 每条边的 fromStatus 对该 action 合法（assert 放行不抛）
            invokeAssert(e.getAction(), e.getFromStatus());
            // 每条边的 toStatus 与 <action>TargetStatus() 一致
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (g) 终态/初始态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Collections.singletonList(ErpMfgConstants.FORECAST_STATUS_CANCELLED),
                sm.terminalStatuses(), "终态集合 = {CANCELLED}");
        assertEquals(Collections.singletonList(ErpMfgConstants.FORECAST_STATUS_DRAFT),
                sm.initialStatuses(), "初始态集合 = {DRAFT}");

        assertTrue(sm.isTerminal(ErpMfgConstants.FORECAST_STATUS_CANCELLED));
        assertFalse(sm.isTerminal(ErpMfgConstants.FORECAST_STATUS_DRAFT));
        assertFalse(sm.isTerminal(ErpMfgConstants.FORECAST_STATUS_APPROVED));
    }

    // ---------- (h) CONSUMED 无任何边、不在终态集（预留死状态） ----------

    @Test
    public void testConsumedIsDeadStateNoEdgesNotTerminal() {
        // CONSUMED 既非任何边的来源，亦非任何边的目标
        for (ErpMfgForecastStateMachine.TransitionDefinition e : sm.transitions()) {
            assertFalse(ErpMfgConstants.FORECAST_STATUS_CONSUMED.equals(e.getFromStatus()),
                    "CONSUMED 不应是任何边的来源: edge=" + e.getAction());
            assertFalse(ErpMfgConstants.FORECAST_STATUS_CONSUMED.equals(e.getToStatus()),
                    "CONSUMED 不应是任何边的目标: edge=" + e.getAction());
        }
        // CONSUMED 不在终态集
        assertFalse(sm.isTerminal(ErpMfgConstants.FORECAST_STATUS_CONSUMED),
                "CONSUMED 不入终态集（预留死状态，owner doc Decision A）");
        assertFalse(sm.terminalStatuses().contains(ErpMfgConstants.FORECAST_STATUS_CONSUMED),
                "terminalStatuses() 不应包含 CONSUMED");
        // CONSUMED 不在初始态集
        assertFalse(sm.initialStatuses().contains(ErpMfgConstants.FORECAST_STATUS_CONSUMED),
                "initialStatuses() 不应包含 CONSUMED");
        // CONSUMED 对 approve/cancel 均非法（Bean 不编码涉及 CONSUMED 的边）
        assertThrows(NopException.class, () -> sm.assertCanApprove(ErpMfgConstants.FORECAST_STATUS_CONSUMED),
                "approve 对 CONSUMED 应非法");
        assertThrows(NopException.class, () -> sm.assertCanCancel(ErpMfgConstants.FORECAST_STATUS_CONSUMED),
                "cancel 对 CONSUMED 应非法");
    }

    // ---------- helpers ----------

    private void invokeAssert(String action, String status) {
        switch (action) {
            case "approve":
                sm.assertCanApprove(status);
                break;
            case "cancel":
                sm.assertCanCancel(status);
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "approve":
                return sm.approveTargetStatus();
            case "cancel":
                return sm.cancelTargetStatus();
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
            for (ErpMfgForecastStateMachine.TransitionDefinition e : sm.transitions()) {
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
