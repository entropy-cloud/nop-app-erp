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
    // O-11 扩展参数键
    String ARG_SAFETY_STOCK = "safetyStock";
    String ARG_REORDER_POINT = "reorderPoint";
    String ARG_DEMAND_QTY = "demandQty";
    String ARG_AVAILABLE_QTY = "availableQty";
    String ARG_SERVICE_LEVEL = "serviceLevel";
    String ARG_REASON = "reason";

    ErrorCode ERR_DRP_PLAN_ILLEGAL_TRANSITION = ErrorCode.define(
            "erp.err.drp.plan.illegal-transition",
            "DRP计划[{planCode}]当前状态[{currentStatus}]不允许此操作，期望状态[{expectedStatus}]",
            ARG_PLAN_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_DRP_LINE_NOT_SUGGESTED = ErrorCode.define(
            "erp.err.drp.line.not-suggested",
            "DRP明细行[{drpLineId}]当前状态不允许释放，仅 APPROVED 行可释放",
            ARG_DRP_LINE_ID);

    ErrorCode ERR_DRP_PARAMETER_MISSING = ErrorCode.define(
            "erp.err.drp.parameter.missing",
            "物料[{materialId}]在仓库[{warehouseId}]未配置仓库补货参数（ErpDrpParameter），无法计算净需求",
            ARG_MATERIAL_ID, ARG_WAREHOUSE_ID);

    ErrorCode ERR_DRP_NO_SOURCE_WAREHOUSE = ErrorCode.define(
            "erp.err.drp.release.no-source-warehouse",
            "DRP明细行[{drpLineId}]补货类型为 TRANSFER 但仓库补货参数未配置首选调出仓库[preferredSourceWarehouseId]",
            ARG_DRP_LINE_ID);

    ErrorCode ERR_DRP_NO_PREFERRED_SUPPLIER = ErrorCode.define(
            "erp.err.drp.release.no-preferred-supplier",
            "DRP明细行[{drpLineId}]补货类型为 PURCHASE 但仓库补货参数未配置首选供应商[preferredSupplierId]",
            ARG_DRP_LINE_ID);

    ErrorCode ERR_DRP_LINE_ALREADY_ORDERED = ErrorCode.define(
            "erp.err.drp.line.already-ordered",
            "DRP明细行[{drpLineId}]已释放下单，不可重复释放",
            ARG_DRP_LINE_ID);

    ErrorCode ERR_DRP_SS_INSUFFICIENT_HISTORY = ErrorCode.define(
            "erp.err.drp.ss.insufficient-history",
            "安全库存计算：物料[{materialId}]历史需求样本不足（配置{historyMonths}月），降级使用 SIMPLE 方法",
            ARG_MATERIAL_ID, ARG_HISTORY_MONTHS);

    ErrorCode ERR_DRP_SS_METHOD_UNSUPPORTED = ErrorCode.define(
            "erp.err.drp.ss.method-unsupported",
            "安全库存计算：不支持的计算方法[{method}]（本期仅支持 STATISTICAL/SIMPLE/DDMRP）",
            ARG_METHOD);

    // ---------- O-11 扩展：计划/行/参数/库存等细粒度错误码 ----------

    ErrorCode ERR_DRP_PLAN_NOT_FOUND = ErrorCode.define(
            "erp.err.drp.plan.not-found",
            "DRP计划[{planCode}]不存在", ARG_PLAN_CODE);

    ErrorCode ERR_DRP_LINE_NOT_FOUND = ErrorCode.define(
            "erp.err.drp.line.not-found",
            "DRP明细行[{drpLineId}]不存在", ARG_DRP_LINE_ID);

    ErrorCode ERR_DRP_PLAN_ALREADY_RUN = ErrorCode.define(
            "erp.err.drp.plan.already-run",
            "DRP计划[{planCode}]已运行过净需求计算，不可重复运行（请先重置）",
            ARG_PLAN_CODE);

    ErrorCode ERR_DRP_NET_REQ_NEGATIVE = ErrorCode.define(
            "erp.err.drp.net-req.negative",
            "DRP净需求计算异常：物料[{materialId}]需求量[{demandQty}]为负数",
            ARG_MATERIAL_ID, ARG_DEMAND_QTY);

    ErrorCode ERR_DRP_STOCK_BELOW_SAFETY = ErrorCode.define(
            "erp.err.drp.stock.below-safety",
            "物料[{materialId}]可用量[{availableQty}]低于安全库存[{safetyStock}]，建议立即补货",
            ARG_MATERIAL_ID, ARG_AVAILABLE_QTY, ARG_SAFETY_STOCK);

    ErrorCode ERR_DRP_STOCK_BELOW_REORDER = ErrorCode.define(
            "erp.err.drp.stock.below-reorder",
            "物料[{materialId}]可用量[{availableQty}]低于再订货点[{reorderPoint}]",
            ARG_MATERIAL_ID, ARG_AVAILABLE_QTY, ARG_REORDER_POINT);

    ErrorCode ERR_DRP_SS_SERVICE_LEVEL_INVALID = ErrorCode.define(
            "erp.err.drp.ss.service-level-invalid",
            "安全库存计算：服务水平[{serviceLevel}]非法（须 0~1 之间）",
            ARG_SERVICE_LEVEL);

    ErrorCode ERR_DRP_RELEASE_FAILED = ErrorCode.define(
            "erp.err.drp.release.failed",
            "DRP明细行[{drpLineId}]释放失败：原因[{reason}]",
            ARG_DRP_LINE_ID, ARG_REASON);

    ErrorCode ERR_DRP_PARAMETER_LEAD_TIME_INVALID = ErrorCode.define(
            "erp.err.drp.parameter.lead-time-invalid",
            "物料[{materialId}]仓库补货参数提前期非法（须为正数）",
            ARG_MATERIAL_ID);

    ErrorCode ERR_DRP_CALC_ENGINE_ERROR = ErrorCode.define(
            "erp.err.drp.calc.engine-error",
            "DRP计算引擎执行异常：方法[{method}] / 原因[{reason}]",
            ARG_METHOD, ARG_REASON);
}
