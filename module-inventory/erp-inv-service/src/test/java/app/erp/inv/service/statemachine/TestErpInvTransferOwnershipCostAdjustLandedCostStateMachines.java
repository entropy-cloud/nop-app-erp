package app.erp.inv.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.inv.dao.constants.ErpInvDocStatus;
import app.erp.inv.service.ErpInvConstants;
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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-13-0810-3 Phase 1 Proof）。
 *
 * <p>针对 4 Bean 的纯矩阵完备性遍历：{@link ErpInvTransferOrderStateMachine}（调拨单 docStatus 单轴单边）、
 * {@link ErpInvOwnershipTransferStateMachine}（所有权转移单 docStatus 单轴 3 动作，独立 dict
 * {@code erp-inv/ownership-transfer-status}）、{@link ErpInvCostAdjustStateMachine}（成本调整单 docStatus
 * 单轴 2 动作，approveStatus 轴不在 Bean）、{@link ErpInvLandedCostStateMachine}（到岸成本单 docStatus
 * 单轴 2 动作，双轴联动中 Bean 仅 docStatus 边）。
 * 不经 BizModel 入口（层 3 职责），不断言副作用/过账/余额/成本层。
 *
 * <p>每轴覆盖：(a) 无重复/冲突边；(b) 从初始态可达全部声明状态 + 终态无出边；(c) 各动作合法/非法来源态全集；
 * (d) {@code transitions()} 元数据与显式方法语义一致；(e) 终态/初始态集合正确；(f) dict 值可达性
 * （无死状态；CANCELLED 跨实体内部编排写例外按容忍登记）。
 * Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 *
 * <p><b>dict 核对</b>：TransferOrder/CostAdjust/LandedCost 复用 {@code erp-inv/move-status} 4 值
 * DRAFT/CONFIRMED/DONE/CANCELLED；OwnershipTransfer 用独立 dict {@code erp-inv/ownership-transfer-status}
 * （值相同 DRAFT/CONFIRMED/DONE/CANCELLED，常量 {@code OWNERSHIP_TRANSFER_STATUS_*} 非 {@code DOC_STATUS_*}）。
 */
public class TestErpInvTransferOwnershipCostAdjustLandedCostStateMachines {

    // erp-inv/move-status 字典四态（TransferOrder/CostAdjust/LandedCost 共享）
    private static final List<String> ALL_MOVE_STATUSES = Arrays.asList(
            ErpInvDocStatus.DOC_STATUS_DRAFT,
            ErpInvDocStatus.DOC_STATUS_CONFIRMED,
            ErpInvDocStatus.DOC_STATUS_DONE,
            ErpInvDocStatus.DOC_STATUS_CANCELLED);

    // erp-inv/ownership-transfer-status 字典四态（OwnershipTransfer 独立 dict，值相同）
    private static final List<String> ALL_OWNERSHIP_TRANSFER_STATUSES = Arrays.asList(
            ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DRAFT,
            ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CONFIRMED,
            ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DONE,
            ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CANCELLED);

    private final ErpInvTransferOrderStateMachine transferOrderSm = new ErpInvTransferOrderStateMachine();
    private final ErpInvOwnershipTransferStateMachine ownershipTransferSm = new ErpInvOwnershipTransferStateMachine();
    private final ErpInvCostAdjustStateMachine costAdjustSm = new ErpInvCostAdjustStateMachine();
    private final ErpInvLandedCostStateMachine landedCostSm = new ErpInvLandedCostStateMachine();

    // ==================== 调拨单 docStatus 轴（单边） ====================

    @Test
    public void transferOrderNoDuplicateOrConflictingEdges() {
        List<ErpInvTransferOrderStateMachine.TransitionDefinition> edges = transferOrderSm.transitions();
        assertNoDuplicateEdges(edges, "TransferOrder");
        assertEquals(1, edges.size(), "迁移矩阵应有 1 条边（仅 confirm 单边）");
    }

    @Test
    public void transferOrderConfirmAllowsOnlyDraft() {
        for (String s : ALL_MOVE_STATUSES) {
            if (ErpInvDocStatus.DOC_STATUS_DRAFT.equals(s)) {
                transferOrderSm.assertCanConfirm(s); // 合法不抛
            } else {
                assertIllegalCommon(transferOrderSm, "confirm", s);
            }
        }
    }

    @Test
    public void transferOrderTerminalAndInitialSets() {
        assertEquals(Collections.singletonList(ErpInvDocStatus.DOC_STATUS_CONFIRMED),
                transferOrderSm.terminalStatuses(), "终态集合 = {CONFIRMED}（无后续出边，物理移动归独立 StockMove 流）");
        assertEquals(Collections.singletonList(ErpInvDocStatus.DOC_STATUS_DRAFT),
                transferOrderSm.initialStatuses(), "初始态集合 = {DRAFT}");
        assertTrue(transferOrderSm.isTerminal(ErpInvDocStatus.DOC_STATUS_CONFIRMED));
        assertFalse(transferOrderSm.isTerminal(ErpInvDocStatus.DOC_STATUS_DRAFT));
        assertFalse(transferOrderSm.isTerminal(ErpInvDocStatus.DOC_STATUS_DONE));
        assertFalse(transferOrderSm.isTerminal(ErpInvDocStatus.DOC_STATUS_CANCELLED));
    }

    /** CONFIRMED 为真终态（无出边——TransferOrder 无 DONE/CANCELLED writer，cancel/complete 生命周期 out-of-scope）。 */
    @Test
    public void transferOrderTerminalsAreTrueTerminals() {
        for (ErpInvTransferOrderStateMachine.TransitionDefinition e : transferOrderSm.transitions()) {
            assertFalse(transferOrderSm.isTerminal(e.getFromStatus()),
                    "真终态不应有出边: but edge " + e.getAction() + " leaves " + e.getFromStatus());
        }
        assertEquals("confirm", transferOrderSm.transitions().get(0).getAction());
        assertEquals(ErpInvDocStatus.DOC_STATUS_CONFIRMED, transferOrderSm.confirmTargetStatus());
    }

    @Test
    public void transferOrderDictValuesReachability() {
        for (String s : ALL_MOVE_STATUSES) {
            if (ErpInvDocStatus.DOC_STATUS_DRAFT.equals(s)) {
                assertTrue(transferOrderSm.initialStatuses().contains(s), "DRAFT 为初始态（seed 写入）");
            } else if (ErpInvDocStatus.DOC_STATUS_CONFIRMED.equals(s)) {
                boolean written = transferOrderSm.transitions().stream().anyMatch(e -> s.equals(e.getToStatus()));
                assertTrue(written, "CONFIRMED 应有 writer 可达（confirm 边）");
            } else {
                // DONE/CANCELLED 无 writer：调拨单生命周期止于 CONFIRMED，后续物理移动是独立 StockMove 流
                boolean written = transferOrderSm.transitions().stream().anyMatch(e -> s.equals(e.getToStatus()));
                assertFalse(written, "DONE/CANCELLED 无 Bean writer（TransferOrder 单边矩阵，非死状态漂移）");
            }
        }
    }

    // ==================== 所有权转移单 docStatus 轴（3 动作，独立 dict） ====================

    @Test
    public void ownershipTransferNoDuplicateOrConflictingEdges() {
        List<ErpInvOwnershipTransferStateMachine.TransitionDefinition> edges = ownershipTransferSm.transitions();
        assertNoDuplicateEdges(edges, "OwnershipTransfer");
        assertEquals(4, edges.size(), "迁移矩阵应有 4 条边（confirm 1 + done 1 + cancel 2 来源）");
    }

    @Test
    public void ownershipTransferReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DRAFT,
                ownershipTransferSm.transitions().stream()
                        .map(e -> new String[]{e.getFromStatus(), e.getToStatus()})
                        .collect(Collectors.toList()));
        assertTrue(reachable.contains(ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CONFIRMED), "从 DRAFT 应可达 CONFIRMED");
        assertTrue(reachable.contains(ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DONE), "从 DRAFT 应可达 DONE");
        assertTrue(reachable.contains(ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CANCELLED), "从 DRAFT 应可达 CANCELLED");
    }

    @Test
    public void ownershipTransferConfirmAllowsOnlyDraft() {
        for (String s : ALL_OWNERSHIP_TRANSFER_STATUSES) {
            if (ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DRAFT.equals(s)) {
                ownershipTransferSm.assertCanConfirm(s);
            } else {
                assertIllegalCommon(ownershipTransferSm, "confirm", s);
            }
        }
    }

    @Test
    public void ownershipTransferDoneAllowsOnlyConfirmed() {
        for (String s : ALL_OWNERSHIP_TRANSFER_STATUSES) {
            if (ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CONFIRMED.equals(s)) {
                ownershipTransferSm.assertCanDone(s);
            } else {
                assertIllegalCommon(ownershipTransferSm, "done", s);
            }
        }
    }

    @Test
    public void ownershipTransferCancelAllowsOnlyDraftOrConfirmed() {
        for (String s : ALL_OWNERSHIP_TRANSFER_STATUSES) {
            if (ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DRAFT.equals(s)
                    || ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CONFIRMED.equals(s)) {
                ownershipTransferSm.assertCanCancel(s); // 合法不抛
            } else {
                assertIllegalCommon(ownershipTransferSm, "cancel", s);
            }
        }
    }

    @Test
    public void ownershipTransferTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpInvOwnershipTransferStateMachine.TransitionDefinition e : ownershipTransferSm.transitions()) {
            invokeOwnershipTransferAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), ownershipTransferTargetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    @Test
    public void ownershipTransferTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DONE,
                        ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CANCELLED),
                ownershipTransferSm.terminalStatuses(), "终态集合 = {DONE, CANCELLED}");
        assertEquals(Collections.singletonList(ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DRAFT),
                ownershipTransferSm.initialStatuses(), "初始态集合 = {DRAFT}");
        assertTrue(ownershipTransferSm.isTerminal(ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DONE));
        assertTrue(ownershipTransferSm.isTerminal(ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CANCELLED));
        assertFalse(ownershipTransferSm.isTerminal(ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DRAFT));
        assertFalse(ownershipTransferSm.isTerminal(ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CONFIRMED));
    }

    /** DONE 与 CANCELLED 均为真终态（无出边）。 */
    @Test
    public void ownershipTransferTerminalsAreTrueTerminals() {
        for (ErpInvOwnershipTransferStateMachine.TransitionDefinition e : ownershipTransferSm.transitions()) {
            assertFalse(ownershipTransferSm.isTerminal(e.getFromStatus()),
                    "真终态不应有出边: but edge " + e.getAction() + " leaves " + e.getFromStatus());
        }
    }

    @Test
    public void ownershipTransferTargetStatusMethods() {
        assertEquals(ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CONFIRMED, ownershipTransferSm.confirmTargetStatus());
        assertEquals(ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DONE, ownershipTransferSm.doneTargetStatus());
        assertEquals(ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CANCELLED, ownershipTransferSm.cancelTargetStatus());
    }

    /** 独立 dict 语义核对：常量取 OWNERSHIP_TRANSFER_STATUS_*（非 DOC_STATUS_*），dict 值相同。 */
    @Test
    public void ownershipTransferIndependentDictSemantics() {
        assertEquals(ALL_MOVE_STATUSES, ALL_OWNERSHIP_TRANSFER_STATUSES,
                "独立 dict erp-inv/ownership-transfer-status 值与 move-status 相同");
        assertEquals(ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DRAFT, ErpInvDocStatus.DOC_STATUS_DRAFT);
        assertEquals(ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CONFIRMED, ErpInvDocStatus.DOC_STATUS_CONFIRMED);
        assertEquals(ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DONE, ErpInvDocStatus.DOC_STATUS_DONE);
        assertEquals(ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CANCELLED, ErpInvDocStatus.DOC_STATUS_CANCELLED);
    }

    @Test
    public void ownershipTransferAllDictValuesReachable() {
        for (String s : ALL_OWNERSHIP_TRANSFER_STATUSES) {
            if (ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DRAFT.equals(s)) {
                assertTrue(ownershipTransferSm.initialStatuses().contains(s), "DRAFT 为初始态（seed 写入）");
            } else {
                boolean written = ownershipTransferSm.transitions().stream().anyMatch(e -> s.equals(e.getToStatus()));
                assertTrue(written, "dict 值应有 writer 可达: " + s);
            }
        }
    }

    // ==================== 成本调整单 docStatus 轴（2 动作，approveStatus 轴不在 Bean） ====================

    @Test
    public void costAdjustNoDuplicateOrConflictingEdges() {
        List<ErpInvCostAdjustStateMachine.TransitionDefinition> edges = costAdjustSm.transitions();
        assertNoDuplicateEdges(edges, "CostAdjust");
        assertEquals(3, edges.size(), "迁移矩阵应有 3 条边（applyCostAdjust 2 来源 + reverseCostAdjust 1）");
    }

    @Test
    public void costAdjustReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpInvDocStatus.DOC_STATUS_DRAFT,
                costAdjustSm.transitions().stream()
                        .map(e -> new String[]{e.getFromStatus(), e.getToStatus()})
                        .collect(Collectors.toList()));
        assertTrue(reachable.contains(ErpInvDocStatus.DOC_STATUS_CONFIRMED), "从 DRAFT 应可达 CONFIRMED（经 reverse）");
        assertTrue(reachable.contains(ErpInvDocStatus.DOC_STATUS_DONE), "从 DRAFT 应可达 DONE");
    }

    @Test
    public void costAdjustApplyAllowsOnlyDraftOrConfirmed() {
        for (String s : ALL_MOVE_STATUSES) {
            if (ErpInvDocStatus.DOC_STATUS_DRAFT.equals(s) || ErpInvDocStatus.DOC_STATUS_CONFIRMED.equals(s)) {
                costAdjustSm.assertCanApplyCostAdjust(s); // 合法不抛
            } else {
                assertIllegalCommon(costAdjustSm, "applyCostAdjust", s);
            }
        }
    }

    @Test
    public void costAdjustReverseAllowsOnlyDone() {
        for (String s : ALL_MOVE_STATUSES) {
            if (ErpInvDocStatus.DOC_STATUS_DONE.equals(s)) {
                costAdjustSm.assertCanReverseCostAdjust(s);
            } else {
                assertIllegalCommon(costAdjustSm, "reverseCostAdjust", s);
            }
        }
    }

    @Test
    public void costAdjustTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpInvCostAdjustStateMachine.TransitionDefinition e : costAdjustSm.transitions()) {
            invokeCostAdjustAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), costAdjustTargetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    @Test
    public void costAdjustTerminalAndInitialSets() {
        assertEquals(Collections.singletonList(ErpInvDocStatus.DOC_STATUS_DONE),
                costAdjustSm.terminalStatuses(), "终态集合 = {DONE}（CONFIRMED 可逆非终态）");
        assertEquals(Collections.singletonList(ErpInvDocStatus.DOC_STATUS_DRAFT),
                costAdjustSm.initialStatuses(), "初始态集合 = {DRAFT}");
        assertTrue(costAdjustSm.isTerminal(ErpInvDocStatus.DOC_STATUS_DONE));
        assertFalse(costAdjustSm.isTerminal(ErpInvDocStatus.DOC_STATUS_DRAFT));
        assertFalse(costAdjustSm.isTerminal(ErpInvDocStatus.DOC_STATUS_CONFIRMED));
    }

    /**
     * 终态出边核对：DONE 为计划定义终态（terminal={DONE}）但<b>有且仅有 reverseCostAdjust 出边</b>
     * （红冲逆转为可逆终态，CONFIRMED 可 re-apply——与 StockMove「DONE 冲销=生成新单非状态回退」不同，
     * CostAdjust 红冲是 DONE→CONFIRMED 状态回退）；CANCELLED（跨实体编排写）在矩阵中无出边。
     */
    @Test
    public void costAdjustTerminalsAreTrueTerminals() {
        List<ErpInvCostAdjustStateMachine.TransitionDefinition> edges = costAdjustSm.transitions();
        for (ErpInvCostAdjustStateMachine.TransitionDefinition e : edges) {
            if (ErpInvDocStatus.DOC_STATUS_DONE.equals(e.getFromStatus())) {
                assertEquals("reverseCostAdjust", e.getAction(),
                        "DONE 唯一出边应为 reverseCostAdjust（红冲逆转）");
                assertEquals(ErpInvDocStatus.DOC_STATUS_CONFIRMED, e.getToStatus());
            } else {
                assertFalse(costAdjustSm.isTerminal(e.getFromStatus()),
                        "非 DONE 来源不应是终态: edge " + e.getAction() + " leaves " + e.getFromStatus());
            }
        }
        // CANCELLED 无 Bean 出边（跨实体编排写，矩阵不含 CANCELLED 边）
        boolean cancelledHasOutEdge = edges.stream().anyMatch(e -> ErpInvDocStatus.DOC_STATUS_CANCELLED.equals(e.getFromStatus()));
        assertFalse(cancelledHasOutEdge, "CANCELLED 在 Bean 矩阵中应无出边");
    }

    @Test
    public void costAdjustTargetStatusMethods() {
        assertEquals(ErpInvDocStatus.DOC_STATUS_DONE, costAdjustSm.applyCostAdjustTargetStatus());
        assertEquals(ErpInvDocStatus.DOC_STATUS_CONFIRMED, costAdjustSm.reverseCostAdjustTargetStatus());
    }

    /**
     * dict 值可达性：DRAFT=初始 seed；CONFIRMED 经 reverseCostAdjust 可达；DONE 经 apply 可达；
     * CANCELLED 无 Bean writer——仅由跨实体内部编排（LandedCost facade 写子 CostAdjust docStatus）写入，
     * 属容忍的外部 writer（契约 §9.2 内部编排，非 Bean 边、非死状态漂移）。
     */
    @Test
    public void costAdjustDictValuesReachability() {
        for (String s : ALL_MOVE_STATUSES) {
            if (ErpInvDocStatus.DOC_STATUS_DRAFT.equals(s)) {
                assertTrue(costAdjustSm.initialStatuses().contains(s), "DRAFT 为初始态（seed 写入）");
            } else if (ErpInvDocStatus.DOC_STATUS_CANCELLED.equals(s)) {
                boolean written = costAdjustSm.transitions().stream().anyMatch(e -> s.equals(e.getToStatus()));
                assertFalse(written, "CANCELLED 无 Bean writer（跨实体内部编排写，Bean 容忍）");
            } else {
                boolean written = costAdjustSm.transitions().stream().anyMatch(e -> s.equals(e.getToStatus()));
                assertTrue(written, "dict 值应有 writer 可达: " + s);
            }
        }
    }

    // ==================== 到岸成本单 docStatus 轴（2 动作，双轴联动中 Bean 仅 docStatus 边） ====================

    @Test
    public void landedCostNoDuplicateOrConflictingEdges() {
        List<ErpInvLandedCostStateMachine.TransitionDefinition> edges = landedCostSm.transitions();
        assertNoDuplicateEdges(edges, "LandedCost");
        assertEquals(2, edges.size(), "迁移矩阵应有 2 条边（approve 1 + reverseApprove 1）");
    }

    @Test
    public void landedCostReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpInvDocStatus.DOC_STATUS_DRAFT,
                landedCostSm.transitions().stream()
                        .map(e -> new String[]{e.getFromStatus(), e.getToStatus()})
                        .collect(Collectors.toList()));
        assertTrue(reachable.contains(ErpInvDocStatus.DOC_STATUS_DONE), "从 DRAFT 应可达 DONE");
        assertTrue(reachable.contains(ErpInvDocStatus.DOC_STATUS_CANCELLED), "从 DRAFT 应可达 CANCELLED");
    }

    @Test
    public void landedCostApproveAllowsOnlyDraft() {
        for (String s : ALL_MOVE_STATUSES) {
            if (ErpInvDocStatus.DOC_STATUS_DRAFT.equals(s)) {
                landedCostSm.assertCanApprove(s); // 合法不抛（无 CONFIRMED 写，DRAFT→DONE 直达）
            } else {
                assertIllegalCommon(landedCostSm, "approve", s);
            }
        }
    }

    @Test
    public void landedCostReverseApproveAllowsOnlyDone() {
        for (String s : ALL_MOVE_STATUSES) {
            if (ErpInvDocStatus.DOC_STATUS_DONE.equals(s)) {
                landedCostSm.assertCanReverseApprove(s);
            } else {
                assertIllegalCommon(landedCostSm, "reverseApprove", s);
            }
        }
    }

    @Test
    public void landedCostTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpInvLandedCostStateMachine.TransitionDefinition e : landedCostSm.transitions()) {
            invokeLandedCostAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), landedCostTargetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    @Test
    public void landedCostTerminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpInvDocStatus.DOC_STATUS_DONE, ErpInvDocStatus.DOC_STATUS_CANCELLED),
                landedCostSm.terminalStatuses(), "终态集合 = {DONE, CANCELLED}");
        assertEquals(Collections.singletonList(ErpInvDocStatus.DOC_STATUS_DRAFT),
                landedCostSm.initialStatuses(), "初始态集合 = {DRAFT}");
        assertTrue(landedCostSm.isTerminal(ErpInvDocStatus.DOC_STATUS_DONE));
        assertTrue(landedCostSm.isTerminal(ErpInvDocStatus.DOC_STATUS_CANCELLED));
        assertFalse(landedCostSm.isTerminal(ErpInvDocStatus.DOC_STATUS_DRAFT));
        assertFalse(landedCostSm.isTerminal(ErpInvDocStatus.DOC_STATUS_CONFIRMED));
    }

    /**
     * 终态出边核对：DONE 为计划定义终态（terminal={DONE, CANCELLED}）但<b>有且仅有 reverseApprove 出边</b>
     * （红冲 DONE→CANCELLED 状态回退）；CANCELLED 在矩阵中无出边。
     */
    @Test
    public void landedCostTerminalsAreTrueTerminals() {
        List<ErpInvLandedCostStateMachine.TransitionDefinition> edges = landedCostSm.transitions();
        for (ErpInvLandedCostStateMachine.TransitionDefinition e : edges) {
            if (ErpInvDocStatus.DOC_STATUS_DONE.equals(e.getFromStatus())) {
                assertEquals("reverseApprove", e.getAction(),
                        "DONE 唯一出边应为 reverseApprove（红冲）");
                assertEquals(ErpInvDocStatus.DOC_STATUS_CANCELLED, e.getToStatus());
            } else {
                assertFalse(landedCostSm.isTerminal(e.getFromStatus()),
                        "非 DONE 来源不应是终态: edge " + e.getAction() + " leaves " + e.getFromStatus());
            }
        }
        boolean cancelledHasOutEdge = edges.stream().anyMatch(e -> ErpInvDocStatus.DOC_STATUS_CANCELLED.equals(e.getFromStatus()));
        assertFalse(cancelledHasOutEdge, "CANCELLED 在 Bean 矩阵中应无出边");
    }

    @Test
    public void landedCostTargetStatusMethods() {
        assertEquals(ErpInvDocStatus.DOC_STATUS_DONE, landedCostSm.approveTargetStatus());
        assertEquals(ErpInvDocStatus.DOC_STATUS_CANCELLED, landedCostSm.reverseApproveTargetStatus());
    }

    /**
     * dict 值可达性：DRAFT=初始 seed；DONE 经 approve 可达；CANCELLED 经 reverseApprove 可达；
     * CONFIRMED 无 Bean writer（LandedCost 无 CONFIRMED 写，DRAFT→DONE 直达——非死状态漂移）。
     */
    @Test
    public void landedCostDictValuesReachability() {
        for (String s : ALL_MOVE_STATUSES) {
            if (ErpInvDocStatus.DOC_STATUS_DRAFT.equals(s)) {
                assertTrue(landedCostSm.initialStatuses().contains(s), "DRAFT 为初始态（seed 写入）");
            } else if (ErpInvDocStatus.DOC_STATUS_CONFIRMED.equals(s)) {
                boolean written = landedCostSm.transitions().stream().anyMatch(e -> s.equals(e.getToStatus()));
                assertFalse(written, "CONFIRMED 无 Bean writer（到岸成本无 CONFIRMED 写）");
            } else {
                boolean written = landedCostSm.transitions().stream().anyMatch(e -> s.equals(e.getToStatus()));
                assertTrue(written, "dict 值应有 writer 可达: " + s);
            }
        }
    }

    // ==================== helpers ====================

    private static void assertNoDuplicateEdges(List<?> edges, String label) {
        Set<String> seen = new HashSet<>();
        for (Object e : edges) {
            String action;
            String from;
            if (e instanceof ErpInvTransferOrderStateMachine.TransitionDefinition) {
                ErpInvTransferOrderStateMachine.TransitionDefinition t = (ErpInvTransferOrderStateMachine.TransitionDefinition) e;
                action = t.getAction();
                from = t.getFromStatus();
            } else if (e instanceof ErpInvOwnershipTransferStateMachine.TransitionDefinition) {
                ErpInvOwnershipTransferStateMachine.TransitionDefinition t = (ErpInvOwnershipTransferStateMachine.TransitionDefinition) e;
                action = t.getAction();
                from = t.getFromStatus();
            } else if (e instanceof ErpInvCostAdjustStateMachine.TransitionDefinition) {
                ErpInvCostAdjustStateMachine.TransitionDefinition t = (ErpInvCostAdjustStateMachine.TransitionDefinition) e;
                action = t.getAction();
                from = t.getFromStatus();
            } else {
                ErpInvLandedCostStateMachine.TransitionDefinition t = (ErpInvLandedCostStateMachine.TransitionDefinition) e;
                action = t.getAction();
                from = t.getFromStatus();
            }
            String key = action + "|" + from;
            assertTrue(seen.add(key), "重复/冲突边: " + label + " action=" + action + ", fromStatus=" + from);
        }
    }

    private static void assertIllegalCommon(Object bean, String action, String status) {
        NopException ex = assertThrows(NopException.class, () -> invokeAssert(bean, action, status),
                action + " 对非允许来源态应非法: " + status);
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + status);
        assertEquals(action, ex.getParam(ErpInvTransferOrderStateMachine.ARG_ACTION));
        assertEquals(status, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
    }

    private static void invokeAssert(Object bean, String action, String status) {
        if (bean instanceof ErpInvTransferOrderStateMachine) {
            ErpInvTransferOrderStateMachine sm = (ErpInvTransferOrderStateMachine) bean;
            switch (action) {
                case "confirm":
                    sm.assertCanConfirm(status);
                    return;
                default:
                    throw new IllegalArgumentException("unknown action: " + action);
            }
        }
        if (bean instanceof ErpInvOwnershipTransferStateMachine) {
            ErpInvOwnershipTransferStateMachine sm = (ErpInvOwnershipTransferStateMachine) bean;
            switch (action) {
                case "confirm":
                    sm.assertCanConfirm(status);
                    return;
                case "done":
                    sm.assertCanDone(status);
                    return;
                case "cancel":
                    sm.assertCanCancel(status);
                    return;
                default:
                    throw new IllegalArgumentException("unknown action: " + action);
            }
        }
        if (bean instanceof ErpInvCostAdjustStateMachine) {
            ErpInvCostAdjustStateMachine sm = (ErpInvCostAdjustStateMachine) bean;
            switch (action) {
                case "applyCostAdjust":
                    sm.assertCanApplyCostAdjust(status);
                    return;
                case "reverseCostAdjust":
                    sm.assertCanReverseCostAdjust(status);
                    return;
                default:
                    throw new IllegalArgumentException("unknown action: " + action);
            }
        }
        ErpInvLandedCostStateMachine sm = (ErpInvLandedCostStateMachine) bean;
        switch (action) {
            case "approve":
                sm.assertCanApprove(status);
                return;
            case "reverseApprove":
                sm.assertCanReverseApprove(status);
                return;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private void invokeOwnershipTransferAssert(String action, String status) {
        invokeAssert(ownershipTransferSm, action, status);
    }

    private String ownershipTransferTargetStatusFor(String action) {
        switch (action) {
            case "confirm":
                return ownershipTransferSm.confirmTargetStatus();
            case "done":
                return ownershipTransferSm.doneTargetStatus();
            case "cancel":
                return ownershipTransferSm.cancelTargetStatus();
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private void invokeCostAdjustAssert(String action, String status) {
        invokeAssert(costAdjustSm, action, status);
    }

    private String costAdjustTargetStatusFor(String action) {
        switch (action) {
            case "applyCostAdjust":
                return costAdjustSm.applyCostAdjustTargetStatus();
            case "reverseCostAdjust":
                return costAdjustSm.reverseCostAdjustTargetStatus();
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private void invokeLandedCostAssert(String action, String status) {
        invokeAssert(landedCostSm, action, status);
    }

    private String landedCostTargetStatusFor(String action) {
        switch (action) {
            case "approve":
                return landedCostSm.approveTargetStatus();
            case "reverseApprove":
                return landedCostSm.reverseApproveTargetStatus();
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private static Set<String> reachableFrom(String start, List<String[]> edges) {
        Set<String> visited = new LinkedHashSet<>();
        List<String> frontier = new ArrayList<>();
        frontier.add(start);
        while (!frontier.isEmpty()) {
            String cur = frontier.remove(0);
            if (!visited.add(cur)) {
                continue;
            }
            for (String[] e : edges) {
                if (e[0].equals(cur) && !visited.contains(e[1])) {
                    frontier.add(e[1]);
                }
            }
        }
        return visited.stream()
                .filter(s -> !s.equals(start))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
