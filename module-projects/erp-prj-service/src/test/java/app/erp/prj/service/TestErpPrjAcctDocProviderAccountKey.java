package app.erp.prj.service;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.service.posting.VoucherFact;
import app.erp.prj.service.posting.ProjectSettlementAcctDocProvider;
import io.nop.core.unittest.BaseTestCase;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * ProjectSettlementAcctDocProvider GL 映射 accountKey 接入单元测试（plan 2026-07-24-1351-1 Phase 3 抽样）。
 * 命中/fallback/strict 三路径由 resolver 承担（{@code TestErpFinGlMappingResolver}）。
 */
public class TestErpPrjAcctDocProviderAccountKey extends BaseTestCase {

    @Test
    public void testCloseTransferToAsset() {
        ProjectSettlementAcctDocProvider provider = new ProjectSettlementAcctDocProvider();
        PostingEvent e = event();
        e.getBillData().put(ErpPrjConstants.BILL_DATA_SETTLEMENT_TYPE, ErpPrjConstants.SETTLEMENT_TYPE_CLOSE);
        e.getBillData().put(ErpPrjConstants.BILL_DATA_FINAL_COST, new BigDecimal("100"));
        e.getBillData().put(ErpPrjConstants.BILL_DATA_PROJECT_ID, 1L);
        e.getBillData().put(ErpPrjConstants.BILL_DATA_TRANSFER_TO_ASSET, Boolean.TRUE);

        assertKeys(provider.createFacts(e, null), "FIXED_ASSET", "CIP");
    }

    @Test
    public void testFinalSettlement() {
        ProjectSettlementAcctDocProvider provider = new ProjectSettlementAcctDocProvider();
        PostingEvent e = event();
        e.getBillData().put(ErpPrjConstants.BILL_DATA_SETTLEMENT_TYPE, ErpPrjConstants.SETTLEMENT_TYPE_FINAL);
        e.getBillData().put(ErpPrjConstants.BILL_DATA_FINAL_REVENUE, new BigDecimal("120"));
        e.getBillData().put(ErpPrjConstants.BILL_DATA_FINAL_COST, new BigDecimal("100"));
        e.getBillData().put(ErpPrjConstants.BILL_DATA_PROJECT_ID, 1L);
        e.getBillData().put(ErpPrjConstants.BILL_DATA_TRANSFER_TO_ASSET, Boolean.FALSE);

        // 借项目成本 + 借本年利润(利润20) / 贷项目收入(120)
        assertKeys(provider.createFacts(e, null), "PROJECT_COST", "PROFIT_LOSS", "REVENUE");
    }

    private PostingEvent event() {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.PROJECT_SETTLEMENT);
        return event;
    }

    private void assertKeys(java.util.List<VoucherFact> facts, String... expectedKeys) {
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
