package app.erp.mfg.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.posting.AcctDocContext;
import app.erp.fin.service.posting.IErpFinAcctDocProvider;
import app.erp.fin.service.posting.VoucherFact;
import app.erp.mfg.service.ErpMfgConstants;
import io.nop.api.core.config.AppConfig;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 委外加工费过账 Provider（manufacturing 域，非默认 Provider，plan 2026-07-13-0455-1 §Phase 3）。
 *
 * <p>支持业务类型：{@link ErpFinBusinessType#SUBCONTRACT_FEE}。
 * 委外加工费凭证：Dr: 委外物资 1408（config {@code erp-mfg.subcontract-subject-code}）/ Cr: 应付账款 2202。
 * 对齐设计 {@code subcontracting.md §成本分配规则}：加工费按发票金额分配，借委外物资/贷应付账款。
 */
public class SubcontractFeeAcctDocProvider implements IErpFinAcctDocProvider {

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;

    static final String KEY_SUBCONTRACT_CODE = "SUBCONTRACT_CODE";
    static final String KEY_PROCESSING_FEE = "PROCESSING_FEE";

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return Collections.unmodifiableSet(EnumSet.of(ErpFinBusinessType.SUBCONTRACT_FEE));
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        Map<String, Object> data = event.getBillData();
        String subcontractCode = (String) data.get(KEY_SUBCONTRACT_CODE);
        BigDecimal fee = readAmount(data.get(KEY_PROCESSING_FEE));
        String subcontractSubject = resolveSubcontractSubjectCode();

        List<VoucherFact> facts = new ArrayList<>();
        if (fee.signum() <= 0) {
            return facts;
        }

        String memo = subcontractCode != null ? "委外加工费（" + subcontractCode + "）" : "委外加工费";

        facts.add(fact(subcontractSubject, "委外物资", DC_DEBIT, fee, memo, event));
        facts.add(fact(ErpMfgConstants.SUBJECT_ACCOUNTS_PAYABLE, "应付账款", DC_CREDIT, fee, memo, event));
        return facts;
    }

    private String resolveSubcontractSubjectCode() {
        String code = AppConfig.var(ErpMfgConstants.CONFIG_SUBCONTRACT_SUBJECT_CODE,
                ErpMfgConstants.DEFAULT_SUBCONTRACT_SUBJECT_CODE);
        return code != null && !code.trim().isEmpty() ? code.trim() : ErpMfgConstants.DEFAULT_SUBCONTRACT_SUBJECT_CODE;
    }

    private VoucherFact fact(String subjectCode, String subjectName, String dcDirection,
                             BigDecimal amount, String memo, PostingEvent event) {
        VoucherFact fact = new VoucherFact();
        fact.setSubjectCode(subjectCode);
        fact.setSubjectName(subjectName);
        fact.setDcDirection(dcDirection);
        fact.setAmount(amount);
        fact.setBusinessType(event.getBusinessType().name());
        fact.setMemo(memo);
        return fact;
    }

    private BigDecimal readAmount(Object value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.toString().trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
