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
 * 应付票据业财过账 Provider（finance 域，{@code treasury.md §业财过账}）。支持 2 业务类型：
 * <ul>
 *   <li>{@code NOTES_PAYABLE_ISSUED}：借应付账款 / 贷应付票据（开出）。</li>
 *   <li>{@code NOTES_PAYABLE_HONORED}：借应付票据 / 贷银行存款（到期兑付）。</li>
 * </ul>
 *
 * <p>方向与应收票据对称：开出时贷应付票据（负债），到期兑付时借应付票据 / 贷银行存款。
 */
public class NotesPayableAcctDocProvider implements IErpFinAcctDocProvider {

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;

    static final String SUBJECT_ACCOUNTS_PAYABLE = "2202"; // 应付账款
    static final String SUBJECT_NOTES_PAYABLE = "2203"; // 应付票据
    static final String SUBJECT_BANK_DEPOSIT = "1002"; // 银行存款

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return EnumSet.of(
                ErpFinBusinessType.NOTES_PAYABLE_ISSUED,
                ErpFinBusinessType.NOTES_PAYABLE_HONORED);
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        Long partnerId = asLong(event.getBillData().get(ErpFinConstants.BILL_DATA_PARTNER_ID));
        BigDecimal face = readDecimal(event, ErpFinConstants.BILL_DATA_FACE_AMOUNT);
        List<VoucherFact> facts = new ArrayList<>();
        switch (event.getBusinessType()) {
            case NOTES_PAYABLE_ISSUED: {
                VoucherFact dr = fact(SUBJECT_ACCOUNTS_PAYABLE, "应付账款", DC_DEBIT, face, event);
                dr.setPartnerId(partnerId);
                facts.add(dr);
                facts.add(fact(SUBJECT_NOTES_PAYABLE, "应付票据", DC_CREDIT, face, event));
                break;
            }
            case NOTES_PAYABLE_HONORED: {
                facts.add(fact(SUBJECT_NOTES_PAYABLE, "应付票据", DC_DEBIT, face, event));
                facts.add(fact(SUBJECT_BANK_DEPOSIT, "银行存款", DC_CREDIT, face, event));
                break;
            }
            default:
                return Collections.emptyList();
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
