package app.erp.hr.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.hr.service.ErpHrConstants;
import io.nop.api.core.exceptions.NopException;

/**
 * M2.11 Delta 覆盖实证（测试作用域）：派生 {@link ErpHrLeaveRequestStateMachine}，放开 cancel 来源态。
 *
 * <p><strong>基线语义</strong>：{@code assertCanCancel} 仅允许 {@code APPROVED}（单源，已批准休假由 HR/员工取消）。
 * <p><strong>Delta 后语义</strong>：{@code assertCanCancel} 允许 {@code APPROVED} + {@code SUBMITTED}
 * （某客户要求员工可自撤已提交但未审批的休假，对齐 owner doc §2/§6 目标业务行为）。
 *
 * <p>仅覆盖 {@code assertCanCancel} 一个方法；其余动作（submit/approve/reject/isTerminal/transitions）继承基线不变。
 * 用于在真实 IoC 容器下证明 Delta 同名 bean id 覆盖替换基线（契约 §6）。
 */
public class ErpHrLeaveRequestStateMachineDelta extends ErpHrLeaveRequestStateMachine {

    @Override
    public void assertCanCancel(String status) {
        if (!ErpHrConstants.LEAVE_STATUS_APPROVED.equals(status)
                && !ErpHrConstants.LEAVE_STATUS_SUBMITTED.equals(status)) {
            throw new NopException(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpCommonErrors.ARG_CURRENT_STATUS, status)
                    .param(ErpCommonErrors.ARG_EXPECTED_STATUS, "APPROVED/SUBMITTED")
                    .param(ARG_ACTION, "cancel");
        }
    }
}
