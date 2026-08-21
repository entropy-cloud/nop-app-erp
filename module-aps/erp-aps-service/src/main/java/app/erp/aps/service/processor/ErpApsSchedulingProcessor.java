package app.erp.aps.service.processor;

import app.erp.aps.biz.SchedulingResult;
import app.erp.aps.dao.entity.ErpApsCapacityReservation;
import app.erp.aps.dao.entity.ErpApsConstraint;
import app.erp.aps.dao.entity.ErpApsOperationOrder;
import app.erp.aps.dao.entity.ErpApsOpRouting;
import app.erp.aps.dao.entity.ErpApsSchedule;
import app.erp.aps.service.ErpApsConfigs;
import app.erp.aps.service.ErpApsConstants;
import app.erp.aps.service.ErpApsErrors;
import app.erp.aps.service.scheduling.ErpApsSchedulingEngine;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.dao.exceptions.JdbcException;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.gt;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.le;
import static io.nop.api.core.beans.FilterBeans.lt;

/**
 * APS 排产编排 Processor facade（{@code processor-extension-pattern.md} 两层结构）。
 *
 * <p>R6.7 delete-after-extract：{@code scheduleForward}/{@code scheduleBackward}/{@code insertRushOrder}
 * 三个 D-mutation 已拆为独立
 * {@link ErpApsSchedulingScheduleForwardProcessor}/{@link ErpApsSchedulingScheduleBackwardProcessor}/
 * {@link ErpApsSchedulingInsertRushOrderProcessor}。本类保留为共享 protected helper 单一真相源
 * （拉取排产方案 + 待排工序（DRAFT）+ 维护约束（MAINTENANCE）、调用 {@link ErpApsSchedulingEngine}
 * 的编排、写回 {@code plannedStartDateT/plannedEndDateT/status}）。
 *
 * <p>每个步骤方法为 {@code protected}，下游可逐个覆盖（产品化拓扑可变场景的最小化形态：
 * 排产拓扑稳定，但允许覆盖数据加载与结果处理）。
 */
public class ErpApsSchedulingProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IOrmTemplate ormTemplate;

    // ---------- 编排 ----------

    protected SchedulingResult run(ErpApsSchedule schedule, String mode, IServiceContext context) {
        List<ErpApsOperationOrder> pending = loadPendingOrders(schedule);
        List<ErpApsConstraint> maintenance = loadMaintenanceConstraints(schedule);
        // RC-R1.87：启用路由全集（引擎按默认行 machineId 关联 + 生效期/批量过滤）
        List<ErpApsOpRouting> routings = loadEnabledRoutings();
        int buffer = AppConfig.var(ErpApsConfigs.CONFIG_BUFFER_MINUTES_BETWEEN_OPS,
                ErpApsConfigs.DEFAULT_BUFFER_MINUTES_BETWEEN_OPS);
        LocalDateTime horizonStart = schedule.getHorizonStart() == null ? null : schedule.getHorizonStart().toLocalDateTime();
        LocalDateTime horizonEnd = schedule.getHorizonEnd() == null ? null : schedule.getHorizonEnd().toLocalDateTime();

        ErpApsSchedulingEngine engine = newEngine(buffer, horizonStart, horizonEnd, CoreMetrics.today());
        SchedulingResult result = ErpApsConstants.SCHEDULING_MODE_BACKWARD.equals(mode)
                ? engine.scheduleBackward(pending, maintenance, routings, horizonStart)
                : engine.scheduleForward(pending, maintenance, null, routings, horizonStart);

        persist(pending, result);
        return result;
    }

    // ---------- step：数据加载（protected，下游可覆盖） ----------

    protected List<ErpApsOperationOrder> loadPendingOrders(ErpApsSchedule schedule) {
        QueryBean q = new QueryBean();
        // UNSCHEDULABLE 与 DRAFT 同池重试（RC-R1.87 自愈语义：路由/停机恢复后重排自动翻回 PLANNED）
        q.addFilter(in("status", java.util.Arrays.asList(
                ErpApsConstants.OP_STATUS_DRAFT, ErpApsConstants.OP_STATUS_UNSCHEDULABLE)));
        if (schedule.getHorizonStart() != null) {
            q.addFilter(ge("earliestStartDateT", schedule.getHorizonStart()));
        }
        if (schedule.getHorizonEnd() != null) {
            q.addFilter(le("earliestStartDateT", schedule.getHorizonEnd()));
        }
        return opOrderDao().findAllByQuery(q);
    }

    /** 启用路由全集（同域实体，IDaoProvider 直访；引擎负责默认行关联 + 生效期/批量过滤）。 */
    protected List<ErpApsOpRouting> loadEnabledRoutings() {
        QueryBean q = new QueryBean();
        q.addFilter(eq("isEnabled", Boolean.TRUE));
        return opRoutingDao().findAllByQuery(q);
    }

    protected List<ErpApsConstraint> loadMaintenanceConstraints(ErpApsSchedule schedule) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("constraintType", ErpApsConstants.CONSTRAINT_TYPE_MAINTENANCE));
        if (schedule.getHorizonStart() != null && schedule.getHorizonEnd() != null) {
            q.addFilter(ge("endTime", schedule.getHorizonStart()));
            q.addFilter(le("startTime", schedule.getHorizonEnd()));
        }
        return constraintDao().findAllByQuery(q);
    }

    /** 窗口内同工作中心 PLANNED/IN_PROGRESS 工序（区间重叠：plannedEnd > windowStart 且 plannedStart < windowEnd）。
     * IN_PROGRESS 一并载入以触发不可回退硬约束校验。 */
    protected List<ErpApsOperationOrder> loadPlannedInWindow(String machineId,
                                                             LocalDateTime windowStart,
                                                             LocalDateTime windowEnd) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("machineId", machineId));
        q.addFilter(in("status",
                java.util.Arrays.asList(ErpApsConstants.OP_STATUS_PLANNED, ErpApsConstants.OP_STATUS_IN_PROGRESS)));
        q.addFilter(ge("plannedEndDateT", windowStart));
        q.addFilter(le("plannedStartDateT", windowEnd));
        return opOrderDao().findAllByQuery(q);
    }

    protected List<ErpApsConstraint> loadMaintenanceConstraintsByMachine(String machineId,
                                                                         LocalDateTime windowStart,
                                                                         LocalDateTime windowEnd) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("constraintType", ErpApsConstants.CONSTRAINT_TYPE_MAINTENANCE));
        q.addFilter(eq("machineId", machineId));
        q.addFilter(ge("endTime", windowStart));
        q.addFilter(le("startTime", windowEnd));
        return constraintDao().findAllByQuery(q);
    }

    // ---------- step：结果写回 ----------

    protected void persist(List<ErpApsOperationOrder> orders, SchedulingResult result) {
        IEntityDao<ErpApsOperationOrder> dao = opOrderDao();
        for (ErpApsOperationOrder op : orders) {
            // P0-MA2-019：仅 PLANNED 且带有效时段/工作中心的工序才占产能预留。
            // 引擎标记为 DRAFT（无可用时段/交期不可达）的工序不占预留，直接落库。
            if (isPlannedScheduled(op)) {
                acquireReservation(op);
            }
            // 引擎已直接写回实体字段，此处统一落库；未排定的保持 DRAFT
            dao.saveOrUpdateEntity(op);
        }
    }

    private boolean isPlannedScheduled(ErpApsOperationOrder op) {
        return ErpApsConstants.OP_STATUS_PLANNED.equals(op.getStatus())
                && op.getMachineId() != null
                && op.getPlannedStartDateT() != null
                && op.getPlannedEndDateT() != null;
    }

    /**
     * 获取（INSERT）一条产能预留（P0-MA2-019）。
     *
     * <p>两段防护：
     * <ol>
     *   <li>重叠 pre-check：查询 {@code erp_aps_capacity_reservation} 是否已有重叠时段。
     *       命中即抛 {@link ErpApsErrors#ERR_APS_CAPACITY_CONFLICT}（友好错误，覆盖单会话/已提交并发场景）。</li>
     *   <li>UK {@code UK_APS_CAPACITY_RESERVATION_SLOT (machineId, plannedStartT, plannedEndT)} 兜底：
     *       两并发 scheduleForward 同时通过 pre-check（TOCTOU 窗口），DB 唯一约束保证只一行落地；
     *       兜底触发时翻译为 {@link ErpApsErrors#ERR_APS_CAPACITY_CONFLICT}。</li>
     * </ol>
     *
     * <p>每次 INSERT 后立即 {@code flushSession}：使 TOCTOU UK 违例在本方法边界内抛出并翻译为业务错误码，
     * 而非在 {@code @BizMutation} 事务提交时以 {@code nop.err.dao.sql.data-integrity-violation} 透出到调用方。
     */
    protected void acquireReservation(ErpApsOperationOrder op) {
        Timestamp start = op.getPlannedStartDateT();
        Timestamp end = op.getPlannedEndDateT();
        if (hasOverlappingReservation(op.getMachineId(), start, end)) {
            throw new NopException(ErpApsErrors.ERR_APS_CAPACITY_CONFLICT)
                    .param(ErpApsErrors.ARG_MACHINE_ID, op.getMachineId())
                    .param(ErpApsErrors.ARG_OP_CODE, op.getCode());
        }
        ErpApsCapacityReservation r = capacityReservationDao().newEntity();
        r.setMachineId(op.getMachineId());
        r.setPlannedStartT(start);
        r.setPlannedEndT(end);
        r.setOperationOrderId(op.getId());
        r.setOrgId(op.getOrgId());
        try {
            capacityReservationDao().saveEntity(r);
            ormTemplate.flushSession();
        } catch (JdbcException ex) {
            // TOCTOU 兜底：两并发排产同时通过 pre-check，DB UK 仅允许一行落地。
            throw new NopException(ErpApsErrors.ERR_APS_CAPACITY_CONFLICT, ex)
                    .param(ErpApsErrors.ARG_MACHINE_ID, op.getMachineId())
                    .param(ErpApsErrors.ARG_OP_CODE, op.getCode());
        }
    }

    /** 区间重叠判定（边界可相切）：existing.start &lt; newEnd AND existing.end &gt; newStart。 */
    protected boolean hasOverlappingReservation(String machineId, Timestamp newStart, Timestamp newEnd) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("machineId", machineId));
        q.addFilter(lt("plannedStartT", newEnd));
        q.addFilter(gt("plannedEndT", newStart));
        return !capacityReservationDao().findAllByQuery(q).isEmpty();
    }

    /** 释放某工序的全部产能预留（PLANNED→DRAFT/CANCELLED 重排前置）。ErpApsCapacityReservation 不启用逻辑删除，硬删除。
     *  释放后立即 flushSession：确保后续 persist 的重叠 pre-check 查询能反映已删除行，避免幻读阻塞重排。 */
    protected void releaseReservationsByOrder(String operationOrderId) {
        if (operationOrderId == null) {
            return;
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("operationOrderId", operationOrderId));
        IEntityDao<ErpApsCapacityReservation> dao = capacityReservationDao();
        List<ErpApsCapacityReservation> toDelete = dao.findAllByQuery(q);
        if (toDelete.isEmpty()) {
            return;
        }
        for (ErpApsCapacityReservation r : toDelete) {
            dao.deleteEntity(r);
        }
        ormTemplate.flushSession();
    }

    // ---------- 查询/校验辅助 ----------

    protected ErpApsSchedule requireSchedule(String scheduleId, IServiceContext context) {
        ErpApsSchedule schedule = scheduleDao().getEntityById(scheduleId);
        if (schedule == null) {
            throw new NopException(ErpApsErrors.ERR_APS_SCHEDULE_NOT_FOUND)
                    .param(ErpApsErrors.ARG_SCHEDULE_ID, scheduleId);
        }
        // 排产方案须为 DRAFT 才允许重排（PUBLISHED/ARCHIVED 为锁定/历史参照）
        if (!Objects.equals(schedule.getStatus(), ErpApsConstants.SCHEDULE_STATUS_DRAFT)) {
            throw new NopException(ErpApsErrors.ERR_APS_SCHEDULE_ILLEGAL_STATUS)
                    .param(ErpApsErrors.ARG_SCHEDULE_ID, scheduleId)
                    .param(ErpApsErrors.ARG_CURRENT_STATUS, schedule.getStatus());
        }
        return schedule;
    }

    protected ErpApsOperationOrder requireOperationOrder(String operationOrderId, IServiceContext context) {
        ErpApsOperationOrder op = opOrderDao().getEntityById(operationOrderId);
        if (op == null) {
            throw new NopException(ErpApsErrors.ERR_APS_OP_ORDER_NOT_FOUND)
                    .param(ErpApsErrors.ARG_OP_ORDER_ID, operationOrderId);
        }
        return op;
    }

    /** 当前时间，供未指定 earliestStartDateT 的工序兜底。 */
    protected LocalDateTime currentDateTime() {
        return CoreMetrics.currentDateTime();
    }

    protected ErpApsSchedulingEngine newEngine(int bufferMinutes, LocalDateTime horizonStart, LocalDateTime horizonEnd) {
        // ErpApsSchedulingEngine 为纯算法 POJO（无 Spring/DB 依赖），非 ORM 实体，不适用 newEntity()
        return new ErpApsSchedulingEngine(bufferMinutes, horizonStart, horizonEnd);
    }

    /** 路由生效期判定基准日版构造（RC-R1.87；默认取当前日期，测试可覆盖注入固定日期）。 */
    protected ErpApsSchedulingEngine newEngine(int bufferMinutes, LocalDateTime horizonStart,
                                               LocalDateTime horizonEnd, LocalDate routingEffectiveDate) {
        return new ErpApsSchedulingEngine(bufferMinutes, horizonStart, horizonEnd, routingEffectiveDate);
    }

    // ---------- DAO 访问（同域实体，IDaoProvider 直接访问） ----------

    protected IEntityDao<ErpApsOperationOrder> opOrderDao() {
        return daoProvider.daoFor(ErpApsOperationOrder.class);
    }

    protected IEntityDao<ErpApsConstraint> constraintDao() {
        return daoProvider.daoFor(ErpApsConstraint.class);
    }

    protected IEntityDao<ErpApsSchedule> scheduleDao() {
        return daoProvider.daoFor(ErpApsSchedule.class);
    }

    protected IEntityDao<ErpApsCapacityReservation> capacityReservationDao() {
        return daoProvider.daoFor(ErpApsCapacityReservation.class);
    }

    protected IEntityDao<ErpApsOpRouting> opRoutingDao() {
        return daoProvider.daoFor(ErpApsOpRouting.class);
    }
}
