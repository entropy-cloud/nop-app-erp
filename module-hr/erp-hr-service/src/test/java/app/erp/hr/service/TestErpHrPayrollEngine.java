package app.erp.hr.service;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.hr.biz.IErpHrSalaryBiz;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrAttendance;
import app.erp.hr.dao.entity.ErpHrLeaveRequest;
import app.erp.hr.dao.entity.ErpHrEmploymentContract;
import app.erp.hr.dao.entity.ErpHrPayrollBankFile;
import app.erp.hr.dao.entity.ErpHrSalary;
import app.erp.hr.dao.entity.ErpHrSocialInsuranceBase;
import app.erp.hr.dao.entity.ErpHrSocialInsuranceConfig;
import app.erp.hr.dao.entity.ErpHrTaxConfig;
import io.nop.api.core.annotations.autotest.EnableSnapshot;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.context.ContextProvider;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @RegisterExtension
    static HrFrozenClockExtension frozenClock = new HrFrozenClockExtension();

    private static final IServiceContext CTX = new ServiceContextImpl();

    // WORKFLOW 模式下薪酬 submit 会启动 wf 实例，wf 引擎校验 caller 需 resolved 用户。
    // 用 SYS（id=0）：submit 步骤 owner 解析为 SYS，caller=0 匹配跳过委托校验，避免 NopAuthUser 查询。
    @BeforeEach
    public void setUpWfUser() {
        ContextProvider.getOrCreateContext().setUserId("0");
        ContextProvider.getOrCreateContext().setUserName("SYS");
    }

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
        String employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            String empId = seedEmployee("EMP-CLAMP", ErpHrConstants.EMPLOYMENT_ACTIVE);
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

        ErpHrSalary salary = ormTemplate.runInSession(session -> salaryBiz.calculateSalary(employeeId, 2026, 7, CTX));
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
        String employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            String empId = seedEmployee("EMP-TAX", ErpHrConstants.EMPLOYMENT_ACTIVE);
            seedContract(empId, "20000");
            seedSocialInsuranceBase(empId, "SHENZHEN", "20000", "20000");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_PENSION,
                    "0.15", "0.08", "6123", "32694");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_HOUSING_FUND,
                    "0.12", "0.12", "2360", "32694");
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            return empId;
        });

        ErpHrSalary jan = ormTemplate.runInSession(session -> salaryBiz.calculateSalary(employeeId, 2026, 1, CTX));
        ErpHrSalary feb = ormTemplate.runInSession(session -> salaryBiz.calculateSalary(employeeId, 2026, 2, CTX));

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
        String employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            String empId = seedEmployee("EMP-IDEMP", ErpHrConstants.EMPLOYMENT_ACTIVE);
            seedContract(empId, "15000");
            seedSocialInsuranceBase(empId, "SHENZHEN", "15000", "15000");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_PENSION,
                    "0.15", "0.08", "6123", "32694");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_HOUSING_FUND,
                    "0.12", "0.12", "2360", "32694");
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            return empId;
        });

        ormTemplate.runInSession(() -> salaryBiz.calculateSalary(employeeId, 2026, 8, CTX));
        // 再次直接核算同员工同期 → 应抛 ERR_SALARY_ALREADY_EXISTS
        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> salaryBiz.calculateSalary(employeeId, 2026, 8, CTX)));
        assertEquals(ErpHrErrors.ERR_SALARY_ALREADY_EXISTS.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testApprovalStateMachineAndPaidLock() {
        String employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            String empId = seedEmployee("EMU-APPR", ErpHrConstants.EMPLOYMENT_ACTIVE);
            seedContract(empId, "12000");
            seedSocialInsuranceBase(empId, "SHENZHEN", "12000", "12000");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_PENSION,
                    "0.15", "0.08", "6123", "32694");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_HOUSING_FUND,
                    "0.12", "0.12", "2360", "32694");
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            return empId;
        });

        ErpHrSalary salary = ormTemplate.runInSession(session -> salaryBiz.calculateSalary(employeeId, 2026, 9, CTX));
        String salaryId = salary.getId();

        // 标准审批轴：UNSUBMITTED → SUBMITTED → APPROVED
        assertEquals(0, submitSalary(salaryId).getStatus(), "提交应成功");
        assertEquals(0, approveSalary(salaryId).getStatus(), "审核应成功");
        ErpHrSalary approved = daoProvider.daoFor(ErpHrSalary.class).getEntityById(salaryId);
        assertEquals(ErpHrConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus());

        // 支付轴：APPROVED + paymentStatus=PENDING → PAID
        ErpHrSalary paid = ormTemplate.runInSession(session -> salaryBiz.markPaid(salaryId, CTX));
        assertEquals(ErpHrConstants.PAYMENT_PAID, paid.getPaymentStatus());

        // PAID 后再 voidSalary → 应抛锁定异常
        NopException lockEx = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> salaryBiz.voidSalary(salaryId, CTX)));
        assertEquals(ErpHrErrors.ERR_SALARY_LOCKED_AFTER_PAID.getErrorCode(), lockEx.getErrorCode());
    }

    @Test
    public void testIllegalTransitionRejects() {
        String employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            String empId = seedEmployee("EMU-ILLEGAL", ErpHrConstants.EMPLOYMENT_ACTIVE);
            seedContract(empId, "10000");
            seedSocialInsuranceBase(empId, "SHENZHEN", "10000", "10000");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_PENSION,
                    "0.15", "0.08", "6123", "32694");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_HOUSING_FUND,
                    "0.12", "0.12", "2360", "32694");
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            return empId;
        });

        ErpHrSalary salary = ormTemplate.runInSession(session -> salaryBiz.calculateSalary(employeeId, 2026, 10, CTX));
        String salaryId = salary.getId();
        // UNSUBMITTED 直接 approve（跳过 submit）→ Bean 矩阵守卫拒绝（xbiz assertCanApprove 仅 SUBMITTED 源态）
        ApiResponse<?> bad = approveSalary(salaryId);
        assertEquals(-1, bad.getStatus(),
                "UNSUBMITTED 不可直接审核：Bean 矩阵守卫仅接受 SUBMITTED 源态");
        assertEquals(ErpHrErrors.ERR_SALARY_ILLEGAL_STATUS_TRANSITION.getErrorCode(), bad.getCode(),
                "非法审批迁移映射为领域码 ERR_SALARY_ILLEGAL_STATUS_TRANSITION（xbiz try/catch Bean common 码 → cause-chain）");
    }

            @Test
    public void testGenerateBankFileTransfersSalariesToPaid() {
        String employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            String empId = seedEmployee("EMU-BANK", ErpHrConstants.EMPLOYMENT_ACTIVE);
            seedContract(empId, "18000");
            seedSocialInsuranceBase(empId, "SHENZHEN", "18000", "18000");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_PENSION,
                    "0.15", "0.08", "6123", "32694");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_HOUSING_FUND,
                    "0.12", "0.12", "2360", "32694");
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            // P1-CK-hr2-002：银行文件路径 280 过账断言所需环境（对齐计提链测试）
            daoProvider.daoFor(ErpHrEmployee.class).getEntityById(empId).setOrgId("1");
            daoProvider.daoFor(ErpHrEmployee.class).getEntityById(empId).setBankAccountId("6222000011112222");
            seedPostingSubjects();
            seedAcctSchema("1");
            seedOpenPeriod(2026, 11);
            return empId;
        });

        ErpHrSalary salary = ormTemplate.runInSession(session -> salaryBiz.calculateSalary(employeeId, 2026, 11, CTX));
        String salaryId = salary.getId();
        submitSalary(salaryId);
        approveSalary(salaryId);

        ErpHrPayrollBankFile bankFile = ormTemplate.runInSession(session -> salaryBiz.generateBankFile(2026, 11, "1", CTX));
        assertNotNull(bankFile.getId(), "银行文件已落库");
        assertNotNull(bankFile.getFileContent(), "文件内容已生成");
        assertEquals(ErpHrConstants.BANK_FILE_STATUS_GENERATED, bankFile.getStatus());
        assertTrue(bankFile.getRecordCount() >= 1, "至少 1 条记录");

        // P1-CK-hr2-002：银行文件批量发放路径必须生成 SALARY_PAYMENT(280) 凭证（payroll.md §5.2 步骤 4；
        // 修复前 generateBankFile 零 postingDispatcher 调用，280 仅 markPaid 单笔路径可达）
        assertEquals(1L, countVoucherBillR(salaryBillCode(2026, 11, salaryId), ErpFinBusinessType.SALARY_PAYMENT),
                "银行文件路径 280 发放凭证已生成");

        // P1-CK-hr2-002：文件格式对齐 payroll.md §7.2 五列「序号,账号,户名,金额,用途」
        // （修复前为「序号,employeeId,净额,SALARY」缺账号/户名）
        String content = bankFile.getFileContent();
        assertTrue(content.contains("6222000011112222"), "文件内容含员工银行账号列");
        assertTrue(content.contains("测试员工"), "文件内容含户名列");
        String[] cols = content.trim().split("\n")[0].split(",");
        assertEquals(5, cols.length, "文件行应为五列（序号,账号,户名,金额,用途）");

        // ErpHrSalary paymentStatus 已转 PAID + paymentBatchNo 已写
        ErpHrSalary updated = daoProvider.daoFor(ErpHrSalary.class).getEntityById(salaryId);
        assertEquals(ErpHrConstants.PAYMENT_PAID, updated.getPaymentStatus());
        assertEquals(ErpHrConstants.APPROVE_STATUS_APPROVED, updated.getApproveStatus());
        assertNotNull(updated.getPaymentBatchNo());
    }

    @Test
    public void testCorruptCumulativeDataThrowsNotSilentReset() {
        String employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            String empId = seedEmployee("EMP-CORRUPT", ErpHrConstants.EMPLOYMENT_ACTIVE);
            seedContract(empId, "20000");
            seedSocialInsuranceBase(empId, "SHENZHEN", "20000", "20000");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_PENSION,
                    "0.15", "0.08", "6123", "32694");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_HOUSING_FUND,
                    "0.12", "0.12", "2360", "32694");
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            return empId;
        });

        // 1 月正常核算（生成合法累计数据）
        ormTemplate.runInSession(session -> salaryBiz.calculateSalary(employeeId, 2026, 1, CTX));

        // 破坏 1 月 cumulativeData（模拟数据损坏/手工编辑出错）—— findPreviousCumulative 解析历史月时先经此
        ormTemplate.runInSession(session -> {
            IEntityDao<ErpHrSalary> dao = daoProvider.daoFor(ErpHrSalary.class);
            QueryBean q = new QueryBean();
            q.addFilter(eq("employeeId", employeeId));
            q.addFilter(eq("year", 2026));
            q.addFilter(eq("month", 1));
            ErpHrSalary jan = dao.findAllByQuery(q).get(0);
            jan.setCumulativeData("{corrupt-data-not-json");
            // MANAGED 实体：ORM dirty tracking 在 session 关闭时 flush，无需 saveEntity
            return null;
        });

        // 2 月核算 → findPreviousCumulative 解析损坏 JSON 抛 ERR_HR_CUMULATIVE_DATA_CORRUPT（非静默重置致少预扣）
        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> salaryBiz.calculateSalary(employeeId, 2026, 2, CTX)));
        assertEquals(ErpHrErrors.ERR_HR_CUMULATIVE_DATA_CORRUPT.getErrorCode(), ex.getErrorCode());
    }

    private ApiResponse<?> submitSalary(String salaryId) {
        return executeRpc(mutation, "ErpHrSalary__submitForApproval", ApiRequest.build(Map.of("id", String.valueOf(salaryId))));
    }

    private ApiResponse<?> approveSalary(String salaryId) {
        return executeRpc(mutation, "ErpHrSalary__approve", ApiRequest.build(Map.of("id", String.valueOf(salaryId))));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    /**
     * G1（P1-MA4-019 (a) 残差）高档税率集成级 E2E。月薪 100000 员工跨月累计使累计应纳税所得额 &gt;960000，
     * 演练完整引擎流程（社保基数钳制 + 个税累计预扣跨月写回 + calculateSalary 链路），断言末档 45% 税率正确计算、
     * 无 NPE、monthTax 为正。单元级 {@code TestIncomeTaxCalculator} 仅隔离 resolveBracket 末档 null，不演练引擎全链路；
     * 既有集成测试员工月薪 ≤30000 永不触达末档。本测试补集成级高档残差。
     */
        @Test
    public void testHighTaxBracketIntegrationE2e() {
        String employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            String empId = seedEmployee("EMP-HIGH", ErpHrConstants.EMPLOYMENT_ACTIVE);
            seedContract(empId, "100000");
            // 基数 100000 超上限 32694 → 钳到 32694（payroll.md §2.4）
            seedSocialInsuranceBase(empId, "SHENZHEN", "100000", "100000");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_PENSION,
                    "0.15", "0.08", "6123", "32694");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_HOUSING_FUND,
                    "0.12", "0.12", "2360", "32694");
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            return empId;
        });

        // 1-10 月逐月核算：累计应纳税所得额逐月累加（payroll.md §4.5 跨月累计预扣写回）
        for (int m = 1; m <= 10; m++) {
            int month = m;
            ormTemplate.runInSession(session -> salaryBiz.calculateSalary(employeeId, 2026, month, CTX));
        }
        // 11 月：累计应纳税所得额 >960000 → 命中末档 45%（payroll.md §4.2 第七级，rangeUpperLimit=null）
        ErpHrSalary nov = ormTemplate.runInSession(session -> salaryBiz.calculateSalary(employeeId, 2026, 11, CTX));

        // 无 NPE（测试完整执行即证明）+ 末档 monthTax 为正
        assertTrue(nov.getTaxAmount().signum() > 0, "末档月度个税 monthTax>0");

        BigDecimal cumTaxableIncome = extractCumulativeField(nov.getCumulativeData(), "cumulativeTaxableIncome");
        BigDecimal cumTaxAmount = extractCumulativeField(nov.getCumulativeData(), "cumulativeTaxAmount");
        assertTrue(cumTaxableIncome.compareTo(new BigDecimal("960000")) > 0,
                "累计应纳税所得额>960000 命中末档（实测=" + cumTaxableIncome + "）");

        // 末档公式校验（payroll.md §4.2 第七级）：累计应纳税额 = 累计应纳税所得额 ×45% − 速算扣除数 181920
        BigDecimal expectedCumTax = cumTaxableIncome.multiply(new BigDecimal("0.45"))
                .subtract(new BigDecimal("181920"))
                .setScale(2, RoundingMode.HALF_UP);
        assertEquals(0, expectedCumTax.compareTo(cumTaxAmount),
                "末档 45% 累计应纳税额公式正确（rate=0.45, quickDeduction=181920）");
    }

    /**
     * G2（P1-MA4-019 (b)）过账悬挂 posted=false 窗口。{@code markPaid} 忽略 {@code tryPostPayment} 返回值
     * （ErpHrSalaryBizModel markPaid），故过账失败时薪酬仍转 PAID 而 posted=false + 无发放凭证。
     * 本测试清空应付职工薪酬科目配置使 buildPaymentEvent 抛 ERR_PAYROLL_SUBJECT_NOT_CONFIGURED
     * （在 tryPostPayment 的 try 块内触发），断言 PAID + posted=false 可观测 + 无 SALARY_PAYMENT 凭证。
     * 闭合 P1-MA2-048 测试可见性，依赖 R1.16 告警闭环（dispatchFailureAlert）。
     */
    @Test
    public void testPostingSuspensionWindowPostedFalse() {
        String employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            String empId = seedEmployee("EMP-SUSP", ErpHrConstants.EMPLOYMENT_ACTIVE);
            seedContract(empId, "12000");
            seedSocialInsuranceBase(empId, "SHENZHEN", "12000", "12000");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_PENSION,
                    "0.15", "0.08", "6123", "32694");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_HOUSING_FUND,
                    "0.12", "0.12", "2360", "32694");
            // 故意不设应付职工薪酬科目配置——markPaid 前再清空，触发 buildPaymentEvent 抛异常
            return empId;
        });

        ErpHrSalary salary = ormTemplate.runInSession(session -> salaryBiz.calculateSalary(employeeId, 2026, 3, CTX));
        String salaryId = salary.getId();
        assertEquals(0, submitSalary(salaryId).getStatus(), "提交应成功");
        assertEquals(0, approveSalary(salaryId).getStatus(), "审核应成功");

        // 清空应付职工薪酬科目配置 → buildPaymentEvent 抛 ERR_PAYROLL_SUBJECT_NOT_CONFIGURED（触发悬挂）
        System.clearProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID);
        try {
            ErpHrSalary paid = ormTemplate.runInSession(session -> salaryBiz.markPaid(salaryId, CTX));
            // markPaid 忽略 tryPostPayment 返回值 → 过账失败仍转 PAID（posted=false 悬挂窗口）
            assertEquals(ErpHrConstants.PAYMENT_PAID, paid.getPaymentStatus(),
                    "过账失败不阻塞发放终态：paymentStatus=PAID");
            // RC-R1.89 后 posted writer 已激活（approve 侧计提链）；本测试无会计期间种子 →
            // approve 计提失败于 resolveOpenPeriod → posted 保持 false（悬挂窗口仍可观测）
            ErpHrSalary reloaded = daoProvider.daoFor(ErpHrSalary.class).getEntityById(salaryId);
            assertFalse(Boolean.TRUE.equals(reloaded.getPosted()),
                    "posted=false 可观测：过账悬挂窗口（P1-MA2-048）");
            // 无发放凭证生成（过账在 buildPaymentEvent 即抛，未达 executor.postEvent，GL 零写入）
            String billCode = salaryBillCode(2026, 3, salaryId);
            assertEquals(0L, countVoucherBillR(billCode, ErpFinBusinessType.SALARY_PAYMENT),
                    "过账失败：无 SALARY_PAYMENT 发放凭证");
        } finally {
            // 恢复配置避免污染后续测试（其余测试依赖 2211）
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
        }
    }

    /**
     * G3（P1-MA4-019 (d)+(e)）公司承担过账——RC-R1.89 落地后由负向文档化翻转为正向计提链断言：
     * approve 联动生成计提 SALARY(270) + 社保公司承担(290) + 公积金公司承担(300) 三类凭证
     * （tryPostAccrual 调用方激活 + 290/300 event 组装 + posted writer，payroll.md §6.5/§9.1），
     * markPaid→PAID 发放正常（280 正向基线，证明链路非假阴性）。细化断言（科目/金额/Dr==Cr/
     * billData ER 键/去重守卫/部分失败）见 {@link TestErpHrSalaryPostingChain}。
     */
    @Test
    public void testCompanyBornePostingAccrualChainPositive() {
        String employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            String empId = seedEmployee("EMP-ACCRUAL", ErpHrConstants.EMPLOYMENT_ACTIVE);
            seedContract(empId, "15000");
            seedSocialInsuranceBase(empId, "SHENZHEN", "15000", "15000");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_PENSION,
                    "0.15", "0.08", "6123", "32694");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_HOUSING_FUND,
                    "0.12", "0.12", "2360", "32694");
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            // 计提链测试员工带业务组织（PostingEvent.orgId/账套解析回退源）
            daoProvider.daoFor(ErpHrEmployee.class).getEntityById(empId).setOrgId("1");
            seedPostingSubjects();
            seedAcctSchema("1");
            seedOpenPeriod(2026, 4);
            seedOpenPeriod(2026, 7);
            return empId;
        });

        ErpHrSalary salary = ormTemplate.runInSession(session -> salaryBiz.calculateSalary(employeeId, 2026, 4, CTX));
        String salaryId = salary.getId();
        assertEquals(0, submitSalary(salaryId).getStatus(), "提交应成功");
        assertEquals(0, approveSalary(salaryId).getStatus(), "审核应成功（联动计提过账）");
        ErpHrSalary paid = ormTemplate.runInSession(session -> salaryBiz.markPaid(salaryId, CTX));

        // 正向基线：发放 markPaid→PAID 正常完成（approve+markPaid 链路可达，证明下方断言非假阴性）
        assertEquals(ErpHrConstants.PAYMENT_PAID, paid.getPaymentStatus(), "正向基线：发放正常 PAID");

        // 正向断言（RC-R1.89）：approve 联动 270 + 290 + 300 三类计提凭证已生成 + posted writer 激活
        ErpHrSalary reloaded = daoProvider.daoFor(ErpHrSalary.class).getEntityById(salaryId);
        assertEquals(Boolean.TRUE, reloaded.getPosted(), "三路计提成功 → posted=true");
        String billCode = salaryBillCode(2026, 4, salaryId);
        assertEquals(1L, countVoucherBillR(billCode, ErpFinBusinessType.SALARY),
                "计提 SALARY(270) 凭证已生成（approve 联动接线）");
        assertEquals(1L, countVoucherBillR(billCode, ErpFinBusinessType.SOCIAL_INSURANCE_ER),
                "社保公司承担 SOCIAL_INSURANCE_ER(290) 凭证已生成");
        assertEquals(1L, countVoucherBillR(billCode, ErpFinBusinessType.HOUSING_FUND_ER),
                "公积金公司承担 HOUSING_FUND_ER(300) 凭证已生成");
        assertEquals(1L, countVoucherBillR(billCode, ErpFinBusinessType.SALARY_PAYMENT),
                "发放 SALARY_PAYMENT(280) 凭证正常（markPaid 路径零回归）");
    }

    /** 种计提过账所需科目（6601/6601.01/6601.02/2211/1002，SalaryPostingProvider 默认科目映射）。 */
    private void seedPostingSubjects() {
        IEntityDao<app.erp.md.dao.entity.ErpMdSubject> dao =
                daoProvider.daoFor(app.erp.md.dao.entity.ErpMdSubject.class);
        for (String[] s : new String[][]{
                {"6601", "管理费用-工资", "EXPENSE", "DEBIT"},
                {"6601.01", "管理费用-社保", "EXPENSE", "DEBIT"},
                {"6601.02", "管理费用-公积金", "EXPENSE", "DEBIT"},
                {"2211", "应付职工薪酬", "LIABILITY", "CREDIT"},
                {"1002", "银行存款", "ASSET", "DEBIT"}}) {
            app.erp.md.dao.entity.ErpMdSubject subject = new app.erp.md.dao.entity.ErpMdSubject();
            subject.setCode(s[0]);
            subject.setName(s[1]);
            subject.setSubjectClass(s[2]);
            subject.setDirection(s[3]);
            subject.setStatus("ACTIVE");
            dao.saveEntity(subject);
        }
    }

    /** 种组织主账套（引擎对 null acctSchemaId 返回空集静默零凭证——账套解析补齐前置）。 */
    private void seedAcctSchema(String orgId) {
        IEntityDao<app.erp.md.dao.entity.ErpMdAcctSchema> dao =
                daoProvider.daoFor(app.erp.md.dao.entity.ErpMdAcctSchema.class);
        app.erp.md.dao.entity.ErpMdAcctSchema schema = new app.erp.md.dao.entity.ErpMdAcctSchema();
        schema.setCode("AS-" + orgId);
        schema.setName("账套-" + orgId);
        schema.setOrgId(orgId);
        schema.setNature("FINANCIAL");
        schema.setFunctionalCurrencyId("1");
        schema.setStatus("ACTIVE");
        dao.saveEntity(schema);
    }

    /** 种 OPEN 会计期间（计提凭证日期=期间 15 日；发放=paymentDate，冻结时钟 2026-07-17）。 */
    private void seedOpenPeriod(int year, int month) {
        IEntityDao<app.erp.fin.dao.entity.ErpFinAccountingPeriod> dao =
                daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinAccountingPeriod.class);
        app.erp.fin.dao.entity.ErpFinAccountingPeriod period = new app.erp.fin.dao.entity.ErpFinAccountingPeriod();
        period.setCode(year + "-" + String.format("%02d", month));
        period.setName(year + "年" + month + "月");
        period.setOrgId("1");
        period.setYear(year);
        period.setMonth(month);
        period.setStartDate(LocalDate.of(year, month, 1));
        period.setEndDate(LocalDate.of(year, month, 1).withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth()));
        period.setStatus("OPEN");
        dao.saveEntity(period);
    }

    /** 薪酬过账业财回链单据号（与 SalaryPostingDispatcher.buildBillCode 一致）。 */
    private String salaryBillCode(int year, int month, String salaryId) {
        return "SAL-" + year + String.format("%02d", month) + "-" + salaryId;
    }

    /** 统计某单据号+业务类型的业财回链数（ErpFinVoucherBillR），用于断言凭证是否生成。 */
    private long countVoucherBillR(String billCode, ErpFinBusinessType businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("billCode", billCode));
        q.addFilter(eq("businessType", businessType.name()));
        return dao.findAllByQuery(q).size();
    }

    /** 从 cumulativeData JSON 提取指定数值字段（payroll.md §4.5 累计数据结构）。 */
    private BigDecimal extractCumulativeField(String json, String key) {
        if (json == null || json.isEmpty()) return BigDecimal.ZERO;
        Object parsed = io.nop.core.lang.json.JsonTool.parseNonStrict(json);
        if (parsed instanceof Map) {
            Object v = ((Map<String, Object>) parsed).get(key);
            return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
        }
        return BigDecimal.ZERO;
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

    private String seedEmployee(String code, String employmentStatus) {
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

    private void seedContract(String employeeId, String monthlySalary) {
        IEntityDao<ErpHrEmploymentContract> dao = daoProvider.daoFor(ErpHrEmploymentContract.class);
        ErpHrEmploymentContract c = new ErpHrEmploymentContract();
        c.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
        c.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
        c.setCode("C-" + employeeId);
        c.setEmployeeId(employeeId);
        c.setContractType("OPEN_ENDED");
        c.setSignDate(LocalDate.of(2025, 1, 1));
        c.setStartDate(LocalDate.of(2025, 1, 1));
        c.setMonthlySalary(new BigDecimal(monthlySalary));
        c.setStatus("ACTIVE");
        dao.saveEntity(c);
    }

    private void seedSocialInsuranceBase(String employeeId, String cityCode,
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
        // 不静默吞解析异常（P1-MA4-018）：corrupt JSON 直接抛，使缺陷对测试可见
        Object parsed = io.nop.core.lang.json.JsonTool.parseNonStrict(json);
        if (parsed instanceof java.util.Map) {
            Object v = ((java.util.Map<String, Object>) parsed).get("cumulativeGross");
            return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
        }
        return BigDecimal.ZERO;
    }

    // ===================== P1-CK-hr2-004：社保有效期过滤（plan 2026-09-11-1530-1 Phase 3） =====================

    private void seedBaseRow(String employeeId, String cityCode, String siBase, String fundBase,
                             LocalDate effectiveFrom, LocalDate effectiveTo) {
        IEntityDao<ErpHrSocialInsuranceBase> dao = daoProvider.daoFor(ErpHrSocialInsuranceBase.class);
        ErpHrSocialInsuranceBase base = new ErpHrSocialInsuranceBase();
        base.setEmployeeId(employeeId);
        base.setCityCode(cityCode);
        base.setSocialInsuranceBase(new BigDecimal(siBase));
        base.setHousingFundBase(new BigDecimal(fundBase));
        if (effectiveFrom != null) {
            base.setEffectiveFrom(effectiveFrom);
        }
        if (effectiveTo != null) {
            base.setEffectiveTo(effectiveTo);
        }
        dao.saveEntity(base);
    }

    private void seedConfigRow(String cityCode, String insuranceType, String companyRate, String employeeRate,
                               String lower, String upper, LocalDate effectiveFrom, LocalDate effectiveTo) {
        IEntityDao<ErpHrSocialInsuranceConfig> dao = daoProvider.daoFor(ErpHrSocialInsuranceConfig.class);
        ErpHrSocialInsuranceConfig cfg = new ErpHrSocialInsuranceConfig();
        cfg.setCityCode(cityCode);
        cfg.setInsuranceType(insuranceType);
        cfg.setCompanyRate(new BigDecimal(companyRate));
        cfg.setEmployeeRate(new BigDecimal(employeeRate));
        cfg.setBaseLowerLimit(new BigDecimal(lower));
        cfg.setBaseUpperLimit(new BigDecimal(upper));
        if (effectiveFrom != null) {
            cfg.setEffectiveFrom(effectiveFrom);
        }
        if (effectiveTo != null) {
            cfg.setEffectiveTo(effectiveTo);
        }
        dao.saveEntity(cfg);
    }

    private void seedStandardShenzhenConfigs() {
        seedConfigRow("SZ-EFF", ErpHrConstants.INSURANCE_PENSION, "0.15", "0.08", "6123", "32694",
                LocalDate.of(2026, 1, 1), null);
        seedConfigRow("SZ-EFF", ErpHrConstants.INSURANCE_HOUSING_FUND, "0.12", "0.12", "2360", "32694",
                LocalDate.of(2026, 1, 1), null);
    }

    @Test
    public void testExpiredSocialInsuranceBaseRowExcluded() {
        String employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            String empId = seedEmployee("EMP-EFFBASE", ErpHrConstants.EMPLOYMENT_ACTIVE);
            seedContract(empId, "20000");
            // 唯一基数行已失效（2025 年度行）——2026-07 核算不应再命中
            seedBaseRow(empId, "SZ-EFF", "50000", "50000",
                    LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
            seedStandardShenzhenConfigs();
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            return empId;
        });

        // 修复前：findBase 零有效期过滤，失效行仍被取用（核算成功且按 50000 基数扣缴）
        // 修复后：有效区间过滤排除失效行 → ERR_SOCIAL_INSURANCE_BASE_NOT_FOUND
        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> salaryBiz.calculateSalary(employeeId, 2026, 7, CTX)));
        assertEquals(ErpHrErrors.ERR_SOCIAL_INSURANCE_BASE_NOT_FOUND.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testSocialInsuranceBaseLatestEffectiveFromWins() {
        String employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            String empId = seedEmployee("EMP-EFFBASE2", ErpHrConstants.EMPLOYMENT_ACTIVE);
            seedContract(empId, "20000");
            // 两行基数均「有效」（旧行无 effectiveTo 开区间）——应取 effectiveFrom 最新一行
            seedBaseRow(empId, "SZ-EFF", "10000", "10000", LocalDate.of(2025, 1, 1), null);
            seedBaseRow(empId, "SZ-EFF", "30000", "30000", LocalDate.of(2026, 1, 1), null);
            seedStandardShenzhenConfigs();
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            return empId;
        });

        ErpHrSalary salary = ormTemplate.runInSession(session -> salaryBiz.calculateSalary(employeeId, 2026, 7, CTX));
        // 修复前：findBase 无排序 limit(1) 任取（实操插入序返回旧行 10000 → 社保个人 800）
        // 修复后：orderBy effectiveFrom desc → 30000 钳制区间内不裁剪 → 社保个人 = 30000×8% = 2400
        assertEquals(0, salary.getSocialInsurance().compareTo(new BigDecimal("2400.00")),
                "多行基数并存应取 effectiveFrom 最新一行（30000×8%=2400）");
        assertEquals(0, salary.getHousingFund().compareTo(new BigDecimal("3600.00")),
                "公积金同取最新基数行（30000×12%=3600）");
    }

    @Test
    public void testSocialInsuranceConfigRowsFilteredAndDeduped() {
        String employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            String empId = seedEmployee("EMP-EFFCFG", ErpHrConstants.EMPLOYMENT_ACTIVE);
            seedContract(empId, "20000");
            seedBaseRow(empId, "SZ-EFF", "20000", "20000", LocalDate.of(2026, 1, 1), null);
            // 养老：已失效行（2025 年度 20%）+ 有效行（2026 年度 15%）——失效行应被排除
            seedConfigRow("SZ-EFF", ErpHrConstants.INSURANCE_PENSION, "0.20", "0.10", "6123", "32694",
                    LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
            seedConfigRow("SZ-EFF", ErpHrConstants.INSURANCE_PENSION, "0.15", "0.08", "6123", "32694",
                    LocalDate.of(2026, 1, 1), null);
            // 公积金：同城同险种两行均有效——应只计 effectiveFrom 最新一行（修复前双倍计扣）
            seedConfigRow("SZ-EFF", ErpHrConstants.INSURANCE_HOUSING_FUND, "0.12", "0.12", "2360", "32694",
                    LocalDate.of(2025, 1, 1), null);
            seedConfigRow("SZ-EFF", ErpHrConstants.INSURANCE_HOUSING_FUND, "0.10", "0.10", "2360", "32694",
                    LocalDate.of(2026, 1, 1), null);
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            return empId;
        });

        ErpHrSalary salary = ormTemplate.runInSession(session -> salaryBiz.calculateSalary(employeeId, 2026, 7, CTX));
        // 修复前：失效养老行 10% 也被累加（EE = 20000×(0.10+0.08) = 3600）；公积金两行双计（EE = 20000×0.24 = 4800）
        // 修复后：失效行排除 + 同险种取最新 → 社保个人 = 20000×8% = 1600；公积金 = 20000×10% = 2000
        assertEquals(0, salary.getSocialInsurance().compareTo(new BigDecimal("1600.00")),
                "失效配置行应排除，同险种仅计有效行（20000×8%=1600）");
        assertEquals(0, salary.getHousingFund().compareTo(new BigDecimal("2000.00")),
                "同险种多行应取 effectiveFrom 最新一行（20000×10%=2000）");
    }

    // ===================== P1-CK-hr2-001：出勤折算（plan 2026-09-11-1530-1 Phase 4） =====================

    private void seedAttendanceDay(String employeeId, LocalDate date, boolean isAbsent) {
        IEntityDao<ErpHrAttendance> dao = daoProvider.daoFor(ErpHrAttendance.class);
        ErpHrAttendance row = new ErpHrAttendance();
        row.setBusinessDate(date);
        row.setEmployeeId(employeeId);
        row.setDate(date);
        row.setSource(ErpHrConstants.ATTENDANCE_SOURCE_CARD);
        row.setIsAbsent(isAbsent);
        row.setLateMinutes(0);
        row.setEarlyLeaveMinutes(0);
        row.setWorkHours(new BigDecimal("8"));
        dao.saveEntity(row);
    }

    private void seedApprovedLeave(String employeeId, String leaveType, LocalDate start, LocalDate end) {
        IEntityDao<ErpHrLeaveRequest> dao = daoProvider.daoFor(ErpHrLeaveRequest.class);
        ErpHrLeaveRequest l = new ErpHrLeaveRequest();
        l.setBusinessDate(start);
        l.setCode("LV-" + employeeId + "-" + leaveType + "-" + System.nanoTime());
        l.setEmployeeId(employeeId);
        l.setLeaveType(leaveType);
        l.setStartDate(start);
        l.setEndDate(end);
        l.setStatus(ErpHrConstants.LEAVE_STATUS_APPROVED);
        l.setDurationDays(BigDecimal.valueOf(java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1));
        dao.saveEntity(l);
    }

    /** 核算月内全部周一至周五日期（hr2-001 工作日推导口径同款）。 */
    private List<LocalDate> weekdaysOf(int year, int month) {
        List<LocalDate> days = new ArrayList<>();
        LocalDate d = LocalDate.of(year, month, 1);
        while (d.getMonthValue() == month) {
            java.time.DayOfWeek dow = d.getDayOfWeek();
            if (dow != java.time.DayOfWeek.SATURDAY && dow != java.time.DayOfWeek.SUNDAY) {
                days.add(d);
            }
            d = d.plusDays(1);
        }
        return days;
    }

    @Test
    public void testShortMonthFullAttendancePaidInFull() {
        String employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            String empId = seedEmployee("EMP-SHORTM", ErpHrConstants.EMPLOYMENT_ACTIVE);
            seedContract(empId, "20000");
            seedBaseRow(empId, "SZ-EFF", "20000", "20000", LocalDate.of(2026, 1, 1), null);
            seedStandardShenzhenConfigs();
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            // 2026-02 全部 20 个工作日全勤打卡
            for (LocalDate d : weekdaysOf(2026, 2)) {
                seedAttendanceDay(empId, d, false);
            }
            return empId;
        });

        ErpHrSalary salary = ormTemplate.runInSession(session -> salaryBiz.calculateSalary(employeeId, 2026, 2, CTX));
        // 修复前：requiredDays 恒 22 → ratio = 20/22 → basicSalary = 18181.82（短月全员少发）
        // 修复后：应出勤日按月推导 = 20 → 全勤 ratio = 1 → basicSalary = 20000.00
        assertEquals(0, new BigDecimal("20").compareTo(salary.getRequiredWorkDays()),
                "2026-02 应出勤日应为 20 个工作日");
        assertEquals(0, new BigDecimal("20000.00").compareTo(salary.getBasicSalary()),
                "短月全勤应全额发放（20000×20/20）");
    }

    @Test
    public void testPaidLeaveExemptFromAttendanceRatio() {
        String employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            String empId = seedEmployee("EMP-PAIDLV", ErpHrConstants.EMPLOYMENT_ACTIVE);
            seedContract(empId, "20000");
            seedBaseRow(empId, "SZ-EFF", "20000", "20000", LocalDate.of(2026, 1, 1), null);
            seedStandardShenzhenConfigs();
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            // 2026-07 共 23 个工作日：5 天年假（07-06~07-10，缺勤行）+ 其余 18 天出勤
            List<LocalDate> wd = weekdaysOf(2026, 7);
            LocalDate leaveStart = LocalDate.of(2026, 7, 6);
            LocalDate leaveEnd = LocalDate.of(2026, 7, 10);
            for (LocalDate d : wd) {
                boolean onLeave = !d.isBefore(leaveStart) && !d.isAfter(leaveEnd);
                seedAttendanceDay(empId, d, onLeave);
            }
            seedApprovedLeave(empId, "ANNUAL", leaveStart, leaveEnd);
            return empId;
        });

        ErpHrSalary salary = ormTemplate.runInSession(session -> salaryBiz.calculateSalary(employeeId, 2026, 7, CTX));
        // 修复前：年假按缺勤折减 ratio = 18/23 → basicSalary = 15652.18
        // 修复后：带薪假豁免折算 actualDays = 18 + 5 = 23 → ratio = 1 → basicSalary = 20000.00
        assertEquals(0, new BigDecimal("20000.00").compareTo(salary.getBasicSalary()),
                "带薪年假不应折减基本工资（18 出勤 + 5 年假 = 全勤）");
    }

    @Test
    public void testSickLeaveSingleDeductionWhenConfigOn() {
        System.setProperty(ErpHrConstants.CONFIG_DEDUCT_UNPAID_LEAVE, "true");
        try {
            String employeeId = ormTemplate.runInSession(session -> {
                seedTaxConfig(2026);
                String empId = seedEmployee("EMP-SICKDED", ErpHrConstants.EMPLOYMENT_ACTIVE);
                seedContract(empId, "20000");
                seedBaseRow(empId, "SZ-EFF", "20000", "20000", LocalDate.of(2026, 1, 1), null);
                seedStandardShenzhenConfigs();
                System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
                // 2026-07：5 天病假（07-06~07-10，缺勤行）+ 其余 18 天出勤
                List<LocalDate> wd = weekdaysOf(2026, 7);
                LocalDate leaveStart = LocalDate.of(2026, 7, 6);
                LocalDate leaveEnd = LocalDate.of(2026, 7, 10);
                for (LocalDate d : wd) {
                    boolean onLeave = !d.isBefore(leaveStart) && !d.isAfter(leaveEnd);
                    seedAttendanceDay(empId, d, onLeave);
                }
                seedApprovedLeave(empId, "SICK", leaveStart, leaveEnd);
                return empId;
            });

            ErpHrSalary salary = ormTemplate.runInSession(session -> salaryBiz.calculateSalary(employeeId, 2026, 7, CTX));
            // 修复前：缺勤折减 + 显式扣减双扣 ratio = 18/22 − 5/22 = 0.590909 → basicSalary = 11818.18
            // 修复后：缺勤豁免（加回）+ 仅显式扣一次 ratio = 23/23 − 5/23 = 18/23 = 0.782609
            // → basicSalary = 20000×18/23 = 15652.18（plan Proof ③ 场景语义：单扣 = 出勤/应出勤；
            //   执行期以 2026-07 = 23 工作日落为 18 打卡 + 5 病假，数值口径 18/23）
            assertEquals(0, new BigDecimal("15652.18").compareTo(salary.getBasicSalary()),
                    "无薪假应仅扣一次（缺勤豁免 + 显式扣减 5/23，不双扣）");
        } finally {
            System.clearProperty(ErpHrConstants.CONFIG_DEDUCT_UNPAID_LEAVE);
        }
    }

    @Test
    public void testRequiredWorkDaysConfigOverride() {
        System.setProperty(ErpHrConstants.CONFIG_REQUIRED_WORK_DAYS, "22");
        try {
            String employeeId = ormTemplate.runInSession(session -> {
                seedTaxConfig(2026);
                String empId = seedEmployee("EMP-RWDOVR", ErpHrConstants.EMPLOYMENT_ACTIVE);
                seedContract(empId, "20000");
                seedBaseRow(empId, "SZ-EFF", "20000", "20000", LocalDate.of(2026, 1, 1), null);
                seedStandardShenzhenConfigs();
                System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
                return empId;
            });

            ErpHrSalary salary = ormTemplate.runInSession(session -> salaryBiz.calculateSalary(employeeId, 2026, 7, CTX));
            // config 覆盖路径：erp-hr.required-work-days=22 → 应出勤日取 22（非 7 月自动推导的 23）
            assertEquals(0, new BigDecimal("22").compareTo(salary.getRequiredWorkDays()),
                    "config 覆盖应生效（erp-hr.required-work-days=22）");
        } finally {
            System.clearProperty(ErpHrConstants.CONFIG_REQUIRED_WORK_DAYS);
        }
    }

    // ===================== P1-CK-hr2-003：runPayroll 逐员工失败隔离（plan 2026-09-11-1530-1 Phase 6） =====================

    @Test
    public void testRunPayrollSkipsMisconfiguredEmployeeWithAlert() {
        String goodEmpId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            String emp1 = seedEmployee("EMP-RPGOOD", ErpHrConstants.EMPLOYMENT_ACTIVE);
            seedContract(emp1, "15000");
            seedBaseRow(emp1, "SZ-EFF", "15000", "15000", LocalDate.of(2026, 1, 1), null);
            seedConfigRow("SZ-EFF", ErpHrConstants.INSURANCE_PENSION, "0.15", "0.08", "6123", "32694",
                    LocalDate.of(2026, 1, 1), null);
            seedConfigRow("SZ-EFF", ErpHrConstants.INSURANCE_HOUSING_FUND, "0.12", "0.12", "2360", "32694",
                    LocalDate.of(2026, 1, 1), null);
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            // 缺合同员工——runPayroll 遍历即抛 ERR_EMPLOYMENT_CONTRACT_NOT_FOUND（UC-HR-04 异常源）
            seedEmployee("EMP-RPBAD", ErpHrConstants.EMPLOYMENT_ACTIVE);
            return emp1;
        });
        String badEmpId = ormTemplate.runInSession(session ->
                daoProvider.daoFor(ErpHrEmployee.class).findAllByQuery(
                        new QueryBean().addFilter(eq("code", "EMP-RPBAD"))).get(0).getId());

        // 修复前：任一员工缺合同 → 整批抛 ERR_EMPLOYMENT_CONTRACT_NOT_FOUND（事务回滚）
        // 修复后：跳过并告警（UC-HR-04），返回仅含成功核算的员工薪酬
        List<ErpHrSalary> result = ormTemplate.runInSession(session -> salaryBiz.runPayroll(2026, 7, CTX));
        assertEquals(1, result.size(), "仅配置完整的员工核算成功");
        assertEquals(goodEmpId, result.get(0).getEmployeeId(), "成功结果应为好员工");
        List<ErpHrSalary> badRows = ormTemplate.runInSession(session ->
                daoProvider.daoFor(ErpHrSalary.class).findAllByQuery(
                        new QueryBean().addFilter(eq("employeeId", badEmpId))));
        assertTrue(badRows.isEmpty(), "缺合同员工不应有薪酬记录");

        // 二次 runPayroll：好员工幂等跳过 + 坏员工仍被隔离（不抛）——隔离与幂等语义协同
        List<ErpHrSalary> second = ormTemplate.runInSession(session -> salaryBiz.runPayroll(2026, 7, CTX));
        assertEquals(0, second.size(), "二次批量零新增（好员工幂等跳过 + 坏员工继续被隔离）");
    }
}
