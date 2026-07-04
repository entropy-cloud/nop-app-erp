package app.erp.log.service.spi;

import app.erp.log.service.spi.model.DeliveryOrderRequest;
import app.erp.log.service.spi.model.DeliveryOrderResult;
import app.erp.log.service.spi.model.PackageLabel;
import app.erp.log.service.spi.model.RateQuoteRequest;
import app.erp.log.service.spi.model.RateQuoteResult;
import app.erp.log.service.spi.model.ShipmentAdvice;
import app.erp.log.service.spi.model.TrackingResult;

import java.util.List;

/**
 * 承运商网关客户端 SPI（第一层）。每个承运商实现本接口，封装与该承运商网关的 HTTP/gRPC 通信细节。
 *
 * <p>对应 {@code carrier-integration.md §一}。承运商中立 DTO 见 {@link app.erp.log.service.spi.model} 包。
 */
public interface IErpLogCarrierGatewayClient {

    /** 提交发运订单（下单），返回运单号 + 面单。幂等键= {@code request.referenceNo}。 */
    DeliveryOrderResult completeDeliveryOrder(DeliveryOrderRequest request);

    /** 获取面单标签（支持多包裹）。 */
    List<PackageLabel> getPackageLabelsList(String shipmentNo);

    /** 预约取件/发运通知。 */
    void adviseShipment(ShipmentAdvice advice);

    /** 追踪运单。 */
    TrackingResult trackShipment(String trackingNo);

    /** 取消发运。 */
    void cancelShipment(String shipmentNo);

    /** 获取运费报价。 */
    RateQuoteResult getRateQuote(RateQuoteRequest request);
}
