package app.erp.mfg.service.costing;

import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdMaterialSku;
import app.erp.mfg.biz.CostRollupLineView;
import app.erp.mfg.biz.CostRollupResult;
import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgBomLine;
import app.erp.mfg.dao.entity.ErpMfgBomOperation;
import app.erp.mfg.dao.entity.ErpMfgCostRollup;
import app.erp.mfg.dao.entity.ErpMfgCostRollupLine;
import app.erp.mfg.dao.entity.ErpMfgSubcontractOrder;
import app.erp.mfg.dao.entity.ErpMfgSubcontractOrderLine;
import app.erp.mfg.dao.entity.ErpMfgWorkcenter;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.mfg.service.bom.BomExpander;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 成本卷算（Cost Rollup）服务。服务于 {@code IErpMfgBomBiz.rollupCost}。
 *
 * <p>算法（{@code bom-and-routing.md §多级成本卷算}）：自下而上递归 + 按物料记忆化（避免共享组件重复计算）。
 * <ul>
 *   <li>采购件（无默认 BOM）：单位成本 = 默认 SKU {@code purchasePrice}；空则抛
 *       {@code ERR_ROLLUP_BASE_COST_MISSING}。</li>
 *   <li>制造件（有默认 BOM）：
 *     <ul>
 *       <li>材料 = Σ(line.quantity / BOM.qty × 子件单位成本)；</li>
 *       <li>直接人工+制造费用 = Σ(工序 standardTime/60 × workcenter.hourlyRate)；</li>
 *       <li>单位成本 = 材料 + 人工（+ 制造费用，见下方 Decision）。</li>
 *     </ul></li>
 * </ul>
 *
 * <p><b>Decision（人工/制造费用分列）</b>：工作中心仅有单一 {@code hourlyRate}（无独立人工/制造费率分列），
 * 故工序工时成本统一计入 {@code laborCost}；{@code overheadCost} 经 config-gated 分配率（plan 2026-07-13-0455-2）
 * 应用——关时恒 0（向后兼容），开时按 {@code erp-mfg.overhead-allocation-mode}（MACHINE_HOUR=机器工时×rate /
 * LABOR_RATIO=laborCost×rate）× {@code erp-mfg.overhead-allocation-rate} 计算。
 * 工作中心费率拆分（laborRate/overheadRate 分列）为 successor（ask-first ORM 保护区域）。
 *
 * <p>结果写入既有 {@link ErpMfgCostRollup}(status=CALCULATED) + {@link ErpMfgCostRollupLine}（每物料一行，
 * 均为单位标准成本分解）。FIRMED 由人工动作置位（Non-Goal）。scrapRate 本期不纳入（Non-Goal）。
 *
 * <p>本类为非 BizModel 服务助手（对齐 inventory costing 范式），直接用 {@link IDaoProvider}。
 */
public class CostRollupService {

    static final BigDecimal SIXTY = new BigDecimal("60");
    static final int SCALE = 6;
    static final RoundingMode RM = RoundingMode.HALF_UP;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    BomExpander bomExpander;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public void setBomExpander(BomExpander bomExpander) {
        this.bomExpander = bomExpander;
    }

    /**
     * 卷算指定 BOM 的产出物料（及其全部子件）的单位标准成本，并落库。
     */
    public CostRollupResult rollup(Long bomId) {
        ErpMfgBom bom = requireBom(bomId);

        Map<Long, CostBreakdown> computed = new LinkedHashMap<>();
        computeUnit(bom.getProductId(), computed, new LinkedHashSet<>());

        ErpMfgCostRollup head = createHead(bom);
        writeLines(head.getId(), computed);

        return toResult(head, computed);
    }

    private ErpMfgCostRollup createHead(ErpMfgBom bom) {
        IEntityDao<ErpMfgCostRollup> dao = daoProvider.daoFor(ErpMfgCostRollup.class);
        ErpMfgCostRollup head = dao.newEntity();
        LocalDate today = CoreMetrics.today();
        head.setCode("ROLLUP-" + today.toString() + "-" + bom.getId());
        head.setBusinessDate(today);
        head.setStatus(ErpMfgConstants.COST_ROLLUP_STATUS_CALCULATED);
        head.setCostingVersion("STD-" + today.toString());
        dao.saveEntity(head);
        return head;
    }

    private void writeLines(Long rollupId, Map<Long, CostBreakdown> computed) {
        IEntityDao<ErpMfgCostRollupLine> dao = daoProvider.daoFor(ErpMfgCostRollupLine.class);
        int lineNo = 10;
        for (Map.Entry<Long, CostBreakdown> e : computed.entrySet()) {
            CostBreakdown cb = e.getValue();
            ErpMfgCostRollupLine line = dao.newEntity();
            line.setCostRollupId(rollupId);
            line.setLineNo(lineNo);
            line.setMaterialId(e.getKey());
            line.setUoMId(cb.uoMId);
            line.setMaterialCost(scale(cb.material));
            line.setLaborCost(scale(cb.labor));
            line.setOverheadCost(scale(cb.overhead));
            line.setSubcontractCost(scale(cb.subcontract));
            line.setTotalCost(scale(cb.unit));
            line.setUnitCost(scale(cb.unit));
            dao.saveEntity(line);
            lineNo += 10;
        }
    }

    private CostBreakdown computeUnit(Long materialId, Map<Long, CostBreakdown> computed, Set<Long> path) {
        CostBreakdown cached = computed.get(materialId);
        if (cached != null) {
            return cached;
        }
        if (path.contains(materialId)) {
            throw new NopException(ErpMfgErrors.ERR_BOM_CYCLE)
                    .param(ErpMfgErrors.ARG_MATERIAL_ID, materialId)
                    .param(ErpMfgErrors.ARG_PATH, path.toString());
        }
        ErpMdMaterial material = daoProvider.daoFor(ErpMdMaterial.class).getEntityById(materialId);
        CostBreakdown cb = new CostBreakdown();
        cb.uoMId = material != null ? material.getUoMId() : null;

        ErpMfgBom bom = bomExpander.findDefaultBomOrNull(materialId);
        if (bom == null) {
            // 采购件：默认 SKU 采购价为基础成本
            BigDecimal price = defaultSkuPurchasePrice(materialId);
            if (price == null) {
                throw new NopException(ErpMfgErrors.ERR_ROLLUP_BASE_COST_MISSING)
                        .param(ErpMfgErrors.ARG_MATERIAL_ID, materialId);
            }
            cb.material = price;
            cb.labor = BigDecimal.ZERO;
            cb.overhead = BigDecimal.ZERO;
            cb.subcontract = aggregateSubcontractCost(materialId);
            cb.unit = cb.material.add(cb.subcontract);
        } else {
            path.add(materialId);
            try {
                BigDecimal bomQty = nz(bom.getQty());
                BigDecimal mat = BigDecimal.ZERO;
                for (ErpMfgBomLine line : loadLines(bom.getId())) {
                    BigDecimal qtyPerUnit = divide(nz(line.getQuantity()), bomQty);
                    CostBreakdown child = computeUnit(line.getMaterialId(), computed, path);
                    mat = mat.add(qtyPerUnit.multiply(child.unit));
                }
                OperationCost oc = sumOperationCost(bom.getId());
                BigDecimal overhead = computeOverhead(oc);
                cb.material = mat;
                cb.labor = oc.labor;
                cb.overhead = overhead;
                cb.subcontract = aggregateSubcontractCost(materialId);
                cb.unit = mat.add(oc.labor).add(overhead).add(cb.subcontract);
            } finally {
                path.remove(materialId);
            }
        }
        computed.put(materialId, cb);
        return cb;
    }

    private OperationCost sumOperationCost(Long bomId) {
        OperationCost oc = new OperationCost();
        for (ErpMfgBomOperation op : loadOperations(bomId)) {
            BigDecimal minutes = nz(op.getStandardTime());
            Long wcId = op.getWorkcenterId();
            if (wcId == null || minutes.signum() <= 0) {
                continue;
            }
            ErpMfgWorkcenter wc = daoProvider.daoFor(ErpMfgWorkcenter.class).getEntityById(wcId);
            if (wc == null) {
                continue;
            }
            BigDecimal rate = nz(wc.getHourlyRate());
            if (rate.signum() <= 0) {
                continue;
            }
            BigDecimal hours = divide(minutes, SIXTY);
            oc.labor = oc.labor.add(hours.multiply(rate));
            oc.machineHours = oc.machineHours.add(hours);
        }
        return oc;
    }

    /**
     * 计算 overhead 制造费用（plan 2026-07-13-0455-2 §Phase 1）。
     * config 关时恒 0（向后兼容）；开时按 mode × rate 计算：
     * MACHINE_HOUR = 机器工时 × rate；LABOR_RATIO = laborCost × rate。
     */
    BigDecimal computeOverhead(OperationCost oc) {
        if (!overheadAllocationEnabled()) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = overheadAllocationRate();
        if (rate.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        String mode = overheadAllocationMode();
        if (ErpMfgConstants.OVERHEAD_ALLOCATION_MODE_MACHINE_HOUR.equals(mode)) {
            return oc.machineHours.multiply(rate);
        }
        if (ErpMfgConstants.OVERHEAD_ALLOCATION_MODE_LABOR_RATIO.equals(mode)) {
            return oc.labor.multiply(rate);
        }
        return BigDecimal.ZERO;
    }

    private boolean overheadAllocationEnabled() {
        Boolean flag = AppConfig.var(ErpMfgConstants.CONFIG_OVERHEAD_ALLOCATION_ENABLED, Boolean.FALSE);
        return Boolean.TRUE.equals(flag);
    }

    private String overheadAllocationMode() {
        String mode = AppConfig.var(ErpMfgConstants.CONFIG_OVERHEAD_ALLOCATION_MODE,
                ErpMfgConstants.OVERHEAD_ALLOCATION_MODE_MACHINE_HOUR);
        return mode != null ? mode : ErpMfgConstants.OVERHEAD_ALLOCATION_MODE_MACHINE_HOUR;
    }

    private BigDecimal overheadAllocationRate() {
        String raw = AppConfig.var(ErpMfgConstants.CONFIG_OVERHEAD_ALLOCATION_RATE,
                ErpMfgConstants.DEFAULT_OVERHEAD_ALLOCATION_RATE);
        if (raw == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 归集 subcontract 委外费（plan 2026-07-13-0455-2 §Phase 2）。
     * config 关时恒 0（向后兼容）；开时按物料聚合已过账（COMPLETED）委外订单加工费，
     * 按产量（委外行 quantity 之和）分摊为单位委外成本。归集源来自 N=1（2026-07-13-0455-1）。
     */
    BigDecimal aggregateSubcontractCost(Long materialId) {
        if (!subcontractAggregationEnabled()) {
            return BigDecimal.ZERO;
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("productId", materialId));
        q.addFilter(eq("docStatus", ErpMfgConstants.SUBCONTRACT_STATUS_COMPLETED));
        List<ErpMfgSubcontractOrder> orders = daoProvider.daoFor(ErpMfgSubcontractOrder.class).findAllByQuery(q);
        if (orders.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalFee = BigDecimal.ZERO;
        BigDecimal totalQty = BigDecimal.ZERO;
        for (ErpMfgSubcontractOrder order : orders) {
            totalFee = totalFee.add(nz(order.getProcessingFee()));
            for (ErpMfgSubcontractOrderLine line : loadSubcontractLines(order.getId(), materialId)) {
                totalQty = totalQty.add(nz(line.getQuantity()));
            }
        }
        if (totalQty.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return divide(totalFee, totalQty);
    }

    private boolean subcontractAggregationEnabled() {
        Boolean flag = AppConfig.var(ErpMfgConstants.CONFIG_SUBCONTRACT_COST_AGGREGATION_ENABLED, Boolean.FALSE);
        return Boolean.TRUE.equals(flag);
    }

    private List<ErpMfgSubcontractOrderLine> loadSubcontractLines(Long orderId, Long materialId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("subcontractOrderId", orderId));
        q.addFilter(eq("materialId", materialId));
        return daoProvider.daoFor(ErpMfgSubcontractOrderLine.class).findAllByQuery(q);
    }

    private BigDecimal defaultSkuPurchasePrice(Long materialId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("isDefault", Boolean.TRUE));
        q.setLimit(1);
        List<ErpMdMaterialSku> list = daoProvider.daoFor(ErpMdMaterialSku.class).findAllByQuery(q);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0).getPurchasePrice();
    }

    private ErpMfgBom requireBom(Long bomId) {
        if (bomId == null) {
            throw new NopException(ErpMfgErrors.ERR_BOM_NOT_FOUND).param(ErpMfgErrors.ARG_BOM_ID, bomId);
        }
        ErpMfgBom bom = daoProvider.daoFor(ErpMfgBom.class).getEntityById(bomId);
        if (bom == null) {
            throw new NopException(ErpMfgErrors.ERR_BOM_NOT_FOUND).param(ErpMfgErrors.ARG_BOM_ID, bomId);
        }
        return bom;
    }

    private List<ErpMfgBomLine> loadLines(Long bomId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("bomId", bomId));
        q.addOrderField("lineNo", false);
        return daoProvider.daoFor(ErpMfgBomLine.class).findAllByQuery(q);
    }

    private List<ErpMfgBomOperation> loadOperations(Long bomId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("bomId", bomId));
        q.addOrderField("lineNo", false);
        return daoProvider.daoFor(ErpMfgBomOperation.class).findAllByQuery(q);
    }

    private CostRollupResult toResult(ErpMfgCostRollup head, Map<Long, CostBreakdown> computed) {
        CostRollupResult result = new CostRollupResult();
        result.setRollupId(head.getId());
        result.setStatus(head.getStatus());
        for (Map.Entry<Long, CostBreakdown> e : computed.entrySet()) {
            CostBreakdown cb = e.getValue();
            CostRollupLineView v = new CostRollupLineView();
            v.setMaterialId(e.getKey());
            v.setMaterialCost(scale(cb.material));
            v.setLaborCost(scale(cb.labor));
            v.setOverheadCost(scale(cb.overhead));
            v.setSubcontractCost(scale(cb.subcontract));
            v.setTotalCost(scale(cb.unit));
            v.setUnitCost(scale(cb.unit));
            result.getLines().add(v);
        }
        return result;
    }

    static BigDecimal divide(BigDecimal a, BigDecimal b) {
        if (b.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return a.divide(b, SCALE, RM);
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    static BigDecimal scale(BigDecimal v) {
        return nz(v).setScale(4, RM);
    }

    private static class CostBreakdown {
        BigDecimal material = BigDecimal.ZERO;
        BigDecimal labor = BigDecimal.ZERO;
        BigDecimal overhead = BigDecimal.ZERO;
        BigDecimal subcontract = BigDecimal.ZERO;
        BigDecimal unit = BigDecimal.ZERO;
        Long uoMId;
    }

    /** 工序工时成本中间结果（labor=人工费，machineHours=机器工时，供 overhead 分配率计算）。 */
    static class OperationCost {
        BigDecimal labor = BigDecimal.ZERO;
        BigDecimal machineHours = BigDecimal.ZERO;
    }
}
