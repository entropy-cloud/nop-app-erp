package app.erp.ast.service.statemachine;

import app.erp.ast.service.ErpAstConstants;
import app.erp.common.service.ErpCommonErrors;
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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan Phase 2 Proof）。
 *
 * <p>针对 {@link ErpAstInventoryStateMachine}（ErpAstInventory.status 生命周期轴，6 命名动作——cancel 双源）的
 * 纯矩阵完备性遍历：不经 BizModel 入口（层 3 职责），不断言副作用/审计。
 *
 * <p>覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边（6 条边，5 命名动作——cancel 双源；create 创建种子不登记边 §9.2）；</li>
 *   <li>(b) 从 DRAFT 可达 COUNTING/RECONCILING/POSTED/CANCELLED，POSTED 经 reverse 回卷 RECONCILING；</li>
 *   <li>(c) 各动作合法来源态通过、非法来源态抛 common 层码（携带 action/currentStatus）；
 *       create null 归一化 DRAFT 合法；approve/processVariance 不迁移（动态守卫保留在 Processor，本 Bean 无对应动作）；</li>
 *   <li>(d) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(e) 终态/初始态集合正确——terminal={POSTED, CANCELLED}；reverse 回卷边（POSTED→RECONCILING）
 *       显式断言非终态；cancel 边显式断言终态分类正确。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpAstInventoryStateMachineMatrix {

    /** dict erp-ast/inventory-status 全 5 值。 */
    private static final List<String> ALL_INVENTORY_STATUSES = Arrays.asList(
            ErpAstConstants.INVENTORY_STATUS_DRAFT,
            ErpAstConstants.INVENTORY_STATUS_COUNTING,
            ErpAstConstants.INVENTORY_STATUS_RECONCILING,
            ErpAstConstants.INVENTORY_STATUS_POSTED,
            ErpAstConstants.INVENTORY_STATUS_CANCELLED);

    private final ErpAstInventoryStateMachine sm = new ErpAstInventoryStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void noDuplicateOrConflictingEdges() {
        List<ErpAstInventoryStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpAstInventoryStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(6, edges.size(), "迁移矩阵应有 6 条边（5 命名动作，cancel 双源；create 创建种子不登记边）");
    }

    // ---------- (b) 可达性 ----------

    @Test
    public void reachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpAstConstants.INVENTORY_STATUS_DRAFT);
        assertTrue(reachable.contains(ErpAstConstants.INVENTORY_STATUS_COUNTING), "从 DRAFT 应可达 COUNTING");
        assertTrue(reachable.contains(ErpAstConstants.INVENTORY_STATUS_RECONCILING), "从 DRAFT 应可达 RECONCILING");
        assertTrue(reachable.contains(ErpAstConstants.INVENTORY_STATUS_POSTED), "从 DRAFT 应可达 POSTED");
        assertTrue(reachable.contains(ErpAstConstants.INVENTORY_STATUS_CANCELLED), "从 DRAFT 应可达 CANCELLED");
        // POSTED 经 reverse 回卷 RECONCILING（非终态回退，纠错路径）
        boolean rollsBack = sm.transitions().stream()
                .anyMatch(e -> ErpAstConstants.INVENTORY_STATUS_POSTED.equals(e.getFromStatus())
                        && ErpAstConstants.INVENTORY_STATUS_RECONCILING.equals(e.getToStatus()));
        assertTrue(rollsBack, "POSTED 经 reverse 应有回卷 RECONCILING 的边");
    }

    // ---------- (c) 各动作合法/非法来源态 ----------

    @Test
    public void createAllowsOnlyDraft() {
        assertAllowsOnly("create", ErpAstConstants.INVENTORY_STATUS_DRAFT);
        assertEquals(ErpAstConstants.INVENTORY_STATUS_DRAFT, sm.createTargetStatus(),
                "create 目标态=DRAFT（创建种子初始态）");
    }

    @Test
    public void createNullTreatedAsDraft() {
        sm.assertCanCreate(null); // null 归一化为 DRAFT（初始态），合法不抛
    }

    @Test
    public void submitForCountAllowsOnlyDraft() {
        assertAllowsOnly("submitForCount", ErpAstConstants.INVENTORY_STATUS_DRAFT);
        assertEquals(ErpAstConstants.INVENTORY_STATUS_COUNTING, sm.submitForCountTargetStatus());
    }

    @Test
    public void reconcileAllowsOnlyCounting() {
        assertAllowsOnly("reconcile", ErpAstConstants.INVENTORY_STATUS_COUNTING);
        assertEquals(ErpAstConstants.INVENTORY_STATUS_RECONCILING, sm.reconcileTargetStatus());
    }

    @Test
    public void postAllowsOnlyReconciling() {
        assertAllowsOnly("post", ErpAstConstants.INVENTORY_STATUS_RECONCILING);
        assertEquals(ErpAstConstants.INVENTORY_STATUS_POSTED, sm.postTargetStatus());
    }

    @Test
    public void cancelAllowsOnlyDraftOrCounting() {
        assertAllowsOnly("cancel", ErpAstConstants.INVENTORY_STATUS_DRAFT, ErpAstConstants.INVENTORY_STATUS_COUNTING);
        assertEquals(ErpAstConstants.INVENTORY_STATUS_CANCELLED, sm.cancelTargetStatus());
    }

    @Test
    public void reverseAllowsOnlyPosted() {
        assertAllowsOnly("reverse", ErpAstConstants.INVENTORY_STATUS_POSTED);
        assertEquals(ErpAstConstants.INVENTORY_STATUS_RECONCILING, sm.reverseTargetStatus(),
                "reverse 目标态=RECONCILING（回卷非终态）");
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void transitionsMetadataConsistentWithExplicitMethods() {
        for (ErpAstInventoryStateMachine.TransitionDefinition e : sm.transitions()) {
            invokeAssert(e.getAction(), e.getFromStatus());
            assertEquals(targetStatusFor(e.getAction()), e.getToStatus(),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (e) 终态/初始态集合 + reverse 回卷非终态 + cancel 终态 ----------

    @Test
    public void terminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpAstConstants.INVENTORY_STATUS_POSTED, ErpAstConstants.INVENTORY_STATUS_CANCELLED),
                sm.terminalStatuses(), "终态集合 = {POSTED, CANCELLED}");
        assertEquals(Arrays.asList(ErpAstConstants.INVENTORY_STATUS_DRAFT),
                sm.initialStatuses(), "初始态集合 = {DRAFT}");

        assertTrue(sm.isTerminal(ErpAstConstants.INVENTORY_STATUS_POSTED));
        assertTrue(sm.isTerminal(ErpAstConstants.INVENTORY_STATUS_CANCELLED));
        assertFalse(sm.isTerminal(ErpAstConstants.INVENTORY_STATUS_DRAFT));
        assertFalse(sm.isTerminal(ErpAstConstants.INVENTORY_STATUS_COUNTING));
        assertFalse(sm.isTerminal(ErpAstConstants.INVENTORY_STATUS_RECONCILING));
    }

    @Test
    public void reverseTargetIsNonTerminal() {
        // reverse 回卷边目标态 RECONCILING 显式断言非终态（回卷后允许修订重新 post）
        assertFalse(sm.isTerminal(sm.reverseTargetStatus()),
                "reverse 目标态 RECONCILING 必须为非终态");
        // 且 RECONCILING 有出边（post），非死状态
        boolean hasOutgoing = sm.transitions().stream()
                .anyMatch(e -> ErpAstConstants.INVENTORY_STATUS_RECONCILING.equals(e.getFromStatus()));
        assertTrue(hasOutgoing, "RECONCILING 应有出边（post）");
    }

    @Test
    public void cancelTargetIsTerminal() {
        // cancel 边目标态 CANCELLED 显式断言终态（作废不可再走）
        assertTrue(sm.isTerminal(sm.cancelTargetStatus()), "cancel 目标态 CANCELLED 必须为终态");
        boolean hasOutgoing = sm.transitions().stream()
                .anyMatch(e -> ErpAstConstants.INVENTORY_STATUS_CANCELLED.equals(e.getFromStatus()));
        assertFalse(hasOutgoing, "CANCELLED 不应有出边（终态）");
    }

    @Test
    public void allDictStatusesClassified() {
        // 机器化核对：dict 全 5 值在 Bean 中均有语义归类
        for (String s : ALL_INVENTORY_STATUSES) {
            sm.isTerminal(s); // 不抛即可
        }
    }

    // ==================== helpers ====================

    private void assertAllowsOnly(String action, String allowedFrom) {
        assertAllowsOnly(action, allowedFrom, null);
    }

    private void assertAllowsOnly(String action, String allowedFrom, String allowedFrom2) {
        for (String s : ALL_INVENTORY_STATUSES) {
            if (allowedFrom.equals(s) || (allowedFrom2 != null && allowedFrom2.equals(s))) {
                invokeAssert(action, s); // 不抛 = 合法
            } else {
                NopException ex = assertThrows(NopException.class, () -> invokeAssert(action, s),
                        action + " 对非允许来源态应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                        "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + s);
                assertEquals(action, ex.getParam(ErpAstInventoryStateMachine.ARG_ACTION),
                        "拒绝元数据携带动作名: action=" + action);
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                        "拒绝元数据携带当前态: action=" + action + ", status=" + s);
            }
        }
    }

    private void invokeAssert(String action, String status) {
        switch (action) {
            case "create":
                sm.assertCanCreate(status);
                break;
            case "submitForCount":
                sm.assertCanSubmitForCount(status);
                break;
            case "reconcile":
                sm.assertCanReconcile(status);
                break;
            case "post":
                sm.assertCanPost(status);
                break;
            case "cancel":
                sm.assertCanCancel(status);
                break;
            case "reverse":
                sm.assertCanReverse(status);
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "create":
                return sm.createTargetStatus();
            case "submitForCount":
                return sm.submitForCountTargetStatus();
            case "reconcile":
                return sm.reconcileTargetStatus();
            case "post":
                return sm.postTargetStatus();
            case "cancel":
                return sm.cancelTargetStatus();
            case "reverse":
                return sm.reverseTargetStatus();
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private Set<String> reachableFrom(String start) {
        Set<String> visited = new LinkedHashSet<>();
        List<String> frontier = new java.util.ArrayList<>();
        frontier.add(start);
        while (!frontier.isEmpty()) {
            String cur = frontier.remove(0);
            if (!visited.add(cur)) {
                continue;
            }
            for (ErpAstInventoryStateMachine.TransitionDefinition e : sm.transitions()) {
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
