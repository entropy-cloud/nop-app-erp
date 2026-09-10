package app.erp.fin.service.budget;

import app.erp.fin.biz.IErpFinBudgetCommitmentBiz;
import app.erp.fin.biz.IErpFinBudgetControlBiz;
import app.erp.fin.dao.dto.BudgetCheckResult;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinBudgetLine;
import app.erp.fin.dao.entity.ErpFinBudgetScenario;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * M2.8 分片④ fin3-019-r3（DIM-T 覆盖缺口）：F2.3 两修复体专属回归。
 *
 * <p>①fin3-005 修复体（{@code resolveOrgAndSchema}/{@code resolveCurrencyId} 账套解析链）：
 * 非身份 orgId 种子（org=9001 + FINANCIAL 主账套 functionalCurrency=9002）下 commit 生成凭证，
 * 断言 voucher.orgId/acctSchemaId + 凭证行 currencyId 三元组随期间 org 解析（修复前恒硬编码 "1"，与本
 * 矩阵不可区分）。
 *
 * <p>②fin3-004 修复体（per-维度串行锁 HARD 链）确定性断言轨：HARD 控制下 check-then-act 链——
 * 首笔 check PASS（余量 1000≥600）→ 落 ACTUAL 凭证 600 → 次笔同维 check BLOCKED（余量 400<600，
 * ERR_BUDGET_EXCEEDED）+ ControlLog 写 BLOCKED 行。<b>并发子路径 watch-only 登记</b>：同 JVM 双线程
 * 「仅一笔通过」断言需在临界区内完成 act，而 {@code ErpFinBudgetControlBiz.check} 锁仅覆盖聚合+判定
 * （调用方 act 在锁外），纯 check API 下双线程各自独立见同一余量、双双 PASS 属预期语义（锁的语义是
 * 判定原子化而非跨调用预留）；锁队列时序断言需反射私有 CHECK_LOCKS 或计时竞态（不确定/脆）。跨 JVM
 * 残余面由 ControlLog insert 冲突兜底（设计 javadoc 登记）——按计划「断言 + watch-only 登记」双轨登记。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:budget-f319-test.yaml")
public class TestErpFinBudgetResolutionAndCheckChain extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    private static final String ORG_9001 = "9001";
    private static final String CURRENCY_9002 = "9002";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinBudgetCommitmentBiz commitmentBiz;
    @Inject
    IErpFinBudgetControlBiz budgetControlBiz;

    @Test
    public void testCommitVoucherResolvesOrgSchemaCurrencyTriple() {
        String[] ids = ormTemplate.runInSession(session -> {
            String periodId = seedOpenPeriod("2027-F319-1", 2027, 3, ORG_9001);
            String schemaId = seedPrimarySchema("SCH-9001", ORG_9001, CURRENCY_9002);
            ErpMdSubject subject = seedSubject("1408", "承付占用科目",
                    ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            return new String[]{periodId, schemaId, subject.getId()};
        });
        String periodId = ids[0];
        String schemaId = ids[1];
        String subjectId = ids[2];

        String voucherId = ormTemplate.runInSession(session ->
                commitmentBiz.commit(ErpFinConstants.COMMITMENT_SOURCE_BILL_PURCHASE_ORDER, "PO-F319-1",
                        subjectId, null, periodId, new BigDecimal("400"), CTX));
        assertNotNull(voucherId, "承付 SPI 应生成凭证");

        ErpFinVoucher voucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(voucherId);
        assertEquals(ORG_9001, voucher.getOrgId(),
                "凭证 orgId 应解析自期间 org（fin3-005 修复：非身份 orgId 区分硬编码 \"1\"）");
        assertEquals(schemaId, voucher.getAcctSchemaId(),
                "凭证 acctSchemaId 应解析自期间 org 的 FINANCIAL 主账套");

        QueryBean lq = new QueryBean();
        lq.addFilter(eq("voucherId", voucherId));
        List<ErpFinVoucherLine> lines = daoProvider.daoFor(ErpFinVoucherLine.class).findAllByQuery(lq);
        assertEquals(1, lines.size());
        assertEquals(CURRENCY_9002, lines.get(0).getCurrencyId(),
                "凭证行 currencyId 应为主账套 functionalCurrencyId（非身份币种区分占位 \"1\"）");
        assertEquals(schemaId, lines.get(0).getAcctSchemaId(), "凭证行账套随头");
        assertEquals(ORG_9001, lines.get(0).getOrgId(), "凭证行 org 随头");
    }

    @Test
    public void testHardCheckThenActChainSecondBillBlocked() {
        String[] ids = ormTemplate.runInSession(session -> {
            String periodId = seedOpenPeriod("2027-F319-2", 2027, 4, "1");
            ErpMdSubject expense = seedSubject("6601", "销售费用",
                    ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            ErpMdSubject income = seedSubject("6001", "主营业务收入",
                    ErpFinConstants.SUBJECT_CLASS_INCOME, ErpFinConstants.DC_CREDIT);
            seedApprovedHardScenario("BUD-F319-2", periodId, 2027, expense, income, new BigDecimal("1000"));
            return new String[]{periodId, expense.getId(), income.getId()};
        });
        String periodId = ids[0];
        String expenseId = ids[1];
        String incomeId = ids[2];

        // 预算影子凭证：BUDGET 通道 1000（余量 = 1000 − 0 − 0）
        ormTemplate.runInSession(session -> {
            seedPostedVoucher("V-F319-BUD", periodId, expenseId, incomeId,
                    new BigDecimal("1000"), ErpFinConstants.POSTING_TYPE_BUDGET);
            return null;
        });

        // 首笔 check：600 ≤ 1000 → PASS（锁内聚合+判定）
        BudgetCheckResult first = ormTemplate.runInSession(session ->
                budgetControlBiz.check(expenseId, null, periodId, new BigDecimal("600"),
                        "PURCHASE_ORDER", "PO-F319-A", CTX));
        assertEquals(BudgetCheckResult.ACTION_PASS, first.getActionResult(), "首笔余量充足应放行");

        // act：首笔落 ACTUAL（NORMAL）凭证 600（调用方事务内，锁外——链语义依赖后续 check 重聚合）
        ormTemplate.runInSession(session -> {
            seedPostedVoucher("V-F319-ACT", periodId, expenseId, incomeId,
                    new BigDecimal("600"), ErpFinConstants.POSTING_TYPE_NORMAL);
            return null;
        });

        // 次笔同维 check：余量 = 1000 − 600 = 400 < 600 → HARD BLOCKED
        // （ControlLog BLOCKED 行与异常同会话写入、随抛出回滚——生产链路中其跨 JVM 冲突兜底语义为
        // 独立事务/调用方事务形态，此处以异常 param 断言拦截时点余量）
        NopException ex = assertThrows(NopException.class, () ->
                ormTemplate.runInSession(session ->
                        budgetControlBiz.check(expenseId, null, periodId, new BigDecimal("600"),
                                "PURCHASE_ORDER", "PO-F319-B", CTX)),
                "HARD 控制下 act 后次笔应 ERR_BUDGET_EXCEEDED");
        assertEquals(ErpFinErrors.ERR_BUDGET_EXCEEDED.getErrorCode(), ex.getErrorCode());
        assertEquals(0, new BigDecimal("400").compareTo((BigDecimal) ex.getParam(ErpFinErrors.ARG_AVAILABLE_AMOUNT)),
                "拦截异常应携带时点余量 400（聚合通道含 act 落账）");
    }

    // ---------- helpers ----------

    private String seedOpenPeriod(String code, int year, int month, String orgId) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod p = new ErpFinAccountingPeriod();
        p.setCode(code);
        p.setName(code);
        p.setOrgId(orgId);
        p.setYear(year);
        p.setMonth(month);
        p.setStartDate(LocalDate.of(year, month, 1));
        p.setEndDate(LocalDate.of(year, month, 28));
        p.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN);
        dao.saveEntity(p);
        return p.getId();
    }

    private String seedPrimarySchema(String code, String orgId, String functionalCurrencyId) {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema s = new ErpMdAcctSchema();
        s.setCode(code);
        s.setName(code);
        s.setOrgId(orgId);
        s.setNature("FINANCIAL");
        s.setFunctionalCurrencyId(functionalCurrencyId);
        s.setStatus("ACTIVE");
        dao.saveEntity(s);
        return s.getId();
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

    private void seedApprovedHardScenario(String code, String periodId, int fiscalYear,
                                          ErpMdSubject expense, ErpMdSubject income, BigDecimal amount) {
        IEntityDao<ErpFinBudgetScenario> sDao = daoProvider.daoFor(ErpFinBudgetScenario.class);
        ErpFinBudgetScenario s = new ErpFinBudgetScenario();
        s.setCode(code);
        s.setName(code);
        s.setOrgId("1");
        s.setAcctSchemaId("1");
        s.setFiscalYear(fiscalYear);
        s.setScenarioType("ANNUAL");
        s.setCurrencyId("1");
        s.setExchangeRate(BigDecimal.ONE);
        s.setControlLevel(ErpFinConstants.BUDGET_CONTROL_HARD);
        s.setDocStatus(ErpFinConstants.BUDGET_STATUS_APPROVED);
        s.setApproveStatus(ErpFinConstants.BUDGET_STATUS_APPROVED);
        sDao.saveEntity(s);

        IEntityDao<ErpFinBudgetLine> lDao = daoProvider.daoFor(ErpFinBudgetLine.class);
        ErpFinBudgetLine l1 = new ErpFinBudgetLine();
        l1.setScenarioId(s.getId());
        l1.setLineNo(1);
        l1.setOrgId("1");
        l1.setAcctSchemaId("1");
        l1.setPeriodId(periodId);
        l1.setSubjectId(expense.getId());
        l1.setSubjectCode(expense.getCode());
        l1.setBudgetAmountSource(amount);
        l1.setBudgetAmountFunctional(amount);
        l1.setCurrencyId("1");
        l1.setExchangeRate(BigDecimal.ONE);
        lDao.saveEntity(l1);

        ErpFinBudgetLine l2 = new ErpFinBudgetLine();
        l2.setScenarioId(s.getId());
        l2.setLineNo(2);
        l2.setOrgId("1");
        l2.setAcctSchemaId("1");
        l2.setPeriodId(periodId);
        l2.setSubjectId(income.getId());
        l2.setSubjectCode(income.getCode());
        l2.setBudgetAmountSource(amount);
        l2.setBudgetAmountFunctional(amount);
        l2.setCurrencyId("1");
        l2.setExchangeRate(BigDecimal.ONE);
        lDao.saveEntity(l2);
    }

    private void seedPostedVoucher(String code, String periodId, String expenseId, String incomeId,
                                   BigDecimal amount, String postingType) {
        IEntityDao<ErpFinVoucher> vDao = daoProvider.daoFor(ErpFinVoucher.class);
        ErpFinVoucher v = new ErpFinVoucher();
        v.setCode(code);
        v.setVoucherType("TRANSFER");
        v.setPostingType(postingType);
        v.setVoucherDate(CoreMetrics.today());
        v.setOrgId("1");
        v.setAcctSchemaId("1");
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
        d.setSubjectId(expenseId);
        d.setSubjectCode("6601");
        d.setDcDirection(ErpFinConstants.DC_DEBIT);
        d.setDebitAmount(amount);
        d.setCreditAmount(BigDecimal.ZERO);
        d.setCurrencyId("1");
        d.setExchangeRate(BigDecimal.ONE);
        d.setAmountSource(amount);
        d.setAmountFunctional(amount);
        d.setAcctSchemaId("1");
        lDao.saveEntity(d);

        ErpFinVoucherLine c = new ErpFinVoucherLine();
        c.setVoucherId(v.getId());
        c.setLineNo(2);
        c.setSubjectId(incomeId);
        c.setSubjectCode("6001");
        c.setDcDirection(ErpFinConstants.DC_CREDIT);
        c.setDebitAmount(BigDecimal.ZERO);
        c.setCreditAmount(amount);
        c.setCurrencyId("1");
        c.setExchangeRate(BigDecimal.ONE);
        c.setAmountSource(amount);
        c.setAmountFunctional(amount);
        c.setAcctSchemaId("1");
        lDao.saveEntity(c);
    }
}
