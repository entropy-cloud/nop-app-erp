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
 * 员工借款业财过账 Provider（finance 域，非默认 Provider）。同类集中处理两业务类型（对齐
 * {@code SalAcctDocProvider} 同类集中范式）：
 * <ul>
 *   <li>{@link ErpFinBusinessType#EMPLOYEE_ADVANCE}（借款审核）：借其他应收款-员工预支 / 贷银行存款，
 *       金额 = 票面（billData.TOTAL）。科目方向 = 其他应收（资产/借方），与报销（应付-员工，负债/贷方）相反，
 *       不污染应收应付余额（对齐 ERPNext {@code book_advance_payments_in_separate_party_account}）。</li>
 *   <li>{@link ErpFinBusinessType#EMPLOYEE_ADVANCE_SETTLE}（报销抵扣清算）：借应付-员工（净额）/ 贷其他应收款-员工预支（净额）。
 *       净额 = min(借款未还, 报销应付-员工)。由 {@code AdvanceOffsetOrchestrator} 在报销抵扣时触发。</li>
 * </ul>
 */
public class EmployeeAdvanceAcctDocProvider implements IErpFinAcctDocProvider {

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;

    static final String SUBJECT_RECEIVABLE_EMPLOYEE = "1221"; // 其他应收款-员工预支
    static final String SUBJECT_BANK_DEPOSIT = "1002";        // 银行存款
    static final String SUBJECT_PAYABLE_EMPLOYEE = "2241";    // 其他应付款-员工（应付-员工）

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return EnumSet.of(ErpFinBusinessType.EMPLOYEE_ADVANCE, ErpFinBusinessType.EMPLOYEE_ADVANCE_SETTLE);
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        List<VoucherFact> facts = new ArrayList<>();
        BigDecimal amount = readDecimal(event, "TOTAL");
        Long partnerId = asLong(event.getBillData().get(ErpFinConstants.BILL_DATA_EMPLOYEE_ID));

        if (event.getBusinessType() == ErpFinBusinessType.EMPLOYEE_ADVANCE) {
            VoucherFact debit = fact(SUBJECT_RECEIVABLE_EMPLOYEE, "其他应收款-员工预支", DC_DEBIT, amount, event);
            debit.setPartnerId(partnerId);
            facts.add(debit);
            facts.add(fact(SUBJECT_BANK_DEPOSIT, "银行存款", DC_CREDIT, amount, event));
        } else { // EMPLOYEE_ADVANCE_SETTLE
            VoucherFact debit = fact(SUBJECT_PAYABLE_EMPLOYEE, "其他应付款-员工", DC_DEBIT, amount, event);
            debit.setPartnerId(partnerId);
            facts.add(debit);
            VoucherFact credit = fact(SUBJECT_RECEIVABLE_EMPLOYEE, "其他应收款-员工预支", DC_CREDIT, amount, event);
            credit.setPartnerId(partnerId);
            facts.add(credit);
        }
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
