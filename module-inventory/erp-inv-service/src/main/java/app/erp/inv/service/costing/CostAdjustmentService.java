package app.erp.inv.service.costing;

import app.erp.inv.dao.entity.ErpInvCostAdjust;
import app.erp.inv.dao.entity.ErpInvCostAdjustLine;
import app.erp.inv.dao.entity.ErpInvCostLayer;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockLedger;
import app.erp.inv.service.ErpInvConstants;
import app.erp.inv.service.ErpInvErrors;
import app.erp.mfg.dao.entity.ErpMfgCostRollup;
import app.erp.mfg.dao.entity.ErpMfgCostRollupLine;
import app.erp.md.dao.entity.ErpMdMaterial;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.StringHelper;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 成本调整引擎（plan 2026-07-05-2352-3；costing-methods.md §成本调整）。
 *
 * <p>纯成本变更（数量不变）：按行更新 {@link ErpInvStockBalance} 的 avgCost/totalCost、写不可变
 * {@link ErpInvStockLedger} 流水（quantity=0）、按计价方法处理成本层（FIFO 追加调整层）、
 * 标准成本重估发布 FIRMED {@link ErpMfgCostRollup}。不生成 StockMove（Non-Goal）。
 *
 * <p>三轴分派（{@code ErpMdMaterial.costMethod} → balance.costMethod）：
 * <ul>
 *   <li>MOVING_AVERAGE：avgCost=newUnitCost，totalCost += adjustAmount</li>
 *   <li>FIFO：totalCost += adjustAmount，追加 delta 调整层（unitCost=Δ，incomingMoveId=-行ID 哨兵）</li>
 *   <li>STANDARD：avgCost=newUnitCost，totalCost += adjustAmount；STANDARD_REVALUATION 额外发布 FIRMED rollup</li>
 * </ul>
 *
 * <p>流水 moveId/moveLineId 置 {@link ErpInvConstants#LEDGER_MOVE_ID_COST_ADJUST}(0) 哨兵——
 * 成本调整无 StockMove，0 标识非移动单来源；成本调整过账经独立 {@code CostAdjustmentPostingDispatcher}
 * （不经 {@code InvPostingDispatcher} 的 moveId 通道），故 0 哨兵不冲突。
 */
public class CostAdjustmentService {

    static final int SCALE = 6;

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IOrmTemplate ormTemplate;

    @Inject
    StandardCostResolver standardCostResolver;

    /**
     * 执行成本调整：按行更新余额/层/流水。返回 Σ 行 adjustAmount（带符号，供过账方向判定）。
     */
    public BigDecimal applyCostAdjust(ErpInvCostAdjust adjust, List<ErpInvCostAdjustLine> lines) {
        BigDecimal totalAdjustAmount = BigDecimal.ZERO;
        for (ErpInvCostAdjustLine line : lines) {
            totalAdjustAmount = totalAdjustAmount.add(applyLine(adjust, line));
        }
        return totalAdjustAmount;
    }

    /**
     * 冲销成本调整：按行回退余额/层（流水保留为历史记录，凭证红字由派发器处理）。
     */
    public void reverseCostAdjust(ErpInvCostAdjust adjust, List<ErpInvCostAdjustLine> lines) {
        for (ErpInvCostAdjustLine line : lines) {
            reverseLine(adjust, line);
        }
    }

    // ---------- apply ----------

    private BigDecimal applyLine(ErpInvCostAdjust adjust, ErpInvCostAdjustLine line) {
        ormTemplate.flushSession();
        ErpInvStockBalance balance = findBalance(adjust.getOrgId(), line.getMaterialId(),
                line.getWarehouseId(), line.getBatchNo());
        if (balance == null || nz(balance.getTotalQuantity()).signum() <= 0) {
            throw new NopException(ErpInvErrors.ERR_COST_ADJUST_NO_BALANCE)
                    .param(ErpInvErrors.ARG_ADJUST_CODE, adjust.getCode())
                    .param(ErpInvErrors.ARG_MATERIAL_ID, line.getMaterialId())
                    .param(ErpInvErrors.ARG_WAREHOUSE_ID, line.getWarehouseId());
        }
        BigDecimal newUnitCost = nz(line.getNewUnitCost());
        if (newUnitCost.signum() < 0) {
            throw new NopException(ErpInvErrors.ERR_COST_ADJUST_NEGATIVE_COST)
                    .param(ErpInvErrors.ARG_ADJUST_CODE, adjust.getCode())
                    .param(ErpInvErrors.ARG_UNIT_COST, newUnitCost);
        }

        String costMethod = resolveCostMethod(balance, line.getMaterialId());
        BigDecimal onHand = nz(balance.getTotalQuantity());
        BigDecimal oldUnitCost = resolveOldUnitCost(adjust, line, balance, costMethod);
        BigDecimal adjustAmount = (newUnitCost.subtract(oldUnitCost)).multiply(onHand);

        if (Objects.equals(costMethod, ErpInvConstants.COST_METHOD_FIFO)) {
            applyFifo(adjust, line, balance, onHand, newUnitCost, oldUnitCost, adjustAmount);
        } else {
            applyAverageLike(adjust, line, balance, costMethod, onHand, newUnitCost, oldUnitCost, adjustAmount);
        }

        line.setOldUnitCost(oldUnitCost);
        line.setAdjustQty(onHand);
        line.setAdjustAmount(adjustAmount);
        daoProvider.daoFor(ErpInvCostAdjustLine.class).saveOrUpdateEntity(line);

        writeLedger(adjust, line, balance, costMethod, newUnitCost, adjustAmount);
        return adjustAmount;
    }

    private void applyAverageLike(ErpInvCostAdjust adjust, ErpInvCostAdjustLine line, ErpInvStockBalance balance,
                                   String costMethod, BigDecimal onHand, BigDecimal newUnitCost,
                                   BigDecimal oldUnitCost, BigDecimal adjustAmount) {
        balance.setAvgCost(newUnitCost);
        BigDecimal newTotalCost = nz(balance.getTotalCost()).add(adjustAmount);
        balance.setTotalCost(newTotalCost);
        balance.setCostMethod(costMethod);
        daoProvider.daoFor(ErpInvStockBalance.class).saveOrUpdateEntity(balance);

        if (Objects.equals(adjust.getAdjustType(), ErpInvConstants.ADJUST_TYPE_STANDARD_REVALUATION)) {
            publishFirmedRollup(adjust, line, newUnitCost);
        }
    }

    private void applyFifo(ErpInvCostAdjust adjust, ErpInvCostAdjustLine line, ErpInvStockBalance balance,
                            BigDecimal onHand, BigDecimal newUnitCost, BigDecimal oldUnitCost,
                            BigDecimal adjustAmount) {
        BigDecimal newTotalCost = nz(balance.getTotalCost()).add(adjustAmount);
        balance.setTotalCost(newTotalCost);
        balance.setCostMethod(ErpInvConstants.COST_METHOD_FIFO);
        balance.setAvgCost(null);
        daoProvider.daoFor(ErpInvStockBalance.class).saveOrUpdateEntity(balance);

        appendFifoAdjustLayer(adjust, line, balance, onHand, newUnitCost.subtract(oldUnitCost), adjustAmount);
    }

    private void appendFifoAdjustLayer(ErpInvCostAdjust adjust, ErpInvCostAdjustLine line,
                                        ErpInvStockBalance balance, BigDecimal qty, BigDecimal deltaUnitCost,
                                        BigDecimal adjustAmount) {
        IEntityDao<ErpInvCostLayer> dao = daoProvider.daoFor(ErpInvCostLayer.class);
        ErpInvCostLayer layer = dao.newEntity();
        layer.setOrgId(adjust.getOrgId());
        layer.setMaterialId(line.getMaterialId());
        layer.setSkuId(balance.getSkuId());
        layer.setWarehouseId(line.getWarehouseId());
        layer.setBatchNo(line.getBatchNo());
        layer.setCostMethod(ErpInvConstants.COST_METHOD_FIFO);
        layer.setIncomingQuantity(qty);
        layer.setRemainingQuantity(qty);
        layer.setUnitCost(deltaUnitCost);
        layer.setTotalCost(adjustAmount);
        layer.setCurrencyId(line.getCurrencyId() != null ? line.getCurrencyId() : balance.getCurrencyId());
        LocalDate incomingDate = adjust.getBusinessDate() != null ? adjust.getBusinessDate() : CoreMetrics.today();
        layer.setIncomingDate(incomingDate);
        // 负行 ID 哨兵：区别于正常移动单正 ID，reverse 据此精确删除本调整层
        layer.setIncomingMoveId(-line.getId());
        layer.setAcctSchemaId(null);
        dao.saveEntity(layer);
    }

    // ---------- reverse ----------

    private void reverseLine(ErpInvCostAdjust adjust, ErpInvCostAdjustLine line) {
        ormTemplate.flushSession();
        ErpInvStockBalance balance = findBalance(adjust.getOrgId(), line.getMaterialId(),
                line.getWarehouseId(), line.getBatchNo());
        BigDecimal adjustAmount = nz(line.getAdjustAmount());
        BigDecimal oldUnitCost = nz(line.getOldUnitCost());

        if (balance != null) {
            balance.setTotalCost(nz(balance.getTotalCost()).subtract(adjustAmount));
            balance.setAvgCost(oldUnitCost);
            daoProvider.daoFor(ErpInvStockBalance.class).saveOrUpdateEntity(balance);
        }

        removeFifoAdjustLayer(line);
        if (Objects.equals(adjust.getAdjustType(), ErpInvConstants.ADJUST_TYPE_STANDARD_REVALUATION)) {
            removeFirmedRollup(adjust, line);
        }
    }

    private void removeFifoAdjustLayer(ErpInvCostAdjustLine line) {
        IEntityDao<ErpInvCostLayer> dao = daoProvider.daoFor(ErpInvCostLayer.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("incomingMoveId", -line.getId()));
        List<ErpInvCostLayer> layers = dao.findAllByQuery(q);
        for (ErpInvCostLayer layer : layers) {
            dao.deleteEntity(layer);
        }
    }

    // ---------- standard revaluation rollup ----------

    private void publishFirmedRollup(ErpInvCostAdjust adjust, ErpInvCostAdjustLine line, BigDecimal newUnitCost) {
        IEntityDao<ErpMdMaterial> mdDao = daoProvider.daoFor(ErpMdMaterial.class);
        ErpMdMaterial material = mdDao.getEntityById(line.getMaterialId());
        Long uomId = material != null ? material.getUoMId() : null;

        IEntityDao<ErpMfgCostRollup> headerDao = daoProvider.daoFor(ErpMfgCostRollup.class);
        ErpMfgCostRollup header = headerDao.newEntity();
        header.setCode(buildRollupCode(adjust, line));
        header.setOrgId(adjust.getOrgId());
        header.setBusinessDate(adjust.getBusinessDate() != null ? adjust.getBusinessDate() : CoreMetrics.today());
        header.orm_propValueByName("status", StandardCostResolver.STATUS_FIRMED);
        header.setRemark("由成本调整单 " + adjust.getCode() + " 自动发布");
        headerDao.saveEntity(header);

        IEntityDao<ErpMfgCostRollupLine> lineDao = daoProvider.daoFor(ErpMfgCostRollupLine.class);
        ErpMfgCostRollupLine rollupLine = lineDao.newEntity();
        rollupLine.setCostRollupId(header.getId());
        rollupLine.setLineNo(1);
        rollupLine.setMaterialId(line.getMaterialId());
        rollupLine.setUoMId(uomId);
        rollupLine.setUnitCost(newUnitCost);
        rollupLine.setTotalCost(newUnitCost);
        rollupLine.setMaterialCost(newUnitCost);
        rollupLine.setCurrencyId(line.getCurrencyId() != null ? line.getCurrencyId() : adjust.getCurrencyId());
        lineDao.saveEntity(rollupLine);
    }

    private void removeFirmedRollup(ErpInvCostAdjust adjust, ErpInvCostAdjustLine line) {
        IEntityDao<ErpMfgCostRollup> dao = daoProvider.daoFor(ErpMfgCostRollup.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", buildRollupCode(adjust, line)));
        List<ErpMfgCostRollup> list = dao.findAllByQuery(q);
        for (ErpMfgCostRollup header : list) {
            dao.deleteEntity(header);
        }
    }

    private String buildRollupCode(ErpInvCostAdjust adjust, ErpInvCostAdjustLine line) {
        return "ROLLUP-CA-" + adjust.getCode() + "-" + line.getId();
    }

    // ---------- ledger ----------

    private void writeLedger(ErpInvCostAdjust adjust, ErpInvCostAdjustLine line, ErpInvStockBalance balance,
                              String costMethod, BigDecimal newUnitCost, BigDecimal adjustAmount) {
        IEntityDao<ErpInvStockLedger> dao = daoProvider.daoFor(ErpInvStockLedger.class);
        ErpInvStockLedger ledger = dao.newEntity();
        ledger.setCode("CA-" + StringHelper.generateUUID());
        ledger.setOrgId(adjust.getOrgId());
        ledger.setMoveId(ErpInvConstants.LEDGER_MOVE_ID_COST_ADJUST);
        ledger.setMoveLineId(ErpInvConstants.LEDGER_MOVE_ID_COST_ADJUST);
        ledger.setMaterialId(line.getMaterialId());
        ledger.setSkuId(balance.getSkuId());
        ledger.setWarehouseId(line.getWarehouseId());
        ledger.setLocationId(balance.getLocationId());
        ledger.setQuantity(BigDecimal.ZERO);
        ledger.setUnitCost(newUnitCost);
        ledger.setTotalCost(adjustAmount);
        ledger.setBalanceQuantity(balance.getTotalQuantity());
        ledger.setBalanceTotalCost(balance.getTotalCost());
        ledger.setCostMethod(costMethod);
        ledger.setCurrencyId(line.getCurrencyId() != null ? line.getCurrencyId() : balance.getCurrencyId());
        ledger.setBusinessDate(adjust.getBusinessDate());
        ledger.setBatchNo(line.getBatchNo());
        dao.saveEntity(ledger);
    }

    // ---------- helpers ----------

    private ErpInvStockBalance findBalance(Long orgId, Long materialId, Long warehouseId, String batchNo) {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("orgId", orgId));
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", warehouseId));
        if (batchNo != null) {
            q.addFilter(eq("batchNo", batchNo));
        }
        List<ErpInvStockBalance> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private String resolveCostMethod(ErpInvStockBalance balance, Long materialId) {
        if (balance.getCostMethod() != null) {
            return balance.getCostMethod();
        }
        ErpMdMaterial material = daoProvider.daoFor(ErpMdMaterial.class).getEntityById(materialId);
        if (material != null && material.getCostMethod() != null) {
            return material.getCostMethod();
        }
        return ErpInvConstants.DEFAULT_COST_METHOD;
    }

    private BigDecimal resolveOldUnitCost(ErpInvCostAdjust adjust, ErpInvCostAdjustLine line,
                                           ErpInvStockBalance balance, String costMethod) {
        if (Objects.equals(adjust.getAdjustType(), ErpInvConstants.ADJUST_TYPE_STANDARD_REVALUATION)) {
            try {
                return standardCostResolver.resolve(line.getMaterialId());
            } catch (Exception e) {
                return nz(balance.getAvgCost());
            }
        }
        if (Objects.equals(costMethod, ErpInvConstants.COST_METHOD_FIFO)) {
            BigDecimal qty = nz(balance.getTotalQuantity());
            return qty.signum() != 0
                    ? nz(balance.getTotalCost()).divide(qty, SCALE, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
        }
        return nz(balance.getAvgCost());
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
