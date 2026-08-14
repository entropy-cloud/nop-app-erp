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
 * <p>针对 {@link ErpAstMaintenanceStateMachine}（ErpAstMaintenance.status 生命周期轴，6 命名动作——cancel 双源）的
 * 纯矩阵完备性遍历：不经 BizModel 入口（层 3 职责），不断言副作用/审计。
 *
 * <p>覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边（7 条边，6 命名动作——cancel 双源；create 创建种子不登记边 §9.2）；</li>
 *   <li>(b) 从 DRAFT 可达 SUBMITTED/IN_PROGRESS/COMPLETED/POSTED/CANCELLED，POSTED 经 reverse 回卷 COMPLETED；</li>
 *   <li>(c) 各动作合法来源态通过、非法来源态抛 common 层码（携带 action/currentStatus）；
 *       create null 归一化 DRAFT 合法；decideTreatment/approve 不迁移（动态守卫保留在 Processor，本 Bean 无对应动作）；</li>
 *   <li>(d) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(e) 终态/初始态集合正确——terminal={POSTED, CANCELLED}；reverse 回卷边（POSTED→COMPLETED）
 *       显式断言非终态；cancel 边显式断言终态分类正确。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpAstMaintenanceStateMachineMatrix {

    /** dict erp-ast/maintenance-status 全 6 值。 */
    private static final List<String> ALL_MAINTENANCE_STATUSES = Arrays.asList(
            ErpAstConstants.MAINTENANCE_STATUS_DRAFT,
            ErpAstConstants.MAINTENANCE_STATUS_SUBMITTED,
            ErpAstConstants.MAINTENANCE_STATUS_IN_PROGRESS,
            ErpAstConstants.MAINTENANCE_STATUS_COMPLETED,
            ErpAstConstants.MAINTENANCE_STATUS_POSTED,
            ErpAstConstants.MAINTENANCE_STATUS_CANCELLED);

    private final ErpAstMaintenanceStateMachine sm = new ErpAstMaintenanceStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void noDuplicateOrConflictingEdges() {
        List<ErpAstMaintenanceStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpAstMaintenanceStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(7, edges.size(), "迁移矩阵应有 7 条边（6 命名动作，cancel 双源；create 创建种子不登记边）");
    }

    // ---------- (b) 可达性 ----------

    @Test
    public void reachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpAstConstants.MAINTENANCE_STATUS_DRAFT);
        assertTrue(reachable.contains(ErpAstConstants.MAINTENANCE_STATUS_SUBMITTED), "从 DRAFT 应可达 SUBMITTED");
        assertTrue(reachable.contains(ErpAstConstants.MAINTENANCE_STATUS_IN_PROGRESS), "从 DRAFT 应可达 IN_PROGRESS");
        assertTrue(reachable.contains(ErpAstConstants.MAINTENANCE_STATUS_COMPLETED), "从 DRAFT 应可达 COMPLETED");
        assertTrue(reachable.contains(ErpAstConstants.MAINTENANCE_STATUS_POSTED), "从 DRAFT 应可达 POSTED");
        assertTrue(reachable.contains(ErpAstConstants.MAINTENANCE_STATUS_CANCELLED), "从 DRAFT 应可达 CANCELLED");
        // POSTED 经 reverse 回卷 COMPLETED（非终态回退，纠错路径）
        boolean rollsBack = sm.transitions().stream()
                .anyMatch(e -> ErpAstConstants.MAINTENANCE_STATUS_POSTED.equals(e.getFromStatus())
                        && ErpAstConstants.MAINTENANCE_STATUS_COMPLETED.equals(e.getToStatus()));
        assertTrue(rollsBack, "POSTED 经 reverse 应有回卷 COMPLETED 的边");
    }

    // ---------- (c) 各动作合法/非法来源态 ----------

    @Test
    public void createAllowsOnlyDraft() {
        assertAllowsOnly("create", ErpAstConstants.MAINTENANCE_STATUS_DRAFT);
        assertEquals(ErpAstConstants.MAINTENANCE_STATUS_DRAFT, sm.createTargetStatus(),
                "create 目标态=DRAFT（创建种子初始态）");
    }

    @Test
    public void createNullTreatedAsDraft() {
        sm.assertCanCreate(null); // null 归一化为 DRAFT（初始态），合法不抛
    }

    @Test
    public void submitAllowsOnlyDraft() {
        assertAllowsOnly("submit", ErpAstConstants.MAINTENANCE_STATUS_DRAFT);
        assertEquals(ErpAstConstants.MAINTENANCE_STATUS_SUBMITTED, sm.submitTargetStatus());
    }

    @Test
    public void startWorkAllowsOnlySubmitted() {
        assertAllowsOnly("startWork", ErpAstConstants.MAINTENANCE_STATUS_SUBMITTED);
        assertEquals(ErpAstConstants.MAINTENANCE_STATUS_IN_PROGRESS, sm.startWorkTargetStatus());
    }

    @Test
    public void completeWorkAllowsOnlyInProgress() {
        assertAllowsOnly("completeWork", ErpAstConstants.MAINTENANCE_STATUS_IN_PROGRESS);
        assertEquals(ErpAstConstants.MAINTENANCE_STATUS_COMPLETED, sm.completeWorkTargetStatus());
    }

    @Test
    public void postAllowsOnlyCompleted() {
        assertAllowsOnly("post", ErpAstConstants.MAINTENANCE_STATUS_COMPLETED);
        assertEquals(ErpAstConstants.MAINTENANCE_STATUS_POSTED, sm.postTargetStatus());
    }

    @Test
    public void cancelAllowsOnlyDraftOrSubmitted() {
        assertAllowsOnly("cancel", ErpAstConstants.MAINTENANCE_STATUS_DRAFT, ErpAstConstants.MAINTENANCE_STATUS_SUBMITTED);
        assertEquals(ErpAstConstants.MAINTENANCE_STATUS_CANCELLED, sm.cancelTargetStatus());
    }

    @Test
    public void reverseAllowsOnlyPosted() {
        assertAllowsOnly("reverse", ErpAstConstants.MAINTENANCE_STATUS_POSTED);
        assertEquals(ErpAstConstants.MAINTENANCE_STATUS_COMPLETED, sm.reverseTargetStatus(),
                "reverse 目标态=COMPLETED（回卷非终态）");
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void transitionsMetadataConsistentWithExplicitMethods() {
        for (ErpAstMaintenanceStateMachine.TransitionDefinition e : sm.transitions()) {
            invokeAssert(e.getAction(), e.getFromStatus());
            assertEquals(targetStatusFor(e.getAction()), e.getToStatus(),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (e) 终态/初始态集合 + reverse 回卷非终态 + cancel 终态 ----------

    @Test
    public void terminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpAstConstants.MAINTENANCE_STATUS_POSTED, ErpAstConstants.MAINTENANCE_STATUS_CANCELLED),
                sm.terminalStatuses(), "终态集合 = {POSTED, CANCELLED}");
        assertEquals(Arrays.asList(ErpAstConstants.MAINTENANCE_STATUS_DRAFT),
                sm.initialStatuses(), "初始态集合 = {DRAFT}");

        assertTrue(sm.isTerminal(ErpAstConstants.MAINTENANCE_STATUS_POSTED));
        assertTrue(sm.isTerminal(ErpAstConstants.MAINTENANCE_STATUS_CANCELLED));
        assertFalse(sm.isTerminal(ErpAstConstants.MAINTENANCE_STATUS_DRAFT));
        assertFalse(sm.isTerminal(ErpAstConstants.MAINTENANCE_STATUS_SUBMITTED));
        assertFalse(sm.isTerminal(ErpAstConstants.MAINTENANCE_STATUS_IN_PROGRESS));
        assertFalse(sm.isTerminal(ErpAstConstants.MAINTENANCE_STATUS_COMPLETED));
    }

    @Test
    public void reverseTargetIsNonTerminal() {
        // reverse 回卷边目标态 COMPLETED 显式断言非终态（回卷后允许修订重新 post）
        assertFalse(sm.isTerminal(sm.reverseTargetStatus()),
                "reverse 目标态 COMPLETED 必须为非终态");
        // 且 COMPLETED 有出边（post），非死状态
        boolean hasOutgoing = sm.transitions().stream()
                .anyMatch(e -> ErpAstConstants.MAINTENANCE_STATUS_COMPLETED.equals(e.getFromStatus()));
        assertTrue(hasOutgoing, "COMPLETED 应有出边（post）");
    }

    @Test
    public void cancelTargetIsTerminal() {
        // cancel 边目标态 CANCELLED 显式断言终态（作废不可再走）
        assertTrue(sm.isTerminal(sm.cancelTargetStatus()), "cancel 目标态 CANCELLED 必须为终态");
        boolean hasOutgoing = sm.transitions().stream()
                .anyMatch(e -> ErpAstConstants.MAINTENANCE_STATUS_CANCELLED.equals(e.getFromStatus()));
        assertFalse(hasOutgoing, "CANCELLED 不应有出边（终态）");
    }

    @Test
    public void allDictStatusesClassified() {
        // 机器化核对：dict 全 6 值在 Bean 中均有语义归类
        for (String s : ALL_MAINTENANCE_STATUSES) {
            sm.isTerminal(s); // 不抛即可
        }
    }

    // ==================== helpers ====================

    private void assertAllowsOnly(String action, String allowedFrom) {
        assertAllowsOnly(action, allowedFrom, null);
    }

    private void assertAllowsOnly(String action, String allowedFrom, String allowedFrom2) {
        for (String s : ALL_MAINTENANCE_STATUSES) {
            if (allowedFrom.equals(s) || (allowedFrom2 != null && allowedFrom2.equals(s))) {
                invokeAssert(action, s); // 不抛 = 合法
            } else {
                NopException ex = assertThrows(NopException.class, () -> invokeAssert(action, s),
                        action + " 对非允许来源态应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                        "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + s);
                assertEquals(action, ex.getParam(ErpAstMaintenanceStateMachine.ARG_ACTION),
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
            case "submit":
                sm.assertCanSubmit(status);
                break;
            case "startWork":
                sm.assertCanStartWork(status);
                break;
            case "completeWork":
                sm.assertCanCompleteWork(status);
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
            case "submit":
                return sm.submitTargetStatus();
            case "startWork":
                return sm.startWorkTargetStatus();
            case "completeWork":
                return sm.completeWorkTargetStatus();
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
            for (ErpAstMaintenanceStateMachine.TransitionDefinition e : sm.transitions()) {
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
