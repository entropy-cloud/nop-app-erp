package app.erp.hr.service;

import io.nop.api.core.exceptions.ErrorCode;

/**
 * HR 域业务错误码。薪酬核算/审批/过账/银行文件流程中的业务异常使用
 * {@link io.nop.api.core.exceptions.NopException} + 本接口的 {@link ErrorCode}。
 * 描述用中文，框架经 i18n 翻译。
 */
public interface ErpHrErrors {

    // --- 作用域参数键 ---
    String ARG_EMPLOYEE_ID = "employeeId";
    String ARG_SALARY_ID = "salaryId";
    String ARG_YEAR = "year";
    String ARG_MONTH = "month";
    String ARG_CURRENT_STATUS = "currentStatus";
    String ARG_EXPECTED_STATUS = "expectedStatus";
    String ARG_CITY_CODE = "cityCode";
    String ARG_SUBJECT_CODE = "subjectCode";
    String ARG_BATCH_NO = "batchNo";
    String ARG_BANK_ID = "bankId";

    // --- 薪酬核算：配置缺失 ---
    ErrorCode ERR_SOCIAL_INSURANCE_BASE_NOT_FOUND = ErrorCode.define(
            "erp.err.hr.social-insurance-base-not-found",
            "员工 {employeeId} 在 {year} 年 {month} 月无有效社保基数配置",
            ARG_EMPLOYEE_ID, ARG_YEAR, ARG_MONTH);
    ErrorCode ERR_SOCIAL_INSURANCE_CONFIG_NOT_FOUND = ErrorCode.define(
            "erp.err.hr.social-insurance-config-not-found",
            "城市 {cityCode} 无有效社保配置（ErpHrSocialInsuranceConfig）",
            ARG_CITY_CODE);
    ErrorCode ERR_HOUSING_FUND_CONFIG_NOT_FOUND = ErrorCode.define(
            "erp.err.hr.housing-fund-config-not-found",
            "城市 {cityCode} 无有效公积金配置（ErpHrSocialInsuranceConfig HOUSING_FUND）",
            ARG_CITY_CODE);
    ErrorCode ERR_TAX_CONFIG_NOT_FOUND = ErrorCode.define(
            "erp.err.hr.tax-config-not-found",
            "{year} 年度个税配置缺失（ErpHrTaxConfig）",
            ARG_YEAR);
    ErrorCode ERR_EMPLOYMENT_CONTRACT_NOT_FOUND = ErrorCode.define(
            "erp.err.hr.employment-contract-not-found",
            "员工 {employeeId} 无生效中的劳动合同（ErpHrEmploymentContract）",
            ARG_EMPLOYEE_ID);

    // --- 薪酬核算：幂等 ---
    ErrorCode ERR_SALARY_ALREADY_EXISTS = ErrorCode.define(
            "erp.err.hr.salary-already-exists",
            "员工 {employeeId} 在 {year} 年 {month} 月已存在非 VOID 薪酬记录，禁止重复核算",
            ARG_EMPLOYEE_ID, ARG_YEAR, ARG_MONTH);

    // --- 审批状态机 ---
    ErrorCode ERR_SALARY_ILLEGAL_STATUS_TRANSITION = ErrorCode.define(
            "erp.err.hr.salary.illegal-status-transition",
            "薪酬记录 {salaryId} 当前状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_SALARY_ID, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);
    ErrorCode ERR_SALARY_LOCKED_AFTER_PAID = ErrorCode.define(
            "erp.err.hr.salary.locked-after-paid",
            "薪酬记录 {salaryId} 已发放（PAID 终态），锁定不可修改，调整走补发/追扣流程",
            ARG_SALARY_ID);

    // --- 业财过账科目配置 ---
    ErrorCode ERR_PAYROLL_SUBJECT_NOT_CONFIGURED = ErrorCode.define(
            "erp.err.hr.payroll-subject-not-configured",
            "应付职工薪酬贷方科目未配置（配置键 erp-hr.default-payroll-subject-id），无法生成薪酬凭证",
            ARG_SUBJECT_CODE);

    // --- 银行文件生成 ---
    ErrorCode ERR_NO_APPROVED_SALARY_FOR_BANK_FILE = ErrorCode.define(
            "erp.err.hr.no-approved-salary-for-bank-file",
            "未找到 APPROVED_MANAGER 状态的薪酬记录，无法生成银行代发文件",
            ARG_BANK_ID);
}
