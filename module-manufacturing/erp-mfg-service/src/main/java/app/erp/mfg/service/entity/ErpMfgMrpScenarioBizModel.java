
package app.erp.mfg.service.entity;

import app.erp.mfg.biz.IErpMfgMrpScenarioBiz;
import app.erp.mfg.dao.dto.SimulationDiffResult;
import app.erp.mfg.dao.entity.ErpMfgMrpPlan;
import app.erp.mfg.dao.entity.ErpMfgMrpScenario;
import app.erp.mfg.dao.entity.ErpMfgMrpScenarioVersion;
import app.erp.mfg.service.processor.ErpMfgMrpScenarioPromoteToFormalPlanProcessor;
import app.erp.mfg.service.processor.ErpMfgMrpScenarioRunSimulationProcessor;
import app.erp.mfg.service.simulation.SimulationVersionComparator;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * MRP 仿真场景 BizModel（plan 2026-07-22-1000-2 §仿真计算编排）。
 *
 * <p>{@code runSimulation}/{@code promoteToFormalPlan}（@BizMutation）各委托独立 per-mutation Processor
 *（R6.2 拆分，config-gate + 委托 SimulationMrpEngine）；{@code compareVersions}（@BizQuery）保留委托
 * {@link SimulationVersionComparator}。
 */
@BizModel("ErpMfgMrpScenario")
public class ErpMfgMrpScenarioBizModel extends CrudBizModel<ErpMfgMrpScenario> implements IErpMfgMrpScenarioBiz {

    @Inject
    SimulationVersionComparator simulationComparator;
    @Inject
    ErpMfgMrpScenarioRunSimulationProcessor runSimulationProcessor;
    @Inject
    ErpMfgMrpScenarioPromoteToFormalPlanProcessor promoteToFormalPlanProcessor;

    public ErpMfgMrpScenarioBizModel() {
        setEntityName(ErpMfgMrpScenario.class.getName());
    }

    public void setSimulationComparator(SimulationVersionComparator simulationComparator) {
        this.simulationComparator = simulationComparator;
    }

    @Override
    @BizMutation
    public ErpMfgMrpScenarioVersion runSimulation(@Name("scenarioId") Long scenarioId, IServiceContext context) {
        return runSimulationProcessor.runSimulation(scenarioId, context);
    }

    @Override
    @BizMutation
    public ErpMfgMrpPlan promoteToFormalPlan(@Name("scenarioVersionId") Long scenarioVersionId, IServiceContext context) {
        return promoteToFormalPlanProcessor.promoteToFormalPlan(scenarioVersionId, context);
    }

    @Override
    @BizQuery
    public SimulationDiffResult compareVersions(@Name("versionIdA") Long versionIdA,
                                                 @Name("versionIdB") Long versionIdB,
                                                 IServiceContext context) {
        return simulationComparator.compareMrpVersions(versionIdA, versionIdB);
    }
}
