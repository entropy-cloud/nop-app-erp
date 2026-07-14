package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinBudgetControlBiz;
import app.erp.fin.biz.IErpFinBudgetLineBiz;
import app.erp.fin.biz.IErpFinBudgetScenarioBiz;
import app.erp.fin.dao.dto.BudgetCheckResult;
import app.erp.fin.dao.dto.BudgetVsActualRow;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinBudgetControlLog;
import app.erp.fin.dao.entity.ErpFinBudgetLine;
import app.erp.fin.dao.entity.ErpFinBudgetScenario;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
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
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 预算管理端到端集成测试（plan 2026-07-10-1100-4 §Phase 2 Proof）。
 *
 * <p>覆盖预算编制→审批→过账→控制→对比→作废全链路（budget.md）：
 * <ul>
 *   <li>场景1：审批 → BUDGET 影子凭证生成 → VoucherLine 聚合预算余额</li>
 *   <li>场景2：HARD 拦截（余量不足抛 ERR_BUDGET_EXCEEDED）</li>
 *   <li>场景3：WARN 放行 + 写 BudgetControlLog</li>
 *   <li>场景4：NONE 不控制（PASS）</li>
 *   <li>场景5：作废 → 红冲 BUDGET 凭证 → 预算余额归零</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:budget-test.yaml")
public class TestErpFinBudgetEndToEnd extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinBudgetScenarioBiz scenarioBiz;
    @Inject
    IErpFinBudgetControlBiz budgetControlBiz;
    @Inject
    IErpFinBudgetLineBiz budgetLineBiz;

    @Test
    public void testApproveGeneratesBudgetVoucher() {
        Long[] ids = seedReturn(() -> {
            Long pid = seedOpenPeriod("2024-06", 2024, 6);
            ErpMdSubject expense = seedSubject("6601", "销售费用", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            ErpMdSubject income = seedSubject("6001", "主营业务收入", ErpFinConstants.SUBJECT_CLASS_INCOME, ErpFinConstants.DC_CREDIT);
            Long scenarioId = seedBudgetScenario("BUD-2024-06", pid, 2024, ErpFinConstants.BUDGET_CONTROL_NONE,
                    expense, income, new BigDecimal("1000"));
            return new Long[]{pid, scenarioId};
        });
        Long periodId = ids[0];
        Long scenarioId = ids[1];

        ormTemplate.runInSession(() -> scenarioBiz.submit(scenarioId, CTX));
        ErpFinBudgetScenario approved = ormTemplate.runInSession(session -> scenarioBiz.approve(scenarioId, CTX));

        assertEquals(ErpFinConstants.BUDGET_STATUS_APPROVED, approved.getDocStatus());
        assertTrue(approved.getVoucherId() != null, "审批后应回写预算凭证 ID");

        // 预算余额从 VoucherLine（关联凭证 postingType=BUDGET）聚合 = 1000
        BigDecimal budgetBalance = budgetAmountForSubject(periodId, "6601");
        assertEquals(0, budgetBalance.compareTo(new BigDecimal("1000")), "预算余额应为 1000");
    }

    @Test
    public void testHardControlBlocked() {
        Long[] ids = seedReturn(() -> {
            Long pid = seedOpenPeriod("2024-07", 2024, 7);
            ErpMdSubject expense = seedSubject("6601", "销售费用", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            ErpMdSubject income = seedSubject("6001", "主营业务收入", ErpFinConstants.SUBJECT_CLASS_INCOME, ErpFinConstants.DC_CREDIT);
            Long scenarioId = seedBudgetScenario("BUD-2024-07", pid, 2024, ErpFinConstants.BUDGET_CONTROL_HARD,
                    expense, income, new BigDecimal("1000"));
            // 实际凭证：费用 800（NORMAL）
            seedActualVoucher("V-ACT-07", pid, expense, income, new BigDecimal("800"));
            return new Long[]{pid, scenarioId};
        });
        Long periodId = ids[0];
        Long scenarioId = ids[1];
        ErpMdSubject expense = findSubjectByCode("6601");

        ormTemplate.runInSession(() -> scenarioBiz.submit(scenarioId, CTX));
        ormTemplate.runInSession(() -> scenarioBiz.approve(scenarioId, CTX));

        // 余量 = 1000 − 800 = 200 < 300 → HARD 拦截
        assertThrows(NopException.class, () ->
                ormTemplate.runInSession(session -> budgetControlBiz.check(expense.getId(), null, periodId, new BigDecimal("300"),
                        "PURCHASE_ORDER", "PO-001", CTX)),
                "HARD 级别余量不足应抛 NopException(ERR_BUDGET_EXCEEDED)");
    }

    @Test
    public void testWarnControlLogsAndPasses() {
        Long[] ids = seedReturn(() -> {
            Long pid = seedOpenPeriod("2024-08", 2024, 8);
            ErpMdSubject expense = seedSubject("6601", "销售费用", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            ErpMdSubject income = seedSubject("6001", "主营业务收入", ErpFinConstants.SUBJECT_CLASS_INCOME, ErpFinConstants.DC_CREDIT);
            Long scenarioId = seedBudgetScenario("BUD-2024-08", pid, 2024, ErpFinConstants.BUDGET_CONTROL_WARN,
                    expense, income, new BigDecimal("1000"));
            seedActualVoucher("V-ACT-08", pid, expense, income, new BigDecimal("800"));
            return new Long[]{pid, scenarioId};
        });
        Long periodId = ids[0];
        Long scenarioId = ids[1];
        ErpMdSubject expense = findSubjectByCode("6601");

        ormTemplate.runInSession(() -> scenarioBiz.submit(scenarioId, CTX));
        ormTemplate.runInSession(() -> scenarioBiz.approve(scenarioId, CTX));

        BudgetCheckResult result = ormTemplate.runInSession(session -> budgetControlBiz.check(expense.getId(), null, periodId,
                new BigDecimal("300"), "PURCHASE_ORDER", "PO-002", CTX));

        assertEquals(BudgetCheckResult.ACTION_WARNED, result.getActionResult());
        assertEquals(0, result.getAvailableAmount().compareTo(new BigDecimal("200")));
        assertTrue(countControlLogs("PO-002") >= 1, "WARN 应写预算控制日志");
    }

    @Test
    public void testNoneControlPasses() {
        Long[] ids = seedReturn(() -> {
            Long pid = seedOpenPeriod("2024-09", 2024, 9);
            ErpMdSubject expense = seedSubject("6601", "销售费用", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            ErpMdSubject income = seedSubject("6001", "主营业务收入", ErpFinConstants.SUBJECT_CLASS_INCOME, ErpFinConstants.DC_CREDIT);
            Long scenarioId = seedBudgetScenario("BUD-2024-09", pid, 2024, ErpFinConstants.BUDGET_CONTROL_NONE,
                    expense, income, new BigDecimal("1000"));
            return new Long[]{pid, scenarioId};
        });
        Long periodId = ids[0];
        Long scenarioId = ids[1];
        ErpMdSubject expense = findSubjectByCode("6601");

        ormTemplate.runInSession(() -> scenarioBiz.submit(scenarioId, CTX));
        ormTemplate.runInSession(() -> scenarioBiz.approve(scenarioId, CTX));

        BudgetCheckResult result = ormTemplate.runInSession(session -> budgetControlBiz.check(expense.getId(), null, periodId,
                new BigDecimal("9999"), "PURCHASE_ORDER", "PO-003", CTX));
        assertEquals(BudgetCheckResult.ACTION_PASS, result.getActionResult());
    }

    @Test
    public void testCancelReversesBudgetVoucher() {
        Long[] ids = seedReturn(() -> {
            Long pid = seedOpenPeriod("2024-10", 2024, 10);
            ErpMdSubject expense = seedSubject("6601", "销售费用", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            ErpMdSubject income = seedSubject("6001", "主营业务收入", ErpFinConstants.SUBJECT_CLASS_INCOME, ErpFinConstants.DC_CREDIT);
            Long scenarioId = seedBudgetScenario("BUD-2024-10", pid, 2024, ErpFinConstants.BUDGET_CONTROL_NONE,
                    expense, income, new BigDecimal("1000"));
            return new Long[]{pid, scenarioId};
        });
        Long periodId = ids[0];
        Long scenarioId = ids[1];

        ormTemplate.runInSession(() -> scenarioBiz.submit(scenarioId, CTX));
        ormTemplate.runInSession(() -> scenarioBiz.approve(scenarioId, CTX));
        assertEquals(0, budgetAmountForSubject(periodId, "6601").compareTo(new BigDecimal("1000")),
                "审批后预算余额为 1000");

        ErpFinBudgetScenario cancelled = ormTemplate.runInSession(session -> scenarioBiz.cancel(scenarioId, CTX));
        assertEquals(ErpFinConstants.BUDGET_STATUS_CANCELLED, cancelled.getDocStatus());

        // 红冲后净预算余额归零（原凭证 isReversed=true 不计入，红冲凭证 isReversed=true 也不计入）
        assertEquals(0, budgetAmountForSubject(periodId, "6601").compareTo(BigDecimal.ZERO),
                "作废红冲后预算余额应归零");
    }

    @Test
    public void testGetBudgetVsActual() {
        Long[] ids = seedReturn(() -> {
            Long pid = seedOpenPeriod("2024-11", 2024, 11);
            ErpMdSubject expense = seedSubject("6601", "销售费用", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            ErpMdSubject income = seedSubject("6001", "主营业务收入", ErpFinConstants.SUBJECT_CLASS_INCOME, ErpFinConstants.DC_CREDIT);
            Long scenarioId = seedBudgetScenario("BUD-2024-11", pid, 2024, ErpFinConstants.BUDGET_CONTROL_NONE,
                    expense, income, new BigDecimal("1000"));
            // 实际凭证：费用 400（NORMAL）
            seedActualVoucher("V-ACT-11", pid, expense, income, new BigDecimal("400"));
            return new Long[]{pid, scenarioId};
        });
        Long periodId = ids[0];
        Long scenarioId = ids[1];
        ErpMdSubject expense = findSubjectByCode("6601");

        ormTemplate.runInSession(() -> scenarioBiz.submit(scenarioId, CTX));
        ormTemplate.runInSession(() -> scenarioBiz.approve(scenarioId, CTX));

        List<BudgetVsActualRow> rows = ormTemplate.runInSession(session -> budgetLineBiz.getBudgetVsActual(1L, periodId, expense.getId(), CTX));
        assertEquals(1, rows.size(), "应返回 1 行（费用科目×期间×成本中心）");
        BudgetVsActualRow row = rows.get(0);
        assertEquals(0, row.getBudgetAmount().compareTo(new BigDecimal("1000")), "预算数=1000");
        assertEquals(0, row.getActualAmount().compareTo(new BigDecimal("400")), "实际数=400");
        assertEquals(0, row.getAvailableAmount().compareTo(new BigDecimal("600")), "余量=600");
    }

    // ---------- helpers ----------

    private <T> T seedReturn(java.util.function.Supplier<T> action) {
        return ormTemplate.runInSession(session -> action.get());
    }

    private Long seedOpenPeriod(String code, int year, int month) {
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

    /** 创建预算方案（DRAFT）+ 2 行预算（费用 + 收入，金额相同以使预算凭证自然平衡）。返回方案 ID。 */
    private Long seedBudgetScenario(String code, Long periodId, int fiscalYear, String controlLevel,
                                    ErpMdSubject expense, ErpMdSubject income, BigDecimal amount) {
        IEntityDao<ErpFinBudgetScenario> sDao = daoProvider.daoFor(ErpFinBudgetScenario.class);
        ErpFinBudgetScenario s = new ErpFinBudgetScenario();
        s.setCode(code);
        s.setName(code);
        s.setOrgId(1L);
        s.setAcctSchemaId(1L);
        s.setFiscalYear(fiscalYear);
        s.setScenarioType("ANNUAL");
        s.setCurrencyId(1L);
        s.setExchangeRate(BigDecimal.ONE);
        s.setControlLevel(controlLevel);
        s.setDocStatus(ErpFinConstants.BUDGET_STATUS_DRAFT);
        s.setApproveStatus(ErpFinConstants.BUDGET_STATUS_DRAFT);
        sDao.saveEntity(s);

        IEntityDao<ErpFinBudgetLine> lDao = daoProvider.daoFor(ErpFinBudgetLine.class);
        ErpFinBudgetLine l1 = new ErpFinBudgetLine();
        l1.setScenarioId(s.getId());
        l1.setLineNo(1);
        l1.setOrgId(1L);
        l1.setAcctSchemaId(1L);
        l1.setPeriodId(periodId);
        l1.setSubjectId(expense.getId());
        l1.setSubjectCode(expense.getCode());
        l1.setBudgetAmountSource(amount);
        l1.setBudgetAmountFunctional(amount);
        l1.setCurrencyId(1L);
        l1.setExchangeRate(BigDecimal.ONE);
        lDao.saveEntity(l1);

        ErpFinBudgetLine l2 = new ErpFinBudgetLine();
        l2.setScenarioId(s.getId());
        l2.setLineNo(2);
        l2.setOrgId(1L);
        l2.setAcctSchemaId(1L);
        l2.setPeriodId(periodId);
        l2.setSubjectId(income.getId());
        l2.setSubjectCode(income.getCode());
        l2.setBudgetAmountSource(amount);
        l2.setBudgetAmountFunctional(amount);
        l2.setCurrencyId(1L);
        l2.setExchangeRate(BigDecimal.ONE);
        lDao.saveEntity(l2);
        return s.getId();
    }

    /** 实际费用凭证（NORMAL）：借费用 / 贷收入，平衡。 */
    private void seedActualVoucher(String code, Long periodId, ErpMdSubject expense, ErpMdSubject income, BigDecimal amount) {
        IEntityDao<ErpFinVoucher> vDao = daoProvider.daoFor(ErpFinVoucher.class);
        ErpFinVoucher v = new ErpFinVoucher();
        v.setCode(code);
        v.setVoucherType("TRANSFER");
        v.setPostingType(ErpFinConstants.POSTING_TYPE_NORMAL);
        v.setVoucherDate(LocalDate.now());
        v.setOrgId(1L);
        v.setAcctSchemaId(1L);
        v.setPeriodId(periodId);
        v.setTotalDebit(amount);
        v.setTotalCredit(amount);
        v.setIsReversed(false);
        v.setDocStatus(ErpFinConstants.VOUCHER_STATUS_POSTED);
        vDao.saveEntity(v);

        IEntityDao<ErpFinVoucherLine> lDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        ErpFinVoucherLine d = new ErpFinVoucherLine();
        d.setVoucherId(v.getId());
        d.setLineNo(1);
        d.setSubjectId(expense.getId());
        d.setSubjectCode(expense.getCode());
        d.setDcDirection(ErpFinConstants.DC_DEBIT);
        d.setDebitAmount(amount);
        d.setCreditAmount(BigDecimal.ZERO);
        d.setCurrencyId(1L);
        d.setExchangeRate(BigDecimal.ONE);
        d.setAmountSource(amount);
        d.setAmountFunctional(amount);
        d.setAcctSchemaId(1L);
        lDao.saveEntity(d);

        ErpFinVoucherLine c = new ErpFinVoucherLine();
        c.setVoucherId(v.getId());
        c.setLineNo(2);
        c.setSubjectId(income.getId());
        c.setSubjectCode(income.getCode());
        c.setDcDirection(ErpFinConstants.DC_CREDIT);
        c.setDebitAmount(BigDecimal.ZERO);
        c.setCreditAmount(amount);
        c.setCurrencyId(1L);
        c.setExchangeRate(BigDecimal.ONE);
        c.setAmountSource(amount);
        c.setAmountFunctional(amount);
        c.setAcctSchemaId(1L);
        lDao.saveEntity(c);
    }

    /** 聚合 BUDGET 凭证在该期间该科目的净预算额（借方科目=借−贷）。 */
    private BigDecimal budgetAmountForSubject(Long periodId, String subjectCode) {
        ErpMdSubject s = findSubjectByCode(subjectCode);
        QueryBean vq = new QueryBean();
        vq.addFilter(eq("periodId", periodId));
        vq.addFilter(eq("docStatus", ErpFinConstants.VOUCHER_STATUS_POSTED));
        vq.addFilter(eq("isReversed", Boolean.FALSE));
        vq.addFilter(eq("postingType", ErpFinConstants.POSTING_TYPE_BUDGET));
        List<Long> vids = daoProvider.daoFor(ErpFinVoucher.class).findAllByQuery(vq).stream()
                .map(ErpFinVoucher::getId).collect(java.util.stream.Collectors.toList());
        BigDecimal debit = BigDecimal.ZERO, credit = BigDecimal.ZERO;
        if (!vids.isEmpty()) {
            QueryBean lq = new QueryBean();
            lq.addFilter(eq("subjectId", s.getId()));
            for (ErpFinVoucherLine l : daoProvider.daoFor(ErpFinVoucherLine.class).findAllByQuery(lq)) {
                if (vids.contains(l.getVoucherId())) {
                    debit = debit.add(l.getDebitAmount() != null ? l.getDebitAmount() : BigDecimal.ZERO);
                    credit = credit.add(l.getCreditAmount() != null ? l.getCreditAmount() : BigDecimal.ZERO);
                }
            }
        }
        return ErpFinConstants.DC_CREDIT.equals(s.getDirection()) ? credit.subtract(debit) : debit.subtract(credit);
    }

    private int countControlLogs(String sourceBillCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("sourceBillCode", sourceBillCode));
        return daoProvider.daoFor(ErpFinBudgetControlLog.class).findAllByQuery(q).size();
    }

    private ErpMdSubject findSubjectByCode(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        List<ErpMdSubject> list = daoProvider.daoFor(ErpMdSubject.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }
}
