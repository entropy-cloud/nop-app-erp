package app.erp.ast.service;

import io.nop.api.core.exceptions.ErrorCode;

/**
 * 资产域业务错误码。资本化/折旧/处置流程中的业务异常使用
 * {@link io.nop.api.core.exceptions.NopException} + 本接口的 {@link ErrorCode}。
 * 描述用中文，框架经 i18n 翻译。
 */
public interface ErpAstErrors {

    // --- 作用域参数键 ---
    String ARG_CAPITALIZATION_CODE = "capitalizationCode";
    String ARG_CAPITALIZATION_ID = "capitalizationId";
    String ARG_DISPOSAL_CODE = "disposalCode";
    String ARG_DISPOSAL_ID = "disposalId";
    String ARG_ASSET_CODE = "assetCode";
    String ARG_ASSET_ID = "assetId";
    String ARG_CATEGORY_ID = "categoryId";
    String ARG_PERIOD = "period";
    String ARG_CURRENT_STATUS = "currentStatus";
    String ARG_EXPECTED_STATUS = "expectedStatus";
    String ARG_CURRENT_DOC_STATUS = "currentDocStatus";
    String ARG_EXPECTED_DOC_STATUS = "expectedDocStatus";
    String ARG_DEPRECIATION_METHOD = "depreciationMethod";
    String ARG_AMOUNT = "amount";

    // --- 资本化 ---
    ErrorCode ERR_CAPITALIZATION_NOT_FOUND = ErrorCode.define(
            "erp.err.ast.capitalization.not-found",
            "资本化单 {capitalizationId} 不存在",
            ARG_CAPITALIZATION_ID);
    ErrorCode ERR_CAPITALIZATION_ILLEGAL_STATUS_TRANSITION = ErrorCode.define(
            "erp.err.ast.capitalization.illegal-status-transition",
            "资本化单 {capitalizationCode} 当前审核状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_CAPITALIZATION_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);
    ErrorCode ERR_CAPITALIZATION_ILLEGAL_DOC_TRANSITION = ErrorCode.define(
            "erp.err.ast.capitalization.illegal-doc-transition",
            "资本化单 {capitalizationCode} 当前单据状态={currentDocStatus}，不允许执行该操作（期望状态={expectedDocStatus}）",
            ARG_CAPITALIZATION_CODE, ARG_CURRENT_DOC_STATUS, ARG_EXPECTED_DOC_STATUS);
    ErrorCode ERR_CAPITALIZATION_CATEGORY_MISSING = ErrorCode.define(
            "erp.err.ast.capitalization.category-missing",
            "资本化单 {capitalizationCode} 未指定资产类别",
            ARG_CAPITALIZATION_CODE);
    ErrorCode ERR_CAPITALIZATION_ORIGINAL_VALUE_INVALID = ErrorCode.define(
            "erp.err.ast.capitalization.original-value-invalid",
            "资本化单 {capitalizationCode} 原值无效",
            ARG_CAPITALIZATION_CODE, ARG_AMOUNT);
    ErrorCode ERR_CAPITALIZATION_USEFUL_LIFE_MISSING = ErrorCode.define(
            "erp.err.ast.capitalization.useful-life-missing",
            "资本化单 {capitalizationCode} 对应资产类别未配置使用年限或折旧方法，无法生成折旧计划",
            ARG_CAPITALIZATION_CODE);

    // --- 折旧 ---
    ErrorCode ERR_ASSET_NOT_FOUND = ErrorCode.define(
            "erp.err.ast.asset.not-found",
            "资产 {assetId} 不存在",
            ARG_ASSET_ID);
    ErrorCode ERR_DEPRECIATION_PERIOD_NOT_FOUND = ErrorCode.define(
            "erp.err.ast.depreciation.period-not-found",
            "折旧期间 {period} 未找到",
            ARG_PERIOD);
    ErrorCode ERR_DEPRECIATION_PERIOD_CLOSED = ErrorCode.define(
            "erp.err.ast.depreciation.period-closed",
            "折旧期间 {period} 已结账，不允许补提折旧",
            ARG_PERIOD);
    ErrorCode ERR_DEPRECIATION_ASSET_NOT_IN_SERVICE = ErrorCode.define(
            "erp.err.ast.depreciation.asset-not-in-service",
            "资产 {assetCode} 非使用中状态，不允许折旧",
            ARG_ASSET_CODE);
    ErrorCode ERR_DEPRECIATION_USEFUL_LIFE_INVALID = ErrorCode.define(
            "erp.err.ast.depreciation.useful-life-invalid",
            "资产 {assetCode} 使用年限或折旧方法无效，无法计算折旧",
            ARG_ASSET_CODE);
    ErrorCode ERR_SCHEDULE_ILLEGAL_STATUS_TRANSITION = ErrorCode.define(
            "erp.err.ast.schedule.illegal-status-transition",
            "折旧计划执行状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    // --- 处置 ---
    ErrorCode ERR_DISPOSAL_NOT_FOUND = ErrorCode.define(
            "erp.err.ast.disposal.not-found",
            "处置单 {disposalId} 不存在",
            ARG_DISPOSAL_ID);
    ErrorCode ERR_DISPOSAL_ILLEGAL_STATUS_TRANSITION = ErrorCode.define(
            "erp.err.ast.disposal.illegal-status-transition",
            "处置单 {disposalCode} 当前审核状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_DISPOSAL_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);
    ErrorCode ERR_DISPOSAL_ILLEGAL_DOC_TRANSITION = ErrorCode.define(
            "erp.err.ast.disposal.illegal-doc-transition",
            "处置单 {disposalCode} 当前单据状态={currentDocStatus}，不允许执行该操作（期望状态={expectedDocStatus}）",
            ARG_DISPOSAL_CODE, ARG_CURRENT_DOC_STATUS, ARG_EXPECTED_DOC_STATUS);
    ErrorCode ERR_DISPOSAL_ASSET_NOT_DISPOSABLE = ErrorCode.define(
            "erp.err.ast.disposal.asset-not-disposable",
            "资产 {assetCode} 当前状态不允许处置（须为使用中或闲置）",
            ARG_ASSET_CODE);
    ErrorCode ERR_DISPOSAL_ASSET_ALREADY_DISPOSED = ErrorCode.define(
            "erp.err.ast.disposal.asset-already-disposed",
            "资产 {assetCode} 已处置（终态不可恢复，需通过冲销处理）",
            ARG_ASSET_CODE);
}
