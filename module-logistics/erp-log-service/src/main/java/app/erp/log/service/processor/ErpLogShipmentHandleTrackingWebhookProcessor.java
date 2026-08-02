package app.erp.log.service.processor;

import app.erp.log.dao.entity.ErpLogCarrier;
import app.erp.log.dao.entity.ErpLogShipment;
import app.erp.log.service.ErpLogConfigs;
import app.erp.log.service.ErpLogConstants;
import app.erp.log.service.ErpLogErrors;
import app.erp.log.service.gateway.GatewayDispatcher;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.core.lang.json.JsonTool;
import jakarta.inject.Inject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * ErpLogShipment handleTrackingWebhook per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 *
 * <p>承运商 tracking webhook 入口：HMAC 签名校验（config-gated）+ payload 解析 + 按 trackingNo 定位运单 +
 * 幂等追踪推进（{@link GatewayDispatcher#advanceTracking}）+ 写 webhook 日志；DELIVERED 触发 {@link #onDelivered}
 * 运费过账/到岸成本编排（继承 {@link AbstractErpLogShipmentDeliveredProcessor}）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 * （R6.7，processor-extension-pattern.md 每 mutation 一 Processor）。
 */
public class ErpLogShipmentHandleTrackingWebhookProcessor extends AbstractErpLogShipmentDeliveredProcessor {

    public ErpLogShipment handleTrackingWebhook(String carrierCode, String signature, String payload,
                                                IServiceContext context) {
        ErpLogCarrier carrier = gatewayDispatcher.findCarrierByCode(carrierCode);
        if (carrier == null) {
            throw new NopException(ErpLogErrors.ERR_LOG_GATEWAY_NOT_REGISTERED)
                    .param(ErpLogErrors.ARG_CARRIER_CODE, carrierCode);
        }
        String secret = resolveWebhookSecret(carrier);

        boolean required = AppConfig.var(ErpLogConfigs.CONFIG_WEBHOOK_SIGNATURE_REQUIRED,
                ErpLogConfigs.DEFAULT_WEBHOOK_SIGNATURE_REQUIRED);
        if (required && !verifySignature(payload, signature, secret)) {
            throw new NopException(ErpLogErrors.ERR_LOG_WEBHOOK_SIGNATURE_INVALID)
                    .param(ErpLogErrors.ARG_CARRIER_CODE, carrierCode);
        }

        Map<String, Object> event = parsePayload(payload);
        String trackingNo = asString(event.get("trackingNo"));
        String eventType = asString(event.get("eventType"));
        String signedBy = asString(event.get("signedBy"));

        ErpLogShipment shipment = gatewayDispatcher.findShipmentByTrackingNo(trackingNo);
        if (shipment == null) {
            return null;
        }
        // 幂等：advanceTracking 已 DELIVERED 返回 false；同 event 重复不重复推进。
        boolean advanced = gatewayDispatcher.advanceTracking(shipment, eventType, signedBy);
        gatewayDispatcher.writeWebhookLog(shipment, eventType, payload, advanced);
        if (advanced && ErpLogConstants.TRACKING_EVENT_DELIVERED.equals(eventType)) {
            onDelivered(shipment, context);
        }
        return shipment;
    }

    /** webhook HMAC 密钥：取承运商编码（mock 测试可控；真实部署可扩展 credentials JSON 内 webhookSecret）。 */
    protected String resolveWebhookSecret(ErpLogCarrier carrier) {
        return carrier.getCode();
    }

    protected boolean verifySignature(String payload, String signature, String secret) {
        if (signature == null || signature.isEmpty()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            String expected = sb.toString();
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    signature.toLowerCase().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> parsePayload(String payload) {
        return (Map<String, Object>) JsonTool.parseNonStrict(payload);
    }

    protected String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
