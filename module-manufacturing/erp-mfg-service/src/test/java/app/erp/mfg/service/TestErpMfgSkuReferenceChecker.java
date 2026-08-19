package app.erp.mfg.service;

import app.erp.md.dao.entity.ErpMdMaterialSku;
import app.erp.mfg.dao.ErpMfgDaoConstants;
import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgBomLine;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssue;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssueLine;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderLine;
import app.erp.mfg.service.spi.ErpMfgSkuReferenceChecker;
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
 * RC-R1.72 Phase 2 Proof：manufacturing 域 SKU 引用检查器（D3 口径——BomLine/BomByproduct 经
 * bom.isActive；WorkOrderLine 经 docStatus ∉ 终态 {CLOSED, CANCELLED}；MaterialIssueLine 经
 * docStatus ∈ {DRAFT, CONFIRMED}）。真实本域实体构造活跃/停用 BOM、开放/终态工单对照
 * （plan 2026-08-19-0445-1）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgSkuReferenceChecker extends JunitAutoTestCase {

    static final Long PRODUCT_ID = 9401L;
    static final Long MATERIAL_ID = 9402L;
    static final Long UOM_ID = 9403L;
    static final Long WORK_ORDER_ID = 9404L;
    static final Long WAREHOUSE_ID = 9405L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ErpMfgSkuReferenceChecker checker;

    @Test
    public void testActiveBomLineReferencesSku() {
        Long activeSkuId = seedSku("SKU-MFG-BOM-ACTIVE");
        Long bomId = seedBom("BOM-REF-ACTIVE", true);
        seedBomLine(bomId, activeSkuId);
        assertTrue(checker.isReferencedByBill(loadSku(activeSkuId)), "活跃 BOM 行应构成引用");

        Long inactiveSkuId = seedSku("SKU-MFG-BOM-INACTIVE");
        Long inactiveBomId = seedBom("BOM-REF-INACTIVE", false);
        seedBomLine(inactiveBomId, inactiveSkuId);
        assertFalse(checker.isReferencedByBill(loadSku(inactiveSkuId)), "停用 BOM 行不阻断");
    }

    @Test
    public void testWorkOrderLineTerminalContrast() {
        Long openSkuId = seedSku("SKU-MFG-WO-OPEN");
        Long openWoId = seedWorkOrder("WO-REF-OPEN", ErpMfgDaoConstants.WORK_ORDER_STATUS_IN_PROCESS);
        seedWorkOrderLine(openWoId, openSkuId);
        assertTrue(checker.isReferencedByBill(loadSku(openSkuId)), "生产中工单行应构成引用");

        Long closedSkuId = seedSku("SKU-MFG-WO-CLOSED");
        Long closedWoId = seedWorkOrder("WO-REF-CLOSED", ErpMfgDaoConstants.WORK_ORDER_STATUS_CLOSED);
        seedWorkOrderLine(closedWoId, closedSkuId);
        assertFalse(checker.isReferencedByBill(loadSku(closedSkuId)), "CLOSED（终态）工单行不阻断");

        Long cancelledSkuId = seedSku("SKU-MFG-WO-CANCEL");
        Long cancelledWoId = seedWorkOrder("WO-REF-CANCEL", ErpMfgDaoConstants.WORK_ORDER_STATUS_CANCELLED);
        seedWorkOrderLine(cancelledWoId, cancelledSkuId);
        assertFalse(checker.isReferencedByBill(loadSku(cancelledSkuId)), "CANCELLED（终态）工单行不阻断");
    }

    @Test
    public void testMaterialIssueLineContrast() {
        Long openSkuId = seedSku("SKU-MFG-ISSUE-OPEN");
        Long openIssueId = seedMaterialIssue("MI-REF-OPEN", ErpMfgDaoConstants.ISSUE_STATUS_DRAFT);
        seedMaterialIssueLine(openIssueId, openSkuId);
        assertTrue(checker.isReferencedByBill(loadSku(openSkuId)), "DRAFT 领料单行应构成引用");

        Long doneSkuId = seedSku("SKU-MFG-ISSUE-DONE");
        Long doneIssueId = seedMaterialIssue("MI-REF-DONE", ErpMfgDaoConstants.ISSUE_STATUS_DONE);
        seedMaterialIssueLine(doneIssueId, doneSkuId);
        assertFalse(checker.isReferencedByBill(loadSku(doneSkuId)), "DONE（终态）领料单行不阻断");
    }

    @Test
    public void testUnreferencedSkuFalse() {
        Long skuId = seedSku("SKU-MFG-UNREF");
        assertFalse(checker.isReferencedByBill(loadSku(skuId)), "无 BOM/工单/领料引用应为 false");
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

    private Long seedBom(String code, boolean isActive) {
        ErpMfgBom bom = new ErpMfgBom();
        bom.setCode(code);
        bom.setProductId(PRODUCT_ID);
        bom.setBomType("NORMAL");
        bom.setIsActive(isActive);
        ormTemplate.runInSession(() -> daoProvider.daoFor(ErpMfgBom.class).saveEntity(bom));
        return bom.getId();
    }

    private void seedBomLine(Long bomId, Long skuId) {
        ErpMfgBomLine line = new ErpMfgBomLine();
        line.setBomId(bomId);
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setSkuId(skuId);
        line.setUoMId(UOM_ID);
        line.setQuantity(BigDecimal.TEN);
        ormTemplate.runInSession(() -> daoProvider.daoFor(ErpMfgBomLine.class).saveEntity(line));
    }

    private Long seedWorkOrder(String code, String docStatus) {
        ErpMfgWorkOrder wo = new ErpMfgWorkOrder();
        wo.setCode(code);
        wo.setProductId(PRODUCT_ID);
        wo.setPlannedQuantity(BigDecimal.TEN);
        wo.setBusinessDate(LocalDate.of(2026, 8, 19));
        wo.setDocStatus(docStatus);
        ormTemplate.runInSession(() -> daoProvider.daoFor(ErpMfgWorkOrder.class).saveEntity(wo));
        return wo.getId();
    }

    private void seedWorkOrderLine(Long workOrderId, Long skuId) {
        ErpMfgWorkOrderLine line = new ErpMfgWorkOrderLine();
        line.setWorkOrderId(workOrderId);
        line.setLineNo(1);
        line.setLineType("INPUT");
        line.setMaterialId(MATERIAL_ID);
        line.setSkuId(skuId);
        line.setUoMId(UOM_ID);
        line.setPlannedQuantity(BigDecimal.TEN);
        ormTemplate.runInSession(() -> daoProvider.daoFor(ErpMfgWorkOrderLine.class).saveEntity(line));
    }

    private Long seedMaterialIssue(String code, String docStatus) {
        ErpMfgMaterialIssue issue = new ErpMfgMaterialIssue();
        issue.setCode(code);
        issue.setWorkOrderId(WORK_ORDER_ID);
        issue.setWarehouseId(WAREHOUSE_ID);
        issue.setBusinessDate(LocalDate.of(2026, 8, 19));
        issue.setDocStatus(docStatus);
        issue.setApproveStatus("APPROVED");
        ormTemplate.runInSession(() -> daoProvider.daoFor(ErpMfgMaterialIssue.class).saveEntity(issue));
        return issue.getId();
    }

    private void seedMaterialIssueLine(Long issueId, Long skuId) {
        ErpMfgMaterialIssueLine line = new ErpMfgMaterialIssueLine();
        line.setIssueId(issueId);
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setSkuId(skuId);
        line.setUoMId(UOM_ID);
        line.setRequiredQuantity(BigDecimal.TEN);
        ormTemplate.runInSession(() -> daoProvider.daoFor(ErpMfgMaterialIssueLine.class).saveEntity(line));
    }

    private IEntityDao<ErpMdMaterialSku> skuDao() {
        return daoProvider.daoFor(ErpMdMaterialSku.class);
    }
}
