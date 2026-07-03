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
 * 应收票据业财过账 Provider（finance 域，{@code treasury.md §业财过账}）。支持 4 业务类型：
 * <ul>
 *   <li>{@code NOTES_RECEIVABLE_RECEIVED}：借应收票据 / 贷应收账款（抵客户欠款）。</li>
 *   <li>{@code NOTES_RECEIVABLE_DISCOUNTED}：科目分解五件套——借银行存款(netAmount)/借财务费用-利息支出(discountInterest)/
 *       [借/贷]汇兑损益(exchangeGainLoss)/贷应收票据(faceAmount)（借 Metasfresh {@code Doc_BankStatement} 科目分解范式）。</li>
 *   <li>{@code NOTES_RECEIVABLE_ENDORSED}：借应付账款（抵供应商）/贷应收票据。</li>
 *   <li>{@code NOTES_RECEIVABLE_COLLECTION}：借银行存款/贷应收票据（到期托收承兑）。</li>
 * </ul>
 *
 * <p>贴现息走「财务费用-利息支出」（不冲减应收票据，{@code treasury.md §规则2}）。金额取本位币（dispatcher 填入
 * billData）。科目编码由引擎 resolveSubjects 按 code 解析。
 */
public class NotesReceivableAcctDocProvider implements IErpFinAcctDocProvider {

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;

    static final String SUBJECT_NOTES_RECEIVABLE = "1121"; // 应收票据
    static final String SUBJECT_ACCOUNTS_RECEIVABLE = "1122"; // 应收账款
    static final String SUBJECT_ACCOUNTS_PAYABLE = "2202"; // 应付账款
    static final String SUBJECT_BANK_DEPOSIT = "1002"; // 银行存款
    static final String SUBJECT_FINANCIAL_EXPENSE_INTEREST = "6603"; // 财务费用-利息支出
    static final String SUBJECT_EXCHANGE_GAIN_LOSS = "6051"; // 汇兑损益

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return EnumSet.of(
                ErpFinBusinessType.NOTES_RECEIVABLE_RECEIVED,
                ErpFinBusinessType.NOTES_RECEIVABLE_DISCOUNTED,
                ErpFinBusinessType.NOTES_RECEIVABLE_ENDORSED,
                ErpFinBusinessType.NOTES_RECEIVABLE_COLLECTION);
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        Long partnerId = asLong(event.getBillData().get(ErpFinConstants.BILL_DATA_PARTNER_ID));
        List<VoucherFact> facts = new ArrayList<>();
        switch (event.getBusinessType()) {
            case NOTES_RECEIVABLE_RECEIVED: {
                BigDecimal face = readDecimal(event, ErpFinConstants.BILL_DATA_FACE_AMOUNT);
                facts.add(fact(SUBJECT_NOTES_RECEIVABLE, "应收票据", DC_DEBIT, face, event));
                VoucherFact cr = fact(SUBJECT_ACCOUNTS_RECEIVABLE, "应收账款", DC_CREDIT, face, event);
                cr.setPartnerId(partnerId);
                facts.add(cr);
                break;
            }
            case NOTES_RECEIVABLE_DISCOUNTED: {
                // 科目分解五件套
                BigDecimal face = readDecimal(event, ErpFinConstants.BILL_DATA_FACE_AMOUNT);
                BigDecimal discountInterest = readDecimal(event, ErpFinConstants.BILL_DATA_DISCOUNT_INTEREST);
                BigDecimal netAmount = readDecimal(event, ErpFinConstants.BILL_DATA_NET_AMOUNT);
                BigDecimal fx = readDecimal(event, ErpFinConstants.BILL_DATA_EXCHANGE_GAIN_LOSS);
                facts.add(fact(SUBJECT_BANK_DEPOSIT, "银行存款", DC_DEBIT, netAmount, event));
                facts.add(fact(SUBJECT_FINANCIAL_EXPENSE_INTEREST, "财务费用-利息支出", DC_DEBIT, discountInterest, event));
                if (fx.signum() != 0) {
                    if (fx.signum() > 0) {
                        facts.add(fact(SUBJECT_EXCHANGE_GAIN_LOSS, "汇兑损益", DC_DEBIT, fx, event));
                    } else {
                        facts.add(fact(SUBJECT_EXCHANGE_GAIN_LOSS, "汇兑损益", DC_CREDIT, fx.negate(), event));
                    }
                }
                facts.add(fact(SUBJECT_NOTES_RECEIVABLE, "应收票据", DC_CREDIT, face, event));
                break;
            }
            case NOTES_RECEIVABLE_ENDORSED: {
                BigDecimal face = readDecimal(event, ErpFinConstants.BILL_DATA_FACE_AMOUNT);
                VoucherFact dr = fact(SUBJECT_ACCOUNTS_PAYABLE, "应付账款", DC_DEBIT, face, event);
                dr.setPartnerId(partnerId);
                facts.add(dr);
                facts.add(fact(SUBJECT_NOTES_RECEIVABLE, "应收票据", DC_CREDIT, face, event));
                break;
            }
            case NOTES_RECEIVABLE_COLLECTION: {
                BigDecimal face = readDecimal(event, ErpFinConstants.BILL_DATA_FACE_AMOUNT);
                facts.add(fact(SUBJECT_BANK_DEPOSIT, "银行存款", DC_DEBIT, face, event));
                facts.add(fact(SUBJECT_NOTES_RECEIVABLE, "应收票据", DC_CREDIT, face, event));
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
