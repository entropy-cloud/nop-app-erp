
package app.erp.mfg.service.entity;

import app.erp.mfg.biz.IErpMfgWorkOrderBiz;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.processor.ErpMfgScheduleToJobCardProcessor;
import app.erp.mfg.service.processor.ErpMfgWorkOrderProcessor;
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
 * 工单 BizModel（Facade，{@code processor-extension-pattern.md} 两层结构）。
 * 工单 10 态状态机 + 三轴审批 + 齐套校验 + 完工入库编排委托
 * {@link ErpMfgWorkOrderProcessor}（protected step 方法，下游可逐 step 覆盖）。
 * APS 排程→工序卡自动生成委托 {@link ErpMfgScheduleToJobCardProcessor}（plan 2026-07-05-0427-3）。
 *
 * <p>语义见 {@code docs/design/manufacturing/state-machine.md §适用对象一}。
 */
@BizModel("ErpMfgWorkOrder")
public class ErpMfgWorkOrderBizModel extends CrudBizModel<ErpMfgWorkOrder> implements IErpMfgWorkOrderBiz {

    @Inject
    ErpMfgWorkOrderProcessor workOrderProcessor;
    @Inject
    ErpMfgScheduleToJobCardProcessor scheduleToJobCardProcessor;

    public ErpMfgWorkOrderBizModel() {
        setEntityName(ErpMfgWorkOrder.class.getName());
    }

    @Override
    @BizMutation
    public ErpMfgWorkOrder submit(@Name("workOrderId") Long workOrderId, IServiceContext context) {
        return workOrderProcessor.submit(workOrderId, context);
    }

    @Override
    @BizMutation
    public ErpMfgWorkOrder approve(@Name("workOrderId") Long workOrderId, IServiceContext context) {
        return workOrderProcessor.approve(workOrderId, context);
    }

    @Override
    @BizMutation
    public ErpMfgWorkOrder checkAvailability(@Name("workOrderId") Long workOrderId, IServiceContext context) {
        return workOrderProcessor.checkAvailability(workOrderId, context);
    }

    @Override
    @BizMutation
    public ErpMfgWorkOrder start(@Name("workOrderId") Long workOrderId, IServiceContext context) {
        return workOrderProcessor.start(workOrderId, context);
    }

    @Override
    @BizMutation
    public ErpMfgWorkOrder stop(@Name("workOrderId") Long workOrderId, IServiceContext context) {
        return workOrderProcessor.stop(workOrderId, context);
    }

    @Override
    @BizMutation
    public ErpMfgWorkOrder resume(@Name("workOrderId") Long workOrderId, IServiceContext context) {
        return workOrderProcessor.resume(workOrderId, context);
    }

    @Override
    @BizMutation
    public ErpMfgWorkOrder close(@Name("workOrderId") Long workOrderId, IServiceContext context) {
        return workOrderProcessor.close(workOrderId, context);
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
        return workOrderProcessor.reportCompletion(workOrderId, completedQty, context);
    }

    @Override
    @BizMutation
    public ErpMfgWorkOrder generateJobCardsFromSchedule(@Name("workOrderId") Long workOrderId, IServiceContext context) {
        return scheduleToJobCardProcessor.generateJobCardsFromSchedule(workOrderId, context);
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
        return scheduleToJobCardProcessor.generatePendingJobCards(context);
    }
}
