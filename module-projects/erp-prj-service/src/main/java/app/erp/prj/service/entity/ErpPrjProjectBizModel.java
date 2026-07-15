package app.erp.prj.service.entity;

import app.erp.prj.biz.IErpPrjProjectBiz;
import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.service.ErpPrjConfigs;
import app.erp.prj.service.ErpPrjConstants;
import app.erp.prj.service.ErpPrjErrors;
import app.erp.prj.service.cost.ExpenseCostAggregator;
import app.erp.prj.service.cost.ProjectCostAggregator;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Objects;

import java.math.BigDecimal;

/**
 * 项目 BizModel。CRUD 之上承载项目状态引用校验（{@code cost-collection.md §七}）与
 * 成本归集回写（{@code §4.2}）。
 *
 * <p>{@code closeProject} 实现 OPEN→COMPLETED 冻结（对齐 §4.3「项目关闭」）；关闭后
 * {@link #requireReferenceable} 拒绝新单据引用，从而拒绝新归集。
 */
@BizModel("ErpPrjProject")
public class ErpPrjProjectBizModel extends CrudBizModel<ErpPrjProject> implements IErpPrjProjectBiz {

    @Inject
    ProjectCostAggregator costAggregator;
    @Inject
    ExpenseCostAggregator expenseCostAggregator;

    public ErpPrjProjectBizModel() {
        setEntityName(ErpPrjProject.class.getName());
    }

    @Override
    @BizMutation
    public ErpPrjProject requireReferenceable(@Name("projectId") Long projectId, IServiceContext context) {
        ErpPrjProject project = requireEntity(String.valueOf(projectId), null, context);
        String status = project.getStatus();
        if (status == null || !Objects.equals(status, ErpPrjConstants.PROJECT_STATUS_OPEN)) {
            throw new NopException(ErpPrjErrors.ERR_PROJECT_NOT_REFERENCEABLE)
                    .param(ErpPrjErrors.ARG_PROJECT_ID, projectId)
                    .param(ErpPrjErrors.ARG_CURRENT_STATUS, status);
        }
        return project;
    }

    @Override
    @BizMutation
    public BigDecimal refreshActualCost(@Name("projectId") Long projectId, IServiceContext context) {
        return costAggregator.refreshActualCost(projectId);
    }

    @Override
    @BizMutation
    public ErpPrjProject closeProject(@Name("projectId") Long projectId, IServiceContext context) {
        ErpPrjProject project = requireEntity(String.valueOf(projectId), null, context);
        String status = project.getStatus();
        if (status == null || !Objects.equals(status, ErpPrjConstants.PROJECT_STATUS_OPEN)) {
            throw new NopException(ErpPrjErrors.ERR_PROJECT_NOT_CLOSABLE)
                    .param(ErpPrjErrors.ARG_PROJECT_ID, projectId)
                    .param(ErpPrjErrors.ARG_CURRENT_STATUS, status);
        }
        // 关闭前刷新实际成本（保证关账数据完整，对齐 §4.3）
        costAggregator.refreshActualCost(projectId);
        // 关闭前刷新费用报销归集（config-gated，保证关账费用完整，对齐计划 Phase 3 Decision）
        if (ErpPrjConfigs.expenseAggregationEnabled()) {
            expenseCostAggregator.refreshExpenseCost(projectId);
        }
        project = requireEntity(String.valueOf(projectId), null, context);
        project.setStatus(ErpPrjConstants.PROJECT_STATUS_COMPLETED);
        updateEntity(project, null, context);
        return project;
    }

}
