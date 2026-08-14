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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-14-1931-2 Phase 3 Proof，M4.42）。
 *
 * <p>针对 {@link ErpAstValueAdjustmentDocumentStateMachine}（docStatus 业务生命周期轴）的纯矩阵完备性遍历：
 * 不经 BizModel 入口（层 3 职责）。
 *
 * <p>覆盖（layer-2 四方对照裁定，plan Phase 3 Proof——非退化轴，唯一有独立 cancel mutation 的实体）：
 * <ul>
 *   <li>(a) 无重复/冲突边（2 条边：approve DRAFT→ACTIVE + cancel DRAFT→CANCELLED）；</li>
 *   <li>(b) 从 DRAFT 可达 ACTIVE（approve）与 CANCELLED（cancel mutation）；</li>
 *   <li>(c) approve 守卫：非 CANCELLED 合法；cancel 守卫：仅 DRAFT/null 合法（ACTIVE「非已生效」/CANCELLED「非已作废」非法）；</li>
 *   <li>(d) {@code transitions()} 元数据与显式方法语义一致（含 cancelTargetStatus()=CANCELLED）；</li>
 *   <li>(e) 终态/初始态集合正确（terminal={ACTIVE, CANCELLED}，initial={DRAFT}）；</li>
 *   <li>(f) ACTIVE 无出边（reverseApprove 不写 docStatus）；</li>
 *   <li>(g) dict 3 值全覆盖（isCancelled/isTerminal）。</li>
 * </ul>
 *
 * <p>posted 动态守卫（「非已过账」拒绝 cancel）不在 Bean 矩阵范围（posted 不入轴，契约 §3），保留在
 * {@code ErpAstValueAdjustmentProcessor.validateTransitionForCancel} 原位——本测试仅验证 Bean 固定状态守卫。
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpAstValueAdjustmentDocumentStateMachineMatrix {

    private static final List<String> ALL_DOC_STATUSES = Arrays.asList(
            ErpAstConstants.DOC_STATUS_DRAFT,
            ErpAstConstants.DOC_STATUS_ACTIVE,
            ErpAstConstants.DOC_STATUS_CANCELLED);

    private final ErpAstValueAdjustmentDocumentStateMachine documentSm =
            new ErpAstValueAdjustmentDocumentStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void noDuplicateOrConflictingEdges() {
        List<ErpAstValueAdjustmentDocumentStateMachine.TransitionDefinition> edges = documentSm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpAstValueAdjustmentDocumentStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(2, edges.size(), "迁移矩阵应有 2 条边（approve DRAFT→ACTIVE + cancel DRAFT→CANCELLED）");
    }

    // ---------- (b) 可达性 ----------

    @Test
    public void reachabilityFromDraft() {
        Set<String> reachable = reachableFrom(ErpAstConstants.DOC_STATUS_DRAFT);
        assertTrue(reachable.contains(ErpAstConstants.DOC_STATUS_ACTIVE),
                "从 DRAFT 应可达 ACTIVE（approve/auto-approve 双 writer）");
        assertTrue(reachable.contains(ErpAstConstants.DOC_STATUS_CANCELLED),
                "从 DRAFT 应可达 CANCELLED（独立 cancel mutation）");
    }

    // ---------- (c) approve/cancel 守卫 ----------

    @Test
    public void approveGuardAllowsNonCancelled() {
        documentSm.assertCanApprove(ErpAstConstants.DOC_STATUS_DRAFT); // 合法不抛
        documentSm.assertCanApprove(ErpAstConstants.DOC_STATUS_ACTIVE); // 合法不抛（重入场景，行为保持）
        documentSm.assertCanApprove(null); // null 放行
    }

    @Test
    public void approveGuardRejectsCancelled() {
        NopException ex = assertThrows(NopException.class,
                () -> documentSm.assertCanApprove(ErpAstConstants.DOC_STATUS_CANCELLED),
                "approve 对 CANCELLED 应非法（已作废单据禁止审批）");
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "Bean 报告 common 层非法迁移码");
        assertEquals("approve", ex.getParam(ErpAstValueAdjustmentDocumentStateMachine.ARG_ACTION),
                "拒绝元数据携带动作名");
        assertEquals(ErpAstConstants.DOC_STATUS_CANCELLED, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                "拒绝元数据携带当前态");
    }

    @Test
    public void cancelGuardAllowsOnlyDraft() {
        documentSm.assertCanCancel(ErpAstConstants.DOC_STATUS_DRAFT); // 合法不抛
        documentSm.assertCanCancel(null); // null 放行（无 docStatus 场景）
    }

    @Test
    public void cancelGuardRejectsActive() {
        NopException ex = assertThrows(NopException.class,
                () -> documentSm.assertCanCancel(ErpAstConstants.DOC_STATUS_ACTIVE),
                "cancel 对 ACTIVE 应非法（已生效单据禁止作废）");
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "Bean 报告 common 层非法迁移码");
        assertEquals("cancel", ex.getParam(ErpAstValueAdjustmentDocumentStateMachine.ARG_ACTION),
                "拒绝元数据携带动作名");
        assertEquals(ErpAstConstants.DOC_STATUS_ACTIVE, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                "拒绝元数据携带当前态");
        assertEquals("非已生效", ex.getParam(ErpCommonErrors.ARG_EXPECTED_STATUS),
                "拒绝元数据携带期望态诊断（接线方按当前态区分领域码 expected 参数）");
    }

    @Test
    public void cancelGuardRejectsCancelled() {
        NopException ex = assertThrows(NopException.class,
                () -> documentSm.assertCanCancel(ErpAstConstants.DOC_STATUS_CANCELLED),
                "cancel 对 CANCELLED 应非法（已作废单据禁止重复作废）");
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "Bean 报告 common 层非法迁移码");
        assertEquals(ErpAstConstants.DOC_STATUS_CANCELLED, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                "拒绝元数据携带当前态");
        assertEquals("非已作废", ex.getParam(ErpCommonErrors.ARG_EXPECTED_STATUS),
                "拒绝元数据携带期望态诊断");
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void transitionsMetadataConsistentWithExplicitMethods() {
        for (ErpAstValueAdjustmentDocumentStateMachine.TransitionDefinition e : documentSm.transitions()) {
            switch (e.getAction()) {
                case "approve":
                    documentSm.assertCanApprove(e.getFromStatus());
                    assertEquals(e.getToStatus(), documentSm.approveTargetStatus(),
                            "toStatus 与 approveTargetStatus 不一致");
                    break;
                case "cancel":
                    documentSm.assertCanCancel(e.getFromStatus());
                    assertEquals(e.getToStatus(), documentSm.cancelTargetStatus(),
                            "toStatus 与 cancelTargetStatus 不一致");
                    break;
                default:
                    throw new IllegalArgumentException("unknown action: " + e.getAction());
            }
        }
    }

    @Test
    public void targetStatusMethods() {
        assertEquals(ErpAstConstants.DOC_STATUS_ACTIVE, documentSm.approveTargetStatus());
        assertEquals(ErpAstConstants.DOC_STATUS_CANCELLED, documentSm.cancelTargetStatus());
    }

    // ---------- (e) 终态/初始态集合正确 ----------

    @Test
    public void terminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpAstConstants.DOC_STATUS_ACTIVE, ErpAstConstants.DOC_STATUS_CANCELLED),
                documentSm.terminalStatuses(), "终态集合 = {ACTIVE, CANCELLED}");
        assertEquals(Arrays.asList(ErpAstConstants.DOC_STATUS_DRAFT),
                documentSm.initialStatuses(), "初始态集合 = {DRAFT}");

        assertTrue(documentSm.isTerminal(ErpAstConstants.DOC_STATUS_ACTIVE),
                "ACTIVE 为 approve 后业务终态（无出边）");
        assertTrue(documentSm.isTerminal(ErpAstConstants.DOC_STATUS_CANCELLED),
                "CANCELLED 为 cancel mutation + useLogicalDelete 终态");
        assertFalse(documentSm.isTerminal(ErpAstConstants.DOC_STATUS_DRAFT), "DRAFT 为初始态非终态");
    }

    // ---------- (f) ACTIVE 无出边（reverseApprove 不写 docStatus） ----------

    @Test
    public void activeHasNoOutgoingEdge() {
        boolean hasOutgoing = documentSm.transitions().stream()
                .anyMatch(e -> ErpAstConstants.DOC_STATUS_ACTIVE.equals(e.getFromStatus()));
        assertFalse(hasOutgoing, "ACTIVE 应无出边（ValueAdjustment reverseApprove 仅写 approveStatus，不写 docStatus）");
    }

    // ---------- (g) dict 3 值全覆盖 ----------

    @Test
    public void allStatusesCovered() {
        for (String s : ALL_DOC_STATUSES) {
            documentSm.isCancelled(s); // 不抛即可
            documentSm.isTerminal(s);
        }
        assertTrue(documentSm.isCancelled(ErpAstConstants.DOC_STATUS_CANCELLED), "CANCELLED 应识别为已作废");
        assertFalse(documentSm.isCancelled(ErpAstConstants.DOC_STATUS_DRAFT));
        assertFalse(documentSm.isCancelled(ErpAstConstants.DOC_STATUS_ACTIVE),
                "ACTIVE 非作废（approve 后业务态）");
    }

    // ==================== helpers ====================

    private Set<String> reachableFrom(String start) {
        Set<String> visited = new LinkedHashSet<>();
        List<String> frontier = new java.util.ArrayList<>();
        frontier.add(start);
        while (!frontier.isEmpty()) {
            String cur = frontier.remove(0);
            if (!visited.add(cur)) {
                continue;
            }
            for (ErpAstValueAdjustmentDocumentStateMachine.TransitionDefinition e : documentSm.transitions()) {
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
