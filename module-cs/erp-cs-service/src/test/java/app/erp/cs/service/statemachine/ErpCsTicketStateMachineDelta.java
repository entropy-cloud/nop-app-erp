package app.erp.cs.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.cs.service.ErpCsConstants;
import io.nop.api.core.exceptions.NopException;

/**
 * M1.2 Delta 覆盖实证（测试作用域）：派生 {@link ErpCsTicketStateMachine}，放开 assign 来源态。
 *
 * <p><strong>基线语义</strong>：{@code assertCanAssign} 仅允许 {@code NEW}。
 * <p><strong>Delta 后语义</strong>：{@code assertCanAssign} 额外允许 {@code RESOLVED}（重新分派已解决工单）。
 *
 * <p>仅覆盖 {@code assertCanAssign} 一个方法；其余动作（start/resolve/close/reopen/cancel/isTerminal/
 * transitions）继承基线不变。用于在真实 IoC 容器下证明 Delta 同名 bean id 覆盖替换基线（契约 §6）。
 */
public class ErpCsTicketStateMachineDelta extends ErpCsTicketStateMachine {

    @Override
    public void assertCanAssign(String status) {
        if (!ErpCsConstants.TICKET_STATUS_NEW.equals(status)
                && !ErpCsConstants.TICKET_STATUS_RESOLVED.equals(status)) {
            throw new NopException(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpCommonErrors.ARG_CURRENT_STATUS, status)
                    .param(ErpCommonErrors.ARG_EXPECTED_STATUS, "NEW/RESOLVED")
                    .param(ARG_ACTION, "assign");
        }
    }
}
