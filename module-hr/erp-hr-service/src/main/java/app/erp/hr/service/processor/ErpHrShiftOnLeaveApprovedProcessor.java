package app.erp.hr.service.processor;

import app.erp.hr.dao.entity.ErpHrLeaveRequest;
import app.erp.hr.dao.entity.ErpHrShiftAssignment;
import app.erp.hr.service.ErpHrConstants;
import io.nop.core.context.IServiceContext;

import java.util.List;

/**
 * ErpHrShift onLeaveApproved per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含休假审批通过联动：检索员工休假日期范围内的排班，标记 isAbsent/absenceReason=LEAVE/leaveRequestId/
 * status=ABSENT（shift-scheduling.md §6.1）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpHrShiftProcessor}。
 */
public class ErpHrShiftOnLeaveApprovedProcessor extends AbstractErpHrShiftProcessor {

    public void onLeaveApproved(String leaveRequestId, IServiceContext context) {
        ErpHrLeaveRequest leave = requireLeaveRequest(leaveRequestId, context);
        List<ErpHrShiftAssignment> assignments = findAssignmentsByEmployeeRange(
                leave.getEmployeeId(), leave.getStartDate(), leave.getEndDate(), context);
        for (ErpHrShiftAssignment a : assignments) {
            a.setIsAbsent(true);
            a.setAbsenceReason(ErpHrConstants.ABSENCE_REASON_LEAVE);
            a.setLeaveRequestId(leaveRequestId);
            a.setStatus(ErpHrConstants.ASSIGNMENT_STATUS_ABSENT);
            updateAssignmentStatus(a);
        }
    }
}
