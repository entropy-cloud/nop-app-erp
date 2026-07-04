
package app.erp.hr.biz;

import app.erp.hr.dao.entity.ErpHrShiftAssignment;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.time.LocalDate;
import java.util.List;

/**
 * 排班分配聚合根 Biz（shift-scheduling.md §二/§九）。除标准 CRUD 外，承载：
 * <ul>
 *   <li>{@link #assignSingle} 单个分配，强制一人一天一排班唯一约束。</li>
 *   <li>{@link #assignBatch} 批量分配（员工组 × 日期范围 × 班次）。</li>
 *   <li>{@link #copyFromPeriod} 复制上期排班（源日期范围 → 目标起始日）。</li>
 *   <li>{@link #findByEmployeeAndDate} 查询某员工某日的有效排班。</li>
 * </ul>
 */
public interface IErpHrShiftAssignmentBiz extends ICrudBiz<ErpHrShiftAssignment> {

    /** 单个排班分配（shift-scheduling.md §2.2/§九.2 唯一约束）。 */
    @BizMutation
    @SingleSession
    ErpHrShiftAssignment assignSingle(@Name("employeeId") Long employeeId,
                                      @Name("shiftId") Long shiftId,
                                      @Name("assignmentDate") LocalDate assignmentDate,
                                      IServiceContext context);

    /** 批量分配（员工组 × 日期范围 × 班次）。 */
    @BizMutation
    @SingleSession
    List<ErpHrShiftAssignment> assignBatch(@Name("employeeIds") List<Long> employeeIds,
                                           @Name("shiftId") Long shiftId,
                                           @Name("startDate") LocalDate startDate,
                                           @Name("endDate") LocalDate endDate,
                                           IServiceContext context);

    /** 复制上期排班（源日期范围 → 目标起始日，逐日对齐）。 */
    @BizMutation
    @SingleSession
    List<ErpHrShiftAssignment> copyFromPeriod(@Name("sourceStartDate") LocalDate sourceStartDate,
                                              @Name("sourceEndDate") LocalDate sourceEndDate,
                                              @Name("targetStartDate") LocalDate targetStartDate,
                                              IServiceContext context);

    /** 查询某员工某日的有效排班（一人一天一排班；可能返回 null）。 */
    @BizQuery
    ErpHrShiftAssignment findByEmployeeAndDate(@Name("employeeId") Long employeeId,
                                               @Name("assignmentDate") LocalDate assignmentDate,
                                               IServiceContext context);
}
