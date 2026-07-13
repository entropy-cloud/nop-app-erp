package app.erp.inv.service;

import app.erp.inv.dao.entity.ErpInvCostLayer;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockLedger;
import app.erp.md.dao.entity.ErpMdMaterial;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LIFO（后进先出）成本策略集成测试（L-2，杠杆 E）。镜像 {@code TestErpInvFifoCosting} 结构，
 * 验证 {@code LifoCostingStrategy}（{@code COST_METHOD_LIFO}）的核心差异：出库按 {@code incomingDate}
 * <b>降序</b>消耗 cost layer（后入库的先消耗）。
 *
 * <p>权威：{@code docs/design/finance/costing-methods.md §LIFO}。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvLifoCosting extends JunitAutoTestCase {

    static final Long ORG_ID = 1201L;
    static final Long WAREHOUSE_ID = 3201L;
    static final Long LOCATION_ID = 4201L;
    static final Long UOM_ID = 5201L;
    static final Long CURRENCY_ID = 6201L;
    static final Long ACCT_SCHEMA_ID = 7201L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testIncomingAppendsCostLayer() {
        Long materialId = 2301L;
        seedLifoMaterial(materialId);

        generateIncoming(materialId, "PR-LIFO-001", new BigDecimal("50"), new BigDecimal("10"), "2026-07-01");

        List<ErpInvCostLayer> layers = findCostLayers(materialId);
        assertEquals(1, layers.size(), "入库应追加 1 个 cost layer");
        ErpInvCostLayer layer = layers.get(0);
        assertEquals(0, layer.getRemainingQuantity().compareTo(new BigDecimal("50")), "remainingQuantity=50");
        assertEquals(0, layer.getUnitCost().compareTo(new BigDecimal("10")), "unitCost=10");
        assertEquals(0, layer.getTotalCost().compareTo(new BigDecimal("500")), "totalCost=500");
        assertEquals(ErpInvConstants.COST_METHOD_LIFO, layer.getCostMethod(), "costMethod=LIFO(40)");

        ErpInvStockBalance balance = findBalance(materialId);
        assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("50")), "余额 totalQuantity=50");
        assertEquals(0, balance.getTotalCost().compareTo(new BigDecimal("500")), "余额 totalCost=500");
        assertNull(balance.getAvgCost(), "LIFO 物料 avgCost 置空");
    }

    @Test
    public void testOutgoingConsumesSingleLayer() {
        Long materialId = 2302L;
        seedLifoMaterial(materialId);

        generateIncoming(materialId, "PR-LIFO-002", new BigDecimal("50"), new BigDecimal("10"), "2026-07-01");
        generateOutgoing(materialId, "SS-LIFO-002", new BigDecimal("30"), "2026-07-10");

        List<ErpInvCostLayer> layers = findCostLayers(materialId);
        assertEquals(1, layers.size(), "单层消耗不删层");
        assertEquals(0, layers.get(0).getRemainingQuantity().compareTo(new BigDecimal("20")),
                "队列 50@10 消耗 30 → remainingQuantity=20");

        ErpInvStockLedger outLedger = findOutgoingLedger(materialId);
        assertEquals(0, outLedger.getUnitCost().compareTo(new BigDecimal("10")), "出库 unitCost=10（单层）");
        assertEquals(0, outLedger.getTotalCost().compareTo(new BigDecimal("-300")), "出库 totalCost=-300（负号）");

        ErpInvStockBalance balance = findBalance(materialId);
        assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("20")), "余额 50-30=20");
        assertEquals(0, balance.getTotalCost().compareTo(new BigDecimal("200")), "余额 500-300=200");
    }

    @Test
    public void testOutgoingConsumesNewestLayerFirst() {
        Long materialId = 2303L;
        seedLifoMaterial(materialId);

        // 不同日期两层：07-01 入 50@10（旧层），07-05 入 40@12（新层）
        generateIncoming(materialId, "PR-LIFO-003A", new BigDecimal("50"), new BigDecimal("10"), "2026-07-01");
        generateIncoming(materialId, "PR-LIFO-003B", new BigDecimal("40"), new BigDecimal("12"), "2026-07-05");
        // 07-10 出 60：LIFO 降序 → 先消耗新层 40@12（全部），再消耗旧层 20@10
        generateOutgoing(materialId, "SS-LIFO-003", new BigDecimal("60"), "2026-07-10");

        List<ErpInvCostLayer> layers = findCostLayers(materialId);
        assertEquals(2, layers.size(), "多层消耗不删层");

        ErpInvCostLayer oldLayer = layers.stream()
                .filter(l -> l.getUnitCost().compareTo(new BigDecimal("10")) == 0).findFirst().orElseThrow();
        assertEquals(0, oldLayer.getRemainingQuantity().compareTo(new BigDecimal("30")),
                "旧层 50@10 消耗 20 → remainingQuantity=30（LIFO 后消耗旧层）");

        ErpInvCostLayer newLayer = layers.stream()
                .filter(l -> l.getUnitCost().compareTo(new BigDecimal("12")) == 0).findFirst().orElseThrow();
        assertEquals(0, newLayer.getRemainingQuantity().compareTo(BigDecimal.ZERO),
                "新层 40@12 全消耗 → remainingQuantity=0（LIFO 先消耗新层）");

        ErpInvStockLedger outLedger = findOutgoingLedger(materialId);
        // 40×12 + 20×10 = 680（LIFO 先消耗新层 40@12 再消耗旧层 20@10；与 FIFO 顺序相反）
        assertEquals(0, outLedger.getTotalCost().compareTo(new BigDecimal("-680")),
                "LIFO 跨层消耗 COGS totalCost=-680（40×12+20×10，负号）");
    }

    @Test
    public void testFirstOutgoingWithoutCostLayerRejected() {
        Long materialId = 2304L;
        seedLifoMaterial(materialId);
        // 不先入库，直接出库 → ERR_COST_NOT_AVAILABLE（无 remainingQuantity>0 的层）
        setNegativeStock(true);
        try {
            ApiResponse<?> resp = genMove(outgoingReq(materialId, "SS-LIFO-004", new BigDecimal("5"), "2026-07-10"));
            assertEquals(ErpInvErrors.ERR_COST_NOT_AVAILABLE.getErrorCode(), resp.getCode(),
                    "LIFO 物料未入库直接出库应返回 ERR_COST_NOT_AVAILABLE");
            assertTrue(findCostLayers(materialId).isEmpty(), "拒绝不应残留 cost layer");
        } finally {
            setNegativeStock(false);
        }
    }

    // ---------- helpers ----------

    private Long generateIncoming(Long materialId, String billCode, BigDecimal qty, BigDecimal unitCost, String date) {
        Map<String, Object> req = baseReq(materialId, ErpInvConstants.MOVE_TYPE_INCOMING, date);
        req.put("destWarehouseId", WAREHOUSE_ID);
        req.put("destLocationId", LOCATION_ID);
        req.put("relatedBillType", "PUR_RECEIPT");
        req.put("relatedBillCode", billCode);
        req.put("lines", Collections.singletonList(line(materialId, qty, unitCost)));
        return idOf(genMove(req));
    }

    private Long generateOutgoing(Long materialId, String billCode, BigDecimal qty, String date) {
        Map<String, Object> req = baseReq(materialId, ErpInvConstants.MOVE_TYPE_OUTGOING, date);
        req.put("sourceWarehouseId", WAREHOUSE_ID);
        req.put("sourceLocationId", LOCATION_ID);
        req.put("relatedBillType", "SALES_SHIP");
        req.put("relatedBillCode", billCode);
        req.put("lines", Collections.singletonList(line(materialId, qty, null)));
        return idOf(genMove(req));
    }

    private Map<String, Object> outgoingReq(Long materialId, String billCode, BigDecimal qty, String date) {
        Map<String, Object> req = baseReq(materialId, ErpInvConstants.MOVE_TYPE_OUTGOING, date);
        req.put("sourceWarehouseId", WAREHOUSE_ID);
        req.put("sourceLocationId", LOCATION_ID);
        req.put("relatedBillType", "SALES_SHIP");
        req.put("relatedBillCode", billCode);
        req.put("lines", Collections.singletonList(line(materialId, qty, null)));
        return req;
    }

    private ApiResponse<?> genMove(Map<String, Object> req) {
        return executeRpc(mutation, "ErpInvStockMove__generateMove", ApiRequest.build(Map.of("request", req)));
    }

    private Map<String, Object> baseReq(Long materialId, String moveType, String date) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("moveType", moveType);
        req.put("orgId", ORG_ID);
        req.put("businessDate", date);
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

    private Long idOf(ApiResponse<?> resp) {
        Object id = ((Map<?, ?>) resp.getData()).get("id");
        return id instanceof Number ? ((Number) id).longValue() : Long.parseLong(String.valueOf(id));
    }

    private ErpInvStockBalance findBalance(Long materialId) {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        List<ErpInvStockBalance> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpInvCostLayer> findCostLayers(Long materialId) {
        IEntityDao<ErpInvCostLayer> dao = daoProvider.daoFor(ErpInvCostLayer.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        return dao.findAllByQuery(q);
    }

    private ErpInvStockLedger findOutgoingLedger(Long materialId) {
        IEntityDao<ErpInvStockLedger> dao = daoProvider.daoFor(ErpInvStockLedger.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        return dao.findAllByQuery(q).stream()
                .filter(l -> l.getQuantity() != null && l.getQuantity().signum() < 0)
                .findFirst()
                .orElse(null);
    }

    private void seedLifoMaterial(Long id) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            ErpMdMaterial material = new ErpMdMaterial();
            material.orm_propValueByName("id", id);
            material.setCode("MATL-" + id);
            material.setName("LIFO Material " + id);
            material.orm_propValueByName("materialType", "GOODS");
            material.setUoMId(UOM_ID);
            material.setStatus("ACTIVE");
            material.setCostMethod(ErpInvConstants.COST_METHOD_LIFO);
            dao.saveEntity(material);
        });
    }

    private void setNegativeStock(boolean value) {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpInvConstants.CONFIG_ALLOW_NEGATIVE_STOCK, String.valueOf(value));
    }
}
