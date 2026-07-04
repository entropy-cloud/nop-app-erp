package app.erp.log.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.posting.AcctDocContext;
import app.erp.fin.service.posting.IErpFinAcctDocProvider;
import app.erp.fin.service.posting.VoucherFact;
import app.erp.log.service.ErpLogConfigs;
import app.erp.log.service.ErpLogConstants;
import io.nop.api.core.config.AppConfig;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 物流产费过账 Provider（logistics 域，**非默认** Provider——Registry 中优先于默认 fallback）。
 *
 * <p>支持业务类型 {@link ErpFinBusinessType#FREIGHT}（3.18 path-1 销售运费）：
 * <ul>
 *   <li>{@code relatedBillType=SALES_DELIVERY}：借销售费用-运费科目 / 贷方按 {@code freightTerms}——
 *       {@code PREPAID}（运费预付）→ 银行存款，{@code COLLECT}（到付）→ 应付账款（partnerId=承运商 partnerId）。</li>
 * </ul>
 * 金额 = 运单 {@code freightAmount}。科目编码由引擎 {@code resolveSubjects} 按 code 解析。
 *
 * <p><b>path-2（PURCHASE_RECEIPT 采购运费）不在此 Provider 出凭证</b>：到岸成本分摊依赖 finance Landed Cost
 * 能力（{@code costing-methods.md:40} Deferred），本期仅事件交接，分摊归 finance 后续计划。
 */
public class LogisticsFreightProvider implements IErpFinAcctDocProvider {

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;

    static final String SUBJECT_BANK_DEPOSIT = "1002";   // 银行存款
    static final String SUBJECT_PAYABLE = "2202";        // 应付账款

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return Collections.singleton(ErpFinBusinessType.FREIGHT);
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        BigDecimal freightAmount = readDecimal(event, ErpLogConstants.BILL_DATA_FREIGHT_AMOUNT);
        String freightTerms = readString(event, ErpLogConstants.BILL_DATA_FREIGHT_TERMS,
                ErpLogConstants.FREIGHT_TERMS_PREPAID);
        String expenseSubject = AppConfig.var(ErpLogConfigs.CONFIG_SALES_FREIGHT_EXPENSE_SUBJECT,
                ErpLogConfigs.DEFAULT_SALES_FREIGHT_EXPENSE_SUBJECT);

        List<VoucherFact> facts = new ArrayList<>();
        // 借：销售费用-运费
        facts.add(fact(expenseSubject, "销售费用-运费", DC_DEBIT, freightAmount, event));

        // 贷方按 freightTerms：PREPAID→银行存款（已预付现金）；COLLECT→应付账款（欠承运商）
        boolean collect = Objects.equals(freightTerms, ErpLogConstants.FREIGHT_TERMS_COLLECT);
        String creditSubject = collect ? SUBJECT_PAYABLE : SUBJECT_BANK_DEPOSIT;
        String creditName = collect ? "应付账款" : "银行存款";
        VoucherFact credit = fact(creditSubject, creditName, DC_CREDIT, freightAmount, event);
        if (collect) {
            // 到付挂应付账款，携带承运商往来维度（partnerId = 承运商 partnerId）
            credit.setPartnerId(asLong(event.getBillData().get(ErpLogConstants.BILL_DATA_CARRIER_PARTNER_ID)));
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
