package app.erp.cs.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.cs.service.ErpCsConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 客服工单（{@code ErpCsTicket}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code status}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/customer-service/state-machine.md}。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载六态迁移矩阵
 * （NEW/ASSIGNED/IN_PROGRESS/RESOLVED/CLOSED/CANCELLED）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor/BizModel（契约 §7）。
 *
 * <p>迁移矩阵（9 条边）：assign(NEW→ASSIGNED)、start(ASSIGNED→IN_PROGRESS)、resolve(IN_PROGRESS→RESOLVED)、
 * close(RESOLVED→CLOSED)、reopen(RESOLVED→IN_PROGRESS)、cancel(NEW/ASSIGNED/IN_PROGRESS/RESOLVED→CANCELLED)。
 */
public class ErpCsTicketStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    public void assertCanAssign(String status) {
        if (!ErpCsConstants.TICKET_STATUS_NEW.equals(status)) {
            throw illegal("assign", status, ErpCsConstants.TICKET_STATUS_NEW);
        }
    }

    public String assignTargetStatus() {
        return ErpCsConstants.TICKET_STATUS_ASSIGNED;
    }

    public void assertCanStart(String status) {
        if (!ErpCsConstants.TICKET_STATUS_ASSIGNED.equals(status)) {
            throw illegal("start", status, ErpCsConstants.TICKET_STATUS_ASSIGNED);
        }
    }

    public String startTargetStatus() {
        return ErpCsConstants.TICKET_STATUS_IN_PROGRESS;
    }

    public void assertCanResolve(String status) {
        if (!ErpCsConstants.TICKET_STATUS_IN_PROGRESS.equals(status)) {
            throw illegal("resolve", status, ErpCsConstants.TICKET_STATUS_IN_PROGRESS);
        }
    }

    public String resolveTargetStatus() {
        return ErpCsConstants.TICKET_STATUS_RESOLVED;
    }

    public void assertCanClose(String status) {
        if (!ErpCsConstants.TICKET_STATUS_RESOLVED.equals(status)) {
            throw illegal("close", status, ErpCsConstants.TICKET_STATUS_RESOLVED);
        }
    }

    public String closeTargetStatus() {
        return ErpCsConstants.TICKET_STATUS_CLOSED;
    }

    public void assertCanReopen(String status) {
        if (!ErpCsConstants.TICKET_STATUS_RESOLVED.equals(status)) {
            throw illegal("reopen", status, ErpCsConstants.TICKET_STATUS_RESOLVED);
        }
    }

    public String reopenTargetStatus() {
        return ErpCsConstants.TICKET_STATUS_IN_PROGRESS;
    }

    /**
     * cancel 守卫：非终态（NEW/ASSIGNED/IN_PROGRESS/RESOLVED）均合法。
     *
     * <p>注意：终态（CLOSED/CANCELLED）的 cancel 由 BizModel 抛领域码 {@code ERR_TICKET_ALREADY_TERMINAL}
     * （保持既有外部错误码）。本方法对终态同样报告 common 非法边，接线时 BizModel 须令终态优先走领域码路径
     * （见 plan Phase 2 cancel 防冲突说明）。
     */
    public void assertCanCancel(String status) {
        if (isTerminal(status)) {
            throw illegal("cancel", status, "非终态(NEW/ASSIGNED/IN_PROGRESS/RESOLVED)");
        }
    }

    public String cancelTargetStatus() {
        return ErpCsConstants.TICKET_STATUS_CANCELLED;
    }

    // ---------- 终态/初始态分类 ----------

    public boolean isTerminal(String status) {
        return ErpCsConstants.TICKET_STATUS_CLOSED.equals(status)
                || ErpCsConstants.TICKET_STATUS_CANCELLED.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("assign", ErpCsConstants.TICKET_STATUS_NEW, ErpCsConstants.TICKET_STATUS_ASSIGNED),
                new TransitionDefinition("start", ErpCsConstants.TICKET_STATUS_ASSIGNED, ErpCsConstants.TICKET_STATUS_IN_PROGRESS),
                new TransitionDefinition("resolve", ErpCsConstants.TICKET_STATUS_IN_PROGRESS, ErpCsConstants.TICKET_STATUS_RESOLVED),
                new TransitionDefinition("close", ErpCsConstants.TICKET_STATUS_RESOLVED, ErpCsConstants.TICKET_STATUS_CLOSED),
                new TransitionDefinition("reopen", ErpCsConstants.TICKET_STATUS_RESOLVED, ErpCsConstants.TICKET_STATUS_IN_PROGRESS),
                new TransitionDefinition("cancel", ErpCsConstants.TICKET_STATUS_NEW, ErpCsConstants.TICKET_STATUS_CANCELLED),
                new TransitionDefinition("cancel", ErpCsConstants.TICKET_STATUS_ASSIGNED, ErpCsConstants.TICKET_STATUS_CANCELLED),
                new TransitionDefinition("cancel", ErpCsConstants.TICKET_STATUS_IN_PROGRESS, ErpCsConstants.TICKET_STATUS_CANCELLED),
                new TransitionDefinition("cancel", ErpCsConstants.TICKET_STATUS_RESOLVED, ErpCsConstants.TICKET_STATUS_CANCELLED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpCsConstants.TICKET_STATUS_CLOSED, ErpCsConstants.TICKET_STATUS_CANCELLED));
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpCsConstants.TICKET_STATUS_NEW);
    }

    // ---------- 内部 ----------

    private static NopException illegal(String action, String currentStatus, String expectedStatus) {
        return new NopException(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                .param(ErpCommonErrors.ARG_CURRENT_STATUS, currentStatus)
                .param(ErpCommonErrors.ARG_EXPECTED_STATUS, expectedStatus)
                .param(ARG_ACTION, action);
    }

    /** 只读迁移定义记录（供 M5.1/M5.2 可达性/完备性分析与文档一致性校验消费）。 */
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
