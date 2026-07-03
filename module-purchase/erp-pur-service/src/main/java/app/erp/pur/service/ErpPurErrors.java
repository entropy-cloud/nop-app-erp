package app.erp.pur.service;

import io.nop.api.core.exceptions.ErrorCode;

/**
 * 采购域业务异常错误码。所有采购流程中的业务异常使用 {@link io.nop.api.core.exceptions.NopException} + 本接口的 {@link ErrorCode}。
 */
public interface ErpPurErrors {

    String ARG_RECEIVE_ID = "receiveId";
    String ARG_RECEIVE_CODE = "receiveCode";
    String ARG_ORDER_CODE = "orderCode";
    String ARG_REQUISITION_CODE = "requisitionCode";
    String ARG_CURRENT_STATUS = "currentStatus";
    String ARG_EXPECTED_STATUS = "expectedStatus";
    String ARG_CURRENT_DOC_STATUS = "currentDocStatus";
    String ARG_EXPECTED_DOC_STATUS = "expectedDocStatus";
    String ARG_SUPPLIER_ID = "supplierId";
    String ARG_LINE_TEXT = "lineText";
    String ARG_PRICE_TEXT = "priceText";
    String ARG_MOVE_CODE = "moveCode";
    String ARG_REQUISITION_ID = "requisitionId";

    // --- 发票作用域参数键（不复用入库单 ARG_RECEIVE_CODE / 「入库单…」文案） ---
    String ARG_INVOICE_CODE = "invoiceCode";
    String ARG_INVOICE_ID = "invoiceId";
    String ARG_RECEIVED_QTY = "receivedQty";
    String ARG_INVOICE_QTY = "invoiceQty";
    String ARG_ORDER_PRICE = "orderPrice";
    String ARG_INVOICE_PRICE = "invoicePrice";
    String ARG_LINE_NO = "lineNo";

    // --- 付款作用域参数键 ---
    String ARG_PAYMENT_CODE = "paymentCode";
    String ARG_PAYMENT_ID = "paymentId";
    String ARG_SETTLE_AMOUNT = "settleAmount";
    String ARG_INVOICE_BALANCE = "invoiceBalance";
    String ARG_PAYMENT_BALANCE = "paymentBalance";

    // --- 退货作用域参数键（不复用入库单/发票文案） ---
    String ARG_RETURN_CODE = "returnCode";
    String ARG_RETURN_ID = "returnId";
    String ARG_MAX_RETURN_QTY = "maxReturnQty";
    String ARG_RECEIVED_QTY_RETURN = "receivedQtyReturn";
    ErrorCode ERR_ILLEGAL_STATUS_TRANSITION = ErrorCode.define("erp.err.pur.illegal-status-transition",
            "入库单 {receiveCode} 当前审核状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_RECEIVE_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_ILLEGAL_DOC_STATUS_TRANSITION = ErrorCode.define("erp.err.pur.illegal-doc-status-transition",
            "入库单 {receiveCode} 当前单据状态={currentDocStatus}，不允许执行该操作（期望状态={expectedDocStatus}）",
            ARG_RECEIVE_CODE, ARG_CURRENT_DOC_STATUS, ARG_EXPECTED_DOC_STATUS);

    // --- 订单作用域错误码（消息文案绑定订单参数，避免复用入库单文案产生误导） ---

    ErrorCode ERR_ORDER_ILLEGAL_STATUS_TRANSITION = ErrorCode.define("erp.err.pur.order-illegal-status-transition",
            "订单 {orderCode} 当前审核状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_ORDER_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_ORDER_ILLEGAL_DOC_STATUS_TRANSITION = ErrorCode.define("erp.err.pur.order-illegal-doc-status-transition",
            "订单 {orderCode} 当前单据状态={currentDocStatus}，不允许执行该操作（期望状态={expectedDocStatus}）",
            ARG_ORDER_CODE, ARG_CURRENT_DOC_STATUS, ARG_EXPECTED_DOC_STATUS);

    ErrorCode ERR_ORDER_NOT_FOUND = ErrorCode.define("erp.err.pur.order-not-found",
            "采购订单 {orderCode} 不存在", ARG_ORDER_CODE);

    ErrorCode ERR_ORDER_LINES_EMPTY = ErrorCode.define("erp.err.pur.order-lines-empty",
            "采购订单 {orderCode} 无行明细，不可提交审核",
            ARG_ORDER_CODE);

    // --- 请购单作用域错误码 ---

    ErrorCode ERR_REQ_ILLEGAL_STATUS_TRANSITION = ErrorCode.define("erp.err.pur.req-illegal-status-transition",
            "请购单 {requisitionCode} 当前审核状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_REQUISITION_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_REQ_ILLEGAL_DOC_STATUS_TRANSITION = ErrorCode.define("erp.err.pur.req-illegal-doc-status-transition",
            "请购单 {requisitionCode} 当前单据状态={currentDocStatus}，不允许执行该操作（期望状态={expectedDocStatus}）",
            ARG_REQUISITION_CODE, ARG_CURRENT_DOC_STATUS, ARG_EXPECTED_DOC_STATUS);

    ErrorCode ERR_REQ_NOT_FOUND = ErrorCode.define("erp.err.pur.req-not-found",
            "请购单 {requisitionId} 不存在", ARG_REQUISITION_ID);

    ErrorCode ERR_REQ_LINES_EMPTY = ErrorCode.define("erp.err.pur.req-lines-empty",
            "请购单 {requisitionCode} 无行明细，不可提交审核",
            ARG_REQUISITION_CODE);

    ErrorCode ERR_REQ_NOT_APPROVED = ErrorCode.define("erp.err.pur.req-not-approved",
            "请购单 {requisitionCode} 未审核通过，不可转化为订单（当前审核状态={currentStatus}）",
            ARG_REQUISITION_CODE, ARG_CURRENT_STATUS);

    ErrorCode ERR_REQ_ALREADY_CONVERTED = ErrorCode.define("erp.err.pur.req-already-converted",
            "请购单 {requisitionCode} 已存在未作废的采购订单，不可重复转化",
            ARG_REQUISITION_CODE);

    ErrorCode ERR_REQ_MIXED_OR_MISSING_SUPPLIER = ErrorCode.define("erp.err.pur.req-mixed-or-missing-supplier",
            "请购单 {requisitionCode} 行建议供应商缺失或不一致，不可转化（MVP 要求单请购单供应商）",
            ARG_REQUISITION_CODE);

    ErrorCode ERR_RECEIVE_NOT_FOUND = ErrorCode.define("erp.err.pur.receive-not-found",
            "入库单 {receiveId} 不存在", ARG_RECEIVE_ID);

    ErrorCode ERR_RECEIVE_LINES_EMPTY = ErrorCode.define("erp.err.pur.receive-lines-empty",
            "入库单 {receiveCode} 无行明细，不可提交审核",
            ARG_RECEIVE_CODE);

    ErrorCode ERR_PARTNER_INACTIVE = ErrorCode.define("erp.err.pur.partner-inactive",
            "供应商 {supplierId} 已停用，不可开单或审核",
            ARG_SUPPLIER_ID);

    ErrorCode ERR_INVALID_UNIT_PRICE = ErrorCode.define("erp.err.pur.invalid-unit-price",
            "入库单价格式非法：{priceText}",
            ARG_PRICE_TEXT);

    ErrorCode ERR_MOVE_NOT_FOUND = ErrorCode.define("erp.err.pur.move-not-found",
            "已审核入库单 {receiveCode} 找不到关联的入库移动单（数据不一致）",
            ARG_RECEIVE_CODE, ARG_MOVE_CODE);

    // --- 发票作用域错误码（消息文案绑定发票参数，避免复用入库单文案产生误导） ---

    ErrorCode ERR_INVOICE_ILLEGAL_STATUS_TRANSITION = ErrorCode.define("erp.err.pur.invoice-illegal-status-transition",
            "采购发票 {invoiceCode} 当前审核状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_INVOICE_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_INVOICE_ILLEGAL_DOC_STATUS_TRANSITION = ErrorCode.define("erp.err.pur.invoice-illegal-doc-status-transition",
            "采购发票 {invoiceCode} 当前单据状态={currentDocStatus}，不允许执行该操作（期望状态={expectedDocStatus}）",
            ARG_INVOICE_CODE, ARG_CURRENT_DOC_STATUS, ARG_EXPECTED_DOC_STATUS);

    ErrorCode ERR_INVOICE_LINES_EMPTY = ErrorCode.define("erp.err.pur.invoice-lines-empty",
            "采购发票 {invoiceCode} 无行明细，不可提交审核",
            ARG_INVOICE_CODE);

    ErrorCode ERR_INVOICE_NOT_FOUND = ErrorCode.define("erp.err.pur.invoice-not-found",
            "采购发票 {invoiceId} 不存在", ARG_INVOICE_ID);

    ErrorCode ERR_PAYMENT_NOT_FOUND = ErrorCode.define("erp.err.pur.payment-not-found",
            "付款单 {paymentId} 不存在", ARG_PAYMENT_ID);

    ErrorCode ERR_INVOICE_QTY_MISMATCH = ErrorCode.define("erp.err.pur.invoice-qty-mismatch",
            "采购发票 {invoiceCode} 第 {lineNo} 行发票数量 {invoiceQty} 超过入库数量 {receivedQty}，三单匹配失败",
            ARG_INVOICE_CODE, ARG_LINE_NO, ARG_INVOICE_QTY, ARG_RECEIVED_QTY);

    ErrorCode ERR_INVOICE_PRICE_MISMATCH = ErrorCode.define("erp.err.pur.invoice-price-mismatch",
            "采购发票 {invoiceCode} 第 {lineNo} 行发票单价 {invoicePrice} 与订单单价 {orderPrice} 差异超容差，三单匹配失败",
            ARG_INVOICE_CODE, ARG_LINE_NO, ARG_INVOICE_PRICE, ARG_ORDER_PRICE);

    // --- 付款作用域错误码 ---

    ErrorCode ERR_PAYMENT_ILLEGAL_STATUS_TRANSITION = ErrorCode.define("erp.err.pur.payment-illegal-status-transition",
            "付款单 {paymentCode} 当前审核状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_PAYMENT_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_PAYMENT_ILLEGAL_DOC_STATUS_TRANSITION = ErrorCode.define("erp.err.pur.payment-illegal-doc-status-transition",
            "付款单 {paymentCode} 当前单据状态={currentDocStatus}，不允许执行该操作（期望状态={expectedDocStatus}）",
            ARG_PAYMENT_CODE, ARG_CURRENT_DOC_STATUS, ARG_EXPECTED_DOC_STATUS);

    ErrorCode ERR_SETTLE_SUPPLIER_MISMATCH = ErrorCode.define("erp.err.pur.settle-supplier-mismatch",
            "付款单 {paymentCode} 与发票 {invoiceCode} 供应商不一致，不可核销",
            ARG_PAYMENT_CODE, ARG_INVOICE_CODE);

    ErrorCode ERR_SETTLE_INVOICE_NOT_APPROVED = ErrorCode.define("erp.err.pur.settle-invoice-not-approved",
            "发票 {invoiceCode} 未审核通过，不可核销（当前审核状态={currentStatus}）",
            ARG_INVOICE_CODE, ARG_CURRENT_STATUS);

    ErrorCode ERR_SETTLE_PAYMENT_NOT_APPROVED = ErrorCode.define("erp.err.pur.settle-payment-not-approved",
            "付款单 {paymentCode} 未审核通过，不可核销（当前审核状态={currentStatus}）",
            ARG_PAYMENT_CODE, ARG_CURRENT_STATUS);

    ErrorCode ERR_SETTLE_OVER_INVOICE_BALANCE = ErrorCode.define("erp.err.pur.settle-over-invoice-balance",
            "核销金额 {settleAmount} 超过发票 {invoiceCode} 未付余额 {invoiceBalance}",
            ARG_SETTLE_AMOUNT, ARG_INVOICE_CODE, ARG_INVOICE_BALANCE);

    ErrorCode ERR_SETTLE_OVER_PAYMENT_BALANCE = ErrorCode.define("erp.err.pur.settle-over-payment-balance",
            "核销金额 {settleAmount} 超过付款单 {paymentCode} 未核销余额 {paymentBalance}",
            ARG_SETTLE_AMOUNT, ARG_PAYMENT_CODE, ARG_PAYMENT_BALANCE);

    // --- 退货作用域错误码（消息文案绑定退货单参数，避免复用入库单/发票文案产生误导） ---

    ErrorCode ERR_RETURN_ILLEGAL_STATUS_TRANSITION = ErrorCode.define("erp.err.pur.return-illegal-status-transition",
            "采购退货单 {returnCode} 当前审核状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_RETURN_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_RETURN_ILLEGAL_DOC_STATUS_TRANSITION = ErrorCode.define("erp.err.pur.return-illegal-doc-status-transition",
            "采购退货单 {returnCode} 当前单据状态={currentDocStatus}，不允许执行该操作（期望状态={expectedDocStatus}）",
            ARG_RETURN_CODE, ARG_CURRENT_DOC_STATUS, ARG_EXPECTED_DOC_STATUS);

    ErrorCode ERR_RETURN_NOT_FOUND = ErrorCode.define("erp.err.pur.return-not-found",
            "采购退货单 {returnId} 不存在", ARG_RETURN_ID);

    ErrorCode ERR_RETURN_LINES_EMPTY = ErrorCode.define("erp.err.pur.return-lines-empty",
            "采购退货单 {returnCode} 无行明细，不可提交审核",
            ARG_RETURN_CODE);

    ErrorCode ERR_RETURN_QTY_EXCEED = ErrorCode.define("erp.err.pur.return-qty-exceed",
            "采购退货单 {returnCode} 第 {lineNo} 行退货数量 {invoiceQty} 超过可退数量 {maxReturnQty}（已入库 {receivedQtyReturn}）",
            ARG_RETURN_CODE, ARG_LINE_NO, ARG_INVOICE_QTY, ARG_MAX_RETURN_QTY, ARG_RECEIVED_QTY_RETURN);

    ErrorCode ERR_RETURN_RECEIVE_NOT_APPROVED = ErrorCode.define("erp.err.pur.return-receive-not-approved",
            "源入库单未审核通过，不可退货（当前审核状态={currentStatus}）",
            ARG_CURRENT_STATUS);

    ErrorCode ERR_RETURN_REASON_REQUIRED = ErrorCode.define("erp.err.pur.return-reason-required",
            "采购退货单 {returnCode} 第 {lineNo} 行缺少退货原因（按配置必填）",
            ARG_RETURN_CODE, ARG_LINE_NO);

    // 强制质检阻塞（plan 2026-07-02-2237-3 Phase 2）：入库单 {receiveCode} 属强制质检类型，
    // 关联质检单未得出合格/让步结论，审核暂挂（首次审核已生成 PENDING 质检单，待质检结论后再次审核放行）
    ErrorCode ERR_RECEIVE_INSPECTION_BLOCKED = ErrorCode.define("erp.err.pur.receive-inspection-blocked",
            "入库单 {receiveCode} 属强制质检类型，关联质检单未合格/让步，审核暂挂待质检结论",
            ARG_RECEIVE_CODE);
}
