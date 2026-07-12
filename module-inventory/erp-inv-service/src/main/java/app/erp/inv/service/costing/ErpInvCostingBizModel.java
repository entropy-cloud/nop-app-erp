package app.erp.inv.service.costing;

import app.erp.inv.biz.CostingRecloseReport;
import app.erp.inv.biz.IErpInvCostingBiz;
import app.erp.inv.dao.entity.ErpInvCostLayer;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockLedger;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.inv.service.ErpInvConstants;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import java.util.Objects;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.gt;
import static io.nop.api.core.beans.FilterBeans.le;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 存货成本核算服务 BizModel。承载期末成本兜底重算（{@code period-close.md §步骤2}）。
 *
 * <p>{@code @BizModel("ErpInvCosting")} 为独立服务型 BizObject（非实体聚合），由 finance 期末结账经
 * {@code IBizObjectManager.getBizObject("ErpInvCosting")} 跨模块解析调用（finance→inventory R，DAG 合法）。
 *
 * <p>{@link #reclosePeriodCosts} 扫描本期 DONE 的 FIFO 移动单：对成本层缺失的入库补建 {@link ErpInvCostLayer}
 * （使后续出库有层可消耗）、对 COGS 异常（{@code ledger.unitCost} 空/零）的出库按 FIFO 重算并刷新
 * {@link ErpInvStockLedger} 的 unitCost/totalCost。正常路径（{@link FifoCostingStrategy} DONE 时已维护成本层）
 * 下补算数为 0；非 0 表示历史/异常单据经兜底修复。
 *
 * <p>权威：{@code docs/design/finance/costing-methods.md}、{@code docs/design/finance/period-close.md §步骤2}。
 */
@BizModel("ErpInvCosting")
public class ErpInvCostingBizModel implements IErpInvCostingBiz {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    CostMethodResolver costMethodResolver;

    public ErpInvCostingBizModel() {
    }

    private static final Set<String> LAYER_BASED_METHODS = new HashSet<>(Arrays.asList(
            ErpInvConstants.COST_METHOD_FIFO,
            ErpInvConstants.COST_METHOD_LIFO,
            ErpInvConstants.COST_METHOD_BATCH,
            ErpInvConstants.COST_METHOD_INDIVIDUAL));

    @Override
    @BizMutation
    public CostingRecloseReport reclosePeriodCosts(@Name("periodId") Long periodId,
                                                   @Name("startDate") LocalDate startDate,
                                                   @Name("endDate") LocalDate endDate,
                                                   IServiceContext context) {
        CostingRecloseReport report = new CostingRecloseReport();
        report.setPeriodId(periodId);
        if (startDate == null || endDate == null) {
            return report;
        }

        for (ErpInvStockMove move : findDoneMovesInPeriod(startDate, endDate)) {
            report.setScannedMoves(report.getScannedMoves() + 1);
            List<ErpInvStockMoveLine> lines = loadLines(move.getId());
            for (ErpInvStockMoveLine line : lines) {
                List<ErpInvStockLedger> ledgers = findLedgers(move.getId(), line.getId());
                for (ErpInvStockLedger ledger : ledgers) {
                    String method = costMethodResolver.resolve(line, ledger.getAcctSchemaId());

                    if (LAYER_BASED_METHODS.contains(method)) {
                        if (ledger.getQuantity() != null && ledger.getQuantity().signum() > 0) {
                            if (recomputeIncomingLayerIfMissing(move, line, ledger, method)) {
                                report.setRecomputedIncomingLayers(report.getRecomputedIncomingLayers() + 1);
                            }
                        } else if (ledger.getUnitCost() == null
                                || ledger.getUnitCost().compareTo(BigDecimal.ZERO) == 0) {
                            if (recomputeOutgoingCogs(move, line, ledger, method)) {
                                report.setRecomputedOutgoingLedgers(report.getRecomputedOutgoingLedgers() + 1);
                            }
                        }
                    } else if (Objects.equals(method, ErpInvConstants.COST_METHOD_MONTHLY_WEIGHTED_AVERAGE)) {
                        if (ledger.getQuantity() != null && ledger.getQuantity().signum() < 0) {
                            if (recomputeWeightedAverageOutgoing(move, line, ledger)) {
                                report.setRecomputedOutgoingLedgers(report.getRecomputedOutgoingLedgers() + 1);
                            }
                        }
                    }
                }
            }
        }
        ormTemplate.flushSession();
        return report;
    }

    /**
     * 入库兜底：若该入库移动单未建 cost layer（如成本核算开关曾关闭期间入库），按 {@code ledger.unitCost × qty}
     * 补建并校正流水 costMethod。返回是否补建。
     */
    private boolean recomputeIncomingLayerIfMissing(ErpInvStockMove move, ErpInvStockMoveLine line,
                                                    ErpInvStockLedger ledger, String costMethod) {
        if (findExistingLayer(move.getId(), line.getMaterialId(), ledger.getWarehouseId(), costMethod) != null) {
            return false;
        }
        BigDecimal qty = nz(ledger.getQuantity());
        BigDecimal unitCost = nz(ledger.getUnitCost());
        if (unitCost.signum() == 0) {
            unitCost = nz(line.getUnitCost());
        }
        if (qty.signum() <= 0 || unitCost.signum() <= 0) {
            return false;
        }
        appendLayer(move, line, ledger, qty, unitCost, unitCost.multiply(qty), costMethod);
        if (ledger.getCostMethod() == null
                || !Objects.equals(ledger.getCostMethod(), costMethod)) {
            ledger.setCostMethod(costMethod);
            daoProvider.daoFor(ErpInvStockLedger.class).saveOrUpdateEntity(ledger);
        }
        return true;
    }

    /**
     * 出库兜底：流水 unitCost 空/零时，按指定成本方法的层查询顺序消耗可用 cost layer 重算 COGS 并刷新流水。
     * 余额在原 DONE 记账时已扣减，此处仅校正流水成本（不重复触碰余额，避免双计）。返回是否重算。
     */
    private boolean recomputeOutgoingCogs(ErpInvStockMove move, ErpInvStockMoveLine line,
                                          ErpInvStockLedger ledger, String costMethod) {
        BigDecimal qty = nz(ledger.getQuantity()).abs();
        if (qty.signum() <= 0) {
            return false;
        }
        List<ErpInvCostLayer> layers = findLayersForMethod(line.getMaterialId(), ledger.getWarehouseId(),
                line.getBatchNo(), move.getBusinessDate(), costMethod);
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
            totalCost = totalCost.add(take.multiply(nz(layer.getUnitCost())));
            remaining = remaining.subtract(take);
        }
        if (remaining.signum() > 0) {
            return false;
        }
        ledger.setUnitCost(qty.signum() != 0 ? totalCost.divide(qty, FifoCostingStrategy.SCALE,
                RoundingMode.HALF_UP) : BigDecimal.ZERO);
        ledger.setTotalCost(totalCost);
        ledger.setCostMethod(costMethod);
        daoProvider.daoFor(ErpInvStockLedger.class).saveOrUpdateEntity(ledger);
        return true;
    }

    /**
     * 全月一次加权平均期末调整：重算全月实际加权平均并调整出库流水。
     *
     * <p>全月实际加权平均 = 余额 totalCost / 余额 totalQuantity（此时入库已全量累加、出库按期初暂估已扣减，
     * totalCost/totalQuantity 反映期初+本期入库净额）。将出库流水的 unitCost/totalCost 调整为实际值。
     */
    private boolean recomputeWeightedAverageOutgoing(ErpInvStockMove move, ErpInvStockMoveLine line,
                                                      ErpInvStockLedger ledger) {
        BigDecimal qty = nz(ledger.getQuantity()).abs();
        if (qty.signum() <= 0) {
            return false;
        }
        List<ErpInvStockBalance> balances = findBalanceForLedger(line.getMaterialId(), ledger.getWarehouseId(),
                line.getBatchNo());
        if (balances.isEmpty()) {
            return false;
        }
        ErpInvStockBalance balance = balances.get(0);
        BigDecimal totalQty = nz(balance.getTotalQuantity());
        if (totalQty.signum() <= 0) {
            return false;
        }
        BigDecimal monthlyWa = WeightedAverageCostingStrategy.computeWeightedAverage(
                nz(balance.getTotalCost()).add(nz(ledger.getTotalCost()).abs()), totalQty.add(qty));

        BigDecimal newTotalCost = monthlyWa.multiply(qty);
        BigDecimal currentTotalCost = nz(ledger.getTotalCost());
        if (currentTotalCost.compareTo(newTotalCost) == 0
                && nz(ledger.getUnitCost()).compareTo(monthlyWa) == 0) {
            return false;
        }
        ledger.setUnitCost(monthlyWa);
        ledger.setTotalCost(newTotalCost.negate());
        daoProvider.daoFor(ErpInvStockLedger.class).saveOrUpdateEntity(ledger);

        BigDecimal costDiff = newTotalCost.subtract(currentTotalCost.abs());
        balance.setTotalCost(nz(balance.getTotalCost()).subtract(costDiff));
        balance.setAvgCost(monthlyWa);
        daoProvider.daoFor(ErpInvStockBalance.class).saveOrUpdateEntity(balance);
        return true;
    }

    // ---------- queries ----------

    private List<ErpInvStockMove> findDoneMovesInPeriod(LocalDate startDate, LocalDate endDate) {
        ormTemplate.flushSession();
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(ge("businessDate", startDate));
        q.addFilter(le("businessDate", endDate));
        q.addFilter(eq("docStatus", ErpInvConstants.DOC_STATUS_DONE));
        return dao.findAllByQuery(q);
    }

    private List<ErpInvStockMoveLine> loadLines(Long moveId) {
        IEntityDao<ErpInvStockMoveLine> dao = daoProvider.daoFor(ErpInvStockMoveLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("moveId", moveId));
        return dao.findAllByQuery(q);
    }

    private List<ErpInvStockLedger> findLedgers(Long moveId, Long moveLineId) {
        IEntityDao<ErpInvStockLedger> dao = daoProvider.daoFor(ErpInvStockLedger.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("moveId", moveId));
        q.addFilter(eq("moveLineId", moveLineId));
        return dao.findAllByQuery(q);
    }

    private ErpInvCostLayer findExistingLayer(Long incomingMoveId, Long materialId, Long warehouseId,
                                               String costMethod) {
        IEntityDao<ErpInvCostLayer> dao = daoProvider.daoFor(ErpInvCostLayer.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("incomingMoveId", incomingMoveId));
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", warehouseId));
        q.addFilter(eq("costMethod", costMethod));
        List<ErpInvCostLayer> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 按成本方法查询可用 cost layer。FIFO/批次内升序；LIFO 降序；SPECIFIC 按 batchNo 精确匹配。
     */
    private List<ErpInvCostLayer> findLayersForMethod(Long materialId, Long warehouseId, String batchNo,
                                                      LocalDate businessDate, String costMethod) {
        IEntityDao<ErpInvCostLayer> dao = daoProvider.daoFor(ErpInvCostLayer.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", warehouseId));
        q.addFilter(eq("costMethod", costMethod));
        q.addFilter(gt("remainingQuantity", BigDecimal.ZERO));
        if (batchNo != null) {
            q.addFilter(eq("batchNo", batchNo));
        }
        if (businessDate != null) {
            q.addFilter(le("incomingDate", businessDate));
        }
        List<ErpInvCostLayer> list = dao.findAllByQuery(q);
        Comparator<ErpInvCostLayer> byDate = Comparator.comparing(
                l -> l.getIncomingDate() != null ? l.getIncomingDate() : CoreMetrics.today());
        if (Objects.equals(costMethod, ErpInvConstants.COST_METHOD_LIFO)) {
            list.sort(byDate.reversed());
        } else {
            list.sort(byDate);
        }
        return list;
    }

    private List<ErpInvStockBalance> findBalanceForLedger(Long materialId, Long warehouseId, String batchNo) {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", warehouseId));
        if (batchNo != null) {
            q.addFilter(eq("batchNo", batchNo));
        }
        return dao.findAllByQuery(q);
    }

    // ---------- layer append ----------

    private void appendLayer(ErpInvStockMove move, ErpInvStockMoveLine line, ErpInvStockLedger ledger,
                             BigDecimal qty, BigDecimal unitCost, BigDecimal totalCost, String costMethod) {
        IEntityDao<ErpInvCostLayer> dao = daoProvider.daoFor(ErpInvCostLayer.class);
        ErpInvCostLayer layer = dao.newEntity();
        layer.setOrgId(move.getOrgId());
        layer.setMaterialId(line.getMaterialId());
        layer.setSkuId(line.getSkuId());
        layer.setWarehouseId(ledger.getWarehouseId());
        layer.setBatchNo(line.getBatchNo());
        layer.setCostMethod(costMethod);
        layer.setIncomingQuantity(qty);
        layer.setRemainingQuantity(qty);
        layer.setUnitCost(unitCost);
        layer.setTotalCost(totalCost);
        layer.setCurrencyId(line.getCurrencyId());
        layer.setIncomingDate(move.getBusinessDate() != null ? move.getBusinessDate() : CoreMetrics.today());
        layer.setIncomingMoveId(move.getId());
        layer.setAcctSchemaId(ledger.getAcctSchemaId());
        dao.saveEntity(layer);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
