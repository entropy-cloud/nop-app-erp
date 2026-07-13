package app.erp.mfg.service;

import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdOrganization;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdUoM;
import app.erp.md.dao.entity.ErpMdWarehouse;
import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgBomLine;
import app.erp.mfg.dao.entity.ErpMfgCostRollup;
import app.erp.mfg.dao.entity.ErpMfgCostRollupLine;
import app.erp.mfg.dao.entity.ErpMfgSubcontractOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.FieldSelectionBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 高价值外键名称解析 BizLoader 测试（机制 D：xmeta 派生 *Name + @BizLoader 批量加载）。
 *
 * <p>覆盖 ErpMfgWorkOrderLine 双仓库（sourceWarehouseName/destWarehouseName）。
 * 经 {@link IGraphQLEngine} findList + FieldSelectionBean 触发 BizLoader 字段解析，
 * 验证批量加载名称正确（防 N+1 + 名称对齐 master-data）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgFkNameLoader extends JunitAutoTestCase {

    static final Long ORG_ID = 7101L;
    static final Long SOURCE_WAREHOUSE_ID = 7301L;
    static final Long DEST_WAREHOUSE_ID = 7302L;
    static final Long UOM_ID = 7501L;
    static final Long MATERIAL_ID = 7601L;
    static final Long CURRENCY_ID = 7701L;
    static final Long BOM_ID = 7401L;
    static final Long PARTNER_ID = 7201L;
    static final Long SUBCONTRACT_ORDER_ID = 7003L;
    static final Long COST_ROLLUP_ID = 7402L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testWorkOrderLineDualWarehouseNameResolution() {
        ormTemplate.runInSession(() -> {
            seedWarehouse(SOURCE_WAREHOUSE_ID, "原料仓");
            seedWarehouse(DEST_WAREHOUSE_ID, "成品仓");
            seedUoM(UOM_ID, "个");
            seedMaterial(MATERIAL_ID, "物料Kappa");
            seedWorkOrder(7001L);
            seedWorkOrderLine(7801L, 7001L);
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpMfgWorkOrderLine__findList",
                "id", "sourceWarehouseName", "destWarehouseName");
        assertNotNull(rows);
        assertEquals(false, rows.isEmpty(), "至少 1 条工单行");
        Map<String, Object> first = rows.get(0);
        assertEquals("原料仓", first.get("sourceWarehouseName"));
        assertEquals("成品仓", first.get("destWarehouseName"));
    }

    // ---------- query helper ----------

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> queryWithSelection(String action, String... fields) {
        FieldSelectionBean selection = new FieldSelectionBean();
        for (String f : fields) {
            selection.addField(f);
        }
        ApiRequest<?> request = ApiRequest.build(Map.of());
        request.setSelection(selection);
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(
                GraphQLOperationType.query, action, request);
        ApiResponse<?> resp = graphQLEngine.executeRpc(ctx);
        assertEquals(0, resp.getStatus(), action + " 查询成功");
        Object data = resp.getData();
        if (data instanceof List) {
            return (List<Map<String, Object>>) data;
        }
        return (List<Map<String, Object>>) ((Map<?, ?>) data).get("items");
    }

    // ---------- seed helpers ----------

    private void seedWarehouse(long id, String name) {
        IEntityDao<ErpMdWarehouse> dao = daoProvider.daoFor(ErpMdWarehouse.class);
        ErpMdWarehouse w = dao.newEntity();
        w.orm_propValue(1, id);
        w.setCode("WH-" + id);
        w.setName(name);
        w.setStatus("ACTIVE");
        dao.saveEntity(w);
    }

    private void seedUoM(long id, String name) {
        IEntityDao<ErpMdUoM> dao = daoProvider.daoFor(ErpMdUoM.class);
        ErpMdUoM u = dao.newEntity();
        u.orm_propValue(1, id);
        u.setCode("UOM-" + id);
        u.setName(name);
        dao.saveEntity(u);
    }

    private void seedMaterial(long id, String name) {
        IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
        ErpMdMaterial m = dao.newEntity();
        m.orm_propValue(1, id);
        m.setCode("MAT-" + id);
        m.setName(name);
        m.orm_propValueByName("materialType", "GOODS");
        m.setUoMId(UOM_ID);
        m.setStatus("ACTIVE");
        dao.saveEntity(m);
    }

    private void seedWorkOrder(long id) {
        IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
        ErpMfgWorkOrder wo = dao.newEntity();
        wo.orm_propValue(1, id);
        wo.setCode("WO-FK-" + id);
        wo.setOrgId(ORG_ID);
        wo.setProductId(MATERIAL_ID);
        wo.setCurrencyId(CURRENCY_ID);
        wo.setPlannedQuantity(BigDecimal.TEN);
        wo.setBusinessDate(LocalDate.of(2026, 7, 1));
        wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED);
        wo.setApproveStatus(ErpMfgConstants.APPROVE_STATUS_UNSUBMITTED);
        dao.saveEntity(wo);
    }

    private void seedWorkOrderLine(long id, long workOrderId) {
        IEntityDao<ErpMfgWorkOrderLine> dao = daoProvider.daoFor(ErpMfgWorkOrderLine.class);
        ErpMfgWorkOrderLine line = dao.newEntity();
        line.orm_propValue(1, id);
        line.setWorkOrderId(workOrderId);
        line.setLineNo(1);
        line.orm_propValueByName("lineType", ErpMfgConstants.WORK_ORDER_LINE_TYPE_INPUT);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setSourceWarehouseId(SOURCE_WAREHOUSE_ID);
        line.setDestWarehouseId(DEST_WAREHOUSE_ID);
        line.setPlannedQuantity(BigDecimal.TEN);
        dao.saveEntity(line);
    }

    // ---------- Phase 1 扩展用例：ErpMfgWorkOrder productName/currencyName + ErpMfgBomLine materialName/uomName ----------

    @Test
    public void testWorkOrderFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "制造测试组织");
            seedCurrency(CURRENCY_ID, "人民币");
            seedUoM(UOM_ID, "个");
            seedMaterial(MATERIAL_ID, "产品Omega");
            seedWorkOrder(7002L);
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpMfgWorkOrder__findList",
                "id", "code", "productName", "currencyName", "orgName");
        assertNotNull(rows);
        assertEquals(false, rows.isEmpty(), "至少 1 条工单");
        Map<String, Object> first = rows.get(0);
        assertEquals("产品Omega", first.get("productName"));
        assertEquals("人民币", first.get("currencyName"));
        assertEquals("制造测试组织", first.get("orgName"));
    }

    @Test
    public void testBomLineFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedUoM(UOM_ID, "个");
            seedMaterial(MATERIAL_ID, "物料BomLine");
            seedWarehouse(DEST_WAREHOUSE_ID, "发货仓");
            seedBom(BOM_ID);
            seedBomLine(7802L, BOM_ID);
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpMfgBomLine__findList",
                "id", "bomCode", "materialName", "uomName", "warehouseName");
        assertNotNull(rows);
        assertEquals(false, rows.isEmpty(), "至少 1 条 BOM 行");
        Map<String, Object> first = rows.get(0);
        assertEquals("BOM-7401", first.get("bomCode"));
        assertEquals("物料BomLine", first.get("materialName"));
        assertEquals("个", first.get("uomName"));
        assertEquals("发货仓", first.get("warehouseName"));
    }

    // ---------- additional seed helpers ----------

    private void seedOrg(long id, String name) {
        IEntityDao<ErpMdOrganization> dao = daoProvider.daoFor(ErpMdOrganization.class);
        ErpMdOrganization o = dao.newEntity();
        o.orm_propValue(1, id);
        o.setCode("ORG-" + id);
        o.setName(name);
        o.setOrgType("COMPANY");
        o.setStatus("ACTIVE");
        dao.saveEntity(o);
    }

    private void seedCurrency(long id, String name) {
        IEntityDao<ErpMdCurrency> dao = daoProvider.daoFor(ErpMdCurrency.class);
        ErpMdCurrency c = dao.newEntity();
        c.orm_propValue(1, id);
        c.setCode("CNY");
        c.setName(name);
        dao.saveEntity(c);
    }

    private void seedBom(long id) {
        IEntityDao<ErpMfgBom> dao = daoProvider.daoFor(ErpMfgBom.class);
        ErpMfgBom bom = dao.newEntity();
        bom.orm_propValue(1, id);
        bom.setCode("BOM-" + id);
        bom.setProductId(MATERIAL_ID);
        bom.orm_propValueByName("bomType", "STANDARD");
        dao.saveEntity(bom);
    }

    private void seedBomLine(long id, long bomId) {
        IEntityDao<ErpMfgBomLine> dao = daoProvider.daoFor(ErpMfgBomLine.class);
        ErpMfgBomLine line = dao.newEntity();
        line.orm_propValue(1, id);
        line.setBomId(bomId);
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setWarehouseId(DEST_WAREHOUSE_ID);
        line.setQuantity(BigDecimal.TEN);
        dao.saveEntity(line);
    }

    // ---------- Phase 2 扩展用例：ErpMfgSubcontractOrder supplierName/productName + ErpMfgCostRollupLine materialName/uomName ----------

    @Test
    public void testSubcontractOrderFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "制造测试组织");
            seedCurrency(CURRENCY_ID, "人民币");
            seedUoM(UOM_ID, "个");
            seedMaterial(MATERIAL_ID, "委外产品");
            seedPartner(PARTNER_ID, "委外供应商Eta");
            seedSubcontractOrder(SUBCONTRACT_ORDER_ID);
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpMfgSubcontractOrder__findList",
                "id", "code", "supplierName", "productName", "currencyName", "orgName");
        assertNotNull(rows);
        assertEquals(false, rows.isEmpty(), "至少 1 条委外单");
        Map<String, Object> first = rows.get(0);
        assertEquals("委外供应商Eta", first.get("supplierName"));
        assertEquals("委外产品", first.get("productName"));
        assertEquals("人民币", first.get("currencyName"));
        assertEquals("制造测试组织", first.get("orgName"));
    }

    @Test
    public void testCostRollupLineFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedCurrency(CURRENCY_ID, "人民币");
            seedUoM(UOM_ID, "个");
            seedMaterial(MATERIAL_ID, "成本物料Theta");
            seedCostRollup(COST_ROLLUP_ID);
            seedCostRollupLine(7803L, COST_ROLLUP_ID);
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpMfgCostRollupLine__findList",
                "id", "costRollupCode", "materialName", "uomName", "currencyName");
        assertNotNull(rows);
        assertEquals(false, rows.isEmpty(), "至少 1 条成本滚算行");
        Map<String, Object> first = rows.get(0);
        assertEquals("CR-7402", first.get("costRollupCode"));
        assertEquals("成本物料Theta", first.get("materialName"));
        assertEquals("个", first.get("uomName"));
        assertEquals("人民币", first.get("currencyName"));
    }

    // ---------- Phase 2 additional seed helpers ----------

    private void seedPartner(long id, String name) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner p = dao.newEntity();
        p.orm_propValue(1, id);
        p.setCode("SUP-" + id);
        p.setName(name);
        p.setPartnerType("SUPPLIER");
        p.setStatus("ACTIVE");
        p.setReceivableBalance(BigDecimal.ZERO);
        p.setPayableBalance(BigDecimal.ZERO);
        dao.saveEntity(p);
    }

    private void seedSubcontractOrder(long id) {
        IEntityDao<ErpMfgSubcontractOrder> dao = daoProvider.daoFor(ErpMfgSubcontractOrder.class);
        ErpMfgSubcontractOrder so = dao.newEntity();
        so.orm_propValue(1, id);
        so.setCode("SCO-" + id);
        so.setOrgId(ORG_ID);
        so.setSupplierId(PARTNER_ID);
        so.setProductId(MATERIAL_ID);
        so.setBusinessDate(LocalDate.of(2026, 7, 1));
        so.setCurrencyId(CURRENCY_ID);
        so.setExchangeRate(new BigDecimal("1"));
        so.orm_propValueByName("docStatus", ErpMfgConstants.SUBCONTRACT_STATUS_DRAFT);
        so.orm_propValueByName("approveStatus", ErpMfgConstants.APPROVE_STATUS_UNSUBMITTED);
        so.orm_propValueByName("postedStatus", "UNPOSTED");
        dao.saveEntity(so);
    }

    private void seedCostRollup(long id) {
        IEntityDao<ErpMfgCostRollup> dao = daoProvider.daoFor(ErpMfgCostRollup.class);
        ErpMfgCostRollup cr = dao.newEntity();
        cr.orm_propValue(1, id);
        cr.setCode("CR-" + id);
        cr.setBusinessDate(LocalDate.of(2026, 7, 1));
        cr.orm_propValueByName("status", "DRAFT");
        dao.saveEntity(cr);
    }

    private void seedCostRollupLine(long id, long costRollupId) {
        IEntityDao<ErpMfgCostRollupLine> dao = daoProvider.daoFor(ErpMfgCostRollupLine.class);
        ErpMfgCostRollupLine line = dao.newEntity();
        line.orm_propValue(1, id);
        line.setCostRollupId(costRollupId);
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setCurrencyId(CURRENCY_ID);
        dao.saveEntity(line);
    }
}
