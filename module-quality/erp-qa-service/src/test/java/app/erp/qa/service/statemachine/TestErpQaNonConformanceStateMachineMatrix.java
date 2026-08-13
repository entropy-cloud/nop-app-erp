package app.erp.qa.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.qa.service.ErpQaConstants;
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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-14-0930-2 Phase 2 Proof）。
 *
 * <p>针对 {@link ErpQaNonConformanceStateMachine} Bean（NCR status 单轴 5 态）的纯矩阵完备性遍历：不经 BizModel
 * 入口（层 3 职责），不断言副作用/审计。覆盖：
 * <ul>
 *   <li>(a) 无完全重复边（action|from|to 三元唯一，cancel 多来源态 + postNcr/reverseNcr 自环）；</li>
 *   <li>(b) 从 OPEN 可达 IN_REVIEW/RESOLVED/ESCALATED_TO_RECALL/CANCELLED；</li>
 *   <li>(c) 各 {@code assertCanXxx} 合法来源态通过、非法来源态抛 common 码携带 {@code action}/{@code fromStatus}；</li>
 *   <li>(d) {@code transitions()} 与显式方法语义一致；</li>
 *   <li>(e) 初始/终态集合正确（ESCALATED_TO_RECALL/CANCELLED 无出边；RESOLVED 为带过账操作终态，有 postNcr/reverseNcr 自环）。</li>
 * </ul>
 *
 * <p>层 2 四方对照：dict {@code erp-qa/ncr-status}（OPEN/IN_REVIEW/RESOLVED/ESCALATED_TO_RECALL/CANCELLED）
 * ↔ {@code docs/design/quality/state-machine.md} §适用对象二 ↔ Bean 元数据 ↔ 全部 writer
 * （BizModel submitReview/escalateToRecall/cancel + Processor resolve/postNcr/reverseNcr/upgradeToRecall live 委托 Bean
 * + CRUD 路径 §9.4 选项 c 排除）。
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpQaNonConformanceStateMachineMatrix {

    private static final List<String> ALL_NCR_STATUSES = Arrays.asList(
            ErpQaConstants.NCR_STATUS_OPEN,
            ErpQaConstants.NCR_STATUS_IN_REVIEW,
            ErpQaConstants.NCR_STATUS_RESOLVED,
            ErpQaConstants.NCR_STATUS_ESCALATED_TO_RECALL,
            ErpQaConstants.NCR_STATUS_CANCELLED);

    private final ErpQaNonConformanceStateMachine sm = new ErpQaNonConformanceStateMachine();

    // ---------- (a) 无完全重复边 ----------

    @Test
    public void testNoDuplicateEdges() {
        List<ErpQaNonConformanceStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpQaNonConformanceStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus() + "|" + e.getToStatus();
            assertTrue(seen.add(key), "重复边: " + key);
        }
        // submitReview + resolve + upgradeToRecall + cancel×2 + postNcr自环 + reverseNcr自环 = 7 边
        assertEquals(7, edges.size(), "迁移矩阵应有 7 条边");
    }

    // ---------- (b) 从 OPEN 可达全部声明状态 ----------

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpQaConstants.NCR_STATUS_OPEN);
        assertTrue(reachable.contains(ErpQaConstants.NCR_STATUS_IN_REVIEW), "从 OPEN 应可达 IN_REVIEW");
        assertTrue(reachable.contains(ErpQaConstants.NCR_STATUS_RESOLVED), "从 OPEN 应可达 RESOLVED");
        assertTrue(reachable.contains(ErpQaConstants.NCR_STATUS_ESCALATED_TO_RECALL), "从 OPEN 应可达 ESCALATED_TO_RECALL");
        assertTrue(reachable.contains(ErpQaConstants.NCR_STATUS_CANCELLED), "从 OPEN 应可达 CANCELLED");
    }

    // ---------- (c) assertCanXxx 合法/非法 ----------

    @Test
    public void testAssertCanSubmitReviewLegalAndIllegal() {
        sm.assertCanSubmitReview(ErpQaConstants.NCR_STATUS_OPEN);
        assertEquals(ErpQaConstants.NCR_STATUS_IN_REVIEW, sm.submitReviewTargetStatus());
        for (String illegal : Arrays.asList(ErpQaConstants.NCR_STATUS_IN_REVIEW,
                ErpQaConstants.NCR_STATUS_RESOLVED, ErpQaConstants.NCR_STATUS_ESCALATED_TO_RECALL,
                ErpQaConstants.NCR_STATUS_CANCELLED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanSubmitReview(illegal));
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
            assertEquals("submitReview", ex.getParam(ErpQaNonConformanceStateMachine.ARG_ACTION));
            assertEquals(illegal, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
        }
    }

    @Test
    public void testAssertCanResolveLegalAndIllegal() {
        sm.assertCanResolve(ErpQaConstants.NCR_STATUS_IN_REVIEW);
        assertEquals(ErpQaConstants.NCR_STATUS_RESOLVED, sm.resolveTargetStatus());
        for (String illegal : Arrays.asList(ErpQaConstants.NCR_STATUS_OPEN,
                ErpQaConstants.NCR_STATUS_RESOLVED, ErpQaConstants.NCR_STATUS_ESCALATED_TO_RECALL,
                ErpQaConstants.NCR_STATUS_CANCELLED)) {
            assertThrows(NopException.class, () -> sm.assertCanResolve(illegal));
        }
    }

    @Test
    public void testAssertCanUpgradeToRecallLegalAndIllegal() {
        sm.assertCanUpgradeToRecall(ErpQaConstants.NCR_STATUS_IN_REVIEW);
        assertEquals(ErpQaConstants.NCR_STATUS_ESCALATED_TO_RECALL, sm.upgradeToRecallTargetStatus());
        for (String illegal : Arrays.asList(ErpQaConstants.NCR_STATUS_OPEN,
                ErpQaConstants.NCR_STATUS_RESOLVED, ErpQaConstants.NCR_STATUS_ESCALATED_TO_RECALL,
                ErpQaConstants.NCR_STATUS_CANCELLED)) {
            assertThrows(NopException.class, () -> sm.assertCanUpgradeToRecall(illegal));
        }
    }

    @Test
    public void testAssertCanCancelLegalAndIllegal() {
        sm.assertCanCancel(ErpQaConstants.NCR_STATUS_OPEN);
        sm.assertCanCancel(ErpQaConstants.NCR_STATUS_IN_REVIEW);
        assertEquals(ErpQaConstants.NCR_STATUS_CANCELLED, sm.cancelTargetStatus());
        for (String illegal : Arrays.asList(ErpQaConstants.NCR_STATUS_RESOLVED,
                ErpQaConstants.NCR_STATUS_ESCALATED_TO_RECALL, ErpQaConstants.NCR_STATUS_CANCELLED)) {
            assertThrows(NopException.class, () -> sm.assertCanCancel(illegal));
        }
    }

    @Test
    public void testAssertCanPostNcrLegalAndIllegal() {
        sm.assertCanPostNcr(ErpQaConstants.NCR_STATUS_RESOLVED);
        for (String illegal : Arrays.asList(ErpQaConstants.NCR_STATUS_OPEN,
                ErpQaConstants.NCR_STATUS_IN_REVIEW, ErpQaConstants.NCR_STATUS_ESCALATED_TO_RECALL,
                ErpQaConstants.NCR_STATUS_CANCELLED)) {
            assertThrows(NopException.class, () -> sm.assertCanPostNcr(illegal));
        }
    }

    @Test
    public void testAssertCanReverseNcrLegalAndIllegal() {
        sm.assertCanReverseNcr(ErpQaConstants.NCR_STATUS_RESOLVED);
        for (String illegal : Arrays.asList(ErpQaConstants.NCR_STATUS_OPEN,
                ErpQaConstants.NCR_STATUS_IN_REVIEW, ErpQaConstants.NCR_STATUS_ESCALATED_TO_RECALL,
                ErpQaConstants.NCR_STATUS_CANCELLED)) {
            assertThrows(NopException.class, () -> sm.assertCanReverseNcr(illegal));
        }
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpQaNonConformanceStateMachine.TransitionDefinition e : sm.transitions()) {
            invokeAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (e) 初始/终态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpQaConstants.NCR_STATUS_OPEN), sm.initialStatuses(),
                "初始态集合 = {OPEN}");
        assertEquals(Arrays.asList(ErpQaConstants.NCR_STATUS_RESOLVED,
                ErpQaConstants.NCR_STATUS_ESCALATED_TO_RECALL,
                ErpQaConstants.NCR_STATUS_CANCELLED), sm.terminalStatuses(),
                "终态集合 = {RESOLVED, ESCALATED_TO_RECALL, CANCELLED}");

        assertTrue(sm.isTerminal(ErpQaConstants.NCR_STATUS_RESOLVED));
        assertTrue(sm.isTerminal(ErpQaConstants.NCR_STATUS_ESCALATED_TO_RECALL));
        assertTrue(sm.isTerminal(ErpQaConstants.NCR_STATUS_CANCELLED));
        assertFalse(sm.isTerminal(ErpQaConstants.NCR_STATUS_OPEN));
        assertFalse(sm.isTerminal(ErpQaConstants.NCR_STATUS_IN_REVIEW));
    }

    /** ESCALATED_TO_RECALL/CANCELLED 无出边（真正终态）。RESOLVED 有 postNcr/reverseNcr 自环（带过账操作终态）。 */
    @Test
    public void testResolvedIsPostingOperationTerminal() {
        boolean resolvedHasSelfLoop = false;
        for (ErpQaNonConformanceStateMachine.TransitionDefinition e : sm.transitions()) {
            if (e.getFromStatus().equals(ErpQaConstants.NCR_STATUS_RESOLVED)) {
                assertEquals(ErpQaConstants.NCR_STATUS_RESOLVED, e.getToStatus(), "RESOLVED 出边应为自环（posted 操作不改 status）");
                assertTrue(e.getAction().equals("postNcr") || e.getAction().equals("reverseNcr"),
                        "RESOLVED 自环动作应为 postNcr/reverseNcr: " + e.getAction());
                resolvedHasSelfLoop = true;
            }
        }
        assertTrue(resolvedHasSelfLoop, "RESOLVED 应有 postNcr/reverseNcr 自环（带过账操作终态）");
    }

    @Test
    public void testEscalatedAndCancelledHaveNoOutgoing() {
        for (String terminal : Arrays.asList(ErpQaConstants.NCR_STATUS_ESCALATED_TO_RECALL,
                ErpQaConstants.NCR_STATUS_CANCELLED)) {
            for (ErpQaNonConformanceStateMachine.TransitionDefinition e : sm.transitions()) {
                assertFalse(e.getFromStatus().equals(terminal),
                        "终态 " + terminal + " 不应有出边: " + e.getAction());
            }
        }
    }

    // ---------- helpers ----------

    private void invokeAssert(String action, String status) {
        switch (action) {
            case "submitReview":
                sm.assertCanSubmitReview(status);
                break;
            case "resolve":
                sm.assertCanResolve(status);
                break;
            case "upgradeToRecall":
                sm.assertCanUpgradeToRecall(status);
                break;
            case "cancel":
                sm.assertCanCancel(status);
                break;
            case "postNcr":
                sm.assertCanPostNcr(status);
                break;
            case "reverseNcr":
                sm.assertCanReverseNcr(status);
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "submitReview":
                return sm.submitReviewTargetStatus();
            case "resolve":
                return sm.resolveTargetStatus();
            case "upgradeToRecall":
                return sm.upgradeToRecallTargetStatus();
            case "cancel":
                return sm.cancelTargetStatus();
            case "postNcr":
            case "reverseNcr":
                return ErpQaConstants.NCR_STATUS_RESOLVED; // 自环不改 status
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
            for (ErpQaNonConformanceStateMachine.TransitionDefinition e : sm.transitions()) {
                if (e.getFromStatus().equals(cur) && !visited.contains(e.getToStatus())) {
                    frontier.add(e.getToStatus());
                }
            }
        }
        visited.remove(start);
        return visited;
    }
}
