package app.erp.fin.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.fin.service.ErpFinConstants;
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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-13-1146-2 Phase 1 Proof）。
 *
 * <p>针对 {@link ErpFinExpenseClaimDocumentStateMachine} Bean（docStatus 业务生命周期轴）的纯矩阵完备性遍历：
 * 不经 BizModel 入口（层 3 职责），不断言副作用/审计/前置校验。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边（1 条命名边 cancel）；</li>
 *   <li>(b) cancel（DRAFT 等非 CANCELLED 合法、CANCELLED 非法）；</li>
 *   <li>(c) 终态 CANCELLED 无出边；</li>
 *   <li>(d) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(e) initial/terminal 集合正确；</li>
 *   <li><b>docStatus 残余值 SUBMITTED/APPROVED/REJECTED 不在 initial/terminal/transitions 任一集合</b>
 *       （intentional reserved，生命周期推进由 approveStatus 轴承载）；</li>
 *   <li>isTerminal/isCancelled 分类 helper。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpFinExpenseClaimDocumentStateMachineMatrix {

    /** dict 5 值全集（erp-fin/expense-claim-status）：DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED。 */
    private static final List<String> ALL_DICT_STATUSES = Arrays.asList(
            ErpFinConstants.DOC_STATUS_DRAFT,
            "SUBMITTED",
            "APPROVED",
            "REJECTED",
            ErpFinConstants.DOC_STATUS_CANCELLED);

    /** 残余值（代码零 writer，dict 有但 Bean 不纳入）：SUBMITTED/APPROVED/REJECTED。 */
    private static final List<String> RESIDUAL_STATUSES = Arrays.asList("SUBMITTED", "APPROVED", "REJECTED");

    private final ErpFinExpenseClaimDocumentStateMachine sm = new ErpFinExpenseClaimDocumentStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpFinExpenseClaimDocumentStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpFinExpenseClaimDocumentStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(1, edges.size(), "迁移矩阵应有 1 条边（cancel: DRAFT→CANCELLED 代表边）");
    }

    // ---------- (b) cancel：非 CANCELLED 合法、CANCELLED 非法 ----------

    @Test
    public void testAssertCanCancelLegalAndIllegal() {
        // 非 CANCELLED 合法（DRAFT 为实际唯一生产来源态；残余值亦放行——Bean 仅拒绝已作废，对齐 Processor loose 守卫）
        sm.assertCanCancel(ErpFinConstants.DOC_STATUS_DRAFT);
        sm.assertCanCancel(null);
        for (String residual : RESIDUAL_STATUSES) {
            sm.assertCanCancel(residual);
        }
        assertEquals(ErpFinConstants.DOC_STATUS_CANCELLED, sm.cancelTargetStatus());

        // CANCELLED 非法（抛 common 码 + action/fromStatus 元数据）
        NopException ex = assertThrows(NopException.class, () -> sm.assertCanCancel(ErpFinConstants.DOC_STATUS_CANCELLED),
                "cancel 对 CANCELLED 应非法");
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "Bean 报告 common 层非法迁移码");
        assertEquals("cancel", ex.getParam(ErpFinExpenseClaimDocumentStateMachine.ARG_ACTION),
                "拒绝元数据携带动作名");
        assertEquals(ErpFinConstants.DOC_STATUS_CANCELLED, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                "拒绝元数据携带当前态");
    }

    // ---------- (c) 终态 CANCELLED 无出边 ----------

    @Test
    public void testTerminalStatusesHaveNoOutgoingEdges() {
        for (String terminal : sm.terminalStatuses()) {
            for (ErpFinExpenseClaimDocumentStateMachine.TransitionDefinition e : sm.transitions()) {
                assertFalse(e.getFromStatus().equals(terminal),
                        "终态不应有出边: terminal=" + terminal + ", but edge " + e.getAction() + " leaves it");
            }
        }
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpFinExpenseClaimDocumentStateMachine.TransitionDefinition e : sm.transitions()) {
            // 每条边的 fromStatus 对该 action 合法（assert 放行不抛）
            sm.assertCanCancel(e.getFromStatus());
            // 每条边的 toStatus 与 cancelTargetStatus() 一致
            assertEquals(e.getToStatus(), sm.cancelTargetStatus(),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
            assertEquals("cancel", e.getAction(), "action 名一致");
        }
    }

    // ---------- (e) 终态/初始态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpFinConstants.DOC_STATUS_CANCELLED), sm.terminalStatuses(),
                "终态集合 = {CANCELLED}");
        assertEquals(Arrays.asList(ErpFinConstants.DOC_STATUS_DRAFT), sm.initialStatuses(),
                "初始态集合 = {DRAFT}");

        assertTrue(sm.isTerminal(ErpFinConstants.DOC_STATUS_CANCELLED));
        assertFalse(sm.isTerminal(ErpFinConstants.DOC_STATUS_DRAFT));
        for (String residual : RESIDUAL_STATUSES) {
            assertFalse(sm.isTerminal(residual), "残余值非终态: " + residual);
        }

        assertTrue(sm.isCancelled(ErpFinConstants.DOC_STATUS_CANCELLED));
        assertFalse(sm.isCancelled(ErpFinConstants.DOC_STATUS_DRAFT));
        for (String residual : RESIDUAL_STATUSES) {
            assertFalse(sm.isCancelled(residual), "残余值非 CANCELLED: " + residual);
        }
    }

    // ---------- 残余值排除（intentional reserved，Decision plan Phase 1/3）----------

    @Test
    public void testResidualStatusesNotInAnySet() {
        for (String residual : RESIDUAL_STATUSES) {
            assertFalse(sm.initialStatuses().contains(residual), "残余值不在 initialStatuses: " + residual);
            assertFalse(sm.terminalStatuses().contains(residual), "残余值不在 terminalStatuses: " + residual);
            for (ErpFinExpenseClaimDocumentStateMachine.TransitionDefinition e : sm.transitions()) {
                assertFalse(residual.equals(e.getFromStatus()), "残余值不应作为迁移边的 fromStatus: " + residual);
                assertFalse(residual.equals(e.getToStatus()), "残余值不应作为迁移边的 toStatus: " + residual);
            }
        }
    }

    // ---------- 可达性：从 DRAFT 可达 CANCELLED，且残余值不可达 ----------

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpFinConstants.DOC_STATUS_DRAFT);
        assertTrue(reachable.contains(ErpFinConstants.DOC_STATUS_CANCELLED), "从 DRAFT 应可达 CANCELLED");
        for (String residual : RESIDUAL_STATUSES) {
            assertFalse(reachable.contains(residual), "残余值不可达（无入边）: " + residual);
        }
    }

    // ---------- helpers ----------

    private Set<String> reachableFrom(String start) {
        Set<String> visited = new HashSet<>();
        List<String> frontier = new java.util.ArrayList<>();
        frontier.add(start);
        while (!frontier.isEmpty()) {
            String cur = frontier.remove(0);
            if (!visited.add(cur)) {
                continue;
            }
            for (ErpFinExpenseClaimDocumentStateMachine.TransitionDefinition e : sm.transitions()) {
                if (e.getFromStatus().equals(cur) && !visited.contains(e.getToStatus())) {
                    frontier.add(e.getToStatus());
                }
            }
        }
        visited.remove(start);
        return visited;
    }
}
