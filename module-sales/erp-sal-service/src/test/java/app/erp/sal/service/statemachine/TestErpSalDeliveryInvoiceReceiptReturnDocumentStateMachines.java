package app.erp.sal.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.sal.dao.constants.ErpSalDocStatus;
import io.nop.api.core.exceptions.NopException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-13-0810-2 Phase 1 Proof）。
 *
 * <p>针对 4 个 docStatus 轴 Bean（{@link ErpSalDeliveryDocumentStateMachine} /
 * {@link ErpSalInvoiceDocumentStateMachine} / {@link ErpSalReceiptDocumentStateMachine} /
 * {@link ErpSalReturnDocumentStateMachine}）的纯矩阵完备性遍历：不经 BizModel 入口（层 3 职责），
 * 不断言副作用/审计。4 实体 docStatus 矩阵一致（单边 cancel: DRAFT→CANCELLED），故以参数化表驱动复用同一断言套件。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边；</li>
 *   <li>(b) 从 DRAFT 可达 CANCELLED、CANCELLED 终态无出边；</li>
 *   <li>(c) cancel 对 DRAFT 合法、对 CANCELLED/ACTIVE 抛 common 码携带 {@code action=cancel}/{@code currentStatus}；</li>
 *   <li>(d) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(e) 初始/终态集合正确。</li>
 * </ul>
 *
 * <p>ACTIVE 为 dict 存在但无生产 writer 的死状态（同 Order/Quotation 裁定），cancel 对 ACTIVE 合法（非终态放行）。
 * Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpSalDeliveryInvoiceReceiptReturnDocumentStateMachines {

    /** 把 4 个同构 Bean 的方法统一适配，复用同一断言套件。 */
    private static final class Subject {
        final String name;
        final Runnable assertCancelDraft;
        final java.util.function.Consumer<String> assertCancel;
        final java.util.function.Supplier<String> cancelTarget;
        final java.util.function.Function<String, Boolean> isTerminal;
        final java.util.function.Supplier<List<? extends TransitionEdge>> transitions;
        final java.util.function.Supplier<List<String>> terminalStatuses;
        final java.util.function.Supplier<List<String>> initialStatuses;
        final java.util.function.Supplier<String> argAction;

        Subject(String name, Runnable assertCancelDraft, java.util.function.Consumer<String> assertCancel,
                java.util.function.Supplier<String> cancelTarget, java.util.function.Function<String, Boolean> isTerminal,
                java.util.function.Supplier<List<? extends TransitionEdge>> transitions,
                java.util.function.Supplier<List<String>> terminalStatuses,
                java.util.function.Supplier<List<String>> initialStatuses,
                java.util.function.Supplier<String> argAction) {
            this.name = name;
            this.assertCancelDraft = assertCancelDraft;
            this.assertCancel = assertCancel;
            this.cancelTarget = cancelTarget;
            this.isTerminal = isTerminal;
            this.transitions = transitions;
            this.terminalStatuses = terminalStatuses;
            this.initialStatuses = initialStatuses;
            this.argAction = argAction;
        }
    }

    /** Bean 内部 TransitionDefinition 的无类型适配（4 Bean 各自定义同名记录）。 */
    private interface TransitionEdge {
        String action();

        String fromStatus();

        String toStatus();
    }

    static Stream<Subject> subjects() {
        return Stream.of(
                wrap("Delivery", new ErpSalDeliveryDocumentStateMachine()),
                wrap("Invoice", new ErpSalInvoiceDocumentStateMachine()),
                wrap("Receipt", new ErpSalReceiptDocumentStateMachine()),
                wrap("Return", new ErpSalReturnDocumentStateMachine()));
    }

    private static Subject wrap(String name, ErpSalDeliveryDocumentStateMachine sm) {
        return new Subject(name,
                () -> sm.assertCanCancel(ErpSalDocStatus.DOC_STATUS_DRAFT),
                sm::assertCanCancel, sm::cancelTargetStatus, sm::isTerminal,
                () -> sm.transitions().stream()
                        .map(TestErpSalDeliveryInvoiceReceiptReturnDocumentStateMachines::edge)
                        .collect(java.util.stream.Collectors.toList()),
                sm::terminalStatuses, sm::initialStatuses,
                () -> ErpSalDeliveryDocumentStateMachine.ARG_ACTION);
    }

    private static Subject wrap(String name, ErpSalInvoiceDocumentStateMachine sm) {
        return new Subject(name,
                () -> sm.assertCanCancel(ErpSalDocStatus.DOC_STATUS_DRAFT),
                sm::assertCanCancel, sm::cancelTargetStatus, sm::isTerminal,
                () -> sm.transitions().stream().map(TestErpSalDeliveryInvoiceReceiptReturnDocumentStateMachines::edge)
                        .collect(java.util.stream.Collectors.toList()),
                sm::terminalStatuses, sm::initialStatuses,
                () -> ErpSalInvoiceDocumentStateMachine.ARG_ACTION);
    }

    private static Subject wrap(String name, ErpSalReceiptDocumentStateMachine sm) {
        return new Subject(name,
                () -> sm.assertCanCancel(ErpSalDocStatus.DOC_STATUS_DRAFT),
                sm::assertCanCancel, sm::cancelTargetStatus, sm::isTerminal,
                () -> sm.transitions().stream().map(TestErpSalDeliveryInvoiceReceiptReturnDocumentStateMachines::edge)
                        .collect(java.util.stream.Collectors.toList()),
                sm::terminalStatuses, sm::initialStatuses,
                () -> ErpSalReceiptDocumentStateMachine.ARG_ACTION);
    }

    private static Subject wrap(String name, ErpSalReturnDocumentStateMachine sm) {
        return new Subject(name,
                () -> sm.assertCanCancel(ErpSalDocStatus.DOC_STATUS_DRAFT),
                sm::assertCanCancel, sm::cancelTargetStatus, sm::isTerminal,
                () -> sm.transitions().stream().map(TestErpSalDeliveryInvoiceReceiptReturnDocumentStateMachines::edge)
                        .collect(java.util.stream.Collectors.toList()),
                sm::terminalStatuses, sm::initialStatuses,
                () -> ErpSalReturnDocumentStateMachine.ARG_ACTION);
    }

    private static TransitionEdge edge(Object o) {
        try {
            java.lang.reflect.Method a = o.getClass().getMethod("getAction");
            java.lang.reflect.Method f = o.getClass().getMethod("getFromStatus");
            java.lang.reflect.Method t = o.getClass().getMethod("getToStatus");
            String action = (String) a.invoke(o);
            String from = (String) f.invoke(o);
            String to = (String) t.invoke(o);
            return new TransitionEdge() {
                public String action() { return action; }
                public String fromStatus() { return from; }
                public String toStatus() { return to; }
            };
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    // ---------- (a) 无重复/冲突边 ----------

    @ParameterizedTest
    @MethodSource("subjects")
    public void testNoDuplicateOrConflictingEdges(Subject s) {
        List<? extends TransitionEdge> edges = s.transitions.get();
        Set<String> seen = new HashSet<>();
        for (TransitionEdge e : edges) {
            String key = e.action() + "|" + e.fromStatus();
            assertTrue(seen.add(key), "[" + s.name + "] 重复/冲突边: action=" + e.action() + ", fromStatus=" + e.fromStatus());
        }
        assertEquals(1, edges.size(), "[" + s.name + "] 迁移矩阵应有 1 条边（DRAFT→CANCELLED）");
    }

    // ---------- (b) 从 DRAFT 可达 CANCELLED；终态无出边 ----------

    @ParameterizedTest
    @MethodSource("subjects")
    public void testReachabilityFromInitial(Subject s) {
        Set<String> reachable = reachableFrom(s, ErpSalDocStatus.DOC_STATUS_DRAFT);
        assertTrue(reachable.contains(ErpSalDocStatus.DOC_STATUS_CANCELLED),
                "[" + s.name + "] 从 DRAFT 应可达 CANCELLED");
    }

    @ParameterizedTest
    @MethodSource("subjects")
    public void testTerminalStatusesHaveNoOutgoingEdges(Subject s) {
        for (String terminal : s.terminalStatuses.get()) {
            for (TransitionEdge e : s.transitions.get()) {
                assertFalse(e.fromStatus().equals(terminal),
                        "[" + s.name + "] 终态不应有出边: terminal=" + terminal + ", but edge " + e.action() + " leaves it");
            }
        }
    }

    // ---------- (c) cancel 对 DRAFT 合法、对 CANCELLED 非法、ACTIVE 放行 ----------

    @ParameterizedTest
    @MethodSource("subjects")
    public void testCancelLegalForDraftAndIllegalForCancelled(Subject s) {
        // DRAFT 合法（不抛）
        s.assertCancelDraft.run();
        assertEquals(ErpSalDocStatus.DOC_STATUS_CANCELLED, s.cancelTarget.get(), "[" + s.name + "] cancel 目标态 = CANCELLED");

        // ACTIVE 死状态：非终态放行（不抛）
        s.assertCancel.accept(ErpSalDocStatus.DOC_STATUS_ACTIVE);

        // CANCELLED 非法 → 抛 common 层码 + action/currentStatus 元数据
        NopException ex = assertThrows(NopException.class,
                () -> s.assertCancel.accept(ErpSalDocStatus.DOC_STATUS_CANCELLED),
                "[" + s.name + "] cancel 对 CANCELLED 应非法");
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "[" + s.name + "] Bean 报告 common 层非法迁移码");
        assertEquals("cancel", ex.getParam(s.argAction.get()), "[" + s.name + "] 拒绝元数据携带动作名");
        assertEquals(ErpSalDocStatus.DOC_STATUS_CANCELLED, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                "[" + s.name + "] 拒绝元数据携带当前态");
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @ParameterizedTest
    @MethodSource("subjects")
    public void testTransitionsMetadataConsistentWithExplicitMethods(Subject s) {
        for (TransitionEdge e : s.transitions.get()) {
            // 每条边的 fromStatus 对该 action 合法（assert 放行不抛）
            invokeAssert(s, e.action(), e.fromStatus());
            // 每条边的 toStatus 与 <action>TargetStatus() 一致
            assertEquals(e.toStatus(), targetStatusFor(s, e.action()),
                    "[" + s.name + "] toStatus 与目标态方法不一致: action=" + e.action());
        }
    }

    // ---------- (e) 终态/初始态集合正确 ----------

    @ParameterizedTest
    @MethodSource("subjects")
    public void testTerminalAndInitialSets(Subject s) {
        assertEquals(Arrays.asList(ErpSalDocStatus.DOC_STATUS_CANCELLED), s.terminalStatuses.get(),
                "[" + s.name + "] 终态集合 = {CANCELLED}");
        assertEquals(Arrays.asList(ErpSalDocStatus.DOC_STATUS_DRAFT), s.initialStatuses.get(),
                "[" + s.name + "] 初始态集合 = {DRAFT}");

        assertTrue(s.isTerminal.apply(ErpSalDocStatus.DOC_STATUS_CANCELLED), "[" + s.name + "] CANCELLED 为终态");
        assertFalse(s.isTerminal.apply(ErpSalDocStatus.DOC_STATUS_DRAFT), "[" + s.name + "] DRAFT 非终态");
        assertFalse(s.isTerminal.apply(ErpSalDocStatus.DOC_STATUS_ACTIVE), "[" + s.name + "] ACTIVE 非终态");
    }

    // ---------- helpers ----------

    private void invokeAssert(Subject s, String action, String docStatus) {
        switch (action) {
            case "cancel":
                s.assertCancel.accept(docStatus);
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String targetStatusFor(Subject s, String action) {
        switch (action) {
            case "cancel":
                return s.cancelTarget.get();
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private Set<String> reachableFrom(Subject s, String start) {
        Set<String> visited = new java.util.LinkedHashSet<>();
        List<String> frontier = new java.util.ArrayList<>();
        frontier.add(start);
        while (!frontier.isEmpty()) {
            String cur = frontier.remove(0);
            if (!visited.add(cur)) {
                continue;
            }
            for (TransitionEdge e : s.transitions.get()) {
                if (e.fromStatus().equals(cur) && !visited.contains(e.toStatus())) {
                    frontier.add(e.toStatus());
                }
            }
        }
        visited.remove(start);
        return visited;
    }
}
