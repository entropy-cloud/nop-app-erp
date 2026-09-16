package app.erp.prj.service.processor;

import app.erp.prj.dao.entity.ErpPrjTimesheet;
import app.erp.prj.service.ErpPrjConstants;
import app.erp.prj.service.ErpPrjErrors;
import app.erp.prj.service.cost.ProjectCostAggregator;
import app.erp.prj.service.posting.TimesheetPostingDispatcher;
import app.erp.prj.service.statemachine.ErpPrjTimesheetStateMachine;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.Objects;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpPrjTimesheet）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * ErpPrjTimesheet approve per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 SUBMITTED→APPROVED 审批编排：状态守卫 → 业财过账 → 置 APPROVED（成功则 posted=true）→ 归集行增量回写。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpPrjTimesheetApproveProcessor {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    TimesheetPostingDispatcher postingDispatcher;
    @Inject
    ProjectCostAggregator costAggregator;
    @Inject
    ErpPrjTimesheetStateMachine stateMachine;

    public ErpPrjTimesheet approve(String timesheetId, IServiceContext context) {
        ErpPrjTimesheet timesheet = requireTimesheet(timesheetId);
        String status = timesheet.getStatus();
        // 幂等：已审批直接返回（既有行为保持）
        if (status != null && Objects.equals(status, ErpPrjConstants.APPROVE_STATUS_APPROVED)) {
            return timesheet;
        }
        // 固定来源态守卫委托 StateMachine Bean（Bean 直抛领域码，本处同码补参 timesheetCode，plan 2026-09-07-2200-1）
        try {
            stateMachine.assertCanApprove(status);
        } catch (NopException e) {
            throw e.param(ErpPrjErrors.ARG_TIMESHEET_CODE, timesheet.getCode());
        }

        // F1.2（P1-CK-prj-006 工时链）：成本归集前移到 tryPost（REQUIRES_NEW 凭证独立提交）之前——
        // 归集可抛（STRICT 预算超限/行异常），原顺序下失败会回滚主事务但凭证已提交（孤儿凭证）。
        // 前移后预算拒绝发生在凭证提交前（工时单留 SUBMITTED 无凭证 = 正确语义）；
        // 「归集与过账同事务」（cost-collection.md §4.2）经同主事务保持。
        costAggregator.aggregateFromTimesheet(timesheet);

        boolean posted = postingDispatcher.tryPost(timesheet);
        timesheet = timesheetDao().getEntityById(timesheetId);
        timesheet.setStatus(stateMachine.approveTargetStatus());
        timesheet.setApprovedBy(currentUserId());
        timesheet.setApprovedAt(CoreMetrics.currentTimestamp());
        if (posted) {
            timesheet.setPosted(true);
            timesheet.setPostedAt(CoreMetrics.currentTimestamp());
            timesheet.setPostedBy(currentUserId());
        }
        timesheetDao().updateEntity(timesheet);
        return timesheet;
    }

    // ---------- helpers ----------

    private ErpPrjTimesheet requireTimesheet(String timesheetId) {
        ErpPrjTimesheet timesheet = timesheetDao().getEntityById(timesheetId);
        if (timesheet == null) {
            throw new NopException(ErpPrjErrors.ERR_TIMESHEET_NOT_FOUND)
                    .param(ErpPrjErrors.ARG_TIMESHEET_ID, timesheetId);
        }
        return timesheet;
    }

    private String currentUserId() {
        try {
            IUserContext ctx = IUserContext.get();
            return ctx == null ? null : ctx.getUserId();
        } catch (Exception e) {
            LOG.warn("currentUserId resolution failed (degraded): {}", e.getMessage());            return null;
        }
    }

    private IEntityDao<ErpPrjTimesheet> timesheetDao() {
        return daoProvider.daoFor(ErpPrjTimesheet.class);
    }
}
