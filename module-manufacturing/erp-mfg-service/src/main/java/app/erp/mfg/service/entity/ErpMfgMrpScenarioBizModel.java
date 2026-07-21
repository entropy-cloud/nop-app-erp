
package app.erp.mfg.service.entity;

import app.erp.mfg.biz.IErpMfgMrpScenarioBiz;
import app.erp.mfg.dao.dto.SimulationDiffResult;
import app.erp.mfg.dao.entity.ErpMfgMrpPlan;
import app.erp.mfg.dao.entity.ErpMfgMrpScenario;
import app.erp.mfg.dao.entity.ErpMfgMrpScenarioVersion;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.mfg.service.simulation.SimulationMrpEngine;
import app.erp.mfg.service.simulation.SimulationVersionComparator;
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
 * MRP 仿真场景 BizModel（plan 2026-07-22-1000-2 §仿真计算编排）。
 *
 * <p>薄委派层：{@link #runSimulation} / {@link #promoteToFormalPlan} 委派给 {@link SimulationMrpEngine}，
 * {@link #compareVersions} 委派给 {@link SimulationVersionComparator}
 * （{@code mrp.md §服务层} 范式：BizModel 只负责注解 + config-gate + 用户身份，编排逻辑在 helper 引擎）。
 */
@BizModel("ErpMfgMrpScenario")
public class ErpMfgMrpScenarioBizModel extends CrudBizModel<ErpMfgMrpScenario> implements IErpMfgMrpScenarioBiz {

    @Inject
    SimulationMrpEngine simulationMrpEngine;
    @Inject
    SimulationVersionComparator simulationComparator;

    public ErpMfgMrpScenarioBizModel() {
        setEntityName(ErpMfgMrpScenario.class.getName());
    }

    public void setSimulationMrpEngine(SimulationMrpEngine simulationMrpEngine) {
        this.simulationMrpEngine = simulationMrpEngine;
    }

    public void setSimulationComparator(SimulationVersionComparator simulationComparator) {
        this.simulationComparator = simulationComparator;
    }

    @Override
    @BizMutation
    public ErpMfgMrpScenarioVersion runSimulation(@Name("scenarioId") Long scenarioId, IServiceContext context) {
        requireSimulationEnabled(scenarioId);
        return simulationMrpEngine.runSimulation(scenarioId);
    }

    @Override
    @BizMutation
    public ErpMfgMrpPlan promoteToFormalPlan(@Name("scenarioVersionId") Long scenarioVersionId, IServiceContext context) {
        requireSimulationEnabled(scenarioVersionId);
        return simulationMrpEngine.promoteToFormalPlan(scenarioVersionId);
    }

    @Override
    @BizQuery
    public SimulationDiffResult compareVersions(@Name("versionIdA") Long versionIdA,
                                                 @Name("versionIdB") Long versionIdB,
                                                 IServiceContext context) {
        return simulationComparator.compareMrpVersions(versionIdA, versionIdB);
    }

    private void requireSimulationEnabled(Long id) {
        boolean enabled = AppConfig.var(ErpMfgConstants.CONFIG_MFG_SIMULATION_ENABLED,
                ErpMfgConstants.DEFAULT_MFG_SIMULATION_ENABLED);
        if (!enabled) {
            throw new NopException(ErpMfgErrors.ERR_MFG_SIMULATION_DISABLED)
                    .param(ErpMfgErrors.ARG_SCENARIO_ID, id);
        }
    }
}

