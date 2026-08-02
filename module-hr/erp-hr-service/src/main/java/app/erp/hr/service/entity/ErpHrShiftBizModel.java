
package app.erp.hr.service.entity;

import app.erp.hr.biz.IErpHrAttendanceBiz;
import app.erp.hr.biz.IErpHrLeaveRequestBiz;
import app.erp.hr.biz.IErpHrShiftAssignmentBiz;
import app.erp.hr.biz.IErpHrShiftBiz;
import app.erp.hr.dao.entity.ErpHrAttendance;
import app.erp.hr.dao.entity.ErpHrLeaveRequest;
import app.erp.hr.dao.entity.ErpHrShift;
import app.erp.hr.dao.entity.ErpHrShiftAssignment;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import app.erp.hr.service.processor.ErpHrShiftCalcAttendanceProcessor;
import app.erp.hr.service.processor.ErpHrShiftOnLeaveApprovedProcessor;
import app.erp.hr.service.processor.ErpHrShiftOnLeaveCancelledProcessor;
import app.erp.hr.service.scheduling.ShiftAttendanceCalculator;
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
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.dateBetween;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 班次模板 + 考勤计算聚合根 BizModel（shift-scheduling.md §一/§四）。继承 {@link CrudBizModel} 标准 CRUD，
 * 扩展 {@link #calcAttendance} 跨实体读 ShiftAssignment/Attendance 计算迟到/早退/缺勤，结果写 Attendance 已有列。
 *
 * <p>跨实体访问经注入 {@link IErpHrShiftAssignmentBiz} / {@link IErpHrAttendanceBiz}（同域 I*Biz），
 * 不直接操作 daoProvider（除了 ErpHrAttendance 的 findExistingByDate 辅助）。
 */
@BizModel("ErpHrShift")
public class ErpHrShiftBizModel extends CrudBizModel<ErpHrShift> implements IErpHrShiftBiz {

    @Inject
    IErpHrShiftAssignmentBiz assignmentBiz;
    @Inject
    IErpHrAttendanceBiz attendanceBiz;
    @Inject
    IErpHrLeaveRequestBiz leaveRequestBiz;
    @Inject
    ErpHrShiftCalcAttendanceProcessor calcAttendanceProcessor;
    @Inject
    ErpHrShiftOnLeaveApprovedProcessor onLeaveApprovedProcessor;
    @Inject
    ErpHrShiftOnLeaveCancelledProcessor onLeaveCancelledProcessor;

    public ErpHrShiftBizModel() {
        setEntityName(ErpHrShift.class.getName());
    }

    @Override
    @BizMutation
    public ErpHrAttendance calcAttendance(@Name("employeeId") Long employeeId,
                                          @Name("assignmentDate") LocalDate assignmentDate,
                                          IServiceContext context) {
        return calcAttendanceProcessor.calcAttendance(employeeId, assignmentDate, context);
    }

    @Override
    @BizQuery
    public ErpHrAttendance findAttendanceByDate(@Name("employeeId") Long employeeId,
                                                @Name("date") LocalDate date,
                                                IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("employeeId", employeeId), eq("date", date)));
        q.setLimit(1);
        return attendanceBiz.findFirst(q, null, context);
    }

    @Override
    @BizMutation
    public void onLeaveApproved(@Name("leaveRequestId") Long leaveRequestId, IServiceContext context) {
        onLeaveApprovedProcessor.onLeaveApproved(leaveRequestId, context);
    }

    @Override
    @BizMutation
    public void onLeaveCancelled(@Name("leaveRequestId") Long leaveRequestId, IServiceContext context) {
        onLeaveCancelledProcessor.onLeaveCancelled(leaveRequestId, context);
    }

    ErpHrLeaveRequest requireLeaveRequest(Long leaveRequestId, IServiceContext context) {
        ErpHrLeaveRequest leave = leaveRequestBiz.get(String.valueOf(leaveRequestId), false, context);
        if (leave == null) {
            throw new NopException(ErpHrErrors.ERR_LEAVE_REQUEST_NOT_FOUND)
                    .param(ErpHrErrors.ARG_LEAVE_REQUEST_ID, leaveRequestId);
        }
        return leave;
    }

    // ---------- helpers ----------

    ErpHrAttendance upsertAttendanceForAbsent(ErpHrAttendance existing, Long employeeId, LocalDate date,
                                              IServiceContext context) {
        if (existing == null) {
            existing = newAttendance(employeeId, date, context);
        }
        existing.setIsAbsent(true);
        existing.setLateMinutes(0);
        existing.setEarlyLeaveMinutes(0);
        saveOrUpdateAttendance(existing);
        return existing;
    }

    ErpHrAttendance upsertAttendanceForLeave(ErpHrAttendance existing, Long employeeId, LocalDate date,
                                             ErpHrShiftAssignment assignment,
                                             IServiceContext context) {
        if (existing == null) {
            existing = newAttendance(employeeId, date, context);
        }
        existing.setIsAbsent(true);
        existing.setLeaveRequestId(assignment.getLeaveRequestId());
        existing.setLateMinutes(0);
        existing.setEarlyLeaveMinutes(0);
        saveOrUpdateAttendance(existing);
        return existing;
    }

    ErpHrAttendance newAttendance(Long employeeId, LocalDate date, IServiceContext context) {
        IEntityDao<ErpHrAttendance> dao = daoProvider().daoFor(ErpHrAttendance.class);
        ErpHrAttendance a = dao.newEntity();
        a.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        a.setEmployeeId(employeeId);
        a.setDate(date);
        a.setIsAbsent(false);
        a.setLateMinutes(0);
        a.setEarlyLeaveMinutes(0);
        dao.saveEntity(a);
        return a;
    }

    void updateAttendance(ErpHrAttendance attendance) {
        IEntityDao<ErpHrAttendance> dao = daoProvider().daoFor(ErpHrAttendance.class);
        dao.saveOrUpdateEntity(attendance);
    }

    void saveOrUpdateAttendance(ErpHrAttendance attendance) {
        IEntityDao<ErpHrAttendance> dao = daoProvider().daoFor(ErpHrAttendance.class);
        dao.saveOrUpdateEntity(attendance);
    }

    void updateAssignmentStatus(ErpHrShiftAssignment assignment) {
        IEntityDao<ErpHrShiftAssignment> dao = daoProvider().daoFor(ErpHrShiftAssignment.class);
        dao.updateEntity(assignment);
    }

    /**
     * 内部便利方法：查询某员工日期范围内的排班（供 Phase 4 休假联动使用）。
     */
    public List<ErpHrShiftAssignment> findAssignmentsByEmployeeRange(Long employeeId,
                                                                     LocalDate startDate,
                                                                     LocalDate endDate,
                                                                     IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("employeeId", employeeId),
                dateBetween("assignmentDate", startDate, endDate)));
        return assignmentBiz.findList(q, null, context);
    }

}
