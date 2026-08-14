package app.erp.fin.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.exceptions.NopException;
import org.junit.jupiter.api.Test;

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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-14-0456-1 Phase 1 Proof）。
 *
 * <p>针对 {@link ErpFinReconciliationDocumentStateMachine} Bean（docStatus 核销单生命周期轴）的纯矩阵完备性遍历：
 * 不经 BizModel 入口（层 3 职责），不断言副作用/审计/前置校验。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边（post + reverse = 2 条边、2 命名动作）；</li>
 *   <li>(b) post（DRAFT 合法、POSTED/REVERSED 非法）；</li>
 *   <li>(c) reverse（POSTED 合法、DRAFT/REVERSED 非法）；</li>
 *   <li>(d) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(e) 初始={DRAFT}/终态={REVERSED}（POSTED 为中间态，非终态且有出边）；</li>
 *   <li>(f) 可达性：从 DRAFT 可达 POSTED 与 REVERSED，无多余可达态。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpFinReconciliationStateMachineMatrix {

    private static final List<String> ALL_DICT_STATUSES = Arrays.asList(
            ErpFinConstants.RECON_STATUS_DRAFT,
            ErpFinConstants.RECON_STATUS_POSTED,
            ErpFinConstants.RECON_STATUS_REVERSED);

    private final ErpFinReconciliationDocumentStateMachine sm = new ErpFinReconciliationDocumentStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpFinReconciliationDocumentStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpFinReconciliationDocumentStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(2, edges.size(), "迁移矩阵应有 2 条边（post: DRAFT→POSTED + reverse: POSTED→REVERSED）");
        Set<String> actions = edges.stream()
                .map(ErpFinReconciliationDocumentStateMachine.TransitionDefinition::getAction)
                .collect(Collectors.toSet());
        assertEquals(2, actions.size(), "2 命名动作（post/reverse）");
    }

    // ---------- (b) post：DRAFT 合法、POSTED/REVERSED 非法 ----------

    @Test
    public void testAssertCanPostLegalAndIllegal() {
        // DRAFT 合法（唯一迁移边来源态）
        sm.assertCanPost(ErpFinConstants.RECON_STATUS_DRAFT); // 不抛
        assertEquals(ErpFinConstants.RECON_STATUS_POSTED, sm.postTargetStatus());

        // POSTED / REVERSED 非法（抛 common 码 + action/currentStatus 元数据）
        for (String s : Arrays.asList(ErpFinConstants.RECON_STATUS_POSTED, ErpFinConstants.RECON_STATUS_REVERSED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanPost(s),
                    "post 对非 DRAFT 应非法: " + s);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                    "Bean 报告 common 层非法迁移码: status=" + s);
            assertEquals("post", ex.getParam(ErpFinReconciliationDocumentStateMachine.ARG_ACTION),
                    "拒绝元数据携带动作名: status=" + s);
            assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                    "拒绝元数据携带当前态: status=" + s);
        }
    }

    // ---------- (c) reverse：POSTED 合法、DRAFT/REVERSED 非法 ----------

    @Test
    public void testAssertCanReverseLegalAndIllegal() {
        // POSTED 合法（红冲侧唯一迁移边来源态）
        sm.assertCanReverse(ErpFinConstants.RECON_STATUS_POSTED); // 不抛
        assertEquals(ErpFinConstants.RECON_STATUS_REVERSED, sm.reverseTargetStatus());

        // DRAFT / REVERSED 非法（抛 common 码 + action/currentStatus 元数据）
        for (String s : Arrays.asList(ErpFinConstants.RECON_STATUS_DRAFT, ErpFinConstants.RECON_STATUS_REVERSED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanReverse(s),
                    "reverse 对非 POSTED 应非法: " + s);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                    "Bean 报告 common 层非法迁移码: status=" + s);
            assertEquals("reverse", ex.getParam(ErpFinReconciliationDocumentStateMachine.ARG_ACTION),
                    "拒绝元数据携带动作名: status=" + s);
            assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                    "拒绝元数据携带当前态: status=" + s);
        }
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpFinReconciliationDocumentStateMachine.TransitionDefinition e : sm.transitions()) {
            // 每条边的 fromStatus 对该 action 合法（assert 放行不抛）
            if ("post".equals(e.getAction())) {
                sm.assertCanPost(e.getFromStatus());
                assertEquals(e.getToStatus(), sm.postTargetStatus(), "toStatus 与目标态方法不一致: action=" + e.getAction());
            } else if ("reverse".equals(e.getAction())) {
                sm.assertCanReverse(e.getFromStatus());
                assertEquals(e.getToStatus(), sm.reverseTargetStatus(), "toStatus 与目标态方法不一致: action=" + e.getAction());
            } else {
                throw new IllegalStateException("未知 action: " + e.getAction());
            }
        }
    }

    // ---------- (e) 初始={DRAFT}/终态={REVERSED}（POSTED 中间态） ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpFinConstants.RECON_STATUS_DRAFT), sm.initialStatuses(),
                "初始态集合 = {DRAFT}");
        assertEquals(Arrays.asList(ErpFinConstants.RECON_STATUS_REVERSED), sm.terminalStatuses(),
                "终态集合 = {REVERSED}");

        assertTrue(sm.isTerminal(ErpFinConstants.RECON_STATUS_REVERSED), "REVERSED → isTerminal=true");
        assertFalse(sm.isTerminal(ErpFinConstants.RECON_STATUS_DRAFT), "DRAFT 初始态非终态");
        assertFalse(sm.isTerminal(ErpFinConstants.RECON_STATUS_POSTED), "POSTED 中间态非终态（经 reverse 有出边）");
    }

    // ---------- (f) 可达性：从 DRAFT 可达 POSTED 与 REVERSED，且无多余可达态 ----------

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpFinConstants.RECON_STATUS_DRAFT);
        assertTrue(reachable.contains(ErpFinConstants.RECON_STATUS_POSTED), "从 DRAFT 应可达 POSTED");
        assertTrue(reachable.contains(ErpFinConstants.RECON_STATUS_REVERSED), "从 DRAFT 应可达 REVERSED");
        assertEquals(2, reachable.size(), "可达态集合 = {POSTED, REVERSED}（dict 3 值全活跃，无死状态）");
    }

    // ---------- 终态 REVERSED 无出边 ----------

    @Test
    public void testTerminalStatusesHaveNoOutgoingEdges() {
        for (String terminal : sm.terminalStatuses()) {
            for (ErpFinReconciliationDocumentStateMachine.TransitionDefinition e : sm.transitions()) {
                assertFalse(e.getFromStatus().equals(terminal),
                        "终态不应有出边: terminal=" + terminal + ", but edge " + e.getAction() + " leaves it");
            }
        }
    }

    // ---------- helpers ----------

    private Set<String> reachableFrom(String start) {
        Set<String> visited = new LinkedHashSet<>();
        List<String> frontier = new java.util.ArrayList<>();
        frontier.add(start);
        while (!frontier.isEmpty()) {
            String cur = frontier.remove(0);
            if (!visited.add(cur)) {
                continue;
            }
            for (ErpFinReconciliationDocumentStateMachine.TransitionDefinition e : sm.transitions()) {
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
