package app.erp.log.service.spi.mock;

import app.erp.log.service.ErpLogConstants;
import app.erp.log.service.ErpLogErrors;
import app.erp.log.service.spi.IErpLogCarrierGatewayClient;
import app.erp.log.service.spi.IErpLogCarrierGatewayClientFactory;
import app.erp.log.service.spi.model.DeliveryOrderRequest;
import app.erp.log.service.spi.model.DeliveryOrderResult;
import app.erp.log.service.spi.model.PackageLabel;
import app.erp.log.service.spi.model.RateQuoteRequest;
import app.erp.log.service.spi.model.RateQuoteResult;
import app.erp.log.service.spi.model.ShipmentAdvice;
import app.erp.log.service.spi.model.TrackingEvent;
import app.erp.log.service.spi.model.TrackingResult;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mock 承运商网关客户端工厂（{@code gatewayId="mock"}，无外部 HTTP，内联可测试实现）。
 *
 * <p>用于全链行为验证（状态机推进/trackingNo/labelUrl 回写/重试/死信）。真实承运商 HTTP 集成归 follow-up
 * （Non-Goal）。
 *
 * <p><b>测试钩子</b>（static，测试用例可控失败模式以覆盖重试/死信路径）：
 * <ul>
 *   <li>{@link #FAILURE_MODE_SUCCESS}（默认）：completeDeliveryOrder 成功生成确定性 trackingNo/labelUrl。</li>
 *   <li>{@link #FAILURE_MODE_5XX}：抛 5xx 异常（可重试，触发指数退避至死信）。</li>
 *   <li>{@link #FAILURE_MODE_4XX}：抛 4xx 异常（不可重试）。</li>
 * </ul>
 *
 * <p><b>trackShipment 状态推进</b>：按调用次数确定性推进——首次 IN_TRANSIT，其后 DELIVERED（轮询兜底测试用）。
 */
public class MockCarrierGatewayClientFactory implements IErpLogCarrierGatewayClientFactory {

    public static final int FAILURE_MODE_SUCCESS = 0;
    public static final int FAILURE_MODE_5XX = 1;
    public static final int FAILURE_MODE_4XX = 2;

    /** 测试钩子：失败模式（默认成功）。测试用例设置后影响后续 completeDeliveryOrder。 */
    public static volatile int failureMode = FAILURE_MODE_SUCCESS;

    /** 共享状态：trackingNo → 轮询调用次数（确定性状态推进）。 */
    private final Map<String, AtomicInteger> trackCallCounts = new ConcurrentHashMap<>();

    @Override
    public String getGatewayId() {
        return ErpLogConstants.GATEWAY_ID_MOCK;
    }

    @Override
    public IErpLogCarrierGatewayClient newClientForCarrierId(Long carrierId) {
        // mock 无需真实凭证/端点；真实 Factory 在此读 ErpLogCarrierConfig + EncryptionHelper 解密（Non-Goal，本期 stub）。
        return new MockClient();
    }

    /** 测试重置：清空共享状态与失败模式（@BeforeEach 调用）。 */
    public void resetTestState() {
        trackCallCounts.clear();
        failureMode = FAILURE_MODE_SUCCESS;
    }

    private final class MockClient implements IErpLogCarrierGatewayClient {

        @Override
        public DeliveryOrderResult completeDeliveryOrder(DeliveryOrderRequest request) {
            if (failureMode == FAILURE_MODE_5XX) {
                throw new NopException(ErpLogErrors.ERR_LOG_GATEWAY_CALL_FAILED)
                        .param(ErpLogErrors.ARG_SHIPMENT_CODE, request.getShipmentCode())
                        .param(ErpLogErrors.ARG_ACTION_TYPE, ErpLogConstants.GATEWAY_ACTION_COMPLETE_DELIVERY)
                        .param("httpStatus", 500);
            }
            if (failureMode == FAILURE_MODE_4XX) {
                throw new NopException(ErpLogErrors.ERR_LOG_GATEWAY_CALL_FAILED)
                        .param(ErpLogErrors.ARG_SHIPMENT_CODE, request.getShipmentCode())
                        .param(ErpLogErrors.ARG_ACTION_TYPE, ErpLogConstants.GATEWAY_ACTION_COMPLETE_DELIVERY)
                        .param("httpStatus", 401);
            }
            String referenceNo = request.getReferenceNo() != null ? request.getReferenceNo() : request.getShipmentCode();
            String trackingNo = "MOCK-" + referenceNo;
            DeliveryOrderResult result = new DeliveryOrderResult();
            result.setTrackingNo(trackingNo);
            result.setLabelUrl("https://mock.label/" + trackingNo + ".pdf");
            result.setEstimatedDelivery(CoreMetrics.today().plusDays(3));
            trackCallCounts.computeIfAbsent(trackingNo, k -> new AtomicInteger(0));
            return result;
        }

        @Override
        public List<PackageLabel> getPackageLabelsList(String shipmentNo) {
            PackageLabel label = new PackageLabel();
            label.setParcelNo(shipmentNo);
            label.setLabelUrl("https://mock.label/" + shipmentNo + ".pdf");
            label.setFormat("PDF");
            return new ArrayList<>(Collections.singletonList(label));
        }

        @Override
        public void adviseShipment(ShipmentAdvice advice) {
            // 无副作用（mock）
        }

        @Override
        public TrackingResult trackShipment(String trackingNo) {
            AtomicInteger count = trackCallCounts.computeIfAbsent(trackingNo, k -> new AtomicInteger(0));
            int n = count.incrementAndGet();
            TrackingResult result = new TrackingResult();
            result.setTrackingNo(trackingNo);
            // 确定性推进：首次 IN_TRANSIT，其后 DELIVERED
            result.setCurrentStatus(n <= 1
                    ? ErpLogConstants.TRACKING_EVENT_IN_TRANSIT
                    : ErpLogConstants.TRACKING_EVENT_DELIVERED);

            TrackingEvent event = new TrackingEvent();
            event.setEventTime(LocalDateTime.now());
            event.setStatusCode(result.getCurrentStatus());
            event.setLocation("MOCK-HUB");
            event.setDescription(result.getCurrentStatus());
            result.setEvents(Collections.singletonList(event));
            return result;
        }

        @Override
        public void cancelShipment(String shipmentNo) {
            // 无副作用（mock 支持取消）
        }

        @Override
        public RateQuoteResult getRateQuote(RateQuoteRequest request) {
            // 固定费率（比价生产路径归 Non-Goal，本期仅 mock 返回）
            RateQuoteResult quote = new RateQuoteResult();
            quote.setGatewayId(ErpLogConstants.GATEWAY_ID_MOCK);
            quote.setServiceType("STANDARD");
            quote.setFreight(new BigDecimal("18"));
            quote.setCurrency("CNY");
            quote.setEstimatedTransitDays("1-2");
            return quote;
        }
    }
}
