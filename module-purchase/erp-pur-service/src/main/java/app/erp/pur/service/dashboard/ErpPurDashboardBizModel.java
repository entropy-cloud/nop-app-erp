package app.erp.pur.service.dashboard;

import app.erp.fin.biz.IErpFinArApItemBiz;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.service.ErpFinConstants;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.dao.entity.ErpPurInvoiceLine;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.dao.entity.ErpPurReceiveLine;
import app.erp.pur.service.ErpPurConstants;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

/**
 * 采购看板聚合入口（{@code dashboards.md §2}）。服务型 BizObject（非实体聚合），
 * 注入 {@link IDaoProvider}/{@link IOrmTemplate} 经 {@link QueryBean} 过滤后内存聚合，
 * 镜像 {@code ErpFinDashboardBizModel} 范式。
 *
 * <p>KPI 口径：本期采购额取自 {@link ErpPurInvoice}（docStatus=ACTIVE Σ amountFunctional）；
 * 本期订单量取自 {@link ErpPurOrder}（docStatus=ACTIVE count）；
 * 应付余额跨域读 {@link ErpFinArApItem}（direction=PAYABLE），经 {@link IErpFinArApItemBiz} 注入（R 跨域只读）；
 * 到货及时率 = {@link ErpPurReceive}（businessDate ≤ 关联 order.deliveryDate）数 / 总 receive 数。
 *
 * <p>三单匹配差异预警口径对齐 {@code purchase/three-way-match.md §差异处理}：检测 ACTIVE 发票行
 * unitPrice 与关联 order line unitPrice 差异超 {@code erp-pur.match-price-tolerance}（默认 5%）的发票数。
 */
@BizModel("ErpPurDashboard")
public class ErpPurDashboardBizModel {

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

            List<ErpPurInvoice> invoices = loadActiveInvoicesInRange(from, to);
            BigDecimal purchaseAmount = BigDecimal.ZERO;
            Map<Long, BigDecimal> bySupplier = new HashMap<>();
            for (ErpPurInvoice inv : invoices) {
                purchaseAmount = purchaseAmount.add(nz(inv.getAmountFunctional()));
                if (inv.getSupplierId() != null) {
                    bySupplier.merge(inv.getSupplierId(), nz(inv.getAmountFunctional()), BigDecimal::add);
                }
            }

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
            List<ErpPurInvoice> invoices = loadActiveInvoicesInRange(from, today);
            Map<String, BigDecimal> amountByMonth = new LinkedHashMap<>();
            for (ErpPurInvoice inv : invoices) {
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
            List<ErpPurInvoice> invoices = loadActiveInvoicesInRange(null, null);
            Map<Long, BigDecimal> bySupplier = new LinkedHashMap<>();
            for (ErpPurInvoice inv : invoices) {
                Long sid = inv.getSupplierId();
                if (sid == null) continue;
                bySupplier.merge(sid, nz(inv.getAmountFunctional()), BigDecimal::add);
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            bySupplier.entrySet().stream()
                    .sorted(Map.Entry.<Long, BigDecimal>comparingByValue(Comparator.reverseOrder()))
                    .limit(topN)
                    .forEach(e -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("supplierId", e.getKey());
                        row.put("purchaseAmount", e.getValue());
                        rows.add(row);
                    });
            return rows;
        });
    }

    /**
     * 三单匹配差异预警：检测 ACTIVE 发票行 unitPrice 与关联 order line unitPrice 差异
     * 超 {@code erp-pur.match-price-tolerance}（默认 5%）的发票数。
     * 口径对齐 {@code purchase/three-way-match.md §价格差异}。
     */
    @BizQuery
    public List<Map<String, Object>> findThreeWayMatchDiffAlert(IServiceContext context) {
        BigDecimal configured = AppConfig.var(
                ErpPurConstants.CONFIG_MATCH_PRICE_TOLERANCE, new BigDecimal("0.05"));
        final BigDecimal tolerance = (configured == null || configured.signum() <= 0)
                ? new BigDecimal("0.05") : configured;
        return ormTemplate.runInSession(session -> {
            List<ErpPurInvoice> invoices = loadActiveInvoicesInRange(null, null);
            List<Map<String, Object>> rows = new ArrayList<>();
            for (ErpPurInvoice inv : invoices) {
                if (hasPriceVariance(inv.getId(), tolerance)) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("invoiceId", inv.getId());
                    row.put("invoiceCode", inv.getCode());
                    row.put("supplierId", inv.getSupplierId());
                    row.put("varianceType", "PRICE");
                    rows.add(row);
                }
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
        for (ErpFinArApItem it : items) {
            LocalDate base = it.getDueDate() != null ? it.getDueDate() : it.getBusinessDate();
            long age = base != null ? ChronoUnit.DAYS.between(base, today) : 0L;
            if (age < 0) age = 0L;
            if (age > daysThreshold) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("partnerId", it.getPartnerId());
                row.put("sourceBillCode", it.getSourceBillCode());
                row.put("openAmount", nz(it.getOpenAmountFunctional()));
                row.put("ageDays", age);
                rows.add(row);
            }
        }
        return rows;
    }

    // ===================== helpers =====================

    private List<ErpPurInvoice> loadActiveInvoicesInRange(LocalDate from, LocalDate to) {
        IEntityDao<ErpPurInvoice> dao = daoProvider.daoFor(ErpPurInvoice.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("docStatus", ErpPurConstants.DOC_STATUS_ACTIVE));
        if (from != null) q.addFilter(ge("businessDate", from));
        if (to != null) q.addFilter(le("businessDate", to));
        return dao.findAllByQuery(q);
    }

    private long countActiveOrders() {
        IEntityDao<ErpPurOrder> dao = daoProvider.daoFor(ErpPurOrder.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("docStatus", ErpPurConstants.DOC_STATUS_ACTIVE));
        return dao.findAllByQuery(q).size();
    }

    private BigDecimal sumArApOpen(String direction, IServiceContext context) {
        List<ErpFinArApItem> items = arApItemBiz.findOpenItems(direction, context);
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpFinArApItem it : items) {
            sum = sum.add(nz(it.getOpenAmountFunctional()));
        }
        return sum;
    }

    /** 到货及时率 = receive.businessDate ≤ 关联 order.deliveryDate 的 receive 数 / 总 receive 数。 */
    private double computeOnTimeRate() {
        IEntityDao<ErpPurReceive> rDao = daoProvider.daoFor(ErpPurReceive.class);
        QueryBean rq = new QueryBean();
        rq.addFilter(eq("docStatus", ErpPurConstants.DOC_STATUS_ACTIVE));
        List<ErpPurReceive> receives = rDao.findAllByQuery(rq);
        if (receives.isEmpty()) return 0.0;
        Map<Long, LocalDate> orderDeliveryMap = loadOrderDeliveryDates();
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

    private Map<Long, LocalDate> loadOrderDeliveryDates() {
        IEntityDao<ErpPurOrder> dao = daoProvider.daoFor(ErpPurOrder.class);
        Map<Long, LocalDate> map = new HashMap<>();
        for (ErpPurOrder o : dao.findAll()) {
            if (o.getDeliveryDate() != null) {
                map.put(o.getId(), o.getDeliveryDate());
            }
        }
        return map;
    }

    /** 检测发票是否存在价格差异行（发票行 unitPrice vs 关联 order line unitPrice）。 */
    private boolean hasPriceVariance(Long invoiceId, BigDecimal tolerance) {
        IEntityDao<ErpPurInvoiceLine> ilDao = daoProvider.daoFor(ErpPurInvoiceLine.class);
        QueryBean ilq = new QueryBean();
        ilq.addFilter(eq("invoiceId", invoiceId));
        List<ErpPurInvoiceLine> invLines = ilDao.findAllByQuery(ilq);
        if (invLines.isEmpty()) return false;
        Set<Long> receiveLineIds = new HashSet<>();
        for (ErpPurInvoiceLine il : invLines) {
            if (il.getReceiveLineId() != null) receiveLineIds.add(il.getReceiveLineId());
        }
        if (receiveLineIds.isEmpty()) return false;
        IEntityDao<ErpPurReceiveLine> rlDao = daoProvider.daoFor(ErpPurReceiveLine.class);
        QueryBean rlq = new QueryBean();
        rlq.addFilter(in("id", receiveLineIds));
        List<ErpPurReceiveLine> receiveLines = rlDao.findAllByQuery(rlq);
        Set<Long> orderLineIds = new HashSet<>();
        for (ErpPurReceiveLine rl : receiveLines) {
            if (rl.getOrderLineId() != null) orderLineIds.add(rl.getOrderLineId());
        }
        if (orderLineIds.isEmpty()) return false;
        IEntityDao<ErpPurOrderLine> olDao = daoProvider.daoFor(ErpPurOrderLine.class);
        QueryBean olq = new QueryBean();
        olq.addFilter(in("id", orderLineIds));
        Map<Long, BigDecimal> orderLinePrice = new HashMap<>();
        for (ErpPurOrderLine ol : olDao.findAllByQuery(olq)) {
            orderLinePrice.put(ol.getId(), ol.getUnitPrice());
        }
        Map<Long, BigDecimal> receiveToOrderPrice = new HashMap<>();
        for (ErpPurReceiveLine rl : receiveLines) {
            receiveToOrderPrice.put(rl.getId(), orderLinePrice.get(rl.getOrderLineId()));
        }
        for (ErpPurInvoiceLine il : invLines) {
            BigDecimal invPrice = il.getUnitPrice();
            BigDecimal orderPrice = receiveToOrderPrice.get(il.getReceiveLineId());
            if (invPrice == null || orderPrice == null || orderPrice.signum() == 0) continue;
            BigDecimal diff = invPrice.subtract(orderPrice).abs();
            BigDecimal ratio = diff.divide(orderPrice, 4, BigDecimal.ROUND_HALF_UP);
            if (ratio.compareTo(tolerance) > 0) {
                return true;
            }
        }
        return false;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
