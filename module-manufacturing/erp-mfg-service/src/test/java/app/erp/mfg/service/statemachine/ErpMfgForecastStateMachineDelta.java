package app.erp.mfg.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.mfg.service.ErpMfgConstants;
import io.nop.api.core.exceptions.NopException;

/**
 * Delta 覆盖实证（测试作用域，plan Phase 3）：派生 {@link ErpMfgForecastStateMachine}，收紧 cancel 来源态。
 *
 * <p><strong>基线语义</strong>：{@code assertCanCancel} 允许 {DRAFT, APPROVED}（多来源）。
 * <p><strong>Delta 后语义</strong>：{@code assertCanCancel} 仅允许 {DRAFT}（移除 APPROVED 源——如业务规则要求
 * 已审批预测不可取消，须经反审批回 DRAFT 再取消）。
 *
 * <p>仅覆盖 {@code assertCanCancel} 一个方法；其余动作（approve/isTerminal/transitions）继承基线不变。
 * 用于在真实 IoC 容器下证明 Delta 同名 bean id 覆盖替换基线（契约 §6 业务级 Delta 实证义务）。
 */
public class ErpMfgForecastStateMachineDelta extends ErpMfgForecastStateMachine {

    @Override
    public void assertCanCancel(String status) {
        if (!ErpMfgConstants.FORECAST_STATUS_DRAFT.equals(status)) {
            throw new NopException(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpCommonErrors.ARG_CURRENT_STATUS, status)
                    .param(ErpCommonErrors.ARG_EXPECTED_STATUS, ErpMfgConstants.FORECAST_STATUS_DRAFT)
                    .param(ARG_ACTION, "cancel");
        }
    }
}
