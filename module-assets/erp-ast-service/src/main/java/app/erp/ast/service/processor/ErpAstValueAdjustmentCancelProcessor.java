package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstValueAdjustment;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import app.erp.common.service.AbstractCancelProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpAstValueAdjustment cancel per-mutation Processor (plan 2026-07-25-1057-2, R5.4 Pattern B).
 * Self-contained orchestration: require → validateTransitionForCancel → set CANCELLED → save.
 * Domain logic via facade protected helpers (single source of truth).
 * Dormant until R5.8 rewire（BizModel Java 直调 facade.cancel，不经 xbiz 委托链）。
 */
public class ErpAstValueAdjustmentCancelProcessor extends AbstractCancelProcessor<ErpAstValueAdjustment> {

    @Inject
    ErpAstValueAdjustmentProcessor processor;

    @Override
    public ErpAstValueAdjustment cancel(String id, IServiceContext context) {
        ErpAstValueAdjustment adjustment = processor.requireAdjustment(id, context);
        processor.validateTransitionForCancel(adjustment, context);
        adjustment.setDocStatus(ErpAstConstants.DOC_STATUS_CANCELLED);
        processor.adjustmentDao().updateEntity(adjustment);
        return adjustment;
    }

    @Override
    protected IEntityDao<ErpAstValueAdjustment> dao() {
        return daoProvider.daoFor(ErpAstValueAdjustment.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    /**
     * 骨架守卫裸奔通道修正（plan 2026-09-07-2200-1 form-3）：非法迁移错误由 common 码改为直抛领域码
     * ERR_ADJUSTMENT_ILLEGAL_STATUS_TRANSITION（参数形态与 facade 组装一致）。
     */
    @Override
    protected NopException illegalStatusException(ErpAstValueAdjustment entity, String current, String... expected) {
        return new NopException(ErpAstErrors.ERR_ADJUSTMENT_ILLEGAL_STATUS_TRANSITION)
                .param(ErpAstErrors.ARG_ADJUSTMENT_CODE, entity.getCode())
                .param(ErpAstErrors.ARG_CURRENT_STATUS, current)
                .param(ErpAstErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected String getDocStatus(ErpAstValueAdjustment entity) {
        return entity.getDocStatus();
    }

    @Override
    protected void setDocStatus(ErpAstValueAdjustment entity, String status) {
        entity.setDocStatus(status);
    }

    @Override
    protected String cancelledDocStatus() {
        return ErpAstConstants.DOC_STATUS_CANCELLED;
    }
}
