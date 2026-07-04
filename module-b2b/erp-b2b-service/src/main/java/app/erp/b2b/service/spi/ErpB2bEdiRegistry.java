package app.erp.b2b.service.spi;

import app.erp.b2b.service.ErpB2bErrors;
import app.erp.b2b.service.spi.model.EdiApplicability;
import app.erp.b2b.service.spi.model.ParsedPayload;
import io.nop.api.core.exceptions.NopException;
import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * EDI 格式 Provider 注册中心。启动时收集所有 {@link IErpB2bEdiProvider} Bean，
 * 按 {@link IErpB2bEdiProvider#getCode()} 建立 formatCode→Provider 查找表。
 *
 * <p><b>注入范式</b>：镜像 logistics {@code ErpLogCarrierGatewayRegistry} / finance {@code ErpFinAcctDocRegistry}
 * 的 {@code List} 注入 + 内部建图（避免 {@code @Inject Map<String,T>} 以 bean-name 为键的脆弱耦合）：
 * {@code providers} 由容器按类型收集后经 setter 注入，{@link #init()} 在属性设置完成后由容器调用。
 *
 * <p>对应 {@code edi-formats.md §2.3}。
 */
public class ErpB2bEdiRegistry {

    private List<IErpB2bEdiProvider> providers = Collections.emptyList();
    private final Map<String, IErpB2bEdiProvider> providerMap = new LinkedHashMap<>();

    public void setProviders(List<IErpB2bEdiProvider> providers) {
        this.providers = providers == null ? Collections.emptyList() : providers;
    }

    @PostConstruct
    public void init() {
        Map<String, IErpB2bEdiProvider> map = new LinkedHashMap<>();
        for (IErpB2bEdiProvider provider : providers) {
            IErpB2bEdiProvider existing = map.put(provider.getCode(), provider);
            if (existing != null && existing != provider) {
                throw new NopException(ErpB2bErrors.ERR_B2B_EDI_FORMAT_NOT_REGISTERED)
                        .param(ErpB2bErrors.ARG_EDI_FORMAT_CODE, provider.getCode());
            }
        }
        providerMap.clear();
        providerMap.putAll(map);
    }

    /** 按 formatCode 取 Provider。未注册抛 {@link ErpB2bErrors#ERR_B2B_EDI_FORMAT_NOT_REGISTERED}。 */
    public IErpB2bEdiProvider getProvider(String formatCode) {
        IErpB2bEdiProvider provider = providerMap.get(formatCode);
        if (provider == null) {
            throw new NopException(ErpB2bErrors.ERR_B2B_EDI_FORMAT_NOT_REGISTERED)
                    .param(ErpB2bErrors.ARG_EDI_FORMAT_CODE, formatCode);
        }
        return provider;
    }

    /** 按 relatedBillType 找适用的格式列表（出站方向）。 */
    public List<IErpB2bEdiProvider> findOutboundProviders(String relatedBillType) {
        List<IErpB2bEdiProvider> result = new ArrayList<>();
        for (IErpB2bEdiProvider provider : providerMap.values()) {
            EdiApplicability applicability = provider.getApplicability(relatedBillType);
            if (applicability != null && applicability.isOutbound()) {
                result.add(provider);
            }
        }
        return result;
    }

    /** 按 relatedBillType 找适用的格式列表（入站方向）。 */
    public List<IErpB2bEdiProvider> findInboundProviders(String relatedBillType) {
        List<IErpB2bEdiProvider> result = new ArrayList<>();
        for (IErpB2bEdiProvider provider : providerMap.values()) {
            EdiApplicability applicability = provider.getApplicability(relatedBillType);
            if (applicability != null && applicability.isInbound()) {
                result.add(provider);
            }
        }
        return result;
    }

    /**
     * 根据负载内容识别格式（入站解析时使用）。
     * 策略：按 providers 顺序尝试，第一个 parsePayload 不抛异常的即为匹配。
     * 未识别抛 {@link ErpB2bErrors#ERR_B2B_EDI_FORMAT_UNIDENTIFIED}。
     */
    public IErpB2bEdiProvider identifyProvider(String payload) {
        for (IErpB2bEdiProvider provider : providerMap.values()) {
            try {
                ParsedPayload parsed = provider.parsePayload(provider.getCode(), payload);
                if (parsed != null) {
                    return provider;
                }
            } catch (Exception e) {
                continue;
            }
        }
        throw new NopException(ErpB2bErrors.ERR_B2B_EDI_FORMAT_UNIDENTIFIED);
    }

    /** 暴露已注册的 formatCode 集合（测试/诊断用）。 */
    public List<String> getRegisteredFormatCodes() {
        return new ArrayList<>(providerMap.keySet());
    }
}
