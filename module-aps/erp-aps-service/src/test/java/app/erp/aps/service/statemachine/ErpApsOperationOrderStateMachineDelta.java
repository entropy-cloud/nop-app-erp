package app.erp.aps.service.statemachine;

import app.erp.aps.service.ErpApsConstants;
import app.erp.common.service.ErpCommonErrors;
import io.nop.api.core.exceptions.NopException;

/**
 * M2.13 Delta 覆盖实证（测试作用域）：派生 {@link ErpApsOperationOrderStateMachine}，收紧 cancel 来源态。
 *
 * <p><strong>基线语义</strong>：{@code assertCanCancel} 允许 {@code DRAFT}（草稿废弃）+ {@code PLANNED}
 * （已排程取消）+ {@code IN_PROGRESS}（异常终止）三类源态。
 * <p><strong>Delta 后语义</strong>：{@code assertCanCancel} 仅允许 {@code DRAFT} + {@code PLANNED}
 * （移除 IN_PROGRESS 异常终止源——某客户要求已开工工序不可取消，须先完工 FINISHED）。
 *
 * <p>仅覆盖 {@code assertCanCancel} 一个方法；其余动作（start/complete/revertToDraft/isTerminal/
 * transitions）继承基线不变。用于在真实 IoC 容器下证明 Delta 同名 bean id 覆盖替换基线（契约 §6）。
 */
public class ErpApsOperationOrderStateMachineDelta extends ErpApsOperationOrderStateMachine {

    @Override
    public void assertCanCancel(String status) {
        if (!ErpApsConstants.OP_STATUS_DRAFT.equals(status)
                && !ErpApsConstants.OP_STATUS_PLANNED.equals(status)) {
            throw new NopException(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpCommonErrors.ARG_CURRENT_STATUS, status)
                    .param(ErpCommonErrors.ARG_EXPECTED_STATUS,
                            ErpApsConstants.OP_STATUS_DRAFT + "/" + ErpApsConstants.OP_STATUS_PLANNED)
                    .param(ARG_ACTION, "cancel");
        }
    }
}
