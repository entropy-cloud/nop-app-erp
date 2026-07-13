package app.erp.aps.service;

import app.erp.aps.dao.entity.ErpApsOperationOrder;
import app.erp.md.dao.entity.ErpMdOrganization;
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
 * <p>覆盖 aps 域核心实体 ErpApsOperationOrder（业务组织名称对齐）。
 * 注意：APS→manufacturing 跨域弱指针（workOrderId/machineId）无 ext:relation，保留原始 ID（Deferred）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpApsFkNameLoader extends JunitAutoTestCase {

    static final Long ORG_ID = 9101L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testOperationOrderFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "APS测试组织");
            seedOperationOrder(8001L);
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpApsOperationOrder__findList",
                "id", "orgName");
        assertNotNull(rows);
        assertEquals(false, rows.isEmpty(), "至少 1 条工序订单");
        Map<String, Object> first = rows.get(0);
        assertEquals("APS测试组织", first.get("orgName"));
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

    private void seedOperationOrder(long id) {
        IEntityDao<ErpApsOperationOrder> dao = daoProvider.daoFor(ErpApsOperationOrder.class);
        ErpApsOperationOrder o = dao.newEntity();
        o.orm_propValue(1, id);
        o.setCode("OO-" + id);
        o.orm_propValueByName("workOrderId", 9901L);
        o.orm_propValueByName("operationName", "工序" + id);
        o.orm_propValueByName("sequence", 10);
        o.orm_propValueByName("machineId", 9902L);
        o.orm_propValueByName("qty", new BigDecimal("100"));
        o.orm_propValueByName("status", "DRAFT");
        o.orm_propValueByName("businessDate", LocalDate.of(2026, 7, 1));
        o.orm_propValueByName("orgId", ORG_ID);
        dao.saveEntity(o);
    }
}
