package app.erp.ast.service.statemachine;

import app.erp.ast.service.ErpAstConstants;
import app.erp.common.service.ErpCommonErrors;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 折旧计划条目（{@code ErpAstDepreciationSchedule}）实体级状态机 Bean —— 一 Bean 对应一实体一轴
 * （{@code status} 执行状态轴）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/assets/state-machine.md §折旧计划条目状态}。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载 4 动作迁移矩阵
 * （execute/reverse/dispose-cancel/restore）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>迁移矩阵（4 命名动作）：execute(PENDING→EXECUTED)、reverse(EXECUTED→REVERSED)、
 * dispose-cancel(PENDING→CANCELLED，处置/逆资本化联动)、restore(CANCELLED→PENDING，处置 reverseApprove
 * 恢复取消条目)。
 *
 * <p>分类：initial={PENDING}，terminal={EXECUTED（可逆终态——经 reverse 有出边，不适用「终态无出边」
 * 强可达性断言，对齐 Movement APPROVED 先例）, REVERSED}。CANCELLED 为<strong>非终态</strong>——经
 * restore 可恢复回 PENDING（对齐 Movement REJECTED 非终态先例：业务恢复路径）。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus} + {@code action} 补充诊断参数）；调用方 Processor 捕获后 cause-chain 映射为领域码
 * {@code ERR_SCHEDULE_ILLEGAL_STATUS_TRANSITION}（契约 §7）。
 */
public class ErpAstDepreciationScheduleStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * execute 守卫：来源态为 {@code PENDING}/{@code null} 合法（单次/批量折旧计提主路径）。
     */
    public void assertCanExecute(String status) {
        String s = normalize(status);
        if (!ErpAstConstants.SCHEDULE_STATUS_PENDING.equals(s)) {
            throw illegal("execute", s, ErpAstConstants.SCHEDULE_STATUS_PENDING);
        }
    }

    /**
     * reverse 守卫：来源态为 {@code EXECUTED} 合法（红冲纠错主路径）。
     */
    public void assertCanReverse(String status) {
        String s = normalize(status);
        if (!ErpAstConstants.SCHEDULE_STATUS_EXECUTED.equals(s)) {
            throw illegal("reverse", s, ErpAstConstants.SCHEDULE_STATUS_EXECUTED);
        }
    }

    /**
     * dispose-cancel 守卫：来源态为 {@code PENDING} 合法（处置/逆资本化联动取消未计提条目）。
     */
    public void assertCanCancel(String status) {
        String s = normalize(status);
        if (!ErpAstConstants.SCHEDULE_STATUS_PENDING.equals(s)) {
            throw illegal("dispose-cancel", s, ErpAstConstants.SCHEDULE_STATUS_PENDING);
        }
    }

    /**
     * restore 守卫：来源态为 {@code CANCELLED} 合法（处置 reverseApprove 恢复取消条目回 PENDING）。
     */
    public void assertCanRestore(String status) {
        String s = normalize(status);
        if (!ErpAstConstants.SCHEDULE_STATUS_CANCELLED.equals(s)) {
            throw illegal("restore", s, ErpAstConstants.SCHEDULE_STATUS_CANCELLED);
        }
    }

    // ---------- 动作目标态（供 Processor 写回） ----------

    public String executeTargetStatus() {
        return ErpAstConstants.SCHEDULE_STATUS_EXECUTED;
    }

    public String reverseTargetStatus() {
        return ErpAstConstants.SCHEDULE_STATUS_REVERSED;
    }

    public String cancelTargetStatus() {
        return ErpAstConstants.SCHEDULE_STATUS_CANCELLED;
    }

    public String restoreTargetStatus() {
        return ErpAstConstants.SCHEDULE_STATUS_PENDING;
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 业务终态判定。EXECUTED 为「可逆终态」——经 reverse 有出边，不适用「终态无出边」强断言；
     * REVERSED 为红冲后终态（矩阵内无出边）。CANCELLED 非终态（经 restore 可恢复）。
     */
    public boolean isTerminal(String status) {
        return ErpAstConstants.SCHEDULE_STATUS_EXECUTED.equals(status)
                || ErpAstConstants.SCHEDULE_STATUS_REVERSED.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("execute", ErpAstConstants.SCHEDULE_STATUS_PENDING, ErpAstConstants.SCHEDULE_STATUS_EXECUTED),
                new TransitionDefinition("reverse", ErpAstConstants.SCHEDULE_STATUS_EXECUTED, ErpAstConstants.SCHEDULE_STATUS_REVERSED),
                new TransitionDefinition("dispose-cancel", ErpAstConstants.SCHEDULE_STATUS_PENDING, ErpAstConstants.SCHEDULE_STATUS_CANCELLED),
                new TransitionDefinition("restore", ErpAstConstants.SCHEDULE_STATUS_CANCELLED, ErpAstConstants.SCHEDULE_STATUS_PENDING)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpAstConstants.SCHEDULE_STATUS_EXECUTED,
                ErpAstConstants.SCHEDULE_STATUS_REVERSED));
    }

    public List<String> initialStatuses() {
        return Collections.unmodifiableList(Arrays.asList(ErpAstConstants.SCHEDULE_STATUS_PENDING));
    }

    // ---------- 内部 ----------

    /** null 归一化为 PENDING（初始态语义：未设置=待计提），与创建写 PENDING（§9.2 创建种子）一致。 */
    private static String normalize(String status) {
        return status == null ? ErpAstConstants.SCHEDULE_STATUS_PENDING : status;
    }

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
