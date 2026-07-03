package app.erp.pur.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.service.ErpFinConstants;
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
 *   <li>{@link ErpFinBusinessType#PURCHASE_RETURN}（采购退货冲减：反向 PURCHASE_INPUT，借暂估应付 / 贷存货，
 *       金额=退货 totalAmount 不含税；进项税属已开票红字发票 Non-Goal）。</li>
 * </ul>
 *
 * <p>科目编码（subjectCode）由引擎 {@code resolveSubjects} 按 code 解析为主数据科目。金额取自 PostingEvent.billData：
 * <ul>
 *   <li>AP_INVOICE：借 1403 在途物资(TOTAL_AMOUNT 不含税) + 借 2221 应交税费-进项税(TOTAL_TAX_AMOUNT) /
 *       贷 2202 应付账款(TOTAL_AMOUNT_WITH_TAX 价税合计)。</li>
 *   <li>PAYMENT：借 2202 应付账款(TOTAL) / 贷 1002 银行存款(TOTAL)。</li>
 *   <li>PURCHASE_RETURN：借 2202 应付账款-暂估(TOTAL_AMOUNT) / 贷 1401 库存商品(TOTAL_AMOUNT)
 *       —— 反向 InvAcctDocProvider.PURCHASE_INPUT（借 1401 / 贷 2202），同口径不含税。</li>
 * </ul>
 * 同类集中到单个 Provider，减少 Bean 数（见 Phase 2 Decision）。
 */
public class PurAcctDocProvider implements IErpFinAcctDocProvider {

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;

    static final String SUBJECT_PURCHASE = "1403";        // 在途物资/采购
    static final String SUBJECT_INPUT_VAT = "2221";       // 应交税费-进项税额
    static final String SUBJECT_ACCOUNTS_PAYABLE = "2202"; // 应付账款
    static final String SUBJECT_BANK_DEPOSIT = "1002";    // 银行存款
    static final String SUBJECT_INVENTORY = "1401";       // 库存商品（与 InvAcctDocProvider.PURCHASE_INPUT 同口径）

    static final String KEY_TOTAL_AMOUNT = "TOTAL_AMOUNT";
    static final String KEY_TOTAL_TAX_AMOUNT = "TOTAL_TAX_AMOUNT";
    static final String KEY_TOTAL_AMOUNT_WITH_TAX = "TOTAL_AMOUNT_WITH_TAX";
    static final String KEY_TOTAL = "TOTAL";

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return EnumSet.of(ErpFinBusinessType.AP_INVOICE, ErpFinBusinessType.PAYMENT,
                ErpFinBusinessType.PURCHASE_RETURN);
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
        } else if (event.getBusinessType() == ErpFinBusinessType.PURCHASE_RETURN) {
            // 反向 PURCHASE_INPUT：借暂估应付 / 贷存货（不含税，对齐 InvAcctDocProvider.PURCHASE_INPUT 的 1401/2202）
            BigDecimal amount = readDecimal(event, KEY_TOTAL_AMOUNT);
            facts.add(fact(SUBJECT_ACCOUNTS_PAYABLE, "应付账款-暂估", DC_DEBIT, amount, event));
            facts.add(fact(SUBJECT_INVENTORY, "库存商品", DC_CREDIT, amount, event));
        } else { // PAYMENT
            BigDecimal total = readDecimal(event, KEY_TOTAL);
            facts.add(fact(SUBJECT_ACCOUNTS_PAYABLE, "应付账款", DC_DEBIT, total, event));
            facts.add(fact(SUBJECT_BANK_DEPOSIT, "银行存款", DC_CREDIT, total, event));
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
}
