package app.erp.prj.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.prj.service.ErpPrjConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 项目（{@code ErpPrjProject}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code status}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/projects/state-machine.md} §适用对象一。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载五态迁移矩阵
 * （DRAFT/OPEN/ON_HOLD/COMPLETED/CANCELLED）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 BizModel/Processor（契约 §7）。
 *
 * <p>迁移矩阵（7 条边）：start(DRAFT→OPEN)、hold(OPEN→ON_HOLD)、resume(ON_HOLD→OPEN)、
 * close(OPEN→COMPLETED)、cancel 多源 {DRAFT/OPEN/ON_HOLD→CANCELLED} = 3 边。
 * OPEN↔ON_HOLD 为合法往复；COMPLETED/CANCELLED 终态无出边。
 *
 * <p>cancel 多源 + 终态领域异常重叠（契约 §11.4 警示）：{@link #assertCanCancel(String)} 对终态
 * （COMPLETED/CANCELLED）同样报告 common 非法边，接线时 BizModel 须令终态优先走领域码
 * * {@code ERR_PROJECT_NOT_CLOSABLE}（保持既有外部错误码）、非终态非法走 Bean→领域映射（参照 M1.1
 * {@code ErpCsTicketBizModel.cancel} 范式防冲突）。
 *
 * <p>动态守卫边界（保留 BizModel/Processor）：{@code validateStartPreconditions}（start，config-gated STRICT/WARN）、
 * {@code validateTasksFinished}（close，config-gated STRICT/WARN）、成本归集、乐观锁不属于状态轴判断，本 Bean 不承载。
 */
public class ErpPrjProjectStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    public void assertCanStart(String status) {
        if (!ErpPrjConstants.PROJECT_STATUS_DRAFT.equals(status)) {
            throw illegal("start", status, ErpPrjConstants.PROJECT_STATUS_DRAFT);
        }
    }

    public String startTargetStatus() {
        return ErpPrjConstants.PROJECT_STATUS_OPEN;
    }

    public void assertCanHold(String status) {
        if (!ErpPrjConstants.PROJECT_STATUS_OPEN.equals(status)) {
            throw illegal("hold", status, ErpPrjConstants.PROJECT_STATUS_OPEN);
        }
    }

    public String holdTargetStatus() {
        return ErpPrjConstants.PROJECT_STATUS_ON_HOLD;
    }

    public void assertCanResume(String status) {
        if (!ErpPrjConstants.PROJECT_STATUS_ON_HOLD.equals(status)) {
            throw illegal("resume", status, ErpPrjConstants.PROJECT_STATUS_ON_HOLD);
        }
    }

    public String resumeTargetStatus() {
        return ErpPrjConstants.PROJECT_STATUS_OPEN;
    }

    public void assertCanClose(String status) {
        if (!ErpPrjConstants.PROJECT_STATUS_OPEN.equals(status)) {
            throw illegal("close", status, ErpPrjConstants.PROJECT_STATUS_OPEN);
        }
    }

    public String closeTargetStatus() {
        return ErpPrjConstants.PROJECT_STATUS_COMPLETED;
    }

    /**
     * cancel 守卫：非终态（DRAFT/OPEN/ON_HOLD）均合法。
     *
     * <p>注意：终态（COMPLETED/CANCELLED）的 cancel 由 BizModel 抛领域码 {@code ERR_PROJECT_NOT_CLOSABLE}
     * （保持既有外部错误码，项目域 start/cancel/Hold/Resume/Close 共享此码）。本方法对终态同样报告 common
     * 非法边，接线时 BizModel 须令终态优先走领域码路径（见 plan Phase 2 cancel 防冲突说明 + 契约 §11.4）。
     */
    public void assertCanCancel(String status) {
        if (isTerminal(status)) {
            throw illegal("cancel", status, "非终态(DRAFT/OPEN/ON_HOLD)");
        }
    }

    public String cancelTargetStatus() {
        return ErpPrjConstants.PROJECT_STATUS_CANCELLED;
    }

    // ---------- 终态/初始态分类 ----------

    public boolean isTerminal(String status) {
        return ErpPrjConstants.PROJECT_STATUS_COMPLETED.equals(status)
                || ErpPrjConstants.PROJECT_STATUS_CANCELLED.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("start", ErpPrjConstants.PROJECT_STATUS_DRAFT, ErpPrjConstants.PROJECT_STATUS_OPEN),
                new TransitionDefinition("hold", ErpPrjConstants.PROJECT_STATUS_OPEN, ErpPrjConstants.PROJECT_STATUS_ON_HOLD),
                new TransitionDefinition("resume", ErpPrjConstants.PROJECT_STATUS_ON_HOLD, ErpPrjConstants.PROJECT_STATUS_OPEN),
                new TransitionDefinition("close", ErpPrjConstants.PROJECT_STATUS_OPEN, ErpPrjConstants.PROJECT_STATUS_COMPLETED),
                new TransitionDefinition("cancel", ErpPrjConstants.PROJECT_STATUS_DRAFT, ErpPrjConstants.PROJECT_STATUS_CANCELLED),
                new TransitionDefinition("cancel", ErpPrjConstants.PROJECT_STATUS_OPEN, ErpPrjConstants.PROJECT_STATUS_CANCELLED),
                new TransitionDefinition("cancel", ErpPrjConstants.PROJECT_STATUS_ON_HOLD, ErpPrjConstants.PROJECT_STATUS_CANCELLED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpPrjConstants.PROJECT_STATUS_COMPLETED, ErpPrjConstants.PROJECT_STATUS_CANCELLED));
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpPrjConstants.PROJECT_STATUS_DRAFT);
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
