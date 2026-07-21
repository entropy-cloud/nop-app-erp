package app.erp.drp.service.simulation;

import app.erp.drp.dao.dto.DrpSimulationDiffResult;
import app.erp.drp.dao.entity.ErpDrpLine;
import app.erp.drp.dao.entity.ErpDrpScenarioVersion;
import app.erp.drp.service.ErpDrpErrors;
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
 * DRP 仿真版本对比引擎（plan 2026-07-22-1000-2 §结果对比算法 Decision C）。
 *
 * <p>2 维 diff（B - A 符号约定）：补货量差（suggestedQty） / 安全库存差（safetyStock）。
 * 同构 MRP {@code SimulationVersionComparator}，但维度更少（DRP 无 BOM 展开/pegging）。
 */
public class DrpSimulationVersionComparator {

    @Inject
    IDaoProvider daoProvider;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public DrpSimulationDiffResult compareDrpVersions(Long versionIdA, Long versionIdB) {
        ErpDrpScenarioVersion va = requireVersion(versionIdA);
        ErpDrpScenarioVersion vb = requireVersion(versionIdB);
        requireComparable(va, vb);

        Map<String, ErpDrpLine> linesA = indexLines(va.getComputedDrpPlanId());
        Map<String, ErpDrpLine> linesB = indexLines(vb.getComputedDrpPlanId());

        Set<String> allKeys = new LinkedHashSet<>();
        allKeys.addAll(linesA.keySet());
        allKeys.addAll(linesB.keySet());

        List<DrpSimulationDiffResult.LineDiff> lineDiffs = new ArrayList<>();
        BigDecimal totalReplenishmentDelta = BigDecimal.ZERO;
        BigDecimal totalSafetyStockDelta = BigDecimal.ZERO;

        for (String key : allKeys) {
            ErpDrpLine la = linesA.get(key);
            ErpDrpLine lb = linesB.get(key);

            BigDecimal suggestedA = la != null ? nz(la.getSuggestedQty()) : BigDecimal.ZERO;
            BigDecimal suggestedB = lb != null ? nz(lb.getSuggestedQty()) : BigDecimal.ZERO;
            BigDecimal safetyA = la != null ? nz(la.getSafetyStock()) : BigDecimal.ZERO;
            BigDecimal safetyB = lb != null ? nz(lb.getSafetyStock()) : BigDecimal.ZERO;

            DrpSimulationDiffResult.LineDiff d = new DrpSimulationDiffResult.LineDiff();
            d.setMaterialId(la != null ? la.getMaterialId() : (lb != null ? lb.getMaterialId() : null));
            d.setWarehouseId(la != null ? la.getWarehouseId() : (lb != null ? lb.getWarehouseId() : null));
            d.setSuggestedQtyA(suggestedA);
            d.setSuggestedQtyB(suggestedB);
            d.setReplenishmentQtyDelta(suggestedB.subtract(suggestedA));
            d.setSafetyStockA(safetyA);
            d.setSafetyStockB(safetyB);
            d.setSafetyStockDelta(safetyB.subtract(safetyA));
            lineDiffs.add(d);

            totalReplenishmentDelta = totalReplenishmentDelta.add(d.getReplenishmentQtyDelta());
            totalSafetyStockDelta = totalSafetyStockDelta.add(d.getSafetyStockDelta());
        }

        DrpSimulationDiffResult result = new DrpSimulationDiffResult();
        result.setVersionIdA(versionIdA);
        result.setVersionIdB(versionIdB);
        result.setLineDiffs(lineDiffs);
        result.setTotalReplenishmentQtyDelta(totalReplenishmentDelta);
        result.setTotalSafetyStockDelta(totalSafetyStockDelta);
        return result;
    }

    private ErpDrpScenarioVersion requireVersion(Long versionId) {
        ErpDrpScenarioVersion v = daoProvider.daoFor(ErpDrpScenarioVersion.class).getEntityById(versionId);
        if (v == null) {
            throw new NopException(ErpDrpErrors.ERR_DRP_SIMULATION_VERSION_ALREADY_PROMOTED)
                    .param(ErpDrpErrors.ARG_SCENARIO_VERSION_ID, versionId);
        }
        return v;
    }

    private void requireComparable(ErpDrpScenarioVersion a, ErpDrpScenarioVersion b) {
        if (!Objects.equals(a.getScenarioId(), b.getScenarioId())) {
            throw new NopException(ErpDrpErrors.ERR_DRP_SIMULATION_VERSIONS_NOT_COMPARABLE)
                    .param(ErpDrpErrors.ARG_SCENARIO_VERSION_ID, a.getId());
        }
    }

    private Map<String, ErpDrpLine> indexLines(Long planId) {
        Map<String, ErpDrpLine> byKey = new LinkedHashMap<>();
        if (planId == null) return byKey;
        QueryBean q = new QueryBean();
        q.addFilter(eq("planId", planId));
        for (ErpDrpLine l : daoProvider.daoFor(ErpDrpLine.class).findAllByQuery(q)) {
            byKey.put(key(l.getMaterialId(), l.getWarehouseId()), l);
        }
        return byKey;
    }

    private String key(Long materialId, Long warehouseId) {
        return (materialId == null ? "_" : materialId) + ":" + (warehouseId == null ? "_" : warehouseId);
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
