package app.erp.prj.service.processor;

import app.erp.prj.dao.entity.ErpPrjTimesheet;
import app.erp.prj.service.ErpPrjConstants;
import app.erp.prj.service.ErpPrjErrors;
import app.erp.prj.service.posting.TimesheetPostingDispatcher;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * ErpPrjTimesheet cancel per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含取消编排：APPROVED 且已过账则先红字冲销，再置回 UNSUBMITTED。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpPrjTimesheetCancelProcessor {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    TimesheetPostingDispatcher postingDispatcher;

    public ErpPrjTimesheet cancel(Long timesheetId, IServiceContext context) {
        ErpPrjTimesheet timesheet = requireTimesheet(timesheetId);
        String status = timesheet.getStatus();
        if (status != null && Objects.equals(status, ErpPrjConstants.APPROVE_STATUS_APPROVED)) {
            if (Boolean.TRUE.equals(timesheet.getPosted())) {
                postingDispatcher.reverse(timesheet);
                timesheet = timesheetDao().getEntityById(timesheetId);
                timesheet.setPosted(false);
                timesheet.setPostedAt(null);
                timesheet.setPostedBy(null);
            }
        }
        timesheet.setStatus(ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED);
        timesheetDao().updateEntity(timesheet);
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

    private IEntityDao<ErpPrjTimesheet> timesheetDao() {
        return daoProvider.daoFor(ErpPrjTimesheet.class);
    }
}
