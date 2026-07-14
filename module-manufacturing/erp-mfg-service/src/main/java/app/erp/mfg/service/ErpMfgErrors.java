package app.erp.mfg.service;

import io.nop.api.core.exceptions.ErrorCode;

/**
 * 制造域错误码。描述为中文，框架经 i18n 翻译；公共/GraphQL 面向错误统一 {@link ErrorCode} + {@code NopException}。
 *
 * <p>权威：`docs/design/manufacturing/bom-and-routing.md`、`docs/design/manufacturing/state-machine.md`、
 * `docs/plans/2026-07-02-1538-2-manufacturing-bom-routing-rollup.md`、`docs/plans/2026-07-02-2237-1-manufacturing-workorder-jobcard-state-machine.md`。
 */
public interface ErpMfgErrors {

    String ARG_BOM_ID = "bomId";
    String ARG_PRODUCT_ID = "productId";
    String ARG_MATERIAL_ID = "materialId";
    String ARG_DEPTH = "depth";
    String ARG_PATH = "path";

    String ARG_WORK_ORDER_ID = "workOrderId";
    String ARG_WORK_ORDER_CODE = "workOrderCode";
    String ARG_CURRENT_STATUS = "currentStatus";
    String ARG_EXPECTED_STATUS = "expectedStatus";
    String ARG_REQUIRED_QTY = "requiredQty";
    String ARG_AVAILABLE_QTY = "availableQty";
    String ARG_JOB_CARD_ID = "jobCardId";
    String ARG_COMPLETED_QTY = "completedQty";
    String ARG_PLANNED_QTY = "plannedQty";

    String ARG_MRP_PLAN_ID = "mrpPlanId";
    String ARG_MRP_LINE_ID = "mrpLineId";
    String ARG_PLAN_CODE = "planCode";
    String ARG_SUPPLIER_ID = "supplierId";

    String ARG_FORECAST_ID = "forecastId";
    String ARG_FORECAST_CODE = "forecastCode";

    String ARG_PERIOD_FROM = "periodFrom";
    String ARG_PERIOD_TO = "periodTo";

    String ARG_SOURCE_SCHEDULE_ID = "sourceScheduleId";
    String ARG_EXISTING_COUNT = "existingCount";

    // --- 报表渲染作用域参数键 ---
    String ARG_REPORT_NAME = "reportName";
    String ARG_RENDER_TYPE = "renderType";

    ErrorCode ERR_BOM_NOT_FOUND = ErrorCode.define(
            "erp.err.mfg.bom.not-found",
            "BOM不存在: {bomId}",
            ARG_BOM_ID);

    ErrorCode ERR_DEFAULT_BOM_NOT_FOUND = ErrorCode.define(
            "erp.err.mfg.bom.default-not-found",
            "物料[{productId}]不存在默认且有效的BOM",
            ARG_PRODUCT_ID);

    ErrorCode ERR_BOM_CYCLE = ErrorCode.define(
            "erp.err.mfg.bom.cycle",
            "BOM存在环引用（物料[{materialId}]在展开路径上重复出现）: {path}",
            ARG_MATERIAL_ID, ARG_PATH);

    ErrorCode ERR_BOM_MAX_DEPTH_EXCEEDED = ErrorCode.define(
            "erp.err.mfg.bom.max-depth-exceeded",
            "BOM展开深度超过上限{depth}（疑似环或层级过深）",
            ARG_DEPTH);

    ErrorCode ERR_ROLLUP_BASE_COST_MISSING = ErrorCode.define(
            "erp.err.mfg.rollup.base-cost-missing",
            "采购件[{materialId}]无默认SKU采购价，无法卷算基础成本，请先配置默认SKU的采购价",
            ARG_MATERIAL_ID);

    ErrorCode ERR_WORK_ORDER_NOT_FOUND = ErrorCode.define(
            "erp.err.mfg.work-order.not-found",
            "工单不存在: {workOrderId}",
            ARG_WORK_ORDER_ID);

    ErrorCode ERR_INVALID_STATUS_TRANSITION = ErrorCode.define(
            "erp.err.mfg.work-order.illegal-status-transition",
            "工单[{workOrderCode}]当前状态[{currentStatus}]不允许此操作，期望状态[{expectedStatus}]",
            ARG_WORK_ORDER_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_PARTIAL_KIT_START_FORBIDDEN = ErrorCode.define(
            "erp.err.mfg.work-order.partial-kit-start-forbidden",
            "工单[{workOrderCode}]齐套校验为部分齐套，配置erp-mfg.allow-partial-kit-start=false时不允许强制开工，请补料后重试",
            ARG_WORK_ORDER_CODE);

    ErrorCode ERR_JOB_CARD_NOT_FOUND = ErrorCode.define(
            "erp.err.mfg.job-card.not-found",
            "作业卡不存在: {jobCardId}",
            ARG_JOB_CARD_ID);

    ErrorCode ERR_INSPECTION_REQUIRED = ErrorCode.define(
            "erp.err.mfg.work-order.inspection-required",
            "工单[{workOrderCode}]BOM要求完工质检且erp-mfg.inspection-gate-enabled=true，完工入库暂挂等待质检结果",
            ARG_WORK_ORDER_CODE);

    ErrorCode ERR_OVER_REPORT = ErrorCode.define(
            "erp.err.mfg.work-order.over-report",
            "报工/完工数量[{completedQty}]超过工单计划数量[{plannedQty}]（未启用超产配置）",
            ARG_COMPLETED_QTY, ARG_PLANNED_QTY);

    ErrorCode ERR_ISSUE_LINES_EMPTY = ErrorCode.define(
            "erp.err.mfg.issue.lines-empty",
            "领料单[{workOrderCode}]无领料行，无法确认出库",
            ARG_WORK_ORDER_CODE);

    ErrorCode ERR_MRP_PLAN_NOT_FOUND = ErrorCode.define(
            "erp.err.mfg.mrp-plan.not-found",
            "MRP计划不存在: {mrpPlanId}",
            ARG_MRP_PLAN_ID);

    ErrorCode ERR_MRP_PLAN_LINE_NOT_FOUND = ErrorCode.define(
            "erp.err.mfg.mrp-plan-line.not-found",
            "MRP计划行不存在: {mrpLineId}",
            ARG_MRP_LINE_ID);

    ErrorCode ERR_MRP_INVALID_PLAN_STATUS = ErrorCode.define(
            "erp.err.mfg.mrp-plan.illegal-status",
            "MRP计划[{planCode}]当前状态不允许此操作",
            ARG_PLAN_CODE, ARG_CURRENT_STATUS);

    ErrorCode ERR_MRP_LINE_ALREADY_FIRMED = ErrorCode.define(
            "erp.err.mfg.mrp-plan-line.already-firmed",
            "MRP计划行[{mrpLineId}]已释放转单，不可重复释放",
            ARG_MRP_LINE_ID);

    ErrorCode ERR_MRP_RELEASE_MISSING_SUPPLIER = ErrorCode.define(
            "erp.err.mfg.mrp-release.missing-supplier",
            "释放采购建议行[{mrpLineId}]须提供供应商[supplierId]",
            ARG_MRP_LINE_ID);

    ErrorCode ERR_MRP_RELEASE_UNSUPPORTED_ORDER_TYPE = ErrorCode.define(
            "erp.err.mfg.mrp-release.unsupported-order-type",
            "MRP计划行[{mrpLineId}]的建议类型不支持释放转单（仅支持采购建议/工单建议）",
            ARG_MRP_LINE_ID);

    ErrorCode ERR_CRP_PERIOD_INVALID = ErrorCode.define(
            "erp.err.mfg.crp.period-invalid",
            "CRP负荷计算区间非法：periodFrom[{periodFrom}]与periodTo[{periodTo}]均不可为空且from不得晚于to",
            ARG_PERIOD_FROM, ARG_PERIOD_TO);

    ErrorCode ERR_FORECAST_NOT_FOUND = ErrorCode.define(
            "erp.err.mfg.forecast.not-found",
            "需求预测不存在: {forecastId}",
            ARG_FORECAST_ID);

    ErrorCode ERR_FORECAST_ILLEGAL_STATUS_TRANSITION = ErrorCode.define(
            "erp.err.mfg.forecast.illegal-status-transition",
            "需求预测[{forecastCode}]当前状态[{currentStatus}]不允许此操作，期望状态[{expectedStatus}]",
            ARG_FORECAST_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_JOB_CARDS_ALREADY_GENERATED = ErrorCode.define(
            "erp.err.mfg.work-order.job-cards-already-generated",
            "工单[{workOrderCode}]已由APS排程生成[{existingCount}]张工序卡，重复调用被拒绝（如需补建缺失卡，启用配置erp-mfg.jobcard-incremental-rebuild=true）",
            ARG_WORK_ORDER_CODE, ARG_EXISTING_COUNT);

    ErrorCode ERR_NO_SCHEDULED_OPERATIONS = ErrorCode.define(
            "erp.err.mfg.work-order.no-scheduled-operations",
            "工单[{workOrderCode}]在APS排程中无已排程(PLANNED)且时间完整的工序，无法生成工序卡",
            ARG_WORK_ORDER_CODE);

    ErrorCode ERR_WORK_ORDER_STATUS_NOT_ALLOWED_FOR_JOB_CARD_GEN = ErrorCode.define(
            "erp.err.mfg.work-order.status-not-allowed-for-job-card-gen",
            "工单[{workOrderCode}]当前状态[{currentStatus}]不允许由APS排程生成工序卡，期望状态为已审核且非终态(NOT_STARTED/STOCK_RESERVED/STOCK_PARTIAL/IN_PROCESS/STOPPED)",
            ARG_WORK_ORDER_CODE, ARG_CURRENT_STATUS);

    ErrorCode ERR_VARIANCE_NO_STANDARD_COST = ErrorCode.define(
            "erp.err.mfg.variance.no-standard-cost",
            "产品[{productId}]无已发布(FIRMED)的标准成本卷算，无法计算生产差异，请先卷算并发布标准成本",
            ARG_PRODUCT_ID);

    ErrorCode ERR_VARIANCE_WORKORDER_NOT_COMPLETED = ErrorCode.define(
            "erp.err.mfg.variance.workorder-not-completed",
            "工单[{workOrderCode}]当前状态[{currentStatus}]未完工(COMPLETED)，不允许手动计算生产差异",
            ARG_WORK_ORDER_CODE, ARG_CURRENT_STATUS);

    // --- 报表渲染作用域（镜像 ErpFinErrors.ERR_REPORT_*，制造域独立错误码，不跨域 import） ---

    ErrorCode ERR_REPORT_NAME_INVALID = ErrorCode.define(
            "erp.err.mfg.report.name-invalid",
            "报表名[{reportName}]非法（含路径注入字符或不合规段），拒绝渲染",
            ARG_REPORT_NAME);

    ErrorCode ERR_REPORT_RENDER_TYPE_INVALID = ErrorCode.define(
            "erp.err.mfg.report.render-type-invalid",
            "渲染类型[{renderType}]非法（仅允许 html/xlsx/pdf）",
            ARG_RENDER_TYPE);

    // --- 生产批次基因链追溯（plan 2026-07-07-0305-3；权威：docs/design/manufacturing/batch-genealogy.md） ---

    String ARG_LOT_ID = "lotId";
    String ARG_OUTPUT_LOT_ID = "outputLotId";
    String ARG_INPUT_LOT_ID = "inputLotId";
    String ARG_DIRECTION = "direction";
    String ARG_MAX_DEPTH = "maxDepth";

    ErrorCode ERR_MFG_GENEALOGY_LOT_NOT_FOUND = ErrorCode.define(
            "erp.err.mfg.genealogy.lot-not-found",
            "批次不存在: {lotId}",
            ARG_LOT_ID);

    ErrorCode ERR_MFG_GENEALOGY_MAX_DEPTH_EXCEEDED = ErrorCode.define(
            "erp.err.mfg.genealogy.max-depth-exceeded",
            "批次基因链追溯深度超过上限{depth}（疑似环路或层级过深）",
            ARG_DEPTH);

    ErrorCode ERR_MFG_GENEALOGY_INVALID_DIRECTION = ErrorCode.define(
            "erp.err.mfg.genealogy.invalid-direction",
            "追溯方向[{direction}]非法（仅允许 FORWARD/BACKWARD）",
            ARG_DIRECTION);

    // --- 委外加工（plan 2026-07-13-0455-1；权威：docs/design/manufacturing/subcontracting.md） ---

    String ARG_SUBCONTRACT_ORDER_ID = "subcontractOrderId";
    String ARG_SUBCONTRACT_ORDER_CODE = "subcontractOrderCode";

    ErrorCode ERR_SUBCONTRACT_ORDER_NOT_FOUND = ErrorCode.define(
            "erp.err.mfg.subcontract-order.not-found",
            "委外单不存在: {subcontractOrderId}",
            ARG_SUBCONTRACT_ORDER_ID);

    ErrorCode ERR_SUBCONTRACT_ILLEGAL_STATUS_TRANSITION = ErrorCode.define(
            "erp.err.mfg.subcontract-order.illegal-status-transition",
            "委外单[{subcontractOrderCode}]当前状态[{currentStatus}]不允许此操作，期望状态[{expectedStatus}]",
            ARG_SUBCONTRACT_ORDER_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_SUBCONTRACT_LINES_EMPTY = ErrorCode.define(
            "erp.err.mfg.subcontract-order.lines-empty",
            "委外单[{subcontractOrderCode}]无委外行，无法发料",
            ARG_SUBCONTRACT_ORDER_CODE);

    ErrorCode ERR_SUBCONTRACT_OVER_ISSUE = ErrorCode.define(
            "erp.err.mfg.subcontract-order.over-issue",
            "委外单[{subcontractOrderCode}]发料数量[{requiredQty}]超过委外行标准数量[{plannedQty}]（未启用超发配置）",
            ARG_SUBCONTRACT_ORDER_CODE, ARG_REQUIRED_QTY, ARG_PLANNED_QTY);

    ErrorCode ERR_SUBCONTRACT_OVER_RECEIPT = ErrorCode.define(
            "erp.err.mfg.subcontract-order.over-receipt",
            "委外单[{subcontractOrderCode}]收货数量[{completedQty}]超过发料数量[{plannedQty}]（扣除损耗）",
            ARG_SUBCONTRACT_ORDER_CODE, ARG_COMPLETED_QTY, ARG_PLANNED_QTY);

    ErrorCode ERR_SUBCONTRACT_RELEASE_MISSING_SUPPLIER = ErrorCode.define(
            "erp.err.mfg.subcontract-release.missing-supplier",
            "释放委外建议行[{mrpLineId}]须提供供应商[supplierId]",
            ARG_MRP_LINE_ID);

    ErrorCode ERR_SUBCONTRACT_CANNOT_REVERSE = ErrorCode.define(
            "erp.err.mfg.subcontract-order.cannot-reverse",
            "委外单[{subcontractOrderCode}]当前状态[{currentStatus}]不允许红冲，仅 COMPLETED 且已过账的委外单可红冲",
            ARG_SUBCONTRACT_ORDER_CODE, ARG_CURRENT_STATUS);
}
