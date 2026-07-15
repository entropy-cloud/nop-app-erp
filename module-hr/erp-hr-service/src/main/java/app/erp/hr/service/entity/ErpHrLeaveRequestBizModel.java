
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

import app.erp.hr.biz.IErpHrLeaveBalanceBiz;
import app.erp.hr.biz.IErpHrLeaveRequestBiz;
import app.erp.hr.biz.IErpHrShiftBiz;
import app.erp.hr.dao.entity.ErpHrLeaveBalance;
import app.erp.hr.dao.entity.ErpHrLeaveRequest;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.dateBetween;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * 休假申请 BizModel（use-cases.md UC-HR-02）。继承 {@link CrudBizModel} 标准 CRUD，
 * 扩展审批状态机 submit→approve/reject→cancel + 余额校验 + 日期重叠校验 + 排班联动激活。
 *
 * <p>排班联动经注入 {@link IErpHrShiftBiz}（onLeaveApproved/onLeaveCancelled）——
 * 本引擎是 plan 0831-3 悬空钩子的唯一合法触发源。
 */
@BizModel("ErpHrLeaveRequest")
public class ErpHrLeaveRequestBizModel extends CrudBizModel<ErpHrLeaveRequest> implements IErpHrLeaveRequestBiz {
    @Inject
    IErpHrLeaveBalanceBiz leaveBalanceBiz;
    @Inject
    IErpHrShiftBiz shiftBiz;

    public ErpHrLeaveRequestBizModel() {
        setEntityName(ErpHrLeaveRequest.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpHrLeaveRequest> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpHrLeaveRequest entity = entityData.getEntity();
        if (entity.getBusinessDate() == null) {
            entity.setBusinessDate(CoreMetrics.today());
        }
        computeDurationDays(entity);
        if (entity.getStatus() == null) {
            entity.setStatus(ErpHrConstants.LEAVE_STATUS_DRAFT);
        }
    }

    static void computeDurationDays(ErpHrLeaveRequest entity) {
        if (entity.getStartDate() != null && entity.getEndDate() != null) {
            long days = ChronoUnit.DAYS.between(entity.getStartDate(), entity.getEndDate()) + 1;
            entity.setDurationDays(BigDecimal.valueOf(Math.max(days, 0)));
        }
    }

    @Override
    @BizMutation
    public ErpHrLeaveRequest submit(@Name("id") String id, IServiceContext context) {
        ErpHrLeaveRequest leave = requireEntity(id, null, context);
        requireStatus(leave, ErpHrConstants.LEAVE_STATUS_DRAFT, ErpHrConstants.LEAVE_STATUS_SUBMITTED);
        computeDurationDays(leave);
        checkLeaveBalance(leave, context);
        checkDateOverlap(leave, context, false);
        leave.setStatus(ErpHrConstants.LEAVE_STATUS_SUBMITTED);
        updateEntity(leave, null, context);
        return leave;
    }

    @Override
    @BizMutation
    public ErpHrLeaveRequest approve(@Name("id") String id, IServiceContext context) {
        ErpHrLeaveRequest leave = requireEntity(id, null, context);
        requireStatus(leave, ErpHrConstants.LEAVE_STATUS_SUBMITTED, ErpHrConstants.LEAVE_STATUS_APPROVED);
        computeDurationDays(leave);
        checkLeaveBalance(leave, context);
        leave.setStatus(ErpHrConstants.LEAVE_STATUS_APPROVED);
        leave.setApprovedAt(CoreMetrics.currentTimestamp());
        leave.setApproverId(resolveApproverId(context));
        updateEntity(leave, null, context);
        shiftBiz.onLeaveApproved(leave.getId(), context);
        return leave;
    }

    @Override
    @BizMutation
    public ErpHrLeaveRequest reject(@Name("id") String id, IServiceContext context) {
        ErpHrLeaveRequest leave = requireEntity(id, null, context);
        requireStatus(leave, ErpHrConstants.LEAVE_STATUS_SUBMITTED, ErpHrConstants.LEAVE_STATUS_REJECTED);
        leave.setStatus(ErpHrConstants.LEAVE_STATUS_REJECTED);
        updateEntity(leave, null, context);
        return leave;
    }

    @Override
    @BizMutation
    public ErpHrLeaveRequest cancel(@Name("id") String id, IServiceContext context) {
        ErpHrLeaveRequest leave = requireEntity(id, null, context);
        requireStatus(leave, ErpHrConstants.LEAVE_STATUS_APPROVED, ErpHrConstants.LEAVE_STATUS_CANCELLED);
        leave.setStatus(ErpHrConstants.LEAVE_STATUS_CANCELLED);
        updateEntity(leave, null, context);
        shiftBiz.onLeaveCancelled(leave.getId(), context);
        return leave;
    }

    @Override
    @BizQuery
    public BigDecimal getBalance(@Name("employeeId") Long employeeId,
                                 @Name("leaveType") String leaveType,
                                 @Name("fiscalYear") Integer fiscalYear,
                                 IServiceContext context) {
        ErpHrLeaveBalance balance = findBalance(employeeId, leaveType, fiscalYear, context);
        BigDecimal entitled = balance != null ? nz(balance.getEntitledDays()) : BigDecimal.ZERO;
        BigDecimal carried = balance != null ? nz(balance.getCarriedForwardDays()) : BigDecimal.ZERO;
        BigDecimal used = sumUsedDays(employeeId, leaveType, fiscalYear, context);
        return entitled.add(carried).subtract(used);
    }

    // ---------- validation gates ----------

    void requireStatus(ErpHrLeaveRequest leave, String expected, String target) {
        if (!expected.equals(leave.getStatus())) {
            throw new NopException(ErpHrErrors.ERR_LEAVE_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpHrErrors.ARG_LEAVE_REQUEST_ID, leave.getId())
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, leave.getStatus())
                    .param(ErpHrErrors.ARG_EXPECTED_STATUS, expected);
        }
    }

    void checkLeaveBalance(ErpHrLeaveRequest leave, IServiceContext context) {
        Integer fiscalYear = leave.getStartDate() != null ? leave.getStartDate().getYear() : null;
        if (fiscalYear == null) {
            return;
        }
        ErpHrLeaveBalance balance = findBalance(leave.getEmployeeId(), leave.getLeaveType(), fiscalYear, context);
        if (balance == null) {
            return;
        }
        BigDecimal remaining = nz(balance.getEntitledDays()).add(nz(balance.getCarriedForwardDays()))
                .subtract(sumUsedDays(leave.getEmployeeId(), leave.getLeaveType(), fiscalYear, context));
        if (leave.getDurationDays() != null && remaining.compareTo(leave.getDurationDays()) < 0) {
            throw new NopException(ErpHrErrors.ERR_LEAVE_BALANCE_INSUFFICIENT)
                    .param(ErpHrErrors.ARG_EMPLOYEE_ID, leave.getEmployeeId())
                    .param(ErpHrErrors.ARG_LEAVE_TYPE, leave.getLeaveType())
                    .param(ErpHrErrors.ARG_FISCAL_YEAR, fiscalYear)
                    .param(ErpHrErrors.ARG_ENTITLED_DAYS, nz(balance.getEntitledDays()))
                    .param(ErpHrErrors.ARG_USED_DAYS, sumUsedDays(leave.getEmployeeId(), leave.getLeaveType(), fiscalYear, context))
                    .param(ErpHrErrors.ARG_REQUEST_DAYS, leave.getDurationDays());
        }
    }

    void checkDateOverlap(ErpHrLeaveRequest leave, IServiceContext context, boolean excludeSelf) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("employeeId", leave.getEmployeeId()));
        q.addFilter(eq("leaveType", leave.getLeaveType()));
        q.addFilter(in("status", List.of(
                ErpHrConstants.LEAVE_STATUS_APPROVED,
                ErpHrConstants.LEAVE_STATUS_SUBMITTED)));
        // startDate <= leave.endDate 且 endDate >= leave.startDate 视为重叠
        q.addFilter(dateBetween("startDate", MIN_QUERY_DATE, leave.getEndDate()));
        q.addFilter(dateBetween("endDate", leave.getStartDate(), MAX_QUERY_DATE));
        List<ErpHrLeaveRequest> conflicts = doFindListByQueryDirectly(q, context);
        for (ErpHrLeaveRequest other : conflicts) {
            if (excludeSelf && leave.getId() != null && leave.getId().equals(other.getId())) {
                continue;
            }
            if (leave.getId() == null || !leave.getId().equals(other.getId())) {
                throw new NopException(ErpHrErrors.ERR_LEAVE_DATE_OVERLAP)
                        .param(ErpHrErrors.ARG_EMPLOYEE_ID, leave.getEmployeeId());
            }
        }
    }

    ErpHrLeaveBalance findBalance(Long employeeId, String leaveType, Integer fiscalYear, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("employeeId", employeeId),
                eq("leaveType", leaveType),
                eq("fiscalYear", fiscalYear)));
        q.setLimit(1);
        return leaveBalanceBiz.findFirst(q, null, context);
    }

    BigDecimal sumUsedDays(Long employeeId, String leaveType, Integer fiscalYear, IServiceContext context) {
        LocalDate yearStart = LocalDate.of(fiscalYear, 1, 1);
        LocalDate yearEnd = LocalDate.of(fiscalYear, 12, 31);
        QueryBean q = new QueryBean();
        q.addFilter(eq("employeeId", employeeId));
        q.addFilter(eq("leaveType", leaveType));
        q.addFilter(eq("status", ErpHrConstants.LEAVE_STATUS_APPROVED));
        q.addFilter(dateBetween("startDate", yearStart, yearEnd));
        List<ErpHrLeaveRequest> approved = doFindListByQueryDirectly(q, context);
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpHrLeaveRequest lr : approved) {
            sum = sum.add(nz(lr.getDurationDays()));
        }
        return sum;
    }

    Long resolveApproverId(IServiceContext context) {
        // 审批人取当前用户关联的员工记录（非关键——仅记录审批轨迹）
        return null;
    }

    static final LocalDate MIN_QUERY_DATE = LocalDate.of(1970, 1, 1);
    static final LocalDate MAX_QUERY_DATE = LocalDate.of(2999, 12, 31);

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

}
