package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgSubcontractOrder;
import app.erp.mfg.dao.entity.ErpMfgSubcontractOrderLine;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.List;

/**
 * ErpMfgSubcontractOrder issueMaterials per-mutation Processor（R6.2，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 APPROVED→ISSUED 发料出库编排（按委外行生成 OUTGOING 移动单 + config-gated GL 过账）；共享 protected helper
 * 单一真相源在 {@link ErpMfgSubcontractOrderProcessor}。事务边界跟随 Facade {@code @BizMutation} 事务。
 */
public class ErpMfgSubcontractOrderIssueMaterialsProcessor {

    @Inject
    ErpMfgSubcontractOrderProcessor facade;

    public ErpMfgSubcontractOrder issueMaterials(Long subcontractOrderId, IServiceContext context) {
        return issueMaterials(subcontractOrderId, null, context);
    }

    public ErpMfgSubcontractOrder issueMaterials(Long subcontractOrderId, Long sourceWarehouseId, IServiceContext context) {
        ErpMfgSubcontractOrder order = facade.requireOrder(String.valueOf(subcontractOrderId), context);
        facade.requireStatus(order, ErpMfgConstants.SUBCONTRACT_STATUS_APPROVED, "APPROVED");

        List<ErpMfgSubcontractOrderLine> lines = facade.loadLines(subcontractOrderId);
        if (lines.isEmpty()) {
            throw new NopException(ErpMfgErrors.ERR_SUBCONTRACT_LINES_EMPTY)
                    .param(ErpMfgErrors.ARG_SUBCONTRACT_ORDER_CODE, order.getCode());
        }

        facade.generateIssueMove(order, lines, sourceWarehouseId, context);

        if (facade.isSubcontractPostingEnabled()) {
            facade.subcontractPostingDispatcher.dispatchIssuePosting(subcontractOrderId);
        }

        order = facade.orderDao().getEntityById(subcontractOrderId);
        order.setDocStatus(ErpMfgConstants.SUBCONTRACT_STATUS_ISSUED);
        facade.orderDao().updateEntity(order);
        return order;
    }
}
