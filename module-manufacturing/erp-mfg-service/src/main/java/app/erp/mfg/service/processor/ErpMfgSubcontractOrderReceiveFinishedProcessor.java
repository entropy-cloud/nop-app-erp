package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgSubcontractOrder;
import app.erp.mfg.service.ErpMfgConstants;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;

/**
 * ErpMfgSubcontractOrder receiveFinished per-mutation Processor（R6.2，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 ISSUED→RECEIVED 成品入库编排（INCOMING 移动单 + 加工费成本归集 + config-gated GL 过账）；共享 protected helper
 * 单一真相源在 {@link ErpMfgSubcontractOrderProcessor}。事务边界跟随 Facade {@code @BizMutation} 事务。
 */
public class ErpMfgSubcontractOrderReceiveFinishedProcessor {

    @Inject
    ErpMfgSubcontractOrderProcessor facade;

    public ErpMfgSubcontractOrder receiveFinished(String subcontractOrderId, BigDecimal receivedQty, IServiceContext context) {
        return receiveFinished(subcontractOrderId, receivedQty, null, context);
    }

    public ErpMfgSubcontractOrder receiveFinished(String subcontractOrderId, BigDecimal receivedQty,
                                                  String destWarehouseId, IServiceContext context) {
        ErpMfgSubcontractOrder order = facade.requireOrder(subcontractOrderId, context);
        facade.requireStatus(order, ErpMfgConstants.SUBCONTRACT_STATUS_ISSUED, "ISSUED");

        // P2-CK-mfg3-007：非正数量拒绝（修复前静默替换为行数量或 ONE）
        if (receivedQty == null || receivedQty.signum() <= 0) {
            throw new IllegalStateException("P2-CK-mfg3-007: receivedQty must be positive for subcontract order " + subcontractOrderId);
        }

        facade.generateReceiptMove(order, receivedQty, destWarehouseId, context);

        if (facade.isSubcontractPostingEnabled()) {
            facade.subcontractPostingDispatcher.dispatchReceiptPosting(subcontractOrderId);
        }

        order = facade.orderDao().getEntityById(subcontractOrderId);
        order.setDocStatus(ErpMfgConstants.SUBCONTRACT_STATUS_RECEIVED);
        facade.orderDao().updateEntity(order);
        return order;
    }
}
