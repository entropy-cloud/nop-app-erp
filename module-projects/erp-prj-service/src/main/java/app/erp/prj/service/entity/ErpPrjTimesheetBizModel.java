package app.erp.prj.service.entity;

import app.erp.prj.biz.IErpPrjTimesheetBiz;
import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.dao.entity.ErpPrjTask;
import app.erp.prj.dao.entity.ErpPrjTimesheet;
import app.erp.prj.service.ErpPrjConstants;
import app.erp.prj.service.ErpPrjErrors;
import app.erp.prj.service.cost.CostRateResolver;
import app.erp.prj.service.cost.BudgetChecker;
import app.erp.prj.service.cost.ProjectCostAggregator;
import app.erp.prj.service.posting.TimesheetPostingDispatcher;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import java.util.Objects;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 工时记录 BizModel（{@code cost-collection.md §2}）。CRUD 之上实现工时状态机：
 * <ul>
 *   <li>{@code submit}（DRAFT→SUBMITTED）：校验项目 OPEN + 任务允许（TODO/IN_PROGRESS）+ 经
 *       {@link CostRateResolver} 取成本率 + {@code costAmount = hours × costRate}。</li>
 *   <li>{@code approve}（SUBMITTED→APPROVED）：触发 {@code PROJECT_COST_COLLECTION} 业财过账
 *       （借项目成本科目/贷应付职工薪酬），过账成功置 {@code posted=true}。</li>
 *   <li>{@code reject}（SUBMITTED→DRAFT）。</li>
 *   <li>{@code cancel}（非终态→CANCELLED via docStatus；已过账单据先红字冲销）。</li>
 * </ul>
 *
 * <p>{@code @BizMutation} 自动包装事务（不叠加 {@code @Transactional}），每迁移校验前置态，违例抛
 * {@link NopException}+{@link ErpPrjErrors} 作用域码。预算检查在 Phase 2 接线（占位 hook）。
 */
@BizModel("ErpPrjTimesheet")
public class ErpPrjTimesheetBizModel extends CrudBizModel<ErpPrjTimesheet> implements IErpPrjTimesheetBiz {

    @Inject
    CostRateResolver costRateResolver;
    @Inject
    TimesheetPostingDispatcher postingDispatcher;
    @Inject
    BudgetChecker budgetChecker;
    @Inject
    ProjectCostAggregator costAggregator;

    public ErpPrjTimesheetBizModel() {
        setEntityName(ErpPrjTimesheet.class.getName());
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpPrjTimesheet submit(@Name("timesheetId") Long timesheetId, IServiceContext context) {
        ErpPrjTimesheet timesheet = requireTimesheet(timesheetId, context);
        String status = timesheet.getStatus();
        if (status != null && Objects.equals(status, ErpPrjConstants.TIMESHEET_STATUS_SUBMITTED)) {
            return timesheet;
        }
        if (status != null && Objects.equals(status, ErpPrjConstants.TIMESHEET_STATUS_APPROVED)) {
            throw illegalTransition(timesheet, status, "DRAFT");
        }
        if (status != null && !Objects.equals(status, ErpPrjConstants.TIMESHEET_STATUS_DRAFT)) {
            throw illegalTransition(timesheet, status, "DRAFT");
        }

        validateProjectReferenceable(timesheet);
        validateTaskAcceptsTimesheet(timesheet);

        BigDecimal hours = nz(timesheet.getHours());
        BigDecimal costRate = costRateResolver.resolve(timesheet, timesheet.getCode());
        BigDecimal costAmount = CostRateResolver.computeCostAmount(hours, costRate)
                .setScale(4, RoundingMode.HALF_UP);

        timesheet.setCostRate(costRate);
        timesheet.setCostAmount(costAmount);
        timesheet.setStatus(ErpPrjConstants.TIMESHEET_STATUS_SUBMITTED);
        runBudgetCheckHook(timesheet, costAmount);
        dao().updateEntity(timesheet);
        return timesheet;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpPrjTimesheet approve(@Name("timesheetId") Long timesheetId, IServiceContext context) {
        ErpPrjTimesheet timesheet = requireTimesheet(timesheetId, context);
        String status = timesheet.getStatus();
        if (status != null && Objects.equals(status, ErpPrjConstants.TIMESHEET_STATUS_APPROVED)) {
            return timesheet;
        }
        if (status == null || !Objects.equals(status, ErpPrjConstants.TIMESHEET_STATUS_SUBMITTED)) {
            throw illegalTransition(timesheet, status, "SUBMITTED");
        }

        boolean posted = postingDispatcher.tryPost(timesheet);
        timesheet = requireEntity(String.valueOf(timesheetId), null, context);
        timesheet.setStatus(ErpPrjConstants.TIMESHEET_STATUS_APPROVED);
        timesheet.setApprovedBy(currentUserId());
        timesheet.setApprovedAt(CoreMetrics.currentDateTime());
        if (posted) {
            timesheet.setPosted(true);
            timesheet.setPostedAt(CoreMetrics.currentDateTime());
            timesheet.setPostedBy(currentUserId());
        }
        dao().updateEntity(timesheet);

        // 归集：工时 APPROVED 同事务生成/追加归集行 + 增量回写 actualCost
        // （cost-collection.md §4.2，归集与过账同事务保证强一致）
        costAggregator.aggregateFromTimesheet(timesheet);
        return timesheet;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpPrjTimesheet reject(@Name("timesheetId") Long timesheetId, IServiceContext context) {
        ErpPrjTimesheet timesheet = requireTimesheet(timesheetId, context);
        String status = timesheet.getStatus();
        if (status == null || !Objects.equals(status, ErpPrjConstants.TIMESHEET_STATUS_SUBMITTED)) {
            throw illegalTransition(timesheet, status, "SUBMITTED");
        }
        timesheet.setStatus(ErpPrjConstants.TIMESHEET_STATUS_DRAFT);
        dao().updateEntity(timesheet);
        return timesheet;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpPrjTimesheet cancel(@Name("timesheetId") Long timesheetId, IServiceContext context) {
        ErpPrjTimesheet timesheet = requireTimesheet(timesheetId, context);
        String status = timesheet.getStatus();
        if (status != null && Objects.equals(status, ErpPrjConstants.TIMESHEET_STATUS_APPROVED)) {
            if (Boolean.TRUE.equals(timesheet.getPosted())) {
                postingDispatcher.reverse(timesheet);
                timesheet = requireEntity(String.valueOf(timesheetId), null, context);
                timesheet.setPosted(false);
                timesheet.setPostedAt(null);
                timesheet.setPostedBy(null);
            }
        }
        timesheet.setStatus(ErpPrjConstants.TIMESHEET_STATUS_DRAFT);
        dao().updateEntity(timesheet);
        return timesheet;
    }

    // ---------- validation ----------

    private void validateProjectReferenceable(ErpPrjTimesheet timesheet) {
        ErpPrjProject project = loadProject(timesheet.getProjectId());
        if (project == null) {
            throw new NopException(ErpPrjErrors.ERR_TIMESHEET_PROJECT_NOT_OPEN)
                    .param(ErpPrjErrors.ARG_TIMESHEET_CODE, timesheet.getCode())
                    .param(ErpPrjErrors.ARG_PROJECT_ID, timesheet.getProjectId());
        }
        String projectStatus = project.getStatus();
        if (projectStatus == null || !Objects.equals(projectStatus, ErpPrjConstants.PROJECT_STATUS_OPEN)) {
            throw new NopException(ErpPrjErrors.ERR_TIMESHEET_PROJECT_NOT_OPEN)
                    .param(ErpPrjErrors.ARG_TIMESHEET_CODE, timesheet.getCode())
                    .param(ErpPrjErrors.ARG_PROJECT_ID, timesheet.getProjectId());
        }
    }

    private void validateTaskAcceptsTimesheet(ErpPrjTimesheet timesheet) {
        if (timesheet.getTaskId() == null) {
            return;
        }
        ErpPrjTask task = loadTask(timesheet.getTaskId());
        if (task == null) {
            throw new NopException(ErpPrjErrors.ERR_TIMESHEET_TASK_NOT_ALLOWED)
                    .param(ErpPrjErrors.ARG_TIMESHEET_CODE, timesheet.getCode())
                    .param(ErpPrjErrors.ARG_TASK_ID, timesheet.getTaskId());
        }
        String taskStatus = task.getStatus();
        if (taskStatus == null
                || (!Objects.equals(taskStatus, ErpPrjConstants.TASK_STATUS_TODO)
                && !Objects.equals(taskStatus, ErpPrjConstants.TASK_STATUS_IN_PROGRESS))) {
            throw new NopException(ErpPrjErrors.ERR_TIMESHEET_TASK_NOT_ALLOWED)
                    .param(ErpPrjErrors.ARG_TIMESHEET_CODE, timesheet.getCode())
                    .param(ErpPrjErrors.ARG_TASK_ID, timesheet.getTaskId());
        }
    }

    /**
     * 预算检查钩子点。按 {@code erp-prj.budget-control-mode}（WARNING 警告放行 / STRICT 抛错）。
     */
    private void runBudgetCheckHook(ErpPrjTimesheet timesheet, BigDecimal costAmount) {
        budgetChecker.check(timesheet.getProjectId(), costAmount);
    }

    // ---------- helpers ----------

    private ErpPrjTimesheet requireTimesheet(Long timesheetId, IServiceContext context) {
        return requireEntity(String.valueOf(timesheetId), null, context);
    }

    private ErpPrjProject loadProject(Long projectId) {
        if (projectId == null) {
            return null;
        }
        IEntityDao<ErpPrjProject> dao = daoProvider().daoFor(ErpPrjProject.class);
        return dao.getEntityById(projectId);
    }

    private ErpPrjTask loadTask(Long taskId) {
        IEntityDao<ErpPrjTask> dao = daoProvider().daoFor(ErpPrjTask.class);
        return dao.getEntityById(taskId);
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
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
}
