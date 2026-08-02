package app.erp.aps.service.processor;

import app.erp.aps.biz.SchedulingResult;
import app.erp.aps.dao.entity.ErpApsConstraint;
import app.erp.aps.dao.entity.ErpApsOperationOrder;
import app.erp.aps.service.ErpApsConfigs;
import app.erp.aps.service.ErpApsConstants;
import app.erp.aps.service.ErpApsErrors;
import app.erp.aps.service.scheduling.ErpApsSchedulingEngine;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ErpApsScheduling insertRushOrder per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 *
 * <p>插单区间重排（{@code scheduling.md §六}）：检测急单工序 {@code [earliestStartDateT, latestEndDateT+buffer]}
 * 时间窗口，窗口内同工作中心、优先级低于新单的 PLANNED 工序回退 DRAFT；IN_PROGRESS 工序永不回退
 * （抛 {@code ERR_APS_OP_IN_PROGRESS_NOT_RESCHEDULABLE}）；窗口外工序与高优先级工序不受影响。
 * 随后仅对窗口内 DRAFT 工序（含新单 + 回退者）重排，保留的 PLANNED 工序作为已占用区间。
 *
 * <p>共享 protected helper 单一真相源在 {@link ErpApsSchedulingProcessor}（delete-after-extract facade）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpApsSchedulingInsertRushOrderProcessor {

    @Inject
    ErpApsSchedulingProcessor facade;

    public SchedulingResult insertRushOrder(Long operationOrderId, IServiceContext context) {
        ErpApsOperationOrder rush = facade.requireOperationOrder(operationOrderId, context);
        int buffer = AppConfig.var(ErpApsConfigs.CONFIG_BUFFER_MINUTES_BETWEEN_OPS,
                ErpApsConfigs.DEFAULT_BUFFER_MINUTES_BETWEEN_OPS);
        int maxWindowDays = AppConfig.var(ErpApsConfigs.CONFIG_MAX_RESCHEDULE_WINDOW_DAYS,
                ErpApsConfigs.DEFAULT_MAX_RESCHEDULE_WINDOW_DAYS);

        LocalDateTime windowStart = rush.getEarliestStartDateT() != null
                ? rush.getEarliestStartDateT().toLocalDateTime()
                : facade.currentDateTime();
        LocalDateTime deadline = rush.getLatestEndDateT() != null
                ? rush.getLatestEndDateT().toLocalDateTime()
                : windowStart.plusDays(maxWindowDays);
        LocalDateTime windowEnd = deadline.plusMinutes(buffer);

        // 窗口内同工作中心 PLANNED 工序
        List<ErpApsOperationOrder> inWindow = facade.loadPlannedInWindow(rush.getMachineId(), windowStart, windowEnd);

        // IN_PROGRESS 工序永不回退（硬约束）
        for (ErpApsOperationOrder op : inWindow) {
            if (ErpApsConstants.OP_STATUS_IN_PROGRESS.equals(op.getStatus())) {
                throw new NopException(ErpApsErrors.ERR_APS_OP_IN_PROGRESS_NOT_RESCHEDULABLE)
                        .param(ErpApsErrors.ARG_OP_CODE, op.getCode())
                        .param(ErpApsErrors.ARG_CURRENT_STATUS, op.getStatus());
            }
        }

        int rushPriority = rush.getPriority() == null ? 50 : rush.getPriority();
        java.util.List<ErpApsOperationOrder> toRevert = new java.util.ArrayList<>();
        java.util.List<ErpApsOperationOrder> frozen = new java.util.ArrayList<>();
        for (ErpApsOperationOrder op : inWindow) {
            int opPriority = op.getPriority() == null ? 50 : op.getPriority();
            // 优先级数字越大 = 优先级越低；低于新单（数字更大）的回退 DRAFT
            if (opPriority > rushPriority) {
                toRevert.add(op);
            } else {
                frozen.add(op);
            }
        }
        for (ErpApsOperationOrder op : toRevert) {
            // 释放该工序在原 PLANNED 时段占用的产能预留（P0-MA2-019），按 operationOrderId 定位，
            // 与 planned 字段是否已清空无关。释放先于 saveOrUpdateEntity 落库，确保回退原子可见。
            facade.releaseReservationsByOrder(op.getId());
            op.setStatus(ErpApsConstants.OP_STATUS_DRAFT);
            op.setPlannedStartDateT(null);
            op.setPlannedEndDateT(null);
            facade.opOrderDao().saveOrUpdateEntity(op);
        }

        // 窗口内 DRAFT 工序（含新单 + 回退者）重排
        java.util.List<ErpApsOperationOrder> toSchedule = new java.util.ArrayList<>();
        toSchedule.add(rush);
        toSchedule.addAll(toRevert);
        // 新单若仍 DRAFT 则纳入；置 DRAFT 统一处理
        if (!ErpApsConstants.OP_STATUS_DRAFT.equals(rush.getStatus())) {
            rush.setStatus(ErpApsConstants.OP_STATUS_DRAFT);
        }

        List<ErpApsConstraint> maintenance = facade.loadMaintenanceConstraintsByMachine(rush.getMachineId(), windowStart, windowEnd);
        ErpApsSchedulingEngine engine = facade.newEngine(buffer, windowStart, windowEnd);
        SchedulingResult result = engine.scheduleForward(toSchedule, maintenance, frozen, windowStart);
        facade.persist(toSchedule, result);
        return result;
    }
}
