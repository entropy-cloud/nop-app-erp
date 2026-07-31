package app.erp.hr.service.processor;

import app.erp.hr.dao.entity.ErpHrShiftAssignment;
import io.nop.core.context.IServiceContext;

import java.time.LocalDate;

/**
 * ErpHrShiftAssignment assignSingle per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含单个排班分配，强制一人一天一排班唯一约束（shift-scheduling.md §2.2/§九.2）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpHrShiftAssignmentProcessor}。
 */
public class ErpHrShiftAssignmentAssignSingleProcessor extends AbstractErpHrShiftAssignmentProcessor {

    public ErpHrShiftAssignment assignSingle(Long employeeId, Long shiftId, LocalDate assignmentDate,
                                             IServiceContext context) {
        requireShift(shiftId, context);
        assertNoExistingAssignment(employeeId, assignmentDate, context);
        return doCreateAssignment(employeeId, shiftId, assignmentDate, context);
    }
}
