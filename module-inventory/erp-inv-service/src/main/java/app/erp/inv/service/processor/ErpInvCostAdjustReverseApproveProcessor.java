package app.erp.inv.service.processor;

import app.erp.inv.dao.entity.ErpInvCostAdjust;
import app.erp.inv.service.ErpInvConstants;
import app.erp.inv.service.ErpInvErrors;
import app.erp.common.service.AbstractReverseApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpInvCostAdjust）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
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
            throw processor.illegalTransition(adjust, processor.currentApproveStatus(adjust), "!POSTED");
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

    /**
     * 骨架守卫裸奔通道修正（plan 2026-09-07-2200-1 form-3）：非法迁移错误由 common 码改为直抛领域码
     * ERR_ILLEGAL_STATUS_TRANSITION（参数形态与 facade illegalTransition helper 一致）。
     */
    @Override
    protected NopException illegalStatusException(ErpInvCostAdjust entity, String current, String... expected) {
        return new NopException(ErpInvErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                .param(ErpInvErrors.ARG_MOVE_CODE, entity.getCode())
                .param(ErpInvErrors.ARG_CURRENT_STATUS, current)
                .param(ErpInvErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
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
