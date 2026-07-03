package app.erp.prj.biz;

import app.erp.prj.dao.entity.ErpPrjTimesheet;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

/**
 * 工时记录 Biz 契约。CRUD 之上承载工时状态机（对齐 {@code cost-collection.md §2.3}）：
 * submit（DRAFT→SUBMITTED，校验项目 OPEN + 任务允许 + 解析成本率 + 计算 costAmount + 预算检查占位）、
 * approve（SUBMITTED→APPROVED + 触发 PROJECT_COST_COLLECTION 业财过账 + posted=true）、
 * reject（SUBMITTED→DRAFT）、cancel（非终态→CANCELLED）。
 *
 * <p>审核通过触发 {@code PROJECT_COST_COLLECTION} 业财过账（借项目成本科目/贷应付职工薪酬），
 * posted 标志在过账成功后置位。成本率解析优先级：Timesheet.costRate → ActivityType.costRate
 * → {@code erp-prj.default-labor-cost-rate}（用户级/角色级无独立载体，本期 Non-Goal）。
 */
public interface IErpPrjTimesheetBiz extends ICrudBiz<ErpPrjTimesheet> {

    @BizMutation
    ErpPrjTimesheet submit(@Name("timesheetId") Long timesheetId, IServiceContext context);

    @BizMutation
    ErpPrjTimesheet approve(@Name("timesheetId") Long timesheetId, IServiceContext context);

    @BizMutation
    ErpPrjTimesheet reject(@Name("timesheetId") Long timesheetId, IServiceContext context);

    @BizMutation
    ErpPrjTimesheet cancel(@Name("timesheetId") Long timesheetId, IServiceContext context);
}
