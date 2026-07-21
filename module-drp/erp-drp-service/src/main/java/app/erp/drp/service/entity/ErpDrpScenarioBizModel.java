
package app.erp.drp.service.entity;

import app.erp.drp.biz.IErpDrpScenarioBiz;
import app.erp.drp.dao.dto.DrpSimulationDiffResult;
import app.erp.drp.dao.entity.ErpDrpPlan;
import app.erp.drp.dao.entity.ErpDrpScenario;
import app.erp.drp.dao.entity.ErpDrpScenarioVersion;
import app.erp.drp.service.ErpDrpConstants;
import app.erp.drp.service.ErpDrpErrors;
import app.erp.drp.service.simulation.DrpSimulationVersionComparator;
import app.erp.drp.service.simulation.SimulationDrpEngine;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * DRP 仿真场景 BizModel（plan 2026-07-22-1000-2 §DRP 对应物）。
 *
 * <p>薄委派层，同构 MRP {@code ErpMfgMrpScenarioBizModel}。
 */
@BizModel("ErpDrpScenario")
public class ErpDrpScenarioBizModel extends CrudBizModel<ErpDrpScenario> implements IErpDrpScenarioBiz {

    @Inject
    SimulationDrpEngine simulationDrpEngine;
    @Inject
    DrpSimulationVersionComparator simulationComparator;

    public ErpDrpScenarioBizModel() {
        setEntityName(ErpDrpScenario.class.getName());
    }

    public void setSimulationDrpEngine(SimulationDrpEngine simulationDrpEngine) {
        this.simulationDrpEngine = simulationDrpEngine;
    }

    public void setSimulationComparator(DrpSimulationVersionComparator simulationComparator) {
        this.simulationComparator = simulationComparator;
    }

    @Override
    @BizMutation
    public ErpDrpScenarioVersion runSimulation(@Name("scenarioId") Long scenarioId, IServiceContext context) {
        requireSimulationEnabled(scenarioId);
        return simulationDrpEngine.runSimulation(scenarioId);
    }

    @Override
    @BizMutation
    public ErpDrpPlan promoteToFormalPlan(@Name("scenarioVersionId") Long scenarioVersionId, IServiceContext context) {
        requireSimulationEnabled(scenarioVersionId);
        return simulationDrpEngine.promoteToFormalPlan(scenarioVersionId);
    }

    @Override
    @BizQuery
    public DrpSimulationDiffResult compareVersions(@Name("versionIdA") Long versionIdA,
                                                    @Name("versionIdB") Long versionIdB,
                                                    IServiceContext context) {
        return simulationComparator.compareDrpVersions(versionIdA, versionIdB);
    }

    private void requireSimulationEnabled(Long id) {
        boolean enabled = AppConfig.var(ErpDrpConstants.CONFIG_DRP_SIMULATION_ENABLED,
                ErpDrpConstants.DEFAULT_DRP_SIMULATION_ENABLED);
        if (!enabled) {
            throw new NopException(ErpDrpErrors.ERR_DRP_SIMULATION_DISABLED)
                    .param(ErpDrpErrors.ARG_SCENARIO_ID, id);
        }
    }
}
