
package app.erp.hr.service.entity;

import app.erp.hr.biz.IErpHrDepartmentBiz;
import app.erp.hr.biz.IErpHrEmployeeBiz;
import app.erp.hr.biz.IErpHrEmploymentContractBiz;
import app.erp.hr.biz.IErpHrLeaveRequestBiz;
import app.erp.hr.biz.IErpHrPositionBiz;
import app.erp.hr.dao.entity.ErpHrDepartment;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrEmploymentContract;
import app.erp.hr.dao.entity.ErpHrLeaveRequest;
import app.erp.hr.dao.entity.ErpHrPosition;
import app.erp.hr.service.ErpHrConfigs;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.dateBetween;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 员工主数据 BizModel（use-cases.md UC-HR-08 部门调动）。继承 {@link CrudBizModel} 标准 CRUD，
 * 扩展 {@link #transferEmployee} 单步直接更新调动（无审批/状态机）。
 *
 * <p>跨实体访问：
 * <ul>
 *   <li>{@link IErpHrDepartmentBiz}/{@link IErpHrPositionBiz}：校验目标部门/职位存在性；</li>
 *   <li>{@link IErpHrEmploymentContractBiz}：合同处理（原 ACTIVE→TERMINATED + 新建 ACTIVE）；</li>
 *   <li>{@link IErpHrLeaveRequestBiz}：查询 APPROVED 休假区间与调动生效日期冲突（告警不阻塞）。</li>
 * </ul>
 */
@BizModel("ErpHrEmployee")
public class ErpHrEmployeeBizModel extends CrudBizModel<ErpHrEmployee> implements IErpHrEmployeeBiz {

    private static final Logger LOG = LoggerFactory.getLogger(ErpHrEmployeeBizModel.class);

    @Inject
    IErpHrDepartmentBiz departmentBiz;
    @Inject
    IErpHrPositionBiz positionBiz;
    @Inject
    IErpHrEmploymentContractBiz employmentContractBiz;
    @Inject
    IErpHrLeaveRequestBiz leaveRequestBiz;

    public ErpHrEmployeeBizModel() {
        setEntityName(ErpHrEmployee.class.getName());
    }

    @Override
    @BizMutation
    public ErpHrEmployee transferEmployee(@Name("employeeId") Long employeeId,
                                          @Name("targetDepartmentId") Long targetDepartmentId,
                                          @Name("targetPositionId") Long targetPositionId,
                                          @Name("targetSuperiorId") Long targetSuperiorId,
                                          @Name("effectiveDate") LocalDate effectiveDate,
                                          @Name("handleContract") String handleContract,
                                          IServiceContext context) {
        ErpHrEmployee employee = requireTransferableEmployee(employeeId, context);
        ErpHrDepartment targetDept = requireTargetDepartment(targetDepartmentId, context);
        if (targetPositionId != null) {
            requireTargetPosition(targetPositionId, targetDept.getId(), context);
        }

        warnIfLeaveConflict(employee.getId(), effectiveDate, context);

        employee.setDepartmentId(targetDept.getId());
        if (targetPositionId != null) {
            employee.setPositionId(targetPositionId);
        }
        if (targetSuperiorId != null) {
            employee.setSuperiorId(targetSuperiorId);
        }
        updateEntity(employee, null, context);

        resolveHandleContract(handleContract, employee, effectiveDate, context);

        return employee;
    }

    // ---------- validation gates ----------

    ErpHrEmployee requireTransferableEmployee(Long employeeId, IServiceContext context) {
        ErpHrEmployee employee = get(String.valueOf(employeeId), false, context);
        if (employee == null) {
            throw new NopException(ErpHrErrors.ERR_EMPLOYEE_NOT_TRANSFERABLE)
                    .param(ErpHrErrors.ARG_EMPLOYEE_ID, employeeId)
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, null);
        }
        String status = employee.getEmploymentStatus();
        if (!isTransferable(status)) {
            throw new NopException(ErpHrErrors.ERR_EMPLOYEE_NOT_TRANSFERABLE)
                    .param(ErpHrErrors.ARG_EMPLOYEE_ID, employeeId)
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, status);
        }
        return employee;
    }

    static boolean isTransferable(String employmentStatus) {
        return ErpHrConstants.EMPLOYMENT_ACTIVE.equals(employmentStatus)
                || ErpHrConstants.EMPLOYMENT_PROBATION.equals(employmentStatus);
    }

    ErpHrDepartment requireTargetDepartment(Long targetDepartmentId, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("id", targetDepartmentId));
        q.setLimit(1);
        ErpHrDepartment dept = departmentBiz.findFirst(q, null, context);
        if (dept == null) {
            throw new NopException(ErpHrErrors.ERR_TRANSFER_TARGET_DEPT_NOT_FOUND)
                    .param(ErpHrErrors.ARG_TARGET_DEPARTMENT_ID, targetDepartmentId);
        }
        return dept;
    }

    ErpHrPosition requireTargetPosition(Long targetPositionId, Long expectedDepartmentId, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("id", targetPositionId));
        q.setLimit(1);
        ErpHrPosition position = positionBiz.findFirst(q, null, context);
        if (position == null
                || (expectedDepartmentId != null
                    && position.getDepartmentId() != null
                    && !expectedDepartmentId.equals(position.getDepartmentId()))) {
            throw new NopException(ErpHrErrors.ERR_TRANSFER_TARGET_POSITION_NOT_FOUND)
                    .param(ErpHrErrors.ARG_TARGET_POSITION_ID, targetPositionId)
                    .param(ErpHrErrors.ARG_TARGET_DEPARTMENT_ID, expectedDepartmentId);
        }
        return position;
    }

    // ---------- leave conflict warn (config-gated, non-blocking) ----------

    void warnIfLeaveConflict(Long employeeId, LocalDate effectiveDate, IServiceContext context) {
        if (!ErpHrConfigs.transferLeaveConflictWarn()) {
            return;
        }
        if (employeeId == null || effectiveDate == null) {
            return;
        }
        // xmeta 仅允许 dateBetween（非 le/ge），故用宽界 dateBetween 表达
        // startDate <= effectiveDate（下界放宽到 1970）且 endDate >= effectiveDate（上界放宽到 2999）
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("employeeId", employeeId),
                eq("status", ErpHrConstants.LEAVE_STATUS_APPROVED),
                dateBetween("startDate", MIN_QUERY_DATE, effectiveDate),
                dateBetween("endDate", effectiveDate, MAX_QUERY_DATE)));
        q.setLimit(1);
        long count = leaveRequestBiz.findCount(q, context);
        if (count > 0) {
            LOG.warn("员工[{}]调动生效日期[{}]与已批准休假冲突，已告警不阻塞（UC-HR-08）", employeeId, effectiveDate);
        }
    }

    static final LocalDate MIN_QUERY_DATE = LocalDate.of(1970, 1, 1);
    static final LocalDate MAX_QUERY_DATE = LocalDate.of(2999, 12, 31);

    // ---------- contract handling ----------

    void resolveHandleContract(String handleContract, ErpHrEmployee employee, LocalDate effectiveDate,
                               IServiceContext context) {
        String mode = normalizeHandleContract(handleContract);
        boolean shouldHandle;
        if (ErpHrConstants.TRANSFER_HANDLE_CONTRACT_YES.equals(mode)) {
            shouldHandle = true;
        } else if (ErpHrConstants.TRANSFER_HANDLE_CONTRACT_NO.equals(mode)) {
            shouldHandle = false;
        } else {
            shouldHandle = ErpHrConfigs.transferAutoHandleContract();
        }
        if (!shouldHandle) {
            return;
        }
        ErpHrEmploymentContract active = findActiveContract(employee.getId(), context);
        if (active != null) {
            active.setStatus(ErpHrConstants.CONTRACT_STATUS_TERMINATED);
            employmentContractBiz.updateEntity(active, null, context);
        }
        ErpHrEmploymentContract successor = newContractFrom(active, employee, effectiveDate);
        employmentContractBiz.saveEntity(successor, null, context);
    }

    static String normalizeHandleContract(String handleContract) {
        if (handleContract == null || handleContract.trim().isEmpty()) {
            return ErpHrConstants.TRANSFER_HANDLE_CONTRACT_AUTO;
        }
        String upper = handleContract.trim().toUpperCase();
        if (ErpHrConstants.TRANSFER_HANDLE_CONTRACT_YES.equals(upper)
                || ErpHrConstants.TRANSFER_HANDLE_CONTRACT_NO.equals(upper)) {
            return upper;
        }
        return ErpHrConstants.TRANSFER_HANDLE_CONTRACT_AUTO;
    }

    ErpHrEmploymentContract findActiveContract(Long employeeId, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("employeeId", employeeId),
                eq("status", ErpHrConstants.CONTRACT_STATUS_ACTIVE)));
        q.setLimit(1);
        return employmentContractBiz.findFirst(q, null, context);
    }

    ErpHrEmploymentContract newContractFrom(ErpHrEmploymentContract active, ErpHrEmployee employee,
                                            LocalDate effectiveDate) {
        ErpHrEmploymentContract c = employmentContractBiz.newEntity();
        c.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        c.setCode(buildSuccessorCode(employee, active, effectiveDate));
        c.setEmployeeId(employee.getId());
        c.setContractType(active != null && active.getContractType() != null
                ? active.getContractType()
                : "FIXED_TERM");
        c.setSignDate(effectiveDate);
        c.setStartDate(effectiveDate);
        if (active != null) {
            c.setEndDate(active.getEndDate());
            c.setProbationMonths(active.getProbationMonths());
            c.setWorkingHoursPerWeek(active.getWorkingHoursPerWeek());
            c.setAnnualSalary(active.getAnnualSalary());
            c.setMonthlySalary(active.getMonthlySalary());
            c.setSalaryCurrencyId(active.getSalaryCurrencyId());
            c.setSalaryPayMethod(active.getSalaryPayMethod());
            c.setSocialInsuranceBase(active.getSocialInsuranceBase());
            c.setHousingFundBase(active.getHousingFundBase());
        }
        c.setStatus(ErpHrConstants.CONTRACT_STATUS_ACTIVE);
        c.setOrgId(employee.getOrgId());
        return c;
    }

    static String buildSuccessorCode(ErpHrEmployee employee, ErpHrEmploymentContract active,
                                     LocalDate effectiveDate) {
        String base = "TRF-" + (employee != null ? employee.getId() : "0") + "-" + effectiveDate.toString();
        if (active != null && active.getCode() != null && !active.getCode().isEmpty()) {
            return base + "-" + active.getCode();
        }
        return base;
    }

    // ---------- helpers for tests ----------

    List<String> nonTransferableStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpHrConstants.EMPLOYMENT_RESIGNED,
                ErpHrConstants.EMPLOYMENT_TERMINATED,
                ErpHrConstants.EMPLOYMENT_RETIRED));
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpHrEmployee.class)
    public List<String> orgName(@ContextSource List<ErpHrEmployee> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrEmployee row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpHrEmployee.class)
    public List<String> departmentName(@ContextSource List<ErpHrEmployee> rows) {
        orm().batchLoadProps(rows, Collections.singleton("department"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrEmployee row : rows) {
            result.add(row.orm_attached() && row.getDepartment() != null ? row.getDepartment().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpHrEmployee.class)
    public List<String> positionName(@ContextSource List<ErpHrEmployee> rows) {
        orm().batchLoadProps(rows, Collections.singleton("position"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrEmployee row : rows) {
            result.add(row.orm_attached() && row.getPosition() != null ? row.getPosition().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpHrEmployee.class)
    public List<String> superiorDisplayName(@ContextSource List<ErpHrEmployee> rows) {
        orm().batchLoadProps(rows, Collections.singleton("superior"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrEmployee row : rows) {
            result.add(row.orm_attached() && row.getSuperior() != null ? row.getSuperior().getFullName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpHrEmployee.class)
    public List<String> costCenterName(@ContextSource List<ErpHrEmployee> rows) {
        orm().batchLoadProps(rows, Collections.singleton("costCenter"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrEmployee row : rows) {
            result.add(row.orm_attached() && row.getCostCenter() != null ? row.getCostCenter().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpHrEmployee.class)
    public List<String> bankAccountName(@ContextSource List<ErpHrEmployee> rows) {
        orm().batchLoadProps(rows, Collections.singleton("bankAccount"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrEmployee row : rows) {
            result.add(row.orm_attached() && row.getBankAccount() != null ? row.getBankAccount().getBankName() : null);
        }
        return result;
    }
}
