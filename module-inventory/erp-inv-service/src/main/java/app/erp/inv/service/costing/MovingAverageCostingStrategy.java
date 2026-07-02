package app.erp.inv.service.costing;

import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.inv.dao.entity.ErpInvStockLedger;
import app.erp.inv.service.ErpInvConstants;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 移动加权平均成本策略（{@code ErpInvConstants.COST_METHOD_MOVING_AVERAGE}=10）。
 *
 * <p>本类是 {@link app.erp.inv.service.stock.StockMoveBookkeeper} 既有 bookIncoming/bookOutgoing/writeLedger
 * 逻辑的逐字节抽取——入库重算 {@code avgCost=(旧totalCost+入库cost)/(旧totalQty+入库qty)}，
 * 出库取 {@code unitCost=balance.avgCost}，扣减余额。任何字段顺序/舍入/符号变更都会破坏既有套件门控。
 *
 * <p>权威：{@code docs/design/inventory/cross-domain.md}（余额/流水同事务 + 移动加权平均由流水维护）。
 */
public class MovingAverageCostingStrategy implements CostingStrategy {

    static final int SCALE = 6;

    @Inject
    IDaoProvider daoProvider;

    @Override
    public int costMethod() {
        return ErpInvConstants.COST_METHOD_MOVING_AVERAGE;
    }

    @Override
    public BigDecimal onIncoming(ErpInvStockMove move, ErpInvStockMoveLine line, Long acctSchemaId,
                                 BigDecimal unitCost, BookingContext ctx) {
        Long warehouseId = move.getDestWarehouseId();
        Long locationId = line.getDestLocationId() != null ? line.getDestLocationId() : move.getDestLocationId();
        ErpInvStockBalance balance = ctx.upsertBalance(move, line, warehouseId, locationId);
        BigDecimal qty = nz(line.getQuantity());
        BigDecimal lineTotalCost = unitCost.multiply(qty);

        BigDecimal oldTotal = nz(balance.getTotalQuantity());
        BigDecimal oldTotalCost = nz(balance.getTotalCost());
        BigDecimal newTotal = oldTotal.add(qty);
        BigDecimal newTotalCost = oldTotalCost.add(lineTotalCost);
        BigDecimal newAvg = newTotal.signum() != 0
                ? newTotalCost.divide(newTotal, SCALE, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        balance.setTotalQuantity(newTotal);
        balance.setTotalCost(newTotalCost);
        balance.setAvgCost(newAvg);
        ctx.recomputeAvailable(balance);
        daoProvider.daoFor(ErpInvStockBalance.class).saveOrUpdateEntity(balance);

        ctx.writeLedger(move, line, acctSchemaId, balance, warehouseId, locationId, qty, unitCost, lineTotalCost,
                ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
        return unitCost;
    }

    @Override
    public BigDecimal onOutgoing(ErpInvStockMove move, ErpInvStockMoveLine line, Long acctSchemaId,
                                 BookingContext ctx) {
        Long warehouseId = move.getSourceWarehouseId();
        Long locationId = line.getSourceLocationId() != null ? line.getSourceLocationId()
                : move.getSourceLocationId();
        ErpInvStockBalance balance = ctx.upsertBalance(move, line, warehouseId, locationId);
        BigDecimal qty = nz(line.getQuantity());
        BigDecimal unitCost = nz(balance.getAvgCost());
        BigDecimal lineTotalCost = unitCost.multiply(qty);

        BigDecimal oldTotal = nz(balance.getTotalQuantity());
        BigDecimal oldTotalCost = nz(balance.getTotalCost());
        BigDecimal newTotal = oldTotal.subtract(qty);
        BigDecimal newTotalCost = oldTotalCost.subtract(lineTotalCost);
        BigDecimal newAvg = newTotal.signum() != 0
                ? newTotalCost.divide(newTotal, SCALE, RoundingMode.HALF_UP) : unitCost;

        balance.setTotalQuantity(newTotal);
        balance.setTotalCost(newTotalCost);
        balance.setAvgCost(newAvg);
        ctx.recomputeAvailable(balance);
        daoProvider.daoFor(ErpInvStockBalance.class).saveOrUpdateEntity(balance);

        ctx.writeLedger(move, line, acctSchemaId, balance, warehouseId, locationId, qty.negate(), unitCost,
                lineTotalCost.negate(), ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
        return unitCost;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
