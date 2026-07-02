package app.erp.mfg.service;

import io.nop.api.core.exceptions.ErrorCode;

/**
 * 制造域错误码。描述为中文，框架经 i18n 翻译；公共/GraphQL 面向错误统一 {@link ErrorCode} + {@code NopException}。
 *
 * <p>权威：`docs/design/manufacturing/bom-and-routing.md`、`docs/plans/2026-07-02-1538-2-manufacturing-bom-routing-rollup.md`。
 */
public interface ErpMfgErrors {

    String ARG_BOM_ID = "bomId";
    String ARG_PRODUCT_ID = "productId";
    String ARG_MATERIAL_ID = "materialId";
    String ARG_DEPTH = "depth";
    String ARG_PATH = "path";

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
}
