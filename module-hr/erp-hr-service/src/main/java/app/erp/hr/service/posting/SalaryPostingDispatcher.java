package app.erp.hr.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrSalary;
import app.erp.hr.service.ErpHrConfigs;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 薪酬过账派发器。APPROVED_MANAGER 后组装 {@link PostingEvent}(SALARY) 经
 * {@link SalaryPostingExecutor}（独立新事务由 Facade {@code IErpFinVoucherBiz.post()} 的 {@code REQUIRES_NEW}
 * 承接）调用财务过账引擎。PAID 触发 SALARY_PAYMENT 发放凭证。
 *
 * <p>对齐 assets/projects 失败语义：过账失败吞异常记日志、保持原状态、{@code posted=false}（薪酬实体无 posted
 * 字段，由调用方在日志层判定），不阻塞终态。
 *
 * <p>贷方科目（应付职工薪酬）取 {@code erp-hr.default-payroll-subject-id}，为空抛
 * {@link ErpHrErrors#ERR_PAYROLL_SUBJECT_NOT_CONFIGURED}。
 */
public class SalaryPostingDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(SalaryPostingDispatcher.class);

    @Inject
    SalaryPostingExecutor executor;
    @Inject
    IDaoProvider daoProvider;

    /**
     * APPROVED_MANAGER 触发计提凭证（借 管理费用-工资 / 贷 应付职工薪酬）。
     * 成功返回 true；失败吞异常返回 false（不阻塞审批流）。
     */
    public boolean tryPostAccrual(ErpHrSalary salary) {
        try {
            PostingEvent event = buildAccrualEvent(salary);
            Long voucherId = executor.postEvent(event);
            return voucherId != null;
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("薪酬计提过账失败，薪酬记录 {} 保持 APPROVED_MANAGER：{}",
                        salary.getId(), e.getMessage());
            } else {
                LOG.error("薪酬计提过账异常，薪酬记录 {} 保持 APPROVED_MANAGER", salary.getId(), e);
            }
            return false;
        }
    }

    /**
     * PAID 触发发放凭证（借 应付职工薪酬 / 贷 银行存款）。
     * 失败吞异常返回 false（不阻塞 PAID 终态）。
     */
    public boolean tryPostPayment(ErpHrSalary salary) {
        try {
            PostingEvent event = buildPaymentEvent(salary);
            Long voucherId = executor.postEvent(event);
            return voucherId != null;
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("薪酬发放过账失败，薪酬记录 {} 已 PAID：{}", salary.getId(), e.getMessage());
            } else {
                LOG.error("薪酬发放过账异常，薪酬记录 {} 已 PAID", salary.getId(), e);
            }
            return false;
        }
    }

    private PostingEvent buildAccrualEvent(ErpHrSalary salary) {
        String creditCode = ErpHrConfigs.defaultPayrollSubjectCode();
        if (creditCode == null) {
            throw new NopException(ErpHrErrors.ERR_PAYROLL_SUBJECT_NOT_CONFIGURED)
                    .param(ErpHrErrors.ARG_SUBJECT_CODE, ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID);
        }
        return buildEvent(salary, ErpFinBusinessType.SALARY,
                ErpHrConstants.SOURCE_BILL_TYPE_SALARY,
                nz(salary.getGrossSalary()),
                null, creditCode);
    }

    private PostingEvent buildPaymentEvent(ErpHrSalary salary) {
        String payrollCode = ErpHrConfigs.defaultPayrollSubjectCode();
        if (payrollCode == null) {
            throw new NopException(ErpHrErrors.ERR_PAYROLL_SUBJECT_NOT_CONFIGURED)
                    .param(ErpHrErrors.ARG_SUBJECT_CODE, ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID);
        }
        return buildEvent(salary, ErpFinBusinessType.SALARY_PAYMENT,
                ErpHrConstants.SOURCE_BILL_TYPE_SALARY_PAYMENT,
                nz(salary.getNetSalary()),
                payrollCode, null);
    }

    private PostingEvent buildEvent(ErpHrSalary salary, ErpFinBusinessType type,
                                    String sourceBillType, BigDecimal amount,
                                    String debitCode, String creditCode) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(type);
        event.setBillHeadCode(buildBillCode(salary));
        event.setOrgId(salary.getOrgId());
        event.setVoucherDate(salary.getPaymentDate() != null ? salary.getPaymentDate()
                : LocalDate.of(salary.getYear(), salary.getMonth(), 15));
        event.setExchangeRate(BigDecimal.ONE);

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(ErpHrConstants.BILL_DATA_SALARY_ID, salary.getId());
        billData.put(ErpHrConstants.BILL_DATA_EMPLOYEE_ID, salary.getEmployeeId());
        billData.put(ErpHrConstants.BILL_DATA_DEPARTMENT_ID, resolveDepartmentId(salary.getEmployeeId()));
        billData.put(ErpHrConstants.BILL_DATA_COST_CENTER_ID, resolveCostCenterId(salary.getEmployeeId()));
        billData.put(ErpHrConstants.BILL_DATA_GROSS_AMOUNT, nz(salary.getGrossSalary()));
        billData.put(ErpHrConstants.BILL_DATA_NET_AMOUNT, nz(salary.getNetSalary()));
        billData.put(ErpHrConstants.BILL_DATA_DEBIT_SUBJECT_CODE, debitCode);
        billData.put(ErpHrConstants.BILL_DATA_CREDIT_SUBJECT_CODE, creditCode);
        billData.put(ErpHrConstants.BILL_DATA_SOURCE_BILL_TYPE, sourceBillType);
        event.setBillData(billData);
        return event;
    }

    private String buildBillCode(ErpHrSalary salary) {
        return "SAL-" + salary.getYear() + String.format("%02d", salary.getMonth()) + "-" + salary.getId();
    }

    private Long resolveDepartmentId(Long employeeId) {
        if (employeeId == null) {
            return null;
        }
        IEntityDao<ErpHrEmployee> dao = daoProvider.daoFor(ErpHrEmployee.class);
        ErpHrEmployee emp = dao.getEntityById(employeeId);
        return emp != null ? emp.getDepartmentId() : null;
    }

    private Long resolveCostCenterId(Long employeeId) {
        if (employeeId == null) {
            return null;
        }
        IEntityDao<ErpHrEmployee> dao = daoProvider.daoFor(ErpHrEmployee.class);
        ErpHrEmployee emp = dao.getEntityById(employeeId);
        return emp != null ? emp.getCostCenterId() : null;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
