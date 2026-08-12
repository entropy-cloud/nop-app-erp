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
 * <p>针对 {@link ErpB2bAsnStateMachine} Bean 的纯矩阵完备性遍历：不经 Processor 入口（层 3 职责），
 * 不断言副作用/HMAC/config-gate。覆盖：
 * <ul>
 *   <li>(a) 无重复/冲突边；</li>
 *   <li>(b) RECEIVED_TO_STOCK 终态无出边；</li>
 *   <li>(c) matchPurchaseOrder 仅 RECEIVED、createReceiveFromAsn 仅 MATCHED；</li>
 *   <li>(d) {@code transitions()} 一致；</li>
 *   <li>(e) CANCELLED 无任何边/不在终态集（D-B2B-2 未落地）；</li>
 *   <li>(f) retryMatch 幂等判定 helper 正确。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpB2bAsnStateMachineMatrix {

    private static final List<String> ALL_STATUSES = Arrays.asList(
            ErpB2bConstants.ASN_STATUS_RECEIVED,
            ErpB2bConstants.ASN_STATUS_MATCHED,
            ErpB2bConstants.ASN_STATUS_RECEIVED_TO_STOCK,
            ErpB2bConstants.ASN_STATUS_CANCELLED);

    private final ErpB2bAsnStateMachine sm = new ErpB2bAsnStateMachine();

    // ---------- (a) 无重复/冲突边 ----------

    @Test
    public void testNoDuplicateOrConflictingEdges() {
        List<ErpB2bAsnStateMachine.TransitionDefinition> edges = sm.transitions();
        Set<String> seen = new HashSet<>();
        for (ErpB2bAsnStateMachine.TransitionDefinition e : edges) {
            String key = e.getAction() + "|" + e.getFromStatus();
            assertTrue(seen.add(key), "重复/冲突边: " + key);
        }
        assertEquals(2, edges.size(), "迁移矩阵应有 2 条边");
    }

    // ---------- (b) RECEIVED_TO_STOCK 终态无出边 ----------

    @Test
    public void testTerminalStatusHasNoOutgoingEdges() {
        for (String terminal : sm.terminalStatuses()) {
            for (ErpB2bAsnStateMachine.TransitionDefinition e : sm.transitions()) {
                assertFalse(e.getFromStatus().equals(terminal),
                        "终态不应有出边: terminal=" + terminal + ", but edge " + e.getAction() + " leaves it");
            }
        }
    }

    // ---------- (c) matchPurchaseOrder 仅 RECEIVED、createReceiveFromAsn 仅 MATCHED ----------

    @Test
    public void testMatchPurchaseOrderAllowsOnlyReceived() {
        assertActionAllowsOnly("matchPurchaseOrder", ErpB2bConstants.ASN_STATUS_RECEIVED);
        assertEquals(ErpB2bConstants.ASN_STATUS_MATCHED, sm.matchPurchaseOrderTargetStatus());
    }

    @Test
    public void testCreateReceiveFromAsnAllowsOnlyMatched() {
        assertActionAllowsOnly("createReceiveFromAsn", ErpB2bConstants.ASN_STATUS_MATCHED);
        assertEquals(ErpB2bConstants.ASN_STATUS_RECEIVED_TO_STOCK, sm.createReceiveFromAsnTargetStatus());
    }

    // ---------- (d) transitions() 一致 ----------

    @Test
    public void testTransitionsMetadataConsistent() {
        for (ErpB2bAsnStateMachine.TransitionDefinition e : sm.transitions()) {
            invokeAssert(e.getAction(), e.getFromStatus());
            assertEquals(e.getToStatus(), targetStatusFor(e.getAction()),
                    "toStatus 与目标态方法不一致: action=" + e.getAction());
        }
    }

    // ---------- (e) CANCELLED 无任何边/不在终态集（D-B2B-2 未落地） ----------

    @Test
    public void testCancelledDeadStateNotInMatrix() {
        for (ErpB2bAsnStateMachine.TransitionDefinition e : sm.transitions()) {
            assertFalse(ErpB2bConstants.ASN_STATUS_CANCELLED.equals(e.getFromStatus()),
                    "CANCELLED 死状态不应作为来源态出现: edge=" + e.getAction());
            assertFalse(ErpB2bConstants.ASN_STATUS_CANCELLED.equals(e.getToStatus()),
                    "CANCELLED 死状态不应作为目标态出现: edge=" + e.getAction());
        }
        // CANCELLED 不在终态集（不可达，未落地）
        assertFalse(sm.isTerminal(ErpB2bConstants.ASN_STATUS_CANCELLED),
                "CANCELLED 不入终态集（D-B2B-2：cancel 未落地）");
        assertFalse(sm.terminalStatuses().contains(ErpB2bConstants.ASN_STATUS_CANCELLED),
                "terminalStatuses() 不含 CANCELLED");
    }

    @Test
    public void testTerminalAndInitialSets() {
        assertEquals(List.of(ErpB2bConstants.ASN_STATUS_RECEIVED_TO_STOCK), sm.terminalStatuses(),
                "终态集合 = {RECEIVED_TO_STOCK}");
        assertEquals(List.of(ErpB2bConstants.ASN_STATUS_RECEIVED), sm.initialStatuses(),
                "初始态集合 = {RECEIVED}");
        assertTrue(sm.isTerminal(ErpB2bConstants.ASN_STATUS_RECEIVED_TO_STOCK));
        assertFalse(sm.isTerminal(ErpB2bConstants.ASN_STATUS_RECEIVED));
        assertFalse(sm.isTerminal(ErpB2bConstants.ASN_STATUS_MATCHED));
    }

    // ---------- (f) retryMatch 幂等判定 helper 正确 ----------

    @Test
    public void testRetryMatchIdempotentHelper() {
        assertTrue(sm.isIdempotentRetryStatus(ErpB2bConstants.ASN_STATUS_MATCHED),
                "retryMatch 幂等短路：MATCHED 应短路");
        assertTrue(sm.isIdempotentRetryStatus(ErpB2bConstants.ASN_STATUS_RECEIVED_TO_STOCK),
                "retryMatch 幂等短路：RECEIVED_TO_STOCK 应短路");
        assertFalse(sm.isIdempotentRetryStatus(ErpB2bConstants.ASN_STATUS_RECEIVED),
                "RECEIVED 不短路（需重新匹配）");
        assertFalse(sm.isIdempotentRetryStatus(ErpB2bConstants.ASN_STATUS_CANCELLED),
                "CANCELLED 不短路（死状态）");
    }

    // ---------- helpers ----------

    private void assertActionAllowsOnly(String action, String allowedFrom) {
        for (String s : ALL_STATUSES) {
            if (allowedFrom.equals(s)) {
                invokeAssert(action, s); // 不抛 = 合法
            } else {
                NopException ex = assertThrows(NopException.class, () -> invokeAssert(action, s),
                        action + " 对非允许来源态应非法: " + s);
                assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                        "Bean 报告 common 层非法迁移码: action=" + action + ", status=" + s);
                assertEquals(action, ex.getParam(ErpB2bAsnStateMachine.ARG_ACTION),
                        "拒绝元数据携带动作名: action=" + action);
                assertEquals(s, ex.getParam(ErpCommonErrors.ARG_CURRENT_STATUS),
                        "拒绝元数据携带当前态: action=" + action + ", status=" + s);
            }
        }
    }

    private void invokeAssert(String action, String status) {
        switch (action) {
            case "matchPurchaseOrder":
                sm.assertCanMatchPurchaseOrder(status);
                break;
            case "createReceiveFromAsn":
                sm.assertCanCreateReceiveFromAsn(status);
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }

    private String targetStatusFor(String action) {
        switch (action) {
            case "matchPurchaseOrder":
                return sm.matchPurchaseOrderTargetStatus();
            case "createReceiveFromAsn":
                return sm.createReceiveFromAsnTargetStatus();
            default:
                throw new IllegalArgumentException("unknown action: " + action);
        }
    }
}
