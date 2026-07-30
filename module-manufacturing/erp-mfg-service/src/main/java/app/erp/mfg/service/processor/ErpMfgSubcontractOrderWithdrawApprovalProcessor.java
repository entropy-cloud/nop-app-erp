package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgSubcontractOrder;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.common.service.AbstractWithdrawApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * ErpMfgSubcontractOrder withdrawApproval per-mutation Processor (plan 2026-07-30-1909-2 R5.5)。
 * Pattern B（custom public override）：原 xbiz withdrawApproval 为 inline-script（NopScriptError 守卫 +
 * set UNSUBMITTED），提取为 custom public override，1:1 复刻 facade 公共 withdrawApproval 编排流，经 facade
 * protected helper（requireOrder → validateTransitionForWithdraw → doWithdrawSubmit）保持单一真相源。
 * NopScriptError → NopException 语义等价：{@code nop.err.wf.approve.invalid-status}（status !== SUBMITTED）
 * → facade illegalTransition 抛 {@link ErpMfgErrors#ERR_SUBCONTRACT_ILLEGAL_STATUS_TRANSITION}
 * （param: subcontractOrderCode/currentStatus/expectedStatus）。
 * Pattern B 额外正当性：custom override 不引入 AbstractWithdrawApprovalProcessor 骨架的 validateNotCancelled
 * （既有 inline-script 仅检查 status !== SUBMITTED），保真既有行为。
 */
public class ErpMfgSubcontractOrderWithdrawApprovalProcessor extends AbstractWithdrawApprovalProcessor<ErpMfgSubcontractOrder> {

    @Inject
    ErpMfgSubcontractOrderProcessor processor;

    @Override
    public ErpMfgSubcontractOrder withdrawApproval(String id, IServiceContext context) {
        ErpMfgSubcontractOrder order = processor.requireOrder(id, context);
        processor.validateTransitionForWithdraw(order, context);
        processor.doWithdrawSubmit(order, context);
        return order;
    }

    @Override
    protected IEntityDao<ErpMfgSubcontractOrder> dao() {
        return daoProvider.daoFor(ErpMfgSubcontractOrder.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpMfgErrors.ERR_SUBCONTRACT_ORDER_NOT_FOUND)
                .param(ErpMfgErrors.ARG_SUBCONTRACT_ORDER_ID, id);
    }

    @Override
    protected String getApproveStatus(ErpMfgSubcontractOrder entity) {
        return entity.getApproveStatus();
    }

    @Override
    protected void setApproveStatus(ErpMfgSubcontractOrder entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected boolean isCancelled(ErpMfgSubcontractOrder entity) {
        return Objects.equals(entity.getDocStatus(), ErpMfgConstants.SUBCONTRACT_STATUS_CANCELLED);
    }

    @Override
    protected String unsubmittedStatus() {
        return ErpMfgConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    @Override
    protected String submittedStatus() {
        return ErpMfgConstants.APPROVE_STATUS_SUBMITTED;
    }
}
