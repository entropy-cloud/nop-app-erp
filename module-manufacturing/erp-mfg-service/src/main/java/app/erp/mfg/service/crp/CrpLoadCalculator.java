package app.erp.mfg.service.crp;

import app.erp.mfg.biz.CrpLoadReportItem;
import app.erp.mfg.dao.entity.ErpMfgCrpLoad;
import app.erp.mfg.dao.entity.ErpMfgRoutingOperation;
import app.erp.mfg.dao.entity.ErpMfgWorkcenter;
import app.erp.mfg.dao.entity.ErpMfgWorkcenterCalendar;
import app.erp.mfg.dao.entity.ErpMfgWorkcenterCapacity;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.le;
import static io.nop.api.core.beans.FilterBeans.ne;

/**
 * CRP 负荷计算引擎。服务于 {@code IErpMfgCrpBiz.calculateLoad/getLoadReport}（{@code crp.md §核心设计点}）。
 *
 * <p><b>负荷来源（fallback，无 APS）</b>：WorkOrder（plannedStartDate~plannedEndDate）经 RoutingOperation
 * 分派到工作中心，按 workcenter×loadDate 聚合 loadHours（RoutingOperation.standardTime 换算小时，均匀分派到区间日）
 * + setupHours（RoutingOperation.setupTime，计入首日）。
 *
 * <p><b>可用产能</b>：WorkcenterCalendar 出勤时段（Σ endTime-startTime 满足 workDatePattern/生效区间的班次）
 * × WorkcenterCapacity.efficiencyFactor（无产能子实体则 1.0）。{@code loadRate = loadHours / capacityHours}；
 * {@code overloaded = loadRate > erp-mfg.crp-overload-threshold}（默认 1.0）。
 *
 * <p><b>Non-Goal</b>：APS 工序级排产（写 OperationOrder）、maintenance 停机扣减可用时段、
 * 标量 Workcenter.capacity 迁移、班次级粒度（本期日级）、AMIS 可视化（见计划 Non-Goals）。
 *
 * <p>本类为非 BizModel 服务助手（对齐 {@code MrpEngine}/{@code BomExpander} 范式），manufacturing 域内只读聚合
 * （WorkOrder/RoutingOperation/Workcenter/Calendar/Capacity），无跨域写依赖。
 */
public class CrpLoadCalculator {

    static final BigDecimal SIXTY = new BigDecimal("60");
    static final int SCALE = 4;

    @Inject
    IDaoProvider daoProvider;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    /**
     * 计算负荷快照：清区间内既有 CrpLoad 行后，扫描 WorkOrder 重算并写入。
     *
     * @return 写入的 CrpLoad 行数。
     */
    public int calculateLoad(LocalDate periodFrom, LocalDate periodTo, List<Long> workcenterIds) {
        requirePeriod(periodFrom, periodTo);
        IEntityDao<ErpMfgCrpLoad> loadDao = daoProvider.daoFor(ErpMfgCrpLoad.class);
        clearExisting(loadDao, periodFrom, periodTo, workcenterIds);

        List<ErpMfgWorkOrder> workOrders = findWorkOrdersInWindow(periodFrom, periodTo);
        if (workOrders.isEmpty()) {
            return 0;
        }
        Set<Long> wcFilter = workcenterIds != null && !workcenterIds.isEmpty() ? new HashSet<>(workcenterIds) : null;

        int written = 0;
        for (ErpMfgWorkOrder wo : workOrders) {
            written += distributeWorkOrder(loadDao, wo, periodFrom, periodTo, wcFilter);
        }
        return written;
    }

    /**
     * 负荷报表：workcenter×date 聚合 loadHours/capacityHours/loadRate/overloaded。
     * 范围内工作中心 = workcenterIds（若提供）否则 = 区间内有 CrpLoad 行或有 Calendar 的工作中心。
     */
    public List<CrpLoadReportItem> getLoadReport(LocalDate periodFrom, LocalDate periodTo, List<Long> workcenterIds) {
        requirePeriod(periodFrom, periodTo);

        Set<Long> wcSet = resolveReportWorkcenters(periodFrom, periodTo, workcenterIds);
        if (wcSet.isEmpty()) {
            return Collections.emptyList();
        }

        Map<WCDate, LoadBucket> buckets = indexLoads(periodFrom, periodTo, wcSet);

        Map<Long, BigDecimal> efficiencyByWc = efficiencyByWorkcenter(wcSet);
        Map<Long, List<ErpMfgWorkcenterCalendar>> calendarByWc = calendarsByWorkcenter(wcSet);
        Map<Long, String> wcCode = workcenterCodes(wcSet);

        double threshold = AppConfig.var(ErpMfgConstants.CONFIG_CRP_OVERLOAD_THRESHOLD,
                ErpMfgConstants.DEFAULT_CRP_OVERLOAD_THRESHOLD);

        List<CrpLoadReportItem> result = new ArrayList<>();
        for (Long wcId : wcSet) {
            List<ErpMfgWorkcenterCalendar> calendars = calendarByWc.getOrDefault(wcId, Collections.emptyList());
            BigDecimal efficiency = efficiencyByWc.getOrDefault(wcId, BigDecimal.ONE);
            for (LocalDate d = periodFrom; !d.isAfter(periodTo); d = d.plusDays(1)) {
                WCDate key = new WCDate(wcId, d);
                LoadBucket bucket = buckets.get(key);
                BigDecimal loadHours = bucket != null ? bucket.loadHours : BigDecimal.ZERO;
                BigDecimal setupHours = bucket != null ? bucket.setupHours : BigDecimal.ZERO;

                BigDecimal rawCapacity = availableHours(calendars, d);
                BigDecimal capacityHours = rawCapacity.multiply(efficiency).setScale(SCALE, RoundingMode.HALF_UP);

                BigDecimal loadRate = computeLoadRate(loadHours, capacityHours);
                boolean overloaded = isOverloaded(loadHours, loadRate, threshold);

                CrpLoadReportItem item = new CrpLoadReportItem();
                item.setWorkcenterId(wcId);
                item.setWorkcenterCode(wcCode.get(wcId));
                item.setLoadDate(d);
                item.setLoadHours(loadHours);
                item.setSetupHours(setupHours);
                item.setCapacityHours(capacityHours);
                item.setLoadRate(loadRate);
                item.setOverloaded(overloaded);
                result.add(item);
            }
        }
        return result;
    }

    // ---------- calculateLoad helpers ----------

    private int distributeWorkOrder(IEntityDao<ErpMfgCrpLoad> loadDao, ErpMfgWorkOrder wo,
                                    LocalDate periodFrom, LocalDate periodTo, Set<Long> wcFilter) {
        Long routingId = wo.getRoutingId();
        if (routingId == null) {
            return 0;
        }
        List<ErpMfgRoutingOperation> ops = findRoutingOperations(routingId);
        if (ops.isEmpty()) {
            return 0;
        }

        LocalDate windowStart = woStart(wo, periodFrom);
        LocalDate windowEnd = woEnd(wo, periodTo);
        if (windowStart == null || windowEnd == null || windowStart.isAfter(windowEnd)) {
            return 0;
        }
        List<LocalDate> days = new ArrayList<>();
        for (LocalDate d = windowStart; !d.isAfter(windowEnd); d = d.plusDays(1)) {
            days.add(d);
        }
        BigDecimal dayCount = new BigDecimal(days.size());

        int written = 0;
        for (ErpMfgRoutingOperation op : ops) {
            Long wcId = op.getWorkcenterId();
            if (wcId == null) {
                continue;
            }
            if (wcFilter != null && !wcFilter.contains(wcId)) {
                continue;
            }
            BigDecimal loadHours = toHours(op.getStandardTime());
            BigDecimal setupHours = toHours(op.getSetupTime());

            BigDecimal perDay = loadHours.divide(dayCount, SCALE, RoundingMode.HALF_UP);
            for (int i = 0; i < days.size(); i++) {
                LocalDate d = days.get(i);
                BigDecimal dayLoad = perDay;
                BigDecimal daySetup = i == 0 ? setupHours : BigDecimal.ZERO;
                if ((dayLoad.signum() == 0 && daySetup.signum() == 0)) {
                    continue;
                }
                ErpMfgCrpLoad row = loadDao.newEntity();
                row.setWorkcenterId(wcId);
                row.setWorkOrderId(wo.getId());
                row.setLoadDate(d);
                row.setLoadHours(dayLoad);
                row.setSetupHours(daySetup);
                loadDao.saveEntity(row);
                written++;
            }
        }
        return written;
    }

    private List<ErpMfgRoutingOperation> findRoutingOperations(Long routingId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("routingId", routingId));
        q.addOrderField("lineNo", false);
        return daoProvider.daoFor(ErpMfgRoutingOperation.class).findAllByQuery(q);
    }

    private List<ErpMfgWorkOrder> findWorkOrdersInWindow(LocalDate periodFrom, LocalDate periodTo) {
        QueryBean q = new QueryBean();
        q.addFilter(ne("docStatus", ErpMfgConstants.WORK_ORDER_STATUS_CANCELLED));
        q.addFilter(le("plannedStartDate", periodTo));
        q.addFilter(ge("plannedEndDate", periodFrom));
        return daoProvider.daoFor(ErpMfgWorkOrder.class).findAllByQuery(q);
    }

    private void clearExisting(IEntityDao<ErpMfgCrpLoad> dao, LocalDate periodFrom, LocalDate periodTo, List<Long> workcenterIds) {
        QueryBean q = new QueryBean();
        q.addFilter(ge("loadDate", periodFrom));
        q.addFilter(le("loadDate", periodTo));
        if (workcenterIds != null && !workcenterIds.isEmpty()) {
            q.addFilter(in("workcenterId", workcenterIds));
        }
        List<ErpMfgCrpLoad> existing = dao.findAllByQuery(q);
        for (ErpMfgCrpLoad l : existing) {
            dao.deleteEntity(l);
        }
    }

    private LocalDate woStart(ErpMfgWorkOrder wo, LocalDate periodFrom) {
        LocalDate s = wo.getPlannedStartDate();
        if (s == null) {
            s = wo.getBusinessDate();
        }
        if (s == null) {
            return null;
        }
        return s.isBefore(periodFrom) ? periodFrom : s;
    }

    private LocalDate woEnd(ErpMfgWorkOrder wo, LocalDate periodTo) {
        LocalDate e = wo.getPlannedEndDate();
        if (e == null) {
            e = wo.getPlannedStartDate();
        }
        if (e == null) {
            e = wo.getBusinessDate();
        }
        if (e == null) {
            return null;
        }
        return e.isAfter(periodTo) ? periodTo : e;
    }

    // ---------- getLoadReport helpers ----------

    private Set<Long> resolveReportWorkcenters(LocalDate periodFrom, LocalDate periodTo, List<Long> workcenterIds) {
        if (workcenterIds != null && !workcenterIds.isEmpty()) {
            return new HashSet<>(workcenterIds);
        }
        Set<Long> set = new HashSet<>();
        QueryBean loadQ = new QueryBean();
        loadQ.addFilter(ge("loadDate", periodFrom));
        loadQ.addFilter(le("loadDate", periodTo));
        for (ErpMfgCrpLoad l : daoProvider.daoFor(ErpMfgCrpLoad.class).findAllByQuery(loadQ)) {
            if (l.getWorkcenterId() != null) {
                set.add(l.getWorkcenterId());
            }
        }
        for (ErpMfgWorkcenterCalendar c : daoProvider.daoFor(ErpMfgWorkcenterCalendar.class).findAll()) {
            if (c.getWorkcenterId() != null) {
                set.add(c.getWorkcenterId());
            }
        }
        return set;
    }

    private Map<WCDate, LoadBucket> indexLoads(LocalDate periodFrom, LocalDate periodTo, Set<Long> wcSet) {
        QueryBean q = new QueryBean();
        q.addFilter(ge("loadDate", periodFrom));
        q.addFilter(le("loadDate", periodTo));
        q.addFilter(in("workcenterId", new ArrayList<>(wcSet)));
        Map<WCDate, LoadBucket> buckets = new LinkedHashMap<>();
        for (ErpMfgCrpLoad l : daoProvider.daoFor(ErpMfgCrpLoad.class).findAllByQuery(q)) {
            WCDate key = new WCDate(l.getWorkcenterId(), l.getLoadDate());
            LoadBucket b = buckets.computeIfAbsent(key, k -> new LoadBucket());
            b.loadHours = b.loadHours.add(nz(l.getLoadHours()));
            b.setupHours = b.setupHours.add(nz(l.getSetupHours()));
        }
        return buckets;
    }

    private Map<Long, BigDecimal> efficiencyByWorkcenter(Set<Long> wcSet) {
        QueryBean q = new QueryBean();
        q.addFilter(in("workcenterId", new ArrayList<>(wcSet)));
        q.addFilter(eq("isActive", Boolean.TRUE));
        Map<Long, BigDecimal> map = new HashMap<>();
        for (ErpMfgWorkcenterCapacity c : daoProvider.daoFor(ErpMfgWorkcenterCapacity.class).findAllByQuery(q)) {
            BigDecimal eff = nz(c.getEfficiencyFactor(), BigDecimal.ONE);
            BigDecimal existing = map.get(c.getWorkcenterId());
            if (existing == null || eff.compareTo(existing) > 0) {
                map.put(c.getWorkcenterId(), eff);
            }
        }
        return map;
    }

    private Map<Long, List<ErpMfgWorkcenterCalendar>> calendarsByWorkcenter(Set<Long> wcSet) {
        QueryBean q = new QueryBean();
        q.addFilter(in("workcenterId", new ArrayList<>(wcSet)));
        q.addFilter(eq("isActive", Boolean.TRUE));
        Map<Long, List<ErpMfgWorkcenterCalendar>> map = new HashMap<>();
        for (ErpMfgWorkcenterCalendar c : daoProvider.daoFor(ErpMfgWorkcenterCalendar.class).findAllByQuery(q)) {
            map.computeIfAbsent(c.getWorkcenterId(), k -> new ArrayList<>()).add(c);
        }
        return map;
    }

    private Map<Long, String> workcenterCodes(Set<Long> wcSet) {
        Map<Long, String> map = new HashMap<>();
        for (ErpMfgWorkcenter wc : daoProvider.daoFor(ErpMfgWorkcenter.class).findAllByQuery(new QueryBean())) {
            if (wcSet.contains(wc.getId())) {
                map.put(wc.getId(), wc.getCode());
            }
        }
        return map;
    }

    private BigDecimal availableHours(List<ErpMfgWorkcenterCalendar> calendars, LocalDate date) {
        BigDecimal total = BigDecimal.ZERO;
        for (ErpMfgWorkcenterCalendar c : calendars) {
            if (c.getEffectiveFrom() != null && c.getEffectiveFrom().isAfter(date)) {
                continue;
            }
            if (c.getEffectiveTo() != null && c.getEffectiveTo().isBefore(date)) {
                continue;
            }
            if (!patternMatches(date, c.getWorkDatePattern())) {
                continue;
            }
            total = total.add(shiftHours(c.getStartTime(), c.getEndTime()));
        }
        return total;
    }

    private boolean patternMatches(LocalDate date, Integer pattern) {
        if (pattern == null || pattern == ErpMfgConstants.WORK_DATE_PATTERN_ALL_WEEK) {
            return true;
        }
        DayOfWeek dow = date.getDayOfWeek();
        if (pattern == ErpMfgConstants.WORK_DATE_PATTERN_WEEKDAYS) {
            return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
        }
        if (pattern == ErpMfgConstants.WORK_DATE_PATTERN_WEEKEND) {
            return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
        }
        return true;
    }

    private BigDecimal shiftHours(String startTime, String endTime) {
        if (startTime == null || startTime.isEmpty() || endTime == null || endTime.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            LocalTime s = LocalTime.parse(startTime.trim());
            LocalTime e = LocalTime.parse(endTime.trim());
            long mins = java.time.Duration.between(s, e).toMinutes();
            if (mins <= 0) {
                return BigDecimal.ZERO;
            }
            return new BigDecimal(mins).divide(SIXTY, SCALE, RoundingMode.HALF_UP);
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal computeLoadRate(BigDecimal loadHours, BigDecimal capacityHours) {
        if (capacityHours == null || capacityHours.signum() <= 0) {
            return loadHours != null && loadHours.signum() > 0 ? new BigDecimal("9999") : BigDecimal.ZERO;
        }
        return nz(loadHours).divide(capacityHours, SCALE, RoundingMode.HALF_UP);
    }

    private boolean isOverloaded(BigDecimal loadHours, BigDecimal loadRate, double threshold) {
        if (loadRate == null) {
            return false;
        }
        if (loadRate.compareTo(BigDecimal.valueOf(threshold)) > 0) {
            return true;
        }
        return loadHours != null && loadHours.signum() > 0 && new BigDecimal("9999").compareTo(loadRate) == 0;
    }

    private BigDecimal toHours(BigDecimal minutes) {
        if (minutes == null || minutes.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return minutes.divide(SIXTY, SCALE, RoundingMode.HALF_UP);
    }

    private void requirePeriod(LocalDate periodFrom, LocalDate periodTo) {
        if (periodFrom == null || periodTo == null || periodFrom.isAfter(periodTo)) {
            throw new NopException(ErpMfgErrors.ERR_CRP_PERIOD_INVALID)
                    .param(ErpMfgErrors.ARG_PERIOD_FROM, periodFrom)
                    .param(ErpMfgErrors.ARG_PERIOD_TO, periodTo);
        }
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static BigDecimal nz(BigDecimal v, BigDecimal fallback) {
        return v != null ? v : fallback;
    }

    private static final class WCDate {
        final Long workcenterId;
        final LocalDate date;

        WCDate(Long workcenterId, LocalDate date) {
            this.workcenterId = workcenterId;
            this.date = date;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof WCDate)) return false;
            WCDate w = (WCDate) o;
            return workcenterId.equals(w.workcenterId) && date.equals(w.date);
        }

        @Override
        public int hashCode() {
            return workcenterId.hashCode() * 31 + date.hashCode();
        }
    }

    private static final class LoadBucket {
        BigDecimal loadHours = BigDecimal.ZERO;
        BigDecimal setupHours = BigDecimal.ZERO;
    }
}
