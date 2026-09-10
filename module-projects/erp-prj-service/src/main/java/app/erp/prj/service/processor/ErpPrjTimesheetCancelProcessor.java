package app.erp.prj.service.processor;

import app.erp.prj.dao.entity.ErpPrjTimesheet;
import app.erp.prj.service.ErpPrjConstants;
import app.erp.prj.service.ErpPrjErrors;
import app.erp.prj.service.posting.TimesheetPostingDispatcher;
import app.erp.prj.service.statemachine.ErpPrjTimesheetStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.Objects;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpPrjTimesheet）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * ErpPrjTimesheet cancel per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含取消编排：APPROVED 且已过账则先红字冲销，再置回 UNSUBMITTED。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpPrjTimesheetCancelProcessor {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    TimesheetPostingDispatcher postingDispatcher;
    @Inject
    ErpPrjTimesheetStateMachine stateMachine;
    @Inject
    app.erp.prj.service.cost.ProjectCostAggregator costAggregator;

    public ErpPrjTimesheet cancel(String timesheetId, IServiceContext context) {
        ErpPrjTimesheet timesheet = requireTimesheet(timesheetId);
        // 固定迁移守卫委托 StateMachine Bean（撤回语义：基线对所有状态放行，不抛——行为保持）
        stateMachine.assertCanCancel(timesheet.getStatus());
        String status = timesheet.getStatus();
        // 既有红冲过账路径原序保留（§11.2 M4 (ii)/(v)）：APPROVED 且已过账则先红字冲销 + 清 posted 契约
        if (status != null && Objects.equals(status, ErpPrjConstants.APPROVE_STATUS_APPROVED)) {
            if (Boolean.TRUE.equals(timesheet.getPosted())) {
                postingDispatcher.reverse(timesheet);
                timesheet = timesheetDao().getEntityById(timesheetId);
                timesheet.setPosted(false);
                timesheet.setPostedAt(null);
                timesheet.setPostedBy(null);
            }
            // P1-CK-prj-001：归集镜像回退（删除 LABOR 归集行 + 头 totalAmount + project.actualCost 减回）
            // ——修复前 GL 凭证已红冲而项目成本/PnL 仍含该笔（业账分叉），撤回改时重提后归集金额陈旧。
            costAggregator.rollbackFromTimesheet(timesheet);
        }
        timesheet.setStatus(stateMachine.cancelTargetStatus());
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

    private IEntityDao<ErpPrjTimesheet> timesheetDao() {
        return daoProvider.daoFor(ErpPrjTimesheet.class);
    }
}
