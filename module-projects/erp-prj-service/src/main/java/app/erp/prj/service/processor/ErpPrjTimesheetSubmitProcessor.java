package app.erp.prj.service.processor;

import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.dao.entity.ErpPrjTask;
import app.erp.prj.dao.entity.ErpPrjTimesheet;
import app.erp.prj.service.ErpPrjConstants;
import app.erp.prj.service.ErpPrjErrors;
import app.erp.prj.service.cost.BudgetChecker;
import app.erp.prj.service.cost.CostRateResolver;
import app.erp.prj.service.statemachine.ErpPrjTimesheetStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * ErpPrjTimesheet submit per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 DRAFT→SUBMITTED 提交编排：状态守卫 → 项目 OPEN/任务允许校验 → 成本率解析与金额计算 → 预算检查 hook。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpPrjTimesheetSubmitProcessor {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    CostRateResolver costRateResolver;
    @Inject
    BudgetChecker budgetChecker;
    @Inject
    ErpPrjTimesheetStateMachine stateMachine;

    public ErpPrjTimesheet submit(Long timesheetId, IServiceContext context) {
        ErpPrjTimesheet timesheet = requireTimesheet(timesheetId);
        String status = timesheet.getStatus();
        // 幂等：已提交直接返回（既有行为保持）
        if (status != null && Objects.equals(status, ErpPrjConstants.APPROVE_STATUS_SUBMITTED)) {
            return timesheet;
        }
        // 固定来源态守卫委托 StateMachine Bean（非法边 Bean 抛 common 层码，映射为领域码 + expected="DRAFT" 文案保持）
        try {
            stateMachine.assertCanSubmit(status);
        } catch (NopException e) {
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
        timesheet.setStatus(stateMachine.submitTargetStatus());
        runBudgetCheckHook(timesheet, costAmount);
        timesheetDao().updateEntity(timesheet);
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

    private ErpPrjTimesheet requireTimesheet(Long timesheetId) {
        ErpPrjTimesheet timesheet = timesheetDao().getEntityById(timesheetId);
        if (timesheet == null) {
            throw new NopException(ErpPrjErrors.ERR_TIMESHEET_NOT_FOUND)
                    .param(ErpPrjErrors.ARG_TIMESHEET_ID, timesheetId);
        }
        return timesheet;
    }

    private ErpPrjProject loadProject(Long projectId) {
        if (projectId == null) {
            return null;
        }
        IEntityDao<ErpPrjProject> dao = daoProvider.daoFor(ErpPrjProject.class);
        return dao.getEntityById(projectId);
    }

    private ErpPrjTask loadTask(Long taskId) {
        IEntityDao<ErpPrjTask> dao = daoProvider.daoFor(ErpPrjTask.class);
        return dao.getEntityById(taskId);
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
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
