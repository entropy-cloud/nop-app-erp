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
import app.erp.mfg.dao.entity.ErpMfgWorkcenter;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.mfg.service.bom.BomExpander;
import io.nop.api.core.beans.query.QueryBean;
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
 * 故工序工时成本统一计入 {@code laborCost}，{@code overheadCost}=0；待工作中心费率拆分后细化（Follow-up）。
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
            line.setSubcontractCost(BigDecimal.ZERO);
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
            cb.unit = price;
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
                BigDecimal labor = sumOperationLabor(bom.getId());
                cb.material = mat;
                cb.labor = labor;
                cb.overhead = BigDecimal.ZERO;
                cb.unit = mat.add(labor);
            } finally {
                path.remove(materialId);
            }
        }
        computed.put(materialId, cb);
        return cb;
    }

    private BigDecimal sumOperationLabor(Long bomId) {
        BigDecimal labor = BigDecimal.ZERO;
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
            labor = labor.add(divide(minutes, SIXTY).multiply(rate));
        }
        return labor;
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
        BigDecimal unit = BigDecimal.ZERO;
        Long uoMId;
    }
}
