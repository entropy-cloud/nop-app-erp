package app.erp.inv.service.processor;

import app.erp.inv.dao.entity.ErpInvCostAdjust;
import app.erp.inv.dao.entity.ErpInvCostAdjustLine;
import app.erp.inv.dao.entity.ErpInvLandedCost;
import app.erp.common.service.AbstractReverseApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.Collections;
import java.util.List;

/**
 * ErpInvLandedCost reverseApprove per-mutation Processor (plan 2026-07-25-1057-2, R5.6 Pattern B).
 * Self-contained orchestration: require → validateCanReverse → findCostAdjustForLandedCost
 * → loadAdjustLines → doReverseApprove (accounting protection area: GL voucher reversal + cost layer reversal
 * + posted=false + postedAt=now + approveStatus=REJECTED + docStatus=CANCELLED + sync CostAdjust entity) → reload.
 * Domain logic via facade protected helpers (single source of truth — accounting rules not copied).
 * Dormant until R5.8 rewire（BizModel Java 直调 facade.reverseApprove，不经 xbiz 委托链）。
 * Long signature boundary: base class public method takes String id, facade takes Long id,
 * conversion at call boundary via Long.valueOf(id).
 */
public class ErpInvLandedCostReverseApproveProcessor extends AbstractReverseApproveProcessor<ErpInvLandedCost> {

    @Inject
    ErpInvLandedCostProcessor processor;

    @Override
    public ErpInvLandedCost reverseApprove(String id, IServiceContext context) {
        Long lid = Long.valueOf(id);
        ErpInvLandedCost landedCost = processor.requireLandedCost(lid, context);
        processor.validateCanReverse(landedCost, context);

        ErpInvCostAdjust costAdjust = processor.findCostAdjustForLandedCost(landedCost.getCode());
        List<ErpInvCostAdjustLine> adjustLines = costAdjust != null
                ? processor.loadAdjustLines(costAdjust.getId())
                : Collections.emptyList();

        processor.doReverseApprove(landedCost, costAdjust, adjustLines, context);

        return processor.reload(lid);
    }

    @Override
    protected IEntityDao<ErpInvLandedCost> dao() {
        return daoProvider.daoFor(ErpInvLandedCost.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getApproveStatus(ErpInvLandedCost entity) {
        return null;
    }

    @Override
    protected void setApproveStatus(ErpInvLandedCost entity, String status) {
        // not reached: Pattern B custom public override
    }

    @Override
    protected void setApprovedBy(ErpInvLandedCost entity, String userId) {
        // not reached: Pattern B custom public override
    }

    @Override
    protected void setApprovedAt(ErpInvLandedCost entity, java.sql.Timestamp ts) {
        // not reached: Pattern B custom public override
    }

    @Override
    protected boolean isRejected(ErpInvLandedCost entity) {
        return false;
    }

    @Override
    protected String approvedStatus() {
        return null;
    }

    @Override
    protected String submittedStatus() {
        return null;
    }
}
