package app.erp.inv.service;

import app.erp.inv.dao.entity.ErpInvCostLayer;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockLedger;
import app.erp.inv.dao.entity.ErpInvStockMove;
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
import org.junit.jupiter.api.extension.RegisterExtension;

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
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Phase 2 集成测试：FIFO 策略（cost layer 维护/消耗 + COGS 经 ledger.totalCost 通道 + 红冲恢复 + 首次无成本拒绝）。
 *
 * <p>覆盖 plan Phase 2 退出标准：入库追加 cost layer remainingQuantity/unitCost；出库多层跨消耗加权 COGS；
 * 首次出库无成本抛 ERR_COST_NOT_AVAILABLE；红冲恢复（Decision (a)）；COGS 经 ledger.totalCost 流入派发器 TOTAL_COST。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvFifoCosting extends JunitAutoTestCase {

    @RegisterExtension
    static InvFrozenClockExtension frozenClock = new InvFrozenClockExtension();

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
        Long materialId = 2201L;
        seedFifoMaterial(materialId);

        generateIncoming(materialId, "PR-FIFO-001", new BigDecimal("50"), new BigDecimal("10"));

        List<ErpInvCostLayer> layers = findCostLayers(materialId);
        assertEquals(1, layers.size(), "入库应追加 1 个 cost layer");
        ErpInvCostLayer layer = layers.get(0);
        assertEquals(0, layer.getIncomingQuantity().compareTo(new BigDecimal("50")), "incomingQuantity=50");
        assertEquals(0, layer.getRemainingQuantity().compareTo(new BigDecimal("50")), "remainingQuantity=50");
        assertEquals(0, layer.getUnitCost().compareTo(new BigDecimal("10")), "unitCost=10");
        assertEquals(0, layer.getTotalCost().compareTo(new BigDecimal("500")), "totalCost=500");
        assertEquals(ErpInvConstants.COST_METHOD_FIFO, layer.getCostMethod(), "costMethod=FIFO(30)");

        ErpInvStockBalance balance = findBalance(materialId);
        assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("50")), "余额 totalQuantity=50");
        assertEquals(0, balance.getTotalCost().compareTo(new BigDecimal("500")), "余额 totalCost=500");
        assertNull(balance.getAvgCost(), "FIFO 物料 avgCost 置空");
    }

    @Test
    public void testOutgoingConsumesSingleLayer() {
        Long materialId = 2202L;
        seedFifoMaterial(materialId);

        generateIncoming(materialId, "PR-FIFO-002", new BigDecimal("50"), new BigDecimal("10"));
        generateOutgoing(materialId, "SS-FIFO-002", new BigDecimal("30"));

        List<ErpInvCostLayer> layers = findCostLayers(materialId);
        assertEquals(1, layers.size(), "单层消耗不删层");
        assertEquals(0, layers.get(0).getRemainingQuantity().compareTo(new BigDecimal("20")),
                "队列1 50@10 消耗 30 → remainingQuantity=20");

        ErpInvStockLedger outLedger = findOutgoingLedger(materialId);
        assertEquals(0, outLedger.getUnitCost().compareTo(new BigDecimal("10")), "出库 unitCost=10（单层）");
        // 出库流水 totalCost 按符号约定为负（与移动加权平均一致），InvPostingDispatcher.buildEvent 用 .abs() 拾取
        assertEquals(0, outLedger.getTotalCost().compareTo(new BigDecimal("-300")), "出库 totalCost=-300（30×10，负号）");
        assertEquals(0, outLedger.getQuantity().compareTo(new BigDecimal("-30")), "出库 quantity 负 -30");

        ErpInvStockBalance balance = findBalance(materialId);
        assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("20")), "余额 50-30=20");
        assertEquals(0, balance.getTotalCost().compareTo(new BigDecimal("200")), "余额 500-300=200");
    }

    @Test
    public void testOutgoingSpansMultipleLayersWeightedCost() {
        Long materialId = 2203L;
        seedFifoMaterial(materialId);

        generateIncoming(materialId, "PR-FIFO-003A", new BigDecimal("50"), new BigDecimal("10"));
        generateIncoming(materialId, "PR-FIFO-003B", new BigDecimal("40"), new BigDecimal("12"));
        generateOutgoing(materialId, "SS-FIFO-003", new BigDecimal("60"));

        List<ErpInvCostLayer> layers = findCostLayers(materialId);
        assertEquals(2, layers.size(), "多层消耗不删层（remainingQuantity=0 仍保留供追溯）");

        ErpInvCostLayer layer1 = layers.stream()
                .filter(l -> l.getUnitCost().compareTo(new BigDecimal("10")) == 0).findFirst().orElseThrow();
        assertEquals(0, layer1.getRemainingQuantity().compareTo(BigDecimal.ZERO),
                "队列1 50@10 全消耗 → remainingQuantity=0");

        ErpInvCostLayer layer2 = layers.stream()
                .filter(l -> l.getUnitCost().compareTo(new BigDecimal("12")) == 0).findFirst().orElseThrow();
        assertEquals(0, layer2.getRemainingQuantity().compareTo(new BigDecimal("30")),
                "队列2 40@12 消耗 10 → remainingQuantity=30");

        ErpInvStockLedger outLedger = findOutgoingLedger(materialId);
        // 50×10 + 10×12 = 620，加权 unitCost = 620/60 ≈ 10.333333；totalCost 按符号约定为负
        assertEquals(0, outLedger.getTotalCost().compareTo(new BigDecimal("-620")),
                "跨层消耗 COGS totalCost=-620（50×10+10×12，负号）");
        assertEquals(0, outLedger.getQuantity().compareTo(new BigDecimal("-60")), "出库 quantity 负 -60");

        ErpInvStockBalance balance = findBalance(materialId);
        assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("30")), "余额 (50+40)-60=30");
        assertEquals(0, balance.getTotalCost().compareTo(new BigDecimal("360")),
                "余额 totalCost=(500+480)-620=360 (=30×12 队列2 剩余)");
    }

    @Test
    public void testFirstOutgoingWithoutCostLayerRejected() {
        Long materialId = 2204L;
        seedFifoMaterial(materialId);
        // 不先入库，直接出库 → ERR_COST_NOT_AVAILABLE（无 remainingQuantity>0 的层）
        // 注：预留量校验（available<0）会先于 FIFO 记账触发，故启用负库存放行预留检查后才能验证 FIFO 拒绝
        setNegativeStock(true);
        try {
            ApiResponse<?> resp = genMove(outgoingReq(materialId, "SS-FIFO-004", new BigDecimal("5")));
            assertEquals(ErpInvErrors.ERR_COST_NOT_AVAILABLE.getErrorCode(), resp.getCode(),
                    "FIFO 物料未入库直接出库应返回 ERR_COST_NOT_AVAILABLE");
            List<ErpInvCostLayer> layers = findCostLayers(materialId);
            assertTrue(layers.isEmpty(), "拒绝不应残留 cost layer");
        } finally {
            setNegativeStock(false);
        }
    }

    @Test
    public void testReverseRestoresCostInvariant() {
        Long materialId = 2205L;
        seedFifoMaterial(materialId);

        // 初始入库：队列1 50@10 + 队列2 40@12 → 总 90 / 总成本 980
        Long in1 = generateIncoming(materialId, "PR-FIFO-005A", new BigDecimal("50"), new BigDecimal("10"));
        generateIncoming(materialId, "PR-FIFO-005B", new BigDecimal("40"), new BigDecimal("12"));

        BigDecimal totalCostBeforeOut = sumRemainingCost(findCostLayers(materialId));
        assertEquals(0, totalCostBeforeOut.compareTo(new BigDecimal("980")), "出库前 Σ layer cost=980");

        // 出库 60 → 消耗 50@10 + 10@12 = 620；剩 0@10 + 30@12=360
        Long outMoveId = generateOutgoing(materialId, "SS-FIFO-005", new BigDecimal("60"));

        BigDecimal totalCostAfterOut = sumRemainingCost(findCostLayers(materialId));
        assertEquals(0, totalCostAfterOut.compareTo(new BigDecimal("360")), "出库后 Σ layer cost=360");

        // 红冲出库：reverse 生成反向入库（incoming）60 行，line.unitCost 已被 FIFO 刷新为加权 10.333...
        // → onIncoming 追加新层 60@(620/60) → 总成本回加 620 → Σ = 360 + 620 = 980（不变量成立）
        reverseMove(outMoveId);

        List<ErpInvCostLayer> layersAfterReverse = findCostLayers(materialId);
        BigDecimal totalCostAfterReverse = sumRemainingCost(layersAfterReverse);
        // 加权 unitCost（620/60=10.333333）经 scale=6 舍入后回乘 60=619.99998，Σ 恢复至 980 存在舍入残差（< 0.01）
        BigDecimal reverseDrift = totalCostAfterReverse.subtract(new BigDecimal("980")).abs();
        assertTrue(reverseDrift.compareTo(new BigDecimal("0.01")) < 0,
                "红冲后 Σ layer cost 应恢复至 980（容差 0.01，实际=" + totalCostAfterReverse + "）");

        ErpInvStockBalance balance = findBalance(materialId);
        assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("90")),
                "红冲后余额 totalQuantity 恢复 90");
        // 同 Σ layer cost：余额 totalCost 由 onIncoming 按 unitCost×qty 累加，存在同源舍入残差（< 0.01）
        BigDecimal balanceCostDrift = balance.getTotalCost().subtract(new BigDecimal("980")).abs();
        assertTrue(balanceCostDrift.compareTo(new BigDecimal("0.01")) < 0,
                "红冲后余额 totalCost 恢复 980（容差 0.01，实际=" + balance.getTotalCost() + "）");

        // 应至少有 3 个层（原队列1=0/队列2=30/新层=60@10.333...）
        long nonZeroLayers = layersAfterReverse.stream()
                .filter(l -> l.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0).count();
        assertTrue(nonZeroLayers >= 2, "红冲后至少 2 个 remainingQuantity>0 的层（队列2 + 新层）");
    }

    @Test
    public void testOutgoingLedgerTotalCostFlowsToDispatcher() {
        Long materialId = 2206L;
        seedFifoMaterial(materialId);

        generateIncoming(materialId, "PR-FIFO-006A", new BigDecimal("20"), new BigDecimal("10"));
        generateIncoming(materialId, "PR-FIFO-006B", new BigDecimal("40"), new BigDecimal("12"));
        generateOutgoing(materialId, "SS-FIFO-006", new BigDecimal("60"));

        ErpInvStockLedger outLedger = findOutgoingLedger(materialId);
        // 20×10 + 40×12 = 680；出库 totalCost 按符号约定为负（InvPostingDispatcher.buildEvent 用 .abs() 拾取）
        assertEquals(0, outLedger.getTotalCost().compareTo(new BigDecimal("-680")),
                "COGS 经 ledger.totalCost 流入 InvPostingDispatcher.TOTAL_COST（20×10+40×12=680，负号）");
        assertEquals(ErpInvConstants.COST_METHOD_FIFO, outLedger.getCostMethod(), "流水 costMethod=FIFO(30)");
    }

    // ---------- helpers ----------

    private Long generateIncoming(Long materialId, String billCode, BigDecimal qty, BigDecimal unitCost) {
        Map<String, Object> req = baseReq(materialId, ErpInvConstants.MOVE_TYPE_INCOMING);
        req.put("destWarehouseId", WAREHOUSE_ID);
        req.put("destLocationId", LOCATION_ID);
        req.put("relatedBillType", "PUR_RECEIPT");
        req.put("relatedBillCode", billCode);
        req.put("lines", Collections.singletonList(line(materialId, qty, unitCost)));
        return idOf(genMove(req));
    }

    private Long generateOutgoing(Long materialId, String billCode, BigDecimal qty) {
        Map<String, Object> req = baseReq(materialId, ErpInvConstants.MOVE_TYPE_OUTGOING);
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

    private Map<String, Object> outgoingReq(Long materialId, String billCode, BigDecimal qty) {
        Map<String, Object> req = baseReq(materialId, ErpInvConstants.MOVE_TYPE_OUTGOING);
        req.put("sourceWarehouseId", WAREHOUSE_ID);
        req.put("sourceLocationId", LOCATION_ID);
        req.put("relatedBillType", "SALES_SHIP");
        req.put("relatedBillCode", billCode);
        req.put("lines", Collections.singletonList(line(materialId, qty, null)));
        return req;
    }

    private Map<String, Object> baseReq(Long materialId, String moveType) {
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
                .orElseGet(() -> fail("应有出库流水"));
    }

    private static <V> V fail(String msg) {
        org.junit.jupiter.api.Assertions.fail(msg);
        return null;
    }

    private BigDecimal sumRemainingCost(List<ErpInvCostLayer> layers) {
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpInvCostLayer l : layers) {
            BigDecimal rem = l.getRemainingQuantity() != null ? l.getRemainingQuantity() : BigDecimal.ZERO;
            BigDecimal unit = l.getUnitCost() != null ? l.getUnitCost() : BigDecimal.ZERO;
            sum = sum.add(rem.multiply(unit));
        }
        return sum;
    }

    private void seedFifoMaterial(Long id) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            ErpMdMaterial material = new ErpMdMaterial();
            material.orm_propValueByName("id", id);
            material.setCode("MATF-" + id);
            material.setName("FIFO Material " + id);
            material.orm_propValueByName("materialType", "GOODS");
            material.setUoMId(UOM_ID);
            material.setStatus("ACTIVE");
            material.setCostMethod(ErpInvConstants.COST_METHOD_FIFO);
            dao.saveEntity(material);
        });
    }

    private void setNegativeStock(boolean value) {
        io.nop.api.core.config.AppConfig.getConfigProvider()
                .assignConfigValue(ErpInvConstants.CONFIG_ALLOW_NEGATIVE_STOCK, String.valueOf(value));
    }
}
