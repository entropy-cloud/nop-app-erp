package app.erp.fin.service.report;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinAccountingPeriodStatus;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinGlBalance;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.WebContentBean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 财务报表渲染子系统端到端测试（plan 2026-07-06-0504-2 Phase 2/3/4 Proof）。
 *
 * <p>覆盖五张种子报表的 {@code renderHtml}/{@code download(xlsx|pdf)} 渲染管线、数据集口径断言、
 * 以及路径注入防护（非法 reportName 抛 {@code ERR_REPORT_NAME_INVALID}）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinReportRendering extends JunitAutoTestCase {

    private static final io.nop.core.context.IServiceContext CTX = new io.nop.core.context.ServiceContextImpl();

    @Inject
    ErpFinReportBizModel reportBiz;
    @Inject
    IDaoProvider daoProvider;

    private Long periodId;
    private Long cashSubjectId;

    // ===================== 数据准备 =====================

    private void seed() {
        periodId = seedPeriod("2025-06", 2025, 6);
        seedCurrency(1L, "CNY");

        ErpMdSubject cash = seedSubject("1001", "库存现金", "ASSET", ErpFinConstants.DC_DEBIT);
        ErpMdSubject ar = seedSubject("1122", "应收账款", "ASSET", ErpFinConstants.DC_DEBIT);
        ErpMdSubject ap = seedSubject("2202", "应付账款", "LIABILITY", ErpFinConstants.DC_CREDIT);
        ErpMdSubject eq = seedSubject("4103", "实收资本", "EQUITY", ErpFinConstants.DC_CREDIT);
        ErpMdSubject inc = seedSubject("6001", "主营业务收入", ErpFinConstants.SUBJECT_CLASS_INCOME, ErpFinConstants.DC_CREDIT);
        ErpMdSubject exp = seedSubject("6601", "销售费用", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
        cashSubjectId = cash.getId();

        // 资产负债表：期末余额
        seedGlBalance(cash, new BigDecimal("500"), null, null, null);     // 资产 500
        seedGlBalance(ar, new BigDecimal("200"), null, null, null);       // 资产 200
        seedGlBalance(ap, null, new BigDecimal("300"), null, null);       // 负债 300
        seedGlBalance(eq, null, new BigDecimal("400"), null, null);       // 权益 400
        // 利润表：本期发生额
        seedGlBalance(inc, null, null, null, new BigDecimal("1000"));     // 收入 1000（贷方科目 periodCredit-periodDebit）
        seedGlBalance(exp, null, null, new BigDecimal("600"), null);      // 费用 600（借方科目 periodDebit-periodCredit）

        // 现金流量表：posted 凭证含 1001 现金科目借方 80（流入）
        Long voucherId = seedPostedVoucherWithCashLine();

        // AR/AP 账龄：一笔 100 天前的开口应收 → 90+ 桶
        seedOpenArAp(CoreMetrics.currentDate().minusDays(100), new BigDecimal("250"));

        // 期末结账报告：模块状态 + 损益结转业财回链
        seedPeriodStatus();
        seedVoucherBillR(voucherId, ErpFinBusinessType.PERIOD_CLOSE.name(), "PERIOD-CLOSE-2025-06");
        seedVoucherBillR(voucherId, ErpFinBusinessType.EXCHANGE_GAIN_LOSS.name(), "FX-REVAL-2025-06");
    }

    // ===================== 渲染端到端 =====================

    @Test
    public void testFiveReportsRenderHtml() {
        seed();
        for (String name : new String[]{"balance-sheet", "income-statement", "cash-flow-statement",
                "ar-ap-aging", "period-close-report"}) {
            Map<String, Object> data = new HashMap<>();
            data.put("periodId", periodId);
            String html = reportBiz.renderHtml(name, data, CTX);
            assertNotNull(html, "renderHtml 非空: " + name);
            assertFalse(html.trim().isEmpty(), "renderHtml 文本非空: " + name);
        }
    }

    @Test
    public void testFiveReportsDownloadXlsxAndPdf() {
        seed();
        for (String name : new String[]{"balance-sheet", "income-statement", "cash-flow-statement",
                "ar-ap-aging", "period-close-report"}) {
            Map<String, Object> data = new HashMap<>();
            data.put("periodId", periodId);
            for (String renderType : new String[]{"xlsx", "pdf"}) {
                WebContentBean bean = reportBiz.download(name, renderType, data, CTX);
                assertNotNull(bean, "download 非空: " + name + "/" + renderType);
                Object content = bean.getContent();
                assertNotNull(content, "download 内容非空: " + name + "/" + renderType);
                assertTrue(content instanceof java.io.File,
                        "download 返回 File: " + name + "/" + renderType);
                java.io.File f = (java.io.File) content;
                assertTrue(f.exists() && f.length() > 0,
                        "download 文件有效: " + name + "/" + renderType);
                f.delete();
            }
        }
    }

    // ===================== 数据集口径断言 =====================

    @Test
    public void testBalanceSheetDataset() {
        seed();
        List<Map<String, Object>> ds = reportBiz.buildBalanceSheetDataset(periodId);
        BigDecimal assetTotal = sumBySection(ds, "ASSET");
        BigDecimal liabilityTotal = sumBySection(ds, "LIABILITY");
        BigDecimal equityTotal = sumBySection(ds, "EQUITY");
        assertEquals(new BigDecimal("700.00"), assetTotal.setScale(2), "资产合计 = 500+200");
        assertEquals(new BigDecimal("300.00"), liabilityTotal.setScale(2), "负债合计 = 300");
        assertEquals(new BigDecimal("400.00"), equityTotal.setScale(2), "权益合计 = 400");
    }

    @Test
    public void testIncomeStatementDataset() {
        seed();
        List<Map<String, Object>> ds = reportBiz.buildIncomeStatementDataset(periodId);
        // 收入（贷方科目）= periodCredit - periodDebit = 1000
        assertEquals(new BigDecimal("1000.00"), sumBySection(ds, "INCOME").setScale(2), "收入 1000");
        // 费用（借方科目）= periodDebit - periodCredit = 600
        assertEquals(new BigDecimal("600.00"), sumBySection(ds, "EXPENSE").setScale(2), "费用 600");
    }

    @Test
    public void testCashFlowDataset() {
        seed();
        List<Map<String, Object>> ds = reportBiz.buildCashFlowDataset(periodId);
        assertFalse(ds.isEmpty(), "现金流量数据集非空");
        BigDecimal inflow = BigDecimal.ZERO;
        for (Map<String, Object> r : ds) {
            if (ErpFinConstants.CASH_FLOW_INFLOW.equals(r.get("direction"))) {
                inflow = inflow.add((BigDecimal) r.get("amount"));
            }
        }
        assertEquals(new BigDecimal("80.00"), inflow.setScale(2), "现金流入 80");
    }

    @Test
    public void testArApAgingBuckets() {
        seed();
        List<Map<String, Object>> ds = reportBiz.buildArApAgingDataset(CoreMetrics.currentDate());
        assertFalse(ds.isEmpty(), "账龄数据集非空");
        BigDecimal bucketTotal = BigDecimal.ZERO;
        BigDecimal openSum = BigDecimal.ZERO;
        for (Map<String, Object> r : ds) {
            bucketTotal = bucketTotal.add((BigDecimal) r.get("openAmount"));
            openSum = openSum.add((BigDecimal) r.get("openAmount"));
            assertEquals("90+", r.get("bucket"), "100 天前应收 → 90+ 桶");
        }
        assertEquals(new BigDecimal("250.00"), bucketTotal.setScale(2), "账龄桶合计 = openAmount");
        // 与 ErpFinArApItem.openAmountFunctional 直接聚合一致
        QueryBean q = new QueryBean();
        q.addFilter(eq("status", ErpFinConstants.AR_AP_STATUS_OPEN));
        BigDecimal direct = BigDecimal.ZERO;
        for (ErpFinArApItem it : daoProvider.daoFor(ErpFinArApItem.class).findAllByQuery(q)) {
            direct = direct.add(it.getOpenAmountFunctional());
        }
        assertEquals(openSum.setScale(2), direct.setScale(2), "账龄合计 = 辅助账开口项合计");
    }

    @Test
    public void testPeriodCloseDataset() {
        seed();
        List<Map<String, Object>> ds = reportBiz.buildPeriodCloseDataset(periodId);
        assertFalse(ds.isEmpty(), "结账报告数据集非空");
        boolean hasModuleStatus = false;
        boolean hasPeriodCloseVoucher = false;
        boolean hasFxRevalVoucher = false;
        for (Map<String, Object> r : ds) {
            String section = (String) r.get("section");
            String label = String.valueOf(r.get("label"));
            if ("module-status".equals(section)) hasModuleStatus = true;
            if ("voucher".equals(section) && label.contains("损益结转")) {
                hasPeriodCloseVoucher = Integer.valueOf(1).equals(asInt(r.get("value")));
            }
            if ("voucher".equals(section) && label.contains("汇兑重估")) {
                hasFxRevalVoucher = Integer.valueOf(1).equals(asInt(r.get("value")));
            }
        }
        assertTrue(hasModuleStatus, "结账报告覆盖模块关账状态");
        assertTrue(hasPeriodCloseVoucher, "结账报告覆盖损益结转凭证");
        assertTrue(hasFxRevalVoucher, "结账报告覆盖汇兑重估凭证");
    }

    // ===================== 路径注入防护 =====================

    @Test
    public void testPathInjectionRejected() {
        assertThrows(NopException.class,
                () -> reportBiz.renderHtml("../etc/passwd", null, CTX),
                "非法 reportName（含 ../）抛 NopException");
        assertThrows(NopException.class,
                () -> reportBiz.renderHtml(null, null, CTX),
                "空 reportName 抛 NopException");
        // 渲染类型非法
        seed();
        assertThrows(NopException.class,
                () -> reportBiz.download("balance-sheet", "exe", null, CTX),
                "非法 renderType 抛 NopException");
    }

    // ===================== helpers =====================

    private static BigDecimal sumBySection(List<Map<String, Object>> ds, String section) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> r : ds) {
            if (section.equals(r.get("section"))) {
                total = total.add((BigDecimal) r.get("amount"));
            }
        }
        return total;
    }

    private static int asInt(Object v) {
        if (v == null) return 0;
        return Integer.parseInt(v.toString());
    }

    private Long seedPeriod(String code, int year, int month) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod p = new ErpFinAccountingPeriod();
        p.setCode(code);
        p.setName(code);
        p.setOrgId(1L);
        p.setYear(year);
        p.setMonth(month);
        p.setStartDate(LocalDate.of(year, month, 1));
        p.setEndDate(LocalDate.of(year, month, 28));
        p.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN);
        dao.saveEntity(p);
        return p.getId();
    }

    private void seedCurrency(Long id, String code) {
        IEntityDao<ErpMdCurrency> dao = daoProvider.daoFor(ErpMdCurrency.class);
        ErpMdCurrency c = new ErpMdCurrency();
        c.setId(id);
        c.setCode(code);
        c.setName(code);
        c.setIsFunctional(true);
        dao.saveEntity(c);
    }

    private ErpMdSubject seedSubject(String code, String name, String subjectClass, String direction) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject s = new ErpMdSubject();
        s.setCode(code);
        s.setName(name);
        s.setSubjectClass(subjectClass);
        s.setDirection(direction);
        s.setStatus("ACTIVE");
        dao.saveEntity(s);
        return s;
    }

    private void seedGlBalance(ErpMdSubject s, BigDecimal closingDebit, BigDecimal closingCredit,
                               BigDecimal periodDebit, BigDecimal periodCredit) {
        IEntityDao<ErpFinGlBalance> dao = daoProvider.daoFor(ErpFinGlBalance.class);
        ErpFinGlBalance b = new ErpFinGlBalance();
        b.setOrgId(1L);
        b.setAcctSchemaId(1L);
        b.setPeriodId(periodId);
        b.setSubjectId(s.getId());
        b.setCurrencyId(1L);
        b.setOpeningDebit(BigDecimal.ZERO);
        b.setOpeningCredit(BigDecimal.ZERO);
        b.setClosingDebit(closingDebit != null ? closingDebit : BigDecimal.ZERO);
        b.setClosingCredit(closingCredit != null ? closingCredit : BigDecimal.ZERO);
        b.setPeriodDebit(periodDebit != null ? periodDebit : BigDecimal.ZERO);
        b.setPeriodCredit(periodCredit != null ? periodCredit : BigDecimal.ZERO);
        b.setYearOpeningDebit(BigDecimal.ZERO);
        b.setYearOpeningCredit(BigDecimal.ZERO);
        dao.saveEntity(b);
    }

    private Long seedPostedVoucherWithCashLine() {
        IEntityDao<ErpFinVoucher> vDao = daoProvider.daoFor(ErpFinVoucher.class);
        BigDecimal amt = new BigDecimal("80");
        ErpFinVoucher v = new ErpFinVoucher();
        v.setCode("V-CF-2025-06");
        v.setVoucherType("TRANSFER");
        v.setVoucherDate(LocalDate.of(2025, 6, 10));
        v.setOrgId(1L);
        v.setAcctSchemaId(1L);
        v.setPeriodId(periodId);
        v.setTotalDebit(amt);
        v.setTotalCredit(amt);
        v.setIsReversed(false);
        v.setDocStatus(ErpFinConstants.VOUCHER_STATUS_POSTED);
        vDao.saveEntity(v);

        IEntityDao<ErpFinVoucherLine> lDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        ErpFinVoucherLine cashLine = new ErpFinVoucherLine();
        cashLine.setVoucherId(v.getId());
        cashLine.setLineNo(1);
        cashLine.setSubjectId(cashSubjectId);
        cashLine.setSubjectCode("1001");
        cashLine.setSubjectName("库存现金");
        cashLine.setDcDirection(ErpFinConstants.DC_DEBIT);
        cashLine.setDebitAmount(amt);
        cashLine.setCreditAmount(BigDecimal.ZERO);
        cashLine.setCurrencyId(1L);
        cashLine.setExchangeRate(BigDecimal.ONE);
        cashLine.setAmountSource(amt);
        cashLine.setAmountFunctional(amt);
        cashLine.setAcctSchemaId(1L);
        lDao.saveEntity(cashLine);

        ErpFinVoucherLine offsetLine = new ErpFinVoucherLine();
        offsetLine.setVoucherId(v.getId());
        offsetLine.setLineNo(2);
        offsetLine.setSubjectId(cashSubjectId); // 对冲科目无所谓口径，仅现金行参与现金流
        offsetLine.setSubjectCode("6001");
        offsetLine.setSubjectName("主营业务收入");
        offsetLine.setDcDirection(ErpFinConstants.DC_CREDIT);
        offsetLine.setDebitAmount(BigDecimal.ZERO);
        offsetLine.setCreditAmount(amt);
        offsetLine.setCurrencyId(1L);
        offsetLine.setExchangeRate(BigDecimal.ONE);
        offsetLine.setAmountSource(amt);
        offsetLine.setAmountFunctional(amt);
        offsetLine.setAcctSchemaId(1L);
        lDao.saveEntity(offsetLine);
        return v.getId();
    }

    private void seedOpenArAp(LocalDate businessDate, BigDecimal openFunctional) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        ErpFinArApItem it = new ErpFinArApItem();
        it.setCode("ARI-AGING-001");
        it.setOrgId(1L);
        it.setAcctSchemaId(1L);
        it.setDirection(ErpFinConstants.DIRECTION_RECEIVABLE);
        it.setPartnerId(1L);
        it.setSourceBillType(ErpFinConstants.SOURCE_BILL_AR_INVOICE);
        it.setSourceBillCode("ARI-AGING-001");
        it.setBusinessDate(businessDate);
        it.setDueDate(businessDate);
        it.setCurrencyId(1L);
        it.setExchangeRate(BigDecimal.ONE);
        it.setAmountSource(openFunctional);
        it.setAmountFunctional(openFunctional);
        it.setSettledAmountSource(BigDecimal.ZERO);
        it.setSettledAmountFunctional(BigDecimal.ZERO);
        it.setOpenAmountSource(openFunctional);
        it.setOpenAmountFunctional(openFunctional);
        it.setStatus(ErpFinConstants.AR_AP_STATUS_OPEN);
        it.setPeriodId(periodId);
        dao.saveEntity(it);
    }

    private void seedPeriodStatus() {
        IEntityDao<ErpFinAccountingPeriodStatus> dao = daoProvider.daoFor(ErpFinAccountingPeriodStatus.class);
        ErpFinAccountingPeriodStatus s = new ErpFinAccountingPeriodStatus();
        s.setPeriodId(periodId);
        s.setAcctSchemaId(1L);
        s.setTotalVouchers(1);
        s.setPostedVouchers(1);
        s.setUnpostedVouchers(0);
        s.setArStatus(ErpFinConstants.MODULE_CLOSE_CLOSED);
        s.setApStatus(ErpFinConstants.MODULE_CLOSE_CLOSED);
        s.setInvStatus(ErpFinConstants.MODULE_CLOSE_CLOSED);
        s.setGlStatus(ErpFinConstants.MODULE_CLOSE_CLOSED);
        s.setAssetStatus(ErpFinConstants.MODULE_CLOSE_CLOSED);
        dao.saveEntity(s);
    }

    private void seedVoucherBillR(Long voucherId, String businessType, String billCode) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        ErpFinVoucherBillR r = new ErpFinVoucherBillR();
        r.setVoucherId(voucherId);
        r.setBillType("PERIOD_CLOSE");
        r.setBillCode(billCode);
        r.setBusinessType(businessType);
        dao.saveEntity(r);
    }
}
