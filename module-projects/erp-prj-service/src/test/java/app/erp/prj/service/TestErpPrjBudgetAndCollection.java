package app.erp.prj.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdEmployee;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.prj.biz.IErpPrjProjectBiz;
import app.erp.prj.biz.IErpPrjTimesheetBiz;
import app.erp.prj.dao.entity.ErpPrjActivityType;
import app.erp.prj.dao.entity.ErpPrjCostCollection;
import app.erp.prj.dao.entity.ErpPrjCostCollectionLine;
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

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 预算控制 + 成本归集汇总 + 项目状态引用校验端到端单测（Phase 2）。验证：
 * <ul>
 *   <li>预算 WARNING 模式：超预算仅警告放行。</li>
 *   <li>预算 STRICT 模式：超预算抛 {@link ErpPrjErrors#ERR_BUDGET_EXCEEDED}。</li>
 *   <li>工时 APPROVED → 归集行生成（costCategory=LABOR/sourceBillType=TIMESHEET）+ actualCost 回写。</li>
 *   <li>{@code closeProject} 冻结后新工时 submit 拒绝（{@link ErpPrjErrors#ERR_TIMESHEET_PROJECT_NOT_OPEN}）。</li>
 *   <li>引用校验非 OPEN 项目拒绝。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPrjBudgetAndCollection extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpPrjTimesheetBiz timesheetBiz;
    @Inject
    IErpPrjProjectBiz projectBiz;

    @Test
    public void testWarningModeAllowsOverBudget() {
        // 默认 WARNING 模式（不设 system property）
        System.clearProperty(ErpPrjConstants.CONFIG_BUDGET_CONTROL_MODE);
        Long tsId = ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07");
            seedAcctSchema(1L);
            Long debitSubjectId = seedSubject("5101", "项目开发成本");
            Long payrollSubjectId = seedSubject("2211", "应付职工薪酬");
            System.setProperty(ErpPrjConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            Long projectTypeId = seedProjectType("PT-RD", "研发", debitSubjectId);
            // 总预算仅 1000，但工时成本 8000 → 超预算
            Long projectId = seedProject("PRJ-W-001", "WARNING 项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_OPEN, new BigDecimal("1000"));
            Long activityTypeId = seedActivityType("DEV", "开发", "800", null);
            Long taskId = seedTask(projectId, "任务-W", ErpPrjConstants.TASK_STATUS_IN_PROGRESS);
            return seedTimesheet("TS-W-001", projectId, taskId, activityTypeId,
                    "10", "800", ErpPrjConstants.TIMESHEET_STATUS_DRAFT);
        });

        // WARNING 模式：submit 应放行（不抛错）
        ErpPrjTimesheet ts = timesheetBiz.submit(tsId, CTX);
        assertEquals(ErpPrjConstants.TIMESHEET_STATUS_SUBMITTED, ts.getStatus());
    }

    @Test
    public void testStrictModeRejectsOverBudget() {
        System.setProperty(ErpPrjConstants.CONFIG_BUDGET_CONTROL_MODE, "STRICT");
        try {
            Long tsId = ormTemplate.runInSession(session -> {
                seedOpenPeriod("2026-07");
                seedAcctSchema(1L);
                Long debitSubjectId = seedSubject("5101", "项目开发成本");
                Long payrollSubjectId = seedSubject("2211", "应付职工薪酬");
                System.setProperty(ErpPrjConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
                Long projectTypeId = seedProjectType("PT-RD", "研发", debitSubjectId);
                Long projectId = seedProject("PRJ-S-001", "STRICT 项目", projectTypeId,
                        ErpPrjConstants.PROJECT_STATUS_OPEN, new BigDecimal("1000"));
                Long activityTypeId = seedActivityType("DEV", "开发", "800", null);
                Long taskId = seedTask(projectId, "任务-S", ErpPrjConstants.TASK_STATUS_IN_PROGRESS);
                return seedTimesheet("TS-S-001", projectId, taskId, activityTypeId,
                        "10", "800", ErpPrjConstants.TIMESHEET_STATUS_DRAFT);
            });

            // STRICT 模式：超预算应抛 ERR_BUDGET_EXCEEDED
            NopException ex = assertThrows(NopException.class, () -> timesheetBiz.submit(tsId, CTX));
            assertEquals(ErpPrjErrors.ERR_BUDGET_EXCEEDED.getErrorCode(), ex.getErrorCode());
        } finally {
            System.clearProperty(ErpPrjConstants.CONFIG_BUDGET_CONTROL_MODE);
        }
    }

    @Test
    public void testApproveGeneratesCollectionLineAndUpdatesActualCost() {
        Long[] projectHolder = new Long[1];
        Long tsId = ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07");
            seedAcctSchema(1L);
            Long debitSubjectId = seedSubject("5101", "项目开发成本");
            Long payrollSubjectId = seedSubject("2211", "应付职工薪酬");
            System.setProperty(ErpPrjConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            Long projectTypeId = seedProjectType("PT-RD", "研发", debitSubjectId);
            Long projectId = seedProject("PRJ-AGG-001", "归集项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_OPEN, new BigDecimal("100000"));
            projectHolder[0] = projectId;
            Long activityTypeId = seedActivityType("DEV", "开发", "800", debitSubjectId);
            Long taskId = seedTask(projectId, "任务-AGG", ErpPrjConstants.TASK_STATUS_IN_PROGRESS);
            return seedTimesheet("TS-AGG-001", projectId, taskId, activityTypeId,
                    "10", "800", ErpPrjConstants.TIMESHEET_STATUS_DRAFT);
        });

        timesheetBiz.submit(tsId, CTX);
        timesheetBiz.approve(tsId, CTX);

        // 归集行已生成
        ErpPrjCostCollectionLine line = findCollectionLine(
                ErpPrjConstants.SOURCE_BILL_TYPE_TIMESHEET, "TS-AGG-001");
        assertTrue(line != null, "归集行已生成");
        assertEquals(ErpPrjConstants.COST_CATEGORY_LABOR, line.getCostCategory());
        assertEquals(ErpPrjConstants.SOURCE_BILL_TYPE_TIMESHEET, line.getSourceBillType());
        assertEquals("TS-AGG-001", line.getSourceBillCode());
        assertEquals(0, line.getAmount().compareTo(new BigDecimal("8000.0000")),
                "归集金额=costAmount");

        // actualCost 已回写
        ErpPrjProject project = daoProvider.daoFor(ErpPrjProject.class).getEntityById(projectHolder[0]);
        assertEquals(0, project.getActualCost().compareTo(new BigDecimal("8000.0000")),
                "项目 actualCost=8000");
    }

    @Test
    public void testAggregationIsIdempotent() {
        Long tsId = ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07");
            seedAcctSchema(1L);
            Long debitSubjectId = seedSubject("5101", "项目开发成本");
            Long payrollSubjectId = seedSubject("2211", "应付职工薪酬");
            System.setProperty(ErpPrjConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            Long projectTypeId = seedProjectType("PT-RD", "研发", debitSubjectId);
            Long projectId = seedProject("PRJ-IDEM-001", "幂等项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_OPEN, new BigDecimal("100000"));
            Long activityTypeId = seedActivityType("DEV", "开发", "800", null);
            Long taskId = seedTask(projectId, "任务-IDEM", ErpPrjConstants.TASK_STATUS_IN_PROGRESS);
            return seedTimesheet("TS-IDEM-001", projectId, taskId, activityTypeId,
                    "10", "800", ErpPrjConstants.TIMESHEET_STATUS_DRAFT);
        });

        timesheetBiz.submit(tsId, CTX);
        timesheetBiz.approve(tsId, CTX);
        // 再次 approve（已是 APPROVED，幂等返回原对象，不重复归集）
        timesheetBiz.approve(tsId, CTX);

        // 仍然只有一条归集行
        List<ErpPrjCostCollectionLine> lines = findAllCollectionLines(
                ErpPrjConstants.SOURCE_BILL_TYPE_TIMESHEET, "TS-IDEM-001");
        assertEquals(1, lines.size(), "幂等：重复 approve 不重复归集");
    }

    @Test
    public void testCloseProjectFreezesAndRejectsNewTimesheet() {
        Long[] projectHolder = new Long[1];
        ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07");
            seedAcctSchema(1L);
            Long debitSubjectId = seedSubject("5101", "项目开发成本");
            Long payrollSubjectId = seedSubject("2211", "应付职工薪酬");
            System.setProperty(ErpPrjConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            Long projectTypeId = seedProjectType("PT-RD", "研发", debitSubjectId);
            Long projectId = seedProject("PRJ-CLOSE-001", "关闭项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_OPEN, new BigDecimal("100000"));
            projectHolder[0] = projectId;
            Long activityTypeId = seedActivityType("DEV", "开发", "800", null);
            Long taskId = seedTask(projectId, "任务-CLOSE", ErpPrjConstants.TASK_STATUS_IN_PROGRESS);
            seedTimesheet("TS-CLOSE-001", projectId, taskId, activityTypeId,
                    "10", "800", ErpPrjConstants.TIMESHEET_STATUS_DRAFT);
            return null;
        });

        // 关闭项目
        ErpPrjProject closed = projectBiz.closeProject(projectHolder[0], CTX);
        assertEquals(ErpPrjConstants.PROJECT_STATUS_COMPLETED, closed.getStatus());

        // 关闭后新工时 submit 应拒绝
        Long newTsId = ormTemplate.runInSession(session -> {
            Long activityTypeId = daoProvider.daoFor(ErpPrjActivityType.class)
                    .findAllByQuery(new QueryBean().addFilter(eq("code", "DEV"))).get(0).getId();
            Long taskId = daoProvider.daoFor(ErpPrjTask.class)
                    .findAllByQuery(new QueryBean().addFilter(eq("title", "任务-CLOSE"))).get(0).getId();
            return seedTimesheet("TS-CLOSE-002", projectHolder[0], taskId, activityTypeId,
                    "5", "800", ErpPrjConstants.TIMESHEET_STATUS_DRAFT);
        });

        NopException ex = assertThrows(NopException.class, () -> timesheetBiz.submit(newTsId, CTX));
        assertEquals(ErpPrjErrors.ERR_TIMESHEET_PROJECT_NOT_OPEN.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testCloseProjectRejectsNonOpen() {
        Long projectId = ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07");
            seedAcctSchema(1L);
            Long debitSubjectId = seedSubject("5101", "项目开发成本");
            Long projectTypeId = seedProjectType("PT-RD", "研发", debitSubjectId);
            Long pid = seedProject("PRJ-NCL-001", "非OPEN项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_DRAFT, new BigDecimal("100000"));
            return pid;
        });

        NopException ex = assertThrows(NopException.class, () -> projectBiz.closeProject(projectId, CTX));
        assertEquals(ErpPrjErrors.ERR_PROJECT_NOT_CLOSABLE.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testRequireReferenceableRejectsNonOpen() {
        Long projectId = ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07");
            seedAcctSchema(1L);
            Long debitSubjectId = seedSubject("5101", "项目开发成本");
            Long projectTypeId = seedProjectType("PT-RD", "研发", debitSubjectId);
            return seedProject("PRJ-REF-001", "引用校验项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_CANCELLED, new BigDecimal("100000"));
        });

        NopException ex = assertThrows(NopException.class,
                () -> projectBiz.requireReferenceable(projectId, CTX));
        assertEquals(ErpPrjErrors.ERR_PROJECT_NOT_REFERENCEABLE.getErrorCode(), ex.getErrorCode());
    }

    // ---------- seed helpers ----------

    private Long seedTimesheet(String code, Long projectId, Long taskId, Long activityTypeId,
                               String hours, String costRate, int status) {
        IEntityDao<ErpPrjTimesheet> dao = daoProvider.daoFor(ErpPrjTimesheet.class);
        ErpPrjTimesheet ts = new ErpPrjTimesheet();
        ts.setCode(code);
        ts.setOrgId(1L);
        ts.setProjectId(projectId);
        ts.setTaskId(taskId);
        ts.setUserId(seedEmployee());
        ts.setActivityTypeId(activityTypeId);
        ts.setWorkDate(LocalDate.of(2026, 7, 15));
        ts.setHours(hours);
        ts.setCostRate(costRate);
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
        emp.setStatus(10);
        dao.saveEntity(emp);
        return emp.getId();
    }

    private Long seedProject(String code, String name, Long projectTypeId, int status, BigDecimal budget) {
        IEntityDao<ErpPrjProject> dao = daoProvider.daoFor(ErpPrjProject.class);
        ErpPrjProject p = new ErpPrjProject();
        p.setCode(code);
        p.setName(name);
        p.setOrgId(1L);
        p.setProjectTypeId(projectTypeId);
        p.setCurrencyId(1L);
        p.setStatus(status);
        p.setBudget(budget != null ? budget.toPlainString() : null);
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

    private Long seedTask(Long projectId, String title, int status) {
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
        a.setCostRate(costRate);
        a.setSubjectId(subjectId);
        dao.saveEntity(a);
        return a.getId();
    }

    private Long seedSubject(String code, String name) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject s = new ErpMdSubject();
        s.setCode(code);
        s.setName(name);
        s.setSubjectClass(10);
        s.setDirection(10);
        s.setStatus(10);
        dao.saveEntity(s);
        return s.getId();
    }

    private void seedAcctSchema(long orgId) {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = new ErpMdAcctSchema();
        schema.setCode("AS-" + orgId);
        schema.setName("账套-" + orgId);
        schema.setOrgId(orgId);
        schema.setNature(10);
        schema.setFunctionalCurrencyId(1L);
        schema.setStatus(10);
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
        period.setStatus(10);
        dao.saveEntity(period);
    }

    private ErpPrjCostCollectionLine findCollectionLine(String sourceBillType, String sourceBillCode) {
        List<ErpPrjCostCollectionLine> lines = findAllCollectionLines(sourceBillType, sourceBillCode);
        return lines.isEmpty() ? null : lines.get(0);
    }

    private List<ErpPrjCostCollectionLine> findAllCollectionLines(String sourceBillType, String sourceBillCode) {
        IEntityDao<ErpPrjCostCollectionLine> dao = daoProvider.daoFor(ErpPrjCostCollectionLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.and(
                eq("sourceBillType", sourceBillType), eq("sourceBillCode", sourceBillCode)));
        return dao.findAllByQuery(q);
    }
}
