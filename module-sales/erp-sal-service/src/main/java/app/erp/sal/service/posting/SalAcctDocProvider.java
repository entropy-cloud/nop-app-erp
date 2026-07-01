package app.erp.sal.service.posting;

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
 * 销售业财过账 Provider（sales 域，**非默认** Provider——Registry 中优先于默认 fallback）。
 *
 * <p>支持业务类型（{@code posting.md} 映射）：
 * <ul>
 *   <li>{@link ErpFinBusinessType#AR_INVOICE}（销售发票：借应收 / 贷收入 / 贷销项税）。</li>
 *   <li>{@link ErpFinBusinessType#RECEIPT}（收款：借银行存款 / 贷应收）。</li>
 *   <li>{@link ErpFinBusinessType#SALES_RETURN}（销售退货冲减：反向 SALES_OUTPUT，借库存商品 / 贷主营业务成本，
 *       金额=退货成本 TOTAL_COST；SALES_OUTPUT 不记账应收/收入/销项税，故本凭证只冲成本/存货）。</li>
 * </ul>
 *
 * <p>科目编码（subjectCode）由引擎 {@code resolveSubjects} 按 code 解析为主数据科目。金额取自 PostingEvent.billData：
 * <ul>
 *   <li>AR_INVOICE：借 1131 应收账款(TOTAL_AMOUNT_WITH_TAX 价税合计) / 贷 6001 主营业务收入(TOTAL_AMOUNT 不含税) /
 *       贷 2221 应交税费-销项税额(TOTAL_TAX_AMOUNT)。</li>
 *   <li>RECEIPT：借 1002 银行存款(TOTAL) / 贷 1131 应收账款(TOTAL)。</li>
 *   <li>SALES_RETURN：借 1401 库存商品(TOTAL_COST) / 贷 6401 主营业务成本(TOTAL_COST)
 *       —— 反向 InvAcctDocProvider.SALES_OUTPUT（借 6401 / 贷 1401），同成本口径。</li>
 * </ul>
 * 同类集中到单个 Provider（与采购域 PurAcctDocProvider 一致，见 Phase 2 Decision (a)）。
 */
public class SalAcctDocProvider implements IErpFinAcctDocProvider {

    static final int DC_DEBIT = 10;
    static final int DC_CREDIT = 20;

    static final String SUBJECT_ACCOUNTS_RECEIVABLE = "1131"; // 应收账款
    static final String SUBJECT_REVENUE = "6001";             // 主营业务收入
    static final String SUBJECT_OUTPUT_VAT = "2221";          // 应交税费-销项税额
    static final String SUBJECT_BANK_DEPOSIT = "1002";        // 银行存款
    static final String SUBJECT_INVENTORY = "1401";           // 库存商品（与 InvAcctDocProvider.SALES_OUTPUT 同口径）
    static final String SUBJECT_COGS = "6401";                // 主营业务成本

    static final String KEY_TOTAL_AMOUNT = "TOTAL_AMOUNT";
    static final String KEY_TOTAL_TAX_AMOUNT = "TOTAL_TAX_AMOUNT";
    static final String KEY_TOTAL_AMOUNT_WITH_TAX = "TOTAL_AMOUNT_WITH_TAX";
    static final String KEY_TOTAL = "TOTAL";
    static final String KEY_TOTAL_COST = "TOTAL_COST";

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return EnumSet.of(ErpFinBusinessType.AR_INVOICE, ErpFinBusinessType.RECEIPT,
                ErpFinBusinessType.SALES_RETURN);
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        List<VoucherFact> facts = new ArrayList<>();
        if (event.getBusinessType() == ErpFinBusinessType.AR_INVOICE) {
            BigDecimal amount = readDecimal(event, KEY_TOTAL_AMOUNT);
            BigDecimal tax = readDecimal(event, KEY_TOTAL_TAX_AMOUNT);
            BigDecimal withTax = readDecimal(event, KEY_TOTAL_AMOUNT_WITH_TAX);
            facts.add(fact(SUBJECT_ACCOUNTS_RECEIVABLE, "应收账款", DC_DEBIT, withTax, event));
            facts.add(fact(SUBJECT_REVENUE, "主营业务收入", DC_CREDIT, amount, event));
            facts.add(fact(SUBJECT_OUTPUT_VAT, "应交税费-销项税额", DC_CREDIT, tax, event));
        } else if (event.getBusinessType() == ErpFinBusinessType.SALES_RETURN) {
            // 反向 SALES_OUTPUT：借库存商品 / 贷主营业务成本（对齐 InvAcctDocProvider.SALES_OUTPUT 的 6401/1401）
            BigDecimal totalCost = readDecimal(event, KEY_TOTAL_COST);
            facts.add(fact(SUBJECT_INVENTORY, "库存商品", DC_DEBIT, totalCost, event));
            facts.add(fact(SUBJECT_COGS, "主营业务成本", DC_CREDIT, totalCost, event));
        } else { // RECEIPT
            BigDecimal total = readDecimal(event, KEY_TOTAL);
            facts.add(fact(SUBJECT_BANK_DEPOSIT, "银行存款", DC_DEBIT, total, event));
            facts.add(fact(SUBJECT_ACCOUNTS_RECEIVABLE, "应收账款", DC_CREDIT, total, event));
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
