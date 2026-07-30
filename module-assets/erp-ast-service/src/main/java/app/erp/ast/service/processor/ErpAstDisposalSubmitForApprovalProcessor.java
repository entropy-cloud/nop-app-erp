package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstDisposal;
import app.erp.ast.service.ErpAstConstants;
import app.erp.common.service.AbstractSubmitForApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpAstDisposal submitForApproval per-mutation Processor (plan 2026-07-25-1057-2, R5.4 Pattern B).
 * Self-contained orchestration: require → validateNotCancelled → validateTransition → validateForApproval → set SUBMITTED → save.
 * wf 启动语义保留在 xbiz inline wrapper（Disposal 是范围内唯一有 wf:wfName 的实体），本 Processor 仅处理状态迁移。
 * Domain logic via facade protected helpers (single source of truth).
 */
public class ErpAstDisposalSubmitForApprovalProcessor extends AbstractSubmitForApprovalProcessor<ErpAstDisposal> {

    @Inject
    ErpAstDisposalProcessor processor;

    public ErpAstDisposalSubmitForApprovalProcessor() {
        super("ErpAstDisposal");
    }

    @Override
    public ErpAstDisposal submitForApproval(String id, IServiceContext context) {
        ErpAstDisposal disposal = processor.requireDisposal(id, context);
        processor.validateNotCancelled(disposal, context);
        processor.validateTransitionForSubmit(disposal, context);
        processor.validateForApproval(disposal, context);
        disposal.setApproveStatus(ErpAstConstants.APPROVE_STATUS_SUBMITTED);
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

    @Override
    protected String getApproveStatus(ErpAstDisposal entity) {
        return entity.getApproveStatus();
    }

    @Override
    protected void setApproveStatus(ErpAstDisposal entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected boolean isCancelled(ErpAstDisposal entity) {
        return entity.isCancelled();
    }

    @Override
    protected String unsubmittedStatus() {
        return ErpAstConstants.APPROVE_STATUS_UNSUBMITTED;
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
