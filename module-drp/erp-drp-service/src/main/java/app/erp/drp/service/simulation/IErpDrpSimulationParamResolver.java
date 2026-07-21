package app.erp.drp.service.simulation;

import app.erp.drp.dao.entity.ErpDrpScenarioParam;
import app.erp.drp.service.ErpDrpConstants;

import java.math.BigDecimal;
import java.util.List;

/**
 * DRP 仿真场景参数变体覆盖解析器（plan 2026-07-22-1000-2 §DRP 对应物；权威：
 * `docs/design/manufacturing/simulation-engine.md §参数变体覆盖语义`）。
 *
 * <p>同构 MRP {@code IErpMfgSimulationParamResolver}：DRP 参数以 (materialId, warehouseId, paramType) 三键定位。
 * 回退顺序：精确 (material,warehouse) → material+warehouse=null → null+warehouse=null → null。
 */
public interface IErpDrpSimulationParamResolver {

    BigDecimal resolveOverride(Long scenarioId, Long materialId, Long warehouseId, String paramType);

    List<ErpDrpScenarioParam> loadParams(Long scenarioId);

    default BigDecimal resolveSafetyStockOverride(Long scenarioId, Long materialId, Long warehouseId) {
        return resolveOverride(scenarioId, materialId, warehouseId, ErpDrpConstants.SIMULATION_PARAM_TYPE_SAFETY_STOCK);
    }

    default BigDecimal resolveLeadTimeOverride(Long scenarioId, Long materialId, Long warehouseId) {
        return resolveOverride(scenarioId, materialId, warehouseId, ErpDrpConstants.SIMULATION_PARAM_TYPE_LEAD_TIME);
    }

    default BigDecimal resolveReplenishmentQtyOverride(Long scenarioId, Long materialId, Long warehouseId) {
        return resolveOverride(scenarioId, materialId, warehouseId, ErpDrpConstants.SIMULATION_PARAM_TYPE_REPLENISHMENT_QTY);
    }
}
