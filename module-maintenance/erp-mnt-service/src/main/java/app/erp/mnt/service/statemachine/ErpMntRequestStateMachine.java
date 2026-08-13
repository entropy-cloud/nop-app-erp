package app.erp.mnt.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.mnt.dao.ErpMntDaoConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 维护请求（{@code ErpMntRequest}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code status}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/maintenance/state-machine.md §适用对象二：维护请求}。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载六态迁移矩阵
 * （OPEN/ACCEPTED/IN_PROGRESS/COMPLETED/REJECTED/CANCELLED）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor/BizModel（契约 §7）。
 *
 * <p>迁移矩阵（7 条边）：accept(OPEN→ACCEPTED)、startRepair(ACCEPTED→IN_PROGRESS)、
 * complete(IN_PROGRESS→COMPLETED)、rejectRequest(OPEN→REJECTED)、rejectRequest(ACCEPTED→REJECTED)、
 * cancel(OPEN→CANCELLED)、cancel(ACCEPTED→CANCELLED)。
 *
 * <p>双源语义：{@code rejectRequest} 与 {@code cancel} 均允许 OPEN/ACCEPTED 两个来源态（保持既有业务路径：
 * 拒绝已受理请求 / 取消已受理请求）。
 */
public class ErpMntRequestStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    public void assertCanAccept(String status) {
        if (!ErpMntDaoConstants.REQUEST_STATUS_OPEN.equals(status)) {
            throw illegal("accept", status, ErpMntDaoConstants.REQUEST_STATUS_OPEN);
        }
    }

    public String acceptTargetStatus() {
        return ErpMntDaoConstants.REQUEST_STATUS_ACCEPTED;
    }

    public void assertCanStartRepair(String status) {
        if (!ErpMntDaoConstants.REQUEST_STATUS_ACCEPTED.equals(status)) {
            throw illegal("startRepair", status, ErpMntDaoConstants.REQUEST_STATUS_ACCEPTED);
        }
    }

    public String startRepairTargetStatus() {
        return ErpMntDaoConstants.REQUEST_STATUS_IN_PROGRESS;
    }

    public void assertCanComplete(String status) {
        if (!ErpMntDaoConstants.REQUEST_STATUS_IN_PROGRESS.equals(status)) {
            throw illegal("complete", status, ErpMntDaoConstants.REQUEST_STATUS_IN_PROGRESS);
        }
    }

    public String completeTargetStatus() {
        return ErpMntDaoConstants.REQUEST_STATUS_COMPLETED;
    }

    /**
     * rejectRequest 守卫：仅 OPEN/ACCEPTED 合法（双源语义——保持既有拒绝已受理请求的业务路径）。
     */
    public void assertCanRejectRequest(String status) {
        if (!ErpMntDaoConstants.REQUEST_STATUS_OPEN.equals(status)
                && !ErpMntDaoConstants.REQUEST_STATUS_ACCEPTED.equals(status)) {
            throw illegal("rejectRequest", status, "OPEN 或 ACCEPTED");
        }
    }

    public String rejectRequestTargetStatus() {
        return ErpMntDaoConstants.REQUEST_STATUS_REJECTED;
    }

    /**
     * cancel 守卫：仅 OPEN/ACCEPTED 合法（双源语义——保持既有取消已受理请求的业务路径）。
     */
    public void assertCanCancel(String status) {
        if (!ErpMntDaoConstants.REQUEST_STATUS_OPEN.equals(status)
                && !ErpMntDaoConstants.REQUEST_STATUS_ACCEPTED.equals(status)) {
            throw illegal("cancel", status, "OPEN 或 ACCEPTED");
        }
    }

    public String cancelTargetStatus() {
        return ErpMntDaoConstants.REQUEST_STATUS_CANCELLED;
    }

    // ---------- 终态/初始态分类 ----------

    public boolean isTerminal(String status) {
        return ErpMntDaoConstants.REQUEST_STATUS_COMPLETED.equals(status)
                || ErpMntDaoConstants.REQUEST_STATUS_REJECTED.equals(status)
                || ErpMntDaoConstants.REQUEST_STATUS_CANCELLED.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("accept", ErpMntDaoConstants.REQUEST_STATUS_OPEN, ErpMntDaoConstants.REQUEST_STATUS_ACCEPTED),
                new TransitionDefinition("startRepair", ErpMntDaoConstants.REQUEST_STATUS_ACCEPTED, ErpMntDaoConstants.REQUEST_STATUS_IN_PROGRESS),
                new TransitionDefinition("complete", ErpMntDaoConstants.REQUEST_STATUS_IN_PROGRESS, ErpMntDaoConstants.REQUEST_STATUS_COMPLETED),
                new TransitionDefinition("rejectRequest", ErpMntDaoConstants.REQUEST_STATUS_OPEN, ErpMntDaoConstants.REQUEST_STATUS_REJECTED),
                new TransitionDefinition("rejectRequest", ErpMntDaoConstants.REQUEST_STATUS_ACCEPTED, ErpMntDaoConstants.REQUEST_STATUS_REJECTED),
                new TransitionDefinition("cancel", ErpMntDaoConstants.REQUEST_STATUS_OPEN, ErpMntDaoConstants.REQUEST_STATUS_CANCELLED),
                new TransitionDefinition("cancel", ErpMntDaoConstants.REQUEST_STATUS_ACCEPTED, ErpMntDaoConstants.REQUEST_STATUS_CANCELLED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpMntDaoConstants.REQUEST_STATUS_COMPLETED,
                ErpMntDaoConstants.REQUEST_STATUS_REJECTED,
                ErpMntDaoConstants.REQUEST_STATUS_CANCELLED));
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpMntDaoConstants.REQUEST_STATUS_OPEN);
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
