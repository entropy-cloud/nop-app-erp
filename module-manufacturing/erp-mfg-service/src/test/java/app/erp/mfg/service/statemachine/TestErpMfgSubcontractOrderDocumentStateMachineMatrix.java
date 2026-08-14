package app.erp.mfg.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.mfg.service.ErpMfgConstants;
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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-14-0930-1 Phase 2 Proof）。
 *
 * <p>针对 {@link ErpMfgSubcontractOrderDocumentStateMachine} Bean（M4.37 docStatus 业务生命周期轴）的纯矩阵完备性遍历：
 * 不经 BizModel/Processor 入口（层 3 职责），不断言副作用/审计（过账/stock move 归层 3）。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边（11 边唯一 action|fromStatus 键）；</li>
 *   <li>(b) 从 DRAFT 可达全部非终态 + 各终态；</li>
 *   <li>(c) 各 {@code assertCanXxx} 合法来源态通过、非法来源态抛 common 码携带 {@code action}/{@code fromStatus}；</li>
 *   <li>(d) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(e) 终态 {COMPLETED, CANCELLED} 无出边；初始态 {DRAFT}；</li>
 *   <li>(f) 终态/初始态集合正确。</li>
 * </ul>
 *
 * <p>Subcontract 独有差异（plan Phase 2 Decision）：
 * <ul>
 *   <li>reject（docStatus 侧）边 SUBMITTED→REJECTED 存在（doReject 联动写 docStatus=REJECTED，与 WorkOrder 不同）；</li>
 *   <li>submit（docStatus 侧）来源 {DRAFT, REJECTED}（驳回后重提）；</li>
 *   <li>reverseCompletion 为「动态不对称守卫 + 固定状态边」——posted 判定保留在 facade {@code validateCanReverse} 原位，
 *       本 Bean 仅编码状态边 COMPLETED→CANCELLED；</li>
 *   <li>MrpRelease spawn（APPROVED/APPROVED）与 {@code MfgSubcontractReversalListener} 回写（CANCELLED + posted=false）
 *       为 §9.2 选项 c 豁免路径，不经 Bean 守卫。</li>
 * </ul>
 *
 * <p>层 2 四方对照（SubcontractOrder docStatus 轴单条）：dict {@code erp-mfg/subcontract-status}（8 值）↔
 * {@code docs/design/manufacturing/state-machine.md} §适用对象三 ↔ Bean 元数据 ↔ 全部 writer
 * （5+4 Processor live + cancel facade 直入 + MrpRelease spawn + MfgSubcontractReversalListener 回写 + CRUD 路径 §9.4 选项 c 排除）。
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpMfgSubcontractOrderDocumentStateMachineMatrix {

    private static final List<String> ALL_STATUSES = Arrays.asList(
            ErpMfgConstants.SUBCONTRACT_STATUS_DRAFT,
            ErpMfgConstants.SUBCONTRACT_STATUS_SUBMITTED,
            ErpMfgConstants.SUBCONTRACT_STATUS_APPROVED,
            ErpMfgConstants.SUBCONTRACT_STATUS_ISSUED,
            ErpMfgConstants.SUBCONTRACT_STATUS_RECEIVED,
            ErpMfgConstants.SUBCONTRACT_STATUS_COMPLETED,
            ErpMfgConstants.SUBCONTRACT_STATUS_CANCELLED,
            ErpMfgConstants.SUBCONTRACT_STATUS_REJECTED);

    private final ErpMfgSubcontractOrderDocumentStateMachine sm = new ErpMfgSubcontractOrderDocumentStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpMfgSubcontractOrderDocumentStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpMfgSubcontractOrderDocumentStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(11, edges.size(),
                "迁移矩阵应有 11 条边（submit 2 + approve 1 + reject 1 + issueMaterials 1 + receiveFinished 1 + postProcessingFee 1 + reverseCompletion 1 + cancel 3）");
    }

    // ---------- (b) 从 DRAFT 可达全部非终态 + 终态 ----------

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpMfgConstants.SUBCONTRACT_STATUS_DRAFT);
        assertTrue(reachable.contains(ErpMfgConstants.SUBCONTRACT_STATUS_SUBMITTED), "从 DRAFT 应可达 SUBMITTED");
        assertTrue(reachable.contains(ErpMfgConstants.SUBCONTRACT_STATUS_APPROVED), "从 DRAFT 应可达 APPROVED");
        assertTrue(reachable.contains(ErpMfgConstants.SUBCONTRACT_STATUS_ISSUED), "从 DRAFT 应可达 ISSUED");
        assertTrue(reachable.contains(ErpMfgConstants.SUBCONTRACT_STATUS_RECEIVED), "从 DRAFT 应可达 RECEIVED");
        assertTrue(reachable.contains(ErpMfgConstants.SUBCONTRACT_STATUS_COMPLETED), "从 DRAFT 应可达 COMPLETED");
        assertTrue(reachable.contains(ErpMfgConstants.SUBCONTRACT_STATUS_REJECTED), "从 DRAFT 应可达 REJECTED（经 reject 分支）");
        assertTrue(reachable.contains(ErpMfgConstants.SUBCONTRACT_STATUS_CANCELLED), "从 DRAFT 应可达 CANCELLED");
    }

    // ---------- (c) assertCanXxx 合法/非法 ----------

    @Test
    public void testAssertCanSubmitLegalAndIllegal() {
        sm.assertCanSubmit(ErpMfgConstants.SUBCONTRACT_STATUS_DRAFT);
        sm.assertCanSubmit(ErpMfgConstants.SUBCONTRACT_STATUS_REJECTED);
        assertEquals(ErpMfgConstants.SUBCONTRACT_STATUS_SUBMITTED, sm.submitTargetStatus());
        for (String illegal : illegalFor(ALL_STATUSES,
                ErpMfgConstants.SUBCONTRACT_STATUS_DRAFT, ErpMfgConstants.SUBCONTRACT_STATUS_REJECTED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanSubmit(illegal),
                    "submit 对非法来源态应抛: " + illegal);
            assertCommonTransitionMetadata(ex, "submit", illegal);
        }
    }

    @Test
    public void testAssertCanApproveLegalAndIllegal() {
        sm.assertCanApprove(ErpMfgConstants.SUBCONTRACT_STATUS_SUBMITTED);
        assertEquals(ErpMfgConstants.SUBCONTRACT_STATUS_APPROVED, sm.approveTargetStatus());
        for (String illegal : illegalFor(ALL_STATUSES, ErpMfgConstants.SUBCONTRACT_STATUS_SUBMITTED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanApprove(illegal),
                    "approve 对非法来源态应抛: " + illegal);
            assertCommonTransitionMetadata(ex, "approve", illegal);
        }
    }

    @Test
    public void testAssertCanRejectLegalAndIllegal() {
        sm.assertCanReject(ErpMfgConstants.SUBCONTRACT_STATUS_SUBMITTED);
        assertEquals(ErpMfgConstants.SUBCONTRACT_STATUS_REJECTED, sm.rejectTargetStatus());
        for (String illegal : illegalFor(ALL_STATUSES, ErpMfgConstants.SUBCONTRACT_STATUS_SUBMITTED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanReject(illegal),
                    "reject 对非法来源态应抛: " + illegal);
            assertCommonTransitionMetadata(ex, "reject", illegal);
        }
    }

    @Test
    public void testAssertCanIssueMaterialsLegalAndIllegal() {
        sm.assertCanIssueMaterials(ErpMfgConstants.SUBCONTRACT_STATUS_APPROVED);
        assertEquals(ErpMfgConstants.SUBCONTRACT_STATUS_ISSUED, sm.issueMaterialsTargetStatus());
        for (String illegal : illegalFor(ALL_STATUSES, ErpMfgConstants.SUBCONTRACT_STATUS_APPROVED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanIssueMaterials(illegal),
                    "issueMaterials 对非法来源态应抛: " + illegal);
            assertCommonTransitionMetadata(ex, "issueMaterials", illegal);
        }
    }

    @Test
    public void testAssertCanReceiveFinishedLegalAndIllegal() {
        sm.assertCanReceiveFinished(ErpMfgConstants.SUBCONTRACT_STATUS_ISSUED);
        assertEquals(ErpMfgConstants.SUBCONTRACT_STATUS_RECEIVED, sm.receiveFinishedTargetStatus());
        for (String illegal : illegalFor(ALL_STATUSES, ErpMfgConstants.SUBCONTRACT_STATUS_ISSUED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanReceiveFinished(illegal),
                    "receiveFinished 对非法来源态应抛: " + illegal);
            assertCommonTransitionMetadata(ex, "receiveFinished", illegal);
        }
    }

    @Test
    public void testAssertCanPostProcessingFeeLegalAndIllegal() {
        sm.assertCanPostProcessingFee(ErpMfgConstants.SUBCONTRACT_STATUS_RECEIVED);
        assertEquals(ErpMfgConstants.SUBCONTRACT_STATUS_COMPLETED, sm.postProcessingFeeTargetStatus());
        for (String illegal : illegalFor(ALL_STATUSES, ErpMfgConstants.SUBCONTRACT_STATUS_RECEIVED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanPostProcessingFee(illegal),
                    "postProcessingFee 对非法来源态应抛: " + illegal);
            assertCommonTransitionMetadata(ex, "postProcessingFee", illegal);
        }
    }

    @Test
    public void testAssertCanReverseCompletionLegalAndIllegal() {
        sm.assertCanReverseCompletion(ErpMfgConstants.SUBCONTRACT_STATUS_COMPLETED);
        assertEquals(ErpMfgConstants.SUBCONTRACT_STATUS_CANCELLED, sm.reverseCompletionTargetStatus());
        for (String illegal : illegalFor(ALL_STATUSES, ErpMfgConstants.SUBCONTRACT_STATUS_COMPLETED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanReverseCompletion(illegal),
                    "reverseCompletion 对非法来源态应抛: " + illegal);
            assertCommonTransitionMetadata(ex, "reverseCompletion", illegal);
        }
    }

    @Test
    public void testAssertCanCancelLegalAndIllegal() {
        sm.assertCanCancel(ErpMfgConstants.SUBCONTRACT_STATUS_DRAFT);
        sm.assertCanCancel(ErpMfgConstants.SUBCONTRACT_STATUS_SUBMITTED);
        sm.assertCanCancel(ErpMfgConstants.SUBCONTRACT_STATUS_APPROVED);
        assertEquals(ErpMfgConstants.SUBCONTRACT_STATUS_CANCELLED, sm.cancelTargetStatus());
        for (String illegal : illegalFor(ALL_STATUSES,
                ErpMfgConstants.SUBCONTRACT_STATUS_DRAFT,
                ErpMfgConstants.SUBCONTRACT_STATUS_SUBMITTED,
                ErpMfgConstants.SUBCONTRACT_STATUS_APPROVED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanCancel(illegal),
                    "cancel 对非法来源态应抛: " + illegal);
            assertCommonTransitionMetadata(ex, "cancel", illegal);
        }
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpMfgSubcontractOrderDocumentStateMachine.TransitionDefinition e : sm.transitions()) {
            invokeAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (e) 终态出边约束：CANCELLED 无出边；COMPLETED 为「可逆业务终态」仅 reverseCompletion 出边 ----------

    @Test
    public void testTerminalStatusesHaveNoOutgoingEdges() {
        for (ErpMfgSubcontractOrderDocumentStateMachine.TransitionDefinition e : sm.transitions()) {
            assertFalse(e.getFromStatus().equals(ErpMfgConstants.SUBCONTRACT_STATUS_CANCELLED),
                    "CANCELLED 终态不应有出边: edge=" + e.getAction());
        }
    }

    /** COMPLETED 是「可逆业务终态」——经 reverseCompletion 有出边（不适用「终态无出边」强断言）。 */
    @Test
    public void testCompletedIsReversibleTerminal() {
        boolean completedHasOutgoing = false;
        for (ErpMfgSubcontractOrderDocumentStateMachine.TransitionDefinition e : sm.transitions()) {
            if (e.getFromStatus().equals(ErpMfgConstants.SUBCONTRACT_STATUS_COMPLETED)) {
                completedHasOutgoing = true;
                assertEquals("reverseCompletion", e.getAction(), "COMPLETED 的唯一出边应为 reverseCompletion");
                assertEquals(ErpMfgConstants.SUBCONTRACT_STATUS_CANCELLED, e.getToStatus());
            }
        }
        assertTrue(completedHasOutgoing, "COMPLETED 应有 reverseCompletion 出边（可逆终态）");
    }

    // ---------- (f) 终态/初始态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpMfgConstants.SUBCONTRACT_STATUS_COMPLETED,
                        ErpMfgConstants.SUBCONTRACT_STATUS_CANCELLED),
                sm.terminalStatuses(), "终态集合 = {COMPLETED, CANCELLED}");
        assertEquals(java.util.Collections.singletonList(ErpMfgConstants.SUBCONTRACT_STATUS_DRAFT),
                sm.initialStatuses(), "初始态集合 = {DRAFT}");

        assertTrue(sm.isTerminal(ErpMfgConstants.SUBCONTRACT_STATUS_COMPLETED));
        assertTrue(sm.isTerminal(ErpMfgConstants.SUBCONTRACT_STATUS_CANCELLED));
        assertFalse(sm.isTerminal(ErpMfgConstants.SUBCONTRACT_STATUS_DRAFT));
        assertFalse(sm.isTerminal(ErpMfgConstants.SUBCONTRACT_STATUS_APPROVED));
        assertFalse(sm.isTerminal(ErpMfgConstants.SUBCONTRACT_STATUS_REJECTED));
    }

    // ---------- helpers ----------

    private void invokeAssert(String action, String status) {
        switch (action) {
            case "submit": sm.assertCanSubmit(status); break;
            case "approve": sm.assertCanApprove(status); break;
            case "reject": sm.assertCanReject(status); break;
            case "issueMaterials": sm.assertCanIssueMaterials(status); break;
            case "receiveFinished": sm.assertCanReceiveFinished(status); break;
            case "postProcessingFee": sm.assertCanPostProcessingFee(status); break;
            case "reverseCompletion": sm.assertCanReverseCompletion(status); break;
            case "cancel": sm.assertCanCancel(status); break;
            default: throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "submit": return sm.submitTargetStatus();
            case "approve": return sm.approveTargetStatus();
            case "reject": return sm.rejectTargetStatus();
            case "issueMaterials": return sm.issueMaterialsTargetStatus();
            case "receiveFinished": return sm.receiveFinishedTargetStatus();
            case "postProcessingFee": return sm.postProcessingFeeTargetStatus();
            case "reverseCompletion": return sm.reverseCompletionTargetStatus();
            case "cancel": return sm.cancelTargetStatus();
            default: throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private void assertCommonTransitionMetadata(NopException ex, String action, String status) {
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + status);
        assertEquals(action, ex.getParam(ErpMfgSubcontractOrderDocumentStateMachine.ARG_ACTION), "拒绝元数据携带动作名");
        assertEquals(status, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS), "拒绝元数据携带当前态");
    }

    private List<String> illegalFor(List<String> all, String... legal) {
        Set<String> legalSet = new HashSet<>(Arrays.asList(legal));
        return all.stream().filter(s -> !legalSet.contains(s)).collect(Collectors.toList());
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
            for (ErpMfgSubcontractOrderDocumentStateMachine.TransitionDefinition e : sm.transitions()) {
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
