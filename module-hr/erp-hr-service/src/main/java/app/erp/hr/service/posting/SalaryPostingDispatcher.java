package app.erp.hr.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.service.ErpFinConstants;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrSalary;
import app.erp.hr.service.ErpHrConfigs;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import app.erp.hr.service.payroll.SocialInsuranceCalculator;
import app.erp.md.dao.AcctSchemaResolver;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 薪酬过账派发器。APPROVED 触发计提链（RC-R1.89）：SALARY(270) + SOCIAL_INSURANCE_ER(290) +
 * HOUSING_FUND_ER(300) 三类计提凭证，经 {@link SalaryPostingExecutor}（独立新事务由 Facade
 * {@code IErpFinVoucherBiz.post()} 的 {@code REQUIRES_NEW} 承接）调用财务过账引擎；PAID 触发
 * SALARY_PAYMENT(280) 发放凭证。计提链由 {@code ErpHrSalaryPostApprovalProcessor}（approve 后置编排）
 * 按 270→290→300 顺序调用，三条路径各自失败隔离（G3 吞异常 + 告警 + 该条计失败）。
 *
 * <p>对齐 assets/projects 失败语义：过账失败吞异常记日志、保持原状态、不阻塞终态。
 *
 * <p><b>{@code posted} writer 语义（RC-R1.89 / P1-MA4-017，D3 裁决）</b>：三条计提路径全部成功（去重
 * 守卫命中计为成功）才 {@code posted=true}（「计提链完整」语义）；任一失败保持 false + 告警，
 * 280 发放不受影响。writer 在编排 Processor 侧（非本类）。**去重守卫**：per-businessType 已过账凭证
 * 存在性反查（{@link #alreadyPosted}，镜像引擎 {@code ErpFinPostingProcessor.alreadyPosted} 范式），
 * reverseApprove→再 approve 路径下已存在凭证跳过该条且计为成功——补投失败条目可收敛 posted=true
 * （引擎级幂等命中返回 null，若不经守卫会被 {@code voucherId != null} 误判为失败）。
 *
 * <p><b>公司承担（ER）金额载体（D1 裁决）</b>：approve 过账时重算（复用 {@link SocialInsuranceCalculator}，
 * 镜像 {@code PayrollCalculator} 口径），经 billData {@code GROSS_AMOUNT}（createFacts 消费键）+
 * 专用键 {@code SOCIAL_INSURANCE_ER}/{@code HOUSING_FUND_ER} 承载；290/300 事件**不带 NET_AMOUNT**
 * （避免 ER=0 时 createFacts 回退读净额）。残留风险：calculate 与 approve 之间社保/公积金 config
 * 变更使重算值偏离原核算口径（运营约定 approve 前不改基数据，payroll.md 注记）。
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
    @Inject
    IErpSysNotificationBiz notificationBiz;
    @Inject
    SocialInsuranceCalculator socialInsuranceCalculator;

    static final String NOTIFY_EVENT_SALARY_FAILURE = "hr.salary-posting-failure";

    /**
     * APPROVED 触发计提凭证（借 管理费用-工资 / 贷 应付职工薪酬），金额 = 应发合计。
     * 成功返回 true；失败吞异常返回 false（不阻塞审批流）。
     *
     * <p>G3 错误传播分级（plan 2026-07-30-0341-2 P1-MA2-048）：失败派发 IErpSysNotificationBiz 告警
     * 使 posted=false 悬挂可被感知。
     *
     * <p>去重守卫（D3）：该 billCode 已存在 POSTED 未冲销 SALARY 凭证时跳过并计为成功
     * （reverseApprove→再 approve 幂等）。
     */
    public boolean tryPostAccrual(ErpHrSalary salary) {
        if (alreadyPosted(buildBillCode(salary), ErpFinBusinessType.SALARY)) {
            LOG.info("薪酬计提凭证已存在，去重守卫跳过：salaryId={}, billCode={}",
                    salary.getId(), buildBillCode(salary));
            return true;
        }
        try {
            PostingEvent event = buildAccrualEvent(salary);
            Long voucherId = executor.postEvent(event);
            return voucherId != null;
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("薪酬计提过账失败，薪酬记录 {} 保持 APPROVED：{}",
                        salary.getId(), e.getMessage());
            } else {
                LOG.error("薪酬计提过账异常，薪酬记录 {} 保持 APPROVED", salary.getId(), e);
            }
            dispatchFailureAlert(salary, "计提", e);
            return false;
        }
    }

    /**
     * APPROVED 触发社保公司承担凭证（借 管理费用-社保 / 贷 应付职工薪酬-社保），金额 = approve 时
     * 重算的公司承担社保（D1）。失败吞异常返回 false + 告警（stage=社保）；去重守卫同
     * {@link #tryPostAccrual}。
     */
    public boolean tryPostSocialInsuranceER(ErpHrSalary salary) {
        if (alreadyPosted(buildBillCode(salary), ErpFinBusinessType.SOCIAL_INSURANCE_ER)) {
            LOG.info("社保公司承担凭证已存在，去重守卫跳过：salaryId={}, billCode={}",
                    salary.getId(), buildBillCode(salary));
            return true;
        }
        try {
            PostingEvent event = buildCompanyBorneEvent(salary, ErpFinBusinessType.SOCIAL_INSURANCE_ER,
                    ErpHrConstants.SOURCE_BILL_TYPE_SOCIAL_INSURANCE_ER,
                    ErpHrConstants.BILL_DATA_SOCIAL_INSURANCE_ER,
                    recomputeSocialInsuranceER(salary));
            Long voucherId = executor.postEvent(event);
            return voucherId != null;
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("社保公司承担过账失败，薪酬记录 {} 保持 APPROVED：{}", salary.getId(), e.getMessage());
            } else {
                LOG.error("社保公司承担过账异常，薪酬记录 {} 保持 APPROVED", salary.getId(), e);
            }
            dispatchFailureAlert(salary, "社保", e);
            return false;
        }
    }

    /**
     * APPROVED 触发公积金公司承担凭证（借 管理费用-公积金 / 贷 应付职工薪酬-公积金），金额 = approve 时
     * 重算的公司承担公积金（D1）。失败吞异常返回 false + 告警（stage=公积金）；去重守卫同
     * {@link #tryPostAccrual}。
     */
    public boolean tryPostHousingFundER(ErpHrSalary salary) {
        if (alreadyPosted(buildBillCode(salary), ErpFinBusinessType.HOUSING_FUND_ER)) {
            LOG.info("公积金公司承担凭证已存在，去重守卫跳过：salaryId={}, billCode={}",
                    salary.getId(), buildBillCode(salary));
            return true;
        }
        try {
            PostingEvent event = buildCompanyBorneEvent(salary, ErpFinBusinessType.HOUSING_FUND_ER,
                    ErpHrConstants.SOURCE_BILL_TYPE_HOUSING_FUND_ER,
                    ErpHrConstants.BILL_DATA_HOUSING_FUND_ER,
                    recomputeHousingFundER(salary));
            Long voucherId = executor.postEvent(event);
            return voucherId != null;
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("公积金公司承担过账失败，薪酬记录 {} 保持 APPROVED：{}", salary.getId(), e.getMessage());
            } else {
                LOG.error("公积金公司承担过账异常，薪酬记录 {} 保持 APPROVED", salary.getId(), e);
            }
            dispatchFailureAlert(salary, "公积金", e);
            return false;
        }
    }

    /**
     * PAID 触发发放凭证（借 应付职工薪酬 / 贷 银行存款）。
     * 失败吞异常返回 false（不阻塞 PAID 终态）+ 派发告警（G3 错误传播分级 P1-MA2-048）。
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
            dispatchFailureAlert(salary, "发放", e);
            return false;
        }
    }

    /**
     * per-businessType 已过账凭证存在性反查（D3 去重守卫）：(billCode, businessType) 查
     * {@link ErpFinVoucherBillR} 回链，关联凭证 POSTED 且未冲销即视为已过账。镜像引擎
     * {@code ErpFinPostingProcessor.alreadyPosted}（已冲销凭证不视为命中——同 billCode 允许重过账）。
     */
    protected boolean alreadyPosted(String billCode, ErpFinBusinessType businessType) {
        IEntityDao<ErpFinVoucherBillR> linkDao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("billCode", billCode));
        q.addFilter(eq("businessType", businessType.name()));
        List<ErpFinVoucherBillR> links = linkDao.findAllByQuery(q);
        if (links.isEmpty()) {
            return false;
        }
        IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
        for (ErpFinVoucherBillR link : links) {
            ErpFinVoucher voucher = voucherDao.getEntityById(link.getVoucherId());
            if (voucher != null && ErpFinConstants.VOUCHER_STATUS_POSTED.equals(voucher.getDocStatus())
                    && !Boolean.TRUE.equals(voucher.getIsReversed())) {
                return true;
            }
        }
        return false;
    }

    /** 薪酬过账失败告警派发（G3；通知失败降级不阻断主流程）。 */
    protected void dispatchFailureAlert(ErpHrSalary salary, String stage, Exception cause) {
        if (notificationBiz == null) {
            return;
        }
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("salaryId", salary.getId());
        ctx.put("employeeId", salary.getEmployeeId());
        ctx.put("year", salary.getYear());
        ctx.put("month", salary.getMonth());
        ctx.put("stage", stage);
        ctx.put("errorCode", cause instanceof NopException ? ((NopException) cause).getErrorCode() : cause.getClass().getName());
        ctx.put("errorMessage", cause.getMessage());
        ctx.put("postingNo", buildBillCode(salary));
        IServiceContext serviceCtx = new ServiceContextImpl();
        try {
            notificationBiz.notify(NOTIFY_EVENT_SALARY_FAILURE, ctx, serviceCtx);
        } catch (Exception notifyErr) {
            LOG.warn("薪酬过账失败告警派发失败（降级）：salaryId={}, reason={}",
                    salary.getId(), notifyErr.getMessage());
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
    /**
     * 组装公司承担（290/300）计提事件（D1）。billData 独立构建：GROSS_AMOUNT = ER 金额
     * （createFacts 消费键）+ 专用 ER 键同值可追溯；**不带 NET_AMOUNT**——ER=0 时避免 createFacts
     * 回退读取净额（Reviewer B 阻断性发现，Phase 1 D1 实现约束）。借方科目不传（Provider 回退
     * 6601.01/6601.02 默认），贷方经 config 解析。
     */
    private PostingEvent buildCompanyBorneEvent(ErpHrSalary salary, ErpFinBusinessType type,
                                                String sourceBillType, String erBillDataKey,
                                                BigDecimal erAmount) {
        String creditCode = ErpHrConfigs.defaultPayrollSubjectCode();
        if (creditCode == null) {
            throw new NopException(ErpHrErrors.ERR_PAYROLL_SUBJECT_NOT_CONFIGURED)
                    .param(ErpHrErrors.ARG_SUBJECT_CODE, ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID);
        }
        PostingEvent event = new PostingEvent();
        event.setBusinessType(type);
        event.setBillHeadCode(buildBillCode(salary));
        applyOrgAndSchema(salary, event);
        event.setVoucherDate(salary.getPaymentDate() != null ? salary.getPaymentDate()
                : LocalDate.of(salary.getYear(), salary.getMonth(), 15));
        event.setExchangeRate(BigDecimal.ONE);

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(ErpHrConstants.BILL_DATA_SALARY_ID, salary.getId());
        billData.put(ErpHrConstants.BILL_DATA_EMPLOYEE_ID, salary.getEmployeeId());
        billData.put(ErpHrConstants.BILL_DATA_DEPARTMENT_ID, resolveDepartmentId(salary.getEmployeeId()));
        billData.put(ErpHrConstants.BILL_DATA_COST_CENTER_ID, resolveCostCenterId(salary.getEmployeeId()));
        billData.put(ErpHrConstants.BILL_DATA_GROSS_AMOUNT, erAmount);
        billData.put(erBillDataKey, erAmount);
        billData.put(ErpHrConstants.BILL_DATA_DEBIT_SUBJECT_CODE, null);
        billData.put(ErpHrConstants.BILL_DATA_CREDIT_SUBJECT_CODE, creditCode);
        billData.put(ErpHrConstants.BILL_DATA_SOURCE_BILL_TYPE, sourceBillType);
        event.setBillData(billData);
        return event;
    }

    /** approve 时重算公司承担社保（D1，镜像 PayrollCalculator:108-110 口径：social[1] + salaryRoundingScale）。 */
    private BigDecimal recomputeSocialInsuranceER(ErpHrSalary salary) {
        return socialInsuranceCalculator
                .calculate(salary.getEmployeeId(), salary.getYear(), salary.getMonth())[1]
                .setScale(ErpHrConfigs.salaryRoundingScale(), RoundingMode.HALF_UP);
    }

    /** approve 时重算公司承担公积金（D1，镜像 PayrollCalculator:113-115 口径：fund[1] + salaryRoundingScale）。 */
    private BigDecimal recomputeHousingFundER(ErpHrSalary salary) {
        return socialInsuranceCalculator
                .calculateHousingFund(salary.getEmployeeId(), salary.getYear(), salary.getMonth())[1]
                .setScale(ErpHrConfigs.salaryRoundingScale(), RoundingMode.HALF_UP);
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
        applyOrgAndSchema(salary, event);
        event.setVoucherDate(salary.getPaymentDate() != null ? salary.getPaymentDate()
                : LocalDate.of(salary.getYear(), salary.getMonth(), 15));
        event.setExchangeRate(BigDecimal.ONE);

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(ErpHrConstants.BILL_DATA_SALARY_ID, salary.getId());
        billData.put(ErpHrConstants.BILL_DATA_EMPLOYEE_ID, salary.getEmployeeId());
        billData.put(ErpHrConstants.BILL_DATA_DEPARTMENT_ID, resolveDepartmentId(salary.getEmployeeId()));
        billData.put(ErpHrConstants.BILL_DATA_COST_CENTER_ID, resolveCostCenterId(salary.getEmployeeId()));
        // GROSS_AMOUNT = createFacts 消费键，承载本事件金额（D1 原则）：270=应发（调用方传 gross）、
        // 280=实发（调用方传 net——银行实付口径；凭证实际生成前该键硬编码 gross 的缺陷不可观测，
        // RC-R1.89 激活生成后随死参数接线纠正）
        billData.put(ErpHrConstants.BILL_DATA_GROSS_AMOUNT, nz(amount));
        billData.put(ErpHrConstants.BILL_DATA_NET_AMOUNT, nz(salary.getNetSalary()));
        billData.put(ErpHrConstants.BILL_DATA_DEBIT_SUBJECT_CODE, debitCode);
        billData.put(ErpHrConstants.BILL_DATA_CREDIT_SUBJECT_CODE, creditCode);
        billData.put(ErpHrConstants.BILL_DATA_SOURCE_BILL_TYPE, sourceBillType);
        event.setBillData(billData);
        return event;
    }

    /**
     * 组织/账套/本位币解析（RC-R1.89 执行期发现补齐）：{@code PayrollCalculator} 不写 salary.orgId → 回退取
     * 员工 org；账套经 {@link AcctSchemaResolver} 按组织解析主账套（镜像 assets 域 Dispatcher 范式，
     * {@code DepreciationPostingDispatcher:149}）；币种取主账套本位币（ErpHrSalary 无币种列，薪酬以
     * 组织本位币计价）。无此前置，引擎 {@code resolveTargetSchemas} 对 null acctSchemaId 返回空集 →
     * 静默零凭证，且凭证行 currencyId 非空约束失败（270/280/290/300 共同前置，280 既有路径同患的
     * 潜伏缺口随本计划统一补齐——G3 吞异常使该缺口此前不可见）。
     */
    private void applyOrgAndSchema(ErpHrSalary salary, PostingEvent event) {
        Long orgId = salary.getOrgId() != null ? salary.getOrgId() : resolveEmployeeOrgId(salary.getEmployeeId());
        event.setOrgId(orgId);
        Long schemaId = AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);
        event.setAcctSchemaId(schemaId);
        event.setCurrencyId(resolveFunctionalCurrencyId(schemaId));
    }

    private Long resolveFunctionalCurrencyId(Long schemaId) {
        if (schemaId == null) {
            return null;
        }
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = dao.getEntityById(schemaId);
        return schema != null ? schema.getFunctionalCurrencyId() : null;
    }

    private Long resolveEmployeeOrgId(Long employeeId) {
        if (employeeId == null) {
            return null;
        }
        ErpHrEmployee emp = findEmployee(employeeId);
        return emp != null ? emp.getOrgId() : null;
    }

    private String buildBillCode(ErpHrSalary salary) {
        return "SAL-" + salary.getYear() + String.format("%02d", salary.getMonth()) + "-" + salary.getId();
    }

    private Long resolveDepartmentId(Long employeeId) {
        ErpHrEmployee emp = findEmployee(employeeId);
        return emp != null ? emp.getDepartmentId() : null;
    }

    private Long resolveCostCenterId(Long employeeId) {
        ErpHrEmployee emp = findEmployee(employeeId);
        return emp != null ? emp.getCostCenterId() : null;
    }

    private ErpHrEmployee findEmployee(Long employeeId) {
        if (employeeId == null) {
            return null;
        }
        IEntityDao<ErpHrEmployee> dao = daoProvider.daoFor(ErpHrEmployee.class);
        return dao.getEntityById(employeeId);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
