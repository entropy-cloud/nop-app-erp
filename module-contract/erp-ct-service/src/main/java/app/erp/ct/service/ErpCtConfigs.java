package app.erp.ct.service;

/**
 * 合同域配置项常量。配置键权威定义见 {@code docs/design/contract/volume-discount.md §配置点}
 * 与 {@code docs/design/contract/state-machine.md}。
 *
 * <p>读取方式：{@code AppConfig.var(KEY, defaultValue)}。缺失走默认，无需 .env。
 *
 * <p>键命名约定：与 {@code volume-discount.md §配置点} 既有键对齐合并，
 * 不重复定义同名异键；本接口仅新增 owner-doc 未覆盖的键。
 */
public interface ErpCtConfigs {

    // --- owner-doc §配置点 既有键（docs/design/contract/volume-discount.md） ---

    /** 批量折扣是否启用。默认 true。 */
    String CFG_VOLUME_DISCOUNT_ENABLED = "erp-ct.volume-discount-enabled";

    /** 年度返利协议是否启用。默认 false（owner-doc 默认值）。 */
    String CFG_REBATE_ENABLED = "erp-ct.rebate-enabled";

    /** 协议到期是否自动触发结算。默认 true。 */
    String CFG_REBATE_AUTO_SETTLE = "erp-ct.rebate-auto-settle";

    /** 默认计提方法（PERIOD_END / PROGRESSIVE）。默认 PERIOD_END。 */
    String CFG_REBATE_ACCRUAL_METHOD = "erp-ct.rebate-accrual-method";

    // --- 本计划新增键（owner-doc §配置点 未覆盖） ---

    /** InvoicePlan 到期是否自动批量触发。默认 true。里程碑/完工条款仍需人工确认（单点入口不变）。 */
    String CFG_INVOICEPLAN_AUTO_TRIGGER = "erp-ct.invoiceplan-auto-trigger";

    /** PROGRESSIVE 计提法跨档时是否逐档追溯补差。默认 true。false=仅按当前累计所在档计提（不追溯）。 */
    String CFG_REBATE_PROGRESSIVE_RETRO_TOPUP = "erp-ct.rebate-progressive-retro-topup";

    /** 结算模式（AUTO / MANUAL）。AUTO=计提后自动生成结算单草稿；MANUAL=需人工新建。默认 AUTO。 */
    String CFG_SETTLEMENT_MODE = "erp-ct.settlement-mode";

    // --- 电子签章（plan 2026-07-04-2200-2，design e-signature.md） ---

    /** 电子签章是否启用。默认 false（未启用走线下签署附件上传 + 手动确认 SIGNED）。 */
    String CFG_E_SIGNATURE_ENABLED = "erp-ct.e-signature-enabled";

    /** 默认签章提供商编码（对应 dict erp-ct/sign-provider 的 code）。默认 MOCK（本期 stub）。 */
    String CFG_SIGNATURE_DEFAULT_PROVIDER = "erp-ct.signature-default-provider";

    /** webhook 回调是否强制签名校验。默认 true。 */
    String CFG_SIGNATURE_CALLBACK_SIGNATURE_REQUIRED = "erp-ct.signature-callback-signature-required";

    /** 主动轮询 cron 表达式（注册归部署 Non-Goal，仅作为配置项）。默认每 2 小时。 */
    String CFG_SIGNATURE_STATUS_POLLING_CRON = "erp-ct.signature-status-polling-cron";

    /** 签署截止默认天数（initSignatureRequest 未传 signingDeadline 时使用）。默认 15。 */
    String CFG_SIGNATURE_DEADLINE_DEFAULT_DAYS = "erp-ct.signature-deadline-default-days";

    String DEFAULT_SIGNATURE_DEFAULT_PROVIDER = "MOCK";
    boolean DEFAULT_SIGNATURE_CALLBACK_SIGNATURE_REQUIRED = true;
    String DEFAULT_SIGNATURE_STATUS_POLLING_CRON = "0 0 */2 * * ?";
    int DEFAULT_SIGNATURE_DEADLINE_DEFAULT_DAYS = 15;
}
