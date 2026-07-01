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

    // --- 发票作用域参数键（不复用出库单 ARG_DELIVERY_CODE / 「出库单…」文案） ---
    String ARG_INVOICE_CODE = "invoiceCode";
    String ARG_INVOICE_ID = "invoiceId";

    // --- 收款作用域参数键 ---
    String ARG_RECEIPT_CODE = "receiptCode";
    String ARG_RECEIPT_ID = "receiptId";
    String ARG_SETTLE_AMOUNT = "settleAmount";
    String ARG_INVOICE_BALANCE = "invoiceBalance";
    String ARG_RECEIPT_BALANCE = "receiptBalance";

    // --- 退货作用域参数键（不复用出库单/发票文案） ---
    String ARG_RETURN_CODE = "returnCode";
    String ARG_RETURN_ID = "returnId";
    String ARG_LINE_NO = "lineNo";
    String ARG_RETURN_QTY = "returnQty";
    String ARG_MAX_RETURN_QTY = "maxReturnQty";
    String ARG_DELIVERED_QTY_RETURN = "deliveredQtyReturn";

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

    // --- 发票作用域错误码（消息文案绑定发票参数，避免复用出库单文案产生误导） ---

    ErrorCode ERR_INVOICE_ILLEGAL_STATUS_TRANSITION = ErrorCode.define("erp.err.sal.invoice-illegal-status-transition",
            "销售发票 {invoiceCode} 当前审核状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_INVOICE_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_INVOICE_ILLEGAL_DOC_STATUS_TRANSITION = ErrorCode.define("erp.err.sal.invoice-illegal-doc-status-transition",
            "销售发票 {invoiceCode} 当前单据状态={currentDocStatus}，不允许执行该操作（期望状态={expectedDocStatus}）",
            ARG_INVOICE_CODE, ARG_CURRENT_DOC_STATUS, ARG_EXPECTED_DOC_STATUS);

    ErrorCode ERR_INVOICE_LINES_EMPTY = ErrorCode.define("erp.err.sal.invoice-lines-empty",
            "销售发票 {invoiceCode} 无行明细，不可提交审核",
            ARG_INVOICE_CODE);

    // --- 收款作用域错误码 ---

    ErrorCode ERR_RECEIPT_ILLEGAL_STATUS_TRANSITION = ErrorCode.define("erp.err.sal.receipt-illegal-status-transition",
            "收款单 {receiptCode} 当前审核状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_RECEIPT_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_RECEIPT_ILLEGAL_DOC_STATUS_TRANSITION = ErrorCode.define("erp.err.sal.receipt-illegal-doc-status-transition",
            "收款单 {receiptCode} 当前单据状态={currentDocStatus}，不允许执行该操作（期望状态={expectedDocStatus}）",
            ARG_RECEIPT_CODE, ARG_CURRENT_DOC_STATUS, ARG_EXPECTED_DOC_STATUS);

    ErrorCode ERR_SETTLE_CUSTOMER_MISMATCH = ErrorCode.define("erp.err.sal.settle-customer-mismatch",
            "收款单 {receiptCode} 与发票 {invoiceCode} 客户不一致，不可核销",
            ARG_RECEIPT_CODE, ARG_INVOICE_CODE);

    ErrorCode ERR_SETTLE_INVOICE_NOT_APPROVED = ErrorCode.define("erp.err.sal.settle-invoice-not-approved",
            "发票 {invoiceCode} 未审核通过，不可核销（当前审核状态={currentStatus}）",
            ARG_INVOICE_CODE, ARG_CURRENT_STATUS);

    ErrorCode ERR_SETTLE_RECEIPT_NOT_APPROVED = ErrorCode.define("erp.err.sal.settle-receipt-not-approved",
            "收款单 {receiptCode} 未审核通过，不可核销（当前审核状态={currentStatus}）",
            ARG_RECEIPT_CODE, ARG_CURRENT_STATUS);

    ErrorCode ERR_SETTLE_OVER_INVOICE_BALANCE = ErrorCode.define("erp.err.sal.settle-over-invoice-balance",
            "核销金额 {settleAmount} 超过发票 {invoiceCode} 未收余额 {invoiceBalance}",
            ARG_SETTLE_AMOUNT, ARG_INVOICE_CODE, ARG_INVOICE_BALANCE);

    ErrorCode ERR_SETTLE_OVER_RECEIPT_BALANCE = ErrorCode.define("erp.err.sal.settle-over-receipt-balance",
            "核销金额 {settleAmount} 超过收款单 {receiptCode} 未核销余额 {receiptBalance}",
            ARG_SETTLE_AMOUNT, ARG_RECEIPT_CODE, ARG_RECEIPT_BALANCE);

    // --- 退货作用域错误码（消息文案绑定退货单参数，避免复用出库单/发票文案产生误导） ---

    ErrorCode ERR_RETURN_ILLEGAL_STATUS_TRANSITION = ErrorCode.define("erp.err.sal.return-illegal-status-transition",
            "销售退货单 {returnCode} 当前审核状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_RETURN_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_RETURN_ILLEGAL_DOC_STATUS_TRANSITION = ErrorCode.define("erp.err.sal.return-illegal-doc-status-transition",
            "销售退货单 {returnCode} 当前单据状态={currentDocStatus}，不允许执行该操作（期望状态={expectedDocStatus}）",
            ARG_RETURN_CODE, ARG_CURRENT_DOC_STATUS, ARG_EXPECTED_DOC_STATUS);

    ErrorCode ERR_RETURN_NOT_FOUND = ErrorCode.define("erp.err.sal.return-not-found",
            "销售退货单 {returnId} 不存在", ARG_RETURN_ID);

    ErrorCode ERR_RETURN_LINES_EMPTY = ErrorCode.define("erp.err.sal.return-lines-empty",
            "销售退货单 {returnCode} 无行明细，不可提交审核",
            ARG_RETURN_CODE);

    ErrorCode ERR_RETURN_QTY_EXCEED = ErrorCode.define("erp.err.sal.return-qty-exceed",
            "销售退货单 {returnCode} 第 {lineNo} 行退货数量 {returnQty} 超过可退数量 {maxReturnQty}（已出库 {deliveredQtyReturn}）",
            ARG_RETURN_CODE, ARG_LINE_NO, ARG_RETURN_QTY, ARG_MAX_RETURN_QTY, ARG_DELIVERED_QTY_RETURN);

    ErrorCode ERR_RETURN_DELIVERY_NOT_APPROVED = ErrorCode.define("erp.err.sal.return-delivery-not-approved",
            "源出库单未审核通过，不可退货（当前审核状态={currentStatus}）",
            ARG_CURRENT_STATUS);

    ErrorCode ERR_RETURN_REASON_REQUIRED = ErrorCode.define("erp.err.sal.return-reason-required",
            "销售退货单 {returnCode} 第 {lineNo} 行缺少退货原因（按配置必填）",
            ARG_RETURN_CODE, ARG_LINE_NO);
}
