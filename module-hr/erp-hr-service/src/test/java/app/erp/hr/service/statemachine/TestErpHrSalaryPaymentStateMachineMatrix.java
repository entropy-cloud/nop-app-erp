package app.erp.hr.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.hr.service.ErpHrConstants;
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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan Phase 1 Proof）。
 *
 * <p>针对 {@link ErpHrSalaryPaymentStateMachine} Bean（paymentStatus 发放执行独立轴）的纯矩阵完备性遍历：
 * 不经 BizModel 入口（层 3 职责），不断言副作用（SALARY_PAYMENT 过账/PAID 锁领域码）。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边（2 边：markPaid/voidSalary）；</li>
 *   <li>(b) 从 PENDING 可达全部 2 非初始态（PAID/VOID），终态（PAID/VOID）无出边（纯终态）；</li>
 *   <li>(c) markPaid/voidSalary 均仅 PENDING 单源：PAID/VOID/null 非法——PAID 源态的领域专属码
 *       {@code ERR_SALARY_LOCKED_AFTER_PAID} 由 BizModel 接线层 §11.4 终态领域异常重叠模式处理，不属于本层 1；</li>
 *   <li>(d) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(e) 终态/初始态集合正确。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpHrSalaryPaymentStateMachineMatrix {

    static final List<String> ALL_STATUSES = Arrays.asList(
            ErpHrConstants.PAYMENT_PENDING,
            ErpHrConstants.PAYMENT_PAID,
            ErpHrConstants.PAYMENT_VOID);

    private final ErpHrSalaryPaymentStateMachine sm = new ErpHrSalaryPaymentStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpHrSalaryPaymentStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpHrSalaryPaymentStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(2, edges.size(), "迁移矩阵应有 2 条边（markPaid/voidSalary 各单源 PENDING）");
    }

    // ---------- (b) 从 PENDING 可达全部 2 非初始态；终态无出边 ----------

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpHrConstants.PAYMENT_PENDING);
        for (String s : ALL_STATUSES) {
            if (ErpHrConstants.PAYMENT_PENDING.equals(s)) {
                continue;
            }
            assertTrue(reachable.contains(s), "从 PENDING 应可达状态: " + s);
        }
    }

    @Test
    public void testTerminalStatusesHaveNoOutgoingEdges() {
        // PAID/VOID 均为纯终态：不应出现在任何边的 fromStatus
        for (ErpHrSalaryPaymentStateMachine.TransitionDefinition e : sm.transitions()) {
            assertFalse(ErpHrConstants.PAYMENT_PAID.equals(e.getFromStatus()),
                    "PAID 纯终态不应有出边: but edge " + e.getAction() + " leaves it");
            assertFalse(ErpHrConstants.PAYMENT_VOID.equals(e.getFromStatus()),
                    "VOID 纯终态不应有出边: but edge " + e.getAction() + " leaves it");
        }
    }

    // ---------- (c) markPaid/voidSalary 单源 PENDING；PAID/VOID/null 全非法 ----------

    @Test
    public void testMarkPaidSingleSourcePending() {
        sm.assertCanMarkPaid(ErpHrConstants.PAYMENT_PENDING); // 不抛
        assertEquals(ErpHrConstants.PAYMENT_PAID, sm.markPaidTargetStatus());

        assertIllegalForAllExcept(sm::assertCanMarkPaid, "markPaid", ErpHrConstants.PAYMENT_PENDING);
        // null 同样非法（矩阵按 owner doc §4 仅 PENDING 单源）
        assertIllegalNull(sm::assertCanMarkPaid, "markPaid");
    }

    @Test
    public void testVoidSingleSourcePending() {
        sm.assertCanVoid(ErpHrConstants.PAYMENT_PENDING); // 不抛
        assertEquals(ErpHrConstants.PAYMENT_VOID, sm.voidTargetStatus());

        assertIllegalForAllExcept(sm::assertCanVoid, "voidSalary", ErpHrConstants.PAYMENT_PENDING);
        assertIllegalNull(sm::assertCanVoid, "voidSalary");
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpHrSalaryPaymentStateMachine.TransitionDefinition e : sm.transitions()) {
            invokeAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (e) 终态/初始态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(
                ErpHrConstants.PAYMENT_PAID,
                ErpHrConstants.PAYMENT_VOID), sm.terminalStatuses(),
                "终态集合 = {PAID, VOID}");
        assertEquals(Arrays.asList(ErpHrConstants.PAYMENT_PENDING), sm.initialStatuses(),
                "初始态集合 = {PENDING}");

        assertTrue(sm.isTerminal(ErpHrConstants.PAYMENT_PAID));
        assertTrue(sm.isTerminal(ErpHrConstants.PAYMENT_VOID));
        assertFalse(sm.isTerminal(ErpHrConstants.PAYMENT_PENDING));
    }

    // ---------- helpers ----------

    private interface AssertAction {
        void assertCan(String status);
    }

    private void assertIllegalForAllExcept(AssertAction action, String actionName, String allowedFrom) {
        for (String s : ALL_STATUSES) {
            if (allowedFrom.equals(s)) {
                continue;
            }
            NopException ex = assertThrows(NopException.class, () -> action.assertCan(s),
                    actionName + " 对非允许来源态应非法: " + s);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                    "Bean 报告 common 层非法迁移码: action=" + actionName + ", status=" + s);
            assertEquals(actionName, ex.getParam(ErpHrSalaryPaymentStateMachine.ARG_ACTION),
                    "拒绝元数据携带动作名: action=" + actionName + ", status=" + s);
            assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                    "拒绝元数据携带当前态: action=" + actionName + ", status=" + s);
        }
    }

    private void assertIllegalNull(AssertAction action, String actionName) {
        NopException ex = assertThrows(NopException.class, () -> action.assertCan(null),
                actionName + " 对 null 应非法（矩阵仅 PENDING 单源）");
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
        assertEquals(actionName, ex.getParam(ErpHrSalaryPaymentStateMachine.ARG_ACTION));
    }

    private void invokeAssert(String action, String status) {
        switch (action) {
            case "markPaid":
                sm.assertCanMarkPaid(status);
                break;
            case "voidSalary":
                sm.assertCanVoid(status);
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "markPaid":
                return sm.markPaidTargetStatus();
            case "voidSalary":
                return sm.voidTargetStatus();
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
            for (ErpHrSalaryPaymentStateMachine.TransitionDefinition e : sm.transitions()) {
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
