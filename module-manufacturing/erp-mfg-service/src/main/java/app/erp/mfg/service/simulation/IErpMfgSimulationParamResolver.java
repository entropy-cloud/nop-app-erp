package app.erp.mfg.service.simulation;

import app.erp.mfg.dao.entity.ErpMfgMrpScenarioParam;
import app.erp.mfg.service.ErpMfgConstants;

import java.math.BigDecimal;
import java.util.List;

/**
 * MRP 仿真场景参数变体覆盖解析器（plan 2026-07-22-1000-2 §参数变体解析；权威：
 * `docs/design/manufacturing/simulation-engine.md §参数变体覆盖语义`）。
 *
 * <p>对齐 D3 `CostingStrategy` + A3 `TransferPriceResolver` 范式：进程内一次性加载场景参数到 Map 缓存，
 * O(1) 查询；CRUD 不主动失效（场景版本一旦生成即不可变，参数变更须新建版本）。
 *
 * <p>回退顺序（Decision B）：场景物料级覆盖 → 场景全局覆盖（materialId=null）→ 返回 null（调用方回退全局配置/主数据）。
 */
public interface IErpMfgSimulationParamResolver {

    /**
     * 解析某物料某参数类型的覆盖值。
     *
     * @param scenarioId 仿真场景 ID
     * @param materialId 物料 ID（null 时仅查全局覆盖）
     * @param paramType  参数类型（LEAD_TIME / LOT_SIZE / SAFETY_STOCK）
     * @return 覆盖值；未覆盖返回 null（调用方回退全局配置/主数据）
     */
    BigDecimal resolveOverride(Long scenarioId, Long materialId, String paramType);

    /**
     * 加载场景的全部参数行（供 SimulationMrpEngine 构建 override context）。
     */
    List<ErpMfgMrpScenarioParam> loadParams(Long scenarioId);

    /**
     * 解析有效提前期（天）覆盖。未覆盖返回 null（调用方回退主数据 leadTimeDays）。
     */
    default BigDecimal resolveLeadTimeOverride(Long scenarioId, Long materialId) {
        return resolveOverride(scenarioId, materialId, ErpMfgConstants.SIMULATION_PARAM_TYPE_LEAD_TIME);
    }

    /**
     * 解析有效全局 lot size 覆盖。未覆盖返回 null（调用方回退 AppConfig CONFIG_MRP_DEFAULT_LOT_SIZE）。
     */
    default BigDecimal resolveLotSizeOverride(Long scenarioId) {
        return resolveOverride(scenarioId, null, ErpMfgConstants.SIMULATION_PARAM_TYPE_LOT_SIZE);
    }

    /**
     * 解析有效安全库存覆盖。未覆盖返回 null（调用方回退主数据 safetyStock）。
     */
    default BigDecimal resolveSafetyStockOverride(Long scenarioId, Long materialId) {
        return resolveOverride(scenarioId, materialId, ErpMfgConstants.SIMULATION_PARAM_TYPE_SAFETY_STOCK);
    }
}

