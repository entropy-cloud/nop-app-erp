package app.erp.md.service.processor;

import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdExchangeRate;
import app.erp.md.service.ErpMdErrors;
import app.erp.md.service.exchange.ErpMdExchangeRateApiClientFactory;
import io.nop.api.core.beans.FilterBeans;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ErpMdCurrency refreshRatesFromApi per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含汇率刷新编排：查全部币种 → 调 Factory（内部 config-gated + 限流 + 缓存 + provider 派发）→ upsert ErpMdExchangeRate。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpMdCurrencyRefreshRatesFromApiProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    ErpMdExchangeRateApiClientFactory exchangeRateApiClientFactory;

    public List<ErpMdExchangeRate> refreshRatesFromApi(String baseCurrency, IServiceContext context) {
        String base = baseCurrency != null ? baseCurrency : "USD";
        LocalDate today = CoreMetrics.today();

        // 1. 查全部币种作为目标（同域实体访问：daoProvider() 来自父类 CrudBizModel；同域不同实体，与 ErpMdMaterialCustomsBizModel 同模式）
        IEntityDao<ErpMdCurrency> currencyDao = daoProvider.daoFor(ErpMdCurrency.class);
        List<ErpMdCurrency> allCurrencies = currencyDao.findAll();
        Set<String> targetCodes = new HashSet<>();
        Map<String, ErpMdCurrency> codeToCurrency = new HashMap<>();
        for (ErpMdCurrency c : allCurrencies) {
            if (c.getCode() == null) continue;
            targetCodes.add(c.getCode());
            codeToCurrency.put(c.getCode(), c);
        }
        if (!codeToCurrency.containsKey(base)) {
            throw new NopException(ErpMdErrors.ERR_CURRENCY_NOT_FOUND)
                    .param("currencyId", base);
        }
        targetCodes.remove(base);

        // 2. 调 Factory（内部 config-gated + 限流 + 缓存 + provider 派发；config 关闭时抛 ERR_EXCHANGE_RATE_API_UNAVAILABLE）
        Map<String, BigDecimal> rates = exchangeRateApiClientFactory.fetchRates(base, targetCodes, today);

        // 3. upsert ErpMdExchangeRate（幂等键：fromCurrencyId + toCurrencyId + validFrom）
        ErpMdCurrency baseCurrencyEntity = codeToCurrency.get(base);
        IEntityDao<ErpMdExchangeRate> rateDao = daoProvider.daoFor(ErpMdExchangeRate.class);
        List<ErpMdExchangeRate> result = new ArrayList<>();
        LocalDate validFrom = today;
        LocalDate validTo = today.plusDays(1);

        for (Map.Entry<String, BigDecimal> entry : rates.entrySet()) {
            String targetCode = entry.getKey();
            BigDecimal rate = entry.getValue();
            ErpMdCurrency targetCurrency = codeToCurrency.get(targetCode);
            if (targetCurrency == null) {
                continue;
            }

            ErpMdExchangeRate rateEntity = findExistingRate(rateDao, baseCurrencyEntity.getId(),
                    targetCurrency.getId(), validFrom);
            boolean isNew = rateEntity == null;
            if (isNew) {
                rateEntity = rateDao.newEntity();
                rateEntity.setFromCurrencyId(baseCurrencyEntity.getId());
                rateEntity.setToCurrencyId(targetCurrency.getId());
                rateEntity.setRateType("MIDDLE");
                rateEntity.setValidFrom(validFrom);
                rateEntity.setValidTo(validTo);
            }
            rateEntity.setRate(rate);
            if (isNew) {
                rateDao.saveEntity(rateEntity);
            } else {
                rateDao.updateEntity(rateEntity);
            }
            result.add(rateEntity);
        }

        return result;
    }

    protected ErpMdExchangeRate findExistingRate(IEntityDao<ErpMdExchangeRate> rateDao,
                                                  String fromCurrencyId, String toCurrencyId, LocalDate validFrom) {
        QueryBean query = new QueryBean();
        query.addFilter(FilterBeans.eq("fromCurrencyId", fromCurrencyId));
        query.addFilter(FilterBeans.eq("toCurrencyId", toCurrencyId));
        query.addFilter(FilterBeans.eq("validFrom", validFrom));
        // 同域子实体直接查（绕过 IBiz 管道的 Map 投影；与 ErpMdMaterialCustomsBizModel 唯一性查重同模式）
        List<ErpMdExchangeRate> list = rateDao.findAllByQuery(query);
        return list.isEmpty() ? null : list.get(0);
    }
}
