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
 * 全月一次加权平均（WAM）成本策略集成测试（L-2，杠杆 E）。验证 {@code WeightedAverageCostingStrategy}
 *（{@code COST_METHOD_MONTHLY_WEIGHTED_AVERAGE}）与移动加权平均的关键差异：
 *
 * <ul>
 *   <li>入库累加 totalQuantity/totalCost 但<b>不重算 avgCost</b>（保持期初值），供期内出库作暂估成本</li>
 *   <li>出库取 balance.avgCost（期初加权平均）作为暂估单位成本；无期初 avgCost 时走 ZERO 路径</li>
 *   <li>不维护 cost layer（区别于 FIFO/LIFO/BATCH/SPECIFIC）</li>
 * </ul>
 *
 * <p>权威：{@code docs/design/finance/costing-methods.md §WEIGHTED_AVERAGE}。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvWeightedAverageCosting extends JunitAutoTestCase {

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
    public void testIncomingAccumulatesButDoesNotRecomputeAvgCost() {
        Long materialId = 2601L;
        seedWamMaterial(materialId);

        generateIncoming(materialId, "PR-WAM-001A", new BigDecimal("50"), new BigDecimal("10"));
        generateIncoming(materialId, "PR-WAM-001B", new BigDecimal("40"), new BigDecimal("12"));

        ErpInvStockBalance balance = findBalance(materialId);
        assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("90")),
                "两次入库累加 totalQuantity=90");
        assertEquals(0, balance.getTotalCost().compareTo(new BigDecimal("980")),
                "两次入库累加 totalCost=980（500+480）");
        // WAM 关键差异：入库不重算 avgCost（保持期初 ZERO）；移动加权平均会算成 980/90≈10.89
        assertEquals(0, balance.getAvgCost().compareTo(BigDecimal.ZERO),
                "WAM 入库不重算 avgCost（保持期初 ZERO，区别于 MOVING_AVERAGE）");
        assertEquals(ErpInvConstants.COST_METHOD_MONTHLY_WEIGHTED_AVERAGE, balance.getCostMethod(),
                "余额 costMethod=WEIGHTED_AVERAGE(20)");
    }

    @Test
    public void testOutgoingUsesFrozenAvgCostZeroWhenNoOpeningBalance() {
        Long materialId = 2602L;
        seedWamMaterial(materialId);

        generateIncoming(materialId, "PR-WAM-002", new BigDecimal("50"), new BigDecimal("10"));
        generateOutgoing(materialId, "SS-WAM-002", new BigDecimal("30"));

        // 无期初 avgCost（新物料）→ 出库取 avgCost=ZERO → 暂估成本 0（月末结账时经 reclosePeriodCosts 调整）
        ErpInvStockLedger outLedger = findOutgoingLedger(materialId);
        assertEquals(0, outLedger.getUnitCost().compareTo(BigDecimal.ZERO),
                "WAM 出库 unitCost=期初 avgCost=0（无期初余额走 ZERO 路径）");
        assertEquals(0, outLedger.getTotalCost().compareTo(BigDecimal.ZERO),
                "WAM 出库 totalCost=0（暂估，月末调整）");
        assertEquals(ErpInvConstants.COST_METHOD_MONTHLY_WEIGHTED_AVERAGE, outLedger.getCostMethod(),
                "流水 costMethod=WEIGHTED_AVERAGE(20)");

        ErpInvStockBalance balance = findBalance(materialId);
        assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("20")), "余额 50-30=20");
        assertEquals(0, balance.getTotalCost().compareTo(new BigDecimal("500")),
                "余额 totalCost 仍 500（出库暂估成本 0，未扣减；月末调整时再修正）");
    }

    @Test
    public void testWamCreatesNoCostLayers() {
        Long materialId = 2603L;
        seedWamMaterial(materialId);

        generateIncoming(materialId, "PR-WAM-003", new BigDecimal("50"), new BigDecimal("10"));
        generateOutgoing(materialId, "SS-WAM-003", new BigDecimal("20"));

        List<ErpInvCostLayer> layers = findCostLayers(materialId);
        assertTrue(layers.isEmpty(),
                "WAM 不维护 cost layer（全月均化，区别于 FIFO/LIFO/BATCH/SPECIFIC 的层消耗模型）");
    }

    @Test
    public void testReversePreservesFrozenAvgCost() {
        Long materialId = 2604L;
        seedWamMaterial(materialId);

        generateIncoming(materialId, "PR-WAM-004", new BigDecimal("50"), new BigDecimal("10"));
        Long outMoveId = generateOutgoing(materialId, "SS-WAM-004", new BigDecimal("30"));

        // 出库后 avgCost 仍为 ZERO（WAM 期初不变式）
        assertEquals(0, findBalance(materialId).getAvgCost().compareTo(BigDecimal.ZERO),
                "出库后 avgCost 仍 ZERO");

        // 红冲出库 → 反向入库，WAM onIncoming 累加但不重算 avgCost
        reverseMove(outMoveId);

        ErpInvStockBalance balance = findBalance(materialId);
        assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("50")),
                "红冲后 totalQuantity 恢复 50");
        assertEquals(0, balance.getTotalCost().compareTo(new BigDecimal("500")),
                "红冲后 totalCost 恢复 500");
        assertEquals(0, balance.getAvgCost().compareTo(BigDecimal.ZERO),
                "红冲后 avgCost 仍 ZERO（WAM 期初不变式贯穿红冲）");
    }

    // ---------- helpers ----------

    private Long generateIncoming(Long materialId, String billCode, BigDecimal qty, BigDecimal unitCost) {
        Map<String, Object> req = baseReq(ErpInvConstants.MOVE_TYPE_INCOMING);
        req.put("destWarehouseId", WAREHOUSE_ID);
        req.put("destLocationId", LOCATION_ID);
        req.put("relatedBillType", "PUR_RECEIPT");
        req.put("relatedBillCode", billCode);
        req.put("lines", Collections.singletonList(line(materialId, qty, unitCost)));
        return idOf(genMove(req));
    }

    private Long generateOutgoing(Long materialId, String billCode, BigDecimal qty) {
        Map<String, Object> req = baseReq(ErpInvConstants.MOVE_TYPE_OUTGOING);
        req.put("sourceWarehouseId", WAREHOUSE_ID);
        req.put("sourceLocationId", LOCATION_ID);
        req.put("relatedBillType", "SALES_SHIP");
        req.put("relatedBillCode", billCode);
        req.put("lines", Collections.singletonList(line(materialId, qty, null)));
        return idOf(genMove(req));
    }

    private void reverseMove(Long moveId) {
        ApiResponse<?> resp = executeRpc(mutation, "ErpInvStockMove__reverse",
                ApiRequest.build(Map.of("moveId", moveId)));
        assertEquals(0, resp.getStatus(), "reverse 应成功");
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

    private void seedWamMaterial(Long id) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            ErpMdMaterial material = new ErpMdMaterial();
            material.orm_propValueByName("id", id);
            material.setCode("MATW-" + id);
            material.setName("WAM Material " + id);
            material.orm_propValueByName("materialType", "GOODS");
            material.setUoMId(UOM_ID);
            material.setStatus("ACTIVE");
            material.setCostMethod(ErpInvConstants.COST_METHOD_MONTHLY_WEIGHTED_AVERAGE);
            dao.saveEntity(material);
        });
    }
}
