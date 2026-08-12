package app.erp.sal.service.processor;

import app.erp.common.service.ErpCommonErrors;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.service.ErpSalErrors;
import app.erp.sal.service.statemachine.ErpSalOrderDocumentStateMachine;
import app.erp.common.service.AbstractCancelProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpSalOrder cancel per-mutation Processor (plan 2026-07-30-1433-2 R5.2；StateMachine 接线 plan 2026-08-12-0918-2 M2.10)。
 *
 * <p>运行 {@link AbstractCancelProcessor} 骨架；固定来源态/目标态判断委托
 * {@link ErpSalOrderDocumentStateMachine}（docStatus 业务生命周期轴 Bean，契约 §4/§7）；
 * 动态业务守卫/副作用（commitment-release/intercompany-reverse）保留在 {@link ErpSalOrderProcessor} 经
 * {@link #beforeCancel} 钩子执行。
 *
 * <p>非法边映射：Bean 抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（含 {@code action=cancel}/
 * {@code fromStatus} 元数据）作 cause，{@link #validateTransitionForCancel} 捕获后映射领域码
 * {@link ErpSalErrors#ERR_ORDER_ILLEGAL_DOC_STATUS_TRANSITION}（+ {@code orderCode} 实体编号/上下文）。
 */
public class ErpSalOrderCancelProcessor extends AbstractCancelProcessor<ErpSalOrder> {

    @Inject
    ErpSalOrderProcessor processor;

    @Inject
    ErpSalOrderDocumentStateMachine stateMachine;

    @Override
    protected IEntityDao<ErpSalOrder> dao() {
        return daoProvider.daoFor(ErpSalOrder.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpSalErrors.ERR_ORDER_NOT_FOUND)
                .param(ErpSalErrors.ARG_ORDER_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpSalOrder entity, String current, String... expected) {
        return new NopException(ErpSalErrors.ERR_ORDER_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_ORDER_CODE, entity.getCode())
                .param(ErpSalErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_DOC_STATUS, String.join(" / ", expected));
    }

    @Override
    protected void validateTransitionForCancel(ErpSalOrder entity, IServiceContext context) {
        try {
            stateMachine.assertCanCancel(entity.getDocStatus());
        } catch (NopException e) {
            throw illegalStatusException(entity, entity.getDocStatus(), "非已作废");
        }
    }

    @Override
    protected void beforeCancel(ErpSalOrder entity, IServiceContext context) {
        processor.runCommitmentReleaseHook(entity, context);
        processor.runIntercompanyReverseHook(entity, context);
    }

    @Override
    protected String getDocStatus(ErpSalOrder entity) {
        return entity.getDocStatus();
    }

    @Override
    protected void setDocStatus(ErpSalOrder entity, String status) {
        entity.setDocStatus(status);
    }

    @Override
    protected String cancelledDocStatus() {
        return stateMachine.cancelTargetStatus();
    }
}
