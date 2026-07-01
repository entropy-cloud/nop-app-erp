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
}
