package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinBudgetScenarioBiz;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinBudgetCarryForwardLog;
import app.erp.fin.dao.entity.ErpFinBudgetLine;
import app.erp.fin.dao.entity.ErpFinBudgetScenario;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
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

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A2 预算结转规则引擎测试（plan 2026-07-21-1206-2 §Phase 2 Proof）。
 *
 * <p>4 规则各 1 测试（budget.md §结转规则引擎）：
 * <ul>
 *   <li>REMAINING_FULL：结转金额 = 预算 − 实际 = 1000 − 400 = 600</li>
 *   <li>REMAINING_RATIO：结转金额 = 余量 × 0.5 = 600 × 0.5 = 300</li>
 *   <li>USED_FULL：结转金额 = 实际 = 400</li>
 *   <li>NONE：不结转 = 0</li>
 * </ul>
 * 每场景验证：源方案 status=CLOSED + closedAt 非空 + CarryForwardLog 写入 + 目标方案结转 BudgetLine 金额。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:budget-a2-test.yaml")
public class TestErpFinBudgetCarryForward extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinBudgetScenarioBiz scenarioBiz;

    @Test
    public void testRemainingFullRule() {
        Long[] ids = seedReturn(() -> {
            Long pid = seedOpenPeriod("2024-CF-1", 2024, 6);
            ErpMdSubject expense = seedSubject("7701", "CF-EXP-1", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            Long sourceId = seedApprovedScenario("CF-SRC-1", pid, 2024, expense, new BigDecimal("1000"));
            Long targetId = seedDraftScenario("CF-TGT-1", pid, 2024, expense);
            seedActualVoucher("CF-ACT-1", pid, expense, new BigDecimal("400"));
            return new Long[]{pid, sourceId, targetId};
        });
        Long sourceId = ids[1];
        Long targetId = ids[2];

        ErpFinBudgetScenario updated = ormTemplate.runInSession(session ->
                scenarioBiz.carryForward(sourceId, targetId, ErpFinConstants.BUDGET_CARRY_FORWARD_REMAINING_FULL, CTX));

        assertEquals(ErpFinConstants.BUDGET_STATUS_CLOSED, updated.getDocStatus(), "源方案应置 CLOSED");
        assertNotNull(updated.getClosedAt(), "closedAt 应非空");
        assertTrue(countCarryForwardLogs(sourceId) >= 1, "应写 CarryForwardLog");

        // REMAINING_FULL：1000 - 400 = 600
        BigDecimal carried = findCarryForwardLineAmount(targetId);
        assertEquals(0, carried.compareTo(new BigDecimal("600")),
                "REMAINING_FULL 结转金额应为 600");
    }

    @Test
    public void testRemainingRatioRule() {
        Long[] ids = seedReturn(() -> {
            Long pid = seedOpenPeriod("2024-CF-2", 2024, 7);
            ErpMdSubject expense = seedSubject("7702", "CF-EXP-2", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            Long sourceId = seedApprovedScenario("CF-SRC-2", pid, 2024, expense, new BigDecimal("1000"));
            Long targetId = seedDraftScenario("CF-TGT-2", pid, 2024, expense);
            seedActualVoucher("CF-ACT-2", pid, expense, new BigDecimal("400"));
            return new Long[]{pid, sourceId, targetId};
        });
        Long sourceId = ids[1];
        Long targetId = ids[2];

        ErpFinBudgetScenario updated = ormTemplate.runInSession(session ->
                scenarioBiz.carryForward(sourceId, targetId, ErpFinConstants.BUDGET_CARRY_FORWARD_REMAINING_RATIO, CTX));

        assertEquals(ErpFinConstants.BUDGET_STATUS_CLOSED, updated.getDocStatus());

        // REMAINING_RATIO：(1000-400) × 0.5 = 300
        BigDecimal carried = findCarryForwardLineAmount(targetId);
        assertEquals(0, carried.compareTo(new BigDecimal("300.0000")),
                "REMAINING_RATIO 50% 结转金额应为 300");
    }

    @Test
    public void testUsedFullRule() {
        Long[] ids = seedReturn(() -> {
            Long pid = seedOpenPeriod("2024-CF-3", 2024, 8);
            ErpMdSubject expense = seedSubject("7703", "CF-EXP-3", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            Long sourceId = seedApprovedScenario("CF-SRC-3", pid, 2024, expense, new BigDecimal("1000"));
            Long targetId = seedDraftScenario("CF-TGT-3", pid, 2024, expense);
            seedActualVoucher("CF-ACT-3", pid, expense, new BigDecimal("400"));
            return new Long[]{pid, sourceId, targetId};
        });
        Long sourceId = ids[1];
        Long targetId = ids[2];

        ErpFinBudgetScenario updated = ormTemplate.runInSession(session ->
                scenarioBiz.carryForward(sourceId, targetId, ErpFinConstants.BUDGET_CARRY_FORWARD_USED_FULL, CTX));

        assertEquals(ErpFinConstants.BUDGET_STATUS_CLOSED, updated.getDocStatus());

        // USED_FULL：actual = 400
        BigDecimal carried = findCarryForwardLineAmount(targetId);
        assertEquals(0, carried.compareTo(new BigDecimal("400")),
                "USED_FULL 结转金额应为 400（实际数）");
    }

    @Test
    public void testNoneRule() {
        Long[] ids = seedReturn(() -> {
            Long pid = seedOpenPeriod("2024-CF-4", 2024, 9);
            ErpMdSubject expense = seedSubject("7704", "CF-EXP-4", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            Long sourceId = seedApprovedScenario("CF-SRC-4", pid, 2024, expense, new BigDecimal("1000"));
            Long targetId = seedDraftScenario("CF-TGT-4", pid, 2024, expense);
            return new Long[]{pid, sourceId, targetId};
        });
        Long sourceId = ids[1];
        Long targetId = ids[2];

        ErpFinBudgetScenario updated = ormTemplate.runInSession(session ->
                scenarioBiz.carryForward(sourceId, targetId, ErpFinConstants.BUDGET_CARRY_FORWARD_NONE, CTX));

        assertEquals(ErpFinConstants.BUDGET_STATUS_CLOSED, updated.getDocStatus());

        // NONE：不结转 = 0；目标方案无新 BudgetLine
        BigDecimal carried = findCarryForwardLineAmount(targetId);
        assertEquals(0, carried.compareTo(BigDecimal.ZERO),
                "NONE 不结转金额应为 0");
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

    private Long seedApprovedScenario(String code, Long periodId, int fiscalYear,
                                      ErpMdSubject expense, BigDecimal amount) {
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
        s.setControlLevel(ErpFinConstants.BUDGET_CONTROL_NONE);
        s.setDocStatus(ErpFinConstants.BUDGET_STATUS_APPROVED);
        s.setApproveStatus(ErpFinConstants.BUDGET_STATUS_APPROVED);
        sDao.saveEntity(s);

        IEntityDao<ErpFinBudgetLine> lDao = daoProvider.daoFor(ErpFinBudgetLine.class);
        ErpFinBudgetLine l = new ErpFinBudgetLine();
        l.setScenarioId(s.getId());
        l.setLineNo(1);
        l.setOrgId(1L);
        l.setAcctSchemaId(1L);
        l.setPeriodId(periodId);
        l.setSubjectId(expense.getId());
        l.setSubjectCode(expense.getCode());
        l.setBudgetAmountSource(amount);
        l.setBudgetAmountFunctional(amount);
        l.setCurrencyId(1L);
        l.setExchangeRate(BigDecimal.ONE);
        lDao.saveEntity(l);
        return s.getId();
    }

    private Long seedDraftScenario(String code, Long periodId, int fiscalYear, ErpMdSubject expense) {
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
        s.setControlLevel(ErpFinConstants.BUDGET_CONTROL_NONE);
        s.setDocStatus(ErpFinConstants.BUDGET_STATUS_DRAFT);
        s.setApproveStatus(ErpFinConstants.BUDGET_STATUS_DRAFT);
        sDao.saveEntity(s);
        return s.getId();
    }

    private void seedActualVoucher(String code, Long periodId, ErpMdSubject expense, BigDecimal amount) {
        IEntityDao<ErpFinVoucher> vDao = daoProvider.daoFor(ErpFinVoucher.class);
        ErpFinVoucher v = new ErpFinVoucher();
        v.setCode(code);
        v.setVoucherType("TRANSFER");
        v.setPostingType(ErpFinConstants.POSTING_TYPE_NORMAL);
        v.setVoucherDate(CoreMetrics.today());
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
    }

    /** 找目标方案中由结转产生的 BudgetLine 金额（subjectCode 含 "CARRY-FORWARD-"）。 */
    private BigDecimal findCarryForwardLineAmount(Long targetScenarioId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("scenarioId", targetScenarioId));
        List<ErpFinBudgetLine> lines = daoProvider.daoFor(ErpFinBudgetLine.class).findAllByQuery(q);
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpFinBudgetLine l : lines) {
            if (l.getSubjectCode() != null && l.getSubjectCode().contains("CARRY-FORWARD-")) {
                sum = sum.add(l.getBudgetAmountFunctional() != null
                        ? l.getBudgetAmountFunctional() : BigDecimal.ZERO);
            }
        }
        return sum;
    }

    private int countCarryForwardLogs(Long sourceScenarioId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("sourceScenarioId", sourceScenarioId));
        return daoProvider.daoFor(ErpFinBudgetCarryForwardLog.class).findAllByQuery(q).size();
    }
}
