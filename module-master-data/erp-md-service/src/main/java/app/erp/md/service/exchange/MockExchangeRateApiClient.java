package app.erp.md.service.exchange;

import app.erp.md.spi.IErpMdExchangeRateApiClient;
import io.nop.api.core.time.CoreMetrics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Mock 汇率查询 API 客户端（{@code provider="mock"}，无外部 HTTP，内联可测试实现）。
 *
 * <p>本 Mock 是 D1 参考实现的默认 provider（{@code erp-md.exchange-rate-api-provider=mock}），
 * 用于全链行为验证（refreshRatesFromApi → fetchRates → 写入 ErpMdExchangeRate 表）。
 * 真实 provider（exchangerate-host / 銀企直连）归 successor。
 *
 * <p><b>确定性数据</b>：固定汇率表（USD→CNY 7.20 / USD→EUR 0.92 / USD→JPY 150 / CNY→USD 0.139）；
 * 其他币种对返回空 Map（不抛异常）。{@code asOfDate} 不参与 mock 计算（实际 API 应返回该日汇率）。
 *
 * <p>对应 {@code docs/architecture/external-api-integration-pattern.md §7.3 案例 C}。
 */
public class MockExchangeRateApiClient implements IErpMdExchangeRateApiClient {

    /** 固定 mock 汇率表（baseCurrency → (targetCurrency → rate)）。 */
    private static final Map<String, Map<String, BigDecimal>> MOCK_RATES = new HashMap<>();

    static {
        // 1 单位 baseCurrency = rate 单位 targetCurrency
        Map<String, BigDecimal> usdRates = new HashMap<>();
        usdRates.put("CNY", new BigDecimal("7.20"));
        usdRates.put("EUR", new BigDecimal("0.92"));
        usdRates.put("JPY", new BigDecimal("150.00"));
        usdRates.put("USD", BigDecimal.ONE);
        MOCK_RATES.put("USD", usdRates);

        Map<String, BigDecimal> cnyRates = new HashMap<>();
        cnyRates.put("USD", new BigDecimal("0.139"));
        cnyRates.put("CNY", BigDecimal.ONE);
        MOCK_RATES.put("CNY", cnyRates);

        Map<String, BigDecimal> eurRates = new HashMap<>();
        eurRates.put("USD", new BigDecimal("1.087"));
        eurRates.put("EUR", BigDecimal.ONE);
        MOCK_RATES.put("EUR", eurRates);
    }

    /**
     * 测试钩子：当设置为非 {@code null} 时，{@link #fetchRates} 直接返回此 Map（绕过固定表）。
     * 测试用例设置后影响后续调用；{@code null}（默认）走固定表。
     */
    public static volatile Map<String, BigDecimal> stubRates = null;

    @Override
    public Map<String, BigDecimal> fetchRates(String baseCurrency, Set<String> targetCurrencies, LocalDate asOfDate) {
        // asOfDate 不参与 mock（实际 API 应返回该日汇率）；CoreMetrics.today() 仅用于日志可追溯
        LocalDate effective = asOfDate != null ? asOfDate : CoreMetrics.today();

        if (stubRates != null) {
            // 测试钩子：直接返回预设结果（过滤 targetCurrencies）
            Map<String, BigDecimal> filtered = new HashMap<>();
            for (String tc : targetCurrencies) {
                BigDecimal r = stubRates.get(tc);
                if (r != null) {
                    filtered.put(tc, r);
                }
            }
            return filtered;
        }

        Map<String, BigDecimal> baseTable = MOCK_RATES.get(baseCurrency);
        Map<String, BigDecimal> result = new HashMap<>();
        if (baseTable == null) {
            // 未知基准币种 → 返回空 Map（不抛异常，与其他 provider 一致）
            return result;
        }
        for (String tc : targetCurrencies) {
            BigDecimal r = baseTable.get(tc);
            if (r != null) {
                result.put(tc, r);
            }
        }
        return result;
    }

    /** 测试重置：清空 stub 钩子（{@code @BeforeEach} 调用）。 */
    public static void resetTestState() {
        stubRates = null;
    }
}
