package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstMerge;
import app.erp.ast.service.ErpAstConstants;
import app.erp.common.service.AbstractSubmitForApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpAstMerge submitForApproval per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractSubmitForApprovalProcessor to activate the abstract base class; delegates to ErpAstMergeProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpAstMergeSubmitForApprovalProcessor extends AbstractSubmitForApprovalProcessor<ErpAstMerge> {

    @Inject
    ErpAstMergeProcessor processor;

    public ErpAstMergeSubmitForApprovalProcessor() {
        super("ErpAstMerge");
    }

    @Override
    public ErpAstMerge submitForApproval(String id, IServiceContext context) {
        return processor.submitForApproval(id, context);
    }

    @Override
    protected IEntityDao<ErpAstMerge> dao() {
        return daoProvider.daoFor(ErpAstMerge.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getApproveStatus(ErpAstMerge entity) {
        return null;
    }

    @Override
    protected void setApproveStatus(ErpAstMerge entity, String status) {
        // not reached: main method delegates to monolithic Processor
    }

    @Override
    protected boolean isCancelled(ErpAstMerge entity) {
        return false;
    }

    @Override
    protected String unsubmittedStatus() {
        return null;
    }

    @Override
    protected String submittedStatus() {
        return null;
    }

    @Override
    protected String rejectedStatus() {
        return null;
    }
}
