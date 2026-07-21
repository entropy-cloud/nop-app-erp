package app.erp.md.biz;

import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdExchangeRate;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.util.List;

public interface IErpMdCurrencyBiz extends ICrudBiz<ErpMdCurrency> {

    /**
     * 从第三方 API 拉取汇率并写入 {@code ErpMdExchangeRate} 表（D1 参考实现入口）。
     *
     * <p>config-gated：{@code erp-md.exchange-rate-api-enabled=false}（默认）时抛
     * {@code ERR_EXCHANGE_RATE_API_UNAVAILABLE}；启用时经
     * {@code ErpMdExchangeRateApiClientFactory.fetchRates} 拉取 + 写入。
     *
     * <p>幂等键：{@code (fromCurrency, toCurrency, validFrom)} —— 同一组合重复 refresh 不创建重复汇率
     * （已存在则更新 rate 字段；语义对齐 {@code idempotency-pattern.md §规则 1}）。
     *
     * @param baseCurrency 基准币种代码（ISO 4217，如 {@code "USD"}）。若为 {@code null}，默认 {@code "USD"}。
     * @param context      服务上下文（用户身份 + 数据权限）。
     * @return 新建/更新的 {@code ErpMdExchangeRate} 实体列表（每目标币种一条）。
     */
    @BizMutation
    List<ErpMdExchangeRate> refreshRatesFromApi(@Name("baseCurrency") String baseCurrency,
                                                IServiceContext context);
}
