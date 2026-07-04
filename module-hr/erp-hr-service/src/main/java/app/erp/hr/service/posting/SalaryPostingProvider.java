package app.erp.hr.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.posting.AcctDocContext;
import app.erp.fin.service.posting.IErpFinAcctDocProvider;
import app.erp.fin.service.posting.VoucherFact;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 薪酬业财过账 Provider（HR 域，非默认 Provider——Registry 中优先于默认 fallback）。
 *
 * <p>支持业务类型（payroll.md §9.1 科目映射）：
 * <ul>
 *   <li>{@link ErpFinBusinessType#SALARY}（计提）：借 管理费用-工资 / 贷 应付职工薪酬（{@code erp-hr.default-payroll-subject-id}，
 *       为空抛 {@link ErpHrErrors#ERR_PAYROLL_SUBJECT_NOT_CONFIGURED}）。</li>
 *   <li>{@link ErpFinBusinessType#SALARY_PAYMENT}（发放）：借 应付职工薪酬 / 贷 银行存款。</li>
 *   <li>{@link ErpFinBusinessType#SOCIAL_INSURANCE_ER}（社保公司承担）：借 管理费用-社保 / 贷 应付职工薪酬-社保。</li>
 *   <li>{@link ErpFinBusinessType#HOUSING_FUND_ER}（公积金公司承担）：借 管理费用-公积金 / 贷 应付职工薪酬-公积金。</li>
 * </ul>
 *
 * <p>触发：APPROVED_MANAGER 计提（{@link ErpHrConstants#SOURCE_BILL_TYPE_SALARY}），
 * PAID 发放（{@link ErpHrConstants#SOURCE_BILL_TYPE_SALARY_PAYMENT}）。
 * 借方科目取 billData[DEBIT_SUBJECT_CODE]，贷方取 billData[CREDIT_SUBJECT_CODE]，
 * 由 {@code SalaryPostingDispatcher} 解析后填入。分录行标 {@code departmentId}/{@code costCenterId} 辅助维度。
 */
public class SalaryPostingProvider implements IErpFinAcctDocProvider {

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;

    /** 应付职工薪酬默认贷方科目（计提时配置缺失的回退；正式要求配置 erp-hr.default-payroll-subject-id）。 */
    static final String SUBJECT_PAYROLL_DEFAULT = "2211";
    /** 管理费用-工资默认借方科目。 */
    static final String SUBJECT_EXPENSE_SALARY_DEFAULT = "6601";
    /** 管理费用-社保默认借方科目。 */
    static final String SUBJECT_EXPENSE_SOCIAL_DEFAULT = "6601.01";
    /** 管理费用-公积金默认借方科目。 */
    static final String SUBJECT_EXPENSE_FUND_DEFAULT = "6601.02";
    /** 银行存款默认贷方科目（发放）。 */
    static final String SUBJECT_BANK_DEFAULT = "1002";

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        Set<ErpFinBusinessType> types = new HashSet<>();
        types.add(ErpFinBusinessType.SALARY);
        types.add(ErpFinBusinessType.SALARY_PAYMENT);
        types.add(ErpFinBusinessType.SOCIAL_INSURANCE_ER);
        types.add(ErpFinBusinessType.HOUSING_FUND_ER);
        return types;
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        ErpFinBusinessType type = event.getBusinessType();
        BigDecimal amount = readDecimal(event, ErpHrConstants.BILL_DATA_GROSS_AMOUNT);
        if (amount == null || amount.signum() == 0) {
            amount = readDecimal(event, ErpHrConstants.BILL_DATA_NET_AMOUNT);
        }
        amount = nz(amount);
        Long departmentId = readLong(event, ErpHrConstants.BILL_DATA_DEPARTMENT_ID);
        Long costCenterId = readLong(event, ErpHrConstants.BILL_DATA_COST_CENTER_ID);
        String memo = buildMemo(event);

        String debitCode = readString(event, ErpHrConstants.BILL_DATA_DEBIT_SUBJECT_CODE);
        String creditCode = readString(event, ErpHrConstants.BILL_DATA_CREDIT_SUBJECT_CODE);

        List<VoucherFact> facts = new ArrayList<>(2);
        switch (type) {
            case SALARY:
                facts.add(fact(defaultIfBlank(debitCode, SUBJECT_EXPENSE_SALARY_DEFAULT),
                        "管理费用-工资", DC_DEBIT, amount, event, memo, departmentId, costCenterId));
                facts.add(fact(resolvePayrollCredit(creditCode),
                        "应付职工薪酬", DC_CREDIT, amount, event, memo, departmentId, costCenterId));
                break;
            case SALARY_PAYMENT:
                facts.add(fact(resolvePayrollCredit(debitCode),
                        "应付职工薪酬", DC_DEBIT, amount, event, memo, departmentId, costCenterId));
                facts.add(fact(defaultIfBlank(creditCode, SUBJECT_BANK_DEFAULT),
                        "银行存款", DC_CREDIT, amount, event, memo, departmentId, costCenterId));
                break;
            case SOCIAL_INSURANCE_ER:
                facts.add(fact(defaultIfBlank(debitCode, SUBJECT_EXPENSE_SOCIAL_DEFAULT),
                        "管理费用-社保", DC_DEBIT, amount, event, memo, departmentId, costCenterId));
                facts.add(fact(resolvePayrollCredit(creditCode),
                        "应付职工薪酬-社保", DC_CREDIT, amount, event, memo, departmentId, costCenterId));
                break;
            case HOUSING_FUND_ER:
                facts.add(fact(defaultIfBlank(debitCode, SUBJECT_EXPENSE_FUND_DEFAULT),
                        "管理费用-公积金", DC_DEBIT, amount, event, memo, departmentId, costCenterId));
                facts.add(fact(resolvePayrollCredit(creditCode),
                        "应付职工薪酬-公积金", DC_CREDIT, amount, event, memo, departmentId, costCenterId));
                break;
            default:
                return Collections.emptyList();
        }
        return facts;
    }

    /**
     * 应付职工薪酬贷方科目解析：billData 传入优先，否则回退默认。调用方（Dispatcher）
     * 在未配置 erp-hr.default-payroll-subject-id 时应已抛 ERR_PAYROLL_SUBJECT_NOT_CONFIGURED。
     */
    private String resolvePayrollCredit(String billDataValue) {
        return defaultIfBlank(billDataValue, SUBJECT_PAYROLL_DEFAULT);
    }

    private String buildMemo(PostingEvent event) {
        Object sourceType = event.getBillData().get(ErpHrConstants.BILL_DATA_SOURCE_BILL_TYPE);
        return (sourceType == null ? "" : sourceType) + ":" + event.getBillHeadCode();
    }

    private VoucherFact fact(String subjectCode, String subjectName, String dcDirection, BigDecimal amount,
                             PostingEvent event, String memo, Long departmentId, Long costCenterId) {
        VoucherFact f = new VoucherFact();
        f.setSubjectCode(subjectCode);
        f.setSubjectName(subjectName);
        f.setDcDirection(dcDirection);
        f.setAmount(amount);
        f.setBusinessType(event.getBusinessType().name());
        f.setMemo(memo);
        f.setDepartmentId(departmentId);
        f.setCostCenterId(costCenterId);
        return f;
    }

    private BigDecimal readDecimal(PostingEvent event, String key) {
        Object value = event.getBillData().get(key);
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value == null) {
            return null;
        }
        return new BigDecimal(value.toString().trim());
    }

    private String readString(PostingEvent event, String key) {
        Object value = event.getBillData().get(key);
        if (value == null) {
            return null;
        }
        String s = value.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private Long readLong(PostingEvent event, String key) {
        Object value = event.getBillData().get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.valueOf(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
