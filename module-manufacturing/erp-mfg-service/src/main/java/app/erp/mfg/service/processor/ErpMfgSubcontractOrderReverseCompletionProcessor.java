package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgSubcontractOrder;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpMfgSubcontractOrder reverseCompletion per-mutation Processor（R6.2，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 COMPLETED→CANCELLED 全量红冲编排（红冲三段 GL 凭证 + 反向两段库存移动 + posted=false + 状态回退）；共享 protected
 * helper 单一真相源在 {@link ErpMfgSubcontractOrderProcessor}。红冲 GL 以 {@code posted==true} 为前置（非 config flag）。
 *
 * <p>事务边界跟随 Facade {@code @BizMutation} 事务。
 */
public class ErpMfgSubcontractOrderReverseCompletionProcessor {

    @Inject
    ErpMfgSubcontractOrderProcessor facade;

    public ErpMfgSubcontractOrder reverseCompletion(Long subcontractOrderId, IServiceContext context) {
        ErpMfgSubcontractOrder order = facade.requireOrder(String.valueOf(subcontractOrderId), context);
        facade.validateCanReverse(order, context);
        facade.reverseGlPostings(order, context);
        facade.reverseInventoryMoves(order, context);
        facade.doReverseCompletion(order, context);
        return order;
    }
}
