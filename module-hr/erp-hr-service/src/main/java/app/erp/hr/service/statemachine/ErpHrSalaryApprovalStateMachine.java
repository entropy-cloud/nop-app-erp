package app.erp.hr.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.hr.service.ErpHrConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 薪酬记录（{@code ErpHrSalary}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code approveStatus} 审批轴）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/human-resource/state-machine.md §适用对象四}（平台标准 {@code wf/approve-status} 四态）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载审批轴 5 动作迁移矩阵
 * （submit/approve/reject/reverseApprove/withdrawApproval）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>命名带 {@code Approval} 后缀（契约 §1 双轴约定，与 {@code ErpHrSalaryPaymentStateMachine} paymentStatus 轴分离）。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归接线方（xbiz XScript / Processor，契约 §7）。
 *
 * <p>迁移矩阵（6 条边，编码<strong>已实现</strong>行为，矩阵裁定依据实仓 {@code ErpHrSalary.xbiz} 守卫 +
 * {@code TestErpHrPayrollEngine.testIllegalTransitionRejects}）：submit({UNSUBMITTED,null,REJECTED}→SUBMITTED)、
 * approve({SUBMITTED}→APPROVED)、reject({SUBMITTED}→REJECTED)、reverseApprove({APPROVED}→SUBMITTED)、
 * withdrawApproval({SUBMITTED}→UNSUBMITTED)。approve/reject 均仅 SUBMITTED 单源——UNSUBMITTED 直接 approve 被平台守卫拒。
 *
 * <p><strong>交叉守卫（一致性，非迁移边）</strong>：{@link #assertCanMarkPaid} 供
 * {@code ErpHrSalaryMarkPaidProcessor} 审批轴前置守卫使用（require APPROVED），markPaid 的迁移边在
 * {@link ErpHrSalaryPaymentStateMachine}。该守卫不在 {@link #transitions()} 元数据中编码。
 */
public class ErpHrSalaryApprovalStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * submit 守卫：来源态为 {@code UNSUBMITTED}/{@code null}/{@code REJECTED} 合法（初始提交或驳回后重新提交）。
     *
     * <p>非法来源态报告 common 层非法边（携带 {@code action=submit}/{@code fromStatus}）。
     * 接线方 {@code ErpHrSalary.xbiz submitForApproval} 映射为领域码 {@code ERR_SALARY_ILLEGAL_STATUS_TRANSITION}。
     */
    public void assertCanSubmit(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpHrConstants.APPROVE_STATUS_UNSUBMITTED.equals(status)
                && !ErpHrConstants.APPROVE_STATUS_REJECTED.equals(status)) {
            throw illegal("submit", status,
                    ErpHrConstants.APPROVE_STATUS_UNSUBMITTED + " / " + ErpHrConstants.APPROVE_STATUS_REJECTED);
        }
    }

    public String submitTargetStatus() {
        return ErpHrConstants.APPROVE_STATUS_SUBMITTED;
    }

    /** approve 守卫：来源态为 {@code SUBMITTED} 合法（UNSUBMITTED 直接 approve 被拒，实仓 {@code ErpHrSalary.xbiz:62}）。 */
    public void assertCanApprove(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpHrConstants.APPROVE_STATUS_SUBMITTED.equals(status)) {
            throw illegal("approve", status, ErpHrConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    public String approveTargetStatus() {
        return ErpHrConstants.APPROVE_STATUS_APPROVED;
    }

    /** reject 守卫：来源态为 {@code SUBMITTED} 合法（与 approve 同源态，实仓 {@code ErpHrSalary.xbiz:87}）。 */
    public void assertCanReject(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpHrConstants.APPROVE_STATUS_SUBMITTED.equals(status)) {
            throw illegal("reject", status, ErpHrConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    public String rejectTargetStatus() {
        return ErpHrConstants.APPROVE_STATUS_REJECTED;
    }

    /** reverseApprove 守卫：来源态为 {@code APPROVED} 合法（实仓 {@code ErpHrSalary.xbiz:113}）。 */
    public void assertCanReverseApprove(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpHrConstants.APPROVE_STATUS_APPROVED.equals(status)) {
            throw illegal("reverseApprove", status, ErpHrConstants.APPROVE_STATUS_APPROVED);
        }
    }

    /** reverseApprove 目标态=SUBMITTED（实仓 {@code ErpHrSalary.xbiz:122}，回退到待审批态重审）。 */
    public String reverseApproveTargetStatus() {
        return ErpHrConstants.APPROVE_STATUS_SUBMITTED;
    }

    /** withdrawApproval 守卫：来源态为 {@code SUBMITTED} 合法（审核人未处理时提交人撤回，实仓 {@code ErpHrSalary.xbiz:138}）。 */
    public void assertCanWithdrawApproval(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpHrConstants.APPROVE_STATUS_SUBMITTED.equals(status)) {
            throw illegal("withdrawApproval", status, ErpHrConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    public String withdrawApprovalTargetStatus() {
        return ErpHrConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    /**
     * markPaid 交叉守卫（一致性，非迁移边）：审批轴须 {@code APPROVED} 才可发放。
     * 供 {@code ErpHrSalaryMarkPaidProcessor} 审批轴前置守卫使用，非法边映射领域码
     * {@code ERR_SALARY_ILLEGAL_STATUS_TRANSITION}。
     */
    public void assertCanMarkPaid(String approveStatus) {
        String status = normalize(approveStatus);
        if (!ErpHrConstants.APPROVE_STATUS_APPROVED.equals(status)) {
            throw illegal("markPaid", status, ErpHrConstants.APPROVE_STATUS_APPROVED);
        }
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 业务终态判定。审批轴业务终态为 {@code APPROVED}（owner doc §适用对象四 §2「审批通过（全部三级均通过，终态）」）；
     * 其为「可逆终态」——经 reverseApprove 有出边，故 {@link #transitions()} 中 APPROVED 存在出边，
     * 不适用「终态无出边」的强可达性断言（见矩阵测试）。
     */
    public boolean isTerminal(String approveStatus) {
        return ErpHrConstants.APPROVE_STATUS_APPROVED.equals(approveStatus);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("submit", ErpHrConstants.APPROVE_STATUS_UNSUBMITTED, ErpHrConstants.APPROVE_STATUS_SUBMITTED),
                new TransitionDefinition("submit", ErpHrConstants.APPROVE_STATUS_REJECTED, ErpHrConstants.APPROVE_STATUS_SUBMITTED),
                new TransitionDefinition("approve", ErpHrConstants.APPROVE_STATUS_SUBMITTED, ErpHrConstants.APPROVE_STATUS_APPROVED),
                new TransitionDefinition("reject", ErpHrConstants.APPROVE_STATUS_SUBMITTED, ErpHrConstants.APPROVE_STATUS_REJECTED),
                new TransitionDefinition("reverseApprove", ErpHrConstants.APPROVE_STATUS_APPROVED, ErpHrConstants.APPROVE_STATUS_SUBMITTED),
                new TransitionDefinition("withdrawApproval", ErpHrConstants.APPROVE_STATUS_SUBMITTED, ErpHrConstants.APPROVE_STATUS_UNSUBMITTED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(ErpHrConstants.APPROVE_STATUS_APPROVED));
    }

    public List<String> initialStatuses() {
        return Collections.unmodifiableList(Arrays.asList(ErpHrConstants.APPROVE_STATUS_UNSUBMITTED));
    }

    // ---------- 内部 ----------

    /** null 归一化为 UNSUBMITTED（初始态语义：未设置=未提交），与各 Processor getApproveStatus 归一一致。 */
    private static String normalize(String approveStatus) {
        return approveStatus == null ? ErpHrConstants.APPROVE_STATUS_UNSUBMITTED : approveStatus;
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
