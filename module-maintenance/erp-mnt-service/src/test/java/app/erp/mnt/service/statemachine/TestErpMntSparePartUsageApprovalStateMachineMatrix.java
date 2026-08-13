package app.erp.mnt.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.mnt.dao.ErpMntDaoConstants;
import io.nop.api.core.exceptions.NopException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan Phase 2 Proof）。
 *
 * <p>针对 {@link ErpMntSparePartUsageApprovalStateMachine} Bean（approveStatus 单轴）的纯矩阵完备性遍历：
 * 不经 BizModel 入口（层 3 职责），不断言副作用/审计。备件消耗单无独立 submit/approve/reject 审批 Processor——
 * confirm 动作一步到位 →APPROVED（非标准 5 动作审批生命周期，Decision (A)），故矩阵为单边最小矩阵。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边（1 边）；</li>
 *   <li>(b) 从 UNSUBMITTED 可达 APPROVED，终态（APPROVED/REJECTED）无出边；</li>
 *   <li>(c) confirmApprove 对 null/UNSUBMITTED 合法、对其余态非法；</li>
 *   <li>(d) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(e) 终态/初始态集合正确。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpMntSparePartUsageApprovalStateMachineMatrix {

    private static final List<String> ALL_APPROVE_STATUSES = Arrays.asList(
            ErpMntDaoConstants.APPROVE_STATUS_UNSUBMITTED,
            ErpMntDaoConstants.APPROVE_STATUS_SUBMITTED,
            ErpMntDaoConstants.APPROVE_STATUS_APPROVED,
            ErpMntDaoConstants.APPROVE_STATUS_REJECTED);

    private final ErpMntSparePartUsageApprovalStateMachine sm = new ErpMntSparePartUsageApprovalStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpMntSparePartUsageApprovalStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpMntSparePartUsageApprovalStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(1, edges.size(), "迁移矩阵应有 1 条边");
    }

    // ---------- (b) 从 UNSUBMITTED 可达 APPROVED；终态无出边 ----------

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpMntDaoConstants.APPROVE_STATUS_UNSUBMITTED);
        assertTrue(reachable.contains(ErpMntDaoConstants.APPROVE_STATUS_APPROVED),
                "从 UNSUBMITTED 应可达 APPROVED");
    }

    @Test
    public void testTerminalStatusesHaveNoOutgoingEdges() {
        for (String terminal : sm.terminalStatuses()) {
            for (ErpMntSparePartUsageApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
                assertFalse(e.getFromStatus().equals(terminal),
                        "终态不应有出边: terminal=" + terminal + ", but edge " + e.getAction() + " leaves it");
            }
        }
    }

    // ---------- (c) confirmApprove 对 null/UNSUBMITTED 合法、对其余态非法 ----------

    @Test
    public void testConfirmApproveAllowsNullAndUnsubmitted() {
        // null（新建实体 approveStatus 未设置）视同 UNSUBMITTED，合法
        assertDoesNotThrow(() -> sm.assertCanConfirmApprove(null));
        assertEquals(ErpMntDaoConstants.APPROVE_STATUS_APPROVED, sm.confirmApproveTargetStatus());

        // UNSUBMITTED 合法
        assertDoesNotThrow(() -> sm.assertCanConfirmApprove(ErpMntDaoConstants.APPROVE_STATUS_UNSUBMITTED));
    }

    @Test
    public void testConfirmApproveRejectsNonInitial() {
        for (String s : ALL_APPROVE_STATUSES) {
            if (ErpMntDaoConstants.APPROVE_STATUS_UNSUBMITTED.equals(s)) {
                continue; // 合法分支已在 testConfirmApproveAllowsNullAndUnsubmitted 覆盖
            }
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanConfirmApprove(s),
                    "confirmApprove 对非初始态应非法: " + s);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                    "Bean 报告 common 层非法迁移码: status=" + s);
            assertEquals("confirmApprove", ex.getParam(ErpMntSparePartUsageApprovalStateMachine.ARG_ACTION),
                    "拒绝元数据携带动作名");
            assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                    "拒绝元数据携带当前态: status=" + s);
        }
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpMntSparePartUsageApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
            sm.assertCanConfirmApprove(e.getFromStatus()); // 声明的边均合法
            assertEquals(e.getToStatus(), sm.confirmApproveTargetStatus(),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (e) 终态/初始态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpMntDaoConstants.APPROVE_STATUS_APPROVED,
                        ErpMntDaoConstants.APPROVE_STATUS_REJECTED),
                sm.terminalStatuses(), "终态集合 = {APPROVED, REJECTED}");
        assertEquals(java.util.Collections.singletonList(ErpMntDaoConstants.APPROVE_STATUS_UNSUBMITTED),
                sm.initialStatuses(), "初始态集合 = {UNSUBMITTED}");

        assertTrue(sm.isTerminal(ErpMntDaoConstants.APPROVE_STATUS_APPROVED));
        assertTrue(sm.isTerminal(ErpMntDaoConstants.APPROVE_STATUS_REJECTED));
        assertFalse(sm.isTerminal(ErpMntDaoConstants.APPROVE_STATUS_UNSUBMITTED));
        assertFalse(sm.isTerminal(ErpMntDaoConstants.APPROVE_STATUS_SUBMITTED));
    }

    @Test
    public void testTargetStatusMethod() {
        assertEquals(ErpMntDaoConstants.APPROVE_STATUS_APPROVED, sm.confirmApproveTargetStatus());
    }

    // ---------- helpers ----------

    private Set<String> reachableFrom(String start) {
        Set<String> visited = new LinkedHashSet<>();
        List<String> frontier = new ArrayList<>();
        frontier.add(start);
        while (!frontier.isEmpty()) {
            String cur = frontier.remove(0);
            if (!visited.add(cur)) {
                continue;
            }
            for (ErpMntSparePartUsageApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
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
