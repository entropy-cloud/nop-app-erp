package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstAssetCapitalization;
import app.erp.ast.service.ErpAstConstants;
import app.erp.common.service.AbstractRejectProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpAstAssetCapitalization reject per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractRejectProcessor to activate the abstract base class; delegates to ErpAstAssetCapitalizationProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpAstAssetCapitalizationRejectProcessor extends AbstractRejectProcessor<ErpAstAssetCapitalization> {

    @Inject
    ErpAstAssetCapitalizationProcessor processor;

    @Override
    public ErpAstAssetCapitalization reject(String id, IServiceContext context) {
        return processor.reject(id, context);
    }

    @Override
    protected IEntityDao<ErpAstAssetCapitalization> dao() {
        return daoProvider.daoFor(ErpAstAssetCapitalization.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getApproveStatus(ErpAstAssetCapitalization entity) {
        return entity.getApproveStatus();
    }

    @Override
    protected void setApproveStatus(ErpAstAssetCapitalization entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpAstAssetCapitalization entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpAstAssetCapitalization entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isRejected(ErpAstAssetCapitalization entity) {
        return entity.isRejected();
    }

    @Override
    protected boolean isCancelled(ErpAstAssetCapitalization entity) {
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
