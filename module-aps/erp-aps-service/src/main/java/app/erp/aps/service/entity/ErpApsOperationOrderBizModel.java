
package app.erp.aps.service.entity;

import java.util.List;
import java.util.Objects;
import app.erp.aps.biz.CtpResult;
import app.erp.aps.biz.IErpApsAtpCtpService;
import app.erp.aps.biz.IErpApsOperationOrderBiz;
import app.erp.aps.biz.SchedulingResult;
import app.erp.aps.biz.BatchOperationResult;
import app.erp.aps.dao.entity.ErpApsOperationOrder;
import app.erp.aps.service.ErpApsConstants;
import app.erp.aps.service.processor.ErpApsSchedulingProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
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

    /**
     * F11 批量前向排产（plan 2026-07-22-0444-2 Phase 2）。逐行调 {@link #scheduleForward}；
     * 行级失败（排程引擎异常）记入 {@link BatchOperationResult#getFailures()}，不阻塞其他行。
     */
    @Override
    @BizMutation
    public BatchOperationResult batchScheduleForward(@Name("ids") Collection<String> ids, IServiceContext context) {
        BatchOperationResult result = BatchOperationResult.forTotal(ids == null ? 0 : ids.size());
        if (ids == null || ids.isEmpty()) {
            return result;
        }
        for (String id : ids) {
            try {
                scheduleForward(Long.valueOf(id), context);
                result.recordSuccess();
            } catch (NopException e) {
                result.recordFailure(id, e.getErrorCode(), e.getDescription());
            } catch (NumberFormatException e) {
                result.recordFailure(id, "INVALID_ID", "非数字 ID：" + id);
            }
        }
        return result;
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

    @Override
    @BizMutation
    public ErpApsOperationOrder start(@Name("operationOrderId") Long operationOrderId, IServiceContext context) {
        ErpApsOperationOrder order = requireEntity(String.valueOf(operationOrderId), null, context);
        if (!Objects.equals(order.getStatus(), ErpApsConstants.OP_STATUS_PLANNED)) {
            order.setStatus(ErpApsConstants.OP_STATUS_IN_PROGRESS);
        } else {
            order.setStatus(ErpApsConstants.OP_STATUS_IN_PROGRESS);
        }
        updateEntity(order, null, context);
        return order;
    }

    @Override
    @BizMutation
    public ErpApsOperationOrder complete(@Name("operationOrderId") Long operationOrderId, IServiceContext context) {
        ErpApsOperationOrder order = requireEntity(String.valueOf(operationOrderId), null, context);
        order.setStatus(ErpApsConstants.OP_STATUS_FINISHED);
        updateEntity(order, null, context);
        return order;
    }

    @Override
    @BizMutation
    public ErpApsOperationOrder cancel(@Name("operationOrderId") Long operationOrderId, IServiceContext context) {
        ErpApsOperationOrder order = requireEntity(String.valueOf(operationOrderId), null, context);
        order.setStatus(ErpApsConstants.OP_STATUS_CANCELLED);
        updateEntity(order, null, context);
        return order;
    }

}
