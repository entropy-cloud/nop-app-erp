package app.erp.md.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.md.service.ErpMdConstants;
import io.nop.api.core.exceptions.NopException;

/**
 * M2.1 Delta 覆盖实证（测试作用域）：派生 {@link ErpMdSupplierApprovalStateMachine}，收紧 approve 来源态。
 *
 * <p><strong>基线语义</strong>：{@code assertCanApprove} 允许 {@code APPLIED}（正式准入）+ {@code PROBATION}
 * （试用通过）两类源态。
 * <p><strong>Delta 后语义</strong>：{@code assertCanApprove} 仅允许 {@code APPLIED}（移除 PROBATION 源——
 * 某客户要求试用期供应商不可直接 approve，须先 reinstate/probate 回到正式流转再 approve）。
 *
 * <p>仅覆盖 {@code assertCanApprove} 一个方法；其余动作（apply/probate/suspend/reinstate/reject/isTerminal/
 * transitions）继承基线不变。用于在真实 IoC 容器下证明 Delta 同名 bean id 覆盖替换基线（契约 §6）。
 */
public class ErpMdSupplierApprovalStateMachineDelta extends ErpMdSupplierApprovalStateMachine {

    @Override
    public void assertCanApprove(String status) {
        if (!ErpMdConstants.APPROVAL_STATUS_APPLIED.equals(status)) {
            throw new NopException(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpCommonErrors.ARG_CURRENT_STATUS, status)
                    .param(ErpCommonErrors.ARG_EXPECTED_STATUS, ErpMdConstants.APPROVAL_STATUS_APPLIED)
                    .param(ARG_ACTION, "approve");
        }
    }
}
