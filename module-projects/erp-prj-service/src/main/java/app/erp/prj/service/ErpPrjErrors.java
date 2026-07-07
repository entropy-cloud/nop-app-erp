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

    // --- 损益汇总/结算作用域参数键 ---
    String ARG_PNL_CODE = "pnlCode";
    String ARG_SETTLEMENT_CODE = "settlementCode";
    String ARG_SETTLEMENT_ID = "settlementId";
    String ARG_PERIOD_FROM = "periodFrom";
    String ARG_PERIOD_TO = "periodTo";
    String ARG_SETTLEMENT_TYPE = "settlementType";
    String ARG_ASSET_CARD_CODE = "assetCardCode";

    // --- 报表渲染作用域参数键 ---
    String ARG_REPORT_NAME = "reportName";
    String ARG_RENDER_TYPE = "renderType";

    // --- 任务依赖/状态机作用域参数键（task-dag.md §7） ---
    String ARG_DEPENDS_ON_TASK_ID = "dependsOnTaskId";
    String ARG_DEPENDS_ON_TASK_STATUS = "dependsOnTaskStatus";
    String ARG_CHAIN = "chain";
    String ARG_MAX_DEPTH = "maxDepth";
    String ARG_ACTUAL_DEPTH = "actualDepth";
    String ARG_TARGET_STATUS = "targetStatus";
    String ARG_BLOCK_REASON = "blockReason";
    String ARG_TASK_PROJECT_ID = "taskProjectId";
    String ARG_DEPENDS_ON_PROJECT_ID = "dependsOnProjectId";

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

    // --- 报表渲染作用域（镜像 ErpMfgErrors.ERR_REPORT_*，项目域独立错误码，不跨域 import） ---

    ErrorCode ERR_REPORT_NAME_INVALID = ErrorCode.define(
            "erp.err.prj.report.name-invalid",
            "报表名[{reportName}]非法（含路径注入字符或不合规段），拒绝渲染",
            ARG_REPORT_NAME);

    ErrorCode ERR_REPORT_RENDER_TYPE_INVALID = ErrorCode.define(
            "erp.err.prj.report.render-type-invalid",
            "渲染类型[{renderType}]非法（仅允许 html/xlsx/pdf）",
            ARG_RENDER_TYPE);

    // --- 损益汇总（plan 2026-07-07-0305-1 Phase 2） ---
    ErrorCode ERR_PRJ_PNL_PERIOD_INVALID = ErrorCode.define(
            "erp.err.prj.pnl.period-invalid",
            "损益汇总期间非法：periodFrom={periodFrom} 晚于 periodTo={periodTo}",
            ARG_PERIOD_FROM, ARG_PERIOD_TO);
    ErrorCode ERR_PRJ_PNL_RECALC_FROZEN = ErrorCode.define(
            "erp.err.prj.pnl.recalc-frozen",
            "损益汇总 {pnlCode} 已过账（posted=true），不允许重算",
            ARG_PNL_CODE);

    // --- 结算单状态机（plan 2026-07-07-0305-1 Phase 3） ---
    ErrorCode ERR_SETTLEMENT_ILLEGAL_STATUS_TRANSITION = ErrorCode.define(
            "erp.err.prj.settlement.illegal-status-transition",
            "结算单 {settlementCode} 当前状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_SETTLEMENT_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);
    ErrorCode ERR_SETTLEMENT_PNL_SNAPSHOT_MISSING = ErrorCode.define(
            "erp.err.prj.settlement.pnl-snapshot-missing",
            "项目 {projectId} 无已计算的损益汇总快照，无法创建结算单",
            ARG_PROJECT_ID);
    ErrorCode ERR_SETTLEMENT_CAPITALIZATION_FAILED = ErrorCode.define(
            "erp.err.prj.settlement.capitalization-failed",
            "结算单 {settlementCode} 转固失败：资产卡片创建异常",
            ARG_SETTLEMENT_CODE);
    ErrorCode ERR_SETTLEMENT_SETTLEMENT_TYPE_NOT_CLOSE = ErrorCode.define(
            "erp.err.prj.settlement.type-not-close",
            "结算单 {settlementCode} 类型={settlementType}，非 CLOSE 不允许转固",
            ARG_SETTLEMENT_CODE, ARG_SETTLEMENT_TYPE);

    // --- 任务依赖与状态机（plan 2026-07-07-0930-3 / task-dag.md §7） ---
    ErrorCode ERR_TASK_SELF_DEPENDENCY = ErrorCode.define(
            "erp.err.prj.task.self-dependency",
            "任务 {taskId} 不允许自依赖（dependsOnId 指向自身）",
            ARG_TASK_ID);
    ErrorCode ERR_TASK_DEPENDENCY_CYCLE = ErrorCode.define(
            "erp.err.prj.task.dependency-cycle",
            "任务 {taskId} 的依赖链存在环路：{chain}",
            ARG_TASK_ID, ARG_CHAIN);
    ErrorCode ERR_TASK_DEPENDENCY_DEPTH_EXCEEDED = ErrorCode.define(
            "erp.err.prj.task.dependency-depth-exceeded",
            "任务 {taskId} 的依赖链深度 {actualDepth} 超过上限 {maxDepth}（疑似恶意长链或数据异常）",
            ARG_TASK_ID, ARG_MAX_DEPTH, ARG_ACTUAL_DEPTH);
    ErrorCode ERR_TASK_DEPENDENCY_CROSS_PROJECT = ErrorCode.define(
            "erp.err.prj.task.dependency-cross-project",
            "任务 {taskId}（项目 {taskProjectId}）不允许依赖跨项目任务 {dependsOnTaskId}（项目 {dependsOnProjectId}）",
            ARG_TASK_ID, ARG_TASK_PROJECT_ID, ARG_DEPENDS_ON_TASK_ID, ARG_DEPENDS_ON_PROJECT_ID);
    ErrorCode ERR_TASK_PREDECESSOR_NOT_DONE = ErrorCode.define(
            "erp.err.prj.task.predecessor-not-done",
            "任务 {taskId} 的前置任务 {dependsOnTaskId} 当前状态={dependsOnTaskStatus}，未完成不可启动",
            ARG_TASK_ID, ARG_DEPENDS_ON_TASK_ID, ARG_DEPENDS_ON_TASK_STATUS);
    ErrorCode ERR_TASK_ILLEGAL_STATUS_TRANSITION = ErrorCode.define(
            "erp.err.prj.task.illegal-status-transition",
            "任务 {taskId} 当前状态={currentStatus}，不允许迁移至 {targetStatus}",
            ARG_TASK_ID, ARG_CURRENT_STATUS, ARG_TARGET_STATUS);
    ErrorCode ERR_TASK_BLOCK_REASON_REQUIRED = ErrorCode.define(
            "erp.err.prj.task.block-reason-required",
            "任务 {taskId} 阻塞须填写 blockReason",
            ARG_TASK_ID);
}
