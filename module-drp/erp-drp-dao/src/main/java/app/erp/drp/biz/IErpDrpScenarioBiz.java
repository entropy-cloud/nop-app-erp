
package app.erp.drp.biz;

import app.erp.drp.dao.dto.DrpSimulationDiffResult;
import app.erp.drp.dao.entity.ErpDrpPlan;
import app.erp.drp.dao.entity.ErpDrpScenario;
import app.erp.drp.dao.entity.ErpDrpScenarioVersion;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

public interface IErpDrpScenarioBiz extends ICrudBiz<ErpDrpScenario> {

    /**
     * 运行 DRP 仿真：基于场景参数变体覆盖生成新场景版本（COMPUTED plan 快照）。
     * config-gated：{@code erp-drp.simulation-enabled} 默认 false。
     */
    @BizMutation
    ErpDrpScenarioVersion runSimulation(@Name("scenarioId") Long scenarioId, IServiceContext context);

    /**
     * 转正式计划：从场景版本复制为新的 DRAFT {@link ErpDrpPlan}。
     */
    @BizMutation
    ErpDrpPlan promoteToFormalPlan(@Name("scenarioVersionId") Long scenarioVersionId, IServiceContext context);

    /**
     * 对比两仿真版本（Decision C 2 维 diff：补货量差 / 安全库存差）。
     */
    @BizQuery
    DrpSimulationDiffResult compareVersions(@Name("versionIdA") Long versionIdA,
                                             @Name("versionIdB") Long versionIdB,
                                             IServiceContext context);
}
