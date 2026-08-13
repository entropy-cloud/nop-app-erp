package app.erp.fin.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 会计期间（{@code ErpFinAccountingPeriod}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code status}
 * 生命周期轴，字典 {@code erp-fin/period-status} 5 值：NEVER_OPENED/OPEN/CLOSING/CLOSED/CLOSED_FINAL）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/finance/state-machine.md} §对象二。
 *
 * <p><b>治理裁定（§11.2 M4 plan-first）</b>：期间结账（OPEN→CLOSING→CLOSED）、最终锁定（CLOSED→CLOSED_FINAL）、
 * 反结账（CLOSED_FINAL→OPEN）均属受保护会计行为（反结账触发期末凭证红冲）。依契约 §11.2 M4 硬约束
 * (i)–(v)：结账/反结账的过账时序/编排/失败回退/红冲闭环继续由 {@code ErpFinAccountingPeriodProcessor} facade
 * + per-mutation Processor 编排管理（§11.2 M4 (ii)/(v)），Bean 不触碰；跨域副作用（模块结账子状态
 * ar/ap/inv/gl/asset、{@code reclosePeriodCosts}、{@code reverseCloseVoucher} 红冲）保留原 Processor/{@code I*Biz}
 * 路径（§11.2 M4 (iv)）；{@code posted} 不入轴（§11.2 M4 (iii)，期间轴为 {@code status}，无 posted 字段）；
 * 反结账 kill-switch（{@code erp-fin.reverse-close-approval-required}）作为动态业务守卫保留原位（§11.2 M4 (v)）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。
 *
 * <p><b>4 命名动作（全部单来源态）</b>：
 * <ul>
 *   <li>{@code openPeriod} {@code NEVER_OPENED→OPEN}；</li>
 *   <li>{@code close} {@code OPEN→CLOSING→CLOSED}（两段：结账步骤全部成功后，事务内先 CLOSING 再 CLOSED）；</li>
 *   <li>{@code finalize} {@code CLOSED→CLOSED_FINAL}；</li>
 *   <li>{@code reverseClose} {@code CLOSED_FINAL→OPEN}（终态恢复边，触发期末凭证红冲）。</li>
 * </ul>
 *
 * <p><b>CLOSING 瞬态（事务内，不持久化）</b>：{@code closePeriod} 为 {@code @BizMutation}（事务包裹）——结账步骤
 * （成本核算/折旧/结转损益/模块结账子状态）全部在期间仍 OPEN 时执行，成功后事务内先 {@code setStatus(CLOSING)}
 * 再 {@code setStatus(CLOSED)}；任一步骤失败则整 mutation 回滚，CLOSING 不持久化。故 owner doc §对象二
 * 「CLOSING→OPEN（结账失败）」即此<b>事务回滚语义</b>，非显式 writer——Bean 不为 CLOSING→OPEN 发明独立命名边
 * （close 两段边 OPEN→CLOSING→CLOSED 已覆盖 CLOSING 的角色）。{@link #assertCanClose(String)} 仅守卫动作入口
 * 来源态 OPEN（CLOSING 不可作为动作入口「发起结账」，仅事务内瞬态中间态）。
 *
 * <p><b>终态 CLOSED_FINAL 与恢复边</b>：{@code CLOSED_FINAL} 为业务终态（{@link #isTerminal(String)}=true），
 * 其唯一出边为 {@code reverseClose}（管理员反结账恢复路径，owner doc §对象二 §3/§5）。reverseClose 是显式
 * 恢复边，非前向推进——本 Bean 如实编码该边（{@link #transitions()} 含 CLOSED_FINAL→OPEN）。
 *
 * <p>非法来源态抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor（契约 §7）。
 *
 * <p><b>generateNextYearPeriods 生成路径不接线 Bean</b>（契约 §9.2 选项 c 初始态/生成写入，不调 {@code assertCan*}）：
 * {@code ErpFinAccountingPeriodGenerateNextYearPeriodsProcessor} 直接写 1 月 OPEN / 2-12 月 NEVER_OPENED
 * （新建期间行 seed，非既有期间迁移），Bean 不覆盖此 §9.2 路径。
 */
public class ErpFinAccountingPeriodStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * openPeriod 入口守卫：来源态为 {@code NEVER_OPENED} 合法。
     *
     * <p>对非法来源态报告 common 层非法边（携带 {@code action=openPeriod}/{@code fromStatus}）；接线方
     * {@code ErpFinAccountingPeriodOpenPeriodProcessor} 映射为领域码 {@code ERR_PERIOD_ILLEGAL_TRANSITION}
     * （common 码作 cause 保留）。
     */
    public void assertCanOpenPeriod(String status) {
        if (!ErpFinConstants.PERIOD_STATUS_NEVER_OPENED.equals(status)) {
            throw illegal("openPeriod", status, ErpFinConstants.PERIOD_STATUS_NEVER_OPENED);
        }
    }

    /**
     * closePeriod 入口守卫：来源态为 {@code OPEN} 合法（结账步骤执行期间期间保持 OPEN）。
     *
     * <p>注意：CLOSING 不可作为「发起结账」的动作入口（仅事务内瞬态中间态，见类级 CLOSING 瞬态说明），
     * 故 {@code assertCanClose(CLOSING)} 抛非法。接线方 {@code ErpFinAccountingPeriodClosePeriodProcessor}。
     */
    public void assertCanClose(String status) {
        if (!ErpFinConstants.PERIOD_STATUS_OPEN.equals(status)) {
            throw illegal("close", status, ErpFinConstants.PERIOD_STATUS_OPEN);
        }
    }

    /**
     * finalizePeriod 入口守卫：来源态为 {@code CLOSED} 合法（待复核中间态 → 最终锁定）。
     *
     * <p>接线方 {@code ErpFinAccountingPeriodFinalizePeriodProcessor}。
     */
    public void assertCanFinalize(String status) {
        if (!ErpFinConstants.PERIOD_STATUS_CLOSED.equals(status)) {
            throw illegal("finalize", status, ErpFinConstants.PERIOD_STATUS_CLOSED);
        }
    }

    /**
     * reverseClose 入口守卫：来源态为 {@code CLOSED_FINAL} 合法（终态恢复边，触发期末凭证红冲）。
     *
     * <p>接线方 {@code ErpFinAccountingPeriodReverseCloseProcessor}。注意：反结账 kill-switch
     * （{@code erp-fin.reverse-close-approval-required}）为动态业务守卫，保留在 Processor 原位，Bean 不持有。
     */
    public void assertCanReverseClose(String status) {
        if (!ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL.equals(status)) {
            throw illegal("reverseClose", status, ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL);
        }
    }

    // ---------- 动作目标态（供 Processor 写回） ----------

    /** openPeriod 的目标态（OPEN）。 */
    public String openPeriodTargetStatus() {
        return ErpFinConstants.PERIOD_STATUS_OPEN;
    }

    /**
     * close 的两段瞬态目标态（CLOSING）：结账步骤全部成功后、事务内写入的中间态，
     * 紧接由 {@link #closeTargetStatus()} 写 CLOSED。CLOSING 不持久化（见类级 CLOSING 瞬态说明）。
     */
    public String closeEnteringTargetStatus() {
        return ErpFinConstants.PERIOD_STATUS_CLOSING;
    }

    /** close 的最终目标态（CLOSED）：CLOSING 之后事务内立即写入，落库为结账完成态（待复核）。 */
    public String closeTargetStatus() {
        return ErpFinConstants.PERIOD_STATUS_CLOSED;
    }

    /** finalize 的目标态（CLOSED_FINAL，业务终态）。 */
    public String finalizeTargetStatus() {
        return ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL;
    }

    /** reverseClose 的目标态（OPEN，恢复记账）。 */
    public String reverseCloseTargetStatus() {
        return ErpFinConstants.PERIOD_STATUS_OPEN;
    }

    // ---------- 终态/初始态 + 分类 ----------

    /**
     * 业务终态判定。期间终态为 {@code CLOSED_FINAL}（最终锁定）。{@code CLOSED} 为待复核中间态，<b>非</b>终态。
     */
    public boolean isTerminal(String status) {
        return ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    /**
     * 迁移矩阵只读快照。close 编码为两段（OPEN→CLOSING 事务内进入 + CLOSING→CLOSED 事务内完成），
     * 如实反映 closePeriod 的两段写；CLOSING→OPEN（结账失败）= 事务回滚语义，<b>不</b>编码为命名边。
     */
    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("openPeriod",
                        ErpFinConstants.PERIOD_STATUS_NEVER_OPENED, ErpFinConstants.PERIOD_STATUS_OPEN),
                new TransitionDefinition("close",
                        ErpFinConstants.PERIOD_STATUS_OPEN, ErpFinConstants.PERIOD_STATUS_CLOSING),
                new TransitionDefinition("close",
                        ErpFinConstants.PERIOD_STATUS_CLOSING, ErpFinConstants.PERIOD_STATUS_CLOSED),
                new TransitionDefinition("finalize",
                        ErpFinConstants.PERIOD_STATUS_CLOSED, ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL),
                new TransitionDefinition("reverseClose",
                        ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL, ErpFinConstants.PERIOD_STATUS_OPEN)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL));
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpFinConstants.PERIOD_STATUS_NEVER_OPENED);
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
