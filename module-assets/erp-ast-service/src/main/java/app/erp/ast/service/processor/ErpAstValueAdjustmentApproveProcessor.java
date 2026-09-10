package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstValueAdjustment;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import app.erp.common.service.AbstractApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpAstValueAdjustment）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * ErpAstValueAdjustment approve per-mutation Processor (plan 2026-07-25-1057-2, R5.4 Pattern B).
 * Self-contained orchestration: require → idempotency → validateNotCancelled → validateTransition → validateForApproval → executeApprove.
 * Domain logic via facade protected helpers (single source of truth).
 */
public class ErpAstValueAdjustmentApproveProcessor extends AbstractApproveProcessor<ErpAstValueAdjustment> {

    @Inject
    ErpAstValueAdjustmentProcessor processor;

    @Override
    public ErpAstValueAdjustment approve(String id, IServiceContext context) {
        ErpAstValueAdjustment adjustment = processor.requireAdjustment(id, context);
        if (adjustment.isApproved()) {
            return adjustment;
        }
        processor.validateNotCancelled(adjustment, context);
        processor.validateTransitionForApprove(adjustment, context);
        processor.validateForApproval(adjustment, context);
        return processor.executeApprove(id, adjustment, context);
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
    protected String getApproveStatus(ErpAstValueAdjustment entity) {
        return entity.getApproveStatus();
    }

    @Override
    protected void setApproveStatus(ErpAstValueAdjustment entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpAstValueAdjustment entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpAstValueAdjustment entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isApproved(ErpAstValueAdjustment entity) {
        return entity.isApproved();
    }

    @Override
    protected boolean isCancelled(ErpAstValueAdjustment entity) {
        return entity.isCancelled();
    }

    @Override
    protected String submittedStatus() {
        return ErpAstConstants.APPROVE_STATUS_SUBMITTED;
    }

    @Override
    protected String approvedStatus() {
        return ErpAstConstants.APPROVE_STATUS_APPROVED;
    }
}
