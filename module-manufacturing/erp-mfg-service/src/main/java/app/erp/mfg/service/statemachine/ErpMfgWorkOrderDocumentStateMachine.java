package app.erp.mfg.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.mfg.service.ErpMfgConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 生产工单（{@code ErpMfgWorkOrder}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code docStatus} 业务生命周期轴）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/manufacturing/state-machine.md} §适用对象一（10 态完整矩阵）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载 docStatus 操作动作迁移矩阵
 * （submit/approve/checkAvailability/start/stop/resume/close/reportCompletion/cancel）+ 终态/初始态分类 +
 * 只读 {@link #transitions()} 元数据。可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>命名带 {@code Document} 后缀（契约 §1 双轴约定，与 {@code ErpMfgWorkOrderApprovalStateMachine} approveStatus 轴分离）。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor（契约 §7）。
 * WorkOrder 领域码为泛型 {@code ERR_INVALID_STATUS_TRANSITION}（路线图 Non-Goal「不借迁移改变既有错误码」）。
 *
 * <p><b>单轴建模约定（plan Phase 1 Decision）</b>：本 Bean 仅承载 docStatus 轴；submit/approve 动作的
 * docStatus 目标写入（{@code doSubmit}→SUBMITTED、{@code doApprove}→NOT_STARTED）与审批轴联动写入保留在
 * facade {@code doXxx} 原位（契约 §9.2 选项 c：联动写入不在 Bean 范围），本 Bean 仅编码 docStatus 侧的
 * 固定来源态守卫（submit 仅 DRAFT、approve 仅 SUBMITTED）与目标态常量。
 *
 * <p><b>checkAvailability 为多目标动作</b>：NOT_STARTED→STOCK_RESERVED 或 STOCK_PARTIAL，目标态由齐套校验
 * 动态结果决定（{@code KitAvailabilityChecker}），故本 Bean 不提供单值 {@code checkAvailabilityTargetStatus()}，
 * facade 保留 {@code wo.setDocStatus(result.getResultingStatus())} 原位写入；{@link #transitions()} 编码两条边。
 * start 的 STOCK_PARTIAL 来源受 config-gated {@code erp-mfg.allow-partial-kit-start} 动态门控（缺省 false），
 * 该配置守卫是动态业务守卫，保留在 facade {@code validateTransitionForStart} 原位（Bean 矩阵层两级来源均合法）。
 */
public class ErpMfgWorkOrderDocumentStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /** submit（docStatus 侧）守卫：仅 DRAFT 合法（审批提交要求工单处于草稿态）。 */
    public void assertCanSubmit(String status) {
        if (!ErpMfgConstants.WORK_ORDER_STATUS_DRAFT.equals(status)) {
            throw illegal("submit", status, ErpMfgConstants.WORK_ORDER_STATUS_DRAFT);
        }
    }

    public String submitTargetStatus() {
        return ErpMfgConstants.WORK_ORDER_STATUS_SUBMITTED;
    }

    /** approve（docStatus 侧）守卫：仅 SUBMITTED 合法（审核通过后工单进入未开始）。 */
    public void assertCanApprove(String status) {
        if (!ErpMfgConstants.WORK_ORDER_STATUS_SUBMITTED.equals(status)) {
            throw illegal("approve", status, ErpMfgConstants.WORK_ORDER_STATUS_SUBMITTED);
        }
    }

    public String approveTargetStatus() {
        return ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED;
    }

    /** checkAvailability 守卫：仅 NOT_STARTED 合法（齐套校验）。目标态为动态结果（STOCK_RESERVED/STOCK_PARTIAL）。 */
    public void assertCanCheckAvailability(String status) {
        if (!ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED.equals(status)) {
            throw illegal("checkAvailability", status, ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED);
        }
    }

    /** start 守卫：来源态 {STOCK_RESERVED, STOCK_PARTIAL}（部分齐套受 config-gated 动态守卫）。 */
    public void assertCanStart(String status) {
        if (!ErpMfgConstants.WORK_ORDER_STATUS_STOCK_RESERVED.equals(status)
                && !ErpMfgConstants.WORK_ORDER_STATUS_STOCK_PARTIAL.equals(status)) {
            throw illegal("start", status,
                    ErpMfgConstants.WORK_ORDER_STATUS_STOCK_RESERVED + " / " + ErpMfgConstants.WORK_ORDER_STATUS_STOCK_PARTIAL);
        }
    }

    public String startTargetStatus() {
        return ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS;
    }

    /** stop 守卫：仅 IN_PROCESS 合法。 */
    public void assertCanStop(String status) {
        if (!ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS.equals(status)) {
            throw illegal("stop", status, ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS);
        }
    }

    public String stopTargetStatus() {
        return ErpMfgConstants.WORK_ORDER_STATUS_STOPPED;
    }

    /** resume 守卫：仅 STOPPED 合法。 */
    public void assertCanResume(String status) {
        if (!ErpMfgConstants.WORK_ORDER_STATUS_STOPPED.equals(status)) {
            throw illegal("resume", status, ErpMfgConstants.WORK_ORDER_STATUS_STOPPED);
        }
    }

    public String resumeTargetStatus() {
        return ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS;
    }

    /** close 守卫：来源态 {STOPPED, IN_PROCESS}（停工或生产中结案）。 */
    public void assertCanClose(String status) {
        if (!ErpMfgConstants.WORK_ORDER_STATUS_STOPPED.equals(status)
                && !ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS.equals(status)) {
            throw illegal("close", status,
                    ErpMfgConstants.WORK_ORDER_STATUS_STOPPED + " / " + ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS);
        }
    }

    public String closeTargetStatus() {
        return ErpMfgConstants.WORK_ORDER_STATUS_CLOSED;
    }

    /** reportCompletion 守卫：仅 IN_PROCESS 合法（完工达量前须生产中）。 */
    public void assertCanReportCompletion(String status) {
        if (!ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS.equals(status)) {
            throw illegal("reportCompletion", status, ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS);
        }
    }

    public String reportCompletionTargetStatus() {
        return ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED;
    }

    /** cancel 守卫：来源态 {DRAFT, SUBMITTED, NOT_STARTED}（未开工可取消）。 */
    public void assertCanCancel(String status) {
        if (!ErpMfgConstants.WORK_ORDER_STATUS_DRAFT.equals(status)
                && !ErpMfgConstants.WORK_ORDER_STATUS_SUBMITTED.equals(status)
                && !ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED.equals(status)) {
            throw illegal("cancel", status,
                    ErpMfgConstants.WORK_ORDER_STATUS_DRAFT + " / " + ErpMfgConstants.WORK_ORDER_STATUS_SUBMITTED
                            + " / " + ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED);
        }
    }

    public String cancelTargetStatus() {
        return ErpMfgConstants.WORK_ORDER_STATUS_CANCELLED;
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 终态分类：COMPLETED / CLOSED / CANCELLED 均为终态（owner doc §适用对象一 §3）。
     */
    public boolean isTerminal(String status) {
        return ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED.equals(status)
                || ErpMfgConstants.WORK_ORDER_STATUS_CLOSED.equals(status)
                || ErpMfgConstants.WORK_ORDER_STATUS_CANCELLED.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("submit",
                        ErpMfgConstants.WORK_ORDER_STATUS_DRAFT, ErpMfgConstants.WORK_ORDER_STATUS_SUBMITTED),
                new TransitionDefinition("approve",
                        ErpMfgConstants.WORK_ORDER_STATUS_SUBMITTED, ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED),
                new TransitionDefinition("checkAvailability",
                        ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED, ErpMfgConstants.WORK_ORDER_STATUS_STOCK_RESERVED),
                new TransitionDefinition("checkAvailability",
                        ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED, ErpMfgConstants.WORK_ORDER_STATUS_STOCK_PARTIAL),
                new TransitionDefinition("start",
                        ErpMfgConstants.WORK_ORDER_STATUS_STOCK_RESERVED, ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS),
                new TransitionDefinition("start",
                        ErpMfgConstants.WORK_ORDER_STATUS_STOCK_PARTIAL, ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS),
                new TransitionDefinition("stop",
                        ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS, ErpMfgConstants.WORK_ORDER_STATUS_STOPPED),
                new TransitionDefinition("resume",
                        ErpMfgConstants.WORK_ORDER_STATUS_STOPPED, ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS),
                new TransitionDefinition("close",
                        ErpMfgConstants.WORK_ORDER_STATUS_STOPPED, ErpMfgConstants.WORK_ORDER_STATUS_CLOSED),
                new TransitionDefinition("close",
                        ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS, ErpMfgConstants.WORK_ORDER_STATUS_CLOSED),
                new TransitionDefinition("reportCompletion",
                        ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS, ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED),
                new TransitionDefinition("cancel",
                        ErpMfgConstants.WORK_ORDER_STATUS_DRAFT, ErpMfgConstants.WORK_ORDER_STATUS_CANCELLED),
                new TransitionDefinition("cancel",
                        ErpMfgConstants.WORK_ORDER_STATUS_SUBMITTED, ErpMfgConstants.WORK_ORDER_STATUS_CANCELLED),
                new TransitionDefinition("cancel",
                        ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED, ErpMfgConstants.WORK_ORDER_STATUS_CANCELLED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED,
                ErpMfgConstants.WORK_ORDER_STATUS_CLOSED,
                ErpMfgConstants.WORK_ORDER_STATUS_CANCELLED));
    }

    public List<String> initialStatuses() {
        return Collections.unmodifiableList(Collections.singletonList(ErpMfgConstants.WORK_ORDER_STATUS_DRAFT));
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
