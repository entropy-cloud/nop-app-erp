package app.erp.md.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.md.service.ErpMdConstants;
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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-12-2142-1 Phase 1 Proof）。
 *
 * <p>针对 {@link ErpMdSupplierApprovalStateMachine} Bean 的纯矩阵完备性遍历：不经 BizModel 入口（层 3 职责），
 * 不断言副作用/审计。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边；</li>
 *   <li>(b) 从 APPLIED 可达 APPROVED/PROBATION/SUSPENDED/REJECTED 全部声明状态（REJECTED 经 apply 可回到 APPLIED，断言此环）；</li>
 *   <li>(c) 多来源态动作（approve {APPLIED, PROBATION}、suspend {APPLIED, APPROVED, PROBATION}、apply {null, REJECTED}）覆盖全集；</li>
 *   <li>(d) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(e) 终态/初始态集合正确（REJECTED 经 apply 可恢复 → 无严格终态 → 终态集 = 空）。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpMdSupplierApprovalStateMachineMatrix {

    /** dict {@code erp-md/supplier-approval-status} 的 5 值。 */
    private static final List<String> ALL_STATUSES = Arrays.asList(
            ErpMdConstants.APPROVAL_STATUS_APPLIED,
            ErpMdConstants.APPROVAL_STATUS_APPROVED,
            ErpMdConstants.APPROVAL_STATUS_PROBATION,
            ErpMdConstants.APPROVAL_STATUS_SUSPENDED,
            ErpMdConstants.APPROVAL_STATUS_REJECTED);

    private final ErpMdSupplierApprovalStateMachine sm = new ErpMdSupplierApprovalStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpMdSupplierApprovalStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpMdSupplierApprovalStateMachine.TransitionDefinition e : edges) {
            // apply 的 null/REJECTED 两源：同一 action 的不同 fromStatus 允许（apply 是多源）；同 action+fromStatus 才冲突
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(10, edges.size(), "迁移矩阵应有 10 条边（apply 多源 2 + approve 多源 2 + suspend 多源 3 + 单源 3）");
    }

    // ---------- (b) 从 APPLIED 可达性 + REJECTED 可恢复环 ----------

    @Test
    public void testReachabilityFromAppliedCoversAllDeclaredStatuses() {
        // 从 APPLIED 命名动作可达集应覆盖 APPROVED/PROBATION/SUSPENDED/REJECTED（全部非初始声明状态）
        Set<String> reachable = reachableFrom(ErpMdConstants.APPROVAL_STATUS_APPLIED);
        assertTrue(reachable.contains(ErpMdConstants.APPROVAL_STATUS_APPROVED), "APPLIED→APPROVED 经 approve 可达");
        assertTrue(reachable.contains(ErpMdConstants.APPROVAL_STATUS_PROBATION), "APPLIED→APPROVED→PROBATION 可达");
        assertTrue(reachable.contains(ErpMdConstants.APPROVAL_STATUS_SUSPENDED), "APPLIED→SUSPENDED 经 suspend 可达");
        assertTrue(reachable.contains(ErpMdConstants.APPROVAL_STATUS_REJECTED), "APPLIED→REJECTED 经 reject 可达");
    }

    @Test
    public void testRejectedIsRecoverableViaApplyEdge() {
        // 关键可恢复性事实：REJECTED 经 apply 可回到 APPLIED（已落地命名动作边）
        Set<String> reachable = reachableFrom(ErpMdConstants.APPROVAL_STATUS_REJECTED);
        assertTrue(reachable.contains(ErpMdConstants.APPROVAL_STATUS_APPLIED),
                "REJECTED→APPLIED 经 apply 可达（可恢复准终态，非严格终态）: " + reachable);
        // 进而 REJECTED 可达全部其余状态（经 apply→approve/probate/suspend/reject）
        assertTrue(reachable.contains(ErpMdConstants.APPROVAL_STATUS_APPROVED));
        assertTrue(reachable.contains(ErpMdConstants.APPROVAL_STATUS_PROBATION));
        assertTrue(reachable.contains(ErpMdConstants.APPROVAL_STATUS_SUSPENDED));
        // REJECTED→APPLIED（apply）与 APPLIED→REJECTED（reject）构成环，REJECTED 非严格终态
        // （reachableFrom 排除起点故不在此断言 REJECTED 自身，环的存在由两条边独立可证）
        boolean hasRejectedToApplied = sm.transitions().stream()
                .anyMatch(e -> "apply".equals(e.getAction())
                        && ErpMdConstants.APPROVAL_STATUS_REJECTED.equals(e.getFromStatus())
                        && ErpMdConstants.APPROVAL_STATUS_APPLIED.equals(e.getToStatus()));
        boolean hasAppliedToRejected = sm.transitions().stream()
                .anyMatch(e -> "reject".equals(e.getAction())
                        && ErpMdConstants.APPROVAL_STATUS_APPLIED.equals(e.getFromStatus())
                        && ErpMdConstants.APPROVAL_STATUS_REJECTED.equals(e.getToStatus()));
        assertTrue(hasRejectedToApplied && hasAppliedToRejected,
                "REJECTED↔APPLIED 经 apply/reject 构成可恢复环（REJECTED 非严格终态）");
    }

    @Test
    public void testNoStrictTerminalStatuses() {
        // 无严格终态：每个 dict 值都有命名动作出边（REJECTED 经 apply 可恢复，其余均有直接出边）
        for (String status : ALL_STATUSES) {
            assertFalse(sm.isTerminal(status), "无严格终态（REJECTED 可恢复）: " + status);
        }
        assertTrue(sm.terminalStatuses().isEmpty(), "终态集合 = 空（REJECTED 可恢复准终态，非严格终态）");
    }

    // ---------- (c) 多来源态动作全集覆盖 ----------

    @Test
    public void testApplyAcceptsNullAndRejectedAndRejectsOthers() {
        // apply 接受 null（新建）+ REJECTED（重新申请）
        sm.assertCanApply(null); // 放行
        sm.assertCanApply(ErpMdConstants.APPROVAL_STATUS_REJECTED); // 放行
        assertEquals(ErpMdConstants.APPROVAL_STATUS_APPLIED, sm.applyTargetStatus());

        // 其余态非法
        for (String s : ALL_STATUSES) {
            if (s.equals(ErpMdConstants.APPROVAL_STATUS_REJECTED)) {
                continue;
            }
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanApply(s),
                    "apply 对非 {null, REJECTED} 应非法: " + s);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                    "Bean 报告 common 层非法迁移码");
            assertEquals("apply", ex.getParam(ErpMdSupplierApprovalStateMachine.ARG_ACTION), "拒绝元数据携带动作名");
            assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS), "拒绝元数据携带当前态");
        }
    }

    @Test
    public void testApproveAcceptsAppliedAndProbationAndRejectsOthers() {
        sm.assertCanApprove(ErpMdConstants.APPROVAL_STATUS_APPLIED); // 放行
        sm.assertCanApprove(ErpMdConstants.APPROVAL_STATUS_PROBATION); // 放行
        assertEquals(ErpMdConstants.APPROVAL_STATUS_APPROVED, sm.approveTargetStatus());

        for (String s : ALL_STATUSES) {
            if (s.equals(ErpMdConstants.APPROVAL_STATUS_APPLIED)
                    || s.equals(ErpMdConstants.APPROVAL_STATUS_PROBATION)) {
                continue;
            }
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanApprove(s),
                    "approve 对非 {APPLIED, PROBATION} 应非法: " + s);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
            assertEquals("approve", ex.getParam(ErpMdSupplierApprovalStateMachine.ARG_ACTION));
        }
    }

    @Test
    public void testSuspendAcceptsAppliedApprovedProbationAndRejectsOthers() {
        sm.assertCanSuspend(ErpMdConstants.APPROVAL_STATUS_APPLIED); // 放行
        sm.assertCanSuspend(ErpMdConstants.APPROVAL_STATUS_APPROVED); // 放行
        sm.assertCanSuspend(ErpMdConstants.APPROVAL_STATUS_PROBATION); // 放行
        assertEquals(ErpMdConstants.APPROVAL_STATUS_SUSPENDED, sm.suspendTargetStatus());

        // SUSPENDED 本身非法（幂等短路留 BizModel/Processor，Bean 到达此处按非法边报告）
        for (String s : ALL_STATUSES) {
            if (s.equals(ErpMdConstants.APPROVAL_STATUS_APPLIED)
                    || s.equals(ErpMdConstants.APPROVAL_STATUS_APPROVED)
                    || s.equals(ErpMdConstants.APPROVAL_STATUS_PROBATION)) {
                continue;
            }
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanSuspend(s),
                    "suspend 对非 {APPLIED, APPROVED, PROBATION} 应非法: " + s);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
            assertEquals("suspend", ex.getParam(ErpMdSupplierApprovalStateMachine.ARG_ACTION));
        }
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpMdSupplierApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
            // 每条边的 fromStatus 对该 action 合法（assert 放行不抛；apply 的 null 源也验证）
            invokeAssert(e.getAction(), e.getFromStatus());
            // 每条边的 toStatus 与 <action>TargetStatus() 一致
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (e) 终态/初始态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Collections.emptyList(), sm.terminalStatuses(), "终态集合 = 空（REJECTED 可恢复）");
        assertEquals(Arrays.asList(ErpMdConstants.APPROVAL_STATUS_APPLIED), sm.initialStatuses(), "初始态集合 = {APPLIED}");

        for (String s : ALL_STATUSES) {
            assertFalse(sm.isTerminal(s), "无严格终态: " + s);
        }
    }

    // ---------- 合法/非法来源态显式断言（补充显式方法语义核对） ----------

    @Test
    public void testExplicitActionGuardsForSingleSourceActions() {
        // probate: 仅 APPROVED 合法
        assertActionAllowsOnly("probate", ErpMdConstants.APPROVAL_STATUS_APPROVED);
        // reinstate: 仅 SUSPENDED 合法
        assertActionAllowsOnly("reinstate", ErpMdConstants.APPROVAL_STATUS_SUSPENDED);
        // reject: 仅 APPLIED 合法
        assertActionAllowsOnly("reject", ErpMdConstants.APPROVAL_STATUS_APPLIED);
    }

    @Test
    public void testTargetStatusMethods() {
        assertEquals(ErpMdConstants.APPROVAL_STATUS_APPLIED, sm.applyTargetStatus());
        assertEquals(ErpMdConstants.APPROVAL_STATUS_APPROVED, sm.approveTargetStatus());
        assertEquals(ErpMdConstants.APPROVAL_STATUS_PROBATION, sm.probateTargetStatus());
        assertEquals(ErpMdConstants.APPROVAL_STATUS_SUSPENDED, sm.suspendTargetStatus());
        assertEquals(ErpMdConstants.APPROVAL_STATUS_APPROVED, sm.reinstateTargetStatus());
        assertEquals(ErpMdConstants.APPROVAL_STATUS_REJECTED, sm.rejectTargetStatus());
    }

    // ---------- helpers ----------

    /**
     * 断言某 action 仅允许指定来源态（在 5 个 dict 值上遍历）：该来源态放行，其余全部非法（抛 common 码 + action 元数据）。
     */
    private void assertActionAllowsOnly(String action, String allowedFrom) {
        for (String s : ALL_STATUSES) {
            if (allowedFrom.equals(s)) {
                invokeAssert(action, s); // 不抛 = 合法
            } else {
                NopException ex = assertThrows(NopException.class, () -> invokeAssert(action, s),
                        action + " 对非允许来源态应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                        "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + s);
                assertEquals(action, ex.getParam(ErpMdSupplierApprovalStateMachine.ARG_ACTION),
                        "拒绝元数据携带动作名: action=" + action);
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                        "拒绝元数据携带当前态: action=" + action + ", status=" + s);
            }
        }
    }

    private void invokeAssert(String action, String status) {
        switch (action) {
            case "apply":
                sm.assertCanApply(status);
                break;
            case "approve":
                sm.assertCanApprove(status);
                break;
            case "probate":
                sm.assertCanProbate(status);
                break;
            case "suspend":
                sm.assertCanSuspend(status);
                break;
            case "reinstate":
                sm.assertCanReinstate(status);
                break;
            case "reject":
                sm.assertCanReject(status);
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "apply":
                return sm.applyTargetStatus();
            case "approve":
                return sm.approveTargetStatus();
            case "probate":
                return sm.probateTargetStatus();
            case "suspend":
                return sm.suspendTargetStatus();
            case "reinstate":
                return sm.reinstateTargetStatus();
            case "reject":
                return sm.rejectTargetStatus();
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
            for (ErpMdSupplierApprovalStateMachine.TransitionDefinition e : sm.transitions()) {
                if (java.util.Objects.equals(e.getFromStatus(), cur) && !visited.contains(e.getToStatus())) {
                    frontier.add(e.getToStatus());
                }
            }
        }
        return visited.stream()
                .filter(s -> !java.util.Objects.equals(s, start))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
