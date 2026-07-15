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
import java.util.Set;

/**
 * 成本调整业财过账 Provider（inventory 域，plan 2026-07-05-2352-3）。
 *
 * <p>支持单一业务类型 {@link ErpFinBusinessType#COST_ADJUSTMENT}。方向相关 Dr/Cr 分解
 * （costing-methods.md §成本调整流程 步骤4 + 承接 PPV/生产差异方向相关范式）：
 * <ul>
 *   <li>成本增加（INCREASE，newUnitCost>oldUnitCost）：借存货(1401) / 贷成本差异(6603)</li>
 *   <li>成本减少（DECREASE）：借成本差异(6603) / 贷存货(1401)</li>
 * </ul>
 * 金额取自 {@code PostingEvent.billData} 的 {@code ADJUST_AMOUNT}（恒正），
 * 方向取自 {@code ADJUST_DIRECTION}。
 */
public class CostAdjustmentAcctDocProvider implements IErpFinAcctDocProvider {

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return Collections.unmodifiableSet(EnumSet.of(ErpFinBusinessType.COST_ADJUSTMENT));
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        BigDecimal amount = readAmount(event);
        String direction = readDirection(event);
        Long materialId = (Long) event.getBillData().get("MATERIAL_ID");
        Long warehouseId = (Long) event.getBillData().get("WAREHOUSE_ID");

        List<VoucherFact> facts = new ArrayList<>(2);
        if (ErpInvConstants.DIRECTION_INCREASE.equals(direction)) {
            facts.add(fact(ErpInvConstants.SUBJECT_INVENTORY, "库存商品", DC_DEBIT, amount, materialId, warehouseId, event));
            facts.add(fact(ErpInvConstants.SUBJECT_COST_VARIANCE, "成本差异", DC_CREDIT, amount, materialId, warehouseId, event));
        } else {
            facts.add(fact(ErpInvConstants.SUBJECT_COST_VARIANCE, "成本差异", DC_DEBIT, amount, materialId, warehouseId, event));
            facts.add(fact(ErpInvConstants.SUBJECT_INVENTORY, "库存商品", DC_CREDIT, amount, materialId, warehouseId, event));
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
        return fact;
    }

    private BigDecimal readAmount(PostingEvent event) {
        Object value = event.getBillData().get(ErpInvConstants.BILL_DATA_ADJUST_AMOUNT);
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.toString().trim());
    }

    private String readDirection(PostingEvent event) {
        Object value = event.getBillData().get(ErpInvConstants.BILL_DATA_ADJUST_DIRECTION);
        return value != null ? value.toString() : ErpInvConstants.DIRECTION_INCREASE;
    }
}
