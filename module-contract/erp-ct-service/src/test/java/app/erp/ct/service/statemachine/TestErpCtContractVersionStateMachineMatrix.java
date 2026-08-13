package app.erp.ct.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.ct.service.ErpCtConstants;
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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-13-1430-3 Phase 1 Proof）。
 *
 * <p>针对 {@link ErpCtContractVersionStateMachine} Bean 的纯矩阵完备性遍历：不经 BizModel 入口（层 3 职责），
 * 不断言副作用/审计。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边；</li>
 *   <li>(b) 从 DRAFT 命名动作可达 FINALIZED/SIGNED（线性无分支，dict 3 值全可达——无死状态）；</li>
 *   <li>(c) finalize 仅 DRAFT 合法、sign 仅 FINALIZED 合法、对终态 SIGNED 非法；</li>
 *   <li>(d) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(e) 终态/初始态集合正确（终态 = {SIGNED}；初始 = {DRAFT}）。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpCtContractVersionStateMachineMatrix {

    /** dict {@code erp-ct/version-status} 的 3 值。 */
    private static final List<String> ALL_STATUSES = Arrays.asList(
            ErpCtConstants.VERSION_STATUS_DRAFT,
            ErpCtConstants.VERSION_STATUS_FINALIZED,
            ErpCtConstants.VERSION_STATUS_SIGNED);

    private final ErpCtContractVersionStateMachine sm = new ErpCtContractVersionStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpCtContractVersionStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpCtContractVersionStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(2, edges.size(), "迁移矩阵应有 2 条边（finalize/sign 线性无分支）");
    }

    // ---------- (b) 从 DRAFT 命名动作可达全部声明状态（无死状态） ----------

    @Test
    public void testReachabilityFromDraftCoversAllDictValues() {
        // 线性无分支：DRAFT→FINALIZED→SIGNED，dict 3 值全可达，无死状态
        Set<String> reachable = reachableFrom(ErpCtConstants.VERSION_STATUS_DRAFT);
        assertTrue(reachable.contains(ErpCtConstants.VERSION_STATUS_FINALIZED), "DRAFT→FINALIZED 经 finalize 可达");
        assertTrue(reachable.contains(ErpCtConstants.VERSION_STATUS_SIGNED), "DRAFT→FINALIZED→SIGNED 经 sign 可达");
        assertEquals(2, reachable.size(), "从 DRAFT 经命名动作可达到 2 个状态（FINALIZED + SIGNED）");
    }

    @Test
    public void testTerminalStatusHasNoOutgoingEdges() {
        for (String terminal : sm.terminalStatuses()) {
            for (ErpCtContractVersionStateMachine.TransitionDefinition e : sm.transitions()) {
                assertFalse(e.getFromStatus().equals(terminal),
                        "终态不应有出边: terminal=" + terminal + ", but edge " + e.getAction() + " leaves it");
            }
        }
    }

    // ---------- (c) finalize/sign 合法+非法来源态 ----------

    @Test
    public void testFinalizeLegalOnlyForDraft() {
        // 仅 DRAFT 合法
        sm.assertCanFinalize(ErpCtConstants.VERSION_STATUS_DRAFT);
        assertEquals(ErpCtConstants.VERSION_STATUS_FINALIZED, sm.finalizeTargetStatus());

        for (String s : ALL_STATUSES) {
            if (s.equals(ErpCtConstants.VERSION_STATUS_DRAFT)) {
                continue;
            }
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanFinalize(s),
                    "finalize 对非 DRAFT 应非法: " + s);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                    "Bean 报告 common 层非法迁移码");
            assertEquals("finalize", ex.getParam(ErpCtContractVersionStateMachine.ARG_ACTION), "拒绝元数据携带动作名");
            assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS), "拒绝元数据携带当前态");
        }
    }

    @Test
    public void testSignLegalOnlyForFinalized() {
        // 仅 FINALIZED 合法；终态 SIGNED 非法（无出边）
        sm.assertCanSign(ErpCtConstants.VERSION_STATUS_FINALIZED);
        assertEquals(ErpCtConstants.VERSION_STATUS_SIGNED, sm.signTargetStatus());

        for (String s : ALL_STATUSES) {
            if (s.equals(ErpCtConstants.VERSION_STATUS_FINALIZED)) {
                continue;
            }
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanSign(s),
                    "sign 对非 FINALIZED 应非法: " + s);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                    "Bean 报告 common 层非法迁移码");
            assertEquals("sign", ex.getParam(ErpCtContractVersionStateMachine.ARG_ACTION), "拒绝元数据携带动作名");
            assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS), "拒绝元数据携带当前态");
        }
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpCtContractVersionStateMachine.TransitionDefinition e : sm.transitions()) {
            invokeAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (e) 终态/初始态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpCtConstants.VERSION_STATUS_SIGNED), sm.terminalStatuses(),
                "终态集合 = {SIGNED}");
        assertEquals(Arrays.asList(ErpCtConstants.VERSION_STATUS_DRAFT), sm.initialStatuses(),
                "初始态集合 = {DRAFT}");

        assertTrue(sm.isTerminal(ErpCtConstants.VERSION_STATUS_SIGNED));
        assertFalse(sm.isTerminal(ErpCtConstants.VERSION_STATUS_DRAFT));
        assertFalse(sm.isTerminal(ErpCtConstants.VERSION_STATUS_FINALIZED));
    }

    // ---------- helpers ----------

    private void invokeAssert(String action, String status) {
        switch (action) {
            case "finalize":
                sm.assertCanFinalize(status);
                break;
            case "sign":
                sm.assertCanSign(status);
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "finalize":
                return sm.finalizeTargetStatus();
            case "sign":
                return sm.signTargetStatus();
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
            for (ErpCtContractVersionStateMachine.TransitionDefinition e : sm.transitions()) {
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
