package app.erp.fin.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 预算方案（{@code ErpFinBudgetScenario}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code docStatus}
 * 业务生命周期轴，字典 {@code erp-fin/budget-status}，6 值：DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED/CLOSED）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/finance/budget.md} §ErpFinBudgetScenario 状态机。
 *
 * <p><b>治理裁定（§11.2 M4 plan-first）</b>：approve 生成 BUDGET 影子凭证（{@code BudgetVoucherGenerator.generate}）、
 * cancel 红冲全部 BUDGET 凭证（{@code BudgetVoucherGenerator.reverse}）属受保护预算过账/红冲行为。依契约 §11.2 M4
 * 硬约束 (i)–(v)：过账时序/编排/失败回退继续由 {@code BudgetVoucherGenerator} + per-mutation Processor 管理（§11.2 M4 (ii)/(v)），
 * Bean 不触碰；跨域副作用保留原路径（§11.2 M4 (iv)）；{@code posted} 不入轴（§11.2 M4 (iii)，本实体无独立 posted 字段）。
 *
 * <p>命名带 {@code Document} 后缀（契约 §1 双轴约定，审批轴 {@link ErpFinBudgetScenarioApprovalStateMachine}
 * 各自独立 Bean）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。
 *
 * <p><b>5 源迁移动作矩阵</b>：
 * <ul>
 *   <li>submit：DRAFT/REJECTED → SUBMITTED（重提据实编码 REJECTED→SUBMITTED，保持 live 行为）</li>
 *   <li>approve：SUBMITTED → APPROVED（生成 BUDGET 影子凭证，编排保留原位）</li>
 *   <li>reject：SUBMITTED → REJECTED</li>
 *   <li>cancel：APPROVED → CANCELLED（红冲 BUDGET 凭证，编排保留原位）</li>
 *   <li>carryForward：APPROVED → CLOSED（源方案结转迁移；<b>运行时绕过 facade validateTransition</b>，
 *       使用自身守卫 + 不同错误码 {@code ERR_BUDGET_SCENARIO_NOT_APPROVED}/{@code ERR_BUDGET_CARRY_FORWARD_RULE_INVALID}。
 *       {@link #assertCanCarryForward(String)} 为矩阵完备性 + 保留 carryForward 错误码而设，运行时不经此路径——
 *       显式 justified runtime-dead）</li>
 * </ul>
 *
 * <p><b>rollForward 非 source 迁移</b>（实仓关键发现）：{@code ErpFinBudgetScenarioRollForwardProcessor} 创建
 * <b>新</b>目标方案 {@code docStatus=DRAFT}，<b>源方案保持 APPROVED 不变</b>——是 spawn-new-entity 操作，
 * 非源 docStatus 转换。因此 <b>不纳入 {@code assertCan*} 矩阵</b>，仅在 {@link #transitions()} 中作
 * metadata-only 记录（{@link TransitionDefinition#isSpawn()} = true）。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 facade（契约 §7）。
 *
 * <p><b>Bean 守卫执行范围</b>：仅 facade {@code validateTransition} 路由的 4 动作（submit/approve/reject/cancel）
 * 经 Bean {@code assertCanXxx} 守卫；carryForward/rollForward <b>绕过 facade validateTransition</b>，Bean 不接管其守卫
 * （carryForward 边仅 {@link #transitions()} metadata），避免改变 carryForward/rollForward 的错误码值（行为保持）。
 */
public class ErpFinBudgetScenarioDocumentStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    private static final String SUBMIT_ALLOWED = ErpFinConstants.BUDGET_STATUS_DRAFT
            + "/" + ErpFinConstants.BUDGET_STATUS_REJECTED;

    // ---------- 显式动作方法（主路径） ----------

    /**
     * submit 目标态守卫：来源态为 {@code DRAFT} 或 {@code REJECTED}（重提）合法。
     *
     * <p>对非法来源态报告 common 层非法边（携带 {@code action=submit}/{@code fromStatus}）；接线方
     * {@code ErpFinBudgetScenarioProcessor.validateTransition} 映射为领域码
     * {@code ERR_BUDGET_SCENARIO_ILLEGAL_TRANSITION}（common 码作 cause 保留）。
     */
    public void assertCanSubmit(String docStatus) {
        if (!ErpFinConstants.BUDGET_STATUS_DRAFT.equals(docStatus)
                && !ErpFinConstants.BUDGET_STATUS_REJECTED.equals(docStatus)) {
            throw illegal("submit", docStatus, SUBMIT_ALLOWED);
        }
    }

    /** approve 目标态守卫：来源态为 {@code SUBMITTED} 合法。 */
    public void assertCanApprove(String docStatus) {
        if (!ErpFinConstants.BUDGET_STATUS_SUBMITTED.equals(docStatus)) {
            throw illegal("approve", docStatus, ErpFinConstants.BUDGET_STATUS_SUBMITTED);
        }
    }

    /** reject 目标态守卫：来源态为 {@code SUBMITTED} 合法。 */
    public void assertCanReject(String docStatus) {
        if (!ErpFinConstants.BUDGET_STATUS_SUBMITTED.equals(docStatus)) {
            throw illegal("reject", docStatus, ErpFinConstants.BUDGET_STATUS_SUBMITTED);
        }
    }

    /** cancel 目标态守卫：来源态为 {@code APPROVED} 合法。 */
    public void assertCanCancel(String docStatus) {
        if (!ErpFinConstants.BUDGET_STATUS_APPROVED.equals(docStatus)) {
            throw illegal("cancel", docStatus, ErpFinConstants.BUDGET_STATUS_APPROVED);
        }
    }

    /**
     * carryForward 目标态守卫：来源态为 {@code APPROVED} 合法。
     *
     * <p><b>runtime-dead justified</b>：carryForward 在运行时绕过 facade {@code validateTransition}，
     * 使用自身守卫 {@code validateCarryForwardPreconditions} + 不同领域错误码
     * （{@code ERR_BUDGET_SCENARIO_NOT_APPROVED}/{@code ERR_BUDGET_CARRY_FORWARD_RULE_INVALID}）。
     * 本方法为<b>矩阵完备性</b>（{@link #transitions()} 含 carryForward APPROVED→CLOSED 边）+ 保留 carryForward
     * 既有错误码而设，运行时不经此路径——若接线此方法会改变 carryForward 错误码值，违反行为保持约束。
     */
    public void assertCanCarryForward(String docStatus) {
        if (!ErpFinConstants.BUDGET_STATUS_APPROVED.equals(docStatus)) {
            throw illegal("carryForward", docStatus, ErpFinConstants.BUDGET_STATUS_APPROVED);
        }
    }

    // ---------- 动作目标态（供 Processor 写回） ----------

    public String submitTargetStatus() {
        return ErpFinConstants.BUDGET_STATUS_SUBMITTED;
    }

    public String approveTargetStatus() {
        return ErpFinConstants.BUDGET_STATUS_APPROVED;
    }

    public String rejectTargetStatus() {
        return ErpFinConstants.BUDGET_STATUS_REJECTED;
    }

    public String cancelTargetStatus() {
        return ErpFinConstants.BUDGET_STATUS_CANCELLED;
    }

    public String carryForwardTargetStatus() {
        return ErpFinConstants.BUDGET_STATUS_CLOSED;
    }

    // ---------- 终态/初始态分类 ----------

    /** 业务终态判定：{@code CANCELLED}（作废）或 {@code CLOSED}（已结转），均无出边。 */
    public boolean isTerminal(String docStatus) {
        return ErpFinConstants.BUDGET_STATUS_CANCELLED.equals(docStatus)
                || ErpFinConstants.BUDGET_STATUS_CLOSED.equals(docStatus);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 facade 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(new ArrayList<>(Arrays.asList(
                new TransitionDefinition("submit", ErpFinConstants.BUDGET_STATUS_DRAFT, ErpFinConstants.BUDGET_STATUS_SUBMITTED, false),
                new TransitionDefinition("submit", ErpFinConstants.BUDGET_STATUS_REJECTED, ErpFinConstants.BUDGET_STATUS_SUBMITTED, false),
                new TransitionDefinition("approve", ErpFinConstants.BUDGET_STATUS_SUBMITTED, ErpFinConstants.BUDGET_STATUS_APPROVED, false),
                new TransitionDefinition("reject", ErpFinConstants.BUDGET_STATUS_SUBMITTED, ErpFinConstants.BUDGET_STATUS_REJECTED, false),
                new TransitionDefinition("cancel", ErpFinConstants.BUDGET_STATUS_APPROVED, ErpFinConstants.BUDGET_STATUS_CANCELLED, false),
                new TransitionDefinition("carryForward", ErpFinConstants.BUDGET_STATUS_APPROVED, ErpFinConstants.BUDGET_STATUS_CLOSED, false),
                // rollForward = spawn-new-entity（源保持 APPROVED 不变），非 source 迁移，metadata-only 标注
                new TransitionDefinition("rollForward", ErpFinConstants.BUDGET_STATUS_APPROVED, ErpFinConstants.BUDGET_STATUS_DRAFT, true)
        )));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpFinConstants.BUDGET_STATUS_CANCELLED,
                ErpFinConstants.BUDGET_STATUS_CLOSED));
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpFinConstants.BUDGET_STATUS_DRAFT);
    }

    // ---------- 内部 ----------

    private static NopException illegal(String action, String currentStatus, String expectedStatus) {
        return new NopException(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                .param(ErpCommonErrors.ARG_CURRENT_STATUS, currentStatus)
                .param(ErpCommonErrors.ARG_EXPECTED_STATUS, expectedStatus)
                .param(ARG_ACTION, action);
    }

    /**
     * 只读迁移定义记录（供 M5.1/M5.2 可达性/完备性分析与文档一致性校验消费）。
     *
     * <p>{@link #isSpawn()} 标注 spawn-new-entity 操作（如 rollForward）：源方案状态不变，创建新实体。
     * spawn 边不参与源迁移矩阵的 assertCan 守卫与终态无出边校验。
     */
    public static final class TransitionDefinition {
        private final String action;
        private final String fromStatus;
        private final String toStatus;
        private final boolean spawn;

        TransitionDefinition(String action, String fromStatus, String toStatus, boolean spawn) {
            this.action = action;
            this.fromStatus = fromStatus;
            this.toStatus = toStatus;
            this.spawn = spawn;
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

        public boolean isSpawn() {
            return spawn;
        }
    }
}
