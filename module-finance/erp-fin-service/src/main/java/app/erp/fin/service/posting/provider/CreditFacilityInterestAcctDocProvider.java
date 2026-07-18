package app.erp.fin.service.posting.provider;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.posting.AcctDocContext;
import app.erp.fin.service.posting.IErpFinAcctDocProvider;
import app.erp.fin.service.posting.VoucherFact;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 授信利息计提业财过账 Provider（finance 域，{@code treasury.md §业财过账 CREDIT_FACILITY_INTEREST}）。
 *
 * <p>支持业务类型：{@link ErpFinBusinessType#CREDIT_FACILITY_INTEREST}。
 *
 * <p>凭证分录（owner doc {@code treasury.md:148}）：
 * <ul>
 *   <li>Dr {@code 6603} 财务费用-利息支出（amount 来自 billData.TOTAL）</li>
 *   <li>Cr {@code 1002} 银行存款（amount 同上，partnerId=null——Dr/Cr 均不设 partnerId）</li>
 * </ul>
 *
 * <p>镜像 {@code EmployeeAdvanceAcctDocProvider}/{@code NotesPayableAcctDocProvider} 范式：
 * {@code readDecimal(event, "TOTAL")} 读利息金额；{@code fact()} helper 构造 {@link VoucherFact}。
 */
public class CreditFacilityInterestAcctDocProvider implements IErpFinAcctDocProvider {

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;

    static final String SUBJECT_FINANCIAL_EXPENSE_INTEREST = "6603"; // 财务费用-利息支出
    static final String SUBJECT_BANK_DEPOSIT = "1002";                // 银行存款

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return EnumSet.of(ErpFinBusinessType.CREDIT_FACILITY_INTEREST);
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        if (event.getBusinessType() != ErpFinBusinessType.CREDIT_FACILITY_INTEREST) {
            return Collections.emptyList();
        }
        BigDecimal amount = readDecimal(event, "TOTAL");
        List<VoucherFact> facts = new ArrayList<>();
        facts.add(fact(SUBJECT_FINANCIAL_EXPENSE_INTEREST, "财务费用-利息支出", DC_DEBIT, amount, event));
        facts.add(fact(SUBJECT_BANK_DEPOSIT, "银行存款", DC_CREDIT, amount, event));
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
}
