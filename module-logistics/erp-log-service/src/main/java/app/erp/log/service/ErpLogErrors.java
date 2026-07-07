package app.erp.log.service;

import io.nop.api.core.exceptions.ErrorCode;

/**
 * 物流域业务异常错误码。所有物流流程中的业务异常使用 {@link io.nop.api.core.exceptions.NopException} + 本接口的 {@link ErrorCode}。
 */
public interface ErpLogErrors {

    String ARG_SHIPMENT_ID = "shipmentId";
    String ARG_SHIPMENT_CODE = "shipmentCode";
    String ARG_CURRENT_STATUS = "currentStatus";
    String ARG_EXPECTED_STATUS = "expectedStatus";
    String ARG_GATEWAY_ID = "gatewayId";
    String ARG_CARRIER_ID = "carrierId";
    String ARG_CARRIER_CODE = "carrierCode";
    String ARG_ACTION_TYPE = "actionType";
    // O-11 扩展参数键
    String ARG_TRACKING_NO = "trackingNo";
    String ARG_TIMEOUT_MS = "timeoutMs";
    String ARG_FIELD_NAME = "fieldName";
    String ARG_FIELD_VALUE = "fieldValue";
    String ARG_REASON = "reason";
    String ARG_WEIGHT = "weight";
    String ARG_VOLUME = "volume";
    String ARG_FREIGHT_AMOUNT = "freightAmount";

    ErrorCode ERR_LOG_GATEWAY_NOT_REGISTERED = ErrorCode.define("erp.err.log.gateway-not-registered",
            "承运商 {carrierId} 的网关标识 {gatewayId} 未注册任何客户端工厂",
            ARG_CARRIER_ID, ARG_GATEWAY_ID);

    ErrorCode ERR_LOG_SHIPMENT_ILLEGAL_TRANSITION = ErrorCode.define("erp.err.log.shipment-illegal-transition",
            "发运单 {shipmentCode} 当前状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_SHIPMENT_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_LOG_GATEWAY_CALL_FAILED = ErrorCode.define("erp.err.log.gateway-call-failed",
            "承运商网关调用失败：发运单 {shipmentCode} / 动作 {actionType}",
            ARG_SHIPMENT_CODE, ARG_ACTION_TYPE);

    ErrorCode ERR_LOG_WEBHOOK_SIGNATURE_INVALID = ErrorCode.define("erp.err.log.webhook-signature-invalid",
            "承运商 {carrierCode} 回调签名校验失败", ARG_CARRIER_CODE);

    ErrorCode ERR_LOG_SHIPMENT_ALREADY_DELIVERED = ErrorCode.define("erp.err.log.shipment-already-delivered",
            "发运单 {shipmentCode} 已签收，运费结算状态非 PENDING，不可重复触发过账",
            ARG_SHIPMENT_CODE);

    ErrorCode ERR_LOG_CARRIER_CONFIG_MISSING = ErrorCode.define("erp.err.log.carrier-config-missing",
            "承运商 {carrierId} 缺少网关配置（ErpLogCarrierConfig）", ARG_CARRIER_ID);

    // ---------- O-11 扩展：gateway/timeout/format/routing/status 等细粒度错误码 ----------

    ErrorCode ERR_LOG_GATEWAY_TIMEOUT = ErrorCode.define("erp.err.log.gateway-timeout",
            "承运商网关调用超时：发运单 {shipmentCode} / 动作 {actionType} / 超时阈值 {timeoutMs}ms",
            ARG_SHIPMENT_CODE, ARG_ACTION_TYPE, ARG_TIMEOUT_MS);

    ErrorCode ERR_LOG_GATEWAY_RATE_LIMITED = ErrorCode.define("erp.err.log.gateway-rate-limited",
            "承运商 {carrierCode} 网关触发限流，请稍后重试", ARG_CARRIER_CODE);

    ErrorCode ERR_LOG_GATEWAY_AUTH_FAILED = ErrorCode.define("erp.err.log.gateway-auth-failed",
            "承运商 {carrierCode} 网关认证失败（凭证过期或无效）", ARG_CARRIER_CODE);

    ErrorCode ERR_LOG_TRACKING_NO_INVALID_FORMAT = ErrorCode.define("erp.err.log.tracking-no-invalid-format",
            "运单号 {trackingNo} 格式不符合承运商 {carrierCode} 规则", ARG_TRACKING_NO, ARG_CARRIER_CODE);

    ErrorCode ERR_LOG_SHIPMENT_NOT_FOUND = ErrorCode.define("erp.err.log.shipment-not-found",
            "发运单 {shipmentCode} 不存在", ARG_SHIPMENT_CODE);

    ErrorCode ERR_LOG_SHIPMENT_NOT_FOUND_BY_TRACKING = ErrorCode.define("erp.err.log.shipment-not-found-by-tracking",
            "按运单号 {trackingNo} 未找到发运单", ARG_TRACKING_NO);

    ErrorCode ERR_LOG_CARRIER_NOT_FOUND = ErrorCode.define("erp.err.log.carrier-not-found",
            "承运商 {carrierCode} 不存在", ARG_CARRIER_CODE);

    ErrorCode ERR_LOG_ROUTING_NO_GATEWAY = ErrorCode.define("erp.err.log.routing-no-gateway",
            "承运商 {carrierCode} 未配置网关路由策略", ARG_CARRIER_CODE);

    ErrorCode ERR_LOG_LABEL_GENERATION_FAILED = ErrorCode.define("erp.err.log.label-generation-failed",
            "面单生成失败：发运单 {shipmentCode} / 原因 {reason}", ARG_SHIPMENT_CODE, ARG_REASON);

    ErrorCode ERR_LOG_WEIGHT_INVALID = ErrorCode.define("erp.err.log.weight-invalid",
            "发运单 {shipmentCode} 重量 {weight} 非法（须为正数）", ARG_SHIPMENT_CODE, ARG_WEIGHT);

    ErrorCode ERR_LOG_VOLUME_INVALID = ErrorCode.define("erp.err.log.volume-invalid",
            "发运单 {shipmentCode} 体积 {volume} 非法（须为正数）", ARG_SHIPMENT_CODE, ARG_VOLUME);

    ErrorCode ERR_LOG_FREIGHT_AMOUNT_INVALID = ErrorCode.define("erp.err.log.freight-amount-invalid",
            "发运单 {shipmentCode} 运费金额 {freightAmount} 非法（不可为负）",
            ARG_SHIPMENT_CODE, ARG_FREIGHT_AMOUNT);

    ErrorCode ERR_LOG_WEBHOOK_EVENT_UNSUPPORTED = ErrorCode.define("erp.err.log.webhook-event-unsupported",
            "承运商 {carrierCode} 回调事件类型 {eventType} 不支持", ARG_CARRIER_CODE, "eventType");
}
