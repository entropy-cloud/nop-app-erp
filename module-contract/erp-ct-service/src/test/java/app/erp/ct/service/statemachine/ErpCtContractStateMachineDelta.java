package app.erp.ct.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.ct.service.ErpCtConstants;
import io.nop.api.core.exceptions.NopException;

/**
 * M2.18 Delta 覆盖实证（测试作用域）：派生 {@link ErpCtContractStateMachine}，收紧 terminate 来源态。
 *
 * <p><strong>基线语义</strong>：{@code assertCanTerminate} 允许 {@code ACTIVE}（生效合同提前终止）+ {@code NEGOTIATION}
 * （谈判破裂放弃）两类源态。
 * <p><strong>Delta 后语义</strong>：{@code assertCanTerminate} 仅允许 {@code ACTIVE}（移除 NEGOTIATION 源——
 * 某客户要求谈判中合同不可提前终止，须先 activate 再 terminate）。
 *
 * <p>仅覆盖 {@code assertCanTerminate} 一个方法；其余动作（activate/suspend/resume/expire/amend/isTerminal/
 * transitions）继承基线不变。用于在真实 IoC 容器下证明 Delta 同名 bean id 覆盖替换基线（契约 §6）。
 */
public class ErpCtContractStateMachineDelta extends ErpCtContractStateMachine {

    @Override
    public void assertCanTerminate(String status) {
        if (!ErpCtConstants.CONTRACT_STATUS_ACTIVE.equals(status)) {
            throw new NopException(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpCommonErrors.ARG_CURRENT_STATUS, status)
                    .param(ErpCommonErrors.ARG_EXPECTED_STATUS, ErpCtConstants.CONTRACT_STATUS_ACTIVE)
                    .param(ARG_ACTION, "terminate");
        }
    }
}
