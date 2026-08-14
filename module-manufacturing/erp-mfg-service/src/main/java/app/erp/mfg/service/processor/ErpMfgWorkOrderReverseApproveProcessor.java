package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.mfg.service.statemachine.ErpMfgWorkOrderApprovalStateMachine;
import app.erp.common.service.AbstractReverseApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * ErpMfgWorkOrder reverseApprove per-mutation Processor (plan 2026-07-30-1909-2 R5.5)。
 * Pattern B（custom public override）：1:1 复刻 facade 公共 reverseApprove 编排流，经 facade protected helper
 * （requireWorkOrder → validateTransitionForReverseApprove → doReverseApprove）保持单一真相源。
 * 域特有保真：doReverseApprove 目标态=REJECTED（非基类 SUBMITTED）+ 清空 approvedBy/approvedAt（对齐 facade，
 * 纠正抽象骨架误设 SUBMITTED）。
 */
public class ErpMfgWorkOrderReverseApproveProcessor extends AbstractReverseApproveProcessor<ErpMfgWorkOrder> {

    @Inject
    ErpMfgWorkOrderProcessor processor;

    @Inject
    ErpMfgWorkOrderApprovalStateMachine stateMachine;

    @Override
    public ErpMfgWorkOrder reverseApprove(String id, IServiceContext context) {
        ErpMfgWorkOrder wo = processor.requireWorkOrder(id, context);
        processor.validateTransitionForReverseApprove(wo, context);
        processor.doReverseApprove(wo, context);
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
    protected String approvedStatus() {
        return stateMachine.approveTargetStatus();
    }

    @Override
    protected String submittedStatus() {
        return stateMachine.submitTargetStatus();
    }
}
