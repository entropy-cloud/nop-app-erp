package app.erp.hr.service.entity;

import app.erp.common.service.MaskHelper;
import app.erp.common.service.StringMaskFormat;
import app.erp.common.service.ErpCommonErrors;
import app.erp.hr.biz.IErpHrSalaryBiz;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrPayrollBankFile;
import app.erp.hr.dao.entity.ErpHrSalary;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import app.erp.hr.service.payroll.PayrollCalculator;
import app.erp.hr.service.posting.SalaryPostingDispatcher;
import app.erp.hr.service.processor.ErpHrSalaryCalculateSalaryProcessor;
import app.erp.hr.service.processor.ErpHrSalaryGenerateBankFileProcessor;
import app.erp.hr.service.processor.ErpHrSalaryMarkPaidProcessor;
import app.erp.hr.service.processor.ErpHrSalaryRunPayrollProcessor;
import app.erp.hr.service.statemachine.ErpHrSalaryPaymentStateMachine;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

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
 * 前提条件 {@code approveStatus=APPROVED}。固定状态判断委托实体级 StateMachine Bean（契约 §4/§7）：
 * {@code ErpHrSalaryPaymentStateMachine}（markPaid/voidSalary 支付轴矩阵），审批轴守卫归
 * {@code ErpHrSalary.xbiz}（XScript inject {@code ErpHrSalaryApprovalStateMachine}）。
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
    @Inject
    ErpHrSalaryCalculateSalaryProcessor calculateSalaryProcessor;
    @Inject
    ErpHrSalaryRunPayrollProcessor runPayrollProcessor;
    @Inject
    ErpHrSalaryMarkPaidProcessor markPaidProcessor;
    @Inject
    ErpHrSalaryGenerateBankFileProcessor generateBankFileProcessor;
    @Inject
    ErpHrSalaryPaymentStateMachine paymentStateMachine;

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
    public ErpHrSalary calculateSalary(@Name("employeeId") String employeeId,
                                       @Name("year") int year,
                                       @Name("month") int month,
                                       IServiceContext context) {
        return calculateSalaryProcessor.calculateSalary(employeeId, year, month, context);
    }

    @Override
    @BizMutation
    public List<ErpHrSalary> runPayroll(@Name("year") int year,
                                        @Name("month") int month,
                                        IServiceContext context) {
        return runPayrollProcessor.runPayroll(year, month, context);
    }

    @Override
    @BizMutation
    public ErpHrSalary markPaid(@Name("salaryId") String salaryId, IServiceContext context) {
        return markPaidProcessor.markPaid(salaryId, context);
    }

    @Override
    @BizMutation
    public ErpHrSalary voidSalary(@Name("salaryId") String salaryId, IServiceContext context) {
        ErpHrSalary salary = requireSalary(salaryId, context);
        if (ErpHrConstants.PAYMENT_PAID.equals(salary.getPaymentStatus())) {
            throw new NopException(ErpHrErrors.ERR_SALARY_LOCKED_AFTER_PAID)
                    .param(ErpHrErrors.ARG_SALARY_ID, salaryId);
        }
        try {
            paymentStateMachine.assertCanVoid(salary.getPaymentStatus());
        } catch (NopException e) {
            throw new NopException(ErpHrErrors.ERR_SALARY_ILLEGAL_STATUS_TRANSITION, e)
                    .param(ErpHrErrors.ARG_SALARY_ID, salaryId)
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, salary.getPaymentStatus())
                    .param(ErpHrErrors.ARG_EXPECTED_STATUS, e.getParam(ErpCommonErrors.ARG_EXPECTED_STATUS));
        }
        salary.setPaymentStatus(paymentStateMachine.voidTargetStatus());
        updateEntity(salary, null, context);
        return salary;
    }

    @Override
    @BizMutation
    public ErpHrPayrollBankFile generateBankFile(@Name("year") int year,
                                                 @Name("month") int month,
                                                 @Name("bankId") String bankId,
                                                 IServiceContext context) {
        return generateBankFileProcessor.generateBankFile(year, month, bankId, context);
    }

    @Override
    @BizQuery
    public String queryCumulativeTaxData(@Name("employeeId") String employeeId,
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

    // ---------- E3.1 后端响应层脱敏（@BizLoader，plan 2026-08-10-2059-2）----------
    // 授权角色 = 薪酬审批人（见明文）；非授权 = 数值 null / cumulativeData 全打码。
    // 13 DECIMAL 金额字段 + cumulativeData（个税机密 JSON）。委托 MaskHelper（fail-closed）。
    private static final Set<String> SALARY_MASK_ROLES = Set.of(MaskHelper.ROLE_SALARY_APPROVER);

    @BizLoader("basicSalary")
    public BigDecimal basicSalaryMask(@ContextSource ErpHrSalary entity) {
        return MaskHelper.maskDecimal(entity.getBasicSalary(), SALARY_MASK_ROLES, entity, "basicSalary");
    }

    @BizLoader("positionAllowance")
    public BigDecimal positionAllowanceMask(@ContextSource ErpHrSalary entity) {
        return MaskHelper.maskDecimal(entity.getPositionAllowance(), SALARY_MASK_ROLES, entity, "positionAllowance");
    }

    @BizLoader("performanceBonus")
    public BigDecimal performanceBonusMask(@ContextSource ErpHrSalary entity) {
        return MaskHelper.maskDecimal(entity.getPerformanceBonus(), SALARY_MASK_ROLES, entity, "performanceBonus");
    }

    @BizLoader("overtimePay")
    public BigDecimal overtimePayMask(@ContextSource ErpHrSalary entity) {
        return MaskHelper.maskDecimal(entity.getOvertimePay(), SALARY_MASK_ROLES, entity, "overtimePay");
    }

    @BizLoader("mealAllowance")
    public BigDecimal mealAllowanceMask(@ContextSource ErpHrSalary entity) {
        return MaskHelper.maskDecimal(entity.getMealAllowance(), SALARY_MASK_ROLES, entity, "mealAllowance");
    }

    @BizLoader("transportAllowance")
    public BigDecimal transportAllowanceMask(@ContextSource ErpHrSalary entity) {
        return MaskHelper.maskDecimal(entity.getTransportAllowance(), SALARY_MASK_ROLES, entity, "transportAllowance");
    }

    @BizLoader("otherAllowance")
    public BigDecimal otherAllowanceMask(@ContextSource ErpHrSalary entity) {
        return MaskHelper.maskDecimal(entity.getOtherAllowance(), SALARY_MASK_ROLES, entity, "otherAllowance");
    }

    @BizLoader("grossSalary")
    public BigDecimal grossSalaryMask(@ContextSource ErpHrSalary entity) {
        return MaskHelper.maskDecimal(entity.getGrossSalary(), SALARY_MASK_ROLES, entity, "grossSalary");
    }

    @BizLoader("socialInsurance")
    public BigDecimal socialInsuranceMask(@ContextSource ErpHrSalary entity) {
        return MaskHelper.maskDecimal(entity.getSocialInsurance(), SALARY_MASK_ROLES, entity, "socialInsurance");
    }

    @BizLoader("housingFund")
    public BigDecimal housingFundMask(@ContextSource ErpHrSalary entity) {
        return MaskHelper.maskDecimal(entity.getHousingFund(), SALARY_MASK_ROLES, entity, "housingFund");
    }

    @BizLoader("taxAmount")
    public BigDecimal taxAmountMask(@ContextSource ErpHrSalary entity) {
        return MaskHelper.maskDecimal(entity.getTaxAmount(), SALARY_MASK_ROLES, entity, "taxAmount");
    }

    @BizLoader("otherDeductions")
    public BigDecimal otherDeductionsMask(@ContextSource ErpHrSalary entity) {
        return MaskHelper.maskDecimal(entity.getOtherDeductions(), SALARY_MASK_ROLES, entity, "otherDeductions");
    }

    @BizLoader("netSalary")
    public BigDecimal netSalaryMask(@ContextSource ErpHrSalary entity) {
        return MaskHelper.maskDecimal(entity.getNetSalary(), SALARY_MASK_ROLES, entity, "netSalary");
    }

    @BizLoader("cumulativeData")
    public String cumulativeDataMask(@ContextSource ErpHrSalary entity) {
        return MaskHelper.maskString(entity.getCumulativeData(), StringMaskFormat.FULL, SALARY_MASK_ROLES, entity, "cumulativeData");
    }

    // ---------- helpers ----------

    ErpHrSalary requireSalary(String salaryId, IServiceContext context) {
        return requireEntity(String.valueOf(salaryId), null, context);
    }

    void assertNotDuplicated(String employeeId, int year, int month, IServiceContext context) {
        if (existsNonVoidSalary(employeeId, year, month, context)) {
            throw new NopException(ErpHrErrors.ERR_SALARY_ALREADY_EXISTS)
                    .param(ErpHrErrors.ARG_EMPLOYEE_ID, employeeId)
                    .param(ErpHrErrors.ARG_YEAR, year)
                    .param(ErpHrErrors.ARG_MONTH, month);
        }
    }

    boolean existsNonVoidSalary(String employeeId, int year, int month, IServiceContext context) {
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

    @Override
    @BizQuery
    public java.util.Map<String, Object> findPayrollSummary(@Optional @Name("year") Integer year,
                                                            @Optional @Name("month") Integer month,
                                                            IServiceContext context) {
        QueryBean q = new QueryBean();
        q.setLimit(2000);
        if (year != null) {
            q.addFilter(eq("year", year));
        }
        if (month != null) {
            q.addFilter(eq("month", month));
        }
        List<ErpHrSalary> rows = findList(q, null, context);

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalSocial = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        java.util.Map<String, BigDecimal[]> orgMap = new java.util.LinkedHashMap<>();
        java.util.List<String> orgOrder = new ArrayList<>();
        for (ErpHrSalary s : rows) {
            totalGross = totalGross.add(nz(s.getGrossSalary()));
            totalSocial = totalSocial.add(nz(s.getSocialInsurance()));
            totalTax = totalTax.add(nz(s.getTaxAmount()));
            totalNet = totalNet.add(nz(s.getNetSalary()));
            String key = s.getOrgId() != null ? s.getOrgId() : "";
            BigDecimal[] agg = orgMap.get(key);
            if (agg == null) {
                agg = new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
                orgMap.put(key, agg);
                orgOrder.add(key);
            }
            agg[0] = agg[0].add(BigDecimal.ONE);
            agg[1] = agg[1].add(nz(s.getGrossSalary()));
            agg[2] = agg[2].add(nz(s.getSocialInsurance()));
            agg[3] = agg[3].add(nz(s.getTaxAmount()));
            agg[4] = agg[4].add(nz(s.getNetSalary()));
        }
        List<java.util.Map<String, Object>> orgGroups = new ArrayList<>();
        for (String key : orgOrder) {
            BigDecimal[] agg = orgMap.get(key);
            java.util.Map<String, Object> g = new java.util.LinkedHashMap<>();
            g.put("orgId", key);
            g.put("count", agg[0].intValue());
            g.put("gross", agg[1].setScale(2, java.math.RoundingMode.HALF_UP));
            g.put("social", agg[2].setScale(2, java.math.RoundingMode.HALF_UP));
            g.put("tax", agg[3].setScale(2, java.math.RoundingMode.HALF_UP));
            g.put("net", agg[4].setScale(2, java.math.RoundingMode.HALF_UP));
            orgGroups.add(g);
        }
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("count", rows.size());
        result.put("totalGross", totalGross.setScale(2, java.math.RoundingMode.HALF_UP));
        result.put("totalSocial", totalSocial.setScale(2, java.math.RoundingMode.HALF_UP));
        result.put("totalTax", totalTax.setScale(2, java.math.RoundingMode.HALF_UP));
        result.put("totalNet", totalNet.setScale(2, java.math.RoundingMode.HALF_UP));
        result.put("orgGroups", orgGroups);
        return result;
    }

}
