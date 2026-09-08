package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstDisposal;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import app.erp.common.service.AbstractRejectProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpAstDisposal reject per-mutation Processor (plan 2026-07-25-1057-2, R5.4 Pattern B).
 * Self-contained orchestration: require → validateNotCancelled → validateTransition → set REJECTED → save.
 * Domain logic via facade protected helpers (single source of truth).
 */
public class ErpAstDisposalRejectProcessor extends AbstractRejectProcessor<ErpAstDisposal> {

    @Inject
    ErpAstDisposalProcessor processor;

    @Override
    public ErpAstDisposal reject(String id, IServiceContext context) {
        ErpAstDisposal disposal = processor.requireDisposal(id, context);
        processor.validateNotCancelled(disposal, context);
        processor.validateTransitionForReject(disposal, context);
        disposal.setApproveStatus(ErpAstConstants.APPROVE_STATUS_REJECTED);
        processor.disposalDao().updateEntity(disposal);
        return disposal;
    }

    @Override
    protected IEntityDao<ErpAstDisposal> dao() {
        return daoProvider.daoFor(ErpAstDisposal.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    /**
     * 骨架守卫裸奔通道修正（plan 2026-09-07-2200-1 form-3）：非法迁移错误由 common 码改为直抛领域码
     * ERR_DISPOSAL_ILLEGAL_STATUS_TRANSITION（参数形态与 facade 组装一致）。
     */
    @Override
    protected NopException illegalStatusException(ErpAstDisposal entity, String current, String... expected) {
        return new NopException(ErpAstErrors.ERR_DISPOSAL_ILLEGAL_STATUS_TRANSITION)
                .param(ErpAstErrors.ARG_DISPOSAL_CODE, entity.getCode())
                .param(ErpAstErrors.ARG_CURRENT_STATUS, current)
                .param(ErpAstErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected String getApproveStatus(ErpAstDisposal entity) {
        return entity.getApproveStatus();
    }

    @Override
    protected void setApproveStatus(ErpAstDisposal entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpAstDisposal entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpAstDisposal entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isRejected(ErpAstDisposal entity) {
        return entity.isRejected();
    }

    @Override
    protected boolean isCancelled(ErpAstDisposal entity) {
        return entity.isCancelled();
    }

    @Override
    protected String submittedStatus() {
        return ErpAstConstants.APPROVE_STATUS_SUBMITTED;
    }

    @Override
    protected String rejectedStatus() {
        return ErpAstConstants.APPROVE_STATUS_REJECTED;
    }
}
