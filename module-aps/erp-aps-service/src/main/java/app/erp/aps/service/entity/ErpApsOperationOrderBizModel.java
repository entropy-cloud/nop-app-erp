
package app.erp.aps.service.entity;

import java.util.List;
import app.erp.aps.biz.CtpResult;
import app.erp.aps.biz.IErpApsAtpCtpService;
import app.erp.aps.biz.IErpApsOperationOrderBiz;
import app.erp.aps.biz.SchedulingResult;
import app.erp.aps.dao.entity.ErpApsOperationOrder;
import app.erp.aps.service.processor.ErpApsSchedulingProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.nop.biz.crud.EntityData;

@BizModel("ErpApsOperationOrder")
public class ErpApsOperationOrderBizModel extends CrudBizModel<ErpApsOperationOrder> implements IErpApsOperationOrderBiz {

    @Inject
    ErpApsSchedulingProcessor schedulingProcessor;

    @Inject
    IErpApsAtpCtpService atpCtpService;

    public ErpApsOperationOrderBizModel() {
        setEntityName(ErpApsOperationOrder.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpApsOperationOrder> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpApsOperationOrder entity = entityData.getEntity();
        if (entity.getBusinessDate() == null) {
            entity.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        }
    }

    @Override
    @BizMutation
    public SchedulingResult scheduleForward(@Name("scheduleId") Long scheduleId, IServiceContext context) {
        return schedulingProcessor.scheduleForward(scheduleId, context);
    }

    @Override
    @BizMutation
    public SchedulingResult scheduleBackward(@Name("scheduleId") Long scheduleId, IServiceContext context) {
        return schedulingProcessor.scheduleBackward(scheduleId, context);
    }

    @Override
    @BizMutation
    public SchedulingResult insertRushOrder(@Name("operationOrderId") Long operationOrderId, IServiceContext context) {
        return schedulingProcessor.insertRushOrder(operationOrderId, context);
    }

    @Override
    @BizQuery
    public LocalDateTime earliestCompletionDate(@Name("materialId") Long materialId, @Name("qty") BigDecimal qty) {
        return atpCtpService.earliestCompletionDate(materialId, qty);
    }

    @Override
    @BizQuery
    public CtpResult checkFeasibility(@Name("materialId") Long materialId,
                                      @Name("qty") BigDecimal qty,
                                      @Name("desiredDate") LocalDateTime desiredDate) {
        return atpCtpService.checkFeasibility(materialId, qty, desiredDate);
    }

}
