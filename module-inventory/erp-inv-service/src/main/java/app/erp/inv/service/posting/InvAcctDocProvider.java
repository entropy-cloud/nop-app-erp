package app.erp.inv.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.service.posting.AcctDocContext;
import app.erp.fin.service.posting.IErpFinAcctDocProvider;
import app.erp.fin.service.posting.VoucherFact;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 存货估值过账 Provider（inventory 域，**非默认** Provider——Registry 中优先于默认 fallback）。
 *
 * <p>支持业务类型：{@link ErpFinBusinessType#PURCHASE_INPUT}（入库：借存货/贷暂估应付）、
 * {@link ErpFinBusinessType#SALES_OUTPUT}（出库：借结转成本/贷存货）。金额取自流水汇总（PostingEvent.billData.TOTAL_COST）。
 *
 * <p>科目编码（subjectCode）由引擎 {@code resolveSubjects} 按 code 解析为主数据科目：
 * <ul>
 *   <li>PURCHASE_INPUT：借 1401 库存商品 / 贷 2202 应付账款-暂估。</li>
 *   <li>SALES_OUTPUT：借 6401 主营业务成本 / 贷 1401 库存商品。</li>
 * </ul>
 * 同法人内部调拨不发事件（{@link InvPostingDispatcher} 已跳过），故本 Provider 不处理 INTER_TRANSFER。
 */
public class InvAcctDocProvider implements IErpFinAcctDocProvider {

    static final int DC_DEBIT = 10;
    static final int DC_CREDIT = 20;

    static final String SUBJECT_INVENTORY = "1401";
    static final String SUBJECT_ESTIMATED_AP = "2202";
    static final String SUBJECT_COGS = "6401";

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return EnumSet.of(ErpFinBusinessType.PURCHASE_INPUT, ErpFinBusinessType.SALES_OUTPUT);
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        BigDecimal total = readTotalCost(event);
        Long materialId = (Long) event.getBillData().get("MATERIAL_ID");
        Long warehouseId = (Long) event.getBillData().get("WAREHOUSE_ID");

        List<VoucherFact> facts = new ArrayList<>(2);
        if (event.getBusinessType() == ErpFinBusinessType.PURCHASE_INPUT) {
            facts.add(fact(SUBJECT_INVENTORY, "库存商品", DC_DEBIT, total, materialId, warehouseId, event));
            facts.add(fact(SUBJECT_ESTIMATED_AP, "应付账款-暂估", DC_CREDIT, total, materialId, warehouseId, event));
        } else {
            facts.add(fact(SUBJECT_COGS, "主营业务成本", DC_DEBIT, total, materialId, warehouseId, event));
            facts.add(fact(SUBJECT_INVENTORY, "库存商品", DC_CREDIT, total, materialId, warehouseId, event));
        }
        return facts;
    }

    private VoucherFact fact(String subjectCode, String subjectName, int dcDirection, BigDecimal amount,
                             Long materialId, Long warehouseId, PostingEvent event) {
        VoucherFact fact = new VoucherFact();
        fact.setSubjectCode(subjectCode);
        fact.setSubjectName(subjectName);
        fact.setDcDirection(dcDirection);
        fact.setAmount(amount);
        fact.setBusinessType(event.getBusinessType().getCode());
        fact.setMaterialId(materialId);
        fact.setWarehouseId(warehouseId);
        fact.setMemo(event.getBillHeadCode());
        return fact;
    }

    private BigDecimal readTotalCost(PostingEvent event) {
        Object value = event.getBillData().get("TOTAL_COST");
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.toString().trim());
    }
}
