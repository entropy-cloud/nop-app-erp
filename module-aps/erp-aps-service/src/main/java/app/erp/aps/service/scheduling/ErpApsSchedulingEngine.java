package app.erp.aps.service.scheduling;

import app.erp.aps.biz.SchedulingResult;
import app.erp.aps.dao.entity.ErpApsConstraint;
import app.erp.aps.dao.entity.ErpApsOperationOrder;
import app.erp.aps.dao.entity.ErpApsOpRouting;
import app.erp.aps.service.ErpApsConstants;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
 * {@link ErpApsConstraint}（MAINTENANCE 类型）+ 替代路由 {@link ErpApsOpRouting} 列表（RC-R1.87）+ 配置参数，
 * 输出 {@link SchedulingResult}，并直接写回每个工序实体的 {@code plannedStartDateT/plannedEndDateT/totalDuration/status}。
 *
 * <p>本期范围（与 Non-Goals 一致）：
 * <ul>
 *   <li>capacity=1 单工位（{@code scheduling.md §5.2}）。</li>
 *   <li>仅消费 MAINTENANCE 类型约束；PERSONNEL/TOOL 归 follow-up。</li>
 *   <li>贪心前向/后向填充，非 ILP/CP 优化求解。</li>
 * </ul>
 *
 * <p><b>替代路由选择（RC-R1.87 / UC-APS-06，{@code alternative-routing.md §二 SELECT_ROUTING}）</b>：
 * 工序与 {@link ErpApsOpRouting} 的关联键 = {@code isDefault=true 且 machineId=工序主工作中心} 的默认行
 * → 该行 {@code operationId} 的全部启用行（priority ASC）为候选集（生效期 + 批量约束过滤），逐个尝试产能窗口，
 * 选中回写 {@code machineId/setupTime/runtimePerUnit/selectedRoutingId/routingSelectionReason}（时间差计入
 * duration）；全不可用标 {@code UNSCHEDULABLE}。无路由配置（或 {@code manualOverride=true} 跳过自动选择）的
 * 工序保持既有 {@code op.machineId} 单候选零行为变化。重复排产时先剥离上次选中路由的时间差再叠加新值（幂等）。
 *
 * <p>前向排产排序键：(priority ASC, latestEndDateT ASC nullsLast, sequence ASC, workOrderId ASC)。
 * 后向排产排序键：(priority ASC, latestEndDateT ASC nullsLast, sequence DESC)。
 */
public class ErpApsSchedulingEngine {

    private final int bufferMinutesBetweenOps;
    private final LocalDateTime horizonStart;
    private final LocalDateTime horizonEnd;
    /** 路由生效期判定基准日（null=不过滤生效期，供测试/模拟路径）。 */
    private final LocalDate routingEffectiveDate;

    public ErpApsSchedulingEngine(int bufferMinutesBetweenOps, LocalDateTime horizonStart, LocalDateTime horizonEnd) {
        this(bufferMinutesBetweenOps, horizonStart, horizonEnd, null);
    }

    public ErpApsSchedulingEngine(int bufferMinutesBetweenOps, LocalDateTime horizonStart, LocalDateTime horizonEnd,
                                  LocalDate routingEffectiveDate) {
        this.bufferMinutesBetweenOps = Math.max(0, bufferMinutesBetweenOps);
        this.horizonStart = horizonStart;
        this.horizonEnd = horizonEnd;
        this.routingEffectiveDate = routingEffectiveDate;
    }

    /**
     * 前向排产：按优先级排序后，从每个工序的 earliestStartDateT 正向填充工作中心可用时段。
     * 同 WorkOrder 工序顺序约束：earliestStart ≥ 前工序 plannedEndDateT + buffer。
     */
    public SchedulingResult scheduleForward(List<ErpApsOperationOrder> orders,
                                            List<ErpApsConstraint> maintenanceConstraints,
                                            LocalDateTime defaultEarliestStart) {
        return scheduleForward(orders, maintenanceConstraints, null, null, defaultEarliestStart);
    }

    /**
     * 前向排产（区间重排重载）：额外把 {@code frozenPlanned}（窗口内保留不动的 PLANNED 工序）
     * 作为已占用区间预填入时间轴。供插单区间重排使用：仅对窗口内 DRAFT 工序重排，保留工序不被动。
     */
    public SchedulingResult scheduleForward(List<ErpApsOperationOrder> orders,
                                            List<ErpApsConstraint> maintenanceConstraints,
                                            List<ErpApsOperationOrder> frozenPlanned,
                                            LocalDateTime defaultEarliestStart) {
        return scheduleForward(orders, maintenanceConstraints, frozenPlanned, null, defaultEarliestStart);
    }

    /**
     * 前向排产（替代路由集成，RC-R1.87）：scheduling.md 步骤 3「获取工作中心」替换为 SELECT_ROUTING——
     * 每工序解析候选路由集（{@link #resolveCandidates}），逐候选尝试产能窗口，首个可用即选中。
     */
    public SchedulingResult scheduleForward(List<ErpApsOperationOrder> orders,
                                            List<ErpApsConstraint> maintenanceConstraints,
                                            List<ErpApsOperationOrder> frozenPlanned,
                                            List<ErpApsOpRouting> routings,
                                            LocalDateTime defaultEarliestStart) {
        SchedulingResult result = new SchedulingResult();
        Map<Long, WorkCenterTimeline> timelines = buildTimelines(maintenanceConstraints);
        seedFrozenPlanned(timelines, frozenPlanned);
        Map<Long, OpChain> chainByWorkOrder = new HashMap<>();

        List<ErpApsOperationOrder> sorted = sortByForward(orders);
        LocalDateTime floor = floor(defaultEarliestStart);

        for (ErpApsOperationOrder op : sorted) {
            List<RoutingCandidate> candidates = resolveCandidates(op, routings);
            if (candidates.isEmpty()) {
                op.setPlannedStartDateT(null);
                op.setPlannedEndDateT(null);
                op.setStatus(ErpApsConstants.OP_STATUS_UNSCHEDULABLE);
                result.addConflict(op.getId(), "NO_AVAILABLE_ROUTING",
                        "工序 " + (op.getCode() == null ? op.getId() : op.getCode())
                                + " 全部启用路由被过滤（生效期/批量约束），无候选路由");
                continue;
            }

            LocalDateTime earliest = effectiveEarliestStart(op, floor);
            earliest = applyPredecessorConstraint(op, earliest, chainByWorkOrder);

            RoutingCandidate chosen = null;
            LocalDateTime start = null;
            for (RoutingCandidate c : candidates) {
                WorkCenterTimeline tl = timelines.computeIfAbsent(c.machineId, WorkCenterTimeline::new);
                LocalDateTime s = tl.findFreeSlotForward(earliest, c.duration, horizonEnd);
                if (s != null) {
                    chosen = c;
                    start = s;
                    break;
                }
            }
            if (chosen == null) {
                if (isLegacyOnly(candidates)) {
                    op.setPlannedStartDateT(null);
                    op.setPlannedEndDateT(null);
                    op.setStatus(ErpApsConstants.OP_STATUS_DRAFT);
                    result.addConflict(op.getId(), "NO_AVAILABLE_SLOT",
                            "工作中心 " + op.getMachineId() + " 展望期内无连续可用时段");
                } else {
                    op.setPlannedStartDateT(null);
                    op.setPlannedEndDateT(null);
                    op.setStatus(ErpApsConstants.OP_STATUS_UNSCHEDULABLE);
                    result.addConflict(op.getId(), "NO_AVAILABLE_ROUTING",
                            "工序 " + (op.getCode() == null ? op.getId() : op.getCode())
                                    + " 全部候选路由（含主选" + (fallbackDisabled(op) ? "，降级已关闭" : "")
                                    + "）无连续可用时段");
                }
                continue;
            }
            LocalDateTime end = start.plusMinutes(chosen.duration);
            applySelection(op, chosen, timelines);
            op.setPlannedStartDateT(DateHelper.dateTimeToTimestamp(start));
            op.setPlannedEndDateT(DateHelper.dateTimeToTimestamp(end));
            op.setStatus(ErpApsConstants.OP_STATUS_PLANNED);
            timelines.computeIfAbsent(chosen.machineId, WorkCenterTimeline::new)
                    .addBusy(start, end, "op:" + (op.getCode() == null ? op.getId() : op.getCode()));
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
        return scheduleBackward(orders, maintenanceConstraints, null, defaultEarliestStart);
    }

    /**
     * 后向排产（替代路由集成，RC-R1.87）：候选路由逐个逆向尝试产能窗口。
     */
    public SchedulingResult scheduleBackward(List<ErpApsOperationOrder> orders,
                                             List<ErpApsConstraint> maintenanceConstraints,
                                             List<ErpApsOpRouting> routings,
                                             LocalDateTime defaultEarliestStart) {
        SchedulingResult result = new SchedulingResult();
        Map<Long, WorkCenterTimeline> timelines = buildTimelines(maintenanceConstraints);
        Map<Long, OpChain> chainByWorkOrder = new HashMap<>();

        List<ErpApsOperationOrder> sorted = sortByBackward(orders);
        LocalDateTime floor = floor(defaultEarliestStart);

        for (ErpApsOperationOrder op : sorted) {
            List<RoutingCandidate> candidates = resolveCandidates(op, routings);
            if (candidates.isEmpty()) {
                op.setStatus(ErpApsConstants.OP_STATUS_UNSCHEDULABLE);
                result.addConflict(op.getId(), "NO_AVAILABLE_ROUTING",
                        "工序 " + (op.getCode() == null ? op.getId() : op.getCode())
                                + " 全部启用路由被过滤（生效期/批量约束），无候选路由");
                continue;
            }

            LocalDateTime before = op.getLatestEndDateT() != null ? op.getLatestEndDateT().toLocalDateTime() : horizonEnd;
            if (before == null) {
                op.setStatus(ErpApsConstants.OP_STATUS_DRAFT);
                result.addConflict(op.getId(), "NO_DEADLINE",
                        "工序未配置 latestEndDateT 且排产方案未限定 horizonEnd，后向排产无终点");
                continue;
            }
            // 后续工序的倒推终点可能由其 successor 的 start - buffer 给出
            before = applySuccessorConstraint(op, before, chainByWorkOrder);

            RoutingCandidate chosen = null;
            LocalDateTime start = null;
            for (RoutingCandidate c : candidates) {
                WorkCenterTimeline tl = timelines.computeIfAbsent(c.machineId, WorkCenterTimeline::new);
                LocalDateTime s = tl.findFreeSlotBackward(before, c.duration);
                if (s != null) {
                    chosen = c;
                    start = s;
                    break;
                }
            }
            if (chosen == null) {
                if (isLegacyOnly(candidates)) {
                    op.setStatus(ErpApsConstants.OP_STATUS_DRAFT);
                    result.addConflict(op.getId(), "NO_AVAILABLE_SLOT",
                            "工作中心 " + op.getMachineId() + " 终点前无连续可用时段");
                } else {
                    op.setStatus(ErpApsConstants.OP_STATUS_UNSCHEDULABLE);
                    result.addConflict(op.getId(), "NO_AVAILABLE_ROUTING",
                            "工序 " + (op.getCode() == null ? op.getId() : op.getCode()) + " 全部候选路由终点前无连续可用时段");
                }
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
            LocalDateTime end = start.plusMinutes(chosen.duration);
            applySelection(op, chosen, timelines);
            op.setPlannedStartDateT(DateHelper.dateTimeToTimestamp(start));
            op.setPlannedEndDateT(DateHelper.dateTimeToTimestamp(end));
            op.setStatus(ErpApsConstants.OP_STATUS_PLANNED);
            timelines.computeIfAbsent(chosen.machineId, WorkCenterTimeline::new)
                    .addBusy(start, end, "op:" + (op.getCode() == null ? op.getId() : op.getCode()));
            recordChainBackward(chainByWorkOrder, op, start);
            result.addScheduled(op.getId());
        }
        return result;
    }

    // ---------- 替代路由选择（RC-R1.87，alternative-routing.md §二） ----------

    /**
     * 解析工序的候选路由集（{@code alternative-routing.md SELECT_ROUTING 步骤 1-2}）。
     *
     * <p>关联键：{@code isDefault=true && machineId=op.machineId} 的默认行定位 operationId，该 operationId
     * 的全部启用行（priority ASC，生效期 + 批量约束过滤）为候选。无路由配置或 {@code manualOverride=true}
     * 时返回仅含工序主工作中心的传统单候选（零行为变化）。
     *
     * @return 候选列表（可能为空 = 全部路由被过滤，调用方标 UNSCHEDULABLE）
     */
    List<RoutingCandidate> resolveCandidates(ErpApsOperationOrder op, List<ErpApsOpRouting> routings) {
        List<RoutingCandidate> candidates = new ArrayList<>();
        if (routings == null || routings.isEmpty() || Boolean.TRUE.equals(op.getManualOverride())) {
            candidates.add(legacyCandidate(op));
            return candidates;
        }
        List<ErpApsOpRouting> effective = filterEffective(routings);
        List<ErpApsOpRouting> defaults = new ArrayList<>();
        for (ErpApsOpRouting r : effective) {
            if (Boolean.TRUE.equals(r.getIsDefault()) && op.getMachineId() != null
                    && op.getMachineId().equals(r.getMachineId())) {
                defaults.add(r);
            }
        }
        if (defaults.isEmpty()) {
            candidates.add(legacyCandidate(op));
            return candidates;
        }
        defaults.sort(routingOrder());
        boolean allowFallback = !Boolean.FALSE.equals(op.getAllowFallback());
        RoutingDeltas base = stripPreviousDelta(op, routings);

        for (ErpApsOpRouting def : defaults) {
            List<ErpApsOpRouting> set = new ArrayList<>();
            for (ErpApsOpRouting r : effective) {
                if (def.getOperationId() != null && def.getOperationId().equals(r.getOperationId())) {
                    set.add(r);
                }
            }
            set.sort(routingOrder());
            boolean defaultBatchExcluded = false;
            for (ErpApsOpRouting row : set) {
                boolean isDefaultRow = Boolean.TRUE.equals(row.getIsDefault());
                if (!isDefaultRow && !allowFallback) {
                    continue; // D3：allowFallback=false → 仅尝试主选路由
                }
                if (!batchOk(row, op.getQty())) {
                    if (isDefaultRow) {
                        defaultBatchExcluded = true;
                    }
                    continue;
                }
                candidates.add(routingCandidate(row, def, base, op.getQty(), defaultBatchExcluded));
            }
        }
        return candidates;
    }

    private RoutingCandidate legacyCandidate(ErpApsOperationOrder op) {
        RoutingCandidate c = new RoutingCandidate();
        c.machineId = op.getMachineId();
        c.row = null;
        c.duration = computeDuration(op);
        return c;
    }

    private RoutingCandidate routingCandidate(ErpApsOpRouting row, ErpApsOpRouting defaultRow,
                                              RoutingDeltas base, BigDecimal qty, boolean defaultBatchExcluded) {
        RoutingCandidate c = new RoutingCandidate();
        c.machineId = row.getMachineId();
        c.row = row;
        c.defaultRow = defaultRow;
        c.defaultBatchExcluded = defaultBatchExcluded;
        BigDecimal setup = base.setup.add(deltaOrZero(row.getSetupTimeDelta()));
        BigDecimal per = base.perUnit.add(deltaOrZero(row.getRuntimePerUnitDelta()));
        BigDecimal q = qty == null ? BigDecimal.ZERO : qty;
        long d = setup.add(per.multiply(q)).setScale(0, RoundingMode.CEILING).longValueExact();
        c.duration = Math.max(1L, d);
        c.effectiveSetup = setup;
        c.effectivePerUnit = per;
        return c;
    }

    /** 选中候选回写工序：machineId/setupTime/runtimePerUnit/selectedRoutingId/routingSelectionReason/totalDuration。 */
    private void applySelection(ErpApsOperationOrder op, RoutingCandidate chosen,
                                Map<Long, WorkCenterTimeline> timelines) {
        op.setTotalDuration(BigDecimal.valueOf(chosen.duration));
        if (chosen.row == null) {
            return; // 传统单候选：不触碰路由字段（零行为变化）
        }
        op.setMachineId(chosen.machineId);
        op.setSetupTime(chosen.effectiveSetup);
        op.setRuntimePerUnit(chosen.effectivePerUnit);
        op.setSelectedRoutingId(chosen.row.getId());
        op.setRoutingSelectionReason(selectionReason(chosen, timelines));
    }

    private String selectionReason(RoutingCandidate chosen, Map<Long, WorkCenterTimeline> timelines) {
        if (Boolean.TRUE.equals(chosen.row.getIsDefault())) {
            return ErpApsConstants.ROUTING_REASON_DEFAULT;
        }
        if (chosen.defaultBatchExcluded) {
            return ErpApsConstants.ROUTING_REASON_BATCH_CONSTRAINT;
        }
        if (chosen.defaultRow != null) {
            WorkCenterTimeline defTl = timelines.get(chosen.defaultRow.getMachineId());
            if (defTl != null && blockedOnlyByMaintenance(defTl)) {
                return ErpApsConstants.ROUTING_REASON_PRIMARY_DOWN;
            }
        }
        return ErpApsConstants.ROUTING_REASON_PRIMARY_OVERBOOKED;
    }

    /** 默认工作中心时间轴上的繁忙区间是否全部为维护停机（区分 PRIMARY_DOWN 与 PRIMARY_OVERBOOKED）。 */
    private boolean blockedOnlyByMaintenance(WorkCenterTimeline timeline) {
        for (WorkCenterTimeline.Interval iv : timeline.getBusy()) {
            String reason = iv.getReason();
            if (reason == null || !reason.startsWith("maintenance")) {
                return false;
            }
        }
        return !timeline.getBusy().isEmpty();
    }

    /**
     * 剥离上次选中路由的时间差，得到工序标准值（重复排产幂等基线）。
     * 上次选中行已被删除时无法恢复标准值，按当前值处理（等效差值 0）。
     */
    private RoutingDeltas stripPreviousDelta(ErpApsOperationOrder op, List<ErpApsOpRouting> routings) {
        RoutingDeltas base = new RoutingDeltas();
        base.setup = op.getSetupTime() == null ? BigDecimal.ZERO : op.getSetupTime();
        base.perUnit = op.getRuntimePerUnit() == null ? BigDecimal.ZERO : op.getRuntimePerUnit();
        if (op.getSelectedRoutingId() == null) {
            return base;
        }
        for (ErpApsOpRouting r : routings) {
            if (op.getSelectedRoutingId().equals(r.getId())) {
                base.setup = base.setup.subtract(deltaOrZero(r.getSetupTimeDelta()));
                base.perUnit = base.perUnit.subtract(deltaOrZero(r.getRuntimePerUnitDelta()));
                break;
            }
        }
        return base;
    }

    private static BigDecimal deltaOrZero(BigDecimal delta) {
        return delta == null ? BigDecimal.ZERO : delta;
    }

    private List<ErpApsOpRouting> filterEffective(List<ErpApsOpRouting> routings) {
        List<ErpApsOpRouting> effective = new ArrayList<>();
        for (ErpApsOpRouting r : routings) {
            if (isEffective(r)) {
                effective.add(r);
            }
        }
        return effective;
    }

    private boolean isEffective(ErpApsOpRouting r) {
        if (routingEffectiveDate == null) {
            return true;
        }
        if (r.getEffectiveFrom() != null && routingEffectiveDate.isBefore(r.getEffectiveFrom())) {
            return false;
        }
        return r.getEffectiveTo() == null || !routingEffectiveDate.isAfter(r.getEffectiveTo());
    }

    private static boolean batchOk(ErpApsOpRouting row, BigDecimal qty) {
        if (qty == null) {
            return true;
        }
        if (row.getMinBatchQty() != null && qty.compareTo(row.getMinBatchQty()) < 0) {
            return false;
        }
        return row.getMaxBatchQty() == null || qty.compareTo(row.getMaxBatchQty()) <= 0;
    }

    private static Comparator<ErpApsOpRouting> routingOrder() {
        return Comparator
                .comparing(ErpApsOpRouting::getPriority, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ErpApsOpRouting::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private static boolean isLegacyOnly(List<RoutingCandidate> candidates) {
        return candidates.size() == 1 && candidates.get(0).row == null;
    }

    private static boolean fallbackDisabled(ErpApsOperationOrder op) {
        return Boolean.FALSE.equals(op.getAllowFallback());
    }

    /** 路由候选（machineId × 路由行 × 时间差计入后的 duration）。row=null 表示传统单候选。 */
    private static final class RoutingCandidate {
        Long machineId;
        ErpApsOpRouting row;
        ErpApsOpRouting defaultRow;
        boolean defaultBatchExcluded;
        long duration;
        BigDecimal effectiveSetup;
        BigDecimal effectivePerUnit;
    }

    /** 工序标准工时（剥离上次选中路由时间差后的基线）。 */
    private static final class RoutingDeltas {
        BigDecimal setup;
        BigDecimal perUnit;
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
