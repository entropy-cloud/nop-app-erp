package app.erp.prj.service.entity;

import app.erp.prj.biz.IErpPrjProjectBiz;
import app.erp.prj.biz.IErpPrjTaskBiz;
import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.service.ErpPrjConfigs;
import app.erp.prj.service.ErpPrjConstants;
import app.erp.prj.service.ErpPrjErrors;
import app.erp.prj.service.cost.ExpenseCostAggregator;
import app.erp.prj.service.cost.ProjectCostAggregator;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * 项目 BizModel。CRUD 之上承载项目状态引用校验（{@code cost-collection.md §七}）与
 * 成本归集回写（{@code §4.2}）。
 *
 * <p>{@code closeProject} 实现 OPEN→COMPLETED 冻结（对齐 §4.3「项目关闭」）；关闭前经
 * {@code validateTasksFinished} 校验任务已结束（config-gated STRICT/WARN，对齐
 * {@code state-machine.md §迁移完整性 OPEN→COMPLETED}）；关闭后
 * {@link #requireReferenceable} 拒绝新单据引用，从而拒绝新归集。
 *
 * <p>{@code startProject} 实现 DRAFT→OPEN 迁移；迁移前经 {@code validateStartPreconditions}
 * 校验必填字段（项目名/起止日期/预算，config-gated STRICT/WARN，对齐
 * {@code state-machine.md §迁移完整性 DRAFT→OPEN}）。
 */
@BizModel("ErpPrjProject")
public class ErpPrjProjectBizModel extends CrudBizModel<ErpPrjProject> implements IErpPrjProjectBiz {

    private static final Logger LOG = LoggerFactory.getLogger(ErpPrjProjectBizModel.class);

    /** 未结束任务状态集合（task-status 字典非 DONE 值；任务取消走 useLogicalDelete，无 CANCELLED）。 */
    private static final List<String> UNFINISHED_TASK_STATUSES = Arrays.asList(
            ErpPrjConstants.TASK_STATUS_TODO,
            ErpPrjConstants.TASK_STATUS_IN_PROGRESS,
            ErpPrjConstants.TASK_STATUS_BLOCKED);

    @Inject
    ProjectCostAggregator costAggregator;
    @Inject
    ExpenseCostAggregator expenseCostAggregator;
    @Inject
    IErpPrjTaskBiz taskBiz;

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
        // 关闭前校验任务已结束（config-gated STRICT/WARN，对齐 state-machine.md §迁移完整性 OPEN→COMPLETED）
        validateTasksFinished(projectId, context);
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

    @Override
    @BizMutation
    public ErpPrjProject startProject(@Name("projectId") Long projectId, IServiceContext context) {
        ErpPrjProject project = requireEntity(String.valueOf(projectId), null, context);
        // 立项前校验必填字段（config-gated STRICT/WARN，对齐 state-machine.md §迁移完整性 DRAFT→OPEN）
        validateStartPreconditions(project);
        String status = project.getStatus();
        if (!Objects.equals(status, ErpPrjConstants.PROJECT_STATUS_DRAFT)) {
            throw new NopException(ErpPrjErrors.ERR_PROJECT_NOT_CLOSABLE)
                    .param(ErpPrjErrors.ARG_PROJECT_ID, projectId)
                    .param(ErpPrjErrors.ARG_CURRENT_STATUS, status);
        }
        project.setStatus(ErpPrjConstants.PROJECT_STATUS_OPEN);
        updateEntity(project, null, context);
        return project;
    }

    @Override
    @BizMutation
    public ErpPrjProject holdProject(@Name("projectId") Long projectId, IServiceContext context) {
        return transition(projectId, ErpPrjConstants.PROJECT_STATUS_OPEN,
                ErpPrjConstants.PROJECT_STATUS_ON_HOLD, context);
    }

    @Override
    @BizMutation
    public ErpPrjProject resumeProject(@Name("projectId") Long projectId, IServiceContext context) {
        return transition(projectId, ErpPrjConstants.PROJECT_STATUS_ON_HOLD,
                ErpPrjConstants.PROJECT_STATUS_OPEN, context);
    }

    @Override
    @BizMutation
    public ErpPrjProject cancelProject(@Name("projectId") Long projectId, IServiceContext context) {
        ErpPrjProject project = requireEntity(String.valueOf(projectId), null, context);
        String status = project.getStatus();
        if (Objects.equals(status, ErpPrjConstants.PROJECT_STATUS_COMPLETED)
                || Objects.equals(status, ErpPrjConstants.PROJECT_STATUS_CANCELLED)) {
            throw new NopException(ErpPrjErrors.ERR_PROJECT_NOT_CLOSABLE)
                    .param(ErpPrjErrors.ARG_PROJECT_ID, projectId)
                    .param(ErpPrjErrors.ARG_CURRENT_STATUS, status);
        }
        project.setStatus(ErpPrjConstants.PROJECT_STATUS_CANCELLED);
        updateEntity(project, null, context);
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

    /**
     * 校验立项必填字段（项目名/起止日期/预算，且 startDate<=endDate）。STRICT 模式（默认）抛
     * {@code ERR_PROJECT_START_PRECONDITION_FAILED}；WARN 模式 {@code LOG.warn} 放行。
     */
    private void validateStartPreconditions(ErpPrjProject project) {
        List<String> missingFields = new ArrayList<>();
        if (project.getName() == null || project.getName().trim().isEmpty()) {
            missingFields.add("name");
        }
        LocalDate startDate = project.getStartDate();
        LocalDate endDate = project.getEndDate();
        if (startDate == null) {
            missingFields.add("startDate");
        }
        if (endDate == null) {
            missingFields.add("endDate");
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            missingFields.add("startDate>endDate");
        }
        if (project.getBudget() == null) {
            missingFields.add("budget");
        }
        if (missingFields.isEmpty()) {
            return;
        }
        if (ErpPrjConfigs.strictProjectStartPrecheck()) {
            throw new NopException(ErpPrjErrors.ERR_PROJECT_START_PRECONDITION_FAILED)
                    .param(ErpPrjErrors.ARG_PROJECT_ID, project.getId())
                    .param(ErpPrjErrors.ARG_MISSING_FIELDS, missingFields);
        }
        LOG.warn("项目 {} 立项前置校验失败：缺少必填字段 {}（WARN 模式放行），state-machine.md §迁移完整性 DRAFT→OPEN",
                project.getId(), missingFields);
    }

    private ErpPrjProject transition(Long projectId, String expected, String target, IServiceContext context) {
        ErpPrjProject project = requireEntity(String.valueOf(projectId), null, context);
        String status = project.getStatus();
        if (!Objects.equals(status, expected)) {
            throw new NopException(ErpPrjErrors.ERR_PROJECT_NOT_CLOSABLE)
                    .param(ErpPrjErrors.ARG_PROJECT_ID, projectId)
                    .param(ErpPrjErrors.ARG_CURRENT_STATUS, status);
        }
        project.setStatus(target);
        updateEntity(project, null, context);
        return project;
    }

}
