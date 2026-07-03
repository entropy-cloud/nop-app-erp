package app.erp.qa.service;

import io.nop.api.core.exceptions.ErrorCode;

/**
 * 质量域错误码。描述为中文，框架经 i18n 翻译；公共/GraphQL 面向错误统一 {@link ErrorCode} + {@code NopException}。
 *
 * <p>权威：{@code docs/design/quality/state-machine.md}、
 * {@code docs/plans/2026-07-02-2237-3-quality-inspection-trigger-ncr-capa.md}。
 */
public interface ErpQaErrors {

    String ARG_INSPECTION_ID = "inspectionId";
    String ARG_INSPECTION_CODE = "inspectionCode";
    String ARG_CURRENT_STATUS = "currentStatus";
    String ARG_EXPECTED_STATUS = "expectedStatus";
    String ARG_NCR_ID = "ncrId";
    String ARG_NCR_CODE = "ncrCode";
    String ARG_ACTION_ID = "actionId";
    String ARG_BILL_TYPE = "billType";
    String ARG_BILL_CODE = "billCode";
    String ARG_MATERIAL_ID = "materialId";

    ErrorCode ERR_INSPECTION_NOT_FOUND = ErrorCode.define(
            "nop.err.qa.inspection.not-found",
            "质检单不存在: {inspectionId}",
            ARG_INSPECTION_ID);

    ErrorCode ERR_INVALID_INSPECTION_STATUS_TRANSITION = ErrorCode.define(
            "nop.err.qa.inspection.illegal-status-transition",
            "质检单[{inspectionCode}]当前结果[{currentStatus}]不允许此操作，期望[{expectedStatus}]",
            ARG_INSPECTION_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_INSPECTION_LINES_EMPTY = ErrorCode.define(
            "nop.err.qa.inspection.lines-empty",
            "质检单[{inspectionCode}]无质检行，无法汇总结果，请先补录检验项",
            ARG_INSPECTION_CODE);

    ErrorCode ERR_INSPECTION_MANDATORY_BLOCKED = ErrorCode.define(
            "nop.err.qa.inspection.mandatory-blocked",
            "业务单据[{billType}:{billCode}]属强制质检类型，关联质检单未得出合格/让步结论，禁止流转",
            ARG_BILL_TYPE, ARG_BILL_CODE);

    ErrorCode ERR_NCR_NOT_FOUND = ErrorCode.define(
            "nop.err.qa.ncr.not-found",
            "不符合项报告不存在: {ncrId}",
            ARG_NCR_ID);

    ErrorCode ERR_INVALID_NCR_STATUS_TRANSITION = ErrorCode.define(
            "nop.err.qa.ncr.illegal-status-transition",
            "NCR[{ncrCode}]当前状态[{currentStatus}]不允许此操作，期望[{expectedStatus}]",
            ARG_NCR_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_NCR_RESOLVE_CAPA_NOT_COMPLETED = ErrorCode.define(
            "nop.err.qa.ncr.resolve-capa-not-completed",
            "NCR[{ncrCode}]存在未完成（含 PENDING/IN_PROGRESS）或未验证（缺验证人/验证日期）的 CAPA 措施，禁止解决",
            ARG_NCR_CODE);

    ErrorCode ERR_ACTION_NOT_FOUND = ErrorCode.define(
            "nop.err.qa.action.not-found",
            "纠正措施不存在: {actionId}",
            ARG_ACTION_ID);

    ErrorCode ERR_INVALID_ACTION_STATUS_TRANSITION = ErrorCode.define(
            "nop.err.qa.action.illegal-status-transition",
            "纠正措施[{actionId}]当前状态[{currentStatus}]不允许此操作，期望[{expectedStatus}]",
            ARG_ACTION_ID, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_ACTION_VERIFY_REQUIRES_COMPLETED = ErrorCode.define(
            "nop.err.qa.action.verify-requires-completed",
            "纠正措施[{actionId}]须先完成（COMPLETED）才能录入效果验证",
            ARG_ACTION_ID);

    ErrorCode ERR_ACTION_VERIFY_MISSING_FIELDS = ErrorCode.define(
            "nop.err.qa.action.verify-missing-fields",
            "纠正措施[{actionId}]效果验证须同时填写验证人与验证日期",
            ARG_ACTION_ID);
}
