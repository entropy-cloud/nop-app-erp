package app.erp.drp.service.simulation;

import app.erp.drp.dao.entity.ErpDrpScenarioParam;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * DRP 仿真参数变体覆盖解析器实现（plan 2026-07-22-1000-2 §DRP 对应物）。
 *
 * <p>同构 MRP {@code ErpMfgSimulationParamResolver}：按 scenarioId 进程内缓存；查询顺序：
 * 精确 (material,warehouse) → material+warehouse=null → null+warehouse=null → 未覆盖返回 null。
 */
public class ErpDrpSimulationParamResolver implements IErpDrpSimulationParamResolver {

    @Inject
    IDaoProvider daoProvider;

    private final Map<Long, List<ErpDrpScenarioParam>> cache = new HashMap<>();

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    @Override
    public BigDecimal resolveOverride(Long scenarioId, Long materialId, Long warehouseId, String paramType) {
        if (scenarioId == null || paramType == null) {
            return null;
        }
        List<ErpDrpScenarioParam> params = loadParams(scenarioId);

        // 1. 精确 (material, warehouse)
        BigDecimal exact = find(params, materialId, warehouseId, paramType);
        if (exact != null) return exact;
        // 2. material + warehouse=null
        if (warehouseId != null) {
            BigDecimal m = find(params, materialId, null, paramType);
            if (m != null) return m;
        }
        // 3. material=null + warehouse
        if (materialId != null) {
            BigDecimal w = find(params, null, warehouseId, paramType);
            if (w != null) return w;
        }
        // 4. 全局（null, null）
        if (materialId != null || warehouseId != null) {
            BigDecimal g = find(params, null, null, paramType);
            if (g != null) return g;
        }
        return null;
    }

    private BigDecimal find(List<ErpDrpScenarioParam> params, Long materialId, Long warehouseId, String paramType) {
        for (ErpDrpScenarioParam p : params) {
            if (Objects.equals(materialId, p.getMaterialId())
                    && Objects.equals(warehouseId, p.getWarehouseId())
                    && Objects.equals(paramType, p.getParamType())) {
                return p.getParamValue();
            }
        }
        return null;
    }

    @Override
    public List<ErpDrpScenarioParam> loadParams(Long scenarioId) {
        if (scenarioId == null) {
            return Collections.emptyList();
        }
        synchronized (cache) {
            return cache.computeIfAbsent(scenarioId, this::doLoadParams);
        }
    }

    public void invalidateCache() {
        synchronized (cache) {
            cache.clear();
        }
    }

    private List<ErpDrpScenarioParam> doLoadParams(Long scenarioId) {
        IEntityDao<ErpDrpScenarioParam> dao = daoProvider.daoFor(ErpDrpScenarioParam.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("scenarioId", scenarioId));
        return dao.findAllByQuery(q);
    }
}
