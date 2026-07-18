package app.erp.mnt.service;

import io.nop.api.core.exceptions.ErrorCode;

/**
 * 维护域业务异常错误码。所有维护流程中的业务异常使用 {@link io.nop.api.core.exceptions.NopException} + 本接口的 {@link ErrorCode}。
 */
public interface ErpMntErrors {

    String ARG_VISIT_ID = "visitId";
    String ARG_VISIT_CODE = "visitCode";
    String ARG_REQUEST_ID = "requestId";
    String ARG_REQUEST_CODE = "requestCode";
    String ARG_SCHEDULE_ID = "scheduleId";
    String ARG_SCHEDULE_CODE = "scheduleCode";
    String ARG_DOWNTIME_ID = "downtimeId";
    String ARG_USAGE_ID = "usageId";
    String ARG_USAGE_CODE = "usageCode";
    String ARG_EQUIPMENT_ID = "equipmentId";
    String ARG_CURRENT_STATUS = "currentStatus";
    String ARG_EXPECTED_STATUS = "expectedStatus";
    String ARG_ASSIGNED_TO = "assignedTo";
    String ARG_VISIT_DATE = "visitDate";
    String ARG_CONFLICT_VISIT_CODE = "conflictVisitCode";

    // --- 报表渲染作用域参数键 ---
    String ARG_REPORT_NAME = "reportName";
    String ARG_RENDER_TYPE = "renderType";

    ErrorCode ERR_VISIT_NOT_FOUND = ErrorCode.define("erp.err.mnt.visit-not-found",
            "维护访问 {visitId} 不存在", ARG_VISIT_ID);

    ErrorCode ERR_INVALID_VISIT_STATUS_TRANSITION = ErrorCode.define("erp.err.mnt.visit-illegal-status-transition",
            "维护访问 {visitCode} 当前状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_VISIT_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_VISIT_ASSIGNED_TO_REQUIRED = ErrorCode.define("erp.err.mnt.visit-assigned-to-required",
            "维护访问 {visitCode} 排程前须分配执行人（assignedTo）",
            ARG_VISIT_CODE, ARG_ASSIGNED_TO);

    ErrorCode ERR_VISIT_DATE_REQUIRED = ErrorCode.define("erp.err.mnt.visit-date-required",
            "维护访问 {visitCode} 排程前须确定计划执行日期（visitDate）",
            ARG_VISIT_CODE, ARG_VISIT_DATE);

    ErrorCode ERR_VISIT_SCHEDULE_CONFLICT = ErrorCode.define("erp.err.mnt.visit-schedule-conflict",
            "维护访问 {visitCode} 排程冲突：设备 {equipmentId} 在该日期已有排程/执行中访问 {conflictVisitCode}",
            ARG_VISIT_CODE, ARG_EQUIPMENT_ID, ARG_CONFLICT_VISIT_CODE);

    ErrorCode ERR_REQUEST_NOT_FOUND = ErrorCode.define("erp.err.mnt.request-not-found",
            "维护请求 {requestId} 不存在", ARG_REQUEST_ID);

    ErrorCode ERR_INVALID_REQUEST_STATUS_TRANSITION = ErrorCode.define("erp.err.mnt.request-illegal-status-transition",
            "维护请求 {requestCode} 当前状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_REQUEST_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_SCHEDULE_NOT_FOUND = ErrorCode.define("erp.err.mnt.schedule-not-found",
            "维护计划 {scheduleId} 不存在", ARG_SCHEDULE_ID);

    ErrorCode ERR_DOWNTIME_NOT_FOUND = ErrorCode.define("erp.err.mnt.downtime-not-found",
            "停机记录 {downtimeId} 不存在", ARG_DOWNTIME_ID);

    ErrorCode ERR_DOWNTIME_ALREADY_COMPLETED = ErrorCode.define("erp.err.mnt.downtime-already-completed",
            "停机记录 {downtimeId} 已结束，不可再次记录/结束", ARG_DOWNTIME_ID);

    ErrorCode ERR_DOWNTIME_NOT_STARTED = ErrorCode.define("erp.err.mnt.downtime-not-started",
            "停机记录 {downtimeId} 尚未开始（无 startTime），不可结束", ARG_DOWNTIME_ID);

    ErrorCode ERR_USAGE_NOT_FOUND = ErrorCode.define("erp.err.mnt.usage-not-found",
            "备件消耗单 {usageId} 不存在", ARG_USAGE_ID);

    ErrorCode ERR_USAGE_LINES_EMPTY = ErrorCode.define("erp.err.mnt.usage-lines-empty",
            "备件消耗单 {usageCode} 无行明细，不可确认出库",
            ARG_USAGE_CODE);

    ErrorCode ERR_SPARE_PART_POSTING_FAILED = ErrorCode.define("erp.err.mnt.spare-part-posting-failed",
            "备件消耗单 {usageCode} 库存已出库，但 GL 过账失败（posted 保持库存出库语义，凭证由人工或兜底补登）",
            ARG_USAGE_CODE);

    // --- 维修工时费用化 GL 过账（plan 2026-07-18-0949-1）---
    ErrorCode ERR_VISIT_TOTAL_MINUTES_MISSING = ErrorCode.define("erp.err.mnt.visit-total-minutes-missing",
            "维护访问 {visitCode} 无有效工时（totalMinutes 缺失或 ≤0），不可工时费用化过账",
            ARG_VISIT_CODE);

    ErrorCode ERR_LABOR_RATE_NOT_CONFIGURED = ErrorCode.define("erp.err.mnt.labor-rate-not-configured",
            "维护访问 {visitCode} 工时费率未配置（erp-mnt.default-labor-hourly-rate ≤0），不可工时费用化过账",
            ARG_VISIT_CODE);

    ErrorCode ERR_EQUIPMENT_NOT_FOUND = ErrorCode.define("erp.err.mnt.equipment-not-found",
            "设备 {equipmentId} 不存在", ARG_EQUIPMENT_ID);

    // --- 报表渲染作用域（镜像 ErpMfgErrors.ERR_REPORT_*，维护域独立错误码，不跨域 import） ---

    ErrorCode ERR_REPORT_NAME_INVALID = ErrorCode.define(
            "erp.err.mnt.report.name-invalid",
            "报表名[{reportName}]非法（含路径注入字符或不合规段），拒绝渲染",
            ARG_REPORT_NAME);

    ErrorCode ERR_REPORT_RENDER_TYPE_INVALID = ErrorCode.define(
            "erp.err.mnt.report.render-type-invalid",
            "渲染类型[{renderType}]非法（仅允许 html/xlsx/pdf）",
            ARG_RENDER_TYPE);
}
