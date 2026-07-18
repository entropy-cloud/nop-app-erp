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
 * 维修工时费用化过账 Provider（maintenance 域，plan 2026-07-18-0949-1）。
 *
 * <p>支持业务类型：{@link ErpFinBusinessType#MAINTENANCE_LABOR}。工时费用化凭证：
 * Dr: 折旧费用 6602（config {@code erp-mnt.expense-subject-code}，种子 subjectName 实测为「折旧费用」）
 * / Cr: 应付职工薪酬 2211（config {@code erp-mnt.payable-subject-code}）。
 *
 * <p>Phase 1 Decision (a) 裁决：Dr 6602 / Cr 2211——权责发生制语义正确（工时本质员工薪酬计提），
 * 与 projects 域 0742-2 工时贷方 + HR 域 0215-3 SALARY 贷方跨域一致；零种子 CSV 加性追加
 * （2211 已由 0742-2 加性追加至种子）。
 *
 * <p>与 {@code MaintenanceIssueAcctDocProvider}（MAINTENANCE_ISSUE=492）互补不冲突：备件消耗贷存货 1403
 * （实物出库 GL 对应），工时费用化贷应付职工薪酬 2211（权责计提）；两者业务类型、触发源、借贷组合均不同。
 *
 * <p>{@code PostingEvent.billData} 由 {@link MaintenanceLaborPostingDispatcher} 装配：
 * <ul>
 *   <li>{@code TOTAL}：工时成本（totalMinutes × hourlyRate / 60，HALF_UP scale=4）</li>
 *   <li>{@code EQUIPMENT_CODE}：关联设备号（凭证摘要用）</li>
 *   <li>{@code TOTAL_MINUTES}：工时分钟数</li>
 *   <li>{@code HOURLY_RATE}：小时费率</li>
 *   <li>{@code VISIT_CODE}：访问编码</li>
 * </ul>
 */
public class MaintenanceLaborAcctDocProvider implements IErpFinAcctDocProvider {

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;

    static final String KEY_TOTAL = "TOTAL";
    static final String KEY_EQUIPMENT_CODE = "EQUIPMENT_CODE";
    static final String KEY_TOTAL_MINUTES = "TOTAL_MINUTES";
    static final String KEY_HOURLY_RATE = "HOURLY_RATE";
    static final String KEY_VISIT_CODE = "VISIT_CODE";

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return Collections.unmodifiableSet(EnumSet.of(ErpFinBusinessType.MAINTENANCE_LABOR));
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        Map<String, Object> data = event.getBillData();
        String expenseSubject = resolveExpenseSubjectCode();
        String payableSubject = resolvePayableSubjectCode();
        String equipmentCode = (String) data.get(KEY_EQUIPMENT_CODE);
        String visitCode = (String) data.get(KEY_VISIT_CODE);

        BigDecimal amount = readAmount(data.get(KEY_TOTAL));
        List<VoucherFact> facts = new ArrayList<>();
        if (amount == null || amount.signum() <= 0) {
            return facts;
        }

        String memo = buildMemo(equipmentCode, visitCode);

        // 借方：维修费用（科目编码 6602，种子 subjectName 经实测为「折旧费用」）
        facts.add(fact(expenseSubject, "维修费用", DC_DEBIT, amount, memo, event));

        // 贷方：应付职工薪酬（科目编码 2211，权责发生制员工薪酬计提）
        facts.add(fact(payableSubject, "应付职工薪酬", DC_CREDIT, amount, memo, event));

        return facts;
    }

    private String resolveExpenseSubjectCode() {
        String code = AppConfig.var(ErpMntConstants.CONFIG_EXPENSE_SUBJECT_CODE,
                ErpMntConstants.DEFAULT_EXPENSE_SUBJECT_CODE);
        return code != null && !code.trim().isEmpty() ? code.trim() : ErpMntConstants.DEFAULT_EXPENSE_SUBJECT_CODE;
    }

    private String resolvePayableSubjectCode() {
        String code = AppConfig.var(ErpMntConstants.CONFIG_LABOR_PAYABLE_SUBJECT_CODE,
                ErpMntConstants.DEFAULT_LABOR_PAYABLE_SUBJECT_CODE);
        return code != null && !code.trim().isEmpty() ? code.trim() : ErpMntConstants.DEFAULT_LABOR_PAYABLE_SUBJECT_CODE;
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

    private String buildMemo(String equipmentCode, String visitCode) {
        StringBuilder sb = new StringBuilder("维修工时费用化");
        if (visitCode != null && !visitCode.isEmpty()) {
            sb.append("（访问 ").append(visitCode);
            if (equipmentCode != null && !equipmentCode.isEmpty()) {
                sb.append(" / 设备 ").append(equipmentCode);
            }
            sb.append("）");
        } else if (equipmentCode != null && !equipmentCode.isEmpty()) {
            sb.append("（设备 ").append(equipmentCode).append("）");
        }
        return sb.toString();
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
