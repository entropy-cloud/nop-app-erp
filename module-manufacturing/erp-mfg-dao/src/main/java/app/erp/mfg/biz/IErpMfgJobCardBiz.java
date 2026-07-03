
package app.erp.mfg.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.RequestBean;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.mfg.dao.entity.ErpMfgJobCard;

/**
 * 作业卡业务接口。除标准 CRUD 外，定义作业卡 8 态状态机
 * （{@code docs/design/manufacturing/state-machine.md §适用对象二`}）+ 报工成本归集契约。
 *
 * <p>状态机方法：
 * <ul>
 *   <li>{@link #startJob}：OPEN→WORK_IN_PROGRESS。</li>
 *   <li>{@link #recordWork}：录入 JobCardTimeLog（durationMins/60 × hourlyRate → laborCost），回写 WorkOrder.laborCost。</li>
 *   <li>{@link #submitJob}：→SUBMITTED。</li>
 *   <li>{@link #completeJob}：→COMPLETED，回写 JobCard.completedQuantity。</li>
 *   <li>{@link #holdJob}：→ON_HOLD；{@link #resumeJob}：ON_HOLD→WORK_IN_PROGRESS。</li>
 *   <li>{@link #cancelJob}：→CANCELLED。</li>
 * </ul>
 *
 * <p>权威：{@code docs/design/manufacturing/state-machine.md}、
 * {@code docs/plans/2026-07-02-2237-1-manufacturing-workorder-jobcard-state-machine.md} Phase 4。
 */
public interface IErpMfgJobCardBiz extends ICrudBiz<ErpMfgJobCard> {

    @BizMutation
    ErpMfgJobCard startJob(@Name("jobCardId") Long jobCardId, IServiceContext context);

    @BizMutation
    ErpMfgJobCard recordWork(@RequestBean JobCardWorkRecord record, IServiceContext context);

    @BizMutation
    ErpMfgJobCard submitJob(@Name("jobCardId") Long jobCardId, IServiceContext context);

    @BizMutation
    ErpMfgJobCard completeJob(@Name("jobCardId") Long jobCardId, IServiceContext context);

    @BizMutation
    ErpMfgJobCard holdJob(@Name("jobCardId") Long jobCardId, IServiceContext context);

    @BizMutation
    ErpMfgJobCard resumeJob(@Name("jobCardId") Long jobCardId, IServiceContext context);

    @BizMutation
    ErpMfgJobCard cancelJob(@Name("jobCardId") Long jobCardId, IServiceContext context);
}
