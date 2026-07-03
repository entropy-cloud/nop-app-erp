package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinAccountingPeriodBiz;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinAccountingPeriodStatus;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 期末结账测试共享基类（注入 + seed/断言 helper）。子类提供 {@code @NopTestConfig} 与测试方法。
 */
public abstract class PeriodCloseTestSupport extends JunitAutoTestCase {

    protected static final io.nop.core.context.IServiceContext CTX = new io.nop.core.context.ServiceContextImpl();

    @Inject
    protected IDaoProvider daoProvider;
    @Inject
    protected IOrmTemplate ormTemplate;
    @Inject
    protected IErpFinAccountingPeriodBiz periodBiz;

    protected Long seedFullPeriod(String code, int year, int month) {
        return ormTemplate.runInSession(session -> {
            Long pid = seedOpenPeriod(code, year, month);
            Map<String, ErpMdSubject> subjects = new HashMap<>();
            subjects.put("1001", seedSubject("1001", "库存现金", "ASSET", ErpFinConstants.DC_DEBIT));
            subjects.put("6001", seedSubject("6001", "主营业务收入", ErpFinConstants.SUBJECT_CLASS_INCOME, ErpFinConstants.DC_CREDIT));
            subjects.put("6601", seedSubject("6601", "销售费用", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT));
            subjects.put("4103", seedSubject("4103", "本年利润", "EQUITY", ErpFinConstants.DC_CREDIT));
            subjects.put("1122", seedSubject("1122", "应收账款", "ASSET", ErpFinConstants.DC_DEBIT));
            subjects.put("2202", seedSubject("2202", "应付账款", "LIABILITY", ErpFinConstants.DC_CREDIT));
            subjects.put("6603", seedSubject("6603", "汇兑损益", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT));
            seedCurrency(1L, "CNY", true);
            seedCurrency(2L, "EUR", false);
            seedPostedVoucher("V-" + code + "-INC", pid, LocalDate.of(year, month, 10), subjects,
                    new Object[]{"1001", "库存现金", ErpFinConstants.DC_DEBIT, new BigDecimal("100")},
                    new Object[]{"6001", "主营业务收入", ErpFinConstants.DC_CREDIT, new BigDecimal("100")});
            seedOpenArAp("ARI-" + code + "-001", pid, LocalDate.of(year, month, 11),
                    ErpFinConstants.DIRECTION_RECEIVABLE, 2L, new BigDecimal("100"), new BigDecimal("800"));
            return pid;
        });
    }

    protected Long seedOpenPeriod(String code, int year, int month) {
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

    protected ErpMdSubject seedSubject(String code, String name, String subjectClass, String direction) {
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

    protected void seedCurrency(Long id, String code, boolean functional) {
        IEntityDao<ErpMdCurrency> dao = daoProvider.daoFor(ErpMdCurrency.class);
        ErpMdCurrency c = new ErpMdCurrency();
        c.setId(id);
        c.setCode(code);
        c.setName(code);
        c.setIsFunctional(functional);
        dao.saveEntity(c);
    }

    protected void seedPostedVoucher(String vcode, Long periodId, LocalDate date,
                                     Map<String, ErpMdSubject> subjects, Object[]... lines) {
        IEntityDao<ErpFinVoucher> vDao = daoProvider.daoFor(ErpFinVoucher.class);
        BigDecimal total = BigDecimal.ZERO;
        for (Object[] l : lines) {
            total = total.add((BigDecimal) l[3]);
        }
        ErpFinVoucher v = new ErpFinVoucher();
        v.setCode(vcode);
        v.setVoucherType("TRANSFER");
        v.setVoucherDate(date);
        v.setOrgId(1L);
        v.setAcctSchemaId(1L);
        v.setPeriodId(periodId);
        v.setTotalDebit(total);
        v.setTotalCredit(total);
        v.setIsReversed(false);
        v.setDocStatus(ErpFinConstants.VOUCHER_STATUS_POSTED);
        vDao.saveEntity(v);
        IEntityDao<ErpFinVoucherLine> lDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        int lineNo = 1;
        for (Object[] l : lines) {
            ErpMdSubject subj = subjects.get((String) l[0]);
            String dc = (String) l[2];
            BigDecimal amt = (BigDecimal) l[3];
            ErpFinVoucherLine line = new ErpFinVoucherLine();
            line.setVoucherId(v.getId());
            line.setLineNo(lineNo++);
            line.setSubjectId(subj.getId());
            line.setSubjectCode((String) l[0]);
            line.setSubjectName((String) l[1]);
            line.setDcDirection(dc);
            line.setDebitAmount(ErpFinConstants.DC_DEBIT.equals(dc) ? amt : BigDecimal.ZERO);
            line.setCreditAmount(ErpFinConstants.DC_CREDIT.equals(dc) ? amt : BigDecimal.ZERO);
            line.setCurrencyId(1L);
            line.setExchangeRate(BigDecimal.ONE);
            line.setAmountSource(amt);
            line.setAmountFunctional(amt);
            line.setAcctSchemaId(1L);
            lDao.saveEntity(line);
        }
    }

    protected void seedOpenArAp(String code, Long periodId, LocalDate date, String direction,
                                Long currencyId, BigDecimal openSource, BigDecimal openFunctional) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        ErpFinArApItem item = new ErpFinArApItem();
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

    protected BigDecimal netCredit(String subjectCode, Long periodId) {
        ErpMdSubject s = findSubjectByCode(subjectCode);
        QueryBean vq = new QueryBean();
        vq.addFilter(eq("periodId", periodId));
        List<ErpFinVoucher> vouchers = daoProvider.daoFor(ErpFinVoucher.class).findAllByQuery(vq);
        BigDecimal credit = BigDecimal.ZERO, debit = BigDecimal.ZERO;
        QueryBean lq = new QueryBean();
        lq.addFilter(eq("subjectId", s.getId()));
        for (ErpFinVoucherLine l : daoProvider.daoFor(ErpFinVoucherLine.class).findAllByQuery(lq)) {
            if (vouchers.stream().anyMatch(v -> v.getId().equals(l.getVoucherId()))) {
                credit = credit.add(l.getCreditAmount() != null ? l.getCreditAmount() : BigDecimal.ZERO);
                debit = debit.add(l.getDebitAmount() != null ? l.getDebitAmount() : BigDecimal.ZERO);
            }
        }
        return credit.subtract(debit);
    }

    protected ErpMdSubject findSubjectByCode(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        return daoProvider.daoFor(ErpMdSubject.class).findAllByQuery(q).get(0);
    }

    protected ErpFinAccountingPeriodStatus loadStatus(Long periodId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", periodId));
        return daoProvider.daoFor(ErpFinAccountingPeriodStatus.class).findAllByQuery(q).get(0);
    }

    protected int countVouchersByBillCode(String billCode, String businessType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("billCode", billCode));
        q.addFilter(eq("businessType", businessType));
        return daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(q).size();
    }
}
