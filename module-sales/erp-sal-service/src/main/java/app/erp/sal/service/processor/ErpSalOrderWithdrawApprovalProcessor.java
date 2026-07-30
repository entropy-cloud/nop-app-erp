package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.common.service.AbstractWithdrawApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpSalOrder withdrawApproval per-mutation Processor (plan 2026-07-30-1433-2 R5.2)。
 * 原 xbiz withdrawApproval 为 inline-script（NopScriptError 守卫 + set UNSUBMITTED），提取为抽象骨架
 * + hook override（validateNotCancelled 委托 facade，validateTransitionForWithdraw 走骨架，doWithdraw 默认设 UNSUBMITTED）。
 * NopScriptError → NopException 语义等价：doc-cancelled→ERR_ORDER_ILLEGAL_DOC_STATUS_TRANSITION，
 * invalid-status→ERR_ORDER_ILLEGAL_STATUS_TRANSITION。
 */
public class ErpSalOrderWithdrawApprovalProcessor extends AbstractWithdrawApprovalProcessor<ErpSalOrder> {

    @Inject
    ErpSalOrderProcessor processor;

    @Override
    protected IEntityDao<ErpSalOrder> dao() {
        return daoProvider.daoFor(ErpSalOrder.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpSalErrors.ERR_ORDER_NOT_FOUND)
                .param(ErpSalErrors.ARG_ORDER_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpSalOrder entity, String current, String... expected) {
        return new NopException(ErpSalErrors.ERR_ORDER_ILLEGAL_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_ORDER_CODE, entity.getCode())
                .param(ErpSalErrors.ARG_CURRENT_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected void validateNotCancelled(ErpSalOrder entity, IServiceContext context) {
        processor.validateNotCancelled(entity, context);
    }

    @Override
    protected String getApproveStatus(ErpSalOrder entity) {
        return entity.getApproveStatus();
    }

    @Override
    protected void setApproveStatus(ErpSalOrder entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected boolean isCancelled(ErpSalOrder entity) {
        return entity.isCancelled();
    }

    @Override
    protected String unsubmittedStatus() {
        return ErpSalConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    @Override
    protected String submittedStatus() {
        return ErpSalConstants.APPROVE_STATUS_SUBMITTED;
    }
}
