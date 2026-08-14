package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.mfg.service.statemachine.ErpMfgWorkOrderApprovalStateMachine;
import app.erp.common.service.AbstractApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * ErpMfgWorkOrder approve per-mutation Processor (plan 2026-07-30-1909-2 R5.5)。
 * Pattern B（custom public override）：1:1 复刻 facade 公共 approve 编排流，经 facade protected helper
 * （requireWorkOrder → validateTransitionForApprove → validateBusinessRulesForApprove → doApprove）
 * 保持单一真相源。doApprove 设 APPROVED + docStatus=NOT_STARTED + approvedBy/approvedAt（跨字段校验
 * 需 docStatus=SUBMITTED）由 facade helper 承载，保真域特有约束。
 */
public class ErpMfgWorkOrderApproveProcessor extends AbstractApproveProcessor<ErpMfgWorkOrder> {

    @Inject
    ErpMfgWorkOrderProcessor processor;

    @Inject
    ErpMfgWorkOrderApprovalStateMachine stateMachine;

    @Override
    public ErpMfgWorkOrder approve(String id, IServiceContext context) {
        ErpMfgWorkOrder wo = processor.requireWorkOrder(id, context);
        processor.validateTransitionForApprove(wo, context);
        processor.validateBusinessRulesForApprove(wo, context);
        processor.doApprove(wo, context);
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
    protected boolean isApproved(ErpMfgWorkOrder entity) {
        return Objects.equals(entity.getApproveStatus(), ErpMfgConstants.APPROVE_STATUS_APPROVED);
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
    protected String approvedStatus() {
        return stateMachine.approveTargetStatus();
    }
}
