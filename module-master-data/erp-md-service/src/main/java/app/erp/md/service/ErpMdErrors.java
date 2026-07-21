package app.erp.md.service;

import app.erp.md.service.daterange.ErpDateRangeOverlapValidator;
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

    // ---------- O-11 扩展：主数据/物料/单位/价格/伙伴等细粒度错误码 ----------

    ErrorCode ERR_MATERIAL_NOT_FOUND = ErrorCode.define(
            "erp.err.md.material.not-found",
            "物料[{materialId}]不存在",
            ARG_MATERIAL_ID);

    ErrorCode ERR_MATERIAL_CODE_DUPLICATE = ErrorCode.define(
            "erp.err.md.material.code-duplicate",
            "物料编码[{materialCode}]已存在",
            "materialCode");

    ErrorCode ERR_MATERIAL_STATUS_INVALID = ErrorCode.define(
            "erp.err.md.material.status-invalid",
            "物料[{materialId}]当前状态不允许该操作（status={currentStatus}）",
            ARG_MATERIAL_ID, ARG_CURRENT_STATUS);

    ErrorCode ERR_PARTNER_NOT_FOUND = ErrorCode.define(
            "erp.err.md.partner.not-found",
            "往来单位[{partnerId}]不存在",
            ARG_PARTNER_ID);

    ErrorCode ERR_PARTNER_CODE_DUPLICATE = ErrorCode.define(
            "erp.err.md.partner.code-duplicate",
            "往来单位编码[{partnerCode}]已存在",
            "partnerCode");

    ErrorCode ERR_SUBJECT_NOT_FOUND = ErrorCode.define(
            "erp.err.md.subject.not-found",
            "会计科目编码[{subjectCode}]不存在",
            "subjectCode");

    ErrorCode ERR_SUBJECT_INACTIVE = ErrorCode.define(
            "erp.err.md.subject.inactive",
            "会计科目[{subjectCode}]已停用，不可用于过账",
            "subjectCode");

    ErrorCode ERR_UOM_NOT_FOUND = ErrorCode.define(
            "erp.err.md.uom.not-found",
            "计量单位[{uomId}]不存在",
            "uomId");

    ErrorCode ERR_CURRENCY_NOT_FOUND = ErrorCode.define(
            "erp.err.md.currency.not-found",
            "币种[{currencyId}]不存在",
            "currencyId");

    ErrorCode ERR_ORG_NOT_FOUND = ErrorCode.define(
            "erp.err.md.org.not-found",
            "组织[{orgId}]不存在",
            "orgId");

    // --- 统一 Party 身份查询（C1，docs/design/master-data/unified-party-identity.md）---

    String ARG_PARTY_TYPE = "partyType";
    String ARG_PARTY_ID = "partyId";

    ErrorCode ERR_PARTY_NOT_FOUND = ErrorCode.define(
            "erp.err.md.party.not-found",
            "Party[{partyType}/{partyId}]不存在",
            ARG_PARTY_TYPE, ARG_PARTY_ID);

    // --- C2 跨境贸易扩展（docs/design/master-data/cross-border-trade.md §3 ErpMdMaterialCustoms） ---

    String ARG_DECLARATION_NO = "declarationNo";
    String ARG_PARTNER_TYPE = "partnerType";

    /** ErpMdMaterialCustoms.partnerId 引用的 Partner 类型非 CUSTOMS_BROKER。 */
    ErrorCode ERR_PARTNER_NOT_CUSTOMS_BROKER = ErrorCode.define(
            "erp.err.md.partner.not-customs-broker",
            "报关行[{partnerId}]的类型[{partnerType}]不是 CUSTOMS_BROKER，不可作为报关记录的报关行",
            ARG_PARTNER_ID, ARG_PARTNER_TYPE);

    /** ErpMdMaterialCustoms.declarationNo 重复（DB UK 前置友好校验，避免 stack trace 暴露）。 */
    ErrorCode ERR_CUSTOMS_DECLARATION_NO_DUPLICATE = ErrorCode.define(
            "erp.err.md.customs.declaration-no-duplicate",
            "报关单号[{declarationNo}]已存在，不允许重复",
            ARG_DECLARATION_NO);

    /** ErpMdMaterialCustoms.sourceBillType/sourceBillCode 业务回链必填校验。 */
    ErrorCode ERR_CUSTOMS_SOURCE_BILL_REQUIRED = ErrorCode.define(
            "erp.err.md.customs.source-bill-required",
            "报关记录必须填写业务单据类型(sourceBillType)或业务单据编码(sourceBillCode)至少一项",
            ARG_DECLARATION_NO);

    // --- D1 外部 API 集成参考实现：汇率查询 API 客户端（plan 2026-07-21-1206-3）---
    // 对应 docs/architecture/external-api-integration-pattern.md §7.3 案例 C。

    String ARG_BASE_CURRENCY = "baseCurrency";
    String ARG_PROVIDER = "provider";
    String ARG_RATE_LIMIT_RPS = "rateLimitRps";

    /** API 不可达（网络错误 / 熔断 / config-gated 未启用）。 */
    ErrorCode ERR_EXCHANGE_RATE_API_UNAVAILABLE = ErrorCode.define(
            "erp.err.md.exchange-rate-api.unavailable",
            "汇率查询 API 不可用（provider={provider}, baseCurrency={baseCurrency}）—— 请检查 erp-md.exchange-rate-api-enabled 是否启用 + provider 配置是否正确",
            ARG_PROVIDER, ARG_BASE_CURRENCY);

    /** 限流触发（超过 erp-md.exchange-rate-api-rate-limit-rps）。 */
    ErrorCode ERR_EXCHANGE_RATE_API_RATE_LIMITED = ErrorCode.define(
            "erp.err.md.exchange-rate-api.rate-limited",
            "汇率查询 API 限流触发（provider={provider}, rateLimitRps={rateLimitRps}）—— 请降低调用频率或调高 erp-md.exchange-rate-api-rate-limit-rps 配置",
            ARG_PROVIDER, ARG_RATE_LIMIT_RPS);

    /** 响应格式错误（JSON 解析失败 / 缺必需字段）。 */
    ErrorCode ERR_EXCHANGE_RATE_API_RESPONSE_INVALID = ErrorCode.define(
            "erp.err.md.exchange-rate-api.response-invalid",
            "汇率查询 API 响应格式错误（provider={provider}, baseCurrency={baseCurrency}）—— 响应非 JSON 或缺必需字段",
            ARG_PROVIDER, ARG_BASE_CURRENCY);

    // --- C3 日期范围有效性模式（docs/design/date-ranged-validity-pattern.md §6）---
    // 试点 master-data 域通用错误码；其他域接入时按域前缀独立定义，不复用此码。
    // ARG_* 参数键常量集中于 ErpDateRangeOverlapValidator（跨域复用，不在此重复声明）。

    /** 「同维度互斥」重叠校验失败（friendly pre-save check，避免 DB 无 UK 兜底导致数据不一致）。 */
    ErrorCode ERR_MD_DATE_RANGE_OVERLAP = ErrorCode.define(
            "erp.err.md.date-range.overlap",
            "{entityName} 在区间 [validFrom={validFrom}, validTo={validTo}] 与既有记录(id={conflictId}) 重叠，同维度互斥策略不允许",
            ErpDateRangeOverlapValidator.ARG_ENTITY_NAME,
            ErpDateRangeOverlapValidator.ARG_VALID_FROM,
            ErpDateRangeOverlapValidator.ARG_VALID_TO,
            ErpDateRangeOverlapValidator.ARG_CONFLICT_ID);
}
