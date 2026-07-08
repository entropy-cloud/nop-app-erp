package app.erp.mfg.service.dashboard;

import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.ErpMfgConstants;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
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
            List<ErpMfgWorkOrder> orders = daoProvider.daoFor(ErpMfgWorkOrder.class).findAll();
            Map<String, Long> countByStatus = new LinkedHashMap<>();
            for (ErpMfgWorkOrder o : orders) {
                String s = o.getDocStatus();
                if (s == null) s = "UNKNOWN";
                countByStatus.merge(s, 1L, Long::sum);
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            countByStatus.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                    .forEach(e -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("status", e.getKey());
                        row.put("count", e.getValue());
                        rows.add(row);
                    });
            return rows;
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

    // ===================== helpers =====================

    private long countByDocStatusIn(List<String> statuses) {
        IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("docStatus", statuses));
        return dao.findAllByQuery(q).size();
    }

    private long countByDocStatus(String status) {
        IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("docStatus", status));
        return dao.findAllByQuery(q).size();
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
}
