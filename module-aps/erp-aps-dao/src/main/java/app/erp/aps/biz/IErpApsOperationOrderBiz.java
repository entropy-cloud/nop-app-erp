
package app.erp.aps.biz;

import app.erp.aps.dao.entity.ErpApsOperationOrder;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;

public interface IErpApsOperationOrderBiz extends ICrudBiz<ErpApsOperationOrder>{

    /**
     * UC-APS-01 工序工单批量创建：读 WorkOrder 绑定工艺路线工序列表，按 sequence 依次创建
     * OperationOrder(DRAFT)，继承 workOrderId/operationName/machineId/setupTime/runtimePerUnit/qty
     * 并计算 totalDuration。工艺路线缺失→整单跳过 + notify 告警；工作中心不存在→该工序拒绝创建 + 告警；
     * 幂等守卫：同 WorkOrder 重复触发不重复建单。计划员手动触发入口（与 job 拉取扫描同源编排）。
     */
    @BizMutation
    WorkOrderOperationCreationResult createOperationOrdersFromWorkOrder(@Name("workOrderId") String workOrderId,
                                                                        IServiceContext context);

    /**
     * UC-APS-01 自动触发（D1 裁决选项 B：aps 侧拉取扫描，R1.76 拉取消费先例）：扫描已下达
     * （已审核 NOT_STARTED 及其后续未开工/执行态）且尚无 OperationOrder 的工单，逐单调
     * {@link #createOperationOrdersFromWorkOrder}。返回本次新建 OperationOrder 总数。
     */
    @BizMutation
    Integer scanReleasedWorkOrders(IServiceContext context);

    /**
     * UC-APS-06 manualOverrideRouting：计划员强制指定路由（覆盖自动选择 + manualOverride=true +
     * remark 审计）。时间差幂等叠加（剥离上次选中路由差值），工序回退 DRAFT 并释放产能预留，
     * 下次重排在新工作中心排程且跳过自动路由选择。
     */
    @BizMutation
    ErpApsOperationOrder manualOverrideRouting(@Name("operationOrderId") String operationOrderId,
                                               @Name("routingId") String routingId,
                                               IServiceContext context);

    /**
     * UC-APS-07 自动派工扫描（RC-R1.88，D1 拉取模型同构入口）：逐工作中心按 DispatchRule 过滤
     * eligible PLANNED 工序（前瞻窗口/提前派工/优先级阈值/maxConcurrentOps），三维度条件
     * （物料齐套/操作工/工装）全满足→IN_PROGRESS + DispatchLog(AUTO)；窗口内缺料→ON_HOLD + 通知计划员。
     * 受全局开关 {@code erp-aps.auto-dispatch-enabled} 门控。返回本次派工数。job bean 调用入口，亦可手动执行。
     */
    @BizMutation
    Integer scanAutoDispatch(IServiceContext context);

    /**
     * UC-APS-07 手动强制派工：PLANNED→IN_PROGRESS，可跳过条件检查但跳检原因（note）必填，
     * DispatchLog dispatchType=MANUAL。
     */
    @BizMutation
    ErpApsOperationOrder dispatchManually(@Name("operationOrderId") String operationOrderId,
                                          @Name("note") String note,
                                          IServiceContext context);

    /**
     * UC-APS-07 派工保持：PLANNED→HOLD（计划员暂不派工，自动派工扫描跳过），DispatchLog dispatchType=HOLD。
     */
    @BizMutation
    ErpApsOperationOrder hold(@Name("operationOrderId") String operationOrderId, IServiceContext context);

    /**
     * UC-APS-07 解除保持：HOLD/ON_HOLD→PLANNED（重新进入自动派工检查循环），DispatchLog dispatchType=UNHOLD。
     */
    @BizMutation
    ErpApsOperationOrder unhold(@Name("operationOrderId") String operationOrderId, IServiceContext context);

    /**
     * 前向排产：按 ErpApsSchedule.horizonStart/horizonEnd 拉取 DRAFT 工序，
     * 从 earliestStartDateT 正向填充工作中心可用时段，写回 plannedStart/EndDateT 并置 PLANNED。
     */
    @BizMutation
    SchedulingResult scheduleForward(@Name("scheduleId") String scheduleId, IServiceContext context);

    /**
     * 后向排产：从 latestEndDateT 逆向倒推每工序最晚开工；交期不可达标记冲突。
     */
    @BizMutation
    SchedulingResult scheduleBackward(@Name("scheduleId") String scheduleId, IServiceContext context);

    /**
     * F11 批量前向排产（plan 2026-07-22-0444-2 Phase 2）：循环调单条 {@link #scheduleForward}，
     * 逐行执行（模式 b：行级失败不阻塞其他行），返回 {@link BatchOperationResult} 含成功数 + 失败明细。
     *
     * <p>{@code ids} 为 {@code ErpApsSchedule.id} 列表（与单条 {@code scheduleId} 同语义）。
     */
    @BizMutation
    BatchOperationResult batchScheduleForward(@Name("ids") Collection<String> ids, IServiceContext context);

    /**
     * 插单区间重排：急单窗口内优先级低于新单的 PLANNED 工序回退 DRAFT 重排，
     * IN_PROGRESS 永不回退，窗口外工序不受影响（{@code scheduling.md §六}）。
     */
    @BizMutation
    SchedulingResult insertRushOrder(@Name("operationOrderId") String operationOrderId, IServiceContext context);

    /**
     * 最早可交付日期（ATP/CTP）：ATP 充足立即承诺，否则影子模拟最早完工。
     */
    @BizQuery
    LocalDateTime earliestCompletionDate(@Name("materialId") String materialId, @Name("qty") BigDecimal qty);

    /**
     * 期望交期可行性检查（CTP）：返回 {@link CtpResult}。
     */
    @BizQuery
    CtpResult checkFeasibility(@Name("materialId") String materialId,
                               @Name("qty") BigDecimal qty,
                               @Name("desiredDate") LocalDateTime desiredDate);

    /**
     * 启动工序工单：PLANNED→IN_PROGRESS。
     */
    @BizMutation
    ErpApsOperationOrder start(@Name("operationOrderId") String operationOrderId, IServiceContext context);

    /**
     * 完成工序工单：IN_PROGRESS→FINISHED。
     */
    @BizMutation
    ErpApsOperationOrder complete(@Name("operationOrderId") String operationOrderId, IServiceContext context);

    /**
     * 作废工序工单：DRAFT/PLANNED/IN_PROGRESS→CANCELLED。
     */
    @BizMutation
    ErpApsOperationOrder cancel(@Name("operationOrderId") String operationOrderId, IServiceContext context);

    /**
     * 甘特图聚合查询（plan 2026-08-03-1232-3 Phase 0）：返回 flux gantt 契约
     * {@code {tasks:[{id,text,start,end,type,progress,machineId,workOrderId,sequence,status}], links:[{source,target,type,lag}]}}。
     * links 按 Phase 0 裁决=方案 A（同 workOrderId 按 sequence 相邻连边，FS type=0）。
     */
    @BizQuery
    Map<String, Object> findGanttData(@Optional @Name("machineId") String machineId,
                                      @Optional @Name("status") String status,
                                      IServiceContext context);

    /**
     * 甘特图拖拽持久化（plan 2026-08-03-1232-3 Phase 0 裁决）：写 plannedStartDateT/plannedEndDateT。
     * 不内联产能校验（裁决：产能由排程引擎在下次运行暴露，见 plan Decision）。
     */
    @BizMutation
    ErpApsOperationOrder updateSchedule(@Name("opOrderId") String opOrderId,
                                        @Name("start") LocalDateTime start,
                                        @Name("end") LocalDateTime end,
                                        IServiceContext context);
}
