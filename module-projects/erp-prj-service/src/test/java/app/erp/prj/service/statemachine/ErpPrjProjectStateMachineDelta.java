package app.erp.prj.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.prj.service.ErpPrjConstants;
import io.nop.api.core.exceptions.NopException;

/**
 * M2.4 Delta 覆盖实证（测试作用域）：派生 {@link ErpPrjProjectStateMachine}，收紧 cancel 来源态。
 *
 * <p><strong>基线语义</strong>：{@code assertCanCancel} 允许 {@code DRAFT/OPEN/ON_HOLD}（非终态均合法）。
 * <p><strong>Delta 后语义</strong>：{@code assertCanCancel} 仅允许 {@code OPEN}（收紧：暂停项目须先恢复才能取消、
 * 草稿项目不可取消，仅活跃项目可取消）。
 *
 * <p>仅覆盖 {@code assertCanCancel} 一个方法；其余动作（start/hold/resume/close/isTerminal/transitions）继承基线不变。
 * 用于在真实 IoC 容器下证明 Delta 同名 bean id 覆盖替换基线（契约 §6）——与
 * {@link TestErpPrjProjectStateMachineBaselineIoC} 对照构成可区分的基线/Delta 双加载证据。
 */
public class ErpPrjProjectStateMachineDelta extends ErpPrjProjectStateMachine {

    @Override
    public void assertCanCancel(String status) {
        if (!ErpPrjConstants.PROJECT_STATUS_OPEN.equals(status)) {
            throw new NopException(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpCommonErrors.ARG_CURRENT_STATUS, status)
                    .param(ErpCommonErrors.ARG_EXPECTED_STATUS, "OPEN")
                    .param(ARG_ACTION, "cancel");
        }
    }
}
