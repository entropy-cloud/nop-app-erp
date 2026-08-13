package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.service.ErpSalErrors;
import app.erp.sal.service.statemachine.ErpSalDeliveryDocumentStateMachine;
import app.erp.common.service.AbstractCancelProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpSalDelivery cancel per-mutation Processor (plan 2026-07-30-1433-2 R5.2, no xbiz source;
 * StateMachine 接线 plan 2026-08-13-0810-2 M4.21)。
 * cancel 在已审核时冲销出库移动单（facade ensureReversed）后 reload setDocStatus(CANCELLED)，需 custom public override
 * （冲销后实体引用变更）。经 BizModel Java 调用，R5.8 重配线前不在 xbiz 委托链（运行时验证移交 R5.8）。
 *
 * <p>固定来源态/目标态判断委托 {@link ErpSalDeliveryDocumentStateMachine}（docStatus 业务生命周期轴 Bean，契约 §4/§7）。
 * 非法边映射：Bean 抛 common 层 {@code ERR_ILLEGAL_STATUS_TRANSITION}（含 {@code action=cancel}/
 * {@code fromStatus} 元数据）作 cause，{@link #validateTransitionForCancel} 捕获后映射领域码
 * {@link ErpSalErrors#ERR_ILLEGAL_DOC_STATUS_TRANSITION}（泛型命名漂移，路线图 Non-Goal 不重命名；
 * {@code deliveryCode}/{@code currentDocStatus}/{@code expectedDocStatus} 参数对外不变）。
 */
public class ErpSalDeliveryCancelProcessor extends AbstractCancelProcessor<ErpSalDelivery> {

    @Inject
    ErpSalDeliveryProcessor processor;

    @Inject
    ErpSalDeliveryDocumentStateMachine stateMachine;

    @Override
    public ErpSalDelivery cancel(String id, IServiceContext context) {
        ErpSalDelivery delivery = requireEntity(id);
        validateTransitionForCancel(delivery, context);
        if (delivery.isApproved()) {
            processor.ensureReversed(delivery, context);
            delivery = dao().getEntityById(id);
        }
        setDocStatus(delivery, cancelledDocStatus());
        dao().updateEntity(delivery);
        return delivery;
    }

    @Override
    protected IEntityDao<ErpSalDelivery> dao() {
        return daoProvider.daoFor(ErpSalDelivery.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpSalErrors.ERR_DELIVERY_NOT_FOUND)
                .param(ErpSalErrors.ARG_DELIVERY_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpSalDelivery entity, String current, String... expected) {
        return new NopException(ErpSalErrors.ERR_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_DELIVERY_CODE, entity.getCode())
                .param(ErpSalErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_DOC_STATUS, String.join(" / ", expected));
    }

    @Override
    protected String getDocStatus(ErpSalDelivery entity) {
        return entity.getDocStatus();
    }

    @Override
    protected void validateTransitionForCancel(ErpSalDelivery entity, IServiceContext context) {
        try {
            stateMachine.assertCanCancel(entity.getDocStatus());
        } catch (NopException e) {
            throw illegalStatusException(entity, entity.getDocStatus(), "非已作废");
        }
    }

    @Override
    protected void setDocStatus(ErpSalDelivery entity, String status) {
        entity.setDocStatus(status);
    }

    @Override
    protected String cancelledDocStatus() {
        return stateMachine.cancelTargetStatus();
    }
}
