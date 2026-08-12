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
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan Phase 1 Proof）。
 *
 * <p>针对 {@link ErpB2bEdiDocStateMachine} Bean 的纯矩阵完备性遍历：不经 BizModel 入口（层 3 职责），
 * 不断言副作用/审计/日志。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边（全三元组 action+fromStatus+toStatus 唯一；retry 为方向依赖多目标动作，豁免 action+from 唯一）；</li>
 *   <li>(b) 终态 {CANCELLED, ACKNOWLEDGED, ARCHIVED} 无出边；</li>
 *   <li>(c) cancel 多源 {TO_SEND, SENT, ERROR} 合法、对终态非法；</li>
 *   <li>(d) markError 仅 {TO_SEND, SENT, RECEIVED} 合法、对终态/ERROR 非法（D-B2B-3 Fix 收紧断言）；</li>
 *   <li>(e) retry 仅 ERROR 合法；</li>
 *   <li>(f) {@code transitions()} 元数据与显式方法语义一致；</li>
 *   <li>(g) 初始/终态集合正确；</li>
 *   <li>(h) TO_CANCEL 无任何边（D-B2B-1 dict 死状态，Bean 不编码）。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpB2bEdiDocStateMachineMatrix {

    private static final List<String> ALL_STATES = Arrays.asList(
            ErpB2bConstants.EDI_DOC_STATE_TO_SEND,
            ErpB2bConstants.EDI_DOC_STATE_SENT,
            ErpB2bConstants.EDI_DOC_STATE_TO_CANCEL,
            ErpB2bConstants.EDI_DOC_STATE_CANCELLED,
            ErpB2bConstants.EDI_DOC_STATE_ERROR,
            ErpB2bConstants.EDI_DOC_STATE_RECEIVED,
            ErpB2bConstants.EDI_DOC_STATE_ACKNOWLEDGED,
            ErpB2bConstants.EDI_DOC_STATE_ARCHIVED);

    private final ErpB2bEdiDocStateMachine sm = new ErpB2bEdiDocStateMachine();

    // ---------- (a) 无重复/冲突边（全三元组唯一） ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpB2bEdiDocStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpB2bEdiDocStateMachine.TransitionDefinition e : edges) {
            // 全三元组唯一：retry 为方向依赖多目标（ERROR→TO_SEND / ERROR→RECEIVED），两个不同 toStatus 不算冲突
            String key = e.getAction() + "|" + e.getFromStatus() + "|" + e.getToStatus();
            assertTrue(seen.add(key), "重复/冲突边: " + key);
        }
        assertEquals(11, edges.size(), "迁移矩阵应有 11 条边（markSent 1 + markAcknowledged 1 + markError 3 + retry 2 + cancel 3 + archive 1）");
    }

    // ---------- (b) 终态无出边 ----------

    @Test
    public void testTerminalStatusesHaveNoOutgoingEdges() {
        for (String terminal : sm.terminalStatuses()) {
            for (ErpB2bEdiDocStateMachine.TransitionDefinition e : sm.transitions()) {
                assertFalse(e.getFromStatus().equals(terminal),
                        "终态不应有出边: terminal=" + terminal + ", but edge " + e.getAction() + " leaves it");
            }
        }
    }

    // ---------- (c) cancel 多源 {TO_SEND, SENT, ERROR} 合法、对终态非法 ----------

    @Test
    public void testCancelLegalForMultiSourceAndIllegalForTerminal() {
        List<String> cancelSources = Arrays.asList(
                ErpB2bConstants.EDI_DOC_STATE_TO_SEND,
                ErpB2bConstants.EDI_DOC_STATE_SENT,
                ErpB2bConstants.EDI_DOC_STATE_ERROR);
        for (String s : cancelSources) {
            sm.assertCanCancel(s); // 合法边不抛
        }
        assertEquals(ErpB2bConstants.EDI_DOC_STATE_CANCELLED, sm.cancelTargetStatus());

        // 终态 + RECEIVED + TO_CANCEL 非法
        for (String s : ALL_STATES) {
            if (cancelSources.contains(s)) {
                continue;
            }
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanCancel(s),
                    "cancel 对非来源态应非法: " + s);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                    "Bean 报告 common 层非法迁移码: status=" + s);
            assertEquals("cancel", ex.getParam(ErpB2bEdiDocStateMachine.ARG_ACTION), "拒绝元数据携带动作名");
            assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS), "拒绝元数据携带当前态");
        }
    }

    // ---------- (d) markError 仅 {TO_SEND, SENT, RECEIVED} 合法、对终态/ERROR/TO_CANCEL 非法（D-B2B-3 Fix 收紧） ----------

    @Test
    public void testMarkErrorTightenedToDocSourcesOnly() {
        List<String> legalSources = Arrays.asList(
                ErpB2bConstants.EDI_DOC_STATE_TO_SEND,
                ErpB2bConstants.EDI_DOC_STATE_SENT,
                ErpB2bConstants.EDI_DOC_STATE_RECEIVED);
        for (String s : legalSources) {
            sm.assertCanMarkError(s); // 合法边不抛（D-B2B-3 Fix 后）
        }
        assertEquals(ErpB2bConstants.EDI_DOC_STATE_ERROR, sm.markErrorTargetStatus());

        // 非法来源：终态 + ERROR 自身 + TO_CANCEL 死状态
        List<String> illegalSources = Arrays.asList(
                ErpB2bConstants.EDI_DOC_STATE_CANCELLED,
                ErpB2bConstants.EDI_DOC_STATE_ACKNOWLEDGED,
                ErpB2bConstants.EDI_DOC_STATE_ARCHIVED,
                ErpB2bConstants.EDI_DOC_STATE_ERROR,
                ErpB2bConstants.EDI_DOC_STATE_TO_CANCEL);
        for (String s : illegalSources) {
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanMarkError(s),
                    "markError 对非文档来源应非法（D-B2B-3 Fix 收紧）: " + s);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                    "Bean 报告 common 层非法迁移码: status=" + s);
            assertEquals("markError", ex.getParam(ErpB2bEdiDocStateMachine.ARG_ACTION), "拒绝元数据携带动作名");
            assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS), "拒绝元数据携带当前态");
        }
    }

    // ---------- (e) retry 仅 ERROR 合法 ----------

    @Test
    public void testRetryOnlyFromError() {
        sm.assertCanRetry(ErpB2bConstants.EDI_DOC_STATE_ERROR); // 唯一合法来源

        // 出/入站目标态
        assertEquals(ErpB2bConstants.EDI_DOC_STATE_TO_SEND, sm.retryOutboundTargetStatus());
        assertEquals(ErpB2bConstants.EDI_DOC_STATE_RECEIVED, sm.retryInboundTargetStatus());

        for (String s : ALL_STATES) {
            if (ErpB2bConstants.EDI_DOC_STATE_ERROR.equals(s)) {
                continue;
            }
            NopException ex = assertThrows(NopException.class, () -> sm.assertCanRetry(s),
                    "retry 对非 ERROR 应非法: " + s);
            assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                    "Bean 报告 common 层非法迁移码: status=" + s);
            assertEquals("retry", ex.getParam(ErpB2bEdiDocStateMachine.ARG_ACTION), "拒绝元数据携带动作名");
        }
    }

    // ---------- (f) transitions() 元数据与显式方法语义一致 ----------

    @Test
    public void testTransitionsMetadataConsistentWithExplicitMethods() {
        for (ErpB2bEdiDocStateMachine.TransitionDefinition e : sm.transitions()) {
            // 每条边的 fromStatus 对该 action 合法（assert 放行不抛）
            invokeAssert(e.getAction(), e.getFromStatus());
            // 每条边的 toStatus 与目标态方法一致（retry 除外——方向依赖，分别由 retryOutbound/InboundTargetStatus 覆盖）
            if (!"retry".equals(e.getAction())) {
                assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                        "toStatus 与目标态方法不一致: action=" + e.getAction());
            } else {
                // retry 目标态必为出/入站之一
                assertTrue(e.getToStatus().equals(sm.retryOutboundTargetStatus())
                                || e.getToStatus().equals(sm.retryInboundTargetStatus()),
                        "retry 目标态必为 TO_SEND 或 RECEIVED: actual=" + e.getToStatus());
            }
        }
    }

    // ---------- (g) 初始/终态集合正确 ----------

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(Arrays.asList(
                ErpB2bConstants.EDI_DOC_STATE_CANCELLED,
                ErpB2bConstants.EDI_DOC_STATE_ACKNOWLEDGED,
                ErpB2bConstants.EDI_DOC_STATE_ARCHIVED), sm.terminalStatuses(), "终态集合 = {CANCELLED, ACKNOWLEDGED, ARCHIVED}");
        assertEquals(Arrays.asList(
                ErpB2bConstants.EDI_DOC_STATE_TO_SEND,
                ErpB2bConstants.EDI_DOC_STATE_RECEIVED), sm.initialStatuses(), "初始态集合 = {TO_SEND, RECEIVED}");

        assertTrue(sm.isTerminal(ErpB2bConstants.EDI_DOC_STATE_CANCELLED));
        assertTrue(sm.isTerminal(ErpB2bConstants.EDI_DOC_STATE_ACKNOWLEDGED));
        assertTrue(sm.isTerminal(ErpB2bConstants.EDI_DOC_STATE_ARCHIVED));
        assertFalse(sm.isTerminal(ErpB2bConstants.EDI_DOC_STATE_TO_SEND));
        assertFalse(sm.isTerminal(ErpB2bConstants.EDI_DOC_STATE_ERROR));
        assertFalse(sm.isTerminal(ErpB2bConstants.EDI_DOC_STATE_RECEIVED));
        // TO_CANCEL 不在终态集（死状态）
        assertFalse(sm.isTerminal(ErpB2bConstants.EDI_DOC_STATE_TO_CANCEL));
    }

    // ---------- (h) TO_CANCEL 无任何边（D-B2B-1 dict 死状态） ----------

    @Test
    public void testToCancelDeadStateHasNoEdges() {
        for (ErpB2bEdiDocStateMachine.TransitionDefinition e : sm.transitions()) {
            assertFalse(ErpB2bConstants.EDI_DOC_STATE_TO_CANCEL.equals(e.getFromStatus()),
                    "TO_CANCEL 死状态不应作为来源态出现: edge=" + e.getAction());
            assertFalse(ErpB2bConstants.EDI_DOC_STATE_TO_CANCEL.equals(e.getToStatus()),
                    "TO_CANCEL 死状态不应作为目标态出现: edge=" + e.getAction());
        }
    }

    // ---------- 单来源动作显式断言（补充 markSent/markAcknowledged/archive） ----------

    @Test
    public void testSingleSourceActionGuards() {
        assertActionAllowsOnly("markSent", ErpB2bConstants.EDI_DOC_STATE_TO_SEND);
        assertActionAllowsOnly("markAcknowledged", ErpB2bConstants.EDI_DOC_STATE_SENT);
        assertActionAllowsOnly("archive", ErpB2bConstants.EDI_DOC_STATE_RECEIVED);
    }

    @Test
    public void testTargetStatusMethods() {
        assertEquals(ErpB2bConstants.EDI_DOC_STATE_SENT, sm.markSentTargetStatus());
        assertEquals(ErpB2bConstants.EDI_DOC_STATE_ACKNOWLEDGED, sm.markAcknowledgedTargetStatus());
        assertEquals(ErpB2bConstants.EDI_DOC_STATE_ERROR, sm.markErrorTargetStatus());
        assertEquals(ErpB2bConstants.EDI_DOC_STATE_CANCELLED, sm.cancelTargetStatus());
        assertEquals(ErpB2bConstants.EDI_DOC_STATE_ARCHIVED, sm.archiveTargetStatus());
    }

    // ---------- helpers ----------

    private void assertActionAllowsOnly(String action, String allowedFrom) {
        for (String s : ALL_STATES) {
            if (allowedFrom.equals(s)) {
                invokeAssert(action, s); // 不抛 = 合法
            } else {
                NopException ex = assertThrows(NopException.class, () -> invokeAssert(action, s),
                        action + " 对非允许来源态应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                        "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + s);
                assertEquals(action, ex.getParam(ErpB2bEdiDocStateMachine.ARG_ACTION),
                        "拒绝元数据携带动作名: action=" + action);
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                        "拒绝元数据携带当前态: action=" + action + ", status=" + s);
            }
        }
    }

    private void invokeAssert(String action, String status) {
        switch (action) {
            case "markSent":
                sm.assertCanMarkSent(status);
                break;
            case "markAcknowledged":
                sm.assertCanMarkAcknowledged(status);
                break;
            case "markError":
                sm.assertCanMarkError(status);
                break;
            case "retry":
                sm.assertCanRetry(status);
                break;
            case "cancel":
                sm.assertCanCancel(status);
                break;
            case "archive":
                sm.assertCanArchive(status);
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "markSent":
                return sm.markSentTargetStatus();
            case "markAcknowledged":
                return sm.markAcknowledgedTargetStatus();
            case "markError":
                return sm.markErrorTargetStatus();
            case "cancel":
                return sm.cancelTargetStatus();
            case "archive":
                return sm.archiveTargetStatus();
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }
}
