package app.erp.inv.service.costing;

import app.erp.inv.service.ErpInvConfigs;

import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.inv.service.ErpInvConstants;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 全月一次加权平均成本策略（{@code ErpInvConstants.COST_METHOD_MONTHLY_WEIGHTED_AVERAGE}=20）。
 *
 * <p>与 {@link MovingAverageCostingStrategy} 的关键区别：移动加权平均在<b>每次入库</b>时即时重算 avgCost，
 * 而全月一次加权平均在期<b>内入库不改变 avgCost</b>——出库使用期初（上月末）加权平均作为暂估成本，
 * 月末结账时统一重算全月实际加权平均并调整出库流水。
 *
 * <p><b>实时路径</b>：
 * <ul>
 *   <li>{@code onIncoming}：累加 totalQuantity/totalCost，<b>不更新</b> avgCost（保持期初值）</li>
 *   <li>{@code onOutgoing}：取 {@code balance.avgCost}（期初加权平均）作为暂估单位成本</li>
 * </ul>
 *
 * <p><b>期末调整</b>（{@code ErpInvCostingBizModel.reclosePeriodCosts} WEIGHTED_AVERAGE 分支）：
 * 全月实际加权平均 = (期初 totalCost + 本期入库 totalCost) / (期初 qty + 本期入库 qty)；
 * 将本期所有出库流水的 unitCost/totalCost 调整为实际值，并更新余额 avgCost。
 *
 * <p>权威：{@code docs/design/finance/costing-methods.md §WEIGHTED_AVERAGE}。
 */
public class WeightedAverageCostingStrategy implements CostingStrategy {

    static final int SCALE = 6;

    @Override
    public String costMethod() {
        return ErpInvConstants.COST_METHOD_MONTHLY_WEIGHTED_AVERAGE;
    }

    /**
     * 入库：累加 totalQuantity/totalCost，但<b>不重算 avgCost</b>——保持期初（上月末）加权平均值，
     * 供期内出库作为暂估成本。月末结账时统一调整。
     */
    @Override
    public BigDecimal onIncoming(ErpInvStockMove move, ErpInvStockMoveLine line, Long acctSchemaId,
                                 BigDecimal unitCost, BookingContext ctx) {
        Long warehouseId = move.getDestWarehouseId();
        Long locationId = line.getDestLocationId() != null ? line.getDestLocationId() : move.getDestWarehouseId();
        ErpInvStockBalance balance = ctx.upsertBalance(move, line, warehouseId, locationId);
        BigDecimal qty = nz(line.getQuantity());
        BigDecimal lineTotalCost = unitCost.multiply(qty);

        ErpInvStockBalance updated = ctx.updateBalanceWithRetry(balance, b -> {
            b.setTotalQuantity(nz(b.getTotalQuantity()).add(qty));
            b.setTotalCost(nz(b.getTotalCost()).add(lineTotalCost));
            b.setCostMethod(ErpInvConstants.COST_METHOD_MONTHLY_WEIGHTED_AVERAGE);
            ctx.recomputeAvailable(b);
        });

        ctx.writeLedger(move, line, acctSchemaId, updated, warehouseId, locationId, qty, unitCost, lineTotalCost,
                ErpInvConstants.COST_METHOD_MONTHLY_WEIGHTED_AVERAGE);
        return unitCost;
    }

    /**
     * 出库：取 {@code balance.avgCost}（期初加权平均）作为暂估成本。月末结账时经
     * {@code ErpInvCostingBizModel} 调整为全月实际加权平均。
     */
    @Override
    public BigDecimal onOutgoing(ErpInvStockMove move, ErpInvStockMoveLine line, Long acctSchemaId,
                                 BookingContext ctx) {
        Long warehouseId = move.getSourceWarehouseId();
        Long locationId = line.getSourceLocationId() != null ? line.getSourceLocationId()
                : move.getSourceWarehouseId();
        ErpInvStockBalance balance = ctx.upsertBalance(move, line, warehouseId, locationId);
        BigDecimal qty = nz(line.getQuantity());
        BigDecimal unitCost = nz(balance.getAvgCost());
        BigDecimal lineTotalCost = unitCost.multiply(qty);

        ErpInvStockBalance updated = ctx.updateBalanceWithRetry(balance, b -> {
            BigDecimal oldTotal = nz(b.getTotalQuantity());
            BigDecimal oldTotalCost = nz(b.getTotalCost());
            BigDecimal newTotal = oldTotal.subtract(qty);
            BigDecimal newTotalCost = oldTotalCost.subtract(lineTotalCost);
            b.setTotalQuantity(newTotal);
            b.setTotalCost(newTotalCost);
            ctx.recomputeAvailable(b);
        });

        ctx.writeLedger(move, line, acctSchemaId, updated, warehouseId, locationId, qty.negate(), unitCost,
                lineTotalCost.negate(), ErpInvConstants.COST_METHOD_MONTHLY_WEIGHTED_AVERAGE);
        return unitCost;
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    static BigDecimal computeWeightedAverage(BigDecimal totalCost, BigDecimal totalQty) {
        if (totalQty == null || totalQty.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return nz(totalCost).divide(totalQty, SCALE, RoundingMode.HALF_UP);
    }
}
