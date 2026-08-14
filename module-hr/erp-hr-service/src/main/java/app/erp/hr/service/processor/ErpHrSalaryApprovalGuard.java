package app.erp.hr.service.processor;

import app.erp.common.service.ErpCommonErrors;
import app.erp.hr.dao.entity.ErpHrSalary;
import app.erp.hr.service.ErpHrErrors;
import app.erp.hr.service.statemachine.ErpHrSalaryApprovalStateMachine;
import io.nop.api.core.exceptions.NopException;
import jakarta.inject.Inject;

/**
 * 薪酬审批轴（approveStatus）xbiz 接线守卫（plan 2026-08-14-0456-2 M4.64，Phase 2 Decision (A) 机制替代注记）。
 *
 * <p>委托实体级 {@link ErpHrSalaryApprovalStateMachine}（Bean 矩阵权威，契约 §4/§7）：调用 {@code assertCanXxx}
 * 断言来源态，非法边由 Bean 抛 common 层 {@code ERR_ILLEGAL_STATUS_TRANSITION}，本守卫映射为领域
 * {@link ErpHrErrors#ERR_SALARY_ILLEGAL_STATUS_TRANSITION} + salaryId/currentStatus/expectedStatus
 * （common 码作 cause 保留）。
 *
 * <p><strong>机制替代注记</strong>：计划原案「XScript try/catch common NopException → cause-chain 领域码」在
 * XLang 引擎不可行——{@code TryStatement} 语法节点被 {@code BuildExecutableProcessor} 拒绝
 * （{@code nop.err.xlang.exec.not-supported-node}，XLang 不支持 try/catch）。故将「Bean 抛 common 码 →
 * 领域映射」下沉到本 Java 守卫 Bean（契约 §7 的接线层职责），XScript 仅 inject 本守卫调用
 * {@code assertCanXxx(entity)} + 经 Bean {@code *TargetStatus()} 写回目标态。行为与错误码契约不变。
 *
 * <p>供 {@code ErpHrSalary.xbiz} 五个审批轴动作（submitForApproval/approve/reject/reverseApprove/withdrawApproval）
 * 经 XScript {@code inject('app.erp.hr.service.processor.ErpHrSalaryApprovalGuard')} 调用。
 */
public class ErpHrSalaryApprovalGuard {

    @Inject
    ErpHrSalaryApprovalStateMachine approvalStateMachine;

    public void assertCanSubmit(ErpHrSalary salary) {
        map(() -> approvalStateMachine.assertCanSubmit(salary.getApproveStatus()), salary);
    }

    public void assertCanApprove(ErpHrSalary salary) {
        map(() -> approvalStateMachine.assertCanApprove(salary.getApproveStatus()), salary);
    }

    public void assertCanReject(ErpHrSalary salary) {
        map(() -> approvalStateMachine.assertCanReject(salary.getApproveStatus()), salary);
    }

    public void assertCanReverseApprove(ErpHrSalary salary) {
        map(() -> approvalStateMachine.assertCanReverseApprove(salary.getApproveStatus()), salary);
    }

    public void assertCanWithdrawApproval(ErpHrSalary salary) {
        map(() -> approvalStateMachine.assertCanWithdrawApproval(salary.getApproveStatus()), salary);
    }

    private static void map(Runnable assertAction, ErpHrSalary salary) {
        try {
            assertAction.run();
        } catch (NopException e) {
            throw new NopException(ErpHrErrors.ERR_SALARY_ILLEGAL_STATUS_TRANSITION, e)
                    .param(ErpHrErrors.ARG_SALARY_ID, salary.getId())
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, salary.getApproveStatus())
                    .param(ErpHrErrors.ARG_EXPECTED_STATUS, e.getParam(ErpCommonErrors.ARG_EXPECTED_STATUS));
        }
    }
}
