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

    // --- AVL 准入状态机错误码（docs/design/purchase/supplier-evaluation.md §状态机） ---

    ErrorCode ERR_APPROVAL_NOT_FOUND = ErrorCode.define("erp.err.md.approval-not-found",
            "供应商准入资格 {approvalId} 不存在", ARG_APPROVAL_ID);

    ErrorCode ERR_INVALID_APPROVAL_STATUS_TRANSITION = ErrorCode.define("erp.err.md.invalid-approval-status-transition",
            "供应商准入资格 {approvalId} 当前状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_APPROVAL_ID, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_APPROVAL_QUALIFICATION_MISSING = ErrorCode.define("erp.err.md.approval-qualification-missing",
            "供应商准入资格 {approvalId} 缺少资质文件或有效期，不可批准",
            ARG_APPROVAL_ID);
}
