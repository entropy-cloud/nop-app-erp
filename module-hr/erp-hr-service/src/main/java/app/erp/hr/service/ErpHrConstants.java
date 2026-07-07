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

    // ---- 排班配置项（shift-scheduling.md §配置）----
    /** 排班调换是否需审批（默认 true）。 */
    String CONFIG_SHIFT_REQUIRE_APPROVAL = "erp-hr.shift-require-approval";
    /** 默认迟到宽容分钟数（默认 15）。 */
    String CONFIG_SHIFT_DEFAULT_GRACE_LATE_MINUTES = "erp-hr.shift-default-grace-late-minutes";
    /** 默认早退宽容分钟数（默认 15）。 */
    String CONFIG_SHIFT_DEFAULT_GRACE_EARLY_LEAVE_MINUTES = "erp-hr.shift-default-grace-early-leave-minutes";
    /** 是否允许跨天班次（默认 true）。 */
    String CONFIG_SHIFT_CROSS_DAY_ENABLED = "erp-hr.shift-cross-day-enabled";

    // ---- approve-status（标准 wf/approve-status 四态，payroll.md §设计修正记录三轴分离） ----
    String APPROVE_STATUS_UNSUBMITTED = "UNSUBMITTED";
    String APPROVE_STATUS_SUBMITTED = "SUBMITTED";
    String APPROVE_STATUS_APPROVED = "APPROVED";
    String APPROVE_STATUS_REJECTED = "REJECTED";

    // ---- salary-payment-status（独立支付轴，与 approveStatus 分离） ----
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

    // ---- ErpHrShiftAssignment.status（shift-scheduling.md §2.1） ----
    String ASSIGNMENT_STATUS_SCHEDULED = "SCHEDULED";
    String ASSIGNMENT_STATUS_PRESENT = "PRESENT";
    String ASSIGNMENT_STATUS_ABSENT = "ABSENT";
    String ASSIGNMENT_STATUS_CANCELLED = "CANCELLED";

    // ---- erp-hr/absence-reason（shift-scheduling.md §2.1） ----
    String ABSENCE_REASON_LEAVE = "LEAVE";
    String ABSENCE_REASON_LATE_NOT_CLOCKED = "LATE_NOT_CLOCKED";
    String ABSENCE_REASON_OTHER = "OTHER";

    // ---- erp-hr/swap-status（shift-scheduling.md §5.1） ----
    String SWAP_STATUS_PENDING = "PENDING";
    String SWAP_STATUS_APPROVED = "APPROVED";
    String SWAP_STATUS_REJECTED = "REJECTED";
    String SWAP_STATUS_CANCELLED = "CANCELLED";

    // ---- erp-hr/leave-status（已存在字典）----
    String LEAVE_STATUS_APPROVED = "APPROVED";
    String LEAVE_STATUS_CANCELLED = "CANCELLED";

    // ---- ErpHrShiftRotationPattern.patternType（shift-scheduling.md §3.1） ----
    String PATTERN_TYPE_CYCLE_DAYS = "CYCLE_DAYS";
    String PATTERN_TYPE_CYCLE_WEEKS = "CYCLE_WEEKS";
    String PATTERN_OFF_SHIFT_CODE = "OFF";

    // ---- erp-hr/simulation-status（payroll-simulation.md §1.1） ----
    String SIMULATION_STATUS_DRAFT = "DRAFT";
    String SIMULATION_STATUS_IN_REVIEW = "IN_REVIEW";
    String SIMULATION_STATUS_APPROVED = "APPROVED";
    String SIMULATION_STATUS_REJECTED = "REJECTED";
    String SIMULATION_STATUS_CONVERTED = "CONVERTED";

    // ---- erp-hr/adjustment-reason（payroll-simulation.md §2.2） ----
    String ADJUSTMENT_REASON_SALARY_CHANGE = "SALARY_CHANGE";
    String ADJUSTMENT_REASON_ALLOWANCE_CHANGE = "ALLOWANCE_CHANGE";
    String ADJUSTMENT_REASON_BONUS_CHANGE = "BONUS_CHANGE";
    String ADJUSTMENT_REASON_MANUAL_ENTRY = "MANUAL_ENTRY";

    // ---- 模拟配置项（payroll-simulation.md §3.3）----
    String CONFIG_SIM_NET_PAY_CHANGE_THRESHOLD = "erp-hr.simulation.net-pay-change-threshold";
    String CONFIG_SIM_TOTAL_CHANGE_THRESHOLD = "erp-hr.simulation.total-change-threshold";
    String CONFIG_SIM_TAX_BRACKET_JUMP_ALERT = "erp-hr.simulation.tax-bracket-jump-alert";
    String CONFIG_SIM_AUTO_CONVERT_ENABLED = "erp-hr.simulation.auto-convert-enabled";

    // ---- 批量调薪类型（payroll-simulation.md §5.1）----
    String BATCH_ADJUST_TYPE_FIXED = "FIXED";
    String BATCH_ADJUST_TYPE_RATIO = "RATIO";
    String BATCH_ADJUST_TYPE_ALLOWANCE = "ALLOWANCE";
    String BATCH_ADJUST_TYPE_LEVEL_MAP = "LEVEL_MAP";

    // ---- 模拟异常告警类型（payroll-simulation.md §3.3）----
    String ANOMALY_NET_PAY_CHANGE = "NET_PAY_CHANGE";
    String ANOMALY_TOTAL_CHANGE = "TOTAL_CHANGE";
    String ANOMALY_TAX_BRACKET_JUMP = "TAX_BRACKET_JUMP";
    String ANOMALY_SOCIAL_BASE_OUT_OF_RANGE = "SOCIAL_BASE_OUT_OF_RANGE";

    // ---- 胜任力管理配置项（competency-management.md §配置点）----
    /** 360 评估自评权重（默认 0.15）。 */
    String CONFIG_ASSESSMENT_SELF_WEIGHT = "erp-hr.assessment-self-weight";
    /** 360 评估上级权重（默认 0.50）。 */
    String CONFIG_ASSESSMENT_MANAGER_WEIGHT = "erp-hr.assessment-manager-weight";
    /** 360 评估同级权重（默认 0.25）。 */
    String CONFIG_ASSESSMENT_PEER_WEIGHT = "erp-hr.assessment-peer-weight";
    /** 360 评估下级权重（默认 0.10）。 */
    String CONFIG_ASSESSMENT_SUBORDINATE_WEIGHT = "erp-hr.assessment-subordinate-weight";
    /** 严重差距阈值（gapValue ≥ 此值视为 CRITICAL，默认 3）。 */
    String CONFIG_GAP_CRITICAL_THRESHOLD = "erp-hr.gap-critical-threshold";

    // ---- erp-hr/assessment-type（competency-management.md §评估类型） ----
    String ASSESSMENT_TYPE_SELF = "SELF";
    String ASSESSMENT_TYPE_MANAGER = "MANAGER";
    String ASSESSMENT_TYPE_PEER = "PEER";
    String ASSESSMENT_TYPE_SUBORDINATE = "SUBORDINATE";
    String ASSESSMENT_TYPE_360 = "360";

    // ---- erp-hr/assessment-status（competency-management.md §评估流程） ----
    String ASSESSMENT_STATUS_DRAFT = "DRAFT";
    String ASSESSMENT_STATUS_SUBMITTED = "SUBMITTED";
    String ASSESSMENT_STATUS_COMPLETED = "COMPLETED";

    // ---- erp-hr/gap-severity（competency-management.md §差距严重程度规则） ----
    String GAP_SEVERITY_NONE = "NONE";
    String GAP_SEVERITY_MINOR = "MINOR";
    String GAP_SEVERITY_MODERATE = "MODERATE";
    String GAP_SEVERITY_CRITICAL = "CRITICAL";

    // ---- erp-hr/devplan-status（competency-management.md §发展计划） ----
    String DEV_PLAN_STATUS_DRAFT = "DRAFT";
    String DEV_PLAN_STATUS_IN_PROGRESS = "IN_PROGRESS";
    String DEV_PLAN_STATUS_COMPLETED = "COMPLETED";
    String DEV_PLAN_STATUS_CANCELLED = "CANCELLED";

    // ---- erp-hr/plan-item-status（competency-management.md §发展计划项） ----
    String PLAN_ITEM_STATUS_NOT_STARTED = "NOT_STARTED";
    String PLAN_ITEM_STATUS_IN_PROGRESS = "IN_PROGRESS";
    String PLAN_ITEM_STATUS_ACHIEVED = "ACHIEVED";
    String PLAN_ITEM_STATUS_OVERDUE = "OVERDUE";

    // ---- 胜任力等级范围（competency-management.md §CompetencyLevel 1-5 量表） ----
    int COMPETENCY_LEVEL_MIN = 1;
    int COMPETENCY_LEVEL_MAX = 5;
}
