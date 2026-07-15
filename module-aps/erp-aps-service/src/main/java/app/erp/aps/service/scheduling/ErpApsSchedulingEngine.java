package app.erp.aps.service.scheduling;

import app.erp.aps.biz.SchedulingResult;
import app.erp.aps.dao.entity.ErpApsConstraint;
import app.erp.aps.dao.entity.ErpApsOperationOrder;
import app.erp.aps.service.ErpApsConstants;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.DateHelper;

/**
 * APS 有限产能排产引擎（贪心启发式，{@code scheduling.md §二/三/四/五}）。
 *
 * <p>纯算法类，无 Spring/DB 依赖：输入待排 {@link ErpApsOperationOrder} 列表 + 维护约束
 * {@link ErpApsConstraint}（MAINTENANCE 类型）+ 配置参数，输出 {@link SchedulingResult}，
 * 并直接写回每个工序实体的 {@code plannedStartDateT/plannedEndDateT/totalDuration/status}。
 *
 * <p>本期范围（与 Non-Goals 一致）：
 * <ul>
 *   <li>capacity=1 单工位（{@code scheduling.md §5.2}）。</li>
 *   <li>仅消费 MAINTENANCE 类型约束；PERSONNEL/TOOL 归 follow-up。</li>
 *   <li>贪心前向/后向填充，非 ILP/CP 优化求解。</li>
 * </ul>
 *
 * <p>前向排产排序键：(priority ASC, latestEndDateT ASC nullsLast, sequence ASC, workOrderId ASC)。
 * 后向排产排序键：(priority ASC, latestEndDateT ASC nullsLast, sequence DESC)。
 */
public class ErpApsSchedulingEngine {

    private final int bufferMinutesBetweenOps;
    private final LocalDateTime horizonStart;
    private final LocalDateTime horizonEnd;

    public ErpApsSchedulingEngine(int bufferMinutesBetweenOps, LocalDateTime horizonStart, LocalDateTime horizonEnd) {
        this.bufferMinutesBetweenOps = Math.max(0, bufferMinutesBetweenOps);
        this.horizonStart = horizonStart;
        this.horizonEnd = horizonEnd;
    }

    /**
     * 前向排产：按优先级排序后，从每个工序的 earliestStartDateT 正向填充工作中心可用时段。
     * 同 WorkOrder 工序顺序约束：earliestStart ≥ 前工序 plannedEndDateT + buffer。
     */
    public SchedulingResult scheduleForward(List<ErpApsOperationOrder> orders,
                                            List<ErpApsConstraint> maintenanceConstraints,
                                            LocalDateTime defaultEarliestStart) {
        return scheduleForward(orders, maintenanceConstraints, null, defaultEarliestStart);
    }

    /**
     * 前向排产（区间重排重载）：额外把 {@code frozenPlanned}（窗口内保留不动的 PLANNED 工序）
     * 作为已占用区间预填入时间轴。供插单区间重排使用：仅对窗口内 DRAFT 工序重排，保留工序不被动。
     */
    public SchedulingResult scheduleForward(List<ErpApsOperationOrder> orders,
                                            List<ErpApsConstraint> maintenanceConstraints,
                                            List<ErpApsOperationOrder> frozenPlanned,
                                            LocalDateTime defaultEarliestStart) {
        SchedulingResult result = new SchedulingResult();
        Map<Long, WorkCenterTimeline> timelines = buildTimelines(maintenanceConstraints);
        seedFrozenPlanned(timelines, frozenPlanned);
        Map<Long, OpChain> chainByWorkOrder = new HashMap<>();

        List<ErpApsOperationOrder> sorted = sortByForward(orders);
        LocalDateTime floor = floor(defaultEarliestStart);

        for (ErpApsOperationOrder op : sorted) {
            long duration = computeDuration(op);
            op.setTotalDuration(BigDecimal.valueOf(duration));

            WorkCenterTimeline tl = timelines.computeIfAbsent(op.getMachineId(), WorkCenterTimeline::new);
            LocalDateTime earliest = effectiveEarliestStart(op, floor);
            earliest = applyPredecessorConstraint(op, earliest, chainByWorkOrder);

            LocalDateTime start = tl.findFreeSlotForward(earliest, duration, horizonEnd);
            if (start == null) {
                op.setPlannedStartDateT(null);
                op.setPlannedEndDateT(null);
                op.setStatus(ErpApsConstants.OP_STATUS_DRAFT);
                result.addConflict(op.getId(), "NO_AVAILABLE_SLOT",
                        "工作中心 " + op.getMachineId() + " 展望期内无连续可用时段");
                continue;
            }
            LocalDateTime end = start.plusMinutes(duration);
            op.setPlannedStartDateT(DateHelper.dateTimeToTimestamp(start));
            op.setPlannedEndDateT(DateHelper.dateTimeToTimestamp(end));
            op.setStatus(ErpApsConstants.OP_STATUS_PLANNED);
            tl.addBusy(start, end, "op:" + (op.getCode() == null ? op.getId() : op.getCode()));
            recordChain(chainByWorkOrder, op, end);
            result.addScheduled(op.getId());
        }
        return result;
    }

    /**
     * 后向排产：从 latestEndDateT（或 horizonEnd 兜底）逆向倒推每工序最晚开工。
     * 交期不可达（推算开工早于 earliestStartDateT）时标记冲突。
     */
    public SchedulingResult scheduleBackward(List<ErpApsOperationOrder> orders,
                                             List<ErpApsConstraint> maintenanceConstraints,
                                             LocalDateTime defaultEarliestStart) {
        SchedulingResult result = new SchedulingResult();
        Map<Long, WorkCenterTimeline> timelines = buildTimelines(maintenanceConstraints);
        Map<Long, OpChain> chainByWorkOrder = new HashMap<>();

        List<ErpApsOperationOrder> sorted = sortByBackward(orders);
        LocalDateTime floor = floor(defaultEarliestStart);

        for (ErpApsOperationOrder op : sorted) {
            long duration = computeDuration(op);
            op.setTotalDuration(BigDecimal.valueOf(duration));

            WorkCenterTimeline tl = timelines.computeIfAbsent(op.getMachineId(), WorkCenterTimeline::new);
            LocalDateTime before = op.getLatestEndDateT() != null ? op.getLatestEndDateT().toLocalDateTime() : horizonEnd;
            if (before == null) {
                op.setStatus(ErpApsConstants.OP_STATUS_DRAFT);
                result.addConflict(op.getId(), "NO_DEADLINE",
                        "工序未配置 latestEndDateT 且排产方案未限定 horizonEnd，后向排产无终点");
                continue;
            }
            // 后续工序的倒推终点可能由其 successor 的 start - buffer 给出
            before = applySuccessorConstraint(op, before, chainByWorkOrder);

            LocalDateTime start = tl.findFreeSlotBackward(before, duration);
            if (start == null) {
                op.setStatus(ErpApsConstants.OP_STATUS_DRAFT);
                result.addConflict(op.getId(), "NO_AVAILABLE_SLOT",
                        "工作中心 " + op.getMachineId() + " 终点前无连续可用时段");
                continue;
            }
            // 交期可达性校验：推算开工不得早于 earliestStartDateT（物料/前序完工约束）
            LocalDateTime earliest = effectiveEarliestStart(op, floor);
            if (start.isBefore(earliest)) {
                op.setStatus(ErpApsConstants.OP_STATUS_DRAFT);
                result.setFeasible(false);
                result.addConflict(op.getId(), "DEADLINE_NOT_REACHABLE",
                        "推算开工 " + start + " 早于最早可开工 " + earliest);
                continue;
            }
            LocalDateTime end = start.plusMinutes(duration);
            op.setPlannedStartDateT(DateHelper.dateTimeToTimestamp(start));
            op.setPlannedEndDateT(DateHelper.dateTimeToTimestamp(end));
            op.setStatus(ErpApsConstants.OP_STATUS_PLANNED);
            tl.addBusy(start, end, "op:" + (op.getCode() == null ? op.getId() : op.getCode()));
            recordChainBackward(chainByWorkOrder, op, start);
            result.addScheduled(op.getId());
        }
        return result;
    }

    // ---------- 时间轴构建 ----------

    private Map<Long, WorkCenterTimeline> buildTimelines(List<ErpApsConstraint> maintenanceConstraints) {
        Map<Long, WorkCenterTimeline> timelines = new HashMap<>();
        if (maintenanceConstraints != null) {
            for (ErpApsConstraint c : maintenanceConstraints) {
                if (!ErpApsConstants.CONSTRAINT_TYPE_MAINTENANCE.equals(c.getConstraintType())) {
                    continue;
                }
                timelines.computeIfAbsent(c.getMachineId(), WorkCenterTimeline::new)
                        .addBusy(c.getStartTime().toLocalDateTime(), c.getEndTime().toLocalDateTime(), "maintenance");
            }
        }
        return timelines;
    }

    private void seedFrozenPlanned(Map<Long, WorkCenterTimeline> timelines,
                                   List<ErpApsOperationOrder> frozenPlanned) {
        if (frozenPlanned == null) {
            return;
        }
        for (ErpApsOperationOrder op : frozenPlanned) {
            if (op.getPlannedStartDateT() == null || op.getPlannedEndDateT() == null
                    || op.getMachineId() == null) {
                continue;
            }
            timelines.computeIfAbsent(op.getMachineId(), WorkCenterTimeline::new)
                    .addBusy(op.getPlannedStartDateT().toLocalDateTime(), op.getPlannedEndDateT().toLocalDateTime(),
                            "frozen:" + (op.getCode() == null ? op.getId() : op.getCode()));
        }
    }

    // ---------- 排序 ----------

    private List<ErpApsOperationOrder> sortByForward(List<ErpApsOperationOrder> orders) {
        List<ErpApsOperationOrder> copy = new ArrayList<>(orders);
        copy.sort(Comparator
                .comparingInt((ErpApsOperationOrder o) -> priorityOr(o, 50))
                .thenComparing(ErpApsOperationOrder::getLatestEndDateT,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingInt(o -> sequenceOr(o, Integer.MAX_VALUE))
                .thenComparing(ErpApsOperationOrder::getWorkOrderId,
                        Comparator.nullsLast(Comparator.naturalOrder())));
        return copy;
    }

    private List<ErpApsOperationOrder> sortByBackward(List<ErpApsOperationOrder> orders) {
        List<ErpApsOperationOrder> copy = new ArrayList<>(orders);
        copy.sort(Comparator
                .comparingInt((ErpApsOperationOrder o) -> priorityOr(o, 50))
                .thenComparing(ErpApsOperationOrder::getLatestEndDateT,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ErpApsOperationOrder::getSequence,
                        Comparator.nullsLast(Comparator.reverseOrder())));
        return copy;
    }

    // ---------- 工序链约束 ----------

    private LocalDateTime applyPredecessorConstraint(ErpApsOperationOrder op, LocalDateTime earliest,
                                                     Map<Long, OpChain> chainByWorkOrder) {
        OpChain chain = chainByWorkOrder.get(op.getWorkOrderId());
        if (chain == null || chain.lastSequence == null || chain.lastEnd == null) {
            return earliest;
        }
        // 仅当当前工序序号大于链上最后序号时施加前序完工约束（保证 sequence 单调）
        if (op.getSequence() != null && op.getSequence() > chain.lastSequence) {
            LocalDateTime predEndWithBuffer = chain.lastEnd.plusMinutes(bufferMinutesBetweenOps);
            return earliest.isBefore(predEndWithBuffer) ? predEndWithBuffer : earliest;
        }
        return earliest;
    }

    private LocalDateTime applySuccessorConstraint(ErpApsOperationOrder op, LocalDateTime before,
                                                   Map<Long, OpChain> chainByWorkOrder) {
        OpChain chain = chainByWorkOrder.get(op.getWorkOrderId());
        if (chain == null || chain.lastSequence == null || chain.lastStart == null) {
            return before;
        }
        // 当前工序序号小于链上最后序号（后向：先排后序工序）→ 终点 ≤ 后序开工 − buffer
        if (op.getSequence() != null && op.getSequence() < chain.lastSequence) {
            LocalDateTime succStartWithBuffer = chain.lastStart.minusMinutes(bufferMinutesBetweenOps);
            return before.isAfter(succStartWithBuffer) ? succStartWithBuffer : before;
        }
        return before;
    }

    private void recordChain(Map<Long, OpChain> chainByWorkOrder, ErpApsOperationOrder op, LocalDateTime end) {
        OpChain chain = chainByWorkOrder.computeIfAbsent(op.getWorkOrderId(), k -> new OpChain());
        if (chain.lastSequence == null || (op.getSequence() != null && op.getSequence() > chain.lastSequence)) {
            chain.lastSequence = op.getSequence();
            chain.lastEnd = end;
        }
    }

    private void recordChainBackward(Map<Long, OpChain> chainByWorkOrder, ErpApsOperationOrder op, LocalDateTime start) {
        OpChain chain = chainByWorkOrder.computeIfAbsent(op.getWorkOrderId(), k -> new OpChain());
        if (chain.lastSequence == null || (op.getSequence() != null && op.getSequence() < chain.lastSequence)) {
            chain.lastSequence = op.getSequence();
            chain.lastStart = start;
        }
    }

    // ---------- 工具 ----------

    public long computeDuration(ErpApsOperationOrder op) {
        BigDecimal setup = op.getSetupTime() == null ? BigDecimal.ZERO : op.getSetupTime();
        BigDecimal per = op.getRuntimePerUnit() == null ? BigDecimal.ZERO : op.getRuntimePerUnit();
        BigDecimal qty = op.getQty() == null ? BigDecimal.ZERO : op.getQty();
        long d = setup.add(per.multiply(qty)).setScale(0, java.math.RoundingMode.CEILING).longValueExact();
        return Math.max(1L, d);
    }

    private LocalDateTime effectiveEarliestStart(ErpApsOperationOrder op, LocalDateTime floor) {
        LocalDateTime base = op.getEarliestStartDateT() == null ? null : op.getEarliestStartDateT().toLocalDateTime();
        if (base == null) {
            base = op.getPlannedStartDateT() == null ? null : op.getPlannedStartDateT().toLocalDateTime();
        }
        if (base == null) {
            base = floor;
        }
        if (horizonStart != null && base.isBefore(horizonStart)) {
            return horizonStart;
        }
        return base;
    }

    private LocalDateTime floor(LocalDateTime defaultEarliestStart) {
        LocalDateTime f = defaultEarliestStart;
        if (f == null) {
            f = horizonStart;
        }
        if (f == null) {
            f = CoreMetrics.currentDateTime();
        }
        return f;
    }

    private static int priorityOr(ErpApsOperationOrder o, int dflt) {
        return o.getPriority() == null ? dflt : o.getPriority();
    }

    private static int sequenceOr(ErpApsOperationOrder o, int dflt) {
        return o.getSequence() == null ? dflt : o.getSequence();
    }

    /** 同 WorkOrder 工序链的游标（前向记 lastEnd，后向记 lastStart）。 */
    private static final class OpChain {
        Integer lastSequence;
        LocalDateTime lastEnd;
        LocalDateTime lastStart;
    }

    /**
     * 暴露内部时间轴（供 ATP/CTP 模拟在现有排产方案上叠加影子工序）。
     * 返回的 Timeline 可被继续 addBusy 而不影响本引擎已记录的状态（按 machineId 复制繁忙区间）。
     */
    public Map<Long, WorkCenterTimeline> snapshotTimelines(List<ErpApsConstraint> maintenanceConstraints,
                                                           List<ErpApsOperationOrder> plannedOrders) {
        Map<Long, WorkCenterTimeline> timelines = buildTimelines(maintenanceConstraints);
        if (plannedOrders != null) {
            for (ErpApsOperationOrder op : plannedOrders) {
                if (op.getPlannedStartDateT() == null || op.getPlannedEndDateT() == null) {
                    continue;
                }
                timelines.computeIfAbsent(op.getMachineId(), WorkCenterTimeline::new)
                        .addBusy(op.getPlannedStartDateT().toLocalDateTime(), op.getPlannedEndDateT().toLocalDateTime(),
                                "op:" + (op.getCode() == null ? op.getId() : op.getCode()));
            }
        }
        return timelines;
    }

    /**
     * 在给定时间轴上对单个工序做前向模拟排产（不修改工序状态字段以外的引擎内部状态），
     * 供 CTP 影子模拟使用。返回排定起点，或 {@code null} 表示无可用时段。
     */
    public LocalDateTime simulateForward(WorkCenterTimeline timeline, ErpApsOperationOrder shadow,
                                         LocalDateTime earliestStart) {
        long duration = computeDuration(shadow);
        shadow.setTotalDuration(BigDecimal.valueOf(duration));
        return timeline.findFreeSlotForward(earliestStart, duration, horizonEnd);
    }

    public LocalDateTime getHorizonEnd() {
        return horizonEnd;
    }
}
