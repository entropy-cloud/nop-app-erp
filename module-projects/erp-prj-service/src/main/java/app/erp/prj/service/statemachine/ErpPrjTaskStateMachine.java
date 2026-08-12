package app.erp.prj.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.prj.service.ErpPrjConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 任务（{@code ErpPrjTask}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code status}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/projects/state-machine.md} §适用对象二。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载四态迁移矩阵
 * （TODO/IN_PROGRESS/DONE/BLOCKED）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 BizModel/Processor（契约 §7）。
 *
 * <p>迁移矩阵（4 条边）：start(TODO→IN_PROGRESS)、complete(IN_PROGRESS→DONE)、
 * block(IN_PROGRESS→BLOCKED)、unblock(BLOCKED→IN_PROGRESS)。IN_PROGRESS↔BLOCKED 为合法往复；
 * DONE 终态无出边。任务取消是项目取消的隐含语义（非 task 轴状态），本 Bean 不编码 CANCELLED。
 *
 * <p>动态守卫边界（保留 BizModel）：任务依赖（{@code dependsOn}）DAG 成环检测、前置任务完成检查
 * （config-gated STRICT/WARN）不属于状态轴判断，本 Bean 不承载。
 */
public class ErpPrjTaskStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    public void assertCanStart(String status) {
        if (!ErpPrjConstants.TASK_STATUS_TODO.equals(status)) {
            throw illegal("start", status, ErpPrjConstants.TASK_STATUS_TODO);
        }
    }

    public String startTargetStatus() {
        return ErpPrjConstants.TASK_STATUS_IN_PROGRESS;
    }

    public void assertCanComplete(String status) {
        if (!ErpPrjConstants.TASK_STATUS_IN_PROGRESS.equals(status)) {
            throw illegal("complete", status, ErpPrjConstants.TASK_STATUS_IN_PROGRESS);
        }
    }

    public String completeTargetStatus() {
        return ErpPrjConstants.TASK_STATUS_DONE;
    }

    public void assertCanBlock(String status) {
        if (!ErpPrjConstants.TASK_STATUS_IN_PROGRESS.equals(status)) {
            throw illegal("block", status, ErpPrjConstants.TASK_STATUS_IN_PROGRESS);
        }
    }

    public String blockTargetStatus() {
        return ErpPrjConstants.TASK_STATUS_BLOCKED;
    }

    public void assertCanUnblock(String status) {
        if (!ErpPrjConstants.TASK_STATUS_BLOCKED.equals(status)) {
            throw illegal("unblock", status, ErpPrjConstants.TASK_STATUS_BLOCKED);
        }
    }

    public String unblockTargetStatus() {
        return ErpPrjConstants.TASK_STATUS_IN_PROGRESS;
    }

    // ---------- 终态/初始态分类 ----------

    public boolean isTerminal(String status) {
        return ErpPrjConstants.TASK_STATUS_DONE.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 BizModel 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("start", ErpPrjConstants.TASK_STATUS_TODO, ErpPrjConstants.TASK_STATUS_IN_PROGRESS),
                new TransitionDefinition("complete", ErpPrjConstants.TASK_STATUS_IN_PROGRESS, ErpPrjConstants.TASK_STATUS_DONE),
                new TransitionDefinition("block", ErpPrjConstants.TASK_STATUS_IN_PROGRESS, ErpPrjConstants.TASK_STATUS_BLOCKED),
                new TransitionDefinition("unblock", ErpPrjConstants.TASK_STATUS_BLOCKED, ErpPrjConstants.TASK_STATUS_IN_PROGRESS)));
    }

    public List<String> terminalStatuses() {
        return Collections.singletonList(ErpPrjConstants.TASK_STATUS_DONE);
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpPrjConstants.TASK_STATUS_TODO);
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
