package app.erp.inv.service;

import app.erp.inv.dao.entity.ErpInvCostLayer;
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
 * BATCH（批次计价）成本策略集成测试（L-2，杠杆 E）。验证 {@code BatchCostingStrategy}（{@code COST_METHOD_BATCH}）：
 * 入库按批次创建 cost layer，出库按移动单行 {@code batchNo} 精确匹配同批次成本层消耗（批次内 FIFO 升序），
 * 出库行未携带 batchNo 时拒绝。
 *
 * <p>权威：{@code docs/design/finance/costing-methods.md §BATCH}。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvBatchCosting extends JunitAutoTestCase {

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
    public void testIncomingAppendsCostLayerByBatch() {
        Long materialId = 2401L;
        seedBatchMaterial(materialId);

        generateIncoming(materialId, "PR-BATCH-001", "B01", new BigDecimal("50"), new BigDecimal("10"));

        List<ErpInvCostLayer> layers = findCostLayers(materialId);
        assertEquals(1, layers.size(), "入库应追加 1 个 BATCH cost layer");
        ErpInvCostLayer layer = layers.get(0);
        assertEquals("B01", layer.getBatchNo(), "层 batchNo=B01");
        assertEquals(ErpInvConstants.COST_METHOD_BATCH, layer.getCostMethod(), "costMethod=BATCH(70)");
        assertEquals(0, layer.getRemainingQuantity().compareTo(new BigDecimal("50")), "remainingQuantity=50");
        assertEquals(0, layer.getUnitCost().compareTo(new BigDecimal("10")), "unitCost=10");
    }

    @Test
    public void testOutgoingMatchesBatchAndConsumes() {
        Long materialId = 2402L;
        seedBatchMaterial(materialId);

        generateIncoming(materialId, "PR-BATCH-002", "B01", new BigDecimal("50"), new BigDecimal("10"));
        generateOutgoing(materialId, "SS-BATCH-002", "B01", new BigDecimal("30"));

        List<ErpInvCostLayer> layers = findCostLayers(materialId);
        assertEquals(1, layers.size(), "单层消耗不删层");
        assertEquals(0, layers.get(0).getRemainingQuantity().compareTo(new BigDecimal("20")),
                "B01 批次 50@10 消耗 30 → remainingQuantity=20");

        ErpInvStockLedger outLedger = findOutgoingLedger(materialId);
        assertEquals(0, outLedger.getUnitCost().compareTo(new BigDecimal("10")), "出库 unitCost=10（B01 单层）");
        assertEquals(0, outLedger.getTotalCost().compareTo(new BigDecimal("-300")),
                "出库 totalCost=-300（30×10，负号）");
        assertEquals(ErpInvConstants.COST_METHOD_BATCH, outLedger.getCostMethod(), "流水 costMethod=BATCH(70)");
    }

    @Test
    public void testOutgoingIsolatesByBatchNo() {
        Long materialId = 2403L;
        seedBatchMaterial(materialId);

        // 两批次：B01 50@10，B02 40@12
        generateIncoming(materialId, "PR-BATCH-003A", "B01", new BigDecimal("50"), new BigDecimal("10"));
        generateIncoming(materialId, "PR-BATCH-003B", "B02", new BigDecimal("40"), new BigDecimal("12"));
        // 出库 B01 30 → 仅消耗 B01 层，B02 层不受影响
        generateOutgoing(materialId, "SS-BATCH-003", "B01", new BigDecimal("30"));

        ErpInvCostLayer b01 = findCostLayerByBatch(materialId, "B01");
        ErpInvCostLayer b02 = findCostLayerByBatch(materialId, "B02");
        assertEquals(0, b01.getRemainingQuantity().compareTo(new BigDecimal("20")),
                "B01 50@10 消耗 30 → remainingQuantity=20");
        assertEquals(0, b02.getRemainingQuantity().compareTo(new BigDecimal("40")),
                "B02 40@12 不受 B01 出库影响 → remainingQuantity=40（批次隔离）");

        ErpInvStockLedger outLedger = findOutgoingLedger(materialId);
        assertEquals(0, outLedger.getTotalCost().compareTo(new BigDecimal("-300")),
                "B01 出库 COGS totalCost=-300（30×10，未跨 B02）");
    }

    @Test
    public void testOutgoingWithoutBatchNoRejected() {
        Long materialId = 2404L;
        seedBatchMaterial(materialId);

        generateIncoming(materialId, "PR-BATCH-004", "B01", new BigDecimal("50"), new BigDecimal("10"));
        // 出库行不携带 batchNo → ERR_COST_NOT_AVAILABLE（个别/批次计价要求明确指定来源）
        setNegativeStock(true);
        try {
            ApiResponse<?> resp = genMove(outgoingReqNoBatch(materialId, "SS-BATCH-004", new BigDecimal("5")));
            assertEquals(ErpInvErrors.ERR_COST_NOT_AVAILABLE.getErrorCode(), resp.getCode(),
                    "BATCH 出库未携带 batchNo 应返回 ERR_COST_NOT_AVAILABLE");
        } finally {
            setNegativeStock(false);
        }
    }

    // ---------- helpers ----------

    private Long generateIncoming(Long materialId, String billCode, String batchNo, BigDecimal qty, BigDecimal unitCost) {
        Map<String, Object> req = baseReq(ErpInvConstants.MOVE_TYPE_INCOMING);
        req.put("destWarehouseId", WAREHOUSE_ID);
        req.put("destLocationId", LOCATION_ID);
        req.put("relatedBillType", "PUR_RECEIPT");
        req.put("relatedBillCode", billCode);
        req.put("lines", Collections.singletonList(line(materialId, batchNo, qty, unitCost)));
        return idOf(genMove(req));
    }

    private Long generateOutgoing(Long materialId, String billCode, String batchNo, BigDecimal qty) {
        Map<String, Object> req = baseReq(ErpInvConstants.MOVE_TYPE_OUTGOING);
        req.put("sourceWarehouseId", WAREHOUSE_ID);
        req.put("sourceLocationId", LOCATION_ID);
        req.put("relatedBillType", "SALES_SHIP");
        req.put("relatedBillCode", billCode);
        req.put("lines", Collections.singletonList(line(materialId, batchNo, qty, null)));
        return idOf(genMove(req));
    }

    private Map<String, Object> outgoingReqNoBatch(Long materialId, String billCode, BigDecimal qty) {
        Map<String, Object> req = baseReq(ErpInvConstants.MOVE_TYPE_OUTGOING);
        req.put("sourceWarehouseId", WAREHOUSE_ID);
        req.put("sourceLocationId", LOCATION_ID);
        req.put("relatedBillType", "SALES_SHIP");
        req.put("relatedBillCode", billCode);
        req.put("lines", Collections.singletonList(line(materialId, null, qty, null)));
        return req;
    }

    private ApiResponse<?> genMove(Map<String, Object> req) {
        return executeRpc(mutation, "ErpInvStockMove__generateMove", ApiRequest.build(Map.of("request", req)));
    }

    private Map<String, Object> baseReq(String moveType) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("moveType", moveType);
        req.put("orgId", ORG_ID);
        req.put("businessDate", "2026-07-01");
        req.put("acctSchemaId", ACCT_SCHEMA_ID);
        req.put("currencyId", CURRENCY_ID);
        return req;
    }

    private Map<String, Object> line(Long materialId, String batchNo, BigDecimal qty, BigDecimal unitCost) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("materialId", materialId);
        line.put("uoMId", UOM_ID);
        line.put("quantity", qty);
        if (unitCost != null) {
            line.put("unitCost", unitCost);
        }
        line.put("currencyId", CURRENCY_ID);
        if (batchNo != null) {
            line.put("batchNo", batchNo);
        }
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

    private List<ErpInvCostLayer> findCostLayers(Long materialId) {
        IEntityDao<ErpInvCostLayer> dao = daoProvider.daoFor(ErpInvCostLayer.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        return dao.findAllByQuery(q);
    }

    private ErpInvCostLayer findCostLayerByBatch(Long materialId, String batchNo) {
        return findCostLayers(materialId).stream()
                .filter(l -> batchNo.equals(l.getBatchNo())).findFirst().orElse(null);
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

    private void seedBatchMaterial(Long id) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            ErpMdMaterial material = new ErpMdMaterial();
            material.orm_propValueByName("id", id);
            material.setCode("MATB-" + id);
            material.setName("BATCH Material " + id);
            material.orm_propValueByName("materialType", "GOODS");
            material.setUoMId(UOM_ID);
            material.setStatus("ACTIVE");
            material.setCostMethod(ErpInvConstants.COST_METHOD_BATCH);
            dao.saveEntity(material);
        });
    }

    private void setNegativeStock(boolean value) {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpInvConstants.CONFIG_ALLOW_NEGATIVE_STOCK, String.valueOf(value));
    }
}
