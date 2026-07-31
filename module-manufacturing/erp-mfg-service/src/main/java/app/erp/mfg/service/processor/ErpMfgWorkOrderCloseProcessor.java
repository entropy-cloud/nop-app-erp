package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.ErpMfgConstants;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * ErpMfgWorkOrder close per-mutation Processor（R6.2，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 STOPPED/IN_PROCESS→CLOSED 结案编排；共享 protected helper 单一真相源在 {@link ErpMfgWorkOrderProcessor}。
 */
public class ErpMfgWorkOrderCloseProcessor {

    @Inject
    ErpMfgWorkOrderProcessor facade;

    public ErpMfgWorkOrder close(Long workOrderId, IServiceContext context) {
        ErpMfgWorkOrder wo = facade.requireWorkOrder(String.valueOf(workOrderId), context);
        validateTransitionForClose(wo);
        doClose(wo);
        return wo;
    }

    protected void validateTransitionForClose(ErpMfgWorkOrder wo) {
        String status = wo.getDocStatus();
        if (status == null || (!Objects.equals(status, ErpMfgConstants.WORK_ORDER_STATUS_STOPPED)
                && !Objects.equals(status, ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS))) {
            throw facade.illegalTransition(wo, status, "STOPPED 或 IN_PROCESS");
        }
    }

    protected void doClose(ErpMfgWorkOrder wo) {
        wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_CLOSED);
        if (wo.getActualEndDate() == null) {
            wo.setActualEndDate(CoreMetrics.today());
        }
        facade.workOrderDao().updateEntity(wo);
    }
}
