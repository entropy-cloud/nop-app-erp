package app.erp.log.service;

/**
 * 物流域配置键常量。经 {@code AppConfig.var(...)} 读取，键名对齐 {@code carrier-integration.md §八 配置点汇总}。
 */
public interface ErpLogConfigs {

    /** 网关调用超时（秒），默认 30，范围 5–120。 */
    String CONFIG_GATEWAY_TIMEOUT_SECS = "erp-log.gateway-timeout-secs";
    /** 网关失败最大重试次数，默认 3。 */
    String CONFIG_GATEWAY_MAX_RETRIES = "erp-log.gateway-max-retries";
    /** 重试间隔（秒，逗号分隔，指数退避序列），默认 {@code 30,120,600}。 */
    String CONFIG_RETRY_BASE_INTERVAL_SECS = "erp-log.retry-base-interval-secs";
    /** 追踪轮询 cron，默认 {@code 0 0 *&#47;4 * * ?}（每 4 小时）。 */
    String CONFIG_TRACKING_POLLING_CRON = "erp-log.tracking-poll-cron";
    /** 运费结算模式：AUTO（默认，DELIVERED 自动过账）/ MANUAL（仅标记待处理）。 */
    String CONFIG_SHIPMENT_SETTLEMENT_MODE = "erp-log.shipment-settlement-mode";
    /** webhook 签名是否必填，默认 true。 */
    String CONFIG_WEBHOOK_SIGNATURE_REQUIRED = "erp-log.webhook-signature-required";

    /** 销售运费费用科目编码（缺省回退），默认 6601 销售费用。 */
    String CONFIG_SALES_FREIGHT_EXPENSE_SUBJECT = "erp-log.sales-freight-expense-subject";

    String DEFAULT_SALES_FREIGHT_EXPENSE_SUBJECT = "6601";

    String SETTLEMENT_MODE_AUTO = "AUTO";
    String SETTLEMENT_MODE_MANUAL = "MANUAL";

    int DEFAULT_GATEWAY_TIMEOUT_SECS = 30;
    int DEFAULT_GATEWAY_MAX_RETRIES = 3;
    String DEFAULT_RETRY_INTERVAL_SECS = "30,120,600";
    String DEFAULT_TRACKING_POLLING_CRON = "0 0 */4 * * ?";
    boolean DEFAULT_WEBHOOK_SIGNATURE_REQUIRED = true;
}
