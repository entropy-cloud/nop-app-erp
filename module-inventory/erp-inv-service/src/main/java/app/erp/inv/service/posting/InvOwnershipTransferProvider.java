package app.erp.inv.service.posting;

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
import java.util.Set;

/**
 * 所有权转移过账 Provider（inventory 域，consignment.md §业财过账）。
 *
 * <p>支持业务类型：{@link ErpFinBusinessType#OWNERSHIP_TRANSFER}。典型 VMI 消耗（VMI_SUPPLIER→OWNED）：
 * 借 1401 存货(自有, TOTAL_COST) / 贷 2202 应付-供应商（生成应付，待供应商采购发票核销）。
 *
 * <p>科目编码（subjectCode）由引擎 {@code resolveSubjects} 按 code 解析为主数据科目。
 * 范式对照 {@link InvAcctDocProvider}（存货估值）与 {@code SalAcctDocProvider}（销售过账）。
 */
public class InvOwnershipTransferProvider implements IErpFinAcctDocProvider {

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;

    static final String SUBJECT_INVENTORY = "1401";   // 库存商品（与 InvAcctDocProvider 同口径）
    static final String SUBJECT_AP = "2202";          // 应付账款-供应商

    static final String KEY_TOTAL_COST = "TOTAL_COST";

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return EnumSet.of(ErpFinBusinessType.OWNERSHIP_TRANSFER);
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        BigDecimal total = readTotalCost(event);
        Long materialId = (Long) event.getBillData().get("MATERIAL_ID");
        Long warehouseId = (Long) event.getBillData().get("WAREHOUSE_ID");
        Long partnerId = (Long) event.getBillData().get("SUPPLIER_ID");
        if (partnerId == null) {
            partnerId = (Long) event.getBillData().get("partnerId");
        }

        List<VoucherFact> facts = new ArrayList<>(2);
        // 借：存货(自有) —— VMI 消耗后归自有，存货成本入账
        facts.add(fact(SUBJECT_INVENTORY, "库存商品", DC_DEBIT, total, materialId, warehouseId, partnerId, event));
        // 贷：应付-供应商 —— 暂估应付，待供应商采购发票核销
        facts.add(fact(SUBJECT_AP, "应付账款-供应商", DC_CREDIT, total, materialId, warehouseId, partnerId, event));
        return facts;
    }

    private VoucherFact fact(String subjectCode, String subjectName, String dcDirection, BigDecimal amount,
                             Long materialId, Long warehouseId, Long partnerId, PostingEvent event) {
        VoucherFact fact = new VoucherFact();
        fact.setSubjectCode(subjectCode);
        fact.setSubjectName(subjectName);
        fact.setDcDirection(dcDirection);
        fact.setAmount(amount);
        fact.setBusinessType(event.getBusinessType().name());
        fact.setMaterialId(materialId);
        fact.setWarehouseId(warehouseId);
        fact.setPartnerId(partnerId);
        fact.setMemo(event.getBillHeadCode());
        return fact;
    }

    private BigDecimal readTotalCost(PostingEvent event) {
        Object value = event.getBillData().get(KEY_TOTAL_COST);
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.toString().trim());
    }
}
