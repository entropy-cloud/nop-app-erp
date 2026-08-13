package app.erp.prj.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.prj.service.ErpPrjConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 工时记录（{@code ErpPrjTimesheet}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code status}，字典
 * {@code wf/approve-status}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/projects/state-machine.md} §适用对象四。
 *
 * <p><b>治理裁定（§11.2 M3(iii)→M4 升级）</b>：{@code approve} 触发业财过账（{@code TimesheetPostingDispatcher.tryPost}
 * → PROJECT_COST_COLLECTION 凭证 + {@code posted=true} + 归集），依契约 §11.2 M3(iii) 与路线图 M2-M4 纪律，
 * 本轴升级为 <b>M4 plan-first</b>。过账编排时序/失败回退/红冲闭环按 §11.2 M4 (ii)/(iv)/(v) 原序保留在
 * Processor/`I*Biz` 路径；{@code posted}（boolean）不入轴（契约 §3）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载 4 命名动作迁移矩阵
 * （submit/approve/reject/cancel）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor/BizModel（契约 §7）。
 *
 * <p>迁移矩阵（5 条边）：submit(UNSUBMITTED→SUBMITTED)、approve(SUBMITTED→APPROVED)、
 * reject(SUBMITTED→UNSUBMITTED)、cancel 多源 {SUBMITTED, APPROVED}→UNSUBMITTED = 2 边。
 *
 * <p><b>cancel 撤回语义（intentional legacy behavior，Phase 3 Decision）</b>：{@code wf/approve-status} 字典无
 * CANCELLED 值，工时 {@code cancel} 的目标态为 {@code UNSUBMITTED}（撤回/重置语义，非作废）。
 * {@link #assertCanCancel(String)} 对全部 dict 值放行（基线对所有状态允许 cancel——行为保持：既有
 * {@code ErpPrjTimesheetCancelProcessor} 对状态不抛错，仅 APPROVED+posted 时先红冲过账再置 UNSUBMITTED）；
 * {@code cancel} 从 UNSUBMITTED 调用为目标==来源的 no-op，从死状态 REJECTED 调用为良性放行（REJECTED 无 writer，
 * 见下）。APPROVED 为<b>可逆终态</b>——经 cancel 有出边（对齐采购/资产审批轴先例），不适用「终态无出边」强断言。
 *
 * <p><b>死状态声明</b>：{@code wf/approve-status} 含 REJECTED，但工时 {@code reject} 目标态为 UNSUBMITTED（非
 * REJECTED），无 writer 产生 REJECTED → REJECTED 对工时为 dict 死状态（保留为字典共享语义，不从 ORM 删除）。
 *
 * <p>动态守卫边界（保留 Processor/BizModel）：项目 OPEN 校验、任务允许（TODO/IN_PROGRESS）校验、成本率解析、
 * 预算检查（config-gated WARNING/STRICT）、过账派发/归集/红冲——不属于状态轴判断，本 Bean 不承载。
 */
public class ErpPrjTimesheetStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * submit 守卫：来源态为 {@code UNSUBMITTED}（{@code null} 归一化为 UNSUBMITTED，初始态语义）合法。
     *
     * <p>非法来源态报告 common 层非法边（携带 {@code action=submit}/{@code fromStatus}）。
     * 接线方 {@code ErpPrjTimesheetSubmitProcessor} 映射为领域码 {@code ERR_TIMESHEET_ILLEGAL_STATUS_TRANSITION}
     * （保持既有 expected="DRAFT" 文案）。
     */
    public void assertCanSubmit(String status) {
        String s = normalizeSubmit(status);
        if (!ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED.equals(s)) {
            throw illegal("submit", s, ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED);
        }
    }

    /** approve 守卫：来源态为 {@code SUBMITTED} 合法（{@code null} 非法）。 */
    public void assertCanApprove(String status) {
        if (!ErpPrjConstants.APPROVE_STATUS_SUBMITTED.equals(status)) {
            throw illegal("approve", status, ErpPrjConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    /** reject 守卫：来源态为 {@code SUBMITTED} 合法（{@code null} 非法）。 */
    public void assertCanReject(String status) {
        if (!ErpPrjConstants.APPROVE_STATUS_SUBMITTED.equals(status)) {
            throw illegal("reject", status, ErpPrjConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    /**
     * cancel 守卫：基线对全部 {@code wf/approve-status} 值放行（撤回/重置语义，行为保持）。
     *
     * <p>本方法对任何状态值（含 {@code null}）均不抛——如实反映既有 {@code ErpPrjTimesheetCancelProcessor}
     * 「对状态不抛错、仅 APPROVED+posted 时先红冲过账」的行为。APPROVED 态 cancel 的红冲过账编排由 Processor
     * 保留（契约 §11.2 M4 (ii)/(v)），Bean 不持有副作用。Delta 覆盖可收紧此边（契约 §6）。
     */
    public void assertCanCancel(String status) {
        // 行为保持：基线 cancel 对状态无限制，目标态恒为 UNSUBMITTED。故意不抛。
    }

    // ---------- 动作目标态（供 Processor 写回） ----------

    public String submitTargetStatus() {
        return ErpPrjConstants.APPROVE_STATUS_SUBMITTED;
    }

    public String approveTargetStatus() {
        return ErpPrjConstants.APPROVE_STATUS_APPROVED;
    }

    public String rejectTargetStatus() {
        return ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    /** cancel 目标态恒为 UNSUBMITTED（撤回/重置语义）。 */
    public String cancelTargetStatus() {
        return ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 业务终态判定。工时业务终态为 {@code APPROVED}；其为「可逆终态」——经 cancel 有出边（红冲后置回
     * UNSUBMITTED），故 {@link #transitions()} 中 APPROVED 存在出边，不适用「终态无出边」的强可达性断言。
     */
    public boolean isTerminal(String status) {
        return ErpPrjConstants.APPROVE_STATUS_APPROVED.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("submit", ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED, ErpPrjConstants.APPROVE_STATUS_SUBMITTED),
                new TransitionDefinition("approve", ErpPrjConstants.APPROVE_STATUS_SUBMITTED, ErpPrjConstants.APPROVE_STATUS_APPROVED),
                new TransitionDefinition("reject", ErpPrjConstants.APPROVE_STATUS_SUBMITTED, ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED),
                new TransitionDefinition("cancel", ErpPrjConstants.APPROVE_STATUS_SUBMITTED, ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED),
                new TransitionDefinition("cancel", ErpPrjConstants.APPROVE_STATUS_APPROVED, ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(ErpPrjConstants.APPROVE_STATUS_APPROVED));
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED);
    }

    // ---------- 内部 ----------

    /** submit 的 null 归一化为 UNSUBMITTED（初始态语义：未设置=未提交），与 SubmitProcessor 既有归一一致。 */
    private static String normalizeSubmit(String status) {
        return status == null ? ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED : status;
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
