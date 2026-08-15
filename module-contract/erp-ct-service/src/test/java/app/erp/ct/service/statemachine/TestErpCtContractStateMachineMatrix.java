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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-12-1118-1 Phase 1 Proof）。
 *
 * <p>针对 {@link ErpCtContractStateMachine} Bean 的纯矩阵完备性遍历：不经 BizModel 入口（层 3 职责），
 * 不断言副作用/审计。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边；</li>
 *   <li>(b) 从 DRAFT 命名动作可达性 = {NEGOTIATION, ACTIVE}（RC-R1.32 落地 submit(DRAFT→NEGOTIATION)
 *       + rejectAmend(DRAFT→ACTIVE) 两出边——§2 漂移 successor 已编码）；</li>
 *   <li>(c) terminate 多源 {ACTIVE, NEGOTIATION} 全覆盖、对其余态（含终态）非法；</li>
 *   <li>(d) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(e) 终态/初始态集合正确（终态 = {EXPIRED, TERMINATED}；CANCELLED 因 dict 缺值 + 零 writer 不纳入）。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpCtContractStateMachineMatrix {

    /** dict {@code erp-ct/contract-status} 的 6 值（CANCELLED 不在 dict，故不纳入全态集）。 */
    private static final List<String> ALL_STATUSES = Arrays.asList(
            ErpCtConstants.CONTRACT_STATUS_DRAFT,
            ErpCtConstants.CONTRACT_STATUS_NEGOTIATION,
            ErpCtConstants.CONTRACT_STATUS_ACTIVE,
            ErpCtConstants.CONTRACT_STATUS_SUSPENDED,
            ErpCtConstants.CONTRACT_STATUS_EXPIRED,
            ErpCtConstants.CONTRACT_STATUS_TERMINATED);

    private final ErpCtContractStateMachine sm = new ErpCtContractStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpCtContractStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpCtContractStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            // 同一 action + 同一 fromStatus 不得出现多次（否则冲突）
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(9, edges.size(), "迁移矩阵应有 9 条边（terminate 多源占 2 + RC-R1.32 新增 submit/rejectAmend）");
    }

    // ---------- (b) 从 DRAFT 命名动作可达性 = {NEGOTIATION, ACTIVE}（RC-R1.32 已落地出边） ----------

    @Test
    public void testReachabilityFromDraftCoversSubmitAndRejectAmend() {
        // RC-R1.32 修复：命名动作路径下从 DRAFT 有 submit(DRAFT→NEGOTIATION) + rejectAmend(DRAFT→ACTIVE)
        // 两出边（§2 漂移 successor 已编码）；CANCELLED 仍不可达（dict 缺值 + 零 writer，dict drift）。
        Set<String> reachable = reachableFrom(ErpCtConstants.CONTRACT_STATUS_DRAFT);
        assertTrue(reachable.contains(ErpCtConstants.CONTRACT_STATUS_NEGOTIATION),
                "从 DRAFT 经 submit 命名动作可达 NEGOTIATION（RC-R1.32 落地）: " + reachable);
        assertTrue(reachable.contains(ErpCtConstants.CONTRACT_STATUS_ACTIVE),
                "从 DRAFT 经 rejectAmend→ACTIVE 可达（驳回恢复出边）: " + reachable);
        assertTrue(reachable.contains(ErpCtConstants.CONTRACT_STATUS_SUSPENDED), "DRAFT→ACTIVE→SUSPENDED 可达");
        assertTrue(reachable.contains(ErpCtConstants.CONTRACT_STATUS_EXPIRED), "DRAFT→ACTIVE→EXPIRED 可达");
        assertTrue(reachable.contains(ErpCtConstants.CONTRACT_STATUS_TERMINATED), "DRAFT→NEGOTIATION→TERMINATED 可达");
    }

    @Test
    public void testReachabilityFromNegotiationCoversImplementedSubgraph() {
        // NEGOTIATION 经 CRUD 可写（M0.1 §9.4 残留风险）；从 NEGOTIATION 命名动作可达已实现子图
        Set<String> reachable = reachableFrom(ErpCtConstants.CONTRACT_STATUS_NEGOTIATION);
        assertTrue(reachable.contains(ErpCtConstants.CONTRACT_STATUS_ACTIVE), "NEGOTIATION→ACTIVE 经 activate 可达");
        assertTrue(reachable.contains(ErpCtConstants.CONTRACT_STATUS_TERMINATED), "NEGOTIATION→TERMINATED 经 terminate 可达");
        assertTrue(reachable.contains(ErpCtConstants.CONTRACT_STATUS_SUSPENDED), "NEGOTIATION→ACTIVE→SUSPENDED 可达");
        assertTrue(reachable.contains(ErpCtConstants.CONTRACT_STATUS_EXPIRED), "NEGOTIATION→ACTIVE→EXPIRED 可达");
    }

    @Test
    public void testTerminalStatusesHaveNoOutgoingEdges() {
        for (String terminal : sm.terminalStatuses()) {
            for (ErpCtContractStateMachine.TransitionDefinition e : sm.transitions()) {
                assertFalse(e.getFromStatus().equals(terminal),
                        "终态不应有出边: terminal=" + terminal + ", but edge " + e.getAction() + " leaves it");
            }
        }
    }

    // ---------- (c) terminate 多源 {ACTIVE, NEGOTIATION} 全覆盖、对其余态非法 ----------

    @Test
    public void testTerminateLegalForActiveAndNegotiationAndIllegalForOthers() {
        // 合法来源态
        sm.assertCanTerminate(ErpCtConstants.CONTRACT_STATUS_ACTIVE);
        sm.assertCanTerminate(ErpCtConstants.CONTRACT_STATUS_NEGOTIATION);
        assertEquals(ErpCtConstants.CONTRACT_STATUS_TERMINATED, sm.terminateTargetStatus());

        // 其余态非法（含终态）
        for (String s : ALL_STATUSES) {
            if (s.equals(ErpCtConstants.CONTRACT_STATUS_ACTIVE)
                    || s.equals(ErpCtConstants.CONTRACT_STATUS_NEGOTIATION)) {
                continue;
            }
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanTerminate(s),
                    "terminate 对非 {ACTIVE,NEGOTIATION} 应非法: " + s);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                    "Bean 报告 common 层非法迁移码");
            assertEquals("terminate", ex.getParam(ErpCtContractStateMachine.ARG_ACTION), "拒绝元数据携带动作名");
            assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS), "拒绝元数据携带当前态");
        }
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpCtContractStateMachine.TransitionDefinition e : sm.transitions()) {
            // 每条边的 fromStatus 对该 action 合法（assert 放行不抛）。单来源动作的「其余态非法」由
            // testExplicitActionGuards 覆盖；terminate 为多来源动作，此处只验证声明的边均合法。
            invokeAssert(e.getAction(), e.getFromStatus());
            // 每条边的 toStatus 与 <action>TargetStatus() 一致
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (e) 终态/初始态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpCtConstants.CONTRACT_STATUS_EXPIRED, ErpCtConstants.CONTRACT_STATUS_TERMINATED),
                sm.terminalStatuses(), "终态集合 = {EXPIRED, TERMINATED}（CANCELLED 不在 dict，不纳入）");
        assertEquals(Arrays.asList(ErpCtConstants.CONTRACT_STATUS_DRAFT), sm.initialStatuses(), "初始态集合 = {DRAFT}");

        assertTrue(sm.isTerminal(ErpCtConstants.CONTRACT_STATUS_EXPIRED));
        assertTrue(sm.isTerminal(ErpCtConstants.CONTRACT_STATUS_TERMINATED));
        assertFalse(sm.isTerminal(ErpCtConstants.CONTRACT_STATUS_DRAFT));
        assertFalse(sm.isTerminal(ErpCtConstants.CONTRACT_STATUS_ACTIVE));
        assertFalse(sm.isTerminal(ErpCtConstants.CONTRACT_STATUS_SUSPENDED));
    }

    // ---------- 合法/非法来源态显式断言（补充显式方法语义核对） ----------

    @Test
    public void testExplicitActionGuards() {
        // submit: 仅 DRAFT 合法（RC-R1.32，§2 漂移 successor）
        assertActionAllowsOnly("submit", ErpCtConstants.CONTRACT_STATUS_DRAFT);
        // rejectAmend: 仅 DRAFT 合法（RC-R1.32，驳回恢复出边）
        assertActionAllowsOnly("rejectAmend", ErpCtConstants.CONTRACT_STATUS_DRAFT);
        // activate: 仅 NEGOTIATION 合法
        assertActionAllowsOnly("activate", ErpCtConstants.CONTRACT_STATUS_NEGOTIATION);
        // suspend: 仅 ACTIVE 合法
        assertActionAllowsOnly("suspend", ErpCtConstants.CONTRACT_STATUS_ACTIVE);
        // resume: 仅 SUSPENDED 合法
        assertActionAllowsOnly("resume", ErpCtConstants.CONTRACT_STATUS_SUSPENDED);
        // expire: 仅 ACTIVE 合法
        assertActionAllowsOnly("expire", ErpCtConstants.CONTRACT_STATUS_ACTIVE);
        // amend: 仅 ACTIVE 合法
        assertActionAllowsOnly("amend", ErpCtConstants.CONTRACT_STATUS_ACTIVE);
    }

    @Test
    public void testTargetStatusMethods() {
        assertEquals(ErpCtConstants.CONTRACT_STATUS_NEGOTIATION, sm.submitTargetStatus());
        assertEquals(ErpCtConstants.CONTRACT_STATUS_ACTIVE, sm.activateTargetStatus());
        assertEquals(ErpCtConstants.CONTRACT_STATUS_SUSPENDED, sm.suspendTargetStatus());
        assertEquals(ErpCtConstants.CONTRACT_STATUS_ACTIVE, sm.resumeTargetStatus());
        assertEquals(ErpCtConstants.CONTRACT_STATUS_TERMINATED, sm.terminateTargetStatus());
        assertEquals(ErpCtConstants.CONTRACT_STATUS_EXPIRED, sm.expireTargetStatus());
        assertEquals(ErpCtConstants.CONTRACT_STATUS_DRAFT, sm.amendTargetStatus());
        assertEquals(ErpCtConstants.CONTRACT_STATUS_ACTIVE, sm.rejectAmendTargetStatus());
    }

    // ---------- helpers ----------

    /**
     * 断言某 action 仅允许指定来源态：该来源态放行（不抛），其余全部状态非法（抛 common 码 + action 元数据）。
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
                assertEquals(action, ex.getParam(ErpCtContractStateMachine.ARG_ACTION),
                        "拒绝元数据携带动作名: action=" + action);
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                        "拒绝元数据携带当前态: action=" + action + ", status=" + s);
            }
        }
    }

    private void invokeAssert(String action, String status) {
        switch (action) {
            case "submit":
                sm.assertCanSubmitForNegotiation(status);
                break;
            case "activate":
                sm.assertCanActivate(status);
                break;
            case "suspend":
                sm.assertCanSuspend(status);
                break;
            case "resume":
                sm.assertCanResume(status);
                break;
            case "terminate":
                sm.assertCanTerminate(status);
                break;
            case "expire":
                sm.assertCanExpire(status);
                break;
            case "amend":
                sm.assertCanAmend(status);
                break;
            case "rejectAmend":
                sm.assertCanRejectAmend(status);
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "submit":
                return sm.submitTargetStatus();
            case "activate":
                return sm.activateTargetStatus();
            case "suspend":
                return sm.suspendTargetStatus();
            case "resume":
                return sm.resumeTargetStatus();
            case "terminate":
                return sm.terminateTargetStatus();
            case "expire":
                return sm.expireTargetStatus();
            case "amend":
                return sm.amendTargetStatus();
            case "rejectAmend":
                return sm.rejectAmendTargetStatus();
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
            for (ErpCtContractStateMachine.TransitionDefinition e : sm.transitions()) {
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
