package app.erp.crm.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.crm.service.ErpCrmConstants;
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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-13-0945-3 Phase 1 Proof）。
 *
 * <p>针对 {@link ErpCrmLeadStateMachine} Bean 的纯矩阵完备性遍历：不经 BizModel 入口（层 3 职责），
 * 不断言副作用/跨域报价单/客户创建/stageId 守卫/审计。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边；</li>
 *   <li>(b) 从 NEW 命名动作可达 QUALIFIED/LOST/CANCELLED/CONVERTED 全部声明状态；</li>
 *   <li>(c) QUALIFIED 不可回 NEW（assertCanQualify 对 QUALIFIED 抛 common 码）；</li>
 *   <li>(d) {@code assertCanConvert} 据 Decision 分支 (a) 仅对 CONVERTED 抛 common 码、
 *       对一切非 CONVERTED 态（NEW/QUALIFIED/LOST/CANCELLED）运行时通过；</li>
 *   <li>(e) 终态 CONVERTED/LOST/CANCELLED 的 qualify/lose/cancel assertCanXxx 抛 common 码携带 action/fromStatus；</li>
 *   <li>(f) {@code transitions()} 编码意图矩阵 {NEW,QUALIFIED}→CONVERTED；</li>
 *   <li>(g) 初始集 {NEW}、终态集 {CONVERTED,LOST,CANCELLED} 正确。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpCrmLeadStateMachineMatrix {

    /** dict {@code erp-crm/lead-doc-status} 的 5 值。 */
    private static final List<String> ALL_STATUSES = Arrays.asList(
            ErpCrmConstants.DOC_STATUS_NEW,
            ErpCrmConstants.DOC_STATUS_QUALIFIED,
            ErpCrmConstants.DOC_STATUS_CONVERTED,
            ErpCrmConstants.DOC_STATUS_LOST,
            ErpCrmConstants.DOC_STATUS_CANCELLED);

    private final ErpCrmLeadStateMachine sm = new ErpCrmLeadStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpCrmLeadStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpCrmLeadStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(7, edges.size(), "迁移矩阵应有 7 条意图边（qualify 1 + lose 2 + cancel 2 + convert 2）");
    }

    // ---------- (b) 从 NEW 命名动作可达全部声明状态 ----------

    @Test
    public void testReachabilityFromNewCoversAllDeclaredStatuses() {
        Set<String> reachable = reachableFrom(ErpCrmConstants.DOC_STATUS_NEW);
        assertTrue(reachable.contains(ErpCrmConstants.DOC_STATUS_QUALIFIED), "NEW→QUALIFIED 经 qualify 可达");
        assertTrue(reachable.contains(ErpCrmConstants.DOC_STATUS_LOST), "NEW→LOST 经 lose 可达");
        assertTrue(reachable.contains(ErpCrmConstants.DOC_STATUS_CANCELLED), "NEW→CANCELLED 经 cancel 可达");
        assertTrue(reachable.contains(ErpCrmConstants.DOC_STATUS_CONVERTED), "NEW→CONVERTED 经 convert 可达");
    }

    @Test
    public void testTerminalStatusesHaveNoOutgoingEdges() {
        for (String terminal : sm.terminalStatuses()) {
            for (ErpCrmLeadStateMachine.TransitionDefinition e : sm.transitions()) {
                assertFalse(e.getFromStatus().equals(terminal),
                        "终态不应有出边: terminal=" + terminal + ", but edge " + e.getAction() + " leaves it");
            }
        }
    }

    // ---------- (c) QUALIFIED 不可回 NEW ----------

    @Test
    public void testQualifiedCannotReturnToNew() {
        NopException ex = assertThrows(NopException.class,
                () -> sm.assertCanQualify(ErpCrmConstants.DOC_STATUS_QUALIFIED),
                "QUALIFIED 不可再次 qualify（不可回 NEW）");
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "assertCanQualify(QUALIFIED) 报告 common 层非法迁移码");
        assertEquals("qualify", ex.getParam(ErpCrmLeadStateMachine.ARG_ACTION), "拒绝元数据携带动作名");
        assertEquals(ErpCrmConstants.DOC_STATUS_QUALIFIED, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                "拒绝元数据携带当前态");
    }

    // ---------- (d) assertCanConvert 仅拒 CONVERTED，其余非 CONVERTED 态运行时通过 ----------

    @Test
    public void testAssertCanConvertOnlyRejectsConverted() {
        // CONVERTED 被拒
        NopException ex = assertThrows(NopException.class,
                () -> sm.assertCanConvert(ErpCrmConstants.DOC_STATUS_CONVERTED),
                "CONVERTED 不可重复转化");
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "assertCanConvert(CONVERTED) 报告 common 层非法迁移码");
        assertEquals("convert", ex.getParam(ErpCrmLeadStateMachine.ARG_ACTION), "拒绝元数据携带动作名");

        // 一切非 CONVERTED 态运行时通过（匹配现行 validateNotConverted 行为，保持既有外部行为不变）
        for (String nonConverted : Arrays.asList(
                ErpCrmConstants.DOC_STATUS_NEW,
                ErpCrmConstants.DOC_STATUS_QUALIFIED,
                ErpCrmConstants.DOC_STATUS_LOST,
                ErpCrmConstants.DOC_STATUS_CANCELLED)) {
            String status = nonConverted;
            assertDoesNotThrow(() -> sm.assertCanConvert(status),
                    "assertCanConvert 对非 CONVERTED 态应运行时通过: " + status);
        }
    }

    // ---------- (e) 终态 qualify/lose/cancel assertCanXxx 抛 common 码 + action/fromStatus ----------

    @Test
    public void testTerminalStatusesRejectQualifyLoseCancel() {
        for (String terminal : sm.terminalStatuses()) {
            assertActionRejectedOn("qualify", terminal);
            assertActionRejectedOn("lose", terminal);
            assertActionRejectedOn("cancel", terminal);
        }
    }

    // ---------- (f) transitions() 编码意图矩阵 {NEW,QUALIFIED}→CONVERTED ----------

    @Test
    public void testTransitionsEncodeIntentMatrixForConvert() {
        List<String> convertFromStatuses = sm.transitions().stream()
                .filter(e -> "convert".equals(e.getAction()))
                .map(ErpCrmLeadStateMachine.TransitionDefinition::getFromStatus)
                .collect(Collectors.toList());
        assertEquals(Arrays.asList(ErpCrmConstants.DOC_STATUS_NEW, ErpCrmConstants.DOC_STATUS_QUALIFIED),
                convertFromStatuses, "意图矩阵 convert 来源态 = {NEW, QUALIFIED}");
    }

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpCrmLeadStateMachine.TransitionDefinition e : sm.transitions()) {
            // 每条意图边的 fromStatus 对该 action 合法（assert 放行不抛）
            invokeAssert(e.getAction(), e.getFromStatus());
            // 每条边的 toStatus 与 <action>TargetStatus() 一致
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (g) 初始集/终态集正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpCrmConstants.DOC_STATUS_CONVERTED, ErpCrmConstants.DOC_STATUS_LOST,
                ErpCrmConstants.DOC_STATUS_CANCELLED), sm.terminalStatuses(),
                "终态集合 = {CONVERTED, LOST, CANCELLED}");
        assertEquals(Arrays.asList(ErpCrmConstants.DOC_STATUS_NEW), sm.initialStatuses(),
                "初始态集合 = {NEW}");

        assertTrue(sm.isTerminal(ErpCrmConstants.DOC_STATUS_CONVERTED));
        assertTrue(sm.isTerminal(ErpCrmConstants.DOC_STATUS_LOST));
        assertTrue(sm.isTerminal(ErpCrmConstants.DOC_STATUS_CANCELLED));
        assertFalse(sm.isTerminal(ErpCrmConstants.DOC_STATUS_NEW));
        assertFalse(sm.isTerminal(ErpCrmConstants.DOC_STATUS_QUALIFIED));
    }

    // ---------- 合法/非法来源态显式断言 ----------

    @Test
    public void testExplicitActionGuards() {
        // qualify: 仅 NEW 合法
        assertActionAllowsOnly("qualify", ErpCrmConstants.DOC_STATUS_NEW);
        // lose: NEW/QUALIFIED 合法
        assertActionAllowsOnly("lose", ErpCrmConstants.DOC_STATUS_NEW, ErpCrmConstants.DOC_STATUS_QUALIFIED);
        // cancel: NEW/QUALIFIED 合法
        assertActionAllowsOnly("cancel", ErpCrmConstants.DOC_STATUS_NEW, ErpCrmConstants.DOC_STATUS_QUALIFIED);
    }

    @Test
    public void testTargetStatusMethods() {
        assertEquals(ErpCrmConstants.DOC_STATUS_QUALIFIED, sm.qualifyTargetStatus());
        assertEquals(ErpCrmConstants.DOC_STATUS_LOST, sm.loseTargetStatus());
        assertEquals(ErpCrmConstants.DOC_STATUS_CANCELLED, sm.cancelTargetStatus());
        assertEquals(ErpCrmConstants.DOC_STATUS_CONVERTED, sm.convertTargetStatus());
    }

    // ---------- helpers ----------

    private void assertActionRejectedOn(String action, String terminal) {
        NopException ex = assertThrows(NopException.class, () -> invokeAssert(action, terminal),
                action + " 对终态应非法: " + terminal);
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                action + "(terminal) 报告 common 层非法迁移码");
        assertEquals(action, ex.getParam(ErpCrmLeadStateMachine.ARG_ACTION),
                action + " 拒绝元数据携带动作名");
        assertEquals(terminal, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                action + " 拒绝元数据携带当前态");
    }

    /**
     * 断言某 action 仅允许指定来源态集合：允许的来源态放行（不抛），其余全部状态非法（抛 common 码 + action 元数据）。
     */
    private void assertActionAllowsOnly(String action, String... allowed) {
        Set<String> allowedSet = new HashSet<>(Arrays.asList(allowed));
        for (String s : ALL_STATUSES) {
            if (allowedSet.contains(s)) {
                invokeAssert(action, s); // 不抛 = 合法
            } else {
                NopException ex = assertThrows(NopException.class, () -> invokeAssert(action, s),
                        action + " 对非允许来源态应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                        "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + s);
                assertEquals(action, ex.getParam(ErpCrmLeadStateMachine.ARG_ACTION),
                        "拒绝元数据携带动作名: action=" + action);
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                        "拒绝元数据携带当前态: action=" + action + ", status=" + s);
            }
        }
    }

    private void invokeAssert(String action, String status) {
        switch (action) {
            case "qualify":
                sm.assertCanQualify(status);
                break;
            case "lose":
                sm.assertCanLose(status);
                break;
            case "cancel":
                sm.assertCanCancel(status);
                break;
            case "convert":
                sm.assertCanConvert(status);
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "qualify":
                return sm.qualifyTargetStatus();
            case "lose":
                return sm.loseTargetStatus();
            case "cancel":
                return sm.cancelTargetStatus();
            case "convert":
                return sm.convertTargetStatus();
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
            for (ErpCrmLeadStateMachine.TransitionDefinition e : sm.transitions()) {
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
