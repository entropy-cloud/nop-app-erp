package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgSubcontractOrder;
import app.erp.mfg.service.ErpMfgConstants;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpMfgSubcontractOrder postProcessingFee per-mutation Processor（R6.2，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 RECEIVED→COMPLETED 加工费过账编排（SUBCONTRACT_FEE 凭证 + posted=true + docStatus 翻转）；共享 protected helper
 * 单一真相源在 {@link ErpMfgSubcontractOrderProcessor}。config-gated {@code erp-mfg.subcontract-posting-enabled}。
 */
public class ErpMfgSubcontractOrderPostProcessingFeeProcessor {

    @Inject
    ErpMfgSubcontractOrderProcessor facade;

    public ErpMfgSubcontractOrder postProcessingFee(Long subcontractOrderId, IServiceContext context) {
        ErpMfgSubcontractOrder order = facade.requireOrder(String.valueOf(subcontractOrderId), context);
        facade.requireStatus(order, ErpMfgConstants.SUBCONTRACT_STATUS_RECEIVED, "RECEIVED");

        if (facade.isSubcontractPostingEnabled()) {
            facade.subcontractPostingDispatcher.dispatchFeePosting(subcontractOrderId);
        }

        order = facade.orderDao().getEntityById(subcontractOrderId);
        order.setDocStatus(ErpMfgConstants.SUBCONTRACT_STATUS_COMPLETED);
        facade.orderDao().updateEntity(order);
        return order;
    }
}
