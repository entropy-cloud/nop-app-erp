package app.erp.md.service;

import io.nop.api.core.exceptions.ErrorCode;

/**
 * 主数据域业务异常错误码。所有主数据流程中的业务异常使用 {@link io.nop.api.core.exceptions.NopException} + 本接口的 {@link ErrorCode}。
 */
public interface ErpMdErrors {

    String ARG_APPROVAL_ID = "approvalId";
    String ARG_PARTNER_ID = "partnerId";
    String ARG_CURRENT_STATUS = "currentStatus";
    String ARG_EXPECTED_STATUS = "expectedStatus";

    // --- 报表渲染作用域参数键 ---
    String ARG_REPORT_NAME = "reportName";
    String ARG_RENDER_TYPE = "renderType";

    // --- AVL 准入状态机错误码（docs/design/purchase/supplier-evaluation.md §状态机） ---

    ErrorCode ERR_APPROVAL_NOT_FOUND = ErrorCode.define("erp.err.md.approval-not-found",
            "供应商准入资格 {approvalId} 不存在", ARG_APPROVAL_ID);

    ErrorCode ERR_INVALID_APPROVAL_STATUS_TRANSITION = ErrorCode.define("erp.err.md.invalid-approval-status-transition",
            "供应商准入资格 {approvalId} 当前状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_APPROVAL_ID, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_APPROVAL_QUALIFICATION_MISSING = ErrorCode.define("erp.err.md.approval-qualification-missing",
            "供应商准入资格 {approvalId} 缺少资质文件或有效期，不可批准",
            ARG_APPROVAL_ID);

    // --- 报表渲染作用域（镜像 ErpMfgErrors.ERR_REPORT_*，主数据域独立错误码，不跨域 import） ---

    ErrorCode ERR_REPORT_NAME_INVALID = ErrorCode.define(
            "erp.err.md.report.name-invalid",
            "报表名[{reportName}]非法（含路径注入字符或不合规段），拒绝渲染",
            ARG_REPORT_NAME);

    ErrorCode ERR_REPORT_RENDER_TYPE_INVALID = ErrorCode.define(
            "erp.err.md.report.render-type-invalid",
            "渲染类型[{renderType}]非法（仅允许 html/xlsx/pdf）",
            ARG_RENDER_TYPE);
}
