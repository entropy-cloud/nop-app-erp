package app.erp.hr.service;

import app.erp.common.service.MaskHelper;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrEmploymentContract;
import app.erp.hr.dao.entity.ErpHrSalary;
import app.erp.hr.dao.entity.ErpHrSalarySimulationItemAdjustment;
import app.erp.hr.dao.entity.ErpHrSocialInsuranceBase;
import app.erp.hr.service.entity.ErpHrEmployeeBizModel;
import app.erp.hr.service.entity.ErpHrEmploymentContractBizModel;
import app.erp.hr.service.entity.ErpHrSalaryBizModel;
import app.erp.hr.service.entity.ErpHrSalarySimulationItemAdjustmentBizModel;
import app.erp.hr.service.entity.ErpHrSocialInsuranceBaseBizModel;
import io.nop.api.core.auth.IUserContext;
import io.nop.auth.core.login.UserContextImpl;
import io.nop.core.unittest.BaseTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * E3.1 后端响应层脱敏 hr 域单元测试（plan 2026-08-10-2059-2 Phase 2 Proof）。
 *
 * <p>直接调用各 entity BizModel 的 {@code @BizLoader} 方法，验证：
 * <ul>
 *   <li>授权角色（薪酬审批人 / HR 专员）→ 明文；</li>
 *   <li>非授权角色 → 数值 null / VARCHAR 打码字符串；</li>
 *   <li>无用户上下文（fail-closed）→ 打码。</li>
 * </ul>
 *
 * <p>纯逻辑测试：无 DB / 无 IoC，BizModel 直接实例化（loader 方法仅依赖 {@link MaskHelper} + 实体 getter，
 * 不触 @Inject 字段）。role 注入经 {@link IUserContext#set}（同 {@code TestErpHrAttendanceMakeUp} 范式）。
 */
public class TestErpHrResponseMasking extends BaseTestCase {

    private static final BigDecimal AMOUNT = new BigDecimal("12345.67");
    private static final String ID_CARD = "110101199001011234";
    private static final String MOBILE = "13812345678";
    private static final Long BANK_ACCT = 6228480402564890018L;
    private static final String SS_NO = "SOC-2026-0001";
    private static final String TAX_FILE = "TAX-2026-0001";
    private static final String CUMULATIVE = "{\"ytdTax\":5000}";

    private final ErpHrSalaryBizModel salaryBiz = new ErpHrSalaryBizModel();
    private final ErpHrEmployeeBizModel employeeBiz = new ErpHrEmployeeBizModel();
    private final ErpHrEmploymentContractBizModel contractBiz = new ErpHrEmploymentContractBizModel();
    private final ErpHrSocialInsuranceBaseBizModel insuranceBaseBiz = new ErpHrSocialInsuranceBaseBizModel();
    private final ErpHrSalarySimulationItemAdjustmentBizModel simAdjBiz = new ErpHrSalarySimulationItemAdjustmentBizModel();

    private IUserContext prevCtx;

    @BeforeEach
    void saveContext() {
        prevCtx = IUserContext.get();
    }

    @AfterEach
    void restoreContext() {
        IUserContext.set(prevCtx);
    }

    @Test
    public void salaryAmountsAuthorizedRoleSeesPlaintext() {
        loginAs(MaskHelper.ROLE_SALARY_APPROVER);
        ErpHrSalary s = newSalary();
        assertEquals(0, AMOUNT.compareTo(salaryBiz.basicSalaryMask(s)), "薪酬审批人见 basicSalary 明文");
        assertEquals(0, AMOUNT.compareTo(salaryBiz.netSalaryMask(s)), "薪酬审批人见 netSalary 明文");
        assertEquals(0, AMOUNT.compareTo(salaryBiz.taxAmountMask(s)), "薪酬审批人见 taxAmount 明文");
        assertEquals(0, AMOUNT.compareTo(salaryBiz.grossSalaryMask(s)), "薪酬审批人见 grossSalary 明文");
        assertEquals(CUMULATIVE, salaryBiz.cumulativeDataMask(s), "薪酬审批人见 cumulativeData 明文");
    }

    @Test
    public void salaryAmountsUnauthorizedSeesNull() {
        loginAs("STAFF");
        ErpHrSalary s = newSalary();
        assertNull(salaryBiz.basicSalaryMask(s), "非授权 basicSalary = null");
        assertNull(salaryBiz.netSalaryMask(s), "非授权 netSalary = null");
        assertNull(salaryBiz.taxAmountMask(s), "非授权 taxAmount = null");
        assertNull(salaryBiz.grossSalaryMask(s), "非授权 grossSalary = null");
        assertEquals("******", salaryBiz.cumulativeDataMask(s), "非授权 cumulativeData = 全打码");
    }

    @Test
    public void salaryAmountsNoContextFailClosed() {
        IUserContext.set(null);
        ErpHrSalary s = newSalary();
        assertNull(salaryBiz.basicSalaryMask(s), "无上下文 basicSalary = null（fail-closed）");
        assertEquals("******", salaryBiz.cumulativeDataMask(s), "无上下文 cumulativeData = 全打码");
    }

    @Test
    public void piiAuthorizedRolesSeePlaintext() {
        loginAs(MaskHelper.ROLE_HR_SPECIALIST);
        ErpHrEmployee e = newEmployee();
        assertEmployeePlaintext(e, "HR 专员");
        loginAs(MaskHelper.ROLE_SALARY_APPROVER);
        assertEmployeePlaintext(e, "薪酬审批人");
    }

    @Test
    public void piiUnauthorizedSeesMasked() {
        loginAs("STAFF");
        ErpHrEmployee e = newEmployee();
        assertEquals(ID_CARD.charAt(0) + "******" + ID_CARD.substring(ID_CARD.length() - 4),
                employeeBiz.idCardNoMask(e), "非授权 idCardNo = 首1******末4");
        assertEquals(MOBILE.substring(0, 3) + "****" + MOBILE.substring(MOBILE.length() - 4),
                employeeBiz.mobilePhoneMask(e), "非授权 mobilePhone = 首3****末4");
        assertNull(employeeBiz.bankAccountIdMask(e), "非授权 bankAccountId = null（BIGINT）");
        assertEquals("******", employeeBiz.socialSecurityNoMask(e), "非授权 socialSecurityNo = 全打码");
    }

    @Test
    public void taxFileNoHrSpecialistSeesPlaintextOthersMasked() {
        ErpHrEmployee e = newEmployee();
        loginAs(MaskHelper.ROLE_HR_SPECIALIST);
        assertEquals(TAX_FILE, employeeBiz.taxFileNoMask(e), "HR 专员见 taxFileNo 明文");
        loginAs(MaskHelper.ROLE_SALARY_APPROVER);
        assertEquals("******", employeeBiz.taxFileNoMask(e), "薪酬审批人（非 HR 专员）见 taxFileNo 打码");
        loginAs("STAFF");
        assertEquals("******", employeeBiz.taxFileNoMask(e), "非授权 taxFileNo = 全打码");
    }

    @Test
    public void employmentContractSocialInsuranceBaseMasked() {
        ErpHrEmploymentContract c = new ErpHrEmploymentContract();
        c.setSocialInsuranceBase(AMOUNT);
        loginAs(MaskHelper.ROLE_SALARY_APPROVER);
        assertEquals(0, AMOUNT.compareTo(contractBiz.socialInsuranceBaseMask(c)), "薪酬审批人见 socialInsuranceBase 明文");
        loginAs("STAFF");
        assertNull(contractBiz.socialInsuranceBaseMask(c), "非授权 socialInsuranceBase = null");
    }

    @Test
    public void socialInsuranceBaseBothFieldsMasked() {
        ErpHrSocialInsuranceBase b = new ErpHrSocialInsuranceBase();
        b.setSocialInsuranceBase(AMOUNT);
        b.setHousingFundBase(AMOUNT);
        loginAs(MaskHelper.ROLE_SALARY_APPROVER);
        assertEquals(0, AMOUNT.compareTo(insuranceBaseBiz.socialInsuranceBaseMask(b)), "授权 socialInsuranceBase 明文");
        assertEquals(0, AMOUNT.compareTo(insuranceBaseBiz.housingFundBaseMask(b)), "授权 housingFundBase 明文");
        loginAs("STAFF");
        assertNull(insuranceBaseBiz.socialInsuranceBaseMask(b), "非授权 socialInsuranceBase = null");
        assertNull(insuranceBaseBiz.housingFundBaseMask(b), "非授权 housingFundBase = null");
    }

    @Test
    public void salarySimulationAdjustmentBothFieldsMasked() {
        ErpHrSalarySimulationItemAdjustment a = new ErpHrSalarySimulationItemAdjustment();
        a.setOriginalAmount(AMOUNT);
        a.setAdjustedAmount(AMOUNT);
        loginAs(MaskHelper.ROLE_SALARY_APPROVER);
        assertEquals(0, AMOUNT.compareTo(simAdjBiz.originalAmountMask(a)), "授权 originalAmount 明文");
        assertEquals(0, AMOUNT.compareTo(simAdjBiz.adjustedAmountMask(a)), "授权 adjustedAmount 明文");
        loginAs("STAFF");
        assertNull(simAdjBiz.originalAmountMask(a), "非授权 originalAmount = null");
        assertNull(simAdjBiz.adjustedAmountMask(a), "非授权 adjustedAmount = null");
    }

    // ---------- helpers ----------

    private void assertEmployeePlaintext(ErpHrEmployee e, String label) {
        assertEquals(ID_CARD, employeeBiz.idCardNoMask(e), label + "见 idCardNo 明文");
        assertEquals(MOBILE, employeeBiz.mobilePhoneMask(e), label + "见 mobilePhone 明文");
        assertEquals(BANK_ACCT, employeeBiz.bankAccountIdMask(e), label + "见 bankAccountId 明文");
        assertEquals(SS_NO, employeeBiz.socialSecurityNoMask(e), label + "见 socialSecurityNo 明文");
    }

    private void loginAs(String... roles) {
        UserContextImpl ctx = new UserContextImpl();
        ctx.setUserId("mask-test");
        ctx.setUserName("mask-test");
        ctx.setRoles(Set.of(roles));
        IUserContext.set(ctx);
    }

    private ErpHrSalary newSalary() {
        ErpHrSalary s = new ErpHrSalary();
        s.setBasicSalary(AMOUNT);
        s.setPositionAllowance(AMOUNT);
        s.setPerformanceBonus(AMOUNT);
        s.setOvertimePay(AMOUNT);
        s.setMealAllowance(AMOUNT);
        s.setTransportAllowance(AMOUNT);
        s.setOtherAllowance(AMOUNT);
        s.setGrossSalary(AMOUNT);
        s.setSocialInsurance(AMOUNT);
        s.setHousingFund(AMOUNT);
        s.setTaxAmount(AMOUNT);
        s.setOtherDeductions(AMOUNT);
        s.setNetSalary(AMOUNT);
        s.setCumulativeData(CUMULATIVE);
        return s;
    }

    private ErpHrEmployee newEmployee() {
        ErpHrEmployee e = new ErpHrEmployee();
        e.setIdCardNo(ID_CARD);
        e.setMobilePhone(MOBILE);
        e.setBankAccountId(BANK_ACCT);
        e.setSocialSecurityNo(SS_NO);
        e.setTaxFileNo(TAX_FILE);
        return e;
    }
}
