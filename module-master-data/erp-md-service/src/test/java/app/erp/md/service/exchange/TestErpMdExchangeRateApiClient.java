package app.erp.md.service.exchange;

import app.erp.md.biz.IErpMdCurrencyBiz;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdExchangeRate;
import app.erp.md.service.ErpMdConfigs;
import app.erp.md.service.ErpMdErrors;
import app.erp.md.spi.IErpMdExchangeRateApiClient;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * D1 外部 API 集成参考实现：汇率查询 API 客户端单元测试（plan 2026-07-21-1206-3 Phase 1）。
 *
 * <p>覆盖 5 场景（per plan §Phase 1 Proof 场景）：
 * <ol>
 *   <li>{@code testMockFetchReturnsDeterministicData}：Mock 实现 fetchRates 返回确定性数据（USD→CNY 7.20 等）。</li>
 *   <li>{@code testRateLimitingTriggersError}：连续调用触发 RATE_LIMITED 错误（IRateLimiter 令牌桶）。</li>
 *   <li>{@code testCacheReusesWithinTtl}：同 cacheKey 第二次调用走缓存（cacheSize 增长 = 1）。</li>
 *   <li>{@code testRefreshRatesFromApiWritesExchangeRate}：refreshRatesFromApi 端到端（fetchRates → 写入 ErpMdExchangeRate 表）。</li>
 *   <li>{@code testConfigGatedDefaultDisabled}：config-gated 默认 false 时 refreshRatesFromApi 抛 ERR_EXCHANGE_RATE_API_UNAVAILABLE。</li>
 * </ol>
 *
 * <p>测试方式：seed Currency + 启用 API config + 调 Factory.fetchRates / IErpMdCurrencyBiz.refreshRatesFromApi，
 * 断言返回值 / 异常 / 缓存 / DB 写入。
 *
 * <p>对应 {@code docs/architecture/external-api-integration-pattern.md §7.3 案例 C}。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMdExchangeRateApiClient extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpMdCurrencyBiz currencyBiz;
    @Inject
    ErpMdExchangeRateApiClientFactory factory;

    @BeforeEach
    void resetState() {
        // 默认启用 API（多数测试场景需要）；具体测试可覆盖
        AppConfig.getConfigProvider().assignConfigValue(
                ErpMdConfigs.CONFIG_EXCHANGE_RATE_API_ENABLED, Boolean.TRUE);
        AppConfig.getConfigProvider().assignConfigValue(
                ErpMdConfigs.CONFIG_EXCHANGE_RATE_API_PROVIDER,
                ErpMdConfigs.DEFAULT_EXCHANGE_RATE_API_PROVIDER);
        // 限流配置为 1 permit/sec（rate-limiting 测试场景需要）；其他测试场景可覆盖
        AppConfig.getConfigProvider().assignConfigValue(
                ErpMdConfigs.CONFIG_EXCHANGE_RATE_API_RATE_LIMIT_RPS, 1.0);
        AppConfig.getConfigProvider().assignConfigValue(
                ErpMdConfigs.CONFIG_EXCHANGE_RATE_API_CACHE_TTL_SECS, 300);
        MockExchangeRateApiClient.resetTestState();
        factory.resetTestState();
    }

    @AfterEach
    void cleanup() {
        // 测试结束恢复默认（关闭 API），避免污染其他测试
        AppConfig.getConfigProvider().assignConfigValue(
                ErpMdConfigs.CONFIG_EXCHANGE_RATE_API_ENABLED,
                ErpMdConfigs.DEFAULT_EXCHANGE_RATE_API_ENABLED);
        MockExchangeRateApiClient.resetTestState();
        factory.resetTestState();
    }

    // ============ 场景 1：Mock 实现 fetchRates 返回确定性数据 ============

    @Test
    public void testMockFetchReturnsDeterministicData() {
        // 直接构造 Mock（不经 Factory），验证 Mock 实现的确定性数据契约
        MockExchangeRateApiClient mock = new MockExchangeRateApiClient();
        Set<String> targets = new HashSet<>(Arrays.asList("CNY", "EUR", "JPY", "GBP"));
        Map<String, BigDecimal> rates = mock.fetchRates("USD", targets, LocalDate.of(2026, 7, 21));

        assertEquals(new BigDecimal("7.20"), rates.get("CNY"), "USD→CNY 应为 7.20（mock 确定性）");
        assertEquals(new BigDecimal("0.92"), rates.get("EUR"), "USD→EUR 应为 0.92");
        assertEquals(new BigDecimal("150.00"), rates.get("JPY"), "USD→JPY 应为 150.00");
        // GBP 不在 mock 表 → 缺该键（不抛异常）
        assertTrue(!rates.containsKey("GBP"), "USD→GBP mock 无数据，应缺键（部分成功语义）");
    }

    // ============ 场景 2：限流触发 RATE_LIMITED 错误 ============

    @Test
    public void testRateLimitingTriggersError() throws Exception {
        // rps=1：第一次调用成功，立即第二次应该被限流
        // 注意：tokens 在 1 秒内只补充 1 个；初始有 1 个令牌可用
        Set<String> targets = new HashSet<>(Collections.singletonList("CNY"));

        // 第一次：成功
        Map<String, BigDecimal> r1 = factory.fetchRates("USD", targets, LocalDate.of(2026, 7, 21));
        assertEquals(new BigDecimal("7.20"), r1.get("CNY"), "第一次调用应成功");

        // 第二次：应被限流（rps=1，无可用 token）
        // 注意：cache 会拦截重复 cacheKey，故换 baseCurrency 绕过缓存
        NopException ex = assertThrows(NopException.class, () ->
                factory.fetchRates("EUR", targets, LocalDate.of(2026, 7, 21)));
        assertEquals(ErpMdErrors.ERR_EXCHANGE_RATE_API_RATE_LIMITED.getErrorCode(), ex.getErrorCode(),
                "第二次调用应触发 RATE_LIMITED 错误");
    }

    // ============ 场景 3：缓存复用（同 cacheKey 走缓存，cacheSize 不增长） ============

    @Test
    public void testCacheReusesWithinTtl() throws Exception {
        // 调高 rps 避免限流干扰
        AppConfig.getConfigProvider().assignConfigValue(
                ErpMdConfigs.CONFIG_EXCHANGE_RATE_API_RATE_LIMIT_RPS, 100.0);
        factory.resetTestState();

        Set<String> targets = new HashSet<>(Arrays.asList("CNY", "EUR"));
        LocalDate asOf = LocalDate.of(2026, 7, 21);

        // 第一次：缓存空，走 fetch + 写缓存
        Map<String, BigDecimal> r1 = factory.fetchRates("USD", targets, asOf);
        int cacheSizeAfter1 = factory.getCacheSize();
        assertTrue(cacheSizeAfter1 >= 1, "第一次调用后缓存应有 1 条");

        // 第二次：同 cacheKey，应走缓存（不触发限流 acquire，不重复 fetch）
        Map<String, BigDecimal> r2 = factory.fetchRates("USD", targets, asOf);
        int cacheSizeAfter2 = factory.getCacheSize();
        assertEquals(cacheSizeAfter1, cacheSizeAfter2, "第二次同 cacheKey 不应新增缓存条目");
        assertEquals(r1, r2, "两次调用结果应一致（缓存复用）");
    }

    // ============ 场景 4：refreshRatesFromApi 端到端 ============

    @Test
    public void testRefreshRatesFromApiWritesExchangeRate() {
        // 提高限流避免干扰
        AppConfig.getConfigProvider().assignConfigValue(
                ErpMdConfigs.CONFIG_EXCHANGE_RATE_API_RATE_LIMIT_RPS, 100.0);
        factory.resetTestState();

        // seed 3 币种：USD（基准）+ CNY + EUR（目标）
        Long usdId = seedCurrency("USD-RT-1", "USD");
        Long cnyId = seedCurrency("CNY-RT-1", "CNY");
        Long eurId = seedCurrency("EUR-RT-1", "EUR");

        // 调 refreshRatesFromApi
        List<ErpMdExchangeRate> result = currencyBiz.refreshRatesFromApi("USD", CTX);

        // 断言：返回 2 条（USD→CNY + USD→EUR），GBP/JPY mock 不在主数据 → 跳过
        assertEquals(2, result.size(), "应写入 2 条汇率（CNY + EUR）");

        // 断言 DB 写入：查 ErpMdExchangeRate 表
        long count = ormTemplate.runInSession(session ->
                (long) daoProvider.daoFor(ErpMdExchangeRate.class).findAll().size());
        assertTrue(count >= 2, "ErpMdExchangeRate 表应有 ≥ 2 条记录");

        // 验证 rate 值正确
        Map<String, BigDecimal> byTarget = new LinkedHashMap<>();
        for (ErpMdExchangeRate r : result) {
            // 反查 target currency code
            ErpMdCurrency tc = daoProvider.daoFor(ErpMdCurrency.class).getEntityById(r.getToCurrencyId());
            byTarget.put(tc.getCode(), r.getRate());
        }
        assertEquals(new BigDecimal("7.20"), byTarget.get("CNY"), "USD→CNY rate 应为 7.20");
        assertEquals(new BigDecimal("0.92"), byTarget.get("EUR"), "USD→EUR rate 应为 0.92");
    }

    // ============ 场景 5：config-gated 默认 false 时抛 ERR_EXCHANGE_RATE_API_UNAVAILABLE ============

    @Test
    public void testConfigGatedDefaultDisabled() {
        // 显式关闭 API
        AppConfig.getConfigProvider().assignConfigValue(
                ErpMdConfigs.CONFIG_EXCHANGE_RATE_API_ENABLED, Boolean.FALSE);
        factory.resetTestState();

        // seed 至少一个币种（避免其他干扰）
        seedCurrency("USD-GATED", "USD");

        // 调 refreshRatesFromApi 应抛 ERR_EXCHANGE_RATE_API_UNAVAILABLE
        NopException ex = assertThrows(NopException.class, () ->
                currencyBiz.refreshRatesFromApi("USD", CTX));
        assertEquals(ErpMdErrors.ERR_EXCHANGE_RATE_API_UNAVAILABLE.getErrorCode(), ex.getErrorCode(),
                "config-gated 关闭时应抛 ERR_EXCHANGE_RATE_API_UNAVAILABLE");
    }

    // ---------- helpers ----------

    private Long seedCurrency(String internalCode, String isoCode) {
        return ormTemplate.runInSession(session -> {
            ErpMdCurrency c = new ErpMdCurrency();
            // code 字段作为 ISO 4217 货币代码（与 BizModel 用 c.getCode() 匹配 API 返回的 targetCurrency 一致）
            c.setCode(isoCode);
            c.setName("E2E-" + internalCode);
            c.setSymbol(isoCode);
            c.setIsActive(true);
            daoProvider.daoFor(ErpMdCurrency.class).saveEntity(c);
            return c.getId();
        });
    }
}
