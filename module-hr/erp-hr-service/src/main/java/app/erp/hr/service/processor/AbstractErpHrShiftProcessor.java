package app.erp.hr.service.processor;

import app.erp.hr.biz.IErpHrAttendanceBiz;
import app.erp.hr.biz.IErpHrLeaveRequestBiz;
import app.erp.hr.biz.IErpHrShiftAssignmentBiz;
import app.erp.hr.dao.entity.ErpHrAttendance;
import app.erp.hr.dao.entity.ErpHrLeaveRequest;
import app.erp.hr.dao.entity.ErpHrShiftAssignment;
import app.erp.hr.service.ErpHrErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.dateBetween;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 班次/考勤计算 per-mutation Processor 共享基类（R6.7，{@code processor-extension-pattern.md} facade protected helper 范式）。
 * 承载 calcAttendance/onLeaveApproved/onLeaveCancelled 共用的加载、考勤 upsert 与排班状态同步辅助（单一真相源）。
 * 子类只编排单 mutation 步骤顺序。
 */
public abstract class AbstractErpHrShiftProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpHrShiftAssignmentBiz assignmentBiz;

    @Inject
    IErpHrAttendanceBiz attendanceBiz;

    @Inject
    IErpHrLeaveRequestBiz leaveRequestBiz;

    protected IEntityDao<ErpHrAttendance> attendanceDao() {
        return daoProvider.daoFor(ErpHrAttendance.class);
    }

    protected IEntityDao<ErpHrShiftAssignment> assignmentDao() {
        return daoProvider.daoFor(ErpHrShiftAssignment.class);
    }

    protected ErpHrLeaveRequest requireLeaveRequest(String leaveRequestId, IServiceContext context) {
        ErpHrLeaveRequest leave = leaveRequestBiz.get(String.valueOf(leaveRequestId), false, context);
        if (leave == null) {
            throw new NopException(ErpHrErrors.ERR_LEAVE_REQUEST_NOT_FOUND)
                    .param(ErpHrErrors.ARG_LEAVE_REQUEST_ID, leaveRequestId);
        }
        return leave;
    }

    protected ErpHrAttendance upsertAttendanceForAbsent(ErpHrAttendance existing, String employeeId, LocalDate date,
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

    protected ErpHrAttendance upsertAttendanceForLeave(ErpHrAttendance existing, String employeeId, LocalDate date,
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

    protected ErpHrAttendance newAttendance(String employeeId, LocalDate date, IServiceContext context) {
        IEntityDao<ErpHrAttendance> dao = attendanceDao();
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

    protected void updateAttendance(ErpHrAttendance attendance) {
        IEntityDao<ErpHrAttendance> dao = attendanceDao();
        dao.saveOrUpdateEntity(attendance);
    }

    protected void saveOrUpdateAttendance(ErpHrAttendance attendance) {
        IEntityDao<ErpHrAttendance> dao = attendanceDao();
        dao.saveOrUpdateEntity(attendance);
    }

    protected void updateAssignmentStatus(ErpHrShiftAssignment assignment) {
        IEntityDao<ErpHrShiftAssignment> dao = assignmentDao();
        dao.updateEntity(assignment);
    }

    protected List<ErpHrShiftAssignment> findAssignmentsByEmployeeRange(String employeeId,
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
