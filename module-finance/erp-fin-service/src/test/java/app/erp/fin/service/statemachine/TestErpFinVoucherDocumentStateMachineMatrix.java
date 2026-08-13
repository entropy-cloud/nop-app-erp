package app.erp.fin.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.exceptions.NopException;
import org.junit.jupiter.api.Test;

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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan Phase 1 Proof）。
 *
 * <p>针对 {@link ErpFinVoucherDocumentStateMachine} Bean 的纯矩阵完备性遍历：不经 BizModel 入口（层 3 职责），
 * 不断言副作用/审计/前置校验。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边；</li>
 *   <li>(b) post（DRAFT 合法、POSTED/CANCELLED 非法）；</li>
 *   <li>(c) 终态 POSTED 无出边；</li>
 *   <li>(d) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(e) initial/terminal 集合正确；</li>
 *   <li><b>CANCELLED 不在 initial/terminal/transitions 任一集合（死状态）</b>；</li>
 *   <li>isPosted(POSTED=true, DRAFT/CANCELLED=false) 分类 helper。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpFinVoucherDocumentStateMachineMatrix {

    private static final List<String> ALL_DICT_STATUSES = Arrays.asList(
            ErpFinConstants.VOUCHER_STATUS_DRAFT,
            ErpFinConstants.VOUCHER_STATUS_POSTED,
            ErpFinConstants.VOUCHER_STATUS_CANCELLED);

    private final ErpFinVoucherDocumentStateMachine sm = new ErpFinVoucherDocumentStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpFinVoucherDocumentStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpFinVoucherDocumentStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: action=" + e.getAction() + ", fromStatus=" + e.getFromStatus());
        }
        assertEquals(1, edges.size(), "迁移矩阵应有 1 条边（postVoucher: DRAFT→POSTED 唯一边）");
    }

    // ---------- (b) post：DRAFT 合法、POSTED/CANCELLED 非法 ----------

    @Test
    public void testPostLegalForDraftAndIllegalForOthers() {
        // DRAFT 合法（唯一迁移边来源态）
        sm.assertCanPost(ErpFinConstants.VOUCHER_STATUS_DRAFT); // 不抛

        // POSTED / CANCELLED 非法（抛 common 码 + action/fromStatus 元数据）
        for (String s : Arrays.asList(ErpFinConstants.VOUCHER_STATUS_POSTED, ErpFinConstants.VOUCHER_STATUS_CANCELLED)) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanPost(s),
                    "postVoucher 对非 DRAFT 应非法: " + s);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                    "Bean 报告 common 层非法迁移码: status=" + s);
            assertEquals("postVoucher", ex.getParam(ErpFinVoucherDocumentStateMachine.ARG_ACTION),
                    "拒绝元数据携带动作名: status=" + s);
            assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                    "拒绝元数据携带当前态: status=" + s);
        }
    }

    // ---------- (c) 终态 POSTED 无出边 ----------

    @Test
    public void testTerminalStatusesHaveNoOutgoingEdges() {
        for (String terminal : sm.terminalStatuses()) {
            for (ErpFinVoucherDocumentStateMachine.TransitionDefinition e : sm.transitions()) {
                assertFalse(e.getFromStatus().equals(terminal),
                        "终态不应有出边: terminal=" + terminal + ", but edge " + e.getAction() + " leaves it");
            }
        }
    }

    // ---------- (d) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpFinVoucherDocumentStateMachine.TransitionDefinition e : sm.transitions()) {
            // 每条边的 fromStatus 对该 action 合法（assert 放行不抛）
            sm.assertCanPost(e.getFromStatus());
            // 每条边的 toStatus 与 postVoucherTargetStatus() 一致
            assertEquals(e.getToStatus(), sm.postVoucherTargetStatus(),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
            assertEquals("postVoucher", e.getAction(), "action 名一致");
        }
    }

    // ---------- (e) 终态/初始态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Collections.singletonList(ErpFinConstants.VOUCHER_STATUS_POSTED), sm.terminalStatuses(),
                "终态集合 = {POSTED}");
        assertEquals(Collections.singletonList(ErpFinConstants.VOUCHER_STATUS_DRAFT), sm.initialStatuses(),
                "初始态集合 = {DRAFT}");

        assertTrue(sm.isTerminal(ErpFinConstants.VOUCHER_STATUS_POSTED));
        assertFalse(sm.isTerminal(ErpFinConstants.VOUCHER_STATUS_DRAFT));
        assertFalse(sm.isTerminal(ErpFinConstants.VOUCHER_STATUS_CANCELLED), "CANCELLED 死状态非终态");
    }

    // ---------- CANCELLED 死状态排除（死状态完备性，§5.1）----------

    @Test
    public void testCancelledIsDeadStatusNotInAnySet() {
        String dead = ErpFinConstants.VOUCHER_STATUS_CANCELLED;
        assertFalse(sm.initialStatuses().contains(dead), "CANCELLED 不在 initialStatuses");
        assertFalse(sm.terminalStatuses().contains(dead), "CANCELLED 不在 terminalStatuses（intentional reserved 死状态）");
        for (ErpFinVoucherDocumentStateMachine.TransitionDefinition e : sm.transitions()) {
            assertFalse(dead.equals(e.getFromStatus()), "CANCELLED 不应作为迁移边的 fromStatus");
            assertFalse(dead.equals(e.getToStatus()), "CANCELLED 不应作为迁移边的 toStatus");
        }
    }

    // ---------- 可达性：从 DRAFT 可达 POSTED，且无多余可达态 ----------

    @Test
    public void testReachabilityFromInitial() {
        Set<String> reachable = reachableFrom(ErpFinConstants.VOUCHER_STATUS_DRAFT);
        assertTrue(reachable.contains(ErpFinConstants.VOUCHER_STATUS_POSTED), "从 DRAFT 应可达 POSTED");
        assertFalse(reachable.contains(ErpFinConstants.VOUCHER_STATUS_CANCELLED),
                "CANCELLED 不可达（死状态，无入边）");
    }

    // ---------- 目标态方法 + isPosted 分类 helper（isReversed 非 docStatus 边）----------

    @Test
    public void testTargetStatusAndIsPostedHelper() {
        assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, sm.postVoucherTargetStatus());

        // isPosted 分类 helper（供 reverseVoucher/previewReverseVoucher 前置守卫复用）
        assertTrue(sm.isPosted(ErpFinConstants.VOUCHER_STATUS_POSTED), "POSTED → isPosted=true");
        assertFalse(sm.isPosted(ErpFinConstants.VOUCHER_STATUS_DRAFT), "DRAFT → isPosted=false");
        assertFalse(sm.isPosted(ErpFinConstants.VOUCHER_STATUS_CANCELLED), "CANCELLED → isPosted=false（死状态）");

        // isPosted 与 isTerminal 对 POSTED 一致（POSTED 既是终态又是分类态）
        assertEquals(sm.isTerminal(ErpFinConstants.VOUCHER_STATUS_POSTED),
                sm.isPosted(ErpFinConstants.VOUCHER_STATUS_POSTED));
    }

    // ---------- helpers ----------

    private Set<String> reachableFrom(String start) {
        Set<String> visited = new LinkedHashSet<>();
        List<String> frontier = new java.util.ArrayList<>();
        frontier.add(start);
        while (!frontier.isEmpty()) {
            String cur = frontier.remove(0);
            if (!visited.add(cur)) {
                continue;
            }
            for (ErpFinVoucherDocumentStateMachine.TransitionDefinition e : sm.transitions()) {
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
