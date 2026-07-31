package app.erp.drp.service.processor;

import app.erp.drp.dao.entity.ErpDrpPlan;
import app.erp.drp.service.ErpDrpConstants;
import app.erp.drp.service.ErpDrpErrors;
import app.erp.drp.service.simulation.SimulationDrpEngine;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpDrpScenario promoteToFormalPlan per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含仿真版本转正编排：config 门（{@code erp-drp.simulation-enabled}）+ 委派 {@link SimulationDrpEngine#promoteToFormalPlan} 产出正式 DrpPlan。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpDrpScenarioPromoteToFormalPlanProcessor {

    @Inject
    SimulationDrpEngine simulationDrpEngine;

    public ErpDrpPlan promoteToFormalPlan(Long scenarioVersionId, IServiceContext context) {
        requireSimulationEnabled(scenarioVersionId);
        return simulationDrpEngine.promoteToFormalPlan(scenarioVersionId);
    }

    // ---------- 内部辅助 ----------

    protected void requireSimulationEnabled(Long id) {
        boolean enabled = AppConfig.var(ErpDrpConstants.CONFIG_DRP_SIMULATION_ENABLED,
                ErpDrpConstants.DEFAULT_DRP_SIMULATION_ENABLED);
        if (!enabled) {
            throw new NopException(ErpDrpErrors.ERR_DRP_SIMULATION_DISABLED)
                    .param(ErpDrpErrors.ARG_SCENARIO_ID, id);
        }
    }
}
