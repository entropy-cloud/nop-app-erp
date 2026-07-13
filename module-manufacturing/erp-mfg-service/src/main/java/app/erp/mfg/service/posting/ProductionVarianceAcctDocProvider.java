package app.erp.mfg.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.posting.AcctDocContext;
import app.erp.fin.service.posting.IErpFinAcctDocProvider;
import app.erp.fin.service.posting.VoucherFact;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 生产差异过账 Provider（manufacturing 域，非默认 Provider——Registry 中优先于默认 fallback）。
 *
 * <p>支持业务类型：{@link ErpFinBusinessType#PRODUCTION_VARIANCE}（plan 2026-07-05-1838-2）。
 * 按成本要素汇总净差异入账（承接 PPV {@code PurchasePriceVarianceAcctDocProvider} 范式），
 * 差异类型作为 {@code ErpMfgCostVariance} 分析维度不入账。
 *
 * <p>{@code PostingEvent.billData} 由 {@link ProductionVarianceDispatcher} 装配：
 * <ul>
 *   <li>{@code MATERIAL_VARIANCE} / {@code LABOR_VARIANCE} / {@code OVERHEAD_VARIANCE}：各要素净差异金额（恒正）+
 *       {@code MATERIAL_DIRECTION} / {@code LABOR_DIRECTION} / {@code OVERHEAD_DIRECTION}（DEBIT=unfavorable 借差异 /
 *       CREDIT=favorable 贷差异，{@link #DIRECTION_DEBIT} / {@link #DIRECTION_CREDIT}）</li>
 *   <li>{@code WORKORDER_CODE}：差异来源工单号（凭证摘要用）</li>
 * </ul>
 *
 * <p>科目（沿用 PPV 范式 + 成本要素差异化科目）：
 * <ul>
 *   <li>材料用量差异：借/贷 {@link #SUBJECT_MATERIAL_VARIANCE}（1410 制造差异-材料）/
 *       贷/借 {@link #SUBJECT_WIP_MATERIAL}（1411 在制品-材料）</li>
 *   <li>人工差异：借/贷 {@link #SUBJECT_LABOR_VARIANCE}（1412 制造差异-人工）/
 *       贷/借 {@link #SUBJECT_WIP_LABOR}（1413 在制品-人工）</li>
 *   <li>制造费用差异：借/贷 {@link #SUBJECT_OVERHEAD_VARIANCE}（1414 制造差异-制造费用）/
 *       贷/借 {@link #SUBJECT_WIP_OVERHEAD}（1415 在制品-制造费用）</li>
 *   <li>委外费差异（plan 2026-07-14-0035-1）：借/贷 {@link #SUBJECT_SUBCONTRACT_VARIANCE}（1416 制造差异-委外）/
 *       贷/借 {@link #SUBJECT_WIP_SUBCONTRACT}（1417 在制品-委外）</li>
 * </ul>
 * unfavorable（实际>标准）→ 借差异科目 / 贷 WIP；favorable（实际<标准）→ 借 WIP / 贷差异科目。
 *
 * <p>权威：{@code docs/design/finance/posting.md}（差异过账承接 PPV）、
 * {@code docs/design/manufacturing/variance-analysis.md}。
 */
public class ProductionVarianceAcctDocProvider implements IErpFinAcctDocProvider {

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;

    static final String SUBJECT_MATERIAL_VARIANCE = "1410";
    static final String SUBJECT_WIP_MATERIAL = "1411";
    static final String SUBJECT_LABOR_VARIANCE = "1412";
    static final String SUBJECT_WIP_LABOR = "1413";
    static final String SUBJECT_OVERHEAD_VARIANCE = "1414";
    static final String SUBJECT_WIP_OVERHEAD = "1415";
    static final String SUBJECT_SUBCONTRACT_VARIANCE = "1416";
    static final String SUBJECT_WIP_SUBCONTRACT = "1417";

    static final String KEY_MATERIAL_VARIANCE = "MATERIAL_VARIANCE";
    static final String KEY_LABOR_VARIANCE = "LABOR_VARIANCE";
    static final String KEY_OVERHEAD_VARIANCE = "OVERHEAD_VARIANCE";
    static final String KEY_SUBCONTRACT_VARIANCE = "SUBCONTRACT_VARIANCE";
    static final String KEY_MATERIAL_DIRECTION = "MATERIAL_DIRECTION";
    static final String KEY_LABOR_DIRECTION = "LABOR_DIRECTION";
    static final String KEY_OVERHEAD_DIRECTION = "OVERHEAD_DIRECTION";
    static final String KEY_SUBCONTRACT_DIRECTION = "SUBCONTRACT_DIRECTION";
    static final String KEY_WORKORDER_CODE = "WORKORDER_CODE";

    static final String DIRECTION_DEBIT = "DEBIT";
    static final String DIRECTION_CREDIT = "CREDIT";

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return Collections.unmodifiableSet(EnumSet.of(ErpFinBusinessType.PRODUCTION_VARIANCE));
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        Map<String, Object> data = event.getBillData();
        List<VoucherFact> facts = new ArrayList<>();

        appendElementFacts(facts, data, KEY_MATERIAL_VARIANCE, KEY_MATERIAL_DIRECTION,
                SUBJECT_MATERIAL_VARIANCE, "制造差异-材料", SUBJECT_WIP_MATERIAL, "在制品-材料", event);
        appendElementFacts(facts, data, KEY_LABOR_VARIANCE, KEY_LABOR_DIRECTION,
                SUBJECT_LABOR_VARIANCE, "制造差异-人工", SUBJECT_WIP_LABOR, "在制品-人工", event);
        appendElementFacts(facts, data, KEY_OVERHEAD_VARIANCE, KEY_OVERHEAD_DIRECTION,
                SUBJECT_OVERHEAD_VARIANCE, "制造差异-制造费用", SUBJECT_WIP_OVERHEAD, "在制品-制造费用", event);
        appendElementFacts(facts, data, KEY_SUBCONTRACT_VARIANCE, KEY_SUBCONTRACT_DIRECTION,
                SUBJECT_SUBCONTRACT_VARIANCE, "制造差异-委外", SUBJECT_WIP_SUBCONTRACT, "在制品-委外", event);

        return facts;
    }

    private void appendElementFacts(List<VoucherFact> facts, Map<String, Object> data,
                                    String amountKey, String directionKey,
                                    String varianceSubject, String varianceSubjectName,
                                    String wipSubject, String wipSubjectName,
                                    PostingEvent event) {
        BigDecimal amount = readAmount(data.get(amountKey));
        if (amount == null || amount.signum() <= 0) {
            return;
        }
        String direction = readDirection(data.get(directionKey));
        String memo = buildMemo(data, varianceSubjectName);
        if (DIRECTION_DEBIT.equals(direction)) {
            facts.add(fact(varianceSubject, varianceSubjectName, DC_DEBIT, amount, memo, event));
            facts.add(fact(wipSubject, wipSubjectName, DC_CREDIT, amount, memo, event));
        } else {
            facts.add(fact(wipSubject, wipSubjectName, DC_DEBIT, amount, memo, event));
            facts.add(fact(varianceSubject, varianceSubjectName, DC_CREDIT, amount, memo, event));
        }
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

    private String readDirection(Object value) {
        return value != null ? value.toString() : DIRECTION_DEBIT;
    }

    private String buildMemo(Map<String, Object> data, String subjectName) {
        Object code = data.get(KEY_WORKORDER_CODE);
        return code != null ? subjectName + "（工单 " + code + "）" : subjectName;
    }
}
