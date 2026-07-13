package app.erp.md.service;

import app.erp.md.dao.entity.ErpMdEmployee;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdMaterialCategory;
import app.erp.md.dao.entity.ErpMdOrganization;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdUoM;
import app.erp.md.dao.entity.ErpMdWarehouse;
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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 高价值外键名称解析 BizLoader 测试（机制 D：xmeta 派生 *Name + @BizLoader 批量加载）。
 *
 * <p>覆盖 master-data 域核心实体。经 {@link IGraphQLEngine} findList + {@link FieldSelectionBean}
 * 请求派生字段触发 @BizLoader，验证批量加载名称正确（防 N+1 + 名称对齐）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMdFkNameLoader extends JunitAutoTestCase {

    static final Long ORG_ID = 9101L;
    static final Long PARTNER_ID = 9201L;
    static final Long CATEGORY_ID = 9301L;
    static final Long UOM_ID = 9401L;
    static final Long WAREHOUSE_ID = 9501L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testMaterialFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "MD测试组织");
            seedCategory(CATEGORY_ID, "原材料");
            seedUoM(UOM_ID, "个");
            seedWarehouse(WAREHOUSE_ID, "中央仓");
            seedMaterial(8001L);
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpMdMaterial__findList",
                "id", "categoryName", "uomName", "defaultWarehouseName");
        assertNotNull(rows);
        assertEquals(false, rows.isEmpty(), "至少 1 条物料");
        Map<String, Object> first = rows.get(0);
        assertEquals("原材料", first.get("categoryName"));
        assertEquals("个", first.get("uomName"));
        assertEquals("中央仓", first.get("defaultWarehouseName"));
    }

    @Test
    public void testEmployeeFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "MD测试组织");
            seedPartner(PARTNER_ID, "员工关联伙伴");
            seedEmployee(8101L);
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpMdEmployee__findList",
                "id", "orgName", "partnerName");
        assertNotNull(rows);
        assertEquals(false, rows.isEmpty(), "至少 1 条员工");
        Map<String, Object> first = rows.get(0);
        assertEquals("MD测试组织", first.get("orgName"));
        assertEquals("员工关联伙伴", first.get("partnerName"));
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

    private void seedOrg(long id, String name) {
        IEntityDao<ErpMdOrganization> dao = daoProvider.daoFor(ErpMdOrganization.class);
        ErpMdOrganization o = dao.newEntity();
        o.orm_propValue(1, id);
        o.setCode("ORG-" + id);
        o.setName(name);
        o.orm_propValueByName("orgType", "COMPANY");
        o.orm_propValueByName("status", "ACTIVE");
        dao.saveEntity(o);
    }

    private void seedPartner(long id, String name) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner p = dao.newEntity();
        p.orm_propValue(1, id);
        p.setCode("CUS-" + id);
        p.setName(name);
        p.orm_propValueByName("partnerType", "CUSTOMER");
        p.orm_propValueByName("status", "ACTIVE");
        dao.saveEntity(p);
    }

    private void seedCategory(long id, String name) {
        IEntityDao<ErpMdMaterialCategory> dao = daoProvider.daoFor(ErpMdMaterialCategory.class);
        ErpMdMaterialCategory c = dao.newEntity();
        c.orm_propValue(1, id);
        c.setCode("CAT-" + id);
        c.setName(name);
        dao.saveEntity(c);
    }

    private void seedUoM(long id, String name) {
        IEntityDao<ErpMdUoM> dao = daoProvider.daoFor(ErpMdUoM.class);
        ErpMdUoM u = dao.newEntity();
        u.orm_propValue(1, id);
        u.setCode("UOM-" + id);
        u.setName(name);
        dao.saveEntity(u);
    }

    private void seedWarehouse(long id, String name) {
        IEntityDao<ErpMdWarehouse> dao = daoProvider.daoFor(ErpMdWarehouse.class);
        ErpMdWarehouse w = dao.newEntity();
        w.orm_propValue(1, id);
        w.setCode("WH-" + id);
        w.setName(name);
        w.orm_propValueByName("status", "ACTIVE");
        dao.saveEntity(w);
    }

    private void seedMaterial(long id) {
        IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
        ErpMdMaterial m = dao.newEntity();
        m.orm_propValue(1, id);
        m.setCode("MAT-" + id);
        m.setName("测试物料" + id);
        m.orm_propValueByName("materialType", "GOODS");
        m.orm_propValueByName("status", "ACTIVE");
        m.orm_propValueByName("categoryId", CATEGORY_ID);
        m.orm_propValueByName("uoMId", UOM_ID);
        m.orm_propValueByName("defaultWarehouseId", WAREHOUSE_ID);
        dao.saveEntity(m);
    }

    private void seedEmployee(long id) {
        IEntityDao<ErpMdEmployee> dao = daoProvider.daoFor(ErpMdEmployee.class);
        ErpMdEmployee e = dao.newEntity();
        e.orm_propValue(1, id);
        e.setCode("EMP-" + id);
        e.setName("员工" + id);
        e.orm_propValueByName("status", "ACTIVE");
        e.orm_propValueByName("orgId", ORG_ID);
        e.orm_propValueByName("partnerId", PARTNER_ID);
        dao.saveEntity(e);
    }
}
