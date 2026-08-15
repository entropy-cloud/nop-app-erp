package app.erp.ast.service;

import app.erp.ast.service.posting.AssetInventoryAcctDocProvider;
import app.erp.ast.service.posting.AssetMergeAcctDocProvider;
import app.erp.ast.service.posting.AssetSplitAcctDocProvider;
import app.erp.ast.service.posting.CapitalizationAcctDocProvider;
import app.erp.ast.service.posting.DepreciationAcctDocProvider;
import app.erp.ast.service.posting.DisposalAcctDocProvider;
import app.erp.ast.service.posting.MaintenanceCapitalizationAcctDocProvider;
import app.erp.ast.service.posting.MaintenanceExpenseAcctDocProvider;
import app.erp.ast.service.posting.ValueAdjustmentAcctDocProvider;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.service.posting.VoucherFact;
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
 * assets 域 9 Provider GL 映射 accountKey 接入单元测试（plan 2026-07-24-1351-1 Phase 3 抽样）。
 * 命中/fallback/strict 三路径由 resolver 承担（{@code TestErpFinGlMappingResolver}）。
 */
public class TestErpAstAcctDocProviderAccountKey extends BaseTestCase {

    private static final BigDecimal AMT = new BigDecimal("100");

    @Test
    public void testDepreciation() {
        DepreciationAcctDocProvider p = new DepreciationAcctDocProvider();
        PostingEvent e = event(ErpFinBusinessType.DEPRECIATION);
        e.getBillData().put(ErpAstConstants.BILL_DATA_DEPRECIATION_AMOUNT, AMT);
        assertKeys(p.createFacts(e, null), "DEPRECIATION_EXPENSE", "ACCUMULATED_DEPRECIATION");
    }

    @Test
    public void testDisposal() {
        DisposalAcctDocProvider p = new DisposalAcctDocProvider();
        PostingEvent e = event(ErpFinBusinessType.DISPOSAL);
        e.getBillData().put(ErpAstConstants.BILL_DATA_ACCUMULATED_DEPRECIATION, new BigDecimal("60"));
        e.getBillData().put(ErpAstConstants.BILL_DATA_ORIGINAL_VALUE, new BigDecimal("100"));
        e.getBillData().put(ErpAstConstants.BILL_DATA_DISPOSAL_AMOUNT, new BigDecimal("30"));
        e.getBillData().put(ErpAstConstants.BILL_DATA_GAIN_LOSS, new BigDecimal("-10"));
        // SOLD 损失：借 1602 累计折旧 / 借 1606 固定资产清理(净值40) / 贷 1601 固定资产 / 借 1002 银行存款 /
        //           贷 1606(30) / 借 6711 营业外支出(10) / 贷 1606(10)（1606 网为零）
        List<VoucherFact> facts = p.createFacts(e, null);
        assertKeys(facts, "ACCUMULATED_DEPRECIATION", "DISPOSAL_CLEARING", "FIXED_ASSET",
                "BANK_DEPOSIT", "DISPOSAL_CLEARING", "NON_OPERATING_EXPENSE", "DISPOSAL_CLEARING");
        assertDisposalClearingIdentity(facts, new BigDecimal("40"), new BigDecimal("40"));
    }

    /** SCRAPPED 损失四组合行级结构 + 1606 网为零恒等式（RC-R1.53 新增断言）。 */
    @Test
    public void testDisposalScrapLossWithClearingLegs() {
        DisposalAcctDocProvider p = new DisposalAcctDocProvider();
        PostingEvent e = event(ErpFinBusinessType.DISPOSAL);
        e.getBillData().put(ErpAstConstants.BILL_DATA_ACCUMULATED_DEPRECIATION, new BigDecimal("60"));
        e.getBillData().put(ErpAstConstants.BILL_DATA_ORIGINAL_VALUE, new BigDecimal("100"));
        e.getBillData().put(ErpAstConstants.BILL_DATA_DISPOSAL_AMOUNT, BigDecimal.ZERO);
        e.getBillData().put(ErpAstConstants.BILL_DATA_GAIN_LOSS, new BigDecimal("-40"));
        // 报废损失：借 1602(60) / 借 1606(净值40) / 贷 1601(100) → 借 6711(40) / 贷 1606(40)
        List<VoucherFact> facts = p.createFacts(e, null);
        assertKeys(facts, "ACCUMULATED_DEPRECIATION", "DISPOSAL_CLEARING", "FIXED_ASSET",
                "NON_OPERATING_EXPENSE", "DISPOSAL_CLEARING");
        assertDisposalClearingIdentity(facts, new BigDecimal("40"), new BigDecimal("40"));
        assertEquals("6711", facts.get(3).getSubjectCode(), "报废损失借 6711 营业外支出");
    }

    /** SCRAPPED 无折旧（净值=原值）四组合行级结构（1606 覆盖全部净值）。 */
    @Test
    public void testDisposalScrapNoDepreciationWithClearingLegs() {
        DisposalAcctDocProvider p = new DisposalAcctDocProvider();
        PostingEvent e = event(ErpFinBusinessType.DISPOSAL);
        e.getBillData().put(ErpAstConstants.BILL_DATA_ACCUMULATED_DEPRECIATION, BigDecimal.ZERO);
        e.getBillData().put(ErpAstConstants.BILL_DATA_ORIGINAL_VALUE, new BigDecimal("100"));
        e.getBillData().put(ErpAstConstants.BILL_DATA_DISPOSAL_AMOUNT, BigDecimal.ZERO);
        e.getBillData().put(ErpAstConstants.BILL_DATA_GAIN_LOSS, new BigDecimal("-100"));
        // 无折旧报废：借 1606(净值100) / 贷 1601(100) → 借 6711(100) / 贷 1606(100)，无 1602 腿
        List<VoucherFact> facts = p.createFacts(e, null);
        assertKeys(facts, "DISPOSAL_CLEARING", "FIXED_ASSET", "NON_OPERATING_EXPENSE", "DISPOSAL_CLEARING");
        assertDisposalClearingIdentity(facts, new BigDecimal("100"), new BigDecimal("100"));
    }

    /** SOLD 收益四组合行级结构 + 1606 网为零恒等式（L1 UC-AST-05 收益分支）。 */
    @Test
    public void testDisposalSaleGainWithClearingLegs() {
        DisposalAcctDocProvider p = new DisposalAcctDocProvider();
        PostingEvent e = event(ErpFinBusinessType.DISPOSAL);
        e.getBillData().put(ErpAstConstants.BILL_DATA_ACCUMULATED_DEPRECIATION, new BigDecimal("60"));
        e.getBillData().put(ErpAstConstants.BILL_DATA_ORIGINAL_VALUE, new BigDecimal("100"));
        e.getBillData().put(ErpAstConstants.BILL_DATA_DISPOSAL_AMOUNT, new BigDecimal("50"));
        e.getBillData().put(ErpAstConstants.BILL_DATA_GAIN_LOSS, new BigDecimal("10"));
        e.getBillData().put(ErpAstConstants.BILL_DATA_DISPOSAL_GAINLOSS_SUBJECT_CODE, "6301");
        // 出售收益：借 1602(60) / 借 1606(净值40) / 贷 1601(100) / 借 1002(50) / 贷 1606(50) /
        //           借 1606(收益10) / 贷 6301(10)（1606 网为零）
        List<VoucherFact> facts = p.createFacts(e, null);
        assertKeys(facts, "ACCUMULATED_DEPRECIATION", "DISPOSAL_CLEARING", "FIXED_ASSET",
                "BANK_DEPOSIT", "DISPOSAL_CLEARING", "DISPOSAL_CLEARING", "NON_OPERATING_EXPENSE");
        assertDisposalClearingIdentity(facts, new BigDecimal("50"), new BigDecimal("50"));
        assertEquals("6301", facts.get(6).getSubjectCode(), "出售收益贷 6301 营业外收入");
        assertBalanced(facts);
    }

    @Test
    public void testAssetInventorySurplus() {
        AssetInventoryAcctDocProvider p = new AssetInventoryAcctDocProvider();
        PostingEvent e = event(ErpFinBusinessType.ASSET_INVENTORY_ADJUSTMENT);
        e.getBillData().put(ErpAstConstants.BILL_DATA_INVENTORY_SURPLUS_AMOUNT, AMT);
        assertKeys(p.createFacts(e, null), "FIXED_ASSET", "NON_OPERATING_INCOME");
    }

    @Test
    public void testAssetInventoryShortage() {
        AssetInventoryAcctDocProvider p = new AssetInventoryAcctDocProvider();
        PostingEvent e = event(ErpFinBusinessType.ASSET_INVENTORY_ADJUSTMENT);
        e.getBillData().put(ErpAstConstants.BILL_DATA_INVENTORY_SHORTAGE_AMOUNT, AMT);
        assertKeys(p.createFacts(e, null), "NON_OPERATING_EXPENSE", "FIXED_ASSET");
    }

    @Test
    public void testValueAdjustmentImpairment() {
        ValueAdjustmentAcctDocProvider p = new ValueAdjustmentAcctDocProvider();
        PostingEvent e = event(ErpFinBusinessType.VALUE_ADJUSTMENT);
        e.getBillData().put(ErpAstConstants.BILL_DATA_ADJUSTMENT_AMOUNT, AMT);
        e.getBillData().put(ErpAstConstants.BILL_DATA_ADJUSTMENT_TYPE, ErpAstConstants.ADJUSTMENT_TYPE_IMPAIRMENT);
        assertKeys(p.createFacts(e, null), "IMPAIRMENT_LOSS", "IMPAIRMENT_PROVISION");
    }

    @Test
    public void testValueAdjustmentRevaluationUp() {
        ValueAdjustmentAcctDocProvider p = new ValueAdjustmentAcctDocProvider();
        PostingEvent e = event(ErpFinBusinessType.VALUE_ADJUSTMENT);
        e.getBillData().put(ErpAstConstants.BILL_DATA_ADJUSTMENT_AMOUNT, AMT);
        e.getBillData().put(ErpAstConstants.BILL_DATA_ADJUSTMENT_TYPE, ErpAstConstants.ADJUSTMENT_TYPE_REVALUATION_UP);
        assertKeys(p.createFacts(e, null), "FIXED_ASSET", "CAPITAL_RESERVE");
    }

    @Test
    public void testCapitalizationDirectPurchase() {
        CapitalizationAcctDocProvider p = new CapitalizationAcctDocProvider();
        PostingEvent e = event(ErpFinBusinessType.CAPITALIZATION);
        e.getBillData().put(ErpAstConstants.BILL_DATA_ORIGINAL_VALUE, AMT);
        e.getBillData().put(ErpAstConstants.BILL_DATA_SOURCE_TYPE, ErpAstConstants.SOURCE_TYPE_DIRECT_PURCHASE);
        assertKeys(p.createFacts(e, null), "FIXED_ASSET", "BANK_DEPOSIT");
    }

    @Test
    public void testCapitalizationCip() {
        CapitalizationAcctDocProvider p = new CapitalizationAcctDocProvider();
        PostingEvent e = event(ErpFinBusinessType.CAPITALIZATION);
        e.getBillData().put(ErpAstConstants.BILL_DATA_ORIGINAL_VALUE, AMT);
        e.getBillData().put(ErpAstConstants.BILL_DATA_SOURCE_TYPE, ErpAstConstants.SOURCE_TYPE_CIP);
        assertKeys(p.createFacts(e, null), "FIXED_ASSET", "CIP");
    }

    @Test
    public void testAssetSplit() {
        AssetSplitAcctDocProvider p = new AssetSplitAcctDocProvider();
        PostingEvent e = event(ErpFinBusinessType.ASSET_SPLIT);
        e.getBillData().put(ErpAstConstants.BILL_DATA_DEBIT_LINES, lineRows(AMT));
        e.getBillData().put(ErpAstConstants.BILL_DATA_CREDIT_LINES, lineRows(AMT));
        assertKeys(p.createFacts(e, null), "FIXED_ASSET", "FIXED_ASSET");
    }

    @Test
    public void testAssetMerge() {
        AssetMergeAcctDocProvider p = new AssetMergeAcctDocProvider();
        PostingEvent e = event(ErpFinBusinessType.ASSET_MERGE);
        e.getBillData().put(ErpAstConstants.BILL_DATA_DEBIT_LINES, lineRows(AMT));
        e.getBillData().put(ErpAstConstants.BILL_DATA_CREDIT_LINES, lineRows(AMT));
        assertKeys(p.createFacts(e, null), "FIXED_ASSET", "FIXED_ASSET");
    }

    @Test
    public void testMaintenanceCapitalizationLinked() {
        MaintenanceCapitalizationAcctDocProvider p = new MaintenanceCapitalizationAcctDocProvider();
        PostingEvent e = event(ErpFinBusinessType.MAINTENANCE_CAPITALIZATION);
        e.getBillData().put(ErpAstConstants.BILL_DATA_MAINTENANCE_CAPITALIZED_AMOUNT, AMT);
        e.getBillData().put(ErpAstConstants.BILL_DATA_MAINTENANCE_LINKED_VISIT, Boolean.TRUE);
        assertKeys(p.createFacts(e, null), "FIXED_ASSET", "MAINTENANCE_CLEARING");
    }

    @Test
    public void testMaintenanceExpenseLinked() {
        MaintenanceExpenseAcctDocProvider p = new MaintenanceExpenseAcctDocProvider();
        PostingEvent e = event(ErpFinBusinessType.MAINTENANCE_EXPENSE);
        e.getBillData().put(ErpAstConstants.BILL_DATA_MAINTENANCE_TOTAL_COST, AMT);
        e.getBillData().put(ErpAstConstants.BILL_DATA_MAINTENANCE_LINKED_VISIT, Boolean.TRUE);
        assertKeys(p.createFacts(e, null), "MAINTENANCE_EXPENSE", "MAINTENANCE_CLEARING");
    }

    private List<Map<String, Object>> lineRows(BigDecimal amt) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("subjectCode", "1601");
        row.put("subjectName", "固定资产");
        row.put("amount", amt);
        return List.of(row);
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

    /**
     * RC-R1.53 1606 固定资产清理中间科目腿恒等式：借腿合计 = 贷腿合计（网为零，两步流不残留余额）。
     */
    private void assertDisposalClearingIdentity(List<VoucherFact> facts, BigDecimal expectedDebit, BigDecimal expectedCredit) {
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        for (VoucherFact fact : facts) {
            if (!"DISPOSAL_CLEARING".equals(fact.getAccountKey())) {
                continue;
            }
            assertEquals("1606", fact.getSubjectCode(), "1606 腿科目编码=1606");
            if ("DEBIT".equals(fact.getDcDirection())) {
                debit = debit.add(fact.getAmount());
            } else {
                credit = credit.add(fact.getAmount());
            }
        }
        assertEquals(0, debit.compareTo(expectedDebit), "1606 借腿合计");
        assertEquals(0, credit.compareTo(expectedCredit), "1606 贷腿合计");
    }

    /** GL 恒等式：Dr Σ = Cr Σ。 */
    private void assertBalanced(List<VoucherFact> facts) {
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        for (VoucherFact fact : facts) {
            if ("DEBIT".equals(fact.getDcDirection())) {
                debit = debit.add(fact.getAmount());
            } else {
                credit = credit.add(fact.getAmount());
            }
        }
        assertEquals(0, debit.compareTo(credit), "GL 借贷平衡 Dr Σ = Cr Σ");
    }
}
