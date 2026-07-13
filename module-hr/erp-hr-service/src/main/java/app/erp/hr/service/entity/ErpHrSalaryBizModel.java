package app.erp.hr.service.entity;

import app.erp.hr.biz.IErpHrSalaryBiz;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrPayrollBankFile;
import app.erp.hr.dao.entity.ErpHrSalary;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import app.erp.hr.service.payroll.PayrollCalculator;
import app.erp.hr.service.posting.SalaryPostingDispatcher;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.EntityData;

/**
 * 薪酬记录聚合根 BizModel（payroll.md §五/§六/§七）。继承 {@link CrudBizModel} 标准 CRUD，
 * 扩展薪酬核算引擎与支付轴动作。
 *
 * <p>审批轴（approveStatus UNSUBMITTED/SUBMITTED/APPROVED/REJECTED）由平台
 * {@code approval-support.xbiz} 标准动作提供（DIRECT 模式，多级 WORKFLOW 归 .xwf 后续计划）。
 * 支付轴（paymentStatus PENDING/PAID/VOID）由本类 {@code markPaid}/{@code voidSalary} 管理，
 * 前提条件 {@code approveStatus=APPROVED}。
 *
 * <p>核算委托 {@link PayrollCalculator}（编排），PAID 发放凭证委托
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
    protected void defaultPrepareSave(EntityData<ErpHrSalary> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpHrSalary entity = entityData.getEntity();
        if (entity.getBusinessDate() == null) {
            entity.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        }
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
    public ErpHrSalary markPaid(@Name("salaryId") Long salaryId, IServiceContext context) {
        ErpHrSalary salary = requireSalary(salaryId, context);
        if (!ErpHrConstants.APPROVE_STATUS_APPROVED.equals(salary.getApproveStatus())) {
            throw new NopException(ErpHrErrors.ERR_SALARY_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpHrErrors.ARG_SALARY_ID, salaryId)
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, salary.getApproveStatus())
                    .param(ErpHrErrors.ARG_EXPECTED_STATUS, "APPROVED");
        }
        if (!ErpHrConstants.PAYMENT_PENDING.equals(salary.getPaymentStatus())) {
            throw new NopException(ErpHrErrors.ERR_SALARY_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpHrErrors.ARG_SALARY_ID, salaryId)
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, salary.getPaymentStatus())
                    .param(ErpHrErrors.ARG_EXPECTED_STATUS, "PENDING(paymentStatus)");
        }
        postingDispatcher.tryPostPayment(salary);
        salary = requireSalary(salaryId, context);
        salary.setPaymentStatus(ErpHrConstants.PAYMENT_PAID);
        salary.setPaymentDate(CoreMetrics.today());
        updateEntity(salary, null, context);
        return salary;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpHrSalary voidSalary(@Name("salaryId") Long salaryId, IServiceContext context) {
        ErpHrSalary salary = requireSalary(salaryId, context);
        if (ErpHrConstants.PAYMENT_PAID.equals(salary.getPaymentStatus())) {
            throw new NopException(ErpHrErrors.ERR_SALARY_LOCKED_AFTER_PAID)
                    .param(ErpHrErrors.ARG_SALARY_ID, salaryId);
        }
        salary.setPaymentStatus(ErpHrConstants.PAYMENT_VOID);
        updateEntity(salary, null, context);
        return salary;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpHrPayrollBankFile generateBankFile(@Name("year") int year,
                                                 @Name("month") int month,
                                                 @Name("bankId") Long bankId,
                                                 IServiceContext context) {
        List<ErpHrSalary> pending = findPayableSalaries(year, month, context);
        if (pending.isEmpty()) {
            throw new NopException(ErpHrErrors.ERR_NO_APPROVED_SALARY_FOR_BANK_FILE)
                    .param(ErpHrErrors.ARG_BANK_ID, bankId);
        }
        String batchNo = "PAY-" + year + String.format("%02d", month) + "-" + CoreMetrics.nanoTime();
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
            s.setPaymentStatus(ErpHrConstants.PAYMENT_PAID);
            s.setPaymentDate(CoreMetrics.today());
            updateEntity(s, null, context);
        }

        ErpHrPayrollBankFile bankFile = daoProvider().daoFor(ErpHrPayrollBankFile.class).newEntity();
        bankFile.setBatchNo(batchNo);
        bankFile.setPaymentDate(CoreMetrics.today());
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
            if (ErpHrConstants.PAYMENT_VOID.equals(s.getPaymentStatus())) {
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
                in("paymentStatus", Arrays.asList(
                        ErpHrConstants.PAYMENT_PENDING,
                        ErpHrConstants.PAYMENT_PAID))));
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

    List<ErpHrSalary> findPayableSalaries(int year, int month, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("year", year),
                eq("month", month),
                eq("approveStatus", ErpHrConstants.APPROVE_STATUS_APPROVED),
                eq("paymentStatus", ErpHrConstants.PAYMENT_PENDING)));
        return findList(q, null, context);
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name/*Code 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpHrSalary.class)
    public List<String> employeeDisplayName(@ContextSource List<ErpHrSalary> rows) {
        orm().batchLoadProps(rows, Collections.singleton("employee"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrSalary row : rows) {
            result.add(row.orm_attached() && row.getEmployee() != null ? row.getEmployee().getFullName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpHrSalary.class)
    public List<String> bankFileBatchNo(@ContextSource List<ErpHrSalary> rows) {
        orm().batchLoadProps(rows, Collections.singleton("bankFile"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrSalary row : rows) {
            result.add(row.orm_attached() && row.getBankFile() != null ? row.getBankFile().getBatchNo() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpHrSalary.class)
    public List<String> orgName(@ContextSource List<ErpHrSalary> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrSalary row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }
}
