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
    String ARG_TRANSFER_CODE = "transferCode";
    String ARG_TRANSFER_TYPE = "transferType";
    String ARG_FROM_OWNERSHIP_TYPE = "fromOwnershipType";
    String ARG_TO_OWNERSHIP_TYPE = "toOwnershipType";
    String ARG_SOURCE_LOC_ID = "sourceLocId";
    String ARG_DEST_LOC_ID = "destLocId";
    String ARG_REQUIRED_QTY = "requiredQty";
    String ARG_AVAILABLE_QTY = "availableQty";

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

    ErrorCode ERR_COST_NOT_AVAILABLE = ErrorCode.define("erp.err.inv.cost-not-available",
            "物料 {materialId} / 仓库 {warehouseId} 无可用成本层，请先入库后再出库",
            ARG_MATERIAL_ID, ARG_WAREHOUSE_ID);

    ErrorCode ERR_STANDARD_COST_NOT_AVAILABLE = ErrorCode.define("erp.err.inv.standard-cost-not-available",
            "物料 {materialId} 无可用标准成本（无已 FIRMED 的成本卷算行，且物料主数据未配置标准成本），不可使用 STANDARD 计价方法",
            ARG_MATERIAL_ID);

    // ---------- 所有权转移（consignment.md §ErpInvOwnershipTransfer） ----------

    ErrorCode ERR_OWNERSHIP_TRANSFER_NOT_FOUND = ErrorCode.define("erp.err.inv.ownership-transfer-not-found",
            "所有权转移单 {transferId} 不存在", "transferId");

    ErrorCode ERR_OWNERSHIP_TRANSFER_ILLEGAL_STATUS = ErrorCode.define("erp.err.inv.ownership-transfer-illegal-status",
            "所有权转移单 {transferCode} 当前状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_TRANSFER_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_OWNERSHIP_TRACKING_DISABLED = ErrorCode.define("erp.err.inv.ownership-tracking-disabled",
            "所有权维度未启用（erp-inv.ownership-tracking-enabled=false），不可执行所有权转移调账",
            ARG_TRANSFER_CODE);

    ErrorCode ERR_OWNERSHIP_TRANSFER_LOC_MISMATCH = ErrorCode.define("erp.err.inv.ownership-transfer-loc-mismatch",
            "所有权转移物理位置必须不变（sourceLocId=destLocId），当前 sourceLocId={sourceLocId} destLocId={destLocId}",
            ARG_TRANSFER_CODE, ARG_SOURCE_LOC_ID, ARG_DEST_LOC_ID);

    ErrorCode ERR_OWNERSHIP_TRANSFER_TYPE_INCONSISTENT = ErrorCode.define("erp.err.inv.ownership-transfer-type-inconsistent",
            "转移类型 {transferType} 与所有权类型迁移 from={fromOwnershipType}→to={toOwnershipType} 不一致",
            ARG_TRANSFER_CODE, ARG_TRANSFER_TYPE, ARG_FROM_OWNERSHIP_TYPE, ARG_TO_OWNERSHIP_TYPE);

    ErrorCode ERR_OWNERSHIP_TRANSFER_INSUFFICIENT = ErrorCode.define("erp.err.inv.ownership-transfer-insufficient",
            "所有权转移余额不足：物料 {materialId} / 所有权类型 {fromOwnershipType}，可用={availableQty}，需要={requiredQty}",
            ARG_TRANSFER_CODE, ARG_MATERIAL_ID, ARG_FROM_OWNERSHIP_TYPE, ARG_AVAILABLE_QTY, ARG_REQUIRED_QTY);
}
