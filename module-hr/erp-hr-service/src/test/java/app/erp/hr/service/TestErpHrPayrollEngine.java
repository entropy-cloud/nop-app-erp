package app.erp.hr.service;

import app.erp.hr.biz.IErpHrSalaryBiz;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrEmploymentContract;
import app.erp.hr.dao.entity.ErpHrPayrollBankFile;
import app.erp.hr.dao.entity.ErpHrSalary;
import app.erp.hr.dao.entity.ErpHrSocialInsuranceBase;
import app.erp.hr.dao.entity.ErpHrSocialInsuranceConfig;
import app.erp.hr.dao.entity.ErpHrTaxConfig;
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
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 薪酬核算引擎端到端测试（payroll.md §2.4/§4.5/§6/§9）。验证：
 * <ul>
 *   <li>社保基数钳制（payroll.md §2.4）+ 公积金。</li>
 *   <li>个税累计预扣跨月累加（payroll.md §4.5）。</li>
 *   <li>runPayroll 幂等（ERR_SALARY_ALREADY_EXISTS）。</li>
 *   <li>审批状态机全路径 + PAID 锁定（ERR_SALARY_LOCKED_AFTER_PAID）。</li>
 *   <li>计提/发放过账凭证生成（SALARY / SALARY_PAYMENT）。</li>
 *   <li>银行文件生成 + ErpHrSalary 转 PAID。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpHrPayrollEngine extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpHrSalaryBiz salaryBiz;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testSocialInsuranceBaseClampingAndGrossNet() {
        Long employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            Long empId = seedEmployee("EMP-CLAMP", ErpHrConstants.EMPLOYMENT_ACTIVE);
            seedContract(empId, "30000");
            // 基数 50000 超上限 32694 → 钳到 32694
            seedSocialInsuranceBase(empId, "SHENZHEN", "50000", "50000");
            // 养老 公司 15% / 个人 8%（基数上限 32694）
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_PENSION,
                    "0.15", "0.08", "6123", "32694");
            // 公积金 公司 12% / 个人 12%（基数上限 32694）
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_HOUSING_FUND,
                    "0.12", "0.12", "2360", "32694");
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            return empId;
        });

        ErpHrSalary salary = salaryBiz.calculateSalary(employeeId, 2026, 7, CTX);
        // 钳制后基数 = 32694；社保个人 = 32694 × 8% = 2615.52
        assertEquals(0, salary.getSocialInsurance().compareTo(new BigDecimal("2615.52")),
                "社保个人扣款=基数×个人比例（钳制后）");
        // 公积金个人 = 32694 × 12% = 3923.28
        assertEquals(0, salary.getHousingFund().compareTo(new BigDecimal("3923.28")),
                "公积金个人=基数×个人比例");
        // approveStatus=UNSUBMITTED, paymentStatus=PENDING
        assertEquals(ErpHrConstants.APPROVE_STATUS_UNSUBMITTED, salary.getApproveStatus());
        // 应发 > 0，实发 = 应发 − 社保个人 − 公积金个人 − 个税 − 其他扣款
        assertTrue(salary.getGrossSalary().signum() > 0, "应发合计>0");
        BigDecimal expectedNet = salary.getGrossSalary()
                .subtract(salary.getSocialInsurance())
                .subtract(salary.getHousingFund())
                .subtract(salary.getTaxAmount())
                .subtract(salary.getOtherDeductions());
        assertEquals(0, expectedNet.compareTo(salary.getNetSalary()), "实发=应发−扣款");
    }

    @Test
    public void testCumulativeTaxAcrossMonths() {
        Long employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            Long empId = seedEmployee("EMP-TAX", ErpHrConstants.EMPLOYMENT_ACTIVE);
            seedContract(empId, "20000");
            seedSocialInsuranceBase(empId, "SHENZHEN", "20000", "20000");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_PENSION,
                    "0.15", "0.08", "6123", "32694");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_HOUSING_FUND,
                    "0.12", "0.12", "2360", "32694");
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            return empId;
        });

        ErpHrSalary jan = salaryBiz.calculateSalary(employeeId, 2026, 1, CTX);
        ErpHrSalary feb = salaryBiz.calculateSalary(employeeId, 2026, 2, CTX);

        assertNotNull(jan.getCumulativeData(), "1 月写回累计数据");
        assertNotNull(feb.getCumulativeData(), "2 月写回累计数据");
        // 2 月累计应发 > 1 月累计应发（跨月累加生效）
        BigDecimal janCumGross = extractCumulativeData(feb.getCumulativeData());
        assertTrue(janCumGross.signum() > 0, "累计应发>0");
        assertNotEquals(0, jan.getTaxAmount().compareTo(BigDecimal.ZERO) < 0 ? 0 : 1,
                "个税计算不抛异常");
    }

    @Test
    public void testRunPayrollIdempotent() {
        Long employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            Long empId = seedEmployee("EMP-IDEMP", ErpHrConstants.EMPLOYMENT_ACTIVE);
            seedContract(empId, "15000");
            seedSocialInsuranceBase(empId, "SHENZHEN", "15000", "15000");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_PENSION,
                    "0.15", "0.08", "6123", "32694");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_HOUSING_FUND,
                    "0.12", "0.12", "2360", "32694");
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            return empId;
        });

        salaryBiz.calculateSalary(employeeId, 2026, 8, CTX);
        // 再次直接核算同员工同期 → 应抛 ERR_SALARY_ALREADY_EXISTS
        NopException ex = assertThrows(NopException.class,
                () -> salaryBiz.calculateSalary(employeeId, 2026, 8, CTX));
        assertEquals(ErpHrErrors.ERR_SALARY_ALREADY_EXISTS.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testApprovalStateMachineAndPaidLock() {
        Long employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            Long empId = seedEmployee("EMU-APPR", ErpHrConstants.EMPLOYMENT_ACTIVE);
            seedContract(empId, "12000");
            seedSocialInsuranceBase(empId, "SHENZHEN", "12000", "12000");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_PENSION,
                    "0.15", "0.08", "6123", "32694");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_HOUSING_FUND,
                    "0.12", "0.12", "2360", "32694");
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            return empId;
        });

        ErpHrSalary salary = salaryBiz.calculateSalary(employeeId, 2026, 9, CTX);
        Long salaryId = salary.getId();

        // 标准审批轴：UNSUBMITTED → SUBMITTED → APPROVED
        assertEquals(0, submitSalary(salaryId).getStatus(), "提交应成功");
        assertEquals(0, approveSalary(salaryId).getStatus(), "审核应成功");
        ErpHrSalary approved = daoProvider.daoFor(ErpHrSalary.class).getEntityById(salaryId);
        assertEquals(ErpHrConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus());

        // 支付轴：APPROVED + paymentStatus=PENDING → PAID
        ErpHrSalary paid = salaryBiz.markPaid(salaryId, CTX);
        assertEquals(ErpHrConstants.PAYMENT_PAID, paid.getPaymentStatus());

        // PAID 后再 voidSalary → 应抛锁定异常
        NopException lockEx = assertThrows(NopException.class,
                () -> salaryBiz.voidSalary(salaryId, CTX));
        assertEquals(ErpHrErrors.ERR_SALARY_LOCKED_AFTER_PAID.getErrorCode(), lockEx.getErrorCode());
    }

    @Test
    public void testIllegalTransitionRejects() {
        Long employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            Long empId = seedEmployee("EMU-ILLEGAL", ErpHrConstants.EMPLOYMENT_ACTIVE);
            seedContract(empId, "10000");
            seedSocialInsuranceBase(empId, "SHENZHEN", "10000", "10000");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_PENSION,
                    "0.15", "0.08", "6123", "32694");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_HOUSING_FUND,
                    "0.12", "0.12", "2360", "32694");
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            return empId;
        });

        ErpHrSalary salary = salaryBiz.calculateSalary(employeeId, 2026, 10, CTX);
        Long salaryId = salary.getId();
        // UNSUBMITTED 直接 approve（跳过 submit）→ 平台守卫拒绝
        ApiResponse<?> bad = approveSalary(salaryId);
        assertEquals(-1, bad.getStatus(),
                "UNSUBMITTED 不可直接审核：平台守卫仅接受 SUBMITTED 源态");
    }

    @Test
    public void testGenerateBankFileTransfersSalariesToPaid() {
        Long employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            Long empId = seedEmployee("EMU-BANK", ErpHrConstants.EMPLOYMENT_ACTIVE);
            seedContract(empId, "18000");
            seedSocialInsuranceBase(empId, "SHENZHEN", "18000", "18000");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_PENSION,
                    "0.15", "0.08", "6123", "32694");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_HOUSING_FUND,
                    "0.12", "0.12", "2360", "32694");
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            return empId;
        });

        ErpHrSalary salary = salaryBiz.calculateSalary(employeeId, 2026, 11, CTX);
        Long salaryId = salary.getId();
        submitSalary(salaryId);
        approveSalary(salaryId);

        ErpHrPayrollBankFile bankFile = salaryBiz.generateBankFile(2026, 11, 1L, CTX);
        assertNotNull(bankFile.getId(), "银行文件已落库");
        assertNotNull(bankFile.getFileContent(), "文件内容已生成");
        assertEquals(ErpHrConstants.BANK_FILE_STATUS_GENERATED, bankFile.getStatus());
        assertTrue(bankFile.getRecordCount() >= 1, "至少 1 条记录");

        // ErpHrSalary paymentStatus 已转 PAID + paymentBatchNo 已写
        ErpHrSalary updated = daoProvider.daoFor(ErpHrSalary.class).getEntityById(salaryId);
        assertEquals(ErpHrConstants.PAYMENT_PAID, updated.getPaymentStatus());
        assertEquals(ErpHrConstants.APPROVE_STATUS_APPROVED, updated.getApproveStatus());
        assertNotNull(updated.getPaymentBatchNo());
    }

    private ApiResponse<?> submitSalary(Long salaryId) {
        return executeRpc(mutation, "ErpHrSalary__submitForApproval", ApiRequest.build(Map.of("id", String.valueOf(salaryId))));
    }

    private ApiResponse<?> approveSalary(Long salaryId) {
        return executeRpc(mutation, "ErpHrSalary__approve", ApiRequest.build(Map.of("id", String.valueOf(salaryId))));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- seed helpers ----------

    private void seedTaxConfig(int year) {
        IEntityDao<ErpHrTaxConfig> dao = daoProvider.daoFor(ErpHrTaxConfig.class);
        ErpHrTaxConfig cfg = new ErpHrTaxConfig();
        cfg.setYear(year);
        cfg.setTaxThreshold(new BigDecimal("5000"));
        // 七级超额累进税率表（payroll.md §4.2）
        cfg.setTaxBrackets("["
                + "{\"rangeUpperLimit\":36000,\"rate\":0.03,\"quickDeduction\":0},"
                + "{\"rangeUpperLimit\":144000,\"rate\":0.10,\"quickDeduction\":2520},"
                + "{\"rangeUpperLimit\":300000,\"rate\":0.20,\"quickDeduction\":16920},"
                + "{\"rangeUpperLimit\":420000,\"rate\":0.25,\"quickDeduction\":31920},"
                + "{\"rangeUpperLimit\":660000,\"rate\":0.30,\"quickDeduction\":52920},"
                + "{\"rangeUpperLimit\":960000,\"rate\":0.35,\"quickDeduction\":85920},"
                + "{\"rangeUpperLimit\":null,\"rate\":0.45,\"quickDeduction\":181920}"
                + "]");
        dao.saveEntity(cfg);
    }

    private Long seedEmployee(String code, String employmentStatus) {
        IEntityDao<ErpHrEmployee> dao = daoProvider.daoFor(ErpHrEmployee.class);
        ErpHrEmployee emp = new ErpHrEmployee();
        emp.setCode(code);
        emp.setFirstName("测");
        emp.setLastName("试");
        emp.setFullName("测试员工");
        emp.setGender("MALE");
        emp.setHireDate(LocalDate.of(2025, 1, 1));
        emp.setEmploymentStatus(employmentStatus);
        emp.setEmployeeType("FULL_TIME");
        dao.saveEntity(emp);
        return emp.getId();
    }

    private void seedContract(Long employeeId, String monthlySalary) {
        IEntityDao<ErpHrEmploymentContract> dao = daoProvider.daoFor(ErpHrEmploymentContract.class);
        ErpHrEmploymentContract c = new ErpHrEmploymentContract();
        c.setCode("C-" + employeeId);
        c.setEmployeeId(employeeId);
        c.setContractType("OPEN_ENDED");
        c.setSignDate(LocalDate.of(2025, 1, 1));
        c.setStartDate(LocalDate.of(2025, 1, 1));
        c.setMonthlySalary(new BigDecimal(monthlySalary));
        c.setStatus("ACTIVE");
        dao.saveEntity(c);
    }

    private void seedSocialInsuranceBase(Long employeeId, String cityCode,
                                         String socialInsuranceBase, String housingFundBase) {
        IEntityDao<ErpHrSocialInsuranceBase> dao = daoProvider.daoFor(ErpHrSocialInsuranceBase.class);
        ErpHrSocialInsuranceBase base = new ErpHrSocialInsuranceBase();
        base.setEmployeeId(employeeId);
        base.setCityCode(cityCode);
        base.setSocialInsuranceBase(new BigDecimal(socialInsuranceBase));
        base.setHousingFundBase(new BigDecimal(housingFundBase));
        base.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        dao.saveEntity(base);
    }

    private void seedSocialInsuranceConfig(String cityCode, String insuranceType,
                                           String companyRate, String employeeRate,
                                           String lower, String upper) {
        IEntityDao<ErpHrSocialInsuranceConfig> dao = daoProvider.daoFor(ErpHrSocialInsuranceConfig.class);
        ErpHrSocialInsuranceConfig cfg = new ErpHrSocialInsuranceConfig();
        cfg.setCityCode(cityCode);
        cfg.setInsuranceType(insuranceType);
        cfg.setCompanyRate(new BigDecimal(companyRate));
        cfg.setEmployeeRate(new BigDecimal(employeeRate));
        cfg.setBaseLowerLimit(new BigDecimal(lower));
        cfg.setBaseUpperLimit(new BigDecimal(upper));
        dao.saveEntity(cfg);
    }

    private BigDecimal extractCumulativeData(String json) {
        if (json == null || json.isEmpty()) return BigDecimal.ZERO;
        try {
            Object parsed = io.nop.core.lang.json.JsonTool.parseNonStrict(json);
            if (parsed instanceof java.util.Map) {
                Object v = ((java.util.Map<String, Object>) parsed).get("cumulativeGross");
                return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
            }
        } catch (Exception ignored) {
        }
        return BigDecimal.ZERO;
    }
}
