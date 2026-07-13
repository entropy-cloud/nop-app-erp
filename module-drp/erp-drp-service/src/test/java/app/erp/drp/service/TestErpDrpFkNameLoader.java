package app.erp.drp.service;

import app.erp.drp.dao.entity.ErpDrpLine;
import app.erp.drp.dao.entity.ErpDrpPlan;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdOrganization;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 高价值外键名称解析 BizLoader 测试（机制 D：xmeta 派生 *Name + @BizLoader 批量加载）。
 *
 * <p>覆盖 drp 域核心实体 ErpDrpLine（物料/目标仓库/来源仓库名称对齐）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpDrpFkNameLoader extends JunitAutoTestCase {

    static final Long ORG_ID = 9101L;
    static final Long PLAN_ID = 9201L;
    static final Long MATERIAL_ID = 9301L;
    static final Long WAREHOUSE_ID = 9401L;
    static final Long SOURCE_WH_ID = 9501L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testDrpLineFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "DRP测试组织");
            seedPlan(PLAN_ID);
            seedMaterial(MATERIAL_ID, "补货物料");
            seedWarehouse(WAREHOUSE_ID, "目标仓");
            seedWarehouse(SOURCE_WH_ID, "来源仓");
            seedDrpLine(8001L);
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpDrpLine__findList",
                "id", "materialName", "warehouseName", "sourceWarehouseName");
        assertNotNull(rows);
        assertEquals(false, rows.isEmpty(), "至少 1 条 DRP 行");
        Map<String, Object> first = rows.get(0);
        assertEquals("补货物料", first.get("materialName"));
        assertEquals("目标仓", first.get("warehouseName"));
        assertEquals("来源仓", first.get("sourceWarehouseName"));
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

    private void seedMaterial(long id, String name) {
        IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
        ErpMdMaterial m = dao.newEntity();
        m.orm_propValue(1, id);
        m.setCode("MAT-" + id);
        m.setName(name);
        m.orm_propValueByName("materialType", "GOODS");
        m.orm_propValueByName("uoMId", 9901L);
        m.orm_propValueByName("status", "ACTIVE");
        dao.saveEntity(m);
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

    private void seedPlan(long id) {
        IEntityDao<ErpDrpPlan> dao = daoProvider.daoFor(ErpDrpPlan.class);
        ErpDrpPlan p = dao.newEntity();
        p.orm_propValue(1, id);
        p.setCode("PLAN-" + id);
        p.orm_propValueByName("planName", "补货计划" + id);
        p.orm_propValueByName("periodFrom", LocalDate.of(2026, 7, 1));
        p.orm_propValueByName("periodTo", LocalDate.of(2026, 7, 31));
        p.orm_propValueByName("status", "DRAFT");
        p.orm_propValueByName("businessDate", LocalDate.of(2026, 7, 1));
        p.orm_propValueByName("orgId", ORG_ID);
        dao.saveEntity(p);
    }

    private void seedDrpLine(long id) {
        IEntityDao<ErpDrpLine> dao = daoProvider.daoFor(ErpDrpLine.class);
        ErpDrpLine l = dao.newEntity();
        l.orm_propValue(1, id);
        l.orm_propValueByName("planId", PLAN_ID);
        l.orm_propValueByName("lineNo", 1);
        l.orm_propValueByName("materialId", MATERIAL_ID);
        l.orm_propValueByName("warehouseId", WAREHOUSE_ID);
        l.orm_propValueByName("sourceWarehouseId", SOURCE_WH_ID);
        l.orm_propValueByName("orgId", ORG_ID);
        l.orm_propValueByName("replenishmentType", "PURCHASE");
        l.orm_propValueByName("status", "DRAFT");
        dao.saveEntity(l);
    }
}
