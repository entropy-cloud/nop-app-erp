package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalReturn;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.common.service.AbstractWithdrawApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpSalReturn withdrawApproval per-mutation Processor (plan 2026-07-30-1433-2 R5.2)。
 * 原 xbiz withdrawApproval inline-script 提取为抽象骨架 + hook override。
 * NopScriptError → NopException 语义等价：doc-cancelled→ERR_RETURN_ILLEGAL_DOC_STATUS_TRANSITION，
 * invalid-status→ERR_RETURN_ILLEGAL_STATUS_TRANSITION。
 */
public class ErpSalReturnWithdrawApprovalProcessor extends AbstractWithdrawApprovalProcessor<ErpSalReturn> {

    @Inject
    ErpSalReturnProcessor processor;

    @Override
    protected IEntityDao<ErpSalReturn> dao() {
        return daoProvider.daoFor(ErpSalReturn.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpSalErrors.ERR_RETURN_NOT_FOUND)
                .param(ErpSalErrors.ARG_RETURN_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpSalReturn entity, String current, String... expected) {
        return new NopException(ErpSalErrors.ERR_RETURN_ILLEGAL_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_RETURN_CODE, entity.getCode())
                .param(ErpSalErrors.ARG_CURRENT_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected void validateNotCancelled(ErpSalReturn entity, IServiceContext context) {
        processor.validateNotCancelled(entity, context);
    }

    @Override
    protected String getApproveStatus(ErpSalReturn entity) {
        return entity.getApproveStatus();
    }

    @Override
    protected void setApproveStatus(ErpSalReturn entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected boolean isCancelled(ErpSalReturn entity) {
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
