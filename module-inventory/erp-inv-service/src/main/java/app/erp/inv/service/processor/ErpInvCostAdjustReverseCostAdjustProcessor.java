package app.erp.inv.service.processor;

import app.erp.inv.dao.entity.ErpInvCostAdjust;
import app.erp.inv.dao.entity.ErpInvCostAdjustLine;
import app.erp.inv.service.ErpInvConstants;
import app.erp.inv.service.ErpInvErrors;
import app.erp.inv.service.costing.CostAdjustmentService;
import app.erp.inv.service.posting.CostAdjustmentPostingDispatcher;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.util.List;

/**
 * ErpInvCostAdjust reverseCostAdjust per-mutation Processor（R6.4，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含红冲编排：require/posted 守卫 → 反向应用成本层 → 红冲凭证 → 回退终态。共享 protected helper 单一真相源在
 * {@link ErpInvCostAdjustProcessor}（slim-to-S-delegation facade）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpInvCostAdjustReverseCostAdjustProcessor {

    @Inject
    ErpInvCostAdjustProcessor facade;

    @Inject
    CostAdjustmentService costAdjustmentService;

    @Inject
    IOrmTemplate ormTemplate;

    @Inject
    CostAdjustmentPostingDispatcher postingDispatcher;

    public ErpInvCostAdjust reverseCostAdjust(Long id, IServiceContext context) {
        ErpInvCostAdjust adjust = requirePosted(id, context);
        List<ErpInvCostAdjustLine> lines = facade.loadLines(adjust.getId());
        reverseCostLayer(adjust, lines);
        postingDispatcher.reverse(adjust);
        return revertToConfirmed(id);
    }

    protected ErpInvCostAdjust requirePosted(Long id, IServiceContext context) {
        ErpInvCostAdjust adjust = facade.requireAdjustment(id, context);
        if (!Boolean.TRUE.equals(adjust.getPosted())) {
            throw new NopException(ErpInvErrors.ERR_COST_ADJUST_NOT_APPLIED)
                    .param(ErpInvErrors.ARG_ADJUST_CODE, adjust.getCode());
        }
        return adjust;
    }

    protected void reverseCostLayer(ErpInvCostAdjust adjust, List<ErpInvCostAdjustLine> lines) {
        costAdjustmentService.reverseCostAdjust(adjust, lines);
        ormTemplate.flushSession();
    }

    protected ErpInvCostAdjust revertToConfirmed(Long id) {
        ErpInvCostAdjust adjust = facade.reload(id);
        adjust.setPosted(false);
        adjust.setPostedAt(null);
        adjust.setPostedBy(null);
        adjust.setDocStatus(ErpInvConstants.DOC_STATUS_CONFIRMED);
        facade.adjustDao().updateEntity(adjust);
        return adjust;
    }
}
