package app.erp.inv.service;

import io.nop.api.core.exceptions.ErrorCode;

/**
 * 库存域业务异常错误码。所有库存流程中的业务异常使用 {@link io.nop.api.core.exceptions.NopException} + 本接口的 {@link ErrorCode}。
 */
public interface ErpInvErrors {

    String ARG_MOVE_ID = "moveId";
    String ARG_MOVE_CODE = "moveCode";
    String ARG_CURRENT_STATUS = "currentStatus";
    String ARG_EXPECTED_STATUS = "expectedStatus";
    String ARG_MATERIAL_ID = "materialId";
    String ARG_WAREHOUSE_ID = "warehouseId";
    String ARG_AVAILABLE = "available";
    String ARG_REQUIRED = "required";
    String ARG_RELATED_BILL_TYPE = "relatedBillType";
    String ARG_RELATED_BILL_CODE = "relatedBillCode";

    ErrorCode ERR_ILLEGAL_STATUS_TRANSITION = ErrorCode.define("erp.err.inv.illegal-status-transition",
            "移动单 {moveCode} 当前状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_MOVE_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_AVAILABLE_INSUFFICIENT = ErrorCode.define("erp.err.inv.available-insufficient",
            "可用量不足：物料 {materialId} / 仓库 {warehouseId}，可用={available}，需要={required}",
            ARG_MATERIAL_ID, ARG_WAREHOUSE_ID, ARG_AVAILABLE, ARG_REQUIRED);

    ErrorCode ERR_MOVE_NOT_FOUND = ErrorCode.define("erp.err.inv.move-not-found",
            "移动单 {moveId} 不存在", ARG_MOVE_ID);

    ErrorCode ERR_REVERSE_NOT_DONE = ErrorCode.define("erp.err.inv.reverse-not-done",
            "仅已完成移动单可冲销，移动单 {moveCode} 当前状态={currentStatus}",
            ARG_MOVE_CODE, ARG_CURRENT_STATUS);

    ErrorCode ERR_LEDGER_ALREADY_EXISTS = ErrorCode.define("erp.err.inv.ledger-already-exists",
            "移动单行 {moveLineId} 已存在库存流水，流水不可变",
            "moveLineId");
}
