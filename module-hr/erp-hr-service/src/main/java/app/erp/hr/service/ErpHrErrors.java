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
    String ARG_EMPLOYEE_IDS = "employeeIds";
    String ARG_SHIFT_ID = "shiftId";
    String ARG_ASSIGNMENT_DATE = "assignmentDate";
    String ARG_PATTERN_ID = "patternId";
    String ARG_SWAP_REQUEST_ID = "swapRequestId";
    String ARG_LEAVE_REQUEST_ID = "leaveRequestId";
    String ARG_SIMULATION_ID = "simulationId";
    String ARG_SALARY_ITEM_CODE = "salaryItemCode";
    String ARG_SOURCE_PERIOD = "sourcePeriod";
    String ARG_TARGET_PERIOD = "targetPeriod";
    String ARG_REVIEWER_ID = "reviewerId";

    // --- 报表渲染作用域参数键 ---
    String ARG_REPORT_NAME = "reportName";
    String ARG_RENDER_TYPE = "renderType";

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

    // --- 排班分配/轮换/调换（shift-scheduling.md） ---
    ErrorCode ERR_SHIFT_DUPLICATE_ASSIGNMENT = ErrorCode.define(
            "erp.err.hr.shift-duplicate-assignment",
            "员工 {employeeId} 在 {assignmentDate} 已存在排班，违反一人一天一排班唯一约束",
            ARG_EMPLOYEE_ID, ARG_ASSIGNMENT_DATE);
    ErrorCode ERR_SHIFT_CROSS_DAY_INVALID = ErrorCode.define(
            "erp.err.hr.shift-cross-day-invalid",
            "班次 {shiftId} 跨天配置非法：endTime 须小于 startTime 才视为夜班跨天",
            ARG_SHIFT_ID);
    ErrorCode ERR_SHIFT_SWAP_TARGET_OCCUPIED = ErrorCode.define(
            "erp.err.hr.shift-swap-target-occupied",
            "调换目标员工在 {assignmentDate} 无有效排班或已被其他调换占用",
            ARG_ASSIGNMENT_DATE);
    ErrorCode ERR_SHIFT_ROTATION_PATTERN_INVALID = ErrorCode.define(
            "erp.err.hr.shift-rotation-pattern-invalid",
            "轮换模板 {patternId} 的 patternData 非法或包含不存在的班次编码",
            ARG_PATTERN_ID);
    ErrorCode ERR_SHIFT_ASSIGNMENT_NOT_SWAPPABLE = ErrorCode.define(
            "erp.err.hr.shift-assignment-not-swappable",
            "调换申请 {swapRequestId} 的排班不可调换（当前状态不允许或已被其他调换引用）",
            ARG_SWAP_REQUEST_ID);
    ErrorCode ERR_SHIFT_SWAP_ILLEGAL_STATUS_TRANSITION = ErrorCode.define(
            "erp.err.hr.shift-swap-illegal-status-transition",
            "调换申请 {swapRequestId} 当前状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_SWAP_REQUEST_ID, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);
    ErrorCode ERR_LEAVE_REQUEST_NOT_FOUND = ErrorCode.define(
            "erp.err.hr.leave-request-not-found",
            "休假申请 {leaveRequestId} 不存在",
            ARG_LEAVE_REQUEST_ID);

    // --- 薪酬模拟（payroll-simulation.md §1.2/§4.2） ---
    ErrorCode ERR_HR_SIMULATION_ILLEGAL_TRANSITION = ErrorCode.define(
            "erp.err.hr.simulation.illegal-status-transition",
            "薪酬模拟 {simulationId} 当前状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_SIMULATION_ID, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);
    ErrorCode ERR_HR_SIMULATION_NO_ADJUSTMENT = ErrorCode.define(
            "erp.err.hr.simulation.no-adjustment",
            "薪酬模拟 {simulationId} 未记录任何调整项，禁止提交审核",
            ARG_SIMULATION_ID);
    ErrorCode ERR_HR_SIMULATION_TARGET_PERIOD_CONFLICT = ErrorCode.define(
            "erp.err.hr.simulation.target-period-conflict",
            "目标期间 {targetPeriod} 已存在 PAID 正式薪酬，禁止转正式（先作废冲突薪酬）",
            ARG_TARGET_PERIOD);
    ErrorCode ERR_HR_SIMULATION_EMPLOYEE_DUPLICATE = ErrorCode.define(
            "erp.err.hr.simulation.employee-duplicate",
            "员工 {employeeId} 在目标期间 {targetPeriod} 已存在正式薪酬，禁止重复转正式",
            ARG_EMPLOYEE_ID, ARG_TARGET_PERIOD);
    ErrorCode ERR_HR_SIMULATION_SOURCE_NOT_FOUND = ErrorCode.define(
            "erp.err.hr.simulation.source-not-found",
            "源期间 {sourcePeriod} 未找到任何正式薪酬记录，无法创建模拟",
            ARG_SOURCE_PERIOD);

    // --- 报表渲染作用域（镜像 ErpMfgErrors.ERR_REPORT_*，HR 域独立错误码，不跨域 import） ---

    ErrorCode ERR_REPORT_NAME_INVALID = ErrorCode.define(
            "erp.err.hr.report.name-invalid",
            "报表名[{reportName}]非法（含路径注入字符或不合规段），拒绝渲染",
            ARG_REPORT_NAME);

    ErrorCode ERR_REPORT_RENDER_TYPE_INVALID = ErrorCode.define(
            "erp.err.hr.report.render-type-invalid",
            "渲染类型[{renderType}]非法（仅允许 html/xlsx/pdf）",
            ARG_RENDER_TYPE);
}
