package app.erp.fin.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 预算方案（{@code ErpFinBudgetScenario}）实体级状态机 Bean —— approveStatus 镜像轴（字典 {@code wf/approve-status}，
 * 4 值：UNSUBMITTED/SUBMITTED/APPROVED/REJECTED）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/finance/budget.md} §ErpFinBudgetScenario 审批轴。
 *
 * <p><b>双轴耦合（契约 §3）</b>：与 docStatus 主轴 {@link ErpFinBudgetScenarioDocumentStateMachine} 各自独立 Bean，
 * 不合并笛卡尔积。两轴矩阵同构：approve Processor 同时写 docStatus=APPROVED + approveStatus=APPROVED，
 * submit/reject 同理。facade {@code validateTransition} 守卫<b>只读 docStatus</b>，approveStatus 不参与迁移守卫——
 * 它的值与 docStatus 同步推进。本 Bean 为 approveStatus 轴的矩阵权威载体（命名带 {@code Approval} 后缀，契约 §1）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。
 *
 * <p><b>3 动作矩阵</b>（无 withdraw/reverseApprove——BudgetScenario 审批生命周期不含反审核/撤回）：
 * <ul>
 *   <li>submit：UNSUBMITTED/REJECTED → SUBMITTED（重提镜像 docStatus REJECTED→SUBMITTED）</li>
 *   <li>approve：SUBMITTED → APPROVED</li>
 *   <li>reject：SUBMITTED → REJECTED</li>
 * </ul>
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数。
 */
public class ErpFinBudgetScenarioApprovalStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    private static final String SUBMIT_ALLOWED = ErpFinConstants.APPROVE_STATUS_UNSUBMITTED
            + "/" + ErpFinConstants.APPROVE_STATUS_REJECTED;

    // ---------- 显式动作方法（主路径） ----------

    /** submit 目标态守卫：来源态为 {@code UNSUBMITTED} 或 {@code REJECTED}（重提）合法。 */
    public void assertCanSubmit(String approveStatus) {
        if (!ErpFinConstants.APPROVE_STATUS_UNSUBMITTED.equals(approveStatus)
                && !ErpFinConstants.APPROVE_STATUS_REJECTED.equals(approveStatus)) {
            throw illegal("submit", approveStatus, SUBMIT_ALLOWED);
        }
    }

    /** approve 目标态守卫：来源态为 {@code SUBMITTED} 合法。 */
    public void assertCanApprove(String approveStatus) {
        if (!ErpFinConstants.APPROVE_STATUS_SUBMITTED.equals(approveStatus)) {
            throw illegal("approve", approveStatus, ErpFinConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    /** reject 目标态守卫：来源态为 {@code SUBMITTED} 合法。 */
    public void assertCanReject(String approveStatus) {
        if (!ErpFinConstants.APPROVE_STATUS_SUBMITTED.equals(approveStatus)) {
            throw illegal("reject", approveStatus, ErpFinConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    // ---------- 动作目标态（供 Processor 写回） ----------

    public String submitTargetStatus() {
        return ErpFinConstants.APPROVE_STATUS_SUBMITTED;
    }

    public String approveTargetStatus() {
        return ErpFinConstants.APPROVE_STATUS_APPROVED;
    }

    public String rejectTargetStatus() {
        return ErpFinConstants.APPROVE_STATUS_REJECTED;
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 业务终态判定：{@code APPROVED}（REJECTED 可重提 → SUBMITTED，故非终态；APPROVED 无 withdraw/reverseApprove，为终态）。
     */
    public boolean isTerminal(String approveStatus) {
        return ErpFinConstants.APPROVE_STATUS_APPROVED.equals(approveStatus);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(new ArrayList<>(Arrays.asList(
                new TransitionDefinition("submit", ErpFinConstants.APPROVE_STATUS_UNSUBMITTED, ErpFinConstants.APPROVE_STATUS_SUBMITTED),
                new TransitionDefinition("submit", ErpFinConstants.APPROVE_STATUS_REJECTED, ErpFinConstants.APPROVE_STATUS_SUBMITTED),
                new TransitionDefinition("approve", ErpFinConstants.APPROVE_STATUS_SUBMITTED, ErpFinConstants.APPROVE_STATUS_APPROVED),
                new TransitionDefinition("reject", ErpFinConstants.APPROVE_STATUS_SUBMITTED, ErpFinConstants.APPROVE_STATUS_REJECTED)
        )));
    }

    public List<String> terminalStatuses() {
        return Collections.singletonList(ErpFinConstants.APPROVE_STATUS_APPROVED);
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED);
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
