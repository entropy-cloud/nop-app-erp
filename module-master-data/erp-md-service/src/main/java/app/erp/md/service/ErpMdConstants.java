package app.erp.md.service;

/**
 * 主数据域状态码常量。权威值来自 {@code module-master-data/model/app-erp-master-data.orm.xml}
 * 关联字典 {@code erp-md/active-status}、{@code erp-md/supplier-approval-status}、{@code erp-md/supplier-approval-type}。
 *
 * <p>SUC 配置项（{@code docs/design/master-data/sku-multi-unit.md §配置项}）经
 * {@link io.nop.api.core.config.AppConfig#var} 读取，缺失走默认，无 .env/外部服务。
 */
public interface ErpMdConstants {

    // 主数据启用状态 erp-md/active-status
    String ACTIVE_STATUS_ACTIVE = "ACTIVE";
    String ACTIVE_STATUS_INACTIVE = "INACTIVE";

    // ---- SKU 多单位/价格业务服务配置项（sku-multi-unit.md §配置项）----
    /** 是否要求每物料必有默认 SKU（默认 true）。 */
    String CONFIG_SKU_DEFAULT_REQUIRED = "erp-md.sku-default-required";
    /** 条码是否全局唯一（默认 true，应用层查重开关）。 */
    String CONFIG_SKU_BARCODE_UNIQUE = "erp-md.sku-barcode-unique";
    /** 创建物料时是否自动创建默认 SKU（默认 true，本计划仅声明配置键，自动创建逻辑归后续）。 */
    String CONFIG_SKU_AUTO_CREATE_DEFAULT = "erp-md.sku-auto-create-default";
    /** 单位换算严格模式（默认 true：未命中系数即报错；false：回退 SKU.conversionRate）。 */
    String CONFIG_UOM_CONVERSION_STRICT = "erp-md.uom-conversion-strict";

    /** 数量换算 BigDecimal 输出小数位（HALF_UP）。 */
    int UOM_CONVERSION_SCALE = 4;

    // ---- 价格校验级别字典值（erp-md/price-validation，string）----
    /** 不校验。 */
    String PRICE_VALIDATION_OFF = "OFF";
    /** 警告放行（不阻断）。 */
    String PRICE_VALIDATION_WARN = "WARN";
    /** 强制拦截（低于底线抛错）。 */
    String PRICE_VALIDATION_HARD = "HARD";

    // ---- 单据类型 → SKU 默认价格档（UC-MD-03 billType 参数权威编码）----
    /** 采购单据 → purchasePrice。 */
    String BILL_TYPE_PURCHASE = "PURCHASE";
    /** 批发单据 → wholesalePrice。 */
    String BILL_TYPE_WHOLESALE = "WHOLESALE";
    /** 零售单据 → retailPrice。 */
    String BILL_TYPE_RETAIL = "RETAIL";
    /** 默认（其他）→ salePrice。 */
    String BILL_TYPE_DEFAULT = "DEFAULT";

    // AVL 准入状态 erp-md/supplier-approval-status（APPLIED→APPROVED→PROBATION→SUSPENDED→REJECTED）
    String APPROVAL_STATUS_APPLIED = "APPLIED";
    String APPROVAL_STATUS_APPROVED = "APPROVED";
    String APPROVAL_STATUS_PROBATION = "PROBATION";
    String APPROVAL_STATUS_SUSPENDED = "SUSPENDED";
    String APPROVAL_STATUS_REJECTED = "REJECTED";

    // AVL 准入类型 erp-md/supplier-approval-type
    String APPROVAL_TYPE_NEW = "NEW";
    String APPROVAL_TYPE_RENEWAL = "RENEWAL";
}
