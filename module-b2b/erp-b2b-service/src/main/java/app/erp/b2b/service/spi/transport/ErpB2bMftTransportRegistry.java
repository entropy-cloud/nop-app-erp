package app.erp.b2b.service.spi.transport;

import app.erp.b2b.service.ErpB2bErrors;
import io.nop.api.core.exceptions.NopException;
import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MFT 传输适配器注册中心。启动时收集所有 {@link IErpB2bTransportAdapter} Bean，
 * 按 {@link IErpB2bTransportAdapter#getSupportedProtocol()} 建立 protocol→Adapter 查找表。
 *
 * <p><b>注入范式</b>：镜像 {@code ErpB2bEdiRegistry} / logistics {@code ErpLogCarrierGatewayRegistry}
 * 的 List 注入 + 内部建图。
 *
 * <p>对应 {@code managed-file-transfer.md §架构概览}。
 */
public class ErpB2bMftTransportRegistry {

    private List<IErpB2bTransportAdapter> adapters = Collections.emptyList();
    private final Map<String, IErpB2bTransportAdapter> adapterMap = new LinkedHashMap<>();

    public void setAdapters(List<IErpB2bTransportAdapter> adapters) {
        this.adapters = adapters == null ? Collections.emptyList() : adapters;
    }

    @PostConstruct
    public void init() {
        Map<String, IErpB2bTransportAdapter> map = new LinkedHashMap<>();
        for (IErpB2bTransportAdapter adapter : adapters) {
            IErpB2bTransportAdapter existing = map.put(adapter.getSupportedProtocol(), adapter);
            if (existing != null && existing != adapter) {
                throw new NopException(ErpB2bErrors.ERR_B2B_MFT_ADAPTER_NOT_REGISTERED)
                        .param(ErpB2bErrors.ARG_PROTOCOL, adapter.getSupportedProtocol());
            }
        }
        adapterMap.clear();
        adapterMap.putAll(map);
    }

    /** 按 protocol 取 Adapter。未注册抛 {@link ErpB2bErrors#ERR_B2B_MFT_ADAPTER_NOT_REGISTERED}。 */
    public IErpB2bTransportAdapter getAdapter(String protocol) {
        IErpB2bTransportAdapter adapter = adapterMap.get(protocol);
        if (adapter == null) {
            throw new NopException(ErpB2bErrors.ERR_B2B_MFT_ADAPTER_NOT_REGISTERED)
                    .param(ErpB2bErrors.ARG_PROTOCOL, protocol);
        }
        return adapter;
    }

    /** 暴露已注册的协议集合（测试/诊断用）。 */
    public List<String> getRegisteredProtocols() {
        return new ArrayList<>(adapterMap.keySet());
    }
}
