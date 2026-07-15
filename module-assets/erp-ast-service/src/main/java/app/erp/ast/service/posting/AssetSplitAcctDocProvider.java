package app.erp.ast.service.posting;

import app.erp.ast.service.ErpAstConstants;
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
import java.util.Map;
import java.util.Set;

/**
 * 资产拆分业财过账 Provider（assets 域）。支持 {@link ErpFinBusinessType#ASSET_SPLIT}。
 *
 * <p>从 billData 读取结构化借/贷明细（DEBIT_LINES / CREDIT_LINES），物化 VoucherFact：
 * 借 = 固定资产-新卡片合计（按 SplitLine 拆 N 行）；贷 = 固定资产-源卡片原值（单行）。
 *
 * <p>科目编码由派发器按资产类别配置解析后经 billData 逐行传入；未配置时回退标准科目编码 1601。
 */
public class AssetSplitAcctDocProvider implements IErpFinAcctDocProvider {

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;

    static final String SUBJECT_FIXED_ASSET = "1601";

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return EnumSet.of(ErpFinBusinessType.ASSET_SPLIT);
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        List<VoucherFact> facts = new ArrayList<>();
        for (Map<String, Object> row : readLines(event, ErpAstConstants.BILL_DATA_DEBIT_LINES)) {
            facts.add(fact(readCode(row, "1601"), readName(row, "固定资产"), DC_DEBIT, readAmount(row), event));
        }
        for (Map<String, Object> row : readLines(event, ErpAstConstants.BILL_DATA_CREDIT_LINES)) {
            facts.add(fact(readCode(row, "1601"), readName(row, "固定资产"), DC_CREDIT, readAmount(row), event));
        }
        return facts;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readLines(PostingEvent event, String key) {
        Object value = event.getBillData().get(key);
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return java.util.Collections.emptyList();
    }

    private VoucherFact fact(String subjectCode, String subjectName, String dcDirection, BigDecimal amount,
                             PostingEvent event) {
        VoucherFact fact = new VoucherFact();
        fact.setSubjectCode(subjectCode);
        fact.setSubjectName(subjectName);
        fact.setDcDirection(dcDirection);
        fact.setAmount(amount);
        fact.setBusinessType(event.getBusinessType().name());
        return fact;
    }

    private String readCode(Map<String, Object> row, String defaultValue) {
        Object value = row.get(ErpAstConstants.BILL_DATA_LINE_SUBJECT_CODE);
        if (value == null || value.toString().trim().isEmpty()) {
            return defaultValue;
        }
        return value.toString().trim();
    }

    private String readName(Map<String, Object> row, String defaultValue) {
        Object value = row.get(ErpAstConstants.BILL_DATA_LINE_SUBJECT_NAME);
        if (value == null || value.toString().trim().isEmpty()) {
            return defaultValue;
        }
        return value.toString().trim();
    }

    private BigDecimal readAmount(Map<String, Object> row) {
        Object value = row.get(ErpAstConstants.BILL_DATA_LINE_AMOUNT);
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.toString().trim());
    }
}
