package app.erp.hr.service.processor;

import app.erp.hr.dao.entity.ErpHrLeaveRequest;
import app.erp.hr.dao.entity.ErpHrShiftAssignment;
import app.erp.hr.service.ErpHrConstants;
import io.nop.core.context.IServiceContext;

import java.util.List;

/**
 * ErpHrShift onLeaveCancelled per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含休假取消联动：解除由该休假标记的排班（leaveRequestId 匹配），status→SCHEDULED，isAbsent=false，
 * absenceReason=null，leaveRequestId=null（shift-scheduling.md §6.2）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpHrShiftProcessor}。
 */
public class ErpHrShiftOnLeaveCancelledProcessor extends AbstractErpHrShiftProcessor {

    public void onLeaveCancelled(Long leaveRequestId, IServiceContext context) {
        ErpHrLeaveRequest leave = requireLeaveRequest(leaveRequestId, context);
        List<ErpHrShiftAssignment> assignments = findAssignmentsByEmployeeRange(
                leave.getEmployeeId(), leave.getStartDate(), leave.getEndDate(), context);
        for (ErpHrShiftAssignment a : assignments) {
            // 仅解除由该休假标记的（leaveRequestId 匹配）
            if (leaveRequestId.equals(a.getLeaveRequestId())) {
                a.setIsAbsent(false);
                a.setAbsenceReason(null);
                a.setLeaveRequestId(null);
                a.setStatus(ErpHrConstants.ASSIGNMENT_STATUS_SCHEDULED);
                updateAssignmentStatus(a);
            }
        }
    }
}
