package app.erp.prj.biz;

import app.erp.prj.dao.entity.ErpPrjTask;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.util.List;

/**
 * 任务 Biz 契约（{@code task-dag.md}）。CRUD 之上承载：
 * <ul>
 *   <li>依赖保存校验（经 {@code defaultPrepareSave}/{@code defaultPrepareUpdate} 钩子，无需接口声明）。</li>
 *   <li>状态迁移：{@link #startTask}（TODO→IN_PROGRESS，前置须 DONE，config-gated STRICT/WARN）、
 *       {@link #completeTask}（IN_PROGRESS→DONE）、
 *       {@link #blockTask}（IN_PROGRESS→BLOCKED，blockReason 必填）、
 *       {@link #unblockTask}（BLOCKED→IN_PROGRESS）。</li>
 *   <li>依赖查询：{@link #findPredecessors}（上行链全量）、{@link #findSuccessors}（下行反查全量）、
 *       {@link #getDependencyChain}（单链全量，对齐单前置模型）。</li>
 * </ul>
 */
public interface IErpPrjTaskBiz extends ICrudBiz<ErpPrjTask> {

    /**
     * 启动任务（TODO→IN_PROGRESS）。前置任务（{@code dependsOn}）须 DONE；
     * 否则按 {@code erp-prj.task-strict-predecessor-check} 配置：STRICT 拦截 / WARN 放行。
     */
    @BizMutation
    ErpPrjTask startTask(@Name("taskId") Long taskId, IServiceContext context);

    /**
     * 完成任务（IN_PROGRESS→DONE）。
     */
    @BizMutation
    ErpPrjTask completeTask(@Name("taskId") Long taskId, IServiceContext context);

    /**
     * 阻塞任务（IN_PROGRESS→BLOCKED）。{@code blockReason} 必填。
     */
    @BizMutation
    ErpPrjTask blockTask(@Name("taskId") Long taskId,
                         @Name("blockReason") String blockReason,
                         IServiceContext context);

    /**
     * 解除阻塞（BLOCKED→IN_PROGRESS）。
     */
    @BizMutation
    ErpPrjTask unblockTask(@Name("taskId") Long taskId, IServiceContext context);

    /**
     * 上行链全量前置任务（前置 + 前置的前置 + ...）。
     */
    @BizQuery
    List<ErpPrjTask> findPredecessors(@Name("taskId") Long taskId, IServiceContext context);

    /**
     * 下行反查全量后继任务（所有直接/间接后继）。
     */
    @BizQuery
    List<ErpPrjTask> findSuccessors(@Name("taskId") Long taskId, IServiceContext context);

    /**
     * 单链全量（对齐单前置模型，至多一条线性链）。
     */
    @BizQuery
    List<ErpPrjTask> getDependencyChain(@Name("taskId") Long taskId, IServiceContext context);
}
