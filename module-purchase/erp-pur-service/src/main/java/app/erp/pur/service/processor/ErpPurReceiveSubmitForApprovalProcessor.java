package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.pur.service.statemachine.ErpPurReceiveApprovalStateMachine;
import app.erp.common.service.AbstractSubmitForApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpPurReceive submitForApproval per-mutation Processor (plan 2026-07-25-1057-2；审批轴 Bean 接线 plan 2026-08-13-1950-1 M4.14)。
 *
 * <p>运行 {@link AbstractSubmitForApprovalProcessor} 骨架；固定来源态/目标态判断委托
 * {@link ErpPurReceiveApprovalStateMachine}（approveStatus 审批轴 Bean，契约 §4/§7）；
 * 动态业务守卫/副作用（requireLinesNonEmpty/requireSupplierActive）保留在 {@link ErpPurReceiveProcessor} 经
 * {@link #validateBusinessRules} 钩子执行。
 *
 * <p>非法边映射：Bean 抛 common 层 {@code ERR_ILLEGAL_STATUS_TRANSITION}（含 {@code action=submit}/
 * {@code fromStatus} 元数据）作 cause，{@link #validateTransitionForSubmit} 捕获后映射领域码
 * {@link ErpPurErrors#ERR_ILLEGAL_STATUS_TRANSITION}（+ {@code receiveCode} 实体编号/上下文）。
 */
public class ErpPurReceiveSubmitForApprovalProcessor extends AbstractSubmitForApprovalProcessor<ErpPurReceive> {

    @Inject
    ErpPurReceiveProcessor processor;

    @Inject
    ErpPurReceiveApprovalStateMachine stateMachine;

    public ErpPurReceiveSubmitForApprovalProcessor() {
        super("ErpPurReceive");
    }

    @Override
    protected IEntityDao<ErpPurReceive> dao() {
        return daoProvider.daoFor(ErpPurReceive.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpPurErrors.ERR_RECEIVE_NOT_FOUND)
                .param(ErpPurErrors.ARG_RECEIVE_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpPurReceive entity, String current, String... expected) {
        return new NopException(ErpPurErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_RECEIVE_CODE, entity.getCode())
                .param(ErpPurErrors.ARG_CURRENT_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected void validateNotCancelled(ErpPurReceive entity, IServiceContext context) {
        processor.validateNotCancelled(entity, context);
    }

    @Override
    protected void validateBusinessRules(ErpPurReceive entity, IServiceContext context) {
        processor.requireLinesNonEmpty(entity, context);
        processor.requireSupplierActive(entity, context);
    }

    @Override
    protected void validateTransitionForSubmit(ErpPurReceive entity, IServiceContext context) {
        try {
            stateMachine.assertCanSubmit(getApproveStatus(entity));
        } catch (NopException e) {
            throw illegalStatusException(entity, getApproveStatus(entity),
                    ErpPurConstants.APPROVE_STATUS_UNSUBMITTED + " / " + ErpPurConstants.APPROVE_STATUS_REJECTED);
        }
    }

    @Override
    protected String getApproveStatus(ErpPurReceive entity) {
        String status = entity.getApproveStatus();
        return status == null ? ErpPurConstants.APPROVE_STATUS_UNSUBMITTED : status;
    }

    @Override
    protected void setApproveStatus(ErpPurReceive entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected boolean isCancelled(ErpPurReceive entity) {
        return entity.isCancelled();
    }

    @Override
    protected String unsubmittedStatus() {
        return ErpPurConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    @Override
    protected String submittedStatus() {
        return stateMachine.submitTargetStatus();
    }

    @Override
    protected String rejectedStatus() {
        return ErpPurConstants.APPROVE_STATUS_REJECTED;
    }
}
