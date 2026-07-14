package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinAccountingPeriodBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 预算凭证隔离回归测试（plan 2026-07-10-1100-4 §Phase 2 Fix/Proof）。
 *
 * <p>验证引入 postingType=BUDGET 的预算影子凭证后，实际财务聚合（损益结转）不被污染：
 * <ul>
 *   <li>本期实际凭证：收入 200 / 费用 80（NORMAL/无 postingType）</li>
 *   <li>本期预算凭证：费用预算 500 / 收入预算 500（postingType=BUDGET）</li>
 *   <li>运行损益结转 → 断言结转仅基于实际数（本年利润=200−80=120），BUDGET 凭证行不进入结转金额</li>
 * </ul>
 *
 * <p>此回归是预算凭证安全引入的门控测试（budget.md 规则4/6/8）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:budget-test.yaml")
public class TestErpFinBudgetIsolation extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinAccountingPeriodBiz periodBiz;

    @Test
    public void testBudgetVoucherExcludedFromProfitLossClosing() {
        Map<String, ErpMdSubject> subjects = seedReturn(() -> {
            Long pid = seedOpenPeriod("2024-05", 2024, 5, LocalDate.of(2024, 5, 1), LocalDate.of(2024, 5, 31));
            Map<String, ErpMdSubject> subs = new HashMap<>();
            subs.put("1001", seedSubject("1001", "库存现金", "ASSET", ErpFinConstants.DC_DEBIT));
            subs.put("6001", seedSubject("6001", "主营业务收入", ErpFinConstants.SUBJECT_CLASS_INCOME, ErpFinConstants.DC_CREDIT));
            subs.put("6601", seedSubject("6601", "销售费用", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT));
            subs.put("4103", seedSubject("4103", "本年利润", "EQUITY", ErpFinConstants.DC_CREDIT));

            // 实际凭证：收入 200
            seedPostedVoucher("V-ACT-INCOME", pid, ErpFinConstants.POSTING_TYPE_NORMAL, subs,
                    line("1001", "库存现金", ErpFinConstants.DC_DEBIT, "200"),
                    line("6001", "主营业务收入", ErpFinConstants.DC_CREDIT, "200"));
            // 实际凭证：费用 80
            seedPostedVoucher("V-ACT-EXP", pid, ErpFinConstants.POSTING_TYPE_NORMAL, subs,
                    line("6601", "销售费用", ErpFinConstants.DC_DEBIT, "80"),
                    line("1001", "库存现金", ErpFinConstants.DC_CREDIT, "80"));
            // 预算凭证：费用预算 500 / 收入预算 500（影子凭证，不得污染实际损益）
            seedPostedVoucher("V-BUDGET", pid, ErpFinConstants.POSTING_TYPE_BUDGET, subs,
                    line("6601", "销售费用", ErpFinConstants.DC_DEBIT, "500"),
                    line("6001", "主营业务收入", ErpFinConstants.DC_CREDIT, "500"));
            return subs;
        });

        Long periodId = findPeriodByCode("2024-05").getId();

        ormTemplate.runInSession(() -> periodBiz.closePeriod(periodId, CTX));

        // 结转凭证存在
        ErpFinVoucher closeVoucher = findCloseVoucher("PERIOD-CLOSE-2024-05",
                ErpFinBusinessType.PERIOD_CLOSE.name());
        assertNotNull(closeVoucher, "应生成损益结转凭证");

        // 本年利润净额 = 收入 200 − 费用 80 = 120（NOT 120 ± 500 预算污染）
        ErpMdSubject cyp = findSubjectByCode("4103");
        BigDecimal cypNet = netCredit(cyp.getId(), periodId);
        assertEquals(0, cypNet.compareTo(new BigDecimal("120")),
                "本年利润净额=实际收入200−实际费用80=120，预算凭证 500 不得污染");

        // 收入/费用科目结转后净额清零（基于实际数结转）
        assertEquals(0, netCredit(findSubjectByCode("6001").getId(), periodId).compareTo(BigDecimal.ZERO),
                "收入科目结转后净额为 0（仅实际数结转）");
        assertEquals(0, netDebit(findSubjectByCode("6601").getId(), periodId).compareTo(BigDecimal.ZERO),
                "费用科目结转后净额为 0（仅实际数结转）");
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

    private Object[] line(String subjectCode, String subjectName, String dc, String amount) {
        return new Object[]{subjectCode, subjectName, dc, new BigDecimal(amount)};
    }

    private void seedPostedVoucher(String code, Long periodId, String postingType,
                                   Map<String, ErpMdSubject> subjects, Object[]... lines) {
        IEntityDao<ErpFinVoucher> vDao = daoProvider.daoFor(ErpFinVoucher.class);
        BigDecimal total = BigDecimal.ZERO;
        for (Object[] l : lines) {
            total = total.add((BigDecimal) l[3]);
        }
        ErpFinVoucher v = new ErpFinVoucher();
        v.setCode(code);
        v.setVoucherType("TRANSFER");
        v.setPostingType(postingType);
        v.setVoucherDate(LocalDate.now());
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

    private ErpMdSubject findSubjectByCode(String code) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        List<ErpMdSubject> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpFinAccountingPeriod findPeriodByCode(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        return daoProvider.daoFor(ErpFinAccountingPeriod.class).findAllByQuery(q).get(0);
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

    private BigDecimal netCredit(Long subjectId, Long periodId) {
        return sum(subjectId, periodId)[1].subtract(sum(subjectId, periodId)[0]);
    }

    private BigDecimal netDebit(Long subjectId, Long periodId) {
        return sum(subjectId, periodId)[0].subtract(sum(subjectId, periodId)[1]);
    }

    private BigDecimal[] sum(Long subjectId, Long periodId) {
        QueryBean vq = new QueryBean();
        vq.addFilter(eq("periodId", periodId));
        vq.addFilter(eq("docStatus", ErpFinConstants.VOUCHER_STATUS_POSTED));
        // 仅聚合实际凭证（排除 BUDGET 影子凭证），验证结转对实际数生效。
        vq.addFilter(io.nop.api.core.beans.FilterBeans.or(
                io.nop.api.core.beans.FilterBeans.isNull("postingType"),
                io.nop.api.core.beans.FilterBeans.ne("postingType", ErpFinConstants.POSTING_TYPE_BUDGET)));
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
}
