package app.erp.hr.service.processor;

import app.erp.hr.biz.IErpHrShiftBiz;
import app.erp.hr.dao.entity.ErpHrLeaveRequest;
import app.erp.hr.service.ErpHrConstants;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpHrLeaveRequest cancel per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 APPROVED→CANCELLED 取消编排：状态守卫 + 排班联动解除（UC-HR-02）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpHrLeaveRequestProcessor}。
 */
public class ErpHrLeaveRequestCancelProcessor extends AbstractErpHrLeaveRequestProcessor {

    @Inject
    IErpHrShiftBiz shiftBiz;

    public ErpHrLeaveRequest cancel(String id, IServiceContext context) {
        ErpHrLeaveRequest leave = requireLeave(id);
        requireStatus(leave, ErpHrConstants.LEAVE_STATUS_APPROVED, ErpHrConstants.LEAVE_STATUS_CANCELLED);
        leave.setStatus(ErpHrConstants.LEAVE_STATUS_CANCELLED);
        leaveRequestDao().updateEntity(leave);
        shiftBiz.onLeaveCancelled(leave.getId(), context);
        return leave;
    }
}
