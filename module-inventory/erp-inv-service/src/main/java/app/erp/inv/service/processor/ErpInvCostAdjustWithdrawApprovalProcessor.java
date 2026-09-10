package app.erp.inv.service.processor;

import app.erp.inv.dao.entity.ErpInvCostAdjust;
import app.erp.inv.service.ErpInvConstants;
import app.erp.inv.service.ErpInvErrors;
import app.erp.common.service.AbstractWithdrawApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpInvCostAdjust）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * ErpInvCostAdjust withdrawApproval per-mutation Processor (plan 2026-07-25-1057-2, R5.6 Pattern B).
 * Self-contained orchestration: require → validateNotCancelled → validateTransitionForWithdraw
 * → set UNSUBMITTED → save. Domain logic via facade protected helpers (single source of truth).
 */
public class ErpInvCostAdjustWithdrawApprovalProcessor extends AbstractWithdrawApprovalProcessor<ErpInvCostAdjust> {

    @Inject
    ErpInvCostAdjustProcessor processor;

    @Override
    public ErpInvCostAdjust withdrawApproval(String id, IServiceContext context) {
        ErpInvCostAdjust adjust = processor.requireAdjustment(id, context);
        processor.validateNotCancelled(adjust, context);
        processor.validateTransitionForWithdraw(adjust);
        adjust.setApproveStatus(ErpInvConstants.APPROVE_STATUS_UNSUBMITTED);
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
    protected boolean isCancelled(ErpInvCostAdjust entity) {
        return entity.isCancelled();
    }

    @Override
    protected String unsubmittedStatus() {
        return ErpInvConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    @Override
    protected String submittedStatus() {
        return ErpInvConstants.APPROVE_STATUS_SUBMITTED;
    }
}
