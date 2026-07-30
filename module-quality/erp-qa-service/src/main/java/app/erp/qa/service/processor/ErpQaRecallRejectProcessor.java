package app.erp.qa.service.processor;

import app.erp.qa.dao.entity.ErpQaRecall;
import app.erp.qa.service.ErpQaConstants;
import app.erp.common.service.AbstractRejectProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpQaRecall reject per-mutation Processor (plan 2026-07-30-2046-1 R5.7, Pattern B)。
 * 自包含编排：requireRecall → validateTransitionForReject → doReject(REJECTED + status=CANCELLED + approvedBy/At)。
 * 域特有保真：facade doReject 设 REJECTED + status=CANCELLED + approvedBy/approvedAt，经 facade protected helper
 * 保留审计字段语义（单一真相源）。
 */
public class ErpQaRecallRejectProcessor extends AbstractRejectProcessor<ErpQaRecall> {

    @Inject
    ErpQaRecallProcessor processor;

    @Override
    public ErpQaRecall reject(String id, IServiceContext context) {
        ErpQaRecall recall = processor.requireRecall(id, context);
        processor.validateTransitionForReject(recall, context);
        processor.doReject(recall, context);
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
    protected void setApprovedBy(ErpQaRecall entity, String userId) {
        // not reached: Pattern B custom public override
    }

    @Override
    protected void setApprovedAt(ErpQaRecall entity, java.sql.Timestamp ts) {
        // not reached: Pattern B custom public override
    }

    @Override
    protected boolean isRejected(ErpQaRecall entity) {
        // not reached: Pattern B custom public override
        return false;
    }

    @Override
    protected boolean isCancelled(ErpQaRecall entity) {
        // not reached: Pattern B custom public override
        return false;
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
