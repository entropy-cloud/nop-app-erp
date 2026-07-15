
package app.erp.hr.service.entity;

import app.erp.hr.biz.IErpHrShiftAssignmentBiz;
import app.erp.hr.biz.IErpHrShiftBiz;
import app.erp.hr.dao.entity.ErpHrShift;
import app.erp.hr.dao.entity.ErpHrShiftAssignment;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.dateBetween;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;
import io.nop.biz.crud.EntityData;

/**
 * 排班分配聚合根 BizModel（shift-scheduling.md §二/§九）。继承 {@link CrudBizModel} 标准 CRUD，
 * 扩展单个/批量/复制分配 + 一人一天一排班唯一约束。
 *
 * <p>跨实体读 Shift（班次模板）经注入 {@link IErpHrShiftBiz}（同域 I*Biz），
 * 不直接操作 {@code daoProvider().daoFor(ErpHrShift.class)}。
 */
@BizModel("ErpHrShiftAssignment")
public class ErpHrShiftAssignmentBizModel extends CrudBizModel<ErpHrShiftAssignment>
        implements IErpHrShiftAssignmentBiz {

    @Inject
    IErpHrShiftBiz shiftBiz;

    public ErpHrShiftAssignmentBizModel() {
        setEntityName(ErpHrShiftAssignment.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpHrShiftAssignment> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpHrShiftAssignment entity = entityData.getEntity();
        if (entity.getBusinessDate() == null) {
            entity.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        }
    }

    @Override
    @BizMutation
    public ErpHrShiftAssignment assignSingle(@Name("employeeId") Long employeeId,
                                             @Name("shiftId") Long shiftId,
                                             @Name("assignmentDate") LocalDate assignmentDate,
                                             IServiceContext context) {
        requireShift(shiftId, context);
        assertNoExistingAssignment(employeeId, assignmentDate, context);
        return doCreateAssignment(employeeId, shiftId, assignmentDate, context);
    }

    @Override
    @BizMutation
    public List<ErpHrShiftAssignment> assignBatch(@Name("employeeIds") List<Long> employeeIds,
                                                   @Name("shiftId") Long shiftId,
                                                   @Name("startDate") LocalDate startDate,
                                                   @Name("endDate") LocalDate endDate,
                                                   IServiceContext context) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return new ArrayList<>();
        }
        requireShift(shiftId, context);
        List<ErpHrShiftAssignment> result = new ArrayList<>();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            for (Long empId : employeeIds) {
                if (existsActiveAssignment(empId, d, context)) {
                    continue;
                }
                result.add(doCreateAssignment(empId, shiftId, d, context));
            }
        }
        return result;
    }

    @Override
    @BizMutation
    public List<ErpHrShiftAssignment> copyFromPeriod(@Name("sourceStartDate") LocalDate sourceStartDate,
                                                      @Name("sourceEndDate") LocalDate sourceEndDate,
                                                      @Name("targetStartDate") LocalDate targetStartDate,
                                                      IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                dateBetween("assignmentDate", sourceStartDate, sourceEndDate),
                in("status", activeStatuses())));
        List<ErpHrShiftAssignment> sources = findList(q, null, context);
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

    @Override
    @BizQuery
    public ErpHrShiftAssignment findByEmployeeAndDate(@Name("employeeId") Long employeeId,
                                                      @Name("assignmentDate") LocalDate assignmentDate,
                                                      IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("employeeId", employeeId),
                eq("assignmentDate", assignmentDate),
                in("status", activeStatuses())));
        q.setLimit(1);
        return findFirst(q, null, context);
    }

    // ---------- helpers ----------

    ErpHrShiftAssignment doCreateAssignment(Long employeeId, Long shiftId, LocalDate date,
                                            IServiceContext context) {
        ErpHrShiftAssignment assignment = newEntity();
        assignment.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        assignment.setEmployeeId(employeeId);
        assignment.setShiftId(shiftId);
        assignment.setAssignmentDate(date);
        assignment.setIsAbsent(false);
        assignment.setStatus(ErpHrConstants.ASSIGNMENT_STATUS_SCHEDULED);
        saveEntity(assignment, null, context);
        return assignment;
    }

    ErpHrShift requireShift(Long shiftId, IServiceContext context) {
        ErpHrShift shift = shiftBiz.get(String.valueOf(shiftId), false, context);
        if (shift == null) {
            throw new NopException(ErpHrErrors.ERR_SHIFT_ROTATION_PATTERN_INVALID)
                    .param(ErpHrErrors.ARG_SHIFT_ID, shiftId);
        }
        return shift;
    }

    void assertNoExistingAssignment(Long employeeId, LocalDate date, IServiceContext context) {
        if (existsActiveAssignment(employeeId, date, context)) {
            throw new NopException(ErpHrErrors.ERR_SHIFT_DUPLICATE_ASSIGNMENT)
                    .param(ErpHrErrors.ARG_EMPLOYEE_ID, employeeId)
                    .param(ErpHrErrors.ARG_ASSIGNMENT_DATE, date);
        }
    }

    boolean existsActiveAssignment(Long employeeId, LocalDate date, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("employeeId", employeeId),
                eq("assignmentDate", date),
                in("status", activeStatuses())));
        q.setLimit(1);
        return findCount(q, context) > 0;
    }

    static List<String> activeStatuses() {
        List<String> list = new ArrayList<>();
        list.add(ErpHrConstants.ASSIGNMENT_STATUS_SCHEDULED);
        list.add(ErpHrConstants.ASSIGNMENT_STATUS_PRESENT);
        list.add(ErpHrConstants.ASSIGNMENT_STATUS_ABSENT);
        return list;
    }

    /**
     * 内部便利方法：按员工 + 日期范围批量查找（供 Phase 4 休假联动使用）。
     */
    public List<ErpHrShiftAssignment> findByEmployeeAndRange(Long employeeId,
                                                             LocalDate startDate,
                                                             LocalDate endDate,
                                                             IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("employeeId", employeeId),
                dateBetween("assignmentDate", startDate, endDate),
                in("status", activeStatuses())));
        return findList(q, null, context);
    }

    /**
     * 内部便利方法：批量删除（供 Phase 4 调换 + 轮换 regenerate 使用）。
     */
    public void deleteAll(List<ErpHrShiftAssignment> list, IServiceContext context) {
        if (list == null) return;
        IEntityDao<ErpHrShiftAssignment> dao = dao();
        for (ErpHrShiftAssignment a : list) {
            dao.deleteEntity(a);
        }
    }

}
