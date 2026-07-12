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
 * 委外收货过账 Provider（manufacturing 域，非默认 Provider，plan 2026-07-13-0455-1 §Phase 3）。
 *
 * <p>支持业务类型：{@link ErpFinBusinessType#SUBCONTRACT_RECEIPT}。
 * 委外成品入库凭证：Dr: 产成品 1405 / Cr: 委外物资 1408（config {@code erp-mfg.subcontract-subject-code}）。
 * 对齐设计 {@code subcontracting.md §成本分配规则}：成品入库结转委外物资。
 */
public class SubcontractReceiptAcctDocProvider implements IErpFinAcctDocProvider {

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;

    static final String KEY_LINES = "LINES";
    static final String KEY_SUBCONTRACT_CODE = "SUBCONTRACT_CODE";
    static final String KEY_MATERIAL_CODE = "MATERIAL_CODE";
    static final String KEY_FINISHED_COST = "FINISHED_COST";
    static final String KEY_FINISHED_SUBJECT = "FINISHED_SUBJECT";

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return Collections.unmodifiableSet(EnumSet.of(ErpFinBusinessType.SUBCONTRACT_RECEIPT));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        Map<String, Object> data = event.getBillData();
        String subcontractCode = (String) data.get(KEY_SUBCONTRACT_CODE);
        String subcontractSubject = resolveSubcontractSubjectCode();

        List<VoucherFact> facts = new ArrayList<>();
        List<Map<String, Object>> lines = (List<Map<String, Object>>) data.get(KEY_LINES);
        if (lines == null || lines.isEmpty()) {
            return facts;
        }

        BigDecimal totalCost = BigDecimal.ZERO;
        for (Map<String, Object> line : lines) {
            BigDecimal lineCost = readAmount(line.get(KEY_FINISHED_COST));
            totalCost = totalCost.add(lineCost);
            String finishedSubject = readString(line.get(KEY_FINISHED_SUBJECT),
                    ErpMfgConstants.SUBJECT_SUBCONTRACT_FINISHED_GOODS);
            String materialCode = readString(line.get(KEY_MATERIAL_CODE), "");

            facts.add(fact(finishedSubject, "产成品", DC_DEBIT, lineCost,
                    buildMemo(subcontractCode, materialCode), event));
        }

        facts.add(fact(subcontractSubject, "委外物资", DC_CREDIT, totalCost,
                buildMemo(subcontractCode, null), event));
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

    private String buildMemo(String subcontractCode, String materialCode) {
        if (subcontractCode == null) {
            return "委外收货";
        }
        return materialCode != null && !materialCode.isEmpty()
                ? "委外收货（" + subcontractCode + " / " + materialCode + "）"
                : "委外收货（" + subcontractCode + "）";
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

    private String readString(Object value, String defaultValue) {
        return value != null ? value.toString() : defaultValue;
    }
}
