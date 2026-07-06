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

    // --- SKU 多单位/价格业务服务参数键 ---
    String ARG_SKU_ID = "skuId";
    String ARG_MATERIAL_ID = "materialId";
    String ARG_BARCODE = "barcode";
    String ARG_FROM_UOM_ID = "fromUoMId";
    String ARG_TO_UOM_ID = "toUoMId";
    String ARG_FINAL_PRICE = "finalPrice";
    String ARG_MIN_PRICE = "minPrice";
    String ARG_PRICE_VALIDATION_LEVEL = "priceValidationLevel";

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

    // --- SKU 多单位/价格业务服务（UC-MD-01~06，docs/design/master-data/use-cases.md） ---

    /** UC-MD-01：条码全局唯一校验失败（应用层查重，DB 唯一索引归 Deferred G1）。 */
    ErrorCode ERR_SKU_BARCODE_DUPLICATE = ErrorCode.define(
            "erp.err.md.sku.barcode-duplicate",
            "条码[{barcode}]已被其他 SKU 占用，不允许重复",
            ARG_BARCODE);

    /** UC-MD-05：无默认 SKU 且配置 sku-default-required=true。 */
    ErrorCode ERR_SKU_DEFAULT_REQUIRED = ErrorCode.define(
            "erp.err.md.sku.default-required",
            "物料[{materialId}]无默认 SKU 且配置 erp-md.sku-default-required=true，不可继续",
            ARG_MATERIAL_ID);

    /** UC-MD-02：单位换算系数未命中（strict 模式）。 */
    ErrorCode ERR_UOM_CONVERSION_NOT_FOUND = ErrorCode.define(
            "erp.err.md.uom.conversion-not-found",
            "物料[{materialId}]从单位[{fromUoMId}]到单位[{toUoMId}]未配置换算系数，且 uom-conversion-strict=true",
            ARG_MATERIAL_ID, ARG_FROM_UOM_ID, ARG_TO_UOM_ID);

    /** UC-MD-04：价格校验 HARD 级别低于底线。 */
    ErrorCode ERR_PRICE_BELOW_MIN = ErrorCode.define(
            "erp.err.md.price.below-min",
            "最终价[{finalPrice}]低于最低价底线[{minPrice}]，价格校验级别={priceValidationLevel} 拒绝",
            ARG_FINAL_PRICE, ARG_MIN_PRICE, ARG_PRICE_VALIDATION_LEVEL);

    /** UC-MD-06：不能停用/删除唯一默认 SKU。 */
    ErrorCode ERR_CANNOT_DEACTIVATE_DEFAULT_SKU = ErrorCode.define(
            "erp.err.md.sku.cannot-deactivate-default",
            "SKU[{skuId}]是物料[{materialId}]的唯一默认 SKU，不可停用/删除（请先设其他 SKU 为默认）",
            ARG_SKU_ID, ARG_MATERIAL_ID);

    /** UC-MD-06：SKU 被未完成单据引用，拒绝删除。 */
    ErrorCode ERR_SKU_REFERENCED_BY_BILL = ErrorCode.define(
            "erp.err.md.sku.referenced-by-bill",
            "SKU[{skuId}]被未完成业务单据引用，不可删除（请改停用）",
            ARG_SKU_ID);
}
