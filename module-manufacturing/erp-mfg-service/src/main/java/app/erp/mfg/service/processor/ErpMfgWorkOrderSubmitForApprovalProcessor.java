package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.common.service.AbstractSubmitForApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * ErpMfgWorkOrder submitForApproval per-mutation Processor (plan 2026-07-30-1909-2 R5.5)。
 * Pattern B（custom public override）：1:1 复刻 facade 公共 submitForApproval 编排流，经 facade protected
 * helper（requireWorkOrder → validateTransitionForSubmit → validateBusinessRulesForSubmit → doSubmit）
 * 保持单一真相源。doReject/doReverseApprove 偏离驱动 Pattern B（custom override 完全绕过基类模板，
 * 零偏离风险）。下游可通过 Delta beans.xml 同名 bean id 覆盖本类或 facade helper。
 */
public class ErpMfgWorkOrderSubmitForApprovalProcessor extends AbstractSubmitForApprovalProcessor<ErpMfgWorkOrder> {

    @Inject
    ErpMfgWorkOrderProcessor processor;

    public ErpMfgWorkOrderSubmitForApprovalProcessor() {
        super("ErpMfgWorkOrder");
    }

    @Override
    public ErpMfgWorkOrder submitForApproval(String id, IServiceContext context) {
        ErpMfgWorkOrder wo = processor.requireWorkOrder(id, context);
        processor.validateTransitionForSubmit(wo, context);
        processor.validateBusinessRulesForSubmit(wo, context);
        processor.doSubmit(wo, context);
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
        return ErpMfgConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    @Override
    protected String submittedStatus() {
        return ErpMfgConstants.APPROVE_STATUS_SUBMITTED;
    }

    @Override
    protected String rejectedStatus() {
        return ErpMfgConstants.APPROVE_STATUS_REJECTED;
    }
}
