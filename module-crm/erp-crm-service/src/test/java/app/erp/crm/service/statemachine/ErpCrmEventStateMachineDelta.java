package app.erp.crm.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.crm.service.ErpCrmConstants;
import io.nop.api.core.exceptions.NopException;

/**
 * M2.2 Delta 覆盖实证（测试作用域）：派生 {@link ErpCrmEventStateMachine}，放宽 cancel 来源态。
 *
 * <p><strong>基线语义</strong>：{@code assertCanCancel} 仅允许 {@code PLANNED}（活动未开始前取消）。
 * <p><strong>Delta 后语义</strong>：{@code assertCanCancel} 允许 {@code PLANNED} **+ COMPLETED**
 * （某客户要求已完成的活动可事后作废取消——「soft void after completion」业务规则）。
 *
 * <p>仅覆盖 {@code assertCanCancel} 一个方法；其余动作（complete/isTerminal/transitions）继承基线不变。
 * 用于在真实 IoC 容器下证明 Delta 同名 bean id 覆盖替换基线（契约 §6）。
 */
public class ErpCrmEventStateMachineDelta extends ErpCrmEventStateMachine {

    @Override
    public void assertCanCancel(String status) {
        if (!ErpCrmConstants.EVENT_STATUS_PLANNED.equals(status)
                && !ErpCrmConstants.EVENT_STATUS_COMPLETED.equals(status)) {
            throw new NopException(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpCommonErrors.ARG_CURRENT_STATUS, status)
                    .param(ErpCommonErrors.ARG_EXPECTED_STATUS,
                            ErpCrmConstants.EVENT_STATUS_PLANNED + "/" + ErpCrmConstants.EVENT_STATUS_COMPLETED)
                    .param(ARG_ACTION, "cancel");
        }
    }
}
