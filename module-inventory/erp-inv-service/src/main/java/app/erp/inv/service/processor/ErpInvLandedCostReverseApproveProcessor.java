package app.erp.inv.service.processor;

import app.erp.inv.dao.entity.ErpInvCostAdjust;
import app.erp.inv.dao.entity.ErpInvCostAdjustLine;
import app.erp.inv.dao.entity.ErpInvLandedCost;
import app.erp.inv.service.ErpInvErrors;
import app.erp.inv.service.statemachine.ErpInvLandedCostStateMachine;
import app.erp.common.service.AbstractReverseApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.Collections;
import java.util.List;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpInvLandedCost）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * ErpInvLandedCost reverseApprove per-mutation Processor (plan 2026-07-25-1057-2, R5.6 Pattern B).
 * Self-contained orchestration: require → validateCanReverse → docStatus 源态守卫（委托
 * {@link ErpInvLandedCostStateMachine}，双轴联动中 Bean 仅 docStatus 边，approveStatus 写留 facade
 * doReverseApprove）→ findCostAdjustForLandedCost → loadAdjustLines → doReverseApprove (accounting protection
 * area: GL voucher reversal + cost layer reversal + posted=false + postedAt=now + approveStatus=REJECTED +
 * docStatus=CANCELLED + sync CostAdjust entity) → reload.
 * Domain logic via facade protected helpers (single source of truth — accounting rules not copied).
 * Dormant until R5.8 rewire（BizModel Java 直调 facade.reverseApprove，不经 xbiz 委托链）。
 * id String 边界：基类与 facade 均为 String id（M2.2 inv id String 化后无转换边界）。
 */
public class ErpInvLandedCostReverseApproveProcessor extends AbstractReverseApproveProcessor<ErpInvLandedCost> {

    @Inject
    ErpInvLandedCostProcessor processor;

    @Inject
    ErpInvLandedCostStateMachine stateMachine;

    @Override
    public ErpInvLandedCost reverseApprove(String id, IServiceContext context) {
        ErpInvLandedCost landedCost = processor.requireLandedCost(id, context);
        processor.validateCanReverse(landedCost, context);
        // 固定来源态守卫委托 StateMachine Bean（Bean 直抛领域码 ERR_ILLEGAL_STATUS_TRANSITION，本处同码补参
        // moveCode；docStatus 无实体专属 illegal-transition 码，见计划 Phase 3 Decision）。
        // 置于 validateCanReverse 之后——保持既有 posted/APPROVED → NOT_POSTED 行为。
        try {
            stateMachine.assertCanReverseApprove(landedCost.getDocStatus());
        } catch (NopException e) {
            throw e.param(ErpInvErrors.ARG_MOVE_CODE, landedCost.getCode());
        }

        ErpInvCostAdjust costAdjust = processor.findCostAdjustForLandedCost(landedCost.getCode());
        List<ErpInvCostAdjustLine> adjustLines = costAdjust != null
                ? processor.loadAdjustLines(costAdjust.getId())
                : Collections.emptyList();

        processor.doReverseApprove(landedCost, costAdjust, adjustLines, context);

        return processor.reload(id);
    }

    @Override
    protected IEntityDao<ErpInvLandedCost> dao() {
        return daoProvider.daoFor(ErpInvLandedCost.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    /**
     * 骨架守卫裸奔通道修正（plan 2026-09-07-2200-1 form-3）：非法迁移错误由 common 码改为直抛领域码
     * ERR_ILLEGAL_STATUS_TRANSITION（docStatus 无实体专属 illegal-transition 码，参数形态与既有 remap 一致）。
     */
    @Override
    protected NopException illegalStatusException(ErpInvLandedCost entity, String current, String... expected) {
        return new NopException(ErpInvErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                .param(ErpInvErrors.ARG_MOVE_CODE, entity.getCode())
                .param(ErpInvErrors.ARG_CURRENT_STATUS, current)
                .param(ErpInvErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
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
