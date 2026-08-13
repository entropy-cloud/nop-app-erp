package app.erp.prj.service.entity;

import app.erp.prj.biz.IErpPrjTimesheetBiz;
import app.erp.prj.dao.entity.ErpPrjTimesheet;
import app.erp.prj.service.ErpPrjErrors;
import app.erp.prj.service.processor.ErpPrjTimesheetApproveProcessor;
import app.erp.prj.service.processor.ErpPrjTimesheetCancelProcessor;
import app.erp.prj.service.processor.ErpPrjTimesheetSubmitProcessor;
import app.erp.prj.service.statemachine.ErpPrjTimesheetStateMachine;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 工时记录 BizModel（{@code cost-collection.md §2}）。CRUD 之上实现工时状态机：
 * <ul>
 *   <li>{@code submit}（UNSUBMITTED→SUBMITTED）：校验项目 OPEN + 任务允许（TODO/IN_PROGRESS）+ 经
 *       {@code CostRateResolver} 取成本率 + {@code costAmount = hours × costRate}。</li>
 *   <li>{@code approve}（SUBMITTED→APPROVED）：触发 {@code PROJECT_COST_COLLECTION} 业财过账
 *       （借项目成本科目/贷应付职工薪酬），过账成功置 {@code posted=true}。</li>
 *   <li>{@code reject}（SUBMITTED→UNSUBMITTED，撤回至可重新提交）。</li>
 *   <li>{@code cancel}（撤回/重置→UNSUBMITTED；{@code wf/approve-status} 无 CANCELLED 值，
 *       APPROVED+posted 单据先红字冲销）。{@code cancel→UNSUBMITTED} 为 intentional legacy behavior
 *       （撤回语义，见 {@code docs/design/projects/state-machine.md} §适用对象四）。</li>
 * </ul>
 *
 * <p>固定迁移守卫委托 {@link ErpPrjTimesheetStateMachine}（{@code reject} 内联；{@code submit}/
 * {@code approve}/{@code cancel} 经 per-mutation Processor），动态守卫（项目/任务/成本率/预算/过账/归集/红冲）
 * 保留在 Processor。{@code @BizMutation} 自动包装事务（不叠加 {@code @Transactional}），违例抛
 * {@link NopException}+{@link ErpPrjErrors} 作用域码。
 *
 * <p>R6.6：{@code submit}/{@code approve}/{@code cancel} 已拆为独立 per-mutation Processor
 * （{@code processor-extension-pattern.md}），本类仅作 facade 单行委托；{@code reject} 保留内联。
 */
@BizModel("ErpPrjTimesheet")
public class ErpPrjTimesheetBizModel extends CrudBizModel<ErpPrjTimesheet> implements IErpPrjTimesheetBiz {

    @Inject
    ErpPrjTimesheetSubmitProcessor submitProcessor;
    @Inject
    ErpPrjTimesheetApproveProcessor approveProcessor;
    @Inject
    ErpPrjTimesheetCancelProcessor cancelProcessor;
    @Inject
    ErpPrjTimesheetStateMachine stateMachine;

    public ErpPrjTimesheetBizModel() {
        setEntityName(ErpPrjTimesheet.class.getName());
    }

    @Override
    @BizMutation
    public ErpPrjTimesheet submit(@Name("timesheetId") Long timesheetId, IServiceContext context) {
        return submitProcessor.submit(timesheetId, context);
    }

    @Override
    @BizMutation
    public ErpPrjTimesheet approve(@Name("timesheetId") Long timesheetId, IServiceContext context) {
        return approveProcessor.approve(timesheetId, context);
    }

    @Override
    @BizMutation
    public ErpPrjTimesheet reject(@Name("timesheetId") Long timesheetId, IServiceContext context) {
        ErpPrjTimesheet timesheet = requireTimesheet(timesheetId, context);
        String status = timesheet.getStatus();
        // 固定来源态守卫委托 StateMachine Bean（非法边映射为领域码 + expected="SUBMITTED" 文案保持）
        try {
            stateMachine.assertCanReject(status);
        } catch (NopException e) {
            throw illegalTransition(timesheet, status, "SUBMITTED");
        }
        timesheet.setStatus(stateMachine.rejectTargetStatus());
        updateEntity(timesheet, null, context);
        return timesheet;
    }

    @Override
    @BizMutation
    public ErpPrjTimesheet cancel(@Name("timesheetId") Long timesheetId, IServiceContext context) {
        return cancelProcessor.cancel(timesheetId, context);
    }

    // ---------- helpers ----------

    private ErpPrjTimesheet requireTimesheet(Long timesheetId, IServiceContext context) {
        return requireEntity(String.valueOf(timesheetId), null, context);
    }

    private NopException illegalTransition(ErpPrjTimesheet timesheet, String current, String expected) {
        return new NopException(ErpPrjErrors.ERR_TIMESHEET_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPrjErrors.ARG_TIMESHEET_CODE, timesheet.getCode())
                .param(ErpPrjErrors.ARG_CURRENT_STATUS, current)
                .param(ErpPrjErrors.ARG_EXPECTED_STATUS, expected);
    }

}
