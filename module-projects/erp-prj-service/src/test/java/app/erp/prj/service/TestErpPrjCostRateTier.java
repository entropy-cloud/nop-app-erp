package app.erp.prj.service;

import app.erp.md.dao.entity.ErpMdEmployee;
import app.erp.md.service.ErpMdConstants;
import app.erp.prj.biz.IErpPrjTimesheetBiz;
import app.erp.prj.dao.entity.ErpPrjActivityType;
import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.dao.entity.ErpPrjProjectType;
import app.erp.prj.dao.entity.ErpPrjProjectUser;
import app.erp.prj.dao.entity.ErpPrjRole;
import app.erp.prj.dao.entity.ErpPrjTask;
import app.erp.prj.dao.entity.ErpPrjTimesheet;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 工时成本率三级降级解析（RC-R1.60 / P1-RC-048，plan 2026-08-16-2043-3）。
 *
 * <p>验证 {@link app.erp.prj.service.cost.CostRateResolver} 五级解析链
 * （单填 &gt; 用户级 &gt; 角色级 &gt; 活动类型 &gt; 全局默认，Phase 1 D2 裁决归位）：
 * <ul>
 *   <li>用户级费率命中（覆盖活动类型）；</li>
 *   <li>角色级费率命中（低于用户级，覆盖活动类型）；</li>
 *   <li>用户级缺失回落角色级；</li>
 *   <li>用户/角色/活动类型三级皆缺回落全局 config；</li>
 *   <li>单填覆盖全链（显式录入优先）；</li>
 *   <li>用户级/角色级费率为 null 跳过对应 tier；</li>
 *   <li>ErpPrjRole 实体 CRUD 冒烟（GraphQL save/find）；</li>
 *   <li>全链皆无抛 {@link ErpPrjErrors#ERR_COST_RATE_NOT_AVAILABLE}。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPrjCostRateTier extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpPrjTimesheetBiz timesheetBiz;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testUserTierOverridesActivityType() {
        // 用户级费率 500 命中，覆盖活动类型 300（tier ② > tier ④）
        Long tsId = ormTemplate.runInSession(session -> {
            clearGlobalRate();
            Long projectId = seedProject("R1-60-001", ErpPrjConstants.PROJECT_STATUS_OPEN);
            Long activityTypeId = seedActivityType("DEV-001", "开发", "300");
            Long taskId = seedTask(projectId, "任务-001", ErpPrjConstants.TASK_STATUS_IN_PROGRESS);
            Long empId = seedEmployee();
            seedProjectMember(projectId, empId, null, "500");
            return seedTimesheet("TS-001", projectId, taskId, activityTypeId, empId, "10", null);
        });

        ErpPrjTimesheet ts = ormTemplate.runInSession(session -> timesheetBiz.submit(tsId, CTX));
        assertEquals(0, ts.getCostRate().compareTo(new BigDecimal("500")),
                "用户级费率应覆盖活动类型费率");
        assertEquals(0, ts.getCostAmount().compareTo(new BigDecimal("5000.0000")),
                "costAmount=10×500");
    }

    @Test
    public void testRoleTierBelowUserTier() {
        // 用户级 500 与角色级 400 并存 → 用户级胜（角色级低于用户级）；角色级高于活动类型 300
        Long tsId = ormTemplate.runInSession(session -> {
            clearGlobalRate();
            Long projectId = seedProject("R1-60-002", ErpPrjConstants.PROJECT_STATUS_OPEN);
            Long activityTypeId = seedActivityType("DEV-002", "开发", "300");
            Long taskId = seedTask(projectId, "任务-002", ErpPrjConstants.TASK_STATUS_IN_PROGRESS);
            Long empId = seedEmployee();
            seedRole("SENIOR", "高级工程师", "400");
            seedProjectMember(projectId, empId, "SENIOR", "500");
            return seedTimesheet("TS-002", projectId, taskId, activityTypeId, empId, "10", null);
        });

        ErpPrjTimesheet ts = ormTemplate.runInSession(session -> timesheetBiz.submit(tsId, CTX));
        assertEquals(0, ts.getCostRate().compareTo(new BigDecimal("500")),
                "用户级费率应优先于角色级费率");
    }

    @Test
    public void testUserMissingFallsBackToRoleTier() {
        // 用户级 costRate 缺失（null）→ 回落角色级 400（覆盖活动类型 300）
        Long tsId = ormTemplate.runInSession(session -> {
            clearGlobalRate();
            Long projectId = seedProject("R1-60-003", ErpPrjConstants.PROJECT_STATUS_OPEN);
            Long activityTypeId = seedActivityType("DEV-003", "开发", "300");
            Long taskId = seedTask(projectId, "任务-003", ErpPrjConstants.TASK_STATUS_IN_PROGRESS);
            Long empId = seedEmployee();
            seedRole("SENIOR", "高级工程师", "400");
            seedProjectMember(projectId, empId, "SENIOR", null);
            return seedTimesheet("TS-003", projectId, taskId, activityTypeId, empId, "10", null);
        });

        ErpPrjTimesheet ts = ormTemplate.runInSession(session -> timesheetBiz.submit(tsId, CTX));
        assertEquals(0, ts.getCostRate().compareTo(new BigDecimal("400")),
                "用户级缺失应回落角色级费率");
        assertEquals(0, ts.getCostAmount().compareTo(new BigDecimal("4000.0000")),
                "costAmount=10×400");
    }

    @Test
    public void testGlobalFallbackWhenAllTiersMissing() {
        // 无成员行 + 活动类型无费率 → 回落全局 config 250
        Long tsId = ormTemplate.runInSession(session -> {
            System.setProperty(ErpPrjConstants.CONFIG_DEFAULT_LABOR_COST_RATE, "250");
            Long projectId = seedProject("R1-60-004", ErpPrjConstants.PROJECT_STATUS_OPEN);
            Long activityTypeId = seedActivityType("DEV-NO-RATE", "开发-无费率", null);
            Long taskId = seedTask(projectId, "任务-004", ErpPrjConstants.TASK_STATUS_IN_PROGRESS);
            Long empId = seedEmployee();
            return seedTimesheet("TS-004", projectId, taskId, activityTypeId, empId, "10", null);
        });

        ErpPrjTimesheet ts = ormTemplate.runInSession(session -> timesheetBiz.submit(tsId, CTX));
        assertEquals(0, ts.getCostRate().compareTo(new BigDecimal("250")),
                "三级皆缺应回落全局 config 费率");
    }

    @Test
    public void testTimesheetRateOverridesAllTiers() {
        // 单填 800 覆盖全链（用户 500 / 角色 400 / 活动类型 300），Phase 1 D2 裁决 A：显式录入优先
        Long tsId = ormTemplate.runInSession(session -> {
            clearGlobalRate();
            Long projectId = seedProject("R1-60-005", ErpPrjConstants.PROJECT_STATUS_OPEN);
            Long activityTypeId = seedActivityType("DEV-005", "开发", "300");
            Long taskId = seedTask(projectId, "任务-005", ErpPrjConstants.TASK_STATUS_IN_PROGRESS);
            Long empId = seedEmployee();
            seedRole("SENIOR", "高级工程师", "400");
            seedProjectMember(projectId, empId, "SENIOR", "500");
            return seedTimesheet("TS-005", projectId, taskId, activityTypeId, empId, "10", "800");
        });

        ErpPrjTimesheet ts = ormTemplate.runInSession(session -> timesheetBiz.submit(tsId, CTX));
        assertEquals(0, ts.getCostRate().compareTo(new BigDecimal("800")),
                "单填费率应覆盖用户/角色/活动类型全链");
    }

    @Test
    public void testNullUserAndRoleRatesSkipped() {
        // 成员行存在但 costRate null + 角色实体 costRate null → 两级跳过，回落活动类型 300
        Long tsId = ormTemplate.runInSession(session -> {
            clearGlobalRate();
            Long projectId = seedProject("R1-60-006", ErpPrjConstants.PROJECT_STATUS_OPEN);
            Long activityTypeId = seedActivityType("DEV-006", "开发", "300");
            Long taskId = seedTask(projectId, "任务-006", ErpPrjConstants.TASK_STATUS_IN_PROGRESS);
            Long empId = seedEmployee();
            seedRole("SENIOR", "高级工程师", null);
            seedProjectMember(projectId, empId, "SENIOR", null);
            return seedTimesheet("TS-006", projectId, taskId, activityTypeId, empId, "10", null);
        });

        ErpPrjTimesheet ts = ormTemplate.runInSession(session -> timesheetBiz.submit(tsId, CTX));
        assertEquals(0, ts.getCostRate().compareTo(new BigDecimal("300")),
                "用户级/角色级费率为 null 应跳过，回落活动类型费率");
    }

    @Test
    public void testErpPrjRoleCrudSmoke() {
        // 新增实体 CRUD 冒烟：GraphQL save + findPage（标准 CRUD 生成链路）
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", "ARCHITECT");
        data.put("name", "架构师");
        data.put("costRate", new BigDecimal("600"));
        ApiResponse<?> saved = executeRpc(GraphQLOperationType.mutation, "ErpPrjRole__save",
                ApiRequest.build(Map.of("data", data)));
        assertEquals(0, saved.getStatus());
        assertNotNull(((Map<?, ?>) saved.getData()).get("id"), "新建应返回非空 ID");

        ApiResponse<?> page = executeRpc(GraphQLOperationType.query, "ErpPrjRole__findPage",
                ApiRequest.build(Map.of("limit", 10)));
        assertEquals(0, page.getStatus());
        assertTrue(((Number) ((Map<?, ?>) page.getData()).get("total")).intValue() >= 1,
                "查询应至少返回 1 条");
    }

    @Test
    public void testCostRateNotAvailableWhenAllTiersMissing() {
        // 单填空 + 成员/角色费率 null + 活动类型无费率 + 全局 config 清空 → 抛 ERR_COST_RATE_NOT_AVAILABLE
        Long tsId = ormTemplate.runInSession(session -> {
            clearGlobalRate();
            Long projectId = seedProject("R1-60-007", ErpPrjConstants.PROJECT_STATUS_OPEN);
            Long activityTypeId = seedActivityType("DEV-NONE", "开发-无费率", null);
            Long taskId = seedTask(projectId, "任务-007", ErpPrjConstants.TASK_STATUS_IN_PROGRESS);
            Long empId = seedEmployee();
            seedRole("SENIOR", "高级工程师", null);
            seedProjectMember(projectId, empId, "SENIOR", null);
            return seedTimesheet("TS-007", projectId, taskId, activityTypeId, empId, "10", null);
        });

        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> timesheetBiz.submit(tsId, CTX)));
        assertEquals(ErpPrjErrors.ERR_COST_RATE_NOT_AVAILABLE.getErrorCode(), ex.getErrorCode());
    }

    // ---------- seed helpers ----------

    private void clearGlobalRate() {
        System.clearProperty(ErpPrjConstants.CONFIG_DEFAULT_LABOR_COST_RATE);
    }

    private Long seedTimesheet(String code, Long projectId, Long taskId, Long activityTypeId,
                               Long userId, String hours, String costRate) {
        IEntityDao<ErpPrjTimesheet> dao = daoProvider.daoFor(ErpPrjTimesheet.class);
        ErpPrjTimesheet ts = new ErpPrjTimesheet();
        ts.setCode(code);
        ts.setOrgId(1L);
        ts.setProjectId(projectId);
        ts.setTaskId(taskId);
        ts.setUserId(userId);
        ts.setActivityTypeId(activityTypeId);
        ts.setWorkDate(LocalDate.of(2026, 7, 15));
        ts.setHours(hours != null ? new BigDecimal(hours) : null);
        ts.setCostRate(costRate != null ? new BigDecimal(costRate) : null);
        ts.setCurrencyId(1L);
        ts.setStatus(ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED);
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

    private Long seedProject(String code, String status) {
        IEntityDao<ErpPrjProject> dao = daoProvider.daoFor(ErpPrjProject.class);
        ErpPrjProject p = new ErpPrjProject();
        p.setCode(code);
        p.setName("成本率测试-" + code);
        p.setOrgId(1L);
        p.setProjectTypeId(seedProjectType());
        p.setCurrencyId(1L);
        p.setStatus(status);
        p.setBudget(new BigDecimal("100000"));
        p.setActualCost(BigDecimal.ZERO);
        dao.saveEntity(p);
        return p.getId();
    }

    private Long seedProjectType() {
        IEntityDao<ErpPrjProjectType> dao = daoProvider.daoFor(ErpPrjProjectType.class);
        ErpPrjProjectType t = new ErpPrjProjectType();
        t.setCode("PT-R1-60-" + System.nanoTime());
        t.setName("研发项目");
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

    private Long seedActivityType(String code, String name, String costRate) {
        IEntityDao<ErpPrjActivityType> dao = daoProvider.daoFor(ErpPrjActivityType.class);
        ErpPrjActivityType a = new ErpPrjActivityType();
        a.setCode(code);
        a.setName(name);
        a.setCostRate(costRate != null ? new BigDecimal(costRate) : null);
        dao.saveEntity(a);
        return a.getId();
    }

    private void seedRole(String code, String name, String costRate) {
        IEntityDao<ErpPrjRole> dao = daoProvider.daoFor(ErpPrjRole.class);
        ErpPrjRole role = new ErpPrjRole();
        role.setCode(code);
        role.setName(name);
        role.setCostRate(costRate != null ? new BigDecimal(costRate) : null);
        dao.saveEntity(role);
    }

    private void seedProjectMember(Long projectId, Long userId, String role, String costRate) {
        IEntityDao<ErpPrjProjectUser> dao = daoProvider.daoFor(ErpPrjProjectUser.class);
        ErpPrjProjectUser member = new ErpPrjProjectUser();
        member.setProjectId(projectId);
        member.setUserId(userId);
        member.setRole(role);
        member.setCostRate(costRate != null ? new BigDecimal(costRate) : null);
        dao.saveEntity(member);
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
