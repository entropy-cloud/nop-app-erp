package app.erp.inv.service.posting;

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
import java.util.Set;

/**
 * 采购价差（PPV）过账 Provider（inventory 域，非默认 Provider——Registry 中优先于默认 fallback）。
 *
 * <p>支持业务类型：{@link ErpFinBusinessType#PURCHASE_PRICE_VARIANCE}。金额与方向取自
 * {@code PostingEvent.billData} 的 {@code PPV_AMOUNT}（恒正）与 {@code PPV_DIRECTION}
 * （{@code DEBIT}=实际>标准借价差 / {@code CREDIT}=实际<标准贷价差）。
 *
 * <p>科目：借/贷 1404 材料成本差异（PPV 科目）/ 贷/借 2202 应付账款-暂估。
 * <ul>
 *   <li>实际>标准（unfavorable）：借 PPV / 贷暂估应付（补计实际高于标准的应付差额）</li>
 *   <li>实际<标准（favorable）：借暂估应付 / 贷 PPV（冲减实际低于标准的应付差额）</li>
 * </ul>
 *
 * <p>权威：{@code docs/design/finance/costing-methods.md}（标准成本法 PPV）、
 * {@code docs/design/manufacturing/variance-analysis.md}（差异分析模型）。
 */
public class PurchasePriceVarianceAcctDocProvider implements IErpFinAcctDocProvider {

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;

    static final String SUBJECT_PPV = "1404";
    static final String SUBJECT_ESTIMATED_AP = "2202";

    static final String KEY_PPV_AMOUNT = "PPV_AMOUNT";
    static final String KEY_PPV_DIRECTION = "PPV_DIRECTION";
    static final String KEY_MATERIAL_ID = "MATERIAL_ID";
    static final String KEY_WAREHOUSE_ID = "WAREHOUSE_ID";

    static final String DIRECTION_DEBIT = "DEBIT";
    static final String DIRECTION_CREDIT = "CREDIT";

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return Collections.unmodifiableSet(EnumSet.of(ErpFinBusinessType.PURCHASE_PRICE_VARIANCE));
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        BigDecimal amount = readAmount(event);
        String direction = readDirection(event);
        Long materialId = (Long) event.getBillData().get(KEY_MATERIAL_ID);
        Long warehouseId = (Long) event.getBillData().get(KEY_WAREHOUSE_ID);

        List<VoucherFact> facts = new ArrayList<>(2);
        if (DIRECTION_DEBIT.equals(direction)) {
            facts.add(fact(SUBJECT_PPV, "材料成本差异", DC_DEBIT, amount, materialId, warehouseId, event));
            facts.add(fact(SUBJECT_ESTIMATED_AP, "应付账款-暂估", DC_CREDIT, amount, materialId, warehouseId, event));
        } else {
            facts.add(fact(SUBJECT_ESTIMATED_AP, "应付账款-暂估", DC_DEBIT, amount, materialId, warehouseId, event));
            facts.add(fact(SUBJECT_PPV, "材料成本差异", DC_CREDIT, amount, materialId, warehouseId, event));
        }
        return facts;
    }

    private VoucherFact fact(String subjectCode, String subjectName, String dcDirection, BigDecimal amount,
                             Long materialId, Long warehouseId, PostingEvent event) {
        VoucherFact fact = new VoucherFact();
        fact.setSubjectCode(subjectCode);
        fact.setSubjectName(subjectName);
        fact.setDcDirection(dcDirection);
        fact.setAmount(amount);
        fact.setBusinessType(event.getBusinessType().name());
        fact.setMaterialId(materialId);
        fact.setWarehouseId(warehouseId);
        fact.setMemo(event.getBillHeadCode());
        return fact;
    }

    private BigDecimal readAmount(PostingEvent event) {
        Object value = event.getBillData().get(KEY_PPV_AMOUNT);
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.toString().trim());
    }

    private String readDirection(PostingEvent event) {
        Object value = event.getBillData().get(KEY_PPV_DIRECTION);
        return value != null ? value.toString() : DIRECTION_DEBIT;
    }
}
