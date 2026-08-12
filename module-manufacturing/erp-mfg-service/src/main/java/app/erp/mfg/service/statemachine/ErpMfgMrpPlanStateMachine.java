package app.erp.mfg.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.mfg.service.ErpMfgConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * MRP 计划（{@code ErpMfgMrpPlan}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code status}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/manufacturing/state-machine.md} §预留死状态指引 §MRP。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载已实现迁移矩阵
 * （3 个状态变更动作，双引擎 writer 形态——formal + simulation 各一条 run/complete 链 + firm 释放副作用）+
 * 终态/初始态分类 + 只读 {@link #transitions()} 元数据。可经 Delta 同名 Bean 覆盖（契约 §6）。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归引擎/释放服务（契约 §7）。
 *
 * <p>迁移矩阵（3 条边）：run(DRAFT→RUNNING)、complete(RUNNING→COMPLETED)、firm(COMPLETED→FIRMED)。
 *
 * <p><strong>CANCELLED 为预留死状态</strong>（owner doc §MRP 已记载 Deferred）：字典保留码值但本期零 writer，
 * Bean 不编码任何涉及 CANCELLED 的边、不纳入终态集（不可达）。cancelPlan 落地归 successor。
 *
 * <p><strong>不编码 COMPLETED→DRAFT revert 边</strong>（M0.2 清单漂移）：清单声明此边但 owner doc §MRP + 实仓均无
 * （{@code MrpEngine.runMrp} 守卫要求 DRAFT，COMPLETED 不可 revert/重跑）。Bean 如实排除此不存在边；
 * revert 落地属业务行为变更，归 successor。
 *
 * <p><strong>run 守卫接受 null</strong>：新建实体未初始化 status（null）视为初始态，允许 run（与
 * {@code MrpEngine.runMrp} 既有守卫「null 或 DRAFT」一致）。{@link #transitions()} 元数据以 DRAFT 为规范初始态。
 */
public class ErpMfgMrpPlanStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（状态变更主路径） ----------

    /**
     * run 守卫：null 或 DRAFT 合法（新建实体未初始化 status 视为初始态）。
     *
     * <p>接线方 {@code MrpEngine.runMrp} / {@code SimulationMrpEngine} 映射为领域码
     * {@code ERR_MRP_INVALID_PLAN_STATUS}（common 码作 cause）。
     */
    public void assertCanRun(String status) {
        if (status != null && !ErpMfgConstants.MRP_STATUS_DRAFT.equals(status)) {
            throw illegal("run", status, "null/" + ErpMfgConstants.MRP_STATUS_DRAFT);
        }
    }

    public String runTargetStatus() {
        return ErpMfgConstants.MRP_STATUS_RUNNING;
    }

    /** complete 守卫：仅 RUNNING 合法。 */
    public void assertCanComplete(String status) {
        if (!ErpMfgConstants.MRP_STATUS_RUNNING.equals(status)) {
            throw illegal("complete", status, ErpMfgConstants.MRP_STATUS_RUNNING);
        }
    }

    public String completeTargetStatus() {
        return ErpMfgConstants.MRP_STATUS_COMPLETED;
    }

    /**
     * firm 守卫：仅 COMPLETED 合法。
     *
     * <p>「全部 line 已释放」动态前置由 {@code MrpReleaseService.advancePlanToFirmedIfComplete} 保留原位判断；
     * Bean 仅承载固定来源态判断。接线方映射为领域码 {@code ERR_MRP_INVALID_PLAN_STATUS}（common 码作 cause）。
     */
    public void assertCanFirm(String status) {
        if (!ErpMfgConstants.MRP_STATUS_COMPLETED.equals(status)) {
            throw illegal("firm", status, ErpMfgConstants.MRP_STATUS_COMPLETED);
        }
    }

    public String firmTargetStatus() {
        return ErpMfgConstants.MRP_STATUS_FIRMED;
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 终态分类：仅 FIRMED 为终态。
     *
     * <p>CANCELLED <strong>不</strong>纳入终态集（预留死状态，不可达——owner doc §MRP Deferred）。
     */
    public boolean isTerminal(String status) {
        return ErpMfgConstants.MRP_STATUS_FIRMED.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非引擎主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("run",
                        ErpMfgConstants.MRP_STATUS_DRAFT, ErpMfgConstants.MRP_STATUS_RUNNING),
                new TransitionDefinition("complete",
                        ErpMfgConstants.MRP_STATUS_RUNNING, ErpMfgConstants.MRP_STATUS_COMPLETED),
                new TransitionDefinition("firm",
                        ErpMfgConstants.MRP_STATUS_COMPLETED, ErpMfgConstants.MRP_STATUS_FIRMED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Collections.singletonList(ErpMfgConstants.MRP_STATUS_FIRMED));
    }

    public List<String> initialStatuses() {
        return Collections.unmodifiableList(Collections.singletonList(ErpMfgConstants.MRP_STATUS_DRAFT));
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
