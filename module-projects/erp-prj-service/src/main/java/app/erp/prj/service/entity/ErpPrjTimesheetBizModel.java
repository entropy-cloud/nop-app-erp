package app.erp.prj.service.entity;

import app.erp.prj.biz.IErpPrjTimesheetBiz;
import app.erp.prj.dao.entity.ErpPrjTimesheet;
import app.erp.prj.service.ErpPrjConstants;
import app.erp.prj.service.ErpPrjErrors;
import app.erp.prj.service.processor.ErpPrjTimesheetApproveProcessor;
import app.erp.prj.service.processor.ErpPrjTimesheetCancelProcessor;
import app.erp.prj.service.processor.ErpPrjTimesheetSubmitProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * 工时记录 BizModel（{@code cost-collection.md §2}）。CRUD 之上实现工时状态机：
 * <ul>
 *   <li>{@code submit}（DRAFT→SUBMITTED）：校验项目 OPEN + 任务允许（TODO/IN_PROGRESS）+ 经
 *       {@code CostRateResolver} 取成本率 + {@code costAmount = hours × costRate}。</li>
 *   <li>{@code approve}（SUBMITTED→APPROVED）：触发 {@code PROJECT_COST_COLLECTION} 业财过账
 *       （借项目成本科目/贷应付职工薪酬），过账成功置 {@code posted=true}。</li>
 *   <li>{@code reject}（SUBMITTED→DRAFT）。</li>
 *   <li>{@code cancel}（非终态→CANCELLED via docStatus；已过账单据先红字冲销）。</li>
 * </ul>
 *
 * <p>{@code @BizMutation} 自动包装事务（不叠加 {@code @Transactional}），每迁移校验前置态，违例抛
 * {@link NopException}+{@link ErpPrjErrors} 作用域码。预算检查在 Phase 2 接线（占位 hook）。
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
        if (status == null || !Objects.equals(status, ErpPrjConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(timesheet, status, "SUBMITTED");
        }
        timesheet.setStatus(ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED);
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
