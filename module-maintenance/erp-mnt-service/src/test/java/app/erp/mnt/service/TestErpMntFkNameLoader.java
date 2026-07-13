package app.erp.mnt.service;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.md.dao.entity.ErpMdLocation;
import app.erp.md.dao.entity.ErpMdOrganization;
import app.erp.md.dao.entity.ErpMdWarehouse;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.dao.entity.ErpMntEquipmentCategory;
import app.erp.mnt.dao.entity.ErpMntRequest;
import app.erp.mnt.dao.entity.ErpMntSparePartUsage;
import app.erp.mnt.dao.entity.ErpMntVisit;
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
 * <p>覆盖 maintenance 域代表实体。经 {@link IGraphQLEngine} findList + {@link FieldSelectionBean}
 * 请求派生字段触发 @BizLoader，验证批量加载名称正确（防 N+1 + 名称对齐 master-data/assets）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMntFkNameLoader extends JunitAutoTestCase {

    static final Long ORG_ID = 8201L;
    static final Long WAREHOUSE_ID = 8203L;
    static final Long LOCATION_ID = 8204L;
    static final Long CATEGORY_ID = 8205L;
    static final Long ASSET_ID = 8206L;
    static final Long EQUIPMENT_ID = 8207L;
    static final Long VISIT_ID = 8208L;
    static final Long REQUEST_ID = 8209L;
    static final Long USAGE_ID = 8210L;
    static final Long REQUESTED_BY_ID = 8211L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testEquipmentFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "维护测试组织");
            seedWarehouse(WAREHOUSE_ID, "备件仓");
            seedLocation(LOCATION_ID, "车间A");
            seedCategory(CATEGORY_ID, "数控设备");
            seedAsset(ASSET_ID, "AST-FKN-001", "数控机床A");
            seedEquipment(EQUIPMENT_ID, "EQ-FKN-001", "数控机床A");
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpMntEquipment__findList",
                "id", "code", "name", "orgName", "categoryName", "locationName", "assetCode");
        assertNotNull(rows);
        assertFalse(rows.isEmpty(), "至少 1 条设备");
        Map<String, Object> first = rows.get(0);
        assertEquals("维护测试组织", first.get("orgName"));
        assertEquals("数控设备", first.get("categoryName"));
        assertEquals("车间A", first.get("locationName"));
        assertEquals("AST-FKN-001", first.get("assetCode"));
    }

    @Test
    public void testSparePartUsageFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "维护测试组织");
            seedWarehouse(WAREHOUSE_ID, "备件仓");
            seedEquipment(EQUIPMENT_ID, "EQ-FKN-001", "数控机床A");
            seedVisit(VISIT_ID, "VIS-FKN-001");
            seedRequest(REQUEST_ID, "REQ-FKN-001");
            seedUsage(USAGE_ID, "SPU-FKN-001");
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpMntSparePartUsage__findList",
                "id", "code", "orgName", "visitCode", "requestCode", "equipmentCode", "warehouseName");
        assertNotNull(rows);
        assertFalse(rows.isEmpty(), "至少 1 条备件消耗");
        Map<String, Object> first = rows.get(0);
        assertEquals("维护测试组织", first.get("orgName"));
        assertEquals("VIS-FKN-001", first.get("visitCode"));
        assertEquals("REQ-FKN-001", first.get("requestCode"));
        assertEquals("EQ-FKN-001", first.get("equipmentCode"));
        assertEquals("备件仓", first.get("warehouseName"));
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

    private void seedWarehouse(long id, String name) {
        IEntityDao<ErpMdWarehouse> dao = daoProvider.daoFor(ErpMdWarehouse.class);
        ErpMdWarehouse w = dao.newEntity();
        w.orm_propValueByName("id", id);
        w.setCode("WH-" + id);
        w.setName(name);
        w.setStatus("ACTIVE");
        dao.saveEntity(w);
    }

    private void seedLocation(long id, String name) {
        IEntityDao<ErpMdLocation> dao = daoProvider.daoFor(ErpMdLocation.class);
        ErpMdLocation l = dao.newEntity();
        l.orm_propValueByName("id", id);
        l.setCode("LOC-" + id);
        l.setName(name);
        l.setWarehouseId(WAREHOUSE_ID);
        dao.saveEntity(l);
    }

    private void seedCategory(long id, String name) {
        IEntityDao<ErpMntEquipmentCategory> dao = daoProvider.daoFor(ErpMntEquipmentCategory.class);
        ErpMntEquipmentCategory c = dao.newEntity();
        c.orm_propValueByName("id", id);
        c.setCode("CAT-" + id);
        c.setName(name);
        dao.saveEntity(c);
    }

    private void seedAsset(long id, String code, String name) {
        IEntityDao<ErpAstAsset> dao = daoProvider.daoFor(ErpAstAsset.class);
        ErpAstAsset a = dao.newEntity();
        a.orm_propValueByName("id", id);
        a.setCode(code);
        a.setName(name);
        a.setAcquisitionDate(LocalDate.of(2026, 7, 1));
        a.setOriginalValue(new BigDecimal("10000"));
        a.orm_propValueByName("status", "IN_SERVICE");
        dao.saveEntity(a);
    }

    private void seedEquipment(long id, String code, String name) {
        IEntityDao<ErpMntEquipment> dao = daoProvider.daoFor(ErpMntEquipment.class);
        ErpMntEquipment e = dao.newEntity();
        e.orm_propValueByName("id", id);
        e.setCode(code);
        e.setName(name);
        e.orm_propValueByName("status", "RUNNING");
        e.setOrgId(ORG_ID);
        e.setCategoryId(CATEGORY_ID);
        e.setLocationId(LOCATION_ID);
        e.setAssetId(ASSET_ID);
        dao.saveEntity(e);
    }

    private void seedVisit(long id, String code) {
        IEntityDao<ErpMntVisit> dao = daoProvider.daoFor(ErpMntVisit.class);
        ErpMntVisit v = dao.newEntity();
        v.orm_propValueByName("id", id);
        v.setCode(code);
        v.setEquipmentId(EQUIPMENT_ID);
        v.setVisitDate(LocalDate.of(2026, 7, 1));
        v.orm_propValueByName("status", "COMPLETED");
        dao.saveEntity(v);
    }

    private void seedRequest(long id, String code) {
        IEntityDao<ErpMntRequest> dao = daoProvider.daoFor(ErpMntRequest.class);
        ErpMntRequest r = dao.newEntity();
        r.orm_propValueByName("id", id);
        r.setCode(code);
        r.setEquipmentId(EQUIPMENT_ID);
        r.setRequestDate(LocalDate.of(2026, 7, 1));
        r.setDescription("报修:" + code);
        r.orm_propValueByName("priority", "NORMAL");
        r.orm_propValueByName("status", "COMPLETED");
        r.setRequestedBy(REQUESTED_BY_ID);
        dao.saveEntity(r);
    }

    private void seedUsage(long id, String code) {
        IEntityDao<ErpMntSparePartUsage> dao = daoProvider.daoFor(ErpMntSparePartUsage.class);
        ErpMntSparePartUsage u = dao.newEntity();
        u.orm_propValueByName("id", id);
        u.setCode(code);
        u.setOrgId(ORG_ID);
        u.setVisitId(VISIT_ID);
        u.setRequestId(REQUEST_ID);
        u.setEquipmentId(EQUIPMENT_ID);
        u.setWarehouseId(WAREHOUSE_ID);
        u.setBusinessDate(LocalDate.of(2026, 7, 1));
        u.orm_propValueByName("docStatus", "DRAFT");
        u.orm_propValueByName("approveStatus", "UNSUBMITTED");
        u.orm_propValueByName("posted", false);
        dao.saveEntity(u);
    }
}
