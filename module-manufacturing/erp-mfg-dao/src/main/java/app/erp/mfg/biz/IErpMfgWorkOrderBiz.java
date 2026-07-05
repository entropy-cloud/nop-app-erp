
package app.erp.mfg.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.mfg.dao.entity.ErpMfgWorkOrder;

import java.util.List;

/**
 * 工单业务接口。除标准 CRUD 外，定义工单 10 态状态机（{@code docs/design/manufacturing/state-machine.md §适用对象一`}）
 * + 三轴审批（提交→审核→NOT_STARTED）+ 齐套校验（STOCK_RESERVED / STOCK_PARTIAL）契约。
 *
 * <p>状态机方法（{@link BizMutation}，自动事务包装）：
 * <ul>
 *   <li>{@link #submit}：DRAFT→SUBMITTED（提交轴）。</li>
 *   <li>{@link #approve}：SUBMITTED→NOT_STARTED（审核轴，置 approveStatus=APPROVED）。</li>
 *   <li>{@link #checkAvailability}：NOT_STARTED→STOCK_RESERVED（全齐）/ STOCK_PARTIAL（部分齐套）。</li>
 *   <li>{@link #start}：STOCK_RESERVED / STOCK_PARTIAL→IN_PROCESS（部分齐套须配置允许）。</li>
 *   <li>{@link #stop}：IN_PROCESS→STOPPED。</li>
 *   <li>{@link #resume}：STOPPED→IN_PROCESS。</li>
 *   <li>{@link #close}：STOPPED / IN_PROCESS→CLOSED（部分完工结转）。</li>
 *   <li>{@link #cancel}：DRAFT / SUBMITTED / NOT_STARTED→CANCELLED。</li>
 *   <li>{@link #reportCompletion}：IN_PROCESS→COMPLETED（完工达量；质检门控见 owner doc）。</li>
 * </ul>
 *
 * <p>非法迁移抛 {@code ErpMfgErrors.ERR_INVALID_STATUS_TRANSITION}。权威状态机见
 * {@code docs/design/manufacturing/state-machine.md}；计划见
 * {@code docs/plans/2026-07-02-2237-1-manufacturing-workorder-jobcard-state-machine.md}。
 */
public interface IErpMfgWorkOrderBiz extends ICrudBiz<ErpMfgWorkOrder> {

    @BizMutation
    ErpMfgWorkOrder submit(@Name("workOrderId") Long workOrderId, IServiceContext context);

    @BizMutation
    ErpMfgWorkOrder approve(@Name("workOrderId") Long workOrderId, IServiceContext context);

    @BizMutation
    ErpMfgWorkOrder checkAvailability(@Name("workOrderId") Long workOrderId, IServiceContext context);

    @BizMutation
    ErpMfgWorkOrder start(@Name("workOrderId") Long workOrderId, IServiceContext context);

    @BizMutation
    ErpMfgWorkOrder stop(@Name("workOrderId") Long workOrderId, IServiceContext context);

    @BizMutation
    ErpMfgWorkOrder resume(@Name("workOrderId") Long workOrderId, IServiceContext context);

    @BizMutation
    ErpMfgWorkOrder close(@Name("workOrderId") Long workOrderId, IServiceContext context);

    @BizMutation
    ErpMfgWorkOrder cancel(@Name("workOrderId") Long workOrderId, IServiceContext context);

    @BizMutation
    ErpMfgWorkOrder reportCompletion(@Name("workOrderId") Long workOrderId,
                                     @Name("completedQty") java.math.BigDecimal completedQty,
                                     IServiceContext context);

    /**
     * 按 APS 排程生成工序卡（plan 2026-07-05-0427-3 §Goals）。
     *
     * <p>读取该工单关联的、已排程（{@code ErpApsOperationOrder.status=PLANNED}，{@code plannedStartT/plannedEndT} 非空）
     * 的工序，按工序生成 JobCard（一工序一卡），JobCard 计划开工/完工时间 = 对应 OperationOrder 排程时间，
     * 回写 {@code JobCard.sourceScheduleId} + {@code WorkOrder.sourceOrderType=APS_SCHEDULE}。
     *
     * <p>幂等：默认重复调用抛 {@code ERR_JOB_CARDS_ALREADY_GENERATED}；
     * {@code erp-mfg.jobcard-incremental-rebuild=true} 时仅补建缺失工序卡（已存在不重建不删）。
     * 工单状态门控：须为已审核且非终态（NOT_STARTED/STOCK_RESERVED/STOCK_PARTIAL/IN_PROCESS/STOPPED）。
     */
    @BizMutation
    ErpMfgWorkOrder generateJobCardsFromSchedule(@Name("workOrderId") Long workOrderId, IServiceContext context);

    /**
     * 查询「已排程但未生成 JobCard」的工单列表（plan 2026-07-05-0427-3 Phase 3，config-gated 自动入口前置查询）。
     *
     * <p>返回 APS 已排程（存在 PLANNED 工序）但 JobCard 数为 0 的工单，供 {@link #generatePendingJobCards} 批量建卡。
     *
     * @param limit 返回条数上限，null/<=0 时取默认 100。
     */
    @BizQuery
    List<ErpMfgWorkOrder> findWorkOrdersPendingJobCards(@Optional @Name("limit") Integer limit,
                                                        IServiceContext context);

    /**
     * 批量为「已排程但未生成 JobCard」的工单生成工序卡（config-gated：{@code erp-mfg.jobcard-auto-generate-on-schedule}）。
     *
     * <p>由 nop-job 定时调用（参照 0306-1 三件套范式），单工单失败隔离（继续处理后续工单）。
     *
     * @return 成功生成工序卡的工单数。
     */
    @BizMutation
    Integer generatePendingJobCards(IServiceContext context);
}
