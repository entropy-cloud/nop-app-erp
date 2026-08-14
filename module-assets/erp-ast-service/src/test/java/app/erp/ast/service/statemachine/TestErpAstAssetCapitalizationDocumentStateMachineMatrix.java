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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-14-1931-2 Phase 2 Proof，M4.46）。
 *
 * <p>针对 {@link ErpAstAssetCapitalizationDocumentStateMachine}（docStatus 业务生命周期轴）的纯矩阵完备性遍历：
 * 不经 BizModel 入口（层 3 职责）。
 *
 * <p>覆盖（layer-2 四方对照裁定，plan Phase 2 Proof——非退化轴 + <b>Capitalization Document 轴特例边</b>）：
 * <ul>
 *   <li>(a) 无重复/冲突边（2 条边：approve DRAFT→ACTIVE + <b>reverseApprove ACTIVE→CANCELLED 特例边</b>）；</li>
 *   <li>(b) 从 DRAFT 可达 ACTIVE → CANCELLED（reverseApprove 特例边是 CANCELLED 的命名动作可达路径）；</li>
 *   <li>(c) approve/reverseApprove 守卫：非 CANCELLED 合法，CANCELLED 非法（common 码 + action/fromStatus 元数据）；</li>
 *   <li>(d) {@code transitions()} 元数据与显式方法语义一致（含 reverseApproveTargetStatus()=CANCELLED）；</li>
 *   <li>(e) 终态/初始态集合正确（terminal={CANCELLED}——ACTIVE 为可逆中间态不纳入；initial={DRAFT}）；</li>
 *   <li>(f) ACTIVE 为「可逆中间态」——经 reverseApprove 有出边（与 Disposal ACTIVE 无出边区分）；</li>
 *   <li>(g) dict 3 值全覆盖（isCancelled/isTerminal）。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpAstAssetCapitalizationDocumentStateMachineMatrix {

    private static final List<String> ALL_DOC_STATUSES = Arrays.asList(
            ErpAstConstants.DOC_STATUS_DRAFT,
            ErpAstConstants.DOC_STATUS_ACTIVE,
            ErpAstConstants.DOC_STATUS_CANCELLED);

    private final ErpAstAssetCapitalizationDocumentStateMachine documentSm =
            new ErpAstAssetCapitalizationDocumentStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void noDuplicateOrConflictingEdges() {
        List<ErpAstAssetCapitalizationDocumentStateMachine.TransitionDefinition> edges = documentSm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpAstAssetCapitalizationDocumentStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(2, edges.size(), "迁移矩阵应有 2 条边（approve DRAFT→ACTIVE + reverseApprove ACTIVE→CANCELLED 特例边）");
    }

    // ---------- (b) 可达性 ----------

    @Test
    public void reachabilityFromDraft() {
        Set<String> reachable = reachableFrom(ErpAstConstants.DOC_STATUS_DRAFT);
        assertTrue(reachable.contains(ErpAstConstants.DOC_STATUS_ACTIVE),
                "从 DRAFT 应可达 ACTIVE（approve 命名 writer）");
        assertTrue(reachable.contains(ErpAstConstants.DOC_STATUS_CANCELLED),
                "从 DRAFT 应可达 CANCELLED（approve→reverseApprove 特例边路径）");
    }

    // ---------- (c) approve/reverseApprove 守卫：非 CANCELLED 合法 ----------

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
        assertEquals("approve", ex.getParam(ErpAstAssetCapitalizationDocumentStateMachine.ARG_ACTION),
                "拒绝元数据携带动作名");
        assertEquals(ErpAstConstants.DOC_STATUS_CANCELLED, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                "拒绝元数据携带当前态");
    }

    @Test
    public void reverseApproveGuardAllowsNonCancelled() {
        documentSm.assertCanReverseApprove(ErpAstConstants.DOC_STATUS_DRAFT); // 合法不抛
        documentSm.assertCanReverseApprove(ErpAstConstants.DOC_STATUS_ACTIVE); // 合法不抛（posted=false 窗口现实来源态）
        documentSm.assertCanReverseApprove(null); // null 放行
    }

    @Test
    public void reverseApproveGuardRejectsCancelled() {
        NopException ex = assertThrows(NopException.class,
                () -> documentSm.assertCanReverseApprove(ErpAstConstants.DOC_STATUS_CANCELLED),
                "reverseApprove 对 CANCELLED 应非法（已作废单据禁止红冲审批）");
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "Bean 报告 common 层非法迁移码");
        assertEquals("reverseApprove", ex.getParam(ErpAstAssetCapitalizationDocumentStateMachine.ARG_ACTION),
                "拒绝元数据携带动作名");
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void transitionsMetadataConsistentWithExplicitMethods() {
        for (ErpAstAssetCapitalizationDocumentStateMachine.TransitionDefinition e : documentSm.transitions()) {
            switch (e.getAction()) {
                case "approve":
                    documentSm.assertCanApprove(e.getFromStatus());
                    assertEquals(e.getToStatus(), documentSm.approveTargetStatus(),
                            "toStatus 与 approveTargetStatus 不一致");
                    break;
                case "reverseApprove":
                    documentSm.assertCanReverseApprove(e.getFromStatus());
                    assertEquals(e.getToStatus(), documentSm.reverseApproveTargetStatus(),
                            "toStatus 与 reverseApproveTargetStatus 不一致");
                    break;
                default:
                    throw new IllegalArgumentException("unknown action: " + e.getAction());
            }
        }
    }

    @Test
    public void targetStatusMethods() {
        assertEquals(ErpAstConstants.DOC_STATUS_ACTIVE, documentSm.approveTargetStatus());
        assertEquals(ErpAstConstants.DOC_STATUS_CANCELLED, documentSm.reverseApproveTargetStatus(),
                "Capitalization 特例边：reverseApprove docStatus 目标态=CANCELLED（Disposal/ValueAdjustment 无此写）");
    }

    // ---------- (e) 终态/初始态集合正确 ----------

    @Test
    public void terminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpAstConstants.DOC_STATUS_CANCELLED),
                documentSm.terminalStatuses(), "终态集合 = {CANCELLED}（ACTIVE 为可逆中间态不纳入）");
        assertEquals(Arrays.asList(ErpAstConstants.DOC_STATUS_DRAFT),
                documentSm.initialStatuses(), "初始态集合 = {DRAFT}");

        assertTrue(documentSm.isTerminal(ErpAstConstants.DOC_STATUS_CANCELLED),
                "CANCELLED 为终态（reverseApprove 特例边 + useLogicalDelete）");
        assertFalse(documentSm.isTerminal(ErpAstConstants.DOC_STATUS_ACTIVE),
                "ACTIVE 为可逆中间态（经 reverseApprove 有出边），非终态");
        assertFalse(documentSm.isTerminal(ErpAstConstants.DOC_STATUS_DRAFT), "DRAFT 为初始态非终态");
    }

    // ---------- (f) ACTIVE 为可逆中间态（有出边） ----------

    @Test
    public void activeIsReversibleIntermediate() {
        boolean hasOutgoing = documentSm.transitions().stream()
                .anyMatch(e -> ErpAstConstants.DOC_STATUS_ACTIVE.equals(e.getFromStatus()));
        assertTrue(hasOutgoing, "ACTIVE 应经 reverseApprove 有出边（可逆中间态，与 Disposal ACTIVE 无出边区分）");
        assertEquals("reverseApprove",
                documentSm.transitions().stream()
                        .filter(e -> ErpAstConstants.DOC_STATUS_ACTIVE.equals(e.getFromStatus()))
                        .findFirst().get().getAction(),
                "ACTIVE 的唯一出边动作应为 reverseApprove（特例边）");
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
            for (ErpAstAssetCapitalizationDocumentStateMachine.TransitionDefinition e : documentSm.transitions()) {
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
