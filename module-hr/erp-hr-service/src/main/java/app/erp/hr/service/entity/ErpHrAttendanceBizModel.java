
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.StringHelper;import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;

import app.erp.hr.biz.IErpHrAttendanceBiz;
import app.erp.hr.dao.entity.ErpHrAttendance;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import app.erp.hr.service.processor.ErpHrAttendanceClockInProcessor;
import app.erp.hr.service.processor.ErpHrAttendanceClockOutProcessor;
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

    @Inject
    ErpHrAttendanceClockInProcessor clockInProcessor;
    @Inject
    ErpHrAttendanceClockOutProcessor clockOutProcessor;

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
        return clockInProcessor.clockIn(employeeId, context);
    }

    @Override
    @BizMutation
    public ErpHrAttendance clockOut(@Name("employeeId") Long employeeId, IServiceContext context) {
        return clockOutProcessor.clockOut(employeeId, context);
    }

    @Override
    @BizQuery
    public ErpHrAttendance getTodayAttendance(@Name("employeeId") Long employeeId, IServiceContext context) {
        return findAttendance(employeeId, CoreMetrics.today(), context);
    }

    @Override
    @BizMutation
    public ErpHrAttendance makeUpClockIn(@Name("employeeId") Long employeeId, @Name("date") LocalDate date,
                                         @Name("clockTime") java.sql.Timestamp clockTime,
                                         @Name("reason") String reason, IServiceContext context) {
        return doMakeUp(employeeId, date, clockTime, reason, true, context);
    }

    @Override
    @BizMutation
    public ErpHrAttendance makeUpClockOut(@Name("employeeId") Long employeeId, @Name("date") LocalDate date,
                                          @Name("clockTime") java.sql.Timestamp clockTime,
                                          @Name("reason") String reason, IServiceContext context) {
        return doMakeUp(employeeId, date, clockTime, reason, false, context);
    }

    // ---------- helpers ----------

    /**
     * 手工补卡公共编排（RC-R1.7，P1-RC-014）。守卫顺序：先 HR 角色后 reason（错误可见性）。
     * 补卡是绕过打卡时序守卫的受控通道：直接写目标时间戳 + source=MANUAL + remark=reason 审计标记。
     */
    private ErpHrAttendance doMakeUp(Long employeeId, LocalDate date, java.sql.Timestamp clockTime,
                                     String reason, boolean clockIn, IServiceContext context) {
        checkMakeUpRole();
        if (StringHelper.isBlank(reason)) {
            throw new NopException(ErpHrErrors.ERR_MAKEUP_REASON_REQUIRED)
                    .param(ErpHrErrors.ARG_EMPLOYEE_ID, employeeId)
                    .param(ErpHrErrors.ARG_ATTENDANCE_DATE, date);
        }
        ErpHrAttendance attendance = findAttendance(employeeId, date, context);
        if (attendance == null) {
            attendance = newEntity();
            attendance.setBusinessDate(date);
            attendance.setEmployeeId(employeeId);
            attendance.setDate(date);
            attendance.setIsAbsent(false);
            attendance.setLateMinutes(0);
            attendance.setEarlyLeaveMinutes(0);
        }
        if (clockIn) {
            attendance.setClockIn(clockTime);
        } else {
            attendance.setClockOut(clockTime);
        }
        attendance.setSource(ErpHrConstants.ATTENDANCE_SOURCE_MANUAL);
        attendance.setRemark(reason);
        if (attendance.getClockIn() != null && attendance.getClockOut() != null) {
            attendance.setWorkHours(computeWorkHours(
                    attendance.getClockIn().toLocalDateTime(),
                    attendance.getClockOut().toLocalDateTime()));
        }
        saveOrUpdateAttendance(attendance, context);
        return attendance;
    }

    /**
     * HR 角色守卫（RC-R1.7，P1-RC-014，Decision 选项 A）：Java 侧经 {@link IUserContext#get()} +
     * {@link IUserContext#isUserInRole(String)} 按 roleId 判定（与 erp-hr.action-auth.xml 菜单 roles
     * 字面一致，SiteMapProvider containsRole 语义）。未登录/缺角色 fail-closed 拒绝。
     */
    private void checkMakeUpRole() {
        IUserContext userContext = IUserContext.get();
        if (userContext == null || !userContext.isUserInRole(ErpHrConstants.HR_ROLE_ID)) {
            throw new NopException(ErpHrErrors.ERR_MAKEUP_ROLE_REQUIRED);
        }
    }

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
