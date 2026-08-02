package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpMfgWorkOrder start per-mutation Processor（R6.2，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 STOCK_RESERVED/STOCK_PARTIAL→IN_PROCESS 开工编排；共享 protected helper 单一真相源在
 * {@link ErpMfgWorkOrderProcessor}。事务边界跟随 Facade {@code @BizMutation} 事务。
 */
public class ErpMfgWorkOrderStartProcessor {

    @Inject
    ErpMfgWorkOrderProcessor facade;

    public ErpMfgWorkOrder start(Long workOrderId, IServiceContext context) {
        ErpMfgWorkOrder wo = facade.requireWorkOrder(String.valueOf(workOrderId), context);
        facade.validateTransitionForStart(wo, context);
        facade.doStart(wo, context);
        return wo;
    }
}
