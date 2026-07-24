package app.erp.qa.service;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.service.posting.VoucherFact;
import app.erp.qa.service.posting.NcrScrapAcctDocProvider;
import io.nop.core.unittest.BaseTestCase;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * NcrScrapAcctDocProvider GL 映射 accountKey 接入单元测试（plan 2026-07-24-1351-1 Phase 3 抽样）。
 * 命中/fallback/strict 三路径由 resolver 承担（{@code TestErpFinGlMappingResolver}）。
 */
public class TestErpQaAcctDocProviderAccountKey extends BaseTestCase {

    @Test
    public void testNcrScrapKeys() {
        NcrScrapAcctDocProvider provider = new NcrScrapAcctDocProvider();
        PostingEvent e = new PostingEvent();
        e.setBusinessType(ErpFinBusinessType.NCR_SCRAP);
        e.getBillData().put("SCRAP_AMOUNT", new BigDecimal("50"));
        e.getBillData().put("MATERIAL_ID", 1L);
        e.getBillData().put("WAREHOUSE_ID", 2L);

        List<VoucherFact> facts = provider.createFacts(e, null);
        for (VoucherFact fact : facts) {
            assertNotNull(fact.getAccountKey(), "accountKey 必须非空");
            assertFalse(fact.getAccountKey().trim().isEmpty(), "accountKey 不能为空白");
        }
        assertEquals(2, facts.size());
        assertEquals("NON_OPERATING_EXPENSE", facts.get(0).getAccountKey());
        assertEquals("INVENTORY", facts.get(1).getAccountKey());
    }
}
