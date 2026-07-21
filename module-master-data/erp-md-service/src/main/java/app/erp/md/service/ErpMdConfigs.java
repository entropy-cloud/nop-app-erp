package app.erp.md.service;

/**
 * 主数据域配置键常量。经 {@link io.nop.api.core.config.AppConfig#var} 读取。
 *
 * <p>键名对齐 {@code docs/architecture/external-api-integration-pattern.md §5.2}（D1）+
 * {@code docs/design/master-data/sku-multi-unit.md §配置项}（SUC）。
 */
public interface ErpMdConfigs {

    // ---- D1 汇率查询 API 客户端（plan 2026-07-21-1206-3）----
    // 对应 docs/architecture/external-api-integration-pattern.md §5.2 参考实现配置点。

    /** 汇率查询 API 客户端启用开关，默认 false（config-gated）。 */
    String CONFIG_EXCHANGE_RATE_API_ENABLED = "erp-md.exchange-rate-api-enabled";
    /** provider 切换（mock/exchangerate-host/...），默认 mock。 */
    String CONFIG_EXCHANGE_RATE_API_PROVIDER = "erp-md.exchange-rate-api-provider";
    /** API Key 配置（OAuth2 场景为 client_id），默认空。 */
    String CONFIG_EXCHANGE_RATE_API_KEY = "erp-md.exchange-rate-api-key";
    /** 每秒请求数限制（Nop {@code IRateLimiter} 令牌桶），默认 10。 */
    String CONFIG_EXCHANGE_RATE_API_RATE_LIMIT_RPS = "erp-md.exchange-rate-api-rate-limit-rps";
    /** 响应/token 缓存 TTL（秒），默认 300（5 分钟）。 */
    String CONFIG_EXCHANGE_RATE_API_CACHE_TTL_SECS = "erp-md.exchange-rate-api-cache-ttl-secs";

    boolean DEFAULT_EXCHANGE_RATE_API_ENABLED = false;
    String DEFAULT_EXCHANGE_RATE_API_PROVIDER = "mock";
    String DEFAULT_EXCHANGE_RATE_API_KEY = "";
    double DEFAULT_EXCHANGE_RATE_API_RATE_LIMIT_RPS = 10.0;
    int DEFAULT_EXCHANGE_RATE_API_CACHE_TTL_SECS = 300;
}
