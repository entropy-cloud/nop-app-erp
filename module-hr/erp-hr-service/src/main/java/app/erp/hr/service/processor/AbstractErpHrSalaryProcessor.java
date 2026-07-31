package app.erp.hr.service.processor;

import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrSalary;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.dao.exceptions.UnknownEntityException;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * 薪酬记录 per-mutation Processor 共享基类（R6.7，{@code processor-extension-pattern.md} facade protected helper 范式）。
 * 承载 calculateSalary/runPayroll/markPaid/generateBankFile 四个 per-mutation Processor 共用的加载、去重校验、
 * 可发放薪酬查询辅助（单一真相源）。子类只编排单 mutation 步骤顺序，薪酬语义不变（payroll.md）。
 */
public abstract class AbstractErpHrSalaryProcessor {

    @Inject
    IDaoProvider daoProvider;

    protected IEntityDao<ErpHrSalary> salaryDao() {
        return daoProvider.daoFor(ErpHrSalary.class);
    }

    protected ErpHrSalary requireSalary(Long salaryId, IServiceContext context) {
        ErpHrSalary salary = salaryDao().getEntityById(salaryId);
        if (salary == null) {
            throw new UnknownEntityException(salaryDao().getEntityName(), salaryId);
        }
        return salary;
    }

    protected void assertNotDuplicated(Long employeeId, int year, int month, IServiceContext context) {
        if (existsNonVoidSalary(employeeId, year, month, context)) {
            throw new NopException(ErpHrErrors.ERR_SALARY_ALREADY_EXISTS)
                    .param(ErpHrErrors.ARG_EMPLOYEE_ID, employeeId)
                    .param(ErpHrErrors.ARG_YEAR, year)
                    .param(ErpHrErrors.ARG_MONTH, month);
        }
    }

    protected boolean existsNonVoidSalary(Long employeeId, int year, int month, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("employeeId", employeeId),
                eq("year", year),
                eq("month", month),
                in("paymentStatus", Arrays.asList(
                        ErpHrConstants.PAYMENT_PENDING,
                        ErpHrConstants.PAYMENT_PAID))));
        q.setLimit(1);
        return !salaryDao().findAllByQuery(q).isEmpty();
    }

    protected List<ErpHrEmployee> findActiveEmployees() {
        IEntityDao<ErpHrEmployee> dao = daoProvider.daoFor(ErpHrEmployee.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("employmentStatus", Arrays.asList(
                ErpHrConstants.EMPLOYMENT_ACTIVE, ErpHrConstants.EMPLOYMENT_PROBATION)));
        return dao.findAllByQuery(q);
    }

    protected List<ErpHrSalary> findPayableSalaries(int year, int month, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("year", year),
                eq("month", month),
                eq("approveStatus", ErpHrConstants.APPROVE_STATUS_APPROVED),
                eq("paymentStatus", ErpHrConstants.PAYMENT_PENDING)));
        return salaryDao().findAllByQuery(q);
    }

    protected static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
