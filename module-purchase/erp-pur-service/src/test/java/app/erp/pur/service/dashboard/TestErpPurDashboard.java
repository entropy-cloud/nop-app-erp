package app.erp.pur.service.dashboard;

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
import app.erp.pur.service.PurFrozenClockExtension;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 采购看板聚合（{@code ErpPurDashboard__*}）集成测试。覆盖：本期采购额/订单量/应付余额/到货及时率、
 * 12 月采购趋势、供应商 TOP10、三单匹配价格差异预警、应付超期预警（触发/不触发两路径）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurDashboard extends JunitAutoTestCase {

    @RegisterExtension
    static PurFrozenClockExtension frozenClock = new PurFrozenClockExtension();

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ErpPurDashboardBizModel dashboardBiz;

    @Test
    public void testKpiEmptyDatasetReturnsZeros() {
        Map<String, Object> kpi = dashboardBiz.getDashboardKpi(null, null, CTX);
        assertEquals(0, ((BigDecimal) kpi.get("purchaseAmount")).compareTo(BigDecimal.ZERO));
        assertEquals(0L, kpi.get("orderCount"));
        assertEquals(0, ((BigDecimal) kpi.get("apBalance")).compareTo(BigDecimal.ZERO));
        assertEquals(0.0, (double) kpi.get("onTimeRate"), 0.001);
    }

    @Test
    public void testKpiAggregationAndOnTimeRate() {
        ormTemplate.runInSession(() -> {
            seedSupplier("501", "S-A");
            seedSupplier("502", "S-B");
            seedInvoice("601", "501", new BigDecimal("100"), CoreMetrics.currentDate());
            seedInvoice("602", "502", new BigDecimal("200"), CoreMetrics.currentDate());
            // 订单 502 是 ACTIVE
            seedOrder("701", "501", ErpPurConstants.DOC_STATUS_ACTIVE, CoreMetrics.currentDate().plusDays(7));
            seedOrder("702", "502", ErpPurConstants.DOC_STATUS_ACTIVE, CoreMetrics.currentDate().plusDays(7));
            // 1 笔到货：onTime (receiveDate ≤ deliveryDate) / 1 total → onTimeRate=1.0
            seedReceive("801", "701", "501", CoreMetrics.currentDate().plusDays(5));
            // 应付余额 600（PAYABLE + OPEN）
            seedArApItem("901", "501", new BigDecimal("600"));
        });

        Map<String, Object> kpi = dashboardBiz.getDashboardKpi(null, null, CTX);
        assertEquals(0, ((BigDecimal) kpi.get("purchaseAmount")).compareTo(new BigDecimal("300")));
        assertEquals(2L, kpi.get("orderCount"));
        assertEquals(0, ((BigDecimal) kpi.get("apBalance")).compareTo(new BigDecimal("600")));
        assertEquals(1.0, (double) kpi.get("onTimeRate"), 0.001);
    }

    /**
     * P1-CK-pur-001 回归：真实单据状态（docStatus=DRAFT/SUBMITTED，approveStatus=APPROVED）下的订单
     * 计入 KPI。修复前主查询过滤 docStatus=ACTIVE 死状态（全域零 writer）→ orderCount/purchaseAmount 恒 0；
     * 修复后按 approveStatus=APPROVED 且非 CANCELLED 口径聚合。
     */
    @Test
    public void testKpiCountsApprovedOrdersWithRealDocStatus() {
        ormTemplate.runInSession(() -> {
            seedSupplier("521", "S-REAL");
            seedOrder("721", "521", ErpPurConstants.DOC_STATUS_DRAFT, CoreMetrics.currentDate().plusDays(7));
            seedOrder("722", "521", ErpPurConstants.DOC_STATUS_CANCELLED, CoreMetrics.currentDate().plusDays(7));
        });

        Map<String, Object> kpi = dashboardBiz.getDashboardKpi(null, null, CTX);
        assertEquals(1L, kpi.get("orderCount"),
                "真实 DRAFT+APPROVED 订单计入（修复前 ACTIVE 过滤恒 0）；CANCELLED 不计入");
    }

    @Test
    public void testOnTimeRateLateDelivery() {
        ormTemplate.runInSession(() -> {
            seedSupplier("511", "S-LATE");
            seedOrder("711", "511", ErpPurConstants.DOC_STATUS_ACTIVE, CoreMetrics.currentDate().plusDays(3));
            seedOrder("712", "511", ErpPurConstants.DOC_STATUS_ACTIVE, CoreMetrics.currentDate().plusDays(3));
            // 第一笔提前（onTime），第二笔迟到（晚于 deliveryDate）→ onTimeRate=0.5
            seedReceive("811", "711", "511", CoreMetrics.currentDate().plusDays(1));
            seedReceive("812", "712", "511", CoreMetrics.currentDate().plusDays(10));
        });
        Map<String, Object> kpi = dashboardBiz.getDashboardKpi(null, null, CTX);
        assertEquals(0.5, (double) kpi.get("onTimeRate"), 0.001, "一早一晚 → 0.5");
    }

    @Test
    public void testTrendMonthlySeries() {
        ormTemplate.runInSession(() -> {
            seedSupplier("521", "S-C");
            seedInvoice("621", "521", new BigDecimal("150"), CoreMetrics.currentDate().minusMonths(1));
            seedInvoice("622", "521", new BigDecimal("250"), CoreMetrics.currentDate());
        });
        List<Map<String, Object>> trend = dashboardBiz.getDashboardTrend(2, CTX);
        assertEquals(2, trend.size());
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> row : trend) {
            total = total.add((BigDecimal) row.get("purchaseAmount"));
        }
        assertEquals(0, total.compareTo(new BigDecimal("400")), "近 2 月采购合计 150+250=400");
    }

    @Test
    public void testVendorTopN() {
        ormTemplate.runInSession(() -> {
            seedSupplier("531", "S-TOP1");
            seedSupplier("532", "S-TOP2");
            seedInvoice("631", "531", new BigDecimal("300"), CoreMetrics.currentDate());
            seedInvoice("632", "532", new BigDecimal("100"), CoreMetrics.currentDate());
            seedInvoice("633", "531", new BigDecimal("50"), CoreMetrics.currentDate());
        });
        List<Map<String, Object>> top = dashboardBiz.findVendorTopN(10, CTX);
        assertEquals(2, top.size());
        assertEquals("531", top.get(0).get("supplierId"));
        assertEquals("S-TOP1", top.get(0).get("supplierName"), "供应商名称已解析");
        assertEquals(0, ((BigDecimal) top.get(0).get("purchaseAmount")).compareTo(new BigDecimal("350")));
    }

    @Test
    public void testThreeWayMatchPriceVariance() {
        ormTemplate.runInSession(() -> {
            seedSupplier("541", "S-PV");
            seedInvoice("641", "541", new BigDecimal("1000"), CoreMetrics.currentDate());
            // 发票行单价 110，关联 receiveLine 901 → orderLine 801 单价 100 → 差异 10% > 5% 阈值
            seedOrderLine("801", "701", new BigDecimal("100"));
            seedReceiveLine("901", "801", "801");
            seedInvoiceLine("1001", "641", "901", new BigDecimal("110"));
            // 第二条发票行无差异（价差 0%）— 仅作对照，hasPriceVariance 一旦命中即返回 true
        });
        List<Map<String, Object>> alerts = dashboardBiz.findThreeWayMatchDiffAlert(CTX);
        assertEquals(1, alerts.size(), "10% 价差 > 5% 容差 → 触发 1 条预警");
        assertEquals("641", alerts.get(0).get("invoiceId"));
    }

    @Test
    public void testPriceWithinToleranceNotTriggered() {
        // P2-CK-pur-007：默认 5（百分比）下 4% 价差不触发（修复前 ratio 口径 0.04 < 0.05 同样不触发，
        // 本用例守护量纲统一后边界语义：percent 口径下 4 ≤ 5 不告警）
        ormTemplate.runInSession(() -> {
            seedSupplier("542", "S-PV2");
            seedInvoice("642", "542", new BigDecimal("1000"), CoreMetrics.currentDate());
            seedOrderLine("802", "702", new BigDecimal("100"));
            seedReceiveLine("902", "802", "802");
            seedInvoiceLine("1002", "642", "902", new BigDecimal("104"));
        });
        List<Map<String, Object>> alerts = dashboardBiz.findThreeWayMatchDiffAlert(CTX);
        assertTrue(alerts.isEmpty(), "4% 价差 ≤ 默认 5% 容差 → 不触发");
    }

    @Test
    public void testZeroPriceToleranceAlertsAnyDiff() {
        // P2-CK-pur-007：0 = 零容差全量告警（与 matcher compareTo>0 语义一致，不再静默回退默认值）；
        // 1% 价差在零容差下也触发
        AppConfig.getConfigProvider().assignConfigValue(ErpPurConstants.CONFIG_MATCH_PRICE_TOLERANCE, "0");
        try {
            ormTemplate.runInSession(() -> {
                seedSupplier("543", "S-PV3");
                seedInvoice("643", "543", new BigDecimal("1000"), CoreMetrics.currentDate());
                seedOrderLine("803", "703", new BigDecimal("100"));
                seedReceiveLine("903", "803", "803");
                seedInvoiceLine("1003", "643", "903", new BigDecimal("101"));
            });
            List<Map<String, Object>> alerts = dashboardBiz.findThreeWayMatchDiffAlert(CTX);
            assertEquals(1, alerts.size(), "零容差下 1% 价差也触发（修复前 0 被静默回退 5% 而漏报）");
            assertEquals("643", alerts.get(0).get("invoiceId"));
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(ErpPurConstants.CONFIG_MATCH_PRICE_TOLERANCE, "5");
        }
    }

    @Test
    public void testApOverdueAlertDisabledByDefault() {
        ormTemplate.runInSession(() -> {
            seedSupplier("551", "S-OVD");
            seedArApItem("951", "551", new BigDecimal("1000"));
        });
        AppConfig.getConfigProvider().assignConfigValue(
                ErpPurConstants.CONFIG_DASH_PUR_AP_OVERDUE_DAYS,
                String.valueOf(ErpPurConstants.DEFAULT_DASH_PUR_AP_OVERDUE_DAYS));
        List<Map<String, Object>> alerts = dashboardBiz.findApOverdueAlert(CTX);
        assertTrue(alerts.isEmpty(), "天数阈值默认 0=关闭 → 不触发预警");
    }

    @Test
    public void testApOverdueAlertTriggers() {
        ormTemplate.runInSession(() -> {
            seedSupplier("561", "S-OVD2");
            seedArApItemWithDue("961", "561", new BigDecimal("800"),
                    CoreMetrics.currentDate().minusDays(100), CoreMetrics.currentDate().minusDays(100));
        });
        AppConfig.getConfigProvider().assignConfigValue(
                ErpPurConstants.CONFIG_DASH_PUR_AP_OVERDUE_DAYS, "90");
        try {
            List<Map<String, Object>> alerts = dashboardBiz.findApOverdueAlert(CTX);
            assertEquals(1, alerts.size(), "账龄 100>90 → 触发");
            assertEquals("561", alerts.get(0).get("partnerId"));
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpPurConstants.CONFIG_DASH_PUR_AP_OVERDUE_DAYS, "0");
        }
    }

    // ---------- helpers ----------

    private void seedSupplier(String id, String code) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner p = dao.newEntity();
        p.orm_propValue(1, id);
        p.setCode(code);
        p.setName(code);
        p.setPartnerType("VENDOR");
        p.setStatus("ACTIVE");
        p.setReceivableBalance(BigDecimal.ZERO);
        p.setPayableBalance(BigDecimal.ZERO);
        dao.saveEntity(p);
    }

    private void seedInvoice(String id, String supplierId, BigDecimal amount, LocalDate date) {
        IEntityDao<ErpPurInvoice> dao = daoProvider.daoFor(ErpPurInvoice.class);
        ErpPurInvoice inv = dao.newEntity();
        inv.orm_propValue(1, id);
        inv.setCode("PI-" + id);
        inv.setOrgId("1");
        inv.setSupplierId(supplierId);
        inv.setInvoiceNo("PINV-" + id);
        inv.setBusinessDate(date);
        inv.setCurrencyId("1");
        inv.setExchangeRate(BigDecimal.ONE);
        inv.setAmountSource(amount);
        inv.setAmountFunctional(amount);
        inv.setTotalAmount(amount);
        inv.setDocStatus(ErpPurConstants.DOC_STATUS_ACTIVE);
        inv.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        dao.saveEntity(inv);
    }

    private void seedOrder(String id, String supplierId, String docStatus, LocalDate deliveryDate) {
        IEntityDao<ErpPurOrder> dao = daoProvider.daoFor(ErpPurOrder.class);
        ErpPurOrder o = dao.newEntity();
        o.orm_propValue(1, id);
        o.setCode("PO-" + id);
        o.setOrgId("1");
        o.setSupplierId(supplierId);
        o.setBusinessDate(CoreMetrics.currentDate());
        o.setDeliveryDate(deliveryDate);
        o.setCurrencyId("1");
        o.setExchangeRate(BigDecimal.ONE);
        o.setDocStatus(docStatus);
        o.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        dao.saveEntity(o);
    }

    private void seedOrderLine(String id, String orderId, BigDecimal unitPrice) {
        IEntityDao<ErpPurOrderLine> dao = daoProvider.daoFor(ErpPurOrderLine.class);
        ErpPurOrderLine l = dao.newEntity();
        l.orm_propValue(1, id);
        l.setOrderId(orderId);
        l.setLineNo(1);
        l.setMaterialId("1");
        l.setUoMId("1");
        l.setQuantity(BigDecimal.TEN);
        l.setUnitPrice(unitPrice);
        l.setAmount(unitPrice.multiply(BigDecimal.TEN));
        dao.saveEntity(l);
    }

    private void seedReceive(String id, String orderId, String supplierId, LocalDate businessDate) {
        IEntityDao<ErpPurReceive> dao = daoProvider.daoFor(ErpPurReceive.class);
        ErpPurReceive r = dao.newEntity();
        r.orm_propValue(1, id);
        r.setCode("PR-" + id);
        r.setOrgId("1");
        r.setOrderId(orderId);
        r.setSupplierId(supplierId);
        r.setWarehouseId("1");
        r.setBusinessDate(businessDate);
        r.setCurrencyId("1");
        r.setExchangeRate(BigDecimal.ONE);
        r.setDocStatus(ErpPurConstants.DOC_STATUS_ACTIVE);
        r.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        dao.saveEntity(r);
    }

    private void seedReceiveLine(String id, String receiveId, String orderLineId) {
        IEntityDao<ErpPurReceiveLine> dao = daoProvider.daoFor(ErpPurReceiveLine.class);
        ErpPurReceiveLine l = dao.newEntity();
        l.orm_propValue(1, id);
        l.setReceiveId(receiveId);
        l.setOrderLineId(orderLineId);
        l.setLineNo(1);
        l.setMaterialId("1");
        l.setUoMId("1");
        l.setQuantity(BigDecimal.TEN);
        l.setUnitPrice(new BigDecimal("100"));
        dao.saveEntity(l);
    }

    private void seedInvoiceLine(String id, String invoiceId, String receiveLineId, BigDecimal unitPrice) {
        IEntityDao<ErpPurInvoiceLine> dao = daoProvider.daoFor(ErpPurInvoiceLine.class);
        ErpPurInvoiceLine l = dao.newEntity();
        l.orm_propValue(1, id);
        l.setInvoiceId(invoiceId);
        l.setReceiveLineId(receiveLineId);
        l.setLineNo(1);
        l.setMaterialId("1");
        l.setUoMId("1");
        l.setQuantity(BigDecimal.TEN);
        l.setUnitPrice(unitPrice);
        dao.saveEntity(l);
    }

    private void seedArApItem(String id, String partnerId, BigDecimal openAmount) {
        seedArApItemWithDue(id, partnerId, openAmount, CoreMetrics.currentDate(), CoreMetrics.currentDate());
    }

    private void seedArApItemWithDue(String id, String partnerId, BigDecimal openAmount,
                                     LocalDate businessDate, LocalDate dueDate) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        ErpFinArApItem it = dao.newEntity();
        it.orm_propValue(1, id);
        it.setCode("PUR-AP-" + id);
        it.setOrgId("1");
        it.setAcctSchemaId("1");
        it.setDirection(ErpFinConstants.DIRECTION_PAYABLE);
        it.setPartnerId(partnerId);
        it.setSourceBillType(ErpFinConstants.SOURCE_BILL_AP_INVOICE);
        it.setSourceBillCode("PUR-BILL-" + id);
        it.setBusinessDate(businessDate);
        it.setDueDate(dueDate);
        it.setCurrencyId("1");
        it.setExchangeRate(BigDecimal.ONE);
        it.setAmountSource(openAmount);
        it.setAmountFunctional(openAmount);
        it.setSettledAmountSource(BigDecimal.ZERO);
        it.setSettledAmountFunctional(BigDecimal.ZERO);
        it.setOpenAmountSource(openAmount);
        it.setOpenAmountFunctional(openAmount);
        it.setStatus(ErpFinConstants.AR_AP_STATUS_OPEN);
        dao.saveEntity(it);
    }
}
