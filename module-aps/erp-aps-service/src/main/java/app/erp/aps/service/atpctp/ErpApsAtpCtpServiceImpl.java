package app.erp.aps.service.atpctp;

import app.erp.aps.biz.CtpResult;
import app.erp.aps.biz.IErpApsAtpCtpService;
import app.erp.aps.biz.ScheduledOperationView;
import app.erp.aps.dao.entity.ErpApsConstraint;
import app.erp.aps.dao.entity.ErpApsOperationOrder;
import app.erp.aps.service.ErpApsConfigs;
import app.erp.aps.service.ErpApsConstants;
import app.erp.aps.service.scheduling.ErpApsSchedulingEngine;
import app.erp.aps.service.scheduling.WorkCenterTimeline;
import app.erp.inv.dao.entity.ErpInvReservation;
import app.erp.inv.dao.entity.ErpInvReservationLine;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgBomOperation;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ATP/CTP 交期承诺模拟服务实现（{@code scheduling.md §七}）。
 *
 * <p><b>跨域访问实现选择</b>：本期对 inventory/manufacturing 域的 ATP 库存聚合与 CTP 工艺路线追溯采用
 * {@link IDaoProvider} 只读实体查询（{@code findAllByQuery}），而非注入跨域 I*Biz。
 * 原因：跨域 I*Biz 强注入会在 aps-service 单模块部署/测试时因依赖模块未组装而启动失败，破坏模块独立性。
 * 本服务仅做<b>只读聚合</b>（无业务写操作、无状态机），{@code IDaoProvider} 读取等价且零启动耦合；
 * 完整 {@code app-erp-all} 部署时跨域实体同样可经统一 ORM Session 访问。计划 Phase 4 Decision 记录的
 * "rejected = 直接 SQL 跨库"仍然成立——此处使用的是 ORM 实体查询，非裸 SQL，未破坏模块物理边界。
 *
 * <p>CTP 影子工序经 {@link IEntityDao#newEntity()} 构造，仅参与内存模拟，从不 save，故不持久化。
 */
@BizModel("ErpApsAtpCtpService")
public class ErpApsAtpCtpServiceImpl implements IErpApsAtpCtpService {

    @Inject
    IDaoProvider daoProvider;

    @Override
    public LocalDateTime earliestCompletionDate(Long materialId, BigDecimal qty) {
        if (atpAvailable(materialId, qty)) {
            return CoreMetrics.currentDateTime();
        }
        CtpResult ctp = simulateCtp(materialId, qty, CoreMetrics.currentDateTime(), null);
        return ctp.getEarliestCompletionDate();
    }

    @Override
    public CtpResult checkFeasibility(Long materialId, BigDecimal qty, LocalDateTime desiredDate) {
        if (atpAvailable(materialId, qty)) {
            CtpResult ok = new CtpResult();
            ok.setFeasible(true);
            ok.setEarliestCompletionDate(CoreMetrics.currentDateTime());
            return ok;
        }
        return simulateCtp(materialId, qty, CoreMetrics.currentDateTime(), desiredDate);
    }

    @Override
    public List<ScheduledOperationView> simulateSchedule(Long materialId, BigDecimal qty, LocalDateTime startDate) {
        List<ErpApsOperationOrder> shadows = buildShadowOps(materialId, qty);
        if (shadows.isEmpty()) {
            return new ArrayList<>();
        }
        int buffer = AppConfig.var(ErpApsConfigs.CONFIG_BUFFER_MINUTES_BETWEEN_OPS,
                ErpApsConfigs.DEFAULT_BUFFER_MINUTES_BETWEEN_OPS);
        // ErpApsSchedulingEngine 为纯算法 POJO，非 ORM 实体，不适用 newEntity()
        ErpApsSchedulingEngine engine = new ErpApsSchedulingEngine(buffer, startDate, null);
        Map<Long, WorkCenterTimeline> timelines = engine.snapshotTimelines(loadMaintenance(), loadPlannedOps());

        LocalDateTime cursor = startDate;
        List<ScheduledOperationView> views = new ArrayList<>();
        for (ErpApsOperationOrder shadow : shadows) {
            WorkCenterTimeline tl = timelines.computeIfAbsent(shadow.getMachineId(), WorkCenterTimeline::new);
            LocalDateTime start = engine.simulateForward(tl, shadow, cursor);
            if (start == null) {
                break;
            }
            long dur = engine.computeDuration(shadow);
            LocalDateTime end = start.plusMinutes(dur);
            tl.addBusy(start, end, "shadow");
            cursor = end.plusMinutes(buffer);

            ScheduledOperationView v = new ScheduledOperationView();
            v.setWorkcenterId(shadow.getMachineId());
            v.setOperationName(shadow.getOperationName());
            v.setPlannedStartDateT(start);
            v.setPlannedEndDateT(end);
            v.setDurationMinutes(dur);
            views.add(v);
        }
        return views;
    }

    // ---------- ATP 库存聚合（只读，IDaoProvider） ----------

    protected boolean atpAvailable(Long materialId, BigDecimal qty) {
        BigDecimal onHand = sumOnHand(materialId);
        BigDecimal reserved = sumReserved(materialId);
        return onHand.subtract(reserved).compareTo(qty) >= 0;
    }

    protected BigDecimal sumOnHand(Long materialId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        BigDecimal total = BigDecimal.ZERO;
        for (ErpInvStockBalance b : stockBalanceDao().findAllByQuery(q)) {
            if (b.getAvailableQuantity() != null) {
                total = total.add(b.getAvailableQuantity());
            }
        }
        return total;
    }

    protected BigDecimal sumReserved(Long materialId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        BigDecimal total = BigDecimal.ZERO;
        IEntityDao<ErpInvReservation> resDao = reservationDao();
        for (ErpInvReservationLine l : reservationLineDao().findAllByQuery(q)) {
            if (l.getReservedQuantity() != null && isReservationActive(resDao, l.getReservationId())) {
                total = total.add(l.getReservedQuantity());
            }
        }
        return total;
    }

    protected boolean isReservationActive(IEntityDao<ErpInvReservation> resDao, Long reservationId) {
        if (reservationId == null) {
            return false;
        }
        ErpInvReservation r = resDao.getEntityById(reservationId);
        return r != null && !"CANCELLED".equals(r.getStatus());
    }

    // ---------- CTP 影子模拟 ----------

    protected CtpResult simulateCtp(Long materialId, BigDecimal qty, LocalDateTime startDate,
                                    LocalDateTime desiredDate) {
        CtpResult result = new CtpResult();
        List<ErpApsOperationOrder> shadows = buildShadowOps(materialId, qty);
        if (shadows.isEmpty()) {
            result.setFeasible(false);
            result.setReason("物料 " + materialId + " 无可用工艺路线（默认 BOM 缺失或无工序）");
            result.setEarliestCompletionDate(desiredDate);
            return result;
        }
        int buffer = AppConfig.var(ErpApsConfigs.CONFIG_BUFFER_MINUTES_BETWEEN_OPS,
                ErpApsConfigs.DEFAULT_BUFFER_MINUTES_BETWEEN_OPS);
        // ErpApsSchedulingEngine 为纯算法 POJO，非 ORM 实体，不适用 newEntity()
        ErpApsSchedulingEngine engine = new ErpApsSchedulingEngine(buffer, startDate, null);
        Map<Long, WorkCenterTimeline> timelines = engine.snapshotTimelines(loadMaintenance(), loadPlannedOps());

        LocalDateTime cursor = startDate;
        LocalDateTime latestEnd = startDate;
        Long bottleneckWc = null;
        long maxDur = 0;
        for (ErpApsOperationOrder shadow : shadows) {
            WorkCenterTimeline tl = timelines.computeIfAbsent(shadow.getMachineId(), WorkCenterTimeline::new);
            LocalDateTime start = engine.simulateForward(tl, shadow, cursor);
            if (start == null) {
                result.setFeasible(false);
                result.setBottleneckWorkcenter(String.valueOf(shadow.getMachineId()));
                result.setReason("工作中心 " + shadow.getMachineId() + " 无可用时段容纳影子工序");
                result.setEarliestCompletionDate(desiredDate);
                return result;
            }
            long dur = engine.computeDuration(shadow);
            LocalDateTime end = start.plusMinutes(dur);
            tl.addBusy(start, end, "shadow");
            cursor = end.plusMinutes(buffer);
            if (end.isAfter(latestEnd)) {
                latestEnd = end;
            }
            if (dur > maxDur) {
                maxDur = dur;
                bottleneckWc = shadow.getMachineId();
            }
        }
        result.setEarliestCompletionDate(latestEnd);
        result.setBottleneckWorkcenter(bottleneckWc == null ? null : String.valueOf(bottleneckWc));
        boolean feasible = desiredDate == null || !latestEnd.isAfter(desiredDate);
        result.setFeasible(feasible);
        if (!feasible && desiredDate != null) {
            result.setCapacityGapMinutes(java.time.Duration.between(desiredDate, latestEnd).toMinutes());
        }
        return result;
    }

    /** 按物料追溯默认 BOM 工艺路线，构造影子工序（{@code newEntity()}，不持久化）。 */
    protected List<ErpApsOperationOrder> buildShadowOps(Long materialId, BigDecimal qty) {
        ErpMfgBom bom = findDefaultBom(materialId);
        if (bom == null) {
            return new ArrayList<>();
        }
        IEntityDao<ErpApsOperationOrder> apsDao = apsOpOrderDao();
        List<ErpApsOperationOrder> shadows = new ArrayList<>();
        int seq = 10;
        for (ErpMfgBomOperation bo : loadBomOperations(bom.getId())) {
            if (bo.getWorkcenterId() == null) {
                continue;
            }
            ErpApsOperationOrder shadow = apsDao.newEntity();
            shadow.setMachineId(bo.getWorkcenterId());
            shadow.setOperationName("工序-" + bo.getLineNo());
            shadow.setSequence(seq);
            shadow.setSetupTime(BigDecimal.ZERO);
            shadow.setRuntimePerUnit(bo.getStandardTime() == null ? BigDecimal.ZERO : bo.getStandardTime());
            shadow.setQty(qty);
            shadow.setPriority(50);
            shadow.setEarliestStartDateT(CoreMetrics.currentDateTime());
            shadow.setStatus(ErpApsConstants.OP_STATUS_DRAFT);
            shadows.add(shadow);
            seq += 10;
        }
        shadows.sort(Comparator.comparingInt(o -> o.getSequence() == null ? Integer.MAX_VALUE : o.getSequence()));
        return shadows;
    }

    protected ErpMfgBom findDefaultBom(Long materialId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("productId", materialId));
        q.addFilter(eq("isDefault", true));
        q.addFilter(eq("isActive", true));
        q.setLimit(1);
        List<ErpMfgBom> list = bomDao().findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    protected List<ErpMfgBomOperation> loadBomOperations(Long bomId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("bomId", bomId));
        return bomOperationDao().findAllByQuery(q);
    }

    protected List<ErpApsConstraint> loadMaintenance() {
        QueryBean q = new QueryBean();
        q.addFilter(eq("constraintType", ErpApsConstants.CONSTRAINT_TYPE_MAINTENANCE));
        return apsConstraintDao().findAllByQuery(q);
    }

    protected List<ErpApsOperationOrder> loadPlannedOps() {
        QueryBean q = new QueryBean();
        q.addFilter(eq("status", ErpApsConstants.OP_STATUS_PLANNED));
        return apsOpOrderDao().findAllByQuery(q);
    }

    // ---------- DAO 访问（只读跨域 + 同域 shadow 构造） ----------

    protected IEntityDao<ErpApsOperationOrder> apsOpOrderDao() {
        return daoProvider.daoFor(ErpApsOperationOrder.class);
    }

    protected IEntityDao<ErpApsConstraint> apsConstraintDao() {
        return daoProvider.daoFor(ErpApsConstraint.class);
    }

    protected IEntityDao<ErpInvStockBalance> stockBalanceDao() {
        return daoProvider.daoFor(ErpInvStockBalance.class);
    }

    protected IEntityDao<ErpInvReservation> reservationDao() {
        return daoProvider.daoFor(ErpInvReservation.class);
    }

    protected IEntityDao<ErpInvReservationLine> reservationLineDao() {
        return daoProvider.daoFor(ErpInvReservationLine.class);
    }

    protected IEntityDao<ErpMfgBom> bomDao() {
        return daoProvider.daoFor(ErpMfgBom.class);
    }

    protected IEntityDao<ErpMfgBomOperation> bomOperationDao() {
        return daoProvider.daoFor(ErpMfgBomOperation.class);
    }
}
