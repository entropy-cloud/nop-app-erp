package app.erp.md.service.exchange;

import app.erp.md.service.ErpMdConfigs;
import app.erp.md.service.ErpMdErrors;
import app.erp.md.spi.IErpMdExchangeRateApiClient;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.commons.concurrent.ratelimit.DefaultRateLimiter;
import io.nop.commons.concurrent.ratelimit.IRateLimiter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 汇率查询 API 客户端工厂（D1 参考实现）。
 *
 * <p>本工厂是 {@code docs/architecture/external-api-integration-pattern.md §7.3 案例 C} 的 Factory 层。
 * 职责：
 * <ol>
 *   <li>按 {@code erp-md.exchange-rate-api-provider} config 切换实现（默认 mock）；</li>
 *   <li>内置 Nop Platform {@link IRateLimiter}（令牌桶限流，platform-first）；</li>
 *   <li>内置 TTL 缓存（同 baseCurrency+targetCurrencies+asOfDate 在 TTL 内返回缓存）；</li>
 *   <li>对外暴露 {@link #fetchRates} 直接合并限流 + 缓存 + 派发。</li>
 * </ol>
 *
 * <p><b>与 logistics 范式异构性</b>：logistics 是 Client/Factory/Registry 三层（多 provider 并存），
 * 本工厂是 Client/Factory 两层（单 provider per 配置切换，非并存），故无 Registry 层。
 *
 * <p><b>Cache 实现</b>：用 {@link ConcurrentHashMap} 简单 TTL 缓存（响应数据小 + TTL 短）；
 * 如需分布式缓存 successor 评估 Nop Platform {@code ICache} + Redis 后端。
 */
public class ErpMdExchangeRateApiClientFactory {

    /** 单 provider per 配置切换的 client 实例（按 providerCode 缓存）。 */
    private final Map<String, IErpMdExchangeRateApiClient> clients = new ConcurrentHashMap<>();

    /** per-provider 限流器（默认 provider 一个 IRateLimiter 实例）。 */
    private final Map<String, IRateLimiter> limiters = new ConcurrentHashMap<>();

    /** 响应缓存：cacheKey → (expiryTimeMillis, rates)。 */
    private final Map<String, CacheEntry> responseCache = new ConcurrentHashMap<>();

    /** 测试钩子：注入自定义 client（绕过 provider 切换）；{@code null} 走默认 provider 派发。 */
    private volatile IErpMdExchangeRateApiClient testClient = null;

    /**
     * 拉取汇率（合并限流 + 缓存 + 派发）。
     *
     * <p>调用顺序：检查启用 → 检查缓存 → 限流 → 派发到 provider client。
     */
    public Map<String, BigDecimal> fetchRates(String baseCurrency, Set<String> targetCurrencies, LocalDate asOfDate) {
        // 1. 检查启用（config-gated 默认关）；config 关闭时抛 ERR_EXCHANGE_RATE_API_UNAVAILABLE
        boolean enabled = AppConfig.var(ErpMdConfigs.CONFIG_EXCHANGE_RATE_API_ENABLED,
                ErpMdConfigs.DEFAULT_EXCHANGE_RATE_API_ENABLED);
        if (!enabled) {
            throw new NopException(ErpMdErrors.ERR_EXCHANGE_RATE_API_UNAVAILABLE)
                    .param(ErpMdErrors.ARG_PROVIDER, getProvider())
                    .param(ErpMdErrors.ARG_BASE_CURRENCY, baseCurrency);
        }

        // 2. 检查缓存（cacheKey = baseCurrency + sorted(targetCurrencies) + asOfDate）
        String cacheKey = buildCacheKey(baseCurrency, targetCurrencies, asOfDate);
        CacheEntry cached = responseCache.get(cacheKey);
        long now = System.currentTimeMillis();
        if (cached != null && cached.expiryTimeMillis > now) {
            return new LinkedHashMap<>(cached.rates);
        }

        // 3. 限流（IRateLimiter 令牌桶，platform-first）
        IRateLimiter limiter = getOrCreateLimiter();
        if (!limiter.tryAcquire()) {
            throw new NopException(ErpMdErrors.ERR_EXCHANGE_RATE_API_RATE_LIMITED)
                    .param(ErpMdErrors.ARG_PROVIDER, getProvider())
                    .param(ErpMdErrors.ARG_RATE_LIMIT_RPS, limiter.getPermitsPerSecond());
        }

        // 4. 派发到 provider client
        IErpMdExchangeRateApiClient client = resolveClient();
        Map<String, BigDecimal> rates = client.fetchRates(baseCurrency, targetCurrencies, asOfDate);
        if (rates == null) {
            rates = Collections.emptyMap();
        }

        // 5. 写入缓存
        int ttlSecs = AppConfig.var(ErpMdConfigs.CONFIG_EXCHANGE_RATE_API_CACHE_TTL_SECS,
                ErpMdConfigs.DEFAULT_EXCHANGE_RATE_API_CACHE_TTL_SECS);
        responseCache.put(cacheKey, new CacheEntry(now + ttlSecs * 1000L, new LinkedHashMap<>(rates)));

        return new LinkedHashMap<>(rates);
    }

    /** 当前配置的 provider 代码。 */
    public String getProvider() {
        return AppConfig.var(ErpMdConfigs.CONFIG_EXCHANGE_RATE_API_PROVIDER,
                ErpMdConfigs.DEFAULT_EXCHANGE_RATE_API_PROVIDER);
    }

    /** 测试钩子：注入自定义 client。 */
    public void setTestClient(IErpMdExchangeRateApiClient client) {
        this.testClient = client;
    }

    /** 测试钩子：清空缓存 + 限流统计 + 测试 client。 */
    public void resetTestState() {
        responseCache.clear();
        for (IRateLimiter l : limiters.values()) {
            l.resetStats();
        }
        testClient = null;
    }

    /** 暴露缓存大小（测试/诊断用）。 */
    public int getCacheSize() {
        return responseCache.size();
    }

    /** 暴露已注册 provider 集合（测试/诊断用）。 */
    public java.util.Set<String> getRegisteredProviders() {
        return new java.util.LinkedHashSet<>(clients.keySet());
    }

    // ---------- 内部方法 ----------

    private IErpMdExchangeRateApiClient resolveClient() {
        if (testClient != null) {
            return testClient;
        }
        String provider = getProvider();
        return clients.computeIfAbsent(provider, this::instantiateClient);
    }

    /**
     * 按 provider 代码实例化 client。
     *
     * <p>当前仅 {@code "mock"} 落地；真实 provider（{@code exchangerate-host}/{@code fixed-fetch}/...）
     * 触发 successor。未知 provider 抛 {@code ERR_EXCHANGE_RATE_API_UNAVAILABLE}。
     */
    private IErpMdExchangeRateApiClient instantiateClient(String provider) {
        if ("mock".equals(provider)) {
            return new MockExchangeRateApiClient();
        }
        throw new NopException(ErpMdErrors.ERR_EXCHANGE_RATE_API_UNAVAILABLE)
                .param(ErpMdErrors.ARG_PROVIDER, provider)
                .param(ErpMdErrors.ARG_BASE_CURRENCY, "<unknown-provider>");
    }

    private IRateLimiter getOrCreateLimiter() {
        String provider = getProvider();
        return limiters.computeIfAbsent(provider, k -> {
            double rps = AppConfig.var(ErpMdConfigs.CONFIG_EXCHANGE_RATE_API_RATE_LIMIT_RPS,
                    ErpMdConfigs.DEFAULT_EXCHANGE_RATE_API_RATE_LIMIT_RPS);
            return DefaultRateLimiter.create(rps);
        });
    }

    private String buildCacheKey(String baseCurrency, Set<String> targetCurrencies, LocalDate asOfDate) {
        // 排序目标币种保证 cacheKey 稳定
        java.util.List<String> sorted = new java.util.ArrayList<>(targetCurrencies);
        java.util.Collections.sort(sorted);
        return baseCurrency + "|" + String.join(",", sorted) + "|" + asOfDate;
    }

    /** 强制让 IoC 容器可以无参构造（默认构造）+ 测试可手动实例化。 */
    public ErpMdExchangeRateApiClientFactory() {
    }

    /** 缓存条目。 */
    private static final class CacheEntry {
        final long expiryTimeMillis;
        final Map<String, BigDecimal> rates;

        CacheEntry(long expiryTimeMillis, Map<String, BigDecimal> rates) {
            this.expiryTimeMillis = expiryTimeMillis;
            this.rates = rates;
        }
    }
}
