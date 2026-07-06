package app.erp.fin.service.dashboard;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinFundAccount;
import app.erp.fin.dao.entity.ErpFinGlBalance;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdSubject;
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
 * 财务看板聚合（{@code ErpFinDashboard__getDashboardKpi/getDashboardTrend/findCashFlowAlert}）集成测试。
 *
 * <p>覆盖：本期收入/支出/净利润算术、AR/AP 余额方向、银行存款 Σ、12 月趋势分月序列、
 * 现金流预警触发/不触发两路径（阈值默认 0=关闭 → 空列表）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinDashboard extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ErpFinDashboardBizModel dashboardBiz;

    @Test
    public void testKpiEmptyDatasetReturnsZeros() {
        Map<String, Object> kpi = dashboardBiz.getDashboardKpi(null, CTX);
        assertEquals(0, ((BigDecimal) kpi.get("revenue")).compareTo(BigDecimal.ZERO));
        assertEquals(0, ((BigDecimal) kpi.get("expense")).compareTo(BigDecimal.ZERO));
        assertEquals(0, ((BigDecimal) kpi.get("netProfit")).compareTo(BigDecimal.ZERO));
        assertEquals(0, ((BigDecimal) kpi.get("bankBalance")).compareTo(BigDecimal.ZERO));
        assertEquals(0, ((BigDecimal) kpi.get("arBalance")).compareTo(BigDecimal.ZERO));
        assertEquals(0, ((BigDecimal) kpi.get("apBalance")).compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testKpiAggregationArithmetic() {
        ormTemplate.runInSession(() -> {
            ErpMdSubject income = seedSubject(101L, "6001", "INCOME", ErpFinConstants.DC_CREDIT);
            ErpMdSubject expense = seedSubject(102L, "6601", "EXPENSE", ErpFinConstants.DC_DEBIT);
            ErpFinAccountingPeriod period = seedPeriod(201L, 2026, 7);
            // 收入 1000（credit-debit=1000），费用 300（debit-credit=300）→ 净利润 700
            seedGlBalance(301L, period.getId(), income.getId(),
                    new BigDecimal("0"), new BigDecimal("0"),       // opening
                    new BigDecimal("0"), new BigDecimal("1000"),    // period debit/credit
                    new BigDecimal("0"), new BigDecimal("1000"));   // closing
            seedGlBalance(302L, period.getId(), expense.getId(),
                    new BigDecimal("0"), new BigDecimal("0"),
                    new BigDecimal("300"), new BigDecimal("0"),
                    new BigDecimal("300"), new BigDecimal("0"));
            // 银行存款 5000
            seedBankAccount(401L, "BANK-A", new BigDecimal("5000"));
            // AR 余额 800（RECEIVABLE + OPEN）
            seedArApItem(501L, ErpFinConstants.DIRECTION_RECEIVABLE, ErpFinConstants.AR_AP_STATUS_OPEN, new BigDecimal("800"));
            // AP 余额 400（PAYABLE + OPEN）
            seedArApItem(502L, ErpFinConstants.DIRECTION_PAYABLE, ErpFinConstants.AR_AP_STATUS_OPEN, new BigDecimal("400"));
        });

        Map<String, Object> kpi = dashboardBiz.getDashboardKpi(201L, CTX);
        assertEquals(0, ((BigDecimal) kpi.get("revenue")).compareTo(new BigDecimal("1000")));
        assertEquals(0, ((BigDecimal) kpi.get("expense")).compareTo(new BigDecimal("300")));
        assertEquals(0, ((BigDecimal) kpi.get("netProfit")).compareTo(new BigDecimal("700")));
        assertEquals(0, ((BigDecimal) kpi.get("bankBalance")).compareTo(new BigDecimal("5000")));
        assertEquals(0, ((BigDecimal) kpi.get("arBalance")).compareTo(new BigDecimal("800")));
        assertEquals(0, ((BigDecimal) kpi.get("apBalance")).compareTo(new BigDecimal("400")));
    }

    @Test
    public void testTrendMonthlySeries() {
        ormTemplate.runInSession(() -> {
            ErpMdSubject income = seedSubject(111L, "6001", "INCOME", ErpFinConstants.DC_CREDIT);
            ErpFinAccountingPeriod jun = seedPeriod(211L, 2026, 6);
            ErpFinAccountingPeriod jul = seedPeriod(212L, 2026, 7);
            seedGlBalance(311L, jun.getId(), income.getId(),
                    BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, new BigDecimal("200"),
                    BigDecimal.ZERO, new BigDecimal("200"));
            seedGlBalance(312L, jul.getId(), income.getId(),
                    BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, new BigDecimal("300"),
                    BigDecimal.ZERO, new BigDecimal("300"));
        });

        List<Map<String, Object>> trend = dashboardBiz.getDashboardTrend(2, CTX);
        assertEquals(2, trend.size(), "近 2 月序列长度");
        // 仅断言包含数据月的关键字段，月份键按 LocalDate 计算
        boolean hasJun = false, hasJul = false;
        for (Map<String, Object> row : trend) {
            String m = (String) row.get("month");
            BigDecimal rev = (BigDecimal) row.get("revenue");
            if (m.endsWith("-06")) {
                hasJun = true;
                assertEquals(0, rev.compareTo(new BigDecimal("200")));
            }
            if (m.endsWith("-07")) {
                hasJul = true;
                assertEquals(0, rev.compareTo(new BigDecimal("300")));
            }
        }
        assertTrue(hasJun, "趋势包含 6 月");
        assertTrue(hasJul, "趋势包含 7 月");
    }

    @Test
    public void testCashFlowAlertDisabledByDefault() {
        ormTemplate.runInSession(() -> {
            seedBankAccount(411L, "BANK-LOW", new BigDecimal("100"));
        });
        AppConfig.getConfigProvider().assignConfigValue(
                ErpFinConstants.CONFIG_DASH_FIN_CASH_FLOW_THRESHOLD,
                ErpFinConstants.DEFAULT_DASH_FIN_CASH_FLOW_THRESHOLD.toString());
        List<Map<String, Object>> alerts = dashboardBiz.findCashFlowAlert(CTX);
        assertTrue(alerts.isEmpty(), "阈值=0 默认关闭 → 不触发预警");
    }

    @Test
    public void testCashFlowAlertTriggersWhenBelowThreshold() {
        ormTemplate.runInSession(() -> {
            seedBankAccount(421L, "BANK-LOW", new BigDecimal("100"));
        });
        AppConfig.getConfigProvider().assignConfigValue(
                ErpFinConstants.CONFIG_DASH_FIN_CASH_FLOW_THRESHOLD, "500");
        try {
            List<Map<String, Object>> alerts = dashboardBiz.findCashFlowAlert(CTX);
            assertEquals(1, alerts.size(), "余额 100 < 阈值 500 → 触发 1 条预警");
            Map<String, Object> a = alerts.get(0);
            assertEquals(0, ((BigDecimal) a.get("bankBalance")).compareTo(new BigDecimal("100")));
            assertEquals(0, ((BigDecimal) a.get("threshold")).compareTo(new BigDecimal("500")));
            assertEquals(0, ((BigDecimal) a.get("shortfall")).compareTo(new BigDecimal("400")));
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpFinConstants.CONFIG_DASH_FIN_CASH_FLOW_THRESHOLD, "0");
        }
    }

    // ---------- helpers ----------

    private ErpMdSubject seedSubject(long id, String code, String subjectClass, String direction) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject s = dao.newEntity();
        s.orm_propValue(1, id);
        s.setCode(code);
        s.setName(code);
        s.setSubjectClass(subjectClass);
        s.setDirection(direction);
        s.setStatus("ACTIVE");
        dao.saveEntity(s);
        return s;
    }

    private ErpFinAccountingPeriod seedPeriod(long id, int year, int month) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod p = dao.newEntity();
        p.orm_propValue(1, id);
        p.setCode(year + "-" + String.format("%02d", month));
        p.setName(year + "-" + month);
        p.setYear(year);
        p.setMonth(month);
        LocalDate start = LocalDate.of(year, month, 1);
        p.setStartDate(start);
        p.setEndDate(start.withDayOfMonth(start.lengthOfMonth()));
        p.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN);
        dao.saveEntity(p);
        return p;
    }

    private void seedGlBalance(long id, long periodId, long subjectId,
                               BigDecimal openingDebit, BigDecimal openingCredit,
                               BigDecimal periodDebit, BigDecimal periodCredit,
                               BigDecimal closingDebit, BigDecimal closingCredit) {
        IEntityDao<ErpFinGlBalance> dao = daoProvider.daoFor(ErpFinGlBalance.class);
        ErpFinGlBalance b = dao.newEntity();
        b.orm_propValue(1, id);
        b.setOrgId(1L);
        b.setAcctSchemaId(1L);
        b.setPeriodId(periodId);
        b.setSubjectId(subjectId);
        b.setCurrencyId(1L);
        b.setOpeningDebit(openingDebit);
        b.setOpeningCredit(openingCredit);
        b.setPeriodDebit(periodDebit);
        b.setPeriodCredit(periodCredit);
        b.setClosingDebit(closingDebit);
        b.setClosingCredit(closingCredit);
        dao.saveEntity(b);
    }

    private void seedBankAccount(long id, String code, BigDecimal balance) {
        IEntityDao<ErpFinFundAccount> dao = daoProvider.daoFor(ErpFinFundAccount.class);
        ErpFinFundAccount a = dao.newEntity();
        a.orm_propValue(1, id);
        a.setCode(code);
        a.setName(code);
        a.setOrgId(1L);
        a.setAccountType(ErpFinConstants.FUND_ACCOUNT_TYPE_BANK);
        a.setCurrencyId(1L);
        a.setOpeningBalance(BigDecimal.ZERO);
        a.setCurrentBalance(balance);
        a.setStatus("ACTIVE");
        dao.saveEntity(a);
    }

    private void seedArApItem(long id, String direction, String status, BigDecimal openAmount) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        ErpFinArApItem it = dao.newEntity();
        it.orm_propValue(1, id);
        it.setCode("ITEM-" + id);
        it.setOrgId(1L);
        it.setAcctSchemaId(1L);
        it.setDirection(direction);
        it.setPartnerId(900L);
        it.setSourceBillType(ErpFinConstants.SOURCE_BILL_AR_INVOICE);
        it.setSourceBillCode("BILL-" + id);
        it.setBusinessDate(LocalDate.of(2026, 7, 1));
        it.setDueDate(LocalDate.of(2026, 7, 30));
        it.setCurrencyId(1L);
        it.setExchangeRate(BigDecimal.ONE);
        it.setAmountSource(openAmount);
        it.setAmountFunctional(openAmount);
        it.setSettledAmountSource(BigDecimal.ZERO);
        it.setSettledAmountFunctional(BigDecimal.ZERO);
        it.setOpenAmountSource(openAmount);
        it.setOpenAmountFunctional(openAmount);
        it.setStatus(status);
        dao.saveEntity(it);
    }
}
