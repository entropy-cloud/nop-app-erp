package app.erp.hr.service;

import app.erp.hr.biz.IErpHrAttendanceBiz;
import app.erp.hr.dao.entity.ErpHrAttendance;
import app.erp.hr.dao.entity.ErpHrEmployee;
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
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 手工补卡测试（use-cases.md UC-HR-06⑮「设备故障时支持手工补卡」+ shift-scheduling.md 手工补卡注记，RC-R1.7 P1-RC-014）。
 * 覆盖：
 * <ul>
 *   <li>① makeUpClockIn 新建行（无既有记录）：source=MANUAL + remark=reason + clockIn=补录时间 + businessDate=补卡日期（历史日期）。</li>
 *   <li>② makeUpClockIn 覆盖既有行（同日已有打卡记录）：clockIn 覆盖 + source 改 MANUAL。</li>
 *   <li>③ makeUpClockOut：clockOut 落值 + clockIn/clockOut 均存在时 workHours 重算。</li>
 *   <li>④ reason 空/空白 → ERR_MAKEUP_REASON_REQUIRED。</li>
 *   <li>⑤ HR 角色守卫双侧：显式构造 IUserContext 并经 {@link IUserContext#set} 注入——HR 角色通过 / 无角色被拒 / 无用户上下文被拒。</li>
 *   <li>⑥ 历史日期补卡 → 正确写入该日期行。</li>
 *   <li>⑦ GraphQL 层冒烟：{@code ErpHrAttendance__makeUpClockIn} 经 graphQLEngine.executeRpc 可达（契约可达性证据）。</li>
 * </ul>
 * 角色守卫为 Java 侧 {@code IUserContext.get() + isUserInRole(HR_ROLE_ID)}，与 enableActionAuth 无关——
 * {@code enableActionAuth=FALSE} 下平台不自动构建用户上下文，测试显式注入 roleIds（Decision 选项 A，roleId 与
 * erp-hr.action-auth.xml 菜单 roles 字面一致）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpHrAttendanceMakeUp extends JunitAutoTestCase {

    @RegisterExtension
    static HrFrozenClockExtension frozenClock = new HrFrozenClockExtension();

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpHrAttendanceBiz attendanceBiz;
    @Inject
    IGraphQLEngine graphQLEngine;

    private IUserContext prevUserContext;

    @BeforeEach
    void injectHrUserContext() {
        prevUserContext = IUserContext.get();
        UserContextImpl hr = new UserContextImpl();
        hr.setUserId("hr-makeup");
        hr.setUserName("hr-makeup");
        hr.setRoles(Set.of(ErpHrConstants.HR_ROLE_ID));
        IUserContext.set(hr);
    }

    @AfterEach
    void restoreUserContext() {
        IUserContext.set(prevUserContext);
    }

    @Test
    public void testMakeUpClockInCreatesNewRowOnHistoricalDate() {
        Long empId = ormTemplate.runInSession(session -> seedEmployee("EMP-MK-NEW"));
        LocalDate date = HrFrozenClockExtension.REFERENCE_DATE.minusDays(3);
        Timestamp clockTime = Timestamp.valueOf(LocalDateTime.of(2026, 7, 14, 9, 5, 0));

        ErpHrAttendance after = ormTemplate.runInSession(session ->
                attendanceBiz.makeUpClockIn(empId, date, clockTime, "打卡机故障，手工补录", CTX));

        assertNotNull(after.getId(), "新建行应持久化");
        assertEquals(clockTime, after.getClockIn(), "clockIn 应为补录时间");
        assertNull(after.getClockOut(), "仅补签 clockIn 时 clockOut 保持空");
        assertEquals(ErpHrConstants.ATTENDANCE_SOURCE_MANUAL, after.getSource(), "source 应为 MANUAL");
        assertEquals("打卡机故障，手工补录", after.getRemark(), "remark 应承载 reason");
        assertEquals(date, after.getDate(), "date 应为补卡日期（历史日期）");
        assertEquals(date, after.getBusinessDate(), "businessDate 应对齐补卡日期（非 today 兜底）");

        ErpHrAttendance reloaded = findAttendanceByDate(empId, date);
        assertNotNull(reloaded, "历史日期行应落库");
        assertEquals(clockTime, reloaded.getClockIn(), "持久化 clockIn 应为补录时间");
        assertEquals(ErpHrConstants.ATTENDANCE_SOURCE_MANUAL, reloaded.getSource(), "持久化 source 应为 MANUAL");

        ErpHrAttendance today = ormTemplate.runInSession(session -> attendanceBiz.getTodayAttendance(empId, CTX));
        assertNull(today, "补卡日期为历史日期时今日行不应被误建");
    }

    @Test
    public void testMakeUpClockInOverwritesExistingRow() {
        Long empId = ormTemplate.runInSession(session -> {
            Long emp = seedEmployee("EMP-MK-OVR");
            seedAttendance(emp, HrFrozenClockExtension.REFERENCE_DATE,
                    LocalDateTime.of(2026, 7, 17, 8, 30, 0), null, ErpHrConstants.ATTENDANCE_SOURCE_CARD);
            return emp;
        });
        LocalDate date = HrFrozenClockExtension.REFERENCE_DATE;
        Timestamp newClockTime = Timestamp.valueOf(LocalDateTime.of(2026, 7, 17, 9, 20, 0));

        ErpHrAttendance after = ormTemplate.runInSession(session ->
                attendanceBiz.makeUpClockIn(empId, date, newClockTime, "设备故障二次补录覆盖", CTX));

        assertEquals(newClockTime, after.getClockIn(), "补卡应覆盖既有 clockIn");
        assertEquals(ErpHrConstants.ATTENDANCE_SOURCE_MANUAL, after.getSource(), "覆盖后 source 应改 MANUAL");
        assertEquals("设备故障二次补录覆盖", after.getRemark(), "remark 应更新为本次 reason");
        assertEquals(1, countAttendance(empId), "覆盖不应新建第二行");
    }

    @Test
    public void testMakeUpClockOutRecomputesWorkHours() {
        Long empId = ormTemplate.runInSession(session -> {
            Long emp = seedEmployee("EMP-MK-OUT");
            seedAttendance(emp, HrFrozenClockExtension.REFERENCE_DATE.minusDays(1),
                    LocalDateTime.of(2026, 7, 16, 9, 0, 0), null, ErpHrConstants.ATTENDANCE_SOURCE_CARD);
            return emp;
        });
        LocalDate date = HrFrozenClockExtension.REFERENCE_DATE.minusDays(1);
        Timestamp clockIn = Timestamp.valueOf(LocalDateTime.of(2026, 7, 16, 9, 0, 0));
        Timestamp clockOutTime = Timestamp.valueOf(LocalDateTime.of(2026, 7, 16, 18, 30, 0));

        ErpHrAttendance after = ormTemplate.runInSession(session ->
                attendanceBiz.makeUpClockOut(empId, date, clockOutTime, "签退设备故障补录", CTX));

        assertEquals(clockOutTime, after.getClockOut(), "clockOut 应为补录时间");
        assertEquals(clockIn, after.getClockIn(), "既有 clockIn 应保留");
        assertEquals(ErpHrConstants.ATTENDANCE_SOURCE_MANUAL, after.getSource(), "source 应改 MANUAL");
        BigDecimal expected = BigDecimal.valueOf(
                        Duration.between(clockIn.toLocalDateTime(), clockOutTime.toLocalDateTime()).toMinutes())
                .divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP);
        assertEquals(expected, after.getWorkHours(), "clockIn/clockOut 均存在时应按镜像公式重算 workHours（9.50）");
    }

    @Test
    public void testMakeUpReasonRequiredBlankRejected() {
        Long empId = ormTemplate.runInSession(session -> seedEmployee("EMP-MK-REASON"));

        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> attendanceBiz.makeUpClockIn(empId,
                        HrFrozenClockExtension.REFERENCE_DATE,
                        Timestamp.valueOf(LocalDateTime.of(2026, 7, 17, 9, 0, 0)), "   ", CTX)));
        assertEquals(ErpHrErrors.ERR_MAKEUP_REASON_REQUIRED.getErrorCode(), ex.getErrorCode(),
                "reason 空白应抛 ERR_MAKEUP_REASON_REQUIRED");
        assertEquals(0, countAttendance(empId), "reason 拒绝不应产生考勤行");
    }

    @Test
    public void testMakeUpRoleGuardDualSide() {
        Long empId = ormTemplate.runInSession(session -> seedEmployee("EMP-MK-ROLE"));
        LocalDate date = HrFrozenClockExtension.REFERENCE_DATE;
        Timestamp clockTime = Timestamp.valueOf(LocalDateTime.of(2026, 7, 17, 10, 0, 0));

        // 无 HR 角色（普通员工角色）→ 拒绝
        IUserContext prev = IUserContext.get();
        try {
            UserContextImpl plain = new UserContextImpl();
            plain.setUserId("plain-staff");
            plain.setUserName("plain-staff");
            plain.setRoles(Set.of("STAFF"));
            IUserContext.set(plain);
            NopException ex = assertThrows(NopException.class,
                    () -> ormTemplate.runInSession(session -> attendanceBiz.makeUpClockIn(empId, date, clockTime, "越权补卡", CTX)));
            assertEquals(ErpHrErrors.ERR_MAKEUP_ROLE_REQUIRED.getErrorCode(), ex.getErrorCode(),
                    "非 HR 角色应被拒 ERR_MAKEUP_ROLE_REQUIRED");
        } finally {
            IUserContext.set(prev);
        }
        assertEquals(0, countAttendance(empId), "角色拒绝不应产生考勤行");

        // 无用户上下文（IUserContext.get()==null，fail-closed）→ 拒绝
        IUserContext prev2 = IUserContext.get();
        try {
            IUserContext.set(null);
            NopException ex = assertThrows(NopException.class,
                    () -> ormTemplate.runInSession(session -> attendanceBiz.makeUpClockIn(empId, date, clockTime, "无上下文补卡", CTX)));
            assertEquals(ErpHrErrors.ERR_MAKEUP_ROLE_REQUIRED.getErrorCode(), ex.getErrorCode(),
                    "无用户上下文（fail-closed）应被拒 ERR_MAKEUP_ROLE_REQUIRED");
        } finally {
            IUserContext.set(prev2);
        }
        assertEquals(0, countAttendance(empId), "无上下文拒绝不应产生考勤行");

        // HR 角色（@BeforeEach 注入）→ 通过
        ErpHrAttendance after = ormTemplate.runInSession(session ->
                attendanceBiz.makeUpClockIn(empId, date, clockTime, "HR 角色补卡", CTX));
        assertNotNull(after.getId(), "HR 角色应可补卡");
        assertEquals(ErpHrConstants.ATTENDANCE_SOURCE_MANUAL, after.getSource());
    }

    @Test
    public void testMakeUpClockInViaGraphQL() {
        Long empId = ormTemplate.runInSession(session -> seedEmployee("EMP-MK-GQL"));
        LocalDate date = HrFrozenClockExtension.REFERENCE_DATE.minusDays(2);

        ApiResponse<?> resp = executeRpc(mutation, "ErpHrAttendance__makeUpClockIn",
                ApiRequest.build(Map.of(
                        "employeeId", String.valueOf(empId),
                        "date", date.toString(),
                        "clockTime", "2026-07-15 09:15:00",
                        "reason", "GraphQL 冒烟补卡")));

        assertEquals(0, resp.getStatus(), "GraphQL 层补卡 mutation 应成功");
        ErpHrAttendance reloaded = findAttendanceByDate(empId, date);
        assertNotNull(reloaded, "GraphQL 调用后行应落库");
        assertEquals(Timestamp.valueOf(LocalDateTime.of(2026, 7, 15, 9, 15, 0)), reloaded.getClockIn(),
                "clockTime 字符串应反序列化为 Timestamp 并落库");
        assertEquals(ErpHrConstants.ATTENDANCE_SOURCE_MANUAL, reloaded.getSource());
        assertEquals("GraphQL 冒烟补卡", reloaded.getRemark());
    }

    // ---------- helpers ----------

    private ApiResponse<?> executeRpc(io.nop.graphql.core.ast.GraphQLOperationType opType,
                                      String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private Long seedEmployee(String code) {
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

    private void seedAttendance(Long employeeId, LocalDate date, LocalDateTime clockIn, LocalDateTime clockOut,
                                String source) {
        IEntityDao<ErpHrAttendance> dao = daoProvider.daoFor(ErpHrAttendance.class);
        ErpHrAttendance row = new ErpHrAttendance();
        row.setBusinessDate(HrFrozenClockExtension.REFERENCE_DATE);
        row.setEmployeeId(employeeId);
        row.setDate(date);
        row.setSource(source);
        row.setIsAbsent(false);
        row.setLateMinutes(0);
        row.setEarlyLeaveMinutes(0);
        row.setClockIn(clockIn != null ? Timestamp.valueOf(clockIn) : null);
        row.setClockOut(clockOut != null ? Timestamp.valueOf(clockOut) : null);
        dao.saveEntity(row);
    }

    private ErpHrAttendance findAttendanceByDate(Long employeeId, LocalDate date) {
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("employeeId", employeeId), eq("date", date)));
        q.setLimit(1);
        List<ErpHrAttendance> list = daoProvider.daoFor(ErpHrAttendance.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private long countAttendance(Long employeeId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("employeeId", employeeId));
        return daoProvider.daoFor(ErpHrAttendance.class).findAllByQuery(q).size();
    }
}
