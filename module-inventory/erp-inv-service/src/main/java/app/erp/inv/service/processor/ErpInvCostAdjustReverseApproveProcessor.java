package app.erp.inv.service.processor;

import app.erp.inv.dao.entity.ErpInvCostAdjust;
import app.erp.inv.service.ErpInvConstants;
import app.erp.common.service.AbstractReverseApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpInvCostAdjust reverseApprove per-mutation Processor (plan 2026-07-25-1057-2, R5.6 Pattern B).
 * Self-contained orchestration: require → idempotency → validateTransitionForReverseApprove
 * → posted guard (posted=true rejects with "先冲销再反审") → set REJECTED (not SUBMITTED, not clearing audit fields
 * — preserves facade semantics, deviates from base skeleton) → save.
 * Domain logic via facade protected helpers (single source of truth).
 */
public class ErpInvCostAdjustReverseApproveProcessor extends AbstractReverseApproveProcessor<ErpInvCostAdjust> {

    @Inject
    ErpInvCostAdjustProcessor processor;

    @Override
    public ErpInvCostAdjust reverseApprove(String id, IServiceContext context) {
        ErpInvCostAdjust adjust = processor.requireAdjustment(id, context);
        if (adjust.isRejected()) {
            return adjust;
        }
        processor.validateTransitionForReverseApprove(adjust);
        if (Boolean.TRUE.equals(adjust.getPosted())) {
            throw processor.illegalTransition(adjust, processor.currentApproveStatus(adjust), "未过账（先冲销再反审）");
        }
        adjust.setApproveStatus(ErpInvConstants.APPROVE_STATUS_REJECTED);
        processor.adjustDao().updateEntity(adjust);
        return adjust;
    }

    @Override
    protected IEntityDao<ErpInvCostAdjust> dao() {
        return daoProvider.daoFor(ErpInvCostAdjust.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getApproveStatus(ErpInvCostAdjust entity) {
        return entity.getApproveStatus();
    }

    @Override
    protected void setApproveStatus(ErpInvCostAdjust entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpInvCostAdjust entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpInvCostAdjust entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isRejected(ErpInvCostAdjust entity) {
        return entity.isRejected();
    }

    @Override
    protected String approvedStatus() {
        return ErpInvConstants.APPROVE_STATUS_APPROVED;
    }

    @Override
    protected String submittedStatus() {
        return ErpInvConstants.APPROVE_STATUS_SUBMITTED;
    }
}
