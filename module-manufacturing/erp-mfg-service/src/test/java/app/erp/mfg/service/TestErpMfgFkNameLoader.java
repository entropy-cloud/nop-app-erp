package app.erp.mfg.service;

import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdUoM;
import app.erp.md.dao.entity.ErpMdWarehouse;
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
}
