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
    String ARG_ADJUST_ID = "adjustId";
    String ARG_ADJUST_CODE = "adjustCode";
    String ARG_UNIT_COST = "unitCost";
    String ARG_BALANCE_ID = "balanceId";
    String ARG_ATTEMPTS = "attempts";
    String ARG_LANDED_COST_ID = "landedCostId";
    String ARG_LANDED_COST_CODE = "landedCostCode";
    String ARG_RECEIVE_ID = "receiveId";

    // --- 报表渲染作用域参数键 ---
    String ARG_REPORT_NAME = "reportName";
    String ARG_RENDER_TYPE = "renderType";

    /** O-23：moveLineId 参数键命名常量（替代字符串字面量）。 */
    String ARG_MOVE_LINE_ID = "moveLineId";

    ErrorCode ERR_ILLEGAL_STATUS_TRANSITION = ErrorCode.define("erp.err.inv.illegal-status-transition",
            "移动单 {moveCode} 当前状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_MOVE_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_AVAILABLE_INSUFFICIENT = ErrorCode.define("erp.err.inv.available-insufficient",
            "可用量不足：物料 {materialId} / 仓库 {warehouseId}，可用={available}，需要={required}",
            ARG_MATERIAL_ID, ARG_WAREHOUSE_ID, ARG_AVAILABLE, ARG_REQUIRED);

    // 并发扣减乐观锁冲突重试耗尽（plan 2026-07-07-0024-2；UC-INV-08；concurrency-and-transactions.md §模式四）
    ErrorCode ERR_INV_CONCURRENT_DEDUCT_CONFLICT = ErrorCode.define("erp.err.inv.concurrent-deduct-conflict",
            "库存余额 {balanceId} 并发扣减乐观锁冲突，重试 {attempts} 次后仍失败，请重试或检查并发负载",
            ARG_BALANCE_ID, ARG_ATTEMPTS);

    ErrorCode ERR_MOVE_NOT_FOUND = ErrorCode.define("erp.err.inv.move-not-found",
            "移动单 {moveId} 不存在", ARG_MOVE_ID);

    ErrorCode ERR_REVERSE_NOT_DONE = ErrorCode.define("erp.err.inv.reverse-not-done",
            "仅已完成移动单可冲销，移动单 {moveCode} 当前状态={currentStatus}",
            ARG_MOVE_CODE, ARG_CURRENT_STATUS);

    ErrorCode ERR_LEDGER_ALREADY_EXISTS = ErrorCode.define("erp.err.inv.ledger-already-exists",
            "移动单行 {moveLineId} 已存在库存流水，流水不可变",
            ARG_MOVE_LINE_ID);

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

    // ---------- 成本调整（plan 2026-07-05-2352-3） ----------

    ErrorCode ERR_COST_ADJUST_NOT_FOUND = ErrorCode.define("erp.err.inv.cost-adjust-not-found",
            "成本调整单 {adjustId} 不存在", ARG_ADJUST_ID);

    ErrorCode ERR_COST_ADJUST_ALREADY_APPLIED = ErrorCode.define("erp.err.inv.cost-adjust-already-applied",
            "成本调整单 {adjustCode} 已执行过账，不可重复 apply",
            ARG_ADJUST_CODE);

    ErrorCode ERR_COST_ADJUST_NOT_APPROVED = ErrorCode.define("erp.err.inv.cost-adjust-not-approved",
            "成本调整单 {adjustCode} 未审核（当前审核状态={currentStatus}），审批门控开启时须先审核",
            ARG_ADJUST_CODE, ARG_CURRENT_STATUS);

    ErrorCode ERR_COST_ADJUST_NO_BALANCE = ErrorCode.define("erp.err.inv.cost-adjust-no-balance",
            "物料 {materialId} / 仓库 {warehouseId} 无库存余额或余量为零，不可成本调整",
            ARG_MATERIAL_ID, ARG_WAREHOUSE_ID);

    ErrorCode ERR_COST_ADJUST_NEGATIVE_COST = ErrorCode.define("erp.err.inv.cost-adjust-negative-cost",
            "成本调整单 {adjustCode} 新单位成本 {unitCost} 不可为负",
            ARG_ADJUST_CODE, ARG_UNIT_COST);

    ErrorCode ERR_COST_ADJUST_NOT_APPLIED = ErrorCode.define("erp.err.inv.cost-adjust-not-applied",
            "成本调整单 {adjustCode} 未过账，不可冲销",
            ARG_ADJUST_CODE);

    // ---------- 到岸成本（plan 2026-07-10-1100-3） ----------

    ErrorCode ERR_LANDED_COST_NOT_FOUND = ErrorCode.define("erp.err.inv.landed-cost-not-found",
            "到岸成本单 {landedCostId} 不存在", ARG_LANDED_COST_ID);

    ErrorCode ERR_LANDED_COST_RECEIVE_NOT_APPROVED = ErrorCode.define("erp.err.inv.landed-cost-receive-not-approved",
            "关联采购入库单 {receiveId} 未审核，不可分摊到岸成本", ARG_RECEIVE_ID);

    ErrorCode ERR_LANDED_COST_ALREADY_ALLOCATED = ErrorCode.define("erp.err.inv.landed-cost-already-allocated",
            "采购入库单 {receiveId} 已有审核完成的到岸成本单 {landedCostCode}，不可重复分摊",
            ARG_RECEIVE_ID, ARG_LANDED_COST_CODE);

    ErrorCode ERR_LANDED_COST_ALREADY_APPROVED = ErrorCode.define("erp.err.inv.landed-cost-already-approved",
            "到岸成本单 {landedCostCode} 已审核，不可重复审核", ARG_LANDED_COST_CODE);

    ErrorCode ERR_LANDED_COST_NO_LINES = ErrorCode.define("erp.err.inv.landed-cost-no-lines",
            "到岸成本单 {landedCostCode} 无费用行，不可分摊", ARG_LANDED_COST_CODE);

    ErrorCode ERR_LANDED_COST_DRAFT_EXISTS = ErrorCode.define("erp.err.inv.landed-cost-draft-exists",
            "采购入库单 {receiveId} 已有未取消的到岸成本单，不可重复创建", ARG_RECEIVE_ID);

    ErrorCode ERR_LANDED_COST_RECEIVE_NOT_FOUND = ErrorCode.define("erp.err.inv.landed-cost-receive-not-found",
            "采购入库单 {receiveCode} 不存在", "receiveCode");

    // --- 报表渲染作用域（镜像 ErpMfgErrors.ERR_REPORT_*，库存域独立错误码，不跨域 import） ---

    ErrorCode ERR_REPORT_NAME_INVALID = ErrorCode.define("erp.err.inv.report.name-invalid",
            "报表名[{reportName}]非法（含路径注入字符或不合规段），拒绝渲染",
            ARG_REPORT_NAME);

    ErrorCode ERR_REPORT_RENDER_TYPE_INVALID = ErrorCode.define("erp.err.inv.report.render-type-invalid",
            "渲染类型[{renderType}]非法（仅允许 html/xlsx/pdf）",
            ARG_RENDER_TYPE);
}
