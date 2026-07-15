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
import io.nop.api.core.beans.FieldSelectionBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import io.nop.api.core.util.FutureHelper;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * FK 显示名解析测试（平台自动管线：tagSet="disp" → {relation}.{dispCol} 路径属性）。
 *
 * <p>验证 ORM tagSet="disp" 标记后，代码生成器在 xmeta 中自动生成路径属性，
 * GraphQL 引擎在 session 内可通过路径属性访问关联实体的显示列。
 * 列表批量加载由 XuiViewAnalyzer 在 view.xml 渲染时自动注入（E2E 覆盖）。
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

            ErpMdMaterial m = daoProvider.daoFor(ErpMdMaterial.class).getEntityById(8001L);
            assertNotNull(m, "物料应存在");

            FieldSelectionBean selection = new FieldSelectionBean();
            selection.addField("id");
            selection.addCompositeField("category.name", false);
            selection.addCompositeField("uoM.name", false);
            selection.addCompositeField("defaultWarehouse.name", false);

            Map<String, Object> result = (Map<String, Object>) FutureHelper.syncGet(
                    graphQLEngine.fetchResultWithSelection(m, "ErpMdMaterial", selection, null));
            assertEquals("原材料", nestedGet(result, "category.name"));
            assertEquals("个", nestedGet(result, "uoM.name"));
            assertEquals("中央仓", nestedGet(result, "defaultWarehouse.name"));
        });
    }

    @Test
    public void testEmployeeFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "MD测试组织");
            seedPartner(PARTNER_ID, "员工关联伙伴");
            seedEmployee(8101L);

            ErpMdEmployee e = daoProvider.daoFor(ErpMdEmployee.class).getEntityById(8101L);
            assertNotNull(e, "员工应存在");

            FieldSelectionBean selection = new FieldSelectionBean();
            selection.addField("id");
            selection.addCompositeField("organization.name", false);
            selection.addCompositeField("partner.name", false);

            Map<String, Object> result = (Map<String, Object>) FutureHelper.syncGet(
                    graphQLEngine.fetchResultWithSelection(e, "ErpMdEmployee", selection, null));
            assertEquals("MD测试组织", nestedGet(result, "organization.name"));
            assertEquals("员工关联伙伴", nestedGet(result, "partner.name"));
        });
    }

    @SuppressWarnings("unchecked")
    private Object nestedGet(Map<String, Object> map, String dotPath) {
        String[] parts = dotPath.split("\\.");
        Object current = map;
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return null;
            }
        }
        return current;
    }

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
