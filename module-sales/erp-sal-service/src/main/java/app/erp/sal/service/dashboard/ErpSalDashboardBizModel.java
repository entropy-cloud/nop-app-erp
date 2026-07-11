package app.erp.sal.service.dashboard;

import app.erp.fin.biz.IErpFinArApItemBiz;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.service.ErpSalConstants;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.beans.query.QueryFieldBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * 销售看板聚合入口（{@code dashboards.md §1}）。服务型 BizObject（非实体聚合），
 * 注入 {@link IDaoProvider}/{@link IOrmTemplate} 经 {@link QueryBean} 过滤后内存聚合，
 * 镜像 {@code ErpFinDashboardBizModel} 范式。
 *
 * <p>KPI 口径：本期销售额取自 {@link ErpSalInvoice}（posted, businessDate 期内 Σ amountFunctional）；
 * 本期订单量取自 {@link ErpSalOrder}（docStatus=ACTIVE count）；订单→开票转化率 = invoice count / order count；
 * 应收余额跨域读 {@link ErpFinArApItem}（direction=RECEIVABLE, OPEN+PARTIAL），经 {@link IErpFinArApItemBiz} 注入（R 跨域只读）。
 */
@BizModel("ErpSalDashboard")
public class ErpSalDashboardBizModel {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinArApItemBiz arApItemBiz;

    @BizQuery
    public Map<String, Object> getDashboardKpi(@Optional @Name("startDate") LocalDate startDate,
                                                @Optional @Name("endDate") LocalDate endDate,
                                                IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            LocalDate today = CoreMetrics.currentDate();
            LocalDate from = startDate != null ? startDate : today.withDayOfMonth(1);
            LocalDate to = endDate != null ? endDate : today;

            List<ErpSalInvoice> invoices = loadPostedInvoicesInRange(from, to);
            BigDecimal salesAmount = BigDecimal.ZERO;
            for (ErpSalInvoice inv : invoices) {
                salesAmount = salesAmount.add(nz(inv.getAmountFunctional()));
            }

            long orderCount = countActiveOrders();
            long invoiceCount = invoices.size();
            double conversionRate = orderCount > 0 ? (double) invoiceCount / (double) orderCount : 0.0;

            BigDecimal arBalance = sumArApOpen(ErpFinConstants.DIRECTION_RECEIVABLE, context);

            Map<String, Object> kpi = new LinkedHashMap<>();
            kpi.put("startDate", from);
            kpi.put("endDate", to);
            kpi.put("salesAmount", salesAmount);
            kpi.put("orderCount", orderCount);
            kpi.put("invoiceCount", invoiceCount);
            kpi.put("conversionRate", conversionRate);
            kpi.put("arBalance", arBalance);
            return kpi;
        });
    }

    @BizQuery
    public List<Map<String, Object>> getDashboardTrend(@Optional @Name("months") Integer months,
                                                        IServiceContext context) {
        int n = months == null || months <= 0 ? 12 : months;
        LocalDate today = CoreMetrics.currentDate();
        LocalDate from = today.minusMonths(n - 1L).withDayOfMonth(1);
        return ormTemplate.runInSession(session -> {
            List<ErpSalInvoice> invoices = loadPostedInvoicesInRange(from, today);
            Map<String, BigDecimal> amountByMonth = new LinkedHashMap<>();
            for (ErpSalInvoice inv : invoices) {
                LocalDate d = inv.getBusinessDate();
                if (d == null) continue;
                String key = d.getYear() + "-" + String.format("%02d", d.getMonthValue());
                amountByMonth.merge(key, nz(inv.getAmountFunctional()), BigDecimal::add);
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                LocalDate m = from.plusMonths(i);
                String key = m.getYear() + "-" + String.format("%02d", m.getMonthValue());
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("month", key);
                row.put("salesAmount", amountByMonth.getOrDefault(key, BigDecimal.ZERO));
                rows.add(row);
            }
            return rows;
        });
    }

    /** 客户 TOP N（按销售额降序）。 */
    @BizQuery
    public List<Map<String, Object>> findCustomerTopN(@Optional @Name("limit") Integer limit,
                                                       IServiceContext context) {
        int topN = limit == null || limit <= 0 ? 10 : limit;
        return ormTemplate.runInSession(session -> {
            // DB 级 GROUP BY customerId + SUM(amountFunctional) WHERE posted=true（报告 §1.6 严重度项：
            // 原 findAll 全表内存聚合改为 DB 级聚合，消除企业数据量 OOM）
            QueryBean q = new QueryBean();
            q.setSourceName(ErpSalInvoice.class.getName());
            q.addFilter(eq("posted", Boolean.TRUE));
            QueryFieldBean dim = QueryFieldBean.mainField("customerId");
            QueryFieldBean sumAmt = QueryFieldBean.mainField("amountFunctional").sum().alias("salesAmount");
            q.setFields(Arrays.asList(dim, sumAmt));
            List<Map<String, Object>> rows = ormTemplate.findListByQuery(q);
            List<Map<String, Object>> grouped = new ArrayList<>(rows.size());
            for (Map<String, Object> row : rows) {
                if (row.get("customerId") == null) continue;
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("customerId", row.get("customerId"));
                r.put("salesAmount", toBigDecimal(row.get("salesAmount")));
                grouped.add(r);
            }
            grouped.sort(Comparator.<Map<String, Object>, BigDecimal>comparing(
                    r -> (BigDecimal) r.get("salesAmount"), Comparator.reverseOrder()));
            List<Map<String, Object>> result = new ArrayList<>();
            grouped.stream().limit(topN).forEach(result::add);
            Map<Long, String> nameCache = new HashMap<>();
            IEntityDao<ErpMdPartner> partnerDao = daoProvider.daoFor(ErpMdPartner.class);
            for (Map<String, Object> r : result) {
                Long pid = (Long) r.get("customerId");
                if (pid == null) continue;
                String name = nameCache.get(pid);
                if (name == null) {
                    ErpMdPartner p = partnerDao.getEntityById(pid);
                    name = p != null ? p.getName() : null;
                    nameCache.put(pid, name);
                }
                r.put("customerName", name);
            }
            return result;
        });
    }

    /**
     * 应收超期预警：账龄 > 阈值天数 且 openAmount > 阈值金额。
     * 阈值 ≤0 时不触发预警，返回空列表（默认关闭）。
     */
    @BizQuery
    public List<Map<String, Object>> findArOverdueAlert(IServiceContext context) {
        int daysThreshold = AppConfig.var(
                ErpSalConstants.CONFIG_DASH_SAL_AR_OVERDUE_DAYS,
                ErpSalConstants.DEFAULT_DASH_SAL_AR_OVERDUE_DAYS);
        BigDecimal amountThreshold = AppConfig.var(
                ErpSalConstants.CONFIG_DASH_SAL_AR_OVERDUE_AMOUNT,
                ErpSalConstants.DEFAULT_DASH_SAL_AR_OVERDUE_AMOUNT);
        if (daysThreshold <= 0 && (amountThreshold == null || amountThreshold.signum() <= 0)) {
            return Collections.emptyList();
        }
        LocalDate today = CoreMetrics.currentDate();
        List<ErpFinArApItem> items = arApItemBiz.findOpenItems(
                ErpFinConstants.DIRECTION_RECEIVABLE, context);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ErpFinArApItem it : items) {
            LocalDate base = it.getDueDate() != null ? it.getDueDate() : it.getBusinessDate();
            long age = base != null ? ChronoUnit.DAYS.between(base, today) : 0L;
            if (age < 0) age = 0L;
            BigDecimal open = nz(it.getOpenAmountFunctional());
            boolean dayHit = daysThreshold > 0 && age > daysThreshold;
            boolean amountHit = amountThreshold != null && amountThreshold.signum() > 0
                    && open.compareTo(amountThreshold) > 0;
            if (dayHit && amountHit) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("partnerId", it.getPartnerId());
                String partnerName = null;
                if (it.getPartnerId() != null) {
                    ErpMdPartner p = daoProvider.daoFor(ErpMdPartner.class).getEntityById(it.getPartnerId());
                    partnerName = p != null ? p.getName() : null;
                }
                row.put("partnerName", partnerName);
                row.put("sourceBillCode", it.getSourceBillCode());
                row.put("openAmount", open);
                row.put("ageDays", age);
                rows.add(row);
            }
        }
        return rows;
    }

    // ===================== helpers =====================

    private List<ErpSalInvoice> loadPostedInvoicesInRange(LocalDate from, LocalDate to) {
        IEntityDao<ErpSalInvoice> dao = daoProvider.daoFor(ErpSalInvoice.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("posted", Boolean.TRUE));
        if (from != null) q.addFilter(ge("businessDate", from));
        if (to != null) q.addFilter(le("businessDate", to));
        return dao.findAllByQuery(q);
    }

    private long countActiveOrders() {
        IEntityDao<ErpSalOrder> dao = daoProvider.daoFor(ErpSalOrder.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("docStatus", ErpSalConstants.DOC_STATUS_ACTIVE));
        return dao.countByQuery(q);
    }

    private BigDecimal sumArApOpen(String direction, IServiceContext context) {
        List<ErpFinArApItem> items = arApItemBiz.findOpenItems(direction, context);
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpFinArApItem it : items) {
            sum = sum.add(nz(it.getOpenAmountFunctional()));
        }
        return sum;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal toBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Number) return new BigDecimal(v.toString());
        return new BigDecimal(v.toString());
    }
}
