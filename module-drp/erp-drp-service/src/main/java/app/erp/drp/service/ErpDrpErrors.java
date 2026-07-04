package app.erp.drp.service;

import io.nop.api.core.exceptions.ErrorCode;

/**
 * DRP 域错误码。描述为中文，框架经 i18n 翻译；公共/GraphQL 面向错误统一 {@link ErrorCode} + {@code NopException}。
 *
 * <p>权威：`docs/design/drp/README.md`、`docs/design/drp/state-machine.md`、`docs/design/drp/safety-stock-optimization.md`、
 * `docs/plans/2026-07-04-1115-2-drp-net-requirement-safety-stock.md`。
 */
public interface ErpDrpErrors {

    String ARG_DRP_PLAN_ID = "drpPlanId";
    String ARG_DRP_LINE_ID = "drpLineId";
    String ARG_PLAN_CODE = "planCode";
    String ARG_CURRENT_STATUS = "currentStatus";
    String ARG_EXPECTED_STATUS = "expectedStatus";
    String ARG_MATERIAL_ID = "materialId";
    String ARG_WAREHOUSE_ID = "warehouseId";
    String ARG_METHOD = "method";
    String ARG_HISTORY_MONTHS = "historyMonths";

    ErrorCode ERR_DRP_PLAN_ILLEGAL_TRANSITION = ErrorCode.define(
            "nop.err.drp.plan.illegal-transition",
            "DRP计划[{planCode}]当前状态[{currentStatus}]不允许此操作，期望状态[{expectedStatus}]",
            ARG_PLAN_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_DRP_LINE_NOT_SUGGESTED = ErrorCode.define(
            "nop.err.drp.line.not-suggested",
            "DRP明细行[{drpLineId}]当前状态不允许释放，仅 APPROVED 行可释放",
            ARG_DRP_LINE_ID);

    ErrorCode ERR_DRP_PARAMETER_MISSING = ErrorCode.define(
            "nop.err.drp.parameter.missing",
            "物料[{materialId}]在仓库[{warehouseId}]未配置仓库补货参数（ErpDrpParameter），无法计算净需求",
            ARG_MATERIAL_ID, ARG_WAREHOUSE_ID);

    ErrorCode ERR_DRP_NO_SOURCE_WAREHOUSE = ErrorCode.define(
            "nop.err.drp.release.no-source-warehouse",
            "DRP明细行[{drpLineId}]补货类型为 TRANSFER 但仓库补货参数未配置首选调出仓库[preferredSourceWarehouseId]",
            ARG_DRP_LINE_ID);

    ErrorCode ERR_DRP_NO_PREFERRED_SUPPLIER = ErrorCode.define(
            "nop.err.drp.release.no-preferred-supplier",
            "DRP明细行[{drpLineId}]补货类型为 PURCHASE 但仓库补货参数未配置首选供应商[preferredSupplierId]",
            ARG_DRP_LINE_ID);

    ErrorCode ERR_DRP_LINE_ALREADY_ORDERED = ErrorCode.define(
            "nop.err.drp.line.already-ordered",
            "DRP明细行[{drpLineId}]已释放下单，不可重复释放",
            ARG_DRP_LINE_ID);

    ErrorCode ERR_DRP_SS_INSUFFICIENT_HISTORY = ErrorCode.define(
            "nop.err.drp.ss.insufficient-history",
            "安全库存计算：物料[{materialId}]历史需求样本不足（配置{historyMonths}月），降级使用 SIMPLE 方法",
            ARG_MATERIAL_ID, ARG_HISTORY_MONTHS);

    ErrorCode ERR_DRP_SS_METHOD_UNSUPPORTED = ErrorCode.define(
            "nop.err.drp.ss.method-unsupported",
            "安全库存计算：不支持的计算方法[{method}]（本期仅支持 STATISTICAL/SIMPLE/DDMRP）",
            ARG_METHOD);
}
