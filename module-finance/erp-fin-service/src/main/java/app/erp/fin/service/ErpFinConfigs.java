package app.erp.fin.service;

public interface ErpFinConfigs {

    /** E3.5 文档摄取管道开关（document-driven-ap-automation.md 落地策略）：默认 false，向后兼容。 */
    String CONFIG_AP_DOC_PIPELINE_ENABLED = "erp-fin.ap-doc-pipeline-enabled";
    boolean DEFAULT_AP_DOC_PIPELINE_ENABLED = false;

    /** E3.5 分类置信度阈值：低于该值挂人工复核队列（人工门，AP-1）。 */
    String CONFIG_AP_DOC_CONFIDENCE_THRESHOLD = "erp-fin.ap-doc-confidence-threshold";
    double DEFAULT_AP_DOC_CONFIDENCE_THRESHOLD = 0.7;

    /** E3.5 草稿发票缺省币种（解析结果未识别币种时使用）。 */
    String CONFIG_AP_DOC_DEFAULT_CURRENCY_ID = "erp-fin.ap-doc-default-currency-id";
    String DEFAULT_AP_DOC_DEFAULT_CURRENCY_ID = "1";

    /** E3.5 单文档最大文件字节数（上传守卫）。 */
    String CONFIG_AP_DOC_MAX_FILE_LENGTH = "erp-fin.ap-doc-max-file-length";
    long DEFAULT_AP_DOC_MAX_FILE_LENGTH = 20L * 1024 * 1024;

    /** E3.6 护栏：管道上传入口限流（自动化批量面，IRateLimiter 令牌桶；0 = 不限流）。 */
    String CONFIG_AP_DOC_UPLOAD_RATE_LIMIT_RPS = "erp-fin.ap-doc-upload-rate-limit-rps";
    double DEFAULT_AP_DOC_UPLOAD_RATE_LIMIT_RPS = 10.0;
}
