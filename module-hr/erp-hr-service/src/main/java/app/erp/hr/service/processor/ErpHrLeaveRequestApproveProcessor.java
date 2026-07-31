package app.erp.hr.service.processor;

import app.erp.hr.biz.IErpHrShiftBiz;
import app.erp.hr.dao.entity.ErpHrLeaveRequest;
import app.erp.hr.service.ErpHrConstants;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpHrLeaveRequest approve per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 SUBMITTED→APPROVED 审批编排：状态守卫 + 天数/余额校验 + 审计字段写回 + 排班联动激活（UC-HR-02）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpHrLeaveRequestProcessor}。
 */
public class ErpHrLeaveRequestApproveProcessor extends AbstractErpHrLeaveRequestProcessor {

    @Inject
    IErpHrShiftBiz shiftBiz;

    public ErpHrLeaveRequest approve(String id, IServiceContext context) {
        ErpHrLeaveRequest leave = requireLeave(id);
        requireStatus(leave, ErpHrConstants.LEAVE_STATUS_SUBMITTED, ErpHrConstants.LEAVE_STATUS_APPROVED);
        computeDurationDays(leave);
        checkLeaveBalance(leave, context);
        leave.setStatus(ErpHrConstants.LEAVE_STATUS_APPROVED);
        leave.setApprovedAt(CoreMetrics.currentTimestamp());
        leave.setApproverId(resolveApproverId(context));
        leaveRequestDao().updateEntity(leave);
        shiftBiz.onLeaveApproved(leave.getId(), context);
        return leave;
    }
}
