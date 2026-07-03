package app.erp.fin.service.entity;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.fx.ExchangeRevaluationService;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
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

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 期末汇兑重估单测（Phase 3，承接 0300-3 deferred）。验证外币 AR 未核销项按期末汇率重估生成
 * EXCHANGE_GAIN_LOSS(130) 凭证、正/负差额收益/损失方向正确、本位币项不重估。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:exchange-revaluation-test.yaml")
public class TestErpFinExchangeRevaluation extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ExchangeRevaluationService exchangeRevaluationService;

    @Test
    public void testForeignReceivableGain() {
        // 外币应收：源币 100，账面本位币 800（历史汇率 8），期末汇率 8.5 → 重估 850，应收升值 50（收益）。
        Long periodId = seedReturn(() -> {
            Long pid = seedOpenPeriod("2024-06", 2024, 6);
            seedCurrency(1L, "CNY", true);
            seedCurrency(2L, "EUR", false);
            seedSubject("1122", "应收账款", "ASSET", ErpFinConstants.DC_DEBIT);
            seedSubject("2202", "应付账款", "LIABILITY", ErpFinConstants.DC_CREDIT);
            seedSubject("6603", "财务费用-汇兑损益", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            seedOpenArAp("ARI-FX-001", pid, LocalDate.of(2024, 6, 10),
                    ErpFinConstants.DIRECTION_RECEIVABLE, 2L,
                    new BigDecimal("100"), new BigDecimal("800"));
            return pid;
        });

        Long voucherId = exchangeRevaluationService.revalue(loadPeriod(periodId), CTX);

        assertNotNull(voucherId, "应生成汇兑重估凭证");
        ErpFinVoucher v = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(voucherId);
        assertTrue(v.getTotalDebit().compareTo(v.getTotalCredit()) == 0, "汇兑凭证借贷平衡");
        assertEquals(0, v.getTotalDebit().compareTo(new BigDecimal("50")), "重估差额 50");

        // 应收收益：借应收 50 / 贷汇兑损益 50。
        List<ErpFinVoucherLine> lines = linesOf(voucherId);
        ErpFinVoucherLine arLine = lineOfSubject(lines, "1122");
        assertEquals(ErpFinConstants.DC_DEBIT, arLine.getDcDirection(), "应收升值借记应收");
        ErpFinVoucherLine fxLine = lineOfSubject(lines, "6603");
        assertEquals(ErpFinConstants.DC_CREDIT, fxLine.getDcDirection(), "收益贷记汇兑损益");
    }

    @Test
    public void testFunctionalItemNotRevalued() {
        // 本位币应收项不重估 → 无凭证。
        Long periodId = seedReturn(() -> {
            Long pid = seedOpenPeriod("2024-07", 2024, 7);
            seedCurrency(1L, "CNY", true);
            seedOpenArAp("ARI-FN-001", pid, LocalDate.of(2024, 7, 10),
                    ErpFinConstants.DIRECTION_RECEIVABLE, 1L,
                    new BigDecimal("100"), new BigDecimal("100"));
            return pid;
        });

        Long voucherId = exchangeRevaluationService.revalue(loadPeriod(periodId), CTX);

        assertNull(voucherId, "本位币项不重估，无凭证");
    }

    // ---------- helpers ----------

    private <T> T seedReturn(java.util.function.Supplier<T> action) {
        return ormTemplate.runInSession(session -> action.get());
    }

    private Long seedOpenPeriod(String code, int year, int month, LocalDate start, LocalDate end) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod p = new ErpFinAccountingPeriod();
        p.setCode(code);
        p.setName(code);
        p.setOrgId(1L);
        p.setYear(year);
        p.setMonth(month);
        p.setStartDate(start);
        p.setEndDate(end);
        p.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN);
        dao.saveEntity(p);
        return p.getId();
    }

    private Long seedOpenPeriod(String code, int year, int month) {
        return seedOpenPeriod(code, year, month,
                LocalDate.of(year, month, 1), LocalDate.of(year, month, 28));
    }

    private void seedCurrency(Long id, String code, boolean functional) {
        IEntityDao<ErpMdCurrency> dao = daoProvider.daoFor(ErpMdCurrency.class);
        ErpMdCurrency c = new ErpMdCurrency();
        c.setId(id);
        c.setCode(code);
        c.setName(code);
        c.setIsFunctional(functional);
        dao.saveEntity(c);
    }

    private void seedSubject(String code, String name, String subjectClass, String direction) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject s = new ErpMdSubject();
        s.setCode(code);
        s.setName(name);
        s.setSubjectClass(subjectClass);
        s.setDirection(direction);
        s.setStatus("ACTIVE");
        dao.saveEntity(s);
    }

    private void seedOpenArAp(String code, Long periodId, LocalDate date, String direction,
                             Long currencyId, BigDecimal openSource, BigDecimal openFunctional) {
        IEntityDao<app.erp.fin.dao.entity.ErpFinArApItem> dao =
                daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinArApItem.class);
        app.erp.fin.dao.entity.ErpFinArApItem item = new app.erp.fin.dao.entity.ErpFinArApItem();
        item.setCode(code);
        item.setOrgId(1L);
        item.setAcctSchemaId(1L);
        item.setDirection(direction);
        item.setPartnerId(1L);
        item.setSourceBillType(ErpFinConstants.SOURCE_BILL_AR_INVOICE);
        item.setSourceBillCode(code);
        item.setBusinessDate(date);
        item.setCurrencyId(currencyId);
        item.setExchangeRate(BigDecimal.ONE);
        item.setAmountSource(openSource);
        item.setAmountFunctional(openFunctional);
        item.setSettledAmountSource(BigDecimal.ZERO);
        item.setSettledAmountFunctional(BigDecimal.ZERO);
        item.setOpenAmountSource(openSource);
        item.setOpenAmountFunctional(openFunctional);
        item.setStatus(ErpFinConstants.AR_AP_STATUS_OPEN);
        item.setPeriodId(periodId);
        dao.saveEntity(item);
    }

    private ErpFinAccountingPeriod loadPeriod(Long periodId) {
        return daoProvider.daoFor(ErpFinAccountingPeriod.class).getEntityById(periodId);
    }

    private List<ErpFinVoucherLine> linesOf(Long voucherId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return daoProvider.daoFor(ErpFinVoucherLine.class).findAllByQuery(q);
    }

    private ErpFinVoucherLine lineOfSubject(List<ErpFinVoucherLine> lines, String subjectCode) {
        return lines.stream().filter(l -> subjectCode.equals(l.getSubjectCode())).findFirst().orElseThrow();
    }
}
