package app.erp.mnt.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.posting.AcctDocContext;
import app.erp.fin.service.posting.IErpFinAcctDocProvider;
import app.erp.fin.service.posting.VoucherFact;
import app.erp.mnt.service.ErpMntConstants;
import io.nop.api.core.config.AppConfig;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 维修备件消耗过账 Provider（maintenance 域，非默认 Provider，plan 2026-07-10-1100-6）。
 *
 * <p>支持业务类型：{@link ErpFinBusinessType#MAINTENANCE_ISSUE}。备件消耗凭证：
 * Dr: 维修费用 6602（config {@code erp-mnt.expense-subject-code}）/ Cr: 存货 1403（物料类别存货科目）。
 *
 * <p>与 assets 域 {@code MaintenanceExpenseAcctDocProvider}（MAINTENANCE_EXPENSE=470）互补不冲突：
 * assets 侧处理维修工单价值决策（费用化/资本化，关联工单时贷中转清算防双重）；maintenance 侧处理
 * 备件实物出库的 GL 对应（贷存货）。两者业务类型、触发源、借贷组合均不同（design/assets/maintenance.md §五边界）。
 *
 * <p>{@code PostingEvent.billData} 由 {@link MaintenanceIssuePostingDispatcher} 装配：
 * <ul>
 *   <li>{@code LINES}：{@code List<Map>} 每行含 {@code MATERIAL_CODE}、{@code MATERIAL_AMOUNT}、{@code INVENTORY_SUBJECT}</li>
 *   <li>{@code EQUIPMENT_CODE}：关联设备号（凭证摘要用，头级）</li>
 * </ul>
 *
 * <p>多物料消耗时，贷方按物料分别列出（各物料对应不同存货科目），借方汇总到维修费用。
 */
public class MaintenanceIssueAcctDocProvider implements IErpFinAcctDocProvider {

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;

    static final String KEY_LINES = "LINES";
    static final String KEY_EQUIPMENT_CODE = "EQUIPMENT_CODE";
    static final String KEY_MATERIAL_CODE = "MATERIAL_CODE";
    static final String KEY_MATERIAL_AMOUNT = "MATERIAL_AMOUNT";
    static final String KEY_INVENTORY_SUBJECT = "INVENTORY_SUBJECT";

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return Collections.unmodifiableSet(EnumSet.of(ErpFinBusinessType.MAINTENANCE_ISSUE));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        Map<String, Object> data = event.getBillData();
        String expenseSubject = resolveExpenseSubjectCode();
        String equipmentCode = (String) data.get(KEY_EQUIPMENT_CODE);

        List<VoucherFact> facts = new ArrayList<>();
        List<Map<String, Object>> lines = (List<Map<String, Object>>) data.get(KEY_LINES);
        if (lines == null || lines.isEmpty()) {
            return facts;
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Map<String, Object> line : lines) {
            BigDecimal lineAmount = readAmount(line.get(KEY_MATERIAL_AMOUNT));
            totalAmount = totalAmount.add(lineAmount);
            String invSubject = readString(line.get(KEY_INVENTORY_SUBJECT),
                    ErpMntConstants.DEFAULT_INVENTORY_SUBJECT_CODE);
            String materialCode = readString(line.get(KEY_MATERIAL_CODE), "");

            // 贷方：存货（按物料分列）
            facts.add(fact(invSubject, "存货", DC_CREDIT, lineAmount,
                    buildMemo(equipmentCode, materialCode), event));
        }

        // 借方：维修费用（汇总）
        facts.add(fact(expenseSubject, "维修费用", DC_DEBIT, totalAmount,
                buildMemo(equipmentCode, null), event));

        return facts;
    }

    private String resolveExpenseSubjectCode() {
        String code = AppConfig.var(ErpMntConstants.CONFIG_EXPENSE_SUBJECT_CODE,
                ErpMntConstants.DEFAULT_EXPENSE_SUBJECT_CODE);
        return code != null && !code.trim().isEmpty() ? code.trim() : ErpMntConstants.DEFAULT_EXPENSE_SUBJECT_CODE;
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

    private String buildMemo(String equipmentCode, String materialCode) {
        if (equipmentCode == null) {
            return "维修备件消耗";
        }
        return materialCode != null && !materialCode.isEmpty()
                ? "备件消耗（设备 " + equipmentCode + " / " + materialCode + "）"
                : "备件消耗（设备 " + equipmentCode + "）";
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
