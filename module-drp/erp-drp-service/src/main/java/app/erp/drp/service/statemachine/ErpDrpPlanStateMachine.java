package app.erp.drp.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.drp.service.ErpDrpConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * DRP 计划头（{@code ErpDrpPlan}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code status}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/drp/state-machine.md} §适用对象一。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载四态迁移矩阵
 * （DRAFT/COMPUTED/APPROVED/EXECUTED）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 BizModel/Processor（契约 §7）。
 *
 * <p>迁移矩阵（5 条边，按 per-(action, fromStatus, toStatus) 三元组）：
 * <ul>
 *   <li>runDrp(DRAFT→COMPUTED)；</li>
 *   <li>approvePlan(COMPUTED→APPROVED)；</li>
 *   <li>resetToDraft 多源 {COMPUTED→DRAFT, APPROVED→DRAFT}（计划头可逆中间态 APPROVED 经 resetToDraft 回退）；</li>
 *   <li>advanceToExecuted(APPROVED→EXECUTED)（经 {@code DrpReleaseService.advancePlanToExecutedIfComplete}
 *       自动推进，无独立命名 mutation，仍编入矩阵以表达该状态边）。</li>
 * </ul>
 *
 * <p><b>终态裁定（D-DRP-1）</b>：{@link #isTerminal(String)} 仅认定 EXECUTED 为终态。
 * APPROVED 虽在 owner doc §1/§3 部分位置被误标为终态，但 §3「APPROVED 可回退 DRAFT」与代码
 * （{@code DrpEngine.resetToDraft} 接受 APPROVED 为合法来源）一致表明 APPROVED 有出边、非终态。
 * 本 Bean 据此如实编码（APPROVED 非终态），owner doc 漂移由 plan Phase 3 Fix。
 *
 * <p>动态守卫边界（保留 Engine/Processor）：净需求公式、仓库补货参数校验、resetToDraft 的 clearSuggestedLines
 * 副作用与 totalReplenishmentQty 清零、advanceToExecuted 的「全部行终态」隐式门控、approvedQty 回填、乐观锁
 * 不属于状态轴判断，本 Bean 不承载。
 */
public class ErpDrpPlanStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    public void assertCanRunDrp(String status) {
        if (!ErpDrpConstants.DRP_PLAN_STATUS_DRAFT.equals(status)) {
            throw illegal("runDrp", status, ErpDrpConstants.DRP_PLAN_STATUS_DRAFT);
        }
    }

    public String runDrpTargetStatus() {
        return ErpDrpConstants.DRP_PLAN_STATUS_COMPUTED;
    }

    public void assertCanApprovePlan(String status) {
        if (!ErpDrpConstants.DRP_PLAN_STATUS_COMPUTED.equals(status)) {
            throw illegal("approvePlan", status, ErpDrpConstants.DRP_PLAN_STATUS_COMPUTED);
        }
    }

    public String approvePlanTargetStatus() {
        return ErpDrpConstants.DRP_PLAN_STATUS_APPROVED;
    }

    /**
     * resetToDraft 守卫：COMPUTED 或 APPROVED 均合法（多源回退，owner doc §3 + 代码 {@code DrpEngine.resetToDraft}）。
     */
    public void assertCanResetToDraft(String status) {
        if (!ErpDrpConstants.DRP_PLAN_STATUS_COMPUTED.equals(status)
                && !ErpDrpConstants.DRP_PLAN_STATUS_APPROVED.equals(status)) {
            throw illegal("resetToDraft", status,
                    ErpDrpConstants.DRP_PLAN_STATUS_COMPUTED + "/" + ErpDrpConstants.DRP_PLAN_STATUS_APPROVED);
        }
    }

    public String resetToDraftTargetStatus() {
        return ErpDrpConstants.DRP_PLAN_STATUS_DRAFT;
    }

    /**
     * advanceToExecuted 守卫：仅 APPROVED 合法（EXECUTED 由 {@code DrpReleaseService.advancePlanToExecutedIfComplete}
     * 在「全部行终态」隐式门控通过后自动推进）。
     */
    public void assertCanAdvanceToExecuted(String status) {
        if (!ErpDrpConstants.DRP_PLAN_STATUS_APPROVED.equals(status)) {
            throw illegal("advanceToExecuted", status, ErpDrpConstants.DRP_PLAN_STATUS_APPROVED);
        }
    }

    public String advanceToExecutedTargetStatus() {
        return ErpDrpConstants.DRP_PLAN_STATUS_EXECUTED;
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 终态分类：仅 EXECUTED 为终态（D-DRP-1：APPROVED 有 resetToDraft 出边，非终态）。
     */
    public boolean isTerminal(String status) {
        return ErpDrpConstants.DRP_PLAN_STATUS_EXECUTED.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("runDrp",
                        ErpDrpConstants.DRP_PLAN_STATUS_DRAFT, ErpDrpConstants.DRP_PLAN_STATUS_COMPUTED),
                new TransitionDefinition("approvePlan",
                        ErpDrpConstants.DRP_PLAN_STATUS_COMPUTED, ErpDrpConstants.DRP_PLAN_STATUS_APPROVED),
                new TransitionDefinition("resetToDraft",
                        ErpDrpConstants.DRP_PLAN_STATUS_COMPUTED, ErpDrpConstants.DRP_PLAN_STATUS_DRAFT),
                new TransitionDefinition("resetToDraft",
                        ErpDrpConstants.DRP_PLAN_STATUS_APPROVED, ErpDrpConstants.DRP_PLAN_STATUS_DRAFT),
                new TransitionDefinition("advanceToExecuted",
                        ErpDrpConstants.DRP_PLAN_STATUS_APPROVED, ErpDrpConstants.DRP_PLAN_STATUS_EXECUTED)));
    }

    public List<String> terminalStatuses() {
        return Collections.singletonList(ErpDrpConstants.DRP_PLAN_STATUS_EXECUTED);
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpDrpConstants.DRP_PLAN_STATUS_DRAFT);
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
