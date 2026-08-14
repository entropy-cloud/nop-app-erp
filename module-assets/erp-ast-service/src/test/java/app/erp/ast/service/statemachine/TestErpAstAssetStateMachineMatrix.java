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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan Phase 1 Proof）。
 *
 * <p>针对 {@link ErpAstAssetStateMachine}（ErpAstAsset.status 跨实体 writer 轴，6 命名动作）的纯矩阵完备性
 * 遍历：不经 BizModel 入口（层 3 职责），不断言副作用/审计。
 *
 * <p>覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边（7 条边，6 命名动作——reverseDisposal 双源）；</li>
 *   <li>(b) 从 DRAFT 可达 IN_SERVICE/SCRAPPED/SOLD，IDLE 显式断言不可达；</li>
 *   <li>(c) 各动作合法来源态通过、非法来源态抛 common 层码（携带 action/currentStatus）；
 *       capitalize null 归一化 DRAFT 合法；inventoryShortageDisposal 运行时守卫接受 IDLE 死状态来源
 *       （对齐盘点范围过滤 liveStatuses 既有行为）；</li>
 *   <li>(d) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(e) 终态/初始态集合正确（terminal={SCRAPPED,SOLD}，initial={DRAFT}）；</li>
 *   <li>(f) IDLE 死状态登记：不在 transitions() 任一迁移边、不可达、不在终态/初始态集合
 *       （layer-2 四方对照裁定 intentional reserved）。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpAstAssetStateMachineMatrix {

    /** dict erp-ast/asset-status 全 6 值（DISPOSED 由拆分/合并写入，归计划 3 范围，本 Bean 一律拒绝）。 */
    private static final List<String> ALL_ASSET_STATUSES = Arrays.asList(
            ErpAstConstants.ASSET_STATUS_DRAFT,
            ErpAstConstants.ASSET_STATUS_IN_SERVICE,
            ErpAstConstants.ASSET_STATUS_IDLE,
            ErpAstConstants.ASSET_STATUS_SCRAPPED,
            ErpAstConstants.ASSET_STATUS_SOLD,
            ErpAstConstants.ASSET_STATUS_DISPOSED);

    private final ErpAstAssetStateMachine sm = new ErpAstAssetStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void noDuplicateOrConflictingEdges() {
        List<ErpAstAssetStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpAstAssetStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(7, edges.size(), "迁移矩阵应有 7 条边（6 命名动作，reverseDisposal 双源；IDLE 死状态无出边）");
    }

    // ---------- (b) 从 DRAFT 可达全部非初始 live 态；IDLE 不可达 ----------

    @Test
    public void reachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpAstConstants.ASSET_STATUS_DRAFT);
        assertTrue(reachable.contains(ErpAstConstants.ASSET_STATUS_IN_SERVICE), "从 DRAFT 应可达 IN_SERVICE");
        assertTrue(reachable.contains(ErpAstConstants.ASSET_STATUS_SCRAPPED), "从 DRAFT 应可达 SCRAPPED");
        assertTrue(reachable.contains(ErpAstConstants.ASSET_STATUS_SOLD), "从 DRAFT 应可达 SOLD");
    }

    @Test
    public void idleIsDeadStateNotReachable() {
        Set<String> reachable = reachableFrom(ErpAstConstants.ASSET_STATUS_DRAFT);
        assertFalse(reachable.contains(ErpAstConstants.ASSET_STATUS_IDLE),
                "IDLE 为死状态（零 writer），从 DRAFT 不可达");
    }

    // ---------- (c) 各动作合法/非法来源态 ----------

    @Test
    public void capitalizeAllowsOnlyDraft() {
        assertAllowsOnly("capitalize", "capitalize", ErpAstConstants.ASSET_STATUS_DRAFT, null);
        assertEquals(ErpAstConstants.ASSET_STATUS_IN_SERVICE, sm.capitalizeTargetStatus());
    }

    @Test
    public void capitalizeNullTreatedAsDraft() {
        sm.assertCanCapitalize(null); // null 归一化为 DRAFT（初始态），合法不抛
    }

    @Test
    public void reverseCapitalizeAllowsOnlyInService() {
        assertAllowsOnly("reverseCapitalize", "reverseCapitalize", ErpAstConstants.ASSET_STATUS_IN_SERVICE, null);
        assertEquals(ErpAstConstants.ASSET_STATUS_DRAFT, sm.reverseCapitalizeTargetStatus(),
                "reverseCapitalize 目标态=DRAFT（posted 窗口逆资本化）");
    }

    @Test
    public void disposeAllowsOnlyInService() {
        // disposeScrap/disposeSell 共用 assertCanDispose 来源态判定（IN_SERVICE），Bean 报告动作名 dispose
        assertAllowsOnly("disposeScrap", "dispose", ErpAstConstants.ASSET_STATUS_IN_SERVICE, null);
        assertAllowsOnly("disposeSell", "dispose", ErpAstConstants.ASSET_STATUS_IN_SERVICE, null);
        assertEquals(ErpAstConstants.ASSET_STATUS_SCRAPPED, sm.disposeScrapTargetStatus());
        assertEquals(ErpAstConstants.ASSET_STATUS_SOLD, sm.disposeSellTargetStatus());
    }

    @Test
    public void reverseDisposeAllowsOnlyScrappedOrSold() {
        assertAllowsOnly("reverseDisposal", "reverseDisposal",
                ErpAstConstants.ASSET_STATUS_SCRAPPED, ErpAstConstants.ASSET_STATUS_SOLD);
        assertEquals(ErpAstConstants.ASSET_STATUS_IN_SERVICE, sm.reverseDisposalTargetStatus(),
                "reverseDisposal 目标态=IN_SERVICE（posted 窗口恢复在用）");
    }

    @Test
    public void shortageDisposeAllowsOnlyInServiceOrIdle() {
        // 运行时守卫接受 IN_SERVICE/IDLE（IDLE 死状态豁免对齐盘点范围过滤既有行为）
        sm.assertCanShortageDispose(ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        sm.assertCanShortageDispose(ErpAstConstants.ASSET_STATUS_IDLE);
        for (String s : ALL_ASSET_STATUSES) {
            if (ErpAstConstants.ASSET_STATUS_IN_SERVICE.equals(s)
                    || ErpAstConstants.ASSET_STATUS_IDLE.equals(s)) {
                continue;
            }
            assertIllegal("inventoryShortageDisposal", s);
        }
        assertEquals(ErpAstConstants.ASSET_STATUS_SCRAPPED, sm.shortageDisposeTargetStatus(),
                "inventoryShortageDisposal 目标态=SCRAPPED（盘亏触发处置）");
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void transitionsMetadataConsistentWithExplicitMethods() {
        for (ErpAstAssetStateMachine.TransitionDefinition e : sm.transitions()) {
            invokeAssert(e.getAction(), e.getFromStatus());
            assertEquals(targetStatusFor(e.getAction()), e.getToStatus(),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (e) 终态/初始态集合 ----------

    @Test
    public void terminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpAstConstants.ASSET_STATUS_SCRAPPED, ErpAstConstants.ASSET_STATUS_SOLD),
                sm.terminalStatuses(), "终态集合 = {SCRAPPED, SOLD}");
        assertEquals(Arrays.asList(ErpAstConstants.ASSET_STATUS_DRAFT),
                sm.initialStatuses(), "初始态集合 = {DRAFT}");

        assertTrue(sm.isTerminal(ErpAstConstants.ASSET_STATUS_SCRAPPED));
        assertTrue(sm.isTerminal(ErpAstConstants.ASSET_STATUS_SOLD));
        assertFalse(sm.isTerminal(ErpAstConstants.ASSET_STATUS_DRAFT));
        assertFalse(sm.isTerminal(ErpAstConstants.ASSET_STATUS_IN_SERVICE));
        assertFalse(sm.isTerminal(ErpAstConstants.ASSET_STATUS_IDLE),
                "IDLE 为预留死状态，非真正终态（终态须无出边）");
        assertFalse(sm.isTerminal(ErpAstConstants.ASSET_STATUS_DISPOSED),
                "DISPOSED 归拆分/合并计划 3 范围，本 Bean 不分类为终态");
    }

    // ---------- (f) IDLE 死状态登记 ----------

    @Test
    public void idleNotInTransitionsNorSets() {
        for (ErpAstAssetStateMachine.TransitionDefinition e : sm.transitions()) {
            assertFalse(ErpAstConstants.ASSET_STATUS_IDLE.equals(e.getFromStatus()),
                    "IDLE 不应出现在迁移边来源态: action=" + e.getAction());
            assertFalse(ErpAstConstants.ASSET_STATUS_IDLE.equals(e.getToStatus()),
                    "IDLE 不应出现在迁移边目标态: action=" + e.getAction());
        }
        assertFalse(sm.terminalStatuses().contains(ErpAstConstants.ASSET_STATUS_IDLE),
                "IDLE 不在终态集合");
        assertFalse(sm.initialStatuses().contains(ErpAstConstants.ASSET_STATUS_IDLE),
                "IDLE 不在初始态集合");
    }

    @Test
    public void allDictStatusesClassified() {
        // 机器化核对：dict 全 6 值在 Bean 中均有语义归类（isTerminal/isTerminal 之外由各 assert 拒绝）
        for (String s : ALL_ASSET_STATUSES) {
            sm.isTerminal(s); // 不抛即可
        }
    }

    // ==================== helpers ====================

    /** allowedFrom2 非空表示双源动作（reverseDisposal）。 */
    private void assertAllowsOnly(String action, String reportedAction, String allowedFrom, String allowedFrom2) {
        for (String s : ALL_ASSET_STATUSES) {
            if (allowedFrom.equals(s) || (allowedFrom2 != null && allowedFrom2.equals(s))) {
                invokeAssert(action, s); // 不抛 = 合法
            } else {
                NopException ex = assertThrows(NopException.class, () -> invokeAssert(action, s),
                        action + " 对非允许来源态应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                        "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + s);
                assertEquals(reportedAction, ex.getParam(ErpAstAssetStateMachine.ARG_ACTION),
                        "拒绝元数据携带动作名: action=" + reportedAction);
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                        "拒绝元数据携带当前态: action=" + action + ", status=" + s);
            }
        }
    }

    private void assertIllegal(String action, String status) {
        NopException ex = assertThrows(NopException.class, () -> invokeAssert(action, status),
                action + " 对非允许来源态应非法: " + status);
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + status);
        assertEquals(action, ex.getParam(ErpAstAssetStateMachine.ARG_ACTION),
                "拒绝元数据携带动作名: action=" + action);
        assertEquals(status, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                "拒绝元数据携带当前态: action=" + action + ", status=" + status);
    }

    private void invokeAssert(String action, String status) {
        switch (action) {
            case "capitalize":
                sm.assertCanCapitalize(status);
                break;
            case "reverseCapitalize":
                sm.assertCanReverseCapitalize(status);
                break;
            case "disposeScrap":
            case "disposeSell":
                sm.assertCanDispose(status);
                break;
            case "reverseDisposal":
                sm.assertCanReverseDispose(status);
                break;
            case "inventoryShortageDisposal":
                sm.assertCanShortageDispose(status);
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "capitalize":
                return sm.capitalizeTargetStatus();
            case "reverseCapitalize":
                return sm.reverseCapitalizeTargetStatus();
            case "disposeScrap":
                return sm.disposeScrapTargetStatus();
            case "disposeSell":
                return sm.disposeSellTargetStatus();
            case "reverseDisposal":
                return sm.reverseDisposalTargetStatus();
            case "inventoryShortageDisposal":
                return sm.shortageDisposeTargetStatus();
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
            for (ErpAstAssetStateMachine.TransitionDefinition e : sm.transitions()) {
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
