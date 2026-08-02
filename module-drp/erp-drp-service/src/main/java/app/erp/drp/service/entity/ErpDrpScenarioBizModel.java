
package app.erp.drp.service.entity;

import app.erp.drp.biz.IErpDrpScenarioBiz;
import app.erp.drp.dao.dto.DrpSimulationDiffResult;
import app.erp.drp.dao.entity.ErpDrpPlan;
import app.erp.drp.dao.entity.ErpDrpScenario;
import app.erp.drp.dao.entity.ErpDrpScenarioVersion;
import app.erp.drp.service.processor.ErpDrpScenarioPromoteToFormalPlanProcessor;
import app.erp.drp.service.processor.ErpDrpScenarioRunSimulationProcessor;
import app.erp.drp.service.simulation.DrpSimulationVersionComparator;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * DRP 仿真场景 BizModel（plan 2026-07-22-1000-2 §DRP 对应物）。
 *
 * <p>薄委派层（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor），同构 MRP {@code ErpMfgMrpScenarioBizModel}：
 * {@link #runSimulation}/{@link #promoteToFormalPlan} 各委派独立自包含 Processor；{@link #compareVersions} 为只读查询保留委派 {@link DrpSimulationVersionComparator}。
 */
@BizModel("ErpDrpScenario")
public class ErpDrpScenarioBizModel extends CrudBizModel<ErpDrpScenario> implements IErpDrpScenarioBiz {

    @Inject
    DrpSimulationVersionComparator simulationComparator;
    @Inject
    ErpDrpScenarioRunSimulationProcessor runSimulationProcessor;
    @Inject
    ErpDrpScenarioPromoteToFormalPlanProcessor promoteToFormalPlanProcessor;

    public ErpDrpScenarioBizModel() {
        setEntityName(ErpDrpScenario.class.getName());
    }

    @Override
    @BizMutation
    public ErpDrpScenarioVersion runSimulation(@Name("scenarioId") Long scenarioId, IServiceContext context) {
        return runSimulationProcessor.runSimulation(scenarioId, context);
    }

    @Override
    @BizMutation
    public ErpDrpPlan promoteToFormalPlan(@Name("scenarioVersionId") Long scenarioVersionId, IServiceContext context) {
        return promoteToFormalPlanProcessor.promoteToFormalPlan(scenarioVersionId, context);
    }

    @Override
    @BizQuery
    public DrpSimulationDiffResult compareVersions(@Name("versionIdA") Long versionIdA,
                                                    @Name("versionIdB") Long versionIdB,
                                                    IServiceContext context) {
        return simulationComparator.compareDrpVersions(versionIdA, versionIdB);
    }
}
