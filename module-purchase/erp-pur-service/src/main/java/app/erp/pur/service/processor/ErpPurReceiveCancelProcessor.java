package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.service.ErpPurErrors;
import app.erp.pur.service.statemachine.ErpPurReceiveDocumentStateMachine;
import app.erp.common.service.AbstractCancelProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpPurReceive cancel per-mutation Processor (plan 2026-07-25-1057-2；
 * StateMachine 接线 plan 2026-08-13-0810-1 M4.13)。
 * Overrides the public cancel method to replicate the facade flow (stock move reversal if approved + doc status),
 * calling facade helper methods for each step. Downstream can override via Delta beans.xml with same bean id.
 *
 * <p>固定来源态/目标态判断委托 {@link ErpPurReceiveDocumentStateMachine}（docStatus 业务生命周期轴 Bean，契约 §4/§7）。
 * 非法边映射：Bean 抛 common 层 {@code ERR_ILLEGAL_STATUS_TRANSITION}（含 {@code action=cancel}/
 * {@code fromStatus} 元数据）作 cause，{@link #validateTransitionForCancel} 捕获后映射领域码
 * {@link ErpPurErrors#ERR_ILLEGAL_DOC_STATUS_TRANSITION}（泛型命名漂移，路线图 Non-Goal 不重命名；
 * {@code receiveCode}/{@code currentDocStatus}/{@code expectedDocStatus} 参数对外不变）。
 */
public class ErpPurReceiveCancelProcessor extends AbstractCancelProcessor<ErpPurReceive> {

    @Inject
    ErpPurReceiveProcessor processor;

    @Inject
    ErpPurReceiveDocumentStateMachine stateMachine;

    @Override
    public ErpPurReceive cancel(String id, IServiceContext context) {
        ErpPurReceive receive = requireEntity(id);
        validateTransitionForCancel(receive, context);
        if (receive.isApproved()) {
            processor.ensureReversed(receive, context);
            receive = dao().getEntityById(id);
        }
        setDocStatus(receive, cancelledDocStatus());
        dao().updateEntity(receive);
        return receive;
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
        return new NopException(ErpPurErrors.ERR_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_RECEIVE_CODE, entity.getCode())
                .param(ErpPurErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_DOC_STATUS, String.join(" / ", expected));
    }

    @Override
    protected void validateTransitionForCancel(ErpPurReceive entity, IServiceContext context) {
        try {
            stateMachine.assertCanCancel(entity.getDocStatus());
        } catch (NopException e) {
            throw illegalStatusException(entity, entity.getDocStatus(), "非已作废");
        }
    }

    @Override
    protected String getDocStatus(ErpPurReceive entity) {
        return entity.getDocStatus();
    }

    @Override
    protected void setDocStatus(ErpPurReceive entity, String status) {
        entity.setDocStatus(status);
    }

    @Override
    protected String cancelledDocStatus() {
        return stateMachine.cancelTargetStatus();
    }
}
