package app.erp.hr.service;

import app.erp.hr.biz.IErpHrTimesheetBiz;
import app.erp.hr.biz.IErpHrTimesheetLineBiz;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrTimesheet;
import app.erp.hr.dao.entity.ErpHrTimesheetLine;
import io.nop.api.core.annotations.autotest.EnableSnapshot;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.auth.core.login.UserContextImpl;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 工时单族测试（use-cases.md UC-HR-03，RC-R1.8 P1-RC-015 + P1-MA2-043）。覆盖：
 * <ul>
 *   <li>① 行保存同日 Σ>24 拒绝（ERR_TIMESHEET_DAILY_HOURS_EXCEEDED）+ =24 边界放行。</li>
 *   <li>② 行更新后同日 Σ>24 拒绝（defaultPrepareUpdate 守卫）。</li>
 *   <li>③ 跨表口径命中：员工同一日期跨两张工时表合计 &gt;24 拒绝。</li>
 *   <li>④ totalHours 派生汇总：行增/改/删后与 Σ 一致（stale 场景有断言）。</li>
 *   <li>⑤ approve SUBMITTED→APPROVED + 审计字段（updatedBy/updateTime 非空）。</li>
 *   <li>⑥ reject reason 必填 + REJECTED→submit 重新提交→再 approve 全链。</li>
 *   <li>⑦ 非法迁移拒绝（DRAFT approve/reject / APPROVED submit / REJECTED approve）。</li>
 *   <li>⑧ GraphQL 冒烟：ErpHrTimesheet__submit/reject/approve 经 graphQLEngine.executeRpc 可达。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpHrTimesheetFamily extends JunitAutoTestCase {

    @RegisterExtension
    static HrFrozenClockExtension frozenClock = new HrFrozenClockExtension();

    private static final IServiceContext CTX = new ServiceContextImpl();
    private static final LocalDate REF = HrFrozenClockExtension.REFERENCE_DATE;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpHrTimesheetBiz timesheetBiz;
    @Inject
    IErpHrTimesheetLineBiz timesheetLineBiz;
    @Inject
    IGraphQLEngine graphQLEngine;

    private IUserContext prevUserContext;

    @BeforeEach
    void injectUserContext() {
        prevUserContext = IUserContext.get();
        UserContextImpl user = new UserContextImpl();
        user.setUserId("ts-test");
        user.setUserName("ts-test");
        user.setRoles(Set.of("STAFF"));
        IUserContext.set(user);
    }

    @AfterEach
    void restoreUserContext() {
        IUserContext.set(prevUserContext);
    }

    @Test
    public void testLineSaveDailyHoursExceededAndBoundary() {
        String[] seeded = ormTemplate.runInSession(session -> {
            String empId = seedEmployee("EMP-TS-BOUND");
            String tsId = seedTimesheet(empId, "TS-BOUND", ErpHrConstants.TIMESHEET_STATUS_DRAFT);
            return new String[]{empId, tsId};
        });
        String empId = seeded[0];
        String tsId = seeded[1];

        String l1 = ormTemplate.runInSession(session -> saveLine(tsId, empId, REF, "20"));
        assertNotNull(l1, "首行 20h 应保存成功");

        String l2 = ormTemplate.runInSession(session -> saveLine(tsId, empId, REF, "4"));
        assertNotNull(l2, "第二行 4h 合计=24 边界应放行");

        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> saveLine(tsId, empId, REF, "1")));
        assertEquals(ErpHrErrors.ERR_TIMESHEET_DAILY_HOURS_EXCEEDED.getErrorCode(), ex.getErrorCode(),
                "第三行 1h 合计=25 应抛 ERR_TIMESHEET_DAILY_HOURS_EXCEEDED");
        assertEquals(2, countLines(tsId), "拒绝行不应落库");
    }

    @Test
    public void testLineUpdateDailyHoursExceededRejected() {
        String[] seeded = ormTemplate.runInSession(session -> {
            String empId = seedEmployee("EMP-TS-UPD");
            String tsId = seedTimesheet(empId, "TS-UPD", ErpHrConstants.TIMESHEET_STATUS_DRAFT);
            String l1 = seedLine(tsId, empId, REF, new BigDecimal("20"));
            String l2 = seedLine(tsId, empId, REF, new BigDecimal("4"));
            return new String[]{tsId, l1, l2};
        });
        String tsId = seeded[0];
        String l2 = seeded[2];

        Map<String, Object> data = new HashMap<>();
        data.put("id", String.valueOf(l2));
        data.put("hours", new BigDecimal("5"));
        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> timesheetLineBiz.update(data, CTX)));
        assertEquals(ErpHrErrors.ERR_TIMESHEET_DAILY_HOURS_EXCEEDED.getErrorCode(), ex.getErrorCode(),
                "更新后 20+5=25 应拒绝");
        assertEquals(new BigDecimal("4.00"), reloadLine(l2).getHours(), "拒绝后行值应保持原值");

        data.put("hours", new BigDecimal("3"));
        ormTemplate.runInSession(session -> timesheetLineBiz.update(data, CTX));
        assertEquals(new BigDecimal("3.00"), reloadLine(l2).getHours(), "更新后 20+3=23 应放行");
    }

    @Test
    public void testDailyHoursExceededCrossTimesheetScope() {
        String[] seeded = ormTemplate.runInSession(session -> {
            String empId = seedEmployee("EMP-TS-CROSS");
            String ts1 = seedTimesheet(empId, "TS-CROSS-1", ErpHrConstants.TIMESHEET_STATUS_DRAFT);
            String ts2 = seedTimesheet(empId, "TS-CROSS-2", ErpHrConstants.TIMESHEET_STATUS_DRAFT);
            seedLine(ts1, empId, REF, new BigDecimal("10"));
            return new String[]{empId, ts2};
        });
        String empId = seeded[0];
        String ts2 = seeded[1];

        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> saveLine(ts2, empId, REF, "15")));
        assertEquals(ErpHrErrors.ERR_TIMESHEET_DAILY_HOURS_EXCEEDED.getErrorCode(), ex.getErrorCode(),
                "跨表口径：另一工时表同员工同日期 10h + 本表 15h = 25 应拒绝");
        assertEquals(0, countLines(ts2), "被拒行不应落库到 ts2");
    }

    @EnableSnapshot(checkOutput = false)
    @Test
    public void testTotalHoursRecomputedAfterLineChanges() {
        String[] seeded = ormTemplate.runInSession(session -> {
            String empId = seedEmployee("EMP-TS-SUM");
            String tsId = seedTimesheet(empId, "TS-SUM", ErpHrConstants.TIMESHEET_STATUS_DRAFT);
            String l2 = seedLine(tsId, empId, REF, new BigDecimal("9.5"));
            return new String[]{empId, tsId, l2};
        });
        String empId = seeded[0];
        String tsId = seeded[1];
        String l2 = seeded[2];

        assertNull(reloadTimesheet(tsId).getTotalHours(), "DAO 直 seed 场景 totalHours 为 null（stale 前置）");

        String l1 = ormTemplate.runInSession(session -> saveLine(tsId, empId, REF, "8"));
        assertEquals(new BigDecimal("17.50"), reloadTimesheet(tsId).getTotalHours(),
                "行保存后 totalHours = Σ(8+9.5)");

        Map<String, Object> data = new HashMap<>();
        data.put("id", String.valueOf(l1));
        data.put("hours", new BigDecimal("2"));
        ormTemplate.runInSession(session -> timesheetLineBiz.update(data, CTX));
        assertEquals(new BigDecimal("11.50"), reloadTimesheet(tsId).getTotalHours(),
                "行更新后 totalHours = Σ(2+9.5)");

        ormTemplate.runInSession(session -> timesheetLineBiz.delete(String.valueOf(l2), CTX));
        assertEquals(new BigDecimal("2.00"), reloadTimesheet(tsId).getTotalHours(),
                "行删除后 totalHours = Σ(2)");
    }

    @Test
    public void testApproveFromSubmittedSetsAuditFields() {
        String tsId = ormTemplate.runInSession(session -> {
            String empId = seedEmployee("EMP-TS-APP");
            return seedTimesheet(empId, "TS-APP", ErpHrConstants.TIMESHEET_STATUS_DRAFT);
        });

        ormTemplate.runInSession(session -> timesheetBiz.submit(tsId, CTX));
        ErpHrTimesheet approved = ormTemplate.runInSession(session -> timesheetBiz.approve(tsId, CTX));
        assertEquals(ErpHrConstants.TIMESHEET_STATUS_APPROVED, approved.getStatus());
        assertEquals(ErpHrConstants.TIMESHEET_STATUS_APPROVED, reloadTimesheet(tsId).getStatus(),
                "approve 后状态应持久化 APPROVED");
        assertNotNull(approved.getUpdatedBy(), "approve 应写入 updatedBy 审计字段");
        assertNotNull(approved.getUpdateTime(), "approve 应写入 updateTime 审计字段");
    }

    @Test
    public void testRejectReasonRequiredAndResubmitChain() {
        String tsId = ormTemplate.runInSession(session -> {
            String empId = seedEmployee("EMP-TS-REJ");
            return seedTimesheet(empId, "TS-REJ", ErpHrConstants.TIMESHEET_STATUS_DRAFT);
        });
        ormTemplate.runInSession(session -> timesheetBiz.submit(tsId, CTX));

        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> timesheetBiz.reject(tsId, "  ", CTX)));
        assertEquals(ErpHrErrors.ERR_TIMESHEET_REJECT_REASON_REQUIRED.getErrorCode(), ex.getErrorCode(),
                "reason 空白应抛 ERR_TIMESHEET_REJECT_REASON_REQUIRED");
        assertEquals(ErpHrConstants.TIMESHEET_STATUS_SUBMITTED, reloadTimesheet(tsId).getStatus(),
                "拒绝失败后状态应保持 SUBMITTED");

        ErpHrTimesheet rejected = ormTemplate.runInSession(session -> timesheetBiz.reject(tsId, "工时填写错误", CTX));
        assertEquals(ErpHrConstants.TIMESHEET_STATUS_REJECTED, rejected.getStatus());
        assertEquals("工时填写错误", rejected.getRemark(), "reason 应写入 remark");

        ErpHrTimesheet resubmitted = ormTemplate.runInSession(session -> timesheetBiz.submit(tsId, CTX));
        assertEquals(ErpHrConstants.TIMESHEET_STATUS_SUBMITTED, resubmitted.getStatus(),
                "REJECTED 后 submit 应可重新提交");

        ErpHrTimesheet approved = ormTemplate.runInSession(session -> timesheetBiz.approve(tsId, CTX));
        assertEquals(ErpHrConstants.TIMESHEET_STATUS_APPROVED, approved.getStatus(),
                "重新提交后再 approve 全链应闭环");
    }

    @Test
    public void testIllegalTransitionsRejected() {
        String tsId = ormTemplate.runInSession(session -> {
            String empId = seedEmployee("EMP-TS-ILL");
            return seedTimesheet(empId, "TS-ILL", ErpHrConstants.TIMESHEET_STATUS_DRAFT);
        });

        NopException exApprove = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> timesheetBiz.approve(tsId, CTX)));
        assertEquals(ErpHrErrors.ERR_HR_TIMESHEET_ILLEGAL_TRANSITION.getErrorCode(), exApprove.getErrorCode(),
                "DRAFT 直接 approve 应拒绝");
        assertEquals(ErpHrConstants.TIMESHEET_STATUS_DRAFT, reloadTimesheet(tsId).getStatus());

        NopException exReject = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> timesheetBiz.reject(tsId, "理由", CTX)));
        assertEquals(ErpHrErrors.ERR_HR_TIMESHEET_ILLEGAL_TRANSITION.getErrorCode(), exReject.getErrorCode(),
                "DRAFT 直接 reject 应拒绝");

        ormTemplate.runInSession(session -> timesheetBiz.submit(tsId, CTX));
        ormTemplate.runInSession(session -> timesheetBiz.approve(tsId, CTX));
        NopException exResubmit = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> timesheetBiz.submit(tsId, CTX)));
        assertEquals(ErpHrErrors.ERR_HR_TIMESHEET_ILLEGAL_TRANSITION.getErrorCode(), exResubmit.getErrorCode(),
                "APPROVED 再 submit 应拒绝");

        String ts2 = ormTemplate.runInSession(session -> {
            String empId = seedEmployee("EMP-TS-ILL2");
            return seedTimesheet(empId, "TS-ILL2", ErpHrConstants.TIMESHEET_STATUS_DRAFT);
        });
        ormTemplate.runInSession(session -> timesheetBiz.submit(ts2, CTX));
        ormTemplate.runInSession(session -> timesheetBiz.reject(ts2, "驳回", CTX));
        NopException exRejectApprove = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> timesheetBiz.approve(ts2, CTX)));
        assertEquals(ErpHrErrors.ERR_HR_TIMESHEET_ILLEGAL_TRANSITION.getErrorCode(), exRejectApprove.getErrorCode(),
                "REJECTED 直接 approve 应拒绝");
    }

    @Test
    public void testGraphQLSmokeSubmitRejectApprove() {
        String tsId = ormTemplate.runInSession(session -> {
            String empId = seedEmployee("EMP-TS-GQL");
            return seedTimesheet(empId, "TS-GQL", ErpHrConstants.TIMESHEET_STATUS_DRAFT);
        });

        ApiResponse<?> submitResp = executeRpc(mutation, "ErpHrTimesheet__submit",
                ApiRequest.build(Map.of("timesheetId", String.valueOf(tsId))));
        assertEquals(0, submitResp.getStatus(), "GraphQL submit 应成功");
        assertEquals(ErpHrConstants.TIMESHEET_STATUS_SUBMITTED, reloadTimesheet(tsId).getStatus());

        ApiResponse<?> rejectResp = executeRpc(mutation, "ErpHrTimesheet__reject",
                ApiRequest.build(Map.of("timesheetId", String.valueOf(tsId), "reason", "GraphQL 驳回")));
        assertEquals(0, rejectResp.getStatus(), "GraphQL reject 应成功");
        ErpHrTimesheet rejected = reloadTimesheet(tsId);
        assertEquals(ErpHrConstants.TIMESHEET_STATUS_REJECTED, rejected.getStatus());
        assertEquals("GraphQL 驳回", rejected.getRemark(), "GraphQL reject 的 reason 应写入 remark");

        ApiResponse<?> resubmitResp = executeRpc(mutation, "ErpHrTimesheet__submit",
                ApiRequest.build(Map.of("timesheetId", String.valueOf(tsId))));
        assertEquals(0, resubmitResp.getStatus(), "GraphQL 重新提交应成功");
        assertEquals(ErpHrConstants.TIMESHEET_STATUS_SUBMITTED, reloadTimesheet(tsId).getStatus());

        ApiResponse<?> approveResp = executeRpc(mutation, "ErpHrTimesheet__approve",
                ApiRequest.build(Map.of("timesheetId", String.valueOf(tsId))));
        assertEquals(0, approveResp.getStatus(), "GraphQL approve 应成功");
        assertEquals(ErpHrConstants.TIMESHEET_STATUS_APPROVED, reloadTimesheet(tsId).getStatus());
    }

    // ---------- helpers ----------

    private ApiResponse<?> executeRpc(io.nop.graphql.core.ast.GraphQLOperationType opType,
                                      String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private String seedEmployee(String code) {
        IEntityDao<ErpHrEmployee> dao = daoProvider.daoFor(ErpHrEmployee.class);
        ErpHrEmployee emp = new ErpHrEmployee();
        emp.setCode(code);
        emp.setFirstName("测");
        emp.setLastName("试");
        emp.setFullName(code);
        emp.setGender("MALE");
        emp.setHireDate(LocalDate.of(2025, 1, 1));
        emp.setEmploymentStatus(ErpHrConstants.EMPLOYMENT_ACTIVE);
        emp.setEmployeeType("FULL_TIME");
        dao.saveEntity(emp);
        return emp.getId();
    }

    private String seedTimesheet(String employeeId, String code, String status) {
        IEntityDao<ErpHrTimesheet> dao = daoProvider.daoFor(ErpHrTimesheet.class);
        ErpHrTimesheet ts = new ErpHrTimesheet();
        ts.setCode(code);
        ts.setEmployeeId(employeeId);
        ts.setPeriodFrom(REF);
        ts.setPeriodTo(REF.plusDays(6));
        ts.setStatus(status);
        ts.setBusinessDate(REF);
        dao.saveEntity(ts);
        return ts.getId();
    }

    private String seedLine(String timesheetId, String employeeId, LocalDate workDate, BigDecimal hours) {
        IEntityDao<ErpHrTimesheetLine> dao = daoProvider.daoFor(ErpHrTimesheetLine.class);
        ErpHrTimesheetLine line = new ErpHrTimesheetLine();
        line.setTimesheetId(timesheetId);
        line.setEmployeeId(employeeId);
        line.setWorkDate(workDate);
        line.setHours(hours);
        dao.saveEntity(line);
        return line.getId();
    }

    private String saveLine(String timesheetId, String employeeId, LocalDate workDate, String hours) {
        Map<String, Object> data = new HashMap<>();
        data.put("timesheetId", timesheetId);
        data.put("employeeId", employeeId);
        data.put("workDate", workDate);
        data.put("hours", new BigDecimal(hours));
        ErpHrTimesheetLine line = timesheetLineBiz.save(data, CTX);
        return line.getId();
    }

    private ErpHrTimesheet reloadTimesheet(String timesheetId) {
        return ormTemplate.runInSession(session -> timesheetBiz.get(String.valueOf(timesheetId), false, CTX));
    }

    private ErpHrTimesheetLine reloadLine(String lineId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("id", lineId));
        q.setLimit(1);
        return ormTemplate.runInSession(session -> {
            List<ErpHrTimesheetLine> list = timesheetLineBiz.findList(q, null, CTX);
            return list.isEmpty() ? null : list.get(0);
        });
    }

    private long countLines(String timesheetId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("timesheetId", timesheetId));
        return ormTemplate.runInSession(session ->
                timesheetLineBiz.findCount(q, CTX));
    }
}
