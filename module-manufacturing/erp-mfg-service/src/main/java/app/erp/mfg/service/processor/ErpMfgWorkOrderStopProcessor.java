package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.ErpMfgConstants;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpMfgWorkOrder stop per-mutation Processor（R6.2，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 IN_PROCESS→STOPPED 暂停编排；共享 protected helper 单一真相源在 {@link ErpMfgWorkOrderProcessor}。
 */
public class ErpMfgWorkOrderStopProcessor {

    @Inject
    ErpMfgWorkOrderProcessor facade;

    public ErpMfgWorkOrder stop(Long workOrderId, IServiceContext context) {
        ErpMfgWorkOrder wo = facade.requireWorkOrder(String.valueOf(workOrderId), context);
        facade.requireStatus(wo, ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS, "IN_PROCESS");
        doStop(wo);
        return wo;
    }

    protected void doStop(ErpMfgWorkOrder wo) {
        wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_STOPPED);
        facade.workOrderDao().updateEntity(wo);
    }
}
