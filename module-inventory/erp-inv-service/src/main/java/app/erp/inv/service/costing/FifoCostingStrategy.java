package app.erp.inv.service.costing;

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
 * FIFO（先进先出）成本策略（{@code ErpInvConstants.COST_METHOD_FIFO}=30）。
 *
 * <p>入库追加 {@link ErpInvCostLayer}（incomingQuantity=remainingQuantity=入库量、unitCost=入库单价、
 * incomingDate、incomingMoveId、costMethod=FIFO）；余额 totalQuantity/totalCost 累加，avgCost 置空（FIFO 语义）。
 *
 * <p>出库按 {@code incomingDate} 升序消耗 remainingQuantity>0 的 cost layer，多层跨消耗汇总加权出库 unitCost
 * （{@code docs/design/finance/costing-methods.md §FIFO 出库逻辑}）；写 {@link app.erp.inv.dao.entity.ErpInvStockLedger}
 * 的 unitCost/totalCost（与移动加权平均同通道，{@code InvPostingDispatcher} 零改动拾取）。
 *
 * <p>首次出库无可用 cost layer 抛 {@link ErpInvErrors#ERR_COST_NOT_AVAILABLE}（对齐移动加权平均余额 0 时同等语义）。
 *
 * <p><b>红冲语义（Decision）</b>：选择 (a) 反向入库按原出库消耗的加权 unitCost 追加新层。原出库消耗后 line.unitCost
 * 已被刷新为加权成本，reverse 流程将该值透传给反向入库行，{@code onIncoming} 据此创建新层——成本回加对齐，
 * 总量/总成本恢复至原出库前（Proof 验证不变量：红冲后 Σ cost layer remaining×unitCost = 红冲前）。
 */
public class FifoCostingStrategy implements CostingStrategy {

    static final int SCALE = 6;

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IOrmTemplate ormTemplate;

    @Override
    public String costMethod() {
        return ErpInvConstants.COST_METHOD_FIFO;
    }

    @Override
    public BigDecimal onIncoming(ErpInvStockMove move, ErpInvStockMoveLine line, Long acctSchemaId,
                                 BigDecimal unitCost, BookingContext ctx) {
        Long warehouseId = move.getDestWarehouseId();
        Long locationId = line.getDestLocationId() != null ? line.getDestLocationId() : move.getDestLocationId();
        ErpInvStockBalance balance = ctx.upsertBalance(move, line, warehouseId, locationId);
        BigDecimal qty = nz(line.getQuantity());
        BigDecimal lineTotalCost = unitCost.multiply(qty);

        appendCostLayer(move, line, acctSchemaId, warehouseId, qty, unitCost, lineTotalCost);

        ErpInvStockBalance updated = ctx.updateBalanceWithRetry(balance, b -> {
            BigDecimal oldTotal = nz(b.getTotalQuantity());
            BigDecimal oldTotalCost = nz(b.getTotalCost());
            b.setTotalQuantity(oldTotal.add(qty));
            b.setTotalCost(oldTotalCost.add(lineTotalCost));
            b.setCostMethod(ErpInvConstants.COST_METHOD_FIFO);
            b.setAvgCost(null);
            ctx.recomputeAvailable(b);
        });

        ctx.writeLedger(move, line, acctSchemaId, updated, warehouseId, locationId, qty, unitCost, lineTotalCost,
                ErpInvConstants.COST_METHOD_FIFO);
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

        List<ErpInvCostLayer> layers = findFifoLayers(move.getOrgId(), line.getMaterialId(), line.getSkuId(),
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
            // 总剩余不足覆盖出库量：与首次无成本前置一致拒绝（避免负成本）
            throw new NopException(ErpInvErrors.ERR_COST_NOT_AVAILABLE)
                    .param(ErpInvErrors.ARG_MATERIAL_ID, line.getMaterialId())
                    .param(ErpInvErrors.ARG_WAREHOUSE_ID, warehouseId);
        }

        BigDecimal weightedUnitCost = qty.signum() != 0
                ? totalCost.divide(qty, SCALE, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        // 将加权 unitCost 刷回行，供 reverse 流程透传给反向入库（红冲语义 Decision (a)：成本回加对齐）
        line.setUnitCost(weightedUnitCost);
        daoProvider.daoFor(ErpInvStockMoveLine.class).saveOrUpdateEntity(line);

        final BigDecimal fifoTotalCost = totalCost;
        ErpInvStockBalance updated = ctx.updateBalanceWithRetry(balance, b -> {
            BigDecimal oldTotal = nz(b.getTotalQuantity());
            BigDecimal oldTotalCost = nz(b.getTotalCost());
            b.setTotalQuantity(oldTotal.subtract(qty));
            b.setTotalCost(oldTotalCost.subtract(fifoTotalCost));
            b.setCostMethod(ErpInvConstants.COST_METHOD_FIFO);
            b.setAvgCost(null);
            ctx.recomputeAvailable(b);
        });

        ctx.writeLedger(move, line, acctSchemaId, updated, warehouseId, locationId, qty.negate(), weightedUnitCost,
                totalCost.negate(), ErpInvConstants.COST_METHOD_FIFO);
        return weightedUnitCost;
    }

    // ---------- cost layer maintenance ----------

    private void appendCostLayer(ErpInvStockMove move, ErpInvStockMoveLine line, Long acctSchemaId,
                                 Long warehouseId, BigDecimal qty, BigDecimal unitCost, BigDecimal totalCost) {
        IEntityDao<ErpInvCostLayer> dao = daoProvider.daoFor(ErpInvCostLayer.class);
        ErpInvCostLayer layer = dao.newEntity();
        layer.setOrgId(move.getOrgId());
        layer.setMaterialId(line.getMaterialId());
        layer.setSkuId(line.getSkuId());
        layer.setWarehouseId(warehouseId);
        layer.setBatchNo(line.getBatchNo());
        layer.setCostMethod(ErpInvConstants.COST_METHOD_FIFO);
        layer.setIncomingQuantity(qty);
        layer.setRemainingQuantity(qty);
        layer.setUnitCost(unitCost);
        layer.setTotalCost(totalCost);
        layer.setCurrencyId(line.getCurrencyId());
        layer.setIncomingDate(move.getBusinessDate() != null ? move.getBusinessDate() : CoreMetrics.today());
        layer.setIncomingMoveId(move.getId());
        layer.setAcctSchemaId(acctSchemaId);
        dao.saveEntity(layer);
    }

    /**
     * 按 incomingDate 升序返回当前可用 FIFO cost layer（remainingQuantity>0）。
     *
     * <p>出库日约束：仅消耗出库 businessDate 当日及之前入库的层（历史成本原则——后续入库不可回溯覆盖先出库）。
     */
    private List<ErpInvCostLayer> findFifoLayers(Long orgId, Long materialId, Long skuId, Long warehouseId,
                                                 String batchNo, Long acctSchemaId,
                                                 java.time.LocalDate businessDate) {
        ormTemplate.flushSession();
        IEntityDao<ErpInvCostLayer> dao = daoProvider.daoFor(ErpInvCostLayer.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("orgId", orgId));
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", warehouseId));
        q.addFilter(eq("costMethod", ErpInvConstants.COST_METHOD_FIFO));
        q.addFilter(gt("remainingQuantity", BigDecimal.ZERO));
        if (skuId != null) {
            q.addFilter(eq("skuId", skuId));
        }
        if (batchNo != null) {
            q.addFilter(eq("batchNo", batchNo));
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
