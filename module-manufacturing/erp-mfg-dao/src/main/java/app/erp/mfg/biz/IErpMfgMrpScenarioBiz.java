
package app.erp.mfg.biz;

import app.erp.mfg.dao.dto.SimulationDiffResult;
import app.erp.mfg.dao.entity.ErpMfgMrpPlan;
import app.erp.mfg.dao.entity.ErpMfgMrpScenario;
import app.erp.mfg.dao.entity.ErpMfgMrpScenarioVersion;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

public interface IErpMfgMrpScenarioBiz extends ICrudBiz<ErpMfgMrpScenario> {

    /**
     * 运行 MRP 仿真：基于场景参数变体覆盖生成新场景版本（COMPUTED plan 快照）。
     * config-gated：{@code erp-mfg.simulation-enabled} 默认 false。
     */
    @BizMutation
    ErpMfgMrpScenarioVersion runSimulation(@Name("scenarioId") Long scenarioId, IServiceContext context);

    /**
     * 转正式计划：从场景版本复制为新的 DRAFT {@link ErpMfgMrpPlan}。
     * config-gated：{@code erp-mfg.simulation-enabled} 默认 false。
     */
    @BizMutation
    ErpMfgMrpPlan promoteToFormalPlan(@Name("scenarioVersionId") Long scenarioVersionId, IServiceContext context);

    /**
     * 对比两仿真版本（Decision C 4 维 diff）。{@link BizQuery} 无 config-gate（只读不修改状态）。
     */
    @BizQuery
    SimulationDiffResult compareVersions(@Name("versionIdA") Long versionIdA,
                                          @Name("versionIdB") Long versionIdB,
                                          IServiceContext context);
}

