
package app.erp.md.service.entity;

import app.erp.md.biz.IErpMdCurrencyBiz;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdExchangeRate;
import app.erp.md.service.ErpMdErrors;
import app.erp.md.service.exchange.ErpMdExchangeRateApiClientFactory;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.FilterBeans;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
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

@BizModel("ErpMdCurrency")
public class ErpMdCurrencyBizModel extends CrudBizModel<ErpMdCurrency> implements IErpMdCurrencyBiz {

    /**
     * 汇率查询 API 客户端工厂（D1 参考实现）。
     * 注入方式：{@code @Inject}（无需 required=false —— 工厂始终可注入；config-gated 由 Factory 内部判断）。
     */
    @Inject
    ErpMdExchangeRateApiClientFactory exchangeRateApiClientFactory;

    public ErpMdCurrencyBizModel() {
        setEntityName(ErpMdCurrency.class.getName());
    }

    /**
     * 从第三方 API 拉取汇率并写入 {@code ErpMdExchangeRate} 表（D1 参考实现入口）。
     *
     * <p>实现流程（per plan Phase 1）：
     * <ol>
     *   <li>查全部 {@code ErpMdCurrency}（active）作为目标币种集合；</li>
     *   <li>调 {@code exchangeRateApiClientFactory.fetchRates}（内部限流 + 缓存 + provider 派发）；</li>
     *   <li>对每条返回的 (targetCurrency → rate) 写入 {@code ErpMdExchangeRate}（upsert，幂等键：
     *       {@code (fromCurrency, toCurrency, validFrom)}）；</li>
     *   <li>返回新建/更新的 {@code ErpMdExchangeRate} 列表。</li>
     * </ol>
     */
    @Override
    @BizMutation
    public List<ErpMdExchangeRate> refreshRatesFromApi(@Name("baseCurrency") String baseCurrency,
                                                      IServiceContext context) {
        String base = baseCurrency != null ? baseCurrency : "USD";
        LocalDate today = LocalDate.now();

        // 1. 查全部币种作为目标（同域实体访问：daoProvider() 来自父类 CrudBizModel；同域不同实体，与 ErpMdMaterialCustomsBizModel 同模式）
        IEntityDao<ErpMdCurrency> currencyDao = daoProvider().daoFor(ErpMdCurrency.class);
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
        IEntityDao<ErpMdExchangeRate> rateDao = daoProvider().daoFor(ErpMdExchangeRate.class);
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

    /** 查找既存汇率记录（幂等 upsert 用，按 fromCurrencyId + toCurrencyId + validFrom 三元组）。 */
    private ErpMdExchangeRate findExistingRate(IEntityDao<ErpMdExchangeRate> rateDao,
                                                Long fromCurrencyId, Long toCurrencyId, LocalDate validFrom) {
        QueryBean query = new QueryBean();
        query.addFilter(FilterBeans.eq("fromCurrencyId", fromCurrencyId));
        query.addFilter(FilterBeans.eq("toCurrencyId", toCurrencyId));
        query.addFilter(FilterBeans.eq("validFrom", validFrom));
        // 同域子实体直接查（绕过 IBiz 管道的 Map 投影；与 ErpMdMaterialCustomsBizModel 唯一性查重同模式）
        List<ErpMdExchangeRate> list = rateDao.findAllByQuery(query);
        return list.isEmpty() ? null : list.get(0);
    }
}
