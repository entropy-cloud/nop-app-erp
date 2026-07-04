package app.erp.log.service.spi;

import app.erp.log.biz.IErpLogCarrierBiz;
import app.erp.log.dao.entity.ErpLogCarrier;
import app.erp.log.service.ErpLogErrors;
import app.erp.log.service.spi.model.RateQuoteRequest;
import app.erp.log.service.spi.model.RateQuoteResult;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 承运商网关注册中心（第三层）。启动时收集所有 {@link IErpLogCarrierGatewayClientFactory} Bean，
 * 按 {@link IErpLogCarrierGatewayClientFactory#getGatewayId()} 建立 gatewayId→Factory 查找表，
 * {@link #getClient(Long, IServiceContext)} 按 {@link ErpLogCarrier#getGatewayId()} 派发。
 *
 * <p>注入范式镜像 finance {@code ErpFinAcctDocRegistry} 的 {@code List} 注入 + 内部建图
 * （避免 {@code @Inject Map<String,T>} 以 bean-name 为键的脆弱耦合）：{@code factories} 由容器按类型收集后经 setter 注入，
 * {@link #init()} 在属性设置完成后由容器调用。
 *
 * <p>对应 {@code carrier-integration.md §1.3}。
 */
public class ErpLogCarrierGatewayRegistry {

    private List<IErpLogCarrierGatewayClientFactory> factories = Collections.emptyList();
    private final Map<String, IErpLogCarrierGatewayClientFactory> factoryMap = new LinkedHashMap<>();

    @Inject
    IErpLogCarrierBiz carrierBiz;

    public void setFactories(List<IErpLogCarrierGatewayClientFactory> factories) {
        this.factories = factories == null ? Collections.emptyList() : factories;
    }

    @PostConstruct
    public void init() {
        Map<String, IErpLogCarrierGatewayClientFactory> map = new LinkedHashMap<>();
        for (IErpLogCarrierGatewayClientFactory factory : factories) {
            IErpLogCarrierGatewayClientFactory existing = map.put(factory.getGatewayId(), factory);
            if (existing != null && existing != factory) {
                throw new NopException(ErpLogErrors.ERR_LOG_GATEWAY_NOT_REGISTERED)
                        .param(ErpLogErrors.ARG_GATEWAY_ID, factory.getGatewayId());
            }
        }
        factoryMap.clear();
        factoryMap.putAll(map);
    }

    /**
     * 按 carrierId 派发：查 {@link ErpLogCarrier} 取 gatewayId → 在 factoryMap 找 Factory → newClient。
     * 未注册抛 {@link ErpLogErrors#ERR_LOG_GATEWAY_NOT_REGISTERED}。
     */
    public IErpLogCarrierGatewayClient getClient(Long carrierId, IServiceContext context) {
        ErpLogCarrier carrier = carrierBiz.requireEntity(String.valueOf(carrierId), null, context);
        String gatewayId = carrier.getGatewayId();
        IErpLogCarrierGatewayClientFactory factory = factoryMap.get(gatewayId);
        if (factory == null) {
            throw new NopException(ErpLogErrors.ERR_LOG_GATEWAY_NOT_REGISTERED)
                    .param(ErpLogErrors.ARG_CARRIER_ID, carrierId)
                    .param(ErpLogErrors.ARG_GATEWAY_ID, gatewayId);
        }
        return factory.newClientForCarrierId(carrierId);
    }

    /** 暴露已注册的 gatewayId 集合（测试/诊断用）。 */
    public List<String> getRegisteredGatewayIds() {
        return new ArrayList<>(factoryMap.keySet());
    }

    /**
     * 比价聚合：遍历所有已注册 Factory 对每个 carrier 报价。{@code carrier-integration.md §十} 标 ⚪ 未源验，
     * 本期仅 mock 返回，生产比价归 follow-up。
     */
    public List<RateQuoteResult> getRateQuotes(RateQuoteRequest request, IServiceContext context) {
        List<RateQuoteResult> results = new ArrayList<>();
        for (IErpLogCarrierGatewayClientFactory factory : factoryMap.values()) {
            for (String gatewayId : Collections.singletonList(factory.getGatewayId())) {
                IErpLogCarrierGatewayClient client = factory.newClientForCarrierId(null);
                results.add(client.getRateQuote(request));
            }
        }
        return results;
    }
}
