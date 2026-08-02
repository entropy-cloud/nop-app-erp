package app.erp.drp.service.processor;

import app.erp.drp.dao.entity.ErpDrpScenarioVersion;
import app.erp.drp.service.ErpDrpConstants;
import app.erp.drp.service.ErpDrpErrors;
import app.erp.drp.service.simulation.SimulationDrpEngine;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpDrpScenario runSimulation per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含仿真运行编排：config 门（{@code erp-drp.simulation-enabled}）+ 委派 {@link SimulationDrpEngine#runSimulation} 产出 ScenarioVersion。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpDrpScenarioRunSimulationProcessor {

    @Inject
    SimulationDrpEngine simulationDrpEngine;

    public ErpDrpScenarioVersion runSimulation(Long scenarioId, IServiceContext context) {
        requireSimulationEnabled(scenarioId);
        return simulationDrpEngine.runSimulation(scenarioId);
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
