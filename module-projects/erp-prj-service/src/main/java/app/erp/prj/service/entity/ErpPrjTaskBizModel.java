package app.erp.prj.service.entity;

import app.erp.prj.biz.IErpPrjTaskBiz;
import app.erp.prj.dao.entity.ErpPrjTask;
import app.erp.prj.service.ErpPrjConfigs;
import app.erp.prj.service.ErpPrjConstants;
import app.erp.prj.service.ErpPrjErrors;
import app.erp.prj.service.validator.TaskDependencyValidator;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 任务 BizModel（{@code task-dag.md}）。CRUD 之上承载：
 * <ul>
 *   <li>依赖保存校验：经 {@link #defaultPrepareSave}/{@link #defaultPrepareUpdate} 钩子，
 *       自环/跨项目/成环/深度超限检测（委托 {@link TaskDependencyValidator}）。</li>
 *   <li>状态迁移：{@code startTask}/{@code completeTask}/{@code blockTask}/{@code unblockTask}。</li>
 *   <li>依赖查询：{@code findPredecessors}/{@code findSuccessors}/{@code getDependencyChain}。</li>
 * </ul>
 *
 * <p>{@code @BizMutation} 自动包装事务（不叠加 {@code @Transactional}）；每迁移校验前置态，违例抛
 * {@link NopException}+{@link ErpPrjErrors} 作用域码。
 */
@BizModel("ErpPrjTask")
public class ErpPrjTaskBizModel extends CrudBizModel<ErpPrjTask> implements IErpPrjTaskBiz {

    private static final Logger LOG = LoggerFactory.getLogger(ErpPrjTaskBizModel.class);

    public ErpPrjTaskBizModel() {
        setEntityName(ErpPrjTask.class.getName());
    }

    // ============ 依赖保存校验钩子（task-dag.md §2/§3） ============

    @Override
    protected void defaultPrepareSave(EntityData<ErpPrjTask> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        validateDependency(entityData.getEntity(), context);
    }

    @Override
    protected void defaultPrepareUpdate(EntityData<ErpPrjTask> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        validateDependency(entityData.getEntity(), context);
    }

    /**
     * 校验 entity.dependsOnId 合法性：自环 / 跨项目 / 成环 / 深度超限。
     * dependsOnId 为 null 时直接放行（无前置）。
     */
    private void validateDependency(ErpPrjTask task, IServiceContext context) {
        if (task == null || task.getDependsOnId() == null) {
            return;
        }
        Long taskId = task.getId();
        Long dependsOnId = task.getDependsOnId();

        if (taskId != null && taskId.equals(dependsOnId)) {
            throw new NopException(ErpPrjErrors.ERR_TASK_SELF_DEPENDENCY)
                    .param(ErpPrjErrors.ARG_TASK_ID, taskId);
        }

        ErpPrjTask dependsOnTask = loadTask(dependsOnId);
        if (dependsOnTask == null) {
            return;
        }

        if (!Objects.equals(task.getProjectId(), dependsOnTask.getProjectId())) {
            throw new NopException(ErpPrjErrors.ERR_TASK_DEPENDENCY_CROSS_PROJECT)
                    .param(ErpPrjErrors.ARG_TASK_ID, taskId)
                    .param(ErpPrjErrors.ARG_TASK_PROJECT_ID, task.getProjectId())
                    .param(ErpPrjErrors.ARG_DEPENDS_ON_TASK_ID, dependsOnId)
                    .param(ErpPrjErrors.ARG_DEPENDS_ON_PROJECT_ID, dependsOnTask.getProjectId());
        }

        int maxDepth = ErpPrjConfigs.taskDependencyMaxDepth();
        TaskDependencyValidator.detectCycle(taskId, dependsOnId, this::loadTask, maxDepth);
    }

    // ============ 状态迁移方法（task-dag.md §4） ============

    @Override
    @BizMutation
    public ErpPrjTask startTask(@Name("taskId") Long taskId, IServiceContext context) {
        ErpPrjTask task = requireEntity(String.valueOf(taskId), null, context);
        String status = task.getStatus();
        if (status == null || !Objects.equals(status, ErpPrjConstants.TASK_STATUS_TODO)) {
            throw illegalTransition(taskId, status, ErpPrjConstants.TASK_STATUS_IN_PROGRESS);
        }
        validatePredecessorDone(task);
        task.setStatus(ErpPrjConstants.TASK_STATUS_IN_PROGRESS);
        task.setActualStartDate(CoreMetrics.currentDate());
        updateEntity(task, null, context);
        return task;
    }

    @Override
    @BizMutation
    public ErpPrjTask completeTask(@Name("taskId") Long taskId, IServiceContext context) {
        ErpPrjTask task = requireEntity(String.valueOf(taskId), null, context);
        String status = task.getStatus();
        if (status == null || !Objects.equals(status, ErpPrjConstants.TASK_STATUS_IN_PROGRESS)) {
            throw illegalTransition(taskId, status, ErpPrjConstants.TASK_STATUS_DONE);
        }
        task.setStatus(ErpPrjConstants.TASK_STATUS_DONE);
        task.setActualEndDate(CoreMetrics.currentDate());
        updateEntity(task, null, context);
        return task;
    }

    @Override
    @BizMutation
    public ErpPrjTask blockTask(@Name("taskId") Long taskId,
                                @Name("blockReason") String blockReason,
                                IServiceContext context) {
        ErpPrjTask task = requireEntity(String.valueOf(taskId), null, context);
        String status = task.getStatus();
        if (status == null || !Objects.equals(status, ErpPrjConstants.TASK_STATUS_IN_PROGRESS)) {
            throw illegalTransition(taskId, status, ErpPrjConstants.TASK_STATUS_BLOCKED);
        }
        if (blockReason == null || blockReason.trim().isEmpty()) {
            throw new NopException(ErpPrjErrors.ERR_TASK_BLOCK_REASON_REQUIRED)
                    .param(ErpPrjErrors.ARG_TASK_ID, taskId);
        }
        task.setStatus(ErpPrjConstants.TASK_STATUS_BLOCKED);
        task.setBlockReason(blockReason);
        updateEntity(task, null, context);
        return task;
    }

    @Override
    @BizMutation
    public ErpPrjTask unblockTask(@Name("taskId") Long taskId, IServiceContext context) {
        ErpPrjTask task = requireEntity(String.valueOf(taskId), null, context);
        String status = task.getStatus();
        if (status == null || !Objects.equals(status, ErpPrjConstants.TASK_STATUS_BLOCKED)) {
            throw illegalTransition(taskId, status, ErpPrjConstants.TASK_STATUS_IN_PROGRESS);
        }
        task.setStatus(ErpPrjConstants.TASK_STATUS_IN_PROGRESS);
        task.setBlockReason(null);
        updateEntity(task, null, context);
        return task;
    }

    /**
     * 校验前置任务完成（task-dag.md §4.3）。STRICT 模式拦截，WARN 模式仅告警放行。
     */
    private void validatePredecessorDone(ErpPrjTask task) {
        Long dependsOnId = task.getDependsOnId();
        if (dependsOnId == null) {
            return;
        }
        ErpPrjTask predecessor = loadTask(dependsOnId);
        if (predecessor == null) {
            return;
        }
        String predecessorStatus = predecessor.getStatus();
        if (ErpPrjConstants.TASK_STATUS_DONE.equals(predecessorStatus)) {
            return;
        }
        if (ErpPrjConfigs.taskStrictPredecessorCheck()) {
            throw new NopException(ErpPrjErrors.ERR_TASK_PREDECESSOR_NOT_DONE)
                    .param(ErpPrjErrors.ARG_TASK_ID, task.getId())
                    .param(ErpPrjErrors.ARG_DEPENDS_ON_TASK_ID, dependsOnId)
                    .param(ErpPrjErrors.ARG_DEPENDS_ON_TASK_STATUS, predecessorStatus);
        }
        LOG.warn("任务 {} 的前置任务 {} 当前状态 {} 未完成（WARN 模式放行），task-dag.md §4.3",
                task.getId(), dependsOnId, predecessorStatus);
    }

    // ============ 依赖查询入口（task-dag.md §5） ============

    @Override
    @BizQuery
    public List<ErpPrjTask> findPredecessors(@Name("taskId") Long taskId, IServiceContext context) {
        if (taskId == null) {
            return new ArrayList<>();
        }
        int maxDepth = ErpPrjConfigs.taskDependencyMaxDepth();
        return TaskDependencyValidator.collectPredecessors(taskId, this::loadTask, maxDepth);
    }

    @Override
    @BizQuery
    public List<ErpPrjTask> findSuccessors(@Name("taskId") Long taskId, IServiceContext context) {
        List<ErpPrjTask> result = new ArrayList<>();
        if (taskId == null) {
            return result;
        }
        Set<Long> visited = new HashSet<>();
        visited.add(taskId);
        Queue<Long> queue = new ArrayDeque<>();
        queue.offer(taskId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            for (ErpPrjTask successor : findDirectSuccessors(current)) {
                if (visited.contains(successor.getId())) {
                    continue;
                }
                visited.add(successor.getId());
                result.add(successor);
                queue.offer(successor.getId());
            }
        }
        return result;
    }

    @Override
    @BizQuery
    public List<ErpPrjTask> getDependencyChain(@Name("taskId") Long taskId, IServiceContext context) {
        return findPredecessors(taskId, context);
    }

    /**
     * 下行直接后继（dependsOnId == taskId 的所有任务）。经 daoProvider 直接查询，
     * 用于 {@link #findSuccessors} 的 BFS 递归反查。
     */
    private List<ErpPrjTask> findDirectSuccessors(Long taskId) {
        IEntityDao<ErpPrjTask> dao = daoProvider().daoFor(ErpPrjTask.class);
        return dao.findAllByQuery(new io.nop.api.core.beans.query.QueryBean().addFilter(eq("dependsOnId", taskId)));
    }

    // ============ helpers ============

    private ErpPrjTask loadTask(Long taskId) {
        if (taskId == null) {
            return null;
        }
        IEntityDao<ErpPrjTask> dao = daoProvider().daoFor(ErpPrjTask.class);
        return dao.getEntityById(taskId);
    }

    private NopException illegalTransition(Long taskId, String current, String target) {
        return new NopException(ErpPrjErrors.ERR_TASK_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPrjErrors.ARG_TASK_ID, taskId)
                .param(ErpPrjErrors.ARG_CURRENT_STATUS, current)
                .param(ErpPrjErrors.ARG_TARGET_STATUS, target);
    }

    @BizLoader(forType = ErpPrjTask.class)
    public List<String> projectName(@ContextSource List<ErpPrjTask> tasks) {
        orm().batchLoadProps(tasks, Collections.singleton("project"));
        List<String> result = new ArrayList<>(tasks.size());
        for (ErpPrjTask task : tasks) {
            result.add(task.getProject() != null ? task.getProject().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPrjTask.class)
    public List<String> parentTaskName(@ContextSource List<ErpPrjTask> tasks) {
        orm().batchLoadProps(tasks, Collections.singleton("parentTask"));
        List<String> result = new ArrayList<>(tasks.size());
        for (ErpPrjTask task : tasks) {
            result.add(task.getParentTask() != null ? task.getParentTask().getTitle() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPrjTask.class)
    public List<String> assigneeName(@ContextSource List<ErpPrjTask> tasks) {
        orm().batchLoadProps(tasks, Collections.singleton("assignee"));
        List<String> result = new ArrayList<>(tasks.size());
        for (ErpPrjTask task : tasks) {
            result.add(task.getAssignee() != null ? task.getAssignee().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPrjTask.class)
    public List<String> dependsOnTaskName(@ContextSource List<ErpPrjTask> tasks) {
        orm().batchLoadProps(tasks, Collections.singleton("dependsOn"));
        List<String> result = new ArrayList<>(tasks.size());
        for (ErpPrjTask task : tasks) {
            result.add(task.getDependsOn() != null ? task.getDependsOn().getTitle() : null);
        }
        return result;
    }
}
