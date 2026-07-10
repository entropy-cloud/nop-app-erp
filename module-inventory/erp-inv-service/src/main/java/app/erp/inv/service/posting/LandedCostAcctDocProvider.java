package app.erp.inv.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.posting.AcctDocContext;
import app.erp.fin.service.posting.IErpFinAcctDocProvider;
import app.erp.fin.service.posting.VoucherFact;
import app.erp.inv.service.ErpInvConstants;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 到岸成本过账 Provider（inventory 域，plan 2026-07-10-1100-3）。
 *
 * <p>支持单一业务类型 {@link ErpFinBusinessType#LANDED_COST}。凭证行生成（costing-methods.md §到岸成本凭证步骤4）：
 * <ul>
 *   <li>借：每入库行 → 存货科目(1401)，金额=该行分摊金额</li>
 *   <li>贷：每费用要素 → 应付账款(2202)，金额=该费用金额，partnerId=费用行应付对象</li>
 * </ul>
 *
 * <p>核心语义：费用资本化到存货（借存货），贷方是应付给服务商（贷应付账款）。
 * billData 中 ALLOCATIONS 为借方行列表（每行含 materialId/warehouseId/allocatedAmount），
 * COST_ELEMENTS 为贷方行列表（每行含 amount/apPartnerId）。
 */
public class LandedCostAcctDocProvider implements IErpFinAcctDocProvider {

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;

    static final String SUBJECT_INVENTORY = ErpInvConstants.SUBJECT_INVENTORY;
    static final String SUBJECT_AP = ErpInvConstants.SUBJECT_ACCOUNTS_PAYABLE;

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return Collections.unmodifiableSet(EnumSet.of(ErpFinBusinessType.LANDED_COST));
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        List<VoucherFact> facts = new ArrayList<>();
        String billHeadCode = event.getBillHeadCode();

        // 借方：每入库行分摊金额 → 存货
        List<Map<String, Object>> allocations = readList(event, ErpInvConstants.BILL_DATA_LANDED_COST_ALLOCATIONS);
        for (Map<String, Object> alloc : allocations) {
            BigDecimal amount = toBigDecimal(alloc.get("allocatedAmount"));
            if (amount == null || amount.signum() == 0) {
                continue;
            }
            VoucherFact fact = new VoucherFact();
            fact.setSubjectCode(SUBJECT_INVENTORY);
            fact.setSubjectName("库存商品");
            fact.setDcDirection(DC_DEBIT);
            fact.setAmount(amount);
            fact.setBusinessType(event.getBusinessType().name());
            fact.setMaterialId(toLong(alloc.get("materialId")));
            fact.setWarehouseId(toLong(alloc.get("warehouseId")));
            fact.setMemo(billHeadCode);
            facts.add(fact);
        }

        // 贷方：每费用要素 → 应付账款（按 apPartnerId 分行）
        List<Map<String, Object>> costElements = readList(event, ErpInvConstants.BILL_DATA_LANDED_COST_COST_ELEMENTS);
        for (Map<String, Object> elem : costElements) {
            BigDecimal amount = toBigDecimal(elem.get("amount"));
            if (amount == null || amount.signum() == 0) {
                continue;
            }
            VoucherFact fact = new VoucherFact();
            fact.setSubjectCode(SUBJECT_AP);
            fact.setSubjectName("应付账款");
            fact.setDcDirection(DC_CREDIT);
            fact.setAmount(amount);
            fact.setBusinessType(event.getBusinessType().name());
            fact.setPartnerId(toLong(elem.get("apPartnerId")));
            fact.setMemo(billHeadCode);
            facts.add(fact);
        }

        return facts;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readList(PostingEvent event, String key) {
        Object value = event.getBillData().get(key);
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return Collections.emptyList();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value == null) {
            return null;
        }
        return new BigDecimal(value.toString().trim());
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString().trim());
    }
}
