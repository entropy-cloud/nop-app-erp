package app.erp.hr.service.processor;

import app.erp.hr.dao.entity.ErpHrShiftAssignment;
import io.nop.core.context.IServiceContext;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ErpHrShiftAssignment assignBatch per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含批量分配（员工组 × 日期范围 × 班次），逐人逐日跳过已有活跃排班（shift-scheduling.md §九.3）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpHrShiftAssignmentProcessor}。
 */
public class ErpHrShiftAssignmentAssignBatchProcessor extends AbstractErpHrShiftAssignmentProcessor {

    public List<ErpHrShiftAssignment> assignBatch(List<String> employeeIds, String shiftId, LocalDate startDate,
                                                  LocalDate endDate, IServiceContext context) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return new ArrayList<>();
        }
        requireShift(shiftId, context);
        List<ErpHrShiftAssignment> result = new ArrayList<>();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            for (String empId : employeeIds) {
                if (existsActiveAssignment(empId, d, context)) {
                    continue;
                }
                result.add(doCreateAssignment(empId, shiftId, d, context));
            }
        }
        return result;
    }
}
