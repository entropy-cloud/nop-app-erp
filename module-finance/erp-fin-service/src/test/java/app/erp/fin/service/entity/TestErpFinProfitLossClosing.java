package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinAccountingPeriodBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinTrialBalance;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.fin.service.ErpFinConstants;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 损益结转单测（Phase 2）。验证收入(40)/费用(50)/成本(60) 三类余额经 PERIOD_CLOSE(120) 凭证结转至本年利润，
 * 借贷平衡、结转后损益类科目余额清零、本年利润净额=收入−费用−成本，且生成试算平衡表快照。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:profit-loss-test.yaml")
public class TestErpFinProfitLossClosing extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinAccountingPeriodBiz periodBiz;

    @Test
    public void testProfitLossClosing() {
        Long periodId = seedReturn(() -> {
            Long pid = seedOpenPeriod("2024-03", 2024, 3,
                    LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31));
            java.util.Map<String, ErpMdSubject> subjects = new java.util.HashMap<>();
            subjects.put("1001", seedSubject("1001", "库存现金", "ASSET", ErpFinConstants.DC_DEBIT));
            subjects.put("6001", seedSubject("6001", "主营业务收入", ErpFinConstants.SUBJECT_CLASS_INCOME, ErpFinConstants.DC_CREDIT));
            subjects.put("6601", seedSubject("6601", "销售费用", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT));
            subjects.put("6401", seedSubject("6401", "主营业务成本", ErpFinConstants.SUBJECT_CLASS_COST, ErpFinConstants.DC_DEBIT));
            subjects.put("4103", seedSubject("4103", "本年利润", "EQUITY", ErpFinConstants.DC_CREDIT));
            // 收入 150
            seedPostedVoucher("V-INCOME-001", pid, LocalDate.of(2024, 3, 10), subjects,
                    line("1001", "库存现金", ErpFinConstants.DC_DEBIT, "150"),
                    line("6001", "主营业务收入", ErpFinConstants.DC_CREDIT, "150"));
            // 费用 50
            seedPostedVoucher("V-EXP-001", pid, LocalDate.of(2024, 3, 12), subjects,
                    line("6601", "销售费用", ErpFinConstants.DC_DEBIT, "50"),
                    line("1001", "库存现金", ErpFinConstants.DC_CREDIT, "50"));
            // 成本 30
            seedPostedVoucher("V-COST-001", pid, LocalDate.of(2024, 3, 15), subjects,
                    line("6401", "主营业务成本", ErpFinConstants.DC_DEBIT, "30"),
                    line("1001", "库存现金", ErpFinConstants.DC_CREDIT, "30"));
            return pid;
        });

        ormTemplate.runInSession(() -> periodBiz.closePeriod(periodId, CTX));

        // 结转凭证存在（PERIOD_CLOSE，billCode=PERIOD-CLOSE-2024-03）。
        ErpFinVoucher closeVoucher = findCloseVoucher("PERIOD-CLOSE-2024-03",
                ErpFinBusinessType.PERIOD_CLOSE.name());
        assertNotNull(closeVoucher, "应生成损益结转凭证");
        assertTrue(closeVoucher.getTotalDebit().compareTo(closeVoucher.getTotalCredit()) == 0, "结转凭证借贷平衡");

        // 收入/费用/成本科目净额（含结转凭证）清零。
        assertEquals(0, netCredit("6001", periodId).compareTo(BigDecimal.ZERO), "收入科目结转后净额为 0");
        assertEquals(0, netDebit("6601", periodId).compareTo(BigDecimal.ZERO), "费用科目结转后净额为 0");
        assertEquals(0, netDebit("6401", periodId).compareTo(BigDecimal.ZERO), "成本科目结转后净额为 0");

        // 本年利润净额 = 收入 150 − 费用 50 − 成本 30 = 70（贷方余额）。
        ErpMdSubject cyp = findSubjectByCode("4103");
        BigDecimal cypNet = netCredit(cyp.getId(), periodId);
        assertEquals(0, cypNet.compareTo(new BigDecimal("70")), "本年利润净额=收入−费用−成本=70");

        // 试算平衡表快照存在。
        assertTrue(!findTrialBalance(periodId).isEmpty(), "应生成试算平衡表快照");
    }

    // ---------- helpers ----------

    private void seedReturn(Runnable action) {
        ormTemplate.runInSession(action);
    }

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

    private ErpMdSubject findSubjectByCode(String code) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        List<ErpMdSubject> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private Object[] line(String subjectCode, String subjectName, String dc, String amount) {
        return new Object[]{subjectCode, subjectName, dc, new BigDecimal(amount)};
    }

    private void seedPostedVoucher(String code, Long periodId, LocalDate date,
                                   java.util.Map<String, ErpMdSubject> subjects, Object[]... lines) {
        IEntityDao<ErpFinVoucher> vDao = daoProvider.daoFor(ErpFinVoucher.class);
        BigDecimal total = BigDecimal.ZERO;
        for (Object[] l : lines) {
            total = total.add((BigDecimal) l[3]);
        }
        ErpFinVoucher v = new ErpFinVoucher();
        v.setCode(code);
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

    private ErpFinVoucher findCloseVoucher(String billCode, String businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billCode), eq("businessType", businessType)));
        List<ErpFinVoucherBillR> links = dao.findAllByQuery(q);
        if (links.isEmpty()) {
            return null;
        }
        return daoProvider.daoFor(ErpFinVoucher.class).getEntityById(links.get(0).getVoucherId());
    }

    /** 该科目在本期所有已过账非红冲凭证分录的净贷方（贷−借）。 */
    private BigDecimal netCredit(String subjectCode, Long periodId) {
        ErpMdSubject s = findSubjectByCode(subjectCode);
        return netCredit(s.getId(), periodId);
    }

    private BigDecimal netCredit(Long subjectId, Long periodId) {
        return sum(subjectId, periodId)[1].subtract(sum(subjectId, periodId)[0]);
    }

    private BigDecimal netDebit(String subjectCode, Long periodId) {
        return netDebit(findSubjectByCode(subjectCode).getId(), periodId);
    }

    private BigDecimal netDebit(Long subjectId, Long periodId) {
        return sum(subjectId, periodId)[0].subtract(sum(subjectId, periodId)[1]);
    }

    private BigDecimal[] sum(Long subjectId, Long periodId) {
        QueryBean vq = new QueryBean();
        vq.addFilter(eq("periodId", periodId));
        vq.addFilter(eq("docStatus", ErpFinConstants.VOUCHER_STATUS_POSTED));
        List<Long> vids = daoProvider.daoFor(ErpFinVoucher.class).findAllByQuery(vq).stream()
                .map(ErpFinVoucher::getId).collect(java.util.stream.Collectors.toList());
        BigDecimal d = BigDecimal.ZERO, c = BigDecimal.ZERO;
        for (ErpFinVoucherLine l : linesOf(subjectId, vids)) {
            d = d.add(l.getDebitAmount() != null ? l.getDebitAmount() : BigDecimal.ZERO);
            c = c.add(l.getCreditAmount() != null ? l.getCreditAmount() : BigDecimal.ZERO);
        }
        return new BigDecimal[]{d, c};
    }

    private List<ErpFinVoucherLine> linesOf(Long subjectId, List<Long> vids) {
        if (vids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("subjectId", subjectId));
        return dao.findAllByQuery(q).stream()
                .filter(l -> vids.contains(l.getVoucherId()))
                .collect(java.util.stream.Collectors.toList());
    }

    private List<ErpFinTrialBalance> findTrialBalance(Long periodId) {
        IEntityDao<ErpFinTrialBalance> dao = daoProvider.daoFor(ErpFinTrialBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", periodId));
        return dao.findAllByQuery(q);
    }
}
