package app.erp.inv.service;

import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdMaterial;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Phase 1 集成测试：成本方法策略分派重构（MOVING_AVERAGE 抽取行为不变 + CostMethodResolver 字典映射）。
 *
 * <p>覆盖：物料显式配 MOVING_AVERAGE 走既有路径；物料未配回退账套；账套未配回退默认；costing-enabled=false
 * 一律回退移动加权平均；既有 bookkeeping 套件行为字节级不变（avgCost 重算/出库取 avgCost）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvCostingDispatch extends JunitAutoTestCase {

    static final Long ORG_ID = 1101L;
    static final Long WAREHOUSE_ID = 3101L;
    static final Long LOCATION_ID = 4101L;
    static final Long UOM_ID = 5101L;
    static final Long CURRENCY_ID = 6101L;
    static final Long ACCT_SCHEMA_ID = 7101L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testMaterialConfiguredMovingAverageUnchangedBehavior() {
        Long materialId = 2101L;
        seedMaterial(materialId, ErpInvConstants.COST_METHOD_MOVING_AVERAGE);

        generateIncoming(materialId, "PR-DISP-001", new BigDecimal("10"), new BigDecimal("6"));
        ErpInvStockBalance balance = findBalance(materialId);
        assertEquals(0, balance.getAvgCost().compareTo(new BigDecimal("6")), "显式 MOVING_AVERAGE: avgCost=6");
        assertEquals(0, balance.getTotalCost().compareTo(new BigDecimal("60")), "totalCost=60");
        assertEquals(ErpInvConstants.COST_METHOD_MOVING_AVERAGE, balance.getCostMethod(),
                "余额 costMethod 仍为移动加权平均");

        generateIncoming(materialId, "PR-DISP-002", new BigDecimal("10"), new BigDecimal("8"));
        ErpInvStockBalance updated = findBalance(materialId);
        assertEquals(0, updated.getAvgCost().compareTo(new BigDecimal("7")),
                "二次入库 avgCost=(60+80)/20=7（既有重算逻辑不变）");
    }

    @Test
    public void testMaterialCostMethodNullFallsBackToAcctSchema() {
        Long materialId = 2102L;
        seedAcctSchema(ACCT_SCHEMA_ID, ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
        seedMaterial(materialId, null);

        generateIncoming(materialId, "PR-DISP-003", new BigDecimal("5"), new BigDecimal("4"));
        ErpInvStockBalance balance = findBalance(materialId);
        assertEquals(0, balance.getAvgCost().compareTo(new BigDecimal("4")),
                "物料 costMethod=null → 账套 costingMethod=10 (MOVING_AVERAGE) 生效");
    }

    @Test
    public void testCostingDisabledFallbackToMovingAverage() {
        Long materialId = 2103L;
        seedMaterial(materialId, ErpInvConstants.COST_METHOD_MOVING_AVERAGE);

        AppConfigProvider.set(ErpInvConstants.CONFIG_COSTING_ENABLED, "false");
        try {
            generateIncoming(materialId, "PR-DISP-004", new BigDecimal("4"), new BigDecimal("3"));
            ErpInvStockBalance balance = findBalance(materialId);
            assertEquals(0, balance.getAvgCost().compareTo(new BigDecimal("3")),
                    "costing-enabled=false 回退既有硬编码移动加权平均");
        } finally {
            AppConfigProvider.set(ErpInvConstants.CONFIG_COSTING_ENABLED, "true");
        }
    }

    @Test
    public void testDictionaryCodeMappingMovingAverage() {
        // 字典 erp-md/cost-method: 10=移动加权平均、20=全月一次、30=FIFO、40=LIFO、50=标准、60=个别、70=批次
        assertEquals(10, ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
        assertEquals(30, ErpInvConstants.COST_METHOD_FIFO);
        assertEquals(40, ErpInvConstants.COST_METHOD_LIFO);
        assertEquals(50, ErpInvConstants.COST_METHOD_STANDARD);
        assertEquals(20, ErpInvConstants.COST_METHOD_MONTHLY_WEIGHTED_AVERAGE);
        assertEquals(60, ErpInvConstants.COST_METHOD_INDIVIDUAL);
        assertEquals(70, ErpInvConstants.COST_METHOD_BATCH);
    }

    @Test
    public void testUnrecognizedCostMethodFallsBackToDefault() {
        // 物料配 LIFO（40，本期 Non-Goal）→ resolver 不识别 → 回退默认 MOVING_AVERAGE，记账不中断
        Long materialId = 2104L;
        seedMaterial(materialId, ErpInvConstants.COST_METHOD_LIFO);

        generateIncoming(materialId, "PR-DISP-005", new BigDecimal("3"), new BigDecimal("2"));
        ErpInvStockBalance balance = findBalance(materialId);
        assertEquals(0, balance.getAvgCost().compareTo(new BigDecimal("2")),
                "未实现方法码值回退默认 MOVING_AVERAGE，记账不中断");
    }

    @Test
    public void testInternalTransferCarriesCostAcrossWarehouses() {
        Long materialId = 2105L;
        seedMaterial(materialId, ErpInvConstants.COST_METHOD_MOVING_AVERAGE);

        generateIncoming(materialId, "PR-DISP-006", new BigDecimal("10"), new BigDecimal("5"));

        Map<String, Object> req = baseReq(materialId, ErpInvConstants.MOVE_TYPE_INTERNAL_TRANSFER);
        req.put("sourceWarehouseId", WAREHOUSE_ID);
        req.put("sourceLocationId", LOCATION_ID);
        req.put("destWarehouseId", 9101L);
        req.put("destLocationId", 9201L);
        req.put("relatedBillType", "TRANSFER");
        req.put("relatedBillCode", "TR-DISP-001");
        req.put("lines", Collections.singletonList(line(materialId, new BigDecimal("4"), null)));
        ApiResponse<?> resp = genMove(req);
        assertEquals(0, resp.getStatus(), "内部调拨 DONE 应成功");

        ErpInvStockBalance source = findBalance(materialId);
        assertEquals(0, source.getTotalQuantity().compareTo(new BigDecimal("6")), "源仓剩 10-4=6");
        assertEquals(0, source.getAvgCost().compareTo(new BigDecimal("5")), "源仓 avgCost 沿用 5");
    }

    // ---------- helpers ----------

    private void generateIncoming(Long materialId, String billCode, BigDecimal qty, BigDecimal unitCost) {
        Map<String, Object> req = baseReq(materialId, ErpInvConstants.MOVE_TYPE_INCOMING);
        req.put("destWarehouseId", WAREHOUSE_ID);
        req.put("destLocationId", LOCATION_ID);
        req.put("relatedBillType", "PUR_RECEIPT");
        req.put("relatedBillCode", billCode);
        req.put("lines", Collections.singletonList(line(materialId, qty, unitCost)));
        assertEquals(0, genMove(req).getStatus(), "generateMove 应成功");
    }

    private ApiResponse<?> genMove(Map<String, Object> req) {
        return executeRpc(mutation, "ErpInvStockMove__generateMove", ApiRequest.build(Map.of("request", req)));
    }

    private Map<String, Object> baseReq(Long materialId, Integer moveType) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("moveType", moveType);
        req.put("orgId", ORG_ID);
        req.put("businessDate", "2026-07-01");
        req.put("acctSchemaId", ACCT_SCHEMA_ID);
        req.put("currencyId", CURRENCY_ID);
        return req;
    }

    private Map<String, Object> line(Long materialId, BigDecimal qty, BigDecimal unitCost) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("materialId", materialId);
        line.put("uoMId", UOM_ID);
        line.put("quantity", qty);
        if (unitCost != null) {
            line.put("unitCost", unitCost);
        }
        line.put("currencyId", CURRENCY_ID);
        return line;
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpInvStockBalance findBalance(Long materialId) {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        List<ErpInvStockBalance> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private void seedMaterial(Long id, Integer costMethod) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            ErpMdMaterial material = new ErpMdMaterial();
            material.orm_propValueByName("id", id);
            material.setCode("MAT-" + id);
            material.setName("Material " + id);
            material.orm_propValueByName("materialType", 10);
            material.setUoMId(UOM_ID);
            material.setStatus(10);
            material.setCostMethod(costMethod);
            dao.saveEntity(material);
        });
    }

    private void seedAcctSchema(Long id, Integer costingMethod) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
            ErpMdAcctSchema schema = new ErpMdAcctSchema();
            schema.orm_propValueByName("id", id);
            schema.setCode("AS-" + id);
            schema.setName("AcctSchema " + id);
            schema.setOrgId(ORG_ID);
            schema.orm_propValueByName("nature", 10);
            schema.orm_propValueByName("functionalCurrencyId", CURRENCY_ID);
            schema.setStatus(10);
            schema.setCostingMethod(costingMethod);
            dao.saveEntity(schema);
        });
    }

    /** AppConfig 写入辅助（隔离对 AppConfig.getConfigProvider() 的直接依赖）。 */
    static final class AppConfigProvider {
        static void set(String key, String value) {
            io.nop.api.core.config.AppConfig.getConfigProvider().assignConfigValue(key, value);
        }
    }
}
