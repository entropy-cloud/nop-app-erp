package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurReturn;
import app.erp.pur.service.ErpPurErrors;
import app.erp.pur.service.statemachine.ErpPurReturnDocumentStateMachine;
import app.erp.common.service.AbstractCancelProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpPurReturn cancel per-mutation Processor (plan 2026-07-25-1057-2；
 * StateMachine 接线 plan 2026-08-13-0810-1 M4.19)。
 * Overrides the public cancel method to replicate the facade flow (posting reversal + stock move reversal if approved + doc status),
 * calling facade helper methods for each step. Downstream can override via Delta beans.xml with same bean id.
 *
 * <p>固定来源态/目标态判断委托 {@link ErpPurReturnDocumentStateMachine}（docStatus 业务生命周期轴 Bean，契约 §4/§7）。
 * 非法边映射：Bean 抛 common 层 {@code ERR_ILLEGAL_STATUS_TRANSITION}（含 {@code action=cancel}/
 * {@code fromStatus} 元数据）作 cause，{@link #validateTransitionForCancel} 捕获后映射领域码
 * {@link ErpPurErrors#ERR_RETURN_ILLEGAL_DOC_STATUS_TRANSITION}（{@code returnCode}/{@code currentDocStatus}/
 * {@code expectedDocStatus} 参数对外不变）。
 */
public class ErpPurReturnCancelProcessor extends AbstractCancelProcessor<ErpPurReturn> {

    @Inject
    ErpPurReturnProcessor processor;

    @Inject
    ErpPurReturnDocumentStateMachine stateMachine;

    @Override
    public ErpPurReturn cancel(String id, IServiceContext context) {
        ErpPurReturn returnOrder = requireEntity(id);
        validateTransitionForCancel(returnOrder, context);
        boolean wasApproved = returnOrder.isApproved();
        if (wasApproved) {
            processor.ensureReversed(returnOrder, context);
            returnOrder = dao().getEntityById(id);
        }
        setDocStatus(returnOrder, cancelledDocStatus());
        dao().updateEntity(returnOrder);
        processor.runCommitmentRestoreOnReturnReverseHook(returnOrder, wasApproved, context);
        return returnOrder;
    }

    @Override
    protected IEntityDao<ErpPurReturn> dao() {
        return daoProvider.daoFor(ErpPurReturn.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpPurErrors.ERR_RETURN_NOT_FOUND)
                .param(ErpPurErrors.ARG_RETURN_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpPurReturn entity, String current, String... expected) {
        return new NopException(ErpPurErrors.ERR_RETURN_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_RETURN_CODE, entity.getCode())
                .param(ErpPurErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_DOC_STATUS, String.join(" / ", expected));
    }

    @Override
    protected void validateTransitionForCancel(ErpPurReturn entity, IServiceContext context) {
        try {
            stateMachine.assertCanCancel(entity.getDocStatus());
        } catch (NopException e) {
            throw illegalStatusException(entity, entity.getDocStatus(), "非已作废");
        }
    }

    @Override
    protected String getDocStatus(ErpPurReturn entity) {
        return entity.getDocStatus();
    }

    @Override
    protected void setDocStatus(ErpPurReturn entity, String status) {
        entity.setDocStatus(status);
    }

    @Override
    protected String cancelledDocStatus() {
        return stateMachine.cancelTargetStatus();
    }
}
