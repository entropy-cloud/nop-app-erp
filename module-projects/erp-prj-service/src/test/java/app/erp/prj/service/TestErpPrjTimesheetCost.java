package app.erp.prj.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdEmployee;
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
 * 工时成本端到端单测（Phase 1）。验证：
 * <ul>
 *   <li>成本率解析：单填 &gt; 活动类型默认 &gt; 全局默认 &gt; 无费率抛 {@link ErpPrjErrors#ERR_COST_RATE_NOT_AVAILABLE}。</li>
 *   <li>{@code submit}：DRAFT→SUBMITTED，校验项目 OPEN + 任务允许 + costAmount=hours×costRate。</li>
 *   <li>{@code approve}：SUBMITTED→APPROVED + PROJECT_COST_COLLECTION(110) 凭证落库
 *       （借项目成本/贷应付职工薪酬）+ posted=true + projectId 辅助维度。</li>
 *   <li>非法迁移抛 {@link ErpPrjErrors#ERR_TIMESHEET_ILLEGAL_STATUS_TRANSITION}。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPrjTimesheetCost extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpPrjTimesheetBiz timesheetBiz;

    @Test
    public void testCostRateFromTimesheetOverridesActivityType() {
        Long[] ids = new Long[1];
        Long tsId = ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07");
            seedAcctSchema(1L);
            Long debitSubjectId = seedSubject("5101", "项目开发成本");
            Long payrollSubjectId = seedSubject("2211", "应付职工薪酬");
            seedConfigSubject(payrollSubjectId);
            Long projectTypeId = seedProjectType("PT-RD", "研发项目", debitSubjectId);
            Long projectId = seedProject("PRJ-TS-001", "成本率测试项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_OPEN, new BigDecimal("100000"));
            Long activityTypeId = seedActivityType("DEV", "开发", "300", null);
            Long taskId = seedTask(projectId, "任务-001", ErpPrjConstants.TASK_STATUS_IN_PROGRESS);
            ids[0] = projectId;
            // 工时单填 costRate=800（应优先于活动类型 300）
            return seedTimesheet("TS-001", projectId, taskId, activityTypeId,
                    "10", "800", ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED);
        });

        ErpPrjTimesheet ts = timesheetBiz.submit(tsId, CTX);
        assertEquals(ErpPrjConstants.APPROVE_STATUS_SUBMITTED, ts.getStatus());
        // costAmount = 10 × 800 = 8000
        assertEquals(0, ts.getCostAmount().compareTo(new BigDecimal("8000.0000")),
                "costAmount=hours×costRate");
        assertEquals(0, ts.getCostRate().compareTo(new BigDecimal("800")),
                "costRate 取工时单填值");
    }

    @Test
    public void testCostRateFallsBackToActivityType() {
        Long tsId = ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07");
            seedAcctSchema(1L);
            Long debitSubjectId = seedSubject("5101", "项目开发成本");
            Long payrollSubjectId = seedSubject("2211", "应付职工薪酬");
            seedConfigSubject(payrollSubjectId);
            Long projectTypeId = seedProjectType("PT-RD", "研发项目", debitSubjectId);
            Long projectId = seedProject("PRJ-TS-002", "活动类型回退项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_OPEN, new BigDecimal("100000"));
            Long activityTypeId = seedActivityType("DEV", "开发", "300", null);
            Long taskId = seedTask(projectId, "任务-002", ErpPrjConstants.TASK_STATUS_IN_PROGRESS);
            // costRate 留空 → 应取活动类型 300
            return seedTimesheet("TS-002", projectId, taskId, activityTypeId,
                    "10", null, ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED);
        });

        ErpPrjTimesheet ts = timesheetBiz.submit(tsId, CTX);
        // costAmount = 10 × 300 = 3000
        assertEquals(0, ts.getCostAmount().compareTo(new BigDecimal("3000.0000")),
                "回退到活动类型费率 300");
        assertEquals(0, ts.getCostRate().compareTo(new BigDecimal("300")),
                "costRate 回退到活动类型");
    }

    @Test
    public void testCostRateThrowsWhenNoRateAvailable() {
        Long tsId = ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07");
            seedAcctSchema(1L);
            Long debitSubjectId = seedSubject("5101", "项目开发成本");
            Long payrollSubjectId = seedSubject("2211", "应付职工薪酬");
            seedConfigSubject(payrollSubjectId);
            Long projectTypeId = seedProjectType("PT-RD", "研发项目", debitSubjectId);
            Long projectId = seedProject("PRJ-TS-003", "无费率项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_OPEN, new BigDecimal("100000"));
            Long activityTypeId = seedActivityType("DEV-NO-RATE", "开发-无费率", null, null);
            Long taskId = seedTask(projectId, "任务-003", ErpPrjConstants.TASK_STATUS_IN_PROGRESS);
            return seedTimesheet("TS-003", projectId, taskId, activityTypeId,
                    "10", null, ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED);
        });

        NopException ex = assertThrows(NopException.class, () -> timesheetBiz.submit(tsId, CTX));
        assertEquals(ErpPrjErrors.ERR_COST_RATE_NOT_AVAILABLE.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testSubmitRejectsNonOpenProject() {
        Long tsId = ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07");
            seedAcctSchema(1L);
            Long debitSubjectId = seedSubject("5101", "项目开发成本");
            Long payrollSubjectId = seedSubject("2211", "应付职工薪酬");
            seedConfigSubject(payrollSubjectId);
            Long projectTypeId = seedProjectType("PT-RD", "研发项目", debitSubjectId);
            Long projectId = seedProject("PRJ-TS-004", "已完成项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_COMPLETED, new BigDecimal("100000"));
            Long activityTypeId = seedActivityType("DEV", "开发", "300", null);
            Long taskId = seedTask(projectId, "任务-004", ErpPrjConstants.TASK_STATUS_IN_PROGRESS);
            return seedTimesheet("TS-004", projectId, taskId, activityTypeId,
                    "10", "800", ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED);
        });

        NopException ex = assertThrows(NopException.class, () -> timesheetBiz.submit(tsId, CTX));
        assertEquals(ErpPrjErrors.ERR_TIMESHEET_PROJECT_NOT_OPEN.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testSubmitRejectsBlockedTask() {
        Long tsId = ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07");
            seedAcctSchema(1L);
            Long debitSubjectId = seedSubject("5101", "项目开发成本");
            Long payrollSubjectId = seedSubject("2211", "应付职工薪酬");
            seedConfigSubject(payrollSubjectId);
            Long projectTypeId = seedProjectType("PT-RD", "研发项目", debitSubjectId);
            Long projectId = seedProject("PRJ-TS-005", "阻塞任务项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_OPEN, new BigDecimal("100000"));
            Long activityTypeId = seedActivityType("DEV", "开发", "300", null);
            Long taskId = seedTask(projectId, "任务-阻塞", ErpPrjConstants.TASK_STATUS_BLOCKED);
            return seedTimesheet("TS-005", projectId, taskId, activityTypeId,
                    "10", "800", ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED);
        });

        NopException ex = assertThrows(NopException.class, () -> timesheetBiz.submit(tsId, CTX));
        assertEquals(ErpPrjErrors.ERR_TIMESHEET_TASK_NOT_ALLOWED.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testApprovePostsProjectCostCollectionVoucher() {
        final String tsCode = "TS-POST-001";
        Long tsId = ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07");
            seedAcctSchema(1L);
            Long debitSubjectId = seedSubject("5101", "项目开发成本");
            Long payrollSubjectId = seedSubject("2211", "应付职工薪酬");
            seedConfigSubject(payrollSubjectId);
            Long projectTypeId = seedProjectType("PT-RD", "研发项目", debitSubjectId);
            Long projectId = seedProject("PRJ-POST-001", "过账测试项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_OPEN, new BigDecimal("100000"));
            Long activityTypeId = seedActivityType("DEV", "开发", "300", null);
            Long taskId = seedTask(projectId, "任务-过账", ErpPrjConstants.TASK_STATUS_IN_PROGRESS);
            return seedTimesheet(tsCode, projectId, taskId, activityTypeId,
                    "10", "800", ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED);
        });

        timesheetBiz.submit(tsId, CTX);
        ErpPrjTimesheet ts = timesheetBiz.approve(tsId, CTX);
        assertEquals(ErpPrjConstants.APPROVE_STATUS_APPROVED, ts.getStatus());
        assertTrue(Boolean.TRUE.equals(ts.getPosted()), "过账成功 posted=true");

        // PROJECT_COST_COLLECTION(110) 凭证经业财回链可查
        List<ErpFinVoucherBillR> links = findBillLinks(tsCode, "PROJECT_COST_COLLECTION");
        assertFalse(links.isEmpty(), "PROJECT_COST_COLLECTION 凭证回链已落库");

        // 凭证分录：借 5101 项目成本 / 贷 2211 应付职工薪酬，金额=8000
        List<ErpFinVoucherLine> lines = findVoucherLines(links.get(0).getVoucherId());
        assertEquals(2, lines.size(), "借贷各一行");
        ErpFinVoucherLine debit = findLineBySubjectCode(lines, "5101");
        ErpFinVoucherLine credit = findLineBySubjectCode(lines, "2211");
        assertNotNull(debit, "存在借方项目成本分录");
        assertNotNull(credit, "存在贷方应付职工薪酬分录");
        assertEquals(0, debit.getAmountFunctional().compareTo(new BigDecimal("8000.00")),
                "借方金额=8000");
        assertEquals(0, credit.getAmountFunctional().compareTo(new BigDecimal("8000.00")),
                "贷方金额=8000");
        // projectId 辅助维度（cost-collection.md §1.1）
        assertEquals(debit.getProjectId(), credit.getProjectId(), "借贷 projectId 一致");
        assertNotNull(debit.getProjectId(), "projectId 辅助维度已标注");
    }

    @Test
    public void testApproveFromNonSubmittedThrows() {
        Long tsId = ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07");
            seedAcctSchema(1L);
            Long debitSubjectId = seedSubject("5101", "项目开发成本");
            Long payrollSubjectId = seedSubject("2211", "应付职工薪酬");
            seedConfigSubject(payrollSubjectId);
            Long projectTypeId = seedProjectType("PT-RD", "研发项目", debitSubjectId);
            Long projectId = seedProject("PRJ-TS-006", "非法迁移项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_OPEN, new BigDecimal("100000"));
            Long activityTypeId = seedActivityType("DEV", "开发", "300", null);
            Long taskId = seedTask(projectId, "任务-006", ErpPrjConstants.TASK_STATUS_IN_PROGRESS);
            return seedTimesheet("TS-006", projectId, taskId, activityTypeId,
                    "10", "800", ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED);
        });

        // 直接 approve（DRAFT 状态，未 submit）→ 应抛非法迁移
        NopException ex = assertThrows(NopException.class, () -> timesheetBiz.approve(tsId, CTX));
        assertEquals(ErpPrjErrors.ERR_TIMESHEET_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
    }

    // ---------- seed helpers ----------

    private Long seedTimesheet(String code, Long projectId, Long taskId, Long activityTypeId,
                               String hours, String costRate, String status) {
        IEntityDao<ErpPrjTimesheet> dao = daoProvider.daoFor(ErpPrjTimesheet.class);
        ErpPrjTimesheet ts = new ErpPrjTimesheet();
        ts.setCode(code);
        ts.setOrgId(1L);
        ts.setProjectId(projectId);
        ts.setTaskId(taskId);
        ts.setUserId(seedEmployee());
        ts.setActivityTypeId(activityTypeId);
        ts.setWorkDate(LocalDate.of(2026, 7, 15));
        ts.setHours(hours != null ? new BigDecimal(hours) : null);
        ts.setCostRate(costRate != null ? new BigDecimal(costRate) : null);
        ts.setCurrencyId(1L);
        ts.setStatus(status);
        dao.saveEntity(ts);
        return ts.getId();
    }

    private Long seedEmployee() {
        IEntityDao<ErpMdEmployee> dao = daoProvider.daoFor(ErpMdEmployee.class);
        ErpMdEmployee emp = new ErpMdEmployee();
        emp.setCode("EMP-" + System.nanoTime());
        emp.setName("测试员工");
        emp.setOrgId(1L);
        emp.setStatus(ErpMdConstants.ACTIVE_STATUS_ACTIVE);
        dao.saveEntity(emp);
        return emp.getId();
    }

    private Long seedProject(String code, String name, Long projectTypeId, String status, BigDecimal budget) {
        IEntityDao<ErpPrjProject> dao = daoProvider.daoFor(ErpPrjProject.class);
        ErpPrjProject p = new ErpPrjProject();
        p.setCode(code);
        p.setName(name);
        p.setOrgId(1L);
        p.setProjectTypeId(projectTypeId);
        p.setCurrencyId(1L);
        p.setStatus(status);
        p.setBudget(budget);
        p.setActualCost(BigDecimal.ZERO);
        dao.saveEntity(p);
        return p.getId();
    }

    private Long seedProjectType(String code, String name, Long defaultSubjectId) {
        IEntityDao<ErpPrjProjectType> dao = daoProvider.daoFor(ErpPrjProjectType.class);
        ErpPrjProjectType t = new ErpPrjProjectType();
        t.setCode(code);
        t.setName(name);
        t.setDefaultSubjectId(defaultSubjectId);
        dao.saveEntity(t);
        return t.getId();
    }

    private Long seedTask(Long projectId, String title, String status) {
        IEntityDao<ErpPrjTask> dao = daoProvider.daoFor(ErpPrjTask.class);
        ErpPrjTask task = new ErpPrjTask();
        task.setProjectId(projectId);
        task.setTitle(title);
        task.setStatus(status);
        dao.saveEntity(task);
        return task.getId();
    }

    private Long seedActivityType(String code, String name, String costRate, Long subjectId) {
        IEntityDao<ErpPrjActivityType> dao = daoProvider.daoFor(ErpPrjActivityType.class);
        ErpPrjActivityType a = new ErpPrjActivityType();
        a.setCode(code);
        a.setName(name);
        a.setCostRate(costRate != null ? new BigDecimal(costRate) : null);
        a.setSubjectId(subjectId);
        dao.saveEntity(a);
        return a.getId();
    }

    private Long seedSubject(String code, String name) {
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

    private void seedAcctSchema(long orgId) {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = new ErpMdAcctSchema();
        schema.setCode("AS-" + orgId);
        schema.setName("账套-" + orgId);
        schema.setOrgId(orgId);
        schema.setNature("FINANCIAL");
        schema.setFunctionalCurrencyId(1L);
        schema.setStatus(ErpMdConstants.ACTIVE_STATUS_ACTIVE);
        dao.saveEntity(schema);
    }

    private void seedOpenPeriod(String code) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(code);
        period.setName(code);
        period.setOrgId(1L);
        period.setYear(2026);
        period.setMonth(7);
        period.setStartDate(LocalDate.of(2026, 7, 1));
        period.setEndDate(LocalDate.of(2026, 7, 31));
        period.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN);
        dao.saveEntity(period);
    }

    /**
     * 测试用：直接把应付职工薪酬科目编码登记到测试配置覆盖表（写库 NopConfigDao），便于派发器解析。
     * 简化做法：派发器读 {@code AppConfig.var}；测试环境用系统属性注入。
     */
    private void seedConfigSubject(Long payrollSubjectId) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject s = dao.getEntityById(payrollSubjectId);
        if (s != null) {
            System.setProperty(ErpPrjConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, s.getCode());
        }
        // 全局默认费率留空，触发活动类型回退或单填值
        System.clearProperty(ErpPrjConstants.CONFIG_DEFAULT_LABOR_COST_RATE);
    }

    private List<ErpFinVoucherBillR> findBillLinks(String billCode, String businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billCode), eq("businessType", businessType)));
        return dao.findAllByQuery(q);
    }

    private List<ErpFinVoucherLine> findVoucherLines(Long voucherId) {
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
