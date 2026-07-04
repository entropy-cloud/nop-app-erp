package app.erp.ct.service.spi;

import app.erp.ct.service.ErpCtErrors;
import io.nop.api.core.exceptions.NopException;
import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 签章提供商注册中心。启动时收集所有 {@link IErpCtSignatureProvider} Bean，
 * 按 {@link IErpCtSignatureProvider#getProviderCode()} 建立 providerCode→Provider 查找表，
 * {@link #getProvider(String)} 按编码派发，未注册抛 {@link ErpCtErrors#ERR_CT_SIGNATURE_PROVIDER_NOT_REGISTERED}。
 *
 * <p>注入范式镜像 logistics {@code ErpLogCarrierGatewayRegistry} / finance {@code ErpFinAcctDocRegistry}
 * 的 {@code List} 注入 + 内部建图（避免 {@code @Inject Map<String,T>} 以 bean-name 为键的脆弱耦合）：
 * {@code providers} 由容器按类型收集后经 setter 注入，{@link #init()} 在属性设置完成后由容器调用。
 *
 * <p>对应 {@code docs/design/contract/e-signature.md §SPI 接口}。
 */
public class ErpCtSignatureProviderRegistry {

    private List<IErpCtSignatureProvider> providers = Collections.emptyList();
    private final Map<String, IErpCtSignatureProvider> providerMap = new LinkedHashMap<>();

    public void setProviders(List<IErpCtSignatureProvider> providers) {
        this.providers = providers == null ? Collections.emptyList() : providers;
    }

    @PostConstruct
    public void init() {
        Map<String, IErpCtSignatureProvider> map = new LinkedHashMap<>();
        for (IErpCtSignatureProvider provider : providers) {
            IErpCtSignatureProvider existing = map.put(provider.getProviderCode(), provider);
            if (existing != null && existing != provider) {
                throw new NopException(ErpCtErrors.ERR_CT_SIGNATURE_PROVIDER_NOT_REGISTERED)
                        .param(ErpCtErrors.ARG_PROVIDER_CODE, provider.getProviderCode());
            }
        }
        providerMap.clear();
        providerMap.putAll(map);
    }

    /**
     * 按 providerCode 派发。未注册抛 {@link ErpCtErrors#ERR_CT_SIGNATURE_PROVIDER_NOT_REGISTERED}。
     */
    public IErpCtSignatureProvider getProvider(String providerCode) {
        IErpCtSignatureProvider provider = providerMap.get(providerCode);
        if (provider == null) {
            throw new NopException(ErpCtErrors.ERR_CT_SIGNATURE_PROVIDER_NOT_REGISTERED)
                    .param(ErpCtErrors.ARG_PROVIDER_CODE, providerCode);
        }
        return provider;
    }

    /** 暴露已注册的 providerCode 集合（测试/诊断用）。 */
    public List<String> getRegisteredProviderCodes() {
        return new ArrayList<>(providerMap.keySet());
    }
}
