package app.erp.mfg.service;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.service.posting.VoucherFact;
import app.erp.mfg.service.posting.ManufacturingIssueAcctDocProvider;
import app.erp.mfg.service.posting.ProductionVarianceAcctDocProvider;
import app.erp.mfg.service.posting.SubcontractFeeAcctDocProvider;
import app.erp.mfg.service.posting.SubcontractIssueAcctDocProvider;
import app.erp.mfg.service.posting.SubcontractReceiptAcctDocProvider;
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
 * manufacturing 域 5 Provider GL 映射 accountKey 接入单元测试（plan 2026-07-24-1351-1 Phase 3 抽样）。
 * 命中/fallback/strict 三路径由 resolver 承担（{@code TestErpFinGlMappingResolver}）。
 */
public class TestErpMfgAcctDocProviderAccountKey extends BaseTestCase {

    @Test
    public void testManufacturingIssueKeys() {
        ManufacturingIssueAcctDocProvider provider = new ManufacturingIssueAcctDocProvider();
        PostingEvent event = event(ErpFinBusinessType.MANUFACTURING_ISSUE);
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("MATERIAL_CODE", "M1");
        line.put("MATERIAL_COST", new BigDecimal("100"));
        line.put("INVENTORY_SUBJECT", "1401");
        event.getBillData().put("LINES", List.of(line));
        event.getBillData().put("WORKORDER_CODE", "WO1");

        List<VoucherFact> facts = provider.createFacts(event, null);
        assertKeys(facts, "INVENTORY", "MANUFACTURING_WIP");
    }

    @Test
    public void testSubcontractIssueKeys() {
        SubcontractIssueAcctDocProvider provider = new SubcontractIssueAcctDocProvider();
        PostingEvent event = event(ErpFinBusinessType.SUBCONTRACT_ISSUE);
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("MATERIAL_CODE", "M1");
        line.put("MATERIAL_COST", new BigDecimal("100"));
        line.put("INVENTORY_SUBJECT", "1401");
        event.getBillData().put("LINES", List.of(line));
        event.getBillData().put("SUBCONTRACT_CODE", "SC1");

        List<VoucherFact> facts = provider.createFacts(event, null);
        assertKeys(facts, "INVENTORY", "SUBCONTRACT_MATERIAL");
    }

    @Test
    public void testSubcontractFeeKeys() {
        SubcontractFeeAcctDocProvider provider = new SubcontractFeeAcctDocProvider();
        PostingEvent event = event(ErpFinBusinessType.SUBCONTRACT_FEE);
        event.getBillData().put("SUBCONTRACT_CODE", "SC1");
        event.getBillData().put("PROCESSING_FEE", new BigDecimal("200"));

        List<VoucherFact> facts = provider.createFacts(event, null);
        assertKeys(facts, "SUBCONTRACT_MATERIAL", "ACCOUNTS_PAYABLE");
    }

    @Test
    public void testSubcontractReceiptKeys() {
        SubcontractReceiptAcctDocProvider provider = new SubcontractReceiptAcctDocProvider();
        PostingEvent event = event(ErpFinBusinessType.SUBCONTRACT_RECEIPT);
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("MATERIAL_CODE", "M1");
        line.put("FINISHED_COST", new BigDecimal("300"));
        line.put("FINISHED_SUBJECT", "1405");
        event.getBillData().put("LINES", List.of(line));
        event.getBillData().put("SUBCONTRACT_CODE", "SC1");

        List<VoucherFact> facts = provider.createFacts(event, null);
        assertKeys(facts, "FINISHED_GOODS", "SUBCONTRACT_MATERIAL");
    }

    @Test
    public void testProductionVarianceKeys() {
        ProductionVarianceAcctDocProvider provider = new ProductionVarianceAcctDocProvider();
        PostingEvent event = event(ErpFinBusinessType.PRODUCTION_VARIANCE);
        event.getBillData().put("MATERIAL_VARIANCE", new BigDecimal("50"));
        event.getBillData().put("MATERIAL_DIRECTION", "DEBIT");
        event.getBillData().put("WORKORDER_CODE", "WO1");

        List<VoucherFact> facts = provider.createFacts(event, null);
        assertKeys(facts, "MANUFACTURING_VARIANCE", "MANUFACTURING_WIP");
    }

    private PostingEvent event(ErpFinBusinessType type) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(type);
        return event;
    }

    private void assertKeys(List<VoucherFact> facts, String... expectedKeys) {
        for (VoucherFact fact : facts) {
            assertNotNull(fact.getAccountKey(), "accountKey 必须非空");
            assertFalse(fact.getAccountKey().trim().isEmpty(), "accountKey 不能为空白");
        }
        assertEquals(expectedKeys.length, facts.size(), "fact 行数不匹配");
        for (int i = 0; i < expectedKeys.length; i++) {
            assertEquals(expectedKeys[i], facts.get(i).getAccountKey(),
                    "第 " + i + " 条 fact accountKey 语义不匹配");
        }
    }
}
