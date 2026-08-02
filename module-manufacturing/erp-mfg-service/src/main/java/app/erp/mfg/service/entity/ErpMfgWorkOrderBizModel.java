
package app.erp.mfg.service.entity;

import app.erp.mfg.biz.IErpMfgWorkOrderBiz;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.processor.ErpMfgScheduleToJobCardGenerateJobCardsFromScheduleProcessor;
import app.erp.mfg.service.processor.ErpMfgScheduleToJobCardGeneratePendingJobCardsProcessor;
import app.erp.mfg.service.processor.ErpMfgScheduleToJobCardProcessor;
import app.erp.mfg.service.processor.ErpMfgWorkOrderCloseProcessor;
import app.erp.mfg.service.processor.ErpMfgWorkOrderProcessor;
import app.erp.mfg.service.processor.ErpMfgWorkOrderReportCompletionProcessor;
import app.erp.mfg.service.processor.ErpMfgWorkOrderResumeProcessor;
import app.erp.mfg.service.processor.ErpMfgWorkOrderStartProcessor;
import app.erp.mfg.service.processor.ErpMfgWorkOrderStopProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.List;

/**
 * 工单 BizModel（Facade，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 工单 10 态状态机 + 齐套校验 + 完工入库编排委托 5 个 {@code ErpMfgWorkOrder<Method>Processor}
 *（R6.2 per-mutation 拆分）；{@code checkAvailability}（:45 只读可用性校验）+ {@code cancel}（:46 单步状态翻转）
 * 为合法豁免保留委托 facade。APS 排程→工序卡自动生成委托 2 个 {@code ErpMfgScheduleToJobCard<Method>Processor}，
 * {@code findWorkOrdersPendingJobCards}（:45 只读查询）保留委托 facade。
 * 标准审批动作经实体 xbiz 委托 per-mutation Processor（plan 2026-07-30-1909-2 R5.5）。
 *
 * <p>语义见 {@code docs/design/manufacturing/state-machine.md §适用对象一}。
 */
@BizModel("ErpMfgWorkOrder")
public class ErpMfgWorkOrderBizModel extends CrudBizModel<ErpMfgWorkOrder> implements IErpMfgWorkOrderBiz {

    @Inject
    ErpMfgWorkOrderProcessor workOrderProcessor;
    @Inject
    ErpMfgScheduleToJobCardProcessor scheduleToJobCardProcessor;
    @Inject
    ErpMfgWorkOrderStartProcessor startProcessor;
    @Inject
    ErpMfgWorkOrderStopProcessor stopProcessor;
    @Inject
    ErpMfgWorkOrderResumeProcessor resumeProcessor;
    @Inject
    ErpMfgWorkOrderCloseProcessor closeProcessor;
    @Inject
    ErpMfgWorkOrderReportCompletionProcessor reportCompletionProcessor;
    @Inject
    ErpMfgScheduleToJobCardGenerateJobCardsFromScheduleProcessor generateJobCardsFromScheduleProcessor;
    @Inject
    ErpMfgScheduleToJobCardGeneratePendingJobCardsProcessor generatePendingJobCardsProcessor;

    public ErpMfgWorkOrderBizModel() {
        setEntityName(ErpMfgWorkOrder.class.getName());
    }

    @Override
    @BizMutation
    public ErpMfgWorkOrder checkAvailability(@Name("workOrderId") Long workOrderId, IServiceContext context) {
        return workOrderProcessor.checkAvailability(workOrderId, context);
    }

    @Override
    @BizMutation
    public ErpMfgWorkOrder start(@Name("workOrderId") Long workOrderId, IServiceContext context) {
        return startProcessor.start(workOrderId, context);
    }

    @Override
    @BizMutation
    public ErpMfgWorkOrder stop(@Name("workOrderId") Long workOrderId, IServiceContext context) {
        return stopProcessor.stop(workOrderId, context);
    }

    @Override
    @BizMutation
    public ErpMfgWorkOrder resume(@Name("workOrderId") Long workOrderId, IServiceContext context) {
        return resumeProcessor.resume(workOrderId, context);
    }

    @Override
    @BizMutation
    public ErpMfgWorkOrder close(@Name("workOrderId") Long workOrderId, IServiceContext context) {
        return closeProcessor.close(workOrderId, context);
    }

    @Override
    @BizMutation
    public ErpMfgWorkOrder cancel(@Name("workOrderId") Long workOrderId, IServiceContext context) {
        return workOrderProcessor.cancel(workOrderId, context);
    }

    @Override
    @BizMutation
    public ErpMfgWorkOrder reportCompletion(@Name("workOrderId") Long workOrderId,
                                            @Name("completedQty") BigDecimal completedQty,
                                            IServiceContext context) {
        return reportCompletionProcessor.reportCompletion(workOrderId, completedQty, context);
    }

    @Override
    @BizMutation
    public ErpMfgWorkOrder generateJobCardsFromSchedule(@Name("workOrderId") Long workOrderId, IServiceContext context) {
        return generateJobCardsFromScheduleProcessor.generateJobCardsFromSchedule(workOrderId, context);
    }

    @Override
    @BizQuery
    public List<ErpMfgWorkOrder> findWorkOrdersPendingJobCards(@Optional @Name("limit") Integer limit,
                                                               IServiceContext context) {
        return scheduleToJobCardProcessor.findWorkOrdersPendingJobCards(limit, context);
    }

    @Override
    @BizMutation
    public Integer generatePendingJobCards(IServiceContext context) {
        return generatePendingJobCardsProcessor.generatePendingJobCards(context);
    }

}
