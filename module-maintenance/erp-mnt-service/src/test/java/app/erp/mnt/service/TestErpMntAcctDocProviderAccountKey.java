package app.erp.mnt.service;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.service.posting.VoucherFact;
import app.erp.mnt.service.posting.MaintenanceIssueAcctDocProvider;
import app.erp.mnt.service.posting.MaintenanceLaborAcctDocProvider;
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
 * maintenance 域 2 Provider GL 映射 accountKey 接入单元测试（plan 2026-07-24-1351-1 Phase 3 抽样）。
 * 命中/fallback/strict 三路径由 resolver 承担（{@code TestErpFinGlMappingResolver}）。
 */
public class TestErpMntAcctDocProviderAccountKey extends BaseTestCase {

    @Test
    public void testMaintenanceLaborKeys() {
        MaintenanceLaborAcctDocProvider provider = new MaintenanceLaborAcctDocProvider();
        PostingEvent e = new PostingEvent();
        e.setBusinessType(ErpFinBusinessType.MAINTENANCE_LABOR);
        e.getBillData().put("TOTAL", new BigDecimal("200"));
        e.getBillData().put("EQUIPMENT_CODE", "EQ1");
        e.getBillData().put("VISIT_CODE", "V1");

        List<VoucherFact> facts = provider.createFacts(e, null);
        assertKeys(facts, "MAINTENANCE_EXPENSE", "SALARY_PAYABLE");
    }

    @Test
    public void testMaintenanceIssueKeys() {
        MaintenanceIssueAcctDocProvider provider = new MaintenanceIssueAcctDocProvider();
        PostingEvent e = new PostingEvent();
        e.setBusinessType(ErpFinBusinessType.MAINTENANCE_ISSUE);
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("MATERIAL_CODE", "M1");
        line.put("MATERIAL_AMOUNT", new BigDecimal("80"));
        line.put("INVENTORY_SUBJECT", "1403");
        e.getBillData().put("LINES", List.of(line));
        e.getBillData().put("EQUIPMENT_CODE", "EQ1");

        List<VoucherFact> facts = provider.createFacts(e, null);
        // 贷存货(按物料) + 借维修费用(汇总)
        assertKeys(facts, "INVENTORY", "MAINTENANCE_EXPENSE");
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
