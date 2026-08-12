package app.erp.b2b.service.statemachine;

import app.erp.b2b.service.ErpB2bConstants;
import app.erp.common.service.ErpCommonErrors;
import io.nop.api.core.exceptions.NopException;

/**
 * M2.16/M2.17 Delta 覆盖实证（测试作用域）：派生 {@link ErpB2bEdiDocStateMachine}，收紧 cancel 来源态。
 *
 * <p><strong>基线语义</strong>：{@code assertCanCancel} 允许 {@code TO_SEND}/{@code SENT}/{@code ERROR}（多来源）。
 * <p><strong>Delta 后语义</strong>：{@code assertCanCancel} 仅允许 {@code TO_SEND}（移除 SENT/ERROR 源，
 * 模拟「仅未发送时可取消」业务规则）。
 *
 * <p>仅覆盖 {@code assertCanCancel} 一个方法；其余动作（markSent/markAcknowledged/markError/retry/archive/
 * isTerminal/transitions）继承基线不变。用于在真实 IoC 容器下证明 Delta 同名 bean id 覆盖替换基线（契约 §6）。
 */
public class ErpB2bEdiDocStateMachineDelta extends ErpB2bEdiDocStateMachine {

    @Override
    public void assertCanCancel(String state) {
        if (!ErpB2bConstants.EDI_DOC_STATE_TO_SEND.equals(state)) {
            throw new NopException(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpCommonErrors.ARG_CURRENT_STATUS, state)
                    .param(ErpCommonErrors.ARG_EXPECTED_STATUS, ErpB2bConstants.EDI_DOC_STATE_TO_SEND)
                    .param(ARG_ACTION, "cancel");
        }
    }
}
