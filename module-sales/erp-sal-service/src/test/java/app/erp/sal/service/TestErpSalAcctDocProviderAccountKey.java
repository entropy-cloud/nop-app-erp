package app.erp.sal.service;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.service.posting.VoucherFact;
import app.erp.sal.service.posting.SalAcctDocProvider;
import io.nop.core.unittest.BaseTestCase;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * SalAcctDocProvider GL 映射 accountKey 接入单元测试（plan 2026-07-24-1351-1 Phase 2）。
 *
 * <p>纯单元测试：无 DB/IoC，直接 new Provider 构造 PostingEvent 断言每条 fact 的 accountKey 非空且语义正确。
 * 命中覆盖 subjectCode + 未命中保留 fallback + strict-mode 抛错三路径由 resolver 承担（见
 * {@code TestErpFinGlMappingResolver} 8 场景 + {@code TestErpPurInvoicePosting.testGlMappingRuleOverrideChangesSubjectCode}
 * 端到端；接入链路与试点一致），本测试聚焦「Provider 已为所有 fact 设置 accountKey 使 resolver 可达」。
 */
public class TestErpSalAcctDocProviderAccountKey extends BaseTestCase {

    private final SalAcctDocProvider provider = new SalAcctDocProvider();

    @Test
    public void testArInvoiceAccountKeys() {
        PostingEvent event = event(ErpFinBusinessType.AR_INVOICE);
        event.getBillData().put("TOTAL_AMOUNT", new BigDecimal("100"));
        event.getBillData().put("TOTAL_TAX_AMOUNT", new BigDecimal("13"));
        event.getBillData().put("TOTAL_AMOUNT_WITH_TAX", new BigDecimal("113"));

        List<VoucherFact> facts = provider.createFacts(event, null);
        assertEquals(3, facts.size());
        assertKeys(facts, "AR", "REVENUE", "OUTPUT_TAX");
    }

    @Test
    public void testReceiptAccountKeys() {
        PostingEvent event = event(ErpFinBusinessType.RECEIPT);
        event.getBillData().put("TOTAL", new BigDecimal("113"));

        List<VoucherFact> facts = provider.createFacts(event, null);
        assertEquals(2, facts.size());
        assertKeys(facts, "BANK_DEPOSIT", "AR");
    }

    @Test
    public void testSalesReturnAccountKeys() {
        PostingEvent event = event(ErpFinBusinessType.SALES_RETURN);
        event.getBillData().put("TOTAL_COST", new BigDecimal("80"));

        List<VoucherFact> facts = provider.createFacts(event, null);
        assertEquals(2, facts.size());
        assertKeys(facts, "INVENTORY", "COGS");
    }

    private PostingEvent event(ErpFinBusinessType type) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(type);
        return event;
    }

    private void assertKeys(List<VoucherFact> facts, String... expectedKeys) {
        for (VoucherFact fact : facts) {
            assertNotNull(fact.getAccountKey(), "每条 fact 的 accountKey 必须非空（resolver 可达前提）");
            assertFalse(fact.getAccountKey().trim().isEmpty(), "accountKey 不能为空白");
        }
        for (int i = 0; i < expectedKeys.length; i++) {
            assertEquals(expectedKeys[i], facts.get(i).getAccountKey(),
                    "第 " + i + " 条 fact 的 accountKey 语义不匹配");
        }
    }
}
