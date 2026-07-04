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
}
