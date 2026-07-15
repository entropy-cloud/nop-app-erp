package app.erp.inv.service.costing;

import app.erp.inv.service.ErpInvConfigs;


import app.erp.inv.dao.entity.ErpInvCostLayer;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.inv.service.ErpInvConstants;
import app.erp.inv.service.ErpInvErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.gt;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * 批次成本策略（{@code ErpInvConstants.COST_METHOD_BATCH}=70）。
 *
 * <p>入库按批次创建 {@link ErpInvCostLayer}（costMethod=BATCH）；出库时按移动单行的 {@code batchNo}
 * 精确匹配同批次成本层，批次内按 {@code incomingDate} 升序消耗（批次内 FIFO）。
 *
 * <p>与 {@link SpecificCostingStrategy} 的区别：SPECIFIC 每批独立计价且消耗时不排序（一批一层直接取），
 * BATCH 允许同批次多次入库产生多层，按 FIFO 顺序消耗。
 *
 * <p>出库行必须携带 {@code batchNo}，否则抛 {@link ErpInvErrors#ERR_COST_NOT_AVAILABLE}。
 *
 * <p>权威：{@code docs/design/finance/costing-methods.md §BATCH}。
 */
public class BatchCostingStrategy implements CostingStrategy {

    static final int SCALE = 6;

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IOrmTemplate ormTemplate;

    @Override
    public String costMethod() {
        return ErpInvConstants.COST_METHOD_BATCH;
    }

    @Override
    public BigDecimal onIncoming(ErpInvStockMove move, ErpInvStockMoveLine line, Long acctSchemaId,
                                 BigDecimal unitCost, BookingContext ctx) {
        Long warehouseId = move.getDestWarehouseId();
        Long locationId = line.getDestLocationId() != null ? line.getDestLocationId() : move.getDestWarehouseId();
        ErpInvStockBalance balance = ctx.upsertBalance(move, line, warehouseId, locationId);
        BigDecimal qty = nz(line.getQuantity());
        BigDecimal lineTotalCost = unitCost.multiply(qty);

        appendCostLayer(move, line, acctSchemaId, warehouseId, qty, unitCost, lineTotalCost);

        ErpInvStockBalance updated = ctx.updateBalanceWithRetry(balance, b -> {
            b.setTotalQuantity(nz(b.getTotalQuantity()).add(qty));
            b.setTotalCost(nz(b.getTotalCost()).add(lineTotalCost));
            b.setCostMethod(ErpInvConstants.COST_METHOD_BATCH);
            b.setAvgCost(null);
            ctx.recomputeAvailable(b);
        });

        ctx.writeLedger(move, line, acctSchemaId, updated, warehouseId, locationId, qty, unitCost, lineTotalCost,
                ErpInvConstants.COST_METHOD_BATCH);
        return unitCost;
    }

    @Override
    public BigDecimal onOutgoing(ErpInvStockMove move, ErpInvStockMoveLine line, Long acctSchemaId,
                                 BookingContext ctx) {
        Long warehouseId = move.getSourceWarehouseId();
        Long locationId = line.getSourceLocationId() != null ? line.getSourceLocationId()
                : move.getSourceWarehouseId();
        ErpInvStockBalance balance = ctx.upsertBalance(move, line, warehouseId, locationId);
        BigDecimal qty = nz(line.getQuantity());

        if (line.getBatchNo() == null || line.getBatchNo().isEmpty()) {
            throw new NopException(ErpInvErrors.ERR_COST_NOT_AVAILABLE)
                    .param(ErpInvErrors.ARG_MATERIAL_ID, line.getMaterialId())
                    .param(ErpInvErrors.ARG_WAREHOUSE_ID, warehouseId);
        }

        List<ErpInvCostLayer> layers = findBatchLayers(move.getOrgId(), line.getMaterialId(), line.getSkuId(),
                warehouseId, line.getBatchNo(), acctSchemaId, move.getBusinessDate());
        if (layers.isEmpty()) {
            throw new NopException(ErpInvErrors.ERR_COST_NOT_AVAILABLE)
                    .param(ErpInvErrors.ARG_MATERIAL_ID, line.getMaterialId())
                    .param(ErpInvErrors.ARG_WAREHOUSE_ID, warehouseId);
        }

        BigDecimal remaining = qty;
        BigDecimal totalCost = BigDecimal.ZERO;
        for (ErpInvCostLayer layer : layers) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal avail = nz(layer.getRemainingQuantity());
            if (avail.signum() <= 0) {
                continue;
            }
            BigDecimal take = remaining.min(avail);
            BigDecimal takeCost = take.multiply(nz(layer.getUnitCost()));
            layer.setRemainingQuantity(avail.subtract(take));
            layer.setTotalCost(nz(layer.getTotalCost()).subtract(takeCost));
            daoProvider.daoFor(ErpInvCostLayer.class).saveOrUpdateEntity(layer);
            totalCost = totalCost.add(takeCost);
            remaining = remaining.subtract(take);
        }
        if (remaining.signum() > 0) {
            throw new NopException(ErpInvErrors.ERR_COST_NOT_AVAILABLE)
                    .param(ErpInvErrors.ARG_MATERIAL_ID, line.getMaterialId())
                    .param(ErpInvErrors.ARG_WAREHOUSE_ID, warehouseId);
        }

        BigDecimal weightedUnitCost = qty.signum() != 0
                ? totalCost.divide(qty, SCALE, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        line.setUnitCost(ErpInvConfigs.roundCost(weightedUnitCost));
        daoProvider.daoFor(ErpInvStockMoveLine.class).saveOrUpdateEntity(line);

        final BigDecimal batchTotalCost = totalCost;
        ErpInvStockBalance updated = ctx.updateBalanceWithRetry(balance, b -> {
            b.setTotalQuantity(nz(b.getTotalQuantity()).subtract(qty));
            b.setTotalCost(nz(b.getTotalCost()).subtract(batchTotalCost));
            b.setCostMethod(ErpInvConstants.COST_METHOD_BATCH);
            b.setAvgCost(null);
            ctx.recomputeAvailable(b);
        });

        ctx.writeLedger(move, line, acctSchemaId, updated, warehouseId, locationId, qty.negate(), weightedUnitCost,
                totalCost.negate(), ErpInvConstants.COST_METHOD_BATCH);
        return weightedUnitCost;
    }

    private void appendCostLayer(ErpInvStockMove move, ErpInvStockMoveLine line, Long acctSchemaId,
                                 Long warehouseId, BigDecimal qty, BigDecimal unitCost, BigDecimal totalCost) {
        IEntityDao<ErpInvCostLayer> dao = daoProvider.daoFor(ErpInvCostLayer.class);
        ErpInvCostLayer layer = dao.newEntity();
        layer.setOrgId(move.getOrgId());
        layer.setMaterialId(line.getMaterialId());
        layer.setSkuId(line.getSkuId());
        layer.setWarehouseId(warehouseId);
        layer.setBatchNo(line.getBatchNo());
        layer.setCostMethod(ErpInvConstants.COST_METHOD_BATCH);
        layer.setIncomingQuantity(qty);
        layer.setRemainingQuantity(qty);
        layer.setUnitCost(ErpInvConfigs.roundCost(unitCost));
        layer.setTotalCost(totalCost);
        layer.setCurrencyId(line.getCurrencyId());
        layer.setIncomingDate(move.getBusinessDate() != null ? move.getBusinessDate() : CoreMetrics.today());
        layer.setIncomingMoveId(move.getId());
        layer.setAcctSchemaId(acctSchemaId);
        dao.saveEntity(layer);
    }

    /**
     * 按 batchNo 精确匹配 BATCH cost layer，批次内按 incomingDate 升序（批次内 FIFO）。
     */
    private List<ErpInvCostLayer> findBatchLayers(Long orgId, Long materialId, Long skuId, Long warehouseId,
                                                  String batchNo, Long acctSchemaId,
                                                  java.time.LocalDate businessDate) {
        ormTemplate.flushSession();
        IEntityDao<ErpInvCostLayer> dao = daoProvider.daoFor(ErpInvCostLayer.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("orgId", orgId));
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", warehouseId));
        q.addFilter(eq("costMethod", ErpInvConstants.COST_METHOD_BATCH));
        q.addFilter(eq("batchNo", batchNo));
        q.addFilter(gt("remainingQuantity", BigDecimal.ZERO));
        if (skuId != null) {
            q.addFilter(eq("skuId", skuId));
        }
        if (acctSchemaId != null) {
            q.addFilter(eq("acctSchemaId", acctSchemaId));
        }
        if (businessDate != null) {
            q.addFilter(le("incomingDate", businessDate));
        }
        List<ErpInvCostLayer> list = dao.findAllByQuery(q);
        list.sort(Comparator.comparing(
                l -> l.getIncomingDate() != null ? l.getIncomingDate() : CoreMetrics.today()));
        return list;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
