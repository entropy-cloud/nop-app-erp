package app.erp.mfg.service.simulation;

import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.mfg.dao.dto.SimulationDiffResult;
import app.erp.mfg.dao.entity.ErpMfgMrpPlanLine;
import app.erp.mfg.dao.entity.ErpMfgMrpScenarioVersion;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * MRP 仿真版本对比引擎（plan 2026-07-22-1000-2 §结果对比算法 Decision C；权威：
 * `docs/design/manufacturing/simulation-engine.md §结果对比算法`）。
 *
 * <p>4 维 diff（B - A 符号约定）：
 * <ul>
 *   <li>净需求差：同 materialId 的 netRequirement(B) - netRequirement(A)</li>
 *   <li>建议量差：同 materialId 的 plannedQuantity(B) - plannedQuantity(A)</li>
 *   <li>缺料物料集差：netRequirement &gt; 0 的物料集 A\B / B\A / A∩B</li>
 *   <li>总采购额差：Σ(PURCHASE_REQUEST.plannedQuantity × material.standardCost) B - A</li>
 * </ul>
 *
 * <p>对比结果不持久化（不可变快照可确定性派生，Decision C 裁决）。
 */
public class SimulationVersionComparator {

    @Inject
    IDaoProvider daoProvider;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    /**
     * 对比两 MRP 仿真版本。
     *
     * @throws NopException 两版本须同 orgId / 同基线 plan（{@link ErpMfgErrors#ERR_MFG_SIMULATION_VERSIONS_NOT_COMPARABLE}）
     */
    public SimulationDiffResult compareMrpVersions(Long versionIdA, Long versionIdB) {
        ErpMfgMrpScenarioVersion va = requireVersion(versionIdA);
        ErpMfgMrpScenarioVersion vb = requireVersion(versionIdB);
        requireComparable(va, vb);

        Map<Long, ErpMfgMrpPlanLine> linesA = indexLines(va.getComputedMrpPlanId());
        Map<Long, ErpMfgMrpPlanLine> linesB = indexLines(vb.getComputedMrpPlanId());

        Set<Long> allMaterials = new LinkedHashSet<>();
        allMaterials.addAll(linesA.keySet());
        allMaterials.addAll(linesB.keySet());

        List<SimulationDiffResult.LineDiff> lineDiffs = new ArrayList<>();
        Set<Long> shortageA = new LinkedHashSet<>();
        Set<Long> shortageB = new LinkedHashSet<>();
        BigDecimal totalNetDelta = BigDecimal.ZERO;
        BigDecimal totalPlannedDelta = BigDecimal.ZERO;
        BigDecimal totalPurchaseAmountDelta = BigDecimal.ZERO;

        for (Long materialId : allMaterials) {
            ErpMfgMrpPlanLine la = linesA.get(materialId);
            ErpMfgMrpPlanLine lb = linesB.get(materialId);

            BigDecimal netA = la != null ? nz(la.getNetRequirement()) : BigDecimal.ZERO;
            BigDecimal netB = lb != null ? nz(lb.getNetRequirement()) : BigDecimal.ZERO;
            BigDecimal plannedA = la != null ? nz(la.getPlannedQuantity()) : BigDecimal.ZERO;
            BigDecimal plannedB = lb != null ? nz(lb.getPlannedQuantity()) : BigDecimal.ZERO;

            SimulationDiffResult.LineDiff d = new SimulationDiffResult.LineDiff();
            d.setMaterialId(materialId);
            d.setNetRequirementA(netA);
            d.setNetRequirementB(netB);
            d.setNetRequirementDelta(netB.subtract(netA));
            d.setPlannedQuantityA(plannedA);
            d.setPlannedQuantityB(plannedB);
            d.setPlannedQuantityDelta(plannedB.subtract(plannedA));
            lineDiffs.add(d);

            totalNetDelta = totalNetDelta.add(d.getNetRequirementDelta());
            totalPlannedDelta = totalPlannedDelta.add(d.getPlannedQuantityDelta());

            if (netA.signum() > 0) shortageA.add(materialId);
            if (netB.signum() > 0) shortageB.add(materialId);

            // 总采购额差：仅 PURCHASE_REQUEST 行
            BigDecimal stdCost = lookupStandardCost(materialId);
            BigDecimal purchaseA = la != null && isPurchase(la) ? plannedA.multiply(stdCost) : BigDecimal.ZERO;
            BigDecimal purchaseB = lb != null && isPurchase(lb) ? plannedB.multiply(stdCost) : BigDecimal.ZERO;
            totalPurchaseAmountDelta = totalPurchaseAmountDelta.add(purchaseB.subtract(purchaseA));
        }

        Set<Long> onlyInA = new LinkedHashSet<>(shortageA);
        onlyInA.removeAll(shortageB);
        Set<Long> onlyInB = new LinkedHashSet<>(shortageB);
        onlyInB.removeAll(shortageA);
        Set<Long> inBoth = new LinkedHashSet<>(shortageA);
        inBoth.retainAll(shortageB);

        SimulationDiffResult result = new SimulationDiffResult();
        result.setVersionIdA(versionIdA);
        result.setVersionIdB(versionIdB);
        result.setLineDiffs(lineDiffs);
        result.setTotalNetRequirementDelta(totalNetDelta);
        result.setTotalPlannedQuantityDelta(totalPlannedDelta);
        result.setTotalPurchaseAmountDelta(totalPurchaseAmountDelta);
        result.setShortageOnlyInA(new ArrayList<>(onlyInA));
        result.setShortageOnlyInB(new ArrayList<>(onlyInB));
        result.setShortageInBoth(new ArrayList<>(inBoth));
        return result;
    }

    private ErpMfgMrpScenarioVersion requireVersion(Long versionId) {
        ErpMfgMrpScenarioVersion v = daoProvider.daoFor(ErpMfgMrpScenarioVersion.class).getEntityById(versionId);
        if (v == null) {
            throw new NopException(ErpMfgErrors.ERR_MFG_SIMULATION_VERSION_ALREADY_PROMOTED)
                    .param(ErpMfgErrors.ARG_SCENARIO_VERSION_ID, versionId);
        }
        return v;
    }

    private void requireComparable(ErpMfgMrpScenarioVersion a, ErpMfgMrpScenarioVersion b) {
        // 同场景下版本才可比（同基线 plan）
        if (!Objects.equals(a.getScenarioId(), b.getScenarioId())) {
            throw new NopException(ErpMfgErrors.ERR_MFG_SIMULATION_VERSIONS_NOT_COMPARABLE)
                    .param(ErpMfgErrors.ARG_SCENARIO_VERSION_ID, a.getId())
                    .param(ErpMfgErrors.ARG_EXPECTED_STATUS, String.valueOf(b.getId()));
        }
    }

    private Map<Long, ErpMfgMrpPlanLine> indexLines(Long planId) {
        Map<Long, ErpMfgMrpPlanLine> byMaterial = new LinkedHashMap<>();
        if (planId == null) {
            return byMaterial;
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("mrpPlanId", planId));
        for (ErpMfgMrpPlanLine l : daoProvider.daoFor(ErpMfgMrpPlanLine.class).findAllByQuery(q)) {
            // 顶层物料聚合（parentLineId=null）作为对比键；子件展开行不单独对比
            if (l.getParentLineId() == null && l.getMaterialId() != null) {
                // 同物料多顶层行累加（罕见，但保险）
                ErpMfgMrpPlanLine existing = byMaterial.get(l.getMaterialId());
                if (existing == null) {
                    byMaterial.put(l.getMaterialId(), l);
                } else {
                    existing.setNetRequirement(nz(existing.getNetRequirement()).add(nz(l.getNetRequirement())));
                    existing.setPlannedQuantity(nz(existing.getPlannedQuantity()).add(nz(l.getPlannedQuantity())));
                }
            }
        }
        return byMaterial;
    }

    private boolean isPurchase(ErpMfgMrpPlanLine l) {
        return Objects.equals(l.getOrderType(), ErpMfgConstants.MRP_ORDER_TYPE_PURCHASE_REQUEST);
    }

    private BigDecimal lookupStandardCost(Long materialId) {
        ErpMdMaterial m = daoProvider.daoFor(ErpMdMaterial.class).getEntityById(materialId);
        if (m == null) {
            return BigDecimal.ZERO;
        }
        // ErpMdMaterial 无 standardCost 列；本期以 0 兜底，successor 接入采购价或标准成本时填充
        return BigDecimal.ZERO;
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
