package app.erp.sal.service;

import io.nop.api.core.exceptions.ErrorCode;

/**
 * 销售域业务异常错误码。所有销售流程中的业务异常使用 {@link io.nop.api.core.exceptions.NopException} + 本接口的 {@link ErrorCode}。
 */
public interface ErpSalErrors {

    String ARG_DELIVERY_ID = "deliveryId";
    String ARG_DELIVERY_CODE = "deliveryCode";
    String ARG_ORDER_ID = "orderId";
    String ARG_ORDER_CODE = "orderCode";
    String ARG_QUOTATION_ID = "quotationId";
    String ARG_QUOTATION_CODE = "quotationCode";
    String ARG_CURRENT_STATUS = "currentStatus";
    String ARG_EXPECTED_STATUS = "expectedStatus";
    String ARG_CURRENT_DOC_STATUS = "currentDocStatus";
    String ARG_EXPECTED_DOC_STATUS = "expectedDocStatus";
    String ARG_CUSTOMER_ID = "customerId";
    String ARG_MOVE_CODE = "moveCode";
    String ARG_CREDIT_LIMIT = "creditLimit";
    String ARG_AVAILABLE = "available";
    String ARG_ORDER_AMOUNT = "orderAmount";
    String ARG_VALID_TO = "validTo";
    String ARG_IS_ACCEPTED = "isAccepted";

    ErrorCode ERR_ILLEGAL_STATUS_TRANSITION = ErrorCode.define("erp.err.sal.illegal-status-transition",
            "出库单 {deliveryCode} 当前审核状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_DELIVERY_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_ILLEGAL_DOC_STATUS_TRANSITION = ErrorCode.define("erp.err.sal.illegal-doc-status-transition",
            "出库单 {deliveryCode} 当前单据状态={currentDocStatus}，不允许执行该操作（期望状态={expectedDocStatus}）",
            ARG_DELIVERY_CODE, ARG_CURRENT_DOC_STATUS, ARG_EXPECTED_DOC_STATUS);

    ErrorCode ERR_DELIVERY_NOT_FOUND = ErrorCode.define("erp.err.sal.delivery-not-found",
            "出库单 {deliveryId} 不存在", ARG_DELIVERY_ID);

    ErrorCode ERR_DELIVERY_LINES_EMPTY = ErrorCode.define("erp.err.sal.delivery-lines-empty",
            "出库单 {deliveryCode} 无行明细，不可提交审核",
            ARG_DELIVERY_CODE);

    ErrorCode ERR_PARTNER_INACTIVE = ErrorCode.define("erp.err.sal.partner-inactive",
            "客户 {customerId} 已停用，不可开单或审核",
            ARG_CUSTOMER_ID);

    ErrorCode ERR_MOVE_NOT_FOUND = ErrorCode.define("erp.err.sal.move-not-found",
            "已审核出库单 {deliveryCode} 找不到关联的出库移动单（数据不一致）",
            ARG_DELIVERY_CODE, ARG_MOVE_CODE);

    // ---- 销售订单作用域（消息绑定订单参数，避免照搬出库单文案造成误导）----

    ErrorCode ERR_ORDER_NOT_FOUND = ErrorCode.define("erp.err.sal.order-not-found",
            "销售订单 {orderId} 不存在", ARG_ORDER_ID);

    ErrorCode ERR_ORDER_ILLEGAL_STATUS_TRANSITION = ErrorCode.define("erp.err.sal.order-illegal-status-transition",
            "销售订单 {orderCode} 当前审核状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_ORDER_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_ORDER_ILLEGAL_DOC_STATUS_TRANSITION = ErrorCode.define("erp.err.sal.order-illegal-doc-status-transition",
            "销售订单 {orderCode} 当前单据状态={currentDocStatus}，不允许执行该操作（期望状态={expectedDocStatus}）",
            ARG_ORDER_CODE, ARG_CURRENT_DOC_STATUS, ARG_EXPECTED_DOC_STATUS);

    ErrorCode ERR_ORDER_LINES_EMPTY = ErrorCode.define("erp.err.sal.order-lines-empty",
            "销售订单 {orderCode} 无行明细，不可提交审核", ARG_ORDER_CODE);

    ErrorCode ERR_CREDIT_LIMIT_EXCEEDED = ErrorCode.define("erp.err.sal.credit-limit-exceeded",
            "客户 {customerId} 信用额度不足：额度={creditLimit}，可用={available}，本单含税金额={orderAmount}",
            ARG_CUSTOMER_ID, ARG_CREDIT_LIMIT, ARG_AVAILABLE, ARG_ORDER_AMOUNT);

    // ---- 销售报价单作用域 ----

    ErrorCode ERR_QUOTATION_NOT_FOUND = ErrorCode.define("erp.err.sal.quotation-not-found",
            "销售报价单 {quotationId} 不存在", ARG_QUOTATION_ID);

    ErrorCode ERR_QUOTATION_ILLEGAL_STATUS_TRANSITION = ErrorCode.define("erp.err.sal.quotation-illegal-status-transition",
            "销售报价单 {quotationCode} 当前审核状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_QUOTATION_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_QUOTATION_ILLEGAL_DOC_STATUS_TRANSITION = ErrorCode.define("erp.err.sal.quotation-illegal-doc-status-transition",
            "销售报价单 {quotationCode} 当前单据状态={currentDocStatus}，不允许执行该操作（期望状态={expectedDocStatus}）",
            ARG_QUOTATION_CODE, ARG_CURRENT_DOC_STATUS, ARG_EXPECTED_DOC_STATUS);

    ErrorCode ERR_QUOTATION_LINES_EMPTY = ErrorCode.define("erp.err.sal.quotation-lines-empty",
            "销售报价单 {quotationCode} 无行明细，不可提交审核", ARG_QUOTATION_CODE);

    ErrorCode ERR_QUOTATION_EXPIRED = ErrorCode.define("erp.err.sal.quotation-expired",
            "销售报价单 {quotationCode} 已过期（validTo={validTo}），不可客户确认或转订单",
            ARG_QUOTATION_CODE, ARG_VALID_TO);

    ErrorCode ERR_QUOTATION_NOT_READY = ErrorCode.define("erp.err.sal.quotation-not-ready",
            "销售报价单 {quotationCode} 未满足转订单前置：须 APPROVED 且客户已确认（当前 approveStatus={currentStatus}，isAccepted={isAccepted}）",
            ARG_QUOTATION_CODE, ARG_CURRENT_STATUS, ARG_IS_ACCEPTED);

    ErrorCode ERR_QUOTATION_ALREADY_CONVERTED = ErrorCode.define("erp.err.sal.quotation-already-converted",
            "销售报价单 {quotationCode} 已存在未作废的转化订单，不可重复转化",
            ARG_QUOTATION_CODE);
}
