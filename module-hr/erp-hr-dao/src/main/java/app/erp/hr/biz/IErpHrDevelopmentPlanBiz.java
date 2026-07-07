package app.erp.hr.biz;

import app.erp.hr.dao.entity.ErpHrDevelopmentPlan;
import app.erp.hr.dao.entity.ErpHrDevelopmentPlanItem;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

/**
 * 发展计划聚合根 Biz（competency-management.md §发展计划）。CRUD 之上承载：
 * <ul>
 *   <li>{@link #generateDevelopmentPlan} 针对 CRITICAL/MODERATE 差距按 weight/isCritical 排序
 *       自动生成建议计划项（DRAFT→IN_PROGRESS 自动推进）。</li>
 *   <li>{@link #updatePlanItemStatus} 计划项状态机（NOT_STARTED→IN_PROGRESS→ACHIEVED/OVERDUE）。</li>
 *   <li>{@link #completePlan} 计划状态机（DRAFT/IN_PROGRESS→COMPLETED）。</li>
 * </ul>
 *
 * <p>非法迁移抛 {@link app.erp.hr.service.ErpHrErrors#ERR_DEV_PLAN_ILLEGAL_STATUS_TRANSITION}。
 */
public interface IErpHrDevelopmentPlanBiz extends ICrudBiz<ErpHrDevelopmentPlan> {

    /** 针对员工 CRITICAL/MODERATE 差距生成建议发展计划（含计划项）。无符合差距返回 null。 */
    @BizMutation
    ErpHrDevelopmentPlan generateDevelopmentPlan(@Name("employeeId") Long employeeId,
                                                 IServiceContext context);

    /** 推进计划项状态（NOT_STARTED→IN_PROGRESS→ACHIEVED/OVERDUE）。非法迁移抛 ErrorCode。 */
    @BizMutation
    ErpHrDevelopmentPlanItem updatePlanItemStatus(@Name("planItemId") Long planItemId,
                                                  @Name("status") String status,
                                                  IServiceContext context);

    /** 完成发展计划（DRAFT/IN_PROGRESS→COMPLETED）。 */
    @BizMutation
    ErpHrDevelopmentPlan completePlan(@Name("planId") Long planId,
                                      IServiceContext context);
}
