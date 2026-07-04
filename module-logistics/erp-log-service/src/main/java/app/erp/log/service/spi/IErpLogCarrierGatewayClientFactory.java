package app.erp.log.service.spi;

/**
 * 承运商网关客户端工厂 SPI（第二层）。每个网关实现一个 Factory bean，读取 {@code ErpLogCarrierConfig} 注入凭证与端点，
 * 经 {@code EncryptionHelper} 解密凭证后构造配置化的 client 实例。
 *
 * <p>对应 {@code carrier-integration.md §1.2}。{@link ErpLogCarrierGatewayRegistry}（第三层）按 {@link #getGatewayId()}
 * 建立 gatewayId→Factory 查找表并派发。
 */
public interface IErpLogCarrierGatewayClientFactory {

    /** 网关标识，如 {@code "mock"} / {@code "dhl"} / {@code "sf"}，对应 {@code ErpLogCarrier.gatewayId}。 */
    String getGatewayId();

    /**
     * 按 carrierId 创建配置化 client。
     * <p>内部行为：查 {@code ErpLogCarrierConfig}(by carrierId) → 解密 apiKey/apiSecret → 注入超时 → 返回 client。
     */
    IErpLogCarrierGatewayClient newClientForCarrierId(Long carrierId);
}
