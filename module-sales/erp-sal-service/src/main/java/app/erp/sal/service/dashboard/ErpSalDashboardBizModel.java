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
import io.nop.api.core.annotations.core.Description;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.le;
import static io.nop.api.core.beans.FilterBeans.ne;
import app.erp.common.service.DashboardUtil;

/**
 * 销售看板聚合入口（{@code dashboards.md §1}）。服务型 BizObject（非实体聚合），
 * 注入 {@link IDaoProvider}/{@link IOrmTemplate} 经 {@link QueryBean} 过滤后内存聚合，
 * 镜像 {@code ErpFinDashboardBizModel} 范式。
 *
 * <p>KPI 口径：本期销售额取自 {@link ErpSalInvoice}（posted, businessDate 期内 Σ amountFunctional）；
 * 本期订单量取自 {@link ErpSalOrder}（approveStatus=APPROVED 且非 CANCELLED：count）；订单→开票转化率 = invoice count / order count；
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

    @Description("销售看板 KPI（订单额、收入、毛利、回款、应收余额）")
    @BizQuery
    public Map<String, Object> getDashboardKpi(@Optional @Name("startDate") LocalDate startDate,
                                                @Optional @Name("endDate") LocalDate endDate,
                                                IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            LocalDate today = CoreMetrics.currentDate();
            LocalDate from = startDate != null ? startDate : today.withDayOfMonth(1);
            LocalDate to = endDate != null ? endDate : today;

            InvoiceAgg agg = aggPostedInvoicesInRange(from, to);
            BigDecimal salesAmount = agg.sum;

            long orderCount = countActiveOrders();
            long invoiceCount = agg.count;
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
            // DB 级 GROUP BY businessDate 聚合（perf-ux plan 0325-2：原 12 个月全量实体物化内存分桶
            // 改为日期维度分组聚合，行数 ≤ 区间天数，消除 OOM；业务日期为普通列维度，与
            // sumBalanceTotalCost 的 warehouseId 维度同一 fields 机制），null 业务日期行跳过（原语义）。
            IEntityDao<ErpSalInvoice> dao = daoProvider.daoFor(ErpSalInvoice.class);
            QueryBean q = new QueryBean();
            q.setSourceName(ErpSalInvoice.class.getName());
            q.addFilter(eq("posted", Boolean.TRUE));
            q.addFilter(ge("businessDate", from));
            q.addFilter(le("businessDate", today));
            QueryFieldBean dim = QueryFieldBean.mainField("businessDate");
            QueryFieldBean sumAmt = QueryFieldBean.mainField("amountFunctional").sum().alias("salesAmount");
            q.setFields(Arrays.asList(dim, sumAmt));
            List<Map<String, Object>> aggRows = ormTemplate.findListByQuery(q);

            Map<String, BigDecimal> amountByMonth = new LinkedHashMap<>();
            for (Map<String, Object> row : aggRows) {
                Object d = row.get("businessDate");
                if (d == null) continue;
                LocalDate date = toLocalDate(d);
                String key = date.getYear() + "-" + String.format("%02d", date.getMonthValue());
                amountByMonth.merge(key, DashboardUtil.nz(DashboardUtil.toBigDecimal(row.get("salesAmount"))), BigDecimal::add);
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
                r.put("salesAmount", DashboardUtil.toBigDecimal(row.get("salesAmount")));
                grouped.add(r);
            }
            grouped.sort(Comparator.<Map<String, Object>, BigDecimal>comparing(
                    r -> (BigDecimal) r.get("salesAmount"), Comparator.reverseOrder()));
            List<Map<String, Object>> result = new ArrayList<>();
            grouped.stream().limit(topN).forEach(result::add);
            Map<String, String> nameCache = new HashMap<>();
            IEntityDao<ErpMdPartner> partnerDao = daoProvider.daoFor(ErpMdPartner.class);
            for (Map<String, Object> r : result) {
                String pid = (String) r.get("customerId");
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
     * 应收超期预警：OR 语义（P2-CK-sal-020）——任一启用维度命中即告警：daysThreshold>0 时
     * 账龄超天数命中；amountThreshold>0 时 openAmount 超金额命中；未启用维度不参与判定。
     * 双维度均 ≤0 时整体关闭（默认关闭，返回空列表）。修复原 AND 耦合下仅配置单一阈值永不触发。
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
        Set<String> hitPartnerIds = new LinkedHashSet<>();
        for (ErpFinArApItem it : items) {
            LocalDate base = it.getDueDate() != null ? it.getDueDate() : it.getBusinessDate();
            long age = base != null ? ChronoUnit.DAYS.between(base, today) : 0L;
            if (age < 0) age = 0L;
            BigDecimal open = DashboardUtil.nz(it.getOpenAmountFunctional());
            boolean dayEnabled = daysThreshold > 0;
            boolean amountEnabled = amountThreshold != null && amountThreshold.signum() > 0;
            boolean dayHit = dayEnabled && age > daysThreshold;
            boolean amountHit = amountEnabled && open.compareTo(amountThreshold) > 0;
            if (dayHit || amountHit) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("partnerId", it.getPartnerId());
                row.put("sourceBillCode", it.getSourceBillCode());
                row.put("openAmount", open);
                row.put("ageDays", age);
                rows.add(row);
                if (it.getPartnerId() != null)
                    hitPartnerIds.add(it.getPartnerId());
            }
        }
        // 命中行 partner 名称批量预载（perf-ux plan 0325-2：消除逐行 getEntityById N+1）
        Map<String, String> nameByPartner = loadPartnerNames(hitPartnerIds);
        for (Map<String, Object> row : rows) {
            String pid = (String) row.get("partnerId");
            row.put("partnerName", pid != null ? nameByPartner.get(pid) : null);
        }
        return rows;
    }

    // ===================== helpers =====================

    /** in() 列表分块上限（perf-ux plan 0325-2 Decision-a 分块纪律）。 */
    private static final int IN_CLAUSE_CHUNK = 500;

    /** 批量预载 partner 名称（distinct id → name Map；id 超 500 分块查询后合并）。 */
    private Map<String, String> loadPartnerNames(Collection<String> partnerIds) {
        Map<String, String> nameByPartner = new HashMap<>();
        if (partnerIds == null || partnerIds.isEmpty())
            return nameByPartner;
        IEntityDao<ErpMdPartner> partnerDao = daoProvider.daoFor(ErpMdPartner.class);
        List<String> ids = new ArrayList<>(partnerIds);
        for (int i = 0; i < ids.size(); i += IN_CLAUSE_CHUNK) {
            List<String> chunk = ids.subList(i, Math.min(ids.size(), i + IN_CLAUSE_CHUNK));
            QueryBean q = new QueryBean();
            q.addFilter(in("id", chunk));
            for (ErpMdPartner p : partnerDao.findAllByQuery(q)) {
                nameByPartner.put((String) p.orm_id(), p.getName());
            }
        }
        return nameByPartner;
    }

    /** 单趟聚合结果（SUM + COUNT），消除全量实体物化。 */
    private static final class InvoiceAgg {
        BigDecimal sum = BigDecimal.ZERO;
        long count = 0L;
    }

    /**
     * DB 级聚合期内已过票发票（SUM(amountFunctional) + COUNT），按 businessDate 维度分组后内存汇总
     * （perf-ux plan 0325-2：原 findAllByQuery 全量实体物化内存求和改为 SQL 聚合，行数 ≤ 区间天数；
     * 维度分组规避无维度聚合被强制注入主键维度的平台坑）。null 业务日期行单独成组，SUM 天然计入，
     * 与原全量加载语义一致。
     */
    private InvoiceAgg aggPostedInvoicesInRange(LocalDate from, LocalDate to) {
        InvoiceAgg agg = new InvoiceAgg();
        QueryBean q = new QueryBean();
        q.setSourceName(ErpSalInvoice.class.getName());
        q.addFilter(eq("posted", Boolean.TRUE));
        if (from != null) q.addFilter(ge("businessDate", from));
        if (to != null) q.addFilter(le("businessDate", to));
        QueryFieldBean dim = QueryFieldBean.mainField("businessDate");
        QueryFieldBean sumAmt = QueryFieldBean.mainField("amountFunctional").sum().alias("salesAmount");
        QueryFieldBean cnt = QueryFieldBean.mainField("id").count().alias("invoiceCount");
        q.setFields(Arrays.asList(dim, sumAmt, cnt));
        List<Map<String, Object>> rows = ormTemplate.findListByQuery(q);
        for (Map<String, Object> row : rows) {
            agg.sum = agg.sum.add(DashboardUtil.nz(DashboardUtil.toBigDecimal(row.get("salesAmount"))));
            Object c = row.get("invoiceCount");
            if (c instanceof Number)
                agg.count += ((Number) c).longValue();
        }
        return agg;
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate)
            return (LocalDate) value;
        if (value instanceof java.sql.Date)
            return ((java.sql.Date) value).toLocalDate();
        if (value instanceof java.util.Date)
            return new java.sql.Date(((java.util.Date) value).getTime()).toLocalDate();
        return null;
    }

    private long countActiveOrders() {
        IEntityDao<ErpSalOrder> dao = daoProvider.daoFor(ErpSalOrder.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("approveStatus", ErpSalConstants.APPROVE_STATUS_APPROVED), ne("docStatus", ErpSalConstants.DOC_STATUS_CANCELLED)));
        return dao.countByQuery(q);
    }

    private BigDecimal sumArApOpen(String direction, IServiceContext context) {
        List<ErpFinArApItem> items = arApItemBiz.findOpenItems(direction, context);
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpFinArApItem it : items) {
            sum = sum.add(DashboardUtil.nz(it.getOpenAmountFunctional()));
        }
        return sum;
    }
}
