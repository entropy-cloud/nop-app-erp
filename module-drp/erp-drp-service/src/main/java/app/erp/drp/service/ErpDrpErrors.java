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

    ErrorCode ERR_DRP_LINE_ILLEGAL_TRANSITION = ErrorCode.define(
            "erp.err.drp.line.illegal-transition",
            "DRP明细行[{drpLineId}]当前状态[{currentStatus}]不允许此操作",
            ARG_DRP_LINE_ID, ARG_CURRENT_STATUS);

    // --- DRP 仿真引擎（plan 2026-07-22-1000-2；权威：docs/design/manufacturing/simulation-engine.md §DRP 对应物） ---

    String ARG_SCENARIO_ID = "scenarioId";
    String ARG_SCENARIO_VERSION_ID = "scenarioVersionId";

    ErrorCode ERR_DRP_SIMULATION_DISABLED = ErrorCode.define(
            "erp.err.drp.simulation.disabled",
            "DRP仿真入口未启用，请在配置中开启 erp-drp.simulation-enabled=true",
            ARG_SCENARIO_ID);

    ErrorCode ERR_DRP_SIMULATION_SCENARIO_NOT_DRAFT = ErrorCode.define(
            "erp.err.drp.simulation.scenario-not-draft",
            "DRP仿真场景[{scenarioId}]当前状态不允许此操作，期望状态 DRAFT",
            ARG_SCENARIO_ID, ARG_CURRENT_STATUS);

    ErrorCode ERR_DRP_SIMULATION_NO_BASELINE_PLAN = ErrorCode.define(
            "erp.err.drp.simulation.no-baseline-plan",
            "DRP仿真场景[{scenarioId}]未设置基线DRP计划(baseDrpPlanId)，无法运行仿真",
            ARG_SCENARIO_ID);

    ErrorCode ERR_DRP_SIMULATION_VERSION_ALREADY_PROMOTED = ErrorCode.define(
            "erp.err.drp.simulation.version-already-promoted",
            "DRP仿真版本[{scenarioVersionId}]已转正式计划，不可重复转正",
            ARG_SCENARIO_VERSION_ID);

    ErrorCode ERR_DRP_SIMULATION_VERSIONS_NOT_COMPARABLE = ErrorCode.define(
            "erp.err.drp.simulation.versions-not-comparable",
            "DRP仿真版本[{scenarioVersionId}]与另一版本不可对比（须同 orgId / 同基线计划）",
            ARG_SCENARIO_VERSION_ID);

    // ---- 越库执行（RC-R1.81 / P1-RC-081；权威：docs/design/drp/cross-dock.md §越库状态机） ----

    String ARG_XDOCK_ID = "crossDockId";
    String ARG_XDOCK_CODE = "crossDockCode";
    String ARG_INBOUND_MOVE_ID = "inboundMoveId";
    String ARG_STRATEGY = "strategy";
    String ARG_TARGET_BILL_CODE = "targetBillCode";

    ErrorCode ERR_DRP_XDOCK_ILLEGAL_TRANSITION = ErrorCode.define(
            "erp.err.drp.xdock.illegal-transition",
            "越库记录[{crossDockCode}]当前状态[{currentStatus}]不允许此操作，期望状态[{expectedStatus}]",
            ARG_XDOCK_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_DRP_XDOCK_DISABLED = ErrorCode.define(
            "erp.err.drp.xdock.disabled",
            "越库功能未启用（配置 erp-inv.drp-xdock-enabled=true 后重试）");

    ErrorCode ERR_DRP_XDOCK_STRATEGY_UNSUPPORTED = ErrorCode.define(
            "erp.err.drp.xdock.strategy-unsupported",
            "越库记录[{crossDockCode}]匹配策略[{strategy}]不支持（须 PRE_ALLOCATED/ON_RECEIPT/MANUAL）",
            ARG_XDOCK_CODE, ARG_STRATEGY);

    ErrorCode ERR_DRP_XDOCK_NO_PRE_ALLOCATED_TARGET = ErrorCode.define(
            "erp.err.drp.xdock.no-pre-allocated-target",
            "越库记录[{crossDockCode}]策略为 PRE_ALLOCATED 但创建时未预分配目标单（targetBillType/targetBillCode 为空）",
            ARG_XDOCK_CODE);

    ErrorCode ERR_DRP_XDOCK_NO_MATCH = ErrorCode.define(
            "erp.err.drp.xdock.no-match",
            "越库记录[{crossDockCode}]按 ON_RECEIPT 策略未找到待出库销售订单（承诺发货日期 ASC + 创建时间 ASC 候选为空）",
            ARG_XDOCK_CODE);

    ErrorCode ERR_DRP_XDOCK_TARGET_REQUIRED = ErrorCode.define(
            "erp.err.drp.xdock.target-required",
            "越库记录[{crossDockCode}]策略为 MANUAL，必须显式指定目标单据类型与单号",
            ARG_XDOCK_CODE);

    ErrorCode ERR_DRP_XDOCK_QUALITY_GATE_BLOCKED = ErrorCode.define(
            "erp.err.drp.xdock.quality-gate-blocked",
            "越库记录[{crossDockCode}]物料[{materialId}]需质检（存在有效检验模板），暂存区快检合格后才可匹配",
            ARG_XDOCK_CODE, ARG_MATERIAL_ID);

    // ---- 提前期跟踪（RC-R1.82 / P1-RC-082；权威：docs/design/drp/lead-time-tracking.md） ----

    String ARG_SUPPLIER_ID = "supplierId";
    String ARG_PURCHASE_ORDER_CODE = "purchaseOrderCode";

    ErrorCode ERR_DRP_LT_DATES_INVALID = ErrorCode.define(
            "erp.err.drp.lt.dates-invalid",
            "提前期记录输入非法：采购单[{purchaseOrderCode}]订单日期或收货日期缺失/倒置，跳过记录",
            ARG_PURCHASE_ORDER_CODE);

    ErrorCode ERR_DRP_LT_NO_SAMPLES = ErrorCode.define(
            "erp.err.drp.lt.no-samples",
            "供应商[{supplierId}]物料[{materialId}]统计窗口内无提前期样本，无法统计评分",
            ARG_SUPPLIER_ID, ARG_MATERIAL_ID);

    ErrorCode ERR_DRP_LT_STATS_FILTER_REQUIRED = ErrorCode.define(
            "erp.err.drp.lt.stats-filter-required",
            "提前期统计至少需要提供一个过滤参数（supplierId 供应商级 / materialId 物料级 / 两者组合供应商+物料级）");
}
