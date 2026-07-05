package app.erp.qa.service.posting;

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
 * NCR 报废损失过账 Provider（quality 域，非默认 Provider——Registry 中优先于默认 fallback）。
 *
 * <p>支持业务类型：{@link ErpFinBusinessType#NCR_SCRAP}。金额取自 {@code PostingEvent.billData} 的
 * {@code SCRAP_AMOUNT}（恒正 = NCR.quantity × 物料单位成本）。
 *
 * <p>科目：借 6711 营业外支出（报废损失）/ 贷 1401 库存商品（报废消耗存货出库）。
 * <ul>
 *   <li>借：营业外支出（6711）—— 报废损失入账</li>
 *   <li>贷：库存商品（1401）—— 报废存货出库</li>
 * </ul>
 *
 * <p>承接 {@code InvAcctDocProvider}（存货科目 1401）+ {@code PurchasePriceVarianceAcctDocProvider}
 * （方向相关 Dr/Cr 分解范式）。权威：{@code docs/design/quality/state-machine.md §NCR 财务影响规则}。
 */
public class NcrScrapAcctDocProvider implements IErpFinAcctDocProvider {

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;

    static final String SUBJECT_LOSS = "6711";
    static final String SUBJECT_INVENTORY = "1401";

    static final String KEY_SCRAP_AMOUNT = "SCRAP_AMOUNT";
    static final String KEY_MATERIAL_ID = "MATERIAL_ID";
    static final String KEY_WAREHOUSE_ID = "WAREHOUSE_ID";

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return Collections.unmodifiableSet(EnumSet.of(ErpFinBusinessType.NCR_SCRAP));
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        BigDecimal amount = readAmount(event);
        Long materialId = (Long) event.getBillData().get(KEY_MATERIAL_ID);
        Long warehouseId = (Long) event.getBillData().get(KEY_WAREHOUSE_ID);

        List<VoucherFact> facts = new ArrayList<>(2);
        facts.add(fact(SUBJECT_LOSS, "营业外支出-报废损失", DC_DEBIT, amount, materialId, warehouseId, event));
        facts.add(fact(SUBJECT_INVENTORY, "库存商品", DC_CREDIT, amount, materialId, warehouseId, event));
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
        Object value = event.getBillData().get(KEY_SCRAP_AMOUNT);
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.toString().trim());
    }
}
