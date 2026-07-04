package app.erp.mfg.service;

import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
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
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 需求预测实体（ErpMfgForecast/ErpMfgForecastLine）CRUD 冒烟测试。
 *
 * <p>验证 plan 2026-07-05-0427-1 Phase 1 Exit Criteria：实体可生成并可标准 CRUD；
 * 头-行外键引用经 GraphQL mutation/query 返回成功且非空 ID。
 *
 * <p>不依赖快照基线，仅断言 GraphQL 返回状态 + 非空 ID（避免引入 RECORDING 录制工作）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgForecastCrudSmoke extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IOrmTemplate ormTemplate;

    @Test
    public void testCreateHead() {
        ApiResponse<?> result = saveHead();
        assertEquals(0, result.getStatus());
        assertNotNull(((Map<?, ?>) result.getData()).get("id"), "新建应返回非空 ID");
    }

    @Test
    public void testQueryHead() {
        ApiResponse<?> created = saveHead();
        String headId = String.valueOf(((Map<?, ?>) created.getData()).get("id"));

        Map<String, Object> q = new LinkedHashMap<>();
        q.put("filter_id", headId);
        q.put("limit", 10);
        ApiResponse<?> page = executeRpc(GraphQLOperationType.query, "ErpMfgForecast__findPage",
                ApiRequest.build(q));
        assertEquals(0, page.getStatus());
        assertTrue(((Number) ((Map<?, ?>) page.getData()).get("total")).intValue() >= 1, "查询应至少返回 1 条");
    }

    @Test
    public void testLineRelation() {
        seedPrereqs();
        ApiResponse<?> head = saveHead();
        String headId = String.valueOf(((Map<?, ?>) head.getData()).get("id"));

        Map<String, Object> lineData = new LinkedHashMap<>();
        lineData.put("lineNo", 10);
        lineData.put("forecastId", headId);
        lineData.put("materialId", 1001L);
        lineData.put("uoMId", 1001L);
        lineData.put("periodStart", LocalDate.of(2026, 7, 1));
        lineData.put("periodEnd", LocalDate.of(2026, 7, 31));
        lineData.put("forecastQty", "100");
        ApiResponse<?> line = executeRpc(GraphQLOperationType.mutation, "ErpMfgForecastLine__save",
                ApiRequest.build(Map.of("data", lineData)));
        assertEquals(0, line.getStatus(), "保存行应成功: " + line);
        assertNotNull(((Map<?, ?>) line.getData()).get("id"), "新建行应返回非空 ID");

        Map<String, Object> q = new LinkedHashMap<>();
        q.put("filter_forecastId", headId);
        q.put("limit", 10);
        ApiResponse<?> page = executeRpc(GraphQLOperationType.query, "ErpMfgForecastLine__findPage",
                ApiRequest.build(q));
        assertEquals(0, page.getStatus());
        assertTrue(((Number) ((Map<?, ?>) page.getData()).get("total")).intValue() >= 1, "按外键查询应至少返回 1 行");
    }

    private void seedPrereqs() {
        ormTemplate.runInSession(() -> {
            IEntityDao<app.erp.md.dao.entity.ErpMdMaterial> matDao = daoProvider.daoFor(app.erp.md.dao.entity.ErpMdMaterial.class);
            app.erp.md.dao.entity.ErpMdMaterial mat = new app.erp.md.dao.entity.ErpMdMaterial();
            mat.orm_propValueByName("id", 1001L);
            mat.setCode("MAT-FCST");
            mat.setName("Forecast Material");
            mat.orm_propValueByName("materialType", "GOODS");
            mat.setStatus("ACTIVE");
            mat.setUoMId(1001L);
            matDao.saveEntity(mat);

            IEntityDao<app.erp.md.dao.entity.ErpMdUoM> uomDao = daoProvider.daoFor(app.erp.md.dao.entity.ErpMdUoM.class);
            app.erp.md.dao.entity.ErpMdUoM uom = new app.erp.md.dao.entity.ErpMdUoM();
            uom.orm_propValueByName("id", 1001L);
            uom.setCode("PCS");
            uom.setName("件");
            uomDao.saveEntity(uom);
        });
    }

    private ApiResponse<?> saveHead() {
        return executeRpc(GraphQLOperationType.mutation, "ErpMfgForecast__save",
                ApiRequest.build(Map.of("data", newHead())));
    }

    private Map<String, Object> newHead() {
        Map<String, Object> headData = new LinkedHashMap<>();
        headData.put("code", "FCST-SMOKE-" + System.nanoTime());
        headData.put("planName", "冒烟预测");
        headData.put("periodFrom", LocalDate.of(2026, 7, 1));
        headData.put("periodTo", LocalDate.of(2026, 7, 31));
        headData.put("status", "DRAFT");
        return headData;
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
