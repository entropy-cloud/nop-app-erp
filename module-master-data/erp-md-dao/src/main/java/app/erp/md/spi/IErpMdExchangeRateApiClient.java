package app.erp.md.spi;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/**
 * 汇率查询第三方 API 客户端 SPI（D1 参考实现）。
 *
 * <p>本 SPI 是 {@code docs/architecture/external-api-integration-pattern.md §7.3} 案例 C 的落地。
 * 与 logistics Carrier Gateway（Client/Factory/Registry 三层）+ b2b EDI Format（Provider/Registry 两层）
 * 是参考模式异构实现 —— 本 SPI 仅一个动作（{@link #fetchRates}）+ 单 provider per 配置切换
 * （非多 provider 并存），故无 Registry 层。
 *
 * <p>实现方在 {@code erp-md-service} 注册为 bean，由 {@code ErpMdExchangeRateApiClientFactory}
 * 按 {@code erp-md.exchange-rate-api-provider} 配置切换；内置 Nop Platform
 * {@code io.nop.commons.concurrent.ratelimit.IRateLimiter}（令牌桶限流，platform-first）。
 *
 * <p>幂等性：相同 {@code (baseCurrency, targetCurrencies, asOfDate)} 组合在缓存 TTL 内返回相同结果
 * （{@code ErpMdExchangeRateApiClientFactory} 内置缓存）。
 */
public interface IErpMdExchangeRateApiClient {

    /**
     * 拉取指定基准币种到目标币种集合在指定日期的汇率。
     *
     * @param baseCurrency     基准币种代码（ISO 4217，如 {@code "USD"}），不可空。
     * @param targetCurrencies 目标币种代码集合（ISO 4217，如 {@code ["CNY","EUR"]}），不可空。
     * @param asOfDate         汇率日期；{@code null} 表示当日。
     * @return targetCurrency → rate 映射；不可空；任一目标币种无可用汇率时缺该键（不抛异常）。
     *         rate 语义：1 单位 {@code baseCurrency} = rate 单位 {@code targetCurrency}。
     * @throws app.erp.md.service.ErpMdErrors#ERR_EXCHANGE_RATE_API_UNAVAILABLE
     *         API 不可达（熔断/网络错误/未启用 config-gated）。
     * @throws app.erp.md.service.ErpMdErrors#ERR_EXCHANGE_RATE_API_RATE_LIMITED
     *         限流触发（{@code erp-md.exchange-rate-api-rate-limit-rps}）。
     * @throws app.erp.md.service.ErpMdErrors#ERR_EXCHANGE_RATE_API_RESPONSE_INVALID
     *         响应格式错误（JSON 解析失败 / 缺必需字段）。
     */
    Map<String, BigDecimal> fetchRates(String baseCurrency, Set<String> targetCurrencies, LocalDate asOfDate);
}
