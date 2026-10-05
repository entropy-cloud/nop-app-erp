package app.erp.inv.service.entity;

import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockTake;
import app.erp.inv.dao.entity.ErpInvStockTakeLine;
import app.erp.md.dao.entity.ErpMdLocation;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdMaterialSku;
import app.erp.md.dao.entity.ErpMdUoM;
import app.erp.md.dao.entity.ErpMdWarehouse;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PDA 盘点扫码落行 scanSave 测试（plan 2026-10-05-0400-2 Phase 2）。
 *
 * <p>覆盖：happy path 派生字段断言（materialId/uoMId/bookQuantity/differenceQuantity/lineNo）+
 * 连续扫码新行语义（lineNo 递增）+ StockBalance 快照口径（有余额行取 totalQuantity，无余额行=0）
 * + 守卫负路径（DRAFT 单拒绝 / 未知 SKU / 库位仓库不匹配 / 缺库位[门控默认开]）。
 * 调用走 graphQLEngine.executeRpc（IoC 容器管理 BizModel 注入——直接方法调用不注入
 * daoProvider，先例 TestErpInvStockTakeCompleteDiffMove executeRpc 范式）；种数据走 DAO
 * 直种轻路径（先例 TestErpInvBarcodeResolve）。
 */
@NopTestConfig(initDatabaseSchema = OptionalBoolean.TRUE, enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvStockTakeLineScanSave extends JunitAutoTestCase {
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    static int seq = 0;

    /** 种 SKU/库位/仓库/UoM/Material 链 + 指定状态盘点单，返回 [takeId, barcode, locationCode] */
    private String[] seedTakeAndSku(String docStatus) {
        String suffix = String.valueOf(++seq);
        String[] out = new String[4];
        ormTemplate.runInSession((Runnable) () -> {
            ErpMdWarehouse wh = new ErpMdWarehouse();
            wh.setCode("SS-WH-" + suffix);
            wh.setName("scan wh");
            wh.setStatus("ACTIVE");
            daoProvider.daoFor(ErpMdWarehouse.class).saveEntity(wh);
            ErpMdLocation loc = new ErpMdLocation();
            loc.setCode("SS-LOC-" + suffix);
            loc.setName("scan loc");
            loc.setWarehouseId(wh.getId());
            daoProvider.daoFor(ErpMdLocation.class).saveEntity(loc);
            ErpMdUoM uom = new ErpMdUoM();
            uom.setCode("SS-UOM-" + suffix);
            uom.setName("scan uom");
            daoProvider.daoFor(ErpMdUoM.class).saveEntity(uom);
            ErpMdMaterial material = new ErpMdMaterial();
            material.setCode("SS-MAT-" + suffix);
            material.setName("scan material");
            material.setMaterialType("RAW");
            material.setUoMId(uom.getId());
            material.setStatus("ACTIVE");
            daoProvider.daoFor(ErpMdMaterial.class).saveEntity(material);
            ErpMdMaterialSku sku = new ErpMdMaterialSku();
            sku.setSkuCode("SS-SKU-" + suffix);
            sku.setBarcode("690" + String.format("%010d", Long.parseLong(suffix)));
            sku.setMaterialId(material.getId());
            sku.setUoMId(uom.getId());
            daoProvider.daoFor(ErpMdMaterialSku.class).saveEntity(sku);
            ErpInvStockTake take = new ErpInvStockTake();
            take.setCode("SS-TAKE-" + suffix);
            take.setWarehouseId(wh.getId());
            take.setBusinessDate(LocalDate.of(2026, 10, 5));
            take.setDocStatus(docStatus);
            take.setApproveStatus("DRAFT");
            daoProvider.daoFor(ErpInvStockTake.class).saveEntity(take);
            out[0] = take.getId();
            out[1] = sku.getBarcode();
            out[2] = loc.getCode();
            out[3] = take.getCode();
        });
        return out;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> scanSave(String takeId, String skuBarcode, String locationCode, String qty) {
        Map<String, Object> args = new java.util.HashMap<>();
        args.put("takeId", takeId);
        args.put("skuBarcode", skuBarcode);
        if (locationCode != null)
            args.put("locationCode", locationCode);
        args.put("actualQuantity", new BigDecimal(qty));
        ApiResponse<?> resp = executeRpc("ErpInvStockTakeLine__scanSave", ApiRequest.build(args));
        assertEquals(0, resp.getStatus(), "scanSave 应成功: code=" + resp.getCode() + " data=" + resp.getData() + " errs=" + resp.getErrors());
        return (Map<String, Object>) resp.getData();
    }

    private ApiResponse<?> executeRpc(String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx =
                graphQLEngine.newRpcContext(GraphQLOperationType.mutation, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private void assertBizError(Runnable call, String expectCodePart) {
        try {
            call.run();
            throw new AssertionError("预期业务异常未抛出: " + expectCodePart);
        } catch (io.nop.api.core.exceptions.NopException e) {
            assertTrue(String.valueOf(e.getMessage()).contains(expectCodePart)
                            || String.valueOf(e.getErrorCode()).contains(expectCodePart),
                    "异常应含「" + expectCodePart + "」: " + e.getMessage());
        }
    }

    @Test
    public void testScanSaveHappyPathDerivedFields() {
        String[] s = seedTakeAndSku("CONFIRMED");
        Map<String, Object> line = scanSave(s[0], s[1], s[2], "5");
        assertEquals(Integer.valueOf(1), ((Number) line.get("lineNo")).intValue(), "首行 lineNo=1");
        assertNotNull(line.get("skuId"));
        assertNotNull(line.get("materialId"), "materialId 应由 SKU 行派生");
        assertNotNull(line.get("uoMId"), "uoMId 应由 SKU 行派生");
        assertNotNull(line.get("locationId"));
        assertEquals(0, new BigDecimal("0").compareTo(new BigDecimal(String.valueOf(line.get("bookQuantity")))),
                "无余额行 bookQuantity=0");
        assertEquals(0, new BigDecimal("5").compareTo(new BigDecimal(String.valueOf(line.get("differenceQuantity")))),
                "difference=actual−book=5");
    }

    @Test
    public void testScanSaveSecondScanNewLineIncrement() {
        String[] s = seedTakeAndSku("CONFIRMED");
        Map<String, Object> l1 = scanSave(s[0], s[1], s[2], "5");
        Map<String, Object> l2 = scanSave(s[0], s[1], s[2], "3");
        assertEquals(Integer.valueOf(1), ((Number) l1.get("lineNo")).intValue());
        assertEquals(Integer.valueOf(2), ((Number) l2.get("lineNo")).intValue(), "连续扫码新行语义：lineNo 递增");
        assertTrue(!String.valueOf(l1.get("id")).equals(String.valueOf(l2.get("id"))), "每次扫码独立成行");
    }

    @Test
    public void testScanSaveBookQuantityFromBalanceSnapshot() {
        String[] s = seedTakeAndSku("CONFIRMED");
        ErpMdLocation locExample = new ErpMdLocation();
        locExample.setCode(s[2]);
        ErpMdLocation loc = daoProvider.daoFor(ErpMdLocation.class).findFirstByExample(locExample);
        ErpMdMaterialSku skuExample = new ErpMdMaterialSku();
        skuExample.setBarcode(s[1]);
        ErpMdMaterialSku sku = daoProvider.daoFor(ErpMdMaterialSku.class).findFirstByExample(skuExample);
        ormTemplate.runInSession((Runnable) () -> {
            ErpInvStockBalance bal = new ErpInvStockBalance();
            bal.setWarehouseId(loc.getWarehouseId());
            bal.setLocationId(loc.getId());
            bal.setSkuId(sku.getId());
            bal.setMaterialId(sku.getMaterialId());
            bal.setTotalQuantity(new BigDecimal("100"));
            bal.setReservedQuantity(BigDecimal.ZERO);
            bal.setLockedQuantity(BigDecimal.ZERO);
            bal.setAvailableQuantity(new BigDecimal("100"));
            daoProvider.daoFor(ErpInvStockBalance.class).saveEntity(bal);
        });
        ormTemplate.flushSession();

        Map<String, Object> line = scanSave(s[0], s[1], s[2], "97");
        assertEquals(0, new BigDecimal("100").compareTo(new BigDecimal(String.valueOf(line.get("bookQuantity")))),
                "bookQuantity=StockBalance 快照");
        assertEquals(0, new BigDecimal("-3").compareTo(new BigDecimal(String.valueOf(line.get("differenceQuantity")))),
                "difference=97−100=−3");
    }

    @Test
    public void testScanSaveRejectsNonCountingTake() {
        String[] s = seedTakeAndSku("DRAFT");
        assertBizError(() -> scanSaveRaw(s[0], s[1], s[2], "1"), "not-in-counting");
    }

    @Test
    public void testScanSaveByTakeCodeResolvesTake() {
        String[] s = seedTakeAndSku("CONFIRMED");
        // takeCode 路径：无需 takeId，服务端按 code 解析盘点单；连扫两次断言 lineNo 1→2
        // （堵 nextLineNo 误用请求参数 takeId=null 的回归——takeCode 路径行号恒 1 的双盲缺陷）
        Map<String, Object> l1 = scanSaveByCode(s[3], s[1], s[2], "5");
        assertEquals(Integer.valueOf(1), ((Number) l1.get("lineNo")).intValue());
        assertEquals(0, new BigDecimal("5").compareTo(new BigDecimal(String.valueOf(l1.get("differenceQuantity")))));
        Map<String, Object> l2 = scanSaveByCode(s[3], s[1], s[2], "3");
        assertEquals(Integer.valueOf(2), ((Number) l2.get("lineNo")).intValue(), "takeCode 连扫 lineNo 递增");
    }

    @Test
    public void testScanSaveUnknownSkuBarcode() {
        String[] s = seedTakeAndSku("CONFIRMED");
        assertBizError(() -> scanSaveRaw(s[0], "9999999999999", s[2], "1"), "sku-unknown");
    }

    @Test
    public void testScanSaveLocationWarehouseMismatch() {
        String[] s = seedTakeAndSku("CONFIRMED");
        String otherSuffix = "x" + (++seq);
        ErpMdWarehouse otherWh = new ErpMdWarehouse();
        otherWh.setCode("SS-WH-OTHER-" + otherSuffix);
        otherWh.setName("other wh");
        otherWh.setStatus("ACTIVE");
        daoProvider.daoFor(ErpMdWarehouse.class).saveEntity(otherWh);
        ErpMdLocation otherLoc = new ErpMdLocation();
        otherLoc.setCode("SS-LOC-OTHER-" + otherSuffix);
        otherLoc.setName("other loc");
        otherLoc.setWarehouseId(otherWh.getId());
        daoProvider.daoFor(ErpMdLocation.class).saveEntity(otherLoc);
        ormTemplate.flushSession();

        assertBizError(() -> scanSaveRaw(s[0], s[1], otherLoc.getCode(), "1"), "location-warehouse-mismatch");
    }

    @Test
    public void testScanSaveRequiresLocationWhenGateOn() {
        String[] s = seedTakeAndSku("CONFIRMED");
        // 默认 pda-require-location-scan=true：缺 locationCode 抛门控错
        assertBizError(() -> scanSaveRaw(s[0], s[1], null, "1"), "location-required");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> scanSaveByCode(String takeCode, String skuBarcode, String locationCode, String qty) {
        Map<String, Object> args = new java.util.HashMap<>();
        args.put("takeCode", takeCode);
        args.put("skuBarcode", skuBarcode);
        args.put("locationCode", locationCode);
        args.put("actualQuantity", new BigDecimal(qty));
        ApiResponse<?> resp = executeRpc("ErpInvStockTakeLine__scanSave", ApiRequest.build(args));
        assertEquals(0, resp.getStatus(), "scanSave(takeCode) 应成功");
        return (Map<String, Object>) resp.getData();
    }

    private void scanSaveRaw(String takeId, String skuBarcode, String locationCode, String qty) {
        Map<String, Object> args = new java.util.HashMap<>();
        args.put("takeId", takeId);
        args.put("skuBarcode", skuBarcode);
        if (locationCode != null)
            args.put("locationCode", locationCode);
        args.put("actualQuantity", new BigDecimal(qty));
        ApiResponse<?> resp = executeRpc("ErpInvStockTakeLine__scanSave", ApiRequest.build(args));
        if (resp.getStatus() != 0) {
            // NopException 构造器收 ErrorCode；此处以携码 NopException 传递 resp 的 code 供 assertBizError 匹配
            throw new io.nop.api.core.exceptions.NopException(
                    io.nop.api.core.exceptions.ErrorCode.define(String.valueOf(resp.getCode()), String.valueOf(resp.getData())))
                    .param("rpcStatus", resp.getStatus());
        }
    }
}
