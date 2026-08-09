package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalReturn;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.sal.service.entity.ReturnRefundOrchestrator;
import app.erp.common.service.AbstractApproveProcessor;
import app.erp.common.service.SoDGuard;
import io.nop.api.core.exceptions.ErrorCode;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

/**
 * ErpSalReturn approve per-mutation Processor (plan 2026-07-30-1433-2 R5.2).
 * approve 触发反向入库移动 + flush + 过账 + 退款编排（facade doApprove 流程），需 custom public override。
 * 对齐 R5.1 ErpPurReturnApproveProcessor 模式 B。
 */
public class ErpSalReturnApproveProcessor extends AbstractApproveProcessor<ErpSalReturn> {

    @Inject
    ErpSalReturnProcessor processor;

    @Inject
    IOrmTemplate ormTemplate;

    @Inject
    ReturnRefundOrchestrator refundOrchestrator;

    @Override
    public ErpSalReturn approve(String id, IServiceContext context) {
        ErpSalReturn returnOrder = requireEntity(id);
        if (isApproved(returnOrder)) {
            return returnOrder;
        }
        SoDGuard.assertApproverNotCreator(getCreatedBy(returnOrder), currentUserId(), sodErrorCode());
        processor.validateNotCancelled(returnOrder, context);
        validateTransitionForApprove(returnOrder, context);
        processor.validateBusinessRulesForApprove(returnOrder, context);

        processor.triggerIncomingMove(returnOrder, context);
        ormTemplate.flushSession();
        boolean posted = processor.triggerPosting(returnOrder, context);
        refundOrchestrator.orchestrateRefund(returnOrder);

        returnOrder = dao().getEntityById(id);
        setApproveStatus(returnOrder, approvedStatus());
        setApprovedBy(returnOrder, currentUserId());
        setApprovedAt(returnOrder, now());
        processor.applyPosted(returnOrder, posted);
        processor.updateUndeliveredQuantity(returnOrder, context);
        dao().updateEntity(returnOrder);
        return returnOrder;
    }

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
    protected String getApproveStatus(ErpSalReturn entity) {
        String status = entity.getApproveStatus();
        return status == null ? ErpSalConstants.APPROVE_STATUS_UNSUBMITTED : status;
    }

    @Override
    protected void setApproveStatus(ErpSalReturn entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpSalReturn entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpSalReturn entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isApproved(ErpSalReturn entity) {
        return entity.isApproved();
    }

    @Override
    protected boolean isCancelled(ErpSalReturn entity) {
        return entity.isCancelled();
    }

    @Override
    protected String submittedStatus() {
        return ErpSalConstants.APPROVE_STATUS_SUBMITTED;
    }

    @Override
    protected String approvedStatus() {
        return ErpSalConstants.APPROVE_STATUS_APPROVED;
    }

    @Override
    protected ErrorCode sodErrorCode() {
        return ErpSalErrors.ERR_SAL_APPROVER_IS_CREATOR;
    }
}
