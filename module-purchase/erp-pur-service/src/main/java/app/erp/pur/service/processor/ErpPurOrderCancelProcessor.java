package app.erp.pur.service.processor;

import app.erp.common.service.ErpCommonErrors;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.service.ErpPurErrors;
import app.erp.pur.service.statemachine.ErpPurOrderDocumentStateMachine;
import app.erp.common.service.AbstractCancelProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpPurOrder cancel per-mutation Processor (plan 2026-07-25-1057-2；StateMachine 接线 plan 2026-08-12-0918-1 M2.8)。
 *
 * <p>运行 {@link AbstractCancelProcessor} 骨架；固定来源态/目标态判断委托
 * {@link ErpPurOrderDocumentStateMachine}（docStatus 业务生命周期轴 Bean，契约 §4/§7）；
 * 动态业务守卫/副作用（commitment-release/intercompany-reverse）保留在 {@link ErpPurOrderProcessor} 经
 * {@link #beforeCancel} 钩子执行。
 *
 * <p>非法边映射：Bean 抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（含 {@code action=cancel}/
 * {@code fromStatus} 元数据）作 cause，{@link #validateTransitionForCancel} 捕获后映射领域码
 * {@link ErpPurErrors#ERR_ORDER_ILLEGAL_DOC_STATUS_TRANSITION}（+ {@code orderCode} 实体编号/上下文）。
 */
public class ErpPurOrderCancelProcessor extends AbstractCancelProcessor<ErpPurOrder> {

    @Inject
    ErpPurOrderProcessor processor;

    @Inject
    ErpPurOrderDocumentStateMachine stateMachine;

    @Override
    protected IEntityDao<ErpPurOrder> dao() {
        return daoProvider.daoFor(ErpPurOrder.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpPurErrors.ERR_ORDER_NOT_FOUND)
                .param(ErpPurErrors.ARG_ORDER_CODE, id);
    }

    @Override
    protected NopException illegalStatusException(ErpPurOrder entity, String current, String... expected) {
        return new NopException(ErpPurErrors.ERR_ORDER_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_ORDER_CODE, entity.getCode())
                .param(ErpPurErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_DOC_STATUS, String.join(" / ", expected));
    }

    @Override
    protected void validateTransitionForCancel(ErpPurOrder entity, IServiceContext context) {
        try {
            stateMachine.assertCanCancel(entity.getDocStatus());
        } catch (NopException e) {
            throw illegalStatusException(entity, entity.getDocStatus(), "非已作废");
        }
    }

    @Override
    protected void beforeCancel(ErpPurOrder entity, IServiceContext context) {
        processor.runCommitmentReleaseHook(entity, context);
        processor.runIntercompanyReverseHook(entity, context);
    }

    @Override
    protected String getDocStatus(ErpPurOrder entity) {
        return entity.getDocStatus();
    }

    @Override
    protected void setDocStatus(ErpPurOrder entity, String status) {
        entity.setDocStatus(status);
    }

    @Override
    protected String cancelledDocStatus() {
        return stateMachine.cancelTargetStatus();
    }
}
