package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.ErpMfgConstants;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpMfgWorkOrder resume per-mutation Processor（R6.2，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 STOPPED→IN_PROCESS 恢复编排；共享 protected helper 单一真相源在 {@link ErpMfgWorkOrderProcessor}。
 */
public class ErpMfgWorkOrderResumeProcessor {

    @Inject
    ErpMfgWorkOrderProcessor facade;

    public ErpMfgWorkOrder resume(Long workOrderId, IServiceContext context) {
        ErpMfgWorkOrder wo = facade.requireWorkOrder(String.valueOf(workOrderId), context);
        facade.requireStatus(wo, ErpMfgConstants.WORK_ORDER_STATUS_STOPPED, "STOPPED");
        doResume(wo);
        return wo;
    }

    protected void doResume(ErpMfgWorkOrder wo) {
        wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS);
        facade.workOrderDao().updateEntity(wo);
    }
}
