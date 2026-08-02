package app.erp.hr.service.processor;

import app.erp.hr.dao.entity.ErpHrShiftAssignment;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.dateBetween;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * ErpHrShiftAssignment copyFromPeriod per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含复制上期排班（源日期范围 → 目标起始日，逐日对齐），逐条跳过已有活跃排班（shift-scheduling.md §九.4）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpHrShiftAssignmentProcessor}。
 */
public class ErpHrShiftAssignmentCopyFromPeriodProcessor extends AbstractErpHrShiftAssignmentProcessor {

    public List<ErpHrShiftAssignment> copyFromPeriod(LocalDate sourceStartDate, LocalDate sourceEndDate,
                                                     LocalDate targetStartDate, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                dateBetween("assignmentDate", sourceStartDate, sourceEndDate),
                in("status", activeStatuses())));
        List<ErpHrShiftAssignment> sources = assignmentDao().findAllByQuery(q);
        List<ErpHrShiftAssignment> result = new ArrayList<>();
        for (ErpHrShiftAssignment s : sources) {
            long offset = s.getAssignmentDate().toEpochDay() - sourceStartDate.toEpochDay();
            LocalDate target = targetStartDate.plusDays(offset);
            if (existsActiveAssignment(s.getEmployeeId(), target, context)) {
                continue;
            }
            result.add(doCreateAssignment(s.getEmployeeId(), s.getShiftId(), target, context));
        }
        return result;
    }
}
