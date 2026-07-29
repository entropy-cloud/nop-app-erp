package app.erp.inv.service.costing;

import app.erp.inv.service.ErpInvConfigs;

import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.inv.service.ErpInvConstants;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
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

    @Inject
    IDaoProvider daoProvider;

    @Override
    public String costMethod() {
        return ErpInvConstants.COST_METHOD_STANDARD;
    }

    @Override
    public BigDecimal onIncoming(ErpInvStockMove move, ErpInvStockMoveLine line, Long acctSchemaId,
                                 BigDecimal unitCost, BookingContext ctx) {
        Long warehouseId = move.getDestWarehouseId();
        Long locationId = line.getDestLocationId() != null ? line.getDestLocationId() : move.getDestWarehouseId();
        ErpInvStockBalance balance = ctx.upsertBalance(move, line, warehouseId, locationId);

        // 红冲不变量（P1-MA2-024，Choice B）：正常采购入库 line.unitCost 持「实际采购价」（PPV 经
        // InvPostingDispatcher.dispatchPurchasePriceVariance:125 读此值与标准 ledger.unitCost 比对），
        // 故不可一律采用传入 unitCost。仅当本入库为冲销反向入库（move.originReturnedMoveId != null，
        // reverse:144 从原出库行透传、onOutgoing 已刷新为出库时标准成本）且传入值有效时，采用之——
        // 跨 STANDARD_REVALUATION 时反向入库沿用原出库扣减的旧标准成本，保证 balance.totalCost 不变量。
        // 其他场景（正常采购入库 / 内部调拨目的侧 originReturnedMoveId=null）一律重解析当前标准成本。
        BigDecimal standardUnitCost;
        if (move.getOriginReturnedMoveId() != null && unitCost != null && unitCost.signum() > 0) {
            standardUnitCost = unitCost;
        } else {
            standardUnitCost = standardCostResolver.resolve(line.getMaterialId());
        }
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

        // 红冲不变量（P1-MA2-024）：刷回 line.unitCost 为出库时标准成本，供 reverse:144 透传给反向入库行
        // （对齐 FIFO:131-132 onOutgoing 刷新加权 unitCost 的范式），跨 STANDARD_REVALUATION 时反向入库沿用此值。
        line.setUnitCost(ErpInvConfigs.roundCost(standardUnitCost));
        daoProvider.daoFor(ErpInvStockMoveLine.class).saveOrUpdateEntity(line);

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
