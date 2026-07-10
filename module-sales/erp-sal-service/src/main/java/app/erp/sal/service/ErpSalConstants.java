package app.erp.sal.service;

/**
 * 销售域状态码常量。权威值来自 {@code module-sales/model/app-erp-sales.orm.xml}
 * 关联字典 {@code erp-sal/approve-status}、{@code erp-sal/doc-status}、{@code erp-sal/delivery-status}。
 *
 * <p>三轴状态分离见 {@code docs/design/sales/state-machine.md}（与采购域镜像对称）。
 */
public interface ErpSalConstants {

    // 审核轴 approve-status
    String APPROVE_STATUS_UNSUBMITTED = "UNSUBMITTED";
    String APPROVE_STATUS_SUBMITTED = "SUBMITTED";
    String APPROVE_STATUS_APPROVED = "APPROVED";
    String APPROVE_STATUS_REJECTED = "REJECTED";

    // 单据生命周期轴 doc-status
    String DOC_STATUS_DRAFT = "DRAFT";
    String DOC_STATUS_ACTIVE = "ACTIVE";
    String DOC_STATUS_CANCELLED = "CANCELLED";

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
}
