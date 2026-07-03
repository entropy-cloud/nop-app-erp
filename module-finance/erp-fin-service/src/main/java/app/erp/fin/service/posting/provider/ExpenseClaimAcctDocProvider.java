package app.erp.fin.service.posting.provider;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.posting.AcctDocContext;
import app.erp.fin.service.posting.IErpFinAcctDocProvider;
import app.erp.fin.service.posting.VoucherFact;
import java.util.Objects;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 费用报销业财过账 Provider（finance 域，非默认 Provider——Registry 中优先于默认 fallback）。
 *
 * <p>支持业务类型 {@link ErpFinBusinessType#EXPENSE_CLAIM}：借费用科目（不含税）/ 借进项税；
 * 贷方按 {@code paymentMode}（billData.{@code PAYMENT_MODE}）：{@code OWN_ACCOUNT}（员工垫付）→ 应付-员工科目，
 * {@code COMPANY_ACCOUNT}（公司直付）→ 银行存款科目。金额 = 本位币（amountFunctional，由 dispatcher 填入
 * TOTAL_AMOUNT_WITH_TAX/TOTAL_AMOUNT/TOTAL_TAX_AMOUNT）。科目编码由引擎 resolveSubjects 按 code 解析。
 *
 * <p>对应 {@code expense-claim.md §业财过账} EXPENSE_CLAIM 行；paymentMode 贷方路由对齐 Odoo
 * {@code _get_expense_account_destination}。
 */
public class ExpenseClaimAcctDocProvider implements IErpFinAcctDocProvider {

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;

    static final String SUBJECT_EXPENSE = "6602";        // 管理费用（费用科目，聚合口径）
    static final String SUBJECT_INPUT_VAT = "2221";      // 应交税费-进项税额
    static final String SUBJECT_PAYABLE_EMPLOYEE = "2241"; // 其他应付款-员工（应付-员工）
    static final String SUBJECT_BANK_DEPOSIT = "1002";   // 银行存款

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return Collections.singleton(ErpFinBusinessType.EXPENSE_CLAIM);
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        BigDecimal amountWithoutTax = readDecimal(event, ErpFinConstants.BILL_DATA_TOTAL_AMOUNT);
        BigDecimal tax = readDecimal(event, ErpFinConstants.BILL_DATA_TOTAL_TAX_AMOUNT);
        BigDecimal withTax = readDecimal(event, ErpFinConstants.BILL_DATA_TOTAL_AMOUNT_WITH_TAX);
        String paymentMode = readString(event, ErpFinConstants.BILL_DATA_PAYMENT_MODE,
                ErpFinConstants.PAYMENT_MODE_OWN_ACCOUNT);

        List<VoucherFact> facts = new ArrayList<>();
        facts.add(fact(SUBJECT_EXPENSE, "管理费用", DC_DEBIT, amountWithoutTax, event));
        facts.add(fact(SUBJECT_INPUT_VAT, "应交税费-进项税额", DC_DEBIT, tax, event));

        String creditSubject = Objects.equals(paymentMode, ErpFinConstants.PAYMENT_MODE_COMPANY_ACCOUNT)
                ? SUBJECT_BANK_DEPOSIT : SUBJECT_PAYABLE_EMPLOYEE;
        String creditName = Objects.equals(paymentMode, ErpFinConstants.PAYMENT_MODE_COMPANY_ACCOUNT)
                ? "银行存款" : "其他应付款-员工";
        VoucherFact credit = fact(creditSubject, creditName, DC_CREDIT, withTax, event);
        // 员工垫付挂应付-员工，携带往来维度（partnerId = employee.partnerId），便于辅助账与余额归集。
        if (Objects.equals(paymentMode, ErpFinConstants.PAYMENT_MODE_OWN_ACCOUNT)) {
            credit.setPartnerId(asLong(event.getBillData().get(ErpFinConstants.BILL_DATA_EMPLOYEE_ID)));
        }
        facts.add(credit);
        return facts;
    }

    private VoucherFact fact(String subjectCode, String subjectName, String dcDirection, BigDecimal amount,
                             PostingEvent event) {
        VoucherFact fact = new VoucherFact();
        fact.setSubjectCode(subjectCode);
        fact.setSubjectName(subjectName);
        fact.setDcDirection(dcDirection);
        fact.setAmount(amount);
        fact.setBusinessType(event.getBusinessType().name());
        fact.setMemo(event.getBillHeadCode());
        return fact;
    }

    private BigDecimal readDecimal(PostingEvent event, String key) {
        Object value = event.getBillData().get(key);
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.toString().trim());
    }

    private String readString(PostingEvent event, String key, String defaultValue) {
        Object value = event.getBillData().get(key);
        if (value == null) {
            return defaultValue;
        }
        String s = value.toString().trim();
        return s.isEmpty() ? defaultValue : s;
    }

    private Long asLong(Object value) {
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
}
