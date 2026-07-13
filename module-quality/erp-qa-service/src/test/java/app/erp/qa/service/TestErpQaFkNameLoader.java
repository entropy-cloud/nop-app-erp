package app.erp.qa.service;

import app.erp.md.dao.entity.ErpMdEmployee;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdOrganization;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdUoM;
import app.erp.md.dao.entity.ErpMdWarehouse;
import app.erp.qa.dao.entity.ErpQaInspection;
import app.erp.qa.dao.entity.ErpQaInspectionTemplate;
import app.erp.qa.dao.entity.ErpQaNonConformance;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 高价值外键名称解析 BizLoader 测试（机制 D：xmeta 派生 *Name/*Code + @BizLoader 批量加载）。
 *
 * <p>覆盖 quality 域代表实体。经 {@link IGraphQLEngine} findList + {@link FieldSelectionBean}
 * 请求派生字段触发 @BizLoader，验证批量加载名称正确（防 N+1 + 名称对齐 master-data）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpQaFkNameLoader extends JunitAutoTestCase {

    static final Long ORG_ID = 8101L;
    static final Long UOM_ID = 8102L;
    static final Long MATERIAL_ID = 8103L;
    static final Long SUPPLIER_ID = 8104L;
    static final Long WAREHOUSE_ID = 8105L;
    static final Long INSPECTOR_ID = 8106L;
    static final Long TEMPLATE_ID = 8107L;
    static final Long INSPECTION_ID = 8108L;
    static final Long NCR_ID = 8109L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testInspectionFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "质检测试组织");
            seedUoM(UOM_ID, "EA");
            seedMaterial(MATERIAL_ID, "原料X");
            seedPartner(SUPPLIER_ID, "供应商甲");
            seedWarehouse(WAREHOUSE_ID, "中央仓库");
            seedEmployee(INSPECTOR_ID, "检验员张三");
            seedTemplate(TEMPLATE_ID, "TPL-001", "进料检验模板");
            seedInspection(INSPECTION_ID, "INS-FKN-001");
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpQaInspection__findList",
                "id", "code", "materialName", "supplierName", "warehouseName",
                "inspectorName", "orgName", "templateCode");
        assertNotNull(rows);
        assertFalse(rows.isEmpty(), "至少 1 条质检单");
        Map<String, Object> first = rows.get(0);
        assertEquals("原料X", first.get("materialName"));
        assertEquals("供应商甲", first.get("supplierName"));
        assertEquals("中央仓库", first.get("warehouseName"));
        assertEquals("检验员张三", first.get("inspectorName"));
        assertEquals("质检测试组织", first.get("orgName"));
        assertEquals("TPL-001", first.get("templateCode"));
    }

    @Test
    public void testNonConformanceFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "质检测试组织");
            seedUoM(UOM_ID, "EA");
            seedMaterial(MATERIAL_ID, "原料X");
            seedPartner(SUPPLIER_ID, "供应商甲");
            seedTemplate(TEMPLATE_ID, "TPL-001", "进料检验模板");
            seedInspection(INSPECTION_ID, "INS-FKN-001");
            seedNcr(NCR_ID, "NCR-FKN-001");
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpQaNonConformance__findList",
                "id", "code", "materialName", "supplierName", "inspectionCode");
        assertNotNull(rows);
        assertFalse(rows.isEmpty(), "至少 1 条 NCR");
        Map<String, Object> first = rows.get(0);
        assertEquals("原料X", first.get("materialName"));
        assertEquals("供应商甲", first.get("supplierName"));
        assertEquals("INS-FKN-001", first.get("inspectionCode"));
    }

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

    private void seedOrg(long id, String name) {
        IEntityDao<ErpMdOrganization> dao = daoProvider.daoFor(ErpMdOrganization.class);
        ErpMdOrganization o = dao.newEntity();
        o.orm_propValueByName("id", id);
        o.setCode("ORG-" + id);
        o.setName(name);
        o.setOrgType("COMPANY");
        o.setStatus("ACTIVE");
        dao.saveEntity(o);
    }

    private void seedUoM(long id, String code) {
        IEntityDao<ErpMdUoM> dao = daoProvider.daoFor(ErpMdUoM.class);
        ErpMdUoM u = dao.newEntity();
        u.orm_propValueByName("id", id);
        u.setCode(code);
        u.setName("个");
        dao.saveEntity(u);
    }

    private void seedMaterial(long id, String name) {
        IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
        ErpMdMaterial m = dao.newEntity();
        m.orm_propValueByName("id", id);
        m.setCode("MAT-" + id);
        m.setName(name);
        m.orm_propValueByName("materialType", "GOODS");
        m.setUoMId(UOM_ID);
        m.setStatus("ACTIVE");
        dao.saveEntity(m);
    }

    private void seedPartner(long id, String name) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner p = dao.newEntity();
        p.orm_propValueByName("id", id);
        p.setCode("SUP-" + id);
        p.setName(name);
        p.setPartnerType("SUPPLIER");
        p.setStatus("ACTIVE");
        p.setReceivableBalance(BigDecimal.ZERO);
        p.setPayableBalance(BigDecimal.ZERO);
        dao.saveEntity(p);
    }

    private void seedWarehouse(long id, String name) {
        IEntityDao<ErpMdWarehouse> dao = daoProvider.daoFor(ErpMdWarehouse.class);
        ErpMdWarehouse w = dao.newEntity();
        w.orm_propValueByName("id", id);
        w.setCode("WH-" + id);
        w.setName(name);
        w.setStatus("ACTIVE");
        dao.saveEntity(w);
    }

    private void seedEmployee(long id, String name) {
        IEntityDao<ErpMdEmployee> dao = daoProvider.daoFor(ErpMdEmployee.class);
        ErpMdEmployee e = dao.newEntity();
        e.orm_propValueByName("id", id);
        e.setCode("EMP-" + id);
        e.setName(name);
        e.setStatus("ACTIVE");
        dao.saveEntity(e);
    }

    private void seedTemplate(long id, String code, String name) {
        IEntityDao<ErpQaInspectionTemplate> dao = daoProvider.daoFor(ErpQaInspectionTemplate.class);
        ErpQaInspectionTemplate t = dao.newEntity();
        t.orm_propValueByName("id", id);
        t.setCode(code);
        t.setName(name);
        t.setInspectionType("INCOMING");
        t.orm_propValueByName("isActive", 1);
        dao.saveEntity(t);
    }

    private void seedInspection(long id, String code) {
        IEntityDao<ErpQaInspection> dao = daoProvider.daoFor(ErpQaInspection.class);
        ErpQaInspection ins = dao.newEntity();
        ins.orm_propValueByName("id", id);
        ins.setCode(code);
        ins.setInspectionType("INCOMING");
        ins.setMaterialId(MATERIAL_ID);
        ins.setSupplierId(SUPPLIER_ID);
        ins.setWarehouseId(WAREHOUSE_ID);
        ins.setInspectorId(INSPECTOR_ID);
        ins.setOrgId(ORG_ID);
        ins.setTemplateId(TEMPLATE_ID);
        ins.setResult("PENDING");
        ins.orm_propValueByName("docStatus", "ACTIVE");
        ins.orm_propValueByName("approveStatus", "UNSUBMITTED");
        ins.setPosted(Boolean.FALSE);
        ins.setInspectionDate(LocalDate.of(2026, 7, 1));
        ins.setBusinessDate(LocalDate.of(2026, 7, 1));
        dao.saveEntity(ins);
    }

    private void seedNcr(long id, String code) {
        IEntityDao<ErpQaNonConformance> dao = daoProvider.daoFor(ErpQaNonConformance.class);
        ErpQaNonConformance ncr = dao.newEntity();
        ncr.orm_propValueByName("id", id);
        ncr.setCode(code);
        ncr.setNcrDate(LocalDate.of(2026, 7, 1));
        ncr.setMaterialId(MATERIAL_ID);
        ncr.setSupplierId(SUPPLIER_ID);
        ncr.setInspectionId(INSPECTION_ID);
        ncr.setSeverity("NORMAL");
        ncr.orm_propValueByName("status", "OPEN");
        dao.saveEntity(ncr);
    }
}
