package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgMrpPlan;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.mfg.service.simulation.SimulationMrpEngine;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpMfgMrpScenario promoteToFormalPlan per-mutation Processor（R6.2，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含仿真版本提升为正式计划编排（config-gate + 委托 {@link SimulationMrpEngine}）；从 ErpMfgMrpScenarioBizModel 内联 @BizMutation 提取。
 */
public class ErpMfgMrpScenarioPromoteToFormalPlanProcessor {

    @Inject
    SimulationMrpEngine simulationMrpEngine;

    public ErpMfgMrpPlan promoteToFormalPlan(@Name("scenarioVersionId") Long scenarioVersionId, IServiceContext context) {
        requireSimulationEnabled(scenarioVersionId);
        return simulationMrpEngine.promoteToFormalPlan(scenarioVersionId);
    }

    protected void requireSimulationEnabled(Long id) {
        boolean enabled = AppConfig.var(ErpMfgConstants.CONFIG_MFG_SIMULATION_ENABLED,
                ErpMfgConstants.DEFAULT_MFG_SIMULATION_ENABLED);
        if (!enabled) {
            throw new NopException(ErpMfgErrors.ERR_MFG_SIMULATION_DISABLED)
                    .param(ErpMfgErrors.ARG_SCENARIO_ID, id);
        }
    }
}
