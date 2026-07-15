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
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.gt;

/**
 * 个别计价（具体辨认）成本策略（{@code ErpInvConstants.COST_METHOD_INDIVIDUAL}=60）。
 *
 * <p>每批入库创建独立 {@link ErpInvCostLayer}（costMethod=SPECIFIC），出库时按移动单行的 {@code batchNo}
 * 或 {@code serialNo} 精确匹配对应成本层消耗——不同批次/序列号的物料各自保留独立单位成本。
 *
 * <p>若出库行未指定 batchNo/serialNo，抛 {@link ErpInvErrors#ERR_COST_NOT_AVAILABLE}（个别计价要求明确指定来源）。
 *
 * <p>权威：{@code docs/design/finance/costing-methods.md §SPECIFIC}。
 */
public class SpecificCostingStrategy implements CostingStrategy {

    static final int SCALE = 6;

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IOrmTemplate ormTemplate;

    @Override
    public String costMethod() {
        return ErpInvConstants.COST_METHOD_INDIVIDUAL;
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
            b.setCostMethod(ErpInvConstants.COST_METHOD_INDIVIDUAL);
            b.setAvgCost(null);
            ctx.recomputeAvailable(b);
        });

        ctx.writeLedger(move, line, acctSchemaId, updated, warehouseId, locationId, qty, unitCost, lineTotalCost,
                ErpInvConstants.COST_METHOD_INDIVIDUAL);
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

        if ((line.getBatchNo() == null || line.getBatchNo().isEmpty())
                && (line.getSerialNo() == null || line.getSerialNo().isEmpty())) {
            throw new NopException(ErpInvErrors.ERR_COST_NOT_AVAILABLE)
                    .param(ErpInvErrors.ARG_MATERIAL_ID, line.getMaterialId())
                    .param(ErpInvErrors.ARG_WAREHOUSE_ID, warehouseId);
        }

        List<ErpInvCostLayer> layers = findSpecificLayers(move.getOrgId(), line.getMaterialId(), line.getSkuId(),
                warehouseId, line.getBatchNo(), line.getSerialNo(), acctSchemaId);
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

        final BigDecimal specificTotalCost = totalCost;
        ErpInvStockBalance updated = ctx.updateBalanceWithRetry(balance, b -> {
            b.setTotalQuantity(nz(b.getTotalQuantity()).subtract(qty));
            b.setTotalCost(nz(b.getTotalCost()).subtract(specificTotalCost));
            b.setCostMethod(ErpInvConstants.COST_METHOD_INDIVIDUAL);
            b.setAvgCost(null);
            ctx.recomputeAvailable(b);
        });

        ctx.writeLedger(move, line, acctSchemaId, updated, warehouseId, locationId, qty.negate(), weightedUnitCost,
                totalCost.negate(), ErpInvConstants.COST_METHOD_INDIVIDUAL);
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
        layer.setCostMethod(ErpInvConstants.COST_METHOD_INDIVIDUAL);
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
     * 按 batchNo 或 serialNo 精确匹配 SPECIFIC cost layer。
     * batchNo 优先；若 batchNo 为空则按 serialNo 匹配（serialNo 场景每层代表一件）。
     */
    private List<ErpInvCostLayer> findSpecificLayers(Long orgId, Long materialId, Long skuId, Long warehouseId,
                                                     String batchNo, String serialNo, Long acctSchemaId) {
        ormTemplate.flushSession();
        IEntityDao<ErpInvCostLayer> dao = daoProvider.daoFor(ErpInvCostLayer.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("orgId", orgId));
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", warehouseId));
        q.addFilter(eq("costMethod", ErpInvConstants.COST_METHOD_INDIVIDUAL));
        q.addFilter(gt("remainingQuantity", BigDecimal.ZERO));
        if (skuId != null) {
            q.addFilter(eq("skuId", skuId));
        }
        if (batchNo != null && !batchNo.isEmpty()) {
            q.addFilter(eq("batchNo", batchNo));
        }
        if (acctSchemaId != null) {
            q.addFilter(eq("acctSchemaId", acctSchemaId));
        }
        return dao.findAllByQuery(q);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
