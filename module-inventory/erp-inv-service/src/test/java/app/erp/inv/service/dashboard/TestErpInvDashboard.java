package app.erp.inv.service.dashboard;

import app.erp.inv.dao.entity.ErpInvBatch;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.inv.service.ErpInvConstants;
import app.erp.md.dao.entity.ErpMdMaterial;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.config.AppConfig;
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
 * 库存看板聚合（{@code ErpInvDashboard__*}）集成测试。覆盖：库存总值/周转率/出入库量、
 * 仓库分布、缺料预警、滞销预警（触发/不触发两路径）、批次效期预警（触发/不触发两路径）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvDashboard extends JunitAutoTestCase {

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
            seedMaterial(101L, BigDecimal.ZERO);
            // 余额 totalCost=1000
            seedBalance(201L, 101L, 1L, new BigDecimal("100"), new BigDecimal("1000"));
            // 出库移动：DONE + OUTGOING，行 totalCost=200
            ErpInvStockMove m = seedMove(301L, ErpInvConstants.MOVE_TYPE_OUTGOING, ErpInvConstants.DOC_STATUS_DONE, LocalDate.now());
            seedMoveLine(401L, 301L, 101L, new BigDecimal("-10"), new BigDecimal("200"));
            // 入库移动：DONE + INCOMING，行 quantity=50
            ErpInvStockMove m2 = seedMove(302L, ErpInvConstants.MOVE_TYPE_INCOMING, ErpInvConstants.DOC_STATUS_DONE, LocalDate.now());
            seedMoveLine(402L, 302L, 101L, new BigDecimal("50"), new BigDecimal("500"));
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
            seedMaterial(111L, BigDecimal.ZERO);
            seedBalance(211L, 111L, 1L, new BigDecimal("10"), new BigDecimal("100"));
            seedBalance(212L, 111L, 2L, new BigDecimal("20"), new BigDecimal("300"));
        });
        List<Map<String, Object>> dist = dashboardBiz.findWarehouseDistribution(CTX);
        assertEquals(2, dist.size());
        // 仓库 2 (300) > 仓库 1 (100) → 排序后仓库 2 在前
        assertEquals(2L, dist.get(0).get("warehouseId"));
    }

    @Test
    public void testShortageAlert() {
        ormTemplate.runInSession(() -> {
            // 物料 121 安全库存 50
            seedMaterial(121L, new BigDecimal("50"));
            // 余量 30 < 50 → 缺料
            seedBalance(221L, 121L, 1L, new BigDecimal("30"), new BigDecimal("100"));
            // 余量 60 > 50 → 不缺料
            seedBalance(222L, 121L, 1L, new BigDecimal("60"), new BigDecimal("200"));
        });
        List<Map<String, Object>> alerts = dashboardBiz.findShortageAlert(CTX);
        assertEquals(1, alerts.size(), "30 < 50 触发 1 条缺料预警");
        assertEquals(121L, alerts.get(0).get("materialId"));
    }

    @Test
    public void testSlowMovingAlertDisabledByDefault() {
        ormTemplate.runInSession(() -> {
            seedMaterial(131L, BigDecimal.ZERO);
            seedBalance(231L, 131L, 1L, new BigDecimal("10"), new BigDecimal("100"));
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
            seedMaterial(141L, BigDecimal.ZERO);
            // 有库存但无任何出库记录 → 视为滞销
            seedBalance(241L, 141L, 1L, new BigDecimal("10"), new BigDecimal("100"));
        });
        AppConfig.getConfigProvider().assignConfigValue(
                ErpInvConstants.CONFIG_DASH_INV_SLOW_MOVING_DAYS, "30");
        try {
            List<Map<String, Object>> alerts = dashboardBiz.findSlowMovingAlert(CTX);
            assertEquals(1, alerts.size(), "无出库记录 + 库存 > 0 → 滞销");
            assertEquals(141L, alerts.get(0).get("materialId"));
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpInvConstants.CONFIG_DASH_INV_SLOW_MOVING_DAYS, "0");
        }
    }

    @Test
    public void testBatchExpiryAlertDisabledByDefault() {
        ormTemplate.runInSession(() -> {
            seedMaterial(151L, BigDecimal.ZERO);
            seedBatch(251L, "BAT-SOON", 151L, 1L, LocalDate.now().plusDays(5));
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
            seedMaterial(161L, BigDecimal.ZERO);
            // 7 天后过期 < 阈值 30 天 → 触发
            seedBatch(261L, "BAT-7D", 161L, 1L, LocalDate.now().plusDays(7));
            // 100 天后过期 > 阈值 30 天 → 不触发
            seedBatch(262L, "BAT-100D", 161L, 1L, LocalDate.now().plusDays(100));
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

    private void seedMaterial(long id, BigDecimal safetyStock) {
        IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
        ErpMdMaterial m = dao.newEntity();
        m.orm_propValue(1, id);
        m.setCode("M-" + id);
        m.setName("Material " + id);
        m.setMaterialType("GOODS");
        m.setUoMId(1L);
        m.setStatus("ACTIVE");
        m.setSafetyStock(safetyStock);
        dao.saveEntity(m);
    }

    private void seedBalance(long id, long materialId, long warehouseId,
                             BigDecimal qty, BigDecimal totalCost) {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        ErpInvStockBalance b = dao.newEntity();
        b.orm_propValue(1, id);
        b.setOrgId(1L);
        b.setMaterialId(materialId);
        b.setWarehouseId(warehouseId);
        b.setTotalQuantity(qty);
        b.setReservedQuantity(BigDecimal.ZERO);
        b.setLockedQuantity(BigDecimal.ZERO);
        b.setAvailableQuantity(qty);
        b.setCostMethod(ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
        b.setAvgCost(BigDecimal.ONE);
        b.setTotalCost(totalCost);
        b.setCurrencyId(1L);
        dao.saveEntity(b);
    }

    private ErpInvStockMove seedMove(long id, String moveType, String docStatus, LocalDate date) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        ErpInvStockMove m = dao.newEntity();
        m.orm_propValue(1, id);
        m.setCode("MV-" + id);
        m.setMoveType(moveType);
        m.setOrgId(1L);
        m.setBusinessDate(date);
        m.setDocStatus(docStatus);
        m.setApproveStatus("APPROVED");
        dao.saveEntity(m);
        return m;
    }

    private void seedMoveLine(long id, long moveId, long materialId,
                              BigDecimal qty, BigDecimal totalCost) {
        IEntityDao<ErpInvStockMoveLine> dao = daoProvider.daoFor(ErpInvStockMoveLine.class);
        ErpInvStockMoveLine l = dao.newEntity();
        l.orm_propValue(1, id);
        l.setMoveId(moveId);
        l.setLineNo(1);
        l.setMaterialId(materialId);
        l.setUoMId(1L);
        l.setQuantity(qty);
        l.setUnitCost(BigDecimal.ONE);
        l.setTotalCost(totalCost);
        dao.saveEntity(l);
    }

    private void seedBatch(long id, String batchNo, long materialId, long warehouseId, LocalDate expiry) {
        IEntityDao<ErpInvBatch> dao = daoProvider.daoFor(ErpInvBatch.class);
        ErpInvBatch b = dao.newEntity();
        b.orm_propValue(1, id);
        b.setOrgId(1L);
        b.setBatchNo(batchNo);
        b.setMaterialId(materialId);
        b.setWarehouseId(warehouseId);
        b.setTotalQuantity(BigDecimal.TEN);
        b.setAvailableQuantity(BigDecimal.TEN);
        b.setProductionDate(LocalDate.now().minusDays(30));
        b.setExpiryDate(expiry);
        b.setStatus("ACTIVE");
        dao.saveEntity(b);
    }
}
