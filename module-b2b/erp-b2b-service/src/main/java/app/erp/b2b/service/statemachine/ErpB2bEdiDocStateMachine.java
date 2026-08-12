package app.erp.b2b.service.statemachine;

import app.erp.b2b.service.ErpB2bConstants;
import app.erp.common.service.ErpCommonErrors;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * EDI 事务信封（{@code ErpB2bEdiDoc}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code state}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/b2b/state-machine.md §1-§3}。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载八态迁移矩阵
 * （TO_SEND/SENT/CANCELLED/ERROR/RECEIVED/ACKNOWLEDGED/ARCHIVED + dict 预留死状态 TO_CANCEL）
 * + 终态/初始态分类 + 只读 {@link #transitions()} 元数据。可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 BizModel（契约 §7）。
 *
 * <h2>已实现迁移矩阵（6 声明动作，11 边）</h2>
 * <ul>
 *   <li>markSent(TO_SEND→SENT)</li>
 *   <li>markAcknowledged(SENT→ACKNOWLEDGED)</li>
 *   <li>markError(TO_SEND|SENT|RECEIVED→ERROR) —— 经 D-B2B-3 裁定从「任意态」收紧为文档来源</li>
 *   <li>retry(ERROR→TO_SEND 出站 / ERROR→RECEIVED 入站) —— 目标按方向，唯一的多目标动作</li>
 *   <li>cancel(TO_SEND|SENT|ERROR→CANCELLED) —— 多来源</li>
 *   <li>archive(RECEIVED→ARCHIVED)</li>
 * </ul>
 *
 * <h2>死状态 / 预留状态裁定</h2>
 * <ul>
 *   <li><b>TO_CANCEL</b>（D-B2B-1）：dict 预留死状态（owner doc §2 注：两步取消 Deferred）。
 *       本 Bean <b>不编码任何 TO_CANCEL 边</b>（无 {@code assertCan*ToCancel}、{@code transitions()} 不含 TO_CANCEL）。
 *       生产取消经单步 SENT→CANCELLED 等价简化。Successor：两步取消业务流落地时新增边。</li>
 *   <li><b>CANCELLED/ACKNOWLEDGED/ARCHIVED</b>：终态，无出边。</li>
 *   <li><b>ERROR</b>：可恢复（retry 出/入站两路径）。</li>
 * </ul>
 */
public class ErpB2bEdiDocStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    public void assertCanMarkSent(String state) {
        if (!ErpB2bConstants.EDI_DOC_STATE_TO_SEND.equals(state)) {
            throw illegal("markSent", state, ErpB2bConstants.EDI_DOC_STATE_TO_SEND);
        }
    }

    public String markSentTargetStatus() {
        return ErpB2bConstants.EDI_DOC_STATE_SENT;
    }

    public void assertCanMarkAcknowledged(String state) {
        if (!ErpB2bConstants.EDI_DOC_STATE_SENT.equals(state)) {
            throw illegal("markAcknowledged", state, ErpB2bConstants.EDI_DOC_STATE_SENT);
        }
    }

    public String markAcknowledgedTargetStatus() {
        return ErpB2bConstants.EDI_DOC_STATE_ACKNOWLEDGED;
    }

    /**
     * markError 守卫（D-B2B-3 Fix）：仅允许 {@code TO_SEND}/{@code SENT}（出站）+ {@code RECEIVED}（入站）。
     *
     * <p><b>行为变化（已显式裁定）</b>：原生产代码 {@code ErpB2bEdiDocBizModel.markError} 无守卫（任意态→ERROR，
     * 含终态 CANCELLED/ACKNOWLEDGED/ARCHIVED→ERROR）。经 D-B2B-3 层 2 四方对照裁定为 implementation drift → Fix，
     * 收紧为 owner doc §1-§3 文档来源。终态/ERROR 本身 markError 现抛领域码（经 BizModel 映射）。
     */
    public void assertCanMarkError(String state) {
        if (!ErpB2bConstants.EDI_DOC_STATE_TO_SEND.equals(state)
                && !ErpB2bConstants.EDI_DOC_STATE_SENT.equals(state)
                && !ErpB2bConstants.EDI_DOC_STATE_RECEIVED.equals(state)) {
            throw illegal("markError", state, "TO_SEND/SENT/RECEIVED");
        }
    }

    public String markErrorTargetStatus() {
        return ErpB2bConstants.EDI_DOC_STATE_ERROR;
    }

    /**
     * retry 守卫：仅 ERROR 合法。目标态按出/入站方向，由 BizModel 经
     * {@link #retryOutboundTargetStatus()} / {@link #retryInboundTargetStatus()} 选择。
     */
    public void assertCanRetry(String state) {
        if (!ErpB2bConstants.EDI_DOC_STATE_ERROR.equals(state)) {
            throw illegal("retry", state, ErpB2bConstants.EDI_DOC_STATE_ERROR);
        }
    }

    /** 出站 retry 目标态（ERROR→TO_SEND）。 */
    public String retryOutboundTargetStatus() {
        return ErpB2bConstants.EDI_DOC_STATE_TO_SEND;
    }

    /** 入站 retry 目标态（ERROR→RECEIVED）。 */
    public String retryInboundTargetStatus() {
        return ErpB2bConstants.EDI_DOC_STATE_RECEIVED;
    }

    /**
     * cancel 守卫：多来源 {@code TO_SEND}/{@code SENT}/{@code ERROR} 合法；终态
     * （{@code CANCELLED}/{@code ACKNOWLEDGED}/{@code ARCHIVED}）+ dict 死状态 {@code TO_CANCEL}/{@code RECEIVED} 非法。
     */
    public void assertCanCancel(String state) {
        if (!ErpB2bConstants.EDI_DOC_STATE_TO_SEND.equals(state)
                && !ErpB2bConstants.EDI_DOC_STATE_SENT.equals(state)
                && !ErpB2bConstants.EDI_DOC_STATE_ERROR.equals(state)) {
            throw illegal("cancel", state, "TO_SEND/SENT/ERROR");
        }
    }

    public String cancelTargetStatus() {
        return ErpB2bConstants.EDI_DOC_STATE_CANCELLED;
    }

    public void assertCanArchive(String state) {
        if (!ErpB2bConstants.EDI_DOC_STATE_RECEIVED.equals(state)) {
            throw illegal("archive", state, ErpB2bConstants.EDI_DOC_STATE_RECEIVED);
        }
    }

    public String archiveTargetStatus() {
        return ErpB2bConstants.EDI_DOC_STATE_ARCHIVED;
    }

    // ---------- 终态/初始态分类 ----------

    public boolean isTerminal(String state) {
        return ErpB2bConstants.EDI_DOC_STATE_CANCELLED.equals(state)
                || ErpB2bConstants.EDI_DOC_STATE_ACKNOWLEDGED.equals(state)
                || ErpB2bConstants.EDI_DOC_STATE_ARCHIVED.equals(state);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 BizModel 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("markSent", ErpB2bConstants.EDI_DOC_STATE_TO_SEND, ErpB2bConstants.EDI_DOC_STATE_SENT),
                new TransitionDefinition("markAcknowledged", ErpB2bConstants.EDI_DOC_STATE_SENT, ErpB2bConstants.EDI_DOC_STATE_ACKNOWLEDGED),
                new TransitionDefinition("markError", ErpB2bConstants.EDI_DOC_STATE_TO_SEND, ErpB2bConstants.EDI_DOC_STATE_ERROR),
                new TransitionDefinition("markError", ErpB2bConstants.EDI_DOC_STATE_SENT, ErpB2bConstants.EDI_DOC_STATE_ERROR),
                new TransitionDefinition("markError", ErpB2bConstants.EDI_DOC_STATE_RECEIVED, ErpB2bConstants.EDI_DOC_STATE_ERROR),
                // retry 为方向依赖多目标动作：ERROR→TO_SEND（出站）/ ERROR→RECEIVED（入站）
                new TransitionDefinition("retry", ErpB2bConstants.EDI_DOC_STATE_ERROR, ErpB2bConstants.EDI_DOC_STATE_TO_SEND),
                new TransitionDefinition("retry", ErpB2bConstants.EDI_DOC_STATE_ERROR, ErpB2bConstants.EDI_DOC_STATE_RECEIVED),
                new TransitionDefinition("cancel", ErpB2bConstants.EDI_DOC_STATE_TO_SEND, ErpB2bConstants.EDI_DOC_STATE_CANCELLED),
                new TransitionDefinition("cancel", ErpB2bConstants.EDI_DOC_STATE_SENT, ErpB2bConstants.EDI_DOC_STATE_CANCELLED),
                new TransitionDefinition("cancel", ErpB2bConstants.EDI_DOC_STATE_ERROR, ErpB2bConstants.EDI_DOC_STATE_CANCELLED),
                new TransitionDefinition("archive", ErpB2bConstants.EDI_DOC_STATE_RECEIVED, ErpB2bConstants.EDI_DOC_STATE_ARCHIVED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpB2bConstants.EDI_DOC_STATE_CANCELLED,
                ErpB2bConstants.EDI_DOC_STATE_ACKNOWLEDGED,
                ErpB2bConstants.EDI_DOC_STATE_ARCHIVED));
    }

    public List<String> initialStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpB2bConstants.EDI_DOC_STATE_TO_SEND,
                ErpB2bConstants.EDI_DOC_STATE_RECEIVED));
    }

    // ---------- 内部 ----------

    private static NopException illegal(String action, String currentState, String expectedState) {
        return new NopException(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                .param(ErpCommonErrors.ARG_CURRENT_STATUS, currentState)
                .param(ErpCommonErrors.ARG_EXPECTED_STATUS, expectedState)
                .param(ARG_ACTION, action);
    }

    /**
     * 只读迁移定义记录（供 M5.1/M5.2 可达性/完备性分析与文档一致性校验消费）。
     *
     * <p>字段名 {@code fromStatus/toStatus} 为通用契约命名；本轴 {@code state} 字段语义等价。
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
