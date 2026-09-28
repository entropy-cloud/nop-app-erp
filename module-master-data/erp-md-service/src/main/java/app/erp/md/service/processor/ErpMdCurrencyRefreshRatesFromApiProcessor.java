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

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpMdCurrency、ErpMdExchangeRate）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
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

        // perf-ux plan 0835-1 G1：原逐汇率 findExistingRate eq×3 查询（每汇率 1 次往返）改单次
        // in(toCurrencyId) 批载建 Map（币种数≪500 不分块，单 provider 全量币种量级）。
        // 等价性以幂等键唯一性为界：历史重复行时 Map 取一与原 list.get(0) 同为非确定取一。
        Map<String, ErpMdExchangeRate> existingByTargetId = findExistingRates(rateDao,
                baseCurrencyEntity.getId(), codeToCurrency.values(), validFrom);

        for (Map.Entry<String, BigDecimal> entry : rates.entrySet()) {
            String targetCode = entry.getKey();
            BigDecimal rate = entry.getValue();
            ErpMdCurrency targetCurrency = codeToCurrency.get(targetCode);
            if (targetCurrency == null) {
                continue;
            }

            ErpMdExchangeRate rateEntity = existingByTargetId.get(targetCurrency.getId());
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

    /**
     * 批量预载当日既有汇率（同域子实体直接查，绕过 IBiz 管道的 Map 投影；与
     * ErpMdMaterialCustomsBizModel 唯一性查重同模式），按 toCurrencyId 键控。
     */
    protected Map<String, ErpMdExchangeRate> findExistingRates(IEntityDao<ErpMdExchangeRate> rateDao,
                                                               String fromCurrencyId,
                                                               java.util.Collection<ErpMdCurrency> targetCurrencies,
                                                               LocalDate validFrom) {
        Set<String> toCurrencyIds = new HashSet<>();
        for (ErpMdCurrency c : targetCurrencies) {
            toCurrencyIds.add(c.getId());
        }
        if (toCurrencyIds.isEmpty()) {
            return new HashMap<>();
        }
        QueryBean query = new QueryBean();
        query.addFilter(FilterBeans.eq("fromCurrencyId", fromCurrencyId));
        query.addFilter(FilterBeans.in("toCurrencyId", toCurrencyIds));
        query.addFilter(FilterBeans.eq("validFrom", validFrom));
        Map<String, ErpMdExchangeRate> ret = new HashMap<>();
        for (ErpMdExchangeRate rate : rateDao.findAllByQuery(query)) {
            ret.put(rate.getToCurrencyId(), rate);
        }
        return ret;
    }
}
