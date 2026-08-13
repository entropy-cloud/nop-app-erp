package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalReturn;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.sal.service.statemachine.ErpSalReturnApprovalStateMachine;
import app.erp.common.service.AbstractReverseApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpSalReturn reverseApprove per-mutation Processor (plan 2026-07-30-1433-2 R5.2；审批轴 Bean 接线 plan 2026-08-13-1950-2 M4.28)。
 *
 * <p>运行 {@link AbstractReverseApproveProcessor} 骨架；固定来源态判断委托
 * {@link ErpSalReturnApprovalStateMachine}。reverseApprove 冲销反向入库移动 + 过账（facade ensureReversed）
 * 后 reload 设 REJECTED + 清空审计字段，需 custom public override（冲销后实体引用变更）。
 * reverseApprove 目标态=REJECTED 委托 Bean。
 */
public class ErpSalReturnReverseApproveProcessor extends AbstractReverseApproveProcessor<ErpSalReturn> {

    @Inject
    ErpSalReturnProcessor processor;

    @Inject
    ErpSalReturnApprovalStateMachine stateMachine;

    @Override
    public ErpSalReturn reverseApprove(String id, IServiceContext context) {
        ErpSalReturn returnOrder = requireEntity(id);
        if (isRejected(returnOrder)) {
            return returnOrder;
        }
        validateTransitionForReverseApprove(returnOrder, context);
        processor.ensureReversed(returnOrder, context);
        returnOrder = dao().getEntityById(id);
        setApproveStatus(returnOrder, stateMachine.reverseApproveTargetStatus());
        setApprovedBy(returnOrder, null);
        setApprovedAt(returnOrder, null);
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
    protected void validateTransitionForReverseApprove(ErpSalReturn entity, IServiceContext context) {
        try {
            stateMachine.assertCanReverseApprove(getApproveStatus(entity));
        } catch (NopException e) {
            throw illegalStatusException(entity, getApproveStatus(entity), ErpSalConstants.APPROVE_STATUS_APPROVED);
        }
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
    protected boolean isRejected(ErpSalReturn entity) {
        return entity.isRejected();
    }

    @Override
    protected String approvedStatus() {
        return ErpSalConstants.APPROVE_STATUS_APPROVED;
    }

    @Override
    protected String submittedStatus() {
        return ErpSalConstants.APPROVE_STATUS_SUBMITTED;
    }
}
