package app.erp.prj.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdEmployee;
import app.erp.md.dao.entity.ErpMdExchangeRate;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.md.service.ErpMdConstants;
import app.erp.prj.biz.IErpPrjTimesheetBiz;
import app.erp.prj.dao.entity.ErpPrjActivityType;
import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.dao.entity.ErpPrjProjectType;
import app.erp.prj.dao.entity.ErpPrjTask;
import app.erp.prj.dao.entity.ErpPrjTimesheet;
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

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 多币种工时过账测试组（RC-R1.64 / P1-MA1-010 业务逻辑层闭合，L1 UC-PRJ-06「多币种折算到统一币种」工时侧）。
 *
 * <p>验证 {@code TimesheetPostingDispatcher.buildEvent} 汇率解析三态 + {@code ProjectCostCollectionProvider}
 * 双字段折算（amountSource/amountFunctional）：
 * <ol>
 *   <li>非本位币（USD 项目 + CNY 本位账套）汇率 seed → approve → 凭证行 amountSource=源币金额 +
 *       amountFunctional=amount×rate + voucherLine.exchangeRate=rate + GL 借贷按本位币（折算失真消除）；</li>
 *   <li>非本位币汇率缺失 → 抛 {@code ERR_EXCHANGE_RATE_REQUIRED} + 单据保持 SUBMITTED
 *       （D1(ii) 选项 α：buildEvent 抛错传播 → approve 事务回滚，无告警派发）+ 凭证/回链零落库；</li>
 *   <li>本位币 / currencyId=null → rate=1 行为保持（source==functional==amount 向后兼容）。</li>
 * </ol>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPrjTimesheetMulticurrencyPosting extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    private static final String CNY_ID = "1";
    private static final String USD_ID = "2";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpPrjTimesheetBiz timesheetBiz;
    @Inject
    app.erp.prj.service.posting.TimesheetPostingDispatcher timesheetPostingDispatcher;

    // ---------- ① 非本位币 + 汇率 seed → 折算过账 ----------

    @Test
    public void testApprovePostsForeignCurrencyVoucherWithResolvedRate() {
        final String tsCode = "TS-FX-001";
        BigDecimal rate = new BigDecimal("7.0");
        String tsId = ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07");
            seedAcctSchema("1");
            seedCurrency(CNY_ID, "CNY", true);
            seedCurrency(USD_ID, "USD", false);
            seedExchangeRate(USD_ID, CNY_ID, rate, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
            String debitSubjectId = seedSubject("5101", "项目开发成本");
            String payrollSubjectId = seedSubject("2211", "应付职工薪酬");
            seedConfigSubject(payrollSubjectId);
            String projectTypeId = seedProjectType("PT-RD-FX", "研发项目-外币", debitSubjectId);
            String projectId = seedProject("PRJ-FX-001", "外币工时过账项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_OPEN, new BigDecimal("100000"));
            String activityTypeId = seedActivityType("DEV-FX", "开发", "300", null);
            String taskId = seedTask(projectId, "任务-外币", ErpPrjConstants.TASK_STATUS_IN_PROGRESS);
            return seedTimesheet(tsCode, projectId, taskId, activityTypeId,
                    "10", "800", ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED, USD_ID);
        });

        ormTemplate.runInSession(() -> timesheetBiz.submit(tsId, CTX));
        ErpPrjTimesheet ts = ormTemplate.runInSession(session -> timesheetBiz.approve(tsId, CTX));
        assertEquals(ErpPrjConstants.APPROVE_STATUS_APPROVED, ts.getStatus());
        assertTrue(Boolean.TRUE.equals(ts.getPosted()), "外币汇率已 seed，过账成功 posted=true");

        List<ErpFinVoucherBillR> links = findBillLinks(tsCode, "PROJECT_COST_COLLECTION");
        assertFalse(links.isEmpty(), "PROJECT_COST_COLLECTION 凭证回链已落库");

        List<ErpFinVoucherLine> lines = findVoucherLines(links.get(0).getVoucherId());
        assertEquals(2, lines.size(), "借贷各一行");
        ErpFinVoucherLine debit = findLineBySubjectCode(lines, "5101");
        ErpFinVoucherLine credit = findLineBySubjectCode(lines, "2211");
        assertNotNull(debit, "存在借方项目成本分录");
        assertNotNull(credit, "存在贷方应付职工薪酬分录");

        BigDecimal sourceAmount = new BigDecimal("8000");
        BigDecimal functionalAmount = sourceAmount.multiply(rate);
        for (ErpFinVoucherLine line : lines) {
            assertEquals(0, line.getAmountSource().compareTo(sourceAmount),
                    "amountSource=源币金额 8000 USD");
            assertEquals(0, line.getAmountFunctional().compareTo(functionalAmount),
                    "amountFunctional=amount×rate=56000（折算失真消除）");
            assertEquals(0, line.getExchangeRate().compareTo(rate),
                    "voucherLine.exchangeRate=解析出的真实汇率 7.0");
            assertEquals(USD_ID, line.getCurrencyId(), "行级币种为源币种 USD");
        }
        assertEquals(0, debit.getDebitAmount().compareTo(functionalAmount),
                "GL 借方按本位币功能金额记账");
        assertEquals(0, credit.getCreditAmount().compareTo(functionalAmount),
                "GL 贷方按本位币功能金额记账");
        assertEquals(0, debit.getDebitAmount().compareTo(credit.getCreditAmount()),
                "GL 借贷平衡保持（试算平衡以本位币为准）");
        assertEquals(debit.getProjectId(), credit.getProjectId(), "借贷 projectId 辅助维度一致");
        assertNotNull(debit.getProjectId(), "projectId 辅助维度已标注");
    }

    // ---------- ② 非本位币汇率缺失 → 拒绝 + 保持 SUBMITTED（D1(ii) 选项 α） ----------

    @Test
    public void testApproveRejectsForeignCurrencyWhenRateMissing() {
        final String tsCode = "TS-FX-REJECT-001";
        String tsId = ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07");
            seedAcctSchema("1");
            seedCurrency(CNY_ID, "CNY", true);
            seedCurrency(USD_ID, "USD", false);
            // 不 seed ErpMdExchangeRate → 汇率缺失
            String debitSubjectId = seedSubject("5101", "项目开发成本");
            String payrollSubjectId = seedSubject("2211", "应付职工薪酬");
            seedConfigSubject(payrollSubjectId);
            String projectTypeId = seedProjectType("PT-RD-FX", "研发项目-外币", debitSubjectId);
            String projectId = seedProject("PRJ-FX-REJECT-001", "汇率缺失项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_OPEN, new BigDecimal("100000"));
            String activityTypeId = seedActivityType("DEV-FX", "开发", "300", null);
            String taskId = seedTask(projectId, "任务-汇率缺失", ErpPrjConstants.TASK_STATUS_IN_PROGRESS);
            return seedTimesheet(tsCode, projectId, taskId, activityTypeId,
                    "10", "800", ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED, USD_ID);
        });

        ormTemplate.runInSession(() -> timesheetBiz.submit(tsId, CTX));

        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> timesheetBiz.approve(tsId, CTX)),
                "非本位币汇率缺失应抛 NopException 拒绝过账（buildEvent 抛错传播）");
        assertEquals(ErpFinErrors.ERR_EXCHANGE_RATE_REQUIRED.getErrorCode(), ex.getErrorCode(),
                "错误码应为 ERR_EXCHANGE_RATE_REQUIRED（复用 R1.42 语义）");
        assertEquals("USD", ex.getParam(ErpFinErrors.ARG_CURRENCY_CODE), "错误参数含币种编码");

        ormTemplate.runInSession(() -> {
            ErpPrjTimesheet ts = daoProvider.daoFor(ErpPrjTimesheet.class).getEntityById(tsId);
            assertEquals(ErpPrjConstants.APPROVE_STATUS_SUBMITTED, ts.getStatus(),
                    "D1(ii) 选项 α：approve 事务回滚，单据保持 SUBMITTED（无悬挂，补录汇率后可重提）");
            assertFalse(Boolean.TRUE.equals(ts.getPosted()), "posted 未置位");
        });
        assertTrue(findBillLinks(tsCode, "PROJECT_COST_COLLECTION").isEmpty(),
                "被拒不应落库凭证回链");
    }

    // ---------- ③a 本位币 → rate=1 行为保持 ----------

    @Test
    public void testFunctionalCurrencyKeepsRateOneBehavior() {
        final String tsCode = "TS-FX-FUNC-001";
        String tsId = ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07");
            seedAcctSchema("1");
            seedCurrency(CNY_ID, "CNY", true);
            String debitSubjectId = seedSubject("5101", "项目开发成本");
            String payrollSubjectId = seedSubject("2211", "应付职工薪酬");
            seedConfigSubject(payrollSubjectId);
            String projectTypeId = seedProjectType("PT-RD-FX", "研发项目-外币", debitSubjectId);
            String projectId = seedProject("PRJ-FX-FUNC-001", "本位币项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_OPEN, new BigDecimal("100000"));
            String activityTypeId = seedActivityType("DEV-FX", "开发", "300", null);
            String taskId = seedTask(projectId, "任务-本位币", ErpPrjConstants.TASK_STATUS_IN_PROGRESS);
            return seedTimesheet(tsCode, projectId, taskId, activityTypeId,
                    "10", "800", ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED, CNY_ID);
        });

        ormTemplate.runInSession(() -> timesheetBiz.submit(tsId, CTX));
        ErpPrjTimesheet ts = ormTemplate.runInSession(session -> timesheetBiz.approve(tsId, CTX));
        assertTrue(Boolean.TRUE.equals(ts.getPosted()), "本位币过账成功 posted=true");

        List<ErpFinVoucherBillR> links = findBillLinks(tsCode, "PROJECT_COST_COLLECTION");
        assertFalse(links.isEmpty(), "凭证回链已落库");
        List<ErpFinVoucherLine> lines = findVoucherLines(links.get(0).getVoucherId());
        for (ErpFinVoucherLine line : lines) {
            assertEquals(0, line.getExchangeRate().compareTo(BigDecimal.ONE), "本位币回退 rate=1");
            assertEquals(0, line.getAmountSource().compareTo(line.getAmountFunctional()),
                    "rate=1 → source==functional（单币种向后兼容）");
            assertEquals(0, line.getAmountFunctional().compareTo(new BigDecimal("8000")),
                    "amount=8000（既有断言零回归）");
        }
    }

    // ---------- ④ F1.1（ai-check P1-CK-fin-003 传导站点回归）：幂等重入不悬挂 ----------

    /**
     * approve 成功（posted=true）后 dispatcher 二次 tryPost（重试/重入场景）——过账引擎幂等命中
     * 现返回既有凭证 id（F1.1 前返回 null），{@code voucherId != null} 判定成立 → 返回 true，
     * posted 不悬挂。修复前本用例失败（幂等命中被误判失败返回 false）。
     */
    @Test
    public void testIdempotentRepostKeepsPostedTrue() {
        final String tsCode = "TS-FX-IDEM-001";
        String tsId = ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07");
            seedAcctSchema("1");
            seedCurrency(CNY_ID, "CNY", true);
            String debitSubjectId = seedSubject("5101", "项目开发成本");
            String payrollSubjectId = seedSubject("2211", "应付职工薪酬");
            seedConfigSubject(payrollSubjectId);
            String projectTypeId = seedProjectType("PT-RD-IDEM", "研发项目-幂等", debitSubjectId);
            String projectId = seedProject("PRJ-FX-IDEM-001", "幂等重入项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_OPEN, new BigDecimal("100000"));
            String activityTypeId = seedActivityType("DEV-FX", "开发", "300", null);
            String taskId = seedTask(projectId, "任务-幂等", ErpPrjConstants.TASK_STATUS_IN_PROGRESS);
            return seedTimesheet(tsCode, projectId, taskId, activityTypeId,
                    "10", "800", ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED, CNY_ID);
        });

        ormTemplate.runInSession(() -> timesheetBiz.submit(tsId, CTX));
        ErpPrjTimesheet ts = ormTemplate.runInSession(session -> timesheetBiz.approve(tsId, CTX));
        assertTrue(Boolean.TRUE.equals(ts.getPosted()), "首次过账成功 posted=true");

        // 幂等重入：同一工时单再次 tryPost（模拟 sweep 重试/重入）——引擎幂等命中返回既有凭证 id
        Boolean reposted = ormTemplate.runInSession(session ->
                timesheetPostingDispatcher.tryPost(daoProvider.daoFor(ErpPrjTimesheet.class).getEntityById(tsId)));
        assertEquals(Boolean.TRUE, reposted,
                "F1.1：幂等重入应返回 true（引擎返回既有凭证 id，voucherId != null 成立）——修复前因 null 返回 false 造成 posted 悬挂");

        // 凭证仍只 1 张（幂等不重复建凭证）
        List<ErpFinVoucherBillR> links = findBillLinks(tsCode, "PROJECT_COST_COLLECTION");
        assertEquals(1, links.size(), "幂等重入不产生第二张凭证/回链");
    }

    // ---------- ③b currencyId=null → rate=1 行为保持 ----------
    // 派发器层断言见 TestTimesheetFxRateResolution（posting 包，protected 直调）：
    // e2e 侧 currencyId=null 的 PostingEvent 因 ErpFinVoucherLine.currencyId NOT NULL（finance ORM 既有约束）
    // 无法落库（先于本行的既有行为，非本行回归面），故 rate=1 回退语义在派发器层单测覆盖。

    // ---------- seed helpers ----------

    private String seedTimesheet(String code, String projectId, String taskId, String activityTypeId,
                               String hours, String costRate, String status, String currencyId) {
        IEntityDao<ErpPrjTimesheet> dao = daoProvider.daoFor(ErpPrjTimesheet.class);
        ErpPrjTimesheet ts = new ErpPrjTimesheet();
        ts.setCode(code);
        ts.setOrgId("1");
        ts.setProjectId(projectId);
        ts.setTaskId(taskId);
        ts.setUserId(seedEmployee());
        ts.setActivityTypeId(activityTypeId);
        ts.setWorkDate(LocalDate.of(2026, 7, 15));
        ts.setHours(hours != null ? new BigDecimal(hours) : null);
        ts.setCostRate(costRate != null ? new BigDecimal(costRate) : null);
        ts.setCurrencyId(currencyId);
        ts.setStatus(status);
        dao.saveEntity(ts);
        return ts.getId();
    }

    private String seedEmployee() {
        IEntityDao<ErpMdEmployee> dao = daoProvider.daoFor(ErpMdEmployee.class);
        ErpMdEmployee emp = new ErpMdEmployee();
        emp.setCode("EMP-" + System.nanoTime());
        emp.setName("测试员工");
        emp.setOrgId("1");
        emp.setStatus(ErpMdConstants.ACTIVE_STATUS_ACTIVE);
        dao.saveEntity(emp);
        return emp.getId();
    }

    private String seedProject(String code, String name, String projectTypeId, String status, BigDecimal budget) {
        IEntityDao<ErpPrjProject> dao = daoProvider.daoFor(ErpPrjProject.class);
        ErpPrjProject p = new ErpPrjProject();
        p.setCode(code);
        p.setName(name);
        p.setOrgId("1");
        p.setProjectTypeId(projectTypeId);
        p.setCurrencyId(CNY_ID);
        p.setStatus(status);
        p.setBudget(budget);
        p.setActualCost(BigDecimal.ZERO);
        dao.saveEntity(p);
        return p.getId();
    }

    private String seedProjectType(String code, String name, String defaultSubjectId) {
        IEntityDao<ErpPrjProjectType> dao = daoProvider.daoFor(ErpPrjProjectType.class);
        ErpPrjProjectType t = new ErpPrjProjectType();
        t.setCode(code);
        t.setName(name);
        t.setDefaultSubjectId(defaultSubjectId);
        dao.saveEntity(t);
        return t.getId();
    }

    private String seedTask(String projectId, String title, String status) {
        IEntityDao<ErpPrjTask> dao = daoProvider.daoFor(ErpPrjTask.class);
        ErpPrjTask task = new ErpPrjTask();
        task.setProjectId(projectId);
        task.setTitle(title);
        task.setStatus(status);
        dao.saveEntity(task);
        return task.getId();
    }

    private String seedActivityType(String code, String name, String costRate, String subjectId) {
        IEntityDao<ErpPrjActivityType> dao = daoProvider.daoFor(ErpPrjActivityType.class);
        ErpPrjActivityType a = new ErpPrjActivityType();
        a.setCode(code);
        a.setName(name);
        a.setCostRate(costRate != null ? new BigDecimal(costRate) : null);
        a.setSubjectId(subjectId);
        dao.saveEntity(a);
        return a.getId();
    }

    private String seedSubject(String code, String name) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject s = new ErpMdSubject();
        s.setCode(code);
        s.setName(name);
        s.setSubjectClass("ASSET");
        s.setDirection(ErpFinConstants.DC_DEBIT);
        s.setStatus(ErpMdConstants.ACTIVE_STATUS_ACTIVE);
        dao.saveEntity(s);
        return s.getId();
    }

    private void seedCurrency(String id, String code, boolean isFunctional) {
        IEntityDao<ErpMdCurrency> dao = daoProvider.daoFor(ErpMdCurrency.class);
        ErpMdCurrency currency = new ErpMdCurrency();
        currency.setId(id);
        currency.setCode(code);
        currency.setName(code);
        currency.setIsFunctional(isFunctional);
        dao.saveEntity(currency);
    }

    private void seedExchangeRate(String fromCurrencyId, String toCurrencyId, BigDecimal rate,
                                  LocalDate validFrom, LocalDate validTo) {
        IEntityDao<ErpMdExchangeRate> dao = daoProvider.daoFor(ErpMdExchangeRate.class);
        ErpMdExchangeRate rateRow = new ErpMdExchangeRate();
        rateRow.setFromCurrencyId(fromCurrencyId);
        rateRow.setToCurrencyId(toCurrencyId);
        rateRow.setRateType("SPOT");
        rateRow.setRate(rate);
        rateRow.setValidFrom(validFrom);
        rateRow.setValidTo(validTo);
        dao.saveEntity(rateRow);
    }

    private void seedAcctSchema(String orgId) {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = new ErpMdAcctSchema();
        schema.setCode("AS-" + orgId);
        schema.setName("账套-" + orgId);
        schema.setOrgId(orgId);
        schema.setNature("FINANCIAL");
        schema.setFunctionalCurrencyId(CNY_ID);
        schema.setStatus(ErpMdConstants.ACTIVE_STATUS_ACTIVE);
        dao.saveEntity(schema);
    }

    private void seedOpenPeriod(String code) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(code);
        period.setName(code);
        period.setOrgId("1");
        period.setYear(2026);
        period.setMonth(7);
        period.setStartDate(LocalDate.of(2026, 7, 1));
        period.setEndDate(LocalDate.of(2026, 7, 31));
        period.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN);
        dao.saveEntity(period);
    }

    private void seedConfigSubject(String payrollSubjectId) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject s = dao.getEntityById(payrollSubjectId);
        if (s != null) {
            System.setProperty(ErpPrjConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, s.getCode());
        }
        System.clearProperty(ErpPrjConstants.CONFIG_DEFAULT_LABOR_COST_RATE);
    }

    private List<ErpFinVoucherBillR> findBillLinks(String billCode, String businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billCode), eq("businessType", businessType)));
        return dao.findAllByQuery(q);
    }

    private List<ErpFinVoucherLine> findVoucherLines(String voucherId) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return dao.findAllByQuery(q);
    }

    private ErpFinVoucherLine findLineBySubjectCode(List<ErpFinVoucherLine> lines, String code) {
        for (ErpFinVoucherLine l : lines) {
            if (code.equals(l.getSubjectCode())) {
                return l;
            }
        }
        return null;
    }
}
