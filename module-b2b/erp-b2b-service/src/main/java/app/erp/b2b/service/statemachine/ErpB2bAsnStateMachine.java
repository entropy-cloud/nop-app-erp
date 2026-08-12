package app.erp.b2b.service.statemachine;

import app.erp.b2b.service.ErpB2bConstants;
import app.erp.common.service.ErpCommonErrors;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * B2B 提前发货通知（{@code ErpB2bAsn}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code status}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/b2b/state-machine.md §ASN}。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载四态迁移矩阵
 * （RECEIVED/MATCHED/RECEIVED_TO_STOCK + dict 预留死状态 CANCELLED）+ 终态/初始态分类
 * + 只读 {@link #transitions()} 元数据。可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor（契约 §7）。
 *
 * <h2>已实现迁移矩阵（2 边）</h2>
 * <ul>
 *   <li>matchPurchaseOrder(RECEIVED→MATCHED)</li>
 *   <li>createReceiveFromAsn(MATCHED→RECEIVED_TO_STOCK)</li>
 * </ul>
 *
 * <h2>死状态 / 未落地裁定</h2>
 * <ul>
 *   <li><b>CANCELLED</b>（D-B2B-2）：owner doc ASN 段列 cancel 边但<b>全域零 cancel writer、零 cancel mutation</b>
 *       （doc drift，owner doc 已补注）。本 Bean <b>不编码 cancel 边</b>，CANCELLED <b>不入终态集</b>（不可达）。
 *       Successor：PM 要求 ASN cancel 命名动作时开独立 plan。</li>
 *   <li><b>RECEIVED_TO_STOCK</b>：终态，无出边。</li>
 *   <li><b>retryMatch</b>：幂等重置（MATCHED/RECEIVED_TO_STOCK 短路 + 必要时回到 RECEIVED 重新匹配），
 *       非矩阵迁移边。本 Bean 提供 {@link #isIdempotentRetryStatus(String)} helper 供 Processor 短路判定。</li>
 * </ul>
 */
public class ErpB2bAsnStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    public void assertCanMatchPurchaseOrder(String status) {
        if (!ErpB2bConstants.ASN_STATUS_RECEIVED.equals(status)) {
            throw illegal("matchPurchaseOrder", status, ErpB2bConstants.ASN_STATUS_RECEIVED);
        }
    }

    public String matchPurchaseOrderTargetStatus() {
        return ErpB2bConstants.ASN_STATUS_MATCHED;
    }

    public void assertCanCreateReceiveFromAsn(String status) {
        if (!ErpB2bConstants.ASN_STATUS_MATCHED.equals(status)) {
            throw illegal("createReceiveFromAsn", status, ErpB2bConstants.ASN_STATUS_MATCHED);
        }
    }

    public String createReceiveFromAsnTargetStatus() {
        return ErpB2bConstants.ASN_STATUS_RECEIVED_TO_STOCK;
    }

    /**
     * retryMatch 幂等短路判定：{@code MATCHED}/{@code RECEIVED_TO_STOCK} 时 retryMatch 直接返回（无副作用）。
     *
     * <p>非矩阵迁移边（retryMatch 是幂等重置 + 委托 match，不是独立状态边）。Processor 用本 helper 短路，
     * 其余状态回到 RECEIVED 重新匹配（动态行为，保留原位）。
     */
    public boolean isIdempotentRetryStatus(String status) {
        return ErpB2bConstants.ASN_STATUS_MATCHED.equals(status)
                || ErpB2bConstants.ASN_STATUS_RECEIVED_TO_STOCK.equals(status);
    }

    // ---------- 终态/初始态分类 ----------

    public boolean isTerminal(String status) {
        return ErpB2bConstants.ASN_STATUS_RECEIVED_TO_STOCK.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("matchPurchaseOrder",
                        ErpB2bConstants.ASN_STATUS_RECEIVED, ErpB2bConstants.ASN_STATUS_MATCHED),
                new TransitionDefinition("createReceiveFromAsn",
                        ErpB2bConstants.ASN_STATUS_MATCHED, ErpB2bConstants.ASN_STATUS_RECEIVED_TO_STOCK)));
    }

    public List<String> terminalStatuses() {
        return Collections.singletonList(ErpB2bConstants.ASN_STATUS_RECEIVED_TO_STOCK);
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpB2bConstants.ASN_STATUS_RECEIVED);
    }

    // ---------- 内部 ----------

    private static NopException illegal(String action, String currentStatus, String expectedStatus) {
        return new NopException(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                .param(ErpCommonErrors.ARG_CURRENT_STATUS, currentStatus)
                .param(ErpCommonErrors.ARG_EXPECTED_STATUS, expectedStatus)
                .param(ARG_ACTION, action);
    }

    /**
     * 只读迁移定义记录（供 M5.1/M5.2 可达性/完备性分析与文档一致性校验消费）。
     */
    public static final class TransitionDefinition {
        private final String action;
        private final String fromStatus;
        private final String toStatus;

        TransitionDefinition(String action, String fromStatus, String toStatus) {
            this.action = action;
            this.fromStatus = fromStatus;
            this.toStatus = toStatus;
        }

        public String getAction() {
            return action;
        }

        public String getFromStatus() {
            return fromStatus;
        }

        public String getToStatus() {
            return toStatus;
        }
    }
}
