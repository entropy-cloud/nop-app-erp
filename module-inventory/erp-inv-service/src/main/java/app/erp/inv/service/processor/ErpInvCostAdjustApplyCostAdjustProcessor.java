package app.erp.inv.service.processor;

import app.erp.inv.dao.entity.ErpInvCostAdjust;
import app.erp.inv.dao.entity.ErpInvCostAdjustLine;
import app.erp.inv.service.ErpInvConstants;
import app.erp.inv.service.ErpInvErrors;
import app.erp.inv.service.costing.CostAdjustmentService;
import app.erp.inv.service.posting.CostAdjustmentPostingDispatcher;
import app.erp.inv.service.statemachine.ErpInvCostAdjustStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

/**
 * ErpInvCostAdjust applyCostAdjust per-mutation Processor（R6.4，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含应用编排：require/validate（含 docStatus 源态守卫，委托 {@link ErpInvCostAdjustStateMachine}）→ 应用成本层
 * → 过账派发 → 终态回写（目标态取自 Bean）。共享 protected helper 单一真相源在
 * {@link ErpInvCostAdjustProcessor}（slim-to-S-delegation facade）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpInvCostAdjustApplyCostAdjustProcessor {

    @Inject
    ErpInvCostAdjustProcessor facade;

    @Inject
    ErpInvCostAdjustStateMachine stateMachine;

    @Inject
    CostAdjustmentService costAdjustmentService;

    @Inject
    IOrmTemplate ormTemplate;

    @Inject
    CostAdjustmentPostingDispatcher postingDispatcher;

    public ErpInvCostAdjust applyCostAdjust(Long id, IServiceContext context) {
        ErpInvCostAdjust adjust = requireAndValidate(id, context);
        List<ErpInvCostAdjustLine> lines = facade.loadLines(adjust.getId());
        BigDecimal totalAdjustAmount = applyCostLayer(adjust, lines);
        Long voucherId = postingDispatcher.tryPost(adjust, lines, totalAdjustAmount);
        return finalizeApplied(id, voucherId);
    }

    protected ErpInvCostAdjust requireAndValidate(Long id, IServiceContext context) {
        ErpInvCostAdjust adjust = facade.requireAdjustment(id, context);
        facade.validateNotCancelled(adjust, context);
        if (Boolean.TRUE.equals(adjust.getPosted())) {
            throw new NopException(ErpInvErrors.ERR_COST_ADJUST_ALREADY_APPLIED)
                    .param(ErpInvErrors.ARG_ADJUST_CODE, adjust.getCode());
        }
        // 固定来源态守卫委托 StateMachine Bean（非法边 Bean 抛 common 层码，映射为领域码 + common 作 cause；
        // docStatus 无专属 illegal-transition 码，映射到既有 generic ERR_ILLEGAL_STATUS_TRANSITION，
        // 见计划 Phase 3 Decision）。置于已-applied 检查之后——保持既有 DONE+posted=true → ALREADY_APPLIED
        // 行为；对 DONE+posted=false（net-0 边缘）从 DONE 重 apply 属合理收紧（计划 Draft Review Record MINOR=1）。
        try {
            stateMachine.assertCanApplyCostAdjust(adjust.getDocStatus());
        } catch (NopException e) {
            throw new NopException(ErpInvErrors.ERR_ILLEGAL_STATUS_TRANSITION, e)
                    .param(ErpInvErrors.ARG_MOVE_CODE, adjust.getCode())
                    .param(ErpInvErrors.ARG_CURRENT_STATUS, adjust.getDocStatus())
                    .param(ErpInvErrors.ARG_EXPECTED_STATUS, "DRAFT或CONFIRMED");
        }
        if (facade.isApprovalRequired() && !Objects.equals(facade.currentApproveStatus(adjust),
                ErpInvConstants.APPROVE_STATUS_APPROVED)) {
            throw new NopException(ErpInvErrors.ERR_COST_ADJUST_NOT_APPROVED)
                    .param(ErpInvErrors.ARG_ADJUST_CODE, adjust.getCode())
                    .param(ErpInvErrors.ARG_CURRENT_STATUS, facade.currentApproveStatus(adjust));
        }
        return adjust;
    }

    protected BigDecimal applyCostLayer(ErpInvCostAdjust adjust, List<ErpInvCostAdjustLine> lines) {
        BigDecimal totalAdjustAmount = costAdjustmentService.applyCostAdjust(adjust, lines);
        ormTemplate.flushSession();
        return totalAdjustAmount;
    }

    protected ErpInvCostAdjust finalizeApplied(Long id, Long voucherId) {
        ErpInvCostAdjust adjust = facade.reload(id);
        Timestamp now = CoreMetrics.currentTimestamp();
        adjust.setDocStatus(stateMachine.applyCostAdjustTargetStatus());
        if (voucherId != null) {
            adjust.setPosted(true);
            adjust.setPostedAt(now);
            adjust.setPostedBy(facade.currentUserId());
        }
        facade.adjustDao().updateEntity(adjust);
        return adjust;
    }
}
