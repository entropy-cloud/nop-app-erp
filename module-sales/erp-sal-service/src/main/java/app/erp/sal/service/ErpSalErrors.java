package app.erp.sal.service;

import io.nop.api.core.exceptions.ErrorCode;

/**
 * 销售域业务异常错误码。所有销售流程中的业务异常使用 {@link io.nop.api.core.exceptions.NopException} + 本接口的 {@link ErrorCode}。
 */
public interface ErpSalErrors {

    String ARG_DELIVERY_ID = "deliveryId";
    String ARG_DELIVERY_CODE = "deliveryCode";
    String ARG_CURRENT_STATUS = "currentStatus";
    String ARG_EXPECTED_STATUS = "expectedStatus";
    String ARG_CURRENT_DOC_STATUS = "currentDocStatus";
    String ARG_EXPECTED_DOC_STATUS = "expectedDocStatus";
    String ARG_CUSTOMER_ID = "customerId";
    String ARG_MOVE_CODE = "moveCode";

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
}
