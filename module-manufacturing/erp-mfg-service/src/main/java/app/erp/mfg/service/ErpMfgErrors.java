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

    ErrorCode ERR_BOM_NOT_FOUND = ErrorCode.define(
            "nop.err.mfg.bom.not-found",
            "BOM不存在: {bomId}",
            ARG_BOM_ID);

    ErrorCode ERR_DEFAULT_BOM_NOT_FOUND = ErrorCode.define(
            "nop.err.mfg.bom.default-not-found",
            "物料[{productId}]不存在默认且有效的BOM",
            ARG_PRODUCT_ID);

    ErrorCode ERR_BOM_CYCLE = ErrorCode.define(
            "nop.err.mfg.bom.cycle",
            "BOM存在环引用（物料[{materialId}]在展开路径上重复出现）: {path}",
            ARG_MATERIAL_ID, ARG_PATH);

    ErrorCode ERR_BOM_MAX_DEPTH_EXCEEDED = ErrorCode.define(
            "nop.err.mfg.bom.max-depth-exceeded",
            "BOM展开深度超过上限{depth}（疑似环或层级过深）",
            ARG_DEPTH);

    ErrorCode ERR_ROLLUP_BASE_COST_MISSING = ErrorCode.define(
            "nop.err.mfg.rollup.base-cost-missing",
            "采购件[{materialId}]无默认SKU采购价，无法卷算基础成本，请先配置默认SKU的采购价",
            ARG_MATERIAL_ID);

    ErrorCode ERR_WORK_ORDER_NOT_FOUND = ErrorCode.define(
            "nop.err.mfg.work-order.not-found",
            "工单不存在: {workOrderId}",
            ARG_WORK_ORDER_ID);

    ErrorCode ERR_INVALID_STATUS_TRANSITION = ErrorCode.define(
            "nop.err.mfg.work-order.illegal-status-transition",
            "工单[{workOrderCode}]当前状态[{currentStatus}]不允许此操作，期望状态[{expectedStatus}]",
            ARG_WORK_ORDER_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_PARTIAL_KIT_START_FORBIDDEN = ErrorCode.define(
            "nop.err.mfg.work-order.partial-kit-start-forbidden",
            "工单[{workOrderCode}]齐套校验为部分齐套，配置erp-mfg.allow-partial-kit-start=false时不允许强制开工，请补料后重试",
            ARG_WORK_ORDER_CODE);

    ErrorCode ERR_JOB_CARD_NOT_FOUND = ErrorCode.define(
            "nop.err.mfg.job-card.not-found",
            "作业卡不存在: {jobCardId}",
            ARG_JOB_CARD_ID);

    ErrorCode ERR_INSPECTION_REQUIRED = ErrorCode.define(
            "nop.err.mfg.work-order.inspection-required",
            "工单[{workOrderCode}]BOM要求完工质检且erp-mfg.inspection-gate-enabled=true，完工入库暂挂等待质检结果",
            ARG_WORK_ORDER_CODE);

    ErrorCode ERR_OVER_REPORT = ErrorCode.define(
            "nop.err.mfg.work-order.over-report",
            "报工/完工数量[{completedQty}]超过工单计划数量[{plannedQty}]（未启用超产配置）",
            ARG_COMPLETED_QTY, ARG_PLANNED_QTY);

    ErrorCode ERR_ISSUE_LINES_EMPTY = ErrorCode.define(
            "nop.err.mfg.issue.lines-empty",
            "领料单[{workOrderCode}]无领料行，无法确认出库",
            ARG_WORK_ORDER_CODE);
}
