package app.erp.b2b.service;

/**
 * B2B/EDI 域配置键常量。经 {@code AppConfig.var(...)} 读取，键名对齐 {@code asn-processing.md §九} + {@code managed-file-transfer.md}。
 */
public interface ErpB2bConfigs {

    /** B2B 集成模块是否启用，默认 false。 */
    String CONFIG_B2B_ENABLED = "erp-b2b.enabled";
    /** ASN 采购订单匹配重试间隔（分钟），默认 30。 */
    String CONFIG_ASN_AUTO_MATCH_RETRY_INTERVAL = "erp-b2b.asn.auto-match-retry-interval";
    /** ASN 匹配超时（小时），超时后升级通知，默认 48。 */
    String CONFIG_ASN_MATCH_TIMEOUT_HOURS = "erp-b2b.asn.match-timeout-hours";
    /** 是否允许部分收货，默认 true。 */
    String CONFIG_ASN_PARTIAL_RECEIPT_ENABLED = "erp-b2b.asn.partial-receipt-enabled";
    /** ASN→采购入库自动创建（核心零污染门控），默认 false。 */
    String CONFIG_ASN_AUTO_CREATE_RECEIVE = "erp-b2b.asn-auto-create-receive";
    /** 传输模式：AUTO（自动发送）/ MANUAL（手动发送），默认 MANUAL。 */
    String CONFIG_TRANSPORT_MODE = "erp-b2b.transport-mode";
    /** MFT 最大重试次数，默认 3。 */
    String CONFIG_MFT_MAX_RETRIES = "erp-b2b.mft-max-retries";
    /** webhook 签名是否必填，默认 true。 */
    String CONFIG_WEBHOOK_SIGNATURE_REQUIRED = "erp-b2b.webhook-signature-required";
    /** EDI ERROR 是否阻断业务单据流转，默认 false。 */
    String CONFIG_ERROR_BLOCKS_FLOW = "erp-b2b.error-blocks-flow";

    boolean DEFAULT_B2B_ENABLED = false;
    int DEFAULT_ASN_AUTO_MATCH_RETRY_INTERVAL = 30;
    int DEFAULT_ASN_MATCH_TIMEOUT_HOURS = 48;
    boolean DEFAULT_ASN_PARTIAL_RECEIPT_ENABLED = true;
    boolean DEFAULT_ASN_AUTO_CREATE_RECEIVE = false;
    String TRANSPORT_MODE_AUTO = "AUTO";
    String TRANSPORT_MODE_MANUAL = "MANUAL";
    String DEFAULT_TRANSPORT_MODE = TRANSPORT_MODE_MANUAL;
    int DEFAULT_MFT_MAX_RETRIES = 3;
    boolean DEFAULT_WEBHOOK_SIGNATURE_REQUIRED = true;
    boolean DEFAULT_ERROR_BLOCKS_FLOW = false;
}
