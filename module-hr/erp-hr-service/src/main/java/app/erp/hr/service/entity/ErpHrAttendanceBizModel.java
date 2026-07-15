
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;

import app.erp.hr.biz.IErpHrAttendanceBiz;
import app.erp.hr.dao.entity.ErpHrAttendance;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 考勤记录 BizModel（use-cases.md UC-HR-06）。继承 {@link CrudBizModel} 标准 CRUD，
 * 扩展打卡端点 clockIn/clockOut + 当日查询。依赖 Phase 1 新增的 (employeeId, date) 唯一约束。
 */
@BizModel("ErpHrAttendance")
public class ErpHrAttendanceBizModel extends CrudBizModel<ErpHrAttendance> implements IErpHrAttendanceBiz {

    public ErpHrAttendanceBizModel() {
        setEntityName(ErpHrAttendance.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpHrAttendance> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpHrAttendance entity = entityData.getEntity();
        if (entity.getBusinessDate() == null) {
            entity.setBusinessDate(CoreMetrics.today());
        }
    }

    @Override
    @BizMutation
    public ErpHrAttendance clockIn(@Name("employeeId") Long employeeId, IServiceContext context) {
        LocalDate today = CoreMetrics.today();
        ErpHrAttendance attendance = findAttendance(employeeId, today, context);
        if (attendance == null) {
            attendance = newEntity();
            attendance.setBusinessDate(today);
            attendance.setEmployeeId(employeeId);
            attendance.setDate(today);
            attendance.setSource(ErpHrConstants.ATTENDANCE_SOURCE_CARD);
            attendance.setIsAbsent(false);
            attendance.setLateMinutes(0);
            attendance.setEarlyLeaveMinutes(0);
        }
        if (attendance.getClockIn() != null) {
            throw new NopException(ErpHrErrors.ERR_ALREADY_CLOCKED_IN)
                    .param(ErpHrErrors.ARG_EMPLOYEE_ID, employeeId);
        }
        attendance.setClockIn(CoreMetrics.currentTimestamp());
        saveOrUpdateAttendance(attendance, context);
        return attendance;
    }

    @Override
    @BizMutation
    public ErpHrAttendance clockOut(@Name("employeeId") Long employeeId, IServiceContext context) {
        LocalDate today = CoreMetrics.today();
        ErpHrAttendance attendance = findAttendance(employeeId, today, context);
        if (attendance == null || attendance.getClockIn() == null) {
            throw new NopException(ErpHrErrors.ERR_NOT_CLOCKED_IN)
                    .param(ErpHrErrors.ARG_EMPLOYEE_ID, employeeId);
        }
        attendance.setClockOut(CoreMetrics.currentTimestamp());
        attendance.setWorkHours(computeWorkHours(attendance.getClockIn().toLocalDateTime(), attendance.getClockOut().toLocalDateTime()));
        saveOrUpdateAttendance(attendance, context);
        return attendance;
    }

    @Override
    @BizQuery
    public ErpHrAttendance getTodayAttendance(@Name("employeeId") Long employeeId, IServiceContext context) {
        return findAttendance(employeeId, CoreMetrics.today(), context);
    }

    // ---------- helpers ----------

    ErpHrAttendance findAttendance(Long employeeId, LocalDate date, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("employeeId", employeeId), eq("date", date)));
        q.setLimit(1);
        List<ErpHrAttendance> list = findList(q, null, context);
        return list.isEmpty() ? null : list.get(0);
    }

    void saveOrUpdateAttendance(ErpHrAttendance attendance, IServiceContext context) {
        if (attendance.orm_id() != null) {
            updateEntity(attendance, null, context);
        } else {
            saveEntity(attendance, null, context);
        }
    }

    static BigDecimal computeWorkHours(LocalDateTime clockIn, LocalDateTime clockOut) {
        if (clockIn == null || clockOut == null) {
            return BigDecimal.ZERO;
        }
        long minutes = Duration.between(clockIn, clockOut).toMinutes();
        return BigDecimal.valueOf(minutes).divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP);
    }

}
