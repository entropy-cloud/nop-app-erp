package app.erp.fin.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.fin.service.ErpFinConstants;
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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan 2026-08-13-1146-1 Phase 3 Proof）。
 *
 * <p>针对 {@link ErpFinNotesPayableStateMachine} 的纯矩阵完备性遍历：{@code status} 单轴
 * （dict {@code erp-fin/notes-payable-status} 4 值 ISSUED/HONORED/DISHONORED/WRITE_OFF）。
 * 不经 BizModel/Processor 入口（层 3 职责），不断言副作用/过账/红冲/授信占用释放。
 *
 * <p>每轴覆盖：(a) 无重复/冲突边；(b) 从初始态可达全部声明状态 + 终态无出边；(c) 各动作合法/非法来源态全集；
 * (d) {@code transitions()} 元数据与显式方法语义一致；(e) 终态/初始态集合正确；(f) 4 值全可达（无死状态）。
 * Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 *
 * <p><b>issue initial 写入</b>：{@code null}（初始写入 §9.2 选项 c）与 ISSUED（幂等）合法，
 * 其余来源态非法（issue 守卫有意收窄）。
 */
public class TestErpFinNotesPayableStateMachineMatrix {

    // erp-fin/notes-payable-status 字典四态
    private static final List<String> ALL_STATUSES = Arrays.asList(
            ErpFinConstants.NOTES_PAY_ISSUED,
            ErpFinConstants.NOTES_PAY_HONORED,
            ErpFinConstants.NOTES_PAY_DISHONORED,
            ErpFinConstants.NOTES_PAY_WRITE_OFF);

    private final ErpFinNotesPayableStateMachine sm = new ErpFinNotesPayableStateMachine();

    @Test
    public void noDuplicateOrConflictingEdges() {
        List<ErpFinNotesPayableStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpFinNotesPayableStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(4, edges.size(), "迁移矩阵应有 4 命名边（issue/honor/dishonor/writeOff）");
    }

    @Test
    public void reachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpFinConstants.NOTES_PAY_ISSUED);
        assertTrue(reachable.contains(ErpFinConstants.NOTES_PAY_HONORED), "从 ISSUED 应可达 HONORED");
        assertTrue(reachable.contains(ErpFinConstants.NOTES_PAY_DISHONORED), "从 ISSUED 应可达 DISHONORED");
        assertTrue(reachable.contains(ErpFinConstants.NOTES_PAY_WRITE_OFF), "从 ISSUED 应可达 WRITE_OFF");
    }

    @Test
    public void issueAllowsOnlyInitialOrIdempotent() {
        sm.assertCanIssue(null); // initial 写入（§9.2 选项 c）
        sm.assertCanIssue(ErpFinConstants.NOTES_PAY_ISSUED); // 幂等（isAlreadyIssued 短路）
        for (String s : Arrays.asList(ErpFinConstants.NOTES_PAY_HONORED,
                ErpFinConstants.NOTES_PAY_DISHONORED,
                ErpFinConstants.NOTES_PAY_WRITE_OFF)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanIssue(s),
                    "issue 对非 initial 来源态应非法: " + s);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
            assertEquals("issue", ex.getParam(ErpFinNotesPayableStateMachine.ARG_ACTION));
            assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
        }
    }

    @Test
    public void honorAllowsOnlyIssued() {
        assertAllowsOnly("honor", ErpFinConstants.NOTES_PAY_ISSUED);
    }

    @Test
    public void dishonorAllowsOnlyIssued() {
        assertAllowsOnly("dishonor", ErpFinConstants.NOTES_PAY_ISSUED);
    }

    @Test
    public void writeOffAllowsOnlyNonTerminal() {
        sm.assertCanWriteOff(ErpFinConstants.NOTES_PAY_ISSUED); // 唯一非终态合法
        for (String s : Arrays.asList(ErpFinConstants.NOTES_PAY_HONORED,
                ErpFinConstants.NOTES_PAY_DISHONORED,
                ErpFinConstants.NOTES_PAY_WRITE_OFF)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanWriteOff(s),
                    "writeOff 对终态应非法: " + s);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
            assertEquals("writeOff", ex.getParam(ErpFinNotesPayableStateMachine.ARG_ACTION));
            assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
            assertEquals("非终态", ex.getParam(ErpCommonErrors.ARG_EXPECTED_STATUS),
                    "writeOff expected 文案对外不变");
        }
    }

    @Test
    public void terminalsAreTrueTerminals() {
        for (ErpFinNotesPayableStateMachine.TransitionDefinition e : sm.transitions()) {
            assertFalse(sm.isTerminal(e.getFromStatus()),
                    "真终态不应有出边: but edge " + e.getAction() + " leaves " + e.getFromStatus());
        }
    }

    @Test
    public void transitionsMetadataConsistentWithExplicitMethods() {
        for (ErpFinNotesPayableStateMachine.TransitionDefinition e : sm.transitions()) {
            invokeAssert(e.getAction(), e.getFromStatus()); // 代表源态须合法
            assertEquals(targetStatusFor(e.getAction()), e.getToStatus(),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    @Test
    public void targetStatusMethods() {
        assertEquals(ErpFinConstants.NOTES_PAY_ISSUED, sm.issueTargetStatus());
        assertEquals(ErpFinConstants.NOTES_PAY_HONORED, sm.honorTargetStatus());
        assertEquals(ErpFinConstants.NOTES_PAY_DISHONORED, sm.dishonorTargetStatus());
        assertEquals(ErpFinConstants.NOTES_PAY_WRITE_OFF, sm.writeOffTargetStatus());
    }

    @Test
    public void terminalAndInitialSets() {
        assertEquals(Arrays.asList(ErpFinConstants.NOTES_PAY_HONORED,
                ErpFinConstants.NOTES_PAY_DISHONORED,
                ErpFinConstants.NOTES_PAY_WRITE_OFF), sm.terminalStatuses(), "终态集合 = {HONORED, DISHONORED, WRITE_OFF}");
        assertEquals(Collections.singletonList(ErpFinConstants.NOTES_PAY_ISSUED),
                sm.initialStatuses(), "初始态集合 = {ISSUED}");
        assertTrue(sm.isTerminal(ErpFinConstants.NOTES_PAY_HONORED));
        assertTrue(sm.isTerminal(ErpFinConstants.NOTES_PAY_DISHONORED));
        assertTrue(sm.isTerminal(ErpFinConstants.NOTES_PAY_WRITE_OFF));
        assertFalse(sm.isTerminal(ErpFinConstants.NOTES_PAY_ISSUED));
    }

    @Test
    public void allDictValuesReachable() {
        // 4 值全可达：ISSUED=初始态（issue initial 写入），其余 3 值均为某迁移边 toStatus
        for (String s : ALL_STATUSES) {
            if (ErpFinConstants.NOTES_PAY_ISSUED.equals(s)) {
                assertTrue(sm.initialStatuses().contains(s), "ISSUED 为初始态（issue 写入）");
            } else {
                boolean written = sm.transitions().stream().anyMatch(e -> s.equals(e.getToStatus()));
                assertTrue(written, "dict 值应有 writer 可达: " + s);
            }
        }
    }

    // ==================== helpers ====================

    private void assertAllowsOnly(String action, String allowedFrom) {
        for (String s : ALL_STATUSES) {
            if (allowedFrom.equals(s)) {
                invokeAssert(action, s); // 合法不抛
            } else {
                NopException ex = assertThrows(NopException.class, () -> invokeAssert(action, s),
                        action + " 对非允许来源态应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                        "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + s);
                assertEquals(action, ex.getParam(ErpFinNotesPayableStateMachine.ARG_ACTION));
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS));
            }
        }
    }

    private void invokeAssert(String action, String status) {
        switch (action) {
            case "issue":
                sm.assertCanIssue(status);
                break;
            case "honor":
                sm.assertCanHonor(status);
                break;
            case "dishonor":
                sm.assertCanDishonor(status);
                break;
            case "writeOff":
                sm.assertCanWriteOff(status);
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "issue":
                return sm.issueTargetStatus();
            case "honor":
                return sm.honorTargetStatus();
            case "dishonor":
                return sm.dishonorTargetStatus();
            case "writeOff":
                return sm.writeOffTargetStatus();
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
            for (ErpFinNotesPayableStateMachine.TransitionDefinition e : sm.transitions()) {
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
