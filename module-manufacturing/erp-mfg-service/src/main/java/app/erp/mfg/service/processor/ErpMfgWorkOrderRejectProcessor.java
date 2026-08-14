package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.mfg.service.statemachine.ErpMfgWorkOrderApprovalStateMachine;
import app.erp.common.service.AbstractRejectProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * ErpMfgWorkOrder reject per-mutation Processor (plan 2026-07-30-1909-2 R5.5)。
 * Pattern B（custom public override）：1:1 复刻 facade 公共 reject 编排流，经 facade protected helper
 * （requireWorkOrder → validateTransitionForReject → doReject）保持单一真相源。
 * 域特有保真：WorkOrder doReject 仅设 REJECTED，不设 approvedBy/approvedAt、不写 docStatus（对齐 facade，
 * 纠正抽象骨架误设审计字段）。
 */
public class ErpMfgWorkOrderRejectProcessor extends AbstractRejectProcessor<ErpMfgWorkOrder> {

    @Inject
    ErpMfgWorkOrderProcessor processor;

    @Inject
    ErpMfgWorkOrderApprovalStateMachine stateMachine;

    @Override
    public ErpMfgWorkOrder reject(String id, IServiceContext context) {
        ErpMfgWorkOrder wo = processor.requireWorkOrder(id, context);
        processor.validateTransitionForReject(wo, context);
        processor.doReject(wo, context);
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
    protected void setApprovedBy(ErpMfgWorkOrder entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpMfgWorkOrder entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isRejected(ErpMfgWorkOrder entity) {
        return Objects.equals(entity.getApproveStatus(), ErpMfgConstants.APPROVE_STATUS_REJECTED);
    }

    @Override
    protected boolean isCancelled(ErpMfgWorkOrder entity) {
        return Objects.equals(entity.getDocStatus(), ErpMfgConstants.WORK_ORDER_STATUS_CANCELLED);
    }

    @Override
    protected String submittedStatus() {
        return stateMachine.submitTargetStatus();
    }

    @Override
    protected String rejectedStatus() {
        return stateMachine.rejectTargetStatus();
    }
}
