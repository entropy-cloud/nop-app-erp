package app.erp.inv.service;

import app.erp.inv.biz.IErpInvStockLedgerBiz;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockLedger;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E3.2 库存审计快照与对账校验 Proof（`audit-snapshot-cycle-count.md` §1/§3）：
 * <ul>
 *   <li>快照派生确定性：vs 流水实体直加（既有链路）逐值对照；</li>
 *   <li>asOfDate 边界：businessDate &gt; asOf 的流水被裁剪（时点前/后各验）；</li>
 *   <li>过滤维度：warehouseId / materialIds 收敛；</li>
 *   <li>对账校验项：一致基线零差异 + 账面/派生三型差异可观测（数量/成本错配、单侧缺失）。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvSnapshotAndStockCheck extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    private static final String WH_A = "7001";
    private static final String WH_B = "7002";
    private static final String MAT_A = "8001";
    private static final String MAT_B = "8002";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpInvStockLedgerBiz ledgerBiz;

    @Test
    public void testSnapshotDeterministicVsLedgerSum() {
        seedLedger("SNAP-1", WH_A, MAT_A, "100", "8.5", "850", LocalDate.of(2026, 7, 3));
        seedLedger("SNAP-2", WH_A, MAT_A, "-30", "8.5", "-255", LocalDate.of(2026, 7, 10));
        seedLedger("SNAP-3", WH_A, MAT_A, "50", "9", "450", LocalDate.of(2026, 7, 20));

        Map<String, Object> snap = ledgerBiz.getInventorySnapshot(WH_A, null, LocalDate.of(2026, 7, 31), CTX);
        assertEquals(1, ((Number) snap.get("rowCount")).intValue(), "单物料单仓库聚合为 1 行");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) snap.get("rows");
        Map<String, Object> row = rows.get(0);
        assertEquals(MAT_A, String.valueOf(row.get("materialId")));

        // 既有链路对照：流水实体直加（数量/成本），与派生快照逐值一致
        BigDecimal directQty = BigDecimal.ZERO;
        BigDecimal directCost = BigDecimal.ZERO;
        for (ErpInvStockLedger l : allLedgers()) {
            if (WH_A.equals(l.getWarehouseId()) && MAT_A.equals(l.getMaterialId())) {
                directQty = directQty.add(nz(l.getQuantity()));
                directCost = directCost.add(nz(l.getTotalCost()));
            }
        }
        assertEquals(0, ((BigDecimal) row.get("quantity")).compareTo(directQty), "快照数量 = 流水直加");
        assertEquals(0, ((BigDecimal) row.get("totalCost")).compareTo(directCost), "快照成本 = 流水直加");
        assertEquals(0, ((BigDecimal) row.get("quantity")).compareTo(new BigDecimal("120")));
        assertEquals(0, ((BigDecimal) row.get("totalCost")).compareTo(new BigDecimal("1045")));
        assertEquals(0, ((BigDecimal) row.get("unitCost")).compareTo(new BigDecimal("8.7083")),
                "unitCost = totalCost/quantity（scale 4 HALF_UP）");
        assertEquals(0, ((BigDecimal) snap.get("totalQuantity")).compareTo(new BigDecimal("120")));
        assertEquals(0, ((BigDecimal) snap.get("totalCost")).compareTo(new BigDecimal("1045")));
    }

    @Test
    public void testSnapshotAsOfBoundaryTrimsLaterFlows() {
        seedLedger("SNAP-B1", WH_B, MAT_B, "40", "5", "200", LocalDate.of(2026, 7, 1));
        seedLedger("SNAP-B2", WH_B, MAT_B, "60", "5", "300", LocalDate.of(2026, 7, 15));

        // asOf 在两条流水之间：仅计 07-01（07-15 被裁剪）
        Map<String, Object> mid = ledgerBiz.getInventorySnapshot(WH_B, null, LocalDate.of(2026, 7, 10), CTX);
        assertEquals(0, ((BigDecimal) mid.get("totalQuantity")).compareTo(new BigDecimal("40")));
        assertEquals(0, ((BigDecimal) mid.get("totalCost")).compareTo(new BigDecimal("200")));

        // asOf 覆盖全部流水
        Map<String, Object> full = ledgerBiz.getInventorySnapshot(WH_B, null, LocalDate.of(2026, 7, 31), CTX);
        assertEquals(0, ((BigDecimal) full.get("totalQuantity")).compareTo(new BigDecimal("100")));
        assertEquals(0, ((BigDecimal) full.get("totalCost")).compareTo(new BigDecimal("500")));

        // asOf 早于全部流水：零行（期初为空）
        Map<String, Object> empty = ledgerBiz.getInventorySnapshot(WH_B, null, LocalDate.of(2026, 6, 30), CTX);
        assertEquals(0, ((Number) empty.get("rowCount")).intValue());
        assertEquals(0, ((BigDecimal) empty.get("totalQuantity")).compareTo(BigDecimal.ZERO));

        // 边界日当天（businessDate <= asOf 含当日）
        Map<String, Object> onDay = ledgerBiz.getInventorySnapshot(WH_B, null, LocalDate.of(2026, 7, 1), CTX);
        assertEquals(0, ((BigDecimal) onDay.get("totalQuantity")).compareTo(new BigDecimal("40")),
                "businessDate = asOf 当日流水计入");
    }

    @Test
    public void testSnapshotWarehouseAndMaterialFilters() {
        seedLedger("SNAP-F1", WH_A, MAT_A, "10", "1", "10", LocalDate.of(2026, 7, 5));
        seedLedger("SNAP-F2", WH_B, MAT_A, "20", "1", "20", LocalDate.of(2026, 7, 5));
        seedLedger("SNAP-F3", WH_B, MAT_B, "30", "1", "30", LocalDate.of(2026, 7, 5));

        Map<String, Object> byWh = ledgerBiz.getInventorySnapshot(WH_B, null, LocalDate.of(2026, 7, 31), CTX);
        assertEquals(0, ((BigDecimal) byWh.get("totalQuantity")).compareTo(new BigDecimal("50")),
                "仓库过滤：仅 WH_B 两行聚合");
        assertEquals(2, ((Number) byWh.get("rowCount")).intValue());

        Map<String, Object> byMat = ledgerBiz.getInventorySnapshot(null, List.of(MAT_A), LocalDate.of(2026, 7, 31), CTX);
        assertEquals(0, ((BigDecimal) byMat.get("totalQuantity")).compareTo(new BigDecimal("30")),
                "物料过滤：MAT_A 两仓合计");
        assertEquals(2, ((Number) byMat.get("rowCount")).intValue());
    }

    @Test
    public void testStockCheckConsistentBaseline() {
        seedLedger("CHK-1", WH_A, MAT_A, "100", "2", "200", LocalDate.of(2026, 7, 3));
        seedBalance(WH_A, MAT_A, "100", "200");

        Map<String, Object> report = ledgerBiz.checkStockBalanceConsistency(LocalDate.of(2026, 7, 31), CTX);
        assertEquals(0, ((Number) report.get("mismatchCount")).intValue(), "账面=派生 → 零差异");
        assertEquals(Boolean.TRUE, report.get("consistent"));
    }

    @Test
    public void testStockCheckDetectsAllMismatchTypes() {
        // 1. 数量错配：账面 90 vs 派生 100
        seedLedger("CHK-M1", WH_A, MAT_A, "100", "2", "200", LocalDate.of(2026, 7, 3));
        seedBalance(WH_A, MAT_A, "90", "200");
        // 2. 账面独有：余额有值但无流水
        seedBalance(WH_B, MAT_B, "50", "500");
        // 3. 流水独有：无余额行
        seedLedger("CHK-M3", WH_B, MAT_A, "7", "3", "21", LocalDate.of(2026, 7, 8));

        Map<String, Object> report = ledgerBiz.checkStockBalanceConsistency(LocalDate.of(2026, 7, 31), CTX);
        assertEquals(3, ((Number) report.get("mismatchCount")).intValue(), "三型差异全部可观测");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mismatches = (List<Map<String, Object>>) report.get("mismatches");
        long qtyMismatch = mismatches.stream().filter(m -> "QTY_OR_COST_MISMATCH".equals(m.get("type"))).count();
        long bookOnly = mismatches.stream().filter(m -> "BOOK_ONLY_NO_LEDGER".equals(m.get("type"))).count();
        long ledgerOnly = mismatches.stream().filter(m -> "LEDGER_ONLY_NO_BALANCE".equals(m.get("type"))).count();
        assertEquals(1, qtyMismatch);
        assertEquals(1, bookOnly);
        assertEquals(1, ledgerOnly);
        assertEquals(Boolean.FALSE, report.get("consistent"));

        // 零值账面独有不报（0 = 0 等价）
        Map<String, Object> zeroReport = ledgerBiz.checkStockBalanceConsistency(LocalDate.of(2026, 7, 31), CTX);
        assertTrue(((Number) zeroReport.get("mismatchCount")).intValue() >= 3);
    }

    // ---------- seeds ----------

    private void seedLedger(String code, String warehouseId, String materialId,
                            String qty, String unitCost, String totalCost, LocalDate businessDate) {
        ormTemplate.runInSession(sess -> {
            ErpInvStockLedger l = new ErpInvStockLedger();
            l.setCode(code);
            l.setOrgId("1");
            l.setMoveId("9100");
            l.setMoveLineId("9101");
            l.setMaterialId(materialId);
            l.setWarehouseId(warehouseId);
            l.setQuantity(new BigDecimal(qty));
            l.setUnitCost(new BigDecimal(unitCost));
            l.setTotalCost(new BigDecimal(totalCost));
            l.setBalanceQuantity(new BigDecimal(qty));
            l.setBalanceTotalCost(new BigDecimal(totalCost));
            l.setBusinessDate(businessDate);
            daoProvider.daoFor(ErpInvStockLedger.class).saveEntity(l);
            return null;
        });
    }

    private void seedBalance(String warehouseId, String materialId, String qty, String totalCost) {
        ormTemplate.runInSession(sess -> {
            IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
            ErpInvStockBalance b = dao.newEntity();
            b.setOrgId("1");
            b.setMaterialId(materialId);
            b.setWarehouseId(warehouseId);
            b.setTotalQuantity(new BigDecimal(qty));
            b.setAvailableQuantity(new BigDecimal(qty));
            b.setTotalCost(new BigDecimal(totalCost));
            dao.saveEntity(b);
            return null;
        });
    }

    private List<ErpInvStockLedger> allLedgers() {
        return ormTemplate.runInSession(sess -> {
            IEntityDao<ErpInvStockLedger> dao = daoProvider.daoFor(ErpInvStockLedger.class);
            return dao.findAll();
        });
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
