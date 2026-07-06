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
    String ARG_ADJUSTMENT_CODE = "adjustmentCode";
    String ARG_ADJUSTMENT_ID = "adjustmentId";
    String ARG_ADJUSTMENT_TYPE = "adjustmentType";

    // --- 报表渲染作用域参数键 ---
    String ARG_REPORT_NAME = "reportName";
    String ARG_RENDER_TYPE = "renderType";

    // --- CIP 作用域参数键 ---
    String ARG_CIP_CODE = "cipCode";
    String ARG_CIP_ID = "cipId";
    String ARG_TARGET_STATUS = "targetStatus";
    String ARG_COST_TYPE = "costType";

    // --- 拆分/合并 作用域参数键 ---
    String ARG_SPLIT_CODE = "splitCode";
    String ARG_SPLIT_ID = "splitId";
    String ARG_MERGE_CODE = "mergeCode";
    String ARG_MERGE_ID = "mergeId";
    String ARG_ACTUAL = "actual";
    String ARG_EXPECTED = "expected";

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

    // --- 价值调整（减值/重估） ---
    ErrorCode ERR_ADJUSTMENT_NOT_FOUND = ErrorCode.define(
            "erp.err.ast.adjustment.not-found",
            "价值调整单 {adjustmentId} 不存在",
            ARG_ADJUSTMENT_ID);
    ErrorCode ERR_ADJUSTMENT_ILLEGAL_STATUS_TRANSITION = ErrorCode.define(
            "erp.err.ast.adjustment.illegal-status-transition",
            "价值调整单 {adjustmentCode} 当前审核状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_ADJUSTMENT_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);
    ErrorCode ERR_ADJUSTMENT_ILLEGAL_DOC_TRANSITION = ErrorCode.define(
            "erp.err.ast.adjustment.illegal-doc-transition",
            "价值调整单 {adjustmentCode} 当前单据状态={currentDocStatus}，不允许执行该操作（期望状态={expectedDocStatus}）",
            ARG_ADJUSTMENT_CODE, ARG_CURRENT_DOC_STATUS, ARG_EXPECTED_DOC_STATUS);
    ErrorCode ERR_ADJUSTMENT_ASSET_NOT_ADJUSTABLE = ErrorCode.define(
            "erp.err.ast.adjustment.asset-not-adjustable",
            "资产 {assetCode} 当前状态不允许价值调整（须为使用中或闲置）",
            ARG_ASSET_CODE);
    ErrorCode ERR_ADJUSTMENT_ASSET_ALREADY_DISPOSED = ErrorCode.define(
            "erp.err.ast.adjustment.asset-already-disposed",
            "资产 {assetCode} 已处置，不允许价值调整",
            ARG_ASSET_CODE);
    ErrorCode ERR_ADJUSTMENT_TYPE_INVALID = ErrorCode.define(
            "erp.err.ast.adjustment.type-invalid",
            "价值调整单 {adjustmentCode} 调整类型={adjustmentType} 无效",
            ARG_ADJUSTMENT_CODE, ARG_ADJUSTMENT_TYPE);
    ErrorCode ERR_ADJUSTMENT_AMOUNT_INVALID = ErrorCode.define(
            "erp.err.ast.adjustment.amount-invalid",
            "价值调整单 {adjustmentCode} 调整金额无效",
            ARG_ADJUSTMENT_CODE, ARG_AMOUNT);
    ErrorCode ERR_ADJUSTMENT_ALREADY_REVERSED = ErrorCode.define(
            "erp.err.ast.adjustment.already-reversed",
            "价值调整单 {adjustmentCode} 已红冲，不允许二次红冲",
            ARG_ADJUSTMENT_CODE);
    ErrorCode ERR_ADJUSTMENT_APPROVAL_REQUIRED = ErrorCode.define(
            "erp.err.ast.adjustment.approval-required",
            "价值调整单 {adjustmentCode} 配置强制审批，须先审核通过再生效",
            ARG_ADJUSTMENT_CODE);

    // --- 报表渲染作用域（镜像 ErpMfgErrors.ERR_REPORT_*，资产域独立错误码，不跨域 import） ---

    ErrorCode ERR_REPORT_NAME_INVALID = ErrorCode.define(
            "erp.err.ast.report.name-invalid",
            "报表名[{reportName}]非法（含路径注入字符或不合规段），拒绝渲染",
            ARG_REPORT_NAME);

    ErrorCode ERR_REPORT_RENDER_TYPE_INVALID = ErrorCode.define(
            "erp.err.ast.report.render-type-invalid",
            "渲染类型[{renderType}]非法（仅允许 html/xlsx/pdf）",
            ARG_RENDER_TYPE);

    // --- 在建工程（CIP） ---
    ErrorCode ERR_CIP_NOT_FOUND = ErrorCode.define(
            "erp.err.ast.cip.not-found",
            "在建工程 {cipId} 不存在",
            ARG_CIP_ID);
    ErrorCode ERR_CIP_ILLEGAL_STATUS_TRANSITION = ErrorCode.define(
            "erp.err.ast.cip.illegal-status-transition",
            "在建工程 {cipCode} 当前状态={currentStatus}，不允许迁移到目标状态={targetStatus}",
            ARG_CIP_CODE, ARG_CURRENT_STATUS, ARG_TARGET_STATUS);
    ErrorCode ERR_CIP_NOT_IN_CONSTRUCTION = ErrorCode.define(
            "erp.err.ast.cip.not-in-construction",
            "在建工程 {cipCode} 当前状态={currentStatus}，仅建设中(IN_CONSTRUCTION)状态允许此操作",
            ARG_CIP_CODE, ARG_CURRENT_STATUS);
    ErrorCode ERR_CIP_INTEREST_CAPITALIZATION_DISABLED = ErrorCode.define(
            "erp.err.ast.cip.interest-capitalization-disabled",
            "在建工程 {cipCode} 利息资本化功能未启用（配置 erp-ast.cip-interest-capitalization-enabled=false）",
            ARG_CIP_CODE);
    ErrorCode ERR_CIP_COST_ITEM_ALREADY_TRANSFERRED = ErrorCode.define(
            "erp.err.ast.cip.cost-item-already-transferred",
            "在建工程 {cipCode} 选中的成本归集行已转固，不允许重复转固",
            ARG_CIP_CODE);
    ErrorCode ERR_CIP_NO_COST_TO_TRANSFER = ErrorCode.define(
            "erp.err.ast.cip.no-cost-to-transfer",
            "在建工程 {cipCode} 没有可转固的成本归集行",
            ARG_CIP_CODE);
    ErrorCode ERR_CIP_ALREADY_COMPLETED = ErrorCode.define(
            "erp.err.ast.cip.already-completed",
            "在建工程 {cipCode} 已完工转固（终态），不允许再次转固",
            ARG_CIP_CODE);
    ErrorCode ERR_CIP_PARTIAL_TRANSFER_NOT_ALLOWED = ErrorCode.define(
            "erp.err.ast.cip.partial-transfer-not-allowed",
            "在建工程 {cipCode} 配置不允许部分转固，必须选择全部成本归集行",
            ARG_CIP_CODE);
    ErrorCode ERR_CIP_PARTIAL_REVERSE_NOT_SUPPORTED = ErrorCode.define(
            "erp.err.ast.cip.partial-reverse-not-supported",
            "在建工程 {cipCode} 不支持部分红冲（本期 Non-Goal，仅支持全部红冲）",
            ARG_CIP_CODE);
    ErrorCode ERR_CIP_AMOUNT_INVALID = ErrorCode.define(
            "erp.err.ast.cip.amount-invalid",
            "在建工程 {cipCode} 金额无效（须为正数）",
            ARG_CIP_CODE, ARG_AMOUNT);
    ErrorCode ERR_CIP_COST_TYPE_INVALID = ErrorCode.define(
            "erp.err.ast.cip.cost-type-invalid",
            "在建工程 {cipCode} 成本类型={costType} 无效",
            ARG_CIP_CODE, ARG_COST_TYPE);

    // --- 资产拆分 ---
    ErrorCode ERR_AST_SPLIT_NOT_FOUND = ErrorCode.define(
            "erp.err.ast.split.not-found",
            "资产拆分单 {splitId} 不存在",
            ARG_SPLIT_ID);
    ErrorCode ERR_AST_SPLIT_ILLEGAL_STATUS_TRANSITION = ErrorCode.define(
            "erp.err.ast.split.illegal-status-transition",
            "资产拆分单 {splitCode} 当前审核状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_SPLIT_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);
    ErrorCode ERR_AST_SPLIT_ILLEGAL_DOC_TRANSITION = ErrorCode.define(
            "erp.err.ast.split.illegal-doc-transition",
            "资产拆分单 {splitCode} 当前单据状态={currentDocStatus}，不允许执行该操作（期望状态={expectedDocStatus}）",
            ARG_SPLIT_CODE, ARG_CURRENT_DOC_STATUS, ARG_EXPECTED_DOC_STATUS);
    ErrorCode ERR_AST_SPLIT_SOURCE_NOT_IN_SERVICE = ErrorCode.define(
            "erp.err.ast.split.source-not-in-service",
            "资产拆分单 {splitCode} 源资产 {assetCode} 非使用中状态，不允许拆分",
            ARG_SPLIT_CODE, ARG_ASSET_CODE);
    ErrorCode ERR_AST_SPLIT_PROPORTION_NOT_BALANCED = ErrorCode.define(
            "erp.err.ast.split.proportion-not-balanced",
            "资产拆分单 {splitCode} 比例合计 {actual} 不等于 1（期望 {expected}，容差 0.000001）",
            ARG_SPLIT_CODE, ARG_ACTUAL, ARG_EXPECTED);
    ErrorCode ERR_AST_SPLIT_AMOUNT_NOT_BALANCED = ErrorCode.define(
            "erp.err.ast.split.amount-not-balanced",
            "资产拆分单 {splitCode} 固定金额合计 {actual} 不等于源资产原值 {expected}（容差 0.01）",
            ARG_SPLIT_CODE, ARG_ACTUAL, ARG_EXPECTED);
    ErrorCode ERR_AST_SPLIT_CROSS_CATEGORY_NOT_ALLOWED = ErrorCode.define(
            "erp.err.ast.split.cross-category-not-allowed",
            "资产拆分单 {splitCode} 配置不允许跨资产类别拆分（erp-ast.split-merge-allow-cross-category=false）",
            ARG_SPLIT_CODE);
    ErrorCode ERR_AST_SPLIT_ALREADY_POSTED = ErrorCode.define(
            "erp.err.ast.split.already-posted",
            "资产拆分单 {splitCode} 已执行过账，不允许重复执行",
            ARG_SPLIT_CODE);
    ErrorCode ERR_AST_SPLIT_TARGET_ASSET_CODE_DUPLICATE = ErrorCode.define(
            "erp.err.ast.split.target-asset-code-duplicate",
            "资产拆分单 {splitCode} 目标资产编码 {assetCode} 重复",
            ARG_SPLIT_CODE, ARG_ASSET_CODE);
    ErrorCode ERR_AST_SPLIT_INSUFFICIENT_NET_VALUE = ErrorCode.define(
            "erp.err.ast.split.insufficient-net-value",
            "资产拆分单 {splitCode} 源资产账面净值 {amount} 不大于零，不允许拆分",
            ARG_SPLIT_CODE, ARG_AMOUNT);
    ErrorCode ERR_AST_SPLIT_NO_LINES = ErrorCode.define(
            "erp.err.ast.split.no-lines",
            "资产拆分单 {splitCode} 没有拆分行",
            ARG_SPLIT_CODE);
    ErrorCode ERR_AST_SPLIT_REVERSE_NOT_SUPPORTED = ErrorCode.define(
            "erp.err.ast.split.reverse-not-supported",
            "资产拆分单 {splitCode} 执行后不可撤销（owner doc split-merge.md §关键业务规则 5），错误更正请走资产处置 + 新建流程",
            ARG_SPLIT_CODE);

    // --- 资产合并 ---
    ErrorCode ERR_AST_MERGE_NOT_FOUND = ErrorCode.define(
            "erp.err.ast.merge.not-found",
            "资产合并单 {mergeId} 不存在",
            ARG_MERGE_ID);
    ErrorCode ERR_AST_MERGE_ILLEGAL_STATUS_TRANSITION = ErrorCode.define(
            "erp.err.ast.merge.illegal-status-transition",
            "资产合并单 {mergeCode} 当前审核状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_MERGE_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);
    ErrorCode ERR_AST_MERGE_ILLEGAL_DOC_TRANSITION = ErrorCode.define(
            "erp.err.ast.merge.illegal-doc-transition",
            "资产合并单 {mergeCode} 当前单据状态={currentDocStatus}，不允许执行该操作（期望状态={expectedDocStatus}）",
            ARG_MERGE_CODE, ARG_CURRENT_DOC_STATUS, ARG_EXPECTED_DOC_STATUS);
    ErrorCode ERR_AST_MERGE_SOURCE_NOT_IN_SERVICE = ErrorCode.define(
            "erp.err.ast.merge.source-not-in-service",
            "资产合并单 {mergeCode} 源资产 {assetCode} 非使用中状态，不允许合并",
            ARG_MERGE_CODE, ARG_ASSET_CODE);
    ErrorCode ERR_AST_MERGE_CROSS_CATEGORY_NOT_ALLOWED = ErrorCode.define(
            "erp.err.ast.merge.cross-category-not-allowed",
            "资产合并单 {mergeCode} 配置不允许跨资产类别合并（erp-ast.split-merge-allow-cross-category=false）",
            ARG_MERGE_CODE);
    ErrorCode ERR_AST_MERGE_CROSS_CURRENCY_NOT_ALLOWED = ErrorCode.define(
            "erp.err.ast.merge.cross-currency-not-allowed",
            "资产合并单 {mergeCode} 源资产币种不一致，不允许合并",
            ARG_MERGE_CODE);
    ErrorCode ERR_AST_MERGE_NO_SOURCES = ErrorCode.define(
            "erp.err.ast.merge.no-sources",
            "资产合并单 {mergeCode} 没有源资产行",
            ARG_MERGE_CODE);
    ErrorCode ERR_AST_MERGE_ALREADY_POSTED = ErrorCode.define(
            "erp.err.ast.merge.already-posted",
            "资产合并单 {mergeCode} 已执行过账，不允许重复执行",
            ARG_MERGE_CODE);
    ErrorCode ERR_AST_MERGE_REVERSE_NOT_SUPPORTED = ErrorCode.define(
            "erp.err.ast.merge.reverse-not-supported",
            "资产合并单 {mergeCode} 执行后不可撤销（owner doc split-merge.md §关键业务规则 5），错误更正请走资产处置 + 新建流程",
            ARG_MERGE_CODE);
}
