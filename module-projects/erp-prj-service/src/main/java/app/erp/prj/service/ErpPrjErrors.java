package app.erp.prj.service;

import io.nop.api.core.exceptions.ErrorCode;

/**
 * 项目域业务错误码。工时/预算/归集/项目状态流程中的业务异常使用
 * {@link io.nop.api.core.exceptions.NopException} + 本接口的 {@link ErrorCode}。
 * 描述用中文，框架经 i18n 翻译。
 */
public interface ErpPrjErrors {

    // --- 作用域参数键 ---
    String ARG_TIMESHEET_CODE = "timesheetCode";
    String ARG_TIMESHEET_ID = "timesheetId";
    String ARG_PROJECT_CODE = "projectCode";
    String ARG_PROJECT_ID = "projectId";
    String ARG_TASK_ID = "taskId";
    String ARG_CURRENT_STATUS = "currentStatus";
    String ARG_EXPECTED_STATUS = "expectedStatus";
    String ARG_AMOUNT = "amount";
    String ARG_BUDGET_TOTAL = "budgetTotal";
    String ARG_BUDGET_USED = "budgetUsed";
    String ARG_COST_RATE = "costRate";
    String ARG_ACTIVITY_TYPE_ID = "activityTypeId";
    String ARG_SUBJECT_CODE = "subjectCode";
    String ARG_SOURCE_BILL_CODE = "sourceBillCode";

    // --- 工时状态机 ---
    ErrorCode ERR_TIMESHEET_ILLEGAL_STATUS_TRANSITION = ErrorCode.define(
            "erp.err.prj.timesheet.illegal-status-transition",
            "工时单 {timesheetCode} 当前状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_TIMESHEET_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);
    ErrorCode ERR_TIMESHEET_PROJECT_NOT_OPEN = ErrorCode.define(
            "erp.err.prj.timesheet.project-not-open",
            "工时单 {timesheetCode} 引用项目 {projectId} 当前非 OPEN 状态，不允许提交工时",
            ARG_TIMESHEET_CODE, ARG_PROJECT_ID);
    ErrorCode ERR_TIMESHEET_TASK_NOT_ALLOWED = ErrorCode.define(
            "erp.err.prj.timesheet.task-not-allowed",
            "工时单 {timesheetCode} 引用任务 {taskId} 当前状态不允许录入工时（须为待开始/进行中）",
            ARG_TIMESHEET_CODE, ARG_TASK_ID);

    // --- 成本率解析 ---
    ErrorCode ERR_COST_RATE_NOT_AVAILABLE = ErrorCode.define(
            "erp.err.prj.timesheet.cost-rate-not-available",
            "工时单 {timesheetCode} 无法解析成本率：工时未填写 costRate、活动类型 {activityTypeId} 未配置 costRate，且未配置全局默认 erp-prj.default-labor-cost-rate",
            ARG_TIMESHEET_CODE, ARG_ACTIVITY_TYPE_ID);

    // --- 过账科目 ---
    ErrorCode ERR_PAYROLL_SUBJECT_NOT_CONFIGURED = ErrorCode.define(
            "erp.err.prj.payroll-subject-not-configured",
            "应付职工薪酬贷方科目未配置（配置键 erp-prj.default-payroll-subject-id），无法生成工时成本凭证",
            ARG_SUBJECT_CODE);
    ErrorCode ERR_PROJECT_DEBIT_SUBJECT_NOT_RESOLVED = ErrorCode.define(
            "erp.err.prj.project-debit-subject-not-resolved",
            "项目 {projectId} 的项目类型未配置默认成本科目（defaultSubjectId），无法解析工时成本借方科目",
            ARG_PROJECT_ID);

    // --- 预算控制 ---
    ErrorCode ERR_BUDGET_EXCEEDED = ErrorCode.define(
            "erp.err.prj.budget-exceeded",
            "项目 {projectId} 预算超限：总预算={budgetTotal}，已使用={budgetUsed}，拟新增={amount}（控制模式 STRICT）",
            ARG_PROJECT_ID, ARG_BUDGET_TOTAL, ARG_BUDGET_USED, ARG_AMOUNT);

    // --- 项目状态引用校验 ---
    ErrorCode ERR_PROJECT_NOT_REFERENCEABLE = ErrorCode.define(
            "erp.err.prj.project-not-referenceable",
            "项目 {projectId} 当前状态={currentStatus}，不可被新单据引用（须为 OPEN）",
            ARG_PROJECT_ID, ARG_CURRENT_STATUS);
    ErrorCode ERR_PROJECT_NOT_CLOSABLE = ErrorCode.define(
            "erp.err.prj.project-not-closable",
            "项目 {projectId} 当前状态={currentStatus}，不允许关闭（须为 OPEN）",
            ARG_PROJECT_ID, ARG_CURRENT_STATUS);
}
