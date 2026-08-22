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
    String ARG_SURVEY_ID = "surveyId";
    String ARG_QUESTION_ID = "questionId";

    // --- 胜任力管理作用域参数键 ---
    String ARG_ASSESSMENT_ID = "assessmentId";
    String ARG_COMPETENCY_ID = "competencyId";
    String ARG_DEV_PLAN_ID = "devPlanId";
    String ARG_DEV_PLAN_ITEM_ID = "devPlanItemId";
    String ARG_POSITION_ID = "positionId";
    String ARG_REQUIRED_LEVEL = "requiredLevel";
    String ARG_TARGET_STATUS = "targetStatus";
    String ARG_LEVEL_MAP_KEY = "levelMapKey";
    String ARG_LEVEL_MAP_VALUE = "levelMapValue";

    // --- 报表渲染作用域参数键 ---
    String ARG_REPORT_NAME = "reportName";
    String ARG_RENDER_TYPE = "renderType";

    // --- 员工调动作用域参数键 ---
    String ARG_TARGET_DEPARTMENT_ID = "targetDepartmentId";
    String ARG_TARGET_POSITION_ID = "targetPositionId";
    String ARG_EFFECTIVE_DATE = "effectiveDate";

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

    // --- 薪酬核算：累计数据完整性（payroll.md §4.5，P1-MA4-018） ---
    ErrorCode ERR_HR_CUMULATIVE_DATA_CORRUPT = ErrorCode.define(
            "erp.err.hr.cumulative-data-corrupt",
            "员工 {employeeId} 年度 {year} 累计薪酬数据 JSON 解析失败，请核对 cumulativeData 完整性",
            ARG_EMPLOYEE_ID, ARG_YEAR);

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

    // --- 胜任力管理（competency-management.md §评估流程/§差距分析/§发展计划） ---
    ErrorCode ERR_ASSESSMENT_NO_DETAILS = ErrorCode.define(
            "erp.err.hr.assessment-no-details",
            "评估 {assessmentId} 未包含任何评估明细（AssessmentDetail），禁止提交",
            ARG_ASSESSMENT_ID);
    ErrorCode ERR_ASSESSMENT_ILLEGAL_STATUS_TRANSITION = ErrorCode.define(
            "erp.err.hr.assessment-illegal-status-transition",
            "评估 {assessmentId} 当前状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_ASSESSMENT_ID, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);
    ErrorCode ERR_GAP_NO_ROLE_REQUIREMENT = ErrorCode.define(
            "erp.err.hr.gap-no-role-requirement",
            "员工 {employeeId} 无岗位（ErpHrPosition）或岗位未配置胜任力要求（ErpHrRoleCompetency），无法计算差距",
            ARG_EMPLOYEE_ID);
    ErrorCode ERR_GAP_INVALID_LEVEL_MAP = ErrorCode.define(
            "erp.err.hr.gap-invalid-level-map",
            "差距刷新入参 aggregatedLevels 含非法键/值（key={levelMapKey}, value={levelMapValue}），"
                    + "key 须为 String/Number（competencyId），value 须为 Integer/Number/String-Number",
            ARG_LEVEL_MAP_KEY, ARG_LEVEL_MAP_VALUE);
    ErrorCode ERR_ROLE_COMPETENCY_INVALID_LEVEL = ErrorCode.define(
            "erp.err.hr.role-competency-invalid-level",
            "岗位胜任力要求的等级 {requiredLevel} 超出有效范围（须为 1-5）",
            ARG_REQUIRED_LEVEL);
    ErrorCode ERR_DEV_PLAN_ILLEGAL_STATUS_TRANSITION = ErrorCode.define(
            "erp.err.hr.dev-plan-illegal-status-transition",
            "发展计划项 {devPlanItemId} 当前状态={currentStatus}，不允许执行该操作（期望状态={targetStatus}）",
            ARG_DEV_PLAN_ITEM_ID, ARG_CURRENT_STATUS, ARG_TARGET_STATUS);
    ErrorCode ERR_COMPETENCY_PARENT_CYCLE = ErrorCode.define(
            "erp.err.hr.competency-parent-cycle",
            "胜任力 {competencyId} 设置上级 {parentId} 会形成环路（含自引用），禁止保存",
            ARG_COMPETENCY_ID);

    // --- 员工调动（use-cases.md UC-HR-08）---
    ErrorCode ERR_EMPLOYEE_NOT_TRANSFERABLE = ErrorCode.define(
            "erp.err.hr.employee-not-transferable",
            "员工 {employeeId} 当前雇佣状态={currentStatus}，不可调动（仅 ACTIVE/PROBATION 允许调动）",
            ARG_EMPLOYEE_ID, ARG_CURRENT_STATUS);
    ErrorCode ERR_TRANSFER_TARGET_DEPT_NOT_FOUND = ErrorCode.define(
            "erp.err.hr.transfer-target-dept-not-found",
            "调动目标部门 {targetDepartmentId} 不存在",
            ARG_TARGET_DEPARTMENT_ID);
    ErrorCode ERR_TRANSFER_TARGET_POSITION_NOT_FOUND = ErrorCode.define(
            "erp.err.hr.transfer-target-position-not-found",
            "调动目标职位 {targetPositionId} 不存在或不归属目标部门 {targetDepartmentId}",
            ARG_TARGET_POSITION_ID, ARG_TARGET_DEPARTMENT_ID);

    // --- 休假审批引擎（use-cases.md UC-HR-02）---
    String ARG_LEAVE_TYPE = "leaveType";
    String ARG_FISCAL_YEAR = "fiscalYear";
    String ARG_ENTITLED_DAYS = "entitledDays";
    String ARG_USED_DAYS = "usedDays";
    String ARG_REQUEST_DAYS = "requestDays";
    ErrorCode ERR_LEAVE_BALANCE_INSUFFICIENT = ErrorCode.define(
            "erp.err.hr.leave-balance-insufficient",
            "员工 {employeeId} 休假类型 {leaveType} 福利年度 {fiscalYear} 余额不足（应休 {entitledDays} 天/已用 {usedDays} 天/申请 {requestDays} 天）",
            ARG_EMPLOYEE_ID, ARG_LEAVE_TYPE, ARG_FISCAL_YEAR, ARG_ENTITLED_DAYS, ARG_USED_DAYS, ARG_REQUEST_DAYS);
    ErrorCode ERR_LEAVE_DATE_OVERLAP = ErrorCode.define(
            "erp.err.hr.leave-date-overlap",
            "员工 {employeeId} 休假日期与已有 APPROVED/SUBMITTED 休假区间重叠",
            ARG_EMPLOYEE_ID);
    ErrorCode ERR_LEAVE_ILLEGAL_STATUS_TRANSITION = ErrorCode.define(
            "erp.err.hr.leave-illegal-status-transition",
            "休假申请 {leaveRequestId} 当前状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_LEAVE_REQUEST_ID, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    // --- 考勤打卡（use-cases.md UC-HR-06）---
    ErrorCode ERR_ALREADY_CLOCKED_IN = ErrorCode.define(
            "erp.err.hr.already-clocked-in",
            "员工 {employeeId} 当日已签到，禁止重复签到",
            ARG_EMPLOYEE_ID);
    ErrorCode ERR_NOT_CLOCKED_IN = ErrorCode.define(
            "erp.err.hr.not-clocked-in",
            "员工 {employeeId} 当日尚未签到，无法签退",
            ARG_EMPLOYEE_ID);

    // --- 手工补卡（use-cases.md UC-HR-06⑮，RC-R1.7 / P1-RC-014）---
    String ARG_ATTENDANCE_DATE = "date";
    ErrorCode ERR_MAKEUP_REASON_REQUIRED = ErrorCode.define(
            "erp.err.hr.makeup-reason-required",
            "手工补卡必须填写补卡原因（reason），员工 {employeeId} 补卡日期 {date}",
            ARG_EMPLOYEE_ID, ARG_ATTENDANCE_DATE);
    ErrorCode ERR_MAKEUP_ROLE_REQUIRED = ErrorCode.define(
            "erp.err.hr.makeup-role-required",
            "仅 HR 专员角色可执行手工补卡（当前用户缺少角色 " + "HR 专员" + "）");

    // 并发重复排班被 UK_HR_SHIFT_ASSIGNMENT_NATURAL 兜底拦截（plan 2026-07-30-0841-2 R1.28 P1-MA2-091）
    ErrorCode ERR_HR_SHIFT_ASSIGNMENT_DUPLICATE = ErrorCode.define(
            "erp.err.hr.shift-assignment-duplicate",
            "员工 {employeeId} 日期 {assignmentDate} 班次 {shiftId} 排班已由并发事务创建，不可重复排班",
            ARG_EMPLOYEE_ID, ARG_ASSIGNMENT_DATE, ARG_SHIFT_ID);

    // --- 员工调研（use-cases.md UC-HR-11，RC-R1.9 P1-MA2-041 + P1-RC-016） ---
    ErrorCode ERR_HR_SURVEY_ILLEGAL_TRANSITION = ErrorCode.define(
            "erp.err.hr.survey-illegal-transition",
            "问卷 {surveyId} 当前状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_SURVEY_ID, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);
    ErrorCode ERR_HR_SURVEY_PUBLISHED_IMMUTABLE = ErrorCode.define(
            "erp.err.hr.survey-published-immutable",
            "问卷 {surveyId} 已发布（当前状态={currentStatus}），问卷配置字段与题目不可再修改，如需调整请新建版本",
            ARG_SURVEY_ID, ARG_CURRENT_STATUS);
    ErrorCode ERR_HR_SURVEY_NOT_OPEN = ErrorCode.define(
            "erp.err.hr.survey-not-open",
            "问卷 {surveyId} 当前状态={currentStatus}，非 OPEN 状态不可提交答卷",
            ARG_SURVEY_ID, ARG_CURRENT_STATUS);
    ErrorCode ERR_HR_SURVEY_ALREADY_SUBMITTED = ErrorCode.define(
            "erp.err.hr.survey-already-submitted",
            "问卷 {surveyId} 已存在该员工的答卷，禁止重复提交",
            ARG_SURVEY_ID);
    ErrorCode ERR_HR_SURVEY_INVALID_QUESTION = ErrorCode.define(
            "erp.err.hr.survey-invalid-question",
            "问卷 {surveyId} 不含题目 {questionId}，答卷数据非法",
            ARG_SURVEY_ID, ARG_QUESTION_ID);

    // --- 招聘状态机（use-cases.md UC-HR-05）---
    String ARG_RECRUITMENT_ID = "recruitmentId";
    ErrorCode ERR_RECRUITMENT_ILLEGAL_STATUS_TRANSITION = ErrorCode.define(
            "erp.err.hr.recruitment-illegal-status-transition",
            "招聘记录 {recruitmentId} 当前状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_RECRUITMENT_ID, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);
    ErrorCode ERR_RECRUITMENT_EMPLOYEE_CREATE_FAILED = ErrorCode.define(
            "erp.err.hr.recruitment-employee-create-failed",
            "招聘记录 {recruitmentId} 入职联动创建员工失败",
            ARG_RECRUITMENT_ID);

    // --- 合同到期（use-cases.md UC-HR-07）---
    String ARG_CONTRACT_ID = "contractId";
    ErrorCode ERR_CONTRACT_ILLEGAL_STATUS_TRANSITION = ErrorCode.define(
            "erp.err.hr.contract-illegal-status-transition",
            "合同 {contractId} 当前状态={currentStatus}，不允许续签（仅 ACTIVE/EXPIRED 允许）",
            ARG_CONTRACT_ID, ARG_CURRENT_STATUS);

    String ARG_TIMESHEET_ID = "timesheetId";
    ErrorCode ERR_HR_TIMESHEET_ILLEGAL_TRANSITION = ErrorCode.define(
            "erp.err.hr.timesheet-illegal-transition",
            "工时表[{timesheetId}]当前状态[{currentStatus}]不允许此操作",
            ARG_TIMESHEET_ID, ARG_CURRENT_STATUS);

    // --- 工时表（use-cases.md UC-HR-03，RC-R1.8 P1-RC-015/P1-MA2-043） ---
    String ARG_WORK_DATE = "workDate";
    String ARG_TOTAL_HOURS = "totalHours";
    ErrorCode ERR_TIMESHEET_DAILY_HOURS_EXCEEDED = ErrorCode.define(
            "erp.err.hr.timesheet-daily-hours-exceeded",
            "员工 {employeeId} 在 {workDate} 的工时合计 {totalHours} 小时超过每日上限 24 小时",
            ARG_EMPLOYEE_ID, ARG_WORK_DATE, ARG_TOTAL_HOURS);
    ErrorCode ERR_TIMESHEET_REJECT_REASON_REQUIRED = ErrorCode.define(
            "erp.err.hr.timesheet-reject-reason-required",
            "驳回工时表[{timesheetId}]必须填写驳回原因（reason）",
            ARG_TIMESHEET_ID);
}
