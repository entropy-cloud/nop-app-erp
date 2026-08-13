package app.erp.qa.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.qa.service.ErpQaConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 召回事件（{@code ErpQaRecall}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code status} 操作轴）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/quality/recall.md}（§召回状态机 5 态 OPEN/APPROVED/IN_PROGRESS/CLOSED/CANCELLED）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载 status 操作轴动作迁移矩阵
 * + 终态/初始态分类 + 只读 {@link #transitions()} 元数据。可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p><b>审批联动边</b>（plan Phase 3 Decision (B)）：approve（审批通过）联动写 status=APPROVED、reject（审批驳回）
 * 联动写 status=CANCELLED。Bean 按单轴建模，将这两条联动边纳入 status 矩阵（文档完整性 + 全部 writer 盘点），
 * 但实际联动写入保留在 facade {@code ErpQaRecallProcessor.doApprove/doReject} 原位。approve 的 status 来源态守卫
 * （须 OPEN）经 facade {@code validateBusinessRulesForApprove} 委托本 Bean {@link #assertCanApprove}。
 *
 * <p><b>register 初始写</b>（§9.2 选项 c）：register 写 status=OPEN 为初始态写入，不经 Bean assert；
 * Bean 提供 {@link #registerTargetStatus()} 供 RegisterProcessor 写回（替代硬编码常量）。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor/BizModel/facade（契约 §7）。
 */
public class ErpQaRecallStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /** approve 联动守卫：来源态为 {@code OPEN} 合法（审批通过联动写 status=APPROVED）。 */
    public void assertCanApprove(String status) {
        if (!ErpQaConstants.RECALL_STATUS_OPEN.equals(status)) {
            throw illegal("approve", status, ErpQaConstants.RECALL_STATUS_OPEN);
        }
    }

    /** locateTargets 守卫：来源态为 {@code APPROVED} 合法。 */
    public void assertCanLocateTargets(String status) {
        if (!ErpQaConstants.RECALL_STATUS_APPROVED.equals(status)) {
            throw illegal("locateTargets", status, ErpQaConstants.RECALL_STATUS_APPROVED);
        }
    }

    /** close 守卫：来源态为 {@code IN_PROGRESS} 合法（通知门控为动态守卫，保留在 Processor）。 */
    public void assertCanClose(String status) {
        if (!ErpQaConstants.RECALL_STATUS_IN_PROGRESS.equals(status)) {
            throw illegal("close", status, ErpQaConstants.RECALL_STATUS_IN_PROGRESS);
        }
    }

    /** cancel 守卫：来源态为 {@code OPEN}/{@code APPROVED}/{@code IN_PROGRESS} 合法（多来源态动作）。 */
    public void assertCanCancel(String status) {
        if (!ErpQaConstants.RECALL_STATUS_OPEN.equals(status)
                && !ErpQaConstants.RECALL_STATUS_APPROVED.equals(status)
                && !ErpQaConstants.RECALL_STATUS_IN_PROGRESS.equals(status)) {
            throw illegal("cancel", status,
                    ErpQaConstants.RECALL_STATUS_OPEN + " 或 " + ErpQaConstants.RECALL_STATUS_APPROVED
                            + " 或 " + ErpQaConstants.RECALL_STATUS_IN_PROGRESS);
        }
    }

    // ---------- 动作目标态（供 Processor/BizModel/facade 写回） ----------

    /** register 初始写目标态=OPEN（§9.2 选项 c，不经 assert）。 */
    public String registerTargetStatus() {
        return ErpQaConstants.RECALL_STATUS_OPEN;
    }

    /** approve 联动目标态=APPROVED（facade doApprove 写 status）。 */
    public String approveTargetStatus() {
        return ErpQaConstants.RECALL_STATUS_APPROVED;
    }

    public String locateTargetsTargetStatus() {
        return ErpQaConstants.RECALL_STATUS_IN_PROGRESS;
    }

    public String closeTargetStatus() {
        return ErpQaConstants.RECALL_STATUS_CLOSED;
    }

    /** reject 联动目标态=CANCELLED（facade doReject 写 status）。 */
    public String rejectTargetStatus() {
        return ErpQaConstants.RECALL_STATUS_CANCELLED;
    }

    public String cancelTargetStatus() {
        return ErpQaConstants.RECALL_STATUS_CANCELLED;
    }

    // ---------- 终态/初始态分类 ----------

    /** 业务终态判定。status 轴终态为 CLOSED/CANCELLED（owner doc §召回状态机，不可恢复）。 */
    public boolean isTerminal(String status) {
        return ErpQaConstants.RECALL_STATUS_CLOSED.equals(status)
                || ErpQaConstants.RECALL_STATUS_CANCELLED.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("approve", ErpQaConstants.RECALL_STATUS_OPEN, ErpQaConstants.RECALL_STATUS_APPROVED),
                new TransitionDefinition("locateTargets", ErpQaConstants.RECALL_STATUS_APPROVED, ErpQaConstants.RECALL_STATUS_IN_PROGRESS),
                new TransitionDefinition("close", ErpQaConstants.RECALL_STATUS_IN_PROGRESS, ErpQaConstants.RECALL_STATUS_CLOSED),
                new TransitionDefinition("reject", ErpQaConstants.RECALL_STATUS_OPEN, ErpQaConstants.RECALL_STATUS_CANCELLED),
                new TransitionDefinition("cancel", ErpQaConstants.RECALL_STATUS_OPEN, ErpQaConstants.RECALL_STATUS_CANCELLED),
                new TransitionDefinition("cancel", ErpQaConstants.RECALL_STATUS_APPROVED, ErpQaConstants.RECALL_STATUS_CANCELLED),
                new TransitionDefinition("cancel", ErpQaConstants.RECALL_STATUS_IN_PROGRESS, ErpQaConstants.RECALL_STATUS_CANCELLED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpQaConstants.RECALL_STATUS_CLOSED,
                ErpQaConstants.RECALL_STATUS_CANCELLED));
    }

    public List<String> initialStatuses() {
        return Collections.unmodifiableList(Arrays.asList(ErpQaConstants.RECALL_STATUS_OPEN));
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
