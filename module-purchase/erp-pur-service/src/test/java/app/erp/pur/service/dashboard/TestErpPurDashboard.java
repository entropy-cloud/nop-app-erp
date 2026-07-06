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
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.config.AppConfig;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

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
            seedSupplier(501L, "S-A");
            seedSupplier(502L, "S-B");
            seedInvoice(601L, 501L, new BigDecimal("100"), LocalDate.now());
            seedInvoice(602L, 502L, new BigDecimal("200"), LocalDate.now());
            // 订单 502 是 ACTIVE
            seedOrder(701L, 501L, ErpPurConstants.DOC_STATUS_ACTIVE, LocalDate.now().plusDays(7));
            seedOrder(702L, 502L, ErpPurConstants.DOC_STATUS_ACTIVE, LocalDate.now().plusDays(7));
            // 1 笔到货：onTime (receiveDate ≤ deliveryDate) / 1 total → onTimeRate=1.0
            seedReceive(801L, 701L, 501L, LocalDate.now().plusDays(5));
            // 应付余额 600（PAYABLE + OPEN）
            seedArApItem(901L, 501L, new BigDecimal("600"));
        });

        Map<String, Object> kpi = dashboardBiz.getDashboardKpi(null, null, CTX);
        assertEquals(0, ((BigDecimal) kpi.get("purchaseAmount")).compareTo(new BigDecimal("300")));
        assertEquals(2L, kpi.get("orderCount"));
        assertEquals(0, ((BigDecimal) kpi.get("apBalance")).compareTo(new BigDecimal("600")));
        assertEquals(1.0, (double) kpi.get("onTimeRate"), 0.001);
    }

    @Test
    public void testOnTimeRateLateDelivery() {
        ormTemplate.runInSession(() -> {
            seedSupplier(511L, "S-LATE");
            seedOrder(711L, 511L, ErpPurConstants.DOC_STATUS_ACTIVE, LocalDate.now().plusDays(3));
            seedOrder(712L, 511L, ErpPurConstants.DOC_STATUS_ACTIVE, LocalDate.now().plusDays(3));
            // 第一笔提前（onTime），第二笔迟到（晚于 deliveryDate）→ onTimeRate=0.5
            seedReceive(811L, 711L, 511L, LocalDate.now().plusDays(1));
            seedReceive(812L, 712L, 511L, LocalDate.now().plusDays(10));
        });
        Map<String, Object> kpi = dashboardBiz.getDashboardKpi(null, null, CTX);
        assertEquals(0.5, (double) kpi.get("onTimeRate"), 0.001, "一早一晚 → 0.5");
    }

    @Test
    public void testTrendMonthlySeries() {
        ormTemplate.runInSession(() -> {
            seedSupplier(521L, "S-C");
            seedInvoice(621L, 521L, new BigDecimal("150"), LocalDate.now().minusMonths(1));
            seedInvoice(622L, 521L, new BigDecimal("250"), LocalDate.now());
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
            seedSupplier(531L, "S-TOP1");
            seedSupplier(532L, "S-TOP2");
            seedInvoice(631L, 531L, new BigDecimal("300"), LocalDate.now());
            seedInvoice(632L, 532L, new BigDecimal("100"), LocalDate.now());
            seedInvoice(633L, 531L, new BigDecimal("50"), LocalDate.now());
        });
        List<Map<String, Object>> top = dashboardBiz.findVendorTopN(10, CTX);
        assertEquals(2, top.size());
        assertEquals(531L, top.get(0).get("supplierId"));
        assertEquals(0, ((BigDecimal) top.get(0).get("purchaseAmount")).compareTo(new BigDecimal("350")));
    }

    @Test
    public void testThreeWayMatchPriceVariance() {
        ormTemplate.runInSession(() -> {
            seedSupplier(541L, "S-PV");
            seedInvoice(641L, 541L, new BigDecimal("1000"), LocalDate.now());
            // 发票行单价 110，关联 receiveLine 901 → orderLine 801 单价 100 → 差异 10% > 5% 阈值
            seedOrderLine(801L, 701L, new BigDecimal("100"));
            seedReceiveLine(901L, 801L, 801L);
            seedInvoiceLine(1001L, 641L, 901L, new BigDecimal("110"));
            // 第二条发票行无差异（价差 0%）— 仅作对照，hasPriceVariance 一旦命中即返回 true
        });
        List<Map<String, Object>> alerts = dashboardBiz.findThreeWayMatchDiffAlert(CTX);
        assertEquals(1, alerts.size(), "10% 价差 > 5% 容差 → 触发 1 条预警");
        assertEquals(641L, alerts.get(0).get("invoiceId"));
    }

    @Test
    public void testApOverdueAlertDisabledByDefault() {
        ormTemplate.runInSession(() -> {
            seedSupplier(551L, "S-OVD");
            seedArApItem(951L, 551L, new BigDecimal("1000"));
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
            seedSupplier(561L, "S-OVD2");
            seedArApItemWithDue(961L, 561L, new BigDecimal("800"),
                    LocalDate.now().minusDays(100), LocalDate.now().minusDays(100));
        });
        AppConfig.getConfigProvider().assignConfigValue(
                ErpPurConstants.CONFIG_DASH_PUR_AP_OVERDUE_DAYS, "90");
        try {
            List<Map<String, Object>> alerts = dashboardBiz.findApOverdueAlert(CTX);
            assertEquals(1, alerts.size(), "账龄 100>90 → 触发");
            assertEquals(561L, alerts.get(0).get("partnerId"));
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpPurConstants.CONFIG_DASH_PUR_AP_OVERDUE_DAYS, "0");
        }
    }

    // ---------- helpers ----------

    private void seedSupplier(long id, String code) {
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

    private void seedInvoice(long id, long supplierId, BigDecimal amount, LocalDate date) {
        IEntityDao<ErpPurInvoice> dao = daoProvider.daoFor(ErpPurInvoice.class);
        ErpPurInvoice inv = dao.newEntity();
        inv.orm_propValue(1, id);
        inv.setCode("PI-" + id);
        inv.setOrgId(1L);
        inv.setSupplierId(supplierId);
        inv.setInvoiceNo("PINV-" + id);
        inv.setBusinessDate(date);
        inv.setCurrencyId(1L);
        inv.setExchangeRate(BigDecimal.ONE);
        inv.setAmountSource(amount);
        inv.setAmountFunctional(amount);
        inv.setTotalAmount(amount);
        inv.setDocStatus(ErpPurConstants.DOC_STATUS_ACTIVE);
        inv.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        dao.saveEntity(inv);
    }

    private void seedOrder(long id, long supplierId, String docStatus, LocalDate deliveryDate) {
        IEntityDao<ErpPurOrder> dao = daoProvider.daoFor(ErpPurOrder.class);
        ErpPurOrder o = dao.newEntity();
        o.orm_propValue(1, id);
        o.setCode("PO-" + id);
        o.setOrgId(1L);
        o.setSupplierId(supplierId);
        o.setBusinessDate(LocalDate.now());
        o.setDeliveryDate(deliveryDate);
        o.setCurrencyId(1L);
        o.setExchangeRate(BigDecimal.ONE);
        o.setDocStatus(docStatus);
        o.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        dao.saveEntity(o);
    }

    private void seedOrderLine(long id, long orderId, BigDecimal unitPrice) {
        IEntityDao<ErpPurOrderLine> dao = daoProvider.daoFor(ErpPurOrderLine.class);
        ErpPurOrderLine l = dao.newEntity();
        l.orm_propValue(1, id);
        l.setOrderId(orderId);
        l.setLineNo(1);
        l.setMaterialId(1L);
        l.setUoMId(1L);
        l.setQuantity(BigDecimal.TEN);
        l.setUnitPrice(unitPrice);
        l.setAmount(unitPrice.multiply(BigDecimal.TEN));
        dao.saveEntity(l);
    }

    private void seedReceive(long id, long orderId, long supplierId, LocalDate businessDate) {
        IEntityDao<ErpPurReceive> dao = daoProvider.daoFor(ErpPurReceive.class);
        ErpPurReceive r = dao.newEntity();
        r.orm_propValue(1, id);
        r.setCode("PR-" + id);
        r.setOrgId(1L);
        r.setOrderId(orderId);
        r.setSupplierId(supplierId);
        r.setWarehouseId(1L);
        r.setBusinessDate(businessDate);
        r.setCurrencyId(1L);
        r.setExchangeRate(BigDecimal.ONE);
        r.setDocStatus(ErpPurConstants.DOC_STATUS_ACTIVE);
        r.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        dao.saveEntity(r);
    }

    private void seedReceiveLine(long id, long receiveId, long orderLineId) {
        IEntityDao<ErpPurReceiveLine> dao = daoProvider.daoFor(ErpPurReceiveLine.class);
        ErpPurReceiveLine l = dao.newEntity();
        l.orm_propValue(1, id);
        l.setReceiveId(receiveId);
        l.setOrderLineId(orderLineId);
        l.setLineNo(1);
        l.setMaterialId(1L);
        l.setUoMId(1L);
        l.setQuantity(BigDecimal.TEN);
        l.setUnitPrice(new BigDecimal("100"));
        dao.saveEntity(l);
    }

    private void seedInvoiceLine(long id, long invoiceId, long receiveLineId, BigDecimal unitPrice) {
        IEntityDao<ErpPurInvoiceLine> dao = daoProvider.daoFor(ErpPurInvoiceLine.class);
        ErpPurInvoiceLine l = dao.newEntity();
        l.orm_propValue(1, id);
        l.setInvoiceId(invoiceId);
        l.setReceiveLineId(receiveLineId);
        l.setLineNo(1);
        l.setMaterialId(1L);
        l.setUoMId(1L);
        l.setQuantity(BigDecimal.TEN);
        l.setUnitPrice(unitPrice);
        dao.saveEntity(l);
    }

    private void seedArApItem(long id, long partnerId, BigDecimal openAmount) {
        seedArApItemWithDue(id, partnerId, openAmount, LocalDate.now(), LocalDate.now());
    }

    private void seedArApItemWithDue(long id, long partnerId, BigDecimal openAmount,
                                     LocalDate businessDate, LocalDate dueDate) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        ErpFinArApItem it = dao.newEntity();
        it.orm_propValue(1, id);
        it.setCode("PUR-AP-" + id);
        it.setOrgId(1L);
        it.setAcctSchemaId(1L);
        it.setDirection(ErpFinConstants.DIRECTION_PAYABLE);
        it.setPartnerId(partnerId);
        it.setSourceBillType(ErpFinConstants.SOURCE_BILL_AP_INVOICE);
        it.setSourceBillCode("PUR-BILL-" + id);
        it.setBusinessDate(businessDate);
        it.setDueDate(dueDate);
        it.setCurrencyId(1L);
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
