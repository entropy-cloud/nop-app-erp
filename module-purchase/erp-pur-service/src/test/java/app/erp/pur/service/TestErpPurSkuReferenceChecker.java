package app.erp.pur.service;

import app.erp.md.dao.entity.ErpMdMaterialSku;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.dao.entity.ErpPurReceiveLine;
import app.erp.pur.dao.entity.ErpPurReturn;
import app.erp.pur.dao.entity.ErpPurReturnLine;
import app.erp.pur.service.spi.ErpPurSkuReferenceChecker;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RC-R1.72 Phase 2 Proof：purchase 域 SKU 引用检查器（D3 口径——OrderLine/ReceiveLine/ReturnLine
 * 经 header docStatus ≠ CANCELLED）。真实本域实体构造开放/取消对照（plan 2026-08-19-0445-1）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurSkuReferenceChecker extends JunitAutoTestCase {

    static final Long SUPPLIER_ID = 9101L;
    static final Long WAREHOUSE_ID = 9102L;
    static final Long MATERIAL_ID = 9103L;
    static final Long UOM_ID = 9104L;
    static final Long CURRENCY_ID = 9105L;
    static final BigDecimal QTY = new BigDecimal("10");
    static final BigDecimal PRICE = new BigDecimal("5");

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ErpPurSkuReferenceChecker checker;

    @Test
    public void testOpenOrderLineReferencesSku() {
        Long skuId = seedSku("SKU-PUR-OPEN");
        Long orderId = seedOrder("PO-REF-OPEN", "ACTIVE");
        seedOrderLine(orderId, skuId);
        assertTrue(checker.isReferencedByBill(loadSku(skuId)), "开放订单行应构成引用");
    }

    @Test
    public void testCancelledOrderLineNotReference() {
        Long skuId = seedSku("SKU-PUR-CANCEL");
        Long orderId = seedOrder("PO-REF-CANCEL", "CANCELLED");
        seedOrderLine(orderId, skuId);
        assertFalse(checker.isReferencedByBill(loadSku(skuId)), "取消订单行不阻断");
    }

    @Test
    public void testOpenReceiveAndReturnLineReference() {
        Long receiveSkuId = seedSku("SKU-PUR-RECV");
        Long receiveId = seedReceive("PR-REF-OPEN", "ACTIVE");
        seedReceiveLine(receiveId, receiveSkuId);
        assertTrue(checker.isReferencedByBill(loadSku(receiveSkuId)), "开放入库单行应构成引用");

        Long returnSkuId = seedSku("SKU-PUR-RET");
        Long returnId = seedReturn("PT-REF-OPEN", "ACTIVE");
        seedReturnLine(returnId, returnSkuId);
        assertTrue(checker.isReferencedByBill(loadSku(returnSkuId)), "开放退货单行应构成引用");

        Long cancelledReturnSkuId = seedSku("SKU-PUR-RET-CANCEL");
        Long cancelledReturnId = seedReturn("PT-REF-CANCEL", "CANCELLED");
        seedReturnLine(cancelledReturnId, cancelledReturnSkuId);
        assertFalse(checker.isReferencedByBill(loadSku(cancelledReturnSkuId)), "取消退货单行不阻断");
    }

    @Test
    public void testUnreferencedSkuFalse() {
        Long skuId = seedSku("SKU-PUR-UNREF");
        assertFalse(checker.isReferencedByBill(loadSku(skuId)), "无任何单据引用应为 false");
    }

    // ---------- seeds ----------

    private Long seedSku(String skuCode) {
        ErpMdMaterialSku sku = new ErpMdMaterialSku();
        sku.setMaterialId(MATERIAL_ID);
        sku.setSkuCode(skuCode);
        sku.setUoMId(UOM_ID);
        sku.setConversionRate(BigDecimal.ONE);
        ormTemplate.runInSession(() -> skuDao().saveEntity(sku));
        return sku.getId();
    }

    private ErpMdMaterialSku loadSku(Long skuId) {
        return skuDao().getEntityById(skuId);
    }

    private Long seedOrder(String code, String docStatus) {
        ErpPurOrder order = new ErpPurOrder();
        order.setCode(code);
        order.setSupplierId(SUPPLIER_ID);
        order.setWarehouseId(WAREHOUSE_ID);
        order.setBusinessDate(LocalDate.of(2026, 8, 19));
        order.setCurrencyId(CURRENCY_ID);
        order.setDocStatus(docStatus);
        order.setApproveStatus("APPROVED");
        ormTemplate.runInSession(() -> daoProvider.daoFor(ErpPurOrder.class).saveEntity(order));
        return order.getId();
    }

    private void seedOrderLine(Long orderId, Long skuId) {
        ErpPurOrderLine line = new ErpPurOrderLine();
        line.setOrderId(orderId);
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setSkuId(skuId);
        line.setUoMId(UOM_ID);
        line.setQuantity(QTY);
        line.setUnitPrice(PRICE);
        line.setAmount(QTY.multiply(PRICE));
        ormTemplate.runInSession(() -> daoProvider.daoFor(ErpPurOrderLine.class).saveEntity(line));
    }

    private Long seedReceive(String code, String docStatus) {
        ErpPurReceive receive = new ErpPurReceive();
        receive.setCode(code);
        receive.setSupplierId(SUPPLIER_ID);
        receive.setWarehouseId(WAREHOUSE_ID);
        receive.setBusinessDate(LocalDate.of(2026, 8, 19));
        receive.setCurrencyId(CURRENCY_ID);
        receive.setDocStatus(docStatus);
        receive.setApproveStatus("APPROVED");
        ormTemplate.runInSession(() -> daoProvider.daoFor(ErpPurReceive.class).saveEntity(receive));
        return receive.getId();
    }

    private void seedReceiveLine(Long receiveId, Long skuId) {
        ErpPurReceiveLine line = new ErpPurReceiveLine();
        line.setReceiveId(receiveId);
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setSkuId(skuId);
        line.setUoMId(UOM_ID);
        line.setQuantity(QTY);
        line.setUnitPrice(PRICE);
        line.setAmount(QTY.multiply(PRICE));
        ormTemplate.runInSession(() -> daoProvider.daoFor(ErpPurReceiveLine.class).saveEntity(line));
    }

    private Long seedReturn(String code, String docStatus) {
        ErpPurReturn ret = new ErpPurReturn();
        ret.setCode(code);
        ret.setSupplierId(SUPPLIER_ID);
        ret.setWarehouseId(WAREHOUSE_ID);
        ret.setBusinessDate(LocalDate.of(2026, 8, 19));
        ret.setCurrencyId(CURRENCY_ID);
        ret.setDocStatus(docStatus);
        ret.setApproveStatus("APPROVED");
        ormTemplate.runInSession(() -> daoProvider.daoFor(ErpPurReturn.class).saveEntity(ret));
        return ret.getId();
    }

    private void seedReturnLine(Long returnId, Long skuId) {
        ErpPurReturnLine line = new ErpPurReturnLine();
        line.setReturnId(returnId);
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setSkuId(skuId);
        line.setUoMId(UOM_ID);
        line.setQuantity(QTY);
        line.setUnitPrice(PRICE);
        line.setAmount(QTY.multiply(PRICE));
        ormTemplate.runInSession(() -> daoProvider.daoFor(ErpPurReturnLine.class).saveEntity(line));
    }

    private IEntityDao<ErpMdMaterialSku> skuDao() {
        return daoProvider.daoFor(ErpMdMaterialSku.class);
    }
}
