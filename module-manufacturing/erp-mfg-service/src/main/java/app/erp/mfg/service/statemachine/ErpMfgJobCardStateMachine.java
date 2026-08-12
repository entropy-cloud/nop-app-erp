package app.erp.mfg.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.mfg.service.ErpMfgConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 作业卡（{@code ErpMfgJobCard}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code status}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/manufacturing/state-machine.md} §适用对象二。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载已实现迁移矩阵
 * （6 个状态变更动作 + recordWork 来源态校验）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor（契约 §7）。
 *
 * <p>迁移矩阵（9 条边）：startJob(OPEN→WORK_IN_PROGRESS)、submitJob(WORK_IN_PROGRESS→SUBMITTED)、
 * submitJob(ON_HOLD→SUBMITTED)、completeJob(SUBMITTED→COMPLETED)、holdJob(WORK_IN_PROGRESS→ON_HOLD)、
 * resumeJob(ON_HOLD→WORK_IN_PROGRESS)、cancelJob(OPEN→CANCELLED)、
 * cancelJob(WORK_IN_PROGRESS→CANCELLED)、cancelJob(ON_HOLD→CANCELLED)。
 *
 * <p><strong>PARTIALLY_TRANSFERRED / MATERIAL_TRANSFERRED 为预留死状态</strong>（owner doc §适用对象二已记载
 * Deferred）：字典保留码值但本期零 writer，Bean 不编码任何涉及两态的边（既非来源亦非目标）、不纳入终态集（不可达）。
 * 转序/工序转移落地归 successor。
 *
 * <p><strong>recordWork 为 validation-only 动作</strong>（不改 status）：仅校验来源 ∈ {WORK_IN_PROGRESS, SUBMITTED}
 * 后记 TimeLog + 累计报工数量 + laborCost 回写。Bean 暴露 {@link #assertCanRecordWork(String)} 作为可测来源态
 * 校验 API，但此动作不计入 {@link #transitions()} 迁移边（无目标态）。
 */
public class ErpMfgJobCardStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（状态变更主路径） ----------

    /**
     * startJob 守卫：仅 OPEN 合法。
     *
     * <p>接线方 {@code ErpMfgJobCardStartJobProcessor} 经 {@code ErpMfgJobCardProcessor.illegalTransition}
     * 映射为领域码 {@code ERR_INVALID_STATUS_TRANSITION}（既有码，保持误命名）。
     */
    public void assertCanStartJob(String status) {
        if (!ErpMfgConstants.JOB_CARD_STATUS_OPEN.equals(status)) {
            throw illegal("startJob", status, ErpMfgConstants.JOB_CARD_STATUS_OPEN);
        }
    }

    public String startJobTargetStatus() {
        return ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS;
    }

    /**
     * submitJob 守卫：正向 allow-list {WORK_IN_PROGRESS, ON_HOLD}（多来源）。
     */
    public void assertCanSubmitJob(String status) {
        if (!ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS.equals(status)
                && !ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD.equals(status)) {
            throw illegal("submitJob", status,
                    ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS + "/" + ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD);
        }
    }

    public String submitJobTargetStatus() {
        return ErpMfgConstants.JOB_CARD_STATUS_SUBMITTED;
    }

    /** completeJob 守卫：仅 SUBMITTED 合法。 */
    public void assertCanCompleteJob(String status) {
        if (!ErpMfgConstants.JOB_CARD_STATUS_SUBMITTED.equals(status)) {
            throw illegal("completeJob", status, ErpMfgConstants.JOB_CARD_STATUS_SUBMITTED);
        }
    }

    public String completeJobTargetStatus() {
        return ErpMfgConstants.JOB_CARD_STATUS_COMPLETED;
    }

    /** holdJob 守卫：仅 WORK_IN_PROGRESS 合法。 */
    public void assertCanHoldJob(String status) {
        if (!ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS.equals(status)) {
            throw illegal("holdJob", status, ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS);
        }
    }

    public String holdJobTargetStatus() {
        return ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD;
    }

    /** resumeJob 守卫：仅 ON_HOLD 合法。 */
    public void assertCanResumeJob(String status) {
        if (!ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD.equals(status)) {
            throw illegal("resumeJob", status, ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD);
        }
    }

    public String resumeJobTargetStatus() {
        return ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS;
    }

    /**
     * cancelJob 守卫：正向 allow-list {OPEN, WORK_IN_PROGRESS, ON_HOLD}（多来源，3 源）。
     */
    public void assertCanCancelJob(String status) {
        if (!ErpMfgConstants.JOB_CARD_STATUS_OPEN.equals(status)
                && !ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS.equals(status)
                && !ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD.equals(status)) {
            throw illegal("cancelJob", status,
                    ErpMfgConstants.JOB_CARD_STATUS_OPEN + "/" + ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS
                            + "/" + ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD);
        }
    }

    public String cancelJobTargetStatus() {
        return ErpMfgConstants.JOB_CARD_STATUS_CANCELLED;
    }

    // ---------- validation-only 动作（不改 status，不计入 transitions） ----------

    /**
     * recordWork 来源态守卫：正向 allow-list {WORK_IN_PROGRESS, SUBMITTED}。
     *
     * <p>recordWork 是命名动作但<strong>不改 status</strong>（仅校验来源合法后记 TimeLog + 累计报工数量 +
     * laborCost 回写 WorkOrder）。故无 {@code recordWorkTargetStatus()} 方法，且不计入 {@link #transitions()}。
     * 集中化来源态校验与「固定迁移逻辑集中」语义一致；目标态省略如实反映无迁移。
     */
    public void assertCanRecordWork(String status) {
        if (!ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS.equals(status)
                && !ErpMfgConstants.JOB_CARD_STATUS_SUBMITTED.equals(status)) {
            throw illegal("recordWork", status,
                    ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS + "/" + ErpMfgConstants.JOB_CARD_STATUS_SUBMITTED);
        }
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 终态分类：COMPLETED 与 CANCELLED 均为终态。
     *
     * <p>PARTIALLY_TRANSFERRED / MATERIAL_TRANSFERRED <strong>不</strong>纳入终态集（预留死状态，不可达）。
     */
    public boolean isTerminal(String status) {
        return ErpMfgConstants.JOB_CARD_STATUS_COMPLETED.equals(status)
                || ErpMfgConstants.JOB_CARD_STATUS_CANCELLED.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("startJob",
                        ErpMfgConstants.JOB_CARD_STATUS_OPEN, ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS),
                new TransitionDefinition("submitJob",
                        ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS, ErpMfgConstants.JOB_CARD_STATUS_SUBMITTED),
                new TransitionDefinition("submitJob",
                        ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD, ErpMfgConstants.JOB_CARD_STATUS_SUBMITTED),
                new TransitionDefinition("completeJob",
                        ErpMfgConstants.JOB_CARD_STATUS_SUBMITTED, ErpMfgConstants.JOB_CARD_STATUS_COMPLETED),
                new TransitionDefinition("holdJob",
                        ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS, ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD),
                new TransitionDefinition("resumeJob",
                        ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD, ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS),
                new TransitionDefinition("cancelJob",
                        ErpMfgConstants.JOB_CARD_STATUS_OPEN, ErpMfgConstants.JOB_CARD_STATUS_CANCELLED),
                new TransitionDefinition("cancelJob",
                        ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS, ErpMfgConstants.JOB_CARD_STATUS_CANCELLED),
                new TransitionDefinition("cancelJob",
                        ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD, ErpMfgConstants.JOB_CARD_STATUS_CANCELLED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpMfgConstants.JOB_CARD_STATUS_COMPLETED,
                ErpMfgConstants.JOB_CARD_STATUS_CANCELLED));
    }

    public List<String> initialStatuses() {
        return Collections.unmodifiableList(Collections.singletonList(ErpMfgConstants.JOB_CARD_STATUS_OPEN));
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
