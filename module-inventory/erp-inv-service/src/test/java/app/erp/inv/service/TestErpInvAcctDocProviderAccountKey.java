package app.erp.inv.service;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.service.posting.VoucherFact;
import app.erp.inv.service.posting.CostAdjustmentAcctDocProvider;
import app.erp.inv.service.posting.InvAcctDocProvider;
import app.erp.inv.service.posting.LandedCostAcctDocProvider;
import app.erp.inv.service.posting.PurchasePriceVarianceAcctDocProvider;
import io.nop.core.unittest.BaseTestCase;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * inventory 域 4 Provider GL 映射 accountKey 接入单元测试（plan 2026-07-24-1351-1 Phase 2）。
 *
 * <p>纯单元测试：直接 new Provider 构造 PostingEvent 断言每条 fact accountKey 非空且语义正确。
 * 命中覆盖/fallback/strict 三路径由 resolver 承担（{@code TestErpFinGlMappingResolver}）。
 */
public class TestErpInvAcctDocProviderAccountKey extends BaseTestCase {

    @Test
    public void testInvPurchaseInputKeys() {
        InvAcctDocProvider provider = new InvAcctDocProvider();
        PostingEvent event = event(ErpFinBusinessType.PURCHASE_INPUT);
        event.getBillData().put("TOTAL_COST", new BigDecimal("100"));
        event.getBillData().put("MATERIAL_ID", 1L);
        event.getBillData().put("WAREHOUSE_ID", 2L);

        List<VoucherFact> facts = provider.createFacts(event, null);
        assertKeys(facts, "INVENTORY", "ACCOUNTS_PAYABLE");
    }

    @Test
    public void testInvManufacturingReceiptKeys() {
        InvAcctDocProvider provider = new InvAcctDocProvider();
        PostingEvent event = event(ErpFinBusinessType.MANUFACTURING_RECEIPT);
        event.getBillData().put("TOTAL_COST", new BigDecimal("100"));
        event.getBillData().put("MATERIAL_ID", 1L);
        event.getBillData().put("WAREHOUSE_ID", 2L);

        List<VoucherFact> facts = provider.createFacts(event, null);
        assertKeys(facts, "INVENTORY", "MANUFACTURING_WIP");
    }

    @Test
    public void testInvSalesOutputKeys() {
        InvAcctDocProvider provider = new InvAcctDocProvider();
        PostingEvent event = event(ErpFinBusinessType.SALES_OUTPUT);
        event.getBillData().put("TOTAL_COST", new BigDecimal("100"));
        event.getBillData().put("MATERIAL_ID", 1L);
        event.getBillData().put("WAREHOUSE_ID", 2L);

        List<VoucherFact> facts = provider.createFacts(event, null);
        assertKeys(facts, "COGS", "INVENTORY");
    }

    @Test
    public void testCostAdjustmentKeys() {
        CostAdjustmentAcctDocProvider provider = new CostAdjustmentAcctDocProvider();
        PostingEvent event = event(ErpFinBusinessType.COST_ADJUSTMENT);
        event.getBillData().put("ADJUST_AMOUNT", new BigDecimal("50"));
        event.getBillData().put("ADJUST_DIRECTION", "INCREASE");
        event.getBillData().put("MATERIAL_ID", 1L);
        event.getBillData().put("WAREHOUSE_ID", 2L);

        List<VoucherFact> facts = provider.createFacts(event, null);
        assertKeys(facts, "INVENTORY", "COST_VARIANCE");

        // 减少方向键互换
        event.getBillData().put("ADJUST_DIRECTION", "DECREASE");
        List<VoucherFact> dec = provider.createFacts(event, null);
        assertKeys(dec, "COST_VARIANCE", "INVENTORY");
    }

    @Test
    public void testPurchasePriceVarianceKeys() {
        PurchasePriceVarianceAcctDocProvider provider = new PurchasePriceVarianceAcctDocProvider();
        PostingEvent event = event(ErpFinBusinessType.PURCHASE_PRICE_VARIANCE);
        event.getBillData().put("PPV_AMOUNT", new BigDecimal("30"));
        event.getBillData().put("PPV_DIRECTION", "DEBIT");
        event.getBillData().put("MATERIAL_ID", 1L);
        event.getBillData().put("WAREHOUSE_ID", 2L);

        List<VoucherFact> facts = provider.createFacts(event, null);
        assertKeys(facts, "PURCHASE_PRICE_VARIANCE", "ACCOUNTS_PAYABLE");
    }

    @Test
    public void testLandedCostKeys() {
        LandedCostAcctDocProvider provider = new LandedCostAcctDocProvider();
        PostingEvent event = event(ErpFinBusinessType.LANDED_COST);
        Map<String, Object> alloc = new LinkedHashMap<>();
        alloc.put("allocatedAmount", new BigDecimal("100"));
        alloc.put("materialId", 1L);
        alloc.put("warehouseId", 2L);
        event.getBillData().put("ALLOCATIONS", List.of(alloc));
        Map<String, Object> elem = new LinkedHashMap<>();
        elem.put("amount", new BigDecimal("100"));
        elem.put("apPartnerId", 3L);
        event.getBillData().put("COST_ELEMENTS", List.of(elem));

        List<VoucherFact> facts = provider.createFacts(event, null);
        assertKeys(facts, "INVENTORY", "ACCOUNTS_PAYABLE");
    }

    private PostingEvent event(ErpFinBusinessType type) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(type);
        return event;
    }

    private void assertKeys(List<VoucherFact> facts, String... expectedKeys) {
        for (VoucherFact fact : facts) {
            assertNotNull(fact.getAccountKey(), "每条 fact 的 accountKey 必须非空");
            assertFalse(fact.getAccountKey().trim().isEmpty(), "accountKey 不能为空白");
        }
        assertEquals(expectedKeys.length, facts.size(), "fact 行数不匹配");
        for (int i = 0; i < expectedKeys.length; i++) {
            assertEquals(expectedKeys[i], facts.get(i).getAccountKey(),
                    "第 " + i + " 条 fact 的 accountKey 语义不匹配");
        }
    }
}
