package app.erp.inv.service.dashboard;

import app.erp.inv.dao.entity.ErpInvBatch;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.inv.service.ErpInvConstants;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdWarehouse;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import app.erp.inv.service.InvFrozenClockExtension;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 库存看板聚合（{@code ErpInvDashboard__*}）集成测试。覆盖：库存总值/周转率/出入库量、
 * 仓库分布、缺料预警、滞销预警（触发/不触发两路径）、批次效期预警（触发/不触发两路径）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvDashboard extends JunitAutoTestCase {

    @RegisterExtension
    static InvFrozenClockExtension frozenClock = new InvFrozenClockExtension();

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ErpInvDashboardBizModel dashboardBiz;

    @Test
    public void testKpiEmptyDatasetReturnsZeros() {
        Map<String, Object> kpi = dashboardBiz.getDashboardKpi(null, null, CTX);
        assertEquals(0, ((BigDecimal) kpi.get("totalValue")).compareTo(BigDecimal.ZERO));
        assertEquals(0, ((BigDecimal) kpi.get("incomingQty")).compareTo(BigDecimal.ZERO));
        assertEquals(0, ((BigDecimal) kpi.get("outgoingQty")).compareTo(BigDecimal.ZERO));
        assertEquals(0, ((BigDecimal) kpi.get("turnoverRate")).compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testKpiTotalValueAndTurnover() {
        ormTemplate.runInSession(() -> {
            seedMaterial("101", BigDecimal.ZERO);
            // 余额 totalCost=1000
            seedBalance("201", "101", "1", new BigDecimal("100"), new BigDecimal("1000"));
            // 出库移动：DONE + OUTGOING，行 totalCost=200
            ErpInvStockMove m = seedMove("301", ErpInvConstants.MOVE_TYPE_OUTGOING, ErpInvConstants.DOC_STATUS_DONE, CoreMetrics.currentDate());
            seedMoveLine("401", "301", "101", new BigDecimal("-10"), new BigDecimal("200"));
            // 入库移动：DONE + INCOMING，行 quantity=50
            ErpInvStockMove m2 = seedMove("302", ErpInvConstants.MOVE_TYPE_INCOMING, ErpInvConstants.DOC_STATUS_DONE, CoreMetrics.currentDate());
            seedMoveLine("402", "302", "101", new BigDecimal("50"), new BigDecimal("500"));
        });
        Map<String, Object> kpi = dashboardBiz.getDashboardKpi(null, null, CTX);
        assertEquals(0, ((BigDecimal) kpi.get("totalValue")).compareTo(new BigDecimal("1000")));
        assertEquals(0, ((BigDecimal) kpi.get("incomingQty")).compareTo(new BigDecimal("50")));
        assertEquals(0, ((BigDecimal) kpi.get("outgoingQty")).compareTo(new BigDecimal("10")));
        // 周转率 = 出库成本 200 / 平均库存 1000 = 0.2
        assertEquals(0, ((BigDecimal) kpi.get("turnoverRate")).compareTo(new BigDecimal("0.2000")));
    }

    @Test
    public void testWarehouseDistribution() {
        ormTemplate.runInSession(() -> {
            seedMaterial("111", BigDecimal.ZERO);
            seedWarehouse("1", "中央仓");
            seedWarehouse("2", "华东仓");
            seedBalance("211", "111", "1", new BigDecimal("10"), new BigDecimal("100"));
            seedBalance("212", "111", "2", new BigDecimal("20"), new BigDecimal("300"));
        });
        List<Map<String, Object>> dist = dashboardBiz.findWarehouseDistribution(CTX);
        assertEquals(2, dist.size());
        // 仓库 2 (300) > 仓库 1 (100) → 排序后仓库 2 在前
        assertEquals("2", dist.get(0).get("warehouseId"));
        assertEquals("华东仓", dist.get(0).get("warehouseName"), "仓库名称已解析");
        assertEquals("中央仓", dist.get(1).get("warehouseName"), "仓库名称已解析");
    }

    @Test
    public void testShortageAlert() {
        ormTemplate.runInSession(() -> {
            // 物料 121 安全库存 50
            seedMaterial("121", new BigDecimal("50"));
            // 余量 30 < 50 → 缺料
            seedBalance("221", "121", "1", new BigDecimal("30"), new BigDecimal("100"));
            // 余量 60 > 50 → 不缺料
            seedBalance("222", "121", "1", new BigDecimal("60"), new BigDecimal("200"));
        });
        List<Map<String, Object>> alerts = dashboardBiz.findShortageAlert(CTX);
        assertEquals(1, alerts.size(), "30 < 50 触发 1 条缺料预警");
        assertEquals("121", alerts.get(0).get("materialId"));
    }

    @Test
    public void testSlowMovingAlertDisabledByDefault() {
        ormTemplate.runInSession(() -> {
            seedMaterial("131", BigDecimal.ZERO);
            seedBalance("231", "131", "1", new BigDecimal("10"), new BigDecimal("100"));
        });
        AppConfig.getConfigProvider().assignConfigValue(
                ErpInvConstants.CONFIG_DASH_INV_SLOW_MOVING_DAYS,
                String.valueOf(ErpInvConstants.DEFAULT_DASH_INV_SLOW_MOVING_DAYS));
        List<Map<String, Object>> alerts = dashboardBiz.findSlowMovingAlert(CTX);
        assertTrue(alerts.isEmpty(), "天数阈值默认 0=关闭 → 不触发预警");
    }

    @Test
    public void testSlowMovingAlertTriggersWhenNoOutgoing() {
        ormTemplate.runInSession(() -> {
            seedMaterial("141", BigDecimal.ZERO);
            // 有库存但无任何出库记录 → 视为滞销
            seedBalance("241", "141", "1", new BigDecimal("10"), new BigDecimal("100"));
        });
        AppConfig.getConfigProvider().assignConfigValue(
                ErpInvConstants.CONFIG_DASH_INV_SLOW_MOVING_DAYS, "30");
        try {
            List<Map<String, Object>> alerts = dashboardBiz.findSlowMovingAlert(CTX);
            assertEquals(1, alerts.size(), "无出库记录 + 库存 > 0 → 滞销");
            assertEquals("141", alerts.get(0).get("materialId"));
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpInvConstants.CONFIG_DASH_INV_SLOW_MOVING_DAYS, "0");
        }
    }

    @Test
    public void testBatchExpiryAlertDisabledByDefault() {
        ormTemplate.runInSession(() -> {
            seedMaterial("151", BigDecimal.ZERO);
            seedBatch("251", "BAT-SOON", "151", "1", CoreMetrics.currentDate().plusDays(5));
        });
        AppConfig.getConfigProvider().assignConfigValue(
                ErpInvConstants.CONFIG_DASH_INV_BATCH_EXPIRY_DAYS,
                String.valueOf(ErpInvConstants.DEFAULT_DASH_INV_BATCH_EXPIRY_DAYS));
        List<Map<String, Object>> alerts = dashboardBiz.findBatchExpiryAlert(CTX);
        assertTrue(alerts.isEmpty(), "天数阈值默认 0=关闭 → 不触发预警");
    }

    @Test
    public void testBatchExpiryAlertTriggers() {
        ormTemplate.runInSession(() -> {
            seedMaterial("161", BigDecimal.ZERO);
            // 7 天后过期 < 阈值 30 天 → 触发
            seedBatch("261", "BAT-7D", "161", "1", CoreMetrics.currentDate().plusDays(7));
            // 100 天后过期 > 阈值 30 天 → 不触发
            seedBatch("262", "BAT-100D", "161", "1", CoreMetrics.currentDate().plusDays(100));
        });
        AppConfig.getConfigProvider().assignConfigValue(
                ErpInvConstants.CONFIG_DASH_INV_BATCH_EXPIRY_DAYS, "30");
        try {
            List<Map<String, Object>> alerts = dashboardBiz.findBatchExpiryAlert(CTX);
            assertEquals(1, alerts.size(), "7 天 < 30 天阈值 → 触发 1 条");
            assertEquals("BAT-7D", alerts.get(0).get("batchNo"));
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpInvConstants.CONFIG_DASH_INV_BATCH_EXPIRY_DAYS, "0");
        }
    }

    // ---------- helpers ----------

    private void seedMaterial(String id, BigDecimal safetyStock) {
        IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
        ErpMdMaterial m = dao.newEntity();
        m.orm_propValue(1, id);
        m.setCode("M-" + id);
        m.setName("Material " + id);
        m.setMaterialType("GOODS");
        m.setUoMId("1");
        m.setStatus("ACTIVE");
        m.setSafetyStock(safetyStock);
        dao.saveEntity(m);
    }

    private void seedWarehouse(String id, String name) {
        IEntityDao<ErpMdWarehouse> dao = daoProvider.daoFor(ErpMdWarehouse.class);
        ErpMdWarehouse w = dao.newEntity();
        w.orm_propValue(1, id);
        w.setCode("WH-" + id);
        w.setName(name);
        w.setStatus("ACTIVE");
        dao.saveEntity(w);
    }

    private void seedBalance(String id, String materialId, String warehouseId,
                             BigDecimal qty, BigDecimal totalCost) {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        ErpInvStockBalance b = dao.newEntity();
        b.orm_propValue(1, id);
        b.setOrgId("1");
        b.setMaterialId(materialId);
        b.setWarehouseId(warehouseId);
        b.setTotalQuantity(qty);
        b.setReservedQuantity(BigDecimal.ZERO);
        b.setLockedQuantity(BigDecimal.ZERO);
        b.setAvailableQuantity(qty);
        b.setCostMethod(ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
        b.setAvgCost(BigDecimal.ONE);
        b.setTotalCost(totalCost);
        b.setCurrencyId("1");
        dao.saveEntity(b);
    }

    private ErpInvStockMove seedMove(String id, String moveType, String docStatus, LocalDate date) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        ErpInvStockMove m = dao.newEntity();
        m.orm_propValue(1, id);
        m.setCode("MV-" + id);
        m.setMoveType(moveType);
        m.setOrgId("1");
        m.setBusinessDate(date);
        m.setDocStatus(docStatus);
        m.setApproveStatus("APPROVED");
        dao.saveEntity(m);
        return m;
    }

    private void seedMoveLine(String id, String moveId, String materialId,
                              BigDecimal qty, BigDecimal totalCost) {
        IEntityDao<ErpInvStockMoveLine> dao = daoProvider.daoFor(ErpInvStockMoveLine.class);
        ErpInvStockMoveLine l = dao.newEntity();
        l.orm_propValue(1, id);
        l.setMoveId(moveId);
        l.setLineNo(1);
        l.setMaterialId(materialId);
        l.setUoMId("1");
        l.setQuantity(qty);
        l.setUnitCost(BigDecimal.ONE);
        l.setTotalCost(totalCost);
        dao.saveEntity(l);
    }

    private void seedBatch(String id, String batchNo, String materialId, String warehouseId, LocalDate expiry) {
        IEntityDao<ErpInvBatch> dao = daoProvider.daoFor(ErpInvBatch.class);
        ErpInvBatch b = dao.newEntity();
        b.orm_propValue(1, id);
        b.setOrgId("1");
        b.setBatchNo(batchNo);
        b.setMaterialId(materialId);
        b.setWarehouseId(warehouseId);
        b.setTotalQuantity(BigDecimal.TEN);
        b.setAvailableQuantity(BigDecimal.TEN);
        b.setProductionDate(CoreMetrics.currentDate().minusDays(30));
        b.setExpiryDate(expiry);
        b.setStatus("ACTIVE");
        dao.saveEntity(b);
    }
}
