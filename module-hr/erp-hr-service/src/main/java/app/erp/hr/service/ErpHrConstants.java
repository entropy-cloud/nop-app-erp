package app.erp.hr.service;

/**
 * HR 域状态码与配置键常量。权威值来自 {@code module-hr/model/app-erp-hr.orm.xml}
 * 关联字典 {@code erp-hr/salary-approval-status}、{@code erp-hr/salary-item-category} 等。
 */
public interface ErpHrConstants {

    // ---- 配置项（payroll.md §配置），经 AppConfig.var 读取 ----
    /** 默认社保基数参保城市（为空表示须配置）。 */
    String CONFIG_DEFAULT_SOCIAL_INSURANCE_BASE_CITY = "erp-hr.default-social-insurance-base-city";
    /** 个税月起征点（默认 5000）。 */
    String CONFIG_TAX_THRESHOLD_MONTHLY = "erp-hr.tax-threshold-monthly";
    /** 应付职工薪酬贷方科目编码；为空时抛 ERR_PAYROLL_SUBJECT_NOT_CONFIGURED。 */
    String CONFIG_DEFAULT_PAYROLL_SUBJECT_ID = "erp-hr.default-payroll-subject-id";
    /** 薪酬金额四舍五入小数位（默认 2 位）。 */
    String CONFIG_SALARY_ROUNDING_SCALE = "erp-hr.salary-rounding-scale";

    // ---- salary-approval-status（payroll.md §6.1，权威端到端状态） ----
    String APPROVAL_PENDING = "PENDING";
    String APPROVAL_REVIEWED = "REVIEWED";
    String APPROVAL_APPROVED_FINANCE = "APPROVED_FINANCE";
    String APPROVAL_APPROVED_MANAGER = "APPROVED_MANAGER";
    String APPROVAL_PAID = "PAID";
    String APPROVAL_VOID = "VOID";

    // ---- salary-payment-status（存量 3 态，approvalStatus 的派生投影） ----
    String PAYMENT_PENDING = "PENDING";
    String PAYMENT_PAID = "PAID";
    String PAYMENT_VOID = "VOID";

    // ---- salary-item-category ----
    String ITEM_CATEGORY_EARNINGS = "EARNINGS";
    String ITEM_CATEGORY_DEDUCTION = "DEDUCTION";

    // ---- salary-item-group（payroll.md §1.1 预置项目分组） ----
    String ITEM_GROUP_BASIC = "BASIC";
    String ITEM_GROUP_ALLOWANCE = "ALLOWANCE";
    String ITEM_GROUP_BONUS = "BONUS";
    String ITEM_GROUP_OVERTIME = "OVERTIME";
    String ITEM_GROUP_SOCIAL = "SOCIAL";
    String ITEM_GROUP_FUND = "FUND";
    String ITEM_GROUP_TAX = "TAX";
    String ITEM_GROUP_OTHER = "OTHER";

    // ---- calc-method ----
    String CALC_METHOD_FIXED = "FIXED";
    String CALC_METHOD_FORMULA = "FORMULA";
    String CALC_METHOD_INPUT = "INPUT";

    // ---- social-insurance-type ----
    String INSURANCE_PENSION = "PENSION";
    String INSURANCE_MEDICAL = "MEDICAL";
    String INSURANCE_UNEMPLOYMENT = "UNEMPLOYMENT";
    String INSURANCE_WORK_INJURY = "WORK_INJURY";
    String INSURANCE_MATERNITY = "MATERNITY";
    String INSURANCE_HOUSING_FUND = "HOUSING_FUND";

    // ---- bank-file-format ----
    String BANK_FILE_FORMAT_CSV = "CSV";
    String BANK_FILE_FORMAT_TXT = "TXT";

    // ---- bank-file-status ----
    String BANK_FILE_STATUS_GENERATED = "GENERATED";

    // ---- employment-status（用于 runPayroll 限定核算员工范围） ----
    String EMPLOYMENT_ACTIVE = "ACTIVE";
    String EMPLOYMENT_PROBATION = "PROBATION";

    // ---- PostingEvent.billData 键（hr 过账派发器填入，Provider 读取） ----
    String BILL_DATA_SALARY_ID = "SALARY_ID";
    String BILL_DATA_EMPLOYEE_ID = "EMPLOYEE_ID";
    String BILL_DATA_DEPARTMENT_ID = "DEPARTMENT_ID";
    String BILL_DATA_COST_CENTER_ID = "COST_CENTER_ID";
    String BILL_DATA_GROSS_AMOUNT = "GROSS_AMOUNT";
    String BILL_DATA_SOCIAL_INSURANCE_ER = "SOCIAL_INSURANCE_ER";
    String BILL_DATA_HOUSING_FUND_ER = "HOUSING_FUND_ER";
    String BILL_DATA_NET_AMOUNT = "NET_AMOUNT";
    String BILL_DATA_DEBIT_SUBJECT_CODE = "DEBIT_SUBJECT_CODE";
    String BILL_DATA_CREDIT_SUBJECT_CODE = "CREDIT_SUBJECT_CODE";
    String BILL_DATA_SOURCE_BILL_TYPE = "SOURCE_BILL_TYPE";

    // ---- 归集行来源单据类型（ PostingEvent.billData[SOURCE_BILL_TYPE]） ----
    String SOURCE_BILL_TYPE_SALARY = "SALARY";
    String SOURCE_BILL_TYPE_SALARY_PAYMENT = "SALARY_PAYMENT";
    String SOURCE_BILL_TYPE_SOCIAL_INSURANCE_ER = "SOCIAL_INSURANCE_ER";
    String SOURCE_BILL_TYPE_HOUSING_FUND_ER = "HOUSING_FUND_ER";
}
