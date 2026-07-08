package app.erp.sal.service.dashboard;

import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.service.ErpSalConstants;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 销售看板聚合（{@code ErpSalDashboard__*}）集成测试。覆盖：本期销售额/订单量/转化率、
 * 跨域 AR 余额只读聚合、12 月销售趋势、客户 TOP10、应收超期预警触发/不触发两路径。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalDashboard extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ErpSalDashboardBizModel dashboardBiz;

    @Test
    public void testKpiEmptyDatasetReturnsZeros() {
        Map<String, Object> kpi = dashboardBiz.getDashboardKpi(null, null, CTX);
        assertEquals(0, ((BigDecimal) kpi.get("salesAmount")).compareTo(BigDecimal.ZERO));
        assertEquals(0L, kpi.get("orderCount"));
        assertEquals(0.0, (double) kpi.get("conversionRate"), 0.001);
        assertEquals(0, ((BigDecimal) kpi.get("arBalance")).compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testKpiAggregationAndConversionRate() {
        ormTemplate.runInSession(() -> {
            seedCustomer(601L, "C-A");
            seedCustomer(602L, "C-B");
            // 2 张已过票：100 + 200 = 300
            seedInvoice(701L, 601L, new BigDecimal("100"), CoreMetrics.currentDate(), true);
            seedInvoice(702L, 602L, new BigDecimal("200"), CoreMetrics.currentDate(), true);
            // 未过票不计入
            seedInvoice(703L, 601L, new BigDecimal("999"), CoreMetrics.currentDate(), false);
            // 4 张 ACTIVE 订单 → 转化率 = 2/4 = 0.5
            seedOrder(801L, 601L, ErpSalConstants.DOC_STATUS_ACTIVE);
            seedOrder(802L, 602L, ErpSalConstants.DOC_STATUS_ACTIVE);
            seedOrder(803L, 601L, ErpSalConstants.DOC_STATUS_ACTIVE);
            seedOrder(804L, 602L, ErpSalConstants.DOC_STATUS_ACTIVE);
            // AR 余额 500（RECEIVABLE + OPEN）
            seedArApItem(901L, 601L, new BigDecimal("500"));
        });

        Map<String, Object> kpi = dashboardBiz.getDashboardKpi(null, null, CTX);
        assertEquals(0, ((BigDecimal) kpi.get("salesAmount")).compareTo(new BigDecimal("300")));
        assertEquals(4L, kpi.get("orderCount"));
        assertEquals(2L, kpi.get("invoiceCount"));
        assertEquals(0.5, (double) kpi.get("conversionRate"), 0.001);
        assertEquals(0, ((BigDecimal) kpi.get("arBalance")).compareTo(new BigDecimal("500")));
    }

    @Test
    public void testTrendMonthlySeries() {
        ormTemplate.runInSession(() -> {
            seedCustomer(611L, "C-C");
            seedInvoice(711L, 611L, new BigDecimal("150"), CoreMetrics.currentDate().minusMonths(1), true);
            seedInvoice(712L, 611L, new BigDecimal("250"), CoreMetrics.currentDate(), true);
        });
        List<Map<String, Object>> trend = dashboardBiz.getDashboardTrend(2, CTX);
        assertEquals(2, trend.size());
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> row : trend) {
            total = total.add((BigDecimal) row.get("salesAmount"));
        }
        assertEquals(0, total.compareTo(new BigDecimal("400")), "近 2 月销售合计 150+250=400");
    }

    @Test
    public void testCustomerTopN() {
        ormTemplate.runInSession(() -> {
            seedCustomer(621L, "C-TOP1");
            seedCustomer(622L, "C-TOP2");
            seedInvoice(721L, 621L, new BigDecimal("300"), CoreMetrics.currentDate(), true);
            seedInvoice(722L, 622L, new BigDecimal("100"), CoreMetrics.currentDate(), true);
            seedInvoice(723L, 621L, new BigDecimal("50"), CoreMetrics.currentDate(), true);
        });
        List<Map<String, Object>> top = dashboardBiz.findCustomerTopN(10, CTX);
        assertEquals(2, top.size(), "2 个客户");
        // 621 累计 350 > 622 累计 100 → 621 排第一
        assertEquals(621L, top.get(0).get("customerId"));
        assertEquals(0, ((BigDecimal) top.get(0).get("salesAmount")).compareTo(new BigDecimal("350")));
    }

    @Test
    public void testArOverdueAlertDisabledByDefault() {
        ormTemplate.runInSession(() -> {
            seedCustomer(631L, "C-OVD");
            seedArApItem(931L, 631L, new BigDecimal("1000"));
        });
        AppConfig.getConfigProvider().assignConfigValue(
                ErpSalConstants.CONFIG_DASH_SAL_AR_OVERDUE_DAYS,
                String.valueOf(ErpSalConstants.DEFAULT_DASH_SAL_AR_OVERDUE_DAYS));
        AppConfig.getConfigProvider().assignConfigValue(
                ErpSalConstants.CONFIG_DASH_SAL_AR_OVERDUE_AMOUNT,
                ErpSalConstants.DEFAULT_DASH_SAL_AR_OVERDUE_AMOUNT.toString());
        List<Map<String, Object>> alerts = dashboardBiz.findArOverdueAlert(CTX);
        assertTrue(alerts.isEmpty(), "天数/金额阈值默认 0=关闭 → 不触发预警");
    }

    @Test
    public void testArOverdueAlertTriggers() {
        ormTemplate.runInSession(() -> {
            seedCustomer(641L, "C-OVD2");
            // dueDate 100 天前，openAmount 800
            seedArApItemWithDue(941L, 641L, new BigDecimal("800"),
                    CoreMetrics.currentDate().minusDays(100), CoreMetrics.currentDate().minusDays(100));
        });
        AppConfig.getConfigProvider().assignConfigValue(
                ErpSalConstants.CONFIG_DASH_SAL_AR_OVERDUE_DAYS, "90");
        AppConfig.getConfigProvider().assignConfigValue(
                ErpSalConstants.CONFIG_DASH_SAL_AR_OVERDUE_AMOUNT, "500");
        try {
            List<Map<String, Object>> alerts = dashboardBiz.findArOverdueAlert(CTX);
            assertEquals(1, alerts.size(), "账龄 100>90 且金额 800>500 → 触发");
            assertEquals(641L, alerts.get(0).get("partnerId"));
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpSalConstants.CONFIG_DASH_SAL_AR_OVERDUE_DAYS, "0");
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpSalConstants.CONFIG_DASH_SAL_AR_OVERDUE_AMOUNT, "0");
        }
    }

    // ---------- helpers ----------

    private void seedCustomer(long id, String code) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner p = dao.newEntity();
        p.orm_propValue(1, id);
        p.setCode(code);
        p.setName(code);
        p.setPartnerType("CUSTOMER");
        p.setStatus("ACTIVE");
        p.setReceivableBalance(BigDecimal.ZERO);
        p.setPayableBalance(BigDecimal.ZERO);
        dao.saveEntity(p);
    }

    private void seedInvoice(long id, long customerId, BigDecimal amount, LocalDate date, boolean posted) {
        IEntityDao<ErpSalInvoice> dao = daoProvider.daoFor(ErpSalInvoice.class);
        ErpSalInvoice inv = dao.newEntity();
        inv.orm_propValue(1, id);
        inv.setCode("SI-" + id);
        inv.setOrgId(1L);
        inv.setCustomerId(customerId);
        inv.setInvoiceNo("INV-" + id);
        inv.setBusinessDate(date);
        inv.setCurrencyId(1L);
        inv.setExchangeRate(BigDecimal.ONE);
        inv.setAmountSource(amount);
        inv.setAmountFunctional(amount);
        inv.setTotalAmount(amount);
        inv.setDocStatus(ErpSalConstants.DOC_STATUS_ACTIVE);
        inv.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        inv.setPosted(posted);
        dao.saveEntity(inv);
    }

    private void seedOrder(long id, long customerId, String docStatus) {
        IEntityDao<ErpSalOrder> dao = daoProvider.daoFor(ErpSalOrder.class);
        ErpSalOrder o = dao.newEntity();
        o.orm_propValue(1, id);
        o.setCode("SO-" + id);
        o.setOrgId(1L);
        o.setCustomerId(customerId);
        o.setBusinessDate(CoreMetrics.currentDate());
        o.setCurrencyId(1L);
        o.setExchangeRate(BigDecimal.ONE);
        o.setDocStatus(docStatus);
        o.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        dao.saveEntity(o);
    }

    private void seedArApItem(long id, long partnerId, BigDecimal openAmount) {
        seedArApItemWithDue(id, partnerId, openAmount, CoreMetrics.currentDate(), CoreMetrics.currentDate());
    }

    private void seedArApItemWithDue(long id, long partnerId, BigDecimal openAmount,
                                     LocalDate businessDate, LocalDate dueDate) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        ErpFinArApItem it = dao.newEntity();
        it.orm_propValue(1, id);
        it.setCode("SAL-AR-" + id);
        it.setOrgId(1L);
        it.setAcctSchemaId(1L);
        it.setDirection(ErpFinConstants.DIRECTION_RECEIVABLE);
        it.setPartnerId(partnerId);
        it.setSourceBillType(ErpFinConstants.SOURCE_BILL_AR_INVOICE);
        it.setSourceBillCode("SAL-BILL-" + id);
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
