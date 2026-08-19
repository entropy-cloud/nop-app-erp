package app.erp.sal.service;

import app.erp.md.dao.entity.ErpMdMaterialSku;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import app.erp.sal.dao.entity.ErpSalPriceList;
import app.erp.sal.dao.entity.ErpSalPriceListLine;
import app.erp.sal.service.spi.ErpSalSkuReferenceChecker;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import app.erp.sal.dao.constants.ErpSalDocStatus;
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
 * RC-R1.72 Phase 2 Proof：sales 域 SKU 引用检查器（D3 口径——Order/Delivery/ReturnLine 经
 * header docStatus ≠ CANCELLED + PriceListLine 经 priceList.isActive 且 validTo ≥ 当日，
 * 过期价目表不阻断）。真实本域实体构造开放/取消/过期对照（plan 2026-08-19-0445-1）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalSkuReferenceChecker extends JunitAutoTestCase {

    static final Long CUSTOMER_ID = 9201L;
    static final Long MATERIAL_ID = 9202L;
    static final Long UOM_ID = 9203L;
    static final Long CURRENCY_ID = 9204L;
    static final BigDecimal QTY = new BigDecimal("10");
    static final BigDecimal PRICE = new BigDecimal("5");

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ErpSalSkuReferenceChecker checker;

    @Test
    public void testOpenOrderLineReferencesSku() {
        Long skuId = seedSku("SKU-SAL-OPEN");
        Long orderId = seedOrder("SO-REF-OPEN", ErpSalDocStatus.DOC_STATUS_ACTIVE);
        seedOrderLine(orderId, skuId);
        assertTrue(checker.isReferencedByBill(loadSku(skuId)), "开放销售订单行应构成引用");
    }

    @Test
    public void testCancelledOrderLineNotReference() {
        Long skuId = seedSku("SKU-SAL-CANCEL");
        Long orderId = seedOrder("SO-REF-CANCEL", ErpSalDocStatus.DOC_STATUS_CANCELLED);
        seedOrderLine(orderId, skuId);
        assertFalse(checker.isReferencedByBill(loadSku(skuId)), "取消销售订单行不阻断");
    }

    @Test
    public void testActivePriceListReferencesAndExpiredNot() {
        // 活跃且未过期的价目表行 → 构成引用
        Long activeSkuId = seedSku("SKU-SAL-PL-ACTIVE");
        seedPriceListLine(activeSkuId, true, LocalDate.now().plusDays(30));
        assertTrue(checker.isReferencedByBill(loadSku(activeSkuId)), "活跃未过期价目表行应构成引用");

        // 过期价目表行 → 不阻断（validTo < 当日）
        Long expiredSkuId = seedSku("SKU-SAL-PL-EXPIRED");
        seedPriceListLine(expiredSkuId, true, LocalDate.now().minusDays(1));
        assertFalse(checker.isReferencedByBill(loadSku(expiredSkuId)), "过期价目表行不阻断");

        // 停用价目表行 → 不阻断（isActive=false）
        Long inactiveSkuId = seedSku("SKU-SAL-PL-INACTIVE");
        seedPriceListLine(inactiveSkuId, false, LocalDate.now().plusDays(30));
        assertFalse(checker.isReferencedByBill(loadSku(inactiveSkuId)), "停用价目表行不阻断");
    }

    @Test
    public void testUnreferencedSkuFalse() {
        Long skuId = seedSku("SKU-SAL-UNREF");
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
        ErpSalOrder order = new ErpSalOrder();
        order.setCode(code);
        order.setCustomerId(CUSTOMER_ID);
        order.setBusinessDate(LocalDate.of(2026, 8, 19));
        order.setCurrencyId(CURRENCY_ID);
        order.setDocStatus(docStatus);
        order.setApproveStatus("APPROVED");
        ormTemplate.runInSession(() -> daoProvider.daoFor(ErpSalOrder.class).saveEntity(order));
        return order.getId();
    }

    private void seedOrderLine(Long orderId, Long skuId) {
        ErpSalOrderLine line = new ErpSalOrderLine();
        line.setOrderId(orderId);
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setSkuId(skuId);
        line.setUoMId(UOM_ID);
        line.setQuantity(QTY);
        line.setUnitPrice(PRICE);
        line.setAmount(QTY.multiply(PRICE));
        ormTemplate.runInSession(() -> daoProvider.daoFor(ErpSalOrderLine.class).saveEntity(line));
    }

    private void seedPriceListLine(Long skuId, boolean isActive, LocalDate validTo) {
        ErpSalPriceList priceList = new ErpSalPriceList();
        priceList.setCode("PL-REF-" + skuId);
        priceList.setName("价目表-" + skuId);
        priceList.setIsActive(isActive);
        priceList.setValidTo(validTo);
        ormTemplate.runInSession(() -> daoProvider.daoFor(ErpSalPriceList.class).saveEntity(priceList));

        ErpSalPriceListLine line = new ErpSalPriceListLine();
        line.setPriceListId(priceList.getId());
        line.setSkuId(skuId);
        line.setUnitPrice(PRICE);
        ormTemplate.runInSession(() -> daoProvider.daoFor(ErpSalPriceListLine.class).saveEntity(line));
    }

    private IEntityDao<ErpMdMaterialSku> skuDao() {
        return daoProvider.daoFor(ErpMdMaterialSku.class);
    }
}
