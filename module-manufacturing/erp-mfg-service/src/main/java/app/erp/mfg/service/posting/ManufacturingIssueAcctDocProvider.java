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
 * 生产领料出库过账 Provider（manufacturing 域，非默认 Provider）。
 *
 * <p>支持业务类型：{@link ErpFinBusinessType#MANUFACTURING_ISSUE}（plan 2026-07-10-1100-5）。
 * 领料出库凭证：Dr: WIP 在制品 1411（config {@code erp-mfg.wip-subject-code}）/ Cr: 原材料存货 1401。
 *
 * <p>{@code PostingEvent.billData} 由 {@link ManufacturingIssuePostingDispatcher} 装配：
 * <ul>
 *   <li>{@code LINES}：{@code List<Map>} 每行含 {@code MATERIAL_CODE}、{@code MATERIAL_COST}、{@code INVENTORY_SUBJECT}</li>
 *   <li>{@code WORKORDER_CODE}：领料来源工单号（凭证摘要用）</li>
 * </ul>
 *
 * <p>多物料领料时，贷方按物料分别列出（各物料对应不同存货科目），借方汇总到 WIP。
 */
public class ManufacturingIssueAcctDocProvider implements IErpFinAcctDocProvider {

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;

    static final String SUBJECT_INVENTORY = "1401";

    static final String KEY_LINES = "LINES";
    static final String KEY_WORKORDER_CODE = "WORKORDER_CODE";
    static final String KEY_MATERIAL_CODE = "MATERIAL_CODE";
    static final String KEY_MATERIAL_COST = "MATERIAL_COST";
    static final String KEY_INVENTORY_SUBJECT = "INVENTORY_SUBJECT";

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return Collections.unmodifiableSet(EnumSet.of(ErpFinBusinessType.MANUFACTURING_ISSUE));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        Map<String, Object> data = event.getBillData();
        String wipSubject = resolveWipSubjectCode();
        String workOrderCode = (String) data.get(KEY_WORKORDER_CODE);

        List<VoucherFact> facts = new ArrayList<>();
        List<Map<String, Object>> lines = (List<Map<String, Object>>) data.get(KEY_LINES);
        if (lines == null || lines.isEmpty()) {
            return facts;
        }

        BigDecimal totalCost = BigDecimal.ZERO;
        for (Map<String, Object> line : lines) {
            BigDecimal lineCost = readAmount(line.get(KEY_MATERIAL_COST));
            totalCost = totalCost.add(lineCost);
            String invSubject = readString(line.get(KEY_INVENTORY_SUBJECT), SUBJECT_INVENTORY);
            String materialCode = readString(line.get(KEY_MATERIAL_CODE), "");

            // 贷方：原材料存货（按物料分列）
            facts.add(fact(invSubject, "原材料存货", DC_CREDIT, lineCost,
                    buildMemo(workOrderCode, materialCode), event));
        }

        // 借方：WIP 在制品（汇总）
        facts.add(fact(wipSubject, "在制品-WIP", DC_DEBIT, totalCost,
                buildMemo(workOrderCode, null), event));

        return facts;
    }

    private String resolveWipSubjectCode() {
        String code = AppConfig.var(ErpMfgConstants.CONFIG_WIP_SUBJECT_CODE,
                ErpMfgConstants.DEFAULT_WIP_SUBJECT_CODE);
        return code != null && !code.trim().isEmpty() ? code.trim() : ErpMfgConstants.DEFAULT_WIP_SUBJECT_CODE;
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

    private String buildMemo(String workOrderCode, String materialCode) {
        if (workOrderCode == null) {
            return "生产领料";
        }
        return materialCode != null && !materialCode.isEmpty()
                ? "领料（工单 " + workOrderCode + " / " + materialCode + "）"
                : "领料（工单 " + workOrderCode + "）";
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
