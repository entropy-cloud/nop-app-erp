package app.erp.sal.service;

import app.erp.sal.dao.constants.ErpSalDocStatus;

/**
 * 销售域状态码常量。权威值来自 {@code module-sales/model/app-erp-sales.orm.xml}
 * 关联字典 {@code wf/approve-status}、{@code erp-sal/doc-status}、{@code erp-sal/delivery-status}。
 *
 * <p>三轴状态分离见 {@code docs/design/sales/state-machine.md}（与采购域镜像对称）。
 *
 * <p>{@code extends ErpSalDocStatus} 复用 dao 层常量定义，保持 approve-status / doc-status 单一真相源；
 * 本接口仅追加 service 层独有的派生状态与配置项。
 */
public interface ErpSalConstants extends ErpSalDocStatus {

    // 发货进度（派生）delivery-status
    String DELIVERY_STATUS_UNDELIVERED = "UNDELIVERED";
    String DELIVERY_STATUS_PARTIAL = "PARTIAL";
    String DELIVERY_STATUS_DELIVERED = "DELIVERED";

    // 收款进度（派生）received-status：销售发票的收款进度，收款单的核销状态复用本字典（对齐采购域 paid-status）
    String RECEIVED_STATUS_UNRECEIVED = "UNRECEIVED";
    String RECEIVED_STATUS_PARTIAL = "PARTIAL";
    String RECEIVED_STATUS_RECEIVED = "RECEIVED";

    // 主数据启用状态 erp-md/active-status
    String PARTNER_STATUS_ACTIVE = "ACTIVE";

    // 库存作业类型（对齐 erp-inv/operation-type，调用方侧副本避免 main 代码依赖 inventory-service）
    String MOVE_TYPE_OUTGOING = "OUTGOING";
    String MOVE_TYPE_INCOMING = "INCOMING";

    // 出库联动标识（自由字符串，inventory 侧无字典约束）
    String RELATED_BILL_TYPE_SAL_DELIVERY = "ERP_SAL_DELIVERY";
    String RELATED_BILL_TYPE_SAL_RETURN = "ERP_SAL_RETURN";
    String RELATED_BILL_TYPE_REVERSAL = "REVERSAL";

    // 信用额度控制配置项 erp-sal.credit-check-level（默认 SOFT_WARNING）
    String CONFIG_CREDIT_CHECK_LEVEL = "erp-sal.credit-check-level";
    String CREDIT_CHECK_LEVEL_SOFT_WARNING = "SOFT_WARNING";
    String CREDIT_CHECK_LEVEL_SPECIAL_APPROVAL = "SPECIAL_APPROVAL";
    String CREDIT_CHECK_LEVEL_HARD_BLOCK = "HARD_BLOCK";

    // 订单级可用量预校验配置项 erp-sal.order-availability-check-level（默认 OFF；RC-R1.13，对齐 credit-check-level 三级范式：
    // OFF 关闭 / WARN 不足记告警放行 / HARD 不足拒绝审核；出库审核仍是强制校验点，本配置仅作可选前置预校验）
    String CONFIG_ORDER_AVAILABILITY_CHECK_LEVEL = "erp-sal.order-availability-check-level";
    String ORDER_AVAILABILITY_CHECK_LEVEL_OFF = "OFF";
    String ORDER_AVAILABILITY_CHECK_LEVEL_WARN = "WARN";
    String ORDER_AVAILABILITY_CHECK_LEVEL_HARD = "HARD";

    // 出库审核环节是否启用信用冻结检查（默认 false，向后兼容；plan 2026-07-10-1100-2）
    String CONFIG_CREDIT_CHECK_ON_DELIVERY = "erp-sal.credit-check-on-delivery";
    boolean CREDIT_CHECK_ON_DELIVERY_DEFAULT = false;

    // 发票审核环节是否启用信用冻结检查（默认 false，向后兼容；plan 2026-07-10-1100-2）
    String CONFIG_CREDIT_CHECK_ON_INVOICE = "erp-sal.credit-check-on-invoice";
    boolean CREDIT_CHECK_ON_INVOICE_DEFAULT = false;

    // 信用冻结检查的单据类型（区分错误消息来源，非字典值）
    String BILL_TYPE_ORDER = "ORDER";
    String BILL_TYPE_DELIVERY = "DELIVERY";
    String BILL_TYPE_INVOICE = "INVOICE";

    // 信用控制是否纳入 AR 未核销余额（默认 true，关闭时回退纯订单口径）
    String CONFIG_CREDIT_CHECK_INCLUDE_AR = "erp-sal.credit-check-include-ar";
    boolean CREDIT_CHECK_INCLUDE_AR_DEFAULT = true;

    // AR 未核销余额本位币字段缺失时的近似折算容错开关（默认 true）
    String CONFIG_CREDIT_CHECK_AR_FALLBACK = "erp-sal.credit-check-ar-fallback";
    boolean CREDIT_CHECK_AR_FALLBACK_DEFAULT = true;

    // 超信用额度专项审批权限键（SPECIAL_APPROVAL 级别经权限门控实现）
    String PERM_CREDIT_OVER_LIMIT_APPROVE = "erp-sal:creditOverLimitApprove";

    /** 信用额度超限告警通知派发开关（默认 true；plan 2026-07-06-0642-1 §Phase 1）。关闭时跳过 notify 调用。 */
    String CONFIG_CREDIT_NOTIFY_ENABLED = "erp-sal.credit-notify-enabled";

    /** 通知事件类型：信用额度超限（对应 erp_sys_notification_template.notification_type）。 */
    String NOTIFY_EVENT_CREDIT_OVER_LIMIT = "sal.credit-over-limit";

    // 退货配置项（returns.md §配置项，缺失走默认，无需 .env）
    String CONFIG_RETURN_REASON_REQUIRED = "erp-sal.return-reason-required";
    String CONFIG_RETURN_APPROVAL_REQUIRED = "erp-sal.return-approval-required";

    // 退货入库成本策略配置项 erp-sal.return-cost-method（UC-SAL-07，P1-RC-026）：
    // original=按原出库成本（行 unitPrice，默认）/ current=按当前库存成本（库存域 avgCost，缺失回退 unitPrice+LOG.warn）
    // / agreement=按退货协议价（行 unitPrice 协议价语义）。三策略由 ReturnStockMoveBuilder.buildLines 与
    // SalReturnPostingDispatcher.computeTotalCost 同源消费（同一 ReturnCostStrategyResolver）。
    String CONFIG_RETURN_COST_METHOD = "erp-sal.return-cost-method";
    String RETURN_COST_METHOD_ORIGINAL = "original";
    String RETURN_COST_METHOD_CURRENT = "current";
    String RETURN_COST_METHOD_AGREEMENT = "agreement";

    // 会计期间状态常量（UC-SAL-09 期间 CLOSED 守卫，P1-RC-028；镜像 finance/assets 域字面值，避免跨 service 依赖）
    String PERIOD_STATUS_OPEN = "OPEN";
    String PERIOD_STATUS_CLOSING = "CLOSING";
    String PERIOD_STATUS_CLOSED = "CLOSED";
    String PERIOD_STATUS_CLOSED_FINAL = "CLOSED_FINAL";
    String PERIOD_STATUS_NEVER_OPENED = "NEVER_OPENED";

    // 退货类型（erp-sal/return-type，RC-R1.51 P1-RC-025；defaultValue=RETURN 既有退货零行为变化）
    String RETURN_TYPE_RETURN = "RETURN";
    String RETURN_TYPE_EXCHANGE = "EXCHANGE";

    // ---- 看板预警阈值配置项（dashboards.md §实现约定 §5，经 AppConfig.var 读取，NopSysVariable 可运行时覆盖）----
    /** 应收超期预警天数（账龄 > 此值触发）；默认 0=关闭预警。 */
    String CONFIG_DASH_SAL_AR_OVERDUE_DAYS = "erp-dash.sal-ar-overdue-days";
    int DEFAULT_DASH_SAL_AR_OVERDUE_DAYS = 0;
    /** 应收超期预警金额（openAmount > 此值触发）；默认 0=关闭预警。 */
    String CONFIG_DASH_SAL_AR_OVERDUE_AMOUNT = "erp-dash.sal-ar-overdue-amount";
    java.math.BigDecimal DEFAULT_DASH_SAL_AR_OVERDUE_AMOUNT = java.math.BigDecimal.ZERO;

    // ---- 定价引擎配置项（UC-SAL-11）----
    /** 订单保存时是否自动应用促销规则（默认 true）。 */
    String CONFIG_AUTO_PRICING_ON_SAVE = "erp-sal.auto-pricing-on-save";
    /** 促销规则默认是否可叠加（默认 false）。 */
    String CONFIG_PRICING_RULE_STACK_DEFAULT = "erp-sal.pricing-rule-stack-default";

    // ---- 取价来源标记（pricingSource 字段权威编码，UC-SAL-11）----
    String PRICING_SOURCE_MANUAL = "MANUAL";
    String PRICING_SOURCE_PRICE_LIST = "PRICE_LIST";
    String PRICING_SOURCE_PROMOTION = "PROMOTION";
    String PRICING_SOURCE_SKU_DEFAULT = "SKU_DEFAULT";

    // ---- 合同量折扣消费（RC-R1.79 / P1-RC-078，UC-CT-08 A，volume-discount.md §折扣应用）----
    /** 消费门控（D3 裁决）：默认 true 对齐 erp-ct.volume-discount-enabled；false 时 ctContractLineId 仅存储不应用。 */
    String CONFIG_CT_DISCOUNT_ENABLED = "erp-sal.ct-discount-enabled";
    /** 取价来源：合同量折扣（显式合同行引用优先于促销/目录价，优先级语义落 pricingSource 标记）。 */
    String PRICING_SOURCE_CT_VOLUME_DISCOUNT = "CT_VOLUME_DISCOUNT";
}
