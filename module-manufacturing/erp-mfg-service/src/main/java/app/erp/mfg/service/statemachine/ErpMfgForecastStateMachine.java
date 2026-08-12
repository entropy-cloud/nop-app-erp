package app.erp.mfg.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.mfg.service.ErpMfgConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 需求预测（{@code ErpMfgForecast}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code status}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/manufacturing/mrp.md} §预测来源、{@code docs/design/manufacturing/state-machine.md} §Forecast。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载四态迁移矩阵
 * （DRAFT/APPROVED/CONSUMED/CANCELLED）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 BizModel（契约 §7）。
 *
 * <p>迁移矩阵（3 条边）：approve(DRAFT→APPROVED)、cancel(DRAFT→CANCELLED)、cancel(APPROVED→CANCELLED，多来源)。
 *
 * <p><strong>CONSUMED 为预留死状态</strong>（owner doc {@code docs/design/manufacturing/mrp.md} Decision A）：
 * 字典保留码值但本期零 writer，Bean 不编码任何涉及 CONSUMED 的边（既非来源亦非目标）、不纳入终态集（不可达）。
 * {@link #assertCanCancel(String)} 经正向 allow-list {DRAFT, APPROVED} 隐式拒绝 CONSUMED（refuse-dead-state）。
 * consume 命名动作归 successor（预测消费回写需求上线时）。
 */
public class ErpMfgForecastStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * approve 守卫：仅 DRAFT 合法。
     *
     * <p>对 APPROVED/CANCELLED/CONSUMED 及其它值报告 common 层非法边（携带 {@code action=approve}/
     * {@code fromStatus}）。接线方 {@code ErpMfgForecastBizModel} 映射为领域码
     * {@code ERR_FORECAST_ILLEGAL_STATUS_TRANSITION}。
     */
    public void assertCanApprove(String status) {
        if (!ErpMfgConstants.FORECAST_STATUS_DRAFT.equals(status)) {
            throw illegal("approve", status, ErpMfgConstants.FORECAST_STATUS_DRAFT);
        }
    }

    public String approveTargetStatus() {
        return ErpMfgConstants.FORECAST_STATUS_APPROVED;
    }

    /**
     * cancel 守卫：正向 allow-list {DRAFT, APPROVED}（多来源）。
     *
     * <p>对 CANCELLED 终态与 CONSUMED 预留死状态均报告 common 层非法边（携带 {@code action=cancel}/
     * {@code fromStatus}）。接线方 {@code ErpMfgForecastBizModel} 映射为领域码
     * {@code ERR_FORECAST_ILLEGAL_STATUS_TRANSITION}（保持 refuse-terminal + refuse-dead-state 语义）。
     */
    public void assertCanCancel(String status) {
        if (!ErpMfgConstants.FORECAST_STATUS_DRAFT.equals(status)
                && !ErpMfgConstants.FORECAST_STATUS_APPROVED.equals(status)) {
            throw illegal("cancel", status,
                    ErpMfgConstants.FORECAST_STATUS_DRAFT + "/" + ErpMfgConstants.FORECAST_STATUS_APPROVED);
        }
    }

    public String cancelTargetStatus() {
        return ErpMfgConstants.FORECAST_STATUS_CANCELLED;
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 终态分类：仅 CANCELLED 为终态。
     *
     * <p>CONSUMED <strong>不</strong>纳入终态集（预留死状态，不可达——owner doc Decision A）。CONSUMED 的
     * cancel 拒绝由 {@link #assertCanCancel(String)} 正向 allow-list 表达，而非依赖终态分类。
     */
    public boolean isTerminal(String status) {
        return ErpMfgConstants.FORECAST_STATUS_CANCELLED.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 BizModel 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("approve",
                        ErpMfgConstants.FORECAST_STATUS_DRAFT, ErpMfgConstants.FORECAST_STATUS_APPROVED),
                new TransitionDefinition("cancel",
                        ErpMfgConstants.FORECAST_STATUS_DRAFT, ErpMfgConstants.FORECAST_STATUS_CANCELLED),
                new TransitionDefinition("cancel",
                        ErpMfgConstants.FORECAST_STATUS_APPROVED, ErpMfgConstants.FORECAST_STATUS_CANCELLED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Collections.singletonList(ErpMfgConstants.FORECAST_STATUS_CANCELLED));
    }

    public List<String> initialStatuses() {
        return Collections.unmodifiableList(Collections.singletonList(ErpMfgConstants.FORECAST_STATUS_DRAFT));
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
