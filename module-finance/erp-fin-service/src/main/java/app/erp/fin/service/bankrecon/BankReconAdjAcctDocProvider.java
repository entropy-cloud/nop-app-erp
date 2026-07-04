package app.erp.fin.service.bankrecon;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.posting.AcctDocContext;
import app.erp.fin.service.posting.IErpFinAcctDocProvider;
import app.erp.fin.service.posting.VoucherFact;
import io.nop.api.core.exceptions.NopException;
import io.nop.commons.util.StringHelper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.erp.fin.service.posting.ErpFinPostingErrors;

/**
 * 银行对账未达账项调整凭证 Provider（业财过账可插拔扩展点，处理 {@link ErpFinBusinessType#BANK_RECON_ADJ}）。
 *
 * <p>由 {@code BankReconAdjustmentVoucherBuilder} 构造 {@link PostingEvent}，{@code billData} 携带：
 * <ul>
 *   <li>{@code SUBJECT_CODE} —— 资金账户对应科目编码（银行存款科目，单边借/贷按方向）。</li>
 *   <li>{@code ADJ_SUBJECT_CODE} —— 未达账项调整对方科目编码（由调用方按业务规则解析后传入）。</li>
 *   <li>{@code TOTAL_BANK_CREDIT} —— 银行已收（银行流水贷方合计）：企业应记 借银行存款 / 贷调整科目。</li>
 *   <li>{@code TOTAL_BANK_DEBIT} —— 银行已付（银行流水借方合计）：企业应记 借调整科目 / 贷银行存款。</li>
 * </ul>
 *
 * <p>产出 2 或 4 条 {@link VoucherFact}（每对借贷平衡，整体借贷平衡）。
 */
public class BankReconAdjAcctDocProvider implements IErpFinAcctDocProvider {

    private static final Set<ErpFinBusinessType> SUPPORTED = Collections.unmodifiableSet(
            EnumSet.of(ErpFinBusinessType.BANK_RECON_ADJ));

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return SUPPORTED;
    }

    @Override
    public boolean isFallback() {
        return false;
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        Map<String, Object> billData = event.getBillData();
        String bankSubject = stringValue(billData, ErpFinConstants.BILL_DATA_BANK_SUBJECT_CODE);
        String adjSubject = stringValue(billData, "ADJ_SUBJECT_CODE");
        BigDecimal bankCredit = decimalValue(billData, "TOTAL_BANK_CREDIT");
        BigDecimal bankDebit = decimalValue(billData, "TOTAL_BANK_DEBIT");

        List<VoucherFact> facts = new ArrayList<>(4);
        if (bankCredit.signum() > 0) {
            // 银行已收：企业 借银行存款 / 贷调整科目
            facts.add(fact(bankSubject, ErpFinConstants.DC_DEBIT, bankCredit, "BANK_RECV"));
            facts.add(fact(adjSubject, ErpFinConstants.DC_CREDIT, bankCredit, "ADJ_BANK_RECV"));
        }
        if (bankDebit.signum() > 0) {
            // 银行已付：企业 借调整科目 / 贷银行存款
            facts.add(fact(adjSubject, ErpFinConstants.DC_DEBIT, bankDebit, "ADJ_BANK_PAID"));
            facts.add(fact(bankSubject, ErpFinConstants.DC_CREDIT, bankDebit, "BANK_PAID"));
        }
        return facts;
    }

    protected VoucherFact fact(String subjectCode, String dc, BigDecimal amount, String accountKey) {
        VoucherFact f = new VoucherFact();
        f.setSubjectCode(subjectCode);
        f.setDcDirection(dc);
        f.setAmount(amount);
        f.setAccountKey(accountKey);
        f.setAmountKey(null);
        f.setMemo("银行对账未达账项调整");
        f.setBusinessType(ErpFinBusinessType.BANK_RECON_ADJ.name());
        return f;
    }

    protected String stringValue(Map<String, Object> billData, String key) {
        Object v = billData.get(key);
        if (v == null || StringHelper.isBlank(v.toString())) {
            throw new NopException(ErpFinPostingErrors.ERR_AMOUNT_KEY_NOT_RESOLVED)
                    .param(ErpFinPostingErrors.ARG_AMOUNT_KEY, key);
        }
        return v.toString().trim();
    }

    protected BigDecimal decimalValue(Map<String, Object> billData, String key) {
        Object v = billData.get(key);
        if (v == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(v.toString().trim());
        } catch (NumberFormatException e) {
            throw new NopException(ErpFinPostingErrors.ERR_AMOUNT_KEY_NOT_RESOLVED)
                    .param(ErpFinPostingErrors.ARG_AMOUNT_KEY, key).cause(e);
        }
    }
}
