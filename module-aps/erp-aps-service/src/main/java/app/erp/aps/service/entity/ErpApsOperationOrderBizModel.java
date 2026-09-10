
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
import app.erp.aps.service.processor.ErpApsSchedulingScheduleTocProcessor;
import app.erp.aps.service.processor.ErpApsWorkOrderToOperationProcessor;
import app.erp.aps.service.statemachine.ErpApsOperationOrderStateMachine;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Description;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;
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
public class ErpApsOperationOrderBizModel extends AbstractErpCrudBizModel<ErpApsOperationOrder> implements IErpApsOperationOrderBiz {

    @Inject
    ErpApsSchedulingScheduleForwardProcessor scheduleForwardProcessor;

    @Inject
    ErpApsSchedulingScheduleBackwardProcessor scheduleBackwardProcessor;

    @Inject
    ErpApsSchedulingScheduleTocProcessor scheduleTocProcessor;

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
    public SchedulingResult scheduleForward(@Name("scheduleId") String scheduleId, IServiceContext context) {
        return scheduleForwardProcessor.scheduleForward(scheduleId, context);
    }

    @Override
    @BizMutation
    public SchedulingResult scheduleBackward(@Name("scheduleId") String scheduleId, IServiceContext context) {
        return scheduleBackwardProcessor.scheduleBackward(scheduleId, context);
    }

    @Override
    @BizMutation
    @Description("TOC 瓶颈驱动排产：识别 horizon 内超阈值瓶颈中心先排（拉动式），再前/后向兜底排非瓶颈，结果携带瓶颈清单与各中心负荷率")
    public SchedulingResult scheduleToc(@Name("scheduleId") String scheduleId, IServiceContext context) {
        return scheduleTocProcessor.scheduleToc(scheduleId, context);
    }

    /**
     * F11 批量前向排产（plan 2026-07-22-0444-2 Phase 2）。逐行调 {@link #scheduleForward}；
     * 行级失败（排程引擎异常）记入 {@link BatchOperationResult#getFailures()}，不阻塞其他行。
     *
     * <p>行级容错（P3-CK-aps-013-r3 修复面）：catch 面收窄为 {@link Exception}（非 Nop 引擎/持久层异常
     * 同样行级隔离，不再中止整批违 javadoc「不阻塞其他行」承诺；pur-013/mfg-017 同型族范式），
     * 失败行经 {@code orm().clearSession()} 丢弃半途脏状态（先行成功行已在各自 run 内 flushSession
     * 落库，clear 仅 detach 失败行残留，结果上报与事务边界对齐）。
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
                scheduleForward(id, context);
                result.recordSuccess();
            } catch (NopException e) {
                discardFailedRowState();
                result.recordFailure(id, e.getErrorCode(), e.getDescription());
            } catch (Exception e) {
                discardFailedRowState();
                result.recordFailure(id, e.getClass().getSimpleName(), String.valueOf(e.getMessage()));
            }
        }
        return result;
    }

    /** 丢弃失败行在共享 ORM 会话中的半途脏状态（防成功上报与实际落库背离）。 */
    protected void discardFailedRowState() {
        try {
            orm().clearSession();
        } catch (Exception ex) {
            // 会话清理失败不影响行级失败记录（结果上报为准）
        }
    }

    @Override
    @BizMutation
    public SchedulingResult insertRushOrder(@Name("operationOrderId") String operationOrderId, IServiceContext context) {
        return insertRushOrderProcessor.insertRushOrder(operationOrderId, context);
    }

    /**
     * UC-APS-01 手动触发入口（L1「或计划员手动触发」）：守卫/幂等与 job 拉取扫描同源（同一 Processor）。
     */
    @Override
    @BizMutation
    public WorkOrderOperationCreationResult createOperationOrdersFromWorkOrder(@Name("workOrderId") String workOrderId,
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
    public ErpApsOperationOrder manualOverrideRouting(@Name("operationOrderId") String operationOrderId,
                                                      @Name("routingId") String routingId,
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
    public ErpApsOperationOrder dispatchManually(@Name("operationOrderId") String operationOrderId,
                                                 @Name("note") String note,
                                                 IServiceContext context) {
        return autoDispatchProcessor.dispatchManually(operationOrderId, note, context);
    }

    /**
     * UC-APS-07 派工保持（PLANNED→HOLD）。
     */
    @Override
    @BizMutation
    public ErpApsOperationOrder hold(@Name("operationOrderId") String operationOrderId, IServiceContext context) {
        return autoDispatchProcessor.hold(operationOrderId, context);
    }

    /**
     * UC-APS-07 解除保持（HOLD/ON_HOLD→PLANNED）。
     */
    @Override
    @BizMutation
    public ErpApsOperationOrder unhold(@Name("operationOrderId") String operationOrderId, IServiceContext context) {
        return autoDispatchProcessor.unhold(operationOrderId, context);
    }

    @Override
    @BizQuery
    public LocalDateTime earliestCompletionDate(@Name("materialId") String materialId, @Name("qty") BigDecimal qty) {
        return atpCtpService.earliestCompletionDate(materialId, qty);
    }

    @Override
    @BizQuery
    public CtpResult checkFeasibility(@Name("materialId") String materialId,
                                      @Name("qty") BigDecimal qty,
                                      @Name("desiredDate") LocalDateTime desiredDate) {
        return atpCtpService.checkFeasibility(materialId, qty, desiredDate);
    }

    @Override
    @BizMutation
    public ErpApsOperationOrder start(@Name("operationOrderId") String operationOrderId, IServiceContext context) {
        ErpApsOperationOrder order = requireEntity(operationOrderId, null, context);
        // 矩阵守卫下沉 Bean（PLANNED→IN_PROGRESS），非法边 Bean 直抛领域码（plan 2026-09-07-2200-1），本处同码补参 operationOrderCode。
        try {
            stateMachine.assertCanStart(order.getStatus());
        } catch (NopException e) {
            throw e.param(ErpApsErrors.ARG_OP_CODE, order.getCode());
        }
        order.setStatus(stateMachine.startTargetStatus());
        updateEntity(order, null, context);
        return order;
    }

    @Override
    @BizMutation
    public ErpApsOperationOrder complete(@Name("operationOrderId") String operationOrderId, IServiceContext context) {
        ErpApsOperationOrder order = requireEntity(operationOrderId, null, context);
        // 矩阵守卫下沉 Bean（IN_PROGRESS→FINISHED），非法边 Bean 直抛领域码（plan 2026-09-07-2200-1），本处同码补参 operationOrderCode。
        try {
            stateMachine.assertCanComplete(order.getStatus());
        } catch (NopException e) {
            throw e.param(ErpApsErrors.ARG_OP_CODE, order.getCode());
        }
        order.setStatus(stateMachine.completeTargetStatus());
        updateEntity(order, null, context);
        return order;
    }

    @Override
    @BizMutation
    public ErpApsOperationOrder cancel(@Name("operationOrderId") String operationOrderId, IServiceContext context) {
        ErpApsOperationOrder order = requireEntity(operationOrderId, null, context);
        // 矩阵守卫下沉 Bean（cancel 三源 {DRAFT,PLANNED,IN_PROGRESS}→CANCELLED），非法边 Bean 直抛领域码，
        // 本处同码补参 operationOrderCode（plan 2026-09-07-2200-1）。cancel 三源经 Bean 正向枚举合法来源
        // （对齐 owner doc §2 :24/:29/:33 + §3 终态不可恢复）。
        try {
            stateMachine.assertCanCancel(order.getStatus());
        } catch (NopException e) {
            throw e.param(ErpApsErrors.ARG_OP_CODE, order.getCode());
        }
        order.setStatus(stateMachine.cancelTargetStatus());
        updateEntity(order, null, context);
        return order;
    }

    // ---------- helpers ----------

    @Override
    @BizQuery
    public Map<String, Object> findGanttData(@Optional @Name("machineId") String machineId,
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
        Map<String, List<ErpApsOperationOrder>> byWorkOrder = new LinkedHashMap<>();
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
    public ErpApsOperationOrder updateSchedule(@Name("opOrderId") String opOrderId,
                                               @Name("start") LocalDateTime start,
                                               @Name("end") LocalDateTime end,
                                               IServiceContext context) {
        ErpApsOperationOrder order = requireEntity(opOrderId, null, context);
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
