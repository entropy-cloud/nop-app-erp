package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstMerge;
import app.erp.ast.service.ErpAstConstants;
import app.erp.common.service.AbstractRejectProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpAstMerge reject per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractRejectProcessor to activate the abstract base class; delegates to ErpAstMergeProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpAstMergeRejectProcessor extends AbstractRejectProcessor<ErpAstMerge> {

    @Inject
    ErpAstMergeProcessor processor;

    @Override
    public ErpAstMerge reject(String id, IServiceContext context) {
        return processor.reject(id, context);
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
    protected void setApprovedBy(ErpAstMerge entity, String userId) {
        // not reached: main method delegates to monolithic Processor
    }

    @Override
    protected void setApprovedAt(ErpAstMerge entity, java.sql.Timestamp ts) {
        // not reached: main method delegates to monolithic Processor
    }

    @Override
    protected boolean isRejected(ErpAstMerge entity) {
        return false;
    }

    @Override
    protected boolean isCancelled(ErpAstMerge entity) {
        return false;
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
