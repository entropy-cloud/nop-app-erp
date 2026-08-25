package app.erp.hr.service.report;

import app.erp.common.service.MaskAuditRecorder;
import app.erp.common.service.MaskHelper;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrSalarySimulationItemAdjustment;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.audit.AuditRequest;
import io.nop.api.core.audit.IAuditService;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.config.AppConfig;
import io.nop.auth.core.login.UserContextImpl;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 薪酬模拟对比报表读取面脱敏单测（plan 2026-08-25-1956-1 Phase 3 Proof）。
 *
 * <p>范式 = {@code TestErpMfgResponseMasking}（loginAs 三态上下文）+ {@code TestMaskAuditRecorder}
 * （fake {@link IAuditService} 捕获 {@link AuditRequest}）：
 * <ol>
 *   <li>授权（薪酬审批人）→ originalAmount/adjustedAmount/difference/部门小计明文 + E4.2 披露审计写入
 *       （config 显式 ON，审计键 = ErpHrSalarySimulationItemAdjustment × 明细字段名）；</li>
 *   <li>非授权角色 → 三金额列 + 小计 null，无审计；</li>
 *   <li>无用户上下文 → fail-closed null，无审计。</li>
 * </ol>
 *
 * <p>审计实例替换经 public {@code init()}（{@code @PostConstruct}）入口；config 经
 * {@code AppConfig.getConfigProvider()} 直设（不依赖 profile 解析）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpHrPayrollReportReadPathMasking extends JunitAutoTestCase {

    private static final io.nop.core.context.IServiceContext CTX = new io.nop.core.context.ServiceContextImpl();

    static final String DEPT_1 = "4101";
    static final String EMP_SIM_1 = "9101";
    static final String EMP_SIM_2 = "9102";
    static final String SIMULATION_ID = "8101";

    @Inject
    ErpHrReportBizModel reportBiz;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    private CapturingAuditService auditService;
    private MaskAuditRecorder fakeRecorder;
    private MaskAuditRecorder prevRecorder;
    private Object prevConfigState;
    private IUserContext prevCtx;

    @BeforeEach
    void setUpAuditCapture() {
        prevCtx = IUserContext.get();
        auditService = new CapturingAuditService();
        fakeRecorder = new MaskAuditRecorder();
        fakeRecorder.setAuditService(auditService);
        prevRecorder = MaskAuditRecorder.instance();
        fakeRecorder.init();
        prevConfigState = AppConfig.var(MaskAuditRecorder.CONFIG_FIELD_READ_AUDIT_ENABLED, Boolean.FALSE);
        AppConfig.getConfigProvider().assignConfigValue(MaskAuditRecorder.CONFIG_FIELD_READ_AUDIT_ENABLED, true);
    }

    @AfterEach
    void tearDownAuditCapture() {
        AppConfig.getConfigProvider().assignConfigValue(
                MaskAuditRecorder.CONFIG_FIELD_READ_AUDIT_ENABLED, prevConfigState);
        if (prevRecorder != null) {
            prevRecorder.init();
        } else {
            fakeRecorder.destroy();
        }
        IUserContext.set(prevCtx);
    }

    @Test
    public void authorizedSalaryApproverSeesPlaintextAndAuditWritten() {
        seedPayrollSimulationBaseline();
        loginAs(MaskHelper.ROLE_SALARY_APPROVER);

        List<Map<String, Object>> ds = reportBiz.buildPayrollSimulationComparisonDataset(SIMULATION_ID, CTX);
        assertFalseEmpty(ds);

        Map<String, Object> basicRow = findRow(ds, EMP_SIM_1, "basicSalary");
        assertNotNull(basicRow, "数据集含 emp1 basicSalary 行");
        assertEquals(0, new BigDecimal("10000").compareTo((BigDecimal) basicRow.get("originalAmount")), "授权原值=10000");
        assertEquals(0, new BigDecimal("12000").compareTo((BigDecimal) basicRow.get("adjustedAmount")), "授权调整值=12000");
        assertEquals(0, new BigDecimal("2000").compareTo((BigDecimal) basicRow.get("difference")), "授权差异=+2000");

        Map<String, Object> subtotal = findDeptSubtotal(ds, DEPT_1);
        assertNotNull(subtotal, "数据集含部门小计行");
        assertEquals(0, new BigDecimal("1500").compareTo((BigDecimal) subtotal.get("difference")),
                "授权部门小计差异=+1500（已 mask 差异聚合，授权=明文聚合）");

        assertTrue(!auditService.captured.isEmpty(), "授权读取应写 E4.2 披露审计");
        boolean hasOriginal = false;
        boolean hasDifference = false;
        for (AuditRequest req : auditService.captured) {
            assertEquals(MaskAuditRecorder.OPERATION_FIELD_READ_DISCLOSURE, req.getOperation());
            assertTrue(req.getEntityId().startsWith("ErpHrSalarySimulationItemAdjustment"),
                    "审计载体 = 源 ORM 实体 ErpHrSalarySimulationItemAdjustment，实际 " + req.getEntityId());
            if (req.getRequestData().contains("\"field\":\"originalAmount\"")) hasOriginal = true;
            if (req.getRequestData().contains("\"field\":\"difference\"")) hasDifference = true;
        }
        assertTrue(hasOriginal, "审计含 originalAmount 字段披露");
        assertTrue(hasDifference, "审计含 difference 字段披露（派生列按明细字段名记）");
    }

    @Test
    public void unauthorizedRoleSeesNullNoAudit() {
        seedPayrollSimulationBaseline();
        loginAs("STAFF");

        List<Map<String, Object>> ds = reportBiz.buildPayrollSimulationComparisonDataset(SIMULATION_ID, CTX);
        assertFalseEmpty(ds);

        Map<String, Object> basicRow = findRow(ds, EMP_SIM_1, "basicSalary");
        assertNotNull(basicRow, "数据集含 emp1 basicSalary 行");
        assertNull(basicRow.get("originalAmount"), "非授权原值=null");
        assertNull(basicRow.get("adjustedAmount"), "非授权调整值=null");
        assertNull(basicRow.get("difference"), "非授权差异=null");

        Map<String, Object> subtotal = findDeptSubtotal(ds, DEPT_1);
        assertNotNull(subtotal, "部门小计行仍在（行结构保持）");
        assertNull(subtotal.get("difference"), "非授权部门小计=null（已 mask 差异聚合口径）");

        assertTrue(auditService.captured.isEmpty(), "非授权无明文披露 = 无审计");
    }

    @Test
    public void noContextFailClosedNullNoAudit() {
        seedPayrollSimulationBaseline();
        IUserContext.set(null);

        List<Map<String, Object>> ds = reportBiz.buildPayrollSimulationComparisonDataset(SIMULATION_ID, CTX);
        assertFalseEmpty(ds);

        for (Map<String, Object> row : ds) {
            assertNull(row.get("originalAmount"), "无上下文 fail-closed：originalAmount=null");
            assertNull(row.get("adjustedAmount"), "无上下文 fail-closed：adjustedAmount=null");
            assertNull(row.get("difference"), "无上下文 fail-closed：difference=null");
        }
        assertTrue(auditService.captured.isEmpty(), "无上下文无披露 = 无审计");
    }

    // ---------- helpers ----------

    private void loginAs(String... roles) {
        UserContextImpl ctx = new UserContextImpl();
        ctx.setUserId("hr-rpt-mask-test");
        ctx.setUserName("hr-rpt-mask-test");
        ctx.setRoles(Set.of(roles));
        IUserContext.set(ctx);
    }

    private static void assertFalseEmpty(List<Map<String, Object>> ds) {
        assertNotNull(ds, "数据集非 null");
        assertTrue(!ds.isEmpty(), "数据集非空");
    }

    private void seedPayrollSimulationBaseline() {
        seedEmployee(EMP_SIM_1, "SIM-EMP-1", DEPT_1);
        seedEmployee(EMP_SIM_2, "SIM-EMP-2", DEPT_1);
        // emp1 basicSalary: 10000 → 12000，差异 +2000
        seedAdjustment("9201", EMP_SIM_1, "basicSalary", new BigDecimal("10000"), new BigDecimal("12000"));
        // emp2 performanceBonus: 3000 → 2500，差异 -500
        seedAdjustment("9202", EMP_SIM_2, "performanceBonus", new BigDecimal("3000"), new BigDecimal("2500"));
    }

    private void seedEmployee(String id, String fullName, String departmentId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpHrEmployee> dao = daoProvider.daoFor(ErpHrEmployee.class);
            ErpHrEmployee e = dao.newEntity();
            e.orm_propValue(1, id);
            e.setCode("EMP-" + id);
            e.setFirstName(fullName);
            e.setLastName(fullName);
            e.setFullName(fullName);
            e.orm_propValueByName("gender", "MALE");
            e.setHireDate(LocalDate.of(2024, 1, 1));
            e.orm_propValueByName("employmentStatus", "ACTIVE");
            e.orm_propValueByName("employeeType", "REGULAR");
            e.setDepartmentId(departmentId);
            dao.saveEntity(e);
        });
    }

    private void seedAdjustment(String id, String employeeId, String salaryItemCode,
                                BigDecimal original, BigDecimal adjusted) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpHrSalarySimulationItemAdjustment> dao =
                    daoProvider.daoFor(ErpHrSalarySimulationItemAdjustment.class);
            ErpHrSalarySimulationItemAdjustment adj = dao.newEntity();
            adj.orm_propValue(1, id);
            adj.setSimulationId(SIMULATION_ID);
            adj.setEmployeeId(employeeId);
            adj.setSalaryItemCode(salaryItemCode);
            adj.setOriginalAmount(original);
            adj.setAdjustedAmount(adjusted);
            dao.saveEntity(adj);
        });
    }

    private static Map<String, Object> findRow(List<Map<String, Object>> ds, String employeeId, String salaryItemCode) {
        for (Map<String, Object> row : ds) {
            if ("DETAIL".equals(row.get("rowType"))
                    && String.valueOf(employeeId).equals(row.get("employeeId"))
                    && salaryItemCode.equals(row.get("salaryItemCode"))) {
                return row;
            }
        }
        return null;
    }

    private static Map<String, Object> findDeptSubtotal(List<Map<String, Object>> ds, String departmentId) {
        for (Map<String, Object> row : ds) {
            if ("DEPT_SUBTOTAL".equals(row.get("rowType"))
                    && String.valueOf(departmentId).equals(row.get("departmentId"))) {
                return row;
            }
        }
        return null;
    }

    private static class CapturingAuditService implements IAuditService {
        final List<AuditRequest> captured = new ArrayList<>();

        @Override
        public void saveAudit(AuditRequest request) {
            captured.add(request);
        }

        @Override
        public boolean isAllProcessed() {
            return true;
        }
    }
}
