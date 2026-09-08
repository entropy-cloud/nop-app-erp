package app.erp.hr.service.processor;

import app.erp.hr.dao.entity.ErpHrSalary;
import app.erp.hr.service.ErpHrErrors;
import app.erp.hr.service.statemachine.ErpHrSalaryApprovalStateMachine;
import io.nop.api.core.exceptions.NopException;
import jakarta.inject.Inject;

/**
 * 薪酬审批轴（approveStatus）xbiz 接线守卫（plan 2026-08-14-0456-2 M4.64）。
 *
 * <p>委托实体级 {@link ErpHrSalaryApprovalStateMachine}（Bean 矩阵权威，契约 §4/§7）：调用 {@code assertCanXxx}
 * 断言来源态；Bean 自 plan 2026-09-07-2200-1 起直抛领域码 {@link ErpHrErrors#ERR_SALARY_ILLEGAL_STATUS_TRANSITION}
 * （action/currentStatus/expectedStatus），本守卫仅做同码补参（G2 ①）——catch 后补 {@code salaryId} 元数据，
 * 码与 cause 不变，无异常重建。
 *
 * <p><strong>机制注记（2026-09-08 更新）</strong>：本 Bean 原为「Bean 抛 common 码 → 守卫转码领域码」转码层
 * （历史根源见 lesson 15/19：2026-08 XScript 无 try/catch 迫使转码下沉 Java）；StateMachine 直抛领域码后转码
 * 职责退役，保留本 Bean 仅因 salaryId 实体元数据增强点在此调用链（删则端到端消息丢 salaryId，违反码值/参数
 * 不变锚）。XScript 经 inject 调用 {@code assertCanXxx(entity)} + 经 Bean {@code *TargetStatus()} 写回目标态。
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
            throw e.param(ErpHrErrors.ARG_SALARY_ID, salary.getId());
        }
    }
}
