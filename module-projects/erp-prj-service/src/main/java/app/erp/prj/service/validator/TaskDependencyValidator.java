package app.erp.prj.service.validator;

import app.erp.prj.dao.entity.ErpPrjTask;
import app.erp.prj.service.ErpPrjConfigs;
import app.erp.prj.service.ErpPrjConstants;
import app.erp.prj.service.ErpPrjErrors;
import io.nop.api.core.exceptions.NopException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * 任务依赖校验工具（{@code task-dag.md §2 成环检测算法}）。纯函数式 —— 接收一个
 * {@code loader}（taskId -> ErpPrjTask），便于在 BizModel 中传入 dao 加载函数、
 * 在单元测试中传入 mock 加载函数。
 *
 * <p>本工具覆盖单前置依赖模型（{@code ErpPrjTask.dependsOnId} 单列）：
 * <ul>
 *   <li>{@link #detectCycle} —— 上行链追溯 + HashSet revisit 检测 + 深度上限兜底。</li>
 *   <li>{@link #collectPredecessors} —— 上行链全量收集（前置 + 前置的前置 + ...）。</li>
 * </ul>
 *
 * <p>所有方法不持锁、不开事务；调用方（BizModel 钩子）负责在合适的事务/会话上下文中调用。
 */
public final class TaskDependencyValidator {

    private TaskDependencyValidator() {
    }

    /**
     * 校验 {@code taskId} 设置 {@code dependsOnId} 后的依赖链合法性。
     *
     * @param taskId       当前任务 ID（保存/更新场景下 entity.id）
     * @param dependsOnId  拟设置的依赖任务 ID（entity.dependsOnId）
     * @param loader       加载函数（taskId -> ErpPrjTask，返回 null 表示不存在）
     * @param maxDepth     上行链深度上限（来自 {@link ErpPrjConfigs#taskDependencyMaxDepth()}）
     * @throws NopException 自环（{@link ErpPrjErrors#ERR_TASK_SELF_DEPENDENCY}）/
     *                      成环（{@link ErpPrjErrors#ERR_TASK_DEPENDENCY_CYCLE}）/
     *                      深度超限（{@link ErpPrjErrors#ERR_TASK_DEPENDENCY_DEPTH_EXCEEDED}）
     */
    public static void detectCycle(Long taskId, Long dependsOnId,
                                   Function<Long, ErpPrjTask> loader, int maxDepth) {
        if (dependsOnId == null) {
            return;
        }
        if (taskId != null && taskId.equals(dependsOnId)) {
            throw new NopException(ErpPrjErrors.ERR_TASK_SELF_DEPENDENCY)
                    .param(ErpPrjErrors.ARG_TASK_ID, taskId);
        }

        Set<Long> visited = new HashSet<>();
        List<Long> chainOrder = new ArrayList<>();
        if (taskId != null) {
            visited.add(taskId);
            chainOrder.add(taskId);
        }

        Long cursor = dependsOnId;
        int depth = 0;
        while (cursor != null) {
            depth++;
            if (depth > maxDepth) {
                throw new NopException(ErpPrjErrors.ERR_TASK_DEPENDENCY_DEPTH_EXCEEDED)
                        .param(ErpPrjErrors.ARG_TASK_ID, taskId)
                        .param(ErpPrjErrors.ARG_MAX_DEPTH, maxDepth)
                        .param(ErpPrjErrors.ARG_ACTUAL_DEPTH, depth);
            }
            if (visited.contains(cursor)) {
                chainOrder.add(cursor);
                throw new NopException(ErpPrjErrors.ERR_TASK_DEPENDENCY_CYCLE)
                        .param(ErpPrjErrors.ARG_TASK_ID, taskId)
                        .param(ErpPrjErrors.ARG_CHAIN, formatChain(chainOrder));
            }
            visited.add(cursor);
            chainOrder.add(cursor);

            ErpPrjTask predecessor = loader.apply(cursor);
            if (predecessor == null) {
                break;
            }
            cursor = predecessor.getDependsOnId();
        }
    }

    /**
     * 收集 {@code taskId} 的上行链全量前置任务（不含 {@code taskId} 自身）。
     * 上行链顺序：直接前置在前，间接前置在后。
     *
     * @param taskId   起点任务 ID
     * @param loader   加载函数
     * @param maxDepth 深度上限（与 {@link #detectCycle} 一致，防恶意长链）
     * @return 上行链全量列表（{@code taskId} 无前置时返回空列表）
     */
    public static List<ErpPrjTask> collectPredecessors(Long taskId,
                                                       Function<Long, ErpPrjTask> loader,
                                                       int maxDepth) {
        List<ErpPrjTask> result = new ArrayList<>();
        if (taskId == null) {
            return result;
        }
        ErpPrjTask start = loader.apply(taskId);
        if (start == null) {
            return result;
        }
        Set<Long> visited = new HashSet<>();
        visited.add(taskId);

        Long cursor = start.getDependsOnId();
        int depth = 0;
        while (cursor != null) {
            depth++;
            if (depth > maxDepth) {
                throw new NopException(ErpPrjErrors.ERR_TASK_DEPENDENCY_DEPTH_EXCEEDED)
                        .param(ErpPrjErrors.ARG_TASK_ID, taskId)
                        .param(ErpPrjErrors.ARG_MAX_DEPTH, maxDepth)
                        .param(ErpPrjErrors.ARG_ACTUAL_DEPTH, depth);
            }
            if (visited.contains(cursor)) {
                throw new NopException(ErpPrjErrors.ERR_TASK_DEPENDENCY_CYCLE)
                        .param(ErpPrjErrors.ARG_TASK_ID, taskId)
                        .param(ErpPrjErrors.ARG_CHAIN, formatChainWith(result, cursor));
            }
            visited.add(cursor);
            ErpPrjTask predecessor = loader.apply(cursor);
            if (predecessor == null) {
                break;
            }
            result.add(predecessor);
            cursor = predecessor.getDependsOnId();
        }
        return result;
    }

    private static String formatChain(List<Long> chainOrder) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chainOrder.size(); i++) {
            if (i > 0) {
                sb.append("→");
            }
            sb.append(chainOrder.get(i));
        }
        return sb.toString();
    }

    private static String formatChainWith(List<ErpPrjTask> collected, Long cycleNode) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (ErpPrjTask t : collected) {
            if (!first) {
                sb.append("→");
            }
            sb.append(t.getId());
            first = false;
        }
        if (!first) {
            sb.append("→");
        }
        sb.append(cycleNode);
        return sb.toString();
    }

    /**
     * 便捷入口：当前任务状态是否允许录入工时（TODO/IN_PROGRESS 允许，BLOCKED/DONE 拒绝）。
     */
    public static boolean acceptsTimesheet(String status) {
        return ErpPrjConstants.TASK_STATUS_TODO.equals(status)
                || ErpPrjConstants.TASK_STATUS_IN_PROGRESS.equals(status);
    }
}
