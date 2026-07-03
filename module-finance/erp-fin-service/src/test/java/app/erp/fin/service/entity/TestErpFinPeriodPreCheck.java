package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinAccountingPeriodBiz;
import app.erp.fin.dao.PeriodPreCheckReport;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.exceptions.NopException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 期末结账前置检查单测（Phase 1）。验证 preCheck 列出未过账凭证 / 未核销应收应付清单，
 * 以及阻断模式（auto-post-on-close=false，默认）下存在问题时 closePeriod 被阻止。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinPeriodPreCheck extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinAccountingPeriodBiz periodCloseBiz;

    @Test
    public void testPreCheckListsIssues() {
        Long periodId = seedReturn(() -> {
            Long pid = seedOpenPeriod("2026-10", 2026, 10,
                    LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                    ErpFinConstants.PERIOD_STATUS_OPEN);
            seedUnpostedVoucher("V-DRAFT-002", pid, LocalDate.of(2026, 10, 5),
                    ErpFinConstants.VOUCHER_STATUS_DRAFT);
            seedUnsettledArAp("ARI-OPEN-001", pid, LocalDate.of(2026, 10, 5),
                    ErpFinConstants.AR_AP_STATUS_OPEN);
            return pid;
        });

        PeriodPreCheckReport report = periodCloseBiz.preCheck(periodId, CTX);

        assertTrue(report.hasIssues(), "应检出问题");
        assertEquals(1, report.getUnpostedVoucherCodes().size(), "1 张未过账凭证");
        assertEquals("V-DRAFT-002", report.getUnpostedVoucherCodes().get(0));
        assertEquals(1, report.getUnsettledArApCodes().size(), "1 笔未核销应收应付");
        assertEquals("ARI-OPEN-001", report.getUnsettledArApCodes().get(0));
    }

    @Test
    public void testPreCheckCleanPeriod() {
        Long periodId = seedReturn(() -> seedOpenPeriod("2026-11", 2026, 11,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                ErpFinConstants.PERIOD_STATUS_OPEN));

        PeriodPreCheckReport report = periodCloseBiz.preCheck(periodId, CTX);

        assertFalse(report.hasIssues(), "干净期间无问题");
    }

    @Test
    public void testBlockingCloseRejectsWithIssues() {
        // 默认 auto-post-on-close=false（阻断模式）：存在未过账凭证 → closePeriod 被阻止。
        Long periodId = seedReturn(() -> {
            Long pid = seedOpenPeriod("2026-12", 2026, 12,
                    LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31),
                    ErpFinConstants.PERIOD_STATUS_OPEN);
            seedUnpostedVoucher("V-DRAFT-003", pid, LocalDate.of(2026, 12, 5),
                    ErpFinConstants.VOUCHER_STATUS_DRAFT);
            return pid;
        });

        assertThrows(NopException.class, () -> periodCloseBiz.closePeriod(periodId, CTX),
                "阻断模式下未过账凭证应阻止结账");

        ErpFinAccountingPeriod period = daoProvider.daoFor(ErpFinAccountingPeriod.class).getEntityById(periodId);
        assertEquals(ErpFinConstants.PERIOD_STATUS_OPEN, period.getStatus(), "被阻止后期间仍 OPEN");
    }

    // ---------- helpers ----------

    private void seed(Runnable action) {
        ormTemplate.runInSession(action);
    }

    private <T> T seedReturn(java.util.function.Supplier<T> action) {
        return ormTemplate.runInSession(session -> action.get());
    }

    private Long seedOpenPeriod(String code, int year, int month, LocalDate start, LocalDate end, String status) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(code);
        period.setName(code);
        period.setOrgId(1L);
        period.setYear(year);
        period.setMonth(month);
        period.setStartDate(start);
        period.setEndDate(end);
        period.setStatus(status);
        dao.saveEntity(period);
        return period.getId();
    }

    private void seedUnpostedVoucher(String code, Long periodId, LocalDate date, String docStatus) {
        IEntityDao<ErpFinVoucher> dao = daoProvider.daoFor(ErpFinVoucher.class);
        ErpFinVoucher v = new ErpFinVoucher();
        v.setCode(code);
        v.setVoucherType("TRANSFER");
        v.setVoucherDate(date);
        v.setOrgId(1L);
        v.setAcctSchemaId(1L);
        v.setPeriodId(periodId);
        v.setTotalDebit(BigDecimal.ZERO);
        v.setTotalCredit(BigDecimal.ZERO);
        v.setDocStatus(docStatus);
        dao.saveEntity(v);
    }

    private void seedUnsettledArAp(String code, Long periodId, LocalDate businessDate, String status) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        ErpFinArApItem item = new ErpFinArApItem();
        item.setCode(code);
        item.setOrgId(1L);
        item.setAcctSchemaId(1L);
        item.setDirection(ErpFinConstants.DIRECTION_RECEIVABLE);
        item.setPartnerId(1L);
        item.setSourceBillType(ErpFinConstants.SOURCE_BILL_AR_INVOICE);
        item.setSourceBillCode(code);
        item.setBusinessDate(businessDate);
        item.setCurrencyId(1L);
        item.setExchangeRate(BigDecimal.ONE);
        item.setAmountSource(BigDecimal.ZERO);
        item.setAmountFunctional(BigDecimal.ZERO);
        item.setSettledAmountSource(BigDecimal.ZERO);
        item.setSettledAmountFunctional(BigDecimal.ZERO);
        item.setOpenAmountSource(BigDecimal.ZERO);
        item.setOpenAmountFunctional(BigDecimal.ZERO);
        item.setStatus(status);
        item.setPeriodId(periodId);
        dao.saveEntity(item);
    }
}
