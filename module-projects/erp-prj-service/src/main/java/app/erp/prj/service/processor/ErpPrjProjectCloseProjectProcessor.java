package app.erp.prj.service.processor;

import app.erp.prj.biz.IErpPrjTaskBiz;
import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.service.ErpPrjConfigs;
import app.erp.prj.service.ErpPrjConstants;
import app.erp.prj.service.ErpPrjErrors;
import app.erp.prj.service.cost.ExpenseCostAggregator;
import app.erp.prj.service.cost.ProjectCostAggregator;
import app.erp.prj.service.statemachine.ErpPrjProjectStateMachine;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * ErpPrjProject closeProject per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 OPEN→COMPLETED 冻结编排：状态守卫 → 任务已结束校验（config-gated）→ 刷新实际成本/费用归集 → 置 COMPLETED。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpPrjProjectCloseProjectProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpPrjProjectCloseProjectProcessor.class);

    /** 未结束任务状态集合（task-status 字典非 DONE 值；任务取消走 useLogicalDelete，无 CANCELLED）。 */
    private static final List<String> UNFINISHED_TASK_STATUSES = Arrays.asList(
            ErpPrjConstants.TASK_STATUS_TODO,
            ErpPrjConstants.TASK_STATUS_IN_PROGRESS,
            ErpPrjConstants.TASK_STATUS_BLOCKED);

    @Inject
    IDaoProvider daoProvider;
    @Inject
    ProjectCostAggregator costAggregator;
    @Inject
    ExpenseCostAggregator expenseCostAggregator;
    @Inject
    IErpPrjTaskBiz taskBiz;
    @Inject
    ErpPrjProjectStateMachine stateMachine;

    public ErpPrjProject closeProject(Long projectId, IServiceContext context) {
        ErpPrjProject project = requireProject(projectId);
        String status = project.getStatus();
        try {
            stateMachine.assertCanClose(status);
        } catch (NopException e) {
            // 非法边（Bean 报告 common 层码）映射为领域 ERR_PROJECT_NOT_CLOSABLE + 项目上下文，common 码作 cause（契约 §7）
            throw new NopException(ErpPrjErrors.ERR_PROJECT_NOT_CLOSABLE, e)
                    .param(ErpPrjErrors.ARG_PROJECT_ID, projectId)
                    .param(ErpPrjErrors.ARG_CURRENT_STATUS, status);
        }
        // 动态守卫保留原位：关闭前校验任务已结束（config-gated STRICT/WARN，对齐 state-machine.md §迁移完整性 OPEN→COMPLETED）
        validateTasksFinished(projectId, context);
        // 关闭前刷新实际成本（保证关账数据完整，对齐 §4.3）
        costAggregator.refreshActualCost(projectId);
        // 关闭前刷新费用报销归集（config-gated，保证关账费用完整，对齐计划 Phase 3 Decision）
        if (ErpPrjConfigs.expenseAggregationEnabled()) {
            expenseCostAggregator.refreshExpenseCost(projectId);
        }
        project = projectDao().getEntityById(projectId);
        project.setStatus(stateMachine.closeTargetStatus());
        projectDao().updateEntity(project);
        return project;
    }

    /**
     * 校验项目下无未结束任务。STRICT 模式（默认）抛
     * {@code ERR_PROJECT_HAS_UNFINISHED_TASKS}；WARN 模式 {@code LOG.warn} 放行。
     *
     * <p>使用 {@code in {TODO, IN_PROGRESS, BLOCKED}} 计数未结束任务——task-status 字典仅
     * 这 4 态（无 CANCELLED，任务取消走 useLogicalDelete），故 in 未结束集等价于 not in {DONE}；
     * ErpPrjTask.status 的 XMeta 仅允许 in 不允许 notIn。
     */
    private void validateTasksFinished(Long projectId, IServiceContext context) {
        QueryBean query = new QueryBean();
        query.addFilter(eq("projectId", projectId));
        query.addFilter(in("status", UNFINISHED_TASK_STATUSES));
        long count = taskBiz.findCount(query, context);
        if (count <= 0) {
            return;
        }
        if (ErpPrjConfigs.strictProjectTaskCompletionCheck()) {
            throw new NopException(ErpPrjErrors.ERR_PROJECT_HAS_UNFINISHED_TASKS)
                    .param(ErpPrjErrors.ARG_PROJECT_ID, projectId)
                    .param(ErpPrjErrors.ARG_TASK_STATUSES, UNFINISHED_TASK_STATUSES);
        }
        LOG.warn("项目 {} 存在 {} 个未结束任务（状态={}，WARN 模式放行），state-machine.md §迁移完整性 OPEN→COMPLETED",
                projectId, count, UNFINISHED_TASK_STATUSES);
    }

    private ErpPrjProject requireProject(Long projectId) {
        ErpPrjProject project = projectDao().getEntityById(projectId);
        if (project == null) {
            throw new NopException(ErpPrjErrors.ERR_PROJECT_NOT_FOUND)
                    .param(ErpPrjErrors.ARG_PROJECT_ID, projectId);
        }
        return project;
    }

    private IEntityDao<ErpPrjProject> projectDao() {
        return daoProvider.daoFor(ErpPrjProject.class);
    }
}
