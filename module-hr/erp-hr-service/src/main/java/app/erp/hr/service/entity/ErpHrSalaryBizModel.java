package app.erp.hr.service.entity;

import app.erp.hr.biz.IErpHrSalaryBiz;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrPayrollBankFile;
import app.erp.hr.dao.entity.ErpHrSalary;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import app.erp.hr.service.payroll.PayrollCalculator;
import app.erp.hr.service.posting.SalaryPostingDispatcher;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * 薪酬记录聚合根 BizModel（payroll.md §五/§六/§七）。继承 {@link CrudBizModel} 标准 CRUD，
 * 扩展薪酬核算引擎、审批状态机、银行代发文件生成入口。
 *
 * <p>核算委托 {@link PayrollCalculator}（编排），APPROVED_MANAGER 计提及 PAID 发放凭证委托
 * {@link SalaryPostingDispatcher}（跨域经 finance {@code IErpFinVoucherBiz}）。
 */
@BizModel("ErpHrSalary")
public class ErpHrSalaryBizModel extends CrudBizModel<ErpHrSalary> implements IErpHrSalaryBiz {

    @Inject
    PayrollCalculator payrollCalculator;
    @Inject
    SalaryPostingDispatcher postingDispatcher;

    public ErpHrSalaryBizModel() {
        setEntityName(ErpHrSalary.class.getName());
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpHrSalary calculateSalary(@Name("employeeId") Long employeeId,
                                       @Name("year") int year,
                                       @Name("month") int month,
                                       IServiceContext context) {
        assertNotDuplicated(employeeId, year, month, context);
        ErpHrSalary salary = payrollCalculator.calculate(employeeId, year, month);
        saveEntity(salary, null, context);
        return salary;
    }

    @Override
    @BizMutation
    @SingleSession
    public List<ErpHrSalary> runPayroll(@Name("year") int year,
                                        @Name("month") int month,
                                        IServiceContext context) {
        List<ErpHrEmployee> activeEmployees = findActiveEmployees();
        List<ErpHrSalary> result = new ArrayList<>();
        for (ErpHrEmployee emp : activeEmployees) {
            if (existsNonVoidSalary(emp.getId(), year, month, context)) {
                continue;
            }
            ErpHrSalary salary = payrollCalculator.calculate(emp.getId(), year, month);
            saveEntity(salary, null, context);
            result.add(salary);
        }
        return result;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpHrSalary review(@Name("salaryId") Long salaryId, IServiceContext context) {
        ErpHrSalary salary = requireSalary(salaryId, context);
        assertTransition(salary, ErpHrConstants.APPROVAL_PENDING, ErpHrConstants.APPROVAL_REVIEWED);
        salary.setApprovalStatus(ErpHrConstants.APPROVAL_REVIEWED);
        updateEntity(salary, null, context);
        return salary;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpHrSalary approveFinance(@Name("salaryId") Long salaryId, IServiceContext context) {
        ErpHrSalary salary = requireSalary(salaryId, context);
        assertTransition(salary, ErpHrConstants.APPROVAL_REVIEWED, ErpHrConstants.APPROVAL_APPROVED_FINANCE);
        salary.setApprovalStatus(ErpHrConstants.APPROVAL_APPROVED_FINANCE);
        updateEntity(salary, null, context);
        return salary;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpHrSalary approveManager(@Name("salaryId") Long salaryId, IServiceContext context) {
        ErpHrSalary salary = requireSalary(salaryId, context);
        assertTransition(salary, ErpHrConstants.APPROVAL_APPROVED_FINANCE, ErpHrConstants.APPROVAL_APPROVED_MANAGER);
        // 计提过账在状态迁移前触发（失败吞异常不阻塞审批流，对齐 assets/projects 失败语义）
        postingDispatcher.tryPostAccrual(salary);
        // 过账可能跨事务，重新加载实体后更新状态
        salary = requireSalary(salaryId, context);
        salary.setApprovalStatus(ErpHrConstants.APPROVAL_APPROVED_MANAGER);
        dao().updateEntity(salary);
        return salary;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpHrSalary rejectSalary(@Name("salaryId") Long salaryId, IServiceContext context) {
        ErpHrSalary salary = requireSalary(salaryId, context);
        String current = salary.getApprovalStatus();
        if (!ErpHrConstants.APPROVAL_REVIEWED.equals(current)
                && !ErpHrConstants.APPROVAL_APPROVED_FINANCE.equals(current)) {
            throw new NopException(ErpHrErrors.ERR_SALARY_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpHrErrors.ARG_SALARY_ID, salaryId)
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, current)
                    .param(ErpHrErrors.ARG_EXPECTED_STATUS, "REVIEWED|APPROVED_FINANCE");
        }
        salary.setApprovalStatus(ErpHrConstants.APPROVAL_PENDING);
        updateEntity(salary, null, context);
        return salary;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpHrSalary voidSalary(@Name("salaryId") Long salaryId, IServiceContext context) {
        ErpHrSalary salary = requireSalary(salaryId, context);
        if (ErpHrConstants.APPROVAL_PAID.equals(salary.getApprovalStatus())) {
            throw new NopException(ErpHrErrors.ERR_SALARY_LOCKED_AFTER_PAID)
                    .param(ErpHrErrors.ARG_SALARY_ID, salaryId);
        }
        salary.setApprovalStatus(ErpHrConstants.APPROVAL_VOID);
        salary.setPaymentStatus(ErpHrConstants.PAYMENT_VOID);
        updateEntity(salary, null, context);
        return salary;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpHrSalary markPaid(@Name("salaryId") Long salaryId, IServiceContext context) {
        ErpHrSalary salary = requireSalary(salaryId, context);
        assertTransition(salary, ErpHrConstants.APPROVAL_APPROVED_MANAGER, ErpHrConstants.APPROVAL_PAID);
        // 发放过账在状态迁移前触发（失败吞异常不阻塞 PAID 终态）
        postingDispatcher.tryPostPayment(salary);
        salary = requireSalary(salaryId, context);
        salary.setApprovalStatus(ErpHrConstants.APPROVAL_PAID);
        salary.setPaymentStatus(ErpHrConstants.PAYMENT_PAID);
        salary.setPaymentDate(LocalDate.now());
        dao().updateEntity(salary);
        return salary;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpHrPayrollBankFile generateBankFile(@Name("year") int year,
                                                 @Name("month") int month,
                                                 @Name("bankId") Long bankId,
                                                 IServiceContext context) {
        List<ErpHrSalary> pending = findApprovedManagerSalaries(year, month, context);
        if (pending.isEmpty()) {
            throw new NopException(ErpHrErrors.ERR_NO_APPROVED_SALARY_FOR_BANK_FILE)
                    .param(ErpHrErrors.ARG_BANK_ID, bankId);
        }
        String batchNo = "PAY-" + year + String.format("%02d", month) + "-" + System.nanoTime();
        StringBuilder content = new StringBuilder();
        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        for (ErpHrSalary s : pending) {
            count++;
            BigDecimal net = nz(s.getNetSalary());
            total = total.add(net);
            content.append(String.format("%03d", count)).append(",")
                    .append(s.getEmployeeId()).append(",")
                    .append(net.toPlainString()).append(",工资\n");
            s.setPaymentBatchNo(batchNo);
            s.setApprovalStatus(ErpHrConstants.APPROVAL_PAID);
            s.setPaymentStatus(ErpHrConstants.PAYMENT_PAID);
            s.setPaymentDate(LocalDate.now());
            updateEntity(s, null, context);
        }

        ErpHrPayrollBankFile bankFile = new ErpHrPayrollBankFile();
        bankFile.setBatchNo(batchNo);
        bankFile.setPaymentDate(LocalDate.now());
        bankFile.setTotalAmount(total);
        bankFile.setRecordCount(count);
        bankFile.setFileFormat(ErpHrConstants.BANK_FILE_FORMAT_CSV);
        bankFile.setFileContent(content.toString());
        bankFile.setStatus(ErpHrConstants.BANK_FILE_STATUS_GENERATED);
        bankFile.setBankId(bankId);
        IEntityDao<ErpHrPayrollBankFile> bankFileDao = daoProvider().daoFor(ErpHrPayrollBankFile.class);
        bankFileDao.saveEntity(bankFile);

        for (ErpHrSalary s : pending) {
            s.setBankFileId(bankFile.getId());
            updateEntity(s, null, context);
        }
        return bankFile;
    }

    @Override
    @BizQuery
    public String queryCumulativeTaxData(@Name("employeeId") Long employeeId,
                                         @Name("year") int year,
                                         @Name("upToMonth") int upToMonth,
                                         IServiceContext context) {
        IEntityDao<ErpHrSalary> dao = daoProvider().daoFor(ErpHrSalary.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("employeeId", employeeId), eq("year", year)));
        List<ErpHrSalary> all = dao.findAllByQuery(q);
        ErpHrSalary latest = null;
        for (ErpHrSalary s : all) {
            if (ErpHrConstants.APPROVAL_VOID.equals(s.getApprovalStatus())) {
                continue;
            }
            if (s.getMonth() != null && s.getMonth() <= upToMonth) {
                if (latest == null || s.getMonth() > latest.getMonth()) {
                    latest = s;
                }
            }
        }
        return latest != null ? latest.getCumulativeData() : "{}";
    }

    // ---------- helpers ----------

    ErpHrSalary requireSalary(Long salaryId, IServiceContext context) {
        return requireEntity(String.valueOf(salaryId), null, context);
    }

    void assertTransition(ErpHrSalary salary, String expectedFrom, String targetTo) {
        String current = salary.getApprovalStatus();
        if (!expectedFrom.equals(current)) {
            throw new NopException(ErpHrErrors.ERR_SALARY_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpHrErrors.ARG_SALARY_ID, salary.getId())
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, current)
                    .param(ErpHrErrors.ARG_EXPECTED_STATUS, targetTo);
        }
    }

    void assertNotDuplicated(Long employeeId, int year, int month, IServiceContext context) {
        if (existsNonVoidSalary(employeeId, year, month, context)) {
            throw new NopException(ErpHrErrors.ERR_SALARY_ALREADY_EXISTS)
                    .param(ErpHrErrors.ARG_EMPLOYEE_ID, employeeId)
                    .param(ErpHrErrors.ARG_YEAR, year)
                    .param(ErpHrErrors.ARG_MONTH, month);
        }
    }

    boolean existsNonVoidSalary(Long employeeId, int year, int month, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("employeeId", employeeId),
                eq("year", year),
                eq("month", month),
                in("approvalStatus", Arrays.asList(
                        ErpHrConstants.APPROVAL_PENDING,
                        ErpHrConstants.APPROVAL_REVIEWED,
                        ErpHrConstants.APPROVAL_APPROVED_FINANCE,
                        ErpHrConstants.APPROVAL_APPROVED_MANAGER,
                        ErpHrConstants.APPROVAL_PAID))));
        q.setLimit(1);
        return findCount(q, context) > 0;
    }

    List<ErpHrEmployee> findActiveEmployees() {
        IEntityDao<ErpHrEmployee> dao = daoProvider().daoFor(ErpHrEmployee.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("employmentStatus", Arrays.asList(
                ErpHrConstants.EMPLOYMENT_ACTIVE, ErpHrConstants.EMPLOYMENT_PROBATION)));
        return dao.findAllByQuery(q);
    }

    List<ErpHrSalary> findApprovedManagerSalaries(int year, int month, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("year", year),
                eq("month", month),
                eq("approvalStatus", ErpHrConstants.APPROVAL_APPROVED_MANAGER)));
        return findList(q, null, context);
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
