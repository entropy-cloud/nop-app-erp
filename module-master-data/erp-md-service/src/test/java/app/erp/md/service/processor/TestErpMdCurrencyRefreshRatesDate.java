package app.erp.md.service.processor;

import app.erp.common.test.ThreadLocalFrozenClock;
import app.erp.md.biz.IErpMdCurrencyBiz;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdExchangeRate;
import app.erp.md.service.ErpMdConfigs;
import app.erp.md.service.exchange.ErpMdExchangeRateApiClientFactory;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitBaseTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 汇率刷新 {@code refreshRatesFromApi} 日期敏感路径聚焦测试（plan 2026-08-02-0650-1 Phase 1 Proof）。
 *
 * <p>Q6 successor 闭合：生产侧 {@link ErpMdCurrencyRefreshRatesFromApiProcessor#refreshRatesFromApi}
 * 原读 {@code LocalDate.now()}（墙钟）作为 rate date + upsert 的 {@code validFrom}/{@code validTo}，
 * 现改读 {@link CoreMetrics#today()}（可冻结）。本测试冻结时钟到两个不同日期，断言：
 * <ul>
 *   <li>{@code ErpMdExchangeRate.validFrom == 冻结日} 且 {@code validTo == 冻结日+1}；</li>
 *   <li>切换 {@code install(其他日期)} 复跑，{@code validFrom}/{@code validTo} 随冻结日改变——
 *       证明走冻结时钟而非墙钟（若走墙钟则两次写入的日期相同）。</li>
 * </ul>
 *
 * <p>区分价值：既有 {@code TestErpMdExchangeRateApiClient.testRefreshRatesFromApiWritesExchangeRate}
 * 端到端触达 Processor 的 upsert 路径但<b>未冻结时钟且未断言 validFrom/validTo</b>；本测试补「冻结时钟下的
 * 日期确定性 + validFrom/validTo 显式断言」。
 *
 * <p>用 {@link JunitBaseTestCase}（容器+DB，无快照），方法内手动 install/clear。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMdCurrencyRefreshRatesDate extends JunitBaseTestCase {

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
    void enableApi() {
        AppConfig.getConfigProvider().assignConfigValue(
                ErpMdConfigs.CONFIG_EXCHANGE_RATE_API_ENABLED, Boolean.TRUE);
        AppConfig.getConfigProvider().assignConfigValue(
                ErpMdConfigs.CONFIG_EXCHANGE_RATE_API_PROVIDER,
                ErpMdConfigs.DEFAULT_EXCHANGE_RATE_API_PROVIDER);
        // 高 rps 避免限流干扰两次刷新调用
        AppConfig.getConfigProvider().assignConfigValue(
                ErpMdConfigs.CONFIG_EXCHANGE_RATE_API_RATE_LIMIT_RPS, 100.0);
        factory.resetTestState();
    }

    @AfterEach
    void cleanup() {
        ThreadLocalFrozenClock.clear();
        AppConfig.getConfigProvider().assignConfigValue(
                ErpMdConfigs.CONFIG_EXCHANGE_RATE_API_ENABLED,
                ErpMdConfigs.DEFAULT_EXCHANGE_RATE_API_ENABLED);
        factory.resetTestState();
    }

    @Test
    public void testValidFromToTracksFrozenClock() {
        // seed 3 币种：USD（基准）+ CNY + EUR（目标；mock USD→CNY 7.20 / USD→EUR 0.92）
        seedCurrency("USD");
        seedCurrency("CNY");
        seedCurrency("EUR");

        ThreadLocalFrozenClock.ensureRegistered();

        // 冻结日 1：validFrom 应 = 冻结日，validTo 应 = 冻结日+1
        LocalDate frozen1 = LocalDate.of(2026, 7, 17);
        ThreadLocalFrozenClock.install(frozen1);
        assertEquals(frozen1, CoreMetrics.today(), "CoreMetrics.today() 应返回冻结日 1");
        List<ErpMdExchangeRate> r1 = currencyBiz.refreshRatesFromApi("USD", CTX);
        assertValidFromTo(r1, frozen1);

        // 切换冻结日 2：validFrom/validTo 应随之改变（证明走冻结时钟而非墙钟）
        LocalDate frozen2 = LocalDate.of(2026, 9, 15);
        // 不同 asOfDate → 不同 cacheKey → 自动走新 fetch；reset 仅为隔离保险
        factory.resetTestState();
        ThreadLocalFrozenClock.install(frozen2);
        assertEquals(frozen2, CoreMetrics.today(), "CoreMetrics.today() 应返回冻结日 2");
        List<ErpMdExchangeRate> r2 = currencyBiz.refreshRatesFromApi("USD", CTX);
        assertValidFromTo(r2, frozen2);

        // 反向证明：两冻结日的 validFrom 不同（若走墙钟则两次相同）
        assertTrue(r1.stream().anyMatch(e -> frozen1.equals(e.getValidFrom())),
                "冻结日 1 的写入 validFrom 应 = " + frozen1);
        assertTrue(r2.stream().anyMatch(e -> frozen2.equals(e.getValidFrom())),
                "冻结日 2 的写入 validFrom 应 = " + frozen2);
    }

    private void assertValidFromTo(List<ErpMdExchangeRate> rates, LocalDate frozen) {
        assertTrue(!rates.isEmpty(), "refreshRatesFromApi 应写入至少 1 条汇率");
        for (ErpMdExchangeRate r : rates) {
            assertEquals(frozen, r.getValidFrom(),
                    "validFrom 应 = 冻结日（" + frozen + "）");
            assertEquals(frozen.plusDays(1), r.getValidTo(),
                    "validTo 应 = 冻结日+1（" + frozen.plusDays(1) + "）");
        }
    }

    private void seedCurrency(String isoCode) {
        ormTemplate.runInSession(session -> {
            IEntityDao<ErpMdCurrency> dao = daoProvider.daoFor(ErpMdCurrency.class);
            ErpMdCurrency c = dao.newEntity();
            c.setCode(isoCode);
            c.setName("FRZ-" + isoCode);
            c.setSymbol(isoCode);
            c.setIsActive(true);
            dao.saveEntity(c);
            return null;
        });
    }
}
