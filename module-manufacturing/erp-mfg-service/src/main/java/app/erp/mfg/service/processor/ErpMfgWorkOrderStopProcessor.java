package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
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
        facade.validateTransitionForStop(wo, context);
        doStop(wo);
        return wo;
    }

    protected void doStop(ErpMfgWorkOrder wo) {
        wo.setDocStatus(facade.documentStateMachine.stopTargetStatus());
        facade.workOrderDao().updateEntity(wo);
    }
}
