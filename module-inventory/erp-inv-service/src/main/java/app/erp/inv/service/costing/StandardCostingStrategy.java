package app.erp.inv.service.costing;

import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.inv.service.ErpInvConstants;
import jakarta.inject.Inject;

import java.math.BigDecimal;

/**
 * 标准成本法策略（{@code ErpInvConstants.COST_METHOD_STANDARD}=50，plan 2026-07-05-0427-2）。
 *
 * <p>入库：按标准成本写 {@code ledger.unitCost/totalCost} + 累加余额；实际成本经 PPV 通道分离
 * （{@code InvPostingDispatcher} 采购入库 DONE 时捕获差异 → {@code PURCHASE_PRICE_VARIANCE} 凭证）。
 *
 * <p>出库：{@code unitCost=标准成本}，写 {@code ledger.unitCost/totalCost} 走既有 {@code InvPostingDispatcher}
 * 拾取（COGS 通道零改动，同 FIFO/移动加权平均范式）。
 *
 * <p>无标准成本时抛 {@link app.erp.inv.service.ErpInvErrors#ERR_STANDARD_COST_NOT_AVAILABLE}。
 *
 * <p>权威：{@code docs/design/finance/costing-methods.md}（STANDARD 方法）。
 */
public class StandardCostingStrategy implements CostingStrategy {

    @Inject
    StandardCostResolver standardCostResolver;

    @Override
    public String costMethod() {
        return ErpInvConstants.COST_METHOD_STANDARD;
    }

    @Override
    public BigDecimal onIncoming(ErpInvStockMove move, ErpInvStockMoveLine line, Long acctSchemaId,
                                 BigDecimal unitCost, BookingContext ctx) {
        Long warehouseId = move.getDestWarehouseId();
        Long locationId = line.getDestLocationId() != null ? line.getDestLocationId() : move.getDestLocationId();
        ErpInvStockBalance balance = ctx.upsertBalance(move, line, warehouseId, locationId);

        BigDecimal standardUnitCost = standardCostResolver.resolve(line.getMaterialId());
        BigDecimal qty = nz(line.getQuantity());
        BigDecimal lineTotalCost = standardUnitCost.multiply(qty);

        ErpInvStockBalance updated = ctx.updateBalanceWithRetry(balance, b -> {
            BigDecimal oldTotal = nz(b.getTotalQuantity());
            BigDecimal oldTotalCost = nz(b.getTotalCost());
            b.setTotalQuantity(oldTotal.add(qty));
            b.setTotalCost(oldTotalCost.add(lineTotalCost));
            b.setCostMethod(ErpInvConstants.COST_METHOD_STANDARD);
            b.setAvgCost(standardUnitCost);
            ctx.recomputeAvailable(b);
        });

        ctx.writeLedger(move, line, acctSchemaId, updated, warehouseId, locationId, qty, standardUnitCost,
                lineTotalCost, ErpInvConstants.COST_METHOD_STANDARD);
        return standardUnitCost;
    }

    @Override
    public BigDecimal onOutgoing(ErpInvStockMove move, ErpInvStockMoveLine line, Long acctSchemaId,
                                 BookingContext ctx) {
        Long warehouseId = move.getSourceWarehouseId();
        Long locationId = line.getSourceLocationId() != null ? line.getSourceLocationId()
                : move.getSourceLocationId();
        ErpInvStockBalance balance = ctx.upsertBalance(move, line, warehouseId, locationId);

        BigDecimal standardUnitCost = standardCostResolver.resolve(line.getMaterialId());
        BigDecimal qty = nz(line.getQuantity());
        BigDecimal lineTotalCost = standardUnitCost.multiply(qty);

        ErpInvStockBalance updated = ctx.updateBalanceWithRetry(balance, b -> {
            BigDecimal oldTotal = nz(b.getTotalQuantity());
            BigDecimal oldTotalCost = nz(b.getTotalCost());
            b.setTotalQuantity(oldTotal.subtract(qty));
            b.setTotalCost(oldTotalCost.subtract(lineTotalCost));
            b.setCostMethod(ErpInvConstants.COST_METHOD_STANDARD);
            b.setAvgCost(standardUnitCost);
            ctx.recomputeAvailable(b);
        });

        ctx.writeLedger(move, line, acctSchemaId, updated, warehouseId, locationId, qty.negate(),
                standardUnitCost, lineTotalCost.negate(), ErpInvConstants.COST_METHOD_STANDARD);
        return standardUnitCost;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
