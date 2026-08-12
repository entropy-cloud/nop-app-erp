package app.erp.drp.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.drp.service.ErpDrpConstants;
import io.nop.api.core.exceptions.NopException;

/**
 * M2.14 Delta 覆盖实证（测试作用域）：派生 {@link ErpDrpPlanStateMachine}，收紧 resetToDraft 来源态。
 *
 * <p><strong>基线语义</strong>：{@code assertCanResetToDraft} 允许 {@code COMPUTED/APPROVED}（多源回退）。
 * <p><strong>Delta 后语义</strong>：{@code assertCanResetToDraft} 仅允许 {@code COMPUTED}（收紧：APPROVED 计划须先经人工评审
 * 转回 COMPUTED 才能再回退 DRAFT，禁止 APPROVED 直回 DRAFT；保护已批准计划的审计留痕）。
 *
 * <p>仅覆盖 {@code assertCanResetToDraft} 一个方法；其余动作（runDrp/approvePlan/advanceToExecuted/isTerminal/transitions）
 * 继承基线不变。用于在真实 IoC 容器下证明 Delta 同名 bean id 覆盖替换基线（契约 §6）——与
 * {@link TestErpDrpPlanStateMachineBaselineIoC} 对照构成可区分的基线/Delta 双加载证据。
 */
public class ErpDrpPlanStateMachineDelta extends ErpDrpPlanStateMachine {

    @Override
    public void assertCanResetToDraft(String status) {
        if (!ErpDrpConstants.DRP_PLAN_STATUS_COMPUTED.equals(status)) {
            throw new NopException(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpCommonErrors.ARG_CURRENT_STATUS, status)
                    .param(ErpCommonErrors.ARG_EXPECTED_STATUS, ErpDrpConstants.DRP_PLAN_STATUS_COMPUTED)
                    .param(ARG_ACTION, "resetToDraft");
        }
    }
}
