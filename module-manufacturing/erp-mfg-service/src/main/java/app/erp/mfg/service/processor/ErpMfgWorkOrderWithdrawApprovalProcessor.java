package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.mfg.service.statemachine.ErpMfgWorkOrderApprovalStateMachine;
import app.erp.common.service.AbstractWithdrawApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * ErpMfgWorkOrder withdrawApproval per-mutation Processor (plan 2026-07-30-1909-2 R5.5)。
 * Pattern B（custom public override）：原 xbiz withdrawApproval 为 inline-script（NopScriptError 守卫 +
 * set UNSUBMITTED），提取为 custom public override，1:1 复刻 facade 公共 withdrawApproval 编排流，经 facade
 * protected helper（requireWorkOrder → validateTransitionForWithdraw → doWithdrawSubmit）保持单一真相源。
 * NopScriptError → NopException 语义等价：{@code nop.err.wf.approve.invalid-status}（status !== SUBMITTED）
 * → 守卫直抛 {@link ErpMfgErrors#ERR_INVALID_STATUS_TRANSITION}
 * （param: workOrderCode/currentStatus/expectedStatus）。
 * Pattern B 额外正当性：custom override 不引入 AbstractWithdrawApprovalProcessor 骨架的 validateNotCancelled
 * （既有 inline-script 仅检查 status !== SUBMITTED），保真既有行为。
 */
public class ErpMfgWorkOrderWithdrawApprovalProcessor extends AbstractWithdrawApprovalProcessor<ErpMfgWorkOrder> {

    @Inject
    ErpMfgWorkOrderProcessor processor;

    @Inject
    ErpMfgWorkOrderApprovalStateMachine stateMachine;

    @Override
    public ErpMfgWorkOrder withdrawApproval(String id, IServiceContext context) {
        ErpMfgWorkOrder wo = processor.requireWorkOrder(id, context);
        processor.validateTransitionForWithdraw(wo, context);
        processor.doWithdrawSubmit(wo, context);
        return wo;
    }

    @Override
    protected IEntityDao<ErpMfgWorkOrder> dao() {
        return daoProvider.daoFor(ErpMfgWorkOrder.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpMfgErrors.ERR_WORK_ORDER_NOT_FOUND)
                .param(ErpMfgErrors.ARG_WORK_ORDER_CODE, id);
    }

    /**
     * 骨架守卫裸奔通道修正（plan 2026-09-07-2200-1 form-3，有意契约修正）：非法迁移错误由 common 码
     * 改为直抛领域码 {@link ErpMfgErrors#ERR_INVALID_STATUS_TRANSITION}
     * （参数形态与 facade 同码补参后的端到端消息一致）。
     */
    @Override
    protected NopException illegalStatusException(ErpMfgWorkOrder entity, String current, String... expected) {
        return new NopException(ErpMfgErrors.ERR_INVALID_STATUS_TRANSITION)
                .param(ErpMfgErrors.ARG_WORK_ORDER_CODE, entity.getCode())
                .param(ErpMfgErrors.ARG_CURRENT_STATUS, current)
                .param(ErpMfgErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected String getApproveStatus(ErpMfgWorkOrder entity) {
        return entity.getApproveStatus();
    }

    @Override
    protected void setApproveStatus(ErpMfgWorkOrder entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected boolean isCancelled(ErpMfgWorkOrder entity) {
        return Objects.equals(entity.getDocStatus(), ErpMfgConstants.WORK_ORDER_STATUS_CANCELLED);
    }

    @Override
    protected String unsubmittedStatus() {
        return stateMachine.withdrawTargetStatus();
    }

    @Override
    protected String submittedStatus() {
        return stateMachine.submitTargetStatus();
    }
}
