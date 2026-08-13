package app.erp.mnt.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.mnt.dao.ErpMntDaoConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 维护访问（{@code ErpMntVisit}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code status}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/maintenance/state-machine.md §适用对象一：维护访问}。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载五态迁移矩阵
 * （DRAFT/SCHEDULED/IN_PROGRESS/COMPLETED/CANCELLED）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor/BizModel（契约 §7）。
 *
 * <p>迁移矩阵（6 条边）：schedule(DRAFT→SCHEDULED)、start(SCHEDULED→IN_PROGRESS)、complete(IN_PROGRESS→COMPLETED)、
 * cancel(DRAFT→CANCELLED)、cancel(SCHEDULED→CANCELLED)、cancel(IN_PROGRESS→CANCELLED)。
 *
 * <p>{@code cancel} 三源语义：DRAFT/SCHEDULED/IN_PROGRESS 均允许 → CANCELLED（保持既有 non-terminal→CANCELLED 业务路径）。
 */
public class ErpMntVisitStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    public void assertCanSchedule(String status) {
        if (!ErpMntDaoConstants.VISIT_STATUS_DRAFT.equals(status)) {
            throw illegal("schedule", status, ErpMntDaoConstants.VISIT_STATUS_DRAFT);
        }
    }

    public String scheduleTargetStatus() {
        return ErpMntDaoConstants.VISIT_STATUS_SCHEDULED;
    }

    public void assertCanStart(String status) {
        if (!ErpMntDaoConstants.VISIT_STATUS_SCHEDULED.equals(status)) {
            throw illegal("start", status, ErpMntDaoConstants.VISIT_STATUS_SCHEDULED);
        }
    }

    public String startTargetStatus() {
        return ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS;
    }

    public void assertCanComplete(String status) {
        if (!ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS.equals(status)) {
            throw illegal("complete", status, ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS);
        }
    }

    public String completeTargetStatus() {
        return ErpMntDaoConstants.VISIT_STATUS_COMPLETED;
    }

    /**
     * cancel 守卫：仅 DRAFT/SCHEDULED/IN_PROGRESS 合法（三源语义——保持既有 non-terminal→CANCELLED 业务路径）。
     */
    public void assertCanCancel(String status) {
        if (!ErpMntDaoConstants.VISIT_STATUS_DRAFT.equals(status)
                && !ErpMntDaoConstants.VISIT_STATUS_SCHEDULED.equals(status)
                && !ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS.equals(status)) {
            throw illegal("cancel", status, "非终态");
        }
    }

    public String cancelTargetStatus() {
        return ErpMntDaoConstants.VISIT_STATUS_CANCELLED;
    }

    // ---------- 终态/初始态分类 ----------

    public boolean isTerminal(String status) {
        return ErpMntDaoConstants.VISIT_STATUS_COMPLETED.equals(status)
                || ErpMntDaoConstants.VISIT_STATUS_CANCELLED.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("schedule", ErpMntDaoConstants.VISIT_STATUS_DRAFT, ErpMntDaoConstants.VISIT_STATUS_SCHEDULED),
                new TransitionDefinition("start", ErpMntDaoConstants.VISIT_STATUS_SCHEDULED, ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS),
                new TransitionDefinition("complete", ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS, ErpMntDaoConstants.VISIT_STATUS_COMPLETED),
                new TransitionDefinition("cancel", ErpMntDaoConstants.VISIT_STATUS_DRAFT, ErpMntDaoConstants.VISIT_STATUS_CANCELLED),
                new TransitionDefinition("cancel", ErpMntDaoConstants.VISIT_STATUS_SCHEDULED, ErpMntDaoConstants.VISIT_STATUS_CANCELLED),
                new TransitionDefinition("cancel", ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS, ErpMntDaoConstants.VISIT_STATUS_CANCELLED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpMntDaoConstants.VISIT_STATUS_COMPLETED,
                ErpMntDaoConstants.VISIT_STATUS_CANCELLED));
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpMntDaoConstants.VISIT_STATUS_DRAFT);
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
