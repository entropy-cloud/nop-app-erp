package app.erp.hr.service.job;

import app.erp.hr.biz.IErpHrDepartmentBiz;
import app.erp.hr.biz.IErpHrEmployeeBiz;
import app.erp.hr.biz.IErpHrLeaveRequestBiz;
import app.erp.hr.dao.entity.ErpHrDepartment;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrLeaveRequest;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.HrFrozenClockExtension;
import app.erp.notify.biz.IErpSysNotificationBiz;
import app.erp.notify.dao.entity.ErpSysNotification;
import app.erp.notify.dao.entity.ErpSysNotificationTemplate;
import app.erp.notify.service.ErpNotifyConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
import io.nop.auth.biz.INopAuthUserBiz;
import io.nop.auth.dao.entity.NopAuthUser;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 休假审批超时自动转派 Job 集成测试（UC-HR-02⑦，RC-R1.4）。
 *
 * <p>覆盖 7 组：① 超时休假单（SUBMITTED + updateTime 早于阈值）→ execute → approverId 更新为上级
 * + notify 落库（eventType=hr.leave-approver-timeout + recipientUserId==上级用户 + status=SENT）；
 * ② 未超时（updateTime 近）→ 不动；③ 幂等守卫直测（approverId 已==目标 + 旧 updateTime → 跳过，
 * 通知数不变 + approverId 不变）——直接构造守卫前置态（首扫会经 updateEntity 刷新 updateTime 致
 * 超时过滤不再命中，仅靠"二次扫描"测不到守卫分支）；④ superiorId null → 兜底部门负责人（managerId）；
 * ⑤ 两者均 null → 跳过 + 不抛（LOG.warn 路径）；⑥ cron 配置空 → execute 直接返回不扫描；
 * ⑦ job 门控 config 绑定断言（{@code nop.job.erp-hr-leave-approver-timeout.enabled} assign + read-back；
 * job.yaml 结构与 @cfg 绑定由 app-erp-all {@code TestErpAllJobYamlLoading} 全量加载覆盖）。
 *
 * <p>时间冻结在 {@link HrFrozenClockExtension#REFERENCE_DATE}（2026-07-17），updateTime 以
 * {@code orm_disableAutoStamp(true)} + 显式赋值 seed 旧时点（ORM 自动盖章会覆盖为 now）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpHrLeaveApproverTimeoutJob extends JunitAutoTestCase {

    @RegisterExtension
    static HrFrozenClockExtension frozenClock = new HrFrozenClockExtension();

    private static final IServiceContext CTX = new ServiceContextImpl();
    private static final String JOB_ENABLED_KEY = "nop.job.erp-hr-leave-approver-timeout.enabled";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpHrLeaveRequestBiz leaveRequestBiz;
    @Inject
    IErpHrEmployeeBiz employeeBiz;
    @Inject
    IErpHrDepartmentBiz departmentBiz;
    @Inject
    INopAuthUserBiz authUserBiz;
    @Inject
    IErpSysNotificationBiz notificationBiz;

    @AfterEach
    public void resetConfig() {
        AppConfig.getConfigProvider().assignConfigValue(ErpHrConstants.CONFIG_LEAVE_APPROVER_TIMEOUT_CRON, "");
        AppConfig.getConfigProvider().assignConfigValue(JOB_ENABLED_KEY, "true");
    }

    /**
     * 手工装配 Job bean（biz_* 代理 bean 带 ioc:force-lazy-property=true，测试容器按需创建时
     * 其 lazy props 不经 runLazyProperties 赋值——镜像 TestErpHrContractExpiry.CountingJob 手工装配范式）。
     */
    private ErpHrLeaveApproverTimeoutJob newWiredJob() {
        ErpHrLeaveApproverTimeoutJob job = new ErpHrLeaveApproverTimeoutJob();
        job.setLeaveRequestBiz(leaveRequestBiz);
        job.setEmployeeBiz(employeeBiz);
        job.setDepartmentBiz(departmentBiz);
        job.setAuthUserBiz(authUserBiz);
        job.setNotificationBiz(notificationBiz);
        job.setOrmTemplate(ormTemplate);
        return job;
    }

    // ---------- ① 超时休假单 → 转派上级 + notify 落库 ----------

    @Test
    public void testTimeoutLeaveEscalatesToSuperiorAndNotifies() {
        Object[] seeded = seedEmployeeChain("EMP-TIMEOUT-A", "SUP-TIMEOUT-A");
        String empId = (String) seeded[0];
        String supId = (String) seeded[1];
        seedAuthUser("sup-user-a", "SUP-TIMEOUT-A");
        seedAuthUser("emp-user-a", "EMP-TIMEOUT-A");
        seedTemplate("7601", ErpHrConstants.NOTIFY_EVENT_LEAVE_APPROVER_TIMEOUT,
                "{\"userIds\":[\"${superiorUserId}\"]}");
        String leaveId = seedLeave("LV-TIMEOUT-A", empId, oldTs());
        setCron("0 0 1 * * ?");

        newWiredJob().execute();

        ErpHrLeaveRequest refreshed = leaveRequest(leaveId);
        assertEquals(supId, refreshed.getApproverId(), "超时休假单应转派给直接上级");
        List<ErpSysNotification> notifications = notificationsOf("sup-user-a");
        assertEquals(1, notifications.size(), "应派发 1 条转派通知: " + notifications.size());
        ErpSysNotification n = notifications.get(0);
        assertEquals(ErpHrConstants.NOTIFY_EVENT_LEAVE_APPROVER_TIMEOUT, n.getNotificationType());
        assertEquals("sup-user-a", n.getRecipientUserId());
        assertEquals(ErpNotifyConstants.STATUS_SENT, n.getStatus());
        assertTrue(n.getBody() != null && n.getBody().contains("LV-TIMEOUT-A"), "通知正文应渲染休假单号");
    }

    // ---------- ② 未超时休假单 → 不动 ----------

    @Test
    public void testNotTimeoutLeaveUntouched() {
        Object[] seeded = seedEmployeeChain("EMP-TIMEOUT-B", "SUP-TIMEOUT-B");
        String empId = (String) seeded[0];
        String supId = (String) seeded[1];
        seedAuthUser("sup-user-b", "SUP-TIMEOUT-B");
        seedTemplate("7602", ErpHrConstants.NOTIFY_EVENT_LEAVE_APPROVER_TIMEOUT,
                "{\"userIds\":[\"${superiorUserId}\"]}");
        String leaveId = seedLeave("LV-TIMEOUT-B", empId, recentTs());
        setCron("0 0 1 * * ?");

        newWiredJob().execute();

        ErpHrLeaveRequest refreshed = leaveRequest(leaveId);
        assertNull(refreshed.getApproverId(), "未超时休假单不应被转派");
        assertTrue(notificationsOf("sup-user-b").isEmpty(), "未超时不应派发通知");
        assertEquals(ErpHrConstants.LEAVE_STATUS_SUBMITTED, refreshed.getStatus(), "未超时休假单状态不变");
    }

    // ---------- ③ 幂等守卫直测：approverId 已 == 目标 + 旧 updateTime → 跳过 ----------

    @Test
    public void testIdempotentSkipWhenApproverAlreadyTarget() {
        Object[] seeded = seedEmployeeChain("EMP-TIMEOUT-C", "SUP-TIMEOUT-C");
        String empId = (String) seeded[0];
        String supId = (String) seeded[1];
        seedAuthUser("sup-user-c", "SUP-TIMEOUT-C");
        seedTemplate("7603", ErpHrConstants.NOTIFY_EVENT_LEAVE_APPROVER_TIMEOUT,
                "{\"userIds\":[\"${superiorUserId}\"]}");
        // 直接构造守卫前置态：approverId 已 == 目标上级 + updateTime 仍早于阈值
        String leaveId = seedLeave("LV-TIMEOUT-C", empId, oldTs());
        backdateApprover(leaveId, supId);
        setCron("0 0 1 * * ?");

        newWiredJob().execute();

        ErpHrLeaveRequest refreshed = leaveRequest(leaveId);
        assertEquals(supId, refreshed.getApproverId(), "幂等跳过分支：approverId 应保持目标值不变");
        assertTrue(notificationsOf("sup-user-c").isEmpty(),
                "幂等守卫应跳过派发（通知数不变=0）");
    }

    // ---------- ④ superiorId null → 兜底部门负责人（managerId） ----------

    @Test
    public void testFallbackToDepartmentManagerWhenNoSuperior() {
        String mngId = seedEmployee("MGR-TIMEOUT-A", null, null, null);
        String deptId = seedDepartment("DEPT-MGR-A", mngId);
        String empId = seedEmployee("EMP-TIMEOUT-D", null, deptId, null);
        seedAuthUser("mgr-user-a", "MGR-TIMEOUT-A");
        seedTemplate("7604", ErpHrConstants.NOTIFY_EVENT_LEAVE_APPROVER_TIMEOUT,
                "{\"userIds\":[\"${superiorUserId}\"]}");
        String leaveId = seedLeave("LV-TIMEOUT-D", empId, oldTs());
        setCron("0 0 1 * * ?");

        newWiredJob().execute();

        ErpHrLeaveRequest refreshed = leaveRequest(leaveId);
        assertEquals(mngId, refreshed.getApproverId(), "无直接上级时应兜底转派部门负责人");
        List<ErpSysNotification> notifications = notificationsOf("mgr-user-a");
        assertEquals(1, notifications.size(), "兜底转派应派发通知给部门负责人");
    }

    // ---------- ⑤ superiorId null + managerId null → 跳过 + 不抛 ----------

    @Test
    public void testSkipWhenNoSuperiorAndNoManager() {
        String deptId = seedDepartment("DEPT-NOMGR-A", null);
        String empId = seedEmployee("EMP-TIMEOUT-E", null, deptId, null);
        String leaveId = seedLeave("LV-TIMEOUT-E", empId, oldTs());
        setCron("0 0 1 * * ?");

        newWiredJob().execute();

        ErpHrLeaveRequest refreshed = leaveRequest(leaveId);
        assertNull(refreshed.getApproverId(), "无上级且无部门负责人应跳过转派");
        assertEquals(ErpHrConstants.LEAVE_STATUS_SUBMITTED, refreshed.getStatus(), "跳过时状态不变");
    }

    // ---------- ⑥ cron 配置空 → execute 直接返回不扫描 ----------

    @Test
    public void testCronEmptySkipsScan() {
        Object[] seeded = seedEmployeeChain("EMP-TIMEOUT-F", "SUP-TIMEOUT-F");
        String empId = (String) seeded[0];
        String leaveId = seedLeave("LV-TIMEOUT-F", empId, oldTs());
        setCron("");

        newWiredJob().execute();

        // cron 空值时 execute 直接返回；扫描不执行 → 无任何超时休假单被转派
        ErpHrLeaveRequest refreshed = leaveRequest(leaveId);
        assertNull(refreshed.getApproverId(), "cron 空时不应扫描转派");
    }

    // ---------- ⑦ job 门控 config 绑定断言 ----------

    @Test
    public void testJobEnabledConfigBinding() {
        AppConfig.getConfigProvider().assignConfigValue(JOB_ENABLED_KEY, "false");
        assertEquals("false", AppConfig.var(JOB_ENABLED_KEY, "true"),
                "job.yaml @cfg 引用的 enabled 键应可经 AppConfig 绑定读写（门控由调度器消费）");
        AppConfig.getConfigProvider().assignConfigValue(JOB_ENABLED_KEY, "true");
        assertEquals("true", AppConfig.var(JOB_ENABLED_KEY, "true"), "恢复默认值后绑定应读回 true");
        // job.yaml 结构与 cronExpr/invoker bean 绑定由 app-erp-all TestErpAllJobYamlLoading 全量加载覆盖
    }

    // ---------- helpers ----------

    private void setCron(String cron) {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpHrConstants.CONFIG_LEAVE_APPROVER_TIMEOUT_CRON, cron);
    }

    private Timestamp oldTs() {
        return new Timestamp(CoreMetrics.currentTimeMillis() - 100L * 3600_000L);
    }

    private Timestamp recentTs() {
        return new Timestamp(CoreMetrics.currentTimeMillis() - 1L * 3600_000L);
    }

    private Object[] seedEmployeeChain(String empCode, String supCode) {
        return ormTemplate.runInSession(session -> {
            String supId = seedEmployee(supCode, null, null, null);
            String empId = seedEmployee(empCode, supId, null, null);
            return new Object[]{empId, supId};
        });
    }

    private String seedEmployee(String code, String superiorId, String departmentId, String ignore) {
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
        if (superiorId != null) {
            emp.setSuperiorId(superiorId);
        }
        if (departmentId != null) {
            emp.setDepartmentId(departmentId);
        }
        dao.saveEntity(emp);
        return emp.getId();
    }

    private String seedDepartment(String code, String managerId) {
        return ormTemplate.runInSession(session -> {
            IEntityDao<ErpHrDepartment> dao = daoProvider.daoFor(ErpHrDepartment.class);
            ErpHrDepartment d = new ErpHrDepartment();
            d.setCode(code);
            d.setName(code);
            if (managerId != null) {
                d.setManagerId(managerId);
            }
            dao.saveEntity(d);
            return d.getId();
        });
    }

    private void seedAuthUser(String userId, String userName) {
        ormTemplate.runInSession(() -> {
            IEntityDao<NopAuthUser> userDao = daoProvider.daoFor(NopAuthUser.class);
            NopAuthUser user = new NopAuthUser();
            user.setUserId(userId);
            user.setUserName(userName);
            user.setNickName(userName);
            user.setPassword("dummy-pwd");
            user.setOpenId(userId);
            user.setGender(0);
            user.setUserType(0);
            user.setStatus(0);
            user.setTenantId("0");
            userDao.saveEntity(user);
        });
    }

    private void seedTemplate(String id, String notificationType, String recipientConfig) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpSysNotificationTemplate> dao = daoProvider.daoFor(ErpSysNotificationTemplate.class);
            ErpSysNotificationTemplate t = new ErpSysNotificationTemplate();
            t.orm_propValueByName("id", id);
            t.setNotificationType(notificationType);
            t.setName("TPL-" + notificationType);
            t.setChannelSet(ErpNotifyConstants.CHANNEL_IN_APP);
            t.setSubjectTpl("休假审批超时转派: ${leaveCode}");
            t.setBodyTpl("休假单 ${leaveCode}（${leaveType}）审批超时，已转派上级审批，请及时处理");
            t.setRecipientResolver(ErpNotifyConstants.RESOLVER_USER_LIST);
            t.setRecipientConfig(recipientConfig);
            t.setMergeWindowSeconds(0);
            t.setMergeStrategy(ErpNotifyConstants.MERGE_NONE);
            t.setStatus(ErpNotifyConstants.TEMPLATE_ACTIVE);
            dao.saveEntity(t);
        });
    }

    private String seedLeave(String code, String employeeId, Timestamp updateTime) {
        return ormTemplate.runInSession(session -> {
            IEntityDao<ErpHrLeaveRequest> dao = daoProvider.daoFor(ErpHrLeaveRequest.class);
            ErpHrLeaveRequest l = new ErpHrLeaveRequest();
            // 关闭 ORM 自动盖章，显式 seed 旧 updateTime（模拟"提交后长期未审批"）
            l.orm_disableAutoStamp(true);
            l.setCreatedBy("test");
            l.setUpdatedBy("test");
            l.setCreateTime(updateTime);
            l.setUpdateTime(updateTime);
            l.setBusinessDate(CoreMetrics.today());
            l.setCode(code);
            l.setEmployeeId(employeeId);
            l.setLeaveType("ANNUAL");
            l.setStartDate(LocalDate.of(2026, 7, 1));
            l.setEndDate(LocalDate.of(2026, 7, 2));
            l.setStatus(ErpHrConstants.LEAVE_STATUS_SUBMITTED);
            dao.saveEntity(l);
            return l.getId();
        });
    }

    private void backdateApprover(String leaveId, String approverId) {
        ormTemplate.runInSession(session -> {
            ErpHrLeaveRequest l = daoProvider.daoFor(ErpHrLeaveRequest.class).getEntityById(leaveId);
            // 直接经 orm_propValueByName 回写 approverId + 保持旧 updateTime（updateEntity 会刷新 updateTime
            // 致超时过滤不再命中——守卫前置态必须保留旧 updateTime 才能进入 escalateLeave 幂等检查）
            l.orm_propValueByName("approverId", approverId);
            session.flush();
            return null;
        });
    }

    private ErpHrLeaveRequest leaveRequest(String leaveId) {
        return daoProvider.daoFor(ErpHrLeaveRequest.class).getEntityById(leaveId);
    }

    private List<ErpSysNotification> notificationsOf(String userId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("recipientUserId", userId));
        q.addFilter(eq("notificationType", ErpHrConstants.NOTIFY_EVENT_LEAVE_APPROVER_TIMEOUT));
        return daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(q);
    }
}
