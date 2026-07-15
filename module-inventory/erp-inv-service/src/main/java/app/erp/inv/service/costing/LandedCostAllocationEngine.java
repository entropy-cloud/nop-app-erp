package app.erp.inv.service.costing;

import app.erp.inv.service.ErpInvConfigs;

import app.erp.inv.service.ErpInvConstants;
import io.nop.api.core.exceptions.NopException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 到岸成本分摊引擎（plan 2026-07-10-1100-3；costing-methods.md §到岸成本分摊）。
 *
 * <p>纯函数式引擎——输入入库行 + 费用要素行 + 分摊方法，输出每入库行的分摊金额与新单位成本，
 * 不触碰 ORM session。被 {@code ErpInvLandedCostProcessor} 在审核时调用，也可被 allocate query
 * 用于前端分摊预览（不落库）。
 *
 * <p>分摊基数按方法选择：
 * <ul>
 *   <li>BY_AMOUNT：基数 = Σ 入库行 amount（采购金额）</li>
 *   <li>BY_QUANTITY：基数 = Σ 入库行 quantity</li>
 *   <li>BY_WEIGHT：基数 = Σ 入库行 weight（本期从物料主数据读取或手工录入，见 Non-Goal）</li>
 * </ul>
 *
 * <p>分摊公式（costing-methods.md:341-365 算例）：
 * <pre>
 *   share = receiveLine.base / totalBase
 *   allocatedAmount = Σ(costElement.amount) × share
 *   newUnitCost = receiveLine.unitPrice + allocatedAmount / receiveLine.quantity
 * </pre>
 */
public class LandedCostAllocationEngine {

    static final int SCALE = 6;

    /**
     * 执行分摊计算。
     *
     * @param receiveLines    入库行列表（每行携带 amount/quantity/weight/unitPrice/materialId）
     * @param totalCostAmount 到岸成本合计（Σ 费用要素金额）
     * @param allocationMethod 分摊方法（BY_AMOUNT/BY_QUANTITY/BY_WEIGHT）
     * @return 每入库行的分摊结果列表（按入库行顺序）
     */
    public List<AllocationResult> allocate(List<ReceiveLineInput> receiveLines,
                                            BigDecimal totalCostAmount,
                                            String allocationMethod) {
        if (receiveLines == null || receiveLines.isEmpty()) {
            throw new NopException(app.erp.inv.service.ErpInvErrors.ERR_LANDED_COST_NO_LINES)
                    .param(app.erp.inv.service.ErpInvErrors.ARG_LANDED_COST_CODE, "N/A");
        }

        BigDecimal totalBase = BigDecimal.ZERO;
        for (ReceiveLineInput line : receiveLines) {
            totalBase = totalBase.add(baseOf(line, allocationMethod));
        }
        if (totalBase.signum() == 0) {
            throw new NopException(app.erp.inv.service.ErpInvErrors.ERR_LANDED_COST_NO_LINES)
                    .param(app.erp.inv.service.ErpInvErrors.ARG_LANDED_COST_CODE, "N/A");
        }

        BigDecimal totalToAllocate = nz(totalCostAmount);
        List<AllocationResult> results = new ArrayList<>(receiveLines.size());
        BigDecimal allocated = BigDecimal.ZERO;

        for (int i = 0; i < receiveLines.size(); i++) {
            ReceiveLineInput line = receiveLines.get(i);
            BigDecimal lineBase = baseOf(line, allocationMethod);
            BigDecimal allocatedAmount;
            if (i == receiveLines.size() - 1) {
                // 末行吸收舍入差，保证 Σ allocatedAmount == totalToAllocate
                allocatedAmount = totalToAllocate.subtract(allocated);
            } else {
                allocatedAmount = totalToAllocate.multiply(lineBase)
                        .divide(totalBase, SCALE, RoundingMode.HALF_UP)
                        .setScale(4, RoundingMode.HALF_UP);
                allocated = allocated.add(allocatedAmount);
            }

            BigDecimal newUnitCost = line.getUnitPrice();
            BigDecimal qty = nz(line.getQuantity());
            if (qty.signum() != 0) {
                newUnitCost = nz(line.getUnitPrice())
                        .add(allocatedAmount.divide(qty, SCALE, RoundingMode.HALF_UP))
                        .setScale(4, RoundingMode.HALF_UP);
            }

            results.add(new AllocationResult(
                    line.getReceiveLineId(),
                    line.getMaterialId(),
                    line.getWarehouseId(),
                    allocatedAmount,
                    newUnitCost
            ));
        }
        return results;
    }

    private BigDecimal baseOf(ReceiveLineInput line, String allocationMethod) {
        if (Objects.equals(allocationMethod, ErpInvConstants.ALLOC_METHOD_BY_QUANTITY)) {
            return nz(line.getQuantity());
        }
        if (Objects.equals(allocationMethod, ErpInvConstants.ALLOC_METHOD_BY_WEIGHT)) {
            return nz(line.getWeight());
        }
        // BY_AMOUNT（默认）
        return nz(line.getAmount());
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    /**
     * 入库行输入 DTO。
     */
    public static class ReceiveLineInput {
        private final Long receiveLineId;
        private final Long materialId;
        private final Long warehouseId;
        private final BigDecimal quantity;
        private final BigDecimal amount;
        private final BigDecimal unitPrice;
        private final BigDecimal weight;

        public ReceiveLineInput(Long receiveLineId, Long materialId, Long warehouseId,
                                 BigDecimal quantity, BigDecimal amount, BigDecimal unitPrice,
                                 BigDecimal weight) {
            this.receiveLineId = receiveLineId;
            this.materialId = materialId;
            this.warehouseId = warehouseId;
            this.quantity = quantity;
            this.amount = amount;
            this.unitPrice = unitPrice;
            this.weight = weight;
        }

        public Long getReceiveLineId() { return receiveLineId; }
        public Long getMaterialId() { return materialId; }
        public Long getWarehouseId() { return warehouseId; }
        public BigDecimal getQuantity() { return quantity; }
        public BigDecimal getAmount() { return amount; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public BigDecimal getWeight() { return weight; }
    }

    /**
     * 分摊结果 DTO。
     */
    public static class AllocationResult {
        private final Long receiveLineId;
        private final Long materialId;
        private final Long warehouseId;
        private final BigDecimal allocatedAmount;
        private final BigDecimal newUnitCost;

        public AllocationResult(Long receiveLineId, Long materialId, Long warehouseId,
                                 BigDecimal allocatedAmount, BigDecimal newUnitCost) {
            this.receiveLineId = receiveLineId;
            this.materialId = materialId;
            this.warehouseId = warehouseId;
            this.allocatedAmount = allocatedAmount;
            this.newUnitCost = newUnitCost;
        }

        public Long getReceiveLineId() { return receiveLineId; }
        public Long getMaterialId() { return materialId; }
        public Long getWarehouseId() { return warehouseId; }
        public BigDecimal getAllocatedAmount() { return allocatedAmount; }
        public BigDecimal getNewUnitCost() { return newUnitCost; }
    }
}
