package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalReceipt;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.common.service.AbstractSubmitForApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpSalReceipt submitForApproval per-mutation Processor (plan 2026-07-30-1433-2 R5.2).
 * Runs the AbstractSubmitForApprovalProcessor skeleton; delegates domain-specific hooks to ErpSalReceiptProcessor.
 */
public class ErpSalReceiptSubmitForApprovalProcessor extends AbstractSubmitForApprovalProcessor<ErpSalReceipt> {

    @Inject
    ErpSalReceiptProcessor processor;

    public ErpSalReceiptSubmitForApprovalProcessor() {
        super("ErpSalReceipt");
    }

    /**
     * 收款单 submitForApproval 的 wf 启动由 ErpSalReceipt.xbiz submitForApproval source 内联持有
     * （ApprovalFlowHelper.start）。本类 override 跳过抽象骨架的 maybeStartWorkflow，避免与 xbiz 双重启动 wf
     * （plan 2026-07-30-1433-2 R5.2：xbiz 工作流所有权保留至 R5.8 统一配线）。
     */
    @Override
    public ErpSalReceipt submitForApproval(String id, IServiceContext context) {
        ErpSalReceipt entity = requireEntity(id);
        validateNotCancelled(entity, context);
        validateTransitionForSubmit(entity, context);
        validateBusinessRules(entity, context);
        beforeStateChange(entity, context);
        doSubmit(entity, context);
        afterStateChange(entity, context);
        dao().updateEntity(entity);
        return entity;
    }

    @Override
    protected IEntityDao<ErpSalReceipt> dao() {
        return daoProvider.daoFor(ErpSalReceipt.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpSalErrors.ERR_RECEIPT_NOT_FOUND)
                .param(ErpSalErrors.ARG_RECEIPT_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpSalReceipt entity, String current, String... expected) {
        return new NopException(ErpSalErrors.ERR_RECEIPT_ILLEGAL_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_RECEIPT_CODE, entity.getCode())
                .param(ErpSalErrors.ARG_CURRENT_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected void validateNotCancelled(ErpSalReceipt entity, IServiceContext context) {
        processor.validateNotCancelled(entity, context);
    }

    @Override
    protected void validateBusinessRules(ErpSalReceipt entity, IServiceContext context) {
        processor.requireCustomerActive(entity, context);
    }

    @Override
    protected String getApproveStatus(ErpSalReceipt entity) {
        String status = entity.getApproveStatus();
        return status == null ? ErpSalConstants.APPROVE_STATUS_UNSUBMITTED : status;
    }

    @Override
    protected void setApproveStatus(ErpSalReceipt entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected boolean isCancelled(ErpSalReceipt entity) {
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

    @Override
    protected String rejectedStatus() {
        return ErpSalConstants.APPROVE_STATUS_REJECTED;
    }
}
