package app.erp.qa.service.processor;

import app.erp.qa.dao.entity.ErpQaRecall;
import app.erp.common.service.AbstractSubmitForApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpQaRecall submitForApproval per-mutation Processor (plan 2026-07-30-2046-1 R5.7, Pattern B)。
 * 自包含编排：requireRecall → validateTransitionForSubmit → validateBusinessRulesForSubmit → doSubmit(SUBMITTED)。
 * 域逻辑经 facade {@link ErpQaRecallProcessor} protected helper（单一真相源）。
 */
public class ErpQaRecallSubmitForApprovalProcessor extends AbstractSubmitForApprovalProcessor<ErpQaRecall> {

    @Inject
    ErpQaRecallProcessor processor;

    public ErpQaRecallSubmitForApprovalProcessor() {
        super("ErpQaRecall");
    }

    @Override
    public ErpQaRecall submitForApproval(String id, IServiceContext context) {
        ErpQaRecall recall = processor.requireRecall(id, context);
        processor.validateTransitionForSubmit(recall, context);
        processor.validateBusinessRulesForSubmit(recall, context);
        processor.doSubmit(recall, context);
        return recall;
    }

    @Override
    protected IEntityDao<ErpQaRecall> dao() {
        return daoProvider.daoFor(ErpQaRecall.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getApproveStatus(ErpQaRecall entity) {
        // not reached: Pattern B custom public override
        return null;
    }

    @Override
    protected void setApproveStatus(ErpQaRecall entity, String status) {
        // not reached: Pattern B custom public override
    }

    @Override
    protected boolean isCancelled(ErpQaRecall entity) {
        // not reached: Pattern B custom public override
        return false;
    }

    @Override
    protected String unsubmittedStatus() {
        // not reached: Pattern B custom public override
        return null;
    }

    @Override
    protected String submittedStatus() {
        // not reached: Pattern B custom public override
        return null;
    }

    @Override
    protected String rejectedStatus() {
        // not reached: Pattern B custom public override
        return null;
    }
}
