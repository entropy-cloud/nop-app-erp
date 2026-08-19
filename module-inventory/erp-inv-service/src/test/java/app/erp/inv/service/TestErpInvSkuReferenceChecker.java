package app.erp.inv.service;

import app.erp.inv.dao.ErpInvDaoConstants;
import app.erp.inv.dao.entity.ErpInvSerialNumber;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.inv.service.spi.ErpInvSkuReferenceChecker;
import app.erp.md.dao.entity.ErpMdMaterialSku;
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
 * RC-R1.72 Phase 2 Proof：inventory 域 SKU 引用检查器（D3 口径——在手量 + 开放单据行 + 活跃批次/序列，
 * StockLedger 不可变历史不阻断）。真实本域实体构造在手/零余额、DRAFT/DONE/CANCELLED、在库/出库对照
 * （plan 2026-08-19-0445-1）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvSkuReferenceChecker extends JunitAutoTestCase {

    static final Long WAREHOUSE_ID = 9301L;
    static final Long MATERIAL_ID = 9302L;
    static final Long UOM_ID = 9303L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ErpInvSkuReferenceChecker checker;

    @Test
    public void testStockBalanceOnHandReferencesSku() {
        Long onHandSkuId = seedSku("SKU-INV-ONHAND");
        seedStockBalance(onHandSkuId, new BigDecimal("5"));
        assertTrue(checker.isReferencedByBill(loadSku(onHandSkuId)), "在手量 ≠ 0 应构成引用");

        Long zeroSkuId = seedSku("SKU-INV-ZERO");
        seedStockBalance(zeroSkuId, BigDecimal.ZERO);
        assertFalse(checker.isReferencedByBill(loadSku(zeroSkuId)), "零余额不阻断");
    }

    @Test
    public void testOpenStockMoveLineReferencesSku() {
        Long draftSkuId = seedSku("SKU-INV-MOVE-DRAFT");
        Long draftMoveId = seedStockMove("SM-REF-DRAFT", ErpInvDaoConstants.MOVE_STATUS_DRAFT);
        seedStockMoveLine(draftMoveId, draftSkuId);
        assertTrue(checker.isReferencedByBill(loadSku(draftSkuId)), "DRAFT 移动单行应构成引用");

        Long doneSkuId = seedSku("SKU-INV-MOVE-DONE");
        Long doneMoveId = seedStockMove("SM-REF-DONE", ErpInvDaoConstants.MOVE_STATUS_DONE);
        seedStockMoveLine(doneMoveId, doneSkuId);
        assertFalse(checker.isReferencedByBill(loadSku(doneSkuId)), "DONE（终态）移动单行不阻断");

        Long cancelledSkuId = seedSku("SKU-INV-MOVE-CANCEL");
        Long cancelledMoveId = seedStockMove("SM-REF-CANCEL", ErpInvDaoConstants.MOVE_STATUS_CANCELLED);
        seedStockMoveLine(cancelledMoveId, cancelledSkuId);
        assertFalse(checker.isReferencedByBill(loadSku(cancelledSkuId)), "CANCELLED 移动单行不阻断");
    }

    @Test
    public void testSerialNumberStatusContrast() {
        Long inStockSkuId = seedSku("SKU-INV-SN-IN");
        seedSerialNumber(inStockSkuId, "SN-REF-IN", ErpInvDaoConstants.SERIAL_STATUS_IN_STOCK);
        assertTrue(checker.isReferencedByBill(loadSku(inStockSkuId)), "在库序列号应构成引用");

        Long outSkuId = seedSku("SKU-INV-SN-OUT");
        seedSerialNumber(outSkuId, "SN-REF-OUT", ErpInvDaoConstants.SERIAL_STATUS_OUT);
        assertFalse(checker.isReferencedByBill(loadSku(outSkuId)), "已出库序列号不阻断");
    }

    @Test
    public void testUnreferencedSkuFalse() {
        Long skuId = seedSku("SKU-INV-UNREF");
        assertFalse(checker.isReferencedByBill(loadSku(skuId)), "无库存/单据引用应为 false");
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

    private void seedStockBalance(Long skuId, BigDecimal totalQuantity) {
        ErpInvStockBalance balance = new ErpInvStockBalance();
        balance.setWarehouseId(WAREHOUSE_ID);
        balance.setMaterialId(MATERIAL_ID);
        balance.setSkuId(skuId);
        balance.setTotalQuantity(totalQuantity);
        balance.setAvailableQuantity(totalQuantity);
        ormTemplate.runInSession(() -> daoProvider.daoFor(ErpInvStockBalance.class).saveEntity(balance));
    }

    private Long seedStockMove(String code, String docStatus) {
        ErpInvStockMove move = new ErpInvStockMove();
        move.setCode(code);
        move.setMoveType("INTERNAL");
        move.setBusinessDate(LocalDate.of(2026, 8, 19));
        move.setDocStatus(docStatus);
        move.setApproveStatus("APPROVED");
        ormTemplate.runInSession(() -> daoProvider.daoFor(ErpInvStockMove.class).saveEntity(move));
        return move.getId();
    }

    private void seedStockMoveLine(Long moveId, Long skuId) {
        ErpInvStockMoveLine line = new ErpInvStockMoveLine();
        line.setMoveId(moveId);
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setSkuId(skuId);
        line.setUoMId(UOM_ID);
        line.setQuantity(BigDecimal.TEN);
        ormTemplate.runInSession(() -> daoProvider.daoFor(ErpInvStockMoveLine.class).saveEntity(line));
    }

    private void seedSerialNumber(Long skuId, String serialNo, String status) {
        ErpInvSerialNumber sn = new ErpInvSerialNumber();
        sn.setSerialNo(serialNo);
        sn.setMaterialId(MATERIAL_ID);
        sn.setSkuId(skuId);
        sn.setStatus(status);
        ormTemplate.runInSession(() -> daoProvider.daoFor(ErpInvSerialNumber.class).saveEntity(sn));
    }

    private IEntityDao<ErpMdMaterialSku> skuDao() {
        return daoProvider.daoFor(ErpMdMaterialSku.class);
    }
}
