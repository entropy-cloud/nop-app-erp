package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalReturn;
import app.erp.sal.service.ErpSalErrors;
import app.erp.sal.service.statemachine.ErpSalReturnDocumentStateMachine;
import app.erp.common.service.AbstractCancelProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpSalReturn）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * ErpSalReturn cancel per-mutation Processor (plan 2026-07-30-1433-2 R5.2, no xbiz source;
 * StateMachine 接线 plan 2026-08-13-0810-2 M4.27)。
 * cancel 在已审核时冲销反向入库移动 + 过账（facade ensureReversed）后 reload setDocStatus(CANCELLED)，
 * 需 custom public override。经 BizModel Java 调用，R5.8 重配线前不在 xbiz 委托链（运行时验证移交 R5.8）。
 *
 * <p>固定来源态/目标态判断委托 {@link ErpSalReturnDocumentStateMachine}（docStatus 业务生命周期轴 Bean，契约 §4/§7）。
 * 非法边直抛领域码 {@link ErpSalErrors#ERR_RETURN_ILLEGAL_DOC_STATUS_TRANSITION}（plan 2026-09-07-2200-1），
 * {@link #validateTransitionForCancel} 同码补参 {@code returnCode} 实体编号。
 */
public class ErpSalReturnCancelProcessor extends AbstractCancelProcessor<ErpSalReturn> {

    @Inject
    ErpSalReturnProcessor processor;

    @Inject
    ErpSalReturnDocumentStateMachine stateMachine;

    @Override
    public ErpSalReturn cancel(String id, IServiceContext context) {
        ErpSalReturn returnOrder = requireEntity(id);
        validateTransitionForCancel(returnOrder, context);
        if (returnOrder.isApproved()) {
            processor.ensureReversed(returnOrder, context);
            returnOrder = dao().getEntityById(id);
        }
        setDocStatus(returnOrder, cancelledDocStatus());
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
        return new NopException(ErpSalErrors.ERR_RETURN_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_RETURN_CODE, entity.getCode())
                .param(ErpSalErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_DOC_STATUS, String.join(" / ", expected));
    }

    @Override
    protected String getDocStatus(ErpSalReturn entity) {
        return entity.getDocStatus();
    }

    @Override
    protected void validateTransitionForCancel(ErpSalReturn entity, IServiceContext context) {
        try {
            stateMachine.assertCanCancel(entity.getDocStatus());
        } catch (NopException e) {
            throw e.param(ErpSalErrors.ARG_RETURN_CODE, entity.getCode());
        }
    }

    @Override
    protected void setDocStatus(ErpSalReturn entity, String status) {
        entity.setDocStatus(status);
    }

    @Override
    protected String cancelledDocStatus() {
        return stateMachine.cancelTargetStatus();
    }
}
