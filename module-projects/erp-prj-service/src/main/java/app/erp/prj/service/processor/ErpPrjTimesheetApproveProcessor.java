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

    public ErpPrjTimesheet approve(Long timesheetId, IServiceContext context) {
        ErpPrjTimesheet timesheet = requireTimesheet(timesheetId);
        String status = timesheet.getStatus();
        // 幂等：已审批直接返回（既有行为保持）
        if (status != null && Objects.equals(status, ErpPrjConstants.APPROVE_STATUS_APPROVED)) {
            return timesheet;
        }
        // 固定来源态守卫委托 StateMachine Bean（非法边映射为领域码 + expected="SUBMITTED" 文案保持）
        try {
            stateMachine.assertCanApprove(status);
        } catch (NopException e) {
            throw illegalTransition(timesheet, status, "SUBMITTED");
        }

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

        // 归集：工时 APPROVED 同事务生成/追加归集行 + 增量回写 actualCost
        // （cost-collection.md §4.2，归集与过账同事务保证强一致）
        costAggregator.aggregateFromTimesheet(timesheet);
        return timesheet;
    }

    // ---------- helpers ----------

    private ErpPrjTimesheet requireTimesheet(Long timesheetId) {
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
            return null;
        }
    }

    private NopException illegalTransition(ErpPrjTimesheet timesheet, String current, String expected) {
        return new NopException(ErpPrjErrors.ERR_TIMESHEET_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPrjErrors.ARG_TIMESHEET_CODE, timesheet.getCode())
                .param(ErpPrjErrors.ARG_CURRENT_STATUS, current)
                .param(ErpPrjErrors.ARG_EXPECTED_STATUS, expected);
    }

    private IEntityDao<ErpPrjTimesheet> timesheetDao() {
        return daoProvider.daoFor(ErpPrjTimesheet.class);
    }
}
