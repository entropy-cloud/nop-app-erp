package app.erp.mfg.service.dashboard;

import app.erp.mfg.biz.CrpLoadReportItem;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.ErpMfgConfigs;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.crp.CrpLoadCalculator;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.beans.query.QueryFieldBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.le;
import static io.nop.api.core.beans.FilterBeans.ne;

/**
 * 制造看板聚合入口（{@code dashboards.md §7}）。服务型 BizObject（非实体聚合），
 * 注入 {@link IDaoProvider}/{@link IOrmTemplate} 经 {@link QueryBean} 过滤后内存聚合，
 * 镜像 {@code ErpFinDashboardBizModel} 范式。
 *
 * <p>KPI 口径：在制工单数取自 {@link ErpMfgWorkOrder}（docStatus IN [IN_PROCESS, STOCK_RESERVED] count）；
 * 本期完工量取自 {@link ErpMfgWorkOrder}（COMPLETED 期内 Σ completedQuantity）；
 * 工单准时率 = count(COMPLETED 且 actualEndDate ≤ plannedEndDate) / count(COMPLETED)；
 * 齐套待产 = count(docStatus=STOCK_PARTIAL)。
 *
 * <p>齐套不足预警缺件明细 Non-Goal（{@code ErpMfgMaterialReservation} 未物化，见 plan 2026-07-06-1606-1 Phase 1 Decision）。
 */
@BizModel("ErpMfgDashboard")
public class ErpMfgDashboardBizModel {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    CrpLoadCalculator crpLoadCalculator;

    public void setCrpLoadCalculator(CrpLoadCalculator crpLoadCalculator) {
        this.crpLoadCalculator = crpLoadCalculator;
    }

    @BizQuery
    public Map<String, Object> getDashboardKpi(@Optional @Name("startDate") LocalDate startDate,
                                                @Optional @Name("endDate") LocalDate endDate,
                                                IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            LocalDate today = CoreMetrics.currentDate();
            LocalDate from = startDate != null ? startDate : today.withDayOfMonth(1);
            LocalDate to = endDate != null ? endDate : today;

            long inProcessCount = countByDocStatusIn(Arrays.asList(
                    ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS,
                    ErpMfgConstants.WORK_ORDER_STATUS_STOCK_RESERVED));
            BigDecimal periodCompletedQty = sumCompletedQtyInRange(from, to);
            long stockPartialCount = countByDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_STOCK_PARTIAL);
            double onTimeRate = computeOnTimeRate();

            Map<String, Object> kpi = new LinkedHashMap<>();
            kpi.put("startDate", from);
            kpi.put("endDate", to);
            kpi.put("inProcessCount", inProcessCount);
            kpi.put("periodCompletedQty", periodCompletedQty);
            kpi.put("stockPartialCount", stockPartialCount);
            kpi.put("onTimeRate", onTimeRate);
            return kpi;
        });
    }

    /** 工单状态分布（按 docStatus 聚合）。 */
    @BizQuery
    public List<Map<String, Object>> getWorkOrderStatusDistribution(IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            // DB 级 GROUP BY docStatus + COUNT，避免全表物化
            QueryBean q = new QueryBean();
            q.setSourceName(ErpMfgWorkOrder.class.getName());
            QueryFieldBean dim = QueryFieldBean.mainField("docStatus");
            QueryFieldBean cnt = QueryFieldBean.mainField("docStatus").count().alias("cnt");
            q.setFields(Arrays.asList(dim, cnt));
            List<Map<String, Object>> rows = ormTemplate.findListByQuery(q);
            List<Map<String, Object>> result = new ArrayList<>(rows.size());
            for (Map<String, Object> row : rows) {
                String s = row.get("docStatus") == null ? "UNKNOWN" : String.valueOf(row.get("docStatus"));
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("status", s);
                r.put("count", ((Number) row.get("cnt")).longValue());
                result.add(r);
            }
            result.sort(Comparator.<Map<String, Object>, Long>comparing(
                    r -> (Long) r.get("count"), Comparator.reverseOrder()));
            return result;
        });
    }

    /** 产成品产出趋势（近 N 月完工量，按 actualEndDate 月份聚合 completedQuantity）。 */
    @BizQuery
    public List<Map<String, Object>> getDashboardTrend(@Optional @Name("months") Integer months,
                                                        IServiceContext context) {
        int n = months == null || months <= 0 ? 12 : months;
        LocalDate today = CoreMetrics.currentDate();
        LocalDate from = today.minusMonths(n - 1L).withDayOfMonth(1);
        return ormTemplate.runInSession(session -> {
            List<ErpMfgWorkOrder> orders = loadCompletedInRange(from, today);
            Map<String, BigDecimal> qtyByMonth = new LinkedHashMap<>();
            for (ErpMfgWorkOrder o : orders) {
                LocalDate d = o.getActualEndDate();
                if (d == null) continue;
                String key = d.getYear() + "-" + String.format("%02d", d.getMonthValue());
                qtyByMonth.merge(key, nz(o.getCompletedQuantity()), BigDecimal::add);
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                LocalDate m = from.plusMonths(i);
                String key = m.getYear() + "-" + String.format("%02d", m.getMonthValue());
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("month", key);
                row.put("completedQty", qtyByMonth.getOrDefault(key, BigDecimal.ZERO));
                rows.add(row);
            }
            return rows;
        });
    }

    /** 工单延期预警（plannedEndDate < today 且未 COMPLETED/CLOSED/CANCELLED）。 */
    @BizQuery
    public List<Map<String, Object>> findDelayedWorkOrderAlert(IServiceContext context) {
        LocalDate today = CoreMetrics.currentDate();
        return ormTemplate.runInSession(session -> {
            IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
            QueryBean q = new QueryBean();
            q.addFilter(ne("docStatus", ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED));
            q.addFilter(ne("docStatus", ErpMfgConstants.WORK_ORDER_STATUS_CLOSED));
            q.addFilter(ne("docStatus", ErpMfgConstants.WORK_ORDER_STATUS_CANCELLED));
            List<ErpMfgWorkOrder> orders = dao.findAllByQuery(q);
            List<Map<String, Object>> rows = new ArrayList<>();
            for (ErpMfgWorkOrder o : orders) {
                LocalDate planned = o.getPlannedEndDate();
                if (planned != null && planned.isBefore(today)) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("workOrderId", o.getId());
                    row.put("workOrderCode", o.getCode());
                    row.put("plannedEndDate", planned);
                    row.put("docStatus", o.getDocStatus());
                    long overdueDays = java.time.temporal.ChronoUnit.DAYS.between(planned, today);
                    row.put("overdueDays", overdueDays);
                    rows.add(row);
                }
            }
            return rows;
        });
    }

    /**
     * CRP 负荷/产能对比图数据（{@code crp.md §负载报表}，plan 2026-07-17-2010-1）。
     *
     * <p>委派 {@link CrpLoadCalculator#getLoadReport} 复用既有产能/负荷率派生链（{@code WorkcenterCalendar}
     * 出勤时段 × {@code WorkcenterCapacity.efficiencyFactor}），再按 {@code loadDate} 聚合返回的
     * {@link CrpLoadReportItem}（Σ loadHours / Σ capacityHours / 派生 loadRate），返回结构化 DTO
     * 供看板 echarts 渲染 bar（负荷小时）+ line（产能小时）+ 负荷率。
     *
     * <p>dateFrom/dateTo 为空时取近 N 天（{@code erp-dash.mfg-crp-default-days}，默认 7）；空串当 null
     * 宽容处理（对齐 1321-3 范式）。空数据返回零值结构（非 {@code null}）。
     */
    @BizQuery
    public Map<String, Object> getCrpLoadChartData(@Optional @Name("workcenterId") Long workcenterId,
                                                    @Optional @Name("dateFrom") String dateFrom,
                                                    @Optional @Name("dateTo") String dateTo,
                                                    IServiceContext context) {
        LocalDate today = CoreMetrics.currentDate();
        LocalDate from = parseDate(dateFrom, null);
        LocalDate to = parseDate(dateTo, null);
        if (from == null && to == null) {
            int n = ErpMfgConfigs.getDashMfgCrpDefaultDays();
            to = today;
            from = today.minusDays((long) n - 1L);
        } else if (from == null) {
            from = to.minusDays(0);
        } else if (to == null) {
            to = today;
        }
        if (from.isAfter(to)) {
            LocalDate tmp = from;
            from = to;
            to = tmp;
        }

        List<Long> workcenterIds = workcenterId != null ? Collections.singletonList(workcenterId) : null;
        List<CrpLoadReportItem> items = crpLoadCalculator.getLoadReport(from, to, workcenterIds);

        // 按 loadDate 聚合（多个工作中心合并为日总量）
        Map<LocalDate, BigDecimal> loadByDate = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> capByDate = new LinkedHashMap<>();
        for (CrpLoadReportItem item : items) {
            LocalDate d = item.getLoadDate();
            if (d == null) continue;
            loadByDate.merge(d, nz(item.getLoadHours()), BigDecimal::add);
            capByDate.merge(d, nz(item.getCapacityHours()), BigDecimal::add);
        }

        List<Map<String, Object>> series = new ArrayList<>();
        BigDecimal totalLoad = BigDecimal.ZERO;
        BigDecimal totalCap = BigDecimal.ZERO;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            BigDecimal load = loadByDate.getOrDefault(d, BigDecimal.ZERO);
            BigDecimal cap = capByDate.getOrDefault(d, BigDecimal.ZERO);
            BigDecimal rate = deriveLoadRate(load, cap);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("loadDate", d);
            row.put("loadHours", load);
            row.put("capacityHours", cap);
            row.put("loadRate", rate);
            series.add(row);
            totalLoad = totalLoad.add(load);
            totalCap = totalCap.add(cap);
        }
        BigDecimal overallRate = deriveLoadRate(totalLoad, totalCap);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dateFrom", from);
        result.put("dateTo", to);
        result.put("workcenterId", workcenterId);
        result.put("series", series);
        result.put("totalLoadHours", totalLoad);
        result.put("totalCapacityHours", totalCap);
        result.put("overallLoadRate", overallRate);
        return result;
    }

    // ===================== helpers =====================

    private long countByDocStatusIn(List<String> statuses) {
        IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("docStatus", statuses));
        return dao.countByQuery(q);
    }

    private long countByDocStatus(String status) {
        IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("docStatus", status));
        return dao.countByQuery(q);
    }

    private BigDecimal sumCompletedQtyInRange(LocalDate from, LocalDate to) {
        IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("docStatus", ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED));
        q.addFilter(ge("actualEndDate", from));
        q.addFilter(le("actualEndDate", to));
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpMfgWorkOrder o : dao.findAllByQuery(q)) {
            sum = sum.add(nz(o.getCompletedQuantity()));
        }
        return sum;
    }

    private double computeOnTimeRate() {
        IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("docStatus", ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED));
        List<ErpMfgWorkOrder> completed = dao.findAllByQuery(q);
        if (completed.isEmpty()) return 0.0;
        long onTime = 0;
        for (ErpMfgWorkOrder o : completed) {
            LocalDate actual = o.getActualEndDate();
            LocalDate planned = o.getPlannedEndDate();
            if (actual != null && planned != null && !actual.isAfter(planned)) {
                onTime++;
            }
        }
        return (double) onTime / (double) completed.size();
    }

    private List<ErpMfgWorkOrder> loadCompletedInRange(LocalDate from, LocalDate to) {
        IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("docStatus", ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED));
        q.addFilter(ge("actualEndDate", from));
        q.addFilter(le("actualEndDate", to));
        return dao.findAllByQuery(q);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static LocalDate parseDate(String raw, LocalDate fallback) {
        if (raw == null || raw.trim().isEmpty()) {
            return fallback;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static BigDecimal deriveLoadRate(BigDecimal loadHours, BigDecimal capacityHours) {
        loadHours = nz(loadHours);
        capacityHours = nz(capacityHours);
        if (capacityHours.signum() <= 0) {
            return loadHours.signum() > 0 ? new BigDecimal("9999") : BigDecimal.ZERO;
        }
        return loadHours.divide(capacityHours, 4, RoundingMode.HALF_UP);
    }
}
