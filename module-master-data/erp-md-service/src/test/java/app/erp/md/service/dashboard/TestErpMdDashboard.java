package app.erp.md.service.dashboard;

import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdMaterialSku;
import app.erp.md.dao.entity.ErpMdPartner;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 主数据看板聚合（{@code ErpMdDashboard__*}）集成测试。覆盖：物料/往来单位计数（按 partnerType 分）、
 * 停用主数据计数、无 SKU 物料预警触发/不触发、无价格 SKU 预警触发/不触发、空数据集零值。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMdDashboard extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ErpMdDashboardBizModel dashboardBiz;

    @Test
    public void testKpiEmptyDatasetReturnsZeros() {
        Map<String, Object> kpi = dashboardBiz.getDashboardKpi(CTX);
        assertEquals(0L, kpi.get("materialCount"));
        assertEquals(0L, kpi.get("customerCount"));
        assertEquals(0L, kpi.get("vendorCount"));
        assertEquals(0L, kpi.get("inactiveMaterialCount"));
        assertEquals(0L, kpi.get("inactivePartnerCount"));
    }

    @Test
    public void testKpiCountsByPartnerTypeAndStatus() {
        ormTemplate.runInSession(() -> {
            // 3 物料：1 启用 + 2 停用
            seedMaterial(101L, "M-A", "ACTIVE");
            seedMaterial(102L, "M-B", "INACTIVE");
            seedMaterial(103L, "M-C", "INACTIVE");
            // 4 往来单位：2 客户（1 启用 + 1 停用）+ 2 供应商（均启用）
            seedPartner(201L, "P-CUST-1", "CUSTOMER", "ACTIVE");
            seedPartner(202L, "P-CUST-2", "CUSTOMER", "INACTIVE");
            seedPartner(203L, "P-VEND-1", "SUPPLIER", "ACTIVE");
            seedPartner(204L, "P-VEND-2", "SUPPLIER", "ACTIVE");
        });

        Map<String, Object> kpi = dashboardBiz.getDashboardKpi(CTX);
        assertEquals(3L, kpi.get("materialCount"));
        assertEquals(2L, kpi.get("customerCount"));
        assertEquals(2L, kpi.get("vendorCount"));
        assertEquals(2L, kpi.get("inactiveMaterialCount"));
        assertEquals(1L, kpi.get("inactivePartnerCount"));
    }

    @Test
    public void testMaterialWithoutSkuAlertTriggersAndNot() {
        ormTemplate.runInSession(() -> {
            // 物料 A: 有 SKU → 不触发
            seedMaterial(111L, "M-WITH-SKU", "ACTIVE");
            seedSku(211L, 111L, new BigDecimal("10"));
            // 物料 B: 无 SKU → 触发
            seedMaterial(112L, "M-WITHOUT-SKU", "ACTIVE");
        });
        List<Map<String, Object>> alerts = dashboardBiz.findMaterialWithoutSkuAlert(CTX);
        assertEquals(1, alerts.size(), "仅物料 B 触发预警");
        assertEquals(112L, alerts.get(0).get("materialId"));
    }

    @Test
    public void testSkuWithoutPriceAlertTriggersAndNot() {
        ormTemplate.runInSession(() -> {
            seedMaterial(121L, "M-1", "ACTIVE");
            seedMaterial(122L, "M-2", "ACTIVE");
            // SKU A: 有采购价 → 不触发
            seedSku(221L, 121L, new BigDecimal("10"));
            // SKU B: 无任何价格 → 触发
            seedSkuWithoutPrice(222L, 122L);
        });
        List<Map<String, Object>> alerts = dashboardBiz.findSkuWithoutPriceAlert(CTX);
        assertEquals(1, alerts.size(), "仅 SKU B 触发预警");
        assertEquals(222L, alerts.get(0).get("skuId"));
    }

    // ---------- helpers ----------

    private void seedMaterial(long id, String code, String status) {
        IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
        ErpMdMaterial m = dao.newEntity();
        m.orm_propValue(1, id);
        m.setCode(code);
        m.setName("物料-" + code);
        m.setMaterialType("GOODS");
        m.setUoMId(1L);
        m.setStatus(status);
        m.setCostMethod("MOVING_AVERAGE");
        dao.saveEntity(m);
    }

    private void seedPartner(long id, String code, String partnerType, String status) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner p = dao.newEntity();
        p.orm_propValue(1, id);
        p.setCode(code);
        p.setName("单位-" + code);
        p.setPartnerType(partnerType);
        p.setStatus(status);
        p.setReceivableBalance(BigDecimal.ZERO);
        p.setPayableBalance(BigDecimal.ZERO);
        dao.saveEntity(p);
    }

    private void seedSku(long id, long materialId, BigDecimal purchasePrice) {
        IEntityDao<ErpMdMaterialSku> dao = daoProvider.daoFor(ErpMdMaterialSku.class);
        ErpMdMaterialSku s = dao.newEntity();
        s.orm_propValue(1, id);
        s.setMaterialId(materialId);
        s.setSkuCode("SKU-" + id);
        s.setUoMId(1L);
        s.setConversionRate(BigDecimal.ONE);
        s.setPurchasePrice(purchasePrice);
        dao.saveEntity(s);
    }

    private void seedSkuWithoutPrice(long id, long materialId) {
        IEntityDao<ErpMdMaterialSku> dao = daoProvider.daoFor(ErpMdMaterialSku.class);
        ErpMdMaterialSku s = dao.newEntity();
        s.orm_propValue(1, id);
        s.setMaterialId(materialId);
        s.setSkuCode("SKU-NOPRICE-" + id);
        s.setUoMId(1L);
        s.setConversionRate(BigDecimal.ONE);
        dao.saveEntity(s);
    }
}
