package app.erp.pur.service.dashboard;

import app.erp.fin.biz.IErpFinArApItemBiz;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.dao.entity.ErpPurInvoiceLine;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.dao.entity.ErpPurReceiveLine;
import app.erp.pur.service.ErpPurConstants;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Description;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import io.nop.api.core.beans.query.QueryFieldBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
 * 采购看板聚合入口（{@code dashboards.md §2}）。服务型 BizObject（非实体聚合），
 * 注入 {@link IDaoProvider}/{@link IOrmTemplate} 经 {@link QueryBean} 过滤后内存聚合，
 * 镜像 {@code ErpFinDashboardBizModel} 范式。
 *
 * <p>KPI 口径：本期采购额取自 {@link ErpPurInvoice}（approveStatus=APPROVED 且 docStatus≠CANCELLED
 * Σ amountFunctional）；本期订单量取自 {@link ErpPurOrder}（同口径 count）；
 * 应付余额跨域读 {@link ErpFinArApItem}（direction=PAYABLE），经 {@link IErpFinArApItemBiz} 注入（R 跨域只读）；
 * 到货及时率 = {@link ErpPurReceive}（businessDate ≤ 关联 order.deliveryDate）数 / 总 receive 数。
 *
 * <p>三单匹配差异预警口径对齐 {@code purchase/three-way-match.md §差异处理}：检测在账（approveStatus=APPROVED
 * 且 docStatus≠CANCELLED）发票行 unitPrice 与关联 order line unitPrice 差异超
 * {@code erp-pur.match-price-tolerance}（默认 5%）的发票数。
 */
@BizModel("ErpPurDashboard")
public class ErpPurDashboardBizModel {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinArApItemBiz arApItemBiz;

    @Description("采购看板 KPI（采购订单额、应付余额、到货及时率、三单匹配状态分布）")
    @BizQuery
    public Map<String, Object> getDashboardKpi(@Optional @Name("startDate") LocalDate startDate,
                                                @Optional @Name("endDate") LocalDate endDate,
                                                IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            LocalDate today = CoreMetrics.currentDate();
            LocalDate from = startDate != null ? startDate : today.withDayOfMonth(1);
            LocalDate to = endDate != null ? endDate : today;

            BigDecimal purchaseAmount = sumActiveInvoiceAmounts(from, to);

            long orderCount = countActiveOrders();
            BigDecimal apBalance = sumArApOpen(ErpFinConstants.DIRECTION_PAYABLE, context);
            double onTimeRate = computeOnTimeRate();

            Map<String, Object> kpi = new LinkedHashMap<>();
            kpi.put("startDate", from);
            kpi.put("endDate", to);
            kpi.put("purchaseAmount", purchaseAmount);
            kpi.put("orderCount", orderCount);
            kpi.put("apBalance", apBalance);
            kpi.put("onTimeRate", onTimeRate);
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
            // 改为日期维度分组聚合，行数 ≤ 区间天数；null 业务日期行跳过，与原语义一致）。
            QueryBean q = new QueryBean();
            q.setSourceName(ErpPurInvoice.class.getName());
            q.addFilter(and(eq("approveStatus", ErpPurConstants.APPROVE_STATUS_APPROVED), ne("docStatus", ErpPurConstants.DOC_STATUS_CANCELLED)));
            q.addFilter(ge("businessDate", from));
            q.addFilter(le("businessDate", today));
            QueryFieldBean dim = QueryFieldBean.mainField("businessDate");
            QueryFieldBean sumAmt = QueryFieldBean.mainField("amountFunctional").sum().alias("purchaseAmount");
            q.setFields(Arrays.asList(dim, sumAmt));
            List<Map<String, Object>> aggRows = ormTemplate.findListByQuery(q);

            Map<String, BigDecimal> amountByMonth = new LinkedHashMap<>();
            for (Map<String, Object> row : aggRows) {
                Object d = row.get("businessDate");
                if (d == null) continue;
                LocalDate date = toLocalDate(d);
                String key = date.getYear() + "-" + String.format("%02d", date.getMonthValue());
                amountByMonth.merge(key, DashboardUtil.nz(DashboardUtil.toBigDecimal(row.get("purchaseAmount"))), BigDecimal::add);
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                LocalDate m = from.plusMonths(i);
                String key = m.getYear() + "-" + String.format("%02d", m.getMonthValue());
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("month", key);
                row.put("purchaseAmount", amountByMonth.getOrDefault(key, BigDecimal.ZERO));
                rows.add(row);
            }
            return rows;
        });
    }

    /** 供应商 TOP N（按采购额降序）。 */
    @BizQuery
    public List<Map<String, Object>> findVendorTopN(@Optional @Name("limit") Integer limit,
                                                     IServiceContext context) {
        int topN = limit == null || limit <= 0 ? 10 : limit;
        return ormTemplate.runInSession(session -> {
            // DB 级 GROUP BY supplierId 聚合（perf-ux plan 0325-2：原全表发票物化内存分组改为
            // SQL 聚合；行数=供应商数，天然有界；DB 端 ORDER BY 聚合列支持不确定，内存排序兜底）。
            QueryBean q = new QueryBean();
            q.setSourceName(ErpPurInvoice.class.getName());
            q.addFilter(and(eq("approveStatus", ErpPurConstants.APPROVE_STATUS_APPROVED), ne("docStatus", ErpPurConstants.DOC_STATUS_CANCELLED)));
            QueryFieldBean dim = QueryFieldBean.mainField("supplierId");
            QueryFieldBean sumAmt = QueryFieldBean.mainField("amountFunctional").sum().alias("purchaseAmount");
            q.setFields(Arrays.asList(dim, sumAmt));
            List<Map<String, Object>> aggRows = ormTemplate.findListByQuery(q);
            Map<String, BigDecimal> bySupplier = new LinkedHashMap<>();
            for (Map<String, Object> row : aggRows) {
                Object sid = row.get("supplierId");
                if (sid == null) continue;
                bySupplier.merge((String) sid, DashboardUtil.nz(DashboardUtil.toBigDecimal(row.get("purchaseAmount"))), BigDecimal::add);
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            bySupplier.entrySet().stream()
                    .sorted(Map.Entry.<String, BigDecimal>comparingByValue(Comparator.reverseOrder()))
                    .limit(topN)
                    .forEach(e -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("supplierId", e.getKey());
                        row.put("purchaseAmount", e.getValue());
                        rows.add(row);
                    });
            Map<String, String> nameCache = new HashMap<>();
            IEntityDao<ErpMdPartner> partnerDao = daoProvider.daoFor(ErpMdPartner.class);
            for (Map<String, Object> r : rows) {
                String sid = (String) r.get("supplierId");
                if (sid == null) continue;
                String name = nameCache.get(sid);
                if (name == null) {
                    ErpMdPartner p = partnerDao.getEntityById(sid);
                    name = p != null ? p.getName() : null;
                    nameCache.put(sid, name);
                }
                r.put("supplierName", name);
            }
            return rows;
        });
    }

    /**
     * 三单匹配差异预警：检测在账（approveStatus=APPROVED 且 docStatus≠CANCELLED）发票行 unitPrice
     * 与关联 order line unitPrice 差异超 {@code erp-pur.match-price-tolerance}（默认 5%）的发票数。
     * 口径对齐 {@code purchase/three-way-match.md §价格差异}（P3-CK-pur-016-r3 javadoc 口径同步）。
     */
    @BizQuery
    public List<Map<String, Object>> findThreeWayMatchDiffAlert(IServiceContext context) {
        // P2-CK-pur-007：量纲与 ThreeWayMatcher#priceTolerancePercent 统一为百分比（默认 5 = 5%）。
        // 0（或退化负值）= 零容差全量告警（与 matcher compareTo>0 语义一致），不再静默回退默认值。
        BigDecimal configured = AppConfig.var(
                ErpPurConstants.CONFIG_MATCH_PRICE_TOLERANCE, new BigDecimal("5"));
        final BigDecimal tolerance = configured != null ? configured : new BigDecimal("5");
        return ormTemplate.runInSession(session -> {
            // 批量预载三类行 + 内存 Map 比对（perf-ux plan 0325-2：原逐发票 hasPriceVariance
            // 三连查 = 每发票 3-4 次往返的 N+1，改 in() 分块批量化；逐行比较逻辑逐字平移，纯等价变换）。
            // 发票头改投影查询（id/code/supplierId 三列，perf-ux 结束审计 Blocker-1 整改：
            // 消除对交易大表的无投影全量实体物化——命中行仅需头三字段）。
            List<Map<String, Object>> invoiceHeads = loadActiveInvoiceHeadProjections();
            Map<String, List<ErpPurInvoiceLine>> linesByInvoice = loadInvoiceLinesByInvoice(invoiceHeads);
            Set<String> receiveLineIds = new HashSet<>();
            for (List<ErpPurInvoiceLine> lines : linesByInvoice.values()) {
                for (ErpPurInvoiceLine il : lines) {
                    if (il.getReceiveLineId() != null) receiveLineIds.add(il.getReceiveLineId());
                }
            }
            // receiveLineId → orderLineId
            Map<String, String> orderLineIdByReceiveLine = new HashMap<>();
            IEntityDao<ErpPurReceiveLine> rlDao = daoProvider.daoFor(ErpPurReceiveLine.class);
            for (java.util.Collection<String> chunkId : chunkIds(receiveLineIds)) {
                QueryBean rlq = new QueryBean();
                rlq.addFilter(in("id", chunkId));
                for (ErpPurReceiveLine rl : rlDao.findAllByQuery(rlq)) {
                    if (rl.getOrderLineId() != null)
                        orderLineIdByReceiveLine.put(rl.getId(), rl.getOrderLineId());
                }
            }
            // orderLineId → unitPrice
            Map<String, BigDecimal> orderLinePrice = new HashMap<>();
            IEntityDao<ErpPurOrderLine> olDao = daoProvider.daoFor(ErpPurOrderLine.class);
            for (java.util.Collection<String> chunkId : chunkIds(orderLineIdByReceiveLine.values())) {
                QueryBean olq = new QueryBean();
                olq.addFilter(in("id", chunkId));
                for (ErpPurOrderLine ol : olDao.findAllByQuery(olq)) {
                    orderLinePrice.put(ol.getId(), ol.getUnitPrice());
                }
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Map<String, Object> head : invoiceHeads) {
                String invoiceId = (String) head.get("id");
                if (hasPriceVariance(linesByInvoice.get(invoiceId), orderLineIdByReceiveLine, orderLinePrice, tolerance)) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("invoiceId", invoiceId);
                    row.put("invoiceCode", head.get("code"));
                    row.put("supplierId", head.get("supplierId"));
                    row.put("varianceType", "PRICE");
                    rows.add(row);
                }
            }
            // 命中行 supplier 名称批量预载（消除循环内逐行 getEntityById）
            Set<String> supplierIds = new LinkedHashSet<>();
            for (Map<String, Object> row : rows) {
                String sid = (String) row.get("supplierId");
                if (sid != null) supplierIds.add(sid);
            }
            Map<String, String> nameBySupplier = loadPartnerNames(supplierIds);
            for (Map<String, Object> row : rows) {
                String sid = (String) row.get("supplierId");
                row.put("supplierName", sid != null ? nameBySupplier.get(sid) : null);
            }
            return rows;
        });
    }

    /**
     * 应付超期预警：账龄 > 阈值天数。阈值 ≤0 时不触发预警（默认关闭）。
     */
    @BizQuery
    public List<Map<String, Object>> findApOverdueAlert(IServiceContext context) {
        int daysThreshold = AppConfig.var(
                ErpPurConstants.CONFIG_DASH_PUR_AP_OVERDUE_DAYS,
                ErpPurConstants.DEFAULT_DASH_PUR_AP_OVERDUE_DAYS);
        if (daysThreshold <= 0) {
            return Collections.emptyList();
        }
        LocalDate today = CoreMetrics.currentDate();
        List<ErpFinArApItem> items = arApItemBiz.findOpenItems(
                ErpFinConstants.DIRECTION_PAYABLE, context);
        List<Map<String, Object>> rows = new ArrayList<>();
        Set<String> hitPartnerIds = new LinkedHashSet<>();
        for (ErpFinArApItem it : items) {
            LocalDate base = it.getDueDate() != null ? it.getDueDate() : it.getBusinessDate();
            long age = base != null ? ChronoUnit.DAYS.between(base, today) : 0L;
            if (age < 0) age = 0L;
            if (age > daysThreshold) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("partnerId", it.getPartnerId());
                row.put("sourceBillCode", it.getSourceBillCode());
                row.put("openAmount", DashboardUtil.nz(it.getOpenAmountFunctional()));
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

    /** 到货及时率订单扫描硬上限（perf-ux plan 0325-2 Decision：内存比对算法逐位保留；
     * 超 cap 订单的 receive 计入分母但永不计入分子 = 准时率下偏（保守方向），语义登记于 plan。 */
    private static final int ON_TIME_RATE_SCAN_CAP = 5000;

    /** 批量预载 partner 名称（distinct id → name Map；id 超 500 分块查询后合并）。 */
    private Map<String, String> loadPartnerNames(Collection<String> partnerIds) {
        Map<String, String> nameByPartner = new HashMap<>();
        if (partnerIds == null || partnerIds.isEmpty())
            return nameByPartner;
        IEntityDao<ErpMdPartner> partnerDao = daoProvider.daoFor(ErpMdPartner.class);
        for (java.util.Collection<String> chunkId : chunkIds(partnerIds)) {
            QueryBean q = new QueryBean();
            q.addFilter(in("id", chunkId));
            for (ErpMdPartner p : partnerDao.findAllByQuery(q)) {
                nameByPartner.put((String) p.orm_id(), p.getName());
            }
        }
        return nameByPartner;
    }

    /** 分块 IN 查询辅助：把 id 集合切成 ≤500 的子集合列表。 */
    private static List<java.util.Collection<String>> chunkIds(Collection<String> ids) {
        List<String> list = new ArrayList<>(ids);
        List<java.util.Collection<String>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += IN_CLAUSE_CHUNK) {
            chunks.add(new HashSet<>(list.subList(i, Math.min(list.size(), i + IN_CLAUSE_CHUNK))));
        }
        return chunks;
    }

    /** DB 级聚合期内活跃发票总额（GROUP BY businessDate 维度分组后内存汇总，行数 ≤ 区间天数）。 */
    private BigDecimal sumActiveInvoiceAmounts(LocalDate from, LocalDate to) {
        QueryBean q = new QueryBean();
        q.setSourceName(ErpPurInvoice.class.getName());
        q.addFilter(and(eq("approveStatus", ErpPurConstants.APPROVE_STATUS_APPROVED), ne("docStatus", ErpPurConstants.DOC_STATUS_CANCELLED)));
        if (from != null) q.addFilter(ge("businessDate", from));
        if (to != null) q.addFilter(le("businessDate", to));
        QueryFieldBean dim = QueryFieldBean.mainField("businessDate");
        QueryFieldBean sumAmt = QueryFieldBean.mainField("amountFunctional").sum().alias("purchaseAmount");
        q.setFields(Arrays.asList(dim, sumAmt));
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> row : ormTemplate.findListByQuery(q)) {
            total = total.add(DashboardUtil.nz(DashboardUtil.toBigDecimal(row.get("purchaseAmount"))));
        }
        return total;
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

    /** 活跃发票头投影（id/code/supplierId 三列，无聚合普通投影；替代全量实体物化）。 */
    private List<Map<String, Object>> loadActiveInvoiceHeadProjections() {
        QueryBean q = new QueryBean();
        q.setSourceName(ErpPurInvoice.class.getName());
        q.addFilter(and(eq("approveStatus", ErpPurConstants.APPROVE_STATUS_APPROVED), ne("docStatus", ErpPurConstants.DOC_STATUS_CANCELLED)));
        q.setFields(Arrays.asList(
                QueryFieldBean.mainField("id"),
                QueryFieldBean.mainField("code"),
                QueryFieldBean.mainField("supplierId")));
        return ormTemplate.findListByQuery(q);
    }

    /** 批量预载全部活跃发票的发票行（in(invoiceId) 分块），按 invoiceId 分组。 */
    private Map<String, List<ErpPurInvoiceLine>> loadInvoiceLinesByInvoice(List<Map<String, Object>> invoiceHeads) {
        Set<String> invoiceIds = new HashSet<>();
        for (Map<String, Object> head : invoiceHeads) {
            invoiceIds.add((String) head.get("id"));
        }
        Map<String, List<ErpPurInvoiceLine>> byInvoice = new HashMap<>();
        IEntityDao<ErpPurInvoiceLine> ilDao = daoProvider.daoFor(ErpPurInvoiceLine.class);
        for (java.util.Collection<String> chunkId : chunkIds(invoiceIds)) {
            QueryBean q = new QueryBean();
            q.addFilter(in("invoiceId", chunkId));
            for (ErpPurInvoiceLine il : ilDao.findAllByQuery(q)) {
                byInvoice.computeIfAbsent(il.getInvoiceId(), k -> new ArrayList<>()).add(il);
            }
        }
        return byInvoice;
    }

    private long countActiveOrders() {
        IEntityDao<ErpPurOrder> dao = daoProvider.daoFor(ErpPurOrder.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("approveStatus", ErpPurConstants.APPROVE_STATUS_APPROVED), ne("docStatus", ErpPurConstants.DOC_STATUS_CANCELLED)));
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

    /** 到货及时率 = receive.businessDate ≤ 关联 order.deliveryDate 的 receive 数 / 总 receive 数。 */
    private double computeOnTimeRate() {
        IEntityDao<ErpPurReceive> rDao = daoProvider.daoFor(ErpPurReceive.class);
        QueryBean rq = new QueryBean();
        rq.addFilter(and(eq("approveStatus", ErpPurConstants.APPROVE_STATUS_APPROVED), ne("docStatus", ErpPurConstants.DOC_STATUS_CANCELLED)));
        List<ErpPurReceive> receives = rDao.findAllByQuery(rq);
        if (receives.isEmpty()) return 0.0;
        Map<String, LocalDate> orderDeliveryMap = loadOrderDeliveryDates();
        int onTime = 0;
        int totalWithOrder = 0;
        for (ErpPurReceive r : receives) {
            if (r.getOrderId() == null) continue;
            totalWithOrder++;
            LocalDate delivery = orderDeliveryMap.get(r.getOrderId());
            if (delivery == null) continue;
            LocalDate receiveDate = r.getBusinessDate();
            if (receiveDate != null && !receiveDate.isAfter(delivery)) {
                onTime++;
            }
        }
        return totalWithOrder > 0 ? (double) onTime / (double) totalWithOrder : 0.0;
    }

    /** 收集订单 id → deliveryDate（类 C：单字段收集，带硬上限的受限扫描）。 */
    private Map<String, LocalDate> loadOrderDeliveryDates() {
        IEntityDao<ErpPurOrder> dao = daoProvider.daoFor(ErpPurOrder.class);
        QueryBean q = new QueryBean();
        q.setLimit(ON_TIME_RATE_SCAN_CAP);
        Map<String, LocalDate> map = new HashMap<>();
        for (ErpPurOrder o : dao.findAllByQuery(q)) {
            if (o.getDeliveryDate() != null) {
                map.put(o.getId(), o.getDeliveryDate());
            }
        }
        return map;
    }

    /** 检测发票是否存在价格差异行（批量预载 Map 版本，逐行比较逻辑与原逐发票查询版逐字等价）。 */
    private boolean hasPriceVariance(List<ErpPurInvoiceLine> invLines,
                                     Map<String, String> orderLineIdByReceiveLine,
                                     Map<String, BigDecimal> orderLinePrice,
                                     BigDecimal tolerance) {
        if (invLines == null || invLines.isEmpty()) return false;
        boolean hasReceiveLine = false;
        for (ErpPurInvoiceLine il : invLines) {
            if (il.getReceiveLineId() != null) {
                hasReceiveLine = true;
                break;
            }
        }
        if (!hasReceiveLine) return false;
        for (ErpPurInvoiceLine il : invLines) {
            BigDecimal invPrice = il.getUnitPrice();
            String orderLineId = orderLineIdByReceiveLine.get(il.getReceiveLineId());
            BigDecimal orderPrice = orderLineId != null ? orderLinePrice.get(orderLineId) : null;
            if (invPrice == null || orderPrice == null || orderPrice.signum() == 0) continue;
            // P2-CK-pur-007：与 ThreeWayMatcher#priceDiffPercent 同式（×100 百分比，HALF_UP 4 位）
            BigDecimal diff = invPrice.subtract(orderPrice).abs();
            BigDecimal percent = diff.multiply(BigDecimal.valueOf(100))
                    .divide(orderPrice, 4, BigDecimal.ROUND_HALF_UP);
            if (percent.compareTo(tolerance) > 0) {
                return true;
            }
        }
        return false;
    }
}
