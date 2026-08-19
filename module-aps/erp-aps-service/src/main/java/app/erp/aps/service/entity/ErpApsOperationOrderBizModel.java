
package app.erp.aps.service.entity;

import java.util.List;
import app.erp.aps.biz.CtpResult;
import app.erp.aps.biz.IErpApsAtpCtpService;
import app.erp.aps.biz.IErpApsOperationOrderBiz;
import app.erp.aps.biz.SchedulingResult;
import app.erp.aps.biz.BatchOperationResult;
import app.erp.aps.biz.WorkOrderOperationCreationResult;
import app.erp.aps.dao.entity.ErpApsOperationOrder;
import app.erp.aps.service.ErpApsConstants;
import app.erp.aps.service.ErpApsErrors;
import app.erp.aps.service.processor.ErpApsAutoDispatchProcessor;
import app.erp.aps.service.processor.ErpApsRoutingManualOverrideProcessor;
import app.erp.aps.service.processor.ErpApsSchedulingInsertRushOrderProcessor;
import app.erp.aps.service.processor.ErpApsSchedulingProcessor;
import app.erp.aps.service.processor.ErpApsSchedulingScheduleBackwardProcessor;
import app.erp.aps.service.processor.ErpApsSchedulingScheduleForwardProcessor;
import app.erp.aps.service.processor.ErpApsWorkOrderToOperationProcessor;
import app.erp.aps.service.statemachine.ErpApsOperationOrderStateMachine;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import io.nop.biz.crud.EntityData;

import static io.nop.api.core.beans.FilterBeans.eq;

@BizModel("ErpApsOperationOrder")
public class ErpApsOperationOrderBizModel extends CrudBizModel<ErpApsOperationOrder> implements IErpApsOperationOrderBiz {

    @Inject
    ErpApsSchedulingScheduleForwardProcessor scheduleForwardProcessor;

    @Inject
    ErpApsSchedulingScheduleBackwardProcessor scheduleBackwardProcessor;

    @Inject
    ErpApsSchedulingInsertRushOrderProcessor insertRushOrderProcessor;

    @Inject
    IErpApsAtpCtpService atpCtpService;

    @Inject
    ErpApsOperationOrderStateMachine stateMachine;

    @Inject
    ErpApsWorkOrderToOperationProcessor workOrderToOperationProcessor;

    @Inject
    ErpApsSchedulingProcessor schedulingProcessor;

    @Inject
    ErpApsRoutingManualOverrideProcessor routingManualOverrideProcessor;

    @Inject
    ErpApsAutoDispatchProcessor autoDispatchProcessor;

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
        return scheduleForwardProcessor.scheduleForward(scheduleId, context);
    }

    @Override
    @BizMutation
    public SchedulingResult scheduleBackward(@Name("scheduleId") Long scheduleId, IServiceContext context) {
        return scheduleBackwardProcessor.scheduleBackward(scheduleId, context);
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
        return insertRushOrderProcessor.insertRushOrder(operationOrderId, context);
    }

    /**
     * UC-APS-01 手动触发入口（L1「或计划员手动触发」）：守卫/幂等与 job 拉取扫描同源（同一 Processor）。
     */
    @Override
    @BizMutation
    public WorkOrderOperationCreationResult createOperationOrdersFromWorkOrder(@Name("workOrderId") Long workOrderId,
                                                                               IServiceContext context) {
        return workOrderToOperationProcessor.createOperationOrdersFromWorkOrder(workOrderId, context);
    }

    /**
     * UC-APS-01 自动触发（D1 选项 B 拉取扫描）：job bean 调用入口，亦可手动执行。
     */
    @Override
    @BizMutation
    public Integer scanReleasedWorkOrders(IServiceContext context) {
        return workOrderToOperationProcessor.scanReleasedWorkOrders(context);
    }

    /**
     * UC-APS-06 人工强制指定路由（RC-R1.87）。
     */
    @Override
    @BizMutation
    public ErpApsOperationOrder manualOverrideRouting(@Name("operationOrderId") Long operationOrderId,
                                                      @Name("routingId") Long routingId,
                                                      IServiceContext context) {
        return routingManualOverrideProcessor.manualOverrideRouting(
                schedulingProcessor, operationOrderId, routingId, context);
    }

    /**
     * UC-APS-07 自动派工扫描入口（RC-R1.88；job bean 与手动共用，全局开关门控在 Processor 内）。
     */
    @Override
    @BizMutation
    public Integer scanAutoDispatch(IServiceContext context) {
        return autoDispatchProcessor.scanOnce(context);
    }

    /**
     * UC-APS-07 手动强制派工（跳检原因必填）。
     */
    @Override
    @BizMutation
    public ErpApsOperationOrder dispatchManually(@Name("operationOrderId") Long operationOrderId,
                                                 @Name("note") String note,
                                                 IServiceContext context) {
        return autoDispatchProcessor.dispatchManually(operationOrderId, note, context);
    }

    /**
     * UC-APS-07 派工保持（PLANNED→HOLD）。
     */
    @Override
    @BizMutation
    public ErpApsOperationOrder hold(@Name("operationOrderId") Long operationOrderId, IServiceContext context) {
        return autoDispatchProcessor.hold(operationOrderId, context);
    }

    /**
     * UC-APS-07 解除保持（HOLD/ON_HOLD→PLANNED）。
     */
    @Override
    @BizMutation
    public ErpApsOperationOrder unhold(@Name("operationOrderId") Long operationOrderId, IServiceContext context) {
        return autoDispatchProcessor.unhold(operationOrderId, context);
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
        // 矩阵守卫下沉 Bean（PLANNED→IN_PROGRESS），非法边 Bean 抛 common 码，此处映射领域码。
        try {
            stateMachine.assertCanStart(order.getStatus());
        } catch (NopException e) {
            throw illegalTransition(order, ErpApsConstants.OP_STATUS_PLANNED, e);
        }
        order.setStatus(stateMachine.startTargetStatus());
        updateEntity(order, null, context);
        return order;
    }

    @Override
    @BizMutation
    public ErpApsOperationOrder complete(@Name("operationOrderId") Long operationOrderId, IServiceContext context) {
        ErpApsOperationOrder order = requireEntity(String.valueOf(operationOrderId), null, context);
        // 矩阵守卫下沉 Bean（IN_PROGRESS→FINISHED），非法边 Bean 抛 common 码，此处映射领域码。
        try {
            stateMachine.assertCanComplete(order.getStatus());
        } catch (NopException e) {
            throw illegalTransition(order, ErpApsConstants.OP_STATUS_IN_PROGRESS, e);
        }
        order.setStatus(stateMachine.completeTargetStatus());
        updateEntity(order, null, context);
        return order;
    }

    @Override
    @BizMutation
    public ErpApsOperationOrder cancel(@Name("operationOrderId") Long operationOrderId, IServiceContext context) {
        ErpApsOperationOrder order = requireEntity(String.valueOf(operationOrderId), null, context);
        // 矩阵守卫下沉 Bean（cancel 三源 {DRAFT,PLANNED,IN_PROGRESS}→CANCELLED），非法边 Bean 抛 common 码，
        // 此处映射领域码。cancel 三源经 Bean 正向枚举合法来源（对齐 owner doc §2 :24/:29/:33 + §3 终态不可恢复）。
        try {
            stateMachine.assertCanCancel(order.getStatus());
        } catch (NopException e) {
            throw illegalTransition(order,
                    ErpApsConstants.OP_STATUS_DRAFT + "/"
                            + ErpApsConstants.OP_STATUS_PLANNED + "/"
                            + ErpApsConstants.OP_STATUS_IN_PROGRESS, e);
        }
        order.setStatus(stateMachine.cancelTargetStatus());
        updateEntity(order, null, context);
        return order;
    }

    // ---------- helpers ----------

    /**
     * 领域非法迁移异常构造。可选 {@code cause} 保留 Bean 抛出的 common 层非法边报告（契约 §7：
     * Bean 报 common 码 + action/fromStatus 元数据，BizModel 映射领域码 + 实体编号/上下文，common 码作 cause 保留）。
     *
     * <p>保留对外契约不变：错误码 {@code ERR_APS_OP_ILLEGAL_TRANSITION} + 参数
     * {@code operationOrderCode}/{@code currentStatus}/{@code expectedStatus}（层 3 断言证实）。
     */
    protected NopException illegalTransition(ErpApsOperationOrder order, String expected, Throwable cause) {
        return new NopException(ErpApsErrors.ERR_APS_OP_ILLEGAL_TRANSITION, cause)
                .param(ErpApsErrors.ARG_OP_CODE, order.getCode())
                .param(ErpApsErrors.ARG_CURRENT_STATUS, order.getStatus())
                .param(ErpApsErrors.ARG_EXPECTED_STATUS, expected);
    }

    @Override
    @BizQuery
    public Map<String, Object> findGanttData(@Optional @Name("machineId") Long machineId,
                                             @Optional @Name("status") String status,
                                             IServiceContext context) {
        QueryBean query = new QueryBean();
        query.setLimit(500);
        query.addOrderField("plannedStartDateT", false);
        if (machineId != null) {
            query.addFilter(eq("machineId", machineId));
        }
        if (status != null && !status.isEmpty()) {
            query.addFilter(eq("status", status));
        }
        List<ErpApsOperationOrder> orders = findList(query, null, context);

        List<Map<String, Object>> tasks = new ArrayList<>();
        for (ErpApsOperationOrder o : orders) {
            Map<String, Object> task = new LinkedHashMap<>();
            task.put("id", String.valueOf(o.getId()));
            task.put("text", (o.getOperationName() != null ? o.getOperationName() : "")
                    + (o.getCode() != null ? " · " + o.getCode() : ""));
            task.put("start", o.getPlannedStartDateT());
            task.put("end", o.getPlannedEndDateT());
            task.put("type", "task");
            task.put("progress", ErpApsConstants.OP_STATUS_FINISHED.equals(o.getStatus()) ? 1.0
                    : ErpApsConstants.OP_STATUS_IN_PROGRESS.equals(o.getStatus()) ? 0.5 : 0.0);
            task.put("machineId", o.getMachineId());
            task.put("workOrderId", o.getWorkOrderId());
            task.put("sequence", o.getSequence());
            task.put("status", o.getStatus());
            tasks.add(task);
        }

        List<Map<String, Object>> links = new ArrayList<>();
        Map<Long, List<ErpApsOperationOrder>> byWorkOrder = new LinkedHashMap<>();
        for (ErpApsOperationOrder o : orders) {
            byWorkOrder.computeIfAbsent(o.getWorkOrderId(), k -> new ArrayList<>()).add(o);
        }
        for (List<ErpApsOperationOrder> group : byWorkOrder.values()) {
            List<ErpApsOperationOrder> sorted = new ArrayList<>(group);
            sorted.sort(Comparator.comparing(ErpApsOperationOrder::getSequence,
                    Comparator.nullsLast(Comparator.naturalOrder())));
            for (int i = 0; i < sorted.size() - 1; i++) {
                ErpApsOperationOrder cur = sorted.get(i);
                ErpApsOperationOrder next = sorted.get(i + 1);
                if (cur.getPlannedStartDateT() == null || next.getPlannedStartDateT() == null) {
                    continue;
                }
                Map<String, Object> link = new LinkedHashMap<>();
                link.put("source", String.valueOf(cur.getId()));
                link.put("target", String.valueOf(next.getId()));
                link.put("type", 0);
                link.put("lag", 0);
                links.add(link);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tasks", tasks);
        result.put("links", links);
        return result;
    }

    @Override
    @BizMutation
    public ErpApsOperationOrder updateSchedule(@Name("opOrderId") Long opOrderId,
                                               @Name("start") LocalDateTime start,
                                               @Name("end") LocalDateTime end,
                                               IServiceContext context) {
        ErpApsOperationOrder order = requireEntity(String.valueOf(opOrderId), null, context);
        if (start == null) {
            throw new NopException(ErpApsErrors.ERR_APS_OP_ILLEGAL_TRANSITION)
                    .param(ErpApsErrors.ARG_OP_CODE, order.getCode())
                    .param(ErpApsErrors.ARG_CURRENT_STATUS, order.getStatus());
        }
        order.setPlannedStartDateT(Timestamp.valueOf(start));
        if (end != null) {
            order.setPlannedEndDateT(Timestamp.valueOf(end));
        }
        updateEntity(order, null, context);
        return order;
    }

}
