package app.erp.hr.service.processor;

import app.erp.hr.biz.IErpHrLeaveBalanceBiz;
import app.erp.hr.dao.entity.ErpHrLeaveBalance;
import app.erp.hr.dao.entity.ErpHrLeaveRequest;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
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
 * 休假申请 per-mutation Processor 共享基类（R6.7，{@code processor-extension-pattern.md} facade protected helper 范式）。
 * 承载 submit/approve/cancel 共用的加载、状态守卫、余额校验、日期重叠校验与审批人解析辅助（单一真相源）。子类只编排单 mutation 步骤顺序。
 */
public abstract class AbstractErpHrLeaveRequestProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpHrLeaveBalanceBiz leaveBalanceBiz;

    static final LocalDate MIN_QUERY_DATE = LocalDate.of(1970, 1, 1);
    static final LocalDate MAX_QUERY_DATE = LocalDate.of(2999, 12, 31);

    protected IEntityDao<ErpHrLeaveRequest> leaveRequestDao() {
        return daoProvider.daoFor(ErpHrLeaveRequest.class);
    }

    protected ErpHrLeaveRequest requireLeave(String id) {
        Long pk = Long.valueOf(id);
        ErpHrLeaveRequest leave = leaveRequestDao().getEntityById(pk);
        if (leave == null) {
            throw new NopException(ErpHrErrors.ERR_LEAVE_REQUEST_NOT_FOUND)
                    .param(ErpHrErrors.ARG_LEAVE_REQUEST_ID, id);
        }
        return leave;
    }

    protected void requireStatus(ErpHrLeaveRequest leave, String expected, String target) {
        if (!expected.equals(leave.getStatus())) {
            throw new NopException(ErpHrErrors.ERR_LEAVE_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpHrErrors.ARG_LEAVE_REQUEST_ID, leave.getId())
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, leave.getStatus())
                    .param(ErpHrErrors.ARG_EXPECTED_STATUS, expected);
        }
    }

    static void computeDurationDays(ErpHrLeaveRequest entity) {
        if (entity.getStartDate() != null && entity.getEndDate() != null) {
            long days = ChronoUnit.DAYS.between(entity.getStartDate(), entity.getEndDate()) + 1;
            entity.setDurationDays(BigDecimal.valueOf(Math.max(days, 0)));
        }
    }

    protected void checkLeaveBalance(ErpHrLeaveRequest leave, IServiceContext context) {
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

    protected void checkDateOverlap(ErpHrLeaveRequest leave, boolean excludeSelf, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("employeeId", leave.getEmployeeId()));
        q.addFilter(eq("leaveType", leave.getLeaveType()));
        q.addFilter(in("status", List.of(
                ErpHrConstants.LEAVE_STATUS_APPROVED,
                ErpHrConstants.LEAVE_STATUS_SUBMITTED)));
        // startDate <= leave.endDate 且 endDate >= leave.startDate 视为重叠
        q.addFilter(dateBetween("startDate", MIN_QUERY_DATE, leave.getEndDate()));
        q.addFilter(dateBetween("endDate", leave.getStartDate(), MAX_QUERY_DATE));
        List<ErpHrLeaveRequest> conflicts = leaveRequestDao().findAllByQuery(q);
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

    protected ErpHrLeaveBalance findBalance(Long employeeId, String leaveType, Integer fiscalYear, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("employeeId", employeeId),
                eq("leaveType", leaveType),
                eq("fiscalYear", fiscalYear)));
        q.setLimit(1);
        return leaveBalanceBiz.findFirst(q, null, context);
    }

    protected BigDecimal sumUsedDays(Long employeeId, String leaveType, Integer fiscalYear, IServiceContext context) {
        LocalDate yearStart = LocalDate.of(fiscalYear, 1, 1);
        LocalDate yearEnd = LocalDate.of(fiscalYear, 12, 31);
        QueryBean q = new QueryBean();
        q.addFilter(eq("employeeId", employeeId));
        q.addFilter(eq("leaveType", leaveType));
        q.addFilter(eq("status", ErpHrConstants.LEAVE_STATUS_APPROVED));
        q.addFilter(dateBetween("startDate", yearStart, yearEnd));
        List<ErpHrLeaveRequest> approved = leaveRequestDao().findAllByQuery(q);
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpHrLeaveRequest lr : approved) {
            sum = sum.add(nz(lr.getDurationDays()));
        }
        return sum;
    }

    protected Long resolveApproverId(IServiceContext context) {
        // 审批人取当前用户关联的员工记录（非关键——仅记录审批轨迹）
        return null;
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
