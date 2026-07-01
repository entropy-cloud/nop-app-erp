package app.erp.pur.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.service.posting.AcctDocContext;
import app.erp.fin.service.posting.IErpFinAcctDocProvider;
import app.erp.fin.service.posting.VoucherFact;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 采购业财过账 Provider（purchase 域，**非默认** Provider——Registry 中优先于默认 fallback）。
 *
 * <p>支持业务类型（{@code posting.md} 映射）：
 * <ul>
 *   <li>{@link ErpFinBusinessType#AP_INVOICE}（采购发票：借费用/采购 + 借进项税 / 贷应付）。</li>
 *   <li>{@link ErpFinBusinessType#PAYMENT}（付款：借应付 / 贷银行存款）。</li>
 * </ul>
 *
 * <p>科目编码（subjectCode）由引擎 {@code resolveSubjects} 按 code 解析为主数据科目。金额取自 PostingEvent.billData：
 * <ul>
 *   <li>AP_INVOICE：借 1403 在途物资(TOTAL_AMOUNT 不含税) + 借 2221 应交税费-进项税(TOTAL_TAX_AMOUNT) /
 *       贷 2202 应付账款(TOTAL_AMOUNT_WITH_TAX 价税合计)。</li>
 *   <li>PAYMENT：借 2202 应付账款(TOTAL) / 贷 1002 银行存款(TOTAL)。</li>
 * </ul>
 * 同类集中到单个 Provider，减少 Bean 数（见 Phase 2 Decision）。
 */
public class PurAcctDocProvider implements IErpFinAcctDocProvider {

    static final int DC_DEBIT = 10;
    static final int DC_CREDIT = 20;

    static final String SUBJECT_PURCHASE = "1403";        // 在途物资/采购
    static final String SUBJECT_INPUT_VAT = "2221";       // 应交税费-进项税额
    static final String SUBJECT_ACCOUNTS_PAYABLE = "2202"; // 应付账款
    static final String SUBJECT_BANK_DEPOSIT = "1002";    // 银行存款

    static final String KEY_TOTAL_AMOUNT = "TOTAL_AMOUNT";
    static final String KEY_TOTAL_TAX_AMOUNT = "TOTAL_TAX_AMOUNT";
    static final String KEY_TOTAL_AMOUNT_WITH_TAX = "TOTAL_AMOUNT_WITH_TAX";
    static final String KEY_TOTAL = "TOTAL";

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return EnumSet.of(ErpFinBusinessType.AP_INVOICE, ErpFinBusinessType.PAYMENT);
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        List<VoucherFact> facts = new ArrayList<>();
        if (event.getBusinessType() == ErpFinBusinessType.AP_INVOICE) {
            BigDecimal amount = readDecimal(event, KEY_TOTAL_AMOUNT);
            BigDecimal tax = readDecimal(event, KEY_TOTAL_TAX_AMOUNT);
            BigDecimal withTax = readDecimal(event, KEY_TOTAL_AMOUNT_WITH_TAX);
            facts.add(fact(SUBJECT_PURCHASE, "在途物资", DC_DEBIT, amount, event));
            facts.add(fact(SUBJECT_INPUT_VAT, "应交税费-进项税额", DC_DEBIT, tax, event));
            facts.add(fact(SUBJECT_ACCOUNTS_PAYABLE, "应付账款", DC_CREDIT, withTax, event));
        } else { // PAYMENT
            BigDecimal total = readDecimal(event, KEY_TOTAL);
            facts.add(fact(SUBJECT_ACCOUNTS_PAYABLE, "应付账款", DC_DEBIT, total, event));
            facts.add(fact(SUBJECT_BANK_DEPOSIT, "银行存款", DC_CREDIT, total, event));
        }
        return facts;
    }

    private VoucherFact fact(String subjectCode, String subjectName, int dcDirection, BigDecimal amount,
                             PostingEvent event) {
        VoucherFact fact = new VoucherFact();
        fact.setSubjectCode(subjectCode);
        fact.setSubjectName(subjectName);
        fact.setDcDirection(dcDirection);
        fact.setAmount(amount);
        fact.setBusinessType(event.getBusinessType().getCode());
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
}
