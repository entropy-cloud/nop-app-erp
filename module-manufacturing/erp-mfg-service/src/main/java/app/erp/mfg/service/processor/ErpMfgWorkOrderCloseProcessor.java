package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.statemachine.ErpMfgWorkOrderDocumentStateMachine;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpMfgWorkOrder close per-mutation Processor（R6.2，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 STOPPED/IN_PROCESS→CLOSED 结案编排；共享 protected helper 单一真相源在 {@link ErpMfgWorkOrderProcessor}。
 * 固定来源态/目标态判断委托 {@link ErpMfgWorkOrderDocumentStateMachine}（plan 2026-08-14-0930-1 M4.35）。
 */
public class ErpMfgWorkOrderCloseProcessor {

    @Inject
    ErpMfgWorkOrderProcessor facade;

    @Inject
    ErpMfgWorkOrderDocumentStateMachine documentStateMachine;

    public ErpMfgWorkOrder close(Long workOrderId, IServiceContext context) {
        ErpMfgWorkOrder wo = facade.requireWorkOrder(String.valueOf(workOrderId), context);
        validateTransitionForClose(wo);
        doClose(wo);
        return wo;
    }

    protected void validateTransitionForClose(ErpMfgWorkOrder wo) {
        String status = wo.getDocStatus();
        try {
            documentStateMachine.assertCanClose(status);
        } catch (io.nop.api.core.exceptions.NopException e) {
            throw facade.illegalTransition(wo, status, "STOPPED 或 IN_PROCESS");
        }
    }

    protected void doClose(ErpMfgWorkOrder wo) {
        wo.setDocStatus(documentStateMachine.closeTargetStatus());
        if (wo.getActualEndDate() == null) {
            wo.setActualEndDate(CoreMetrics.today());
        }
        facade.workOrderDao().updateEntity(wo);
    }
}
