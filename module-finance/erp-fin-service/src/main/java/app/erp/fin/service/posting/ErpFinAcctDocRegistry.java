package app.erp.fin.service.posting;

import io.nop.api.core.exceptions.NopException;
import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 业财过账 Provider 注册中心。启动时收集所有 {@link IErpFinAcctDocProvider} 与 {@link IErpFinFactsValidator}
 * Bean，按 {@link ErpFinBusinessType} 建立 O(1) 查找表。
 *
 * <p>冲突裁决语义（后续各域 Provider 接入的稳定契约）：
 * <ul>
 *   <li>非默认 Provider（{@code isFallback()==false}）优先装配；同一 businessType 出现两个非默认 Provider
 *       → 启动期 fail-fast（抛 {@link NopException}，暴露配置错误而非静默覆盖）。</li>
 *   <li>默认 Provider（{@code isFallback()==true}）仅填充未被任何域 Provider 接管的空缺 key。</li>
 * </ul>
 *
 * <p>本类为非 BizModel 服务（无聚合根 xmeta），通过 IoC 注册为 Bean：{@code providers}/{@code validators}
 * 由容器按类型收集后经 setter 注入，{@link #init()} 在属性设置完成后由容器调用。
 */
public class ErpFinAcctDocRegistry {

    static final int DC_DEBIT = 10;

    private List<IErpFinAcctDocProvider> providers = Collections.emptyList();
    private List<IErpFinFactsValidator> validators = Collections.emptyList();

    private final Map<ErpFinBusinessType, IErpFinAcctDocProvider> providerMap = new EnumMap<>(
            ErpFinBusinessType.class);
    private List<IErpFinFactsValidator> sortedValidators = Collections.emptyList();

    public void setProviders(List<IErpFinAcctDocProvider> providers) {
        this.providers = providers == null ? Collections.emptyList() : providers;
    }

    public void setValidators(List<IErpFinFactsValidator> validators) {
        this.validators = validators == null ? Collections.emptyList() : validators;
    }

    @PostConstruct
    public void init() {
        Map<ErpFinBusinessType, IErpFinAcctDocProvider> map = new EnumMap<>(ErpFinBusinessType.class);

        for (IErpFinAcctDocProvider provider : providers) {
            if (provider.isFallback()) {
                continue;
            }
            for (ErpFinBusinessType type : provider.getSupportedBusinessTypes()) {
                IErpFinAcctDocProvider existing = map.put(type, provider);
                if (existing != null && existing != provider) {
                    throw new NopException(ErpFinPostingErrors.ERR_DUPLICATE_PROVIDER)
                            .param(ErpFinPostingErrors.ARG_BUSINESS_TYPE, type)
                            .param(ErpFinPostingErrors.ARG_EXISTING_PROVIDER, existing.getClass().getName())
                            .param(ErpFinPostingErrors.ARG_CONFLICT_PROVIDER, provider.getClass().getName());
                }
            }
        }

        for (IErpFinAcctDocProvider provider : providers) {
            if (!provider.isFallback()) {
                continue;
            }
            for (ErpFinBusinessType type : provider.getSupportedBusinessTypes()) {
                map.putIfAbsent(type, provider);
            }
        }

        providerMap.clear();
        providerMap.putAll(map);

        List<IErpFinFactsValidator> sorted = new ArrayList<>(validators);
        sorted.sort(Comparator.comparingInt(IErpFinFactsValidator::getOrder));
        sortedValidators = Collections.unmodifiableList(sorted);
    }

    public IErpFinAcctDocProvider getProvider(ErpFinBusinessType businessType) {
        return providerMap.get(businessType);
    }

    public List<IErpFinFactsValidator> getValidators() {
        return sortedValidators;
    }
}
