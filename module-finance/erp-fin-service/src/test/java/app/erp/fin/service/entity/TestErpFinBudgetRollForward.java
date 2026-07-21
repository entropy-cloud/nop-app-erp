package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinBudgetScenarioBiz;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinBudgetLine;
import app.erp.fin.dao.entity.ErpFinBudgetRollforwardLog;
import app.erp.fin.dao.entity.ErpFinBudgetScenario;
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

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A2 预算滚动复制引擎测试（plan 2026-07-21-1206-2 §Phase 2 Proof）。
 *
 * <p>3 策略各 1 测试（budget.md §滚动预算自动复制引擎）：
 * <ul>
 *   <li>FIXED_PERCENTAGE：源 BudgetLine 金额 100% 复制至新 Scenario（newFiscalYear=2025）</li>
 *   <li>ZERO_BASED：仅复制结构，金额清零</li>
 *   <li>INCREMENTAL：按 5% 增长率上调</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:budget-a2-test.yaml")
public class TestErpFinBudgetRollForward extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinBudgetScenarioBiz scenarioBiz;

    @Test
    public void testFixedPercentageStrategy() {
        Long[] ids = seedReturn(() -> {
            Long pid2024 = seedOpenPeriod("2024-06", 2024, 6);
            Long pid2025 = seedOpenPeriod("2025-06", 2025, 6);
            ErpMdSubject expense = seedSubject("6601", "销售费用", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            Long scenarioId = seedApprovedScenario("ROLL-FIX-2024", pid2024, 2024, expense, new BigDecimal("1000"));
            return new Long[]{pid2024, pid2025, scenarioId};
        });
        Long scenarioId = ids[2];

        ErpFinBudgetScenario target = ormTemplate.runInSession(session ->
                scenarioBiz.rollForward(scenarioId, 2025, ErpFinConstants.BUDGET_ROLLFORWARD_FIXED_PERCENTAGE, CTX));

        assertNotNull(target.getId(), "应生成新方案");
        assertEquals(2025, target.getFiscalYear(), "目标年度应为 2025");
        assertEquals(ErpFinConstants.BUDGET_STATUS_DRAFT, target.getDocStatus(), "目标方案 DRAFT 状态");
        assertEquals(scenarioId, target.getParentScenarioId(), "parentScenarioId 指向源方案");

        BigDecimal copiedAmount = findLineAmount(target.getId(), "6601");
        assertEquals(0, copiedAmount.compareTo(new BigDecimal("1000")),
                "FIXED_PERCENTAGE 应 100% 复制金额 = 1000");

        assertTrue(countRollforwardLogs(scenarioId) >= 1, "应写 RollforwardLog");
    }

    @Test
    public void testZeroBasedStrategy() {
        Long[] ids = seedReturn(() -> {
            Long pid2024 = seedOpenPeriod("2024-07", 2024, 7);
            Long pid2025 = seedOpenPeriod("2025-07", 2025, 7);
            ErpMdSubject expense = seedSubject("6602", "管理费用", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            Long scenarioId = seedApprovedScenario("ROLL-ZB-2024", pid2024, 2024, expense, new BigDecimal("800"));
            return new Long[]{pid2024, pid2025, scenarioId};
        });
        Long scenarioId = ids[2];

        ErpFinBudgetScenario target = ormTemplate.runInSession(session ->
                scenarioBiz.rollForward(scenarioId, 2025, ErpFinConstants.BUDGET_ROLLFORWARD_ZERO_BASED, CTX));

        BigDecimal copiedAmount = findLineAmount(target.getId(), "6602");
        assertEquals(0, copiedAmount.compareTo(BigDecimal.ZERO),
                "ZERO_BASED 应仅复制结构金额清零");
    }

    @Test
    public void testIncrementalStrategy() {
        Long[] ids = seedReturn(() -> {
            Long pid2024 = seedOpenPeriod("2024-08", 2024, 8);
            Long pid2025 = seedOpenPeriod("2025-08", 2025, 8);
            ErpMdSubject expense = seedSubject("6603", "研发费用", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            Long scenarioId = seedApprovedScenario("ROLL-INC-2024", pid2024, 2024, expense, new BigDecimal("2000"));
            return new Long[]{pid2024, pid2025, scenarioId};
        });
        Long scenarioId = ids[2];

        ErpFinBudgetScenario target = ormTemplate.runInSession(session ->
                scenarioBiz.rollForward(scenarioId, 2025, ErpFinConstants.BUDGET_ROLLFORWARD_INCREMENTAL, CTX));

        BigDecimal copiedAmount = findLineAmount(target.getId(), "6603");
        // 2000 × (1 + 0.05) = 2100.0000
        assertEquals(0, copiedAmount.compareTo(new BigDecimal("2100.0000")),
                "INCREMENTAL 应按 5% 上调：2000 → 2100.0000");
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

    private BigDecimal findLineAmount(Long scenarioId, String subjectCode) {
        ErpMdSubject s = findSubjectByCode(subjectCode);
        if (s == null) {
            return BigDecimal.ZERO;
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("scenarioId", scenarioId));
        q.addFilter(eq("subjectId", s.getId()));
        List<ErpFinBudgetLine> lines = daoProvider.daoFor(ErpFinBudgetLine.class).findAllByQuery(q);
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpFinBudgetLine l : lines) {
            sum = sum.add(l.getBudgetAmountFunctional() != null ? l.getBudgetAmountFunctional() : BigDecimal.ZERO);
        }
        return sum;
    }

    private int countRollforwardLogs(Long scenarioId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("sourceScenarioId", scenarioId));
        return daoProvider.daoFor(ErpFinBudgetRollforwardLog.class).findAllByQuery(q).size();
    }

    private ErpMdSubject findSubjectByCode(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        List<ErpMdSubject> list = daoProvider.daoFor(ErpMdSubject.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }
}
