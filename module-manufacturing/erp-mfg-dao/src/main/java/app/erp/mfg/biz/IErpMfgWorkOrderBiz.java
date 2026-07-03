
package app.erp.mfg.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.mfg.dao.entity.ErpMfgWorkOrder;

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
}
