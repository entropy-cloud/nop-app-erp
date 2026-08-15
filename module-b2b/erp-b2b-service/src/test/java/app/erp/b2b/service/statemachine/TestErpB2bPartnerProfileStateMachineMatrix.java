package app.erp.b2b.service.statemachine;

import app.erp.b2b.service.ErpB2bConstants;
import app.erp.common.service.ErpCommonErrors;
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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-15-1023-3 Phase 4）。
 *
 * <p>针对 {@link ErpB2bPartnerProfileStateMachine} Bean 的纯矩阵完备性遍历：不经 BizModel 入口（层 3 职责），
 * 不断言副作用（goLiveDate/archivedAt 回写归 TestErpB2bPartnerOnboarding）。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边（全三元组 action+fromStatus+toStatus 唯一，12 边）；</li>
 *   <li>(b) 终态 {TERMINATED} 无出边；</li>
 *   <li>(c) promoteToTesting 仅 REGISTERED 合法；</li>
 *   <li>(d) promoteToCertified 仅 TESTING 合法；</li>
 *   <li>(e) activate 仅 CERTIFIED 合法（业务规则 1 不可跳过阶段）；</li>
 *   <li>(f) suspend 四源 {REGISTERED, TESTING, CERTIFIED, PRODUCTION} 合法、SUSPENDED/TERMINATED 非法；</li>
 *   <li>(g) deactivate 任意非终态合法、TERMINATED 非法；</li>
 *   <li>(h) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(i) 初始/终态集合正确。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpB2bPartnerProfileStateMachineMatrix {

    private static final List<String> ALL_STATES = Arrays.asList(
            ErpB2bConstants.PARTNER_STATUS_REGISTERED,
            ErpB2bConstants.PARTNER_STATUS_TESTING,
            ErpB2bConstants.PARTNER_STATUS_CERTIFIED,
            ErpB2bConstants.PARTNER_STATUS_PRODUCTION,
            ErpB2bConstants.PARTNER_STATUS_SUSPENDED,
            ErpB2bConstants.PARTNER_STATUS_TERMINATED);

    private final ErpB2bPartnerProfileStateMachine sm = new ErpB2bPartnerProfileStateMachine();

    // ---------- (a) 无重复/冲突边（全三元组唯一） ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpB2bPartnerProfileStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpB2bPartnerProfileStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus() + "|" + e.getToStatus();
            assertTrue(seen.add(key), "重复/冲突边: " + key);
        }
        assertEquals(12, edges.size(),
                "迁移矩阵应有 12 条边（promoteToTesting 1 + promoteToCertified 1 + activate 1 + suspend 4 + deactivate 5）");
    }

    // ---------- (b) 终态无出边 ----------

    @Test
    public void testTerminalStatusesHaveNoOutgoingEdges() {
        for (String terminal : sm.terminalStatuses()) {
            for (ErpB2bPartnerProfileStateMachine.TransitionDefinition e : sm.transitions()) {
                assertFalse(e.getFromStatus().equals(terminal),
                        "终态不应有出边: terminal=" + terminal + ", but edge " + e.getAction() + " leaves it");
            }
        }
    }

    // ---------- (c) promoteToTesting 仅 REGISTERED 合法 ----------

    @Test
    public void testPromoteToTestingLegalOnlyFromRegistered() {
        sm.assertCanPromoteToTesting(ErpB2bConstants.PARTNER_STATUS_REGISTERED);
        assertEquals(ErpB2bConstants.PARTNER_STATUS_TESTING, sm.promoteToTestingTargetStatus());
        for (String state : ALL_STATES) {
            if (ErpB2bConstants.PARTNER_STATUS_REGISTERED.equals(state)) {
                continue;
            }
            assertThrows(NopException.class, () -> sm.assertCanPromoteToTesting(state),
                    "promoteToTesting 对非 REGISTERED 态应非法: " + state);
        }
    }

    // ---------- (d) promoteToCertified 仅 TESTING 合法 ----------

    @Test
    public void testPromoteToCertifiedLegalOnlyFromTesting() {
        sm.assertCanPromoteToCertified(ErpB2bConstants.PARTNER_STATUS_TESTING);
        assertEquals(ErpB2bConstants.PARTNER_STATUS_CERTIFIED, sm.promoteToCertifiedTargetStatus());
        for (String state : ALL_STATES) {
            if (ErpB2bConstants.PARTNER_STATUS_TESTING.equals(state)) {
                continue;
            }
            assertThrows(NopException.class, () -> sm.assertCanPromoteToCertified(state),
                    "promoteToCertified 对非 TESTING 态应非法: " + state);
        }
    }

    // ---------- (e) activate 仅 CERTIFIED 合法（不可跳过阶段） ----------

    @Test
    public void testActivateLegalOnlyFromCertified() {
        sm.assertCanActivate(ErpB2bConstants.PARTNER_STATUS_CERTIFIED);
        assertEquals(ErpB2bConstants.PARTNER_STATUS_PRODUCTION, sm.activateTargetStatus());
        for (String state : ALL_STATES) {
            if (ErpB2bConstants.PARTNER_STATUS_CERTIFIED.equals(state)) {
                continue;
            }
            assertThrows(NopException.class, () -> sm.assertCanActivate(state),
                    "activate 对非 CERTIFIED 态应非法（不可跳过 TESTING/CERTIFIED）: " + state);
        }
    }

    // ---------- (f) suspend 四源合法、SUSPENDED/TERMINATED 非法 ----------

    @Test
    public void testSuspendLegalFromFourSources() {
        for (String state : Arrays.asList(
                ErpB2bConstants.PARTNER_STATUS_REGISTERED,
                ErpB2bConstants.PARTNER_STATUS_TESTING,
                ErpB2bConstants.PARTNER_STATUS_CERTIFIED,
                ErpB2bConstants.PARTNER_STATUS_PRODUCTION)) {
            sm.assertCanSuspend(state);
        }
        assertEquals(ErpB2bConstants.PARTNER_STATUS_SUSPENDED, sm.suspendTargetStatus());
        assertThrows(NopException.class, () -> sm.assertCanSuspend(ErpB2bConstants.PARTNER_STATUS_SUSPENDED),
                "已暂停态再 suspend 应非法");
        assertThrows(NopException.class, () -> sm.assertCanSuspend(ErpB2bConstants.PARTNER_STATUS_TERMINATED),
                "终态 suspend 应非法");
    }

    // ---------- (g) deactivate 任意非终态合法、TERMINATED 非法 ----------

    @Test
    public void testDeactivateLegalFromAnyNonTerminal() {
        for (String state : ALL_STATES) {
            if (ErpB2bConstants.PARTNER_STATUS_TERMINATED.equals(state)) {
                assertThrows(NopException.class, () -> sm.assertCanDeactivate(state),
                        "终态 TERMINATED 再 deactivate 应非法");
            } else {
                sm.assertCanDeactivate(state);
            }
        }
        assertEquals(ErpB2bConstants.PARTNER_STATUS_TERMINATED, sm.deactivateTargetStatus());
    }

    // ---------- (h) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        Set<String> expected = new HashSet<>();
        expected.add("promoteToTesting|REGISTERED|TESTING");
        expected.add("promoteToCertified|TESTING|CERTIFIED");
        expected.add("activate|CERTIFIED|PRODUCTION");
        expected.add("suspend|REGISTERED|SUSPENDED");
        expected.add("suspend|TESTING|SUSPENDED");
        expected.add("suspend|CERTIFIED|SUSPENDED");
        expected.add("suspend|PRODUCTION|SUSPENDED");
        expected.add("deactivate|REGISTERED|TERMINATED");
        expected.add("deactivate|TESTING|TERMINATED");
        expected.add("deactivate|CERTIFIED|TERMINATED");
        expected.add("deactivate|PRODUCTION|TERMINATED");
        expected.add("deactivate|SUSPENDED|TERMINATED");
        Set<String> actual = new HashSet<>();
        for (ErpB2bPartnerProfileStateMachine.TransitionDefinition e : sm.transitions()) {
            actual.add(e.getAction() + "|" + e.getFromStatus() + "|" + e.getToStatus());
        }
        assertEquals(expected, actual, "transitions() 元数据与显式动作方法矩阵应完全一致");
    }

    // ---------- (i) 初始/终态集合正确 ----------

    @Test
    public void testInitialAndTerminalSets() {
        assertEquals(1, sm.initialStatuses().size(), "初始态应仅 REGISTERED");
        assertTrue(sm.initialStatuses().contains(ErpB2bConstants.PARTNER_STATUS_REGISTERED));
        assertEquals(1, sm.terminalStatuses().size(), "终态应仅 TERMINATED");
        assertTrue(sm.terminalStatuses().contains(ErpB2bConstants.PARTNER_STATUS_TERMINATED));
        assertTrue(sm.isTerminal(ErpB2bConstants.PARTNER_STATUS_TERMINATED));
        assertFalse(sm.isTerminal(ErpB2bConstants.PARTNER_STATUS_PRODUCTION));
    }

    // ---------- 非法迁移抛 common 层码（契约 §2/§7） ----------

    @Test
    public void testIllegalEdgeThrowsCommonErrorCode() {
        NopException e = assertThrows(NopException.class,
                () -> sm.assertCanActivate(ErpB2bConstants.PARTNER_STATUS_REGISTERED));
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), e.getErrorCode(),
                "Bean 应抛 common 层 ERR_ILLEGAL_STATUS_TRANSITION（领域映射归 BizModel）");
        assertEquals("activate", e.getParam(ErpB2bPartnerProfileStateMachine.ARG_ACTION), "拒绝元数据携带动作名");
        assertEquals(ErpB2bConstants.PARTNER_STATUS_REGISTERED, e.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                "拒绝元数据携带当前态");
    }
}
