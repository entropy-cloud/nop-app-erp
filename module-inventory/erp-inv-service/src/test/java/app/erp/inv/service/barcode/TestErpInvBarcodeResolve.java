package app.erp.inv.service.barcode;

import app.erp.inv.dao.entity.ErpInvStockTake;
import app.erp.inv.service.ErpInvConstants;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssue;
import app.erp.md.dao.entity.ErpMdLocation;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdMaterialSku;
import app.erp.md.dao.entity.ErpMdWarehouse;
import app.erp.md.dao.entity.ErpMdUoM;
import app.erp.pur.dao.entity.ErpPurReceive;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.api.core.exceptions.NopException;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * USC-05 条码解析路由测试（plan 2026-10-01-0930-1）。
 *
 * <p>覆盖：pur/mfg/inv-take 单据直查 + SKU 委托解析（materialId/uoMId 种子链）+
 * 库位 code + 双门控（barcode-enabled / pda-require-location-scan）。
 * Delivery 单号解析断言在 app-erp-all 集成测试（sal classpath 在聚合 classpath；
 * inv-service 单域 classpath 容错跳过——B1 裁决）。种单走 DAO 直种轻路径
 * （先例 TestErpInvStockTakeCompleteDiffMove seedTake）。
 */
@NopTestConfig(initDatabaseSchema = OptionalBoolean.TRUE, enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvBarcodeResolve extends JunitAutoTestCase {
    @Inject
    ErpInvBarcodeBizModel barcodeBizModel;

    @Inject
    io.nop.dao.api.IDaoProvider daoProvider;
    @Inject
    io.nop.orm.IOrmTemplate ormTemplate;

    static final String ORG_ID = "4051";

    @Test
    public void testReceiveDocCodeResolved() {
        ErpPurReceive receive = new ErpPurReceive();
        receive.setCode("BAR-RCV-001");
        receive.setSupplierId("1");
        receive.setWarehouseId("1");
        receive.setCurrencyId("1");
        receive.setApproveStatus("DRAFT");
        receive.setOrgId("2");
        receive.setBusinessDate(java.time.LocalDate.of(2026, 10, 1));
        receive.setDocStatus("DRAFT");
        daoProvider.daoFor(ErpPurReceive.class).saveEntity(receive);
        Map<String, Object> r = barcodeBizModel.resolveBarcode("BAR-RCV-001", null);
        assertEquals("PUR_RECEIVE", r.get("refType"));
        assertEquals("BAR-RCV-001", r.get("refCode"));
    }

    @Test
    public void testMaterialIssueDocCodeResolved() {
        ErpMfgMaterialIssue issue = new ErpMfgMaterialIssue();
        issue.setCode("BAR-ISS-001");
        issue.setOrgId("2");
        issue.setWorkOrderId("1");
        issue.setWarehouseId("1");
        issue.setApproveStatus("DRAFT");
        issue.setExchangeRate(new java.math.BigDecimal("1"));
        issue.setBusinessDate(java.time.LocalDate.of(2026, 10, 1));
        issue.setDocStatus("DRAFT");
        daoProvider.daoFor(ErpMfgMaterialIssue.class).saveEntity(issue);
        Map<String, Object> r = barcodeBizModel.resolveBarcode("BAR-ISS-001", null);
        assertEquals("MFG_MATERIAL_ISSUE", r.get("refType"));
    }

    @Test
    public void testStockTakeDocCodeResolved() {
        // 先种仓库 + 库位（location.warehouseId FK → erp_md_warehouse）
        ErpMdWarehouse wh = new ErpMdWarehouse();
        wh.setCode("BAR-WH");
        wh.setName("probe warehouse");
        wh.setStatus("ACTIVE");
        daoProvider.daoFor(ErpMdWarehouse.class).saveEntity(wh);
        ErpMdLocation loc = new ErpMdLocation();
        loc.setCode("BAR-TAKE-LOC");
        loc.setName("probe location");
        loc.setWarehouseId(wh.getId());
        daoProvider.daoFor(ErpMdLocation.class).saveEntity(loc);
        ormTemplate.flushSession();
        long locCount = daoProvider.daoFor(ErpMdLocation.class).countByExample(locExampleProbe());
        org.junit.jupiter.api.Assertions.assertEquals(1, locCount, "location 应已持久化（flush 后可查）");

        ErpInvStockTake take = new ErpInvStockTake();
        take.setCode("BAR-TAKE-001");
        take.setWarehouseId(wh.getId());
        take.setBusinessDate(java.time.LocalDate.of(2026, 10, 1));
        take.setDocStatus("DRAFT");
        take.setApproveStatus("DRAFT");
        daoProvider.daoFor(ErpInvStockTake.class).saveEntity(take);

        // 门控默认 true：STOCK_TAKE 分支必须带 locationCode（专项门控测试见 testStockTakeRequiresLocationWhenGateOn）
        Map<String, Object> r = barcodeBizModel.resolveBarcode("BAR-TAKE-001", "BAR-TAKE-LOC");
        assertEquals("STOCK_TAKE", r.get("refType"));
        @SuppressWarnings("unchecked")
        Map<String, Object> location = (Map<String, Object>) r.get("location");
        org.junit.jupiter.api.Assertions.assertEquals("BAR-TAKE-LOC", location.get("locationCode"));
    }

    private ErpMdLocation locExampleProbe() {
        ErpMdLocation ex = new ErpMdLocation();
        ex.setCode("BAR-TAKE-LOC");
        return ex;
    }

    @Test
    public void testStockTakeRequiresLocationWhenGateOn() {
        ErpInvStockTake take = new ErpInvStockTake();
        take.setCode("BAR-TAKE-LOC");
        take.setWarehouseId("1");
        take.setApproveStatus("DRAFT");
        take.setBusinessDate(java.time.LocalDate.of(2026, 10, 1));
        take.setDocStatus("DRAFT");
        daoProvider.daoFor(ErpInvStockTake.class).saveEntity(take);
        // 默认 pda-require-location-scan=true：STOCK_TAKE 分支缺 locationCode 抛门控错
        NopException e = assertThrows(NopException.class,
                () -> barcodeBizModel.resolveBarcode("BAR-TAKE-LOC", null));
        assertTrue(String.valueOf(e.getMessage()).contains("库位"));
    }

    @Test
    public void testLocationCodeResolved() {
        ErpMdLocation loc = new ErpMdLocation();
        loc.setCode("BAR-LOC-001");
        loc.setName("probe location");
        loc.setWarehouseId("1");
        daoProvider.daoFor(ErpMdLocation.class).saveEntity(loc);
        ormTemplate.flushSession();
        System.out.println("[probe-early] locId=" + loc.getId());
        Map<String, Object> r = barcodeBizModel.resolveBarcode("NO-SUCH-DOC", "BAR-LOC-001");
        assertEquals("LOCATION", r.get("type"));
        assertEquals("BAR-LOC-001", r.get("refCode"));
    }

    static int skuSeq = 0;

    @Test
    public void testSkuBarcodeDelegatedResolution() {
        String suffix = String.valueOf(++skuSeq);
        // SKU barcode = "690"+10 位数字 = 13 位纯数字（EAN13 格式声明内，SKU-first 命中）
        String barcode = "690" + String.format("%010d", Long.parseLong(suffix));
        // 沿 TestErpInvImmutableSaveChannel 先例：runInSession 内直种 UoM/Material/Sku 链
        ormTemplate.runInSession((Runnable) () -> {
            ErpMdUoM uom = new ErpMdUoM();
            uom.setCode("BAR-UOM-" + suffix);
            uom.setName("probe uom");
            daoProvider.daoFor(ErpMdUoM.class).saveEntity(uom);
            ErpMdMaterial material = new ErpMdMaterial();
            material.setCode("BAR-MAT-" + suffix);
            material.setName("probe material");
            material.setMaterialType("RAW");
            material.setUoMId(uom.getId());
            material.setStatus("ACTIVE");
            daoProvider.daoFor(ErpMdMaterial.class).saveEntity(material);
            ErpMdMaterialSku sku = new ErpMdMaterialSku();
            sku.setSkuCode("BAR-SKU-" + suffix);
            sku.setBarcode(barcode);
            sku.setMaterialId(material.getId());
            sku.setUoMId(uom.getId());
            daoProvider.daoFor(ErpMdMaterialSku.class).saveEntity(sku);
        });

        Map<String, Object> r = barcodeBizModel.resolveBarcode(barcode, null);
        org.junit.jupiter.api.Assertions.assertEquals("SKU", r.get("type"));
        @SuppressWarnings("unchecked")
        Map<String, Object> skuInfo = (Map<String, Object>) r.get("skuInfo");
        org.junit.jupiter.api.Assertions.assertEquals("BAR-SKU-" + suffix, skuInfo.get("skuCode"));
    }

    @Test
    public void testUnresolvedBarcodeThrows() {
        assertThrows(NopException.class,
                () -> barcodeBizModel.resolveBarcode("NO-SUCH-BARCODE-XYZ-000", null));
    }
}
