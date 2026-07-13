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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SPECIFIC（个别计价/具体辨认）成本策略集成测试（L-2，杠杆 E）。验证 {@code SpecificCostingStrategy}
 *（{@code COST_METHOD_INDIVIDUAL}）：每批入库创建独立 cost layer，出库按移动单行 {@code batchNo}
 * 精确匹配对应成本层消耗——不同批次物料各自保留独立单位成本；出库行未指定 batchNo/serialNo 时拒绝。
 *
 * <p>权威：{@code docs/design/finance/costing-methods.md §SPECIFIC}。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvSpecificCosting extends JunitAutoTestCase {

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
        Long materialId = 2501L;
        seedSpecificMaterial(materialId);

        generateIncoming(materialId, "PR-SPEC-001", "S01", new BigDecimal("10"), new BigDecimal("100"));

        List<ErpInvCostLayer> layers = findCostLayers(materialId);
        assertEquals(1, layers.size(), "入库应追加 1 个 SPECIFIC cost layer");
        ErpInvCostLayer layer = layers.get(0);
        assertEquals("S01", layer.getBatchNo(), "层 batchNo=S01");
        assertEquals(ErpInvConstants.COST_METHOD_INDIVIDUAL, layer.getCostMethod(), "costMethod=SPECIFIC(60)");
        assertEquals(0, layer.getUnitCost().compareTo(new BigDecimal("100")), "unitCost=100");
        assertEquals(0, layer.getRemainingQuantity().compareTo(new BigDecimal("10")), "remainingQuantity=10");
    }

    @Test
    public void testOutgoingMatchesSpecificBatch() {
        Long materialId = 2502L;
        seedSpecificMaterial(materialId);

        generateIncoming(materialId, "PR-SPEC-002", "S01", new BigDecimal("10"), new BigDecimal("100"));
        generateOutgoing(materialId, "SS-SPEC-002", "S01", new BigDecimal("4"));

        ErpInvCostLayer layer = findCostLayerByBatch(materialId, "S01");
        assertEquals(0, layer.getRemainingQuantity().compareTo(new BigDecimal("6")),
                "S01 10@100 消耗 4 → remainingQuantity=6");

        ErpInvStockLedger outLedger = findOutgoingLedger(materialId);
        // SPECIFIC 每层独立计价：4×100=400
        assertEquals(0, outLedger.getUnitCost().compareTo(new BigDecimal("100")),
                "出库 unitCost=100（S01 批次单价）");
        assertEquals(0, outLedger.getTotalCost().compareTo(new BigDecimal("-400")),
                "出库 totalCost=-400（4×100，负号）");
        assertEquals(ErpInvConstants.COST_METHOD_INDIVIDUAL, outLedger.getCostMethod(),
                "流水 costMethod=SPECIFIC(60)");
    }

    @Test
    public void testOutgoingIsolatesByBatch() {
        Long materialId = 2503L;
        seedSpecificMaterial(materialId);

        // 两批次各自独立单价：S01 10@100，S02 8@200
        generateIncoming(materialId, "PR-SPEC-003A", "S01", new BigDecimal("10"), new BigDecimal("100"));
        generateIncoming(materialId, "PR-SPEC-003B", "S02", new BigDecimal("8"), new BigDecimal("200"));
        // 出库 S01 4 → 仅消耗 S01 层（4×100=400），S02 层单价/余量不变
        generateOutgoing(materialId, "SS-SPEC-003", "S01", new BigDecimal("4"));

        ErpInvCostLayer s01 = findCostLayerByBatch(materialId, "S01");
        ErpInvCostLayer s02 = findCostLayerByBatch(materialId, "S02");
        assertEquals(0, s01.getRemainingQuantity().compareTo(new BigDecimal("6")),
                "S01 10@100 消耗 4 → remainingQuantity=6");
        assertEquals(0, s02.getRemainingQuantity().compareTo(new BigDecimal("8")),
                "S02 8@200 不受 S01 出库影响（批次隔离）");
        assertEquals(0, s02.getUnitCost().compareTo(new BigDecimal("200")), "S02 单价仍 200");

        ErpInvStockLedger outLedger = findOutgoingLedger(materialId);
        assertEquals(0, outLedger.getTotalCost().compareTo(new BigDecimal("-400")),
                "S01 出库 COGS totalCost=-400（4×100，未取 S02 的高价）");
    }

    @Test
    public void testOutgoingWithoutBatchOrSerialRejected() {
        Long materialId = 2504L;
        seedSpecificMaterial(materialId);

        generateIncoming(materialId, "PR-SPEC-004", "S01", new BigDecimal("10"), new BigDecimal("100"));
        // 出库行既无 batchNo 也无 serialNo → ERR_COST_NOT_AVAILABLE（个别计价要求明确指定来源）
        setNegativeStock(true);
        try {
            ApiResponse<?> resp = genMove(outgoingReqNoBatch(materialId, "SS-SPEC-004", new BigDecimal("2")));
            assertEquals(ErpInvErrors.ERR_COST_NOT_AVAILABLE.getErrorCode(), resp.getCode(),
                    "SPECIFIC 出库未携带 batchNo/serialNo 应返回 ERR_COST_NOT_AVAILABLE");
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

    private void seedSpecificMaterial(Long id) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            ErpMdMaterial material = new ErpMdMaterial();
            material.orm_propValueByName("id", id);
            material.setCode("MATS-" + id);
            material.setName("SPECIFIC Material " + id);
            material.orm_propValueByName("materialType", "GOODS");
            material.setUoMId(UOM_ID);
            material.setStatus("ACTIVE");
            material.setCostMethod(ErpInvConstants.COST_METHOD_INDIVIDUAL);
            dao.saveEntity(material);
        });
    }

    private void setNegativeStock(boolean value) {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpInvConstants.CONFIG_ALLOW_NEGATIVE_STOCK, String.valueOf(value));
    }
}
